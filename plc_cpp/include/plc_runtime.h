#pragma once
#include "plc_types.h"
#include "gpio_driver.h"
#include "shared_memory.h"
#include "ladder_engine.h"
#include "timer_counter.h"
#include "watchdog.h"
#include <modbus/modbus.h>
#include <atomic>
#include <thread>
#include <mutex>
#include <string>
#include <memory>
#include <vector>

class PLCRuntime {
public:
    PLCRuntime(SharedMemoryManager& shm,
               const std::string& system_config_file, 
               const std::string& user_config_file,
               const std::string& unified_config_file);
    ~PLCRuntime();
    
    // 初始化和清理
    bool init();
    void shutdown();
    
    // 主运行循环（对应Python的run方法）
    void run();
    void stop();
    void request_stop();
    
    // 扫描周期（对应Python的scan_cycle方法）
    void scan_cycle();
    
    // 配置管理
    bool reload_user_config();
    void set_config_files(const std::string& system_config, const std::string& user_config);
    
    // 状态查询
    bool is_running() const { return running_.load(); }
    double get_scan_time() const { return scan_time_us_.load() / 1000.0; }  // 转换为毫秒
    uint64_t get_scan_count() const;
    uint32_t get_error_code() const;
    
    // 外部API接口（对应Python的外部API）
    bool api_set_yolo_flag(int level, bool value);
    bool api_get_output_status(const std::string& output_name);
    std::vector<bool> api_get_all_outputs();
    std::vector<bool> api_get_memory_range(int start, int end);
    
    // 紧急停止
    void emergency_stop();
    void clear_emergency_stop();
    bool is_emergency_stopped() const;
    
    // GPIO控制
    void set_enable_pin(bool enabled);
 
     // GPIO引脚定义（对应Python的引脚定义）
     static constexpr int ENABLE_PIN = 33;
     static constexpr int INDICATOR_PIN = 51;
     static const std::vector<int>& get_input_pins();
     static const std::vector<int>& get_output_pins();
    
private:
    // 核心组件
    std::unique_ptr<GPIODriver> gpio_;
    SharedMemoryManager& shm_;
    std::unique_ptr<LadderEngine> ladder_;
    std::unique_ptr<TimerManager> timer_mgr_;
    std::unique_ptr<CounterManager> counter_mgr_;
    std::unique_ptr<Watchdog> watchdog_;
    
    // 运行状态
    std::atomic<bool> running_;
    std::atomic<bool> initialized_;
    std::atomic<double> scan_time_us_;  // 扫描时间（微秒）
    
    // 线程管理
    std::thread main_thread_;
    std::thread config_watcher_thread_;
    std::thread heartbeat_thread_;
    std::thread modbus_thread_;
    std::thread indicator_thread_;
    
    // 同步
    mutable std::mutex runtime_mutex_;
    
    // 配置
    std::string system_config_file_;
    std::string user_config_file_;
    std::string unified_config_file_;

    // Modbus 支持
    modbus_t* modbus_ctx_;
    modbus_mapping_t* modbus_mapping_;
    int modbus_server_socket_;
    int modbus_client_socket_;
    std::vector<uint8_t> modbus_coil_shadow_;
    std::mutex modbus_mutex_;
    
    // 内部方法
    void main_loop();
    void config_watcher_loop();
    void heartbeat_loop();
    void modbus_loop();
    void handle_modbus_client(int client_socket); // 处理单个客户端连接
    bool init_modbus();
    void shutdown_modbus();
    void signal_modbus_shutdown();
    void update_modbus_mapping();
    void apply_modbus_writes();
    void reset_shared_memory_state();
    void handle_error(uint32_t error_code);
    void indicator_loop();
    void start_indicator();
    void stop_indicator(bool set_high = true);
    
    static const std::vector<int> INPUT_PINS;
    static const std::vector<int> OUTPUT_PINS;
    
    // 扫描周期设置
    static constexpr double DEFAULT_SCAN_INTERVAL_MS = 20.0;  // 20ms扫描周期
    static constexpr int MODBUS_COIL_COUNT = PLCConstants::MAX_OUTPUTS + PLCConstants::MAX_MEMORY;
    static constexpr int MODBUS_DISCRETE_COUNT = PLCConstants::MAX_INPUTS;
    static constexpr int MODBUS_HOLDING_REGISTER_COUNT = 32;
    static constexpr int MODBUS_INPUT_REGISTER_COUNT = 32;

    std::atomic<bool> indicator_running_{false};
};
