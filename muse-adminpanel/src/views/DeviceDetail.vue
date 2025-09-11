<template>
  <div class="device-detail">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-button @click="goBack" size="small">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <h2>设备详情 - {{ deviceId }}</h2>
    </div>

    <el-row :gutter="20" v-loading="deviceStore.loading">
      <!-- 左侧信息面板 -->
      <el-col :xs="24" :sm="24" :md="10" :lg="10" :xl="10">
        <!-- 设备基本信息 -->
        <el-card class="info-panel" shadow="hover">
          <template #header>
            <span>设备信息</span>
          </template>
          <div class="device-info" v-if="deviceDetail">
            <div class="info-item">
              <label>设备ID:</label>
              <span>{{ deviceDetail.deviceId }}</span>
            </div>
            <div class="info-item">
              <label>设备类型:</label>
              <span>{{ deviceDetail.deviceType }}</span>
            </div>
            <div class="info-item">
              <label>版本:</label>
              <span>{{ deviceDetail.version }}</span>
            </div>
            <div class="info-item">
              <label>驾驶员:</label>
              <span>{{ deviceDetail.user?.username || '未知' }}</span>
            </div>
            <div class="info-item">
              <label>联系方式:</label>
              <span>{{ deviceDetail.user?.phone || '未知' }}</span>
            </div>
            <div class="info-item">
              <label>状态:</label>
              <DeviceStatusTag :status="deviceDetail.status" />
            </div>
            <div class="info-item">
              <label>网络健康状态:</label>
              <HealthProgress :score="deviceDetail.healthScore || 0" />
            </div>
            <div class="info-item">
              <label>最后心跳:</label>
              <span>{{ formatTime(deviceDetail.lastSeen) }}</span>
            </div>
          </div>
        </el-card>

        <!-- 实时数据 -->
        <el-card class="realtime-data" shadow="hover">
          <template #header>
            <span>实时数据</span>
            <el-button size="small" @click="refreshRealtimeData">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </template>
          <div class="data-grid" v-if="deviceDetail?.currentLocation">
            <div class="data-item">
              <label>位置:</label>
              <span>{{ formatLocation(deviceDetail.currentLocation) }}</span>
            </div>
            <div class="data-item">
              <label>速度:</label>
              <span>{{ deviceDetail.currentLocation.speed || 0 }} km/h</span>
            </div>
            <div class="data-item">
              <label>方向:</label>
              <span>{{ deviceDetail.currentLocation.direction || 0 }}°</span>
            </div>
            <div class="data-item">
              <label>海拔:</label>
              <span>{{ deviceDetail.currentLocation.altitude || 0 }}m</span>
            </div>
            <div class="data-item">
              <label>GPS质量:</label>
              <span>{{ getGPSQuality(deviceDetail.currentLocation.hdop) }}</span>
            </div>
            <div class="data-item">
              <label>卫星数:</label>
              <span>{{ deviceDetail.currentLocation.satellites || 0 }}颗</span>
            </div>
          </div>
          <el-empty v-else description="暂无实时数据" />
        </el-card>

        <!-- 疲劳行为统计 -->
        <el-card class="fatigue-stats" shadow="hover">
          <template #header>
            <span>疲劳行为统计</span>
          </template>
          <div class="stats-grid" v-if="deviceDetail?.statistics">
            <div class="stat-item">
              <label>今日疲劳事件:</label>
              <span class="stat-value">{{ deviceDetail.statistics.fatigueEvents || 0 }}</span>
            </div>
            <div class="stat-item">
              <label>今日分心事件:</label>
              <span class="stat-value">{{ deviceDetail.statistics.distractionEvents || 0 }}</span>
            </div>
            <div class="stat-item">
              <label>总驾驶时长:</label>
              <span class="stat-value">{{ formatDuration(deviceDetail.statistics.totalDrivingTime) }}</span>
            </div>
            <div class="stat-item">
              <label>总行驶距离:</label>
              <span class="stat-value">{{ deviceDetail.statistics.totalDistance || 0 }} km</span>
            </div>
            <div class="stat-item">
              <label>最后疲劳事件:</label>
              <span class="stat-value">{{ formatTime(deviceDetail.statistics.lastFatigueEvent) }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无统计数据" />
        </el-card>

        <!-- 历史疲劳事件 -->
        <el-card class="event-history" shadow="hover">
          <template #header>
            <span>历史疲劳事件</span>
            <el-button size="small" @click="loadEventHistory">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </template>
          <el-timeline v-if="fatigueEvents.length">
            <el-timeline-item
              v-for="event in fatigueEvents"
              :key="event.eventId"
              :timestamp="formatTime(event.timestamp)"
              :type="getEventTypeColor(event.severity)"
              @click="showEventOnMap(event)"
            >
              <el-tag :type="getEventTypeColor(event.severity)" size="small">
                {{ getEventTypeText(event.eventType) }}
              </el-tag>
              <p class="event-context">{{ event.context }}</p>
              <p class="event-details">
                置信度: {{ (event.confidence * 100).toFixed(1) }}% | 
                持续时间: {{ event.duration }}秒
              </p>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无疲劳事件" />
        </el-card>
      </el-col>

      <!-- 右侧地图面板 -->
      <el-col :xs="24" :sm="24" :md="14" :lg="14" :xl="14">
        <el-card class="map-panel" shadow="hover">
          <template #header>
            <span>设备轨迹与事件位置</span>
            <div class="header-actions">
              <el-button size="small" @click="loadTrackData">
                <el-icon><Refresh /></el-icon>
                刷新轨迹
              </el-button>
              <el-button size="small" @click="showMapConfig">
                <el-icon><Setting /></el-icon>
                配置
              </el-button>
            </div>
          </template>
          <SimpleTrackMap
            :device-id="deviceId"
            :track-data="trackData"
            :events="fatigueEvents"
            @event-click="showEventDetail"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 事件详情对话框 -->
    <el-dialog
      v-model="eventDetailVisible"
      title="事件详情"
      width="600px"
    >
      <div v-if="selectedEvent" class="event-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="事件ID">{{ selectedEvent.eventId }}</el-descriptions-item>
          <el-descriptions-item label="设备ID">{{ selectedEvent.deviceId }}</el-descriptions-item>
          <el-descriptions-item label="事件类型">{{ getEventTypeText(selectedEvent.eventType) }}</el-descriptions-item>
          <el-descriptions-item label="严重程度">{{ getSeverityText(selectedEvent.severity) }}</el-descriptions-item>
          <el-descriptions-item label="置信度">{{ (selectedEvent.confidence * 100).toFixed(1) }}%</el-descriptions-item>
          <el-descriptions-item label="持续时间">{{ selectedEvent.duration }}秒</el-descriptions-item>
          <el-descriptions-item label="发生时间" :span="2">{{ formatTime(selectedEvent.timestamp) }}</el-descriptions-item>
          <el-descriptions-item label="位置" :span="2">
            {{ formatLocation(selectedEvent.location) }}
          </el-descriptions-item>
          <el-descriptions-item label="行为描述" :span="2">{{ selectedEvent.behavior }}</el-descriptions-item>
          <el-descriptions-item label="上下文" :span="2">{{ selectedEvent.context }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 地图配置对话框 -->
    <el-dialog
      v-model="mapConfigVisible"
      title="地图配置"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="mapConfig" label-width="100px">
        <el-form-item label="高德地图API Key">
          <el-input
            v-model="mapConfig.apiKey"
            type="password"
            placeholder="请输入高德地图API Key"
            show-password
          />
          <div class="form-tip">
            请到 <a href="https://console.amap.com/" target="_blank">高德开放平台</a> 申请API Key
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Refresh, Setting } from '@element-plus/icons-vue'

import DeviceStatusTag from '@/components/common/DeviceStatusTag.vue'
import HealthProgress from '@/components/common/HealthProgress.vue'
import SimpleTrackMap from '@/components/maps/SimpleTrackMap.vue'

import { useDeviceStore } from '@/stores/device'
import { useEventStore } from '@/stores/event'
import wsManager from '@/utils/websocket'

const route = useRoute()
const router = useRouter()
const deviceStore = useDeviceStore()
const eventStore = useEventStore()

// 响应式数据
const deviceId = computed(() => route.params.deviceId)
const deviceDetail = computed(() => deviceStore.currentDevice)
const fatigueEvents = ref([])
const trackData = ref([])
const eventDetailVisible = ref(false)
const selectedEvent = ref(null)
const mapConfigVisible = ref(false)
const mapConfig = ref({
  apiKey: ''
})

// 方法
const goBack = () => {
  router.go(-1)
}

const loadDeviceDetail = async () => {
  try {
    await deviceStore.fetchDeviceDetail(deviceId.value)
  } catch (error) {
    console.error('加载设备详情失败:', error)
    ElMessage.error('加载设备详情失败')
  }
}

const loadEventHistory = async () => {
  try {
    const params = {
      deviceId: deviceId.value,
      eventType: 'FATIGUE',
      limit: 10
    }
    const response = await eventStore.fetchEvents(params)
    fatigueEvents.value = response.events || []
  } catch (error) {
    console.error('加载事件历史失败:', error)
  }
}

const loadTrackData = async () => {
  try {
    const endTime = new Date()
    const startTime = new Date(endTime.getTime() - 24 * 60 * 60 * 1000) // 24小时前
    
    const params = {
      startTime: startTime.toISOString(),
      endTime: endTime.toISOString()
    }
    
    const response = await deviceStore.fetchDeviceTrack(deviceId.value, params)
    trackData.value = response.track || []
  } catch (error) {
    console.error('加载轨迹数据失败:', error)
  }
}

const refreshRealtimeData = () => {
  loadDeviceDetail()
}

const showEventOnMap = (event) => {
  // 这里可以在地图上高亮显示事件位置
  console.log('显示事件位置:', event)
}

const showEventDetail = (event) => {
  selectedEvent.value = event
  eventDetailVisible.value = true
}

const showMapConfig = () => {
  mapConfig.value.apiKey = localStorage.getItem('amap_api_key') || ''
  mapConfigVisible.value = true
}

const saveMapConfig = () => {
  if (!mapConfig.value.apiKey.trim()) {
    ElMessage.warning('请输入API Key')
    return
  }
  
  localStorage.setItem('amap_api_key', mapConfig.value.apiKey)
  mapConfigVisible.value = false
  ElMessage.success('地图配置已保存')
  
  // 重新加载页面以应用新配置
  setTimeout(() => {
    window.location.reload()
  }, 1000)
}

// 格式化函数
const formatTime = (time) => {
  if (!time) return '未知'
  return new Date(time).toLocaleString()
}

const formatLocation = (location) => {
  if (!location) return '未知'
  return `${location.lat.toFixed(6)}, ${location.lng.toFixed(6)}`
}

const formatDuration = (seconds) => {
  if (!seconds) return '0分钟'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) {
    return `${hours}小时${minutes}分钟`
  }
  return `${minutes}分钟`
}

const getGPSQuality = (hdop) => {
  if (!hdop) return '未知'
  if (hdop < 1) return '优秀'
  if (hdop < 2) return '良好'
  if (hdop < 5) return '一般'
  return '较差'
}

const getEventTypeText = (eventType) => {
  const typeMap = {
    FATIGUE: '疲劳检测',
    DISTRACTION: '分心行为',
    EMERGENCY: '紧急事件'
  }
  return typeMap[eventType] || '未知事件'
}

const getSeverityText = (severity) => {
  const severityMap = {
    LOW: '轻微',
    MEDIUM: '中等',
    HIGH: '严重',
    CRITICAL: '危急'
  }
  return severityMap[severity] || '未知'
}

const getEventTypeColor = (severity) => {
  const colorMap = {
    LOW: 'success',
    MEDIUM: 'warning',
    HIGH: 'danger',
    CRITICAL: 'danger'
  }
  return colorMap[severity] || 'info'
}

// WebSocket事件处理
const handleRealtimeData = (data) => {
  if (data.deviceId === deviceId.value) {
    // 更新当前设备数据
    loadDeviceDetail()
  }
}

// 生命周期
onMounted(() => {
  loadDeviceDetail()
  loadEventHistory()
  loadTrackData()
  
  // 监听WebSocket事件
  wsManager.on('realtime_data', handleRealtimeData)
  
  // 定时刷新实时数据
  const refreshInterval = setInterval(() => {
    loadDeviceDetail()
  }, 30000) // 每30秒刷新一次
  
  // 清理定时器
  onUnmounted(() => {
    clearInterval(refreshInterval)
    wsManager.off('realtime_data', handleRealtimeData)
  })
})
</script>

<style scoped lang="scss">
.device-detail {
  padding: 20px;
  background: #f5f7fa;
  height: calc(100vh - 60px);
  overflow-y: auto;
}

.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;

  h2 {
    margin: 0;
    color: #303133;
  }
}

.info-panel,
.realtime-data,
.fatigue-stats,
.event-history,
.map-panel {
  margin-bottom: 20px;
}

.device-info,
.data-grid,
.stats-grid {
  display: grid;
  gap: 12px;
}

.info-item,
.data-item,
.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }

  label {
    font-weight: 500;
    color: #606266;
    min-width: 80px;
  }

  span {
    color: #303133;
  }
}

.stat-value {
  font-weight: bold;
  color: #409EFF;
}

.event-context {
  margin: 8px 0 4px 0;
  color: #303133;
  font-size: 14px;
}

.event-details {
  margin: 0;
  color: #909399;
  font-size: 12px;
}

.header-actions {
  display: flex;
  gap: 8px;
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
  .device-detail {
    padding: 10px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .info-item,
  .data-item,
  .stat-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
    
    label {
      min-width: auto;
    }
  }
}
</style>
