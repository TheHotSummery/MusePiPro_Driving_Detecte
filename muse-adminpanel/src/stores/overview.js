import { defineStore } from 'pinia'
import { ref } from 'vue'
import { overviewApi } from '@/api/overview'

export const useOverviewStore = defineStore('overview', () => {
  // 状态
  const overview = ref(null)
  const loading = ref(false)
  const error = ref(null)

  // 方法
  const fetchOverview = async () => {
    loading.value = true
    error.value = null
    try {
      const response = await overviewApi.getOverview()
      overview.value = response
      return response
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const clearError = () => {
    error.value = null
  }

  return {
    // 状态
    overview,
    loading,
    error,
    
    // 方法
    fetchOverview,
    clearError
  }
})

