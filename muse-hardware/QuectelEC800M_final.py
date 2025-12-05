# QuectelEC800M_final.py
# Final Baseline Version

import serial
import time
import logging
import threading
import re
from datetime import datetime, timezone

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

class QuectelError(Exception):
    def __init__(self, message, at_command=None, raw_response=None):
        super().__init__(message)
        self.at_command = at_command
        self.raw_response = raw_response
    def __str__(self):
        return f"{self.__class__.__name__}: {self.args[0]} | Command: {self.at_command} | Response: {self.raw_response}"

class GNSSFixError(QuectelError): pass
class HttpRequestError(QuectelError): pass
class NetworkError(QuectelError): pass
class ModuleOperationError(QuectelError): pass

class QuectelEC800M:
    def __init__(self, port, baudrate=115200, apn="UNINET"):
        self.port = port
        self.baudrate = baudrate
        self.apn = apn
        self.ser = None
        self.lock = threading.Lock()
        self.http_request_lock = threading.Lock()  # 确保HTTP会话全程串行
        self.gnss_is_on = False
        self.time_offset = 0

        try:
            self.ser = serial.Serial(self.port, self.baudrate, timeout=1)
            logging.info(f"成功打开串口 {self.port}，波特率 {self.baudrate}")
        except serial.SerialException as e:
            raise ModuleOperationError(f"无法打开串口: {e}") from e

    def close(self):
        if self.ser and self.ser.is_open:
            self.ser.close()
            logging.info(f"串口 {self.port} 已关闭。")

    def _send_at_command(self, command, expected_responses=['OK'], timeout=5):
        with self.lock:
            logging.info(f"发送 -> {command}")
            self.ser.reset_input_buffer()
            self.ser.write((command + '\r\n').encode())
            response_lines = []
            start_time = time.time()
            while time.time() - start_time < timeout:
                try:
                    line = self.ser.readline().decode('utf-8', errors='ignore').strip()
                    if line:
                        logging.info(f"接收 <- {line}")
                        response_lines.append(line)
                        if any(expected in line for expected in expected_responses): return response_lines
                        if 'ERROR' in line: return None
                except Exception: pass
            logging.error(f"命令 '{command}' 等待响应超时 ({timeout}秒)。")
            return None

    def initialize_module(self):
        logging.info("--- 开始初始化模块 ---")
        try:
            if not self._send_at_command('ATE0'): logging.warning("关闭回显失败。")
            if not self._send_at_command('AT'): raise ModuleOperationError("模块无响应 (AT)", 'AT')
            if not self._send_at_command('AT+CPIN?'): raise ModuleOperationError("SIM卡状态错误", 'AT+CPIN?')
            if not self._send_at_command('AT+CGREG?'): raise ModuleOperationError("网络注册失败", 'AT+CGREG?')
        except QuectelError as e:
            logging.error(f"模块初始化失败: {e}"); raise
        logging.info("--- 模块初始化成功 ---")

    def sync_time_with_ntp(self, server="ntp.aliyun.com"):
        logging.info("--- 开始NTP时间同步 ---")
        self._check_and_activate_pdp()
        if not self._send_at_command(f'AT+QNTP=1,"{server}"'): raise ModuleOperationError("配置NTP服务器失败")
        ntp_urc = self._wait_for_urc('+QNTP:', timeout=65)
        if not ntp_urc or '0' not in ntp_urc: raise ModuleOperationError(f"NTP同步失败或超时, URC: {ntp_urc}")
        response = self._send_at_command('AT+CCLK?')
        if not response or not response[0].startswith('+CCLK:'): raise ModuleOperationError("获取网络时间失败")
        cclk_time_str = response[0].replace('+CCLK: "', '').replace('"', '')
        try:
            parts = re.split(r'[+-]', cclk_time_str); module_time_str = parts[0]; tz_str = parts[1]
            module_dt = datetime.strptime(module_time_str, '%y/%m/%d,%H:%M:%S')
            tz_offset_hours = int(tz_str) / 4
            module_utc_timestamp = module_dt.timestamp() - (tz_offset_hours * 3600)
            local_utc_timestamp = datetime.now(timezone.utc).timestamp()
            self.time_offset = module_utc_timestamp - local_utc_timestamp
            logging.info(f"NTP时间同步成功！与本地时间偏移: {self.time_offset:.2f} 秒")
        except Exception as e:
            raise ModuleOperationError(f"解析模块时间失败: {cclk_time_str}, Error: {e}")

    def get_accurate_timestamp(self):
        return int(time.time() + self.time_offset)

    def _check_and_activate_pdp(self, context_id=1):
        logging.info("--- 检查网络连接 (PDP上下文) ---")
        response = self._send_at_command(f'AT+QIACT?', timeout=5)
        if response and any(f'+QIACT: {context_id}' in line for line in response):
            logging.info(f"PDP上下文 {context_id} 已激活。"); return
        logging.info(f"PDP上下文 {context_id} 未激活，尝试配置...")
        response = self._send_at_command(f'AT+QICSGP={context_id},1,"{self.apn}","","",1')
        if not response: raise NetworkError("配置APN失败", f'AT+QICSGP={context_id},...')
        if any(f'+QIACT: {context_id}' in line for line in response):
            logging.info("模块在配置APN后自动激活成功。"); return
        logging.info("执行手动激活...");
        if not self._send_at_command(f'AT+QIACT={context_id}', timeout=150):
            time.sleep(1); response = self._send_at_command(f'AT+QIACT?', timeout=5)
            if not (response and any(f'+QIACT: {context_id}' in line for line in response)):
                 raise NetworkError("手动激活PDP失败", f'AT+QIACT={context_id}')
        logging.info(f"PDP上下文 {context_id} 激活成功。")

    def gnss_start(self):
        if self.gnss_is_on: logging.info("GNSS引擎已开启。"); return
        logging.info("--- 正在开启GNSS引擎 ---")
        self._send_at_command('AT+QGPSEND', timeout=3); time.sleep(1)
        if not self._send_at_command('AT+QGPSCFG="gnssconfig",1'): logging.warning("配置GPS+北斗双模失败。")
        if not self._send_at_command('AT+QGPS=1'): raise GNSSFixError("开启GNSS引擎失败", 'AT+QGPS=1')
        self.gnss_is_on = True; logging.info("GNSS引擎已成功开启。")

    def gnss_stop(self):
        if not self.gnss_is_on: logging.info("GNSS引擎已关闭。"); return
        logging.info("--- 正在关闭GNSS引擎 ---")
        if self._send_at_command('AT+QGPSEND'): self.gnss_is_on = False; logging.info("GNSS引擎已关闭。")
        else: self.gnss_is_on = False; logging.warning("发送关闭GNSS命令失败，但状态已重置。")

    def get_gnss_location(self, retries=3, interval=2):
        if not self.gnss_is_on: raise GNSSFixError("无法获取定位，GNSS引擎未开启。")
        logging.info(f"--- 尝试获取定位 (最多{retries}次) ---")
        for i in range(retries):
            response = self._send_at_command('AT+QGPSLOC=0', timeout=2)
            if response:
                for line in response:
                    if line.startswith('+QGPSLOC:'):
                        logging.info("成功获取到真实定位信息！"); location_data = self._parse_qgpsloc(line)
                        return location_data
            if i < retries - 1: time.sleep(interval)
        raise GNSSFixError(f"在 {retries} 次尝试后仍未获取到真实定位。")

    def get_current_satellites_info(self):
        if not self.gnss_is_on: raise GNSSFixError("无法获取卫星信息，GNSS引擎未开启。")
        response = self._send_at_command('AT+QGPSGNMEA="GSV"'); systems = {}; raw_gsv = []
        if response:
            for line in response:
                if 'GSV' in line and '$' in line:
                    gsv_part = '$' + line.split('$', 1)[1]; raw_gsv.append(gsv_part)
                    try:
                        parts = gsv_part.split(',');
                        if len(parts) >= 4:
                            system_key = {"GP": "GPS", "GB": "BeiDou", "GL": "GLONASS", "GA": "Galileo"}.get(parts[0][1:3], "Unknown")
                            if int(parts[2]) == 1: systems[system_key] = int(parts[3])
                    except Exception: pass
            total_sats = sum(systems.values())
            logging.info(f"当前搜索到 {total_sats} 颗卫星: {systems}")
            return {"total": total_sats, "systems": systems, "raw_gsv": raw_gsv}
        return None

    def _parse_qgpsloc(self, loc_string):
        try:
            parts = loc_string.replace('+QGPSLOC: ', '').split(',')
            lat_raw = parts[1]; lat_deg = int(lat_raw[:2]); lat_min = float(lat_raw[2:-1]); lat_dir = lat_raw[-1]; wgs_lat = lat_deg + (lat_min / 60.0)
            if lat_dir == 'S': wgs_lat = -wgs_lat
            lon_raw = parts[2]; lon_deg = int(lon_raw[:3]); lon_min = float(lon_raw[3:-1]); lon_dir = lon_raw[-1]; wgs_lng = lon_deg + (lon_min / 60.0)
            if lon_dir == 'W': wgs_lng = -wgs_lng
            return {"utc_time": parts[0], "wgs84": {"latitude": wgs_lat, "longitude": wgs_lng}, "hdop": float(parts[3]), "altitude_m": float(parts[4]), "fix_mode": int(parts[5]), "speed_kmh": float(parts[7]), "utc_date": parts[9], "satellites_in_use": int(parts[10])}
        except (IndexError, ValueError) as e:
            raise GNSSFixError(f"解析定位字符串失败: {loc_string}") from e

    def http_request(self, method, url, data=None, headers=None, context_id=1, timeout=60):
        logging.info(f"--- 准备执行 HTTP(S) {method} 请求到: {url} ---")
        with self.http_request_lock:
            try:
                # 所有串口操作都通过_send_at_command等方法，它们内部使用lock保护
                self._check_and_activate_pdp(context_id)
                self._http_config(url, context_id, headers, data is not None)
                
                url_len = len(url)
                # URL设置命令需要等待CONNECT响应，增加超时时间到30秒
                if not self._send_at_command(f'AT+QHTTPURL={url_len},{timeout}', expected_responses=['CONNECT'], timeout=30):
                    raise HttpRequestError("设置URL失败", at_command='AT+QHTTPURL')
                
                # 发送URL数据（需要在lock保护下）
                with self.lock:
                    self.ser.write(url.encode())
                if not self._read_until_ok(): raise HttpRequestError("输入URL后未收到OK")
                
                urc_prefix = ''
                if method.upper() == 'GET':
                    urc_prefix = '+QHTTPGET:'; command = f'AT+QHTTPGET={timeout}'
                    if not self._send_at_command(command): raise HttpRequestError("发送GET命令失败", command)
                
                elif method.upper() == 'POST':
                    urc_prefix = '+QHTTPPOST:'
                    
                    # --- 【核心修正】 ---
                    # 正确处理 data 为 None 的情况
                    if data is None:
                        # 如果data为None，发送空JSON对象，避免EC800M不支持data_len=0的情况
                        post_data = b'{}'
                    else:
                        post_data = data.encode('utf-8') if isinstance(data, str) else data
                    # --- 修正结束 ---
                    
                    data_len = len(post_data)
                    command = f'AT+QHTTPPOST={data_len},{timeout},{timeout}'
                    
                    # POST命令需要等待CONNECT响应
                    # 对于小数据（如2字节的{}），模块可能需要更长时间建立连接
                    # 增加超时时间到60秒
                    connect_timeout = max(60, timeout)  # 至少60秒
                    if not self._send_at_command(command, expected_responses=['CONNECT'], timeout=connect_timeout): 
                        raise HttpRequestError("发送POST命令失败", command)
                    
                    if data_len > 0: 
                        # 发送POST数据（需要在lock保护下）
                        with self.lock:
                            self.ser.write(post_data)
                        # 等待OK响应，增加超时时间
                        if not self._read_until_ok(timeout=10): 
                            raise HttpRequestError("输入POST数据后未收到OK")
                    # 如果data_len=0，命令可能已经返回OK，不需要额外等待
                
                result_line = self._wait_for_urc(urc_prefix, timeout + 10)
                if not result_line: raise HttpRequestError(f"等待{method}响应URC超时")
                err_code, http_status, _ = self._parse_http_urc(result_line)
                if err_code != 0: raise HttpRequestError(f"{method}请求失败, 模块内部错误码: {err_code}", raw_response=result_line)
                
                response_body = self._http_read_response(timeout)
                
                return {"status_code": http_status, "body": response_body}
            finally:
                self.http_stop()

    def _http_config(self, url, context_id, headers=None, has_post_data=False):
        if not self._send_at_command(f'AT+QHTTPCFG="contextid",{context_id}'): raise NetworkError("绑定PDP上下文失败")
        if url.lower().startswith('https://'):
            if not self._send_at_command(f'AT+QHTTPCFG="sslctxid",1'): raise NetworkError("绑定SSL上下文失败")
            if not self._send_at_command(f'AT+QSSLCFG="seclevel",1,0'): logging.warning("设置SSL不验证证书失败。")
        
        # 如果headers为None或空，使用自动Header模式
        if not headers:
            if not self._send_at_command('AT+QHTTPCFG="requestheader",0'): raise HttpRequestError("切换到自动Header模式失败")
            if has_post_data:
                logging.info("自动设置Content-Type为application/json。")
                if not self._send_at_command('AT+QHTTPCFG="contenttype",4'): raise HttpRequestError("设置Content-Type为JSON失败")
        else:
            # 使用自定义Header模式（仅当需要时，比如需要特殊的Content-Type）
            logging.info(f"配置自定义Headers: {headers}")
            if not self._send_at_command('AT+QHTTPCFG="requestheader",1'): raise HttpRequestError("启用自定义Header模式失败")
            # 转义header值中的引号（EC800M AT命令要求）
            def escape_header_value(value):
                # 将值中的引号转义为两个引号
                return str(value).replace('"', '""')
            
            # 先添加Content-Type（如果存在），确保它在最前面
            if 'Content-Type' in headers:
                content_type = escape_header_value(headers['Content-Type'])
                if not self._send_at_command(f'AT+QHTTPCFG="reqheader/add","Content-Type","{content_type}"'): 
                    raise HttpRequestError(f"添加Content-Type Header失败")
            # 然后添加其他headers（但不包括Authorization，因为token通过URL传递）
            for key, value in headers.items():
                if key != 'Content-Type' and key != 'Authorization':  # Content-Type已添加，Authorization通过URL传递
                    escaped_key = escape_header_value(key)
                    escaped_value = escape_header_value(str(value))
                    if not self._send_at_command(f'AT+QHTTPCFG="reqheader/add","{escaped_key}","{escaped_value}"'): 
                        raise HttpRequestError(f"添加自定义Header '{key}' 失败")
            # 如果有POST数据但Content-Type不在headers中，添加默认的
            if has_post_data and 'Content-Type' not in headers:
                if not self._send_at_command('AT+QHTTPCFG="reqheader/add","Content-Type","application/json"'): 
                    raise HttpRequestError("自定义模式下添加Content-Type失败")

    def _http_read_response(self, timeout):
        if not self._send_at_command(f'AT+QHTTPREAD={timeout}', expected_responses=['CONNECT']):
            raise HttpRequestError("发送读取响应命令失败", 'AT+QHTTPREAD')
        response_data = b''
        start_time = time.time()
        while time.time() - start_time < timeout:
            if self.ser.in_waiting > 0:
                response_data += self.ser.read(self.ser.in_waiting)
            decoded_data = response_data.decode('utf-8', errors='ignore')
            if '+QHTTPREAD: 0' in decoded_data:
                logging.info("HTTP响应内容读取完毕。")
                clean_data = decoded_data.split('+QHTTPREAD: 0')[0].strip()
                return clean_data
            if '+QHTTPREAD:' in decoded_data and '+QHTTPREAD: 0' not in decoded_data:
                 error_code = self._parse_cme_error(decoded_data)
                 raise HttpRequestError(f"读取响应内容时发生错误 (码: {error_code})", raw_response=decoded_data)
        raise HttpRequestError("读取HTTP响应内容超时", 'AT+QHTTPREAD')

    def _wait_for_urc(self, urc_prefix, timeout):
        """等待URC响应（在lock保护下）"""
        with self.lock:
            start_time = time.time()
            while time.time() - start_time < timeout:
                line = self.ser.readline().decode('utf-8', errors='ignore').strip()
                if line:
                    logging.info(f"接收 <- {line}")
                    if line.startswith(urc_prefix): return line
        return None

    def _read_until_ok(self, timeout=5):
        """读取直到收到OK（在lock保护下）"""
        with self.lock:
            start_time = time.time()
            while time.time() - start_time < timeout:
                line = self.ser.readline().decode('utf-8', errors='ignore').strip()
                if line:
                    logging.info(f"接收 <- {line}")
                    if line == 'OK': return True
        return False
    
    def _parse_cme_error(self, response_str):
        match = re.search(r'ERROR: (\d+)', response_str)
        return match.group(1) if match else "Unknown"
        
    def _parse_http_urc(self, urc_line):
        try: parts = urc_line.split(':')[1].strip().split(','); return int(parts[0]), int(parts[1]), int(parts[2]) if len(parts) > 2 else 0
        except (IndexError, ValueError) as e: raise QuectelError(f"解析URC失败: {urc_line}", raw_response=urc_line) from e

    def http_stop(self):
        logging.info("--- 停止HTTP(S)会话 ---")
        if not self._send_at_command('AT+QHTTPSTOP'): logging.warning("停止HTTP(S)会话失败。")