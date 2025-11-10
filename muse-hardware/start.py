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
from config import PERFORMANCE_MONITOR_CONFIG, HARDWARE_WATCHDOG_CONFIG, NETWORK_MODULE_CONFIG
from performance_monitor import PerformanceMonitor
from hardware_watchdog import HardwareWatchdog

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

# 全局硬件看门狗和性能监控器实例
hardware_watchdog = None
performance_monitor = None

def signal_handler(signum, frame):
    """信号处理器，用于优雅退出"""
    print(f"\n 收到信号 {signum}，正在关闭程序...")
    
    try:
        # 停止硬件看门狗和性能监控
        global hardware_watchdog, performance_monitor
        if hardware_watchdog:
            hardware_watchdog.stop()
        if performance_monitor:
            performance_monitor.stop()
        
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
    global hardware_watchdog, performance_monitor  # 在函数开头声明全局变量
    
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
        
        # 初始化网络管理器（如果启用）
        if NETWORK_MODULE_CONFIG.get("enabled", False):
            print(" 正在初始化网络模块...")
            try:
                network_manager = web_server.network_manager
                if network_manager.initialize_module():
                    print(" 串口模块初始化成功")
                    logging.info("串口模块初始化成功")
                    
                    # 启动GNSS（如果未跳过）
                    if not NETWORK_MODULE_CONFIG.get("skip_gnss", False):
                        print(" 正在启动GNSS...")
                        if network_manager.start_gnss():
                            print(" GNSS启动成功")
                            logging.info("GNSS启动成功")
                        else:
                            print(" GNSS启动失败，将在后台继续尝试")
                            logging.warning("GNSS启动失败")
                    else:
                        print(" GNSS初始化已跳过")
                        logging.info("GNSS初始化已跳过")
                    
                    # 尝试NTP时间同步（如果未跳过）
                    if not NETWORK_MODULE_CONFIG.get("skip_ntp", False):
                        print(" 正在同步时间...")
                        success, message = network_manager.sync_time_with_ntp()
                        if success:
                            print(" 时间同步成功")
                            logging.info("时间同步成功")
                        else:
                            print(f" 时间同步失败: {message}")
                            logging.warning(f"时间同步失败: {message}")
                    else:
                        print(" NTP时间同步已跳过")
                        logging.info("NTP时间同步已跳过")
                    
                    # 尝试设备登录（如果未跳过）
                    if not NETWORK_MODULE_CONFIG.get("skip_login", False):
                        print(" 正在尝试设备登录...")
                        success, message = network_manager.device_login()
                        if success:
                            print(" 设备登录成功")
                            logging.info("设备登录成功")
                        else:
                            print(f" 设备登录失败: {message}")
                            logging.warning(f"设备登录失败: {message}")
                    else:
                        print(" 设备登录已跳过")
                        logging.info("设备登录已跳过")
                    
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
        else:
            print(" 网络模块已禁用（可通过 config.py 中的 NETWORK_MODULE_CONFIG 启用）")
            logging.info("网络模块已禁用")
            # 直接设置为离线模式
            try:
                network_manager = web_server.network_manager
                network_manager.set_offline_mode(True)
            except:
                pass
        
        # 设置信号处理器
        signal.signal(signal.SIGINT, signal_handler)
        signal.signal(signal.SIGTERM, signal_handler)
        print(" 信号处理器设置完成")
        
        # 初始化硬件看门狗喂狗（如果启用）
        # 注意：默认禁用，因为需要root权限，且可能触发系统重启
        if HARDWARE_WATCHDOG_CONFIG.get("enabled", False):
            try:
                hardware_watchdog = HardwareWatchdog(
                    feed_interval=HARDWARE_WATCHDOG_CONFIG.get("feed_interval", 10.0),
                    enabled=True
                )
                # 只有在enabled=True且初始化成功时才启动
                if hardware_watchdog.enabled:
                    hardware_watchdog.start()
                    print(" 硬件看门狗喂狗已启动")
                    logging.info("硬件看门狗喂狗已启动")
                else:
                    print(" 硬件看门狗喂狗初始化失败（权限不足或设备不可用），已自动禁用")
                    logging.info("硬件看门狗喂狗初始化失败，已自动禁用")
                    hardware_watchdog = None
            except Exception as e:
                print(f" 硬件看门狗喂狗启动失败: {e} (继续运行)")
                logging.warning(f"硬件看门狗喂狗启动失败: {e}")
                hardware_watchdog = None
        else:
            print(" 硬件看门狗喂狗已禁用（默认禁用，因为需要root权限）")
            logging.info("硬件看门狗喂狗已禁用")
        
        # 初始化性能监控（如果启用）
        if PERFORMANCE_MONITOR_CONFIG.get("enabled", False):
            try:
                performance_monitor = PerformanceMonitor(
                    log_dir=PERFORMANCE_MONITOR_CONFIG.get("log_dir", "performance_logs"),
                    interval=PERFORMANCE_MONITOR_CONFIG.get("interval", 5.0),
                    enabled=True
                )
                performance_monitor.start()
                print(" 性能监控已启动")
                logging.info("性能监控已启动")
            except Exception as e:
                print(f" 性能监控启动失败: {e} (继续运行)")
                logging.warning(f"性能监控启动失败: {e}")
                performance_monitor = None
        else:
            print(" 性能监控已禁用（可通过 config.py 中的 PERFORMANCE_MONITOR_CONFIG 启用）")
            logging.info("性能监控已禁用")
        
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
            # 停止硬件看门狗和性能监控（已在函数开头声明global，这里不需要再次声明）
            if hardware_watchdog:
                hardware_watchdog.stop()
            if performance_monitor:
                performance_monitor.stop()
            
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






