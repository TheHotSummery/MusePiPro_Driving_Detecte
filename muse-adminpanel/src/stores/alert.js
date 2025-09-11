import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { alertApi } from '@/api/alert'

export const useAlertStore = defineStore('alert', () => {
  // 状态
  const realtimeAlerts = ref([])
  const alertHistory = ref([])
  const loading = ref(false)
  const error = ref(null)

  // 计算属性
  const alertCount = computed(() => ({
    active: realtimeAlerts.value.filter(a => a.status === 'ACTIVE').length,
    critical: realtimeAlerts.value.filter(a => a.severity === 'CRITICAL').length,
    acknowledged: realtimeAlerts.value.filter(a => a.acknowledged).length,
    total: realtimeAlerts.value.length
  }))

  const unacknowledgedAlerts = computed(() => 
    realtimeAlerts.value.filter(a => !a.acknowledged)
  )

  // 方法
  const fetchRealtimeAlerts = async () => {
    loading.value = true
    error.value = null
    try {
      const response = await alertApi.getRealtimeAlerts()
      realtimeAlerts.value = response.alerts || []
      return response
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const fetchAlertHistory = async (params = {}) => {
    try {
      const response = await alertApi.getAlertHistory(params)
      alertHistory.value = response.alerts || []
      return response
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const acknowledgeAlert = async (alertId) => {
    try {
      await alertApi.acknowledgeAlert(alertId)
      const alert = realtimeAlerts.value.find(a => a.alertId === alertId)
      if (alert) {
        alert.acknowledged = true
      }
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const handleAlert = async (alertId, data) => {
    try {
      await alertApi.handleAlert(alertId, data)
      const alert = realtimeAlerts.value.find(a => a.alertId === alertId)
      if (alert) {
        alert.status = 'HANDLED'
      }
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const addAlert = (alert) => {
    realtimeAlerts.value.unshift(alert)
  }

  const removeAlert = (alertId) => {
    const index = realtimeAlerts.value.findIndex(a => a.alertId === alertId)
    if (index > -1) {
      realtimeAlerts.value.splice(index, 1)
    }
  }

  const clearError = () => {
    error.value = null
  }

  return {
    // 状态
    realtimeAlerts,
    alertHistory,
    loading,
    error,
    
    // 计算属性
    alertCount,
    unacknowledgedAlerts,
    
    // 方法
    fetchRealtimeAlerts,
    fetchAlertHistory,
    acknowledgeAlert,
    handleAlert,
    addAlert,
    removeAlert,
    clearError
  }
})

