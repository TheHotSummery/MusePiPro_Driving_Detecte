<template>
  <div class="status-monitor">
    <el-card class="status-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <el-icon><Monitor /></el-icon>
          <span>状态信息</span>
          <div class="connection-status">
            <el-tag 
              :type="connectionType" 
              size="small"
              :icon="connectionIcon"
            >
              {{ connectionStatus }}
            </el-tag>
          </div>
        </div>
      </template>
      
      <div class="status-content">
        <div class="status-grid">
          <div class="status-item">
            <div class="status-label">
              <el-icon><Warning /></el-icon>
              <span>分心驾驶总次数</span>
            </div>
            <div class="status-value distracted-count">
              {{ distractedCount }}
            </div>
          </div>
          
          <div class="status-item">
            <div class="status-label">
              <el-icon><InfoFilled /></el-icon>
              <span>当前状态</span>
            </div>
            <div class="status-value" :class="statusClass">
              {{ status }}
            </div>
          </div>
          
          <div class="status-item">
            <div class="status-label">
              <el-icon><Timer /></el-icon>
              <span>帧率 (FPS)</span>
            </div>
            <div class="status-value fps">
              {{ fps.toFixed(1) }}
            </div>
          </div>
          
          <div class="status-item">
            <div class="status-label">
              <el-icon><Cpu /></el-icon>
              <span>CPU 使用率</span>
            </div>
            <div class="status-value cpu">
              {{ cpuUsage.toFixed(1) }}%
            </div>
          </div>
        </div>
        
        <!-- 错误信息显示 -->
        <div v-if="errorMessage" class="error-message">
          <el-alert
            :title="errorMessage"
            type="error"
            :closable="false"
            show-icon
          />
        </div>
        
        <!-- 最后更新时间 -->
        <div v-if="lastUpdateTime" class="last-update">
          <el-icon><Clock /></el-icon>
          <span>最后更新: {{ formatTime(lastUpdateTime) }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { 
  Monitor, 
  Warning, 
  InfoFilled, 
  Timer, 
  Cpu, 
  Clock,
  Connection,
  Close
} from '@element-plus/icons-vue'
import { useDetectionStore } from '@/stores/detection'
import { useSystemStore } from '@/stores/system'

const detectionStore = useDetectionStore()
const systemStore = useSystemStore()

// 计算属性
const distractedCount = computed(() => detectionStore.distractedCount)
const status = computed(() => detectionStore.status)
const fps = computed(() => detectionStore.fps)
const cpuUsage = computed(() => detectionStore.cpuUsage)
const connectionStatus = computed(() => systemStore.connectionStatus)
const errorMessage = computed(() => systemStore.errorMessage)
const lastUpdateTime = computed(() => systemStore.lastUpdateTime)

const connectionType = computed(() => {
  if (systemStore.isConnected) return 'success'
  if (errorMessage.value) return 'danger'
  return 'warning'
})

const connectionIcon = computed(() => {
  return systemStore.isConnected ? Connection : Close
})

const statusClass = computed(() => {
  if (detectionStore.isFatigue) return 'status-fatigue'
  if (detectionStore.isDistracted) return 'status-distracted'
  return 'status-normal'
})

// 方法
const formatTime = (date) => {
  if (!date) return ''
  return date.toLocaleTimeString('zh-CN', {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}
</script>

<style scoped lang="scss">
.status-monitor {
  width: 100%;
}

.status-card {
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
  
  .connection-status {
    margin-left: auto;
  }
}

.status-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.status-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  background: rgba(31, 41, 55, 0.5);
  border-radius: 8px;
  border: 1px solid rgba(16, 185, 129, 0.3);
}

.status-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #9ca3af;
  font-weight: 500;
}

.status-value {
  font-size: 18px;
  font-weight: 600;
  color: white;
  
  &.distracted-count {
    color: #f59e0b;
  }
  
  &.fps {
    color: #10b981;
  }
  
  &.cpu {
    color: #3b82f6;
  }
  
  &.status-normal {
    color: #10b981;
  }
  
  &.status-distracted {
    color: #f59e0b;
  }
  
  &.status-fatigue {
    color: #ef4444;
  }
}

.error-message {
  :deep(.el-alert) {
    background: rgba(239, 68, 68, 0.1);
    border: 1px solid rgba(239, 68, 68, 0.3);
    color: #fca5a5;
  }
}

.last-update {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #9ca3af;
  justify-content: center;
  padding: 8px;
  background: rgba(31, 41, 55, 0.3);
  border-radius: 4px;
}

// 响应式设计
@media (max-width: 768px) {
  .status-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    
    .connection-status {
      margin-left: 0;
    }
  }
}
</style>
