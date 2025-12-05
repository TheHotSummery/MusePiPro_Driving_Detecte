# Muse Pi Pro 疲劳驾驶监测系统

基于 YOLOv8 的实时疲劳驾驶检测硬件端程序，运行在 Spacemit Muse Pi Pro (RISC-V) 开发板上。

## 目录

- [项目简介](#项目简介)
- [功能特性](#功能特性)
- [系统架构](#系统架构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [模块说明](#模块说明)
- [API接口](#api接口)
- [使用指南](#使用指南)
- [故障排除](#故障排除)
- [开发指南](#开发指南)
- [相关文档](#相关文档)

---

## 项目简介

Muse Pi Pro 疲劳驾驶监测系统是一个基于深度学习的实时驾驶员行为分析系统，通过摄像头采集驾驶员图像，使用 YOLOv8 n模型进行实时检测，识别疲劳、分心等危险驾驶行为，并通过 GPIO、PLC 联动等方式进行预警。

### 核心能力

- **实时视频处理**：基于 YOLOv8 的实时行为检测
- **疲劳等级评估**：四级疲劳等级（Normal、Level 1、Level 2、Level 3）
- **多维度检测**：闭眼、打哈欠、低头、左右乱看等行为识别
- **硬件联动**：GPIO 控制（LED、振动马达、蜂鸣器）、PLC Modbus 通信
- **数据上报**：事件、状态、GPS 数据实时上报到后端服务器
- **离线存储**：网络异常时自动缓存数据，网络恢复后自动重发

---

## 功能特性

### 🎯 核心功能

- ✅ **实时视频流处理**：支持 MJPEG 视频流输出，前端实时查看
- ✅ **YOLOv8n 行为检测**：支持 ONNX 和 PyTorch 模型格式（实际会检测是否有SpaceMITExecutionProvider，有则优先调用）
- ✅ **疲劳等级计算**：基于行为权重和持续时间的智能评分系统
- ✅ **GPIO 硬件控制**：LED、振动马达、蜂鸣器三级预警
- ✅ **PLC Modbus 联动**：通过 Modbus TCP 与 PLC 系统通信
- ✅ **GPS 定位**：基于 Quectel EC800M 模块的 GPS 数据采集
- ✅ **数据上报**：事件驱动 + 定期状态上报 + GPS 位置上报
- ✅ **离线模式**：网络异常（隧道，山区）时自动切换离线模式，数据本地缓存

### 🔧 系统特性

- ✅ **性能监控**：CPU、内存、温度等系统指标实时监控
- ✅ **硬件看门狗**：防止系统异常重启（可选，需 root 权限）
- ✅ **日志管理**：自动日志轮转，支持调试模式
- ✅ **配置热重载**：部分配置支持运行时修改
- ✅ **优雅退出**：信号处理，资源清理

---

## 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                    Muse Pi Pro 硬件端                   │
├────────────────────────────────────────────────────────┤
│                                                        │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────┐  │
│  │  摄像头采集   │───▶│ 视频处理模块  │───▶│ YOLOv8 │  │
│  │  (Video20)   │    │VideoProcessor│    │  模型推理│   │
│  └──────────────┘    └──────────────┘    └──────────┘  │
│                              │                         │
│                              ▼                         │
│                    ┌─────────────────┐                 │
│                    │  行为分析模块    │                 │
│                    │BehaviorAnalyzer │                 │
│                    └─────────────────┘                 │
│           │                    │                    │  │
│           ▼                    ▼                    ▼  │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐│
│  │ GPIO控制器    │   │  PLC桥接     │   │ 网络管理器   ││
│  │GPIOController│   │  PLCBridge   │   │NetworkManager││
│  └──────────────┘   └──────────────┘   └──────────────┘│
│           │                    │                    │  │
│           ▼                    ▼                    ▼  │
│      LED/振动/蜂鸣器      Modbus TCP        数据上报API │
│                                                        │
└─────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │   后端服务器     │
                    │  muse-backendv2 │
                    └─────────────────┘
```

### 模块说明

- **start.py**：主启动脚本，系统初始化入口
- **web_server.py**：Flask Web 服务器，提供 HTTP API 和 WebSocket
- **video_processor.py**：视频处理模块，负责摄像头采集和 YOLO 推理
- **behavior_analyzer.py**：行为分析模块，疲劳等级计算和事件触发
- **gpio_controller.py**：GPIO 控制器，硬件输出控制
- **plc_bridge.py**：PLC Modbus 通信桥接
- **network_manager.py**：网络管理器，GPS 采集和数据上报
- **model_manager.py**：模型管理器，YOLO 模型加载和推理
- **performance_monitor.py**：性能监控模块
- **hardware_watchdog.py**：硬件看门狗模块
- **QuectelEC800M_final.py**：经过反复修改的Quectel EC800M AT通信模块

---

## 环境要求

### 硬件要求

- **开发板**：Spacemit Muse Pi Pro (RISC-V)
- **操作系统**：Bianbu
- **摄像头**：USB 摄像头（设备索引：/dev/video20）
- **网络模块**：Quectel EC800M（4G 模块，可选）
- **GPIO**：支持 lgpio 库的 GPIO 控制

### 软件依赖

- **Python**：3.8+
- **系统库**：需要安装 lgpio、libgpiod 等系统库
- **Python 包**：见 `requirements.txt`

---

## 快速开始

### 1. 安装依赖

```bash
# 安装系统依赖
sudo apt-get update
sudo apt-get install -y python3-pip python3-dev libgpiod-dev

# 安装 Python 依赖
pip3 install -r requirements.txt
```

### 2. 配置文件

主要配置文件：`config.py`

关键配置项：
- **摄像头设备索引**：`CAMERA_CONFIG["device_index"]`（默认：20）
- **模型路径**：`ONNX_MODEL_PATH` 
- **Web 服务器端口**：`WEB_CONFIG["port"]`（默认：5200）
- **网络模块**：`NETWORK_MODULE_CONFIG`（如果你手上没有EC800M，可以先禁用这个模块）

### 3. 运行程序

```bash
# 普通模式
python3 start.py

# 调试模式（详细日志）
python3 start.py --debug
```

### 4. 访问 Web 界面

浏览器访问：`http://<设备IP>:5200`

如果你不知道你设备ip
```bash
ifconfig
```

---

## 配置说明

### 摄像头配置

```python
CAMERA_CONFIG = {
    "display_width": 640,   # 前端显示分辨率
    "display_height": 640,
    "model_width": 320,     # 模型输入分辨率
    "model_height": 320,
    "device_index": 20      # 摄像头设备索引
}
```

### 模型配置

```python
MODEL_TYPE = "onnx"  # 或 "pt"
ONNX_MODEL_PATH = "muse_yolov8n_driving.q.onnx"
PT_MODEL_PATH = "best.pt"
MODEL_INPUT_SIZE = (320, 320)
```

### 疲劳检测配置

```python
CONFIG = {
    "duration_threshold": 1.5,      # 行为持续时间阈值（秒）
    "fatigue_duration_threshold": 2.0,
    "score_threshold": 0.8,           # 检测置信度阈值
    "fps_target": 4.5,                # 目标帧率
    "progress_increment": 3.0,       # 疲劳分数增量
    "progress_decrement_focused": 5.0 # 专注驾驶分数减量
}
```

### 网络模块配置

```python
NETWORK_MODULE_CONFIG = {
    "enabled": True,      # 是否启用网络模块
    "skip_gnss": False,   # 是否跳过 GPS 初始化
    "skip_ntp": False,    # 是否跳过 NTP 时间同步
    "skip_login": False   # 是否跳过设备登录
}
```

### 性能监控配置

```python
PERFORMANCE_MONITOR_CONFIG = {
    "enabled": True,           # 是否启用性能监控
    "log_dir": "performance_logs",  # 日志目录
    "interval": 5.0            # 记录间隔（秒）
}
```



---

## 模块说明

### 1. 视频处理模块 (video_processor.py)

负责摄像头采集、YOLO 模型推理、帧处理。

**主要功能**：
- 摄像头初始化和管理
- 视频帧采集和预处理
- YOLO 模型推理
- 检测结果后处理
- MJPEG 视频流生成

### 2. 行为分析模块 (behavior_analyzer.py)

负责疲劳等级计算、事件触发、行为追踪。

**主要功能**：
- 行为检测结果分析
- 疲劳分数计算（基于行为权重和持续时间）
- 疲劳等级判定（Normal、Level 1、Level 2、Level 3）
- 事件生成和记录
- 分心行为统计

**疲劳等级规则**：
- **Normal**：分数 < 60
- **Level 1**：分数 60-79
- **Level 2**：分数 80-94
- **Level 3**：分数 ≥ 95

### 3. GPIO 控制器 (gpio_controller.py)

- 已经替换成plc控制了，但是仍保留

### 4. PLC 桥接模块 (plc_bridge.py)

负责与 PLC 系统的 Modbus TCP 通信。

**主要功能**：
- Modbus TCP 客户端连接管理
- YOLO 等级信号写入（M40-M42 线圈）
- 连接状态监控和自动重连

**Modbus 资源映射**：
- **M40**：Level 1 信号
- **M41**：Level 2 信号
- **M42**：Level 3 信号

### 5. 网络管理器 (network_manager.py)

负责 GPS 数据采集、数据上报、离线存储。

**主要功能**：
- Quectel EC800M 模块管理
- GPS 数据采集和解析
- NTP 时间同步
- 设备登录和 Token 管理
- 数据上报（事件、状态、GPS）
- 离线数据缓存和重发

**数据上报类型**：
- **事件数据**：疲劳等级变化时上报
- **状态数据**：每 30 秒定期上报
- **GPS 数据**：每 20 秒上报一次

### 6. 模型管理器 (model_manager.py)

负责 YOLO 模型的加载和推理。

**支持格式**：
- ONNX 格式（需要配合SpaceMITExecutionProvider）
- PyTorch 格式（.pt 文件）

### 7. 性能监控模块 (performance_monitor.py)

负责系统性能指标监控和记录。

**监控指标**：
- CPU 使用率
- 内存使用率
- 设备温度
- 进程信息

。



---

## API接口

### Web 接口

#### 1. 视频流接口

```
GET /feed/webcam/
```
返回 MJPEG 视频流。

#### 2. 系统状态接口

```
GET /status
```
返回系统运行状态、疲劳等级、检测统计等信息。

#### 3. 配置接口

```
GET /config
POST /config
```
获取或更新系统配置。

#### 4. 行为权重接口

```
GET /weights
POST /weights
```
获取或更新行为权重配置。

#### 5. 事件列表接口

```
GET /events
```
获取历史事件列表。

#### 6. 清除事件接口

```
POST /clear_events
```
清除历史事件记录。

#### 7. GPIO 测试接口

```
POST /trigger_gpio
```
手动触发 GPIO 输出（测试用）。

### WebSocket 事件

#### 客户端监听事件

- `yolo_detection`：YOLO 检测结果
- `fatigue_level`：疲劳等级变化
- `event`：事件通知
- `status`：系统状态更新

#### 客户端发送事件

- `request_status`：请求系统状态
- `update_config`：更新配置
- `update_weights`：更新行为权重

### 数据上报接口（后端服务器）

#### 统一数据上报接口

```
POST /api/v2/data/report
```

**认证方式（任选其一）**：
- URL 参数：`?token=YOUR_TOKEN`
- Header：`Authorization: Bearer YOUR_TOKEN`
- Header：`token: YOUR_TOKEN`

**请求格式**：
```json
{
  "dataType": "event|status|gps",
  "timestamp": 1705123456789,
  "data": {
    // 根据 dataType 不同，data 结构不同
  }
}
```

详细接口规范请参考：`HARDWARE_DATA_API_SPECIFICATION-确定稿.md`

---

## 使用指南

### 启动系统

```bash
# 1. 进入项目目录
cd muse-hardware

# 2. 启动程序
python3 start.py

# 3. 或使用调试模式
python3 start.py --debug
```

### 配置开机启动
新建 systemd 服务
```bash
sudo nano /etc/systemd/system/musepiproplus.service
```
写入，需要换成你的实际目录
```bash
[Unit]
Description=Muse Pi Pro Plus YOLO Service
After=network-online.target
Wants=network-online.target

[Service]
User=hyit
WorkingDirectory=/home/hyit/文档/musepiproplus
Environment="PYTHONUNBUFFERED=1"
# 使用 bash -lc 保证 source 生效，并在虚拟环境里运行 start.py
ExecStart=/bin/bash -lc 'source yolo_env/bin/activate && python start.py'
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```
然后执行
```bash
sudo systemctl daemon-reload
sudo systemctl enable --now musepiproplus.service    # 立刻启动并设置开机自启
sudo systemctl status musepiproplus.service         # 查看运行状态
```

控制
```bash
# 停止
sudo systemctl stop musepiproplus.service

# 重新启动
sudo systemctl restart musepiproplus.service

# 看日志
sudo journalctl -u musepiproplus.service -f
```




### 访问 Web 界面

1. 在浏览器中打开：`http://<设备IP>:5200`
2. 查看实时视频流
3. 查看疲劳等级和检测统计
4. 查看历史事件记录

### 配置网络模块

编辑 `network_config.json`：

```json
{
  "device_id": "MUSE_PI_PRO_001",
  "backend_url": "http://your-backend-server:8080",
  "serial_port": "/dev/ttyUSB2",
  "baud_rate": 115200
}
```

### 禁用网络模块（离线模式）

编辑 `config.py`：

```python
NETWORK_MODULE_CONFIG = {
    "enabled": False  # 禁用网络模块
}
```

### 测试 GPIO 输出

通过 Web 界面或 API 调用：

```bash
curl -X POST http://localhost:5200/trigger_gpio \
  -H "Content-Type: application/json" \
  -d '{"level": "Level 2"}'
```

### 查看日志

```bash
# 查看应用日志
tail -f app_log.txt

# 查看性能监控日志
tail -f performance_logs/performance_*.log
```

---

## 故障排除

### 1. 摄像头无法打开

**问题**：摄像头初始化失败

**解决方案**：
- 检查摄像头设备索引是否正确（默认：/dev/video20）
- 检查摄像头是否被其他程序占用
- 尝试修改 `config.py` 中的 `device_index`

### 2. GPIO 控制失败

**问题**：GPIO 操作权限不足

**解决方案**：
- 使用 sudo 运行程序（不推荐）
- 检查用户是否在 gpio 组中
- 检查 `/dev/gpiochip*` 设备权限

### 3. 网络模块初始化失败

**问题**：EC800M 模块无法连接

**解决方案**：
- 检查串口设备路径（默认：/dev/ttyUSB2）
- 检查串口权限
- 检查模块是否正常工作
- 可以临时禁用网络模块：`NETWORK_MODULE_CONFIG["enabled"] = False`

### 4. PLC Modbus 连接失败

**问题**：无法连接到 PLC

**解决方案**：
- 检查 PLC 服务是否运行
- 检查 Modbus 端口（默认：502）
- 检查防火墙设置
- 查看日志中的连接错误信息

### 5. 模型加载失败

**问题**：YOLO 模型无法加载

**解决方案**：
- 检查模型文件是否存在
- 检查模型文件格式（ONNX 或 PyTorch）
- 检查 `MODEL_TYPE` 配置是否正确
- 查看日志中的错误信息

### 6. 性能问题

**问题**：系统运行缓慢，帧率低

**解决方案**：
- 降低模型输入分辨率
- 降低目标帧率（`fps_target`）
- 禁用性能监控（如果不需要）
- 检查系统资源使用情况

### 7. 数据上报失败

**问题**：数据无法上报到后端服务器

**解决方案**：
- 检查网络连接
- 检查后端服务器地址和端口
- 检查设备 ID 配置
- 查看网络管理器日志
- 使用测试模式：`?device_id=YOUR_DEVICE_ID`

---

## 开发指南

### 项目结构

```
muse-hardware/
├── start.py                    # 主启动脚本
├── config.py                   # 配置文件
├── web_server.py               # Web 服务器
├── video_processor.py          # 视频处理模块
├── behavior_analyzer.py        # 行为分析模块
├── gpio_controller.py           # GPIO 控制器
├── plc_bridge.py               # PLC 桥接模块
├── network_manager.py          # 网络管理器
├── model_manager.py            # 模型管理器
├── performance_monitor.py      # 性能监控模块
├── hardware_watchdog.py        # 硬件看门狗模块
├── utils.py                    # 工具函数
├── QuectelEC800M_final.py      # EC800M 模块驱动
├── requirements.txt            # Python 依赖
├── network_config.json         # 网络配置
├── templates/                  # Web 前端文件
│   ├── index.html
│   └── static/
├── *.md                        # 文档文件
└── *.pt / *.onnx              # 模型文件
```

### 添加新功能

1. **添加新的行为检测**：
   - 在 `config.py` 中添加新的类别到 `MODEL_CLASSES`
   - 在 `LABEL_MAP` 中添加标签映射
   - 在 `BEHAVIOR_WEIGHTS` 中添加权重

2. **修改疲劳等级规则**：
   - 编辑 `behavior_analyzer.py` 中的等级判定逻辑
   - 修改 `CONFIG` 中的相关阈值

3. **添加新的硬件输出**：
   - 在 `GPIO_CONFIG` 中添加新的引脚配置
   - 在 `gpio_controller.py` 中添加控制逻辑

### 调试

1. **启用调试模式**：
   ```bash
   python3 start.py --debug
   ```

2. **查看详细日志**：
   
   - 编辑 `config.py` 中的 `LOG_CONFIG["level"] = "DEBUG"`
   
3. **性能分析**：
   - 启用性能监控：`PERFORMANCE_MONITOR_CONFIG["enabled"] = True`
   - 查看性能日志文件

4. **测试单个模块**：
   - 使用测试脚本：`test_ec800m.py`、`test_mpu6050.py` 等

---

## 相关文档

### 核心文档

- **HARDWARE_DATA_API_SPECIFICATION-确定稿.md**：硬件数据上报 API 接口规范
- **DATA_STRUCTURE_SPECIFICATION.md**：数据结构规范
- **USEFUL_DATA_INVENTORY.md**：有用数据清单

### 其他文档

- **DATA_MAPPING_ANALYSIS.md**：数据映射分析

### 后端文档

- 后端 API 文档请参考 `muse-backendv2` 项目

---

## 版本信息

- **当前版本**：v2.0
- **适用平台**：Spacemit Muse Pi Pro (RISC-V)
- **Python 版本**：3.8+
- **最后更新**：2025-11-15

---

## 许可证

本项目为

---

## 联系方式

如有问题或建议，请联系https://gitee.com/achenjiayi。或者来提出ISSUES？

---

**注意**：本文档会持续更新，请定期查看最新版本。
