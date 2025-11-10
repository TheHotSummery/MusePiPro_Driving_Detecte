# -*- coding: utf-8 -*-
"""
视频处理模块
管理摄像头、图像处理和模型推理流程
"""

import cv2
import time
import threading
import logging
import numpy as np
import psutil
from config import CONFIG, MODEL_INPUT_SIZE, NUM_CLASSES, CAMERA_CONFIG
from utils import preprocess_image, draw_boxes, log_system_metrics
from model_manager import ModelManager
from behavior_analyzer import BehaviorAnalyzer

class VideoProcessor:
    """视频处理器类"""
    
    def __init__(self, gpio_controller, socketio, network_manager=None):
        self.gpio_controller = gpio_controller
        self.socketio = socketio
        self.network_manager = network_manager
        self.model_manager = None  # 延迟初始化
        self.behavior_analyzer = BehaviorAnalyzer(gpio_controller, socketio, network_manager)
        
        # 处理状态
        self.processing_thread_running = False
        self.processing_thread_stop_event = threading.Event()
        self.frame_processing_lock = threading.Lock()
        self.latest_annotated_frame = np.zeros((CAMERA_CONFIG["display_height"], CAMERA_CONFIG["display_width"], 3), dtype=np.uint8)
        
        # 性能统计
        self.frame_count = 0
        self.last_progress_update = time.time()
        self.fps_counter = 0
        self.fps_start_time = time.time()
        self.current_fps = 0.0
        
        # YOLO处理时间统计（线程安全存储，供性能监控模块读取）
        self._timing_stats_lock = threading.Lock()
        self._latest_timing_stats = {
            'preprocess': 0.0,
            'inference': 0.0,
            'postprocess': 0.0,
            'logic': 0.0,
            'draw': 0.0,
            'total': 0.0
        }
    
    def start_processing_thread(self, source="webcam", device_index=20, image_path=None):
        """启动处理线程"""
        if not self.processing_thread_running:
            # 延迟初始化模型管理器
            if self.model_manager is None:
                logging.info("初始化模型管理器...")
                self.model_manager = ModelManager()
                logging.info("模型管理器初始化完成")
            
            self.processing_thread_stop_event.clear()
            thread = threading.Thread(
                target=self._processing_thread_function, 
                args=(source, device_index, image_path)
            )
            thread.daemon = True
            thread.start()
            logging.info(f"{self.model_manager.get_model_type()} 处理线程已启动")
    
    def _processing_thread_function(self, source="webcam", device_index=20, image_path=None):
        """处理线程主函数"""
        if not self.model_manager.is_loaded():
            logging.critical("模型未加载，无法启动处理线程")
            self.socketio.emit("error", {"message": "模型未加载"})
            return
        
        logging.info(f"处理线程启动 (源: {source}, 模型: {self.model_manager.get_model_type()})")
        self.processing_thread_running = True
        
        cap = None
        frame = None
        is_static_image_mode = False
        
        try:
            if source == "image" and image_path:
                frame = cv2.imread(image_path)
                if frame is None:
                    logging.error(f"无法读取图像: {image_path}")
                    self.socketio.emit("error", {"message": f"无法读取图像: {image_path}"})
                    self.processing_thread_running = False
                    return
                is_static_image_mode = True
            else:
                cap = cv2.VideoCapture(device_index, cv2.CAP_V4L2)
                if not cap.isOpened():
                    logging.critical(f"无法打开摄像头 /dev/video{device_index}")
                    self.socketio.emit("error", {"message": f"无法打开摄像头 /dev/video{device_index}"})
                    self.processing_thread_running = False
                    return
                
                cap.set(cv2.CAP_PROP_FRAME_WIDTH, CAMERA_CONFIG["display_width"])
                cap.set(cv2.CAP_PROP_FRAME_HEIGHT, CAMERA_CONFIG["display_height"])
                cap.set(cv2.CAP_PROP_FPS, CONFIG["fps_target"])
                logging.info(f"摄像头 /dev/video{device_index} 初始化成功，设置分辨率 {CAMERA_CONFIG['display_width']}x{CAMERA_CONFIG['display_height']}，FPS {CONFIG['fps_target']}")
            
            prev_frame_time = time.time()
            last_socketio_emit = 0.0
            
            while not self.processing_thread_stop_event.is_set():
                loop_start = time.time()
                
                # 获取帧
                if is_static_image_mode:
                    if self.frame_count == 0:
                        current_frame = frame.copy()
                        self.frame_count += 1
                    else:
                        break
                else:
                    ret, current_frame = cap.read()
                    if not ret or current_frame is None:
                        logging.error("视频流读取失败")
                        self.socketio.emit("error", {"message": "视频流读取失败"})
                        
                        # 连续失败计数，如果失败太多次，尝试重新初始化摄像头
                        if not hasattr(self, '_read_failure_count'):
                            self._read_failure_count = 0
                        self._read_failure_count += 1
                        
                        # 如果连续失败超过10次，尝试重新初始化摄像头
                        if self._read_failure_count >= 10:
                            logging.warning(f"视频流连续失败 {self._read_failure_count} 次，尝试重新初始化摄像头...")
                            try:
                                cap.release()
                                time.sleep(1.0)  # 等待1秒
                                cap = cv2.VideoCapture(device_index, cv2.CAP_V4L2)
                                if cap.isOpened():
                                    cap.set(cv2.CAP_PROP_FRAME_WIDTH, CAMERA_CONFIG["display_width"])
                                    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, CAMERA_CONFIG["display_height"])
                                    cap.set(cv2.CAP_PROP_FPS, CONFIG["fps_target"])
                                    logging.info("摄像头重新初始化成功")
                                    self._read_failure_count = 0  # 重置计数
                                else:
                                    logging.error("摄像头重新初始化失败")
                            except Exception as e:
                                logging.error(f"重新初始化摄像头时出错: {e}")
                        
                        time.sleep(0.1)
                        continue
                    
                    # 读取成功，重置失败计数
                    if hasattr(self, '_read_failure_count'):
                        if self._read_failure_count > 0:
                            logging.info(f"视频流恢复成功（之前失败 {self._read_failure_count} 次）")
                        self._read_failure_count = 0
                    
                    self.frame_count += 1
                    self.fps_counter += 1
                
                try:
                    # 预处理
                    start_preprocess = time.time()
                    input_tensor, h_orig, w_orig = preprocess_image(current_frame, MODEL_INPUT_SIZE)
                    preprocess_time = (time.time() - start_preprocess) * 1000
                    
                    if input_tensor is None:
                        continue
                    
                    # 模型推理
                    start_inference = time.time()
                    model_output = self.model_manager.predict(input_tensor)
                    inference_time = (time.time() - start_inference) * 1000
                    
                    # 后处理
                    start_postprocess = time.time()
                    if self.model_manager.get_model_type() == "onnx":
                        detections = self.model_manager.postprocess_onnx(
                            model_output, CONFIG["min_confidence"], CONFIG["iou_threshold"], 
                            NUM_CLASSES, h_orig, w_orig
                        )
                    else:
                        detections = self.model_manager.postprocess_pytorch(
                            model_output, CONFIG["min_confidence"], CONFIG["iou_threshold"], 
                            NUM_CLASSES, h_orig, w_orig
                        )
                    postprocess_time = (time.time() - start_postprocess) * 1000
                    
                    # 行为分析
                    start_logic = time.time()
                    current_time = time.time()
                    logging.debug(f"[VideoProcessor] 开始行为分析，检测数量: {len(detections) if detections else 0}")
                    is_fatigue, is_distracted, new_events, distracted_count, progress_score, level = \
                        self.behavior_analyzer.check_fatigue_or_distracted(detections, current_time)
                    logic_time = (time.time() - start_logic) * 1000
                    logging.debug(f"[VideoProcessor] 行为分析完成，结果: 疲劳={is_fatigue}, 分心={is_distracted}, 等级={level}")
                    
                    # 绘制检测框
                    start_draw = time.time()
                    display_size = (CAMERA_CONFIG["display_height"], CAMERA_CONFIG["display_width"])
                    annotated_frame = draw_boxes(
                        current_frame.copy(), detections, MODEL_INPUT_SIZE, 
                        self.behavior_analyzer.get_label_map(), 
                        self.behavior_analyzer.get_fatigue_classes(),
                        display_size
                    )
                    draw_time = (time.time() - start_draw) * 1000
                    
                    # 计算总时间
                    total_time = preprocess_time + inference_time + postprocess_time + logic_time + draw_time
                    
                    # 更新最新的处理时间统计（线程安全）
                    with self._timing_stats_lock:
                        self._latest_timing_stats = {
                            'preprocess': preprocess_time,
                            'inference': inference_time,
                            'postprocess': postprocess_time,
                            'logic': logic_time,
                            'draw': draw_time,
                            'total': total_time
                        }
                    
                    # 记录系统指标
                    log_system_metrics(preprocess_time, inference_time, postprocess_time, logic_time, draw_time)
                    
                    # 更新帧
                    with self.frame_processing_lock:
                        self.latest_annotated_frame = annotated_frame.copy()
                    
                    # 发送SocketIO更新（降低更新频率，减少延迟）
                    if current_time - last_socketio_emit >= 0.2:  # 从0.25秒降低到0.2秒，提高响应速度
                        self._emit_detection_update(
                            detections, is_fatigue, is_distracted, distracted_count, 
                            progress_score, level
                        )
                        last_socketio_emit = current_time
                    
                    # 更新进度和FPS（优化延迟：减少不必要的sleep）
                    if current_time - self.last_progress_update >= 1.0:
                        self.last_progress_update = current_time
                        # 计算FPS
                        if self.fps_counter > 0:
                            self.current_fps = self.fps_counter / (current_time - self.fps_start_time)
                            self.fps_counter = 0
                            self.fps_start_time = current_time
                    
                    # 控制帧率（优化延迟：只在需要时才sleep）
                    time_spent = time.time() - loop_start
                    target_frame_time = 1.0 / CONFIG["fps_target"]
                    if time_spent < target_frame_time:
                        time_to_sleep = target_frame_time - time_spent
                        if time_to_sleep > 0.01:  # 只sleep超过10ms的情况，减少微小延迟
                            time.sleep(time_to_sleep)
                    # 如果处理时间超过目标时间，不sleep，直接处理下一帧（避免累积延迟）
                    
                    prev_frame_time = loop_start + (1.0 / CONFIG["fps_target"])
                    
                    # 定期内存监控
                    if self.frame_count % 30 == 0:
                        mem = psutil.virtual_memory()
                        logging.info(f"内存使用: {mem.percent}% ({mem.used / 1024**2:.1f} MB / {mem.total / 1024**2:.1f} MB)")
                
                except Exception as e:
                    import traceback
                    error_traceback = traceback.format_exc()
                    logging.error(f"[VideoProcessor] 帧处理错误: {e}")
                    logging.error(f"[VideoProcessor] 错误堆栈: {error_traceback}")
                    self.socketio.emit("error", {"message": f"帧处理错误: {str(e)}"})
                    time.sleep(0.5)
        
        except Exception as e:
            logging.critical(f"处理线程错误: {e}")
            self.socketio.emit("error", {"message": f"处理线程错误: {str(e)}"})
        
        finally:
            if cap is not None:
                cap.release()
                logging.info("摄像头已释放")
            
            self.processing_thread_running = False
            logging.info("处理线程退出")
    
    def _emit_detection_update(self, detections, is_fatigue, is_distracted, distracted_count, progress_score, level):
        """发送检测更新"""
        try:
            start_emit = time.time()
            self.socketio.emit("detection_update", {
                "detections": detections,
                "is_fatigue": is_fatigue,
                "is_distracted": is_distracted,
                "events": self.behavior_analyzer.get_events(),
                "distracted_count": distracted_count,
                "cpu_usage": psutil.cpu_percent(interval=None),
                "progress": progress_score,
                "level": level,
                "fps": self.current_fps
            })
            emit_time = (time.time() - start_emit) * 1000
            logging.debug(f"SocketIO 传输时间: {emit_time:.2f} ms")
        except Exception as e:
            logging.error(f"发送检测更新失败: {e}")
    
    def get_latest_timing_stats(self):
        """获取最新的处理时间统计（线程安全）"""
        with self._timing_stats_lock:
            return self._latest_timing_stats.copy()
    
    def get_latest_frame(self):
        """获取最新处理后的帧"""
        with self.frame_processing_lock:
            return self.latest_annotated_frame.copy()
    
    def is_processing(self):
        """检查是否正在处理"""
        return self.processing_thread_running
    
    def stop_processing(self):
        """停止处理"""
        self.processing_thread_stop_event.set()
        self.processing_thread_running = False
    
    def get_behavior_analyzer(self):
        """获取行为分析器"""
        return self.behavior_analyzer
    
    def get_model_manager(self):
        """获取模型管理器"""
        return self.model_manager
