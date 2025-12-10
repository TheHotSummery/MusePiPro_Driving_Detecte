#!/usr/bin/env python3
"""
PLC C++接口的Python包装器
用于与C++ PLC核心进程通信
"""

import ctypes
import os
import sys
from pathlib import Path

class PLCInterface:
    """PLC C++接口的Python包装器"""
    
    def __init__(self, lib_path=None):
        """
        初始化PLC接口
        
        Args:
            lib_path: C++共享库路径，如果为None则自动查找
        """
        if lib_path is None:
            # 自动查找共享库
            current_dir = Path(__file__).parent
            lib_path = current_dir / "build" / "lib" / "libplc_interface.so"
            
            if not lib_path.exists():
                # 尝试其他可能的路径
                lib_path = current_dir / "libplc_interface.so"
        
        if not os.path.exists(lib_path):
            raise FileNotFoundError(f"PLC接口库未找到: {lib_path}")
        
        # 加载共享库
        self.lib = ctypes.CDLL(str(lib_path))
        
        # 定义函数签名
        self._setup_function_signatures()
        
        # 初始化接口
        if not self.lib.plc_interface_init():
            raise RuntimeError("PLC接口初始化失败")
        
        print("PLC Python接口初始化成功")
    
    def _setup_function_signatures(self):
        """设置C函数签名"""
        # bool plc_interface_init()
        self.lib.plc_interface_init.restype = ctypes.c_bool
        
        # void plc_interface_cleanup()
        self.lib.plc_interface_cleanup.restype = None
        
        # bool plc_set_yolo_flag(int level, bool value)
        self.lib.plc_set_yolo_flag.argtypes = [ctypes.c_int, ctypes.c_bool]
        self.lib.plc_set_yolo_flag.restype = ctypes.c_bool
        
        # bool plc_get_output_status(const char* output_name)
        self.lib.plc_get_output_status.argtypes = [ctypes.c_char_p]
        self.lib.plc_get_output_status.restype = ctypes.c_bool
        
        # void plc_get_all_outputs(bool* outputs, int size)
        self.lib.plc_get_all_outputs.argtypes = [ctypes.POINTER(ctypes.c_bool), ctypes.c_int]
        self.lib.plc_get_all_outputs.restype = None
        
        # void plc_get_memory_range(int start, int end, bool* memory, int size)
        self.lib.plc_get_memory_range.argtypes = [
            ctypes.c_int, ctypes.c_int, 
            ctypes.POINTER(ctypes.c_bool), ctypes.c_int
        ]
        self.lib.plc_get_memory_range.restype = None
        
        # double plc_get_scan_time()
        self.lib.plc_get_scan_time.restype = ctypes.c_double
        
        # uint64_t plc_get_scan_count()
        self.lib.plc_get_scan_count.restype = ctypes.c_uint64
        
        # uint32_t plc_get_error_code()
        self.lib.plc_get_error_code.restype = ctypes.c_uint32
        
        # bool plc_is_running()
        self.lib.plc_is_running.restype = ctypes.c_bool
        
        # void plc_emergency_stop()
        self.lib.plc_emergency_stop.restype = None
        
        # void plc_clear_emergency_stop()
        self.lib.plc_clear_emergency_stop.restype = None
        
        # bool plc_is_emergency_stopped()
        self.lib.plc_is_emergency_stopped.restype = ctypes.c_bool
    
    def __del__(self):
        """析构函数，清理资源"""
        if hasattr(self, 'lib'):
            self.lib.plc_interface_cleanup()
    
    def set_yolo_flag(self, level, value):
        """
        设置YOLO标志位
        
        Args:
            level: YOLO级别 (1-10)
            value: 标志值
            
        Returns:
            bool: 是否设置成功
        """
        if not (1 <= level <= 10):
            raise ValueError(f"YOLO level超出范围: {level}")
        
        return self.lib.plc_set_yolo_flag(level, bool(value))
    
    def get_output_status(self, output_name):
        """
        获取输出状态
        
        Args:
            output_name: 输出名称 (如 "Q0", "Q1" 等)
            
        Returns:
            bool: 输出状态
        """
        return self.lib.plc_get_output_status(output_name.encode('utf-8'))
    
    def get_all_outputs(self):
        """
        获取所有输出状态
        
        Returns:
            list: 6个输出的状态列表
        """
        outputs = (ctypes.c_bool * 6)()
        self.lib.plc_get_all_outputs(outputs, 6)
        return list(outputs)
    
    def get_memory_range(self, start, end):
        """
        获取中间继电器状态范围
        
        Args:
            start: 起始索引
            end: 结束索引
            
        Returns:
            list: 中间继电器状态列表
        """
        if start < 0 or end >= 37 or start > end:
            raise ValueError(f"内存范围无效: {start}-{end}")
        
        size = end - start + 1
        memory = (ctypes.c_bool * size)()
        self.lib.plc_get_memory_range(start, end, memory, size)
        return list(memory)
    
    def get_scan_time(self):
        """
        获取扫描时间
        
        Returns:
            float: 扫描时间（毫秒）
        """
        return self.lib.plc_get_scan_time()
    
    def get_scan_count(self):
        """
        获取扫描计数
        
        Returns:
            int: 扫描计数
        """
        return self.lib.plc_get_scan_count()
    
    def get_error_code(self):
        """
        获取错误码
        
        Returns:
            int: 错误码
        """
        return self.lib.plc_get_error_code()
    
    def is_running(self):
        """
        检查PLC是否运行中
        
        Returns:
            bool: 是否运行中
        """
        return self.lib.plc_is_running()
    
    def emergency_stop(self):
        """紧急停止"""
        self.lib.plc_emergency_stop()
    
    def clear_emergency_stop(self):
        """清除紧急停止"""
        self.lib.plc_clear_emergency_stop()
    
    def is_emergency_stopped(self):
        """
        检查是否紧急停止
        
        Returns:
            bool: 是否紧急停止
        """
        return self.lib.plc_is_emergency_stopped()


# 测试函数
def test_plc_interface():
    """测试PLC接口"""
    try:
        # 创建接口实例
        plc = PLCInterface()
        
        print("=== PLC接口测试 ===")
        
        # 测试基本状态
        print(f"PLC运行状态: {plc.is_running()}")
        print(f"扫描时间: {plc.get_scan_time():.2f}ms")
        print(f"扫描计数: {plc.get_scan_count()}")
        print(f"错误码: {plc.get_error_code()}")
        
        # 测试输出状态
        outputs = plc.get_all_outputs()
        print(f"输出状态: {outputs}")
        
        # 测试中间继电器
        memory = plc.get_memory_range(0, 5)
        print(f"中间继电器M0-M5: {memory}")
        
        # 测试YOLO标志设置
        print("设置YOLO标志...")
        for i in range(1, 6):
            success = plc.set_yolo_flag(i, i % 2 == 0)
            print(f"YOLO标志Y{i}: {success}")
        
        # 检查YOLO标志是否生效
        yolo_memory = plc.get_memory_range(21, 25)  # M21-M25
        print(f"YOLO中间继电器M21-M25: {yolo_memory}")
        
        print("=== 测试完成 ===")
        
    except Exception as e:
        print(f"测试失败: {e}")
        return False
    
    return True


if __name__ == "__main__":
    test_plc_interface()


