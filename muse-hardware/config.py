# -*- coding: utf-8 -*-
"""
配置文件
包含所有系统配置参数
"""

# 模型配置
MODEL_TYPE = "onnx"
ONNX_MODEL_PATH = "muse_yolov8n_driving.q.onnx"
PT_MODEL_PATH = "best.pt"
MODEL_INPUT_SIZE = (320, 320)

# 摄像头配置
CAMERA_CONFIG = {
    "display_width": 640,   # 前端显示分辨率宽度
    "display_height": 640,  # 前端显示分辨率高度
    "model_width": 320,     # 模型输入分辨率宽度
    "model_height": 320,    # 模型输入分辨率高度
    "device_index": 20      # 摄像头设备索引
}

# 模型类别
MODEL_CLASSES = [
    "head_down", "eyes_closed_head_left", "seeing_left", "head_up", "seeing_right",
    "yarning", "eyes_closed_head_right", "focused", "eyes_closed"
]
NUM_CLASSES = len(MODEL_CLASSES)

# 标签映射
LABEL_MAP = {
    "head_down": "低头", 
    "eyes_closed_head_left": "闭眼向左", 
    "seeing_left": "左右看",
    "seeing_right": "左右看",
    "yarning": "打哈欠",
    "eyes_closed_head_right": "闭眼向右", 
    "focused": "专注驾驶", 
    "eyes_closed": "闭眼",
    "head_up": "抬头"
}

# 疲劳类别
FATIGUE_CLASSES = ["eyes_closed", "yarning", "eyes_closed_head_left", "eyes_closed_head_right", "head_up"]

# 行为权重
BEHAVIOR_WEIGHTS = {
    "eyes_closed": 0.8, 
    "yarning": 0.65,  # 降低打哈欠的权重，减少0.05
    "eyes_closed_head_left": 0.6, 
    "eyes_closed_head_right": 0.6,
    "head_down": 0.5, 
    "seeing_left": 0.4, 
    "seeing_right": 0.4,
    "head_up": 0.3, 
    "focused": 0.0
}

# 系统配置
CONFIG = {
    "duration_threshold": 1.5,
    "fatigue_duration_threshold": 2.0,
    "min_detections_for_duration": 2,
    "count_threshold": 3,
    "window_size": 30.0,
    "score_threshold": 0.8,
    "min_confidence": 0.8,
    "fatigue_min_confidence": 0.85,
    "multi_event_cooldown": 10.0,
    "level3_cooldown": 5.0,
    "level_reset_threshold": 10.0,
    "safe_driving_confirm_time": 3.0,
    "fps_target": 4.5,
    "distracted_persist_time": 5.0,
    "continuous_distracted_window": 90.0,
    "continuous_distracted_count": 7,
    "iou_threshold": 0.5,
    "progress_increment": 3.0,  # 降低增量，减缓等级上升速度（原值5.0）
    "progress_decrement_focused": 5.0,
    "progress_decrement_normal": 0.5,
    "event_merge_window": 5.0,
    "focused_confidence_boost": 0.15,  # 提高专注驾驶的置信度提升（从0.12提高到0.18）
    "focused_min_confidence": 0.72,    # 专注驾驶的最低置信度阈值（从0.75降低到0.70，更容易识别）
    "distraction_confidence_threshold": 0.85  # 分心行为的置信度阈值
}

# GPIO配置（v2版本已经废弃，但需要考虑兼容）
GPIO_CONFIG = {
    "led": 70,       # LED
    "vibrator": 71,  # 振动马达
    "buzzer": 72     # 蜂鸣器
}

# Web服务器配置
WEB_CONFIG = {
    "host": "0.0.0.0",
    "port": 5200,
    "debug": False,
    "use_reloader": False,
    "jpeg_quality": 85,  # JPEG编码质量 (0-100, 85为高质量)
    "video_fps": 15      # 视频流帧率
}

# 日志配置
LOG_CONFIG = {
    "level": "INFO",
    "format": "%(asctime)s - %(levelname)s - %(message)s",
    "file": "app_log.txt",
    "max_bytes": 10 * 1024 * 1024,  # 10MB
    "backup_count": 5,
    "encoding": "utf-8"
}

# 性能监控配置
# 注意：如需禁用性能监控，将 enabled 设置为 False 即可
PERFORMANCE_MONITOR_CONFIG = {
    "enabled": True,          # 是否启用性能监控（可随时注释掉或设置为False）
    "log_dir": "performance_logs",  # 日志文件目录
    "interval": 5.0           # 记录间隔（秒）
}

# 硬件看门狗配置（调试用的，如果不需要请禁用）
# 注意：硬件看门狗用于防止系统重启，建议保持启用
# 如果遇到权限问题（Permission denied），可以：
# 1. 禁用此功能（设置 enabled=False）
# 2. 使用sudo运行程序
# 3. 配置系统服务来管理看门狗（推荐）
HARDWARE_WATCHDOG_CONFIG = {
    "enabled": False,         # 是否启用硬件看门狗喂狗（默认False，因为需要root权限）
    "feed_interval": 10.0     # 喂狗间隔（秒），建议设置为看门狗超时时间的1/3
}

# 网络模块（EC800M）配置
# 注意：可以临时禁用网络模块，避免串口操作导致的问题
NETWORK_MODULE_CONFIG = {
    "enabled": False,         # 是否启用网络模块（默认False，临时禁用）
    "skip_gnss": False,        # 是否跳过GNSS初始化
    "skip_ntp": False,         # 是否跳过NTP时间同步
    "skip_login": False        # 是否跳过设备登录
}