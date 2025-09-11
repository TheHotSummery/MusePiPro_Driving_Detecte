<template>
  <div class="event-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>事件管理</h2>
      <div class="header-actions">
        <el-button @click="refreshEvents">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button @click="exportEvents">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </div>
    </div>

    <!-- 筛选工具栏 -->
    <el-card class="filter-section" shadow="hover">
      <el-form :model="filterForm" inline>
        <el-form-item label="设备">
          <el-select
            v-model="filterForm.deviceId"
            placeholder="选择设备"
            clearable
            style="width: 200px"
          >
            <el-option
              v-for="device in devices"
              :key="device.deviceId"
              :label="`${device.deviceId} (${device.username})`"
              :value="device.deviceId"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="事件类型">
          <el-select
            v-model="filterForm.eventType"
            placeholder="选择事件类型"
            clearable
            style="width: 150px"
          >
            <el-option label="疲劳检测" value="FATIGUE" />
            <el-option label="分心行为" value="DISTRACTION" />
            <el-option label="紧急事件" value="EMERGENCY" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="严重程度">
          <el-select
            v-model="filterForm.severity"
            placeholder="选择严重程度"
            clearable
            style="width: 150px"
          >
            <el-option label="轻微" value="LOW" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="严重" value="HIGH" />
            <el-option label="危急" value="CRITICAL" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filterForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 350px"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="applyFilter">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="resetFilter">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="20">
      <!-- 左侧事件列表 -->
      <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="16">
        <el-card class="event-list-section" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>事件列表</span>
              <div class="header-info">
                共 {{ eventStore.events.length }} 条记录
              </div>
            </div>
          </template>
          
          <el-table
            :data="eventStore.events"
            v-loading="eventStore.loading"
            stripe
            style="width: 100%"
            @row-click="handleRowClick"
          >
            <el-table-column prop="timestamp" label="时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.timestamp) }}
              </template>
            </el-table-column>
            
            <el-table-column prop="deviceId" label="设备ID" width="150" />
            
            <el-table-column prop="username" label="驾驶员" width="120">
              <template #default="{ row }">
                {{ row.user?.username || '未知' }}
              </template>
            </el-table-column>
            
            <el-table-column prop="eventType" label="事件类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getEventTypeColor(row.eventType)" size="small">
                  {{ getEventTypeText(row.eventType) }}
                </el-tag>
              </template>
            </el-table-column>
            
            <el-table-column prop="severity" label="严重程度" width="100">
              <template #default="{ row }">
                <el-tag :type="getSeverityColor(row.severity)" size="small">
                  {{ getSeverityText(row.severity) }}
                </el-tag>
              </template>
            </el-table-column>
            
            <el-table-column prop="confidence" label="置信度" width="100">
              <template #default="{ row }">
                {{ row.confidence ? (row.confidence * 100).toFixed(1) + '%' : '未知' }}
              </template>
            </el-table-column>
            
            <el-table-column prop="duration" label="持续时间" width="100">
              <template #default="{ row }">
                {{ row.duration ? row.duration + '秒' : '未知' }}
              </template>
            </el-table-column>
            
            <el-table-column prop="context" label="描述" min-width="200" show-overflow-tooltip />
            
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click.stop="viewEventDetail(row)">
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <!-- 分页 -->
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="pagination.currentPage"
              v-model:page-size="pagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="pagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧统计面板 -->
      <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
        <!-- 事件统计图表 -->
        <el-card class="statistics-section" shadow="hover">
          <template #header>
            <span>事件统计图表</span>
          </template>
          <EventTrendChart 
            :data="eventStore.events"
            :loading="eventStore.loading"
            @period-change="onPeriodChange"
          />
        </el-card>

        <!-- 严重程度分布 -->
        <el-card class="severity-section" shadow="hover">
          <template #header>
            <span>严重程度分布</span>
          </template>
          <div class="severity-stats">
            <div 
              v-for="(count, severity) in severityStats" 
              :key="severity"
              class="severity-item"
            >
              <div class="severity-label">
                <el-tag :type="getSeverityColor(severity)" size="small">
                  {{ getSeverityText(severity) }}
                </el-tag>
              </div>
              <div class="severity-count">{{ count }}</div>
              <div class="severity-percentage">
                {{ getSeverityPercentage(count) }}%
              </div>
            </div>
          </div>
        </el-card>

        <!-- 设备统计 -->
        <el-card class="device-stats-section" shadow="hover">
          <template #header>
            <span>设备事件统计</span>
          </template>
          <div class="device-stats">
            <div 
              v-for="device in deviceStats" 
              :key="device.deviceId"
              class="device-stat-item"
            >
              <div class="device-info">
                <div class="device-id">{{ device.deviceId }}</div>
                <div class="device-user">{{ device.username }}</div>
              </div>
              <div class="device-count">{{ device.eventCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 事件详情对话框 -->
    <el-dialog
      v-model="eventDetailVisible"
      title="事件详情"
      width="800px"
    >
      <div v-if="selectedEvent" class="event-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="事件ID">{{ selectedEvent.eventId }}</el-descriptions-item>
          <el-descriptions-item label="设备ID">{{ selectedEvent.deviceId }}</el-descriptions-item>
          <el-descriptions-item label="驾驶员">{{ selectedEvent.user?.username || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="联系方式">{{ selectedEvent.user?.phone || '未知' }}</el-descriptions-item>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Refresh, 
  Download, 
  Search, 
  RefreshLeft 
} from '@element-plus/icons-vue'

import EventTrendChart from '@/components/charts/EventTrendChart.vue'
import { useEventStore } from '@/stores/event'
import { useDeviceStore } from '@/stores/device'
import wsManager from '@/utils/websocket'

const router = useRouter()
const eventStore = useEventStore()
const deviceStore = useDeviceStore()

// 响应式数据
const filterForm = ref({
  deviceId: '',
  eventType: '',
  severity: '',
  timeRange: []
})

const pagination = ref({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

const eventDetailVisible = ref(false)
const selectedEvent = ref(null)

// 计算属性
const devices = computed(() => deviceStore.devices)

const severityStats = computed(() => {
  const stats = { LOW: 0, MEDIUM: 0, HIGH: 0, CRITICAL: 0 }
  eventStore.events.forEach(event => {
    if (stats.hasOwnProperty(event.severity)) {
      stats[event.severity]++
    }
  })
  return stats
})

const deviceStats = computed(() => {
  const stats = {}
  eventStore.events.forEach(event => {
    if (!stats[event.deviceId]) {
      stats[event.deviceId] = {
        deviceId: event.deviceId,
        username: event.user?.username || '未知',
        eventCount: 0
      }
    }
    stats[event.deviceId].eventCount++
  })
  return Object.values(stats).sort((a, b) => b.eventCount - a.eventCount)
})

// 方法
const loadEvents = async () => {
  try {
    console.log('=== EventManagement: 开始加载事件列表 ===')
    // 暂时移除分页参数，只使用基本筛选参数
    const params = {
      ...getFilterParams()
    }
    
    console.log('加载参数:', params)
    const response = await eventStore.fetchEvents(params)
    console.log('=== EventManagement: 事件列表加载完成 ===')
    console.log('响应数据:', response)
    console.log('当前事件数量:', eventStore.events.length)
    console.log('事件列表:', eventStore.events)
    
    pagination.value.total = response.total || 0
  } catch (error) {
    console.error('加载事件列表失败:', error)
    ElMessage.error('加载事件列表失败')
  }
}

const loadDevices = async () => {
  try {
    await deviceStore.fetchDevices()
  } catch (error) {
    console.error('加载设备列表失败:', error)
  }
}

const getFilterParams = () => {
  const params = {}
  
  if (filterForm.value.deviceId) {
    params.deviceId = filterForm.value.deviceId
  }
  
  if (filterForm.value.eventType) {
    params.eventType = filterForm.value.eventType
  }
  
  if (filterForm.value.severity) {
    params.severity = filterForm.value.severity
  }
  
  if (filterForm.value.timeRange && filterForm.value.timeRange.length === 2) {
    params.startTime = filterForm.value.timeRange[0]
    params.endTime = filterForm.value.timeRange[1]
  }
  
  return params
}

const applyFilter = () => {
  pagination.value.currentPage = 1
  loadEvents()
}

const resetFilter = () => {
  filterForm.value = {
    deviceId: '',
    eventType: '',
    severity: '',
    timeRange: []
  }
  pagination.value.currentPage = 1
  loadEvents()
}

const refreshEvents = () => {
  loadEvents()
}

const exportEvents = () => {
  ElMessage.info('导出功能开发中...')
}

const handleRowClick = (row) => {
  viewEventDetail(row)
}

const viewEventDetail = (event) => {
  selectedEvent.value = event
  eventDetailVisible.value = true
}

const handleSizeChange = (size) => {
  pagination.value.pageSize = size
  pagination.value.currentPage = 1
  loadEvents()
}

const handleCurrentChange = (page) => {
  pagination.value.currentPage = page
  loadEvents()
}

const onPeriodChange = (period) => {
  // 根据周期变化重新加载数据
  loadEvents()
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

const getEventTypeText = (eventType) => {
  const typeMap = {
    FATIGUE: '疲劳检测',
    DISTRACTION: '分心行为',
    EMERGENCY: '紧急事件'
  }
  return typeMap[eventType] || '未知事件'
}

const getEventTypeColor = (eventType) => {
  const colorMap = {
    FATIGUE: 'warning',
    DISTRACTION: 'danger',
    EMERGENCY: 'danger'
  }
  return colorMap[eventType] || 'info'
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

const getSeverityColor = (severity) => {
  const colorMap = {
    LOW: 'success',
    MEDIUM: 'warning',
    HIGH: 'danger',
    CRITICAL: 'danger'
  }
  return colorMap[severity] || 'info'
}

const getSeverityPercentage = (count) => {
  const total = eventStore.events.length
  if (total === 0) return 0
  return ((count / total) * 100).toFixed(1)
}

// 处理新事件
const handleNewEvent = (data) => {
  console.log('收到新事件推送:', data)
  const { type, payload } = data
  
  if (type === 'event') {
    const { deviceId, eventType, data: eventData, timestamp } = payload
    
    // 创建新事件对象
    const newEvent = {
      eventId: eventData.eventId || `EVENT_${Date.now()}`,
      deviceId: deviceId,
      eventType: eventType,
      severity: eventData.severity || 'MEDIUM',
      location: eventData.location,
      behavior: eventData.behavior,
      confidence: eventData.confidence,
      duration: eventData.duration,
      alertLevel: eventData.alertLevel,
      context: eventData.context,
      timestamp: new Date(timestamp).toISOString(),
      user: {
        username: '实时推送'
      }
    }
    
    // 添加到事件列表顶部
    eventStore.events.unshift(newEvent)
    
    // 显示通知
    ElMessage.success(`收到新事件: ${eventType}`)
    
    // 播放提示音（可选）
    try {
      const audio = new Audio('/static/audio/alert_mid.mp3')
      audio.play().catch(() => {})
    } catch (error) {
      console.log('无法播放提示音:', error)
    }
  }
}

// 生命周期
onMounted(() => {
  loadDevices()
  loadEvents()
  
  // 监听WebSocket事件
  wsManager.on('event', handleNewEvent)
  
  // 定时刷新数据（降低频率，因为有了实时推送）
  const refreshInterval = setInterval(() => {
    loadEvents()
  }, 300000) // 每5分钟刷新一次，作为数据同步的备选方案
  
  // 清理定时器
  onUnmounted(() => {
    clearInterval(refreshInterval)
    wsManager.off('event', handleNewEvent)
  })
})
</script>

<style scoped lang="scss">
.event-management {
  padding: 20px;
  background: #f5f7fa;
  height: calc(100vh - 60px);
  overflow-y: auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h2 {
    margin: 0;
    color: #303133;
  }
}

.filter-section {
  margin-bottom: 20px;
}

.event-list-section,
.statistics-section,
.severity-section,
.device-stats-section {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-info {
  font-size: 14px;
  color: #909399;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.severity-stats {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.severity-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }
}

.severity-label {
  flex: 1;
}

.severity-count {
  font-weight: bold;
  color: #409EFF;
  min-width: 40px;
  text-align: center;
}

.severity-percentage {
  font-size: 12px;
  color: #909399;
  min-width: 50px;
  text-align: right;
}

.device-stats {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.device-stat-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }
}

.device-info {
  flex: 1;
}

.device-id {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
}

.device-user {
  font-size: 12px;
  color: #909399;
}

.device-count {
  font-weight: bold;
  color: #409EFF;
  min-width: 40px;
  text-align: center;
}

// 响应式布局
@media (max-width: 768px) {
  .event-management {
    padding: 10px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .filter-section {
    .el-form {
      flex-direction: column;
      align-items: stretch;
    }
    
    .el-form-item {
      margin-right: 0;
      margin-bottom: 10px;
    }
  }
}
</style>
