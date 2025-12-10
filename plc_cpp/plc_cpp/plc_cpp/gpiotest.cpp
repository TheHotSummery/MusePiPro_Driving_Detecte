#include <gpiod.h>
#include <chrono>
#include <csignal>
#include <iostream>
#include <thread>
#include <vector>

static bool running = true;

void signal_handler(int) {
    running = false;
}

int main() {
    std::vector<int> output_pins = {35, 46, 37, 71, 72, 73};

    gpiod_chip* chip = gpiod_chip_open_by_name("gpiochip0");
    if (!chip) {
        std::cerr << "无法打开 gpiochip0" << std::endl;
        return 1;
    }

    std::vector<gpiod_line*> lines;
    lines.reserve(output_pins.size());

    bool ok = true;
    for (size_t i = 0; i < output_pins.size(); ++i) {
        int pin = output_pins[i];
        gpiod_line* line = gpiod_chip_get_line(chip, pin);
        if (!line) {
            std::cerr << "获取 GPIO" << pin << " 失败" << std::endl;
            ok = false;
            break;
        }
        if (gpiod_line_request_output(line, ("gpiotest_" + std::to_string(i)).c_str(), 0) < 0) {
            std::cerr << "请求 GPIO" << pin << " 失败" << std::endl;
            ok = false;
            gpiod_line_release(line);
            break;
        }
        lines.push_back(line);
    }

    if (!ok) {
        for (auto line : lines) {
            gpiod_line_release(line);
        }
        gpiod_chip_close(chip);
        return 1;
    }

    std::signal(SIGINT, signal_handler);
    std::signal(SIGTERM, signal_handler);

    std::cout << "流水灯启动 (Ctrl+C 停止)..." << std::endl;

    size_t index = 0;
    const std::chrono::milliseconds on_time(300);
    const std::chrono::milliseconds off_time(200);

    while (running) {
        for (auto line : lines) {
            gpiod_line_set_value(line, 0);
        }
        gpiod_line_set_value(lines[index], 1);
        std::this_thread::sleep_for(on_time);

        gpiod_line_set_value(lines[index], 0);
        std::this_thread::sleep_for(off_time);

        index = (index + 1) % lines.size();
    }

    std::cout << "\n停止，正在清理..." << std::endl;

    for (auto line : lines) {
        gpiod_line_set_value(line, 0);
        gpiod_line_release(line);
    }
    gpiod_chip_close(chip);
    return 0;
}