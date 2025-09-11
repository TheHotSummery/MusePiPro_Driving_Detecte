<template>
  <div class="detection-panel">
    <!-- 当前检测结果 -->
    <el-card class="detection-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <el-icon><View /></el-icon>
          <span>当前检测结果</span>
        </div>
      </template>
      
      <div class="detection-content">
        <div v-if="detections.length > 0" class="detection-list">
          <el-tag 
            v-for="detection in detections" 
            :key="detection.label"
            :type="getDetectionType(detection.label)"
            size="large"
            class="detection-tag"
          >
            {{ detection.label_cn }} ({{ (detection.confidence * 100).toFixed(1) }}%)
          </el-tag>
        </div>
        <div v-else class="no-detection">
          <el-icon><InfoFilled /></el-icon>
          <span>无检测结果</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { View, InfoFilled } from '@element-plus/icons-vue'
import { useDetectionStore } from '@/stores/detection'

const detectionStore = useDetectionStore()

// 计算属性
const detections = computed(() => detectionStore.detections)

// 方法
const getDetectionType = (label) => {
  const fatigueLabels = ['eyes_closed', 'yarning', 'eyes_closed_head_left', 'eyes_closed_head_right']
  const distractedLabels = ['head_down', 'seeing_left', 'seeing_right', 'head_up']
  
  if (fatigueLabels.includes(label)) {
    return 'danger'
  } else if (distractedLabels.includes(label)) {
    return 'warning'
  }
  return 'info'
}
</script>

<style scoped lang="scss">
.detection-panel {
  width: 100%;
}

.detection-card {
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

.detection-content {
  min-height: 60px;
  display: flex;
  align-items: center;
}

.detection-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detection-tag {
  font-size: 14px;
  padding: 8px 12px;
  border-radius: 20px;
}

.no-detection {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #9ca3af;
  font-style: italic;
}

// 响应式设计
@media (max-width: 768px) {
  .detection-list {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>