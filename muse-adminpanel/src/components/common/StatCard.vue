<template>
  <div class="stat-card" :class="cardClass">
    <div class="stat-icon" :class="iconClass">
      <el-icon><component :is="icon" /></el-icon>
    </div>
    <div class="stat-content">
      <div class="stat-value">{{ displayValue }}{{ suffix }}</div>
      <div class="stat-label">{{ title }}</div>
      <div class="stat-trend" v-if="trend !== undefined">
        <el-icon :class="trendClass">
          <component :is="trend > 0 ? 'ArrowUp' : 'ArrowDown'" />
        </el-icon>
        <span>{{ Math.abs(trend) }}%</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  value: {
    type: [Number, String],
    required: true
  },
  icon: {
    type: String,
    required: true
  },
  color: {
    type: String,
    default: 'primary',
    validator: (value) => ['primary', 'success', 'warning', 'danger', 'info'].includes(value)
  },
  suffix: {
    type: String,
    default: ''
  },
  trend: {
    type: Number,
    default: undefined
  },
  pulse: {
    type: Boolean,
    default: false
  },
  highlight: {
    type: Boolean,
    default: false
  },
  urgent: {
    type: Boolean,
    default: false
  }
})

const displayValue = computed(() => {
  if (typeof props.value === 'number') {
    return props.value.toLocaleString()
  }
  return props.value
})

const cardClass = computed(() => `stat-card--${props.color}${props.pulse ? ' stat-card--pulse' : ''}${props.highlight ? ' stat-card--highlight' : ''}${props.urgent ? ' stat-card--urgent' : ''}`)
const iconClass = computed(() => `stat-icon--${props.color}${props.pulse ? ' stat-icon--pulse' : ''}${props.highlight ? ' stat-icon--highlight' : ''}${props.urgent ? ' stat-icon--urgent' : ''}`)
const trendClass = computed(() => props.trend > 0 ? 'trend-up' : 'trend-down')
</script>

<style scoped lang="scss">
.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.15);
    transform: translateY(-2px);
  }

  &--primary {
    border-left: 4px solid #409EFF;
  }

  &--success {
    border-left: 4px solid #67C23A;
  }

  &--warning {
    border-left: 4px solid #E6A23C;
  }

  &--danger {
    border-left: 4px solid #F56C6C;
  }

  &--info {
    border-left: 4px solid #909399;
  }
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 24px;

  &--primary {
    background: #ecf5ff;
    color: #409EFF;
  }

  &--success {
    background: #f0f9ff;
    color: #67C23A;
  }

  &--warning {
    background: #fdf6ec;
    color: #E6A23C;
  }

  &--danger {
    background: #fef0f0;
    color: #F56C6C;
  }

  &--info {
    background: #f4f4f5;
    color: #909399;
  }
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-trend {
  display: flex;
  align-items: center;
  font-size: 12px;
  gap: 4px;

  .trend-up {
    color: #67C23A;
  }

  .trend-down {
    color: #F56C6C;
  }
}

// Pulse动画
.stat-card--pulse {
  animation: pulse-glow 2s ease-in-out infinite;
}

.stat-icon--pulse {
  animation: pulse-scale 1.5s ease-in-out infinite;
}

@keyframes pulse-glow {
  0%, 100% {
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  }
  50% {
    box-shadow: 0 4px 20px 0 rgba(64, 158, 255, 0.3);
  }
}

@keyframes pulse-scale {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

// Highlight样式
.stat-card--highlight {
  border: 2px solid #409EFF;
  box-shadow: 0 4px 20px 0 rgba(64, 158, 255, 0.2);
  
  .stat-value {
    font-weight: 700;
    font-size: 32px;
  }
  
  .stat-label {
    font-weight: 600;
  }
}

.stat-icon--highlight {
  transform: scale(1.1);
}

// Urgent样式 - 紧急告警效果
.stat-card--urgent {
  animation: urgent-flash 1.5s ease-in-out infinite;
  
  .stat-value {
    animation: number-bounce 2s ease-in-out infinite;
    color: #F56C6C !important;
    font-weight: 800 !important;
  }
  
  .stat-icon {
    animation: urgent-bell 1s ease-in-out infinite;
  }
}

.stat-icon--urgent {
  color: #F56C6C !important;
}

@keyframes urgent-flash {
  0%, 100% {
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  }
  50% {
    box-shadow: 0 4px 20px 0 rgba(245, 108, 108, 0.6);
  }
}

@keyframes number-bounce {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

@keyframes urgent-bell {
  0%, 100% {
    transform: rotate(0deg);
  }
  25% {
    transform: rotate(-10deg);
  }
  75% {
    transform: rotate(10deg);
  }
}
</style>

