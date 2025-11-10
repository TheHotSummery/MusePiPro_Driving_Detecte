#include "watchdog.h"
#include <iostream>
#include <thread>

Watchdog::Watchdog(double timeout_sec) 
    : timeout_sec_(timeout_sec), running_(false) {
    last_feed_.store(std::chrono::steady_clock::now());
    // 增加超时时间，避免过于敏感
    if (timeout_sec < 5.0) {
        timeout_sec_ = 5.0;
    }
}

Watchdog::~Watchdog() {
    stop();
}

void Watchdog::feed() {
    last_feed_.store(std::chrono::steady_clock::now());
}

bool Watchdog::is_timeout() const {
    auto now = std::chrono::steady_clock::now();
    auto last_feed = last_feed_.load();
    auto elapsed = std::chrono::duration<double>(now - last_feed).count();
    return elapsed > timeout_sec_;
}

void Watchdog::set_callback(std::function<void()> callback) {
    callback_ = callback;
}

void Watchdog::start() {
    if (running_.load()) {
        return;
    }
    
    running_.store(true);
    monitor_thread_ = std::thread(&Watchdog::monitor_loop, this);
    std::cout << "看门狗已启动，超时时间: " << timeout_sec_ << "秒" << std::endl;
}

void Watchdog::stop() {
    if (!running_.load()) {
        return;
    }
    
    running_.store(false);
    if (monitor_thread_.joinable()) {
        monitor_thread_.join();
    }
    std::cout << "看门狗已停止" << std::endl;
}

double Watchdog::get_elapsed() const {
    auto now = std::chrono::steady_clock::now();
    auto last_feed = last_feed_.load();
    return std::chrono::duration<double>(now - last_feed).count();
}

void Watchdog::monitor_loop() {
    while (running_.load()) {
        std::this_thread::sleep_for(std::chrono::milliseconds(500));  // 降低检查频率
        
        if (is_timeout()) {
            std::cerr << "看门狗超时！已运行 " << get_elapsed() << " 秒" << std::endl;
            
            if (callback_) {
                callback_();
            }
            
            // 超时后继续监控，等待下次喂狗
            feed();  // 重置超时计时
        }
    }
}
