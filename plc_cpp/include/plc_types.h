#pragma once
#include <vector>
#include <string>
#include <atomic>
#include <cstdint>

// === PLC Resource Limits and Constants ===
namespace PLCConstants {
    constexpr int MAX_INPUTS = 3;
    constexpr int MAX_OUTPUTS = 6;
    constexpr int MAX_MEMORY = 52; // M0-M51
    constexpr int MAX_TIMERS = 10;
    constexpr int MAX_COUNTERS = 10;
    constexpr int MAX_YOLO_FLAGS = 10;

    // Memory Mapping
    constexpr int M_GENERAL_START = 0;
    constexpr int M_GENERAL_END = 38;      // M0-M38: 通用中间继电器
    constexpr int M_YOLO_READY = 39;       // M39: YOLO 就绪标志位（心跳）
    constexpr int M_YOLO_START = 40;       // M40-M45: YOLO 状态位
    constexpr int M_YOLO_END = 45;
    constexpr int M_STATUS_START = 46;     // M46-M51: 输出状态镜像（只读）
    constexpr int M_STATUS_END = 51;
}

// === 错误码定义 ===
enum PLCErrors : uint32_t {
    ERR_NONE = 0,
    ERR_GPIO_INIT_FAILED,
    ERR_GPIO_READ_TIMEOUT,
    ERR_SHM_ACCESS_FAILED,
    ERR_CONFIG_PARSE_ERROR,
    ERR_MODBUS_INIT_FAILED,
    ERR_SCAN_TIMEOUT,
    ERR_WATCHDOG_TIMEOUT,
    ERR_EMERGENCY_STOP
};

// === 梯形图逻辑数据结构 ===

// 条件类型
enum class ConditionType { INPUT, OUTPUT, MEMORY, TIMER, COUNTER, YOLO_FLAG, INVALID };

// 动作类型
enum class ActionType { OUTPUT, SET, RESET, MEMORY_SET, MEMORY_RESET, TIMER, COUNTER, RESET_TIMER, RESET_COUNTER, INVALID };

// 条件结构体
struct Condition {
    ConditionType type;
    std::string name;
    bool normally_open;

    Condition(ConditionType t = ConditionType::INVALID, const std::string& n = "", bool no = true)
        : type(t), name(n), normally_open(no) {}
};

// 动作结构体
struct Action {
    ActionType type;
    std::string name;

    Action(ActionType t = ActionType::INVALID, const std::string& n = "")
        : type(t), name(n) {}
};

// 梯级配置
struct RungConfig {
    std::string id;
    bool enabled;
    std::vector<Condition> conditions;
    Action action;
    std::string source; // "system" or "user"

    RungConfig(const std::string& i, bool en, const std::string& src)
        : id(i), enabled(en), source(src) {}
};

// === 组件配置数据结构 ===

// 定时器配置
struct TimerConfig {
    std::string name;
    double preset;
    std::string alias;

    TimerConfig(const std::string& n, double p, const std::string& a)
        : name(n), preset(p), alias(a) {}
};

// 计数器配置
struct CounterConfig {
    std::string name;
    int32_t preset;
    std::string alias;

    CounterConfig(const std::string& n, int32_t p, const std::string& a)
        : name(n), preset(p), alias(a) {}
};

// === 共享内存中的状态结构 ===

// 共享内存中的定时器状态
struct TimerState {
    std::atomic<bool> running;
    std::atomic<bool> done;
    std::atomic<double> elapsed;
    std::atomic<double> preset;
};

// 共享内存中的计数器状态
struct CounterState {
    std::atomic<bool> done;
    std::atomic<int32_t> count;
    std::atomic<int32_t> preset;
};
