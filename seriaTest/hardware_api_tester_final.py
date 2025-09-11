# hardware_api_tester_final.py
# 最终基准版本 v3 - 修复所有已知问题

import tkinter as tk
from tkinter import ttk, messagebox, scrolledtext
import json
import time
import threading
from datetime import datetime, timezone, timedelta
import os

try:
    from QuectelEC800M_final import QuectelEC800M, QuectelError, GNSSFixError
except ImportError:
    messagebox.showerror("依赖缺失", "无法导入 QuectelEC800M_final.py。\n请确保文件存在且与此脚本在同一目录。")
    exit()

class HardwareAPITester:
    def __init__(self, root):
        self.root = root
        self.root.title("Muse Pi Pro Plus - 硬件API测试工具 (最终基准版)")
        self.root.geometry("1100x800")

        self.token = None
        self.module = None
        self.is_task_running = False
        self.event_counter = 0

        # --- 【核心修正】调整初始化顺序 ---
        self.load_config()
        # 必须先创建UI和所有tk变量
        self.setup_ui() 
        # 然后才能加载token并更新UI
        self.load_saved_token()

        self.root.protocol("WM_DELETE_WINDOW", self.on_closing)

    def load_config(self):
        try:
            with open("config.json", "r", encoding="utf-8") as f: config = json.load(f)
            self.base_url = config.get("server", {}).get("base_url")
            self.device_config = config.get("device", {})
            self.serial_port = config.get("serial", {}).get("default_port")
            self.apn = config.get("serial", {}).get("default_apn")
            self.simulated_data = config.get("simulated_data", {})
        except Exception as e:
            messagebox.showerror("配置错误", f"加载 config.json 失败: {e}\n将使用默认配置。")
            self.set_default_config()

    def set_default_config(self):
        self.base_url = "http://spacemit.topcoder.fun"; self.device_config = {"default_device_id": "MUSE_PI_PRO_001", "default_device_type": "Muse Pi Pro Plus", "default_version": "1.0.0", "default_username": "testuser", "default_password": "testpassword"}; self.serial_port = "/dev/ttyUSB0"; self.apn = "UNINET"; self.simulated_data = {"latitude": 39.784500, "longitude": 116.576861, "fatigue_score": 0.85, "yawn_count": 2, "attention_score": 0.78, "cpu_usage": 45.2, "memory_usage": 68.7, "temperature": 42.5, "speed": 60.0, "direction": 90.0}

    def setup_ui(self):
        main_frame = ttk.Frame(self.root, padding="10"); main_frame.pack(fill=tk.BOTH, expand=True)
        left_panel = ttk.Frame(main_frame); left_panel.pack(side=tk.LEFT, fill=tk.Y, padx=(0, 10))
        
        module_frame = ttk.LabelFrame(left_panel, text="1. 模块控制", padding="10"); module_frame.pack(fill=tk.X, pady=5)
        self.init_button = ttk.Button(module_frame, text="初始化模块", command=lambda: self.run_task(self.init_serial)); self.init_button.pack(fill=tk.X)
        self.time_sync_button = ttk.Button(module_frame, text="NTP时间同步", command=lambda: self.run_task(self.sync_time), state=tk.DISABLED); self.time_sync_button.pack(fill=tk.X, pady=(5,0))
        
        gnss_frame = ttk.LabelFrame(left_panel, text="2. GNSS 控制 (常开模式)", padding="10"); gnss_frame.pack(fill=tk.X, pady=5)
        self.gnss_start_button = ttk.Button(gnss_frame, text="开启GNSS", command=lambda: self.run_task(self.module.gnss_start), state=tk.DISABLED); self.gnss_start_button.pack(fill=tk.X)
        self.gnss_stop_button = ttk.Button(gnss_frame, text="关闭GNSS", command=lambda: self.run_task(self.module.gnss_stop), state=tk.DISABLED); self.gnss_stop_button.pack(fill=tk.X, pady=(5,0))
        self.get_pos_button = ttk.Button(gnss_frame, text="获取当前坐标", command=lambda: self.run_task(self.get_gnss_info), state=tk.DISABLED); self.get_pos_button.pack(fill=tk.X, pady=(5,0))
        self.get_sat_button = ttk.Button(gnss_frame, text="获取当前星历", command=lambda: self.run_task(self.get_satellite_info)); self.get_sat_button.pack(fill=tk.X, pady=(5,0))
        auth_frame = ttk.LabelFrame(left_panel, text="3. 认证", padding="10"); auth_frame.pack(fill=tk.X, pady=5)
        self.login_button = ttk.Button(auth_frame, text="设备登录 (获取Token)", command=lambda: self.run_task(self.device_login), state=tk.DISABLED); self.login_button.pack(fill=tk.X)
        ttk.Button(auth_frame, text="清除Token", command=self.clear_token).pack(fill=tk.X, pady=(5,0))
        
        right_panel = ttk.Frame(main_frame); right_panel.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        notebook = ttk.Notebook(right_panel); notebook.pack(fill=tk.X, pady=5)
        tab_mgmt = ttk.Frame(notebook, padding="10"); notebook.add(tab_mgmt, text="设备管理")
        tab_data = ttk.Frame(notebook, padding="10"); notebook.add(tab_data, text="数据上传")
        
        self.api_buttons = {}
        self.api_buttons['online'] = ttk.Button(tab_mgmt, text="设备上线", command=lambda: self.run_task(self._api_call, 'GET', '/api/v1/device/online', None, "设备上线")); self.api_buttons['online'].pack(side=tk.LEFT, padx=5)
        self.api_buttons['offline'] = ttk.Button(tab_mgmt, text="设备离线", command=lambda: self.run_task(self._api_call, 'GET', '/api/v1/device/offline', None, "设备离线")); self.api_buttons['offline'].pack(side=tk.LEFT, padx=5)
        self.api_buttons['heartbeat'] = ttk.Button(tab_mgmt, text="发送心跳", command=lambda: self.run_task(self.test_heartbeat)); self.api_buttons['heartbeat'].pack(side=tk.LEFT, padx=5)
        self.api_buttons['realtime'] = ttk.Button(tab_data, text="上报实时数据", command=lambda: self.run_task(self.test_realtime_data)); self.api_buttons['realtime'].pack(side=tk.LEFT, padx=5)
        self.api_buttons['gps'] = ttk.Button(tab_data, text="上报GPS+疲劳数据", command=lambda: self.run_task(self.test_gps_data)); self.api_buttons['gps'].pack(side=tk.LEFT, padx=5)
        self.api_buttons['event'] = ttk.Button(tab_data, text="上报事件数据", command=lambda: self.run_task(self.test_event_data)); self.api_buttons['event'].pack(side=tk.LEFT, padx=5)
        
        sim_frame = ttk.LabelFrame(right_panel, text="可编辑的模拟数据", padding="10"); sim_frame.pack(fill=tk.X, pady=5)
        self.sim_vars = {}
        col = 0; row = 0
        for key, value in self.simulated_data.items():
            ttk.Label(sim_frame, text=f"{key}:").grid(row=row, column=col, sticky=tk.W, padx=5, pady=2)
            var = tk.StringVar(value=str(value)); self.sim_vars[key] = var
            ttk.Entry(sim_frame, textvariable=var).grid(row=row, column=col+1, sticky=tk.EW, padx=5, pady=2)
            sim_frame.columnconfigure(col+1, weight=1); col += 2
            if col >= 4: col = 0; row += 1
        
        token_frame = ttk.Frame(right_panel); token_frame.pack(fill=tk.X, pady=5); ttk.Label(token_frame, text="Token:").pack(side=tk.LEFT); self.token_var = tk.StringVar(value="N/A"); ttk.Entry(token_frame, textvariable=self.token_var, state="readonly").pack(fill=tk.X, expand=True, side=tk.LEFT, padx=5);
        log_frame = ttk.LabelFrame(right_panel, text="日志", padding="10"); log_frame.pack(fill=tk.BOTH, expand=True, pady=5)
        self.log_text = scrolledtext.ScrolledText(log_frame, height=10); self.log_text.pack(fill=tk.BOTH, expand=True)
        self.status_var = tk.StringVar(value="状态: 未连接"); ttk.Label(right_panel, textvariable=self.status_var, relief=tk.SUNKEN, anchor=tk.W).pack(side=tk.BOTTOM, fill=tk.X)
        self.set_module_ready_ui(False)

    def set_module_ready_ui(self, is_ready):
        state = tk.NORMAL if is_ready else tk.DISABLED
        self.time_sync_button.config(state=state); self.gnss_start_button.config(state=state); self.gnss_stop_button.config(state=state)
        self.get_pos_button.config(state=state); self.login_button.config(state=state)
        if self.token and is_ready: self.set_api_buttons_state(tk.NORMAL)
        else: self.set_api_buttons_state(tk.DISABLED)

    def set_api_buttons_state(self, state):
        for btn in self.api_buttons.values(): btn.config(state=state)

    def run_task(self, target_func, *args):
        if self.is_task_running: self.log_message("警告: 任务执行中。", "WARN"); return
        self.is_task_running = True; self.update_status("操作中...")
        def task_wrapper():
            try: target_func(*args)
            finally:
                self.is_task_running = False
                status = "模块未连接"
                if self.module and self.module.ser.is_open:
                    status = "模块已就绪"
                    if self.token: status += " | 已登录"
                self.update_status(status)
        threading.Thread(target=task_wrapper, daemon=True).start()

    def init_serial(self):
        try:
            if self.module and self.module.ser.is_open: self.module.close()
            self.update_status("正在初始化..."); self.log_message("=== 开始串口模块初始化 ===")
            self.module = QuectelEC800M(port=self.serial_port, apn=self.apn)
            self.module.initialize_module()
            self.set_module_ready_ui(True)
            self.log_message("=== 模块初始化完成 ===", "SUCCESS")
        except Exception as e: self.log_message(f"初始化失败: {e}", "ERROR"); self.set_module_ready_ui(False)

    def sync_time(self):
        if not self.module: return
        try: self.module.sync_time_with_ntp(); self.log_message(f"时间同步成功, 校准后时间戳: {self.module.get_accurate_timestamp()}", "SUCCESS")
        except QuectelError as e: self.log_message(f"时间同步失败: {e}", "ERROR")

    def get_gnss_info(self):
        if not self.module: return
        try:
            self.log_message("正在获取GNSS坐标..."); location = self.module.get_gnss_location()
            self.log_message(f"GNSS定位成功: 来源=真实定位", "SUCCESS")
            self.log_message(f"  坐标(WGS84): {location['wgs84']['latitude']:.6f}, {location['wgs84']['longitude']:.6f}")
        except QuectelError as e: self.log_message(f"获取GNSS坐标失败: {e}", "ERROR")
    def _construct_raw_gps_string(self, location_data):
        """
        根据定位数据字典，构建一个符合格式的11字段raw_gps_data字符串。
        """
        # --- 【核心新增】 ---
        # 1. 从定位字典中获取数据，如果失败则使用安全的默认值
        wgs_lat = location_data.get('wgs84', {}).get('latitude', 0.0)
        wgs_lng = location_data.get('wgs84', {}).get('longitude', 0.0)
        
        # 2. 将十进制度转换回 ddmm.mmmm 和 dddmm.mmmm 格式
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

        # 3. 从字典或模拟数据中获取其他字段
        utc_time = location_data.get('utc_time', datetime.now(timezone.utc).strftime('%H%M%S.00'))
        hdop = location_data.get('hdop', 1.0)
        altitude = location_data.get('altitude_m', 50.0)
        fix_mode = location_data.get('fix_mode', 3)
        speed_kmh = location_data.get('speed_kmh', 0.0)
        speed_knots = speed_kmh / 1.852 # 转换为节
        utc_date = location_data.get('utc_date', datetime.now(timezone.utc).strftime('%d%m%y'))
        satellites = location_data.get('satellites_in_use', 8)

        # 4. 拼接成11个字段的字符串
        return (
            f"{utc_time},"
            f"{lat_str},"
            f"{lng_str},"
            f"{hdop:.2f},"
            f"{altitude:.1f},"
            f"{fix_mode},"
            "," # 第7个字段COG(地面航向)，静止时为空
            f"{speed_kmh:.3f},"
            f"{speed_knots:.3f},"
            f"{utc_date},"
            f"{satellites:02d}"
        )
    def _get_location_for_payload(self):
        try:
            if not self.module or not self.module.gnss_is_on:
                raise GNSSFixError("GNSS引擎未开启")
            location = self.module.get_gnss_location(retries=1)
            return location['wgs84']['latitude'], location['wgs84']['longitude']
        except GNSSFixError as e:
            self.log_message(f"获取真实GPS失败 ({e.args[0]})，使用模拟坐标。", "WARN")
            lat = float(self.sim_vars.get('latitude', tk.StringVar(value='0.0')).get())
            lng = float(self.sim_vars.get('longitude', tk.StringVar(value='0.0')).get())
            return lat, lng

    def _api_call(self, method, endpoint, data, friendly_name):
        if not self.token:
            self.log_message(f"{friendly_name}失败: 请先登录。", "ERROR")
            return
        try:
            self.log_message(f"--- {friendly_name} ---")
            
            # 使用URL参数传递Token，这是最可靠的方式
            url = f"{self.base_url}{endpoint}?token={self.token}"
            
            # --- 【核心修正】 ---
            # 根据data是否为None，决定请求体内容
            request_data = json.dumps(data) if data is not None else None
            # --- 修正结束 ---

            # --- 【新增】打印请求详情 ---
            self.log_message(f"请求 URL: {url}")
            if request_data:
                self.log_message(f"请求 Body: {request_data}")
            else:
                self.log_message("请求 Body: (无)")
            # --- 新增结束 ---

            response_dict = self.module.http_request(method, url, data=request_data)
            
            body = response_dict.get('body', '')
            self.log_message(f"服务器响应: {body}")
            if not body:
                raise ValueError("响应体为空")
            
            start_idx = body.find('{'); end_idx = body.rfind('}');
            if start_idx != -1 and end_idx != -1:
                json_str = body[start_idx : end_idx+1]
                parsed_json = json.loads(json_str)
            else:
                raise json.JSONDecodeError("响应中无JSON对象", body, 0)

            if parsed_json.get("code") == 200:
                self.log_message(f"{friendly_name} 成功！", "SUCCESS")
            else:
                self.log_message(f"{friendly_name} 失败: {parsed_json.get('message', '未知错误')}", "ERROR")

        except Exception as e:
            self.log_message(f"{friendly_name} 过程出错: {e}", "ERROR")

    def device_login(self):
        if not self.module:
            self.log_message("模块未初始化。", "ERROR")
            return
        try:
            self.log_message("=== 开始设备登录 ===")
            self.update_status("正在登录...")
            
            url = f"{self.base_url}/api/v1/auth/login"
            
            # --- 【核心修正】 ---
            # 根据您的API文档，精确地、手动地构建请求体，确保键名(camelCase)完全正确。
            # 这是为了解决 "deviceId: 设备ID不能为空" 的问题。
            payload = {
                "deviceId": self.device_config.get("default_device_id"),
                "deviceType": self.device_config.get("default_device_type"),
                "version": self.device_config.get("default_version"),
                "username": self.device_config.get("default_username"),
                "password": self.device_config.get("default_password")
            }
            payload_str = json.dumps(payload)
            # --- 修正结束 ---

            # --- 【新增】打印请求详情，方便调试 ---
            self.log_message(f"请求 URL: {url}")
            self.log_message(f"请求 Body: {payload_str}")
            # --- 新增结束 ---

            response_dict = self.module.http_request('POST', url, data=payload_str)
            
            body = response_dict['body']
            self.log_message(f"服务器原始响应体: {body}")

            # 使用健壮的JSON提取
            start_index = body.find('{')
            end_index = body.rfind('}')
            if start_index != -1 and end_index != -1:
                json_str = body[start_index : end_index + 1]
                data = json.loads(json_str)
            else:
                raise json.JSONDecodeError("在响应中未找到有效的JSON对象", body, 0)

            if data.get("code") == 200:
                self.token = data["data"]["token"]
                self.token_var.set(self.token[:40] + "...")
                self.save_token(self.token)
                self.set_api_buttons_state(tk.NORMAL)
                self.log_message("登录成功！", "SUCCESS")
            else:
                self.log_message(f"登录失败: {data.get('message', '未知错误')}", "ERROR")

        except Exception as e:
            self.log_message(f"登录过程出错: {e}", "ERROR")
        
    def test_heartbeat(self):
        # 【核心修正】使用 datetime.now(timezone.utc) 并移除毫秒
        payload = {"timestamp": datetime.now(timezone.utc).isoformat(timespec='seconds') + 'Z'}
        self._api_call('POST', '/api/v1/device/heartbeat', data=payload, friendly_name="发送心跳")
        
    def test_realtime_data(self):
        lat, lng = self._get_location_for_payload()
        # 【核心修正】安全地获取模拟数据
        payload = {
            "timestamp": self._get_beijing_time_str(),
            "location_lat": lat,
            "location_lng": lng,
            "speed": float(self.sim_vars.get('speed', tk.StringVar(value='0.0')).get()),
            "direction": float(self.sim_vars.get('direction', tk.StringVar(value='0.0')).get())
        }
        self._api_call('POST', '/api/v1/data/realtime', data=payload, friendly_name="上报实时数据")
        
    def test_gps_data(self):
        # 【核心修正】
        self.log_message("--- 上报GPS+疲劳数据 ---")
        
        # 1. 优先获取真实定位数据
        location_data = None
        try:
            if self.module and self.module.gnss_is_on:
                location_data = self.module.get_gnss_location(retries=1)
                self.log_message("已获取到真实GPS数据用于上报。")
            else:
                self.log_message("GNSS未开启，将使用模拟数据生成GPS字符串。","WARN")
        except GNSSFixError:
            self.log_message("获取真实GPS定位失败，将使用模拟数据生成GPS字符串。","WARN")
        
        # 2. 如果真实数据获取失败，则创建一个模拟的定位数据字典
        if not location_data:
            lat_sim = float(self.sim_vars['latitude'].get())
            lng_sim = float(self.sim_vars['longitude'].get())
            location_data = {
                'wgs84': {'latitude': lat_sim, 'longitude': lng_sim}
            }

        # 3. 调用新函数生成格式化的raw_gps_data字符串
        raw_gps_string = self._construct_raw_gps_string(location_data)

        # 4. 构建最终的请求体
        payload = {
          "raw_gps_data": raw_gps_string,
          "fatigue_score": float(self.sim_vars.get('fatigue_score').get()),
          "eye_blink_rate": 0.45, # Example value, can be added to sim_vars
          "head_movement_score": 0.32, # Example value
          "yawn_count": int(self.sim_vars.get('yawn_count').get()),
          "attention_score": float(self.sim_vars.get('attention_score').get())
        }
        
        self._api_call('POST', '/api/v1/data/gps', data=payload, friendly_name="上报GPS+疲劳数据")
        
    def test_event_data(self):
        lat, lng = self._get_location_for_payload(); self.event_counter += 1
        payload = {"eventId": f"EVT_{self.event_counter}_{int(time.time())}", "timestamp": datetime.now(timezone.utc).isoformat(timespec='seconds') + 'Z', "eventType": "FATIGUE", "severity": "HIGH", "locationLat": lat, "locationLng": lng}
        self._api_call('POST', '/api/v1/data/event', data=payload, friendly_name="上报事件数据")

    def on_closing(self):
        if self.module and self.module.ser.is_open:
            self.log_message("关闭窗口，仅关闭串口连接，GNSS状态保持不变。")
            self.module.close()
        self.root.destroy()
    def load_saved_token(self):
        try:
            if os.path.exists("token.json"):
                with open("token.json", "r") as f: data = json.load(f)
                self.token = data["token"]; self.token_var.set(self.token[:40] + "...")
                self.log_message("已加载Token")
                # Don't enable API buttons until module is ready
        except: self.clear_token()
    def save_token(self, token):
        with open("token.json", "w") as f: json.dump({"token": token}, f)
    def clear_token(self):
        self.token = None; self.token_var.set("N/A"); self.set_api_buttons_state(tk.DISABLED)
        if os.path.exists("token.json"): os.remove("token.json")
    def update_status(self, message): self.root.after(0, lambda: self.status_var.set(f"状态: {message}"))
    def log_message(self, message, level="INFO"): self.root.after(0, lambda: self._log_message_thread_safe(message, level))
    def _log_message_thread_safe(self, message, level):
        log_entry = f"[{datetime.now().strftime('%H:%M:%S')}] [{level}] {message}\n"
        self.log_text.insert(tk.END, log_entry); self.log_text.see(tk.END)
    def get_satellite_info(self):
        if not self.module: return
        try:
            self.log_message("正在获取星历信息..."); sats = self.module.get_current_satellites_info()
            if sats: self.log_message(f"获取星历成功: {sats['systems']}", "SUCCESS")
            else: self.log_message("获取星历失败。", "ERROR")
        except QuectelError as e: self.log_message(f"获取星历失败: {e}", "ERROR")
    def _get_beijing_time_str(self):
        """获取北京时间 (UTC+8) 并格式化为后端需要的字符串"""
        # 【核心修正】
        utc_now = datetime.now(timezone.utc)
        beijing_time = utc_now + timedelta(hours=8)
        # 格式化为 YYYY-MM-DDTHH:MM:SS
        return beijing_time.isoformat(timespec='seconds').split('+')[0]

if __name__ == "__main__":
    root = tk.Tk()
    app = HardwareAPITester(root)
    root.mainloop()