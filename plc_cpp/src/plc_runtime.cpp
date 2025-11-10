#include "plc_runtime.h"
#include "shared_memory.h"
#include <iostream>
#include <thread>
#include <chrono>
// 移除对 <filesystem> 的依赖
// #include <filesystem>
#include <signal.h>
#include <algorithm>
#include <cerrno>
#include <cstring>
#include <sys/socket.h>
#include <sys/select.h>
#include <sys/time.h>
#include <unistd.h>

// 静态成员定义
const std::vector<int> PLCRuntime::INPUT_PINS = {74, 91, 92};
const std::vector<int> PLCRuntime::OUTPUT_PINS = {35, 46, 37, 71, 72, 73};
// ENABLE_PIN 是 public static constexpr, 不需要在这里定义

const std::vector<int>& PLCRuntime::get_input_pins() {
    return INPUT_PINS;
}

const std::vector<int>& PLCRuntime::get_output_pins() {
    return OUTPUT_PINS;
}

PLCRuntime::PLCRuntime(SharedMemoryManager& shm,
                       const std::string& system_config, 
                       const std::string& user_config,
                       const std::string& unified_config) 
    : shm_(shm),
      running_(false), 
      initialized_(false), 
      scan_time_us_(DEFAULT_SCAN_INTERVAL_MS * 1000),
      system_config_file_(system_config),
      user_config_file_(user_config),
      unified_config_file_(unified_config),
      modbus_ctx_(nullptr),
      modbus_mapping_(nullptr),
      modbus_server_socket_(-1),
      modbus_client_socket_(-1) {
    // 构造函数现在非常干净，不创建任何核心组件
}

PLCRuntime::~PLCRuntime() {
    shutdown();
}

bool PLCRuntime::init() {
    // 在单线程初始化阶段，不需要锁
    // std::lock_guard<std::mutex> lock(runtime_mutex_);
    
    if (initialized_.load()) {
        return true;
    }
    
    std::cout << "PLC运行时初始化 v3.0..." << std::endl;
    
    // === 在init()函数中实例化所有核心组件 ===
    gpio_ = std::make_unique<GPIODriver>();
    timer_mgr_ = std::make_unique<TimerManager>();
    counter_mgr_ = std::make_unique<CounterManager>();
    ladder_ = std::make_unique<LadderEngine>(*timer_mgr_, *counter_mgr_);
    watchdog_ = std::make_unique<Watchdog>(10.0); // 10秒超时
    
    // 1. 初始化GPIO
    if (!gpio_->init(INPUT_PINS, OUTPUT_PINS, ENABLE_PIN, INDICATOR_PIN)) {
        handle_error(ERR_GPIO_INIT_FAILED);
        return false;
    }
    
    // 2. 检查共享内存 (它是在外部创建的)
    if (!shm_.is_valid()) {
        handle_error(ERR_SHM_ACCESS_FAILED);
        return false;
    }
    
    // 2. 重置共享内存状态，避免上一轮遗留数据影响启动
    reset_shared_memory_state();

    // 3. 加载配置
    try {
        std::cout << "[WORKER_DEBUG] 尝试加载单一配置文件: " << unified_config_file_ << std::endl;
        if (ladder_->load_unified_config(unified_config_file_)) {
            std::cout << "[WORKER_DEBUG] 单一配置文件加载成功。" << std::endl;
        } else {
            std::cout << "[WORKER_DEBUG] 单一配置文件加载失败或不存在，回落到双文件模式..." << std::endl;
            // 清理可能加载了一半的配置
            ladder_->clear_configs(); 
            
            if (!ladder_->load_system_config(system_config_file_)) {
                std::cerr << "警告: 系统配置文件加载失败: " << system_config_file_ << std::endl;
            }
            if (!ladder_->load_user_config(user_config_file_)) {
                std::cerr << "警告: 用户配置文件加载失败: " << user_config_file_ << std::endl;
            }
            ladder_->merge_configs();
        }
    } catch (const std::exception& e) {
        std::cerr << "加载配置时发生异常: " << e.what() << std::endl;
        handle_error(ERR_CONFIG_PARSE_ERROR);
        return false;
    }
    
    std::cout << "[WORKER_DEBUG] 配置加载完成。" << std::endl;
    
    // 4. 初始化定时器和计数器
    std::cout << "[WORKER_DEBUG] 开始初始化定时器..." << std::endl;
    const auto& timer_configs = ladder_->get_timer_configs();
    std::cout << "[WORKER_DEBUG] 获取到 " << timer_configs.size() << " 个定时器配置。" << std::endl;
    for (const auto& timer_cfg : timer_configs) {
        std::cout << "[WORKER_DEBUG] 正在添加定时器: " << timer_cfg.name << std::endl;
        timer_mgr_->add_timer(timer_cfg.name, timer_cfg.preset, timer_cfg.alias);
    }
    std::cout << "[WORKER_DEBUG] 定时器初始化完成。" << std::endl;

    std::cout << "[WORKER_DEBUG] 开始初始化计数器..." << std::endl;
    const auto& counter_configs = ladder_->get_counter_configs();
    std::cout << "[WORKER_DEBUG] 获取到 " << counter_configs.size() << " 个计数器配置。" << std::endl;
    for (const auto& counter_cfg : counter_configs) {
        std::cout << "[WORKER_DEBUG] 正在添加计数器: " << counter_cfg.name << std::endl;
        counter_mgr_->add_counter(counter_cfg.name, counter_cfg.preset, counter_cfg.alias);
    }
    std::cout << "[WORKER_DEBUG] 计数器初始化完成。" << std::endl;
    
    // 5. 设置看门狗回调
    watchdog_->set_callback([this]() {
        std::cerr << "看门狗超时，执行紧急停止" << std::endl;
        emergency_stop();
    });
    
    // 6. 启动看门狗
    watchdog_->start();

    // 7. 初始化 Modbus 服务（可选但强烈推荐）
    if (!init_modbus()) {
        handle_error(ERR_MODBUS_INIT_FAILED);
        return false;
    }
    
    initialized_ = true;
    std::cout << "PLC运行时初始化完成" << std::endl;
    return true;
}

void PLCRuntime::shutdown() {
    // 在单线程关闭阶段，不需要锁
    // std::lock_guard<std::mutex> lock(runtime_mutex_);
    
    if (!initialized_.load()) {
        return;
    }

    std::cout << "PLC运行时关闭中..." << std::endl;
    
    // 停止运行
    stop();
    
    initialized_ = false;
    std::cout << "PLC运行时已关闭" << std::endl;
}

void PLCRuntime::run() {
    if (!init()) {
        std::cerr << "PLC初始化失败" << std::endl;
        return;
    }
    
    running_ = true;
    std::cout << "PLC控制器开始运行..." << std::endl;
 
    start_indicator();

    // 启动主循环线程
    main_thread_ = std::thread(&PLCRuntime::main_loop, this);
    
    // 启动配置监控线程
    config_watcher_thread_ = std::thread(&PLCRuntime::config_watcher_loop, this);
    
    // 启动心跳线程
    heartbeat_thread_ = std::thread(&PLCRuntime::heartbeat_loop, this);

    // 启动 Modbus 服务线程
    if (modbus_ctx_ && modbus_mapping_) {
        modbus_thread_ = std::thread(&PLCRuntime::modbus_loop, this);
    }
    
    // 等待主循环结束
    if (main_thread_.joinable()) {
        main_thread_.join();
    }
    
    // 等待其他线程结束
    if (config_watcher_thread_.joinable()) {
        config_watcher_thread_.join();
    }
    
    if (heartbeat_thread_.joinable()) {
        heartbeat_thread_.join();
    }

    if (indicator_thread_.joinable()) {
        indicator_thread_.join();
    }
}

void PLCRuntime::start_indicator() {
    if (!gpio_ || !gpio_->has_indicator()) {
        return;
    }

    // 避免重复启动
    bool expected = false;
    if (!indicator_running_.compare_exchange_strong(expected, true)) {
        return;
    }

    // 启动前先拉低一次，进入慢闪节奏
    gpio_->set_indicator(false);

    indicator_thread_ = std::thread(&PLCRuntime::indicator_loop, this);
}

void PLCRuntime::indicator_loop() {
    constexpr auto slow_interval = std::chrono::milliseconds(2000);  // 慢闪：1秒（PLC+YOLO就绪）
    constexpr auto fast_interval = std::chrono::milliseconds(300);   // 快闪：0.3秒（仅PLC就绪）
    bool next_state = true;

    while (indicator_running_.load()) {
        if (!running_.load()) {
            break;
        }

        // 检查 M39（YOLO就绪标志）状态
        bool yolo_ready = false;
        if (shm_.is_valid()) {
            yolo_ready = shm_.data()->memory[PLCConstants::M_YOLO_READY].load();
        }

        // 根据YOLO就绪状态选择闪烁间隔
        auto interval = yolo_ready ? slow_interval : fast_interval;

        std::this_thread::sleep_for(interval);

        if (!indicator_running_.load()) {
            break;
        }

        if (gpio_ && gpio_->has_indicator()) {
            gpio_->set_indicator(next_state);
            next_state = !next_state;
        }
    }

    indicator_running_.store(false);

    if (gpio_ && gpio_->has_indicator()) {
        gpio_->set_indicator(true);
    }
}

void PLCRuntime::stop_indicator(bool set_high) {
    indicator_running_.store(false);

    if (indicator_thread_.joinable()) {
        indicator_thread_.join();
    }

    if (set_high && gpio_ && gpio_->has_indicator()) {
        gpio_->set_indicator(true);
    }
}

void PLCRuntime::stop() {
    running_.store(false);
    
    std::cout << "PLC控制器停止中..." << std::endl;
    
    // 1. 停止看门狗，避免在关闭过程中超时
    if (watchdog_) {
        watchdog_->stop();
    }
    
    // 2. 通知 Modbus 线程即将关闭，唤醒阻塞调用
    signal_modbus_shutdown();
    
    // 3. 等待 Modbus 线程退出并清理资源
    if (modbus_thread_.joinable()) {
        modbus_thread_.join();
    }
    shutdown_modbus();

    // 4. 停止指示灯线程，恢复指示灯常亮
    stop_indicator(true);

    // 5. 立即执行紧急关闭，确保硬件进入安全态
    if (gpio_) {
        gpio_->emergency_shutdown();
    }

    // 6. 清空共享内存状态，使软件视图与硬件一致
    if (shm_.is_valid()) {
        auto* shm_ptr = shm_.data();
        const size_t limit = std::min(static_cast<size_t>(PLCConstants::MAX_OUTPUTS), OUTPUT_PINS.size());
        for (size_t i = 0; i < limit; ++i) {
            shm_ptr->outputs[i].store(false);
            int status_index = PLCConstants::M_STATUS_START + static_cast<int>(i);
            if (status_index < PLCConstants::MAX_MEMORY) {
                shm_ptr->memory[status_index].store(false);
            }
        }
        shm_.sync();
    }
    
    initialized_.store(false);

    std::cout << "PLC控制器已停止" << std::endl;
}

void PLCRuntime::scan_cycle() {
    auto start_time = std::chrono::high_resolution_clock::now();
    
    // 1. 读取输入
    std::vector<bool> inputs = gpio_->read_all_inputs();
    
    // 2. 更新共享内存中的输入状态
    for (size_t i = 0; i < inputs.size() && i < PLCConstants::MAX_INPUTS; ++i) {
        shm_.data()->inputs[i].store(inputs[i]);
    }
    
    // Standard PLC Scan Cycle Order:
    // 1. Update timers and counters based on the *previous* cycle's logic.
    const double delta_time = scan_time_us_.load() / 1000000.0;
    timer_mgr_->update_timers(shm_.data(), delta_time, ladder_->get_enabled_timers());
    counter_mgr_->update_counters(shm_.data(), ladder_->get_triggered_counters());
    
    // 2. Execute ladder logic for the *current* cycle.
    // This will populate the timer/counter enable sets for the *next* cycle.
    ladder_->execute_cycle(shm_.data(), inputs, delta_time);
    
    // 3. Write outputs to GPIO.
    for (int i = 0; i < PLCConstants::MAX_OUTPUTS; ++i) {
        bool output_state = shm_.data()->outputs[i].load();
        gpio_->write_output(i, output_state);
    }

    // Finalize ladder state for next cycle
    ladder_->finalize_cycle();
    
    // 4. Update output status to internal memory M46-M51.
    for (int i = 0; i < PLCConstants::MAX_OUTPUTS; ++i) {
        bool output_state = shm_.data()->outputs[i].load();
        shm_.data()->memory[PLCConstants::M_STATUS_START + i].store(output_state);
    }
    
    // 5. YOLO 心跳检查：每15秒清空M39（YOLO就绪标志）
    static auto last_yolo_heartbeat_clear = std::chrono::steady_clock::now();
    auto now = std::chrono::steady_clock::now();
    auto elapsed = std::chrono::duration_cast<std::chrono::seconds>(now - last_yolo_heartbeat_clear).count();
    if (elapsed >= 15) {
        shm_.data()->memory[PLCConstants::M_YOLO_READY].store(false);
        last_yolo_heartbeat_clear = now;
    }
    
    // 5. Calculate scan time.
    auto end_time = std::chrono::high_resolution_clock::now();
    double actual_scan_time = std::chrono::duration<double, std::micro>(end_time - start_time).count();
    scan_time_us_.store(actual_scan_time);
    shm_.data()->scan_time_us.store(actual_scan_time);
    shm_.data()->scan_counter.fetch_add(1);
    
    // 6. Check for scan timeout.
    if (actual_scan_time > DEFAULT_SCAN_INTERVAL_MS * 1000) {
        std::cerr << "扫描周期超时: " << actual_scan_time / 1000.0 << "ms" << std::endl;
        handle_error(ERR_SCAN_TIMEOUT);
    }
    
    // 7. Feed the watchdog.
    watchdog_->feed();
    
    // 8. Sync shared memory.
    shm_.sync();
}

void PLCRuntime::main_loop() {
    std::cout << "PLC主循环开始" << std::endl;
    
    while (running_.load()) {
        try {
            scan_cycle();
            
            // 控制扫描周期
            auto sleep_time = std::chrono::microseconds(
                static_cast<int>(DEFAULT_SCAN_INTERVAL_MS * 1000 - scan_time_us_.load()));
            
            if (sleep_time.count() > 0) {
                std::this_thread::sleep_for(sleep_time);
            }
        } catch (const std::exception& e) {
            std::cerr << "扫描周期异常: " << e.what() << std::endl;
            handle_error(ERR_SCAN_TIMEOUT);
        }
    }
    
    std::cout << "PLC主循环结束" << std::endl;
}

void PLCRuntime::config_watcher_loop() {
    // 暂时禁用此功能以保证 fork-safety
    // 后续可使用 inotify 等更安全的机制实现
    while (running_.load()) {
        std::this_thread::sleep_for(std::chrono::milliseconds(200));
    }

    /*  --- DISABLED CODE ---
    // 确定要监控哪个文件
    // ...
    */
}

void PLCRuntime::heartbeat_loop() {
    std::cout << "心跳线程开始" << std::endl;
    
    while (running_.load()) {
        if (shm_.is_valid()) {
            uint32_t heartbeat = shm_.data()->heartbeat.load();
            shm_.data()->heartbeat.store(heartbeat + 1);
        }
        
        std::this_thread::sleep_for(std::chrono::seconds(1));
    }
    
    std::cout << "心跳线程结束" << std::endl;
}

bool PLCRuntime::init_modbus() {
    if (modbus_ctx_ || modbus_mapping_) {
        return true;
    }

    modbus_ctx_ = modbus_new_tcp(nullptr, 502);
    if (!modbus_ctx_) {
        std::cerr << "[MODBUS] 无法创建 TCP 上下文: " << modbus_strerror(errno) << std::endl;
        return false;
    }

    modbus_set_slave(modbus_ctx_, 1);

    timeval response_timeout {0, 500000}; // 500ms
    modbus_set_response_timeout(modbus_ctx_, response_timeout.tv_sec, response_timeout.tv_usec);
    modbus_set_byte_timeout(modbus_ctx_, 0, 0);

    modbus_mapping_ = modbus_mapping_new(MODBUS_COIL_COUNT,
                                         MODBUS_DISCRETE_COUNT,
                                         MODBUS_HOLDING_REGISTER_COUNT,
                                         MODBUS_INPUT_REGISTER_COUNT);
    if (!modbus_mapping_) {
        std::cerr << "[MODBUS] 创建映射失败: " << modbus_strerror(errno) << std::endl;
        modbus_free(modbus_ctx_);
        modbus_ctx_ = nullptr;
        return false;
    }

    modbus_coil_shadow_.assign(MODBUS_COIL_COUNT, 0);
    std::fill_n(modbus_mapping_->tab_bits, MODBUS_COIL_COUNT, 0);
    std::fill_n(modbus_mapping_->tab_input_bits, MODBUS_DISCRETE_COUNT, 0);
    std::fill_n(modbus_mapping_->tab_registers, MODBUS_HOLDING_REGISTER_COUNT, 0);
    std::fill_n(modbus_mapping_->tab_input_registers, MODBUS_INPUT_REGISTER_COUNT, 0);

    return true;
}

void PLCRuntime::signal_modbus_shutdown() {
    std::lock_guard<std::mutex> lock(modbus_mutex_);
    if (modbus_client_socket_ >= 0) {
        ::shutdown(modbus_client_socket_, SHUT_RDWR);
    }
    if (modbus_server_socket_ >= 0) {
        ::shutdown(modbus_server_socket_, SHUT_RDWR);
    }
}

void PLCRuntime::shutdown_modbus() {
    std::lock_guard<std::mutex> lock(modbus_mutex_);
    if (modbus_client_socket_ >= 0) {
        close(modbus_client_socket_);
        modbus_client_socket_ = -1;
    }
    if (modbus_server_socket_ >= 0) {
        close(modbus_server_socket_);
        modbus_server_socket_ = -1;
    }
    if (modbus_mapping_) {
        modbus_mapping_free(modbus_mapping_);
        modbus_mapping_ = nullptr;
    }
    if (modbus_ctx_) {
        modbus_close(modbus_ctx_);
        modbus_free(modbus_ctx_);
        modbus_ctx_ = nullptr;
    }
    modbus_coil_shadow_.clear();
}

// 处理单个 Modbus 客户端连接的函数
void PLCRuntime::handle_modbus_client(int client_socket) {
    // 为每个连接创建独立的 modbus 上下文
    modbus_t* client_ctx = modbus_new_tcp(nullptr, 0);
    if (!client_ctx) {
        std::cerr << "[MODBUS] 无法为客户端创建上下文: " << modbus_strerror(errno) << std::endl;
        close(client_socket);
        return;
    }

    modbus_set_slave(client_ctx, 1);
    timeval response_timeout {0, 500000}; // 500ms
    modbus_set_response_timeout(client_ctx, response_timeout.tv_sec, response_timeout.tv_usec);
    modbus_set_byte_timeout(client_ctx, 0, 0);
    modbus_set_socket(client_ctx, client_socket);

    std::cout << "[MODBUS] 新客户端连接，socket=" << client_socket << std::endl;

    while (running_.load()) {
        uint8_t query[MODBUS_TCP_MAX_ADU_LENGTH];
        int rc = modbus_receive(client_ctx, query);
        if (rc > 0) {
            // 更新映射（需要加锁）
            {
                std::lock_guard<std::mutex> lock(modbus_mutex_);
                update_modbus_mapping();
            }
            
            // 回复请求（使用共享的映射）
            if (modbus_reply(client_ctx, query, rc, modbus_mapping_) == -1) {
                if (running_.load()) {
                    std::cerr << "[MODBUS] 回复失败: " << modbus_strerror(errno) << std::endl;
                }
                break;
            }
            
            // 应用写入（需要加锁）
            {
                std::lock_guard<std::mutex> lock(modbus_mutex_);
                apply_modbus_writes();
            }
        } else if (rc == -1) {
            if (!running_.load()) {
                break;
            }
            if (errno == ETIMEDOUT || errno == EAGAIN) {
                continue;
            }
            if (errno != ECONNRESET) {
                std::cerr << "[MODBUS] 接收失败: " << modbus_strerror(errno) << std::endl;
            }
            break;
        } else {
            // rc == 0 表示对方关闭连接
            break;
        }
    }

    std::cout << "[MODBUS] 客户端断开连接，socket=" << client_socket << std::endl;
    modbus_close(client_ctx);
    modbus_free(client_ctx);
    close(client_socket);
}

void PLCRuntime::modbus_loop() {
    if (!modbus_ctx_ || !modbus_mapping_) {
        return;
    }

    {
        std::lock_guard<std::mutex> lock(modbus_mutex_);
        modbus_server_socket_ = modbus_tcp_listen(modbus_ctx_, 10); // 增加 backlog 到 10
    }

    if (modbus_server_socket_ == -1) {
        std::cerr << "[MODBUS] 监听失败: " << modbus_strerror(errno) << std::endl;
        return;
    }

    std::cout << "[MODBUS] 服务正在监听端口 502（支持多连接）..." << std::endl;

    update_modbus_mapping();

    // 存储所有客户端线程
    std::vector<std::thread> client_threads;

    while (running_.load()) {
        int client_socket = modbus_tcp_accept(modbus_ctx_, &modbus_server_socket_);
        if (client_socket == -1) {
            if (!running_.load()) {
                break;
            }
            if (errno == EINTR || errno == EAGAIN) {
                continue;
            }
            std::cerr << "[MODBUS] 接受连接失败: " << modbus_strerror(errno) << std::endl;
            std::this_thread::sleep_for(std::chrono::milliseconds(200));
            continue;
        }

        // 为每个客户端连接创建独立线程
        client_threads.emplace_back(&PLCRuntime::handle_modbus_client, this, client_socket);
        client_threads.back().detach(); // 分离线程，让它独立运行
    }

    // 等待所有客户端线程结束
    for (auto& t : client_threads) {
        if (t.joinable()) {
            t.join();
        }
    }

    {
        std::lock_guard<std::mutex> lock(modbus_mutex_);
        if (modbus_server_socket_ >= 0) {
            close(modbus_server_socket_);
            modbus_server_socket_ = -1;
        }
    }
}

void PLCRuntime::update_modbus_mapping() {
    if (!modbus_mapping_ || !shm_.is_valid()) {
        return;
    }

    PLCSharedMemory* shm_ptr = shm_.data();
    if (!shm_ptr) {
        return;
    }

    if (static_cast<int>(modbus_coil_shadow_.size()) != MODBUS_COIL_COUNT) {
        modbus_coil_shadow_.assign(MODBUS_COIL_COUNT, 0);
    }

    const int coil_outputs = PLCConstants::MAX_OUTPUTS;
    const int coil_memory = PLCConstants::MAX_MEMORY;

    for (int i = 0; i < coil_outputs; ++i) {
        uint8_t value = shm_ptr->outputs[i].load() ? 1 : 0;
        modbus_mapping_->tab_bits[i] = value;
        modbus_coil_shadow_[i] = value;
    }

    for (int i = 0; i < coil_memory; ++i) {
        uint8_t value = shm_ptr->memory[i].load() ? 1 : 0;
        int idx = coil_outputs + i;
        modbus_mapping_->tab_bits[idx] = value;
        modbus_coil_shadow_[idx] = value;
    }

    for (int i = 0; i < PLCConstants::MAX_INPUTS; ++i) {
        modbus_mapping_->tab_input_bits[i] = shm_ptr->inputs[i].load() ? 1 : 0;
    }

    uint64_t scan_counter = shm_ptr->scan_counter.load();
    double scan_time_us = shm_ptr->scan_time_us.load();
    uint32_t error_code = shm_ptr->error_code.load();
    uint32_t heartbeat = shm_ptr->heartbeat.load();
    uint16_t emergency = shm_ptr->emergency_stop.load() ? 1 : 0;

    if (MODBUS_HOLDING_REGISTER_COUNT > 0) {
        modbus_mapping_->tab_registers[0] = static_cast<uint16_t>(scan_counter & 0xFFFF);
    }
    if (MODBUS_HOLDING_REGISTER_COUNT > 1) {
        modbus_mapping_->tab_registers[1] = static_cast<uint16_t>((scan_counter >> 16) & 0xFFFF);
    }
    if (MODBUS_HOLDING_REGISTER_COUNT > 2) {
        uint32_t scan_time_ms100 = static_cast<uint32_t>(std::clamp(scan_time_us / 1000.0, 0.0, 65535.0));
        modbus_mapping_->tab_registers[2] = static_cast<uint16_t>(scan_time_ms100 & 0xFFFF);
    }
    if (MODBUS_HOLDING_REGISTER_COUNT > 3) {
        modbus_mapping_->tab_registers[3] = static_cast<uint16_t>(std::min<uint32_t>(error_code, 0xFFFF));
    }
    if (MODBUS_HOLDING_REGISTER_COUNT > 4) {
        modbus_mapping_->tab_registers[4] = static_cast<uint16_t>(heartbeat & 0xFFFF);
    }
    if (MODBUS_HOLDING_REGISTER_COUNT > 5) {
        modbus_mapping_->tab_registers[5] = emergency;
    }

    for (int i = 0; i < MODBUS_INPUT_REGISTER_COUNT; ++i) {
        modbus_mapping_->tab_input_registers[i] = 0;
    }
}

void PLCRuntime::apply_modbus_writes() {
    if (!modbus_mapping_ || !shm_.is_valid()) {
        return;
    }

    PLCSharedMemory* shm_ptr = shm_.data();
    if (!shm_ptr) {
        return;
    }

    if (static_cast<int>(modbus_coil_shadow_.size()) != MODBUS_COIL_COUNT) {
        modbus_coil_shadow_.assign(MODBUS_COIL_COUNT, 0);
    }

    bool updated = false;
    const int coil_outputs = PLCConstants::MAX_OUTPUTS;
    const int coil_memory = PLCConstants::MAX_MEMORY;

    for (int i = 0; i < coil_outputs; ++i) {
        uint8_t new_value = modbus_mapping_->tab_bits[i] ? 1 : 0;
        if (modbus_coil_shadow_[i] != new_value) {
            bool state = new_value != 0;
            shm_ptr->outputs[i].store(state);
            int status_index = PLCConstants::M_STATUS_START + i;
            if (status_index < PLCConstants::MAX_MEMORY) {
                shm_ptr->memory[status_index].store(state);
            }
            if (gpio_) {
                gpio_->write_output(i, state);
            }
            modbus_coil_shadow_[i] = new_value;
            updated = true;
        }
    }

    for (int i = 0; i < coil_memory; ++i) {
        int idx = coil_outputs + i;
        uint8_t new_value = modbus_mapping_->tab_bits[idx] ? 1 : 0;
        if (modbus_coil_shadow_[idx] != new_value) {
            // M46-M51: 输出状态镜像（只读），任何写入都会被立即回写成真实输出状态
            bool is_status = (i >= PLCConstants::M_STATUS_START && i <= PLCConstants::M_STATUS_END);
            if (is_status) {
                bool actual = shm_ptr->memory[i].load();
                modbus_mapping_->tab_bits[idx] = actual ? 1 : 0;
                modbus_coil_shadow_[idx] = modbus_mapping_->tab_bits[idx];
            } else {
                // M0-M45: 可写区域（包括通用继电器、YOLO就绪标志、YOLO状态位）
                bool state = new_value != 0;
                bool old_state = shm_ptr->memory[i].load();
                shm_ptr->memory[i].store(state);
                
                // 记录写入日志（特别是YOLO相关位）
                if (i == PLCConstants::M_YOLO_READY) {
                    std::cout << "[MODBUS] M39 (YOLO就绪) 写入: " << (state ? "ON" : "OFF") << std::endl;
                } else if (i >= PLCConstants::M_YOLO_START && i <= PLCConstants::M_YOLO_END) {
                    std::cout << "[MODBUS] M" << i << " (YOLO状态位) 写入: " << (state ? "ON" : "OFF") 
                              << " (旧值: " << (old_state ? "ON" : "OFF") << ")" << std::endl;
                }
                
                // 更新 YOLO 标志位（M40-M45）
                if (i >= PLCConstants::M_YOLO_START && i <= PLCConstants::M_YOLO_END) {
                    int yolo_index = i - PLCConstants::M_YOLO_START;
                    if (yolo_index >= 0 && yolo_index < PLCConstants::MAX_YOLO_FLAGS) {
                        shm_ptr->yolo_flags[yolo_index].store(state);
                        std::cout << "[MODBUS] YOLO标志位[" << yolo_index << "] 更新为: " 
                                  << (state ? "ON" : "OFF") << std::endl;
                    }
                }
                
                modbus_coil_shadow_[idx] = new_value;
                updated = true;
            }
        }
    }

    if (updated) {
        shm_.sync();
    }
}

void PLCRuntime::reset_shared_memory_state() {
    if (!shm_.is_valid()) {
        return;
    }

    PLCSharedMemory* shm_ptr = shm_.data();
    if (!shm_ptr) {
        return;
    }

    for (int i = 0; i < PLCConstants::MAX_OUTPUTS; ++i) {
        shm_ptr->outputs[i].store(false);
    }

    for (int i = 0; i < PLCConstants::MAX_MEMORY; ++i) {
        shm_ptr->memory[i].store(false);
    }

    for (int i = 0; i < PLCConstants::MAX_YOLO_FLAGS; ++i) {
        shm_ptr->yolo_flags[i].store(false);
    }

    for (int i = 0; i < PLCConstants::MAX_INPUTS; ++i) {
        shm_ptr->inputs[i].store(false);
    }

    for (int i = 0; i < PLCConstants::MAX_TIMERS; ++i) {
        shm_ptr->timers[i].running.store(false);
        shm_ptr->timers[i].done.store(false);
        shm_ptr->timers[i].elapsed.store(0.0);
        shm_ptr->timers[i].preset.store(0.0);
    }

    for (int i = 0; i < PLCConstants::MAX_COUNTERS; ++i) {
        shm_ptr->counters[i].done.store(false);
        shm_ptr->counters[i].count.store(0);
        shm_ptr->counters[i].preset.store(0);
    }

    shm_ptr->scan_counter.store(0);
    shm_ptr->scan_time_us.store(0.0);
    shm_ptr->error_code.store(ERR_NONE);
    shm_ptr->heartbeat.store(0);
    shm_ptr->emergency_stop.store(false);

    shm_.sync();
}

bool PLCRuntime::reload_user_config() {
    std::lock_guard<std::mutex> lock(runtime_mutex_);

    // 优先重载单一配置
    if (ladder_->reload_unified_config(unified_config_file_)) {
        std::cout << "统一配置文件重载成功。" << std::endl;
        return true;
    }
    
    // 否则，尝试重载用户配置
    if (ladder_->reload_user_config(user_config_file_)) {
        std::cout << "用户配置文件重载成功。" << std::endl;
        return true;
    }

    return false;
}

void PLCRuntime::set_config_files(const std::string& system_config, const std::string& user_config) {
    system_config_file_ = system_config;
    user_config_file_ = user_config;
}


uint64_t PLCRuntime::get_scan_count() const {
    if (shm_.is_valid()) {
        return shm_.data()->scan_counter.load();
    }
    return 0;
}

uint32_t PLCRuntime::get_error_code() const {
    if (shm_.is_valid()) {
        return shm_.data()->error_code.load();
    }
    return ERR_NONE;
}

bool PLCRuntime::api_set_yolo_flag(int level, bool value) {
    if (level < 1 || level > PLCConstants::MAX_YOLO_FLAGS) {
        std::cerr << "YOLO level超出范围: " << level << std::endl;
        return false;
    }
    
    if (shm_.is_valid()) {
        shm_.data()->yolo_flags[level - 1].store(value);
        shm_.data()->memory[PLCConstants::M_YOLO_START + (level - 1)].store(value);
        return true;
    }
    
    return false;
}

bool PLCRuntime::api_get_output_status(const std::string& output_name) {
    if (output_name.length() > 1 && output_name[0] == 'Q') {
        int index = std::stoi(output_name.substr(1));
        if (index >= 0 && index < PLCConstants::MAX_OUTPUTS && shm_.is_valid()) {
            return shm_.data()->outputs[index].load();
        }
    }
    return false;
}

std::vector<bool> PLCRuntime::api_get_all_outputs() {
    std::vector<bool> outputs(PLCConstants::MAX_OUTPUTS, false);
    if (shm_.is_valid()) {
        for (int i = 0; i < PLCConstants::MAX_OUTPUTS; ++i) {
            outputs[i] = shm_.data()->outputs[i].load();
        }
    }
    return outputs;
}

std::vector<bool> PLCRuntime::api_get_memory_range(int start, int end) {
    std::vector<bool> memory;
    if (shm_.is_valid() && start >= 0 && end < PLCConstants::MAX_MEMORY && start <= end) {
        for (int i = start; i <= end; ++i) {
            memory.push_back(shm_.data()->memory[i].load());
        }
    }
    return memory;
}

void PLCRuntime::emergency_stop() {
    if (shm_.is_valid()) {
        shm_.data()->emergency_stop.store(true);
    }
 
    stop_indicator(true);

    gpio_->emergency_shutdown();
    std::cout << "紧急停止已激活" << std::endl;
}

void PLCRuntime::clear_emergency_stop() {
    if (shm_.is_valid()) {
        shm_.data()->emergency_stop.store(false);
    }
    std::cout << "紧急停止已清除" << std::endl;
}

bool PLCRuntime::is_emergency_stopped() const {
    if (shm_.is_valid()) {
        return shm_.data()->emergency_stop.load();
    }
    return false;
}

void PLCRuntime::set_enable_pin(bool enabled) {
    gpio_->set_enable(enabled);
}

void PLCRuntime::handle_error(uint32_t error_code) {
    if (shm_.is_valid()) {
        shm_.data()->error_code.store(error_code);
    }
    
    switch (error_code) {
        case ERR_GPIO_INIT_FAILED:
            std::cerr << "GPIO初始化失败" << std::endl;
            break;
            
        case ERR_GPIO_READ_TIMEOUT:
            std::cerr << "GPIO读取超时" << std::endl;
            break;
            
        case ERR_SHM_ACCESS_FAILED:
            std::cerr << "共享内存访问失败" << std::endl;
            break;
            
        case ERR_CONFIG_PARSE_ERROR:
            std::cerr << "配置解析错误" << std::endl;
            break;
        case ERR_MODBUS_INIT_FAILED:
            std::cerr << "Modbus 初始化失败" << std::endl;
            break;
        case ERR_SCAN_TIMEOUT:
            std::cerr << "扫描周期超时" << std::endl;
            break;
            
        case ERR_WATCHDOG_TIMEOUT:
            std::cerr << "看门狗超时" << std::endl;
            emergency_stop();
            break;
            
        case ERR_EMERGENCY_STOP:
            std::cerr << "紧急停止" << std::endl;
            break;
    }
}

void PLCRuntime::request_stop() {
    running_.store(false);
}
