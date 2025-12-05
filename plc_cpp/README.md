# PLC C++ 控制系统

## 简介
基于C++的简易PLC控制系统，用于控制GPIO输出，实现定时闪烁功能和YOLO疲劳检测联动控制。



## 注意事项

使用前请务必对照资源展示表，确保相关的gpio连接正确！！！！系统由于当时修改设备树不成（差点变砖，且其他人复刻有风险），但设备是上电gpio默认高电平，所以采用绕开因上电默认高电平而误触发的方案：采用机械继电器控制总输出（高电平默认关闭，低电平触发），MOSFSET 作为六路分控制（高电平触发），所以开机瞬间继电器会断开，断开输出。必须等到plc就绪（大概启动2分钟左右？？），才会主动拉低gpio，同时继电器吸合。然后yolo和plc进程都是写成了系统服务，但是yolo初始化需要进行时间戳校准，通信核验，模型加载，历史信息读取，所以还需要1分钟yolo才会就绪，届时指示灯会慢闪。



总体指示灯：

系统上电：指示灯常亮

大约2分钟，plc就绪，但是yolo还在初始化：指示灯快闪

再过1分钟，yolo也就绪，系统持续工作：指示灯慢闪



## 特别注意

这个plc系统是我用来学习和尝试实现再riscv上的可行性的，代码写的非常一般！很多也是现学现用，所以勿喷，感谢各位大佬，后面自己也会去挨个学习，尽量实现功能更加全面。但是适用于这个疲劳驾驶监测系统完全没问题，测试过连续12h运行和随机断电再上电，系统能正常运转。

### 对比表：Siemens S7-1200 vs 本实现

| 特性 | Siemens S7-1200 | 本实现 | 差距 |
|------|----------------|--------|------|
| **编程语言** | LAD/FBD/STL/SCL | JSON配置 | ⚠️ 不足 |
| **指令集** | 200+指令 | ~10指令 | ⚠️ 不足 |
| **I/O点数** | 最多14输入/10输出 | 3输入/6输出 | ⚠️ 有限 |
| **扫描周期** | 可配置，最小1ms | 固定20ms | ⚠️ 不够灵活 |
| **实时性** | 硬实时保证 | 软实时 | ⚠️ 无保证 |
| **可靠性** | MTBF > 100,000小时 | 只测过24h的 | ⚠️ 未长时间验证 |
| **诊断功能** | 完整诊断 | 基础诊断 | ⚠️ 功能有限 |
| **通信协议** | Profinet/Ethernet/Modbus | Modbus TCP | ⚠️ 单一协议 |
| **固件更新** | 在线更新 | 需重新编译 | ⚠️ 不便 |
| **价格** | $200-500 | 开源 | ✅ 成本优势 |

### 对比表：Allen-Bradley MicroLogix vs 本实现

| 特性 | MicroLogix 1400 | 本实现 | 差距 |
|------|-----------------|--------|------|
| **编程软件** | RSLogix 500 | JSON编辑器 | ⚠️ 无IDE |
| **在线监控** | 完整支持 | 部分支持 | ⚠️ 功能有限 |
| **数据记录** | 内置 | 不支持 | ⚠️ 部分支持 |
| **PID控制** | 内置 | 不支持 | ⚠️ 不支持 |
| **网络功能** | Ethernet/IP | Modbus TCP | ⚠️ 比较单一 |

---

## 功能特性
- 6个GPIO输出控制 (Q0-Q5)
- 定时器控制 (T1-T6)
- 计数器支持
- 中间继电器 (M0-M51)
- YOLO疲劳检测联动 (M40-M45)
- Modbus TCP 服务器
- 看门狗保护
- 配置热重载
- Supervisor-Worker 进程架构

## 环境要求

- **硬件**: Spacemit Muse Pi Pro (RISC-V) 开发板
- **操作系统**: Bianbu
- **依赖库**:
  ```bash
  sudo apt-get update
  sudo apt-get install -y build-essential cmake libgpiod-dev libmodbus-dev pkg-config
  ```

## 快速开始

### 1. 手动编译

#### 方法一：使用 CMake（推荐）
```bash
cd plc_cpp
mkdir -p build
cd build
cmake ..
make
```

编译成功后，可执行文件位于 `build/plc_core`

#### 方法二：直接编译（不推荐）
```bash
cd plc_cpp
g++ -std=c++17 -I./include -I./include/nlohmann \
    src/*.cpp -o plc_core \
    -lgpiod -lmodbus -lpthread \
    `pkg-config --cflags --libs libgpiod libmodbus`
```

### 2. 配置文件

程序默认加载配置文件（按优先级）：
1. **优先**: `/home/hyit/plc/plc_config.json` (默认统一配置文件)
2. **备选**: `config/plc_config.json` (本地统一配置文件)
3. **回退**: `config/system_config.json` + `config/user_config.json` (双文件模式，这个是最初版本，但是保留)

**注意**: 如果 `/home/hyit/plc/plc_config.json` 不存在或加载失败，会自动尝试其他配置。

可以通过命令行参数指定配置文件：
```bash
./build/plc_core /home/hyit/plc/plc_config.json
# 或
./build/plc_core config/plc_config.json
```

### 3. 运行

**注意**: 需要 sudo 权限运行（GPIO 访问需要 root 权限）

```bash
cd build
sudo ./plc_core
```

或指定配置文件：
```bash
sudo ./plc_core ../config/plc_config.json
```

若端口 502 无法绑定，可运行：
```bash
sudo setcap 'cap_net_bind_service=+ep' ./build/plc_core
```

### 4. 停止

- 使用 `Ctrl+C` 发送 SIGINT 信号
- 或使用 `kill` 命令：
  ```bash
  sudo pkill -SIGTERM plc_core
  ```

### 5. 检查状态

查看进程状态：
```bash
ps aux | grep plc_core
```

查看共享内存：
```bash
ipcs -m | grep plc_shared_memory
```

### 6. 绑定开机自启（需要换成自己的实际位置，yolo的看muse-hardware文件夹）
创建systemd服务文件
```bash
sudo nano /etc/systemd/system/plc-core.service
```
编辑服务文件内容
```bash
[Unit]
Description=PLC Core Application
After=network.target
Wants=network.target

[Service]
Type=simple
User=hyit
WorkingDirectory=/home/hyit/文档/musepiproplus/plc_cpp/build
ExecStart=/home/hyit/文档/musepiproplus/plc_cpp/build/plc_core
Restart=on-failure
RestartSec=5
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```
 常用操作命令
 ```bash
# 1. 重新加载systemd配置
sudo systemctl daemon-reload

# 2. 启用开机自启
sudo systemctl enable plc-core.service

# 3. 立即启动服务
sudo systemctl start plc-core.service

# 4. 停止服务
sudo systemctl stop plc-core.service

# 5. 取消开机自启
sudo systemctl disable plc-core.service

# 6. 查看服务状态
sudo systemctl status plc-core.service

# 7. 查看服务日志
sudo journalctl -u plc-core.service -f

# 8. 重启服务
sudo systemctl restart plc-core.service

# 9. 查看是否已启用自启
systemctl is-enabled plc-core.service

# 10. 列出所有已启用的服务
systemctl list-unit-files --state=enabled | grep plc
 ```




## 文件说明

- `CMakeLists.txt` - CMake 构建配置
- `src/` - 源代码目录
- `include/` - 头文件目录
- `config/` - 配置文件目录
  - `plc_config.json` - 统一配置文件（推荐使用）
  - `system_config.json` - 系统配置文件
  - `user_config.json` - 用户配置文件
  - `yolo_config.json` - YOLO 联动配置示例
- `build/plc_core` - 编译后的可执行文件
- plc配置文件生成器.html - 简易可视化plc程序配置器

## 配置说明

### 统一配置 (plc_config.json)
包含没有钱疲劳驾驶监测所有梯形图逻辑、定时器和计数器的完整配置。

### 双文件模式
- **系统配置** (system_config.json): 系统级逻辑
- **用户配置** (user_config.json): 用户自定义逻辑

两个配置文件会合并执行。

### YOLO 联动配置
- **M40** = YOLO Level 1 标志位
- **M41** = YOLO Level 2 标志位  
- **M42** = YOLO Level 3 标志位
- 通过 Modbus TCP 写入 M40-M42，PLC 自动响应控制输出

## 故障排除

1. **编译失败**: 
   - 检查依赖是否安装: `sudo apt-get install libgpiod-dev libmodbus-dev`
   - 检查 CMake 版本: `cmake --version` (需要 >= 3.10)
   - 检查编译器: `g++ --version` (需要支持 C++17)

2. **权限问题**: 
   - GPIO 访问需要 root 权限，必须使用 `sudo` 运行
   - 如果仍有权限问题，检查 `/dev/gpiochip*` 设备权限

3. **进程无法停止**: 
   - 使用 `sudo pkill -SIGTERM plc_core` 停止
   - 如果无效，使用 `sudo pkill -9 -f plc_core` 强制终止

4. **配置文件错误**: 
   - 检查 JSON 格式是否正确
   - 使用 `jq` 工具验证: `jq . config/plc_config.json`
   - 查看日志输出中的错误信息

5. **Modbus 连接失败**:
   - 检查端口 502 是否被占用: `sudo netstat -tlnp | grep 502`
   - 检查防火墙设置

6. **共享内存错误**:
   - 清理旧的共享内存: `sudo ipcrm -M <shmid>`
   - 或重启系统

## GPIO 连接对照表

| GPIOX | 功能 |
|------|----------------|
| GPIO35 | Q0（调试呼吸灯） |
| GPIO46 | Q1 |
| GPIO37 | Q2 |
| GPIO71 | Q3（震动马达） |
| GPIO72 | Q4（LED） |
| GPIO73 | Q5（蜂鸣器） |
| GPIO74 | IN1（可选接光电传感器，有人时候启动） |
| GPIO91 | IN2 |
| GPIO92 | IN3 |
| GPIO33 | 总继电器使能 |
| GPIO51 | 指示灯 |
| UART0_TXD | 有线串口调试 |
| UART0_RXD | 有线串口调试 |

## 注意事项

- ⚠️ **必须使用 sudo 权限运行**（GPIO 访问需要 root 权限）
- ⚠️ 确保 GPIO 引脚未被其他程序占用
- ⚠️ 修改配置后需要重启 PLC 才能生效
- ⚠️ 使用 `Ctrl+C` 或 `SIGTERM` 信号优雅停止，避免直接 `kill -9`
- ⚠️ 程序使用 Supervisor-Worker 架构，父进程监控子进程心跳
- ⚠️ 看门狗超时（10秒）会自动触发紧急停止

## 详细文档





- `Modbus资源展示.md` - Modbus 地址映射表
- `yolo_config运行逻辑说明.md` - YOLO 联动逻辑说明
- `CODE_REVIEW_REPORT.md` - 代码审查报告
- `CRITICAL_FIXES.md` - 关键问题修复方案
- 
---

## 许可证

本项目为

---

## 联系方式

如有问题或建议，请联系https://gitee.com/achenjiayi。或者来提出ISSUES？

---
## 版本信息

- **版本**: v2.0
- **C++标准**: C++17
- **扫描周期**: 20ms
- **适用平台**: Spacemit Muse Pi Pro (RISC-V)
- **日期**:2025-12-5
- **项目地址**: [Gitee：Muse Pi Pro 疲劳感知与多级预警系统](https://gitee.com/achenjiayi/MusePiPro_Driving_Detecte)