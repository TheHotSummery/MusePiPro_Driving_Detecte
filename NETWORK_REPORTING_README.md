# 网络上报功能说明

## 功能概述

后端已经实现了完整的网络上报功能，包括定时心跳、GPS数据上报、事件数据上报等。现在这些功能已经与疲劳检测系统完全集成。

## 定时上报配置

### 配置文件：`network_config.json`

```json
{
  "timing": {
    "heartbeat_interval": 60,    // 心跳间隔：60秒
    "gps_interval": 20,          // GPS上报间隔：20秒
    "event_cooldown": 5,         // 事件上报冷却时间：5秒
    "retry_interval": 30         // 重试间隔：30秒
  }
}
```

## 上报功能

### 1. 定时心跳上报
- **间隔**：每60秒发送一次心跳
- **目的**：保持与服务器的连接状态
- **API**：`POST /api/v1/device/heartbeat`

### 2. 定时GPS数据上报
- **间隔**：每20秒上报一次GPS数据
- **内容**：位置信息、疲劳分数、注意力评分等
- **API**：`POST /api/v1/device/gps`

### 3. 事件触发上报
- **触发条件**：检测到疲劳行为时（最小间隔5秒）
- **内容**：行为数据、置信度、疲劳等级等
- **API**：`POST /api/v1/device/event`

### 4. 离线数据重发
- **间隔**：每30秒尝试重发离线数据
- **目的**：确保数据不丢失

## 集成状态

### ✅ 已集成的功能

1. **BehaviorAnalyzer集成**
   - 在疲劳等级变化时自动触发网络上报
   - 支持事件数据上报和GPS数据上报
   - 异步执行，不影响主检测流程

2. **VideoProcessor集成**
   - 传递NetworkManager到BehaviorAnalyzer
   - 确保网络上报与检测流程同步

3. **WebServer集成**
   - 初始化NetworkManager
   - 提供网络测试API接口

### 🔧 上报触发逻辑

```python
def _trigger_alerts(self, prev_level, current_time):
    """触发警报"""
    if self.progress_score >= 95 and prev_level != "Level 3":
        # 触发GPIO警报
        self.gpio_controller.trigger_level3_alert()
        
        # 触发网络上报
        if self.network_manager:
            self._trigger_network_report(current_time, "Level 3")
```

## 上报数据格式

### 事件数据上报
```json
{
  "behavior": "fatigue_detection",
  "confidence": 0.85,
  "progress_score": 75.0,
  "current_level": "Level 2",
  "distracted_count": 1,
  "timestamp": 1640995200.0
}
```

### GPS数据上报
```json
{
  "fatigue_score": 0.75,
  "eye_blink_rate": 0.45,
  "head_movement_score": 0.32,
  "yawn_count": 2,
  "attention_score": 0.78,
  "timestamp": 1640995200.0
}
```

## 测试方法

### 1. 启动后端服务
```bash
cd muse-hardware
python start.py
```

### 2. 观察日志输出
启动后应该能看到以下日志：
```
定时任务已启动
NetworkManager 初始化完成
```

### 3. 使用疲劳测试功能
1. 在前端启用测试模式
2. 设置疲劳分数触发警报
3. 观察后端日志中的网络上报信息

### 4. 检查网络状态
在前端GPIO控制面板中可以：
- 测试网络连接
- 查看网络状态
- 手动触发各种上报

## 日志示例

### 正常上报日志
```
INFO - 定时心跳发送成功
INFO - GPS数据上报成功
INFO - 网络上报已触发: 等级=Level 2, 分数=75.0
INFO - 事件数据上报成功
```

### 错误处理日志
```
WARNING - 定时心跳发送失败: 网络连接超时
ERROR - 网络上报触发失败: 模块未初始化
INFO - 数据已保存到离线队列
```

## 故障排除

### 1. 没有看到定时上报日志
- 检查NetworkManager是否正确初始化
- 确认串口模块是否正常工作
- 查看是否有网络连接问题

### 2. 上报失败
- 检查服务器地址是否正确
- 确认设备登录是否成功
- 查看网络连接状态

### 3. 离线模式
- 如果网络不可用，数据会自动保存到离线队列
- 网络恢复后会自动重发离线数据

## 配置说明

### 服务器配置
```json
{
  "server": {
    "base_url": "http://spacemit.topcoder.fun",
    "timeout": 30
  }
}
```

### 设备配置
```json
{
  "device": {
    "device_id": "A8888",
    "device_type": "Muse Pi Pro Plus",
    "version": "1.0.0",
    "username": "testuser",
    "password": "testpassword"
  }
}
```

### 串口配置
```json
{
  "serial": {
    "port": "/dev/ttyUSB0",
    "baudrate": 115200,
    "apn": "UNINET"
  }
}
```

## 注意事项

1. **网络依赖**：上报功能需要网络连接，离线时会自动缓存数据
2. **频率控制**：事件上报有5秒冷却时间，避免过于频繁
3. **异步执行**：网络上报在独立线程中执行，不影响检测性能
4. **错误处理**：网络错误时会自动重试和离线存储
5. **资源管理**：程序退出时会自动清理网络资源

现在网络上报功能已经完全集成到疲劳检测系统中，会在检测到疲劳行为时自动上报数据，同时保持定时心跳和GPS上报。
