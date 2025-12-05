"""PLC Modbus 通信桥接模块.

此模块负责：
1. 通过 Modbus TCP 连接 PLC 服务（不启动进程，由外部程序管理）；
2. 写入 YOLO 相关线圈，实现等级控制；
3. 为 Python 业务层提供线程安全的写入接口。
"""

from __future__ import annotations

import logging
import os
import threading
import time
from typing import Iterable, Optional

from pymodbus.client import ModbusTcpClient
from pymodbus.exceptions import ModbusException


class PLCBridge:
    """封装 PLC C++ 进程与 Modbus 通信的桥接器。"""

    # Modbus 资源映射常量
    _COIL_OUTPUT_COUNT = 6  # Q0-Q5 占用 0-5
    _YOLO_MEMORY_START = 40  # M40-M45 对应 YOLO 信号（根据 Modbus 资源展示）
    _YOLO_COIL_COUNT = 3     # 仅使用前三个等级（M40, M41, M42）

    def __init__(
        self,
        binary_path: Optional["Path"] = None,  # 保留参数以兼容旧代码，但不再使用
        host: str = "127.0.0.1",
        port: Optional[int] = None,
        unit_id: int = 1,
        auto_start: bool = False,  # 默认不自动启动，由外部程序管理
        connect_timeout: float = 2.0,  # 增加连接超时到 2 秒
    ) -> None:
        # 不再管理 PLC 进程启动，只负责 Modbus 通信

        # 允许通过环境变量覆盖端口
        env_port = os.getenv("PLC_MODBUS_PORT")
        self._port = port or int(env_port) if env_port else 502
        self._host = host
        self._unit_id = unit_id
        # 设置连接和读写超时（pymodbus 的 timeout 参数同时控制连接和读写超时）
        self._timeout = connect_timeout
        # 读写操作的超时时间（秒），如果 PLC 响应慢可以增加
        self._write_timeout = 1.0  # 写入操作最多等待 1 秒（减少阻塞时间）

        self._client: Optional[ModbusTcpClient] = None
        self._client_lock = threading.Lock()
        self._io_lock = threading.Lock()
        self._available = False
        self._current_level = "Normal"
        
        # 测试线圈定时写入
        self._test_coil_thread: Optional[threading.Thread] = None
        self._test_coil_running = False

        # 不再自动启动 PLC 进程
        # 如果需要测试连接，可以调用 test_connection()

    # ------------------------------------------------------------------
    # 公共 API
    # ------------------------------------------------------------------
    def ensure_ready(self) -> bool:
        """确保 Modbus 客户端已连接（不启动 PLC 进程）。"""
        if not self._ensure_client():
            return False
        self._available = True
        return True

    def is_available(self) -> bool:
        """检查 Modbus 连接是否可用。"""
        return self._available and self._ensure_client()

    def test_connection(self, max_retries: int = 10, delay: float = 0.5) -> bool:
        """测试 Modbus 连接，带重试机制。
        
        Args:
            max_retries: 最大重试次数
            delay: 每次重试的延迟（秒）
            
        Returns:
            True 如果连接成功，False 如果失败
        """
        logging.info("测试 Modbus 连接 (host=%s, port=%d)...", self._host, self._port)
        
        # 先检查端口是否可达（简单的 socket 测试）
        import socket
        import errno
        try:
            test_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            test_socket.settimeout(1.0)
            result = test_socket.connect_ex((self._host, self._port))
            test_socket.close()
            if result != 0:
                error_name = errno.errorcode.get(result, f"UNKNOWN({result})")
                if result == errno.EAGAIN or result == errno.EWOULDBLOCK:
                    logging.warning("端口 %d 连接被拒绝或资源暂时不可用（错误码: %d, %s）", 
                                 self._port, result, error_name)
                    logging.warning("提示：可能 Modbus Poll 或其他客户端已连接，PLC 可能只支持一个连接")
                elif result == errno.ECONNREFUSED:
                    logging.warning("端口 %d 连接被拒绝（错误码: %d, %s）", 
                                 self._port, result, error_name)
                elif result == errno.ETIMEDOUT:
                    logging.warning("端口 %d 连接超时（错误码: %d, %s）", 
                                 self._port, result, error_name)
                else:
                    logging.warning("端口 %d 不可达（socket 连接失败，错误码: %d, %s）", 
                                 self._port, result, error_name)
            else:
                logging.info("端口 %d 可达，继续 Modbus 连接测试", self._port)
        except Exception as e:
            logging.warning("端口测试失败: %s", e)
        
        for attempt in range(max_retries):
            if self._ensure_client():
                # 尝试读取一个线圈来验证连接
                try:
                    if self._client:
                        # 读取第一个输出线圈（Q0）来测试连接
                        result = self._client.read_coils(0, 1)
                        if result.isError():  # type: ignore[has-type]
                            logging.warning("连接测试：读取线圈返回错误，但连接已建立")
                        else:
                            logging.info("连接测试：成功读取线圈，连接正常")
                except Exception as e:
                    logging.warning("连接测试：读取线圈时出错，但连接已建立: %s", e)
                
                self._available = True
                logging.info("Modbus 连接成功（尝试 %d/%d）", attempt + 1, max_retries)
                return True
            
            if attempt < max_retries - 1:
                elapsed = (attempt + 1) * delay
                logging.info("连接失败，%.1f 秒后重试... (尝试 %d/%d)", delay, attempt + 1, max_retries)
                time.sleep(delay)
        
        logging.error("Modbus 连接失败（已尝试 %d 次）", max_retries)
        logging.error("=" * 60)
        logging.error("连接失败的可能原因：")
        logging.error("1. Modbus Poll 或其他 Modbus 客户端已连接到 PLC")
        logging.error("   （PLC 一次只支持一个并发连接）")
        logging.error("2. 请关闭其他 Modbus 客户端后重试")
        logging.error("3. 或者检查 PLC 进程是否正常运行")
        logging.error("=" * 60)
        self._available = False
        return False

    def set_alert_level(self, level: str) -> bool:
        """根据疲劳等级写入 YOLO 线圈（累积式逻辑）。

        支持等级："Normal"、"Level 1"、"Level 2"、"Level 3"。
        - Normal: [False, False, False] → M40=OFF, M41=OFF, M42=OFF
        - Level 1: [True, False, False] → M40=ON, M41=OFF, M42=OFF
        - Level 2: [True, True, False] → M40=ON, M41=ON, M42=OFF
        - Level 3: [True, True, True] → M40=ON, M41=ON, M42=ON
        
        注意：这是累积式逻辑，高级别会保留低级别的状态。
        """

        if not self._ensure_client():
            logging.error("Modbus 未连接，无法写入 YOLO 线圈")
            return False

        normalized = (level or "Normal").title()
        
        # 累积式逻辑：每个等级包含之前所有等级的状态
        if normalized == "Level 1":
            values = [True, False, False]  # M40=ON, M41=OFF, M42=OFF
            logging.info("YOLO 等级 %s -> M40=ON, M41=OFF, M42=OFF", normalized)
        elif normalized == "Level 2":
            values = [True, True, False]  # M40=ON, M41=ON, M42=OFF
            logging.info("YOLO 等级 %s -> M40=ON, M41=ON, M42=OFF", normalized)
        elif normalized == "Level 3":
            values = [True, True, True]  # M40=ON, M41=ON, M42=ON
            logging.info("YOLO 等级 %s -> M40=ON, M41=ON, M42=ON", normalized)
        else:
            # Normal 或其他未知等级
            values = [False, False, False]  # M40=OFF, M41=OFF, M42=OFF
            normalized = "Normal"
            logging.debug("YOLO 等级重置为 Normal，所有线圈关闭")

        success = self._write_yolo_coils(values)
        if success:
            self._current_level = normalized
        return success

    def reset_yolo_flags(self) -> None:
        """复位所有 YOLO 标志（M40-M42）"""
        logging.debug("复位 YOLO 标志...")
        self.set_alert_level("Normal")
        logging.debug("YOLO 标志复位完成")

    def send_yolo_heartbeat(self) -> bool:
        """发送 YOLO 心跳：写入 M39 = ON
        
        PLC 会每15秒清空 M39，如果 M39 在10秒内没有收到心跳，
        指示灯将变为快速闪烁（表示 YOLO 未就绪）。
        """
        return self.set_memory_bit(39, True)

    def set_memory_bit(self, index: int, value: bool) -> bool:
        """写入通用中间继电器 (M) 到 Modbus 线圈。

        index=0 对应 M0，index=1 对应 M1，以此类推。
        
        注意：此操作有超时保护（1秒），如果超时会自动返回False。
        """

        if index < 0:
            raise ValueError("内存索引必须为非负整数")

        logging.debug("写入 M%d = %s", index, value)
        
        # 使用超时保护，避免长时间阻塞
        import threading
        result_container = [None]
        exception_container = [None]
        
        def write_operation():
            try:
                if not self._ensure_client():
                    result_container[0] = False
                    return
                
                address = self._COIL_OUTPUT_COUNT + index
                result_container[0] = self._write_coils(address, [bool(value)])
            except Exception as e:
                exception_container[0] = e
                result_container[0] = False
        
        # 在单独线程中执行，带超时
        write_thread = threading.Thread(target=write_operation, daemon=True)
        write_thread.start()
        write_thread.join(timeout=2.0)  # 最多等待2秒（包含1秒写入超时+1秒缓冲）
        
        if write_thread.is_alive():
            logging.warning("M%d 写入操作超时（已等待2秒），可能Modbus连接卡死", index)
            # 强制关闭连接，下次会自动重连
            with self._client_lock:
                if self._client:
                    try:
                        self._client.close()
                    except Exception:
                        pass
                    self._client = None
            return False
        
        if exception_container[0]:
            logging.error("M%d 写入异常: %s", index, exception_container[0])
            return False
        
        result = result_container[0]
        if result:
            logging.debug("M%d 写入成功", index)
        else:
            logging.error("M%d 写入失败", index)
        return result if result is not None else False

    def start_test_coil(self, address: int = 45, value: bool = True, interval: float = 0.005) -> None:
        """启动定时写入测试线圈（地址=45，值=True，周期=5ms）
        
        Args:
            address: 线圈地址，默认45
            value: 写入值，默认True
            interval: 写入周期（秒），默认0.005（5ms）
        """
        if self._test_coil_running:
            logging.warning("测试线圈定时写入已在运行")
            return
        
        self._test_coil_running = True
        logging.info("启动测试线圈定时写入：地址=%d, 值=%s, 周期=%.3f秒", address, value, interval)
        
        def write_test_coil_loop():
            next_run = time.perf_counter()
            while self._test_coil_running:
                try:
                    # 直接调用 _write_coils，不再追加读取验证
                    self._write_coils(address, [value])
                except Exception as e:
                    logging.warning("测试线圈写入异常: %s", e)
                next_run += interval
                sleep_time = next_run - time.perf_counter()
                if sleep_time > 0:
                    time.sleep(sleep_time)
                else:
                    # 线程调度被抢占时，立即进入下一次循环
                    next_run = time.perf_counter()
        
        self._test_coil_thread = threading.Thread(target=write_test_coil_loop, daemon=True)
        self._test_coil_thread.start()
    
    def stop_test_coil(self) -> None:
        """停止测试线圈定时写入"""
        if not self._test_coil_running:
            return
        
        self._test_coil_running = False
        if self._test_coil_thread and self._test_coil_thread.is_alive():
            self._test_coil_thread.join(timeout=1.0)
        self._test_coil_thread = None
        logging.info("测试线圈定时写入已停止")

    def stop(self) -> None:
        """关闭 Modbus 客户端（不管理 PLC 进程）。"""
        
        # 停止测试线圈定时写入
        self.stop_test_coil()

        with self._client_lock:
            if self._client:
                try:
                    self._client.close()
                except Exception:  # noqa: BLE001
                    pass
                self._client = None

        self._available = False

    # ------------------------------------------------------------------
    # 内部实现
    # ------------------------------------------------------------------

    def _ensure_client(self) -> bool:
        with self._client_lock:
            # 检查现有连接是否有效
            if self._client:
                # pymodbus 3.x 使用 is_socket_open() 或 connected 属性
                try:
                    if hasattr(self._client, 'is_socket_open') and self._client.is_socket_open():
                        return True
                    if hasattr(self._client, 'connected') and self._client.connected:
                        return True
                except Exception:  # noqa: BLE001
                    pass
                
                # 连接无效，关闭并重新创建
                try:
                    self._client.close()
                except Exception:  # noqa: BLE001
                    pass
                self._client = None

            try:
                # 创建 Modbus 客户端
                # pymodbus 3.x 的构造函数参数可能不同
                logging.info("创建 Modbus 客户端 (host=%s, port=%d, timeout=%.2f)", 
                            self._host, self._port, self._timeout)
                
                # 尝试不同的创建方式
                try:
                    # 方式1：使用 host 和 port 参数
                    client = ModbusTcpClient(  # type: ignore[call-arg]
                        host=self._host,
                        port=self._port,
                        timeout=self._timeout,
                    )
                except TypeError:
                    # 方式2：使用地址字符串
                    address = f"{self._host}:{self._port}"
                    client = ModbusTcpClient(  # type: ignore[call-arg]
                        address,
                        timeout=self._timeout,
                    )
                
                # 尝试连接
                logging.info("尝试连接 Modbus 服务器...")
                try:
                    connection_result = client.connect()
                except Exception as connect_exc:
                    logging.error("连接过程中出现异常: %s", connect_exc, exc_info=True)
                    connection_result = False
                
                if connection_result:
                    # 验证连接是否真的建立
                    try:
                        if hasattr(client, 'is_socket_open') and client.is_socket_open():
                            logging.info("Modbus 连接成功（socket 已打开）")
                        elif hasattr(client, 'connected') and client.connected:
                            logging.info("Modbus 连接成功（connected=True）")
                        else:
                            logging.warning("Modbus connect() 返回 True，但连接状态未知")
                    except Exception as e:  # noqa: BLE001
                        logging.warning("检查连接状态时出错: %s，但继续使用连接", e)
                    
                    # 设置读写超时
                    if hasattr(client, 'timeout'):
                        client.timeout = self._write_timeout
                        logging.debug("设置读写超时为 %.2f 秒", self._write_timeout)
                    
                    self._client = client
                    return True
                else:
                    logging.warning("Modbus connect() 返回 False")
                    try:
                        client.close()
                    except Exception:  # noqa: BLE001
                        pass
            except ModbusException as exc:
                logging.warning("连接 PLC Modbus 失败 (ModbusException): %s", exc)
            except Exception as exc:  # noqa: BLE001
                logging.error("初始化 Modbus 客户端异常: %s", exc, exc_info=True)

        return False

    def _write_yolo_coils(self, values: Iterable[bool]) -> bool:
        """写入 YOLO 标志位（M40-M42）
        
        根据 Modbus 资源展示：
        - M40-M45 对应 Modbus 地址 46-51 (0-based)
        - 我们只使用前三个：M40, M41, M42
        - 地址计算：6 (Q0-Q5) + 40 (M40) = 46
        """
        base_address = self._COIL_OUTPUT_COUNT + self._YOLO_MEMORY_START
        logging.debug("写入 YOLO 线圈，基地址=%d (M%d), 值=%s", 
                     base_address, self._YOLO_MEMORY_START, list(values))
        return self._write_coils(base_address, list(values))

    def _write_coils(self, address: int, values: Iterable[bool]) -> bool:
        """写入 Modbus 线圈，带超时和详细日志"""
        write_start = time.time()
        logging.info("开始写入线圈，地址=%d, 值=%s", address, list(values))
        
        # 先确保客户端连接（在锁外检查，避免长时间持有锁）
        if not self._ensure_client():
            logging.error("无法确保 Modbus 客户端连接")
            return False

        # 获取客户端引用（在锁内，快速操作）
        client = None
        with self._client_lock:
            client = self._client
            if client is None:
                logging.error("Modbus 客户端为 None")
                return False
            
            # 设置超时（在锁内快速设置）
            if hasattr(client, 'timeout'):
                client.timeout = self._write_timeout
                logging.debug("Modbus 客户端超时设置为 %.2f 秒", self._write_timeout)

        # 在独立锁内执行写入，避免多个线程同时操作同一个 socket
        try:
            with self._io_lock:
                logging.info("调用 write_coils，地址=%d, 值=%s, 单元ID=%d", address, list(values), self._unit_id)
                # pymodbus 3.x 版本中，write_coils 不再接受 unit 参数
                # unit ID 应该在创建客户端时设置，或通过 slave 参数传递
                # 尝试使用 slave 参数，如果不行则直接调用
                try:
                    response = client.write_coils(address, list(values), slave=self._unit_id)
                except TypeError:
                    # 如果 slave 参数也不支持，直接调用（使用默认 unit ID）
                    response = client.write_coils(address, list(values))
                write_time = time.time() - write_start
                logging.info("write_coils 调用完成，耗时 %.3f 秒", write_time)
                # 不再验证，只记录日志
                logging.info("线圈写入完成：地址=%d, 值=%s, 单元ID=%d", address, list(values), self._unit_id)
        except ModbusException as exc:
            write_time = time.time() - write_start
            logging.error("写入 PLC Modbus 线圈失败（耗时 %.3f 秒）: %s", write_time, exc)
            # 关闭客户端连接
            with self._client_lock:
                try:
                    if self._client:
                        self._client.close()
                except Exception:  # noqa: BLE001
                    pass
                self._client = None
            return False
        except Exception as exc:  # noqa: BLE001
            write_time = time.time() - write_start
            logging.error("写入 PLC Modbus 线圈出现异常（耗时 %.3f 秒）: %s", write_time, exc)
            # 关闭客户端连接
            with self._client_lock:
                try:
                    if self._client:
                        self._client.close()
                except Exception:  # noqa: BLE001
                    pass
                self._client = None
            return False

        # 检查响应
        if response.isError():  # type: ignore[has-type]
            total_time = time.time() - write_start
            logging.error("写入 PLC Modbus 线圈返回错误（总耗时 %.3f 秒）: %s", total_time, response)
            return False

        total_time = time.time() - write_start
        logging.info("线圈写入成功，总耗时 %.3f 秒", total_time)
        
        # 不再验证写入，只记录日志
        logging.info("调用 write_coils，地址=%d, 值=%s, 单元ID=%d", address, list(values), self._unit_id)
        
        return True

    # ------------------------------------------------------------------
    # 上下文管理
    # ------------------------------------------------------------------
    def __enter__(self) -> "PLCBridge":
        self.ensure_ready()
        return self

    def __exit__(self, exc_type, exc, tb) -> None:  # noqa: D401,ANN001
        self.stop()

    def __del__(self) -> None:  # noqa: D401
        try:
            self.stop()
        except Exception:  # noqa: BLE001
            pass


__all__ = ["PLCBridge"]




