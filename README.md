# Muse Pi Pro 驾驶行为检测系统

## 项目概述

Muse Pi Pro 驾驶行为检测系统是一款尖端的嵌入式解决方案，专为实时监测驾驶员行为设计，旨在提升道路和驾驶安全。系统利用 YOLOv8 深度学习模型，通过摄像头捕捉驾驶员的 9 种行为（闭眼、打哈欠、闭眼向左/右、低头、向左/右看、抬头、专注驾驶），精准检测疲劳和分心驾驶状态。部署于 Muse Pi Pro 开发板（搭载 SpaceMIT K1 NPU 和 Bianbu 系统），系统通过 NPU 加速实现高效推理，并整合前端可视化界面和硬件警报（振动马达、LED、蜂鸣器），提供多模态反馈。系统支持动态权重调整、快速恢复机制和详细性能日志，适用于车载安全场景。



### V2.1.1 新增PLC控制逻辑

#### 1. 设计愿景与核心诉求

- **安全第一**：任何软件崩溃、断电或人为误操作都不能导致执行器失控。PLC 负责最后一道闸门。
- **层次解耦**：yolo视觉算法关注识别正确性；PLC 关注执行逻辑与硬件安全；上位机/客户系统通过 Modbus 统一接入。
- **可维护性**：自由配置驱动、日志清晰、远程诊断简单；替换模型或轻微调整逻辑，无需改核心代码。
- **工业交付**：支持 24/7 守护运行、485 通讯、状态指示灯。

---

#### 2.1 视觉与行为分析链路

- 摄像头图像经过 YOLO 模型，获取驾驶员疲劳/分心行为标签。
- `BehaviorAnalyzer` 依据行为权重、持续时间等策略生成疲劳等级：`Normal / Level 1 / Level 2 / Level 3`。
- 等级变化时调用 `GPIOController.update_alert_level()`。
- 改造后 `GPIOController` 不再直接驱动 GPIO，而是调用 `PLCBridge` 写入 Modbus 线圈（YOLO Flag）。

#### 2.2 PLC Runtime（Worker 进程）

- 启动后加载 `plc_config.json`，配置定时器、梯形图逻辑、GPIO 资源。
- 以 20 ms 周期执行：读输入 → 梯形图 → 写输出 → 喂看门狗 → 同步 Modbus 映射。
- 对外暴露 Modbus TCP 服务，并且通过Modbus监测yolo存活心跳。
- 指示灯线程以 2 秒周期慢闪指示“正在运行”，快闪表示plc就绪，但是yolo未就绪。
- 停机/异常时自动把 Enable 拉高、输出拉低、指示灯常亮。

#### 2.3 Supervisor（父进程）

- 负责 fork、监控心跳、处理信号、兜底紧急复位GPIO。
- 发现 Worker 无响应或退出后，先 `SIGTERM`，3 秒内无效则 `SIGKILL`。
- 最终调用 `perform_emergency_gpio_shutdown()` 直接拉高 Enable、关闭所有输出、指示灯常亮。

#### 2.4 PLCBridge（Python 包装器）

- 以守护进程方式启动 `plc_core`，维护 Modbus TCP 客户端连接。
- 提供 `set_alert_level(level)`、`set_memory_bit(index,value)` 等方法，供 Python 层写入消费。
- 自动重连机制：PLC 重启或网络瞬断后会重新连接。
- 

#### 3. PLC资源展示一览表（2025-11-8版本）

#### 3.1线圈（Coils, 功能码 0x01 / 0x05）

| 1-based 地址 | 0-based 地址 | PLC 对象 | 说明                                                     |
| ------------ | ------------ | -------- | -------------------------------------------------------- |
| 00001-00006  | 0-5          | Q0-Q5    | 六路物理输出；写入后立即驱动 GPIO 35/46/37/71/72/73      |
| 00007-00046  | 6-45         | M0-M39   | 通用中间继电器；支持读写                                 |
| 00047-00052  | 46-51        | M40-M45  | YOLO 状态位（对应 `Y1-Y6`）；读写均可                    |
| 00053-00058  | 52-57        | M46-M51  | 输出状态镜像，只读；任何写入都会被立即回写成真实输出状态 |

> 当前配置：`Q5` 与 `M0` 保持同步，写 `coil 00007` 可远程控制 `Q5`。
>
> 

#### 3.2 离散量输入（Discrete Inputs, 功能码 0x02）

| 1-based 地址 | 0-based 地址 | PLC 对象 | 硬件 GPIO |
| ------------ | ------------ | -------- | --------- |
| 10001        | 0            | I0       | GPIO 74   |
| 10002        | 1            | I1       | GPIO 91   |
| 10003        | 2            | I2       | GPIO 92   |

#### 3.3 保持寄存器（Holding Registers, 功能码 0x03 / 0x06）

| 1-based 地址 | 说明                                   |
| ------------ | -------------------------------------- |
| 40001        | `scan_counter` 低 16 位（扫描次数）    |
| 40002        | `scan_counter` 高 16 位                |
| 40003        | `scan_time_ms`（上一次扫描周期，毫秒） |
| 40004        | `error_code`（0=正常）                 |
| 40005        | `heartbeat`（心跳计数，每秒加 1）      |
| 40006        | `emergency_stop` 标志（1 表示触发）    |
| 40007-40032  | 预留（当前读取为 0）                   |

#### 3.4 输入寄存器（Input Registers, 功能码 0x04）

- 地址 30001-30032 预留，当前返回 0。











### V2.0.0完善的功能

1. 完善了GPS模块的使用，可以根据真实卫星，来获取当前设备的位置、海拔、朝向、速度、卫星数

2. 完善了4G模块的使用，可以在户外路段上面对用户疲劳驾驶的行为进行分析加密上传，用于集成监测，且当信号不良上传失败（隧道等特殊地段），会预先在本地进行加密缓存，待信号良好的时候，会将之前缓存的疲劳驾驶的信息进行上传。上传的信息包括疲劳驾驶类别，置信度，行为持续时间，gps原始信息。
3. 完善了检测平台的部署，可以通过WebSocket实时接收服务器发送的最新信息，确保重要事件实时显示。通过图表实时显示疲劳总数据和预警分析，确保疲劳行为无处遁形。实施前后端分离设计，方便代码维护和迁移。
4. 硬件代码全部拆成了模块化，便于单个添加功能，避免单文件冗杂。添加了启动自检，确保模块稳定运行不出错，若实在模块有错误，可屏蔽部分功能，始终需确保系统可以正常运行。





### 核心功能
- **实时行为检测**：基于 YOLOv8，识别 9 类驾驶行为，置信度阈值动态可调。
- **分心等级与警报**：
  - 基于行为加权评分（`PROGRESS_SCORE`），分级为正常（<50%）、一级（50-75%）、二级（75-95%）、三级（≥95%）。
  - 通过 GPIO 硬件（GPIO70 振动马达、GPIO71 LED、GPIO72 蜂鸣器）触发警报，与前端音频（低/中/高等级）同步。
- **前端交互界面**：
  - 实时视频流（320x320 分辨率，MJPEG 格式）。
  - 分心等级进度条（绿色/黄色/红色）及文字展示（正常/一级/二级/三级警报）。
  - 动态权重调节模块，允许管理员调整行为权重（如闭眼权重从 0.8 调至 1.0）。
  - 事件记录表格，实时显示行为、持续时间、次数、置信度和等级。
- **快速恢复机制**：
  - 专注驾驶 3 秒后，三级警报降至一级，进度重置至 50%。
  - 无分心行为 10 秒后，恢复正常状态，进度归零。
- **性能监控**：
  - 每次推理记录 CPU、内存、磁盘 I/O、网络 I/O 和推理时间，保存至 `app_log.txt`。
  - 支持 JSON 格式日志，便于性能分析。

### 技术栈
- **后端**：Python, Flask, SocketIO, ONNX (需要SpaceMIT NPU 加速), OpenCV, gpiozero, psutil
- **前端**：HTML, Tailwind CSS, SocketIO, jQuery, DataTables
- **硬件**：Muse Pi Pro
- **系统**：Bianbu

## 系统运行逻辑与原理

### 1. 系统架构
系统采用前后端分离架构，通过 Flask 和 SocketIO 实现实时通信：
- **后端**（`ljy_yolo_detector725.py`）：
  - **视频流处理**：从摄像头（`/dev/video20`）或静态图像获取帧，预处理为 320x320 输入。
  - **推理模块**：使用 YOLOv8 ONNX 模型（`cpu320_yolov8n_final1.q.onnx`），通过 SpaceMIT NPU 加速推理，输出 9 类行为的边界框和置信度。
  - **行为分析**：基于行为权重（`BEHAVIOR_WEIGHTS`）和时间窗口（`CONFIG["window_size"]`），计算分心进度（`PROGRESS_SCORE`），触发多级警报。
  - **硬件控制**：通过 `gpiozero` 控制 GPIO70/71/72，分别触发振动、LED 和蜂鸣器。
  - **日志记录**：记录推理时间、CPU、内存、I/O 等性能指标，支持性能分析。
- **前端**（`index.html`）：
  - 通过 SocketIO 接收后端推送的检测结果（`detection_update`），更新视频流、进度条、事件表格和状态信息。
  - 提供交互式调试面板，支持动态调整权重和参数，实时发送至后端。
  - 音频播放（`alert_low/alert_mid/alert_high.mp3`）与后端警报等级同步，5 秒冷却防止重复触发。

### 2. 行为检测与分心评分
系统基于 YOLOv8 模型检测 9 类行为，每类行为分配权重（`BEHAVIOR_WEIGHTS`）：
- **严重行为**：闭眼（0.8）、打哈欠（0.7）、闭眼向左/右（0.6）。
- **中等行为**：低头（0.5）、向左/右看（0.4）。
- **轻微行为**：抬头（0.3）。
- **正常行为**：专注驾驶（0.0）。

**分心评分逻辑**：
- 每次检测到非专注行为，`PROGRESS_SCORE` 增加 `CONFIG["progress_increment"] * weight`（默认增量 5%）。
- 进度分级：
  - <50%：正常，绿色进度条。
  - 50-75%：一级警报，绿色进度条，触发 GPIO70（振动马达闪烁）。
  - 75-95%：二级警报，黄色进度条，触发 GPIO70 和 GPIO71（LED 常亮）。
  - ≥95%：三级警报，红色进度条，触发 GPIO70、GPIO71、GPIO72（LED 与蜂鸣器交替）。
- **恢复机制**：
  - 专注驾驶检测（`focused` 或置信度低于阈值）触发进度递减：
    - 专注驾驶：每秒 -15%（`progress_decrement_focused`）。
    - 无分心：每秒 -2%（`progress_decrement_normal`）。
  - 三级警报后，3 秒专注驾驶降至一级（`PROGRESS_SCORE=50`）。
  - 10 秒无分心恢复正常（`PROGRESS_SCORE=0`）。

### 3. 特判与特殊设计
系统设计了多项特判和优化，确保鲁棒性和实时性：

- **事件合并机制**：
  - 在 5 秒时间窗口（`event_merge_window`）内，相同行为的连续检测合并为单一事件，更新持续时间和次数，减少事件表格累积速度。
  - 合并逻辑：若新事件与上一事件行为相同且时间差小于 5 秒，更新上一事件的 `duration` 和 `count`，避免重复记录。
- **动态权重调整**：
  - 前端提供滑块，允许用户实时调整行为权重（如将闭眼权重从 0.8 调至 1.0），通过 `update_weights` 事件同步至后端。
  - 权重验证（0.0-1.0），确保调整不影响系统稳定性。
  - 提供“恢复默认权重”按钮，重置为初始值（如闭眼 0.8，打哈欠 0.7）。
- **快速恢复机制**：
  - 针对严重分心（如闭眼），系统允许通过权重调整加快进度累积，同时通过 3 秒专注驾驶快速降级，平衡敏感性和误报率。
  - 10 秒无分心触发全局重置（清空 `BEHAVIOR_TRACKER` 和 `DISTRACTED_TIMESTAMPS`），确保系统适应驾驶员状态变化。
- **GPIO 交替控制**：
  - 三级警报时，GPIO71（LED）和 GPIO72（蜂鸣器）以 0.5 秒间隔交替触发 3 次，增强警报效果。
  - 手动触发模式支持独立控制 GPIO70/71，GPIO72 触发时自动交替 GPIO71，提升调试灵活性。
- **性能优化**：
  - SpaceMIT NPU 加速 ONNX 推理，单帧处理时间约 180ms。
  - SocketIO 事件每 0.25 秒推送，优化带宽占用。
  - 日志采用 JSON 格式（`app_log.txt`），便于性能分析。
- **鲁棒性设计**：
  - 异常处理覆盖视频流断开、模型加载失败、GPIO 初始化错误等场景。
  - 自动重连机制（SocketIO `reconnection_attempts=5`）确保前端稳定。
  - 日志文件使用 `RotatingFileHandler`，限制大小为 10MB，避免存储溢出。

### 4. 算法原理
- **YOLOv8 模型**：
  - 输入：320x320 RGB 图像，归一化至 [0,1]。
  - 输出：边界框（x, y, w, h）和 9 类置信度，经 NMS（IoU 阈值 0.5）过滤。
  - NPU 加速：`spacemit-ort` 将卷积运算卸载至 SpaceMIT K1 NPU，降低 CPU 负载。
- **行为追踪**：
  - 使用 `BEHAVIOR_TRACKER`（字典+队列）记录每类行为的时间戳和置信度，时间窗口内（`window_size=30s`）累计检测次数。
  - 单一行为检测：若行为持续时间超过阈值（`duration_threshold=1.5s`，疲劳行为 2.0s），触发事件。
  - 多行为检测：当时间窗口内不同行为数≥3（`count_threshold`）且加权得分≥0.8（`score_threshold`），触发二级警报。
  - 连续分心：90 秒内分心事件≥7 次（`continuous_distracted_count`），触发三级警报。
- **置信度特判**：
  - 疲劳行为（如闭眼）使用更高置信度阈值（`fatigue_min_confidence=0.85`），非疲劳行为为 0.8。
  - 检测结果过滤：边界框宽高小于 5 像素的无效检测被丢弃。
- **进度计算**：
  - `PROGRESS_SCORE` 通过行为权重累加，每次增量为 `progress_increment * weight`。
  - 冷却机制：多行为警报和三级警报分别设置 10 秒和 30 秒冷却（`multi_event_cooldown`, `level3_cooldown`）。

## 安装步骤

### 1. 环境准备
确保使用 Muse Pi Pro 开发板，运行 Bianbu 系统。系统需支持 SpaceMIT NPU 驱动。

```txt
# 更新系统和安装基本工具
sudo apt update && sudo apt upgrade -y
sudo apt install python3 python3-pip libopencv-dev python3-opencv
```

### 2. 克隆项目
将项目克隆至 Muse Pi Pro 板子：

```txt
git clone https://gitee.com/achenjiayi/MusePiPro_Driving_Detecte.git
cd muse-pi-pro-driver-monitoring
```

### 3. 安装依赖
项目依赖以下 Python 包：

```
pip install -r requirements.txt
```

**requirements.txt** 内容：
```txt
Flask==3.1.1
Flask-SocketIO==5.5.1
numpy==1.26.4
opencv-python==4.10.0.82
onnx==1.18.0
pandas==2.3.1
psutil==7.0.0
python-socketio==5.13.0
gpiozero==2.0.5
lgpio==0.2.2.0
spacemit-ort==1.2.2
pillow==11.3.0
```

**说明**：
- `spacemit-ort`：SpaceMIT NPU 加速，需确保 NPU 驱动已安装。
- `gpiozero` 和 `lgpio`：用于 GPIO 控制。
- 可选 PyTorch 模型依赖：

  ```txt
  torch==2.5.0
  torchvision==0.19.1
  ultralytics==8.3.169
  ```

### 4. 准备模型文件
- **ONNX 模型**：将 `muse_yolov8n_driving.q.onnx` 放置于项目根目录。
- **模型输入**：320x320 分辨率，9 类行为。

### 5. 准备前端静态文件
确保 `static` 文件夹包含：
- `tailwind.min.css`, `jquery-3.6.0.min.js`, `jquery.dataTables.min.css`, `jquery.dataTables.min.js`, `socket.io.min.js`, `style.css`, `zh.json`
- `audio/alert_low.mp3`, `audio/alert_mid.mp3`, `audio/alert_high.mp3`
- `placeholder.jpg`

**目录结构**：
```txt
muse-pi-pro-driver-monitoring/
├── main.py
├── templates/
│   ├── index.html
├── static/
│   ├── tailwind.min.css
│   ├── jquery-3.6.0.min.js
│   ├── jquery.dataTables.min.css
│   ├── jquery.dataTables.min.js
│   ├── socket.io.min.js
│   ├── style.css
│   ├── zh.json
│   ├── audio/
│   │   ├── alert_low.mp3
│   │   ├── alert_mid.mp3
│   │   ├── alert_high.mp3
│   ├── placeholder.jpg
├── muse_yolov8n_driving.q.onnx
├── app_log.txt
├── driving_events.csv
├── requirements.txt
```

### 6. 配置摄像头
- 确保usb摄像头连接至 `/dev/video20`，支持 320x320 分辨率和 4.5 FPS。
- 检查设备：

```txt
  v4l2-ctl --list-devices
```

### 7. 配置 GPIO
- 使用 GPIO70（振动马达）、GPIO71（LED）、GPIO72（蜂鸣器）。
- 由于GPIO无法起到直接驱动作用，所以务必外接驱动板！GPIO采用高电平触发，本项目采用了自制的双路DRV8701驱动，每路最高驱动10A电流。
- 确保 GPIO 连接正确，可通过如下命令查看GPIO：

```txt
sudo chmod 666 /dev/gpiochip0
source ~/gpioenv/bin/activate
pinout

```

## 运行说明

### 1. 启动后端
在项目根目录运行：

```txt
python3 main.py
```

- 服务监听 `0.0.0.0:5200`。
- 自动加载 ONNX 模型并启动推理线程。
- 日志保存至 `app_log.txt`。

### 2. 访问前端
在局域网内任意设备浏览器访问：
```txt
http://<Muse-Pi-Pro-IP>:5200/
```

- 界面显示视频流、分心等级、检测结果和事件记录。

### 3. 调试与监控
- **查看日志**：
```
  tail -f app_log.txt
```
  包含推理时间、CPU、内存、I/O 等指标。
- **手动触发**：前端调试面板支持触发 GPIO 和音频。
- **清空事件**：前端“清空记录和计数器”按钮重置日志和计数。

## 性能对比分析

### 测试所用Muse Pi Pro 硬件性能
- **CPU**：4 核 ARM Cortex-A55 @ 1.8 GHz
- **NPU**：SpaceMIT K1, 4 TOPS
- **存储**：64GB eMMC
  - 日志写入：50-100KB/s，读写次数 100-500 次/分钟。
- **GPIO**：响应时间 <10ms。

### Bianbu 系统性能
- **稳定性**：支持 >24 小时运行。

- **NPU 加速**：ONNX 推理比 PyTorch迅速。

- **实时性**：
  - 视频流延迟：<200ms。
  - SocketIO 带宽：10-20KB/s。
  
  

测试日志（测试于 2025-07-30 03:00:05 ，来源`app_log.txt`）：
```txt
2025-07-30 03:00:05,619 - INFO - Inference Metrics: CPU=46.7%, Per-Core=[86.4, 82.6, 73.9, 82.6, 9.5, 0.0, 31.8, 5.0], Memory=2712.6/7847.1MB (37.6%), DiskIO=Read:1555.1MB,Write:137.9MB,ReadCount:27303,WriteCount:10440, NetIO=Sent:2730.5MB,Received:95.9MB,SentPackets:2116270,RecvPackets:726515, Times=Pre:14.39ms,Inf:136.85ms,Post:12.43ms,Logic:1.62ms,Draw:4.00ms,Total:169.29ms, ProcessCPU=User:99.2s,System:6.6s, Threads=24
2025-07-30 03:00:05,621 - DEBUG - 更新 latest_annotated_frame
2025-07-30 03:00:05,672 - DEBUG - 预处理图像: shape=(1, 3, 320, 320), min=0.0, max=1.0
2025-07-30 03:00:05,673 - DEBUG - 预处理时间: 14.55 ms
2025-07-30 03:00:05,811 - INFO - ONNX 推理时间: 136.49 ms
2025-07-30 03:00:05,814 - DEBUG - ONNX 输出形状: (2100, 13), 数值范围: [0.0000, 322.5339]
2025-07-30 03:00:05,816 - DEBUG - 坐标范围（裁剪后）: [3.4403, 319.0000]
2025-07-30 03:00:05,818 - DEBUG - 检测到概率值，直接使用
2025-07-30 03:00:05,822 - DEBUG - 置信度分布: {0.0: 2089, 0.011: 1, 0.012: 2, 0.015: 1, 0.032: 1, 0.036: 1, 0.045: 1, 0.048: 1, 0.058: 1, 0.085: 1, 0.088: 1}
2025-07-30 03:00:05,824 - DEBUG - 无检测结果通过置信度筛选
2025-07-30 03:00:05,825 - DEBUG - 后处理时间: 11.96 ms
2025-07-30 03:00:05,826 - DEBUG - 发送事件: [], 分心次数: 5, 进度: 3.0, 等级: Normal
2025-07-30 03:00:05,828 - DEBUG - 逻辑处理时间: 2.29 ms
```

## 开源地址

项目开源于：

```txt
https://gitee.com/achenjiayi/MusePiPro_Driving_Detecte
```

## 注意事项
- 确保 `spacemit-ort` 与 NPU 驱动兼容，如果找不到`spacemit-ort` ，需手动从系统库拷贝到虚拟环境下。
- 定期清理 `app_log.txt` 和 `driving_events.csv`。

## 贡献
1. Fork 仓库。
2. 创建分支（`git checkout -b feature/xxx`）。
3. 提交更改（`git commit -m "Add xxx feature"`）。
4. 推送分支（`git push origin feature/xxx`）。
5. 提交 Pull Request。

## 许可证
MIT License

---
**日期**：2025-07-30