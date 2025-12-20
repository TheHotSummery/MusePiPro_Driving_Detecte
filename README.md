
# Muse Pi Pro 驾驶行为检测系统

**基于 SpaceMIT RISC-V 平台的嵌入式视觉与 PLC 控制系统工程原型**

[Gitee 原始仓库](https://gitee.com/achenjiayi/MusePiPro_Driving_Detecte) | [Github 镜像](#)

---

## 项目简介

本项目是一个面向工业与车载场景的 **视觉驱动 PLC 控制系统**，运行于基于 RISC-V 架构的 Muse Pi Pro 开发板（SpaceMIT K1 / Bianbu Linux）。

系统通过摄像头采集视频流，利用 YOLOv8 模型配合 NPU 加速进行实时目标检测与行为分析（DMS），并通过 Modbus TCP 与软 PLC Runtime 通信，最终驱动隔离 GPIO 与继电器完成可靠的硬件控制。

**核心设计目标**：稳定、解耦、工程可复现。
本系统采用“视觉识别”与“安全执行”分离的架构，所有非核心模块（如 4G/GPS）均可按需配置，确保在最小系统配置下仍可稳定运行。

## 系统架构

系统采用 **单进程 + 多线程后端模型**，其中 PLC Runtime 作为独立进程运行，由 Supervisor 统一管理，确保高可用性。
	
![architecture](./assets/architecture.JPG)


### 1. 进程与模块划分
*   **Supervisor**：负责进程拉起、状态监控与异常重启。当 PLC Runtime 崩溃时自动尝试重启。
*   **Vision Backend (YOLO + 行为分析)**：负责摄像头数据采集、NPU 推理、行为判定，并周期性向 PLC 写入心跳。
*   **PLC Runtime (独立进程)**：运行梯形图逻辑，维护 Modbus TCP Server (Port 502)，负责 GPIO 映射与输出控制。

### 2. 数据流向
`Camera → YOLO (NPU) → Behavior Analysis → PLC Runtime (Modbus) → GPIO → Relay → External Device`

### 3. 心跳与故障导向安全 (Fail-Safe)
系统内置严格的心跳监测与异常处理机制：
*   **YOLO 崩溃/卡死**：PLC 检测不到心跳，自动关闭所有输出，状态指示灯进入“快闪”报错模式。
*   **PLC 崩溃**：Supervisor 捕获信号，自动尝试重启 PLC Runtime，期间硬件输出复位。
*   **断电保护**：系统意外断电后，总输出采用机械拉低，防止执行器误动作。

## 核心特性

*   **RISC-V + NPU 加速**：基于 SpaceMIT K1 NPU ，利用 SpaceMITExecutionProvider 运行量化 ONNX 模型，显著降低 CPU 负载。
*   **工业级控制逻辑**：PLC 逻辑与 AI 算法解耦，AI 仅负责“看”，PLC 负责“控”。
*   **标准化接口**：对外暴露标准 Modbus TCP 接口，支持多客户端并发，便于接入上位机。
*   **多模态反馈**：支持振动马达、LED、蜂鸣器等多种硬件反馈组合。
*   **扩展性**：支持 EC800M (4G/GPS) 模块扩展，支持断点续传与本地缓存。

## 隐私与数据安全

本项目遵循 **Privacy by Design** 原则，默认情况下**不收集、不存储、不上传**任何用户隐私数据。

### 1. 数据处理策略
*   **纯本地运行**：核心检测与控制逻辑完全在本地 NPU 上完成，无需联网即可工作。
*   **无云端依赖**：本项目不包含任何强制的云服务绑定，也不提供地图 API Key。

### 2. 可选的数据上传 (需手动开启)
仅在启用 4G/GPS 扩展模块并在配置文件中明确配置服务器地址后，系统才会上传以下脱敏数据：
*   行为事件类型（如“闭眼”、“打哈欠”）
*   事件发生时的 GPS 坐标与时间戳
*   *注：图像视频流默认不上传，仅在调试模式下可通过局域网查看。*

## 硬件支持

*   **主控平台**：Muse Pi Pro (SpaceMIT K1)
*   **操作系统**：Bianbu Linux
*   **视觉输入**：USB 摄像头 (V4L2, 默认 `/dev/video20`)
*   **执行机构**：建议配合mos或其他隔离驱动板使用，切勿直接使用 GPIO 驱动大电流负载。

## 安装与部署

本项目包含完整的硬件接线图、系统环境配置及软件部署流程。

详细的图文部署指南，请参阅文档：
 **[./三端部署说明.md](./三端部署说明.md)**

*(该文档包含：硬件连接示意图、NPU 驱动检查、依赖安装及服务自启动配置等详细步骤)*

详细的Modbus默认资源说明，请参阅文档：
 **[./plc_cpp/Modbus资源展示.md](./plc_cpp/Modbus资源展示.md)**

关于单独plc进程的说明：
 **[./plc_cpp/README.md](./plc_cpp/README.md)**
 

## 许可证

本项目采用 **GPL-3.0** 许可证。
仅供工程研究与原型验证使用。

---
