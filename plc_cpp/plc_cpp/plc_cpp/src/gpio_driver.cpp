#include "gpio_driver.h"
#include <gpiod.h>
#include <iostream>
#include <cstring>
#include <unistd.h>

GPIODriver::GPIODriver() 
    : chip_(nullptr), enable_line_(nullptr), indicator_line_(nullptr), indicator_pin_(-1),
      indicator_state_(true), fault_(false), enabled_(false) {
}

GPIODriver::~GPIODriver() {
    release_all_lines();
    if (chip_) {
        gpiod_chip_close(chip_);
    }
}

bool GPIODriver::init(const std::vector<int>& input_pins, 
                     const std::vector<int>& output_pins,
                     int enable_pin,
                     int indicator_pin) {
    std::lock_guard<std::mutex> lock(gpio_mutex_);
    
    // 打开GPIO芯片
    chip_ = gpiod_chip_open_by_name("gpiochip0");
    if (!chip_) {
        set_error("无法打开GPIO芯片");
        return false;
    }
    
    // 初始化输入引脚
    input_lines_.clear();
    for (int pin : input_pins) {
        gpiod_line* line = gpiod_chip_get_line(chip_, pin);
        if (!line) {
            set_error("无法获取输入引脚 GPIO" + std::to_string(pin));
            release_all_lines();
            return false;
        }
        
        if (!request_line(line, "I" + std::to_string(input_lines_.size()), false)) {
            release_all_lines();
            return false;
        }
        input_lines_.push_back(line);
    }
    
    // 初始化输出引脚
    output_lines_.clear();
    output_pin_numbers_.clear();
    for (int pin : output_pins) {
        gpiod_line* line = gpiod_chip_get_line(chip_, pin);
        if (!line) {
            set_error("无法获取输出引脚 GPIO" + std::to_string(pin));
            release_all_lines();
            return false;
        }
        
        if (!request_line(line, "Q" + std::to_string(output_lines_.size()), true)) {
            release_all_lines();
            return false;
        }
        output_lines_.push_back(line);
        output_pin_numbers_.push_back(pin);
    }
    
    // 初始化使能引脚
    if (enable_pin >= 0) {
        enable_line_ = gpiod_chip_get_line(chip_, enable_pin);
        if (!enable_line_) {
            set_error("无法获取使能引脚 GPIO" + std::to_string(enable_pin));
            release_all_lines();
            return false;
        }
        
        if (!request_line(enable_line_, "ENABLE", true)) {
            release_all_lines();
            return false;
        }
        
        // 初始状态：保持低电平（外设上电），运行期间出现故障会再拉高
        gpiod_line_set_value(enable_line_, 0);
        enabled_ = false;
    }

    // 初始化指示灯引脚（默认高电平，表示待命状态）
    indicator_pin_ = indicator_pin;
    if (indicator_pin_ >= 0) {
        indicator_line_ = gpiod_chip_get_line(chip_, indicator_pin_);
        if (!indicator_line_) {
            set_error("无法获取指示灯引脚 GPIO" + std::to_string(indicator_pin_));
            release_all_lines();
            return false;
        }

        if (!request_line(indicator_line_, "INDICATOR", true)) {
            release_all_lines();
            return false;
        }
        
        gpiod_line_set_value(indicator_line_, 1);
        indicator_state_.store(true);
    }
    
    fault_ = false;
    std::cout << "GPIO驱动初始化成功: " << input_pins.size() 
              << "个输入, " << output_pins.size() << "个输出" << std::endl;
    return true;
}

bool GPIODriver::read_input(int index) {
    if (index < 0 || index >= static_cast<int>(input_lines_.size())) {
        return false;
    }
    
    std::lock_guard<std::mutex> lock(gpio_mutex_);
    if (fault_.load()) {
        return false;
    }
    
    int value = gpiod_line_get_value(input_lines_[index]);
    return value > 0;
}

std::vector<bool> GPIODriver::read_all_inputs() {
    std::vector<bool> inputs;
    for (size_t i = 0; i < input_lines_.size(); ++i) {
        inputs.push_back(read_input(i));
    }
    return inputs;
}

void GPIODriver::write_output(int index, bool value) {
    if (index < 0 || index >= static_cast<int>(output_lines_.size())) {
        return;
    }
    
    std::lock_guard<std::mutex> lock(gpio_mutex_);
    if (fault_.load()) {
        return;
    }
    
    gpiod_line_set_value(output_lines_[index], value ? 1 : 0);
}

void GPIODriver::write_all_outputs(const std::vector<bool>& values) {
    for (size_t i = 0; i < output_lines_.size() && i < values.size(); ++i) {
        write_output(i, values[i]);
    }
}

void GPIODriver::set_enable(bool enabled) {
    std::lock_guard<std::mutex> lock(gpio_mutex_);
    
    if (enable_line_) {
        gpiod_line_set_value(enable_line_, enabled ? 1 : 0);
        enabled_ = enabled;
    }
}

bool GPIODriver::is_enabled() const {
    return enabled_.load();
}

void GPIODriver::set_indicator(bool value) {
    std::lock_guard<std::mutex> lock(gpio_mutex_);
    if (!indicator_line_) {
        return;
    }
    gpiod_line_set_value(indicator_line_, value ? 1 : 0);
    indicator_state_.store(value);
}

void GPIODriver::toggle_indicator() {
    std::lock_guard<std::mutex> lock(gpio_mutex_);
    if (!indicator_line_) {
        return;
    }
    bool next = !indicator_state_.load();
    gpiod_line_set_value(indicator_line_, next ? 1 : 0);
    indicator_state_.store(next);
}

bool GPIODriver::self_test() {
    std::lock_guard<std::mutex> lock(gpio_mutex_);
    
    // 测试输出引脚：写入后立即读取（需要外部回读电路）
    for (size_t i = 0; i < output_lines_.size(); ++i) {
        // 写入高电平
        gpiod_line_set_value(output_lines_[i], 1);
        usleep(1000);  // 等待1ms
        
        // 写入低电平
        gpiod_line_set_value(output_lines_[i], 0);
        usleep(1000);  // 等待1ms
    }
    
    // 测试输入引脚：读取当前状态
    for (size_t i = 0; i < input_lines_.size(); ++i) {
        int value = gpiod_line_get_value(input_lines_[i]);
        if (value < 0) {
            set_error("输入引脚I" + std::to_string(i) + "读取失败");
            return false;
        }
    }
    
    return true;
}

void GPIODriver::emergency_shutdown() {
    std::lock_guard<std::mutex> lock(gpio_mutex_);
    
    // 关闭所有输出
    for (auto line : output_lines_) {
        gpiod_line_set_value(line, 0);
    }
    
    // 使能引脚拉高（禁用输出）
    if (enable_line_) {
        gpiod_line_set_value(enable_line_, 1);
        enabled_ = false;
    }

    if (indicator_line_) {
        gpiod_line_set_value(indicator_line_, 1);
        indicator_state_.store(true);
    }
    
    fault_ = true;
    std::cout << "紧急关闭：所有输出已关闭" << std::endl;
}

bool GPIODriver::request_line(gpiod_line* line, const std::string& name, bool is_output) {
    int ret;
    if (is_output) {
        ret = gpiod_line_request_output(line, name.c_str(), 0);
    } else {
        ret = gpiod_line_request_input(line, name.c_str());
    }
    
    if (ret < 0) {
        set_error("无法请求" + name + "引脚: " + std::strerror(errno));
        return false;
    }
    
    return true;
}

void GPIODriver::release_all_lines() {
    for (auto line : input_lines_) {
        if (line) {
            gpiod_line_release(line);
        }
    }
    input_lines_.clear();
    
    for (auto line : output_lines_) {
        if (line) {
            gpiod_line_release(line);
        }
    }
    output_lines_.clear();
    output_pin_numbers_.clear();
    
    if (enable_line_) {
        gpiod_line_release(enable_line_);
        enable_line_ = nullptr;
    }
    if (indicator_line_) {
        gpiod_line_release(indicator_line_);
        indicator_line_ = nullptr;
    }
    indicator_pin_ = -1;
    indicator_state_.store(true);
}

void GPIODriver::set_error(const std::string& error) {
    last_error_ = error;
    fault_ = true;
    std::cerr << "GPIO错误: " << error << std::endl;
}

void GPIODriver::force_set_output_pin(int pin, bool value) {
    std::lock_guard<std::mutex> lock(gpio_mutex_);
    for (size_t i = 0; i < output_pin_numbers_.size(); ++i) {
        if (output_pin_numbers_[i] == pin && output_lines_[i]) {
            gpiod_line_set_value(output_lines_[i], value ? 1 : 0);
            break;
        }
    }
}

// 独立的紧急清理函数实现
void perform_emergency_gpio_shutdown(const std::vector<int>& output_pins, int enable_pin) {
    perform_emergency_gpio_shutdown(output_pins, enable_pin, -1);
}

void perform_emergency_gpio_shutdown(const std::vector<int>& output_pins, int enable_pin, int indicator_pin) {
    std::cerr << "!!! 执行独立的紧急GPIO关闭 !!!" << std::endl;
    gpiod_chip* chip = gpiod_chip_open_by_name("gpiochip0");
    if (!chip) {
        std::cerr << "紧急关闭：无法打开GPIO芯片" << std::endl;
        return;
    }

    for (int pin : output_pins) {
        gpiod_line* line = gpiod_chip_get_line(chip, pin);
        if (!line) {
            std::cerr << "紧急关闭：无法获取输出引脚 GPIO" << pin << std::endl;
            continue;
        }
        if (gpiod_line_request_output(line, "EMERGENCY_OUTPUT", 0) < 0) {
            std::cerr << "紧急关闭：无法请求输出引脚 GPIO" << pin << std::endl;
            gpiod_line_release(line);
            continue;
        }
        gpiod_line_set_value(line, 0);
        gpiod_line_release(line);
    }

    if (enable_pin >= 0) {
        gpiod_line* enable_line = gpiod_chip_get_line(chip, enable_pin);
        if (!enable_line) {
            std::cerr << "紧急关闭：无法获取使能引脚" << std::endl;
        } else {
            if (gpiod_line_request_output(enable_line, "EMERGENCY_ENABLE", 1) < 0) {
                std::cerr << "紧急关闭：无法请求使能引脚" << std::endl;
            } else {
                gpiod_line_set_value(enable_line, 1);
                std::cerr << "!!! 使能引脚已被强制拉高 !!!" << std::endl;
                gpiod_line_release(enable_line);
            }
        }
    }

    if (indicator_pin >= 0) {
        gpiod_line* indicator_line = gpiod_chip_get_line(chip, indicator_pin);
        if (!indicator_line) {
            std::cerr << "紧急关闭：无法获取指示灯引脚" << std::endl;
        } else {
            if (gpiod_line_request_output(indicator_line, "EMERGENCY_INDICATOR", 1) < 0) {
                std::cerr << "紧急关闭：无法请求指示灯引脚" << std::endl;
            } else {
                gpiod_line_set_value(indicator_line, 1);
                gpiod_line_release(indicator_line);
            }
        }
    }

    gpiod_chip_close(chip);
}


