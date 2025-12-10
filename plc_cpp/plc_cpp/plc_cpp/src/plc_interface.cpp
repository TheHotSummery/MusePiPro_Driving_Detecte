#include "plc_runtime.h"
#include <iostream>
#include <memory>

// 全局PLC运行时实例
static std::unique_ptr<PLCRuntime> g_plc_runtime = nullptr;

// C接口函数，供Python调用
extern "C" {

// 初始化PLC接口
bool plc_interface_init() {
    if (g_plc_runtime) {
        return true;  // 已经初始化
    }
    
    g_plc_runtime = std::make_unique<PLCRuntime>();
    if (!g_plc_runtime->init()) {
        std::cerr << "PLC接口初始化失败" << std::endl;
        g_plc_runtime.reset();
        return false;
    }
    
    std::cout << "PLC接口初始化成功" << std::endl;
    return true;
}

// 清理PLC接口
void plc_interface_cleanup() {
    if (g_plc_runtime) {
        g_plc_runtime->shutdown();
        g_plc_runtime.reset();
        std::cout << "PLC接口已清理" << std::endl;
    }
}

// 设置YOLO标志
bool plc_set_yolo_flag(int level, bool value) {
    if (!g_plc_runtime) {
        std::cerr << "PLC接口未初始化" << std::endl;
        return false;
    }
    
    return g_plc_runtime->api_set_yolo_flag(level, value);
}

// 获取输出状态
bool plc_get_output_status(const char* output_name) {
    if (!g_plc_runtime || !output_name) {
        return false;
    }
    
    return g_plc_runtime->api_get_output_status(std::string(output_name));
}

// 获取所有输出状态
void plc_get_all_outputs(bool* outputs, int size) {
    if (!g_plc_runtime || !outputs || size < 6) {
        return;
    }
    
    auto output_states = g_plc_runtime->api_get_all_outputs();
    for (int i = 0; i < std::min(6, size); ++i) {
        outputs[i] = output_states[i];
    }
}

// 获取中间继电器状态
void plc_get_memory_range(int start, int end, bool* memory, int size) {
    if (!g_plc_runtime || !memory || size < (end - start + 1)) {
        return;
    }
    
    auto memory_states = g_plc_runtime->api_get_memory_range(start, end);
    for (int i = 0; i < std::min(static_cast<int>(memory_states.size()), size); ++i) {
        memory[i] = memory_states[i];
    }
}

// 获取扫描时间
double plc_get_scan_time() {
    if (!g_plc_runtime) {
        return 0.0;
    }
    
    return g_plc_runtime->get_scan_time();
}

// 获取扫描计数
uint64_t plc_get_scan_count() {
    if (!g_plc_runtime) {
        return 0;
    }
    
    return g_plc_runtime->get_scan_count();
}

// 获取错误码
uint32_t plc_get_error_code() {
    if (!g_plc_runtime) {
        return 0;
    }
    
    return g_plc_runtime->get_error_code();
}

// 检查是否运行中
bool plc_is_running() {
    if (!g_plc_runtime) {
        return false;
    }
    
    return g_plc_runtime->is_running();
}

// 紧急停止
void plc_emergency_stop() {
    if (g_plc_runtime) {
        g_plc_runtime->emergency_stop();
    }
}

// 清除紧急停止
void plc_clear_emergency_stop() {
    if (g_plc_runtime) {
        g_plc_runtime->clear_emergency_stop();
    }
}

// 检查是否紧急停止
bool plc_is_emergency_stopped() {
    if (!g_plc_runtime) {
        return false;
    }
    
    return g_plc_runtime->is_emergency_stopped();
}

}  // extern "C"



