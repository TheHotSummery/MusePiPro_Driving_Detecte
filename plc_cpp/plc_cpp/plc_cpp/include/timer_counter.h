#pragma once
#include "plc_types.h"
#include "shared_memory.h"
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <mutex>
#include <atomic>
#include <memory>

// 定时器类（对应Python的Timer类）
class Timer {
public:
    Timer(const std::string& name, double preset, const std::string& alias);
    
    // 更新定时器（对应Python的update方法）
    bool update(bool enable, double delta_time);
    
    // 重置定时器（对应Python的reset方法）
    void reset();
    
    // 状态查询
    bool is_running() const { return running_.load(); }
    bool is_done() const { return done_.load(); }
    double get_elapsed() const { return elapsed_.load(); }
    double get_preset() const { return preset_; }
    std::string get_name() const { return name_; }
    std::string get_alias() const { return alias_; }
    
private:
    std::string name_;
    std::string alias_;
    double preset_;
    
    std::atomic<bool> running_;
    std::atomic<bool> done_;
    std::atomic<double> elapsed_;
    std::atomic<double> start_time_;
    std::atomic<double> done_timestamp_;
};

// 计数器类（对应Python的Counter类）
class Counter {
public:
    Counter(const std::string& name, int32_t preset, const std::string& alias = "");
    
    // 更新计数器（对应Python的update方法）
    bool update(bool count_signal);
    
    // 重置计数器（对应Python的reset方法）
    void reset();
    
    // 状态查询
    bool is_done() const { return done_.load(); }
    int32_t get_count() const { return count_.load(); }
    int32_t get_preset() const { return preset_; }
    std::string get_name() const { return name_; }
    std::string get_alias() const { return alias_; }
    
private:
    std::string name_;
    std::string alias_;
    int32_t preset_;
    
    std::atomic<bool> done_;
    std::atomic<int32_t> count_;
    std::atomic<bool> last_state_;
};

// 定时器管理器
class TimerManager {
public:
    TimerManager();
    ~TimerManager();
    
    // 添加定时器（对应Python的_init_components方法）
    bool add_timer(const std::string& name, double preset, const std::string& alias = "");
    
    // 更新所有定时器
    void update_timers(PLCSharedMemory* shm, double delta_time, const std::unordered_set<std::string>& enabled_timers);
    
    // 重置定时器
    void reset_timer(const std::string& name);
    void reset_all_timers();
    
    // 状态查询
    Timer* get_timer(const std::string& name);
    size_t get_timer_count() const { return timers_.size(); }
    
private:
    std::unordered_map<std::string, std::unique_ptr<Timer>> timers_;
    std::unordered_map<std::string, int> name_to_shm_index_;
    mutable std::mutex timers_mutex_;
};

// 计数器管理器
class CounterManager {
public:
    CounterManager();
    ~CounterManager();
    
    // 添加计数器
    bool add_counter(const std::string& name, int32_t preset, const std::string& alias = "");
    
    // 更新所有计数器
    void update_counters(PLCSharedMemory* shm, const std::unordered_set<std::string>& triggered_counters);
    
    // 重置计数器
    void reset_counter(const std::string& name);
    void reset_all_counters();
    
    // 状态查询
    Counter* get_counter(const std::string& name);
    size_t get_counter_count() const { return counters_.size(); }
    
private:
    std::unordered_map<std::string, std::unique_ptr<Counter>> counters_;
    std::unordered_map<std::string, int> name_to_shm_index_;
    mutable std::mutex counters_mutex_;
};
