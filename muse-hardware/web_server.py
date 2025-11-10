# -*- coding: utf-8 -*-
"""
Web服务模块
管理Flask应用、SocketIO和路由
"""

import time
import logging
import threading
from flask import Flask, Response, render_template, request
from flask_socketio import SocketIO, emit
from flask_cors import CORS
from config import WEB_CONFIG, CONFIG, BEHAVIOR_WEIGHTS
from gpio_controller import GPIOController
from plc_bridge import PLCBridge
from video_processor import VideoProcessor
from network_manager import NetworkManager

class WebServer:
    """Web服务器类"""
    
    def __init__(self):
        self.app = Flask(__name__, 
                        template_folder='templates',
                        static_folder='templates',  # 将静态文件目录设置为templates
                        static_url_path='')  # 静态文件URL路径设为根路径
        
        # 配置CORS，允许所有来源
        CORS(self.app, resources={
            r"/test/*": {"origins": "*"},
            r"/feed/*": {"origins": "*"},
            r"/status": {"origins": "*"},
            r"/config": {"origins": "*"},
            r"/weights": {"origins": "*"},
            r"/clear_events": {"origins": "*"},
            r"/trigger_gpio": {"origins": "*"}
        })
        
        # 优化SocketIO配置，提高启动速度
        self.socketio = SocketIO(
            self.app, 
            async_mode="threading", 
            cors_allowed_origins="*",
            logger=False,  # 禁用SocketIO日志
            engineio_logger=False,  # 禁用Engine.IO日志
            ping_timeout=60,
            ping_interval=25
        )
        
        # 初始化组件（不自动启动 PLC，由外部程序管理）
        self.plc_bridge = PLCBridge(auto_start=False)
        self.gpio_controller = GPIOController(self.plc_bridge)
        self.network_manager = NetworkManager()
        self.video_processor = VideoProcessor(self.gpio_controller, self.socketio, self.network_manager)
        
        # YOLO 心跳线程标志
        self._yolo_heartbeat_running = False
        self._yolo_heartbeat_thread = None
        
        # 设置路由
        self._setup_routes()
        self._setup_socketio_events()
        
        # 启动 YOLO 心跳线程（每10秒发送一次心跳）
        self._start_yolo_heartbeat()
    
    def _setup_routes(self):
        """设置Flask路由"""
        
        @self.app.route("/")
        def index():
            return render_template("index.html")
        
        # 添加静态文件路由，因为现在templates就是静态文件目录
        @self.app.route("/<path:filename>")
        def static_files(filename):
            from flask import send_from_directory
            return send_from_directory('templates', filename)
        
        @self.app.route("/feed/webcam/")
        def feed_webcam():
            self.video_processor.start_processing_thread(source="webcam", device_index=20)
            
            def generate():
                time.sleep(1)
                while True:
                    frame_to_send = self.video_processor.get_latest_frame()
                    if frame_to_send is not None:
                        import cv2
                        ret, buffer = cv2.imencode(".jpg", frame_to_send, [cv2.IMWRITE_JPEG_QUALITY, WEB_CONFIG["jpeg_quality"]])
                        if not ret:
                            logging.error("MJPEG 帧编码失败")
                            continue
                        frame_bytes = buffer.tobytes()
                        yield (b"--frame\r\nContent-Type: image/jpeg\r\n\r\n" + frame_bytes + b"\r\n")
                    else:
                        logging.debug("MJPEG: 无帧可发送")
                    time.sleep(0.01)
            
            return Response(generate(), mimetype="multipart/x-mixed-replace; boundary=frame")
        
        @self.app.route("/feed/image/")
        def feed_image():
            image_path = request.args.get("path", "")
            import os
            if not image_path or not os.path.exists(image_path):
                logging.error(f"图片路径无效: {image_path}")
                return Response(f"图片路径无效: {image_path}", status=400)
            
            self.video_processor.start_processing_thread(source="image", image_path=image_path)
            
            def generate():
                time.sleep(1)
                while True:
                    frame_to_send = self.video_processor.get_latest_frame()
                    if frame_to_send is not None:
                        import cv2
                        ret, buffer = cv2.imencode(".jpg", frame_to_send, [cv2.IMWRITE_JPEG_QUALITY, WEB_CONFIG["jpeg_quality"]])
                        if not ret:
                            continue
                        frame_bytes = buffer.tobytes()
                        yield (b"--frame\r\nContent-Type: image/jpeg\r\n\r\n" + frame_bytes + b"\r\n")
                        if not self.video_processor.is_processing():
                            break
                    time.sleep(0.1)
            
            return Response(generate(), mimetype="multipart/x-mixed-replace; boundary=frame")
        
        # 测试相关API接口
        @self.app.route("/test/fatigue_state", methods=["POST"])
        def set_test_fatigue_state():
            try:
                # 检查模块初始化状态
                if not self._check_modules_initialized():
                    return {"success": False, "message": "模块未完全初始化，无法设置测试状态"}, 503
                
                data = request.get_json()
                score = data.get("score", 0)
                behaviors = data.get("behaviors", [])
                is_test_mode = data.get("is_test_mode", False)
                
                # 设置测试模式状态
                behavior_analyzer = self.video_processor.get_behavior_analyzer()
                behavior_analyzer.set_test_mode(is_test_mode, score, behaviors)
                
                # 发送更新到前端
                self.socketio.emit("detection_update", {
                    "progress": score,
                    "level": behavior_analyzer.get_current_level(),
                    "is_fatigue": score >= 75,
                    "is_distracted": score >= 50 and score < 75,
                    "test_mode": True
                })
                
                logging.info(f"测试疲劳状态已设置: 分数={score}, 行为={behaviors}")
                return {"success": True, "message": "测试疲劳状态设置成功"}
                
            except Exception as e:
                logging.error(f"设置测试疲劳状态失败: {e}")
                return {"success": False, "message": f"设置失败: {str(e)}"}, 500
        
        @self.app.route("/test/reset_fatigue_state", methods=["POST"])
        def reset_test_fatigue_state():
            try:
                behavior_analyzer = self.video_processor.get_behavior_analyzer()
                behavior_analyzer.reset_test_mode()
                
                # 发送重置更新到前端
                self.socketio.emit("detection_update", {
                    "progress": 0,
                    "level": "Normal",
                    "is_fatigue": False,
                    "is_distracted": False,
                    "test_mode": False
                })
                
                logging.info("测试疲劳状态已重置")
                return {"success": True, "message": "测试疲劳状态重置成功"}
                
            except Exception as e:
                logging.error(f"重置测试疲劳状态失败: {e}")
                return {"success": False, "message": f"重置失败: {str(e)}"}, 500
        
        @self.app.route("/test/simulate_fatigue_event", methods=["POST"])
        def simulate_fatigue_event():
            try:
                # 检查模块初始化状态
                if not self._check_modules_initialized():
                    return {"success": False, "message": "模块未完全初始化，无法模拟疲劳事件"}, 503
                
                data = request.get_json()
                behavior = data.get("behavior", "eyes_closed")
                confidence = data.get("confidence", 0.85)
                duration = data.get("duration", 3.0)
                score = data.get("score", 75)
                
                # 更新测试行为
                behavior_analyzer = self.video_processor.get_behavior_analyzer()
                behavior_analyzer.update_test_behavior([behavior])
                
                logging.info(f"疲劳事件模拟完成: 行为={behavior}, 分数={score}")
                return {"success": True, "message": "疲劳事件模拟成功"}
                
            except Exception as e:
                logging.error(f"模拟疲劳事件失败: {e}")
                return {"success": False, "message": f"模拟失败: {str(e)}"}, 500
    
    def _setup_socketio_events(self):
        """设置SocketIO事件处理器"""
        
        @self.socketio.on("connect")
        def handle_connect(auth=None):
            logging.info("前端已连接")
            emit("status", {"message": "已连接，等待检测结果..."})
            emit("config_update", CONFIG)
            emit("weights_update", BEHAVIOR_WEIGHTS)
            emit("events_update", self.video_processor.get_behavior_analyzer().get_events())
            emit("distracted_count_update", {
                "distracted_count": self.video_processor.get_behavior_analyzer().get_distracted_count()
            })
            emit("detection_update", {
                "progress": self.video_processor.get_behavior_analyzer().get_progress_score(),
                "level": self.video_processor.get_behavior_analyzer().get_current_level()
            })
        
        @self.socketio.on("disconnect")
        def handle_disconnect():
            logging.info("前端已断开连接")
        
        @self.socketio.on("update_config")
        def handle_config_update(data):
            try:
                for key in CONFIG:
                    if key in data:
                        if isinstance(CONFIG[key], float):
                            CONFIG[key] = float(data[key])
                        elif isinstance(CONFIG[key], int):
                            CONFIG[key] = int(data[key])
                
                emit("config_update", CONFIG)
                logging.info("配置更新成功")
            except Exception as e:
                logging.error(f"配置更新失败: {e}")
                emit("error", {"message": f"配置更新失败: {str(e)}"})
        
        @self.socketio.on("update_weights")
        def handle_weights_update(data):
            try:
                for key in BEHAVIOR_WEIGHTS:
                    if key in data and 0.0 <= float(data[key]) <= 1.0:
                        BEHAVIOR_WEIGHTS[key] = float(data[key])
                
                emit("weights_update", BEHAVIOR_WEIGHTS)
                logging.info(f"权重更新成功: {BEHAVIOR_WEIGHTS}")
            except Exception as e:
                logging.error(f"权重更新失败: {e}")
                emit("error", {"message": f"权重更新失败: {str(e)}"})
        
        @self.socketio.on("clear_events")
        def handle_clear_events():
            self.video_processor.get_behavior_analyzer().clear_events()
            emit("events_update", self.video_processor.get_behavior_analyzer().get_events())
            emit("distracted_count_update", {
                "distracted_count": self.video_processor.get_behavior_analyzer().get_distracted_count()
            })
            emit("detection_update", {
                "progress": self.video_processor.get_behavior_analyzer().get_progress_score(),
                "level": self.video_processor.get_behavior_analyzer().get_current_level()
            })
            logging.info("事件记录和计数器已清空")
        
        @self.socketio.on("trigger_gpio")
        def handle_trigger_gpio(data):
            try:
                gpio = int(data.get("gpio", -1))
                duration = float(data.get("duration", 1.0))
                threading.Thread(
                    target=self.gpio_controller.trigger_manual, 
                    args=(gpio, duration), 
                    daemon=True
                ).start()
            except Exception as e:
                logging.error(f"手动触发 GPIO 失败: {e}")
                emit("error", {"message": f"手动触发 GPIO 失败: {str(e)}"})
        
        # 网络测试相关事件处理器
        @self.socketio.on("network_test")
        def handle_network_test(data):
            """处理网络测试请求"""
            if self.network_manager.is_network_test_running:
                emit("network_test_result", {
                    "success": False,
                    "message": "网络测试正在进行中，请勿重复点击",
                    "test_type": data.get("test_type", "unknown")
                })
                return
            
            test_type = data.get("test_type", "")
            self.network_manager.is_network_test_running = True
            
            def test_wrapper():
                try:
                    result = self._execute_network_test(test_type)
                    self.socketio.emit("network_test_result", result)
                except Exception as e:
                    logging.error(f"网络测试执行失败: {e}")
                    self.socketio.emit("network_test_result", {
                        "success": False,
                        "message": f"测试执行失败: {str(e)}",
                        "test_type": test_type
                    })
                finally:
                    self.network_manager.is_network_test_running = False
            
            threading.Thread(target=test_wrapper, daemon=True).start()
        
        @self.socketio.on("get_network_status")
        def handle_get_network_status():
            """获取网络状态"""
            try:
                status = self.network_manager.get_current_status()
                emit("network_status", status)
            except Exception as e:
                logging.error(f"获取网络状态失败: {e}")
                emit("error", {"message": f"获取网络状态失败: {str(e)}"})
    
    def _execute_network_test(self, test_type):
        """执行网络测试"""
        result = {
            "success": False,
            "message": "",
            "test_type": test_type,
            "status": self.network_manager.get_current_status()
        }
        
        try:
            if test_type == "ntp_sync":
                success, message = self.network_manager.sync_time_with_ntp()
                result["success"] = success
                result["message"] = message
                
            elif test_type == "get_gps":
                success, message, location = self.network_manager.get_gps_location()
                result["success"] = success
                result["message"] = message
                if location:
                    result["gps_data"] = {
                        "latitude": location['wgs84']['latitude'],
                        "longitude": location['wgs84']['longitude'],
                        "altitude": location.get('altitude_m', 0),
                        "satellites": location.get('satellites_in_use', 0)
                    }
                    
            elif test_type == "get_satellites":
                success, message, sat_info = self.network_manager.get_satellite_info()
                result["success"] = success
                result["message"] = message
                if sat_info:
                    result["satellite_data"] = sat_info
                    
            elif test_type == "device_login":
                success, message = self.network_manager.device_login()
                result["success"] = success
                result["message"] = message
                
            elif test_type == "device_online":
                success, message = self.network_manager.device_online()
                result["success"] = success
                result["message"] = message
                
            elif test_type == "device_offline":
                success, message = self.network_manager.device_offline()
                result["success"] = success
                result["message"] = message
                
            elif test_type == "heartbeat":
                success, message = self.network_manager.send_heartbeat()
                result["success"] = success
                result["message"] = message
                
            elif test_type == "report_event":
                # 获取当前行为检测数据
                behavior_data = self._get_current_behavior_data()
                success, message = self.network_manager.report_event_data(behavior_data)
                result["success"] = success
                result["message"] = message
                result["behavior_data"] = behavior_data
                
            elif test_type == "report_gps":
                # 获取当前疲劳数据
                fatigue_data = self._get_current_fatigue_data()
                success, message = self.network_manager.report_gps_data(fatigue_data)
                result["success"] = success
                result["message"] = message
                result["fatigue_data"] = fatigue_data
                
            else:
                result["message"] = f"未知的测试类型: {test_type}"
                
        except Exception as e:
            result["message"] = f"测试执行异常: {str(e)}"
            logging.error(f"网络测试 {test_type} 执行失败: {e}")
        
        return result
    
    def _check_modules_initialized(self):
        """检查模块是否已完全初始化"""
        try:
            # 检查视频处理器
            if not self.video_processor:
                logging.warning("视频处理器未初始化")
                return False
            
            # 检查行为分析器
            behavior_analyzer = self.video_processor.get_behavior_analyzer()
            if not behavior_analyzer:
                logging.warning("行为分析器未初始化")
                return False
            
            # 检查GPIO控制器
            if not self.gpio_controller:
                logging.warning("GPIO控制器未初始化")
                return False
            
            # 检查网络管理器
            if not self.network_manager:
                logging.warning("网络管理器未初始化")
                return False
            
            # 检查模型管理器（如果视频处理器正在处理）
            if self.video_processor.is_processing():
                model_manager = self.video_processor.get_model_manager()
                if not model_manager or not model_manager.is_loaded():
                    logging.warning("模型管理器未加载")
                    return False
            
            return True
            
        except Exception as e:
            logging.error(f"模块初始化检查失败: {e}")
            return False
    
    def _get_current_behavior_data(self):
        """获取当前行为检测数据"""
        try:
            behavior_analyzer = self.video_processor.get_behavior_analyzer()
            return {
                "behavior": "test_behavior",  # 默认测试行为
                "confidence": 0.85,
                "progress_score": behavior_analyzer.get_progress_score(),
                "current_level": behavior_analyzer.get_current_level(),
                "distracted_count": behavior_analyzer.get_distracted_count()
            }
        except Exception as e:
            logging.error(f"获取行为数据失败: {e}")
            return {
                "behavior": "test_behavior",
                "confidence": 0.85,
                "progress_score": 75.0,
                "current_level": "Level 2",
                "distracted_count": 0
            }
    
    def _get_current_fatigue_data(self):
        """获取当前疲劳数据"""
        try:
            # 这里可以从行为分析器获取更详细的数据
            return {
                "fatigue_score": 0.85,
                "eye_blink_rate": 0.45,
                "head_movement_score": 0.32,
                "yawn_count": 2,
                "attention_score": 0.78
            }
        except Exception as e:
            logging.error(f"获取疲劳数据失败: {e}")
            return {
                "fatigue_score": 0.85,
                "eye_blink_rate": 0.45,
                "head_movement_score": 0.32,
                "yawn_count": 2,
                "attention_score": 0.78
            }
    
    def run(self):
        """启动Web服务器"""
        try:
            self.socketio.run(
                self.app, 
                host=WEB_CONFIG["host"], 
                port=WEB_CONFIG["port"], 
                debug=WEB_CONFIG["debug"],
                use_reloader=WEB_CONFIG.get("use_reloader", False)
                # 注意：Flask-SocketIO 已经默认设置了 threaded=True，不需要重复传递
            )
        except Exception as e:
            logging.error(f"Web服务器启动失败: {e}")
            raise
    
    def _start_yolo_heartbeat(self):
        """启动 YOLO 心跳线程（每10秒写入 M39=ON）
        
        注意：心跳发送有超时保护（2秒），即使Modbus卡死也不会阻塞线程。
        """
        self._yolo_heartbeat_running = True
        
        def heartbeat_loop():
            logging.info("YOLO 心跳线程启动（每10秒发送一次心跳）")
            consecutive_failures = 0
            max_failures = 5  # 连续失败5次后降低日志级别
            
            while self._yolo_heartbeat_running:
                try:
                    if self.plc_bridge and self.plc_bridge.is_available():
                        heartbeat_start = time.time()
                        success = self.plc_bridge.send_yolo_heartbeat()
                        heartbeat_time = time.time() - heartbeat_start
                        
                        if success:
                            if consecutive_failures > 0:
                                logging.info("YOLO 心跳恢复成功（M39=ON）")
                            else:
                                logging.debug("YOLO 心跳发送成功（M39=ON），耗时 %.3f 秒", heartbeat_time)
                            consecutive_failures = 0
                        else:
                            consecutive_failures += 1
                            if consecutive_failures <= max_failures:
                                logging.warning("YOLO 心跳发送失败（连续失败 %d 次）", consecutive_failures)
                            # 超过max_failures后不再记录警告，避免日志刷屏
                    else:
                        logging.debug("Modbus 未连接，跳过 YOLO 心跳")
                        consecutive_failures = 0
                except Exception as e:
                    consecutive_failures += 1
                    logging.error(f"YOLO 心跳发送异常（连续失败 {consecutive_failures} 次）: {e}")
                
                # 等待10秒（分100次检查，每次0.1秒，以便快速响应停止信号）
                for _ in range(100):
                    if not self._yolo_heartbeat_running:
                        break
                    time.sleep(0.1)
            
            logging.info("YOLO 心跳线程结束")
        
        self._yolo_heartbeat_thread = threading.Thread(target=heartbeat_loop, daemon=True)
        self._yolo_heartbeat_thread.start()
    
    def cleanup(self):
        """清理资源"""
        try:
            # 停止 YOLO 心跳线程
            self._yolo_heartbeat_running = False
            if self._yolo_heartbeat_thread and self._yolo_heartbeat_thread.is_alive():
                self._yolo_heartbeat_thread.join(timeout=1.0)
            
            self.video_processor.stop_processing()
            self.gpio_controller.cleanup()
            self.network_manager.cleanup()
            if self.plc_bridge:
                self.plc_bridge.stop()
            logging.info("Web服务器资源清理完成")
        except Exception as e:
            logging.error(f"Web服务器资源清理失败: {e}")

# 全局变量，用于在路由中访问
web_server = None

def create_web_server():
    """创建Web服务器实例"""
    global web_server
    web_server = WebServer()
    return web_server

def get_web_server():
    """获取Web服务器实例"""
    return web_server
