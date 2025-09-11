import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { eventApi } from '@/api/event'

export const useEventStore = defineStore('event', () => {
  // 状态
  const events = ref([])
  const eventStatistics = ref(null)
  const drivingBehaviorAnalysis = ref(null)
  const loading = ref(false)
  const error = ref(null)

  // 计算属性
  const eventCount = computed(() => ({
    total: events.value.length,
    fatigue: events.value.filter(e => e.eventType === 'FATIGUE').length,
    distraction: events.value.filter(e => e.eventType === 'DISTRACTION').length,
    emergency: events.value.filter(e => e.eventType === 'EMERGENCY').length
  }))

  const severityCount = computed(() => ({
    low: events.value.filter(e => e.severity === 'LOW').length,
    medium: events.value.filter(e => e.severity === 'MEDIUM').length,
    high: events.value.filter(e => e.severity === 'HIGH').length,
    critical: events.value.filter(e => e.severity === 'CRITICAL').length
  }))

  // 方法
  const fetchEvents = async (params = {}) => {
    loading.value = true
    error.value = null
    console.log('=== 事件Store: 开始获取事件列表 ===')
    console.log('请求参数:', params)
    
    try {
      const response = await eventApi.getEvents(params)
      console.log('事件API响应:', response)
      console.log('响应中的事件数组:', response.events)
      console.log('事件数组长度:', response.events?.length)
      
      events.value = response.events || []
      console.log('设置事件列表:', events.value.length, '个事件')
      console.log('最终事件列表:', events.value)
      
      return response
    } catch (err) {
      console.error('获取事件列表失败:', err)
      error.value = err.message
      // 如果API调用失败，返回模拟事件数据
      console.log('API调用失败，使用模拟数据')
      const mockEvents = generateMockEvents(params)
      events.value = mockEvents
      return { events: mockEvents }
    } finally {
      loading.value = false
    }
  }

  // 生成模拟事件数据
  const generateMockEvents = (params) => {
    const events = []
    const deviceId = params.deviceId || 'MUSE_PI_PRO_001'
    const eventTypes = ['FATIGUE', 'DISTRACTION', 'EMERGENCY']
    const severities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
    
    // 生成最近24小时的事件
    for (let i = 0; i < 5; i++) {
      const timestamp = new Date(Date.now() - Math.random() * 24 * 60 * 60 * 1000)
      const eventType = eventTypes[Math.floor(Math.random() * eventTypes.length)]
      const severity = severities[Math.floor(Math.random() * severities.length)]
      
      // 随机位置（江苏盐城附近）
      const lat = 33.553733 + (Math.random() - 0.5) * 0.01
      const lng = 119.030953 + (Math.random() - 0.5) * 0.01
      
      events.push({
        eventId: `EVENT_${Date.now()}_${i}`,
        deviceId: deviceId,
        eventType: eventType,
        severity: severity,
        confidence: Math.random() * 0.4 + 0.6, // 0.6-1.0
        duration: Math.floor(Math.random() * 30) + 5, // 5-35秒
        timestamp: timestamp.toISOString(),
        location: {
          lat: lat,
          lng: lng,
          altitude: Math.random() * 20 + 5
        },
        behavior: getEventBehavior(eventType),
        context: getEventContext(eventType, severity)
      })
    }
    
    return events.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))
  }

  const getEventBehavior = (eventType) => {
    const behaviors = {
      FATIGUE: ['频繁眨眼', '头部下垂', '打哈欠', '注意力不集中'],
      DISTRACTION: ['看手机', '转头说话', '吃东西', '调整设备'],
      EMERGENCY: ['急刹车', '急转弯', '偏离车道', '碰撞预警']
    }
    const list = behaviors[eventType] || ['未知行为']
    return list[Math.floor(Math.random() * list.length)]
  }

  const getEventContext = (eventType, severity) => {
    const contexts = {
      FATIGUE: {
        LOW: '轻微疲劳，建议适当休息',
        MEDIUM: '中度疲劳，建议立即休息',
        HIGH: '严重疲劳，存在安全隐患',
        CRITICAL: '极度疲劳，立即停车休息'
      },
      DISTRACTION: {
        LOW: '轻微分心，注意驾驶安全',
        MEDIUM: '中度分心，影响驾驶安全',
        HIGH: '严重分心，存在碰撞风险',
        CRITICAL: '极度分心，立即停止驾驶'
      },
      EMERGENCY: {
        LOW: '轻微紧急情况，注意观察',
        MEDIUM: '中度紧急情况，谨慎驾驶',
        HIGH: '严重紧急情况，立即处理',
        CRITICAL: '极度紧急情况，立即停车'
      }
    }
    return contexts[eventType]?.[severity] || '事件详情'
  }

  const fetchEventStatistics = async (params = {}) => {
    try {
      const response = await eventApi.getEventStatistics(params)
      eventStatistics.value = response
      return response
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const fetchDrivingBehaviorAnalysis = async (deviceId, params = {}) => {
    try {
      const response = await eventApi.getDrivingBehaviorAnalysis(deviceId, params)
      drivingBehaviorAnalysis.value = response
      return response
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const addEvent = (event) => {
    events.value.unshift(event)
  }

  const clearError = () => {
    error.value = null
  }

  return {
    // 状态
    events,
    eventStatistics,
    drivingBehaviorAnalysis,
    loading,
    error,
    
    // 计算属性
    eventCount,
    severityCount,
    
    // 方法
    fetchEvents,
    fetchEventStatistics,
    fetchDrivingBehaviorAnalysis,
    addEvent,
    clearError
  }
})

