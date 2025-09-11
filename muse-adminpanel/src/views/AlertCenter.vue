<template>
  <div class="alert-center">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>告警中心</h2>
      <div class="header-actions">
        <el-button @click="refreshAlerts">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button @click="clearAllAlerts" type="danger">
          <el-icon><Delete /></el-icon>
          清空已处理
        </el-button>
      </div>
    </div>

    <!-- 告警统计卡片 -->
    <el-row :gutter="20" class="alert-stats">
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card active">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><Bell /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ alertStore.alertCount.active }}</div>
              <div class="stat-label">活跃告警</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card critical">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ alertStore.alertCount.critical }}</div>
              <div class="stat-label">严重告警</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card acknowledged">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ alertStore.alertCount.acknowledged }}</div>
              <div class="stat-label">已确认</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card total">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><List /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ alertStore.alertCount.total }}</div>
              <div class="stat-label">总告警数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 实时告警列表 -->
    <el-card class="alert-list-section" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>实时告警列表</span>
          <div class="header-actions">
            <el-select v-model="alertFilter" @change="onFilterChange" size="small" style="width: 150px">
              <el-option label="全部" value="all" />
              <el-option label="活跃" value="active" />
              <el-option label="已确认" value="acknowledged" />
              <el-option label="严重" value="critical" />
            </el-select>
          </div>
        </div>
      </template>
      
      <el-table
        :data="filteredAlerts"
        v-loading="alertStore.loading"
        stripe
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <el-table-column prop="severity" label="严重程度" width="100">
          <template #default="{ row }">
            <el-tag :type="getSeverityColor(row.severity)" size="small">
              {{ getSeverityText(row.severity) }}
            </el-tag>
          </template>
        </el-table-column>
        
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
        
        <el-table-column prop="alertType" label="告警类型" width="150">
          <template #default="{ row }">
            {{ getAlertTypeText(row.alertType) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="message" label="告警信息" min-width="200" show-overflow-tooltip />
        
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusColor(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="!row.acknowledged" 
              size="small" 
              type="primary" 
              @click.stop="acknowledgeAlert(row)"
            >
              确认
            </el-button>
            <el-button 
              size="small" 
              type="success" 
              @click.stop="handleAlert(row)"
            >
              处理
            </el-button>
            <el-button 
              size="small" 
              @click.stop="viewAlertDetail(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 告警处理面板 -->
    <el-card class="alert-handle-section" shadow="hover" v-if="selectedAlert">
      <template #header>
        <span>告警处理</span>
      </template>
      
      <div class="alert-handle-content">
        <div class="alert-info">
          <h4>{{ selectedAlert.message }}</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="告警ID">{{ selectedAlert.alertId }}</el-descriptions-item>
            <el-descriptions-item label="设备">{{ selectedAlert.deviceId }} ({{ selectedAlert.user?.username }})</el-descriptions-item>
            <el-descriptions-item label="时间">{{ formatTime(selectedAlert.timestamp) }}</el-descriptions-item>
            <el-descriptions-item label="位置">{{ formatLocation(selectedAlert.location) }}</el-descriptions-item>
            <el-descriptions-item label="详情" :span="2">{{ selectedAlert.message }}</el-descriptions-item>
          </el-descriptions>
        </div>
        
        <div class="handle-actions">
          <el-form :model="handleForm" label-width="80px">
            <el-form-item label="处理方式">
              <el-select v-model="handleForm.action" placeholder="选择处理方式">
                <el-option label="电话联系" value="call" />
                <el-option label="短信通知" value="sms" />
                <el-option label="现场处理" value="onsite" />
                <el-option label="忽略告警" value="ignore" />
              </el-select>
            </el-form-item>
            <el-form-item label="处理备注">
              <el-input
                v-model="handleForm.note"
                type="textarea"
                placeholder="请输入处理备注"
                :rows="3"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitHandle">提交处理</el-button>
              <el-button @click="cancelHandle">取消</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </el-card>

    <!-- 告警详情对话框 -->
    <el-dialog
      v-model="alertDetailVisible"
      title="告警详情"
      width="600px"
    >
      <div v-if="selectedAlert" class="alert-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="告警ID">{{ selectedAlert.alertId }}</el-descriptions-item>
          <el-descriptions-item label="设备ID">{{ selectedAlert.deviceId }}</el-descriptions-item>
          <el-descriptions-item label="驾驶员">{{ selectedAlert.user?.username || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="联系方式">{{ selectedAlert.user?.phone || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="告警类型">{{ getAlertTypeText(selectedAlert.alertType) }}</el-descriptions-item>
          <el-descriptions-item label="严重程度">{{ getSeverityText(selectedAlert.severity) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ getStatusText(selectedAlert.status) }}</el-descriptions-item>
          <el-descriptions-item label="确认状态">{{ selectedAlert.acknowledged ? '已确认' : '未确认' }}</el-descriptions-item>
          <el-descriptions-item label="发生时间" :span="2">{{ formatTime(selectedAlert.timestamp) }}</el-descriptions-item>
          <el-descriptions-item label="位置" :span="2">
            {{ formatLocation(selectedAlert.location) }}
          </el-descriptions-item>
          <el-descriptions-item label="告警信息" :span="2">{{ selectedAlert.message }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Refresh, 
  Delete, 
  Bell, 
  Warning, 
  CircleCheck, 
  List 
} from '@element-plus/icons-vue'

import { useAlertStore } from '@/stores/alert'
import wsManager from '@/utils/websocket'

const alertStore = useAlertStore()

// 响应式数据
const alertFilter = ref('all')
const selectedAlert = ref(null)
const alertDetailVisible = ref(false)
const handleForm = ref({
  action: '',
  note: ''
})

// 计算属性
const filteredAlerts = computed(() => {
  let alerts = alertStore.realtimeAlerts
  
  switch (alertFilter.value) {
    case 'active':
      return alerts.filter(alert => alert.status === 'ACTIVE')
    case 'acknowledged':
      return alerts.filter(alert => alert.acknowledged)
    case 'critical':
      return alerts.filter(alert => alert.severity === 'CRITICAL')
    default:
      return alerts
  }
})

// 方法
const loadAlerts = async () => {
  try {
    await alertStore.fetchRealtimeAlerts()
  } catch (error) {
    console.error('加载告警列表失败:', error)
    ElMessage.error('加载告警列表失败')
  }
}

const refreshAlerts = () => {
  loadAlerts()
}

const clearAllAlerts = async () => {
  try {
    await ElMessageBox.confirm('确认清空所有已处理的告警？', '确认操作', {
      type: 'warning'
    })
    
    // 这里应该调用API清空已处理的告警
    ElMessage.success('已清空已处理的告警')
  } catch (error) {
    // 用户取消操作
  }
}

const onFilterChange = () => {
  // 筛选变化时的处理逻辑
}

const handleRowClick = (row) => {
  selectedAlert.value = row
}

const acknowledgeAlert = async (alert) => {
  try {
    await alertStore.acknowledgeAlert(alert.alertId)
    ElMessage.success('告警已确认')
  } catch (error) {
    console.error('确认告警失败:', error)
    ElMessage.error('确认告警失败')
  }
}

const handleAlert = (alert) => {
  selectedAlert.value = alert
  handleForm.value = {
    action: '',
    note: ''
  }
}

const viewAlertDetail = (alert) => {
  selectedAlert.value = alert
  alertDetailVisible.value = true
}

const submitHandle = async () => {
  if (!handleForm.value.action) {
    ElMessage.warning('请选择处理方式')
    return
  }
  
  try {
    await alertStore.handleAlert(selectedAlert.value.alertId, handleForm.value)
    ElMessage.success('告警处理完成')
    selectedAlert.value = null
    handleForm.value = {
      action: '',
      note: ''
    }
  } catch (error) {
    console.error('处理告警失败:', error)
    ElMessage.error('处理告警失败')
  }
}

const cancelHandle = () => {
  selectedAlert.value = null
  handleForm.value = {
    action: '',
    note: ''
  }
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

const getAlertTypeText = (alertType) => {
  const typeMap = {
    FATIGUE_HIGH: '疲劳严重',
    FATIGUE_MEDIUM: '疲劳中等',
    DISTRACTION_HIGH: '分心严重',
    DISTRACTION_MEDIUM: '分心中等',
    EMERGENCY: '紧急事件',
    SYSTEM_ERROR: '系统错误'
  }
  return typeMap[alertType] || alertType
}

const getStatusText = (status) => {
  const statusMap = {
    ACTIVE: '活跃',
    HANDLED: '已处理',
    IGNORED: '已忽略'
  }
  return statusMap[status] || '未知'
}

const getStatusColor = (status) => {
  const colorMap = {
    ACTIVE: 'danger',
    HANDLED: 'success',
    IGNORED: 'info'
  }
  return colorMap[status] || 'info'
}

// WebSocket事件处理
const handleNewAlert = (alert) => {
  alertStore.addAlert(alert)
  // 播放告警声音
  playAlertSound(alert.severity)
  // 显示通知
  showAlertNotification(alert)
}

const playAlertSound = (severity) => {
  // 根据严重程度播放不同的声音
  const audio = new Audio()
  if (severity === 'CRITICAL' || severity === 'HIGH') {
    audio.src = '/static/audio/alert_high.mp3'
  } else if (severity === 'MEDIUM') {
    audio.src = '/static/audio/alert_mid.mp3'
  } else {
    audio.src = '/static/audio/alert_low.mp3'
  }
  audio.play().catch(e => console.log('无法播放告警声音:', e))
}

const showAlertNotification = (alert) => {
  ElMessage({
    message: `新告警: ${alert.message}`,
    type: 'warning',
    duration: 5000,
    showClose: true
  })
}

// 生命周期
onMounted(() => {
  loadAlerts()
  
  // 监听WebSocket事件
  wsManager.on('alert', handleNewAlert)
  
  // 定时刷新数据
  const refreshInterval = setInterval(() => {
    loadAlerts()
  }, 30000) // 每30秒刷新一次
  
  // 清理定时器
  onUnmounted(() => {
    clearInterval(refreshInterval)
    wsManager.off('alert', handleNewAlert)
  })
})
</script>

<style scoped lang="scss">
.alert-center {
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

.alert-stats {
  margin-bottom: 20px;
}

.stat-card {
  .stat-content {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .stat-icon {
    width: 50px;
    height: 50px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    color: #fff;
  }

  .stat-info {
    flex: 1;
  }

  .stat-value {
    font-size: 24px;
    font-weight: bold;
    color: #303133;
    line-height: 1;
    margin-bottom: 4px;
  }

  .stat-label {
    font-size: 14px;
    color: #909399;
  }

  &.active .stat-icon {
    background: #409EFF;
  }

  &.critical .stat-icon {
    background: #F56C6C;
  }

  &.acknowledged .stat-icon {
    background: #67C23A;
  }

  &.total .stat-icon {
    background: #909399;
  }
}

.alert-list-section,
.alert-handle-section {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.alert-handle-content {
  display: flex;
  gap: 20px;
}

.alert-info {
  flex: 1;

  h4 {
    margin: 0 0 16px 0;
    color: #303133;
  }
}

.handle-actions {
  flex: 1;
}

// 响应式布局
@media (max-width: 768px) {
  .alert-center {
    padding: 10px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .alert-handle-content {
    flex-direction: column;
  }
  
  .stat-card .stat-content {
    flex-direction: column;
    text-align: center;
    gap: 8px;
  }
}
</style>
