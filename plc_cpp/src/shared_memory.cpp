#include "shared_memory.h"
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <iostream>
#include <cstring>

SharedMemoryManager::SharedMemoryManager(const std::string& name) 
    : name_(name), fd_(-1), ptr_(nullptr) {
    if (!create_shared_memory()) {
        if (!open_shared_memory()) {
            set_error("无法创建或打开共享内存: " + name_);
        }
    }
}

SharedMemoryManager::~SharedMemoryManager() {
    if (ptr_) {
        munmap(ptr_, sizeof(PLCSharedMemory));
    }
    if (fd_ >= 0) {
        close(fd_);
    }
}

PLCSharedMemory* SharedMemoryManager::data() {
    return ptr_;
}

const PLCSharedMemory* SharedMemoryManager::data() const {
    return ptr_;
}

void SharedMemoryManager::sync() {
    if (ptr_) {
        msync(ptr_, sizeof(PLCSharedMemory), MS_SYNC);
    }
}

std::string SharedMemoryManager::get_last_error() const {
    std::lock_guard<std::mutex> lock(shm_mutex_);
    return last_error_;
}

void SharedMemoryManager::set_error(const std::string& error) {
    std::lock_guard<std::mutex> lock(shm_mutex_);
    last_error_ = error;
    std::cerr << "共享内存错误: " << error << std::endl;
}

bool SharedMemoryManager::create_shared_memory() {
    std::lock_guard<std::mutex> lock(shm_mutex_);
    
    // 创建共享内存文件
    fd_ = shm_open(name_.c_str(), O_CREAT | O_RDWR | O_EXCL, 0666);
    if (fd_ < 0) {
        if (errno == EEXIST) {
            // 已存在，尝试打开
            return false;
        }
        set_error(std::string("创建共享内存失败: ") + std::strerror(errno));
        return false;
    }
    
    // 设置大小
    if (ftruncate(fd_, sizeof(PLCSharedMemory)) < 0) {
        set_error(std::string("设置共享内存大小失败: ") + std::strerror(errno));
        close(fd_);
        fd_ = -1;
        return false;
    }
    
    // 映射到内存
    ptr_ = static_cast<PLCSharedMemory*>(
        mmap(nullptr, sizeof(PLCSharedMemory), 
             PROT_READ | PROT_WRITE, MAP_SHARED, fd_, 0));
    
    if (ptr_ == MAP_FAILED) {
        set_error(std::string("映射共享内存失败: ") + std::strerror(errno));
        close(fd_);
        fd_ = -1;
        ptr_ = nullptr;
        return false;
    }
    
    // 初始化共享内存
    initialize_shared_memory();
    
    std::cout << "共享内存创建成功: " << name_ << std::endl;
    return true;
}

bool SharedMemoryManager::open_shared_memory() {
    std::lock_guard<std::mutex> lock(shm_mutex_);
    
    // 打开现有共享内存
    fd_ = shm_open(name_.c_str(), O_RDWR, 0666);
    if (fd_ < 0) {
        set_error(std::string("打开共享内存失败: ") + std::strerror(errno));
        return false;
    }
    
    // 映射到内存
    ptr_ = static_cast<PLCSharedMemory*>(
        mmap(nullptr, sizeof(PLCSharedMemory), 
             PROT_READ | PROT_WRITE, MAP_SHARED, fd_, 0));
    
    if (ptr_ == MAP_FAILED) {
        set_error(std::string("映射共享内存失败: ") + std::strerror(errno));
        close(fd_);
        fd_ = -1;
        ptr_ = nullptr;
        return false;
    }
    
    std::cout << "共享内存打开成功: " << name_ << std::endl;
    return true;
}

void SharedMemoryManager::initialize_shared_memory() {
    if (!ptr_) return;
    
    // 初始化输入状态
    for (int i = 0; i < PLCConstants::MAX_INPUTS; ++i) {
        ptr_->inputs[i].store(false);
    }
    
    // 初始化输出状态
    for (int i = 0; i < PLCConstants::MAX_OUTPUTS; ++i) {
        ptr_->outputs[i].store(false);
    }
    
    // 初始化中间继电器
    for (int i = 0; i < PLCConstants::MAX_MEMORY; ++i) {
        ptr_->memory[i].store(false);
    }
    
    // 初始化YOLO标志
    for (int i = 0; i < PLCConstants::MAX_YOLO_FLAGS; ++i) {
        ptr_->yolo_flags[i].store(false);
    }
    
    // 初始化系统状态
    ptr_->scan_counter.store(0);
    ptr_->scan_time_us.store(0.0);
    ptr_->error_code.store(ERR_NONE);
    ptr_->heartbeat.store(0);
    ptr_->emergency_stop.store(false);
    
    // 初始化定时器状态
    for (int i = 0; i < PLCConstants::MAX_TIMERS; ++i) {
        ptr_->timers[i].running.store(false);
        ptr_->timers[i].done.store(false);
        ptr_->timers[i].elapsed.store(0.0);
        ptr_->timers[i].preset.store(0.0);
    }
    
    // 初始化计数器状态
    for (int i = 0; i < PLCConstants::MAX_COUNTERS; ++i) {
        ptr_->counters[i].done.store(false);
        ptr_->counters[i].count.store(0);
        ptr_->counters[i].preset.store(0);
    }
    
    std::cout << "共享内存初始化完成" << std::endl;
}

bool SharedMemoryManager::is_valid() const {
    return ptr_ != nullptr;
}
