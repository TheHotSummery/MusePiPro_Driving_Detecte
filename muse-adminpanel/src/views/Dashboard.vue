<template>
  <div class="dashboard">
    <!-- 管理员模式切换按钮 -->
    <div class="admin-toggle">
      <el-button 
        :type="isAdminMode ? 'danger' : 'primary'"
        :icon="isAdminMode ? 'Lock' : 'Unlock'"
        @click="toggleAdminMode"
        size="small"
      >
        {{ isAdminMode ? '退出管理' : '管理模式' }}
      </el-button>
    </div>

    <!-- 总览区域 -->
    <el-row :gutter="20" class="overview-section">
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <StatCard
          title="设备总数"
          :value="overview.deviceStatus?.total || 0"
          icon="Monitor"
          color="primary"
          suffix="台"
          :highlight="true"
        />
      </el-col>
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <StatCard
          title="在线设备"
          :value="overview.deviceStatus?.online || 0"
          icon="CircleCheck"
          color="success"
          suffix="台"
          :pulse="overview.deviceStatus?.online > 0"
        />
      </el-col>
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <StatCard
          title="今日事件"
          :value="overview.eventSummary?.today?.total || 0"
          icon="Warning"
          color="primary"
          suffix="起"
          :pulse="overview.eventSummary?.today?.total > 0"
        />
      </el-col>
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <StatCard
          title="活跃告警"
          :value="overview.alertSummary?.active || 0"
          icon="Bell"
          color="danger"
          suffix="条"
          :pulse="overview.alertSummary?.active > 0"
          :highlight="overview.alertSummary?.active > 0"
          :urgent="overview.alertSummary?.active > 0"
        />
      </el-col>
    </el-row>

    <!-- 实时状态指示器 -->
    <el-row :gutter="20" class="status-indicators">
      <el-col :xs="24" :sm="8" :md="8" :lg="8" :xl="8">
        <div class="status-indicator">
          <div class="indicator-header">
            <span>系统状态</span>
            <div class="status-dot" :class="systemStatus">
              <div class="pulse-ring"></div>
            </div>
          </div>
          <div class="status-info">
            <div class="status-item">
              <span>WebSocket连接</span>
              <el-tag :type="wsConnected ? 'success' : 'danger'" size="small">
                {{ wsConnected ? '已连接' : '断开' }}
              </el-tag>
            </div>
            <div class="status-item">
              <span>数据更新</span>
              <span class="update-time">{{ lastUpdateTime }}</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8" :md="8" :lg="8" :xl="8">
        <div class="status-indicator">
          <div class="indicator-header">
            <span>实时事件流</span>
            <div class="event-stream">
              <div class="stream-dot" v-for="i in 5" :key="i" :style="{ animationDelay: `${i * 0.2}s` }"></div>
            </div>
          </div>
          <div class="stream-info">
            <div class="stream-item">
              <span>最近事件</span>
              <span class="event-count">{{ recentEventCount }}</span>
            </div>
            <div class="stream-item">
              <span>处理速度</span>
              <span class="processing-speed">{{ processingSpeed }}ms</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8" :md="8" :lg="8" :xl="8">
        <div class="status-indicator alert-indicator" :class="{ 'has-critical-alerts': criticalAlertCount > 0 }">
          <div class="indicator-header">
            <span>告警监控</span>
            <div class="alert-monitor" :class="{ 'has-alerts': activeAlerts.length > 0 }">
              <el-icon><Bell /></el-icon>
              <div v-if="criticalAlertCount > 0" class="alert-badge">{{ criticalAlertCount }}</div>
            </div>
          </div>
          <div class="alert-info">
            <div class="alert-item">
              <span>严重告警</span>
              <span class="critical-count" :class="{ 'critical': criticalAlertCount > 0 }">{{ criticalAlertCount }}</span>
            </div>
            <div class="alert-item">
              <span>响应时间</span>
              <span class="response-time">{{ responseTime }}s</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 地图和图表区域 -->
    <el-row :gutter="20" class="monitoring-section">
      <!-- 地图区域 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <el-card class="map-section" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>实时设备监控</span>
              <div class="header-actions">
                <div class="map-stats">
                  <el-tag size="small" type="success">在线 {{ onlineDeviceCount }}</el-tag>
                  <el-tag size="small" type="warning">离线 {{ offlineDeviceCount }}</el-tag>
                </div>
                <el-select 
                  v-model="mapStatusFilter" 
                  placeholder="筛选设备状态" 
                  size="small"
                  clearable
                  style="margin-right: 10px; width: 150px;"
                  @change="updateMapFilter"
                >
                  <el-option label="全部设备" value="" />
                  <el-option label="在线设备" value="ONLINE" />
                  <el-option label="离线设备" value="OFFLINE" />
                  <el-option label="失联设备" value="LOST" />
                </el-select>
                <el-button size="small" @click="refreshMap">
                  <el-icon><Refresh /></el-icon>
                  刷新
                </el-button>
                <!-- 只在管理员模式下显示配置按钮 -->
                <el-button 
                  v-if="isAdminMode" 
                  size="small" 
                  @click="showMapConfig"
                >
                  <el-icon><Setting /></el-icon>
                  配置
                </el-button>
              </div>
            </div>
          </template>
          <SimpleMap 
            ref="mapComponent"
            :devices="filteredDevices" 
            @device-click="handleDeviceClick"
            @map-ready="onMapReady"
          />
        </el-card>
      </el-col>

      <!-- 设备详情区域 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" v-if="selectedDevice">
        <DeviceDetailCard :device="selectedDevice" />
      </el-col>

      <!-- 实时监控图表区域 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <el-row :gutter="20" class="charts-row">
          <!-- 事件趋势图表 -->
          <el-col :span="24">
            <el-card class="chart-card" shadow="hover">
              <template #header>
                <span>事件趋势</span>
              </template>
              <EventTrendChart 
                :data="eventData" 
                :loading="loading"
                @period-change="onEventPeriodChange"
              />
            </el-card>
          </el-col>
        </el-row>
      </el-col>
    </el-row>

    <!-- 疲劳分析图表区域 -->
    <el-row :gutter="20" class="analysis-section">
      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span>疲劳行为分析</span>
          </template>
          <FatigueAnalysisChart 
            :data="eventData" 
            :loading="loading"
            @type-change="onFatigueTypeChange"
          />
        </el-card>
      </el-col>

      <!-- 实时告警信息 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <el-card class="alert-panel" :class="{ 'has-critical-alerts': criticalAlertCount > 0 }" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>实时告警</span>
              <div class="alert-header-badge">
                <el-badge :value="activeAlerts.length" :hidden="activeAlerts.length === 0" type="danger">
                  <el-icon><Bell /></el-icon>
                </el-badge>
                <span v-if="criticalAlertCount > 0" class="critical-indicator">严重告警 {{ criticalAlertCount }}</span>
              </div>
            </div>
          </template>
          <div class="alert-list">
            <div 
              v-for="alert in activeAlerts.slice(0, 5)" 
              :key="alert.alertId"
              class="alert-item"
              :class="`alert-${alert.severity.toLowerCase()}`"
              @click="handleAlertClick(alert)"
            >
              <div class="alert-icon">
                <el-icon :class="getAlertIcon(alert.severity)">
                  <component :is="getAlertIcon(alert.severity)" />
                </el-icon>
              </div>
              <div class="alert-content">
                <div class="alert-title">{{ alert.message }}</div>
                <div class="alert-meta">
                  <span class="alert-device">
                    <el-icon><Monitor /></el-icon>
                    {{ alert.deviceId }}
                  </span>
                  <span class="alert-time">
                    <el-icon><Clock /></el-icon>
                    {{ formatTime(alert.timestamp) }}
                  </span>
                </div>
              </div>
              <div class="alert-severity">
                <el-tag :type="getSeverityColor(alert.severity)" size="small">
                  {{ getSeverityText(alert.severity) }}
                </el-tag>
              </div>
            </div>
            <div v-if="activeAlerts.length === 0" class="no-alerts">
              <el-icon><Check /></el-icon>
              <span>暂无活跃告警</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 设备列表区域 - 只在管理员模式下显示详细列表 -->
    <el-card v-if="isAdminMode" class="device-list-section" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>设备管理</span>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索设备ID或驾驶员"
              size="small"
              style="width: 200px; margin-right: 10px;"
              clearable
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button size="small" @click="refreshDevices">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>
      
      <el-table 
        :data="filteredDevices" 
        v-loading="deviceStore.loading"
        stripe
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <el-table-column prop="deviceId" label="设备ID" width="150" />
        <el-table-column prop="username" label="驾驶员" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <DeviceStatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="healthScore" label="网络健康状态" width="120">
          <template #default="{ row }">
            <HealthProgress :score="row.healthScore || 0" />
          </template>
        </el-table-column>
        <el-table-column prop="lastSeen" label="最后心跳" width="150">
          <template #default="{ row }">
            {{ formatLastSeen(row.lastSeen) }}
          </template>
        </el-table-column>
        <el-table-column prop="location" label="位置" min-width="200">
          <template #default="{ row }">
            <span v-if="row.location && row.location.lat !== null && row.location.lng !== null">
              {{ row.location.lat.toFixed(6) }}, {{ row.location.lng.toFixed(6) }}
            </span>
            <span v-else class="text-muted">未知</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button 
              size="small" 
              type="primary" 
              @click.stop="viewDeviceDetail(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 监控模式下显示简化的设备状态 -->
    <el-card v-else class="device-status-section" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>设备状态概览</span>
          <el-button size="small" @click="refreshDevices">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>
      
      <div class="device-status-grid">
        <div 
          v-for="device in filteredDevices.slice(0, 8)" 
          :key="device.deviceId"
          class="device-status-item clean"
          @click="handleDeviceClick(device)"
        >
          <!-- 设备头部信息 -->
          <div class="device-header">
            <div class="device-info">
              <div class="device-id">{{ device.deviceId }}</div>
              <div class="device-type">{{ device.deviceType || 'Muse Pi Pro Plus' }}</div>
            </div>
            <DeviceStatusTag :status="device.status" />
          </div>
          
          <!-- 用户信息 -->
          <div class="user-section">
            <div class="username">{{ device.username || device.user?.username || '未绑定' }}</div>
            <div class="last-seen">⏰ {{ formatLastSeen(device.lastSeen) }}</div>
          </div>
          
          <!-- 实时信息（仅在线设备） -->
          <div class="realtime-section" v-if="device.status === 'ONLINE'">
            <div class="realtime-grid">
              <div class="realtime-item" v-if="device.currentLocation && device.currentLocation.speed !== null">
                <div class="realtime-label">速度</div>
                <el-tag 
                  :type="getSpeedTagType(device.currentLocation.speed)" 
                  size="small"
                  effect="dark"
                >
                  {{ device.currentLocation.speed.toFixed(1) }} km/h
                </el-tag>
              </div>
              <div class="realtime-item" v-if="device.currentLocation && device.currentLocation.direction !== null">
                <div class="realtime-label">方向</div>
                <div class="realtime-value">{{ device.currentLocation.direction.toFixed(0) }}°</div>
              </div>
              <div class="realtime-item" v-if="device.currentLocation && device.currentLocation.satellites !== null">
                <div class="realtime-label">卫星</div>
                <div class="realtime-value">{{ device.currentLocation.satellites }} 颗</div>
              </div>
              <div class="realtime-item" v-if="device.currentLocation && device.currentLocation.altitude !== null">
                <div class="realtime-label">海拔</div>
                <div class="realtime-value">{{ device.currentLocation.altitude.toFixed(1) }}m</div>
              </div>
            </div>
            <!-- 如果在线但没有实时数据，显示提示 -->
            <div v-if="!device.currentLocation" class="no-realtime-data">
              <div class="no-data-message">暂无实时数据</div>
            </div>
          </div>
          
          <!-- 离线提示 -->
          <div v-else class="offline-section">
            <div class="offline-message">设备离线</div>
          </div>
          
          <!-- 网络健康状态 -->
          <div class="health-section">
            <div class="health-label">网络健康状态</div>
            <HealthProgress :score="device.healthScore || 0" />
          </div>
        </div>
      </div>
    </el-card>

    <!-- 地图配置对话框 -->
    <el-dialog
      v-model="mapConfigVisible"
      title="地图配置"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="mapConfig" label-width="120px">
        <el-form-item label="高德地图API Key">
          <el-input
            v-model="mapConfig.apiKey"
            type="password"
            placeholder="请输入高德地图API Key"
            show-password
          />
          <div class="form-tip">
            请到 <a href="https://console.amap.com/" target="_blank">高德开放平台</a> 申请API Key
            <br>
            <a href="/map-config-help" target="_blank">查看详细配置帮助</a>
          </div>
        </el-form-item>
        <el-form-item label="安全密钥">
          <el-input
            v-model="mapConfig.securityKey"
            type="password"
            placeholder="请输入安全密钥（可选）"
            show-password
          />
          <div class="form-tip">
            2021年后申请的API Key需要配置安全密钥，请到控制台获取
          </div>
        </el-form-item>
        <el-form-item label="API版本">
          <el-select v-model="mapConfig.version" placeholder="选择API版本">
            <el-option label="基础版 v1.4.15" value="1.4.15" />
            <el-option label="标准版 v2.0" value="2.0" />
          </el-select>
          <div class="form-tip">
            基础版功能较少但调用限制较少，标准版功能完整但有限制
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mapConfigVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMapConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

// 组件名称
defineOptions({
  name: 'DashboardView'
})
import { ElMessage } from 'element-plus'
import { 
  Refresh, 
  Setting, 
  Search,
  Bell,
  Check,
  Monitor,
  Clock
} from '@element-plus/icons-vue'

import StatCard from '@/components/common/StatCard.vue'
import DeviceStatusTag from '@/components/common/DeviceStatusTag.vue'
import HealthProgress from '@/components/common/HealthProgress.vue'
import SimpleMap from '@/components/maps/SimpleMap.vue'
import EventTrendChart from '@/components/charts/EventTrendChart.vue'
import FatigueAnalysisChart from '@/components/charts/FatigueAnalysisChart.vue'
import DeviceDetailCard from '@/components/common/DeviceDetailCard.vue'

import { useDeviceStore } from '@/stores/device'
import { useOverviewStore } from '@/stores/overview'
import { useEventStore } from '@/stores/event'
import { useAlertStore } from '@/stores/alert'
import wsManager from '@/utils/websocket'

const router = useRouter()
const deviceStore = useDeviceStore()
const overviewStore = useOverviewStore()
const eventStore = useEventStore()

// 响应式数据
const overview = ref({})
const realtimeDevices = ref([])
const searchKeyword = ref('')
const mapStatusFilter = ref('')
const mapConfigVisible = ref(false)
const isAdminMode = ref(false) // 管理员模式状态
const loading = ref(false)
const eventData = ref([]) // 事件数据
const activeAlerts = ref([]) // 活跃告警
const wsConnected = ref(false) // WebSocket连接状态
const lastUpdateTime = ref('刚刚') // 最后更新时间
const recentEventCount = ref(0) // 最近事件数量
const processingSpeed = ref(150) // 处理速度
const criticalAlertCount = ref(0) // 严重告警数量
const responseTime = ref(2.5) // 响应时间
const mapConfig = ref({
  apiKey: '',
  securityKey: '',
  version: '1.4.15'
})

// 地图组件引用
const mapComponent = ref(null)
const selectedDevice = ref(null) // 选中的设备

// 计算属性
const filteredDevices = computed(() => {
  console.log('计算filteredDevices，当前设备数量:', realtimeDevices.value.length)
  console.log('设备列表:', realtimeDevices.value)
  console.log('搜索关键词:', searchKeyword.value)
  console.log('地图状态筛选:', mapStatusFilter.value)
  
  let filtered = realtimeDevices.value
  
  // 按搜索关键词筛选
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(device => 
      device.deviceId.toLowerCase().includes(keyword) ||
      (device.username && device.username.toLowerCase().includes(keyword))
    )
  }
  
  // 按地图状态筛选
  if (mapStatusFilter.value) {
    filtered = filtered.filter(device => device.status === mapStatusFilter.value)
  }
  
  console.log('过滤后设备数量:', filtered.length)
  return filtered
})

// 系统状态计算属性
const systemStatus = computed(() => {
  if (wsConnected.value && overview.value.deviceStatus?.online > 0) {
    return 'healthy'
  } else if (wsConnected.value) {
    return 'warning'
  } else {
    return 'error'
  }
})

// 设备统计
const onlineDeviceCount = computed(() => {
  return deviceStore.devices.filter(device => device.status === 'ONLINE').length
})

const offlineDeviceCount = computed(() => {
  return deviceStore.devices.filter(device => device.status === 'OFFLINE' || device.status === 'LOST').length
})

// 方法
const loadData = async () => {
  try {
    console.log('=== 开始并行加载数据 ===')
    loading.value = true
    // 并行加载数据
    await Promise.all([
      loadOverview(),
      loadDevices(),
      loadRealtimeData(),
      loadEventData(),
      loadActiveAlerts()
    ])
    console.log('=== 数据加载完成 ===')
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadOverview = async () => {
  try {
    const data = await overviewStore.fetchOverview()
    overview.value = data
  } catch (error) {
    console.error('加载概览数据失败:', error)
  }
}

const loadDevices = async () => {
  try {
    console.log('=== Dashboard: 开始加载设备列表 ===')
    await deviceStore.fetchDevices()
    console.log('=== Dashboard: 设备列表加载完成 ===')
    console.log('当前设备数量:', deviceStore.devices.length)
    console.log('设备列表:', deviceStore.devices)
    
    // 检查设备数据
    if (deviceStore.devices.length > 0) {
      console.log('第一个设备详情:', deviceStore.devices[0])
      console.log('第一个设备用户名:', deviceStore.devices[0].username)
      console.log('第一个设备ID:', deviceStore.devices[0].deviceId)
    }
  } catch (error) {
    console.error('加载设备列表失败:', error)
  }
}

const loadRealtimeData = async () => {
  try {
    console.log('=== 开始加载实时数据 ===')
    
    // 1. 获取设备列表
    await deviceStore.fetchDevices()
    console.log('设备列表:', deviceStore.devices)
    
    // 2. 为每个设备获取详细信息（包含完整的currentLocation数据）
    const devicesWithDetails = await Promise.all(
      deviceStore.devices.map(async (device) => {
        try {
          console.log(`获取设备 ${device.deviceId} 的详细信息`)
          const deviceDetail = await deviceStore.fetchDeviceDetail(device.deviceId)
          console.log(`设备 ${device.deviceId} 详情:`, deviceDetail)
          
          // 合并设备列表数据和详细信息
          return {
            ...device,
            ...deviceDetail,
            // 确保currentLocation数据存在
            currentLocation: deviceDetail.currentLocation || null
          }
        } catch (error) {
          console.error(`获取设备 ${device.deviceId} 详情失败:`, error)
          // 如果获取详情失败，返回原始设备数据
          return {
            ...device,
            currentLocation: null
          }
        }
      })
    )
    
    realtimeDevices.value = devicesWithDetails
    console.log('合并后的设备数据:', realtimeDevices.value)
    console.log('合并后设备数量:', realtimeDevices.value.length)
    
    // 检查第一个设备的currentLocation数据
    if (realtimeDevices.value.length > 0) {
      const firstDevice = realtimeDevices.value[0]
      console.log('第一个设备详情:', firstDevice)
      console.log('第一个设备的currentLocation:', firstDevice.currentLocation)
    }
    
  } catch (error) {
    console.error('加载实时数据失败:', error)
    
    // 检查错误类型
    if (error.code === 'ERR_NETWORK' || error.message === 'Network Error') {
      console.log('网络错误，使用设备列表作为备选')
      // 静默处理网络错误，不显示错误消息
    } else {
      console.log('其他错误，使用设备列表作为备选')
    }
    
    // 如果API失败，使用设备列表作为备选
    realtimeDevices.value = deviceStore.devices || []
  }
}

// 加载事件数据
const loadEventData = async () => {
  try {
    console.log('=== 开始加载事件数据 ===')
    
    // 调用真实的事件API
    const eventStore = useEventStore()
    
    // 获取最近7天的事件数据，包含所有类型和严重程度
    const params = {
      startTime: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(), // 7天前
      endTime: new Date().toISOString(), // 现在
      // 不限制设备ID，获取所有设备的事件
      // 不限制事件类型，获取所有类型的事件
      // 不限制严重程度，获取所有严重程度的事件
    }
    
    console.log('事件查询参数:', params)
    const data = await eventStore.fetchEvents(params)
    eventData.value = data.events || []
    
    console.log('事件数据加载完成:', eventData.value.length, '个事件')
    console.log('事件数据详情:', eventData.value)
    
    // 按事件类型分组统计
    const eventTypeStats = {}
    eventData.value.forEach(event => {
      eventTypeStats[event.eventType] = (eventTypeStats[event.eventType] || 0) + 1
    })
    console.log('事件类型统计:', eventTypeStats)
    
  } catch (error) {
    console.error('加载事件数据失败:', error)
    // 如果API调用失败，使用空数组而不是模拟数据
    eventData.value = []
    ElMessage.warning('事件数据加载失败，请检查网络连接')
  }
}

// 加载活跃告警
const loadActiveAlerts = async () => {
  try {
    console.log('=== 开始加载活跃告警 ===')
    
    // 调用真实的告警API
    const alertStore = useAlertStore()
    const data = await alertStore.fetchRealtimeAlerts()
    
    // 按时间排序：新到旧
    const sortedAlerts = (data.alerts || []).sort((a, b) => {
      return new Date(b.timestamp) - new Date(a.timestamp)
    })
    
    activeAlerts.value = sortedAlerts
    console.log('活跃告警加载完成:', activeAlerts.value.length, '个告警')
    console.log('告警数据:', activeAlerts.value)
    
  } catch (error) {
    console.error('加载活跃告警失败:', error)
    activeAlerts.value = []
    ElMessage.warning('告警数据加载失败，请检查网络连接')
  }
}

const refreshMap = () => {
  loadRealtimeData()
}

// 强制刷新地图组件（保留以备后用）
// const forceRefreshMap = () => {
//   if (mapComponent.value) {
//     mapComponent.value.forceRefresh()
//   }
// }

const refreshDevices = () => {
  loadDevices()
}

const updateMapFilter = () => {
  console.log('更新地图筛选:', mapStatusFilter.value)
  // 筛选会自动通过computed属性更新
}

// 切换管理员模式
const toggleAdminMode = () => {
  isAdminMode.value = !isAdminMode.value
  ElMessage({
    message: isAdminMode.value ? '已进入管理模式' : '已退出管理模式',
    type: isAdminMode.value ? 'warning' : 'info',
    duration: 2000
  })
}

// 图表事件处理
const onEventPeriodChange = async (period) => {
  console.log('事件周期变化:', period)
  
  try {
    // 根据选择的周期重新加载数据
    const now = new Date()
    let startTime
    
    switch (period) {
      case 'today':
        startTime = new Date(now.getFullYear(), now.getMonth(), now.getDate())
        break
      case 'week':
        startTime = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
        break
      case 'month':
        startTime = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000)
        break
      default:
        startTime = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
    }
    
    const params = {
      startTime: startTime.toISOString(),
      endTime: now.toISOString()
    }
    
    console.log('重新加载事件数据，参数:', params)
    const data = await eventStore.fetchEvents(params)
    eventData.value = data.events || []
    
    console.log(`加载${period}事件数据完成:`, eventData.value.length, '个事件')
  } catch (error) {
    console.error('重新加载事件数据失败:', error)
    ElMessage.error('重新加载事件数据失败')
  }
}

const onFatigueTypeChange = async (type) => {
  console.log('疲劳分析类型变化:', type)
  
  try {
    // 根据选择的类型重新加载疲劳事件数据
    const params = {
      eventType: 'FATIGUE',
      startTime: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(),
      endTime: new Date().toISOString()
    }
    
    console.log('重新加载疲劳事件数据，参数:', params)
    const data = await eventStore.fetchEvents(params)
    
    // 只更新疲劳相关的事件数据
    const fatigueEvents = data.events?.filter(event => event.eventType === 'FATIGUE') || []
    eventData.value = eventData.value.filter(event => event.eventType !== 'FATIGUE').concat(fatigueEvents)
    
    console.log(`加载${type}疲劳数据完成:`, fatigueEvents.length, '个疲劳事件')
  } catch (error) {
    console.error('重新加载疲劳数据失败:', error)
    ElMessage.error('重新加载疲劳数据失败')
  }
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '未知'
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (seconds < 60) return `${seconds}秒前`
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  // 超过7天显示具体日期和时间
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取严重程度文本
const getSeverityText = (severity) => {
  const severityMap = {
    LOW: '轻微',
    MEDIUM: '中等',
    HIGH: '严重',
    CRITICAL: '危急'
  }
  return severityMap[severity] || '未知'
}

// 获取速度标签类型（颜色分级）
const getSpeedTagType = (speed) => {
  if (speed === null || speed === undefined) return 'info'
  
  if (speed >= 0 && speed < 60) {
    return 'success' // 绿色：0-60 km/h
  } else if (speed >= 60 && speed < 100) {
    return 'warning' // 黄色：60-100 km/h
  } else if (speed >= 100 && speed < 120) {
    return '' // 橙色：100-120 km/h (使用默认橙色)
  } else if (speed >= 120) {
    return 'danger' // 红色：120+ km/h
  }
  
  return 'info' // 默认
}


// 获取严重程度颜色
const getSeverityColor = (severity) => {
  const colorMap = {
    LOW: 'success',
    MEDIUM: 'warning',
    HIGH: 'danger',
    CRITICAL: 'danger'
  }
  return colorMap[severity] || 'info'
}

const getAlertIcon = (severity) => {
  const iconMap = {
    LOW: 'Check',
    MEDIUM: 'Warning', 
    HIGH: 'Bell',
    CRITICAL: 'Bell'
  }
  return iconMap[severity] || 'Check'
}

const handleAlertClick = (alert) => {
  console.log('点击告警:', alert)
  // 可以跳转到告警详情页面
  ElMessage.info(`查看告警详情: ${alert.message}`)
}

const handleDeviceClick = (device) => {
  console.log('设备点击:', device)
  selectedDevice.value = device
  
  // 获取设备详细信息
  if (device.deviceId) {
    loadDeviceDetail(device.deviceId)
  }
}

// 加载设备详细信息
const loadDeviceDetail = async (deviceId) => {
  try {
    console.log('加载设备详情:', deviceId)
    const deviceDetail = await deviceStore.fetchDeviceDetail(deviceId)
    console.log('设备详情:', deviceDetail)
    
    // 更新选中设备的信息
    if (selectedDevice.value && selectedDevice.value.deviceId === deviceId) {
      selectedDevice.value = { ...selectedDevice.value, ...deviceDetail }
    }
  } catch (error) {
    console.error('加载设备详情失败:', error)
    ElMessage.error('加载设备详情失败')
  }
}

const onMapReady = (map) => {
  console.log('地图准备就绪:', map)
}

const handleRowClick = (row) => {
  viewDeviceDetail(row)
}

const viewDeviceDetail = (device) => {
  router.push(`/devices/${device.deviceId}`)
}

const showMapConfig = () => {
  mapConfig.value.apiKey = localStorage.getItem('amap_api_key') || ''
  mapConfig.value.securityKey = localStorage.getItem('amap_security_key') || ''
  mapConfig.value.version = localStorage.getItem('amap_version') || '1.4.15'
  mapConfigVisible.value = true
}

const saveMapConfig = () => {
  if (!mapConfig.value.apiKey.trim()) {
    ElMessage.warning('请输入API Key')
    return
  }
  
  localStorage.setItem('amap_api_key', mapConfig.value.apiKey)
  localStorage.setItem('amap_security_key', mapConfig.value.securityKey)
  localStorage.setItem('amap_version', mapConfig.value.version)
  mapConfigVisible.value = false
  ElMessage.success('地图配置已保存')
  
  // 重新加载页面以应用新配置
  setTimeout(() => {
    window.location.reload()
  }, 1000)
}

const formatLastSeen = (lastSeen) => {
  if (!lastSeen) return '未知'
  
  const date = new Date(lastSeen)
  const now = new Date()
  const diff = now - date
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (seconds < 60) return `${seconds}秒前`
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  // 超过7天显示具体日期和时间
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// WebSocket事件处理
const handleRealtimeData = (data) => {
  console.log('收到实时数据推送:', data)
  const { type, payload } = data
  
  if (type === 'realtime_data') {
    const { deviceId, data: deviceData, timestamp } = payload
    
    // 更新设备实时位置
    const device = realtimeDevices.value.find(d => d.deviceId === deviceId)
    if (device) {
      // 记录位置变化
      const oldLocation = device.location
      const newLocation = deviceData.location
      
      // 更新设备信息
      Object.assign(device, {
        location: deviceData.location,
        speed: deviceData.speed,
        direction: deviceData.direction,
        fatigueLevel: deviceData.fatigueLevel,
        fatigueScore: deviceData.fatigueScore,
        lastSeen: new Date(timestamp).toISOString()
      })
      
      // 检查位置是否发生显著变化
      if (oldLocation && newLocation) {
        const latDiff = Math.abs(newLocation.lat - oldLocation.lat)
        const lngDiff = Math.abs(newLocation.lng - oldLocation.lng)
        const threshold = 0.0001 // 约10米
        
        if (latDiff > threshold || lngDiff > threshold) {
          console.log(`设备 ${deviceId} 位置发生显著变化:`, {
            from: oldLocation,
            to: newLocation,
            latDiff,
            lngDiff
          })
          
          // 显示位置更新通知
          ElMessage.info(`设备 ${deviceId} 位置已更新`)
        }
      }
    } else {
      // 添加新设备
      realtimeDevices.value.push({
        deviceId: deviceId,
        ...deviceData,
        lastSeen: new Date(timestamp).toISOString()
      })
      
      console.log(`新设备 ${deviceId} 上线`)
      ElMessage.success(`新设备 ${deviceId} 已上线`)
    }
    
    // 触发地图更新
    if (mapComponent.value) {
      mapComponent.value.updateMarkers()
    }
    
    // 更新实时统计
    updateRealtimeStats()
  }
}

// 处理设备状态更新
const handleDeviceStatusUpdate = (data) => {
  console.log('收到设备状态更新:', data)
  const { type, payload } = data
  
  if (type === 'device_status') {
    const { deviceId, status, timestamp } = payload
    
    // 更新设备状态
    const device = realtimeDevices.value.find(d => d.deviceId === deviceId)
    if (device) {
      device.status = status
      device.lastSeen = new Date(timestamp).toISOString()
    }
    
    // 显示状态变化通知
    ElMessage.info(`设备 ${deviceId} 状态变更为 ${status}`)
  }
}

const handleAlert = (data) => {
  console.log('收到告警推送:', data)
  const { type, payload } = data
  
  if (type === 'alert') {
    const { alertType, message, severity } = payload
    
    // 显示告警通知
    const alertTypeMap = {
      'CRITICAL': 'error',
      'HIGH': 'warning', 
      'MEDIUM': 'info',
      'LOW': 'success'
    }
    
    ElMessage({
      type: alertTypeMap[severity] || 'warning',
      message: `${alertType}: ${message}`,
      duration: severity === 'CRITICAL' ? 0 : 5000, // 严重告警不自动关闭
      showClose: true
    })
    
    // 播放告警声音（如果有）
    if (severity === 'CRITICAL' || severity === 'HIGH') {
      playAlertSound(severity)
    }
  }
}

// 处理事件通知
const handleEvent = (data) => {
  console.log('收到事件通知:', data)
  const { type, payload } = data
  
  if (type === 'event') {
    const { eventType } = payload
    
    // 更新事件统计
    if (overview.value.eventSummary) {
      const today = new Date().toDateString()
      if (!overview.value.eventSummary[today]) {
        overview.value.eventSummary[today] = { total: 0, fatigue: 0, distraction: 0, emergency: 0 }
      }
      overview.value.eventSummary[today].total++
      overview.value.eventSummary[today][eventType.toLowerCase()]++
    }
  }
}

// 播放告警声音
const playAlertSound = (severity) => {
  try {
    const audio = new Audio(`/static/audio/alert_${severity.toLowerCase()}.mp3`)
    audio.play().catch(error => {
      console.log('无法播放告警声音:', error)
    })
  } catch (error) {
    console.log('告警声音文件不存在:', error)
  }
}

// 实时数据更新
const updateRealtimeStats = () => {
  lastUpdateTime.value = '刚刚'
  recentEventCount.value = eventData.value.length
  criticalAlertCount.value = activeAlerts.value.filter(alert => 
    alert.severity === 'CRITICAL' || alert.severity === 'HIGH'
  ).length
  
  // 模拟处理速度和响应时间的变化
  processingSpeed.value = Math.floor(Math.random() * 50) + 100 // 100-150ms
  responseTime.value = (Math.random() * 2 + 1).toFixed(1) // 1.0-3.0s
}

// WebSocket状态处理
const handleWebSocketStatus = (connected) => {
  wsConnected.value = connected
  console.log('WebSocket状态变化:', connected ? '已连接' : '已断开')
}

// 生命周期
onMounted(async () => {
  console.log('=== Dashboard组件已挂载 ===')
  console.log('开始加载数据...')
  console.log('当前时间:', new Date().toISOString())
  
  try {
    await loadData()
    updateRealtimeStats()
    console.log('=== Dashboard数据加载完成 ===')
  } catch (error) {
    console.error('=== Dashboard数据加载失败 ===', error)
  }
  
  // 连接WebSocket
  console.log('开始连接WebSocket...')
  try {
    wsManager.connect()
    console.log('WebSocket连接请求已发送')
  } catch (error) {
    console.error('WebSocket连接失败:', error)
  }
  
  // 监听WebSocket事件
  wsManager.on('connected', handleWebSocketStatus)
  wsManager.on('disconnected', handleWebSocketStatus)
  wsManager.on('realtime_data', handleRealtimeData)
  wsManager.on('device_status', handleDeviceStatusUpdate)
  wsManager.on('alert', handleAlert)
  wsManager.on('event', handleEvent)
  
  // 初始化WebSocket连接状态
  wsConnected.value = wsManager.getConnectionStatus().isConnected
  
  // 定时刷新数据
  const refreshInterval = setInterval(() => {
    console.log('=== 定时刷新实时数据 ===')
    // 只刷新设备列表，不频繁调用可能不可用的实时数据流API
    loadDevices()
    updateRealtimeStats()
  }, 30000) // 每30秒刷新一次，减少频率
  
  // 实时状态更新
  const statusUpdateInterval = setInterval(() => {
    updateRealtimeStats()
  }, 5000) // 每5秒更新一次状态
  
  // 清理定时器
  onUnmounted(() => {
    clearInterval(refreshInterval)
    clearInterval(statusUpdateInterval)
    wsManager.off('connected', handleWebSocketStatus)
    wsManager.off('disconnected', handleWebSocketStatus)
    wsManager.off('realtime_data', handleRealtimeData)
    wsManager.off('device_status', handleDeviceStatusUpdate)
    wsManager.off('alert', handleAlert)
    wsManager.off('event', handleEvent)
  })
})
</script>

<style scoped lang="scss">
.dashboard {
  padding: 20px;
  background: #f5f7fa;
  height: calc(100vh - 60px);
  overflow-y: auto;
  overflow-x: hidden; // 隐藏水平滚动条
}

.admin-toggle {
  position: fixed;
  top: 80px;
  right: 20px;
  z-index: 1000;
}

.overview-section {
  margin-bottom: 20px;
}

.status-indicators {
  margin-bottom: 20px;
}

.status-indicator {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  border-left: 4px solid #409EFF;
  height: 140px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  
  &.alert-indicator {
    border-left-color: #F56C6C;
    
    &.has-critical-alerts {
      background: linear-gradient(135deg, #fef0f0 0%, #fff 100%);
      border: 2px solid #F56C6C;
      animation: alert-pulse 2s ease-in-out infinite;
    }
  }
}

.indicator-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  
  span {
    font-weight: 600;
    color: #303133;
    font-size: 16px;
  }
}

.status-dot {
  position: relative;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  
  &.healthy {
    background: #67C23A;
  }
  
  &.warning {
    background: #E6A23C;
  }
  
  &.error {
    background: #F56C6C;
  }
}

.pulse-ring {
  position: absolute;
  top: -4px;
  left: -4px;
  width: 20px;
  height: 20px;
  border: 2px solid currentColor;
  border-radius: 50%;
  animation: pulse-ring 2s ease-out infinite;
}

.event-stream {
  display: flex;
  gap: 4px;
}

.stream-dot {
  width: 8px;
  height: 8px;
  background: #409EFF;
  border-radius: 50%;
  animation: stream-flow 1.5s ease-in-out infinite;
}

.alert-monitor {
  position: relative;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  transition: all 0.3s ease;
  
  &.has-alerts {
    color: #F56C6C;
    animation: alert-blink 1s ease-in-out infinite;
  }
}

.alert-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #F56C6C;
  color: #fff;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  font-size: 10px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: badge-pulse 1.5s ease-in-out infinite;
}

.critical-count {
  &.critical {
    color: #F56C6C !important;
    font-weight: 700 !important;
    font-size: 16px !important;
    animation: urgent-pulse 2s ease-in-out infinite;
  }
}

@keyframes urgent-pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
}

.status-info,
.stream-info,
.alert-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  justify-content: space-between;
  min-height: 60px;
}

.status-item,
.stream-item,
.alert-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  min-height: 24px;
  
  span:first-child {
    color: #606266;
    flex-shrink: 0;
  }
  
  span:last-child {
    color: #303133;
    font-weight: 500;
    text-align: right;
  }
}

.update-time,
.event-count,
.processing-speed,
.critical-count,
.response-time {
  color: #409EFF !important;
  font-weight: 600 !important;
}

.monitoring-section {
  margin-bottom: 20px;
}

.analysis-section {
  margin-bottom: 20px;
}

.map-section {
  height: 400px;
  min-height: 400px;
  
  :deep(.el-card__body) {
    height: calc(100% - 60px);
    padding: 0;
  }
}

.chart-card {
  height: 400px;
  min-height: 400px;
  
  :deep(.el-card__body) {
    height: calc(100% - 60px);
    padding: 0;
  }
}

.alert-panel {
  height: 400px;
  min-height: 400px;
  
  &.has-critical-alerts {
    border: 2px solid #F56C6C;
    background: linear-gradient(135deg, #fef0f0 0%, #fff 100%);
    animation: alert-panel-pulse 3s ease-in-out infinite;
  }
}

.alert-header-badge {
  display: flex;
  align-items: center;
  gap: 12px;
}

.critical-indicator {
  background: #F56C6C;
  color: #fff;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
  animation: critical-flash 2s ease-in-out infinite;
}

.device-list-section {
  height: 30vh;
  min-height: 250px;
}

.device-status-section {
  margin-bottom: 20px;
  height: 50vh;
  min-height: 500px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.map-stats {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-right: 10px;
}

.text-muted {
  color: #909399;
}

.device-status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
  padding: 16px;
  max-height: 600px;
  overflow-y: auto;
}

.device-status-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    border-color: #409EFF;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
    transform: translateY(-2px);
  }
}

.device-info {
  flex: 1;
  
  .device-id {
    font-weight: 600;
    color: #303133;
    font-size: 14px;
    margin-bottom: 4px;
  }
  
  .device-user {
    font-size: 12px;
    color: #909399;
  }
}

.device-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin: 0 16px;
}

.device-time {
  font-size: 12px;
  color: #606266;
  text-align: right;
  min-width: 80px;
}

.alert-list {
  padding: 16px;
  max-height: 320px;
  overflow-y: auto;
}

.alert-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 12px;
  background: #fff;
  border-left: 4px solid #e4e7ed;
  border-radius: 6px;
  transition: all 0.3s ease;
  cursor: pointer;

  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    transform: translateX(2px);
  }

  &.alert-critical {
    border-left-color: #f56c6c;
    background: #fef0f0;
  }

  &.alert-high {
    border-left-color: #e6a23c;
    background: #fdf6ec;
  }

  &.alert-medium {
    border-left-color: #409eff;
    background: #ecf5ff;
  }

  &.alert-low {
    border-left-color: #67c23a;
    background: #f0f9ff;
  }
}

.alert-icon {
  margin-right: 12px;
  font-size: 18px;
  
  .el-icon {
    &.Check {
      color: #67C23A;
    }
    &.Warning {
      color: #E6A23C;
    }
    &.Bell {
      color: #F56C6C;
    }
  }
}

.alert-content {
  flex: 1;
  
  .alert-title {
    font-weight: 500;
    color: #303133;
    font-size: 14px;
    margin-bottom: 4px;
  }
  
  .alert-meta {
    display: flex;
    gap: 16px;
    font-size: 12px;
    color: #909399;
  }
}

.alert-severity {
  margin-left: 12px;
}

.no-alerts {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #67c23a;
  font-size: 14px;
  
  .el-icon {
    font-size: 48px;
    margin-bottom: 12px;
  }
}

// 动画关键帧
@keyframes pulse-ring {
  0% {
    transform: scale(0.8);
    opacity: 1;
  }
  100% {
    transform: scale(1.4);
    opacity: 0;
  }
}

@keyframes stream-flow {
  0%, 100% {
    opacity: 0.3;
    transform: scale(0.8);
  }
  50% {
    opacity: 1;
    transform: scale(1.2);
  }
}

@keyframes alert-blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}

@keyframes alert-pulse {
  0%, 100% {
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  }
  50% {
    box-shadow: 0 4px 20px 0 rgba(245, 108, 108, 0.4);
  }
}

@keyframes badge-pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
}

@keyframes alert-panel-pulse {
  0%, 100% {
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  }
  50% {
    box-shadow: 0 4px 20px 0 rgba(245, 108, 108, 0.3);
  }
}

@keyframes critical-flash {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.8;
    transform: scale(1.05);
  }
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  
  a {
    color: #409EFF;
    text-decoration: none;
    
    &:hover {
      text-decoration: underline;
    }
  }
}

// 响应式布局
@media (max-width: 768px) {
  .dashboard {
    padding: 10px;
    min-width: 1200px; // 强制最小宽度
    overflow-x: auto; // 允许水平滚动
  }
  
  .admin-toggle {
    top: 70px;
    right: 10px;
  }
  
  .overview-section {
    .el-col {
      margin-bottom: 10px;
    }
  }
  
  .status-indicators {
    .el-col {
      margin-bottom: 16px;
    }
  }
  
  .status-indicator {
    padding: 16px;
  }
  
  .indicator-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    margin-bottom: 12px;
  }
  
  .status-info,
  .stream-info,
  .alert-info {
    gap: 6px;
  }
  
  .status-item,
  .stream-item,
  .alert-item {
    font-size: 13px;
  }
  
  .map-section,
  .chart-card,
  .alert-panel {
    height: 350px;
    min-height: 350px;
  }
  
  .device-list-section,
  .device-status-section {
    height: 60vh;
    min-height: 500px;
  }
  
  .device-status-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .device-status-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .device-status {
    flex-direction: row;
    margin: 0;
  }
  
  .header-actions {
    flex-direction: column;
    gap: 5px;
  }
}

/* 速度标签样式 */
:deep(.el-tag) {
  &.el-tag--success {
    background-color: #67C23A;
    border-color: #67C23A;
    color: #fff;
  }
  
  &.el-tag--warning {
    background-color: #E6A23C;
    border-color: #E6A23C;
    color: #fff;
  }
  
  &.el-tag--danger {
    background-color: #F56C6C;
    border-color: #F56C6C;
    color: #fff;
  }
  
  /* 橙色样式（100-120 km/h） */
  &:not(.el-tag--success):not(.el-tag--warning):not(.el-tag--danger):not(.el-tag--info) {
    background-color: #FF9800;
    border-color: #FF9800;
    color: #fff;
  }
}

/* 方向值样式 */
.direction-value {
  color: #409EFF;
  font-weight: 600;
  font-size: 13px;
}

/* 卫星数值样式 */
.satellite-value {
  color: #E6A23C;
  font-weight: 600;
  font-size: 13px;
}

/* 文本样式 */
.text-muted {
  color: #909399;
  font-style: italic;
}

/* 简洁版设备状态卡片样式 */
.device-status-item.clean {
  padding: 20px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  transition: all 0.3s ease;
  cursor: pointer;
  min-height: 280px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
    border-color: #409eff;
  }
}

.device-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  
  .device-info {
    flex: 1;
  }
  
  .device-id {
    font-weight: 700;
    color: #303133;
    font-size: 18px;
    line-height: 1.2;
    margin-bottom: 4px;
  }
  
  .device-type {
    font-size: 12px;
    color: #909399;
    font-weight: 500;
  }
}

.user-section {
  margin-bottom: 16px;
  
  .username {
    font-weight: 600;
    color: #303133;
    font-size: 14px;
    margin-bottom: 6px;
  }
  
  .last-seen {
    font-size: 12px;
    color: #606266;
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.realtime-section {
  margin-bottom: 16px;
  
  .realtime-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }
  
  .realtime-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 12px 8px;
    background: rgba(64, 158, 255, 0.05);
    border-radius: 8px;
    border: 1px solid rgba(64, 158, 255, 0.1);
  }
  
  .realtime-label {
    font-size: 11px;
    color: #606266;
    margin-bottom: 6px;
    font-weight: 500;
  }
  
  .realtime-value {
    font-size: 14px;
    color: #303133;
    font-weight: 600;
  }
}

.offline-section {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(144, 147, 153, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(144, 147, 153, 0.1);
  
  .offline-message {
    color: #909399;
    font-style: italic;
    font-size: 14px;
  }
}

.no-realtime-data {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(64, 158, 255, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(64, 158, 255, 0.1);
  
  .no-data-message {
    color: #409eff;
    font-style: italic;
    font-size: 14px;
  }
}

.health-section {
  .health-label {
    font-size: 12px;
    color: #606266;
    margin-bottom: 8px;
    font-weight: 500;
  }
  
  :deep(.el-progress) {
    .el-progress__text {
      font-size: 12px;
      font-weight: 600;
    }
  }
}
</style>
