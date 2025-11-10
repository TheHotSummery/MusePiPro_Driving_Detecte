#pragma once
#include "plc_types.h"
#include "shared_memory.h"
#include <vector>
#include <string>
#include <memory>
#include <mutex>
#include <unordered_set> // Added for unordered_set

// Forward declarations to reduce header dependencies
class TimerManager;
class CounterManager;

class LadderEngine {
public:
    LadderEngine(TimerManager& tm, CounterManager& cm);
    ~LadderEngine();
    
    // 配置加载（对应Python的load_configs方法）
    bool load_system_config(const std::string& file);
    bool load_user_config(const std::string& file);
    bool load_unified_config(const std::string& file); // 新增
    void merge_configs();  // 对应Python的_merge_configs方法
    void clear_configs();
    
    // 梯级执行（对应Python的execute_rung方法）
    void execute_cycle(PLCSharedMemory* shm, 
                      const std::vector<bool>& inputs,
                      double delta_time);
    void finalize_cycle();
    
    // 热重载（对应Python的reload_user_config方法）
    bool reload_user_config(const std::string& file);
    bool reload_unified_config(const std::string& file); // 新增
    
    // 状态查询
    size_t get_rung_count() const { return merged_rungs_.size(); }
    bool is_config_loaded() const { return config_loaded_; }
    
    // 组件配置访问
    const std::vector<TimerConfig>& get_timer_configs() const { return timer_configs_; }
    const std::vector<CounterConfig>& get_counter_configs() const { return counter_configs_; }
    
    // Cycle execution results
    const std::unordered_set<std::string>& get_enabled_timers() const { return enabled_timers_prev_; }
    const std::unordered_set<std::string>& get_triggered_counters() const { return triggered_counters_prev_; }
    
    // 错误处理
    std::string get_last_error() const;
    
private:
    TimerManager& timer_manager_;
    CounterManager& counter_manager_;

    // Cycle state
    std::unordered_set<std::string> enabled_timers_;
    std::unordered_set<std::string> triggered_counters_;
    std::unordered_set<std::string> enabled_timers_prev_;
    std::unordered_set<std::string> triggered_counters_prev_;

    // 配置数据
    std::vector<RungConfig> system_rungs_;
    std::vector<RungConfig> user_rungs_;
    std::vector<RungConfig> merged_rungs_;
    
    // 组件配置
    std::vector<TimerConfig> timer_configs_;
    std::vector<CounterConfig> counter_configs_;
    
    // 状态管理
    std::atomic<bool> config_loaded_;
    mutable std::recursive_mutex config_mutex_;
    std::string last_error_;
    
    // 内部方法
    bool evaluate_condition(const Condition& cond, 
                           const std::vector<bool>& inputs,
                           PLCSharedMemory* shm);
    void execute_action(const Action& act, 
                       PLCSharedMemory* shm, 
                       bool result,
                       double delta_time);
    
    void clear_cycle_state();
    
    // 权限检查（对应Python的权限验证）
    bool check_output_permission(const std::string& output_name, 
                                const std::string& rung_source) const;
    bool check_memory_permission(const std::string& memory_name, 
                                const std::string& operation) const;
    
    void set_error(const std::string& error);
    
    bool validate_config(const std::vector<RungConfig>& rungs,
                         const std::vector<TimerConfig>& timers,
                         const std::vector<CounterConfig>& counters);
    
    // 资源范围定义（对应Python的常量定义）
};
