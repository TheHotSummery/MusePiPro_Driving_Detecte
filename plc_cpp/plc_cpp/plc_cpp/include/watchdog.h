#pragma once
#include <chrono>
#include <functional>
#include <atomic>
#include <thread>

class Watchdog {
public:
    Watchdog(double timeout_sec = 1.0);
    ~Watchdog();
    
    // 喂狗（对应Python的看门狗机制）
    void feed();
    
    // 检查是否超时
    bool is_timeout() const;
    
    // 设置超时回调
    void set_callback(std::function<void()> callback);
    
    // 启动/停止监控
    void start();
    void stop();
    
    // 状态查询
    bool is_running() const { return running_.load(); }
    double get_timeout() const { return timeout_sec_; }
    double get_elapsed() const;
    
private:
    double timeout_sec_;
    std::atomic<bool> running_;
    std::atomic<std::chrono::steady_clock::time_point> last_feed_;
    std::function<void()> callback_;
    std::thread monitor_thread_;
    
    void monitor_loop();
};



