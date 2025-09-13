<template>
  <div class="fatigue-test-panel">
    <el-card class="test-card" shadow="hover">
      <template #header>
        <div class="card-header" @click="toggleCollapse">
          <el-icon><Tools /></el-icon>
          <span>疲劳状态测试</span>
          <el-tag 
            :type="isTestMode ? 'warning' : 'info'" 
            size="small"
            class="test-mode-tag"
          >
            {{ isTestMode ? '测试模式' : '正常模式' }}
          </el-tag>
          <el-icon 
            :class="['collapse-icon', { 'collapsed': isCollapsed }]"
            class="collapse-toggle"
            :title="isCollapsed ? '点击展开' : '点击折叠'"
          >
            <ArrowDown />
          </el-icon>
        </div>
      </template>
      
      <!-- 折叠状态提示 -->
      <div v-show="isCollapsed" class="collapsed-hint">
        <el-text type="info" size="small">
          <el-icon><InfoFilled /></el-icon>
          点击展开疲劳状态测试功能
        </el-text>
      </div>
      
      <div v-show="!isCollapsed" class="test-content">
        <!-- 测试模式开关 -->
        <div class="test-mode-section">
          <el-switch
            v-model="isTestMode"
            active-text="启用测试模式"
            inactive-text="正常检测模式"
            @change="toggleTestMode"
          />
        </div>
        
        <!-- 测试控制面板 -->
        <div v-if="isTestMode" class="test-controls">
          <!-- 分数设置 -->
          <div class="score-section">
            <h4>疲劳分数设置</h4>
            <div class="score-controls">
              <el-slider
                v-model="testScore"
                :min="0"
                :max="100"
                :step="1"
                show-input
                :show-input-controls="false"
                class="score-slider"
              />
              <div class="quick-scores">
                <el-button 
                  v-for="score in quickScores" 
                  :key="score.value"
                  :type="score.type"
                  size="small"
                  @click="setQuickScore(score.value)"
                >
                  {{ score.label }}
                </el-button>
              </div>
            </div>
          </div>
          
          <!-- 疲劳行为设置 -->
          <div class="behavior-section">
            <h4>疲劳行为设置</h4>
            <div class="behavior-controls">
              <el-checkbox-group v-model="selectedBehaviors">
                <el-checkbox 
                  v-for="behavior in fatigueBehaviors" 
                  :key="behavior.value"
                  :label="behavior.value"
                >
                  {{ behavior.label }}
                </el-checkbox>
              </el-checkbox-group>
            </div>
          </div>
          
          <!-- 测试操作 -->
          <div class="test-actions">
            <el-button 
              type="primary" 
              :loading="isApplying"
              @click="applyTestSettings"
            >
              应用测试设置
            </el-button>
            <el-button 
              type="warning" 
              @click="resetTestSettings"
            >
              重置设置
            </el-button>
            <el-button 
              type="success" 
              @click="simulateFatigueEvent"
            >
              模拟疲劳事件
            </el-button>
          </div>
          
          <!-- 测试说明 -->
          <div class="test-info">
            <el-alert
              title="测试模式说明"
              type="info"
              :closable="false"
              show-icon
            >
              <template #default>
                <div class="info-content">
                  <p>• 测试模式会模拟真实的疲劳检测流程</p>
                  <p>• 分数会根据行为权重和时间自动调整</p>
                  <p>• 长时间无行为会自动回退分数</p>
                  <p>• GPIO警报和事件记录功能正常</p>
                  <p>• 支持网络上报和GPS数据发送</p>
                </div>
              </template>
            </el-alert>
          </div>
        </div>
        
        <!-- 当前状态显示 -->
        <div class="current-status">
          <h4>当前状态</h4>
          <div class="status-info">
            <el-tag :type="getLevelType(detectionStore.progress)" size="large">
              {{ detectionStore.levelText }} ({{ detectionStore.progress.toFixed(1) }}%)
            </el-tag>
            <span class="status-text">{{ detectionStore.status }}</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Tools, ArrowDown, InfoFilled } from '@element-plus/icons-vue'
import { useDetectionStore } from '@/stores/detection'
import { apiService } from '@/services/api'

const detectionStore = useDetectionStore()

// 响应式数据
const isTestMode = ref(false)
const testScore = ref(0)
const selectedBehaviors = ref([])
const isApplying = ref(false)
const isCollapsed = ref(true) // 默认折叠状态

// 快速分数设置
const quickScores = [
  { value: 0, label: '正常', type: 'success' },
  { value: 50, label: '一级', type: 'warning' },
  { value: 75, label: '二级', type: 'warning' },
  { value: 95, label: '三级', type: 'danger' }
]

// 疲劳行为选项
const fatigueBehaviors = [
  { value: 'eyes_closed', label: '闭眼' },
  { value: 'yarning', label: '打哈欠' },
  { value: 'eyes_closed_head_left', label: '闭眼左倾' },
  { value: 'eyes_closed_head_right', label: '闭眼右倾' },
  { value: 'head_down', label: '低头' },
  { value: 'seeing_left', label: '左看' },
  { value: 'seeing_right', label: '右看' },
  { value: 'head_up', label: '抬头' }
]

// 方法
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const toggleTestMode = (enabled) => {
  if (enabled) {
    ElMessage.info('已启用测试模式，可以手动设置疲劳状态')
  } else {
    ElMessage.info('已切换到正常检测模式')
    resetTestSettings()
  }
}

const setQuickScore = (score) => {
  testScore.value = score
}

const getLevelType = (progress) => {
  if (progress >= 95) return 'danger'
  if (progress >= 75) return 'warning'
  if (progress >= 50) return 'warning'
  return 'success'
}

const applyTestSettings = async () => {
  if (!isTestMode.value) {
    ElMessage.warning('请先启用测试模式')
    return
  }
  
  isApplying.value = true
  
  try {
    const testData = {
      score: testScore.value,
      behaviors: selectedBehaviors.value,
      is_test_mode: true
    }
    
    await apiService.setTestFatigueState(testData)
    
    ElMessage.success('测试设置已应用')
    
    // 更新本地状态显示
    detectionStore.updateDetection({
      progress: testScore.value,
      level: getLevelFromScore(testScore.value),
      is_fatigue: testScore.value >= 75,
      is_distracted: testScore.value >= 50 && testScore.value < 75
    })
    
  } catch (error) {
    console.error('应用测试设置失败:', error)
    ElMessage.error('应用测试设置失败: ' + error.message)
  } finally {
    isApplying.value = false
  }
}

const resetTestSettings = async () => {
  try {
    await apiService.resetTestFatigueState()
    testScore.value = 0
    selectedBehaviors.value = []
    ElMessage.success('测试设置已重置')
  } catch (error) {
    console.error('重置测试设置失败:', error)
    ElMessage.error('重置测试设置失败: ' + error.message)
  }
}

const simulateFatigueEvent = async () => {
  if (!isTestMode.value) {
    ElMessage.warning('请先启用测试模式')
    return
  }
  
  try {
    const eventData = {
      behavior: selectedBehaviors.value.length > 0 ? selectedBehaviors.value[0] : 'eyes_closed',
      confidence: 0.85,
      duration: 3.0,
      score: testScore.value
    }
    
    await apiService.simulateFatigueEvent(eventData)
    ElMessage.success('疲劳事件模拟已发送')
  } catch (error) {
    console.error('模拟疲劳事件失败:', error)
    ElMessage.error('模拟疲劳事件失败: ' + error.message)
  }
}

const getLevelFromScore = (score) => {
  if (score >= 95) return 'Level 3'
  if (score >= 75) return 'Level 2'
  if (score >= 50) return 'Level 1'
  return 'Normal'
}
</script>

<style scoped lang="scss">
.fatigue-test-panel {
  width: 100%;
}

.test-card {
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
  cursor: pointer;
  user-select: none;
  transition: all 0.3s ease;
  
  &:hover {
    background: rgba(16, 185, 129, 0.1);
    border-radius: 4px;
    padding: 4px 8px;
    margin: -4px -8px;
  }
  
  .test-mode-tag {
    margin-left: auto;
  }
  
  .collapse-toggle {
    margin-left: 8px;
    transition: transform 0.3s ease;
    
    &.collapsed {
      transform: rotate(-90deg);
    }
  }
}

.collapsed-hint {
  padding: 16px;
  text-align: center;
  background: rgba(31, 41, 55, 0.3);
  border-radius: 4px;
  margin: 8px 0;
  
  :deep(.el-text) {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    color: #9ca3af;
  }
}

.test-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.test-mode-section {
  display: flex;
  justify-content: center;
  padding: 10px 0;
  border-bottom: 1px solid rgba(16, 185, 129, 0.3);
}

.test-controls {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.score-section, .behavior-section {
  h4 {
    color: #10b981;
    margin: 0 0 12px 0;
    font-size: 14px;
    font-weight: 600;
  }
}

.score-controls {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.score-slider {
  :deep(.el-slider__runway) {
    background-color: rgba(16, 185, 129, 0.2);
  }
  
  :deep(.el-slider__bar) {
    background-color: #10b981;
  }
  
  :deep(.el-slider__button) {
    border-color: #10b981;
  }
}

.quick-scores {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.behavior-controls {
  :deep(.el-checkbox-group) {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  
  :deep(.el-checkbox) {
    color: white;
    
    .el-checkbox__label {
      color: white;
    }
  }
}

.test-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
  padding-top: 12px;
  border-top: 1px solid rgba(16, 185, 129, 0.3);
}

.test-info {
  margin-top: 16px;
  
  :deep(.el-alert) {
    background: rgba(59, 130, 246, 0.1);
    border: 1px solid rgba(59, 130, 246, 0.3);
    color: #93c5fd;
  }
  
  .info-content {
    p {
      margin: 4px 0;
      font-size: 12px;
      line-height: 1.4;
    }
  }
}

.current-status {
  h4 {
    color: #10b981;
    margin: 0 0 12px 0;
    font-size: 14px;
    font-weight: 600;
  }
}

.status-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

.status-text {
  color: #9ca3af;
  font-size: 12px;
  text-align: center;
}

// 响应式设计
@media (max-width: 768px) {
  .test-actions {
    flex-direction: column;
  }
  
  .quick-scores {
    justify-content: center;
  }
}
</style>
