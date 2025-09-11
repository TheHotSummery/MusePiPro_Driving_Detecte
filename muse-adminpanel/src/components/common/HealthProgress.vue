<template>
  <div class="health-progress">
    <el-progress 
      :percentage="healthScore" 
      :color="healthColor"
      :stroke-width="8"
      :show-text="false"
    />
    <span class="health-text" :class="textClass">{{ healthScore }}%</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  score: {
    type: Number,
    required: true,
    validator: (value) => value >= 0 && value <= 100
  }
})

const healthScore = computed(() => Math.round(props.score))

const healthColor = computed(() => {
  if (props.score >= 80) return '#67C23A'
  if (props.score >= 60) return '#E6A23C'
  return '#F56C6C'
})

const textClass = computed(() => {
  if (props.score >= 80) return 'text-success'
  if (props.score >= 60) return 'text-warning'
  return 'text-danger'
})
</script>

<style scoped lang="scss">
.health-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 100px;
}

.health-text {
  font-size: 12px;
  font-weight: 500;
  min-width: 32px;
  text-align: right;

  &.text-success {
    color: #67C23A;
  }

  &.text-warning {
    color: #E6A23C;
  }

  &.text-danger {
    color: #F56C6C;
  }
}
</style>

