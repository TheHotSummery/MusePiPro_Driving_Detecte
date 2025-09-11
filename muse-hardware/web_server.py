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
from config import WEB_CONFIG, CONFIG, BEHAVIOR_WEIGHTS
from gpio_controller import GPIOController
from video_processor import VideoProcessor
from network_manager import NetworkManager

class WebServer:
    """Web服务器类"""
    
    def __init__(self):
        self.app = Flask(__name__, 
                        template_folder='templates',
                        static_folder='templates',  # 将静态文件目录设置为templates
                        static_url_path='')  # 静态文件URL路径设为根路径
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
        
        # 初始化组件
        self.gpio_controller = GPIOController()
        self.video_processor = VideoProcessor(self.gpio_controller, self.socketio)
        self.network_manager = NetworkManager()
        
        # 设置路由
        self._setup_routes()
        self._setup_socketio_events()
    
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
                        ret, buffer = cv2.imencode(".jpg", frame_to_send, [cv2.IMWRITE_JPEG_QUALITY, 40])
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
                        ret, buffer = cv2.imencode(".jpg", frame_to_send, [cv2.IMWRITE_JPEG_QUALITY, 40])
                        if not ret:
                            continue
                        frame_bytes = buffer.tobytes()
                        yield (b"--frame\r\nContent-Type: image/jpeg\r\n\r\n" + frame_bytes + b"\r\n")
                        if not self.video_processor.is_processing():
                            break
                    time.sleep(0.1)
            
            return Response(generate(), mimetype="multipart/x-mixed-replace; boundary=frame")
    
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
    
    def cleanup(self):
        """清理资源"""
        try:
            self.video_processor.stop_processing()
            self.gpio_controller.cleanup()
            self.network_manager.cleanup()
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
