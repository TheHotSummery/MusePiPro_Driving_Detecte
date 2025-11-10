# -*- coding: utf-8 -*-
"""
工具函数模块
包含图像处理、日志设置、系统监控等通用功能
"""

import cv2
import numpy as np
import logging
from logging import handlers
import psutil
import time
from PIL import Image, ImageDraw, ImageFont
from config import LOG_CONFIG
import platform

def setup_logging():
    """设置日志系统"""
    logging.basicConfig(
        level=getattr(logging, LOG_CONFIG["level"]),
        format=LOG_CONFIG["format"],
        handlers=[
            handlers.RotatingFileHandler(
                LOG_CONFIG["file"], 
                maxBytes=LOG_CONFIG["max_bytes"], 
                backupCount=LOG_CONFIG["backup_count"], 
                encoding=LOG_CONFIG["encoding"]
            ),
            logging.StreamHandler()
        ]
    )

def preprocess_image(img_raw, model_input_size):
    """预处理图像用于模型推理"""
    if img_raw is None or img_raw.size == 0:
        logging.error("preprocess_image: 输入图像为空或无效")
        return None, 0, 0
    
    h_orig, w_orig = img_raw.shape[:2]
    img_rgb = cv2.cvtColor(img_raw, cv2.COLOR_BGR2RGB)
    img_resized = cv2.resize(img_rgb, model_input_size, interpolation=cv2.INTER_AREA)
    img_normalized = img_resized.astype(np.float32) / 255.0
    img_transposed = np.transpose(img_normalized, (2, 0, 1))
    img_final = np.expand_dims(img_transposed, axis=0)
    
    logging.debug(f"预处理图像: shape={img_final.shape}, min={np.min(img_final)}, max={np.max(img_final)}")
    return img_final, h_orig, w_orig

def draw_boxes(frame, detections, model_input_size, label_map, fatigue_classes, display_size=None):
    """在图像上绘制检测框和标签"""
    global font_cache
    img_pil = Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))
    draw = ImageDraw.Draw(img_pil)
    
    # 字体缓存
    if 'font_cache' not in globals():
        font_path = "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc"
        try:
            globals()['font_cache'] = ImageFont.truetype(font_path, 20)
        except:
            globals()['font_cache'] = ImageFont.load_default()
            logging.warning(f"加载字体 {font_path} 失败，使用默认字体")
    
    font_cache = globals()['font_cache']
    
    # 获取显示尺寸
    if display_size is None:
        display_size = (frame.shape[0], frame.shape[1])
    
    for detection in detections:
        x1, y1, x2, y2 = detection["bbox"]
        raw_x1, raw_y1, raw_x2, raw_y2 = detection["raw_bbox"]
        
        # 坐标已经在后处理中正确缩放到显示尺寸，直接使用
        x1, y1, x2, y2 = int(x1), int(y1), int(x2), int(y2)
        
        # 右移20像素
        offset_x = 20
        x1 = max(0, x1 + offset_x)
        x2 = min(display_size[1] - 1, x2 + offset_x)
        y1 = max(0, y1)
        y2 = min(display_size[0] - 1, y2)
        
        label_cn = detection["label_cn"]
        conf = detection["confidence"]
        label_raw = detection["label"]
        
        # 根据行为类型选择颜色
        if label_raw in ["focused"]:
            color = (0, 255, 0)  # 绿色
        elif label_raw in fatigue_classes:
            color = (255, 0, 0)  # 红色
        else:
            color = (255, 165, 0)  # 橙色
        
        # 绘制检测框
        draw.rectangle((x1, y1, x2, y2), outline=color, width=2)
        
        # 绘制标签文本
        text = f"{label_cn} {conf:.2f}"
        text_bbox = draw.textbbox((0, 0), text, font=font_cache)
        text_w, text_h = text_bbox[2] - text_bbox[0], text_bbox[3] - text_bbox[1]
        text_x, text_y = max(0, x1), max(0, y1 - text_h - 2)
        draw.text((text_x, text_y), text, font=font_cache, fill=color)
        
        logging.debug(f"绘制: {label_cn}, 置信度: {conf:.2f}, 原始坐标: [{raw_x1:.1f}, {raw_y1:.1f}, {raw_x2:.1f}, {raw_y2:.1f}], 调整后坐标: [{x1}, {y1}, {x2}, {y2}]")
    
    return cv2.cvtColor(np.array(img_pil), cv2.COLOR_RGB2BGR)

def log_system_metrics(preprocess_time, inference_time, postprocess_time, logic_time, draw_time):
    """记录系统资源使用情况"""
    try:
        cpu_total = psutil.cpu_percent(interval=None)
        cpu_per_core = psutil.cpu_percent(percpu=True)
        mem = psutil.virtual_memory()
        mem_total_mb = mem.total / 1024**2
        mem_used_mb = mem.used / 1024**2
        mem_percent = mem.percent
        
        disk_io = psutil.disk_io_counters()
        disk_read_mb = disk_io.read_bytes / 1024**2 if disk_io else 0
        disk_write_mb = disk_io.write_bytes / 1024**2 if disk_io else 0
        
        net_io = psutil.net_io_counters()
        net_sent_mb = net_io.bytes_sent / 1024**2 if net_io else 0
        net_recv_mb = net_io.bytes_recv / 1024**2 if net_io else 0
        
        process = psutil.Process()
        process_cpu = process.cpu_times()
        process_threads = process.num_threads()
        
        total_time = preprocess_time + inference_time + postprocess_time + logic_time + draw_time
        
        log_message = (
            f"Inference Metrics111: "
            f"CPU={cpu_total:.1f}%, Per-Core={cpu_per_core}, "
            f"Memory={mem_used_mb:.1f}/{mem_total_mb:.1f}MB ({mem_percent:.1f}%), "
            f"DiskIO=Read:{disk_read_mb:.1f}MB,Write:{disk_write_mb:.1f}MB, "
            f"NetIO=Sent:{net_sent_mb:.1f}MB,Received:{net_recv_mb:.1f}MB, "
            f"Times=Pre:{preprocess_time:.2f}ms,Inf:{inference_time:.2f}ms,Post:{postprocess_time:.2f}ms,Logic:{logic_time:.2f}ms,Draw:{draw_time:.2f}ms,Total:{total_time:.2f}ms, "
            f"ProcessCPU=User:{process_cpu.user:.1f}s,System:{process_cpu.system:.1f}s, Threads={process_threads}"
        )
        # logging.info(log_message)
    except Exception as e:
        logging.error(f"记录系统资源日志失败: {e}")

def get_system_info():
    """获取系统基本信息"""
    try:
        return {
            "cpu_count": psutil.cpu_count(),
            "memory_total": psutil.virtual_memory().total,
            "platform": platform.platform(),
            "python_version": platform.python_version()
        }
    except Exception as e:
        logging.error(f"获取系统信息失败: {e}")
        return {}






