#include "timer_counter.h"
#include <chrono>
#include <iostream>

// ========== Timer 实现 ==========

Timer::Timer(const std::string& name, double preset, const std::string& alias)
    : name_(name), alias_(alias.empty() ? name : alias), preset_(preset),
      running_(false), done_(false), elapsed_(0.0), start_time_(0.0), done_timestamp_(0.0) {
}

bool Timer::update(bool enable, double delta_time) {
    (void)delta_time;
    // 【修复】添加异常处理，防止 chrono 操作抛出异常
    double now = 0.0;
    try {
        now = std::chrono::duration<double>(
            std::chrono::steady_clock::now().time_since_epoch()).count();
    } catch (const std::exception& e) {
        // 如果获取时间失败，使用 delta_time 作为备用
        std::cerr << "[TIMER] 获取时间失败: " << e.what() << std::endl;
        now = delta_time;  // 使用传入的 delta_time 作为备用值
    }
    
    if (enable) {
        if (!running_.load()) {
        running_.store(true);
            start_time_.store(now);
            done_.store(false);
            done_timestamp_.store(0.0);
        elapsed_.store(0.0);
        }

        double elapsed = now - start_time_.load();
        elapsed_.store(elapsed);
        
        if (!done_.load() && elapsed >= preset_) {
            done_.store(true);
            running_.store(false);
            done_timestamp_.store(now);
        }
    } else {
        // 进入非使能状态，若计时尚未完成则立即复位
        if (!done_.load()) {
            reset();
        } else {
            // 对于已经完成的定时器，保持 done 状态，直到持续时间达到预设值后再复位
            double hold_start = done_timestamp_.load();
            if (hold_start <= 0.0) {
                done_timestamp_.store(now);
            } else if (now - hold_start >= preset_) {
        reset();
            }
        }
    }
    
    return done_.load();
}

void Timer::reset() {
    running_.store(false);
    elapsed_.store(0.0);
    done_.store(false);
    start_time_.store(0.0);
    done_timestamp_.store(0.0);
}

// ========== Counter 实现 ==========

Counter::Counter(const std::string& name, int32_t preset, const std::string& alias)
    : name_(name), alias_(alias.empty() ? name : alias), preset_(preset),
      done_(false), count_(0), last_state_(false) {
}

bool Counter::update(bool count_signal) {
    if (count_signal && !last_state_.load()) {
        // 上升沿触发
        int32_t new_count = count_.fetch_add(1) + 1;
        if (new_count >= preset_) {
            done_.store(true);
        }
    }
    last_state_.store(count_signal);
    return done_.load();
}

void Counter::reset() {
    count_.store(0);
    done_.store(false);
    last_state_.store(false);
}

// ========== TimerManager 实现 ==========

TimerManager::TimerManager() {
}

TimerManager::~TimerManager() {
}

bool TimerManager::add_timer(const std::string& name, double preset, const std::string& alias) {
    // 初始化阶段是单线程的，不需要锁
    // std::lock_guard<std::mutex> lock(timers_mutex_);
    
    if (timers_.find(name) != timers_.end()) {
        std::cerr << "定时器已存在: " << name << std::endl;
        return false;
    }
    
    int shm_index = timers_.size();
    if (shm_index >= PLCConstants::MAX_TIMERS) {
        std::cerr << "定时器数量已达上限，无法添加: " << name << std::endl;
        return false;
    }
    
    timers_[name] = std::make_unique<Timer>(name, preset, alias);
    name_to_shm_index_[name] = shm_index;
    std::cout << "定时器已添加: " << name << " (" << alias << ") 预设: " << preset << "s" << " @ index " << shm_index << std::endl;
    return true;
}

void TimerManager::update_timers(PLCSharedMemory* shm, double delta_time, const std::unordered_set<std::string>& enabled_timers) {
    if (!shm) return;
    
    std::lock_guard<std::mutex> lock(timers_mutex_);
    
    for (auto& pair : timers_) {
        Timer* timer = pair.second.get();
        if (!timer) continue;
        
        bool enable = (enabled_timers.count(pair.first) > 0);
        
        bool done = timer->update(enable, delta_time);
        
        // 更新共享内存中的定时器状态
        auto it = name_to_shm_index_.find(pair.first);
        if (it != name_to_shm_index_.end()) {
            int index = it->second;
            shm->timers[index].running.store(timer->is_running());
            shm->timers[index].done.store(done);
            shm->timers[index].elapsed.store(timer->get_elapsed());
            shm->timers[index].preset.store(timer->get_preset());
        }
    }
}

void TimerManager::reset_timer(const std::string& name) {
    std::lock_guard<std::mutex> lock(timers_mutex_);
    
    auto it = timers_.find(name);
    if (it != timers_.end() && it->second) {
        it->second->reset();
    }
}

void TimerManager::reset_all_timers() {
    std::lock_guard<std::mutex> lock(timers_mutex_);
    
    for (auto& pair : timers_) {
        if (pair.second) {
            pair.second->reset();
        }
    }
}

Timer* TimerManager::get_timer(const std::string& name) {
    std::lock_guard<std::mutex> lock(timers_mutex_);
    
    auto it = timers_.find(name);
    if (it != timers_.end()) {
        return it->second.get();
    }
    return nullptr;
}

// ========== CounterManager 实现 ==========

CounterManager::CounterManager() {
}

CounterManager::~CounterManager() {
}

bool CounterManager::add_counter(const std::string& name, int32_t preset, const std::string& alias) {
    // 初始化阶段是单线程的，不需要锁
    // std::lock_guard<std::mutex> lock(counters_mutex_);
    
    if (counters_.find(name) != counters_.end()) {
        std::cerr << "计数器已存在: " << name << std::endl;
        return false;
    }
    
    int shm_index = counters_.size();
    if (shm_index >= PLCConstants::MAX_COUNTERS) {
        std::cerr << "计数器数量已达上限，无法添加: " << name << std::endl;
        return false;
    }
    
    counters_[name] = std::make_unique<Counter>(name, preset, alias);
    name_to_shm_index_[name] = shm_index;
    std::cout << "计数器已添加: " << name << " (" << alias << ") 预设: " << preset << " @ index " << shm_index << std::endl;
    return true;
}

void CounterManager::update_counters(PLCSharedMemory* shm, const std::unordered_set<std::string>& triggered_counters) {
    if (!shm) return;
    
    std::lock_guard<std::mutex> lock(counters_mutex_);
    
    for (auto& pair : counters_) {
        Counter* counter = pair.second.get();
        if (!counter) continue;
        
        bool count_signal = (triggered_counters.count(pair.first) > 0);
        
        bool done = counter->update(count_signal);
        
        // 更新共享内存中的计数器状态
        auto it = name_to_shm_index_.find(pair.first);
        if (it != name_to_shm_index_.end()) {
            int index = it->second;
            shm->counters[index].done.store(done);
            shm->counters[index].count.store(counter->get_count());
            shm->counters[index].preset.store(counter->get_preset());
        }
    }
}

void CounterManager::reset_counter(const std::string& name) {
    std::lock_guard<std::mutex> lock(counters_mutex_);
    
    auto it = counters_.find(name);
    if (it != counters_.end() && it->second) {
        it->second->reset();
    }
}

void CounterManager::reset_all_counters() {
    std::lock_guard<std::mutex> lock(counters_mutex_);
    
    for (auto& pair : counters_) {
        if (pair.second) {
            pair.second->reset();
        }
    }
}

Counter* CounterManager::get_counter(const std::string& name) {
    std::lock_guard<std::mutex> lock(counters_mutex_);
    
    auto it = counters_.find(name);
    if (it != counters_.end()) {
        return it->second.get();
    }
    return nullptr;
}
