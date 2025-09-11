<template>
  <div class="analytics">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>数据分析</h2>
      <div class="header-actions">
        <el-select v-model="selectedTimeRange" @change="onTimeRangeChange" size="small">
          <el-option label="全部" value="all" />
          <el-option label="今日" value="today" />
          <el-option label="本周" value="week" />
          <el-option label="本月" value="month" />
          <el-option label="自定义" value="custom" />
        </el-select>
        <el-date-picker
          v-if="selectedTimeRange === 'custom'"
          v-model="customTimeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DDTHH:mm:ss"
          size="small"
          style="width: 350px"
        />
        <el-button @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 数据概览 -->
    <el-row :gutter="20" class="overview-section">
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <StatCard
          title="总事件数"
          :value="analyticsData.totalEvents || 0"
          icon="Warning"
          color="warning"
          suffix="起"
        />
      </el-col>
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <StatCard
          title="疲劳事件"
          :value="analyticsData.fatigueEvents || 0"
          icon="User"
          color="danger"
          suffix="起"
        />
      </el-col>
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <StatCard
          title="分心事件"
          :value="analyticsData.distractionEvents || 0"
          icon="View"
          color="info"
          suffix="起"
        />
      </el-col>
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <StatCard
          title="平均安全评分"
          :value="analyticsData.averageSafetyScore || 0"
          icon="Star"
          color="success"
          suffix="分"
        />
      </el-col>
    </el-row>

    <!-- 图表分析区域 -->
    <el-row :gutter="20" class="charts-section">
      <!-- 驾驶行为分析 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span>驾驶行为分析</span>
          </template>
          <FatigueAnalysisChart 
            :data="analyticsData.events || []"
            :loading="loading"
            @type-change="onChartTypeChange"
          />
        </el-card>
      </el-col>

      <!-- 事件趋势分析 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span>事件趋势分析</span>
          </template>
          <EventTrendChart 
            :data="analyticsData.events || []"
            :loading="loading"
            @period-change="onPeriodChange"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 详细分析区域 -->
    <el-row :gutter="20" class="detailed-section">
      <!-- 设备网络健康状态分析 -->
      <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
        <el-card class="analysis-card" shadow="hover">
          <template #header>
            <span>设备网络健康状态分析</span>
          </template>
          <div class="health-analysis">
            <div 
              v-for="device in deviceHealthData" 
              :key="device.deviceId"
              class="health-item"
            >
              <div class="device-info">
                <div class="device-id">{{ device.deviceId }}</div>
                <div class="device-user">{{ device.username }}</div>
              </div>
              <div class="health-score">
                <HealthProgress :score="device.healthScore" />
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 时间段分析 -->
      <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
        <el-card class="analysis-card" shadow="hover">
          <template #header>
            <span>时间段分析</span>
          </template>
          <div class="time-analysis">
            <div 
              v-for="(count, timeLabel) in hourlyStats" 
              :key="timeLabel"
              class="time-item"
            >
              <div class="time-label">{{ timeLabel }}</div>
              <div class="time-bar">
                <div 
                  class="time-fill" 
                  :style="{ width: getTimeBarWidth(count) }"
                ></div>
              </div>
              <div class="time-count">{{ count }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 风险评估 -->
      <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
        <el-card class="analysis-card" shadow="hover">
          <template #header>
            <span>风险评估</span>
          </template>
          <div class="risk-analysis">
            <div class="risk-item">
              <div class="risk-label">高风险设备</div>
              <div class="risk-value">{{ riskStats.highRisk }}</div>
            </div>
            <div class="risk-item">
              <div class="risk-label">中风险设备</div>
              <div class="risk-value">{{ riskStats.mediumRisk }}</div>
            </div>
            <div class="risk-item">
              <div class="risk-label">低风险设备</div>
              <div class="risk-value">{{ riskStats.lowRisk }}</div>
            </div>
            <div class="risk-item">
              <div class="risk-label">平均风险评分</div>
              <div class="risk-value">{{ riskStats.averageRisk }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 设备详细分析表格 -->
    <el-card class="device-analysis-section" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>设备详细分析</span>
          <el-button @click="exportAnalysis" size="small">
            <el-icon><Download /></el-icon>
            导出报告
          </el-button>
        </div>
      </template>
      
      <el-table
        :data="deviceAnalysisData"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="deviceId" label="设备ID" width="150" />
        <el-table-column prop="username" label="驾驶员" width="120" />
        <el-table-column prop="totalEvents" label="总事件数" width="100" />
        <el-table-column prop="fatigueEvents" label="疲劳事件" width="100" />
        <el-table-column prop="distractionEvents" label="分心事件" width="100" />
        <el-table-column prop="totalDrivingTime" label="驾驶时长" width="120">
          <template #default="{ row }">
            {{ formatDuration(row.totalDrivingTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalDistance" label="行驶距离" width="100">
          <template #default="{ row }">
            {{ row.totalDistance }} km
          </template>
        </el-table-column>
        <el-table-column prop="averageSpeed" label="平均速度" width="100">
          <template #default="{ row }">
            {{ row.averageSpeed }} km/h
          </template>
        </el-table-column>
        <el-table-column prop="safetyScore" label="安全评分" width="100">
          <template #default="{ row }">
            <el-tag :type="getSafetyScoreColor(row.safetyScore)" size="small">
              {{ row.safetyScore }}分
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="riskLevel" label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getRiskLevelColor(row.riskLevel)" size="small">
              {{ getRiskLevelText(row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Download } from '@element-plus/icons-vue'

import StatCard from '@/components/common/StatCard.vue'
import HealthProgress from '@/components/common/HealthProgress.vue'
import EventTrendChart from '@/components/charts/EventTrendChart.vue'
import FatigueAnalysisChart from '@/components/charts/FatigueAnalysisChart.vue'

import { useEventStore } from '@/stores/event'
import { useDeviceStore } from '@/stores/device'

const eventStore = useEventStore()
const deviceStore = useDeviceStore()

// 响应式数据
const selectedTimeRange = ref('all')
const customTimeRange = ref([])
const loading = ref(false)
const analyticsData = ref({})
const deviceHealthData = ref([])
const deviceAnalysisData = ref([])

// 计算属性 - 两小时时间段统计
const hourlyStats = computed(() => {
  const stats = {}
  // 创建12个两小时时间段 (0-2, 2-4, 4-6, ..., 22-24)
  for (let i = 0; i < 12; i++) {
    const startHour = i * 2
    const endHour = startHour + 2
    const timeLabel = `${startHour.toString().padStart(2, '0')}:00-${endHour.toString().padStart(2, '0')}:00`
    stats[timeLabel] = 0
  }
  
  if (analyticsData.value.events) {
    analyticsData.value.events.forEach(event => {
      const hour = new Date(event.timestamp).getHours()
      // 计算属于哪个两小时时间段
      const timeSlot = Math.floor(hour / 2)
      const startHour = timeSlot * 2
      const endHour = startHour + 2
      const timeLabel = `${startHour.toString().padStart(2, '0')}:00-${endHour.toString().padStart(2, '0')}:00`
      stats[timeLabel]++
    })
  }
  
  return stats
})

const riskStats = computed(() => {
  const stats = {
    highRisk: 0,
    mediumRisk: 0,
    lowRisk: 0,
    averageRisk: 0
  }
  
  if (deviceAnalysisData.value.length > 0) {
    deviceAnalysisData.value.forEach(device => {
      if (device.riskLevel === 'HIGH') stats.highRisk++
      else if (device.riskLevel === 'MEDIUM') stats.mediumRisk++
      else stats.lowRisk++
    })
    
    const totalScore = deviceAnalysisData.value.reduce((sum, device) => sum + device.safetyScore, 0)
    stats.averageRisk = Math.round(totalScore / deviceAnalysisData.value.length)
  }
  
  return stats
})

// 方法
const loadAnalyticsData = async () => {
  loading.value = true
  try {
    const params = getTimeRangeParams()
    
    // 并行加载数据
    await Promise.all([
      loadEventStatistics(params),
      loadDeviceAnalysis()
    ])
  } catch (error) {
    console.error('加载分析数据失败:', error)
    ElMessage.error('加载分析数据失败')
  } finally {
    loading.value = false
  }
}

const loadEventStatistics = async (params) => {
  try {
    console.log('=== Analytics: 开始加载事件统计 ===')
    const response = await eventStore.fetchEventStatistics(params)
    console.log('事件统计API响应:', response)
    
    // 独立获取事件列表，不依赖eventStore.events
    let events = []
    try {
      const eventsResponse = await eventStore.fetchEvents(params)
      events = eventsResponse.events || []
      console.log('独立获取的事件列表:', events.length, '个事件')
    } catch (error) {
      console.warn('获取事件列表失败，使用空数组:', error)
    }
    
    // 根据API返回的数据结构进行映射
    analyticsData.value = {
      totalEvents: response.totalEvents || 0,
      fatigueEvents: response.eventTypes?.fatigue || 0,
      distractionEvents: response.eventTypes?.distraction || 0,
      emergencyEvents: response.eventTypes?.emergency || 0,
      averageSafetyScore: response.deviceStatistics?.averageSafetyScore || 0,
      hourlyDistribution: response.hourlyDistribution || [],
      severityLevels: response.severityLevels || {},
      eventTypes: response.eventTypes || {},
      events: events
    }
    
    console.log('处理后的分析数据:', analyticsData.value)
  } catch (error) {
    console.error('加载事件统计失败:', error)
  }
}

const loadDeviceAnalysis = async () => {
  try {
    // 获取设备列表
    await deviceStore.fetchDevices()
    
    // 为每个设备获取详细分析
    const analysisPromises = deviceStore.devices.map(async (device) => {
      try {
        const analysis = await eventStore.fetchDrivingBehaviorAnalysis(device.deviceId, {})
        return {
          deviceId: device.deviceId,
          username: device.username,
          ...analysis.statistics,
          healthScore: device.healthScore,
          riskLevel: getRiskLevel(analysis.statistics?.safetyScore || 0)
        }
      } catch (error) {
        console.error(`获取设备 ${device.deviceId} 分析失败:`, error)
        return {
          deviceId: device.deviceId,
          username: device.username,
          totalEvents: 0,
          fatigueEvents: 0,
          distractionEvents: 0,
          totalDrivingTime: 0,
          totalDistance: 0,
          averageSpeed: 0,
          safetyScore: 0,
          healthScore: device.healthScore,
          riskLevel: 'LOW'
        }
      }
    })
    
    const results = await Promise.all(analysisPromises)
    deviceAnalysisData.value = results
    
    // 生成设备网络健康状态数据
    deviceHealthData.value = deviceStore.devices.map(device => ({
      deviceId: device.deviceId,
      username: device.username,
      healthScore: device.healthScore || 0
    }))
  } catch (error) {
    console.error('加载设备分析失败:', error)
  }
}

const getTimeRangeParams = () => {
  // 默认情况下不返回时间参数，获取所有数据
  if (selectedTimeRange.value === 'all' || !selectedTimeRange.value) {
    return {}
  }
  
  const now = new Date()
  let startTime, endTime
  
  switch (selectedTimeRange.value) {
    case 'today':
      startTime = new Date(now.getFullYear(), now.getMonth(), now.getDate())
      endTime = now
      break
    case 'week':
      startTime = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
      endTime = now
      break
    case 'month':
      startTime = new Date(now.getFullYear(), now.getMonth(), 1)
      endTime = now
      break
    case 'custom':
      if (customTimeRange.value && customTimeRange.value.length === 2) {
        startTime = new Date(customTimeRange.value[0])
        endTime = new Date(customTimeRange.value[1])
      } else {
        // 自定义时间无效时，返回空参数
        return {}
      }
      break
    default:
      // 默认情况不返回时间参数
      return {}
  }
  
  return {
    startTime: startTime.toISOString(),
    endTime: endTime.toISOString()
  }
}

const onTimeRangeChange = () => {
  loadAnalyticsData()
}

const onChartTypeChange = (type) => {
  console.log('图表类型变化:', type)
}

const onPeriodChange = (period) => {
  console.log('周期变化:', period)
}

const refreshData = () => {
  loadAnalyticsData()
}

const exportAnalysis = () => {
  ElMessage.info('导出功能开发中...')
}

const getTimeBarWidth = (count) => {
  const maxCount = Math.max(...Object.values(hourlyStats.value))
  if (maxCount === 0) return '0%'
  return `${(count / maxCount) * 100}%`
}

const getRiskLevel = (safetyScore) => {
  if (safetyScore >= 80) return 'LOW'
  if (safetyScore >= 60) return 'MEDIUM'
  return 'HIGH'
}

const getRiskLevelText = (riskLevel) => {
  const levelMap = {
    LOW: '低风险',
    MEDIUM: '中风险',
    HIGH: '高风险'
  }
  return levelMap[riskLevel] || '未知'
}

const getRiskLevelColor = (riskLevel) => {
  const colorMap = {
    LOW: 'success',
    MEDIUM: 'warning',
    HIGH: 'danger'
  }
  return colorMap[riskLevel] || 'info'
}

const getSafetyScoreColor = (score) => {
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
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

// 生命周期
onMounted(() => {
  loadAnalyticsData()
  
  // 定时刷新数据
  const refreshInterval = setInterval(() => {
    loadAnalyticsData()
  }, 300000) // 每5分钟刷新一次
  
  // 清理定时器
  onUnmounted(() => {
    clearInterval(refreshInterval)
  })
})
</script>

<style scoped lang="scss">
.analytics {
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

.overview-section {
  margin-bottom: 20px;
}

.charts-section {
  margin-bottom: 20px;
}

.detailed-section {
  margin-bottom: 20px;
}

.chart-card,
.analysis-card,
.device-analysis-section {
  margin-bottom: 20px;
}

.health-analysis {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.health-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
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

.health-score {
  min-width: 120px;
}

.time-analysis {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
}

.time-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  background: #fafafa;
}

.time-label {
  font-size: 10px;
  color: #606266;
  text-align: center;
  line-height: 1.2;
}

.time-bar {
  width: 100%;
  height: 6px;
  background: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
}

.time-fill {
  height: 100%;
  background: linear-gradient(90deg, #409eff, #67c23a);
  transition: width 0.3s ease;
}

.time-count {
  font-size: 11px;
  font-weight: 600;
  color: #303133;
  text-align: center;
}

.risk-analysis {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.risk-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }
}

.risk-label {
  color: #606266;
  font-size: 14px;
}

.risk-value {
  font-weight: bold;
  color: #409EFF;
  font-size: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

// 响应式布局
@media (max-width: 768px) {
  .analytics {
    padding: 10px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .health-item,
  .time-item,
  .risk-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .time-bar {
    width: 100%;
  }
}
</style>
