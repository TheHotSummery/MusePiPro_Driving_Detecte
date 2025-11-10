# -*- coding: utf-8 -*-
"""GPIO 控制模块

重写后版本通过 PLC + Modbus 完成输出控制，不再直接操作本地 GPIO。
"""

from __future__ import annotations

import logging
import threading
import time
from typing import Optional

from plc_bridge import PLCBridge


class GPIOController:
    """兼容旧接口的 PLC 驱动控制器。"""

    def __init__(self, plc_bridge: Optional[PLCBridge] = None) -> None:
        self._lock = threading.Lock()
        self._plc_bridge = plc_bridge or PLCBridge()
        self._owns_bridge = plc_bridge is None
        self.current_level = "Normal"

    # ------------------------------------------------------------------
    # 兼容旧接口的方法
    # ------------------------------------------------------------------
    def update_alert_level(self, new_level: str) -> None:
        with self._lock:
            if new_level == self.current_level:
                return
            self.current_level = new_level

        if self._plc_bridge.set_alert_level(new_level):
            logging.info("PLC 警报等级更新为 %s", new_level)
        else:
            logging.error("PLC 警报等级写入失败: %s", new_level)

    def trigger_level1_alert(self) -> None:
        self.update_alert_level("Level 1")

    def trigger_level2_alert(self) -> None:
        self.update_alert_level("Level 2")

    def trigger_level3_alert(self) -> None:
        self.update_alert_level("Level 3")

    def trigger_manual(self, gpio: int, duration: float) -> None:
        logging.warning(
            "手动触发 GPIO%d 已改为 PLC 控制，当前操作被忽略（持续 %.2fs）",
            gpio,
            duration,
        )

    def _immediate_stop_all_gpio(self) -> None:
        self._plc_bridge.reset_yolo_flags()

    def _stop_all_alerts(self) -> None:
        self._plc_bridge.reset_yolo_flags()

    def is_available(self) -> bool:
        return self._plc_bridge.is_available()

    def force_init_and_reset(self) -> bool:
        """测试 Modbus 连接并清空 YOLO 线圈，同时关闭 M0/M1。
        
        注意：不再启动 PLC 进程，假设 PLC 已由外部程序启动。
        """
        import time
        start_time = time.time()
        
        logging.info("测试 Modbus 连接并复位线圈...")
        
        # 测试连接（最多重试 10 次，每次 0.5 秒）
        if not self._plc_bridge.test_connection(max_retries=10, delay=0.5):
            elapsed = time.time() - start_time
            logging.error("Modbus 连接测试失败（耗时 %.2f 秒）", elapsed)
            return False

        init_time = time.time() - start_time
        logging.info("Modbus 连接成功（耗时 %.2f 秒），正在复位线圈...", init_time)
        
        # 使用线程执行写入操作，避免阻塞主线程
        reset_success = [False]  # 使用列表以便在线程中修改
        
        def reset_coils():
            try:
                # 复位 YOLO 标志（M40-M42）
                reset_start = time.time()
                logging.info("正在复位 YOLO 标志（M40-M42）...")
                self._plc_bridge.reset_yolo_flags()
                reset_time = time.time() - reset_start
                logging.info("YOLO 标志复位完成（耗时 %.2f 秒）", reset_time)
                
                # 默认关闭 M0/M1，避免旧逻辑残留导致输出异常
                m0_start = time.time()
                logging.info("正在写入 M0 = False...")
                if not self._plc_bridge.set_memory_bit(0, False):
                    logging.warning("M0 写入失败，但继续执行")
                m0_time = time.time() - m0_start
                logging.info("M0 写入完成（耗时 %.2f 秒）", m0_time)
                
                m1_start = time.time()
                logging.info("正在写入 M1 = False...")
                if not self._plc_bridge.set_memory_bit(1, False):
                    logging.warning("M1 写入失败，但继续执行")
                m1_time = time.time() - m1_start
                logging.info("M1 写入完成（耗时 %.2f 秒）", m1_time)
                
                reset_success[0] = True
            except Exception as e:
                logging.error("线圈复位过程中出错: %s", e, exc_info=True)
                reset_success[0] = False
        
        # 在后台线程中执行复位，最多等待 3 秒
        reset_thread = threading.Thread(target=reset_coils, daemon=True)
        reset_thread.start()
        reset_thread.join(timeout=3.0)  # 最多等待 3 秒
        
        if reset_thread.is_alive():
            logging.warning("线圈复位操作超时（已等待 3 秒），但继续启动系统")
            logging.warning("提示：线圈可能未完全复位，系统将继续运行")
            # 不返回 False，允许系统继续启动
            return True
        
        total_time = time.time() - start_time
        if reset_success[0]:
            logging.info("线圈复位完成（总耗时 %.2f 秒）", total_time)
        else:
            logging.warning("线圈复位失败，但系统将继续运行（总耗时 %.2f 秒）", total_time)
        
        return True  # 即使复位失败，也允许系统继续运行

    def cleanup(self) -> None:
        """清理资源（只关闭 Modbus 连接，不管理 PLC 进程）。"""
        logging.info("开始清理 GPIO 控制器资源...")
        
        # 尝试停止警报，但不阻塞
        try:
            # 使用线程执行，避免阻塞
            def stop_alerts():
                try:
                    self._stop_all_alerts()
                except Exception as e:
                    logging.warning("清理警报时出错: %s", e)
            
            stop_thread = threading.Thread(target=stop_alerts, daemon=True)
            stop_thread.start()
            stop_thread.join(timeout=0.5)  # 最多等待 0.5 秒
            
            if stop_thread.is_alive():
                logging.warning("停止警报操作超时，继续清理")
        except Exception as e:
            logging.warning("清理警报时出错: %s", e)
        
        # 关闭 Modbus 连接
        if self._owns_bridge:
            try:
                self._plc_bridge.stop()
            except Exception as e:
                logging.warning("关闭 Modbus 连接时出错: %s", e)
        
        logging.info("GPIO 控制器资源清理完成")

