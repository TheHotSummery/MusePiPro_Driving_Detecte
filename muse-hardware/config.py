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
    "seeing_left": "向左看",
    "head_up": "抬头", 
    "seeing_right": "向右看", 
    "yarning": "打哈欠",
    "eyes_closed_head_right": "闭眼向右", 
    "focused": "专注驾驶", 
    "eyes_closed": "闭眼"
}

# 疲劳类别
FATIGUE_CLASSES = ["eyes_closed", "yarning", "eyes_closed_head_left", "eyes_closed_head_right"]

# 行为权重
BEHAVIOR_WEIGHTS = {
    "eyes_closed": 0.8, 
    "yarning": 0.7,
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
    "progress_increment": 5.0,  
    "progress_decrement_focused": 5.0,
    "progress_decrement_normal": 0.5,
    "event_merge_window": 5.0,
    "focused_confidence_boost": 0.1
}

# GPIO配置
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
    "use_reloader": False
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