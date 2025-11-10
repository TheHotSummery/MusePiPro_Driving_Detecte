# -*- coding: utf-8 -*-
"""
性能监控模块
定期记录系统资源使用情况，包括CPU、内存、温度、PLC和YOLO进程资源占用
"""

import os
import time
import logging
import threading
import psutil
from datetime import datetime
from pathlib import Path


class PerformanceMonitor:
    """性能监控器类"""
    
    def __init__(self, log_dir="performance_logs", interval=5.0, enabled=True):
        """
        初始化性能监控器
        
        Args:
            log_dir: 日志文件目录
            interval: 记录间隔（秒），默认5秒
            enabled: 是否启用监控，默认True
        """
        self.log_dir = Path(log_dir)
        self.interval = interval
        self.enabled = enabled
        self.running = False
        self.monitor_thread = None
        self.log_file = None
        self.log_file_path = None
        
        # 进程名称映射（用于查找特定进程）
        self.plc_process_name = "plc_core"
        self.yolo_process_name = "python"  # YOLO程序就是当前Python进程
        
        # CPU采样配置（用于提高低使用率核心的精度）
        self.cpu_sample_interval = 0.4  # 采样间隔（秒），增加以提高低使用率核心的精度
        
        # 创建日志目录
        if self.enabled:
            self.log_dir.mkdir(parents=True, exist_ok=True)
            self._create_log_file()
    
    def _create_log_file(self):
        """创建新的日志文件，文件名包含时间戳"""
        if not self.enabled:
            return
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        self.log_file_path = self.log_dir / f"performance_{timestamp}.log"
        
        try:
            self.log_file = open(self.log_file_path, "w", encoding="utf-8")
            # 写入文件头
            self.log_file.write(f"# 性能监控日志 - 开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            self.log_file.write(f"# 记录间隔: {self.interval} 秒\n")
            self.log_file.write("# " + "=" * 100 + "\n")
            self.log_file.write("# 格式: 时间戳 | CPU总占用 | CPU各核心占用 | 内存使用 | 内存占用率 | "
                               "PLC进程CPU | PLC进程内存 | YOLO进程CPU | YOLO进程内存 | 温度 | "
                               "YOLO处理时间(Pre/Inf/Post/Logic/Draw/Total)\n")
            self.log_file.write("# 注意：某些CPU核心可能处于休眠状态或使用率极低，显示0.00%是正常的\n")
            self.log_file.write("# " + "=" * 100 + "\n")
            self.log_file.flush()
            logging.info(f"性能监控日志文件已创建: {self.log_file_path}")
        except Exception as e:
            logging.error(f"创建性能监控日志文件失败: {e}")
            self.log_file = None
    
    def _get_temperature(self):
        """获取系统温度（如果可用）"""
        try:
            # 尝试获取CPU温度（Linux系统）
            if hasattr(psutil, "sensors_temperatures"):
                temps = psutil.sensors_temperatures()
                if temps:
                    # 返回第一个可用的温度传感器值
                    for name, entries in temps.items():
                        if entries:
                            return entries[0].current
            # 如果psutil不支持，尝试从/sys读取（Linux）
            if os.path.exists("/sys/class/thermal/thermal_zone0/temp"):
                with open("/sys/class/thermal/thermal_zone0/temp", "r") as f:
                    temp_str = f.read().strip()
                    return float(temp_str) / 1000.0  # 转换为摄氏度
        except Exception as e:
            logging.debug(f"获取温度失败: {e}")
        return None
    
    def _find_process_by_name(self, name):
        """根据进程名查找进程（返回第一个匹配的）"""
        try:
            for proc in psutil.process_iter(['pid', 'name', 'exe', 'cmdline']):
                try:
                    proc_info = proc.info
                    proc_name = proc_info.get('name', '')
                    proc_exe = proc_info.get('exe', '')
                    proc_cmdline = proc_info.get('cmdline', [])
                    
                    # 检查进程名
                    if name.lower() in proc_name.lower():
                        return proc
                    # 检查可执行文件路径
                    if proc_exe and name.lower() in proc_exe.lower():
                        return proc
                    # 检查命令行参数（对于PLC进程，可能是 ./build/plc_core）
                    if proc_cmdline:
                        cmdline_str = ' '.join(proc_cmdline).lower()
                        if name.lower() in cmdline_str:
                            return proc
                except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
                    continue
        except Exception as e:
            logging.debug(f"查找进程 {name} 失败: {e}")
        return None
    
    def _get_process_stats(self, process):
        """获取进程的CPU和内存使用情况"""
        if process is None:
            return None, None
        
        try:
            # 获取CPU使用率（使用与系统CPU相同的采样间隔以提高精度）
            # 注意：第一次调用cpu_percent()需要interval参数来建立基准
            cpu_percent = process.cpu_percent(interval=self.cpu_sample_interval)
            # 获取内存信息
            mem_info = process.memory_info()
            mem_mb = mem_info.rss / 1024 / 1024  # 转换为MB
            return cpu_percent, mem_mb
        except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
            return None, None
        except Exception as e:
            logging.debug(f"获取进程统计信息失败: {e}")
            return None, None
    
    def _get_yolo_timing_stats(self):
        """获取YOLO处理时间统计（从VideoProcessor实例中读取）"""
        try:
            # 尝试从web_server获取VideoProcessor实例
            from web_server import get_web_server
            web_server = get_web_server()
            if web_server and hasattr(web_server, 'video_processor'):
                video_processor = web_server.video_processor
                if video_processor and hasattr(video_processor, 'get_latest_timing_stats'):
                    return video_processor.get_latest_timing_stats()
        except (ImportError, AttributeError, Exception) as e:
            # 如果无法获取，返回None（不影响主功能）
            logging.debug(f"获取YOLO处理时间统计失败: {e}")
        return None
    
    def _collect_metrics(self):
        """收集系统指标"""
        try:
            timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            
            # CPU信息（使用更长的采样间隔以提高精度，特别是对于低占用率的核心）
            # 注意：psutil.cpu_percent() 需要 interval 参数来建立基准并采样
            # 对于低使用率的核心（如0.6%），较短的采样间隔（0.2秒）可能导致精度不足，显示为0
            # 增加采样间隔（0.4秒）可以提高精度，但会增加每次采集的时间
            # 如果htop显示非零值而这里显示0，说明采样间隔需要进一步增加
            cpu_total = psutil.cpu_percent(interval=self.cpu_sample_interval)
            cpu_per_core = psutil.cpu_percent(percpu=True, interval=self.cpu_sample_interval)
            # 使用2位小数格式化（平衡精度和可读性）
            # 注意：某些CPU核心可能真的使用率很低或处于休眠状态，显示0.00%是正常的
            cpu_per_core_str = ",".join([f"{c:.2f}" for c in cpu_per_core])
            
            # 内存信息
            mem = psutil.virtual_memory()
            mem_total_gb = mem.total / 1024**3
            mem_used_gb = mem.used / 1024**3
            mem_percent = mem.percent
            
            # 查找PLC进程
            plc_process = self._find_process_by_name(self.plc_process_name)
            plc_cpu, plc_mem = self._get_process_stats(plc_process)
            
            # YOLO进程（当前Python进程）
            yolo_process = psutil.Process()
            yolo_cpu, yolo_mem = self._get_process_stats(yolo_process)
            
            # 获取YOLO处理时间统计
            yolo_timing = self._get_yolo_timing_stats()
            
            # 温度
            temperature = self._get_temperature()
            temp_str = f"{temperature:.1f}°C" if temperature is not None else "N/A"
            
            # 格式化YOLO处理时间字符串
            if yolo_timing:
                timing_str = (
                    f"YOLO时间: Pre={yolo_timing.get('preprocess', 0):.2f}ms, "
                    f"Inf={yolo_timing.get('inference', 0):.2f}ms, "
                    f"Post={yolo_timing.get('postprocess', 0):.2f}ms, "
                    f"Logic={yolo_timing.get('logic', 0):.2f}ms, "
                    f"Draw={yolo_timing.get('draw', 0):.2f}ms, "
                    f"Total={yolo_timing.get('total', 0):.2f}ms"
                )
            else:
                timing_str = "YOLO时间: N/A"
            
            # 格式化日志行（使用更高精度）
            if plc_cpu is not None:
                log_line = (
                    f"{timestamp} | "
                    f"CPU总={cpu_total:.2f}% | "
                    f"CPU核心=[{cpu_per_core_str}] | "
                    f"内存={mem_used_gb:.2f}GB/{mem_total_gb:.2f}GB ({mem_percent:.1f}%) | "
                    f"PLC进程: CPU={plc_cpu:.2f}% MEM={plc_mem:.1f}MB | "
                    f"YOLO进程: CPU={yolo_cpu:.2f}% MEM={yolo_mem:.1f}MB | "
                    f"温度={temp_str} | "
                    f"{timing_str}"
                )
            else:
                log_line = (
                    f"{timestamp} | "
                    f"CPU总={cpu_total:.2f}% | "
                    f"CPU核心=[{cpu_per_core_str}] | "
                    f"内存={mem_used_gb:.2f}GB/{mem_total_gb:.2f}GB ({mem_percent:.1f}%) | "
                    f"PLC进程: 未找到 | "
                    f"YOLO进程: CPU={yolo_cpu:.2f}% MEM={yolo_mem:.1f}MB | "
                    f"温度={temp_str} | "
                    f"{timing_str}"
                )
            
            return log_line
        except Exception as e:
            logging.error(f"收集性能指标失败: {e}")
            return None
    
    def _monitor_loop(self):
        """监控循环"""
        logging.info(f"性能监控线程已启动，记录间隔: {self.interval} 秒，CPU采样间隔: {self.cpu_sample_interval} 秒")
        
        # 预热：第一次调用cpu_percent建立基准（不记录结果）
        try:
            psutil.cpu_percent(interval=0.1)
            psutil.cpu_percent(percpu=True, interval=0.1)
        except:
            pass
        
        while self.running:
            try:
                log_line = self._collect_metrics()
                if log_line and self.log_file:
                    self.log_file.write(log_line + "\n")
                    self.log_file.flush()
                
                # 等待指定间隔（减去CPU采样时间，避免累积延迟）
                # 注意：CPU采样会阻塞，所以实际等待时间 = interval - cpu_sample_interval
                wait_time = max(0.1, self.interval - self.cpu_sample_interval)
                time.sleep(wait_time)
            except Exception as e:
                logging.error(f"性能监控循环错误: {e}")
                time.sleep(self.interval)
    
    def start(self):
        """启动性能监控"""
        if not self.enabled:
            logging.info("性能监控已禁用（可通过配置启用）")
            return
        
        if self.running:
            logging.warning("性能监控已在运行")
            return
        
        if self.log_file is None:
            logging.error("性能监控日志文件未创建，无法启动")
            return
        
        self.running = True
        self.monitor_thread = threading.Thread(target=self._monitor_loop, daemon=True)
        self.monitor_thread.start()
        logging.info("性能监控已启动")
    
    def stop(self):
        """停止性能监控"""
        if not self.running:
            return
        
        self.running = False
        if self.monitor_thread:
            self.monitor_thread.join(timeout=2.0)
        
        if self.log_file:
            try:
                self.log_file.write(f"# 性能监控日志 - 结束时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
                self.log_file.close()
                logging.info(f"性能监控已停止，日志文件: {self.log_file_path}")
            except Exception as e:
                logging.error(f"关闭性能监控日志文件失败: {e}")
        
        self.log_file = None

