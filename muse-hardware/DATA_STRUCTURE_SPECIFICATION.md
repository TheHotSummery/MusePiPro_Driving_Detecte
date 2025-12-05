# 硬件数据格式规范

## 数据收集概览

本文档定义了硬件端收集和上报的数据结构，用于前后端联动、后台管理和数据展示大屏。

**设计原则：**
- 数据精简，减少4G流量消耗
- 数据有价值，支持业务分析
- 结构清晰，便于后端处理
- 预留扩展，支持未来功能

---

##  数据上报策略

### 上报频率
- **实时事件上报**：疲劳等级变化时立即上报（事件驱动）
- **定期数据上报**：每30秒上报一次状态数据（心跳+状态）
- **GPS数据上报**：每20秒更新一次GPS位置（与现有逻辑一致）

### 数据压缩
- 使用JSON格式，去除冗余字段
- 数值精度控制在合理范围（减少数据量）
- 布尔值使用0/1代替true/false（可选优化）

---

##  核心数据结构

### 1. 实时事件数据（Event Data）
**触发时机**：疲劳等级变化时

```json
{
  "eventId": "DEVICE_001_1704067200_fatigue",
  "timestamp": 1704067200,
  "eventType": "FATIGUE",  // FATIGUE | DISTRACTION | EMERGENCY
  "severity": "HIGH",       // CRITICAL | HIGH | MEDIUM | LOW
  "alertLevel": "Level 2",  // Normal | Level 1 | Level 2 | Level 3
  
  // 核心疲劳数据（精简）
  "fatigue": {
    "score": 82.5,          // 疲劳分数 0-100
    "level": "Level 2",     // 当前等级
    "behavior": "eyes_closed",  // 检测到的行为
    "confidence": 0.85,     // 置信度 0-1
    "distractedCount": 5    // 累计分心次数
  },
  
  // 位置信息（精简）
  "location": {
    "lat": 33.553485,
    "lng": 119.030977,
    "realtime": true,       // 是否为实时GPS
    "speed": 0.0            // 速度 km/h（可选）
  },
  
  // 设备状态（精简）
  "device": {
    "id": "DEVICE_001",
    "status": "online"      // online | offline | error
  }
}
```

**数据量估算**：约 200-300 字节/次

---

### 2. 定期状态数据（Status Data）
**上报频率**：每30秒

```json
{
  "timestamp": 1704067200,
  "deviceId": "DEVICE_001",
  
  // 疲劳状态（核心数据）
  "fatigue": {
    "score": 75.5,          // 当前疲劳分数
    "level": "Level 1",     // 当前等级
    "distractedCount": 12,  // 累计分心次数
    "currentBehavior": "focused"  // 当前主要行为
  },
  
  // 位置信息
  "location": {
    "lat": 33.553485,
    "lng": 119.030977,
    "realtime": true,
    "speed": 45.2,          // 速度 km/h
    "heading": 180.5        // 航向角度（度）
  },
  
  // 传感器数据（预留MPU6050）
  "sensors": {
    "mpu6050": {
      "accel": {
        "x": 0.12,          // 加速度 X轴 (g)
        "y": -0.05,         // 加速度 Y轴 (g)
        "z": 0.98           // 加速度 Z轴 (g)
      },
      "gyro": {
        "x": 0.02,          // 角速度 X轴 (deg/s)
        "y": -0.01,         // 角速度 Y轴 (deg/s)
        "z": 0.03           // 角速度 Z轴 (deg/s)
      },
      "temperature": 25.5,  // MPU6050内部温度（℃）
      "pressure": null      // 气压（hPa，预留，需要额外传感器）
    }
  },
  
  // 系统状态（精简，仅关键指标）
  "system": {
    "cpu": 45.2,            // CPU使用率 %
    "memory": 68.5,         // 内存使用率 %
    "temperature": 55.0,    // 系统温度 ℃
    "fps": 4.2              // 视频处理帧率
  },
  
  // 设备状态
  "device": {
    "status": "online",
    "uptime": 3600          // 运行时长（秒）
  }
}
```

**数据量估算**：约 400-500 字节/次

---

### 3. GPS轨迹数据（GPS Track Data）
**上报频率**：每20秒（与现有逻辑一致）

```json
{
  "timestamp": 1704067200,
  "deviceId": "DEVICE_001",
  "location": {
    "lat": 33.553485,
    "lng": 119.030977,
    "altitude": 50.5,       // 海拔高度（米）
    "speed": 45.2,          // 速度 km/h
    "heading": 180.5,       // 航向角度（度）
    "realtime": true,       // 是否为实时GPS
    "satellites": 8         // 卫星数量
  },
  "fatigue": {
    "score": 75.5           // 当前疲劳分数（用于轨迹分析）
  }
}
```

**数据量估算**：约 200-250 字节/次

---

##  数据字段说明

### 疲劳相关字段

| 字段 | 类型 | 说明 | 取值范围 |
|------|------|------|----------|
| `fatigue.score` | float | 疲劳分数 | 0-100 |
| `fatigue.level` | string | 疲劳等级 | Normal/Level 1/Level 2/Level 3 |
| `fatigue.behavior` | string | 检测行为 | focused/eyes_closed/yarning/head_down等 |
| `fatigue.confidence` | float | 置信度 | 0-1 |
| `fatigue.distractedCount` | int | 累计分心次数 | >= 0 |

### 位置相关字段

| 字段 | 类型 | 说明 | 精度 |
|------|------|------|------|
| `location.lat` | float | 纬度 | 6位小数 |
| `location.lng` | float | 经度 | 6位小数 |
| `location.altitude` | float | 海拔高度（米） | 1位小数 |
| `location.speed` | float | 速度（km/h） | 1位小数 |
| `location.heading` | float | 航向角度（度） | 1位小数 |
| `location.realtime` | bool | 是否实时GPS | true/false |
| `location.satellites` | int | 卫星数量 | 0-99 |

### MPU6050传感器字段（预留）

| 字段 | 类型 | 说明 | 单位 |
|------|------|------|------|
| `sensors.mpu6050.accel.x/y/z` | float | 三轴加速度 | g (重力加速度) |
| `sensors.mpu6050.gyro.x/y/z` | float | 三轴角速度 | deg/s (度/秒) |
| `sensors.mpu6050.temperature` | float | 传感器温度 | ℃ |
| `sensors.mpu6050.pressure` | float | 气压（预留） | hPa |

**注意**：气压数据需要额外传感器（如BMP280）

### 系统状态字段

| 字段 | 类型 | 说明 | 精度 |
|------|------|------|------|
| `system.cpu` | float | CPU使用率 | 1位小数 |
| `system.memory` | float | 内存使用率 | 1位小数 |
| `system.temperature` | float | 系统温度 | 1位小数 |
| `system.fps` | float | 视频处理帧率 | 1位小数 |

---

##  MPU6050集成预留

### 数据结构
已在 `sensors.mpu6050` 中预留完整的三维状态数据字段：
- 加速度（accel）：x, y, z
- 角速度（gyro）：x, y, z
- 温度：传感器内部温度
- 气压：预留字段（需要额外传感器）

### 实现建议
1. **创建MPU6050驱动模块**：`mpu6050_sensor.py`
2. **数据采集频率**：建议10-20Hz（每50-100ms采集一次）
3. **数据上报频率**：与状态数据同步（每30秒上报一次）
4. **数据平滑**：使用滑动平均或低通滤波，减少噪声

### 应用场景
- **三维状态展示**：实时显示车辆姿态（俯仰、横滚、偏航）
- **急刹车检测**：通过加速度突变检测
- **急转弯检测**：通过角速度检测
- **碰撞检测**：通过加速度阈值检测
- **气压高度**：结合气压传感器计算相对高度变化

---

## 数据流量估算

### 单设备流量计算

| 数据类型 | 频率 | 单次大小 | 每小时流量 | 每天流量 |
|---------|------|----------|-----------|----------|
| 事件数据 | 变化时 | 300字节 | ~50KB | ~1.2MB |
| 状态数据 | 30秒/次 | 500字节 | 60KB | 1.44MB |
| GPS数据 | 20秒/次 | 250字节 | 45KB | 1.08MB |
| **总计** | - | - | **~155KB/h** | **~3.7MB/天** |

**结论**：单设备每天约消耗 **3.7MB** 流量，对于4G套餐来说非常合理。

---

##  数据价值分析

### 1. 疲劳检测数据
- **实时监控**：实时了解驾驶员疲劳状态
- **趋势分析**：分析疲劳分数变化趋势
- **预警管理**：根据等级变化及时预警
- **行为分析**：统计各类疲劳行为发生频率

### 2. 位置数据
- **轨迹回放**：查看车辆行驶轨迹
- **区域分析**：分析不同区域的疲劳发生情况
- **速度监控**：监控车辆行驶速度
- **路线优化**：分析疲劳高发路段

### 3. MPU6050数据（暂未实现）
- **姿态监控**：实时显示车辆三维姿态
- **异常检测**：检测急刹车、急转弯等异常驾驶行为
- **安全评估**：结合疲劳数据评估驾驶安全
- **数据可视化**：三维姿态展示，提升大屏视觉效果

### 4. 系统状态数据
- **设备健康**：监控设备运行状态
- **性能优化**：分析系统性能瓶颈
- **故障预警**：提前发现设备异常
- **资源管理**：优化资源使用

---

##  数据上报API设计

### 1. 事件上报接口
```
POST /api/v1/data/event
Content-Type: application/json

Body: 实时事件数据（Event Data）
```

### 2. 状态上报接口
```
POST /api/v1/data/status
Content-Type: application/json

Body: 定期状态数据（Status Data）
```

### 3. GPS轨迹上报接口
```
POST /api/v1/data/gps
Content-Type: application/json

Body: GPS轨迹数据（GPS Track Data）
```

---

##  实现

### 1. 数据收集模块
- 创建统一的数据收集器：`data_collector.py`
- 整合所有数据源：疲劳检测、GPS、MPU6050、系统状态
- 实现数据缓存和批量上报

### 2. MPU6050驱动
- 创建 `mpu6050_sensor.py` 模块
- 使用 I2C 通信协议
- 实现数据采集和滤波

### 3. 数据上报优化
- 实现数据压缩（可选）
- 实现断线重连和离线缓存
- 实现数据优先级（事件数据优先）

### 4. 后端接口对接
- 与后端API对接
- 实现数据格式验证
- 实现错误处理和重试机制

---









