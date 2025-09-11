<template>
  <div class="weight-panel">
    <el-card class="weight-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <el-icon><Setting /></el-icon>
          <span>行为权重调节</span>
          <div class="header-controls">
            <div class="weight-controls">
              <el-button 
                type="primary" 
                size="small" 
                :icon="Upload"
                @click="updateWeights"
              >
                更新权重
              </el-button>
              <el-button 
                type="default" 
                size="small" 
                :icon="Refresh"
                @click="resetWeights"
              >
                恢复默认
              </el-button>
            </div>
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
        </div>
      </template>
      
      <div v-show="!isCollapsed" class="weight-content">
        <el-form :model="weights" label-width="200px" class="weight-form">
          <div class="weight-section">
            <h4 class="section-title">疲劳行为权重</h4>
            
            <el-form-item label="闭眼权重">
              <div class="slider-container">
                <el-slider
                  v-model="weights.eyes_closed"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  class="slider"
                />
                <el-input-number
                  v-model="weights.eyes_closed"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  :precision="2"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
            
            <el-form-item label="打哈欠权重">
              <div class="slider-container">
                <el-slider
                  v-model="weights.yarning"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  class="slider"
                />
                <el-input-number
                  v-model="weights.yarning"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  :precision="2"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
            
            <el-form-item label="闭眼向左权重">
              <div class="slider-container">
                <el-slider
                  v-model="weights.eyes_closed_head_left"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  class="slider"
                />
                <el-input-number
                  v-model="weights.eyes_closed_head_left"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  :precision="2"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
            
            <el-form-item label="闭眼向右权重">
              <div class="slider-container">
                <el-slider
                  v-model="weights.eyes_closed_head_right"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  class="slider"
                />
                <el-input-number
                  v-model="weights.eyes_closed_head_right"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  :precision="2"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
          </div>
          
          <div class="weight-section">
            <h4 class="section-title">分心行为权重</h4>
            
            <el-form-item label="低头权重">
              <div class="slider-container">
                <el-slider
                  v-model="weights.head_down"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  class="slider"
                />
                <el-input-number
                  v-model="weights.head_down"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  :precision="2"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
            
            <el-form-item label="向左看权重">
              <div class="slider-container">
                <el-slider
                  v-model="weights.seeing_left"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  class="slider"
                />
                <el-input-number
                  v-model="weights.seeing_left"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  :precision="2"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
            
            <el-form-item label="向右看权重">
              <div class="slider-container">
                <el-slider
                  v-model="weights.seeing_right"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  class="slider"
                />
                <el-input-number
                  v-model="weights.seeing_right"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  :precision="2"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
            
            <el-form-item label="抬头权重">
              <div class="slider-container">
                <el-slider
                  v-model="weights.head_up"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  class="slider"
                />
                <el-input-number
                  v-model="weights.head_up"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  :precision="2"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
          </div>
        </el-form>
        
        <!-- 权重统计 -->
        <div class="weight-stats">
          <div class="stat-item">
            <span class="stat-label">疲劳权重总和:</span>
            <span class="stat-value fatigue">{{ fatigueWeightSum.toFixed(2) }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">分心权重总和:</span>
            <span class="stat-value distracted">{{ distractedWeightSum.toFixed(2) }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">总权重:</span>
            <span class="stat-value total">{{ totalWeightSum.toFixed(2) }}</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, computed, watch, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Setting, Upload, Refresh, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { useConfigStore } from '@/stores/config'
import socketService from '@/services/socket'

const configStore = useConfigStore()

// 响应式权重数据
const weights = reactive({ ...configStore.weights })

// 折叠状态
const isCollapsed = ref(true) // 默认折叠

// 监听权重变化，同步到store
watch(weights, (newWeights) => {
  configStore.updateWeights(newWeights)
}, { deep: true })

// 计算属性
const fatigueWeightSum = computed(() => {
  return weights.eyes_closed + weights.yarning + 
         weights.eyes_closed_head_left + weights.eyes_closed_head_right
})

const distractedWeightSum = computed(() => {
  return weights.head_down + weights.seeing_left + 
         weights.seeing_right + weights.head_up
})

const totalWeightSum = computed(() => {
  return fatigueWeightSum.value + distractedWeightSum.value
})

// 方法
const updateWeights = () => {
  try {
    socketService.updateWeights()
    ElMessage.success('权重更新成功')
  } catch (error) {
    ElMessage.error('权重更新失败')
    console.error('更新权重失败:', error)
  }
}

const resetWeights = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要恢复默认权重吗？',
      '确认重置',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    configStore.resetWeights()
    Object.assign(weights, configStore.defaultWeights)
    socketService.updateWeights()
    ElMessage.success('权重已恢复默认值')
  } catch {
    // 用户取消操作
  }
}

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

// 暴露方法给父组件
defineExpose({
  toggleCollapse,
  isCollapsed
})
</script>

<style scoped lang="scss">
.weight-panel {
  width: 100%;
}

.weight-card {
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
  
  .header-controls {
    margin-left: auto;
    display: flex;
    gap: 8px;
    align-items: center;
  }
  
  .weight-controls {
    display: flex;
    gap: 8px;
  }
  
  .collapse-btn {
    color: #10b981;
    
    &:hover {
      color: #059669;
    }
  }
}

.weight-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.weight-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;
    
    .el-form-item__label {
      color: #e5e7eb;
      font-weight: 500;
    }
  }
}

.slider-container {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  
  .slider {
    flex: 1;
    
    :deep(.el-slider__runway) {
      background: rgba(55, 65, 81, 0.8);
    }
    
    :deep(.el-slider__bar) {
      background: #10b981;
    }
    
    :deep(.el-slider__button) {
      border-color: #10b981;
    }
  }
  
  .input-number {
    width: 80px;
    
    :deep(.el-input__wrapper) {
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
    
    :deep(.el-input__inner) {
      color: white;
    }
  }
}

.weight-section {
  margin-bottom: 24px;
  padding: 16px;
  background: rgba(6, 78, 59, 0.3);
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

.weight-stats {
  display: flex;
  justify-content: space-around;
  padding: 16px;
  background: rgba(6, 78, 59, 0.3);
  border-radius: 8px;
  border: 1px solid rgba(16, 185, 129, 0.2);
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
  
  &.total {
    color: #10b981;
  }
}

// 响应式设计
@media (max-width: 768px) {
  .weight-form {
    :deep(.el-form-item) {
      .el-form-item__label {
        width: 100% !important;
        text-align: left;
        margin-bottom: 8px;
      }
    }
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    
    .weight-controls {
      margin-left: 0;
      width: 100%;
      justify-content: space-between;
    }
  }
  
  .weight-stats {
    flex-direction: column;
    gap: 8px;
  }
  
  .stat-item {
    flex-direction: row;
    justify-content: space-between;
  }
  
  .slider-container {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
    
    .input-number {
      width: 100%;
    }
  }
}
</style>