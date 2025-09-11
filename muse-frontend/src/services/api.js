import axios from 'axios'
import apiConfig from '@/config/api'

// 创建axios实例
const api = axios.create({
  baseURL: apiConfig.API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    console.log('发送请求:', config.method?.toUpperCase(), config.url)
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    console.log('收到响应:', response.status, response.config.url)
    return response
  },
  (error) => {
    console.error('响应错误:', error.response?.status, error.message)
    return Promise.reject(error)
  }
)

// API方法
export const apiService = {
  // 获取系统状态
  async getSystemStatus() {
    try {
      const response = await api.get('/status')
      return response.data
    } catch (error) {
      console.error('获取系统状态失败:', error)
      throw error
    }
  },

  // 获取配置信息
  async getConfig() {
    try {
      const response = await api.get('/config')
      return response.data
    } catch (error) {
      console.error('获取配置失败:', error)
      throw error
    }
  },

  // 更新配置
  async updateConfig(config) {
    try {
      const response = await api.post('/config', config)
      return response.data
    } catch (error) {
      console.error('更新配置失败:', error)
      throw error
    }
  },

  // 获取权重配置
  async getWeights() {
    try {
      const response = await api.get('/weights')
      return response.data
    } catch (error) {
      console.error('获取权重配置失败:', error)
      throw error
    }
  },

  // 更新权重配置
  async updateWeights(weights) {
    try {
      const response = await api.post('/weights', weights)
      return response.data
    } catch (error) {
      console.error('更新权重配置失败:', error)
      throw error
    }
  },

  // 清空事件记录
  async clearEvents() {
    try {
      const response = await api.post('/clear_events')
      return response.data
    } catch (error) {
      console.error('清空事件记录失败:', error)
      throw error
    }
  },

  // 触发GPIO
  async triggerGPIO(gpio, duration = 1) {
    try {
      const response = await api.post('/trigger_gpio', { gpio, duration })
      return response.data
    } catch (error) {
      console.error('触发GPIO失败:', error)
      throw error
    }
  }
}

export default api
