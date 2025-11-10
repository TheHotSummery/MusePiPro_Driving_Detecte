# -*- coding: utf-8 -*-
"""
网络管理模块
整合串口通信、API调用、离线存储等功能
"""

import json
import time
import logging
import threading
import hashlib
import hmac
from datetime import datetime, timezone, timedelta
from collections import deque
from Crypto.Cipher import AES
from Crypto.Random import get_random_bytes
import os
import base64
import schedule

from QuectelEC800M_final import QuectelEC800M, QuectelError, GNSSFixError

class NetworkManager:
    """网络管理器类"""
    
    def __init__(self, config_path="network_config.json"):
        self.config = self._load_config(config_path)
        self.module = None
        self.token = None
        self.token_expire_time = 0
        self.is_initialized = False
        self.is_network_test_running = False  # 防重复点击标志
        self.offline_mode = False  # 离线模式标志
        self.request_in_progress = False  # 请求进行中标志
        
        # 状态管理
        self.last_gps_location = None
        self.last_real_gps_location = None  # 存储最后一次成功获取的真实坐标
        self.gps_failure_count = 0  # GPS获取失败计数
        self.max_gps_failures = 3  # 最大连续失败次数，超过后返回空坐标
        self.last_heartbeat_time = 0
        self.last_event_time = 0
        self.last_data_report_time = 0
        
        # 离线存储
        self.offline_queue = deque()
        self.offline_storage_file = "offline_data.json"
        self.encryption_key = self._get_or_create_encryption_key()
        
        # 定时任务
        self.heartbeat_timer = None
        self.gps_timer = None
        self.retry_timer = None
        self.scheduler_thread = None
        self.scheduler_running = False
        
        # 加载离线数据
        self._load_offline_data()
        
        # 启动定时任务
        self._start_scheduled_tasks()
        
        logging.info("NetworkManager 初始化完成")
    
    def set_offline_mode(self, enabled=True):
        """设置离线模式"""
        self.offline_mode = enabled
        if enabled:
            logging.info("网络管理器已设置为离线模式，所有数据将仅本地缓存")
        else:
            logging.info("网络管理器已退出离线模式")
    
    def is_offline_mode(self):
        """检查是否处于离线模式"""
        return self.offline_mode
    
    def _load_config(self, config_path):
        """加载配置文件"""
        default_config = {
            "server": {
                "base_url": "http://spacemit.topcoder.fun",
                "timeout": 30
            },
            "device": {
                "device_id": "MUSE_PI_PRO_001",
                "device_type": "Muse Pi Pro Plus",
                "version": "1.0.0",
                "username": "testuser",
                "password": "testpassword"
            },
            "serial": {
                "port": "/dev/ttyUSB0",
                "baudrate": 115200,
                "apn": "UNINET"
            },
            "timing": {
                "heartbeat_interval": 240,  # 4分钟
                "gps_interval": 20,         # 20秒
                "event_cooldown": 5,        # 事件间隔5秒
                "retry_interval": 30        # 重试间隔30秒
            },
            "default_data": {
                "latitude": 33.553733,
                "longitude": 119.030953,
                "fatigue_score": 0.85,
                "eye_blink_rate": 0.45,
                "head_movement_score": 0.32,
                "yawn_count": 2,
                "attention_score": 0.78
            }
        }
        
        try:
            if os.path.exists(config_path):
                with open(config_path, "r", encoding="utf-8") as f:
                    config = json.load(f)
                # 合并默认配置
                for key, value in default_config.items():
                    if key not in config:
                        config[key] = value
                    elif isinstance(value, dict):
                        for sub_key, sub_value in value.items():
                            if sub_key not in config[key]:
                                config[key][sub_key] = sub_value
                return config
            else:
                # 创建默认配置文件
                with open(config_path, "w", encoding="utf-8") as f:
                    json.dump(default_config, f, indent=2, ensure_ascii=False)
                return default_config
        except Exception as e:
            logging.error(f"加载配置文件失败: {e}")
            return default_config
    
    def _get_or_create_encryption_key(self):
        """获取或创建加密密钥"""
        key_file = "network_key.bin"
        try:
            if os.path.exists(key_file):
                with open(key_file, "rb") as f:
                    return f.read()
            else:
                key = get_random_bytes(32)  # AES-256
                with open(key_file, "wb") as f:
                    f.write(key)
                return key
        except Exception as e:
            logging.error(f"处理加密密钥失败: {e}")
            return get_random_bytes(32)
    
    def _encrypt_data(self, data):
        """加密数据"""
        try:
            cipher = AES.new(self.encryption_key, AES.MODE_EAX)
            ciphertext, tag = cipher.encrypt_and_digest(json.dumps(data).encode())
            return {
                'ciphertext': base64.b64encode(ciphertext).decode(),
                'tag': base64.b64encode(tag).decode(),
                'nonce': base64.b64encode(cipher.nonce).decode()
            }
        except Exception as e:
            logging.error(f"数据加密失败: {e}")
            return None
    
    def _decrypt_data(self, encrypted_data):
        """解密数据"""
        try:
            ciphertext = base64.b64decode(encrypted_data['ciphertext'])
            tag = base64.b64decode(encrypted_data['tag'])
            nonce = base64.b64decode(encrypted_data['nonce'])
            
            cipher = AES.new(self.encryption_key, AES.MODE_EAX, nonce)
            plaintext = cipher.decrypt_and_verify(ciphertext, tag)
            return json.loads(plaintext.decode())
        except Exception as e:
            logging.error(f"数据解密失败: {e}")
            return None
    
    def _save_offline_data(self):
        """保存离线数据到文件"""
        try:
            data_to_save = []
            for item in self.offline_queue:
                data_to_save.append(item)
            
            with open(self.offline_storage_file, "w", encoding="utf-8") as f:
                json.dump(data_to_save, f, indent=2, ensure_ascii=False)
        except Exception as e:
            logging.error(f"保存离线数据失败: {e}")
    
    def _load_offline_data(self):
        """从文件加载离线数据"""
        try:
            if os.path.exists(self.offline_storage_file):
                with open(self.offline_storage_file, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    self.offline_queue.extend(data)
                logging.info(f"加载了 {len(data)} 条离线数据")
        except Exception as e:
            logging.error(f"加载离线数据失败: {e}")
    
    def _add_to_offline_queue(self, data_type, data):
        """添加数据到离线队列"""
        try:
            item = {
                "timestamp": time.time(),
                "type": data_type,
                "data": data,
                "retry_count": 0
            }
            self.offline_queue.append(item)
            self._save_offline_data()
            logging.info(f"数据已添加到离线队列: {data_type}")
        except Exception as e:
            logging.error(f"添加离线数据失败: {e}")
    
    def initialize_module(self):
        """初始化串口模块"""
        try:
            if self.module and self.module.ser.is_open:
                self.module.close()
            
            logging.info("开始初始化串口模块...")
            self.module = QuectelEC800M(
                port=self.config["serial"]["port"],
                baudrate=self.config["serial"]["baudrate"],
                apn=self.config["serial"]["apn"]
            )
            self.module.initialize_module()
            self.is_initialized = True
            logging.info("串口模块初始化成功")
            return True
        except Exception as e:
            logging.error(f"串口模块初始化失败: {e}")
            self.is_initialized = False
            return False
    
    def sync_time_with_ntp(self):
        """NTP时间同步"""
        if not self.is_initialized or not self.module:
            return False, "模块未初始化"
        
        try:
            self.module.sync_time_with_ntp()
            logging.info("NTP时间同步成功")
            return True, f"时间同步成功，校准后时间戳: {self.module.get_accurate_timestamp()}"
        except QuectelError as e:
            logging.error(f"NTP时间同步失败: {e}")
            return False, f"时间同步失败: {e}"
    
    def start_gnss(self):
        """启动GNSS"""
        if not self.is_initialized or not self.module:
            return False, "模块未初始化"
        
        try:
            self.module.gnss_start()
            logging.info("GNSS启动成功")
            return True, "GNSS启动成功"
        except QuectelError as e:
            logging.error(f"GNSS启动失败: {e}")
            return False, f"GNSS启动失败: {e}"
    
    def get_gps_location(self):
        """获取GPS坐标"""
        # 如果处于离线模式，返回默认坐标
        if self.offline_mode:
            lat = self.config["default_data"]["latitude"]
            lng = self.config["default_data"]["longitude"]
            return False, f"离线模式：使用默认GPS坐标: {lat:.6f}, {lng:.6f}", None
        
        if not self.is_initialized or not self.module:
            return False, "模块未初始化", None
        
        try:
            location = self.module.get_gnss_location(retries=1)
            self.last_gps_location = location
            self.last_real_gps_location = location  # 更新最后真实坐标
            self.gps_failure_count = 0  # 重置失败计数
            lat = location['wgs84']['latitude']
            lng = location['wgs84']['longitude']
            logging.info(f"获取GPS坐标成功: {lat:.6f}, {lng:.6f}")
            return True, f"GPS坐标: {lat:.6f}, {lng:.6f}", location
        except GNSSFixError as e:
            self.gps_failure_count += 1
            logging.warning(f"获取GPS坐标失败 (第{self.gps_failure_count}次): {e}")
            
            # 如果是516错误，输出星历信息
            error_str = str(e)
            if "516" in error_str or "CME ERROR: 516" in error_str:
                logging.info("GPS错误516：卫星数量不足，正在获取星历信息...")
                try:
                    satellites_info = self.module.get_current_satellites_info()
                    if satellites_info:
                        logging.info(f"当前卫星信息: 总数={satellites_info.get('total', 0)}")
                        for system, count in satellites_info.get('systems', {}).items():
                            logging.info(f"  {system}: {count}颗卫星")
                    else:
                        logging.info("无法获取卫星信息")
                except Exception as sat_e:
                    logging.warning(f"获取卫星信息失败: {sat_e}")
            else:
                logging.info(f"GPS错误详情: {error_str}")
            
            # 智能回退逻辑
            return self._handle_gps_fallback()
    
    def _handle_gps_fallback(self):
        """处理GPS获取失败的回退逻辑"""
        # 1. 优先使用最后一次成功获取的真实坐标
        if self.last_real_gps_location:
            lat = self.last_real_gps_location['wgs84']['latitude']
            lng = self.last_real_gps_location['wgs84']['longitude']
            
            # 标记为非实时坐标
            fallback_location = self.last_real_gps_location.copy()
            fallback_location['is_realtime'] = False
            fallback_location['fallback_reason'] = f"GPS获取失败，使用历史坐标 (失败次数: {self.gps_failure_count})"
            
            logging.info(f"使用历史真实GPS坐标: {lat:.6f}, {lng:.6f} (非实时)")
            return False, f"使用历史真实GPS坐标: {lat:.6f}, {lng:.6f} (非实时)", fallback_location
        
        # 2. 如果连续失败次数超过阈值，返回空坐标
        if self.gps_failure_count >= self.max_gps_failures:
            logging.warning(f"GPS连续失败{self.gps_failure_count}次，返回空坐标")
            return False, f"GPS连续失败{self.gps_failure_count}次，返回空坐标", None
        
        # 3. 使用默认坐标（仅在启动初期且没有历史坐标时）
        lat = self.config["default_data"]["latitude"]
        lng = self.config["default_data"]["longitude"]
        
        # 创建默认坐标对象
        default_location = {
            'wgs84': {
                'latitude': lat,
                'longitude': lng
            },
            'is_realtime': False,
            'fallback_reason': f"使用默认坐标 (失败次数: {self.gps_failure_count})"
        }
        
        logging.info(f"使用默认GPS坐标: {lat:.6f}, {lng:.6f} (失败次数: {self.gps_failure_count})")
        return False, f"使用默认GPS坐标: {lat:.6f}, {lng:.6f} (失败次数: {self.gps_failure_count})", default_location
    
    def get_satellite_info(self):
        """获取卫星信息"""
        if not self.is_initialized or not self.module:
            return False, "模块未初始化", None
        
        try:
            sat_info = self.module.get_current_satellites_info()
            if sat_info:
                total = sat_info['total']
                systems = sat_info['systems']
                logging.info(f"获取卫星信息成功: {total}颗卫星")
                return True, f"搜索到{total}颗卫星: {systems}", sat_info
            else:
                return False, "未获取到卫星信息", None
        except QuectelError as e:
            logging.error(f"获取卫星信息失败: {e}")
            return False, f"获取卫星信息失败: {e}", None
    
    def device_login(self):
        """设备登录（带超时保护，最多等待30秒）"""
        if not self.is_initialized or not self.module:
            return False, "模块未初始化"
        
        # 检查是否有请求正在进行中
        if self.request_in_progress:
            logging.info("网络请求进行中，跳过登录")
            return False, "网络请求进行中，跳过登录"
        
        # 使用线程超时保护，避免长时间阻塞
        import threading
        result_container = [None]
        exception_container = [None]
        
        def login_operation():
            try:
                # 设置请求进行中标志
                self.request_in_progress = True
                
                url = f"{self.config['server']['base_url']}/api/v1/auth/login"
                payload = {
                    "deviceId": self.config["device"]["device_id"],
                    "deviceType": self.config["device"]["device_type"],
                    "version": self.config["device"]["version"],
                    "username": self.config["device"]["username"],
                    "password": self.config["device"]["password"]
                }
                
                logging.info(f"开始设备登录: {self.config['device']['device_id']}")
                response = self.module.http_request('POST', url, data=json.dumps(payload))
                body = response['body']
                
                logging.info(f"登录响应: {body}")
                
                # 解析响应
                start_idx = body.find('{')
                end_idx = body.rfind('}')
                if start_idx != -1 and end_idx != -1:
                    json_str = body[start_idx:end_idx+1]
                    data = json.loads(json_str)
                    
                    if data.get("code") == 200:
                        self.token = data["data"]["token"]
                        self.token_expire_time = time.time() + 24 * 3600  # 24小时
                        logging.info("✅ 设备登录成功")
                        result_container[0] = (True, "设备登录成功")
                    else:
                        logging.warning(f"❌ 登录失败: {data.get('message', '未知错误')}")
                        result_container[0] = (False, f"登录失败: {data.get('message', '未知错误')}")
                else:
                    logging.warning("❌ 登录响应格式错误")
                    result_container[0] = (False, "响应格式错误")
            except Exception as e:
                logging.error(f"设备登录失败: {e}")
                exception_container[0] = e
                result_container[0] = (False, f"设备登录失败: {e}")
            finally:
                # 确保请求状态标志被重置
                self.request_in_progress = False
        
        # 在单独线程中执行，带超时
        login_thread = threading.Thread(target=login_operation, daemon=True)
        login_thread.start()
        login_thread.join(timeout=30.0)  # 最多等待30秒
        
        if login_thread.is_alive():
            logging.warning("设备登录操作超时（已等待30秒），可能网络或串口卡死")
            self.request_in_progress = False  # 重置标志
            return False, "登录操作超时"
        
        if exception_container[0]:
            logging.error(f"设备登录异常: {exception_container[0]}")
            return False, str(exception_container[0])
        
        return result_container[0] if result_container[0] else (False, "登录失败")
    
    def _check_token_validity(self):
        """检查Token有效性"""
        if not self.token or time.time() > self.token_expire_time:
            logging.info("Token已过期，需要重新登录")
            return False
        return True
    
    def _api_call(self, method, endpoint, data=None, friendly_name="API调用"):
        """通用API调用方法"""
        # 检查是否有请求正在进行中
        if self.request_in_progress:
            if data:
                self._add_to_offline_queue(friendly_name, data)
                logging.info(f"网络请求进行中：{friendly_name}数据已缓存")
            return False, f"{friendly_name}失败: 网络请求进行中，数据已缓存"
        
        # 如果处于离线模式，直接缓存数据
        if self.offline_mode:
            if data:
                self._add_to_offline_queue(friendly_name, data)
                logging.info(f"离线模式：{friendly_name}数据已缓存")
            return False, f"离线模式：{friendly_name}数据已缓存"
        
        if not self.is_initialized or not self.module:
            # 模块未初始化时也缓存数据
            if data:
                self._add_to_offline_queue(friendly_name, data)
                logging.info(f"模块未初始化：{friendly_name}数据已缓存")
            return False, f"{friendly_name}失败: 模块未初始化，数据已缓存"
        
        if not self._check_token_validity():
            success, message = self.device_login()
            if not success:
                # 登录失败时也缓存数据
                if data:
                    self._add_to_offline_queue(friendly_name, data)
                    logging.info(f"登录失败：{friendly_name}数据已缓存")
                return False, f"{friendly_name}失败: {message}，数据已缓存"
        
        try:
            # 设置请求进行中标志
            self.request_in_progress = True
            
            url = f"{self.config['server']['base_url']}{endpoint}?token={self.token}"
            request_data = json.dumps(data) if data is not None else None
            
            logging.info(f"发送HTTP请求: {method} {url}")
            logging.info(f"请求数据: {request_data}")
            
            response = self.module.http_request(method, url, data=request_data)
            body = response['body']
            
            logging.info(f"HTTP响应: {body}")
            
            # 解析响应
            start_idx = body.find('{')
            end_idx = body.rfind('}')
            if start_idx != -1 and end_idx != -1:
                json_str = body[start_idx:end_idx+1]
                parsed_json = json.loads(json_str)
                
                if parsed_json.get("code") == 200:
                    logging.info(f" {friendly_name}成功")
                    return True, f"{friendly_name}成功"
                elif parsed_json.get("code") == 401:
                    # Token无效，重新登录
                    self.token = None
                    logging.warning(" Token无效，需要重新登录")
                    return False, "Token无效，需要重新登录"
                else:
                    logging.warning(f" {friendly_name}失败: {parsed_json.get('message', '未知错误')}")
                    return False, f"{friendly_name}失败: {parsed_json.get('message', '未知错误')}"
            else:
                logging.warning(f" {friendly_name}失败: 响应格式错误")
                return False, f"{friendly_name}失败: 响应格式错误"
                
        except Exception as e:
            logging.error(f"{friendly_name}失败: {e}")
            # 添加到离线队列
            if data:
                self._add_to_offline_queue(friendly_name, data)
            return False, f"{friendly_name}失败: {e}"
        finally:
            # 确保请求状态标志被重置
            self.request_in_progress = False
    
    def device_online(self):
        """设备上线"""
        return self._api_call('GET', '/api/v1/device/online', None, "设备上线")
    
    def device_offline(self):
        """设备离线"""
        return self._api_call('GET', '/api/v1/device/offline', None, "设备离线")
    
    def send_heartbeat(self):
        """发送心跳（带超时保护，最多等待20秒）"""
        payload = {
            "timestamp": datetime.now(timezone.utc).isoformat(timespec='seconds') + 'Z'
        }
        # 使用线程超时保护，避免长时间阻塞
        import threading
        result_container = [None]
        exception_container = [None]
        
        def heartbeat_operation():
            try:
                result_container[0] = self._api_call('POST', '/api/v1/device/heartbeat', payload, "发送心跳")
            except Exception as e:
                exception_container[0] = e
                result_container[0] = (False, f"发送心跳失败: {e}")
        
        # 在单独线程中执行，带超时
        heartbeat_thread = threading.Thread(target=heartbeat_operation, daemon=True)
        heartbeat_thread.start()
        heartbeat_thread.join(timeout=20.0)  # 最多等待20秒
        
        if heartbeat_thread.is_alive():
            logging.warning("发送心跳操作超时（已等待20秒），可能网络或串口卡死")
            return False, "心跳发送超时"
        
        if exception_container[0]:
            logging.error(f"发送心跳异常: {exception_container[0]}")
            return False, str(exception_container[0])
        
        return result_container[0] if result_container[0] else (False, "心跳发送失败")
    
    def report_event_data(self, behavior_data=None):
        """上报事件数据"""
        if not behavior_data:
            behavior_data = {
                "behavior": "test_behavior",
                "confidence": 0.85,
                "progress_score": 75.0
            }
        
        # 获取GPS坐标
        gps_success, gps_msg, location = self.get_gps_location()
        if location:
            lat = location['wgs84']['latitude']
            lng = location['wgs84']['longitude']
            # 检查是否为实时坐标
            is_realtime = location.get('is_realtime', True)
            fallback_reason = location.get('fallback_reason', '')
        else:
            # 返回空坐标，让后端进行判断
            lat = None
            lng = None
            is_realtime = False
            fallback_reason = "GPS获取失败，返回空坐标"
        
        # 确定严重程度（测试阶段：低于40也当作LOW）
        progress_score = behavior_data.get("progress_score", 0)
        if progress_score >= 85:
            severity = "CRITICAL"
        elif progress_score >= 70:
            severity = "HIGH"
        elif progress_score >= 60:
            severity = "MEDIUM"
        elif progress_score >= 40:
            severity = "LOW"
        elif progress_score >= 10:  # 测试阶段：10-40也当作LOW
            severity = "LOW"
        else:
            return False, "进度分数过低，不触发事件"
        
        # 确定事件类型
        behavior = behavior_data.get("behavior", "unknown")
        if behavior in ["eyes_closed", "yarning", "eyes_closed_head_left", "eyes_closed_head_right"]:
            event_type = "FATIGUE"
        elif behavior in ["head_down", "seeing_left", "seeing_right"]:
            event_type = "DISTRACTION"
        else:
            event_type = "EMERGENCY"
        
        # 确定告警级别
        current_level = behavior_data.get("current_level", "Normal")
        alert_level = current_level
        
        # 生成事件ID
        current_time = datetime.now()
        if self.module and hasattr(self.module, 'get_accurate_timestamp'):
            timestamp = self.module.get_accurate_timestamp()
        else:
            timestamp = int(time.time())
        
        event_id = f"{self.config['device']['device_id']}_{timestamp}_{behavior}"
        
        # 构建GPIO触发信息
        gpio_triggered = {
            "level": current_level,
            "gpio_pins": [],
            "trigger_time": current_time.isoformat()
        }
        
        # 根据等级添加GPIO信息
        if current_level == "Level 1":
            gpio_triggered["gpio_pins"] = ["GPIO71"]
            gpio_triggered["action"] = "振动马达间歇性震动"
        elif current_level == "Level 2":
            gpio_triggered["gpio_pins"] = ["GPIO71", "GPIO70"]
            gpio_triggered["action"] = "振动马达间歇性震动+LED闪烁"
        elif current_level == "Level 3":
            gpio_triggered["gpio_pins"] = ["GPIO71", "GPIO70", "GPIO72"]
            gpio_triggered["action"] = "振动马达间歇性震动+LED闪烁+蜂鸣器持续响"
        
        # 构建上下文信息
        context = {
            "device_info": {
                "device_id": self.config["device"]["device_id"],
                "device_type": self.config["device"]["device_type"],
                "version": self.config["device"]["version"]
            },
            "detection_info": {
                "distracted_count": behavior_data.get("distracted_count", 0),
                "progress_score": progress_score,
                "confidence": behavior_data.get("confidence", 0.85)
            },
            "system_info": {
                "timestamp": behavior_data.get("timestamp", time.time()),
                "gps_available": gps_success,
                "gps_realtime": is_realtime,
                "gps_fallback_reason": fallback_reason,
                "gps_failure_count": self.gps_failure_count,
                "offline_mode": self.offline_mode
            }
        }
        
        payload = {
            "eventId": event_id,
            "timestamp": current_time.isoformat(),
            "eventType": event_type,
            "severity": severity,
            "locationLat": lat,
            "locationLng": lng,
            "behavior": behavior,
            "confidence": behavior_data.get("confidence", 0.85),
            "duration": behavior_data.get("duration", 0.0),  # 持续时间
            "alertLevel": alert_level,  # 告警级别
            "gpioTriggered": json.dumps(gpio_triggered),  # GPIO触发信息(JSON字符串)
            "context": json.dumps(context)  # 上下文信息(JSON字符串)
        }
        
        return self._api_call('POST', '/api/v1/data/event', payload, "上报事件数据")
    
    def report_gps_data(self, fatigue_data=None):
        """上报GPS数据"""
        if not fatigue_data:
            fatigue_data = self.config["default_data"]
        
        # 获取GPS坐标
        gps_success, gps_msg, location = self.get_gps_location()
        if location:
            # 构建原始GPS数据字符串
            raw_gps_data = self._construct_raw_gps_string(location)
        else:
            # GPS获取失败，返回空字符串，让后端判断
            raw_gps_data = ""
        
        payload = {
            "raw_gps_data": raw_gps_data,
            "fatigue_score": fatigue_data.get("fatigue_score", 0.85),
            "eye_blink_rate": fatigue_data.get("eye_blink_rate", 0.45),
            "head_movement_score": fatigue_data.get("head_movement_score", 0.32),
            "yawn_count": fatigue_data.get("yawn_count", 2),
            "attention_score": fatigue_data.get("attention_score", 0.78)
        }
        
        return self._api_call('POST', '/api/v1/data/gps', payload, "上报GPS数据")
    
    def _construct_raw_gps_string(self, location_data):
        """构建原始GPS数据字符串"""
        try:
            wgs_lat = location_data['wgs84']['latitude']
            wgs_lng = location_data['wgs84']['longitude']
            
            # 转换为ddmm.mmmm格式
            lat_dir = 'N' if wgs_lat >= 0 else 'S'
            abs_lat = abs(wgs_lat)
            lat_deg = int(abs_lat)
            lat_min = (abs_lat - lat_deg) * 60
            lat_str = f"{lat_deg:02d}{lat_min:07.4f}{lat_dir}"
            
            lng_dir = 'E' if wgs_lng >= 0 else 'W'
            abs_lng = abs(wgs_lng)
            lng_deg = int(abs_lng)
            lng_min = (abs_lng - lng_deg) * 60
            lng_str = f"{lng_deg:03d}{lng_min:07.4f}{lng_dir}"
            
            # 其他字段
            utc_time = location_data.get('utc_time', datetime.now(timezone.utc).strftime('%H%M%S.00'))
            hdop = location_data.get('hdop', 1.0)
            altitude = location_data.get('altitude_m', 50.0)
            fix_mode = location_data.get('fix_mode', 3)
            speed_kmh = location_data.get('speed_kmh', 0.0)
            speed_knots = speed_kmh / 1.852
            utc_date = location_data.get('utc_date', datetime.now(timezone.utc).strftime('%d%m%y'))
            satellites = location_data.get('satellites_in_use', 8)
            
            return (
                f"{utc_time},"
                f"{lat_str},"
                f"{lng_str},"
                f"{hdop:.2f},"
                f"{altitude:.1f},"
                f"{fix_mode},"
                ","
                f"{speed_kmh:.3f},"
                f"{speed_knots:.3f},"
                f"{utc_date},"
                f"{satellites:02d}"
            )
        except Exception as e:
            logging.error(f"构建GPS字符串失败: {e}")
            return self._construct_default_gps_string()
    
    def _construct_default_gps_string(self):
        """构建默认GPS字符串"""
        lat = self.config["default_data"]["latitude"]
        lng = self.config["default_data"]["longitude"]
        
        # 转换为ddmm.mmmm格式
        lat_dir = 'N' if lat >= 0 else 'S'
        abs_lat = abs(lat)
        lat_deg = int(abs_lat)
        lat_min = (abs_lat - lat_deg) * 60
        lat_str = f"{lat_deg:02d}{lat_min:07.4f}{lat_dir}"
        
        lng_dir = 'E' if lng >= 0 else 'W'
        abs_lng = abs(lng)
        lng_deg = int(abs_lng)
        lng_min = (abs_lng - lng_deg) * 60
        lng_str = f"{lng_deg:03d}{lng_min:07.4f}{lng_dir}"
        
        utc_time = datetime.now(timezone.utc).strftime('%H%M%S.00')
        utc_date = datetime.now(timezone.utc).strftime('%d%m%y')
        
        return (
            f"{utc_time},"
            f"{lat_str},"
            f"{lng_str},"
            f"1.23,"
            f"32.4,"
            f"3,"
            f","
            f"0.179,"
            f"0.098,"
            f"{utc_date},"
            f"11"
        )
    
    def get_current_status(self):
        """获取当前状态信息"""
        status = {
            "module_initialized": self.is_initialized,
            "token_valid": self._check_token_validity(),
            "gnss_enabled": self.module.gnss_is_on if self.module else False,
            "last_gps_location": self.last_gps_location,
            "offline_queue_size": len(self.offline_queue),
            "current_time": datetime.now().isoformat(),
            "offline_mode": self.offline_mode
        }
        
        if self.module and hasattr(self.module, 'get_accurate_timestamp'):
            status["accurate_timestamp"] = self.module.get_accurate_timestamp()
        
        return status
    
    def _start_scheduled_tasks(self):
        """启动定时任务"""
        try:
            # 设置定时任务
            schedule.every(self.config["timing"]["heartbeat_interval"]).seconds.do(self._scheduled_heartbeat)
            schedule.every(self.config["timing"]["gps_interval"]).seconds.do(self._scheduled_gps_update)
            schedule.every(self.config["timing"]["retry_interval"]).seconds.do(self._scheduled_retry_offline)
            
            # 启动调度器线程
            self.scheduler_running = True
            self.scheduler_thread = threading.Thread(target=self._run_scheduler, daemon=True)
            self.scheduler_thread.start()
            
            logging.info("定时任务已启动")
        except Exception as e:
            logging.error(f"启动定时任务失败: {e}")
    
    def _run_scheduler(self):
        """运行调度器"""
        while self.scheduler_running:
            try:
                schedule.run_pending()
                time.sleep(1)
            except Exception as e:
                logging.error(f"调度器运行异常: {e}")
                time.sleep(5)
    
    def _scheduled_heartbeat(self):
        """定时心跳任务"""
        try:
            if self.offline_mode:
                logging.debug("跳过定时心跳：离线模式")
                return
            
            if self.is_initialized and self._check_token_validity():
                logging.info("开始定时心跳发送...")
                success, message = self.send_heartbeat()
                if success:
                    logging.info(f"✅ 定时心跳发送成功: {message}")
                else:
                    logging.warning(f"❌ 定时心跳发送失败: {message}")
            else:
                logging.debug("跳过定时心跳：模块未初始化或Token无效")
        except Exception as e:
            logging.error(f"定时心跳任务异常: {e}")
    
    def _scheduled_gps_update(self):
        """定时GPS更新任务"""
        try:
            if self.offline_mode:
                logging.debug("跳过定时GPS更新：离线模式")
                return
            
            if self.is_initialized:
                success, message, location = self.get_gps_location()
                if success:
                    logging.info("定时GPS更新成功")
                    
                    # 获取GPS坐标成功后，上报GPS数据
                    if self._check_token_validity():
                        logging.info("开始定时GPS数据上报...")
                        fatigue_data = {
                            "fatigue_score": 0.85,  # 默认值
                            "eye_blink_rate": 0.45,
                            "head_movement_score": 0.32,
                            "yawn_count": 2,
                            "attention_score": 0.78
                        }
                        report_success, report_message = self.report_gps_data(fatigue_data)
                        if report_success:
                            logging.info(f"✅ 定时GPS数据上报成功: {report_message}")
                        else:
                            logging.warning(f"❌ 定时GPS数据上报失败: {report_message}")
                    else:
                        logging.debug("跳过GPS数据上报：Token无效")
                else:
                    logging.debug(f"定时GPS更新失败: {message}")
            else:
                logging.debug("跳过定时GPS更新：模块未初始化")
        except Exception as e:
            logging.error(f"定时GPS更新任务异常: {e}")
    
    def _scheduled_retry_offline(self):
        """定时重发离线数据任务"""
        try:
            if self.offline_mode:
                logging.debug("跳过定时重发离线数据：离线模式")
                return
            
            if self.is_initialized and self._check_token_validity() and self.offline_queue:
                self._retry_offline_data()
        except Exception as e:
            logging.error(f"定时重发离线数据任务异常: {e}")
    
    def _retry_offline_data(self):
        """重发离线数据"""
        if not self.offline_queue:
            return
        
        retry_count = 0
        max_retries = 3
        
        while self.offline_queue and retry_count < max_retries:
            try:
                item = self.offline_queue[0]
                
                # 根据数据类型重发
                if item["type"] == "发送心跳":
                    success, message = self.send_heartbeat()
                elif item["type"] == "上报事件数据":
                    success, message = self.report_event_data(item["data"])
                elif item["type"] == "上报GPS数据":
                    success, message = self.report_gps_data(item["data"])
                else:
                    # 其他类型暂时跳过
                    self.offline_queue.popleft()
                    continue
                
                if success:
                    # 发送成功，移除该项
                    self.offline_queue.popleft()
                    logging.info(f"离线数据重发成功: {item['type']}")
                else:
                    # 发送失败，增加重试次数
                    item["retry_count"] += 1
                    if item["retry_count"] >= max_retries:
                        # 超过最大重试次数，移除该项
                        self.offline_queue.popleft()
                        logging.warning(f"离线数据重发失败，已移除: {item['type']}")
                    else:
                        logging.warning(f"离线数据重发失败，将重试: {item['type']}")
                    break
                
                retry_count += 1
                
            except Exception as e:
                logging.error(f"重发离线数据异常: {e}")
                break
        
        # 保存更新后的离线队列
        self._save_offline_data()
    
    def trigger_event_report(self, behavior_data):
        """触发事件上报（由行为检测调用）"""
        try:
            current_time = time.time()
            logging.info(f"触发事件上报: 数据={behavior_data}")
            
            # 检查是否有请求正在进行中
            if self.request_in_progress:
                logging.info("网络请求进行中，跳过事件上报")
                return False
            
            # 检查事件间隔
            if current_time - self.last_event_time < self.config["timing"]["event_cooldown"]:
                logging.info(f"事件上报间隔太短，跳过 (间隔: {current_time - self.last_event_time:.1f}s, 要求: {self.config['timing']['event_cooldown']}s)")
                return False
            
            # 检查进度分数（测试阶段：低于40也当作LOW上传）
            progress_score = behavior_data.get("progress_score", 0)
            if progress_score < 10:  # 降低阈值，测试阶段更宽松
                logging.info(f"进度分数过低，不触发事件上报 (分数: {progress_score})")
                return False
            
            logging.info(f"开始上报事件数据: {behavior_data}")
            success, message = self.report_event_data(behavior_data)
            if success:
                self.last_event_time = current_time
                logging.info(f"✅ 事件上报成功: {message}")
                return True
            else:
                logging.warning(f"❌ 事件上报失败: {message}")
                return False
                
        except Exception as e:
            logging.error(f"触发事件上报异常: {e}")
            import traceback
            logging.error(f"错误堆栈: {traceback.format_exc()}")
            return False
    
    def trigger_data_report(self, fatigue_data):
        """触发数据上报（由行为检测调用）"""
        try:
            current_time = time.time()
            logging.info(f"触发数据上报: 数据={fatigue_data}")
            
            # 检查是否有请求正在进行中
            if self.request_in_progress:
                logging.info("网络请求进行中，跳过数据上报")
                return False
            
            # 检查数据上报间隔（可以比事件上报更频繁）
            if current_time - self.last_data_report_time < 30:  # 30秒间隔
                logging.info(f"数据上报间隔太短，跳过 (间隔: {current_time - self.last_data_report_time:.1f}s, 要求: 30s)")
                return False
            
            logging.info(f"开始上报GPS数据: {fatigue_data}")
            success, message = self.report_gps_data(fatigue_data)
            if success:
                self.last_data_report_time = current_time
                logging.info(f"✅ GPS数据上报成功: {message}")
                return True
            else:
                logging.warning(f"❌ GPS数据上报失败: {message}")
                return False
                
        except Exception as e:
            logging.error(f"触发数据上报异常: {e}")
            import traceback
            logging.error(f"错误堆栈: {traceback.format_exc()}")
            return False
    
    def cleanup(self):
        """清理资源"""
        try:
            # 停止调度器
            self.scheduler_running = False
            if self.scheduler_thread and self.scheduler_thread.is_alive():
                self.scheduler_thread.join(timeout=5)
            
            # 取消定时任务
            schedule.clear()
            
            if self.heartbeat_timer:
                self.heartbeat_timer.cancel()
            if self.gps_timer:
                self.gps_timer.cancel()
            if self.retry_timer:
                self.retry_timer.cancel()
            
            if self.module:
                self.module.close()
            
            logging.info("NetworkManager 资源清理完成")
        except Exception as e:
            logging.error(f"NetworkManager 资源清理失败: {e}")
