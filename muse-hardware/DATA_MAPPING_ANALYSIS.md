# 数据映射关系核对分析文档

## 文档说明

本文档核对 `HARDWARE_DATA_API_SPECIFICATION-确定稿.md` 中硬件上传的数据，是否能够支撑 `DATA_DISPLAY_SPECIFICATION-确定稿.md` 中大屏展示的所有数据需求。

**目的**：确保后端重构时，数据库设计和数据处理逻辑能够完整支撑大屏展示需求。

---

## ✓ 1. 数据源分析

### 1.1 硬件上传的数据类型

根据 `HARDWARE_DATA_API_SPECIFICATION-确定稿.md`，硬件端上传三种数据类型：

#### 事件数据 (event)
```json
{
  "dataType": "event",
  "timestamp": 1705123456789,
  "data": {
    "eventId": "MUSE_PI_PRO_001_1705123456789_001",
    "level": "Level 2",
    "score": 75.5,
    "behavior": "eyes_closed",
    "confidence": 0.85,
    "duration": 2.3,
    "locationLat": 33.553485,
    "locationLng": 119.030977,
    "distractedCount": 7
  }
}
```

#### 状态数据 (status)
```json
{
  "dataType": "status",
  "timestamp": 1705123456789,
  "data": {
    "level": "Normal",
    "score": 45.2,
    "locationLat": 33.553485,
    "locationLng": 119.030977,
    "cpuUsage": 45.2,
    "memoryUsage": 68.5,
    "temperature": 42.1
  }
}
```

#### GPS数据 (gps)
```json
{
  "dataType": "gps",
  "timestamp": 1705123456789,
  "data": {
    "locationLat": 33.553485,
    "locationLng": 119.030977,
    "speed": 60.5,
    "direction": 180.0,
    "altitude": 44.4,
    "satellites": 8
  }
}
```

---

##  2. 大屏数据需求核对

### 2.1 实时车辆状态 ✓ 可支撑

**大屏需求**：
```json
{
  "deviceId": "MUSE_PI_PRO_001",
  "driverId": "DRIVER_001",
  "driverName": "张三",
  "status": "online",
  "currentLevel": "Level 1",
  "currentScore": 65.5,
  "location": {
    "lat": 33.553485,
    "lng": 119.030977,
    "speed": 60.5,
    "heading": 180.0
  },
  "lastUpdateTime": 1705123456789,
  "uptime": 3600
}
```

**数据来源分析**：
| 字段 | 来源 | 说明 |
|------|------|------|
| `deviceId` | ✓ Token解析 | 从Token中获取设备ID |
| `driverId` | ⚠️ 需要关联 | 需要设备-驾驶员绑定表 |
| `driverName` | ⚠️ 需要关联 | 从驾驶员表获取 |
| `status` | ✓ 计算 | 根据最后上报时间判断（如30秒内为online） |
| `currentLevel` | ✓ status数据 | 从最新的status数据获取 |
| `currentScore` | ✓ status数据 | 从最新的status数据获取 |
| `location.lat/lng` | ✓ status/gps数据 | 从最新的status或gps数据获取 |
| `location.speed` | ✓ gps数据 | 从最新的gps数据获取 |
| `location.heading` | ✓ gps数据 | 从gps数据的direction字段 |
| `lastUpdateTime` | ✓ 所有数据 | 最新数据的时间戳 |
| `uptime` | ⚠️ 需要计算 | 需要记录设备启动时间或首次上报时间 |

**结论**：✓ **可支撑**，但需要：
1. 设备-驾驶员绑定关系（需要单独管理）
2. 驾驶员信息表（需要单独管理）
3. 设备在线状态判断逻辑（基于最后上报时间）

---

### 2.2 实时告警事件 ✓ 可支撑

**大屏需求**：
```json
{
  "alertId": "ALERT_001",
  "deviceId": "MUSE_PI_PRO_001",
  "driverId": "DRIVER_001",
  "driverName": "张三",
  "level": "Level 2",
  "score": 82.5,
  "behavior": "eyes_closed",
  "location": {
    "lat": 33.553485,
    "lng": 119.030977,
    "address": "江苏省南京市建邺区XX路"
  },
  "timestamp": 1705123456789,
  "duration": 5.2,
  "status": "active"
}
```

**数据来源分析**：
| 字段 | 来源 | 说明 |
|------|------|------|
| `alertId` | ✓ event数据 | 使用eventId |
| `deviceId` | ✓ Token解析 | 从Token中获取 |
| `driverId` | ⚠️ 需要关联 | 从设备-驾驶员绑定表获取 |
| `driverName` | ⚠️ 需要关联 | 从驾驶员表获取 |
| `level` | ✓ event数据 | 直接使用 |
| `score` | ✓ event数据 | 直接使用 |
| `behavior` | ✓ event数据 | 直接使用 |
| `location.lat/lng` | ✓ event数据 | 直接使用 |
| `location.address` | ⚠️ 需要逆地理编码 | 通过GPS坐标调用地图API获取地址 |
| `timestamp` | ✓ event数据 | 直接使用 |
| `duration` | ✓ event数据 | 直接使用 |
| `status` | ✓ 计算 | 根据时间判断（如30分钟内为active） |

**结论**：✓ **可支撑**，但需要：
1. 设备-驾驶员绑定关系
2. 逆地理编码服务（将GPS坐标转换为地址）

---

### 2.3 系统运行状态 ✓ 可支撑

**大屏需求**：
```json
{
  "deviceId": "MUSE_PI_PRO_001",
  "status": "healthy",
  "cpuUsage": 45.2,
  "memoryUsage": 68.5,
  "temperature": 55.0,
  "networkStatus": "online",
  "lastHeartbeat": 1705123456789
}
```

**数据来源分析**：
| 字段 | 来源 | 说明 |
|------|------|------|
| `deviceId` | ✓ Token解析 | 从Token中获取 |
| `status` | ✓ 计算 | 根据cpuUsage、memoryUsage、temperature判断健康状态 |
| `cpuUsage` | ✓ status数据 | 直接使用 |
| `memoryUsage` | ✓ status数据 | 直接使用 |
| `temperature` | ✓ status数据 | 直接使用 |
| `networkStatus` | ✓ 计算 | 根据最后上报时间判断（如30秒内为online） |
| `lastHeartbeat` | ✓ 所有数据 | 最新数据的时间戳 |

**结论**：✓ **可支撑**，需要定义健康状态判断规则。

---

### 2.4 疲劳事件统计 ✓ 可支撑

**大屏需求**：
```json
{
  "summary": {
    "totalEvents": 1250,
    "criticalEvents": 50,
    "highEvents": 300,
    "mediumEvents": 600,
    "lowEvents": 300
  },
  "byLevel": {
    "Level 3": 50,
    "Level 2": 300,
    "Level 1": 600,
    "Normal": 300
  },
  "byType": {
    "FATIGUE": 800,
    "DISTRACTION": 400,
    "EMERGENCY": 50
  },
  "byBehavior": {
    "eyes_closed": 400,
    "yarning": 200,
    "head_down": 300,
    "seeing_left": 150,
    "seeing_right": 150,
    "others": 50
  }
}
```

**数据来源分析**：
| 字段 | 来源 | 说明 |
|------|------|------|
| `totalEvents` | ✓ 聚合计算 | 统计event数据总数 |
| `criticalEvents/highEvents/mediumEvents/lowEvents` | ✓ 聚合计算 | 根据score范围统计（后端约定） |
| `byLevel` | ✓ 聚合计算 | 根据event数据的level字段统计 |
| `byType` | ✓ 聚合计算 | 根据behavior字段映射到事件类型（后端约定） |
| `byBehavior` | ✓ 聚合计算 | 根据event数据的behavior字段统计 |

**结论**：✓ **完全可支撑**，通过聚合event数据即可得到。

---

### 2.5 时间段分析 ✓ 可支撑

**大屏需求**：
```json
{
  "interval": "hour",
  "data": [
    {
      "time": "2025-01-15 00:00:00",
      "timestamp": 1705120000000,
      "eventCount": 25,
      "criticalCount": 2,
      "highCount": 8,
      "mediumCount": 10,
      "lowCount": 5,
      "avgScore": 65.5,
      "maxScore": 95.0
    }
  ],
  "peakHours": [...],
  "statistics": {...}
}
```

**数据来源分析**：
| 字段 | 来源 | 说明 |
|------|------|------|
| `data[].time/timestamp` | ✓ 计算 | 按时间间隔分组 |
| `data[].eventCount` | ✓ 聚合计算 | 统计每个时间段的event数量 |
| `data[].criticalCount/highCount/mediumCount/lowCount` | ✓ 聚合计算 | 根据score范围统计 |
| `data[].avgScore/maxScore` | ✓ 聚合计算 | 计算每个时间段的平均分和最高分 |
| `peakHours` | ✓ 计算 | 找出事件最多的时段 |
| `statistics` | ✓ 聚合计算 | 统计信息 |

**结论**：✓ **完全可支撑**，通过按时间分组聚合event数据即可得到。

---

### 2.6 区域分析 ✓ 可支撑

**大屏需求**：
```json
{
  "regions": [
    {
      "regionId": "REGION_001",
      "regionName": "南京市建邺区",
      "regionType": "district",
      "location": {
        "centerLat": 32.0308,
        "centerLng": 118.7669,
        "bounds": {...}
      },
      "statistics": {
        "eventCount": 250,
        "criticalCount": 10,
        "highCount": 60,
        "mediumCount": 120,
        "lowCount": 60,
        "avgScore": 68.5,
        "riskLevel": "high"
      },
      "heatmap": [...]
    }
  ]
}
```

**数据来源分析**：
| 字段 | 来源 | 说明 |
|------|------|------|
| `regionId/regionName/regionType` | ⚠️ 需要地理服务 | 通过GPS坐标调用地理服务API获取区域信息 |
| `location.centerLat/centerLng` | ✓ 计算 | 区域内所有事件坐标的中心点 |
| `location.bounds` | ✓ 计算 | 区域内所有事件坐标的边界 |
| `statistics.*` | ✓ 聚合计算 | 统计区域内的事件数据 |
| `heatmap` | ✓ 聚合计算 | 按坐标点聚合事件，计算强度 |

**结论**：✓ **可支撑**，但需要：
1. 地理服务API（将GPS坐标映射到行政区域）
2. 区域边界数据（可选，用于更精确的区域划分）

---

### 2.7 车辆轨迹回放 ✓ 可支撑

**大屏需求**：
```json
{
  "deviceId": "MUSE_PI_PRO_001",
  "tripId": "TRIP_001",
  "startTime": 1705120000000,
  "endTime": 1705123456789,
  "totalDistance": 100.5,
  "totalDuration": 3600,
  "track": [
    {
      "timestamp": 1705120000000,
      "location": {
        "lat": 33.553485,
        "lng": 119.030977,
        "speed": 60.5,
        "heading": 180.0
      },
      "fatigue": {
        "score": 45.0,
        "level": "Normal"
      },
      "events": []
    }
  ],
  "events": [...]
}
```

**数据来源分析**：
| 字段 | 来源 | 说明 |
|------|------|------|
| `deviceId` | ✓ Token解析 | 从Token中获取 |
| `tripId` | ⚠️ 需要计算 | 需要定义行程识别规则（如GPS移动超过一定距离开始新行程） |
| `startTime/endTime` | ✓ 计算 | 行程的第一个和最后一个GPS点的时间 |
| `totalDistance` | ✓ 计算 | 计算GPS轨迹点的累计距离 |
| `totalDuration` | ✓ 计算 | endTime - startTime |
| `track[].timestamp` | ✓ gps数据 | 直接使用 |
| `track[].location.*` | ✓ gps数据 | 直接使用 |
| `track[].fatigue.score/level` | ✓ status数据 | 关联同时刻的status数据 |
| `track[].events` | ✓ event数据 | 关联该时间段的event数据 |

**结论**：✓ **可支撑**，但需要：
1. 行程识别算法（定义行程的开始和结束规则）
2. GPS轨迹点与status/event数据的关联逻辑

---

### 2.8 疲劳趋势曲线 ✓ 可支撑

**大屏需求**：
```json
{
  "series": [
    {
      "name": "疲劳分数",
      "type": "line",
      "data": [
        {
          "time": "2025-01-15 10:00:00",
          "timestamp": 1705120000000,
          "value": 45.0,
          "level": "Normal"
        }
      ]
    }
  ]
}
```

**数据来源分析**：
| 字段 | 来源 | 说明 |
|------|------|------|
| `data[].time/timestamp` | ✓ status数据 | 直接使用 |
| `data[].value` | ✓ status数据 | 使用score字段 |
| `data[].level` | ✓ status数据 | 使用level字段 |

**结论**：✓ **完全可支撑**，直接使用status数据即可。

---

### 2.9 驾驶员统计 ⚠️ 部分可支撑

**大屏需求**：
```json
{
  "driverId": "DRIVER_001",
  "driverName": "张三",
  "phone": "13800138000",
  "licenseNumber": "A1234567890",
  "avatar": "https://example.com/avatar/driver_001.jpg",
  "statistics": {
    "totalTrips": 150,
    "totalDistance": 15000.5,
    "totalDuration": 360000,
    "totalEvents": 120,
    "criticalEvents": 5,
    "highEvents": 30,
    "mediumEvents": 50,
    "lowEvents": 35,
    "avgScore": 65.5,
    "maxScore": 95.0,
    "safetyScore": 85.0
  },
  "behaviorStats": {...},
  "timeDistribution": {...}
}
```

**数据来源分析**：
| 字段 | 来源 | 说明 |
|------|------|------|
| `driverId/driverName/phone/licenseNumber/avatar` | ⚠️ 需要单独管理 | 驾驶员信息表（不在硬件数据中） |
| `statistics.totalTrips` | ✓ 计算 | 统计该驾驶员的行程数 |
| `statistics.totalDistance` | ✓ 计算 | 累计该驾驶员所有行程的距离 |
| `statistics.totalDuration` | ✓ 计算 | 累计该驾驶员所有行程的时长 |
| `statistics.totalEvents/criticalEvents/highEvents/mediumEvents/lowEvents` | ✓ 聚合计算 | 统计该驾驶员的所有事件 |
| `statistics.avgScore/maxScore` | ✓ 聚合计算 | 计算该驾驶员事件的平均分和最高分 |
| `statistics.safetyScore` | ⚠️ 需要算法 | 需要定义安全评分算法 |
| `behaviorStats` | ✓ 聚合计算 | 统计该驾驶员的行为分布 |
| `timeDistribution` | ✓ 聚合计算 | 按时间段统计该驾驶员的事件分布 |

**结论**：⚠️ **部分可支撑**，需要：
1. 驾驶员信息表（单独管理，不在硬件数据中）
2. 设备-驾驶员绑定关系
3. 安全评分算法（基于事件数据计算）

---

##  3. 数据映射关系总结

### 3.1 可直接使用的数据 ✓

| 硬件数据字段 | 大屏展示用途 | 说明 |
|------------|------------|------|
| `event.eventId` | 告警ID | 直接使用 |
| `event.level` | 告警级别 | 直接使用 |
| `event.score` | 疲劳分数 | 直接使用 |
| `event.behavior` | 行为类型 | 直接使用 |
| `event.confidence` | 置信度 | 可用于数据质量评估 |
| `event.duration` | 持续时间 | 直接使用 |
| `event.locationLat/locationLng` | 位置信息 | 直接使用 |
| `event.distractedCount` | 分心次数 | 直接使用 |
| `status.level` | 当前疲劳等级 | 直接使用 |
| `status.score` | 当前疲劳分数 | 直接使用 |
| `status.locationLat/locationLng` | 位置信息 | 直接使用 |
| `status.cpuUsage` | 系统性能 | 直接使用 |
| `status.memoryUsage` | 系统性能 | 直接使用 |
| `status.temperature` | 系统温度 | 直接使用 |
| `gps.locationLat/locationLng` | 位置信息 | 直接使用 |
| `gps.speed` | 速度 | 直接使用 |
| `gps.direction` | 方向角 | 直接使用（映射为heading） |
| `gps.altitude` | 海拔 | 直接使用 |
| `gps.satellites` | 卫星数量 | 可用于GPS质量评估 |
| `timestamp` | 时间戳 | 所有数据的时间标识 |

---

### 3.2 需要计算/聚合的数据 ✓

| 计算类型 | 数据来源 | 计算方法 | 说明 |
|---------|---------|---------|------|
| 事件总数 | event数据 | COUNT(*) | 统计事件数量 |
| 按级别统计 | event数据 | GROUP BY level | 按告警级别分组统计 |
| 按行为统计 | event数据 | GROUP BY behavior | 按行为类型分组统计 |
| 按类型统计 | event数据 | 根据behavior映射 | 根据行为映射到事件类型 |
| 时间段统计 | event数据 | GROUP BY 时间间隔 | 按小时/天/周/月分组 |
| 平均分数 | event数据 | AVG(score) | 计算平均疲劳分数 |
| 最高分数 | event数据 | MAX(score) | 找出最高疲劳分数 |
| 区域统计 | event数据 | 按GPS坐标聚合 | 按区域聚合事件 |
| 热力图数据 | event数据 | 按坐标点聚合 | 计算每个坐标点的事件密度 |
| 行程距离 | gps数据 | 计算轨迹点距离 | 累计GPS点之间的距离 |
| 行程时长 | gps数据 | MAX(timestamp) - MIN(timestamp) | 计算行程持续时间 |
| 设备在线状态 | 所有数据 | 判断最后上报时间 | 如30秒内有数据则为online |
| 设备健康状态 | status数据 | 根据阈值判断 | 根据CPU/内存/温度判断 |

---

### 3.3 需要外部数据/服务 ⚠️

| 数据类型 | 来源 | 说明 |
|---------|------|------|
| 设备ID | Token解析 | 从认证Token中获取 |
| 驾驶员ID | 设备-驾驶员绑定表 | 需要单独管理绑定关系 |
| 驾驶员信息 | 驾驶员信息表 | 姓名、电话、驾驶证等（不在硬件数据中） |
| 地址信息 | 逆地理编码API | 将GPS坐标转换为地址（如高德地图API） |
| 区域信息 | 地理服务API | 将GPS坐标映射到行政区域（如高德地图API） |
| 行程识别 | 算法计算 | 定义行程开始/结束规则（如GPS移动距离、时间间隔） |
| 安全评分 | 算法计算 | 基于事件数据计算安全评分 |

---

## ⚠️ 4. 待更新缺失数据

### 4.1 硬件数据中缺失的字段

经过核对，以下字段在大屏展示中需要，但硬件数据中**未提供**：

1. **设备运行时长 (uptime)**
   - 大屏需求：显示设备运行时长
   - 硬件数据：无
   - 解决方案：记录设备首次上报时间，计算 `当前时间 - 首次上报时间`

2. **地址信息 (address)**
   - 大屏需求：显示事件发生地址
   - 硬件数据：只有GPS坐标
   - 解决方案：调用逆地理编码API（高德）

3. **区域信息 (regionName, regionType)**
   - 大屏需求：区域分析
   - 硬件数据：只有GPS坐标
   - 解决方案：调用地理服务API（高德）

4. **行程ID (tripId)**
   - 大屏需求：行程回放
   - 硬件数据：无
   - 解决方案：后端定义行程识别规则，自动生成行程ID

5. **驾驶员相关信息**
   - 大屏需求：驾驶员信息、绑定关系
   - 硬件数据：无
   - 解决方案：需要单独管理驾驶员信息和设备-驾驶员绑定关系

---

### 4.2 需要补充的数据结构

为了完整支撑大屏展示，后端需要管理以下额外数据：

#### 1. 设备表
```sql
CREATE TABLE devices (
  device_id VARCHAR(50) PRIMARY KEY,
  device_name VARCHAR(100),
  device_type VARCHAR(50),
  version VARCHAR(20),
  first_report_time BIGINT,  -- 首次上报时间，用于计算uptime
  created_at BIGINT,
  updated_at BIGINT
);
```

#### 2. 驾驶员表
```sql
CREATE TABLE drivers (
  driver_id VARCHAR(50) PRIMARY KEY,
  driver_name VARCHAR(100),
  phone VARCHAR(20),
  email VARCHAR(100),
  license_number VARCHAR(50),
  license_type VARCHAR(10),
  license_expire DATE,
  avatar_url VARCHAR(255),
  created_at BIGINT,
  updated_at BIGINT
);
```

#### 3. 设备-驾驶员绑定表
```sql
CREATE TABLE device_driver_bindings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_id VARCHAR(50),
  driver_id VARCHAR(50),
  bind_time BIGINT,
  unbind_time BIGINT,
  status VARCHAR(20),  -- active/inactive
  created_at BIGINT,
  updated_at BIGINT,
  INDEX idx_device (device_id),
  INDEX idx_driver (driver_id)
);
```

#### 4. 行程表
```sql
CREATE TABLE trips (
  trip_id VARCHAR(50) PRIMARY KEY,
  device_id VARCHAR(50),
  driver_id VARCHAR(50),
  start_time BIGINT,
  end_time BIGINT,
  start_lat DECIMAL(10, 6),
  start_lng DECIMAL(10, 6),
  end_lat DECIMAL(10, 6),
  end_lng DECIMAL(10, 6),
  total_distance DECIMAL(10, 2),  -- 公里
  total_duration INT,  -- 秒
  event_count INT,
  max_level VARCHAR(20),
  max_score DECIMAL(5, 2),
  avg_score DECIMAL(5, 2),
  safety_score DECIMAL(5, 2),
  created_at BIGINT,
  updated_at BIGINT,
  INDEX idx_device (device_id),
  INDEX idx_driver (driver_id),
  INDEX idx_time (start_time, end_time)
);
```

---

## ✓ 5. 结论与建议

### 5.1 数据支撑情况

| 大屏模块 | 支撑情况 | 说明 |
|---------|---------|------|
| 实时车辆状态 | ✓ 可支撑 | 需要设备-驾驶员绑定关系 |
| 实时告警事件 | ✓ 可支撑 | 需要逆地理编码服务 |
| 系统运行状态 | ✓ 可支撑 | 完全可支撑 |
| 疲劳事件统计 | ✓ 可支撑 | 完全可支撑 |
| 时间段分析 | ✓ 可支撑 | 完全可支撑 |
| 区域分析 | ✓ 可支撑 | 需要地理服务API |
| 车辆轨迹回放 | ✓ 可支撑 | 需要行程识别算法 |
| 疲劳趋势曲线 | ✓ 可支撑 | 完全可支撑 |
| 驾驶员统计 | ⚠️ 部分可支撑 | 需要驾驶员信息表和绑定关系 |
| 热力图 | ✓ 可支撑 | 完全可支撑 |

**总体结论**：✓ **硬件上传的数据基本可以支撑大屏展示需求**，但需要补充以下内容：

---

### 5.2 需要补充的内容

#### 1. 数据表设计
- ✓ 设备表（记录设备信息和首次上报时间）
- ✓ 驾驶员表（管理驾驶员信息）
- ✓ 设备-驾驶员绑定表（管理绑定关系）
- ✓ 行程表（记录行程信息）

#### 2. 外部服务
- ✓ 逆地理编码服务（GPS坐标 → 地址）
- ✓ 地理服务API（GPS坐标 → 行政区域）

#### 3. 算法/规则
- ✓ 行程识别算法（定义行程开始/结束规则）
- ✓ 安全评分算法（基于事件数据计算）
- ✓ 设备在线状态判断规则（如30秒内有数据为online）
- ✓ 设备健康状态判断规则（根据CPU/内存/温度阈值）

#### 4. 数据处理逻辑
- ✓ 事件类型映射（behavior → eventType）
- ✓ 严重程度计算（score → severity）
- ✓ 时间段聚合（按小时/天/周/月分组）
- ✓ 区域聚合（按GPS坐标聚合到行政区域）
- ✓ 热力图数据生成（按坐标点聚合，计算强度）

---

### 5.3 数据库设计建议

#### 核心数据表

1. **event_data** - 事件数据表
   - 存储所有event类型的数据
   - 字段：event_id, device_id, timestamp, level, score, behavior, confidence, duration, location_lat, location_lng, distracted_count

2. **status_data** - 状态数据表
   - 存储所有status类型的数据
   - 字段：id, device_id, timestamp, level, score, location_lat, location_lng, cpu_usage, memory_usage, temperature

3. **gps_data** - GPS数据表
   - 存储所有gps类型的数据
   - 字段：id, device_id, timestamp, location_lat, location_lng, speed, direction, altitude, satellites

4. **devices** - 设备表
   - 管理设备信息
   - 字段：device_id, device_name, device_type, version, first_report_time, created_at, updated_at

5. **drivers** - 驾驶员表
   - 管理驾驶员信息
   - 字段：driver_id, driver_name, phone, email, license_number, license_type, license_expire, avatar_url, created_at, updated_at

6. **device_driver_bindings** - 设备-驾驶员绑定表
   - 管理绑定关系
   - 字段：id, device_id, driver_id, bind_time, unbind_time, status, created_at, updated_at

7. **trips** - 行程表
   - 记录行程信息
   - 字段：trip_id, device_id, driver_id, start_time, end_time, start_lat, start_lng, end_lat, end_lng, total_distance, total_duration, event_count, max_level, max_score, avg_score, safety_score, created_at, updated_at

#### 索引建议

- event_data: INDEX(device_id, timestamp), INDEX(timestamp), INDEX(location_lat, location_lng)
- status_data: INDEX(device_id, timestamp), INDEX(timestamp)
- gps_data: INDEX(device_id, timestamp), INDEX(timestamp), INDEX(location_lat, location_lng)
- trips: INDEX(device_id, start_time), INDEX(driver_id, start_time), INDEX(start_time, end_time)

---

### 5.4 实现优先级建议

#### 高优先级（必须实现）
1. ✓ 事件数据存储和查询
2. ✓ 状态数据存储和查询
3. ✓ GPS数据存储和查询
4. ✓ 设备-驾驶员绑定关系管理
5. ✓ 基础统计功能（事件总数、按级别统计等）

#### 中优先级（重要功能）
6. ✓ 时间段分析
7. ✓ 行为类型统计
8. ✓ 区域分析（需要地理服务API）
9. ✓ 行程识别和轨迹回放
10. ✓ 逆地理编码（地址显示）

#### 低优先级（增强功能）
11. ✓ 热力图数据生成
12. ✓ 安全评分算法
13. ✓ 设备健康状态判断
14. ✓ 高级统计分析



---

**文档版本**: v1.0  
**创建时间**: 2025-11-15  
**适用版本**: Muse Pi Pro Plus v2.0+

