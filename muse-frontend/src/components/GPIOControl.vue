<template>
  <div class="gpio-control">
    <el-card class="gpio-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <el-icon><Tools /></el-icon>
          <span>手动调试</span>
          <el-button 
            type="text" 
            size="small" 
            :icon="isCollapsed ? 'ArrowDown' : 'ArrowUp'"
            @click="toggleCollapse"
            class="collapse-btn"
          >
            {{ isCollapsed ? '展开' : '折叠' }}
          </el-button>
        </div>
      </template>
      
      <div v-show="!isCollapsed" class="gpio-content">
        <!-- GPIO控制 -->
        <div class="control-section">
          <h4 class="section-title">GPIO控制</h4>
          <div class="gpio-buttons">
            <el-button 
              type="success" 
              :icon="Bell"
              @click="triggerGPIO(70, 1)"
              :loading="loadingStates.gpio70"
            >
              触发 GPIO70 (振动马达 1秒)
            </el-button>
            
            <el-button 
              type="success" 
              :icon="Sunny"
              @click="triggerGPIO(71, 1)"
              :loading="loadingStates.gpio71"
            >
              触发 GPIO71 (LED 1秒)
            </el-button>
            
            <el-button 
              type="success" 
              :icon="BellFilled"
              @click="triggerGPIO(72, 1)"
              :loading="loadingStates.gpio72"
            >
              触发 GPIO72 (蜂鸣器 1秒)
            </el-button>
          </div>
        </div>
        
        <!-- 音频控制 -->
        <div class="control-section">
          <h4 class="section-title">音频提示</h4>
          <div class="audio-buttons">
            <el-button 
              type="warning" 
              :icon="Microphone"
              @click="playAudio('low')"
              :loading="loadingStates.audioLow"
            >
              播放一级提示音
            </el-button>
            
            <el-button 
              type="warning" 
              :icon="Microphone"
              @click="playAudio('mid')"
              :loading="loadingStates.audioMid"
            >
              播放二级提示音
            </el-button>
            
            <el-button 
              type="danger" 
              :icon="Microphone"
              @click="playAudio('high')"
              :loading="loadingStates.audioHigh"
            >
              播放三级提示音
            </el-button>
          </div>
        </div>
        
        <!-- 自定义GPIO控制 -->
        <div class="control-section">
          <h4 class="section-title">自定义GPIO控制</h4>
          <div class="custom-gpio">
            <el-form :model="customGPIO" inline>
              <el-form-item label="GPIO编号">
                <el-input-number
                  v-model="customGPIO.gpio"
                  :min="0"
                  :max="100"
                  controls-position="right"
                />
              </el-form-item>
              
              <el-form-item label="持续时间(秒)">
                <el-input-number
                  v-model="customGPIO.duration"
                  :min="0.1"
                  :max="10"
                  :step="0.1"
                  controls-position="right"
                />
              </el-form-item>
              
              <el-form-item>
                <el-button 
                  type="primary" 
                  :icon="VideoPlay"
                  @click="triggerCustomGPIO"
                  :loading="loadingStates.customGPIO"
                >
                  触发
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
        
        <!-- 网络功能测试 -->
        <div class="control-section">
          <h4 class="section-title">网络功能测试</h4>
          <div class="network-test-buttons">
            <div class="button-row">
              <el-button 
                type="primary" 
                @click="sendNetworkTest('ntp_sync')"
                :loading="loadingStates.networkTest"
                size="small"
              >
                NTP时间同步
              </el-button>
              
              <el-button 
                type="success" 
                @click="sendNetworkTest('get_gps')"
                :loading="loadingStates.networkTest"
                size="small"
              >
                获取GPS坐标
              </el-button>
              
              <el-button 
                type="info" 
                @click="sendNetworkTest('get_satellites')"
                :loading="loadingStates.networkTest"
                size="small"
              >
                获取星历
              </el-button>
            </div>
            
            <div class="button-row">
              <el-button 
                type="warning" 
                @click="sendNetworkTest('device_login')"
                :loading="loadingStates.networkTest"
                size="small"
              >
                设备登录
              </el-button>
              
              <el-button 
                type="success" 
                @click="sendNetworkTest('device_online')"
                :loading="loadingStates.networkTest"
                size="small"
              >
                设备上线
              </el-button>
              
              <el-button 
                type="danger" 
                @click="sendNetworkTest('device_offline')"
                :loading="loadingStates.networkTest"
                size="small"
              >
                设备离线
              </el-button>
            </div>
            
            <div class="button-row">
              <el-button 
                type="primary" 
                @click="sendNetworkTest('heartbeat')"
                :loading="loadingStates.networkTest"
                size="small"
              >
                心跳测试
              </el-button>
              
              <el-button 
                type="warning" 
                @click="sendNetworkTest('report_event')"
                :loading="loadingStates.networkTest"
                size="small"
              >
                上报事件
              </el-button>
              
              <el-button 
                type="info" 
                @click="sendNetworkTest('report_gps')"
                :loading="loadingStates.networkTest"
                size="small"
              >
                上报GPS
              </el-button>
            </div>
          </div>
          
          <!-- 网络状态显示 -->
          <div class="network-status-panel">
            <div class="status-header">
              <span>网络状态</span>
              <el-button 
                type="text" 
                size="small" 
                :icon="Refresh"
                @click="getNetworkStatus"
                :loading="loadingStates.networkStatus"
              >
                刷新
              </el-button>
            </div>
            <div class="status-content">
              <div class="status-item" v-if="networkStatus.module_initialized !== undefined">
                <span class="status-label">模块状态:</span>
                <el-tag :type="networkStatus.module_initialized ? 'success' : 'danger'" size="small">
                  {{ networkStatus.module_initialized ? '已初始化' : '未初始化' }}
                </el-tag>
              </div>
              <div class="status-item" v-if="networkStatus.token_valid !== undefined">
                <span class="status-label">Token状态:</span>
                <el-tag :type="networkStatus.token_valid ? 'success' : 'danger'" size="small">
                  {{ networkStatus.token_valid ? '有效' : '无效' }}
                </el-tag>
              </div>
              <div class="status-item" v-if="networkStatus.gnss_enabled !== undefined">
                <span class="status-label">GNSS状态:</span>
                <el-tag :type="networkStatus.gnss_enabled ? 'success' : 'warning'" size="small">
                  {{ networkStatus.gnss_enabled ? '已启用' : '未启用' }}
                </el-tag>
              </div>
              <div class="status-item" v-if="networkStatus.offline_queue_size !== undefined">
                <span class="status-label">离线队列:</span>
                <el-tag :type="networkStatus.offline_queue_size > 0 ? 'warning' : 'success'" size="small">
                  {{ networkStatus.offline_queue_size }} 条
                </el-tag>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 操作历史 -->
        <div class="control-section">
          <h4 class="section-title">操作历史</h4>
          <div class="operation-history">
            <el-timeline>
              <el-timeline-item
                v-for="(operation, index) in operationHistory"
                :key="index"
                :timestamp="operation.timestamp"
                :type="operation.type"
              >
                {{ operation.description }}
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Tools, 
  Bell, 
  BellFilled, 
  Sunny, 
  Microphone, 
  VideoPlay,
  ArrowDown,
  ArrowUp,
  Refresh
} from '@element-plus/icons-vue'
import socketService from '@/services/socket'
import audioService from '@/services/audio'

// 响应式数据
const loadingStates = reactive({
  gpio70: false,
  gpio71: false,
  gpio72: false,
  audioLow: false,
  audioMid: false,
  audioHigh: false,
  customGPIO: false,
  networkTest: false,
  networkStatus: false
})

const customGPIO = reactive({
  gpio: 70,
  duration: 1
})

const operationHistory = ref([])

// 折叠状态
const isCollapsed = ref(true) // 默认折叠

// 网络状态
const networkStatus = reactive({
  module_initialized: undefined,
  token_valid: undefined,
  gnss_enabled: undefined,
  offline_queue_size: undefined,
  current_time: undefined
})

// 音频元素引用
const audioElements = {
  low: null,
  mid: null,
  high: null
}

// 方法
const triggerGPIO = async (gpio, duration) => {
  const loadingKey = `gpio${gpio}`
  loadingStates[loadingKey] = true
  
  try {
    socketService.triggerGPIO(gpio, duration)
    addOperationHistory('success', `触发 GPIO${gpio} (${duration}秒)`)
    ElMessage.success(`GPIO${gpio} 触发成功`)
  } catch (error) {
    addOperationHistory('danger', `GPIO${gpio} 触发失败: ${error.message}`)
    ElMessage.error(`GPIO${gpio} 触发失败`)
  } finally {
    setTimeout(() => {
      loadingStates[loadingKey] = false
    }, 1000)
  }
}

const triggerCustomGPIO = async () => {
  loadingStates.customGPIO = true
  
  try {
    socketService.triggerGPIO(customGPIO.gpio, customGPIO.duration)
    addOperationHistory('success', `触发自定义 GPIO${customGPIO.gpio} (${customGPIO.duration}秒)`)
    ElMessage.success(`自定义GPIO${customGPIO.gpio} 触发成功`)
  } catch (error) {
    addOperationHistory('danger', `自定义GPIO${customGPIO.gpio} 触发失败: ${error.message}`)
    ElMessage.error(`自定义GPIO${customGPIO.gpio} 触发失败`)
  } finally {
    setTimeout(() => {
      loadingStates.customGPIO = false
    }, 1000)
  }
}

const playAudio = async (level) => {
  const loadingKey = `audio${level.charAt(0).toUpperCase() + level.slice(1)}`
  loadingStates[loadingKey] = true
  
  try {
    // 使用新的音频服务，强制播放（忽略防重复机制）
    const success = await audioService.playManual(level)
    
    if (success) {
      addOperationHistory('success', `播放${getAudioLevelText(level)}提示音`)
      ElMessage.success(`${getAudioLevelText(level)}提示音播放成功`)
    } else {
      addOperationHistory('danger', `${getAudioLevelText(level)}提示音播放失败`)
      ElMessage.error(`${getAudioLevelText(level)}提示音播放失败`)
    }
  } catch (error) {
    addOperationHistory('danger', `${getAudioLevelText(level)}提示音播放失败: ${error.message}`)
    ElMessage.error(`${getAudioLevelText(level)}提示音播放失败`)
  } finally {
    setTimeout(() => {
      loadingStates[loadingKey] = false
    }, 1000)
  }
}

const getAudioLevelText = (level) => {
  switch (level) {
    case 'low': return '一级'
    case 'mid': return '二级'
    case 'high': return '三级'
    default: return '未知'
  }
}

const addOperationHistory = (type, description) => {
  operationHistory.value.unshift({
    type,
    description,
    timestamp: new Date().toLocaleTimeString('zh-CN')
  })
  
  // 限制历史记录数量
  if (operationHistory.value.length > 20) {
    operationHistory.value = operationHistory.value.slice(0, 20)
  }
}

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

// 网络测试方法
const sendNetworkTest = async (testType) => {
  loadingStates.networkTest = true
  
  try {
    // 通过socketService发送网络测试请求
    socketService.sendNetworkTest(testType)
    addOperationHistory('success', `发送网络测试: ${getTestTypeText(testType)}`)
    ElMessage.info(`正在执行${getTestTypeText(testType)}...`)
  } catch (error) {
    addOperationHistory('danger', `网络测试失败: ${error.message}`)
    ElMessage.error(`网络测试失败: ${error.message}`)
  } finally {
    setTimeout(() => {
      loadingStates.networkTest = false
    }, 2000) // 2秒后自动取消loading状态
  }
}

// 获取网络状态
const getNetworkStatus = async () => {
  loadingStates.networkStatus = true
  
  try {
    socketService.getNetworkStatus()
    ElMessage.info('正在获取网络状态...')
  } catch (error) {
    ElMessage.error(`获取网络状态失败: ${error.message}`)
  } finally {
    setTimeout(() => {
      loadingStates.networkStatus = false
    }, 1000)
  }
}

// 获取测试类型文本
const getTestTypeText = (testType) => {
  const typeMap = {
    'ntp_sync': 'NTP时间同步',
    'get_gps': '获取GPS坐标',
    'get_satellites': '获取星历',
    'device_login': '设备登录',
    'device_online': '设备上线',
    'device_offline': '设备离线',
    'heartbeat': '心跳测试',
    'report_event': '上报事件数据',
    'report_gps': '上报GPS信息'
  }
  return typeMap[testType] || testType
}

// 处理网络测试结果
const handleNetworkTestResult = (data) => {
  if (data.success) {
    ElMessage.success(data.message)
    addOperationHistory('success', `${getTestTypeText(data.test_type)}: ${data.message}`)
  } else {
    ElMessage.error(data.message)
    addOperationHistory('danger', `${getTestTypeText(data.test_type)}失败: ${data.message}`)
  }
  
  // 更新网络状态
  if (data.status) {
    Object.assign(networkStatus, data.status)
  }
  
  // 显示特定数据
  if (data.gps_data) {
    ElMessage.info(`GPS坐标: ${data.gps_data.latitude}, ${data.gps_data.longitude}`)
  }
  if (data.satellite_data) {
    ElMessage.info(`卫星数量: ${data.satellite_data.total} (GPS: ${data.satellite_data.systems.GPS}, 北斗: ${data.satellite_data.systems.BeiDou})`)
  }
}

// 处理网络状态更新
const handleNetworkStatus = (status) => {
  Object.assign(networkStatus, status)
}

// 暴露方法给父组件
defineExpose({
  toggleCollapse,
  isCollapsed,
  handleNetworkTestResult,
  handleNetworkStatus
})
</script>

<style scoped lang="scss">
.gpio-control {
  width: 100%;
}

.gpio-card {
  background: rgba(6, 78, 59, 0.8);
  border: 1px solid #10b981;
  backdrop-filter: blur(10px);
  color: white;
  
  :deep(.el-card__header) {
    background: rgba(6, 78, 59, 0.9);
    border-bottom: 1px solid #10b981;
  }
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: white;
  
  .collapse-btn {
    margin-left: auto;
    color: #10b981;
    
    &:hover {
      color: #059669;
    }
  }
}

.gpio-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.control-section {
  padding: 16px;
  background: rgba(31, 41, 55, 0.3);
  border-radius: 8px;
  border: 1px solid rgba(16, 185, 129, 0.2);
}

.section-title {
  margin: 0 0 16px 0;
  color: #10b981;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid rgba(16, 185, 129, 0.3);
  padding-bottom: 8px;
}

.gpio-buttons,
.audio-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.network-test-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
  
  .button-row {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    
    .el-button {
      flex: 1;
      min-width: 120px;
    }
  }
}

.network-status-panel {
  margin-top: 16px;
  padding: 12px;
  background: rgba(31, 41, 55, 0.3);
  border-radius: 6px;
  border: 1px solid rgba(16, 185, 129, 0.2);
  
  .status-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    font-weight: 600;
    color: #10b981;
  }
  
  .status-content {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  
  .status-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .status-label {
      font-size: 12px;
      color: #9ca3af;
    }
  }
}

.custom-gpio {
  :deep(.el-form) {
    .el-form-item {
      margin-bottom: 0;
    }
    
    .el-form-item__label {
      color: #e5e7eb;
    }
    
    .el-input-number {
      .el-input__wrapper {
        background: rgba(31, 41, 55, 0.8);
        border: 1px solid #374151;
        color: white;
        
        &:hover {
          border-color: #10b981;
        }
        
        &.is-focus {
          border-color: #10b981;
          box-shadow: 0 0 0 1px #10b981;
        }
      }
      
      .el-input__inner {
        color: white;
      }
    }
  }
}

.operation-history {
  max-height: 200px;
  overflow-y: auto;
  
  :deep(.el-timeline) {
    .el-timeline-item__timestamp {
      color: #9ca3af;
      font-size: 12px;
    }
    
    .el-timeline-item__content {
      color: white;
      font-size: 14px;
    }
    
    .el-timeline-item__node {
      &.el-timeline-item__node--success {
        background: #10b981;
      }
      
      &.el-timeline-item__node--danger {
        background: #ef4444;
      }
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .gpio-buttons,
  .audio-buttons {
    .el-button {
      width: 100%;
    }
  }
  
  .custom-gpio {
    :deep(.el-form) {
      flex-direction: column;
      align-items: stretch;
      
      .el-form-item {
        width: 100%;
        margin-bottom: 12px;
      }
    }
  }
}
</style>
