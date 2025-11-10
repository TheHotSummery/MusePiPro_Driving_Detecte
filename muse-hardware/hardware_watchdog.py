# -*- coding: utf-8 -*-
"""
硬件看门狗喂狗模块
用于定期喂系统固件看门狗，防止系统重启
"""

import os
import time
import logging
import threading
from pathlib import Path


class HardwareWatchdog:
    """硬件看门狗喂狗器类"""
    
    # 看门狗设备路径
    WATCHDOG_TIMEOUT_PATH = "/sys/devices/platform/soc/c0500000.linlon-v5/watchdog_timeout"
    WATCHDOG_FEED_PATHS = [
        "/sys/devices/platform/soc/c0500000.linlon-v5/watchdog_feed",
        "/sys/devices/platform/soc/c0500000.linlon-v5/watchdog_ping",
        "/sys/devices/platform/soc/c0500000.linlon-v5/watchdog_heartbeat",
        "/sys/devices/platform/soc/c0500000.linlon-v5/heartbeat",
        "/sys/devices/platform/soc/c0500000.linlon-v5/ping",
    ]
    
    def __init__(self, feed_interval=10.0, enabled=True):
        """
        初始化硬件看门狗喂狗器
        
        Args:
            feed_interval: 喂狗间隔（秒），建议设置为看门狗超时时间的1/3
            enabled: 是否启用喂狗，默认False（需要root权限）
        """
        self.feed_interval = feed_interval
        self.enabled = enabled
        self.running = False
        self.feed_thread = None
        self.feed_path = None
        
        # 如果未启用，直接返回，不进行任何检查
        if not self.enabled:
            logging.debug("硬件看门狗喂狗功能已禁用")
            return
        
        # 检查看门狗设备是否存在
        if not os.path.exists(self.WATCHDOG_TIMEOUT_PATH):
            logging.warning(f"硬件看门狗设备不存在: {self.WATCHDOG_TIMEOUT_PATH}")
            self.enabled = False
            return
        
        # 检查是否有读取权限（避免后续操作失败）
        try:
            with open(self.WATCHDOG_TIMEOUT_PATH, 'r') as f:
                f.read()
        except PermissionError:
            logging.warning(f"硬件看门狗设备无访问权限: {self.WATCHDOG_TIMEOUT_PATH}")
            logging.warning("提示：需要root权限才能喂硬件看门狗，已自动禁用此功能")
            self.enabled = False
            return
        except Exception as e:
            logging.warning(f"检查看门狗设备时出错: {e}，已自动禁用此功能")
            self.enabled = False
            return
        
        # 查找喂狗接口
        self._find_feed_path()
        
        if self.enabled and not self.feed_path:
            logging.warning("未找到硬件看门狗喂狗接口，尝试使用超时重置方式")
            # 如果找不到专门的喂狗接口，尝试通过重置超时来喂狗
            self.feed_path = self.WATCHDOG_TIMEOUT_PATH
    
    def _find_feed_path(self):
        """查找喂狗接口路径"""
        for path in self.WATCHDOG_FEED_PATHS:
            if os.path.exists(path):
                self.feed_path = path
                logging.info(f"找到硬件看门狗喂狗接口: {self.feed_path}")
                return
        
        logging.debug("未找到标准的喂狗接口，将尝试其他方式")
    
    def _feed_watchdog(self):
        """喂狗操作"""
        if not self.feed_path:
            return False
        
        try:
            # 尝试写入喂狗信号
            # 不同的看门狗可能需要不同的值，常见的有：1, '1', 'ping', 'feed'等
            feed_values = ['1', 'ping', 'feed', 'heartbeat', '\n']
            
            for value in feed_values:
                try:
                    with open(self.feed_path, 'w') as f:
                        f.write(str(value))
                        f.flush()
                    logging.debug(f"硬件看门狗喂狗成功: {self.feed_path} = {value}")
                    return True
                except (IOError, OSError, PermissionError) as e:
                    logging.debug(f"尝试喂狗值 {value} 失败: {e}")
                    continue
            
            # 如果所有值都失败，尝试读取当前超时值然后写回（重置超时）
            if self.feed_path == self.WATCHDOG_TIMEOUT_PATH:
                try:
                    with open(self.feed_path, 'r') as f:
                        current_timeout = f.read().strip()
                    with open(self.feed_path, 'w') as f:
                        f.write(current_timeout)
                        f.flush()
                    logging.debug(f"通过重置超时值喂狗: {current_timeout}")
                    return True
                except PermissionError as e:
                    logging.warning(f"重置超时值失败（权限不足）: {e}")
                    logging.warning("提示：需要root权限才能喂硬件看门狗，建议使用sudo运行程序或配置系统服务")
                    return False
                except (IOError, OSError) as e:
                    logging.warning(f"重置超时值失败: {e}")
            
            return False
        except Exception as e:
            logging.error(f"喂狗操作异常: {e}")
            return False
    
    def _feed_loop(self):
        """喂狗循环
        
        注意：此循环完全独立运行，即使其他线程卡死也不会影响喂狗。
        使用分次sleep，确保可以快速响应停止信号。
        """
        logging.info(f"硬件看门狗喂狗线程已启动，喂狗间隔: {self.feed_interval} 秒")
        
        feed_count = 0
        consecutive_failures = 0
        max_failures = 3  # 连续失败3次后降低日志级别
        
        while self.running:
            try:
                feed_start = time.time()
                success = self._feed_watchdog()
                feed_time = time.time() - feed_start
                
                if success:
                    feed_count += 1
                    consecutive_failures = 0
                    if feed_count % 20 == 0:  # 每20次记录一次日志（减少日志量）
                        logging.debug(f"硬件看门狗已喂狗 {feed_count} 次（耗时 %.3f 秒）", feed_time)
                else:
                    consecutive_failures += 1
                    if consecutive_failures <= max_failures:
                        logging.warning("硬件看门狗喂狗失败（连续失败 %d 次）", consecutive_failures)
                    # 超过max_failures后不再记录警告，避免日志刷屏
                
                # 等待指定间隔（分次sleep，确保可以快速响应停止信号）
                sleep_steps = int(self.feed_interval * 10)  # 每0.1秒检查一次
                for _ in range(sleep_steps):
                    if not self.running:
                        break
                    time.sleep(0.1)
            except Exception as e:
                consecutive_failures += 1
                logging.error(f"硬件看门狗喂狗循环错误（连续失败 {consecutive_failures} 次）: {e}")
                # 即使出错也继续运行，确保喂狗不中断
                time.sleep(self.feed_interval)
        
        logging.info("硬件看门狗喂狗线程已停止")
    
    def start(self):
        """启动喂狗线程"""
        if not self.enabled:
            logging.info("硬件看门狗喂狗已禁用")
            return
        
        if self.running:
            logging.warning("硬件看门狗喂狗线程已在运行")
            return
        
        if not self.feed_path:
            logging.error("硬件看门狗喂狗接口未找到，无法启动")
            return
        
        self.running = True
        self.feed_thread = threading.Thread(target=self._feed_loop, daemon=True)
        self.feed_thread.start()
        logging.info("硬件看门狗喂狗已启动")
    
    def stop(self):
        """停止喂狗线程"""
        if not self.running:
            return
        
        self.running = False
        if self.feed_thread:
            self.feed_thread.join(timeout=2.0)
        
        logging.info("硬件看门狗喂狗已停止")
    
    def feed_once(self):
        """手动喂狗一次（用于测试）"""
        if not self.enabled:
            return False
        return self._feed_watchdog()

