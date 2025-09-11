# -*- coding: utf-8 -*-
"""
调试配置文件
用于开发阶段启用详细的调试日志
"""

import logging
import sys

def setup_debug_logging():
    """设置调试级别的日志"""
    
    # 创建自定义的调试日志格式
    debug_format = (
        "%(asctime)s - [%(levelname)s] - [%(filename)s:%(lineno)d] - [%(funcName)s] - %(message)s"
    )
    
    # 配置根日志器
    logging.basicConfig(
        level=logging.DEBUG,
        format=debug_format,
        handlers=[
            logging.StreamHandler(sys.stdout),  # 输出到控制台
            logging.FileHandler("debug_log.txt", mode='a', encoding='utf-8')  # 输出到文件
        ]
    )
    
    # 设置特定模块的日志级别
    logging.getLogger('flask').setLevel(logging.WARNING)
    logging.getLogger('werkzeug').setLevel(logging.WARNING)
    logging.getLogger('socketio').setLevel(logging.WARNING)
    logging.getLogger('engineio').setLevel(logging.WARNING)
    
    # 启用我们关心的模块的调试日志
    logging.getLogger('__main__').setLevel(logging.DEBUG)
    
    print("🔍 调试日志已启用")
    print("📝 调试日志将同时输出到控制台和 debug_log.txt 文件")
    print("⚠️  注意：调试模式会显著增加日志输出量，仅用于开发阶段")

def log_function_entry(func_name, **kwargs):
    """记录函数进入"""
    logging.debug(f"🚀 进入函数: {func_name}")
    if kwargs:
        for key, value in kwargs.items():
            logging.debug(f"   📋 {key}: {value}")

def log_function_exit(func_name, result=None):
    """记录函数退出"""
    if result is not None:
        logging.debug(f"✅ 退出函数: {func_name}, 结果: {result}")
    else:
        logging.debug(f"✅ 退出函数: {func_name}")

def log_error_with_context(error, context=None):
    """记录带上下文的错误"""
    import traceback
    error_traceback = traceback.format_exc()
    
    logging.error(f"❌ 错误: {error}")
    logging.error(f"📍 错误堆栈: {error_traceback}")
    
    if context:
        logging.error(f"🔍 错误上下文: {context}")

def log_detection_info(detections, stage=""):
    """记录检测信息的调试日志"""
    if detections:
        logging.debug(f"🎯 {stage}检测结果: 数量={len(detections)}")
        for i, detection in enumerate(detections):
            logging.debug(f"   [{i}] {detection.get('label', 'unknown')}: {detection.get('confidence', 0):.3f}")
    else:
        logging.debug(f"🎯 {stage}检测结果: 无检测")

def log_performance_metrics(metrics):
    """记录性能指标的调试日志"""
    logging.debug("⚡ 性能指标:")
    for key, value in metrics.items():
        logging.debug(f"   📊 {key}: {value}")

# 装饰器：自动记录函数调用
def debug_log(func):
    """调试日志装饰器"""
    def wrapper(*args, **kwargs):
        func_name = func.__name__
        log_function_entry(func_name, args=args, kwargs=kwargs)
        try:
            result = func(*args, **kwargs)
            log_function_exit(func_name, result=result)
            return result
        except Exception as e:
            log_error_with_context(e, context=f"函数: {func_name}")
            raise
    return wrapper

if __name__ == "__main__":
    # 测试调试配置
    setup_debug_logging()
    
    @debug_log
    def test_function(x, y):
        return x + y
    
    test_function(1, 2)
    
    log_detection_info([
        {"label": "focused", "confidence": 0.95},
        {"label": "eyes_closed", "confidence": 0.87}
    ], "测试")
    
    log_performance_metrics({
        "推理时间": "123ms",
        "CPU使用率": "45%",
        "内存使用": "2.1GB"
    })


