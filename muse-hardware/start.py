#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Muse Pi Pro Plus - 优化启动脚本
包含性能监控和更好的错误处理
"""

import time
import signal
import sys
import logging
import psutil
import os
from utils import setup_logging
from web_server import create_web_server

# 检查是否启用调试模式
DEBUG_MODE = "--debug" in sys.argv or "-d" in sys.argv

def print_startup_info():
    """打印启动信息"""
    print("=" * 60)
    print(" Muse Pi Pro Plus - 疲劳驾驶检测系统")
    print("=" * 60)
    print(f"Python版本: {sys.version}")
    print(f"工作目录: {os.getcwd()}")
    print(f"进程ID: {os.getpid()}")
    
    # 系统信息
    try:
        cpu_count = psutil.cpu_count()
        mem = psutil.virtual_memory()
        print(f"CPU核心数: {cpu_count}")
        print(f"内存总量: {mem.total / 1024**3:.1f} GB")
        print(f"可用内存: {mem.available / 1024**3:.1f} GB")
    except Exception as e:
        print(f"获取系统信息失败: {e}")
    
    print("=" * 60)

def signal_handler(signum, frame):
    """信号处理器，用于优雅退出"""
    print(f"\n 收到信号 {signum}，正在关闭程序...")
    
    try:
        # 使用全局变量来访问web_server
        from web_server import get_web_server
        web_server = get_web_server()
        if web_server:
            web_server.cleanup()
            print(" 资源清理完成")
    except Exception as e:
        print(f" 资源清理时出现错误: {e}")
    
    print(" 程序已退出")
    sys.exit(0)

def main():
    """主函数"""
    start_time = time.time()
    
    # 设置日志
    if DEBUG_MODE:
        print(" 启用调试模式...")
        from debug_config import setup_debug_logging
        setup_debug_logging()
    else:
        setup_logging()
    
    logging.info("Muse Pi Pro Plus 启动中...")
    if DEBUG_MODE:
        logging.info(" 调试模式已启用")
    
    # 打印启动信息
    print_startup_info()
    
    try:
        print(" 正在初始化系统组件...")
        
        # 创建Web服务器
        web_server = create_web_server()
        logging.info("Web服务器创建成功")
        print(" Web服务器创建成功")
        
        # 初始化GPIO并确保低电平状态
        print(" 正在初始化GPIO...")
        try:
            gpio_controller = web_server.gpio_controller
            if gpio_controller.force_init_and_reset():
                print(" GPIO初始化成功，所有引脚已设置为低电平")
                logging.info("GPIO初始化成功，所有引脚已设置为低电平")
            else:
                print(" GPIO初始化失败，GPIO功能将被禁用")
                logging.warning("GPIO初始化失败")
        except Exception as e:
            print(f" GPIO初始化异常: {e}")
            logging.error(f"GPIO初始化异常: {e}")
        
        # 初始化网络管理器
        print(" 正在初始化网络模块...")
        try:
            network_manager = web_server.network_manager
            if network_manager.initialize_module():
                print(" 串口模块初始化成功")
                logging.info("串口模块初始化成功")
                
                # 启动GNSS
                print(" 正在启动GNSS...")
                if network_manager.start_gnss():
                    print(" GNSS启动成功")
                    logging.info("GNSS启动成功")
                else:
                    print(" GNSS启动失败，将在后台继续尝试")
                    logging.warning("GNSS启动失败")
                
                # 尝试NTP时间同步
                print(" 正在同步时间...")
                success, message = network_manager.sync_time_with_ntp()
                if success:
                    print(" 时间同步成功")
                    logging.info("时间同步成功")
                else:
                    print(f" 时间同步失败: {message}")
                    logging.warning(f"时间同步失败: {message}")
                
                # 尝试设备登录
                print(" 正在尝试设备登录...")
                success, message = network_manager.device_login()
                if success:
                    print(" 设备登录成功")
                    logging.info("设备登录成功")
                else:
                    print(f" 设备登录失败: {message}")
                    logging.warning(f"设备登录失败: {message}")
                
            else:
                print(" 串口模块初始化失败，启用离线模式")
                print(" 疲劳驾驶监测功能将正常运行，但数据将仅本地缓存")
                logging.warning("串口模块初始化失败，启用离线模式")
                # 设置网络管理器为离线模式
                network_manager.set_offline_mode(True)
        except Exception as e:
            print(f" 网络模块初始化异常: {e}")
            print(" 疲劳驾驶监测功能将正常运行，但数据将仅本地缓存")
            logging.error(f"网络模块初始化异常: {e}")
            # 设置网络管理器为离线模式
            try:
                network_manager.set_offline_mode(True)
            except:
                pass
        
        # 设置信号处理器
        signal.signal(signal.SIGINT, signal_handler)
        signal.signal(signal.SIGTERM, signal_handler)
        print(" 信号处理器设置完成")
        
        # 计算启动时间
        init_time = time.time() - start_time
        print(f" 系统初始化耗时: {init_time:.2f} 秒")
        
        # 启动Web服务器
        print(" 正在启动Web服务器...")
        logging.info("正在启动Web服务器...")
        web_server.run()
        
    except KeyboardInterrupt:
        print("\n 收到键盘中断，正在关闭...")
        logging.info("收到键盘中断，正在关闭...")
    except Exception as e:
        print(f"\n 程序运行错误: {e}")
        logging.error(f"程序运行错误: {e}")
        raise
    finally:
        try:
            if 'web_server' in locals():
                web_server.cleanup()
                print(" 资源清理完成")
        except Exception as e:
            print(f" 资源清理时出现错误: {e}")
        
        total_time = time.time() - start_time
        print(f" 程序总运行时间: {total_time:.2f} 秒")
        logging.info("程序已退出")

if __name__ == "__main__":
    main()






