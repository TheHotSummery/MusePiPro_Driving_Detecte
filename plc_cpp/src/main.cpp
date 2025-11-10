#include "plc_runtime.h"
#include "gpio_driver.h" // 包含清理函数的头文件
#include <iostream>
#include <signal.h>
#include <unistd.h>
#include <sys/wait.h> // For waitpid
#include <chrono>
#include <thread>
#include <memory>
#include <cstdlib>
#include <atomic>

// 全局变量，用于信号处理
static pid_t g_worker_pid = 0;
static PLCRuntime* g_worker_runtime = nullptr;
static std::atomic<bool> g_shutdown_requested{false};

void worker_signal_handler(int) {
    if (g_worker_runtime) {
        g_worker_runtime->request_stop();
    }
}

// Supervisor的信号处理函数
void supervisor_signal_handler(int) {
    g_shutdown_requested.store(true);
}

// 显示使用帮助
void show_usage(const char* program_name) {
    std::cout << "用法: " << program_name << " [选项]\n"
              << "选项:\n"
              << "  -h, --help     显示此帮助信息\n"
              << "  -s, --system   系统配置文件路径 (默认: /home/hyit/plc_core/system_config.json)\n"
              << "  -u, --user     用户配置文件路径 (默认: /home/hyit/plc_core/user_config.json)\n"
              << "  -d, --daemon   以守护进程模式运行\n"
              << "  -v, --version  显示版本信息\n"
              << std::endl;
}

// 显示版本信息
void show_version() {
    std::cout << "PLC Core v3.0\n"
              << "适用于 Spacemit Muse Pi Pro (RISC-V)\n"
              << "支持中间继电器、系统/用户配置分离、外部API接口\n"
              << std::endl;
}

int main(int argc, char* argv[]) {
    // 默认配置文件路径
    std::string system_config = "config/system_config.json";
    std::string user_config = "config/user_config.json";
    std::string unified_config = "config/plc_config.json";

    // 简单的命令行参数解析
    if (argc > 1) {
        unified_config = argv[1];
        std::cout << "使用命令行指定的统一配置文件: " << unified_config << std::endl;
    }

    // 使用 fork() 创建子进程
    pid_t pid = fork();

    if (pid < 0) {
        // fork 失败
        std::cerr << "Fork失败!" << std::endl;
                return 1;
            }
    // ===================================================
    // 子进程 (PLC Worker) 的代码
    // ===================================================
    else if (pid == 0) { 
        // 把所有复杂对象的创建都移到子进程内部
        SharedMemoryManager shm("plc_shared_memory");
        if (!shm.is_valid()) {
            std::cerr << "[WORKER] 无法初始化共享内存" << std::endl;
                return 1;
            }

        std::cout << "[WORKER] PLC Worker 进程启动 (PID: " << getpid() << ")" << std::endl;
        
        auto plc_runtime = std::make_unique<PLCRuntime>(shm, system_config, user_config, unified_config);
        g_worker_runtime = plc_runtime.get();
        signal(SIGTERM, worker_signal_handler);
        signal(SIGINT, worker_signal_handler);
        
        if (!plc_runtime->init()) {
            std::cerr << "[WORKER] PLC初始化失败" << std::endl;
            g_worker_runtime = nullptr;
            return 1;
        }

        plc_runtime->set_enable_pin(false); // 拉低使能，开始工作
        
        try {
            plc_runtime->run(); // 阻塞运行
            plc_runtime->stop();
        } catch (const std::exception& e) {
            std::cerr << "[WORKER] PLC运行异常: " << e.what() << std::endl;
            plc_runtime->set_enable_pin(true); // 发生异常时尝试拉高使能引脚
            g_worker_runtime = nullptr;
            return 1;
        }
        
        g_worker_runtime = nullptr;
        std::cout << "[WORKER] PLC Worker 正常退出" << std::endl;
        return 0;
    } 
    // ===================================================
    // 父进程 (Supervisor) 的代码
    // ===================================================
    else { 
        // 父进程也需要自己的 Shm Manager 来访问心跳
        SharedMemoryManager shm("plc_shared_memory");
        if (!shm.is_valid()) {
            std::cerr << "[SUPERVISOR] 无法连接到共享内存" << std::endl;
            kill(pid, SIGKILL);
            return 1;
        }

        g_worker_pid = pid; // 将子进程PID存入全局变量
        std::cout << "[SUPERVISOR] Supervisor 进程启动 (PID: " << getpid() << ")" << std::endl;
        std::cout << "[SUPERVISOR] 正在监控 Worker 进程 (PID: " << g_worker_pid << ")" << std::endl;

        // 父进程捕获SIGINT/SIGTERM
        signal(SIGINT, supervisor_signal_handler);
        signal(SIGTERM, supervisor_signal_handler);
    
        // 等待子进程可能写入第一个心跳，然后读取作为初始值
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
        uint32_t last_heartbeat = shm.data()->heartbeat.load();
        
        const int timeout_seconds = 5; // 5秒没心跳就认为子进程死亡
        int timeout_counter = 0;
        bool child_is_alive = true;

        while (child_is_alive) {
            if (g_shutdown_requested.load()) {
                std::cout << "[SUPERVISOR] 收到关闭请求，正在终止 Worker 进程..." << std::endl;
                if (g_worker_pid > 0) {
                    kill(g_worker_pid, SIGTERM);

                    bool exited_gracefully = false;
                    for (int i = 0; i < 30; ++i) { // 最多等待3秒
                        int status = 0;
                        pid_t result = waitpid(g_worker_pid, &status, WNOHANG);
                        if (result == g_worker_pid) {
                            if (WIFEXITED(status)) {
                                std::cout << "[SUPERVISOR] Worker 进程已退出，状态码: " << WEXITSTATUS(status) << std::endl;
                            } else if (WIFSIGNALED(status)) {
                                std::cerr << "[SUPERVISOR] Worker 进程被信号 " << WTERMSIG(status) << " 终止" << std::endl;
                            }
                            exited_gracefully = true;
                            child_is_alive = false;
                            break;
                        }
                        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }
    
                    if (!exited_gracefully) {
                        kill(g_worker_pid, SIGKILL);
                        std::cerr << "[SUPERVISOR] Worker 未在宽限期内退出，已发送 SIGKILL。" << std::endl;
                        waitpid(g_worker_pid, nullptr, 0);
                        child_is_alive = false;
                    }
                } else {
                    child_is_alive = false;
                }
                break;
            }

    std::this_thread::sleep_for(std::chrono::seconds(1));
    
            // 检查子进程是否已经退出
            int status;
            pid_t result = waitpid(g_worker_pid, &status, WNOHANG);
            if (result == g_worker_pid) {
                if (WIFEXITED(status)) {
                    std::cout << "[SUPERVISOR] Worker 进程已正常退出，状态码: " << WEXITSTATUS(status) << std::endl;
                } else if (WIFSIGNALED(status)) {
                    std::cerr << "[SUPERVISOR] Worker 进程被信号 " << WTERMSIG(status) << " 杀死！" << std::endl;
                }
                child_is_alive = false;
                break; 
            }

            // 检查心跳
            uint32_t current_heartbeat = shm.data()->heartbeat.load();
            if (current_heartbeat == last_heartbeat) {
                timeout_counter++;
                if (timeout_counter >= timeout_seconds) {
                    std::cerr << "[SUPERVISOR] 心跳超时！Worker 进程可能已死锁或崩溃！" << std::endl;
                    child_is_alive = false;
                    
                    // 强制杀死子进程
                    kill(g_worker_pid, SIGKILL); 
                    std::cerr << "[SUPERVISOR] 已发送 SIGKILL 到 Worker 进程。" << std::endl;
                }
            } else {
                last_heartbeat = current_heartbeat;
                timeout_counter = 0; // 重置超时计数器
            }
    }
    
        // 子进程已死，执行最终的清理工作
        std::cerr << "[SUPERVISOR] 执行紧急清理程序..." << std::endl;
        // 注意：这里的引脚号需要与PLCRuntime中的定义保持一致！
        perform_emergency_gpio_shutdown(PLCRuntime::get_output_pins(), PLCRuntime::ENABLE_PIN, PLCRuntime::INDICATOR_PIN); 
        
        std::cout << "[SUPERVISOR] 清理完成，Supervisor 退出。" << std::endl;
        return 1; // 以非0状态退出，表示异常
    }
}
