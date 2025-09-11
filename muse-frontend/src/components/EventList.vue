<template>
  <div class="event-list">
    <el-card class="event-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <el-icon><List /></el-icon>
          <span>事件记录</span>
          <div class="event-controls">
            <el-button 
              type="danger" 
              size="small" 
              :icon="Delete"
              @click="clearEvents"
            >
              清空记录
            </el-button>
          </div>
        </div>
      </template>
      
      <div class="event-content">
        <el-table 
          :data="events" 
          style="width: 100%"
          height="300"
          :row-class-name="getRowClassName"
          empty-text="暂无事件记录"
        >
          <el-table-column prop="time" label="时间" width="120" />
          <el-table-column prop="behavior" label="行为" width="100" />
          <el-table-column prop="duration" label="持续(s)" width="80" />
          <el-table-column prop="count" label="次数" width="60" />
          <el-table-column prop="confidence" label="置信度" width="80">
            <template #default="{ row }">
              <el-tag 
                :type="getConfidenceType(row.confidence)" 
                size="small"
              >
                {{ (row.confidence * 100).toFixed(1) }}%
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="event_type" label="类型" width="80">
            <template #default="{ row }">
              <el-tag 
                :type="getEventType(row.event_type)" 
                size="small"
              >
                {{ getEventTypeText(row.event_type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="level" label="等级" width="80">
            <template #default="{ row }">
              <el-tag 
                :type="getLevelType(row.level)" 
                size="small"
              >
                {{ getLevelText(row.level) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 事件统计 -->
        <div class="event-stats">
          <div class="stat-item">
            <span class="stat-label">总事件数:</span>
            <span class="stat-value">{{ events.length }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">疲劳事件:</span>
            <span class="stat-value fatigue">{{ fatigueEventCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">分心事件:</span>
            <span class="stat-value distracted">{{ distractedEventCount }}</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { List, Delete } from '@element-plus/icons-vue'
import { useDetectionStore } from '@/stores/detection'
import socketService from '@/services/socket'

const detectionStore = useDetectionStore()

// 计算属性
const events = computed(() => detectionStore.events)

const fatigueEventCount = computed(() => {
  return events.value.filter(event => 
    event.event_type === 'fatigue' || 
    ['eyes_closed', 'yarning', 'eyes_closed_head_left', 'eyes_closed_head_right'].includes(event.behavior)
  ).length
})

const distractedEventCount = computed(() => {
  return events.value.filter(event => 
    event.event_type === 'distracted' || 
    ['head_down', 'seeing_left', 'seeing_right', 'head_up'].includes(event.behavior)
  ).length
})

// 方法
const getRowClassName = ({ row }) => {
  if (row.event_type === 'fatigue') return 'fatigue-row'
  if (row.event_type === 'distracted') return 'distracted-row'
  return ''
}

const getConfidenceType = (confidence) => {
  if (confidence >= 0.9) return 'success'
  if (confidence >= 0.7) return 'warning'
  return 'danger'
}

const getEventType = (eventType) => {
  switch (eventType) {
    case 'fatigue': return 'danger'
    case 'distracted': return 'warning'
    default: return 'info'
  }
}

const getEventTypeText = (eventType) => {
  switch (eventType) {
    case 'fatigue': return '疲劳'
    case 'distracted': return '分心'
    default: return '其他'
  }
}

const getLevelType = (level) => {
  switch (level) {
    case 'Level 3': return 'danger'
    case 'Level 2': return 'warning'
    case 'Level 1': return 'success'
    default: return 'info'
  }
}

const getLevelText = (level) => {
  switch (level) {
    case 'Level 3': return '三级'
    case 'Level 2': return '二级'
    case 'Level 1': return '一级'
    default: return '正常'
  }
}

const clearEvents = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清空所有事件记录和计数器吗？此操作不可恢复。',
      '确认清空',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    socketService.clearEvents()
    ElMessage.success('事件记录已清空')
  } catch {
    // 用户取消操作
  }
}
</script>

<style scoped lang="scss">
.event-list {
  width: 100%;
}

.event-card {
  background: rgba(6, 78, 59, 0.8);
  border: 1px solid #10b981;
  backdrop-filter: blur(10px);
  color: white;
  
  :deep(.el-card__header) {
    background: rgba(6, 78, 59, 0.9);
    border-bottom: 1px solid #10b981;
  }
  
  :deep(.el-card__body) {
    padding: 16px;
  }
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: white;
  
  .event-controls {
    margin-left: auto;
  }
}

.event-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

// 表格样式
:deep(.el-table) {
  background: rgba(31, 41, 55, 0.8);
  color: white;
  
  .el-table__header {
    background: #1f2937;
    
    th {
      background: #1f2937;
      color: #9ca3af;
      border-bottom: 1px solid #374151;
    }
  }
  
  .el-table__body {
    tr {
      background: rgba(31, 41, 55, 0.8);
      
      &:hover {
        background: rgba(59, 130, 246, 0.1);
      }
      
      &.fatigue-row {
        background: rgba(239, 68, 68, 0.1);
        
        &:hover {
          background: rgba(239, 68, 68, 0.2);
        }
      }
      
      &.distracted-row {
        background: rgba(245, 158, 11, 0.1);
        
        &:hover {
          background: rgba(245, 158, 11, 0.2);
        }
      }
    }
    
    td {
      border-bottom: 1px solid #374151;
      color: white;
    }
  }
  
  .el-table__empty-block {
    background: rgba(31, 41, 55, 0.8);
    color: #9ca3af;
  }
}

.event-stats {
  display: flex;
  justify-content: space-around;
  padding: 12px;
  background: rgba(31, 41, 55, 0.5);
  border-radius: 8px;
  border: 1px solid rgba(16, 185, 129, 0.3);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: #9ca3af;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: white;
  
  &.fatigue {
    color: #ef4444;
  }
  
  &.distracted {
    color: #f59e0b;
  }
}

// 响应式设计
@media (max-width: 768px) {
  .event-stats {
    flex-direction: column;
    gap: 8px;
  }
  
  .stat-item {
    flex-direction: row;
    justify-content: space-between;
  }
  
  :deep(.el-table) {
    font-size: 12px;
    
    .el-table__header th,
    .el-table__body td {
      padding: 8px 4px;
    }
  }
}
</style>
