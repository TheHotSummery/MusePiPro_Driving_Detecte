#pragma once
#include "plc_types.h"
#include <string>
#include <memory>
#include <mutex>

struct PLCSharedMemory {
    // I/O 状态
    std::atomic<bool> inputs[PLCConstants::MAX_INPUTS];
    std::atomic<bool> outputs[PLCConstants::MAX_OUTPUTS];
    std::atomic<bool> memory[PLCConstants::MAX_MEMORY];
    std::atomic<bool> yolo_flags[PLCConstants::MAX_YOLO_FLAGS];
    
    // 系统状态
    std::atomic<uint64_t> scan_counter;
    std::atomic<double> scan_time_us;
    std::atomic<uint32_t> error_code;
    std::atomic<uint32_t> heartbeat;
    std::atomic<bool> emergency_stop;
    
    // 定时器/计数器状态
    TimerState timers[PLCConstants::MAX_TIMERS];
    CounterState counters[PLCConstants::MAX_COUNTERS];
};

class SharedMemoryManager {
public:
    SharedMemoryManager(const std::string& name = "plc_shared_memory");
    ~SharedMemoryManager();
    
    // 获取共享内存指针
    PLCSharedMemory* data();
    const PLCSharedMemory* data() const;
    
    // 同步操作
    void sync();
    
    // 状态查询
    bool is_valid() const;
    std::string get_name() const { return name_; }
    
    // 错误处理
    std::string get_last_error() const;
    
private:
    std::string name_;
    int fd_;
    PLCSharedMemory* ptr_;
    mutable std::mutex shm_mutex_;
    std::string last_error_;
    
    void set_error(const std::string& error);
    bool create_shared_memory();
    bool open_shared_memory();
    void initialize_shared_memory();
};
