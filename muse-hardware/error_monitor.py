#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
错误监控和报告脚本
用于分析日志文件中的错误模式
"""

import re
import os
import sys
from datetime import datetime
from collections import defaultdict, Counter

class ErrorMonitor:
    """错误监控器"""
    
    def __init__(self, log_file="app_log.txt", debug_log_file="debug_log.txt"):
        self.log_file = log_file
        self.debug_log_file = debug_log_file
        self.error_patterns = defaultdict(int)
        self.error_timestamps = []
        self.threading_errors = []
        self.none_type_errors = []
        
    def analyze_logs(self):
        """分析日志文件"""
        print(" 开始分析日志文件...")
        
        # 分析主日志文件
        if os.path.exists(self.log_file):
            self._analyze_file(self.log_file)
        
        # 分析调试日志文件
        if os.path.exists(self.debug_log_file):
            self._analyze_file(self.debug_log_file)
        
        self._generate_report()
    
    def _analyze_file(self, filename):
        """分析单个日志文件"""
        print(f" 分析文件: {filename}")
        
        try:
            with open(filename, 'r', encoding='utf-8') as f:
                lines = f.readlines()
            
            for line_num, line in enumerate(lines, 1):
                self._analyze_line(line, filename, line_num)
                
        except Exception as e:
            print(f" 读取文件 {filename} 失败: {e}")
    
    def _analyze_line(self, line, filename, line_num):
        """分析单行日志"""
        # 检查错误行
        if "ERROR" in line or "CRITICAL" in line:
            self._extract_error_info(line, filename, line_num)
        
        # 检查特定错误类型
        if "name 'threading' is not defined" in line:
            self.threading_errors.append({
                'file': filename,
                'line': line_num,
                'content': line.strip(),
                'timestamp': self._extract_timestamp(line)
            })
        
        if "'NoneType' object is not subscriptable" in line:
            self.none_type_errors.append({
                'file': filename,
                'line': line_num,
                'content': line.strip(),
                'timestamp': self._extract_timestamp(line)
            })
    
    def _extract_error_info(self, line, filename, line_num):
        """提取错误信息"""
        # 提取时间戳
        timestamp = self._extract_timestamp(line)
        if timestamp:
            self.error_timestamps.append(timestamp)
        
        # 提取错误类型
        error_match = re.search(r'ERROR - (.+?)(?:\n|$)', line)
        if error_match:
            error_msg = error_match.group(1)
            self.error_patterns[error_msg] += 1
    
    def _extract_timestamp(self, line):
        """提取时间戳"""
        timestamp_match = re.search(r'(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})', line)
        if timestamp_match:
            try:
                return datetime.strptime(timestamp_match.group(1), '%Y-%m-%d %H:%M:%S')
            except:
                return None
        return None
    
    def _generate_report(self):
        """生成错误报告"""
        print("\n" + "="*60)
        print(" 错误分析报告")
        print("="*60)
        
        # 总体统计
        total_errors = sum(self.error_patterns.values())
        print(f" 总错误数量: {total_errors}")
        print(f" 错误时间范围: {len(self.error_timestamps)} 个时间点")
        
        # 错误类型统计
        print(f"\n 错误类型统计 (前10个):")
        for error_msg, count in Counter(self.error_patterns).most_common(10):
            print(f"   {count:3d} 次: {error_msg}")
        
        # Threading 错误详情
        if self.threading_errors:
            print(f"\n Threading 错误详情 ({len(self.threading_errors)} 次):")
            for i, error in enumerate(self.threading_errors[:5], 1):  # 只显示前5个
                print(f"   {i}. [{error['timestamp']}] {error['file']}:{error['line']}")
                print(f"      {error['content']}")
        
        # NoneType 错误详情
        if self.none_type_errors:
            print(f"\n NoneType 错误详情 ({len(self.none_type_errors)} 次):")
            for i, error in enumerate(self.none_type_errors[:5], 1):  # 只显示前5个
                print(f"   {i}. [{error['timestamp']}] {error['file']}:{error['line']}")
                print(f"      {error['content']}")
        
        # 建议
        self._generate_suggestions()
    
    def _generate_suggestions(self):
        """生成修复建议"""
        print(f"\n 修复建议:")
        
        if self.threading_errors:
            print("    Threading 错误:")
            print("      - 检查所有使用 threading.Thread 的地方是否已导入 threading 模块")
            print("      - 建议在文件顶部统一导入: import threading")
            print("      - 或者在使用前添加: import threading")
        
        if self.none_type_errors:
            print("    NoneType 错误:")
            print("      - 检查配置文件是否正确加载")
            print("      - 确保所有配置变量都已正确定义")
            print("      - 检查变量初始化顺序")
        
        if not self.threading_errors and not self.none_type_errors:
            print("   未发现已知的常见错误模式")
            print("    建议启用调试模式获取更详细的错误信息")
    
    def monitor_realtime(self):
        """实时监控日志"""
        print(" 开始实时监控日志...")
        print("按 Ctrl+C 停止监控")
        
        try:
            # 监控主日志文件
            if os.path.exists(self.log_file):
                self._tail_file(self.log_file)
        except KeyboardInterrupt:
            print("\n 监控已停止")

def main():
    """主函数"""
    print(" Muse Pi Pro Plus 错误监控器")
    print("="*50)
    
    monitor = ErrorMonitor()
    
    if len(sys.argv) > 1 and sys.argv[1] == "--monitor":
        monitor.monitor_realtime()
    else:
        monitor.analyze_logs()

if __name__ == "__main__":
    main()


