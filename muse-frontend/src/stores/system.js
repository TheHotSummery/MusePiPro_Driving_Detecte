import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSystemStore = defineStore('system', () => {
  // 状态
  const isConnected = ref(false)
  const connectionStatus = ref('未连接')
  const lastUpdateTime = ref(null)
  const errorMessage = ref('')

  // 方法
  const setConnected = (connected) => {
    isConnected.value = connected
    connectionStatus.value = connected ? '已连接' : '连接断开'
    if (connected) {
      errorMessage.value = ''
    }
  }

  const setConnectionStatus = (status) => {
    connectionStatus.value = status
  }

  const setError = (error) => {
    errorMessage.value = error
    isConnected.value = false
    connectionStatus.value = '连接错误'
  }

  const updateLastUpdateTime = () => {
    lastUpdateTime.value = new Date()
  }

  const clearError = () => {
    errorMessage.value = ''
  }

  return {
    // 状态
    isConnected,
    connectionStatus,
    lastUpdateTime,
    errorMessage,
    // 方法
    setConnected,
    setConnectionStatus,
    setError,
    updateLastUpdateTime,
    clearError
  }
})




