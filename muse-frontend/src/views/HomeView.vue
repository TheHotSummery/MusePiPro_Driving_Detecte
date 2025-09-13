<script setup>
import { onMounted, onUnmounted, computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { TrendCharts as TrendChartsIcon, Expand, Fold } from '@element-plus/icons-vue'
import { useDetectionStore } from '@/stores/detection'
import VideoStream from '@/components/VideoStream.vue'
import DetectionPanel from '@/components/DetectionPanel.vue'
import StatusMonitor from '@/components/StatusMonitor.vue'
import EventList from '@/components/EventList.vue'
import ConfigPanel from '@/components/ConfigPanel.vue'
import WeightPanel from '@/components/WeightPanel.vue'
import GPIOControl from '@/components/GPIOControl.vue'
import FatigueTestPanel from '@/components/FatigueTestPanel.vue'
import socketService from '@/services/socket'
import audioService from '@/services/audio'

const detectionStore = useDetectionStore()

// 组件引用
const configPanelRef = ref(null)
const weightPanelRef = ref(null)
const gpioControlRef = ref(null)
const fatigueTestPanelRef = ref(null)

// 计算属性
const progressBarClass = computed(() => {
  if (detectionStore.progress >= 95) return 'level-3'
  if (detectionStore.progress >= 75) return 'level-2'
  if (detectionStore.progress >= 50) return 'level-1'
  return 'normal'
})

// 全局折叠状态
const allCollapsed = computed(() => {
  return configPanelRef.value?.isCollapsed && 
         weightPanelRef.value?.isCollapsed && 
         gpioControlRef.value?.isCollapsed &&
         fatigueTestPanelRef.value?.isCollapsed
})

// 进度变化监听 - 自动播放提示音
watch(
  () => detectionStore.progress,
  async (newProgress, oldProgress) => {
    // 只有当进度增加且达到阈值时才播放提示音
    if (newProgress > oldProgress) {
      await audioService.playAlertByProgress(newProgress)
    }
  },
  { immediate: false }
)

// 生命周期
onMounted(() => {
  // 延迟连接WebSocket，确保Pinia已完全初始化
  setTimeout(() => {
    socketService.connect()
    setupNetworkTestListeners()
    ElMessage.success('系统初始化完成')
  }, 100)
})

onUnmounted(() => {
  // 断开WebSocket连接
  socketService.disconnect()
})

// 设置网络测试事件监听
const setupNetworkTestListeners = () => {
  const socket = socketService.getSocket()
  if (socket) {
    // 监听网络测试结果
    socket.on('network_test_result', (data) => {
      if (gpioControlRef.value) {
        gpioControlRef.value.handleNetworkTestResult(data)
      }
    })
    
    // 监听网络状态更新
    socket.on('network_status', (status) => {
      if (gpioControlRef.value) {
        gpioControlRef.value.handleNetworkStatus(status)
      }
    })
  }
}

// 全局控制方法
const toggleAllPanels = () => {
  const shouldCollapse = !allCollapsed.value
  configPanelRef.value?.toggleCollapse()
  weightPanelRef.value?.toggleCollapse()
  gpioControlRef.value?.toggleCollapse()
  fatigueTestPanelRef.value?.toggleCollapse()
  
  // 如果当前是展开状态，则折叠所有面板
  if (!shouldCollapse) {
    configPanelRef.value.isCollapsed = true
    weightPanelRef.value.isCollapsed = true
    gpioControlRef.value.isCollapsed = true
    fatigueTestPanelRef.value.isCollapsed = true
  }
}
</script>

<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <h1 class="dashboard-title">
        <el-icon><Monitor /></el-icon>
        疲劳驾驶检测与多级预警系统
      </h1>
      <div class="global-controls">
        <el-button 
          :type="allCollapsed ? 'primary' : 'default'"
          :icon="allCollapsed ? Expand : Fold"
          @click="toggleAllPanels"
          size="small"
        >
          {{ allCollapsed ? '展开所有面板' : '折叠所有面板' }}
        </el-button>
      </div>
    </div>
    
    <div class="dashboard-content">
      <!-- 左侧视频流区域 -->
      <div class="left-panel">
        <!-- 视频流 -->
        <VideoStream />
        
        <!-- 状态信息 -->
        <StatusMonitor />
        
        <!-- 当前检测结果 -->
        <DetectionPanel />
        
        <!-- 分心等级 -->
        <div class="level-panel">
          <el-card class="level-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <el-icon><TrendChartsIcon /></el-icon>
                <span>分心等级</span>
              </div>
            </template>
            
            <div class="level-content">
              <div class="progress-container">
                <div class="progress-bar">
                  <div 
                    class="progress-fill" 
                    :style="{ width: detectionStore.progress + '%' }"
                    :class="progressBarClass"
                  ></div>
                </div>
                <div class="progress-text">
                  <span class="level-text">{{ detectionStore.levelText }}</span>
                  <span class="progress-value">{{ detectionStore.progress.toFixed(1) }}%</span>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </div>
      
      <!-- 右侧控制区域 -->
      <div class="right-panel">
        <!-- 事件记录 -->
        <EventList />
        
        <!-- 配置面板 -->
        <ConfigPanel ref="configPanelRef" />
        
        <!-- 权重面板 -->
        <WeightPanel ref="weightPanelRef" />
        
        <!-- GPIO控制 -->
        <GPIOControl ref="gpioControlRef" />
        
        <!-- 疲劳状态测试 -->
        <FatigueTestPanel ref="fatigueTestPanelRef" />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  min-height: 100vh;
  width: 100%;
  color: white;
  padding: 16px;
  font-family: 'Roboto', monospace;
  display: flex;
  flex-direction: column;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  flex-shrink: 0;
  
  .global-controls {
    display: flex;
    gap: 12px;
  }
}

.dashboard-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 2.5rem;
  font-weight: 700;
  color: #10b981;
  text-shadow: 0 0 20px rgba(16, 185, 129, 0.5);
  margin: 0;
  white-space: nowrap;
}

.dashboard-content {
  display: flex;
  gap: 20px;
  max-width: 1600px;
  margin: 0 auto;
  width: 100%;
  flex: 1;
}

.left-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
  transition: all 0.3s ease;
}

.right-panel {
  flex: 0 0 400px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
  transition: all 0.3s ease;
  
  // 自定义滚动条
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-track {
    background: rgba(31, 41, 55, 0.3);
    border-radius: 3px;
  }
  
  &::-webkit-scrollbar-thumb {
    background: #10b981;
    border-radius: 3px;
    
    &:hover {
      background: #059669;
    }
  }
}

// 分心等级面板样式
.level-panel {
  .level-card {
    background: rgba(6, 78, 59, 0.8);
    border: 1px solid #10b981;
    backdrop-filter: blur(10px);
    
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
  }
  
  .level-content {
    padding: 16px;
  }
  
  .progress-container {
    position: relative;
  }
  
  .progress-bar {
    width: 100%;
    height: 24px;
    background: rgba(6, 78, 59, 0.6);
    border-radius: 12px;
    overflow: hidden;
    border: 1px solid rgba(16, 185, 129, 0.3);
  }
  
  .progress-fill {
    height: 100%;
    border-radius: 12px;
    transition: all 0.5s ease;
    
    &.normal {
      background: #10b981;
    }
    
    &.level-1 {
      background: #10b981;
    }
    
    &.level-2 {
      background: #f59e0b;
    }
    
    &.level-3 {
      background: #ef4444;
    }
  }
  
  .progress-text {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    pointer-events: none;
  }
  
  .level-text {
    font-size: 16px;
    font-weight: 600;
    color: white;
  }
  
  .progress-value {
    font-size: 12px;
    color: #9ca3af;
  }
}

// 响应式设计
@media (max-width: 1200px) {
  .dashboard-content {
    flex-direction: column;
    gap: 20px;
  }
  
  .right-panel {
    max-height: none;
    overflow-y: visible;
  }
}

@media (max-width: 768px) {
  .dashboard {
    padding: 12px;
  }
  
  .dashboard-header {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
  
  .dashboard-title {
    font-size: 1.8rem;
    justify-content: center;
  }
  
  .left-panel,
  .right-panel {
    gap: 16px;
  }
  
  .right-panel {
    flex: 0 0 auto;
  }
}

@media (max-width: 480px) {
  .dashboard-title {
    font-size: 1.5rem;
  }
}
</style>
