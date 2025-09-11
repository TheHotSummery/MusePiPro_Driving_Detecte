import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useDetectionStore = defineStore('detection', () => {
  // 状态
  const detections = ref([])
  const isFatigue = ref(false)
  const isDistracted = ref(false)
  const distractedCount = ref(0)
  const progress = ref(0)
  const level = ref('Normal')
  const events = ref([])
  const fps = ref(0)
  const cpuUsage = ref(0)
  const status = ref('等待检测数据...')

  // 计算属性
  const levelText = computed(() => {
    if (progress.value >= 95) return '三级警报'
    if (progress.value >= 75) return '二级警报'
    if (progress.value >= 50) return '一级警报'
    return '正常'
  })

  const levelColor = computed(() => {
    if (progress.value >= 95) return 'danger'
    if (progress.value >= 75) return 'warning'
    if (progress.value >= 50) return 'success'
    return 'success'
  })

  const statusColor = computed(() => {
    if (isFatigue.value) return 'danger'
    if (isDistracted.value) return 'warning'
    return 'success'
  })

  // 方法
  const updateDetection = (data) => {
    detections.value = data.detections || []
    isFatigue.value = data.is_fatigue || false
    isDistracted.value = data.is_distracted || false
    distractedCount.value = data.distracted_count || 0
    progress.value = data.progress || 0
    level.value = data.level || 'Normal'
    events.value = data.events || []
    fps.value = data.fps || 0
    cpuUsage.value = data.cpu_usage || 0

    // 更新状态文本
    if (isFatigue.value) {
      status.value = '检测到疲劳驾驶！'
    } else if (isDistracted.value) {
      status.value = '检测到分心驾驶！'
    } else {
      status.value = '安全驾驶'
    }
  }

  const updateStatus = (message) => {
    status.value = message
  }

  const clearEvents = () => {
    events.value = []
    distractedCount.value = 0
  }

  const addEvent = (event) => {
    events.value.unshift(event)
    // 限制事件数量，避免内存溢出
    if (events.value.length > 1000) {
      events.value = events.value.slice(0, 1000)
    }
  }

  return {
    // 状态
    detections,
    isFatigue,
    isDistracted,
    distractedCount,
    progress,
    level,
    events,
    fps,
    cpuUsage,
    status,
    // 计算属性
    levelText,
    levelColor,
    statusColor,
    // 方法
    updateDetection,
    updateStatus,
    clearEvents,
    addEvent
  }
})




