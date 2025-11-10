#include "ladder_engine.h"
#include "timer_counter.h"
#include "json_parser.h"
#include "shared_memory.h"
#include <iostream>
#include <algorithm>
#include <unordered_set>

LadderEngine::LadderEngine(TimerManager& tm, CounterManager& cm) 
    : timer_manager_(tm), counter_manager_(cm), config_loaded_(false) {
}

LadderEngine::~LadderEngine() {
}

bool LadderEngine::load_system_config(const std::string& file) {
    std::lock_guard<std::recursive_mutex> lock(config_mutex_);
    
    JSONParser parser;
    if (!parser.parse_system_config(file, system_rungs_)) {
        set_error("加载系统配置失败: " + parser.get_last_error());
        return false;
    }
    
    // 标记来源
    for (auto& rung : system_rungs_) {
        rung.source = "system";
    }
    
    std::cout << "系统配置已加载: " << system_rungs_.size() << "个梯级" << std::endl;
    return true;
}

bool LadderEngine::load_user_config(const std::string& file) {
    std::lock_guard<std::recursive_mutex> lock(config_mutex_);
    
    JSONParser parser;
    if (!parser.parse_user_config(file, user_rungs_, timer_configs_, counter_configs_)) {
        set_error("加载用户配置失败: " + parser.get_last_error());
        return false;
    }
    
    // 标记来源
    for (auto& rung : user_rungs_) {
        rung.source = "user";
    }
    
    std::cout << "用户配置已加载: " << user_rungs_.size() << "个梯级, " 
              << timer_configs_.size() << "个定时器, " 
              << counter_configs_.size() << "个计数器" << std::endl;
    return true;
}

bool LadderEngine::load_unified_config(const std::string& file) {
    std::cout << "[LADDER_DEBUG] load_unified_config 函数开始执行" << std::endl;
    
    std::lock_guard<std::recursive_mutex> lock(config_mutex_);
    std::cout << "[LADDER_DEBUG] 获取配置锁成功" << std::endl;
    
    // 清空旧的用户区和组件配置
    user_rungs_.clear();
    timer_configs_.clear();
    counter_configs_.clear();
    std::cout << "[LADDER_DEBUG] 清空现有配置完成" << std::endl;

    // 使用 parse_user_config，因为它能解析所有部分
    JSONParser parser;
    std::cout << "[LADDER_DEBUG] 创建 JSONParser 成功" << std::endl;
    if (!parser.parse_user_config(file, user_rungs_, timer_configs_, counter_configs_)) {
        set_error("加载统一配置失败: " + parser.get_last_error());
        std::cout << "[LADDER_DEBUG] parse_user_config 失败" << std::endl;
        return false;
    }
    std::cout << "[LADDER_DEBUG] parse_user_config 成功完成" << std::endl;

    if (!validate_config(user_rungs_, timer_configs_, counter_configs_)) {
        // Validation failed, error is already set. Clear the invalid config.
        user_rungs_.clear();
        timer_configs_.clear();
        counter_configs_.clear();
        return false;
    }
    
    // 标记来源为user，因为统一配置被视为用户提供的最终配置
    for (auto& rung : user_rungs_) {
        rung.source = "user";
    }

    // 清空系统区梯级，因为统一配置已包含所有逻辑
    system_rungs_.clear();
    
    // 直接合并
    merge_configs();

    std::cout << "统一配置已加载: " << user_rungs_.size() << "个梯级, " 
              << timer_configs_.size() << "个定时器, " 
              << counter_configs_.size() << "个计数器" << std::endl;
    return true;
}

void LadderEngine::clear_configs() {
    std::lock_guard<std::recursive_mutex> lock(config_mutex_);
    system_rungs_.clear();
    user_rungs_.clear();
    merged_rungs_.clear();
    timer_configs_.clear();
    counter_configs_.clear();
    config_loaded_ = false;
}

void LadderEngine::merge_configs() {
    std::lock_guard<std::recursive_mutex> lock(config_mutex_);
    
    merged_rungs_.clear();
    
    // 先添加系统梯级
    for (const auto& rung : system_rungs_) {
        merged_rungs_.push_back(rung);
    }
    
    // 再添加用户梯级
    for (const auto& rung : user_rungs_) {
        merged_rungs_.push_back(rung);
    }
    
    config_loaded_ = true;
    std::cout << "配置已合并: 系统" << system_rungs_.size() 
              << "个 + 用户" << user_rungs_.size() 
              << "个 = 总计" << merged_rungs_.size() << "个梯级" << std::endl;
}

void LadderEngine::execute_cycle(PLCSharedMemory* shm, 
                                const std::vector<bool>& inputs,
                                double delta_time) {
    if (!shm || !config_loaded_) {
        return;
    }
    
    std::lock_guard<std::recursive_mutex> lock(config_mutex_);

    clear_cycle_state();
    
    // 执行所有梯级
    for (const auto& rung : merged_rungs_) {
        if (!rung.enabled) {
            continue;
        }
        
        // 评估条件
        bool result = true;
        if (!rung.conditions.empty()) {
            result = true;
            for (const auto& condition : rung.conditions) {
                if (!evaluate_condition(condition, inputs, shm)) {
                    result = false;
                    break;
                }
            }
        }
        
        // 执行动作
        execute_action(rung.action, shm, result, delta_time);
    }
}

void LadderEngine::finalize_cycle() {
    std::lock_guard<std::recursive_mutex> lock(config_mutex_);
    enabled_timers_prev_ = enabled_timers_;
    triggered_counters_prev_ = triggered_counters_;
}

void LadderEngine::clear_cycle_state() {
    enabled_timers_.clear();
    triggered_counters_.clear();
}

bool LadderEngine::reload_user_config(const std::string& file) {
    std::cout << "重新加载用户配置..." << std::endl;
    
    // 清空现有用户配置
    {
        std::lock_guard<std::recursive_mutex> lock(config_mutex_);
        user_rungs_.clear();
        timer_configs_.clear();
        counter_configs_.clear();
    }
    
    // 重新加载
    if (!load_user_config(file)) {
        return false;
    }
    
    // 重新合并配置
    merge_configs();
    
    std::cout << "用户配置重载成功" << std::endl;
    return true;
}

bool LadderEngine::reload_unified_config(const std::string& file) {
    std::cout << "重新加载统一配置..." << std::endl;
    
    // 直接调用新的加载函数
    if (!load_unified_config(file)) {
        return false;
    }
    
    std::cout << "统一配置重载成功" << std::endl;
    return true;
}

bool LadderEngine::evaluate_condition(const Condition& cond, 
                                     const std::vector<bool>& inputs,
                                     PLCSharedMemory* shm) {
    bool state = false;
    
    switch (cond.type) {
        case ConditionType::INPUT: {
            // 解析输入名称（如"I0" -> 索引0）
            if (cond.name.length() > 1 && cond.name[0] == 'I') {
                int index = std::stoi(cond.name.substr(1));
                if (index >= 0 && index < PLCConstants::MAX_INPUTS) {
                    state = inputs[index];
                }
            }
            break;
        }
        
        case ConditionType::OUTPUT: {
            // 解析输出名称（如"Q0" -> 索引0）
            if (cond.name.length() > 1 && cond.name[0] == 'Q') {
                int index = std::stoi(cond.name.substr(1));
                if (index >= 0 && index < PLCConstants::MAX_OUTPUTS) {
                    state = shm->outputs[index].load();
                }
            }
            break;
        }
        
        case ConditionType::MEMORY: {
            // 解析中间继电器名称（如"M5" -> 索引5）
            if (cond.name.length() > 1 && cond.name[0] == 'M') {
                int index = std::stoi(cond.name.substr(1));
                if (index >= 0 && index < PLCConstants::MAX_MEMORY) {
                    state = shm->memory[index].load();
                }
            }
            break;
        }
        
        case ConditionType::TIMER: {
            Timer* timer = timer_manager_.get_timer(cond.name);
            if (timer) {
                state = timer->is_done();
            }
            break;
        }
        
        case ConditionType::COUNTER: {
            Counter* counter = counter_manager_.get_counter(cond.name);
            if (counter) {
                state = counter->is_done();
            }
            break;
        }
        
        case ConditionType::YOLO_FLAG: {
            // 解析YOLO标志（如"Y1" -> 索引0）
            if (cond.name.length() > 1 && cond.name[0] == 'Y') {
                int index = std::stoi(cond.name.substr(1)) - 1;
                if (index >= 0 && index < PLCConstants::MAX_YOLO_FLAGS) {
                    state = shm->yolo_flags[index].load();
                }
            }
            break;
        }
    }
    
    return cond.normally_open ? state : !state;
}

void LadderEngine::execute_action(const Action& act, 
                                 PLCSharedMemory* shm, 
                                 bool result,
                                 double delta_time) {
    (void)delta_time;  // 避免未使用参数警告
    
    // 如果梯级条件为假，标准线圈应该断电
    if (!result) {
        if (act.type == ActionType::OUTPUT) {
            if (act.name.length() > 1 && act.name[0] == 'Q') {
                int index = std::stoi(act.name.substr(1));
                if (index >= 0 && index < PLCConstants::MAX_OUTPUTS) {
                    shm->outputs[index].store(false);
                }
            }
        }
        // 对于 SET/RESET 等锁存指令，条件为假时不执行任何操作，所以直接返回
        return;
    }
    
    // 下面是 result 为 true 时的逻辑
    switch (act.type) {
        case ActionType::OUTPUT: {
            // 解析输出名称并设置
            if (act.name.length() > 1 && act.name[0] == 'Q') {
                int index = std::stoi(act.name.substr(1));
                if (index >= 0 && index < PLCConstants::MAX_OUTPUTS) {
                    shm->outputs[index].store(true);
                }
            }
            break;
        }
        
        case ActionType::SET: {
            // 置位输出
            if (act.name.length() > 1 && act.name[0] == 'Q') {
                int index = std::stoi(act.name.substr(1));
                if (index >= 0 && index < PLCConstants::MAX_OUTPUTS) {
                    shm->outputs[index].store(true);
                }
            }
            break;
        }
        
        case ActionType::RESET: {
            // 复位输出
            if (act.name.length() > 1 && act.name[0] == 'Q') {
                int index = std::stoi(act.name.substr(1));
                if (index >= 0 && index < PLCConstants::MAX_OUTPUTS) {
                    shm->outputs[index].store(false);
                }
            }
            break;
        }
        
        case ActionType::MEMORY_SET: {
            // 置位中间继电器
            if (act.name.length() > 1 && act.name[0] == 'M') {
                int index = std::stoi(act.name.substr(1));
                if (index >= 0 && index < PLCConstants::MAX_MEMORY) {
                    shm->memory[index].store(true);
                }
            }
            break;
        }
        
        case ActionType::MEMORY_RESET: {
            // 复位中间继电器
            if (act.name.length() > 1 && act.name[0] == 'M') {
                int index = std::stoi(act.name.substr(1));
                if (index >= 0 && index < PLCConstants::MAX_MEMORY) {
                    shm->memory[index].store(false);
                }
            }
            break;
        }
        
        case ActionType::TIMER: {
            if (result) {
                enabled_timers_.insert(act.name);
            }
            break;
        }
        
        case ActionType::COUNTER: {
            if (result) {
                triggered_counters_.insert(act.name);
            }
            break;
        }
        
        case ActionType::RESET_TIMER: {
            timer_manager_.reset_timer(act.name);
            break;
        }
        
        case ActionType::RESET_COUNTER: {
            counter_manager_.reset_counter(act.name);
            break;
        }
    }
}

bool LadderEngine::check_output_permission(const std::string& output_name, 
                                          const std::string& rung_source) const {
    (void)output_name;
    (void)rung_source;
    // 新设计：取消系统/用户输出权限划分，允许任意梯级控制任意输出
    return true;
}

bool LadderEngine::check_memory_permission(const std::string& memory_name, 
                                          const std::string& operation) const {
    // 解析中间继电器编号
    if (memory_name.length() > 1 && memory_name[0] == 'M') {
        int index = std::stoi(memory_name.substr(1));
        
        // 检查范围
        if (index < 0 || index > PLCConstants::M_STATUS_END) {
            return false;
        }
        
        // 写操作禁止输出状态映射区
        if (operation == "write" && index >= PLCConstants::M_STATUS_START && index <= PLCConstants::M_STATUS_END) {
            return false;
        }
    }
    
    return true;
}

std::string LadderEngine::get_last_error() const {
    std::lock_guard<std::recursive_mutex> lock(config_mutex_);
    return last_error_;
}

void LadderEngine::set_error(const std::string& error) {
    std::lock_guard<std::recursive_mutex> lock(config_mutex_);
    last_error_ = error;
    std::cerr << "梯级引擎错误: " << error << std::endl;
}

bool LadderEngine::validate_config(const std::vector<RungConfig>& rungs,
                                   const std::vector<TimerConfig>& timers,
                                   const std::vector<CounterConfig>& counters) {
    std::unordered_set<std::string> timer_names;
    for(const auto& t : timers) {
        timer_names.insert(t.name);
    }

    std::unordered_set<std::string> counter_names;
    for(const auto& c : counters) {
        counter_names.insert(c.name);
    }

    for (const auto& rung : rungs) {
        for (const auto& cond : rung.conditions) {
            if (cond.type == ConditionType::TIMER) {
                if (timer_names.find(cond.name) == timer_names.end()) {
                    set_error("配置校验失败: 梯级 '" + rung.id + "' 引用了未定义的定时器 '" + cond.name + "'");
                    return false;
                }
            } else if (cond.type == ConditionType::COUNTER) {
                if (counter_names.find(cond.name) == counter_names.end()) {
                    set_error("配置校验失败: 梯级 '" + rung.id + "' 引用了未定义的计数器 '" + cond.name + "'");
                    return false;
                }
            }
        }

        if (rung.action.type == ActionType::TIMER || rung.action.type == ActionType::RESET_TIMER) {
            if (timer_names.find(rung.action.name) == timer_names.end()) {
                set_error("配置校验失败: 梯级 '" + rung.id + "' 的动作引用了未定义的定时器 '" + rung.action.name + "'");
                return false;
            }
        } else if (rung.action.type == ActionType::COUNTER || rung.action.type == ActionType::RESET_COUNTER) {
            if (counter_names.find(rung.action.name) == counter_names.end()) {
                set_error("配置校验失败: 梯级 '" + rung.id + "' 的动作引用了未定义的计数器 '" + rung.action.name + "'");
                return false;
            }
        }
    }

    return true;
}
