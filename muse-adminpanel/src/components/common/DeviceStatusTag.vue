<template>
  <el-tag :type="statusType" :effect="effect" size="small">
    <el-icon class="status-icon">
      <component :is="statusIcon" />
    </el-icon>
    {{ statusText }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: {
    type: String,
    required: true,
    validator: (value) => ['ONLINE', 'OFFLINE', 'LOST'].includes(value)
  },
  effect: {
    type: String,
    default: 'light',
    validator: (value) => ['dark', 'light', 'plain'].includes(value)
  }
})

const statusConfig = {
  ONLINE: { 
    type: 'success', 
    icon: 'CircleCheck', 
    text: '在线' 
  },
  OFFLINE: { 
    type: 'info', 
    icon: 'CircleClose', 
    text: '离线' 
  },
  LOST: { 
    type: 'danger', 
    icon: 'Warning', 
    text: '失联' 
  }
}

const statusType = computed(() => statusConfig[props.status]?.type || 'info')
const statusIcon = computed(() => statusConfig[props.status]?.icon || 'CircleClose')
const statusText = computed(() => statusConfig[props.status]?.text || '未知')
</script>

<style scoped lang="scss">
.status-icon {
  margin-right: 4px;
}
</style>

