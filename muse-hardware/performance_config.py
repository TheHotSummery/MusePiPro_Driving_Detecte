# -*- coding: utf-8 -*-
"""
性能优化配置
包含启动优化和运行时优化的配置选项
"""

# 启动优化配置
STARTUP_OPTIMIZATION = {
    "lazy_model_loading": True,      # 延迟模型加载
    "lazy_gpio_init": True,          # 延迟GPIO初始化
    "minimal_logging": True,          # 启动时最小化日志
    "async_initialization": True,     # 异步初始化组件
    "preload_models": False,          # 预加载模型（如果设为True，会提高首次推理速度但增加启动时间）
}

# 模型优化配置
MODEL_OPTIMIZATION = {
    "onnx_optimization_level": 1,    # ONNX优化级别 (0-2)
    "cpu_threads": 4,                # CPU线程数
    "memory_pool_size": 1024*1024*100,  # 内存池大小 (100MB)
    "enable_profiling": False,       # 启用性能分析
}

# 运行时优化配置
RUNTIME_OPTIMIZATION = {
    "frame_skip": 1,                 # 帧跳过（1表示处理每一帧）
    "batch_processing": False,        # 批处理模式
    "memory_cleanup_interval": 100,   # 内存清理间隔（帧数）
    "gpu_memory_fraction": 0.8,      # GPU内存使用比例
}

# 缓存配置
CACHE_CONFIG = {
    "enable_model_cache": True,       # 启用模型缓存
    "enable_result_cache": False,     # 启用结果缓存
    "cache_size": 1000,              # 缓存大小
    "cache_ttl": 300,                # 缓存生存时间（秒）
}

# 监控配置
MONITORING_CONFIG = {
    "enable_performance_monitoring": True,  # 启用性能监控
    "monitoring_interval": 5.0,      # 监控间隔（秒）
    "log_performance_metrics": True,  # 记录性能指标
    "alert_on_high_memory": True,    # 内存使用过高时报警
    "memory_threshold": 80.0,        # 内存使用阈值（百分比）
}






