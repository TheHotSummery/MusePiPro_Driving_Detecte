#pragma once
#include <vector>
#include <memory>
#include <atomic>
#include <mutex>
#include <string>

// 前向声明
struct gpiod_chip;
struct gpiod_line;

class GPIODriver {
public:
    GPIODriver();
    ~GPIODriver();
    
    // 初始化GPIO（对应Python的_init_gpio方法）
    bool init(const std::vector<int>& input_pins, 
              const std::vector<int>& output_pins,
              int enable_pin = 33,
              int indicator_pin = -1);
    
    // 读取输入状态（对应Python的read_inputs方法）
    bool read_input(int index);
    std::vector<bool> read_all_inputs();
    
    // 写入输出状态（对应Python的outputs控制）
    void write_output(int index, bool value);
    void write_all_outputs(const std::vector<bool>& values);
    
    // 使能控制（对应Python的enable引脚）
    void set_enable(bool enabled);
    bool is_enabled() const;

    // 指示灯控制
    void set_indicator(bool value);
    void toggle_indicator();
    bool has_indicator() const { return indicator_line_ != nullptr; }
    int get_indicator_pin() const { return indicator_pin_; }
    
    // 自检和错误处理
    bool self_test();  // 自检（回读验证）
    void emergency_shutdown();  // 紧急关闭所有输出
    
    // 调试辅助：强制设置某个实际GPIO引脚的输出状态
    void force_set_output_pin(int pin, bool value);
    
    // 状态查询
    bool is_fault() const { return fault_.load(); }
    std::string get_last_error() const;
    
private:
    // GPIO芯片和线路
    gpiod_chip* chip_;
    std::vector<gpiod_line*> input_lines_;
    std::vector<gpiod_line*> output_lines_;
    gpiod_line* enable_line_;
    gpiod_line* indicator_line_;
    int indicator_pin_;
    std::atomic<bool> indicator_state_;
    std::vector<int> output_pin_numbers_;
    
    // 状态管理
    std::atomic<bool> fault_;
    std::atomic<bool> enabled_;
    mutable std::mutex gpio_mutex_;
    std::string last_error_;
    
    // 内部方法
    bool request_line(gpiod_line* line, const std::string& name, bool is_output);
    void release_all_lines();
    void set_error(const std::string& error);
};

// 独立的紧急清理函数
void perform_emergency_gpio_shutdown(const std::vector<int>& output_pins, int enable_pin);
void perform_emergency_gpio_shutdown(const std::vector<int>& output_pins, int enable_pin, int indicator_pin);
