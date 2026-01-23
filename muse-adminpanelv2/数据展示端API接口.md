# 数据展示大屏规范文档

## 📋 文档说明

本文档定义了数据展示大屏所需的所有数据格式和展示方式，用于指导专业UI设计师开发数据展示大屏界面。

**目标**：为管理端提供全面、直观、美观的数据可视化大屏，实时监控车辆状态、驾驶员疲劳情况、行驶轨迹等关键信息。

---

## 🎯 大屏展示模块划分

### 1. 实时监控区域
- 当前在线车辆数
- 实时疲劳事件
- 当前告警状态
- 系统运行状态

### 2. 地图展示区域
- 车辆实时位置
- 行驶轨迹回放
- 疲劳事件热力图
- 高风险区域标记

### 3. 统计数据区域
- 疲劳事件统计
- 时间段分析
- 行为类型统计
- 驾驶员统计

### 4. 图表展示区域
- 疲劳趋势曲线
- 时间段分布图
- 区域分布图
- 设备性能监控

### 5. 驾驶员信息区域
- 驾驶员基本信息
- 驾驶时长统计
- 疲劳事件记录
- 安全评分

---

## 📊 数据接口规范

### 基础响应格式

所有接口统一返回格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    // 具体数据内容
  },
  "timestamp": 1705123456789
}
```

---

## 🔴 1. 实时监控数据

### 1.1 实时车辆状态

**接口**: `GET /api/v2/dashboard/realtime/vehicles`

**响应格式**:
```json
{
  "code": 200,
  "data": {
    "totalVehicles": 50,
    "onlineVehicles": 45,
    "offlineVehicles": 5,
    "vehicles": [
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
    ]
  }
}
```

**字段说明**:
- `totalVehicles`: 总车辆数
- `onlineVehicles`: 在线车辆数
- `offlineVehicles`: 离线车辆数
- `vehicles`: 车辆列表
  - `deviceId`: 设备ID
  - `driverId`: 驾驶员ID
  - `driverName`: 驾驶员姓名
  - `status`: 状态（online/offline/error）
  - `currentLevel`: 当前疲劳等级
  - `currentScore`: 当前疲劳分数
  - `location`: 位置信息
  - `lastUpdateTime`: 最后更新时间（毫秒时间戳）
  - `uptime`: 运行时长（秒）

---

### 1.2 实时告警事件

**接口**: `GET /api/v2/dashboard/realtime/alerts`

**响应格式**:
```json
{
  "code": 200,
  "data": {
    "activeAlerts": 8,
    "criticalAlerts": 2,
    "highAlerts": 3,
    "mediumAlerts": 3,
    "alerts": [
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
    ]
  }
}
```

**字段说明**:
- `activeAlerts`: 当前活跃告警数
- `criticalAlerts`: 严重告警数（Level 3）
- `highAlerts`: 高级告警数（Level 2）
- `mediumAlerts`: 中级告警数（Level 1）
- `alerts`: 告警列表
  - `alertId`: 告警ID
  - `deviceId`: 设备ID
  - `driverId`: 驾驶员ID
  - `driverName`: 驾驶员姓名
  - `level`: 告警级别
  - `score`: 疲劳分数
  - `behavior`: 检测到的行为
  - `location`: 位置信息（包含地址）
  - `timestamp`: 发生时间
  - `duration`: 持续时间（秒）
  - `status`: 状态（active/resolved）

---

### 1.3 系统运行状态

**接口**: `GET /api/v2/dashboard/realtime/system`

**响应格式**:
```json
{
  "code": 200,
  "data": {
    "totalDevices": 50,
    "healthyDevices": 45,
    "warningDevices": 3,
    "errorDevices": 2,
    "systemStats": {
      "avgCpuUsage": 45.2,
      "avgMemoryUsage": 68.5,
      "avgTemperature": 55.0,
      "avgNetworkLatency": 120
    },
    "deviceStatus": [
      {
        "deviceId": "MUSE_PI_PRO_001",
        "status": "healthy",
        "cpuUsage": 45.2,
        "memoryUsage": 68.5,
        "temperature": 55.0,
        "networkStatus": "online",
        "lastHeartbeat": 1705123456789
      }
    ]
  }
}
```

---

## 📈 2. 统计数据

### 2.1 疲劳事件统计

**接口**: `GET /api/v2/dashboard/statistics/events?startTime=1705120000000&endTime=1705123456789&driverId=DRIVER_001`

**响应格式**:
```json
{
  "code": 200,
  "data": {
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
    },
    "trend": {
      "today": 120,
      "yesterday": 150,
      "thisWeek": 850,
      "lastWeek": 920,
      "thisMonth": 3500,
      "lastMonth": 3800
    }
  }
}
```

**字段说明**:
- `summary`: 总体统计
- `byLevel`: 按告警级别统计
- `byType`: 按事件类型统计
- `byBehavior`: 按行为类型统计
- `trend`: 趋势对比（今日/昨日/本周/上周/本月/上月）

---

### 2.2 时间段分析

**接口**: `GET /api/v2/dashboard/statistics/timeframe?startTime=1705120000000&endTime=1705123456789&interval=hour`

**响应格式**:
```json
{
  "code": 200,
  "data": {
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
      },
      {
        "time": "2025-01-15 01:00:00",
        "timestamp": 1705123600000,
        "eventCount": 30,
        "criticalCount": 3,
        "highCount": 10,
        "mediumCount": 12,
        "lowCount": 5,
        "avgScore": 68.2,
        "maxScore": 98.0
      }
    ],
    "peakHours": [
      {
        "hour": 2,
        "eventCount": 45,
        "avgScore": 72.5
      },
      {
        "hour": 14,
        "eventCount": 50,
        "avgScore": 75.0
      }
    ],
    "statistics": {
      "totalEvents": 1250,
      "avgEventsPerHour": 52.08,
      "maxEventsInHour": 50,
      "minEventsInHour": 10
    }
  }
}
```

**字段说明**:
- `interval`: 时间间隔（hour/day/week/month）
- `data`: 时间段数据列表
- `peakHours`: 高峰时段（事件最多的时段）
- `statistics`: 统计信息

---

### 2.3 区域分析

**接口**: `GET /api/v2/dashboard/statistics/region?startTime=1705120000000&endTime=1705123456789&level=city`

**响应格式**:
```json
{
  "code": 200,
  "data": {
    "level": "city",
    "regions": [
      {
        "regionId": "REGION_001",
        "regionName": "南京市建邺区",
        "regionType": "district",
        "location": {
          "centerLat": 32.0308,
          "centerLng": 118.7669,
          "bounds": {
            "north": 32.0500,
            "south": 32.0100,
            "east": 118.7800,
            "west": 118.7500
          }
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
        "heatmap": [
          {
            "lat": 32.0308,
            "lng": 118.7669,
            "intensity": 0.85,
            "eventCount": 25
          }
        ]
      }
    ],
    "topRegions": [
      {
        "regionName": "南京市建邺区",
        "eventCount": 250,
        "riskLevel": "high"
      },
      {
        "regionName": "南京市鼓楼区",
        "eventCount": 200,
        "riskLevel": "medium"
      }
    ]
  }
}
```

**字段说明**:
- `level`: 区域级别（city/district/road）
- `regions`: 区域列表
  - `regionId`: 区域ID
  - `regionName`: 区域名称
  - `regionType`: 区域类型
  - `location`: 位置信息（中心点和边界）
  - `statistics`: 统计信息
  - `heatmap`: 热力图数据
- `topRegions`: 高风险区域排行

---

### 2.4 驾驶员统计

**接口**: `GET /api/v2/dashboard/statistics/drivers?startTime=1705120000000&endTime=1705123456789`

**响应格式**:
```json
{
  "code": 200,
  "data": {
    "totalDrivers": 30,
    "activeDrivers": 25,
    "drivers": [
      {
        "driverId": "DRIVER_001",
        "driverName": "张三",
        "phone": "13800138000",
        "licenseNumber": "A1234567890",
        "avatar": "https://example.com/avatar/driver_001.jpg",
        "teamName": "南京一队",
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
        "recentTrips": [
          {
            "tripId": "TRIP_001",
            "startTime": 1705120000000,
            "endTime": 1705123600000,
            "distance": 100.5,
            "duration": 3600,
            "eventCount": 5,
            "maxLevel": "Level 2"
          }
        ],
        "behaviorStats": {
          "eyes_closed": 40,
          "yarning": 20,
          "head_down": 30,
          "seeing_left": 15,
          "seeing_right": 15
        },
        "timeDistribution": {
          "morning": 30,
          "afternoon": 50,
          "evening": 30,
          "night": 10
        }
      }
    ],
    "topDrivers": [
      {
        "driverId": "DRIVER_001",
        "driverName": "张三",
        "safetyScore": 95.0,
        "totalTrips": 150
      }
    ],
    "riskDrivers": [
      {
        "driverId": "DRIVER_002",
        "driverName": "李四",
        "safetyScore": 45.0,
        "criticalEvents": 20
      }
    ]
  }
}
```

**字段说明**:
- `totalDrivers`: 总驾驶员数
- `activeDrivers`: 活跃驾驶员数
- `drivers`: 驾驶员列表
  - `driverId`: 驾驶员ID
  - `driverName`: 姓名
  - `phone`: 电话
  - `licenseNumber`: 驾驶证号
  - `avatar`: 头像URL
  - `statistics`: 统计数据
    - `totalTrips`: 总行程数
    - `totalDistance`: 总里程（公里）
    - `totalDuration`: 总时长（秒）
    - `totalEvents`: 总事件数
    - `safetyScore`: 安全评分（0-100）
  - `recentTrips`: 最近行程
  - `behaviorStats`: 行为统计
  - `timeDistribution`: 时间段分布
- `topDrivers`: 优秀驾驶员排行
- `riskDrivers`: 高风险驾驶员

---

## 🗺️ 3. 地图数据

### 3.1 车辆实时位置

**接口**: `GET /api/v2/dashboard/map/vehicles?bounds={north},{south},{east},{west}`

**响应格式**:
```json
{
  "code": 200,
  "data": {
    "vehicles": [
      {
        "deviceId": "MUSE_PI_PRO_001",
        "driverId": "DRIVER_001",
        "driverName": "张三",
        "location": {
          "lat": 33.553485,
          "lng": 119.030977,
          "speed": 60.5,
          "heading": 180.0,
          "altitude": 50.5
        },
        "status": {
          "level": "Level 1",
          "score": 65.5,
          "behavior": "focused",
          "online": true
        },
        "lastUpdateTime": 1705123456789
      }
    ],
    "bounds": {
      "north": 33.6000,
      "south": 33.5000,
      "east": 119.1000,
      "west": 119.0000
    }
  }
}
```

---

### 3.2 车辆轨迹回放

**接口**: `GET /api/v2/dashboard/map/track?deviceId=MUSE_PI_PRO_001&startTime=1705120000000&endTime=1705123456789`

**请求参数**:
- `deviceId` (必填): 设备ID
- `startTime` (必填): 开始时间（毫秒时间戳）
- `endTime` (必填): 结束时间（毫秒时间戳）

**响应格式**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "deviceId": "MUSE_PI_PRO_001",
    "driverId": "DRIVER_001",
    "driverName": "张三",
    "tripId": "TRIP_001",
    "startTime": 1705120000000,
    "endTime": 1705123456789,
    "totalDistance": 100.50,
    "totalDuration": 3456,
    "track": [
      {
        "timestamp": 1705120000000,
        "location": {
          "lat": 33.553485,
          "lng": 119.030977,
          "speed": 60.50,
          "heading": 180.00,
          "altitude": 44.40,
          "address": null
        },
        "fatigue": {
          "score": 45.0,
          "level": "Normal"
        },
        "events": []
      },
      {
        "timestamp": 1705120020000,
        "location": {
          "lat": 33.554000,
          "lng": 119.031000,
          "speed": 62.00,
          "heading": 180.50,
          "altitude": 45.20,
          "address": null
        },
        "fatigue": {
          "score": 65.0,
          "level": "Level 1"
        },
        "events": [
          {
            "eventId": "MUSE_PI_PRO_001_1705120020000_001",
            "level": "Level 1",
            "behavior": "eyes_closed",
            "timestamp": 1705120020000,
            "score": 65.50,
            "address": "江苏省淮安市清江浦区XX路XX号"
          }
        ]
      },
      {
        "timestamp": 1705120040000,
        "location": {
          "lat": 33.555000,
          "lng": 119.032000,
          "speed": 63.50,
          "heading": 181.00,
          "altitude": 46.00,
          "address": null
        },
        "fatigue": {
          "score": 75.0,
          "level": "Level 2"
        },
        "events": [
          {
            "eventId": "MUSE_PI_PRO_001_1705120040000_001",
            "level": "Level 2",
            "behavior": "eyes_closed",
            "timestamp": 1705120040000,
            "score": 75.50,
            "address": "江苏省淮安市清江浦区XX路XX号"
          },
          {
            "eventId": "MUSE_PI_PRO_001_1705120040000_002",
            "level": "Level 2",
            "behavior": "yarning",
            "timestamp": 1705120040000,
            "score": 78.00,
            "address": "江苏省淮安市清江浦区XX路XX号"
          }
        ]
      }
    ],
    "events": [],
    "statistics": {
      "totalEvents": 3,
      "criticalEvents": 0,
      "highEvents": 2,
      "mediumEvents": 1,
      "lowEvents": 0,
      "avgScore": 73.00,
      "maxScore": 78.00
    }
  },
  "timestamp": 1705123456789
}
```

**字段说明**:
- `track`: 轨迹点数组（按时间顺序排列，每个GPS点一个元素）
  - `timestamp`: GPS时间戳（毫秒）
  - `location`: 位置信息（经纬度、速度、方向、海拔、地址）
  - `fatigue`: 疲劳度信息（分数、级别）
  - `events`: 附加到该GPS点的事件数组（如果该时间点有事件，则包含在数组中；如果没有，则为空数组）
    - `eventId`: 事件ID
    - `level`: 事件级别（Level 1/2/3）
    - `behavior`: 行为类型
    - `timestamp`: 事件时间戳
    - `score`: 事件分数
    - `address`: 事件地址
- `events`: 已废弃，始终返回空数组（事件已附加到track中）
- `statistics`: 行程统计信息
  - `totalEvents`: 总事件数
  - `criticalEvents`: 严重事件数（Level 3）
  - `highEvents`: 高级别事件数（Level 2）
  - `mediumEvents`: 中级别事件数（Level 1）
  - `lowEvents`: 低级别事件数（Normal）
  - `avgScore`: 平均分数
  - `maxScore`: 最高分数

**数据规则**:
1. `track` 数组按时间顺序排列（timestamp从小到大）
2. 每个GPS点只出现一次（相同timestamp的去重）
3. 事件附加到最接近的GPS点上（时间差在5秒内）
4. 如果某个GPS点没有事件，`events` 字段为空数组 `[]`
5. 如果某个GPS点有多个事件，`events` 数组包含所有事件

---

### 3.3 疲劳事件热力图

**接口**: `GET /api/v2/dashboard/map/heatmap?startTime=1705120000000&endTime=1705123456789&level=Level 2`

**响应格式**:
```json
{
  "code": 200,
  "data": {
    "level": "Level 2",
    "startTime": 1705120000000,
    "endTime": 1705123456789,
    "points": [
      {
        "lat": 33.553485,
        "lng": 119.030977,
        "intensity": 0.85,
        "eventCount": 25,
        "maxScore": 95.0,
        "avgScore": 75.5
      },
      {
        "lat": 33.554000,
        "lng": 119.031000,
        "intensity": 0.75,
        "eventCount": 20,
        "maxScore": 90.0,
        "avgScore": 72.0
      }
    ],
    "bounds": {
      "north": 33.6000,
      "south": 33.5000,
      "east": 119.1000,
      "west": 119.0000
    },
    "statistics": {
      "totalPoints": 150,
      "maxIntensity": 0.95,
      "minIntensity": 0.10,
      "totalEvents": 1250
    }
  }
}
```

**字段说明**:
- `level`: 筛选的告警级别（可选，不传则显示所有级别）
- `points`: 热力图数据点
  - `lat/lng`: 坐标
  - `intensity`: 强度（0-1）
  - `eventCount`: 事件数量
  - `maxScore/avgScore`: 分数统计

---

## 📊 4. 图表数据

### 4.1 疲劳趋势曲线

**接口**: `GET /api/v2/dashboard/charts/trend?deviceId=MUSE_PI_PRO_001&startTime=1705120000000&endTime=1705123456789&interval=minute`

**响应格式**:
```json
{
  "code": 200,
  "data": {
    "deviceId": "MUSE_PI_PRO_001",
    "driverId": "DRIVER_001",
    "driverName": "张三",
    "interval": "minute",
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
          },
          {
            "time": "2025-01-15 10:01:00",
            "timestamp": 1705120060000,
            "value": 65.0,
            "level": "Level 1"
          },
          {
            "time": "2025-01-15 10:02:00",
            "timestamp": 1705120120000,
            "value": 82.5,
            "level": "Level 2"
          }
        ]
      },
      {
        "name": "告警级别",
        "type": "bar",
        "data": [
          {
            "time": "2025-01-15 10:00:00",
            "timestamp": 1705120000000,
            "value": 0,
            "level": "Normal"
          },
          {
            "time": "2025-01-15 10:01:00",
            "timestamp": 1705120060000,
            "value": 1,
            "level": "Level 1"
          },
          {
            "time": "2025-01-15 10:02:00",
            "timestamp": 1705120120000,
            "value": 2,
            "level": "Level 2"
          }
        ]
      }
    ],
    "statistics": {
      "minScore": 30.0,
      "maxScore": 95.0,
      "avgScore": 65.5,
      "totalEvents": 25
    }
  }
}
```

---

### 4.2 时间段分布图

**接口**: `GET /api/v2/dashboard/charts/timeDistribution?startTime=1705120000000&endTime=1705123456789&groupBy=hour`

**响应格式**:
```json
{
  "code": 200,
  "data": {
    "groupBy": "hour",
    "data": [
      {
        "label": "00:00",
        "value": 25,
        "criticalCount": 2,
        "highCount": 8,
        "mediumCount": 10,
        "lowCount": 5,
        "avgScore": 65.5
      },
      {
        "label": "01:00",
        "value": 30,
        "criticalCount": 3,
        "highCount": 10,
        "mediumCount": 12,
        "lowCount": 5,
        "avgScore": 68.2
      }
    ],
    "peakPeriods": [
      {
        "start": 2,
        "end": 4,
        "label": "02:00-04:00",
        "eventCount": 45,
        "avgScore": 72.5
      }
    ]
  }
}
```

---

### 4.3 行为类型分布

**接口**: `GET /api/v2/dashboard/charts/behaviorDistribution?startTime=1705120000000&endTime=1705123456789`

**响应格式**:
```json
{
  "code": 200,
  "data": {
    "total": 1250,
    "distribution": [
      {
        "behavior": "eyes_closed",
        "label": "闭眼",
        "count": 400,
        "percentage": 32.0,
        "avgScore": 75.5,
        "maxScore": 95.0
      },
      {
        "behavior": "yarning",
        "label": "打哈欠",
        "count": 200,
        "percentage": 16.0,
        "avgScore": 70.0,
        "maxScore": 90.0
      },
      {
        "behavior": "head_down",
        "label": "低头",
        "count": 300,
        "percentage": 24.0,
        "avgScore": 65.0,
        "maxScore": 85.0
      },
      {
        "behavior": "seeing_left",
        "label": "左看",
        "count": 150,
        "percentage": 12.0,
        "avgScore": 60.0,
        "maxScore": 80.0
      },
      {
        "behavior": "seeing_right",
        "label": "右看",
        "count": 150,
        "percentage": 12.0,
        "avgScore": 60.0,
        "maxScore": 80.0
      },
      {
        "behavior": "others",
        "label": "其他",
        "count": 50,
        "percentage": 4.0,
        "avgScore": 55.0,
        "maxScore": 75.0
      }
    ],
    "byType": {
      "FATIGUE": {
        "count": 800,
        "percentage": 64.0,
        "behaviors": ["eyes_closed", "yarning", "eyes_closed_head_left", "eyes_closed_head_right"]
      },
      "DISTRACTION": {
        "count": 400,
        "percentage": 32.0,
        "behaviors": ["head_down", "seeing_left", "seeing_right"]
      },
      "EMERGENCY": {
        "count": 50,
        "percentage": 4.0,
        "behaviors": ["others"]
      }
    }
  }
}
```

---

### 4.4 区域分布图

**接口**: `GET /api/v2/dashboard/charts/regionDistribution?startTime=1705120000000&endTime=1705123456789&level=city`

**响应格式**:
```json
{
  "code": 200,
  "data": {
    "level": "city",
    "regions": [
      {
        "regionId": "REGION_001",
        "regionName": "南京市建邺区",
        "eventCount": 250,
        "percentage": 20.0,
        "criticalCount": 10,
        "highCount": 60,
        "mediumCount": 120,
        "lowCount": 60,
        "avgScore": 68.5,
        "riskLevel": "high"
      },
      {
        "regionId": "REGION_002",
        "regionName": "南京市鼓楼区",
        "eventCount": 200,
        "percentage": 16.0,
        "criticalCount": 8,
        "highCount": 50,
        "mediumCount": 100,
        "lowCount": 42,
        "avgScore": 65.0,
        "riskLevel": "medium"
      }
    ],
    "topRegions": [
      {
        "regionName": "南京市建邺区",
        "eventCount": 250,
        "riskLevel": "high"
      }
    ]
  }
}
```

---

## 👤 5. 驾驶员详细信息

### 5.1 驾驶员基本信息

**接口**: `GET /api/v2/dashboard/driver/info?driverId=DRIVER_001`

**响应格式**:
```json
{
  "code": 200,
  "data": {
    "driverId": "DRIVER_001",
    "driverName": "张三",
    "phone": "13800138000",
    "email": "zhangsan@example.com",
    "licenseNumber": "A1234567890",
    "licenseType": "A1",
    "licenseExpire": "2026-12-31",
    "avatar": "https://example.com/avatar/driver_001.jpg",
    "teamName": "南京一队",
    "bindDevices": [
      {
        "deviceId": "MUSE_PI_PRO_001",
        "deviceName": "车辆001",
        "bindTime": 1704000000000,
        "status": "active"
      }
    ],
    "statistics": {
      "totalTrips": 150,
      "totalDistance": 15000.5,
      "totalDuration": 360000,
      "totalEvents": 120,
      "safetyScore": 85.0,
      "joinDate": "2024-01-01"
    }
  }
}
```

---

### 5.2 驾驶员行程列表

**接口**: `GET /api/v2/dashboard/driver/trips?driverId=DRIVER_001&startTime=1705120000000&endTime=1705123456789&page=1&pageSize=20`

**响应格式**:
```json
{
  "code": 200,
  "data": {
    "driverId": "DRIVER_001",
    "driverName": "张三",
    "total": 150,
    "page": 1,
    "pageSize": 20,
    "trips": [
      {
        "tripId": "TRIP_001",
        "deviceId": "MUSE_PI_PRO_001",
        "startTime": 1705120000000,
        "endTime": 1705123600000,
        "duration": 3600,
        "distance": 100.5,
        "startLocation": {
          "lat": 33.553485,
          "lng": 119.030977,
          "address": "江苏省南京市建邺区XX路"
        },
        "endLocation": {
          "lat": 33.600000,
          "lng": 119.100000,
          "address": "江苏省南京市鼓楼区YY路"
        },
        "statistics": {
          "eventCount": 5,
          "criticalEvents": 0,
          "highEvents": 2,
          "mediumEvents": 2,
          "lowEvents": 1,
          "maxScore": 85.0,
          "avgScore": 65.5,
          "safetyScore": 80.0
        },
        "behaviors": {
          "eyes_closed": 2,
          "head_down": 2,
          "seeing_left": 1
        }
      }
    ]
  }
}
```

---

### 5.3 驾驶员安全评分

**接口**: `GET /api/v2/dashboard/driver/safety?driverId=DRIVER_001&startTime=1705120000000&endTime=1705123456789`

**响应格式**:
```json
{
  "code": 200,
  "data": {
    "driverId": "DRIVER_001",
    "driverName": "张三",
    "overallScore": 85.0,
    "scoreBreakdown": {
      "fatigueScore": 80.0,
      "behaviorScore": 85.0,
      "complianceScore": 90.0,
      "incidentScore": 85.0
    },
    "trend": [
      {
        "date": "2025-01-01",
        "score": 82.0
      },
      {
        "date": "2025-01-02",
        "score": 83.0
      },
      {
        "date": "2025-01-03",
        "score": 85.0
      }
    ],
    "rankings": {
      "overall": 5,
      "totalDrivers": 30,
      "percentile": 83.3
    },
    "improvements": [
      {
        "type": "fatigue",
        "description": "建议减少夜间驾驶时间",
        "priority": "medium"
      }
    ]
  }
}
```

---

## 📱 6. 实时推送数据（WebSocket）

### 6.1 实时车辆位置更新

**事件**: `vehicle:location:update`

**数据格式**:
```json
{
  "deviceId": "MUSE_PI_PRO_001",
  "driverId": "DRIVER_001",
  "driverName": "张三",
  "location": {
    "lat": 33.553485,
    "lng": 119.030977,
    "speed": 60.5,
    "heading": 180.0
  },
  "status": {
    "level": "Level 1",
    "score": 65.5,
    "behavior": "focused"
  },
  "timestamp": 1705123456789
}
```

---

### 6.2 实时告警事件

**事件**: `alert:new`

**数据格式**:
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
  "duration": 5.2
}
```

---

### 6.3 实时统计数据更新

**事件**: `statistics:update`

**数据格式**:
```json
{
  "type": "summary",
  "data": {
    "totalVehicles": 50,
    "onlineVehicles": 45,
    "activeAlerts": 8,
    "criticalAlerts": 2
  },
  "timestamp": 1705123456789
}
```

---

## 🎨 7. 数据展示建议

### 7.1 颜色规范

| 状态/级别 | 颜色 | RGB | 说明 |
|-----------|------|-----|------|
| Normal | 绿色 | #52C41A | 正常状态 |
| Level 1 | 黄色 | #FAAD14 | 一级告警 |
| Level 2 | 橙色 | #FF7A00 | 二级告警 |
| Level 3 | 红色 | #FF4D4F | 三级告警 |
| 在线 | 蓝色 | #1890FF | 设备在线 |
| 离线 | 灰色 | #8C8C8C | 设备离线 |
| 错误 | 红色 | #FF4D4F | 设备错误 |

---

### 7.2 图表类型建议

| 数据类型 | 推荐图表类型 | 说明 |
|----------|------------|------|
| 疲劳趋势 | 折线图 | 展示分数变化趋势 |
| 时间段分布 | 柱状图/热力图 | 展示不同时段的事件分布 |
| 行为类型 | 饼图/柱状图 | 展示行为类型占比 |
| 区域分布 | 地图热力图 | 展示区域事件密度 |
| 驾驶员排行 | 排行榜 | 展示驾驶员安全评分 |
| 实时位置 | 地图标记 | 展示车辆实时位置 |

---

### 7.3 大屏布局建议

```
┌─────────────────────────────────────────────────────────┐
│  顶部标题栏：系统名称、当前时间、刷新按钮                │
├──────────────┬──────────────────────────┬──────────────┤
│              │                          │              │
│  实时监控    │      地图展示区域         │  统计数据    │
│  区域        │      (车辆位置、轨迹)     │  区域        │
│              │                          │              │
│  - 在线车辆  │                          │  - 事件统计  │
│  - 告警数量  │                          │  - 时间段分析│
│  - 系统状态  │                          │  - 行为统计  │
│              │                          │              │
├──────────────┴──────────────────────────┴──────────────┤
│                                                          │
│  图表展示区域                                            │
│  - 疲劳趋势曲线  - 时间段分布图  - 区域分布图          │
│                                                          │
├─────────────────────────────────────────────────────────┤
│  驾驶员信息区域                                          │
│  - 驾驶员列表  - 安全评分  - 行程记录                   │
└─────────────────────────────────────────────────────────┘
```

---

## 📝 8. 数据字段映射表

### 8.1 行为类型映射

| 行为代码 | 中文名称 | 事件类型 | 说明 |
|---------|---------|---------|------|
| `eyes_closed` | 闭眼 | FATIGUE | 疲劳行为 |
| `yarning` | 打哈欠 | FATIGUE | 疲劳行为 |
| `eyes_closed_head_left` | 闭眼左偏 | FATIGUE | 疲劳行为 |
| `eyes_closed_head_right` | 闭眼右偏 | FATIGUE | 疲劳行为 |
| `head_down` | 低头 | DISTRACTION | 分心行为 |
| `seeing_left` | 左看 | DISTRACTION | 分心行为 |
| `seeing_right` | 右看 | DISTRACTION | 分心行为 |
| `focused` | 专注 | - | 正常状态 |
| `others` | 其他 | EMERGENCY | 其他行为 |

---

### 8.2 告警级别映射

| 级别代码 | 中文名称 | 分数范围 | 颜色 |
|---------|---------|---------|------|
| `Normal` | 正常 | < 50 | 绿色 |
| `Level 1` | 一级告警 | 50-74 | 黄色 |
| `Level 2` | 二级告警 | 75-94 | 橙色 |
| `Level 3` | 三级告警 | ≥ 95 | 红色 |

---

### 8.3 严重程度映射

| 严重程度 | 分数范围 | 说明 |
|---------|---------|------|
| `CRITICAL` | ≥ 85 | 严重疲劳 |
| `HIGH` | 70-84 | 高度疲劳 |
| `MEDIUM` | 60-69 | 中等疲劳 |
| `LOW` | 10-59 | 轻微疲劳 |

---

## ✅ 9. 数据完整性说明

### 9.1 必需字段

所有接口返回的数据必须包含以下字段：
- `code`: 状态码（200表示成功）
- `message`: 消息（"success"表示成功）
- `data`: 数据内容
- `timestamp`: 服务器时间戳

### 9.2 可选字段

以下字段可能为 `null`，前端需要做容错处理：
- GPS坐标（GPS失败时）
- 地址信息（逆地理编码失败时）
- 驾驶员头像（未上传时）
- 某些统计数据（数据不足时）

### 9.3 数据更新频率

| 数据类型 | 更新频率 | 说明 |
|---------|---------|------|
| 实时车辆位置 | 每20秒 | 通过WebSocket推送 |
| 实时告警 | 立即 | 事件发生时推送 |
| 统计数据 | 每5分钟 | 定时刷新 |
| 图表数据 | 按需请求 | 用户操作时加载 |

---

## 🎯 10. 总结

本文档提供了数据展示大屏所需的所有数据接口和格式规范，包括：

1. ✅ **实时监控数据** - 车辆状态、告警事件、系统状态
2. ✅ **统计数据** - 事件统计、时间段分析、区域分析、驾驶员统计
3. ✅ **地图数据** - 车辆位置、轨迹回放、热力图
4. ✅ **图表数据** - 趋势曲线、分布图、排行图
5. ✅ **驾驶员信息** - 基本信息、行程记录、安全评分
6. ✅ **实时推送** - WebSocket事件推送

所有数据格式统一、结构清晰，便于前端开发和UI设计。

---

**文档版本**: v1.0  
**更新时间**: 2025-01-15  
**适用版本**: Muse Pi Pro Plus v1.0+

