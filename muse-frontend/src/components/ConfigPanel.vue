<template>
  <div class="config-panel">
    <el-card class="config-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <el-icon><Setting /></el-icon>
          <span>参数调节</span>
          <div class="header-controls">
            <el-button 
              type="primary" 
              size="small" 
              :icon="Upload"
              @click="updateConfig"
            >
              更新配置
            </el-button>
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
      
      <div v-show="!isCollapsed" class="config-content">
        <el-form :model="config" label-width="200px" class="config-form">
          <div class="config-section">
            <h4 class="section-title">基础参数</h4>
            
            <el-form-item label="单动作持续时间阈值（秒）">
              <div class="slider-container">
                <el-slider
                  v-model="config.duration_threshold"
                  :min="0.5"
                  :max="5"
                  :step="0.1"
                  class="slider"
                />
                <el-input-number
                  v-model="config.duration_threshold"
                  :min="0.5"
                  :max="5"
                  :step="0.1"
                  :precision="1"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
            
            <el-form-item label="疲劳动作持续时间阈值（秒）">
              <div class="slider-container">
                <el-slider
                  v-model="config.fatigue_duration_threshold"
                  :min="0.5"
                  :max="5"
                  :step="0.1"
                  class="slider"
                />
                <el-input-number
                  v-model="config.fatigue_duration_threshold"
                  :min="0.5"
                  :max="5"
                  :step="0.1"
                  :precision="1"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
            
            <el-form-item label="单动作最小检测次数">
              <div class="slider-container">
                <el-slider
                  v-model="config.min_detections_for_duration"
                  :min="1"
                  :max="10"
                  :step="1"
                  class="slider"
                />
                <el-input-number
                  v-model="config.min_detections_for_duration"
                  :min="1"
                  :max="10"
                  :step="1"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
            
            <el-form-item label="多动作种类数阈值">
              <div class="slider-container">
                <el-slider
                  v-model="config.count_threshold"
                  :min="1"
                  :max="5"
                  :step="1"
                  class="slider"
                />
                <el-input-number
                  v-model="config.count_threshold"
                  :min="1"
                  :max="5"
                  :step="1"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
            
            <el-form-item label="多动作时间窗口（秒）">
              <div class="slider-container">
                <el-slider
                  v-model="config.window_size"
                  :min="10"
                  :max="60"
                  :step="1"
                  class="slider"
                />
                <el-input-number
                  v-model="config.window_size"
                  :min="10"
                  :max="60"
                  :step="1"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
            
            <el-form-item label="多动作加权得分阈值">
              <div class="slider-container">
                <el-slider
                  v-model="config.score_threshold"
                  :min="0.5"
                  :max="2"
                  :step="0.01"
                  class="slider"
                />
                <el-input-number
                  v-model="config.score_threshold"
                  :min="0.5"
                  :max="2"
                  :step="0.01"
                  :precision="2"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
          </div>
          
          <div class="config-section">
            <h4 class="section-title">置信度参数</h4>
            
            <el-form-item label="最低检测置信度">
              <div class="slider-container">
                <el-slider
                  v-model="config.min_confidence"
                  :min="0.5"
                  :max="0.95"
                  :step="0.01"
                  class="slider"
                />
                <el-input-number
                  v-model="config.min_confidence"
                  :min="0.5"
                  :max="0.95"
                  :step="0.01"
                  :precision="2"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
            
            <el-form-item label="疲劳检测置信度">
              <div class="slider-container">
                <el-slider
                  v-model="config.fatigue_min_confidence"
                  :min="0.5"
                  :max="0.95"
                  :step="0.01"
                  class="slider"
                />
                <el-input-number
                  v-model="config.fatigue_min_confidence"
                  :min="0.5"
                  :max="0.95"
                  :step="0.01"
                  :precision="2"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
          </div>
          
          <div class="config-section">
            <h4 class="section-title">时间参数</h4>
            
            <el-form-item label="多动作冷却时间（秒）">
              <div class="slider-container">
                <el-slider
                  v-model="config.multi_event_cooldown"
                  :min="5"
                  :max="30"
                  :step="1"
                  class="slider"
                />
                <el-input-number
                  v-model="config.multi_event_cooldown"
                  :min="5"
                  :max="30"
                  :step="1"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
            
            <el-form-item label="三级冷却时间（秒）">
              <div class="slider-container">
                <el-slider
                  v-model="config.level3_cooldown"
                  :min="10"
                  :max="60"
                  :step="1"
                  class="slider"
                />
                <el-input-number
                  v-model="config.level3_cooldown"
                  :min="10"
                  :max="60"
                  :step="1"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
            
            <el-form-item label="清醒重置时间（秒）">
              <div class="slider-container">
                <el-slider
                  v-model="config.level_reset_threshold"
                  :min="60"
                  :max="600"
                  :step="10"
                  class="slider"
                />
                <el-input-number
                  v-model="config.level_reset_threshold"
                  :min="60"
                  :max="600"
                  :step="10"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
            
            <el-form-item label="专注驾驶恢复时间（秒）">
              <div class="slider-container">
                <el-slider
                  v-model="config.safe_driving_confirm_time"
                  :min="1"
                  :max="10"
                  :step="0.1"
                  class="slider"
                />
                <el-input-number
                  v-model="config.safe_driving_confirm_time"
                  :min="1"
                  :max="10"
                  :step="0.1"
                  :precision="1"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
            
            <el-form-item label="目标帧率（FPS）">
              <div class="slider-container">
                <el-slider
                  v-model="config.fps_target"
                  :min="1"
                  :max="10"
                  :step="0.1"
                  class="slider"
                />
                <el-input-number
                  v-model="config.fps_target"
                  :min="1"
                  :max="10"
                  :step="0.1"
                  :precision="1"
                  size="small"
                  class="input-number"
                />
              </div>
            </el-form-item>
          </div>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, watch, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting, Upload, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { useConfigStore } from '@/stores/config'
import socketService from '@/services/socket'

const configStore = useConfigStore()

// 响应式配置数据
const config = reactive({ ...configStore.config })

// 折叠状态
const isCollapsed = ref(true) // 默认折叠

// 监听配置变化，同步到store
watch(config, (newConfig) => {
  configStore.updateConfig(newConfig)
}, { deep: true })

// 方法
const updateConfig = () => {
  try {
    socketService.updateConfig()
    ElMessage.success('配置更新成功')
  } catch (error) {
    ElMessage.error('配置更新失败')
    console.error('更新配置失败:', error)
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
.config-panel {
  width: 100%;
}

.config-card {
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
  
  .collapse-btn {
    color: #10b981;
    
    &:hover {
      color: #059669;
    }
  }
}

.config-content {
  max-height: 600px;
  overflow-y: auto;
}

.config-form {
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

.config-section {
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

// 响应式设计
@media (max-width: 768px) {
  .config-form {
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
    
    .el-button {
      margin-left: 0;
      width: 100%;
    }
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