import axios from 'axios'

const API_BASE = '/api/v2/dashboard'

// 创建axios实例
const api = axios.create({
  baseURL: API_BASE,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    console.error('API请求失败:', error)
    return Promise.reject(error)
  }
)

// 缓存管理器
const cacheManager = {
  cache: new Map(),
  maxAge: 30000, // 30秒缓存
  maxSize: 100, // 最多缓存100条

  get(key) {
    const item = this.cache.get(key)
    if (!item) return null
    if (Date.now() - item.timestamp > this.maxAge) {
      this.cache.delete(key)
      return null
    }
    return item.data
  },

  set(key, data) {
    // 如果缓存已满，删除最旧的
    if (this.cache.size >= this.maxSize) {
      const firstKey = this.cache.keys().next().value
      this.cache.delete(firstKey)
    }
    this.cache.set(key, {
      data,
      timestamp: Date.now()
    })
  },

  generateKey(url, params) {
    return `${url}_${JSON.stringify(params)}`
  }
}

// 带缓存的请求
export const fetchWithCache = async (url, params = {}, useCache = true) => {
  const cacheKey = cacheManager.generateKey(url, params)
  
  if (useCache) {
    const cached = cacheManager.get(cacheKey)
    if (cached) {
      return cached
    }
  }

  try {
    const response = await api.get(url, { params })
    if (useCache) {
      cacheManager.set(cacheKey, response)
    }
    return response
  } catch (error) {
    console.error('API请求失败:', error)
    throw error
  }
}

// 实时数据接口
export const getRealtimeVehicles = () => api.get('/realtime/vehicles')
export const getRealtimeAlerts = () => api.get('/realtime/alerts')
export const getRealtimeSystem = () => api.get('/realtime/system')

// 统计数据接口
export const getEventStatistics = (params) => api.get('/statistics/events', { params })
export const getTimeframeStatistics = (params) => api.get('/statistics/timeframe', { params })
export const getRegionStatistics = (params) => api.get('/statistics/region', { params })
export const getDriverStatistics = (params) => api.get('/statistics/drivers', { params })

// 地图数据接口
export const getMapVehicles = (params) => fetchWithCache('/map/vehicles', params)
export const getMapTrack = (params) => api.get('/map/track', { params })
export const getMapHeatmap = (params) => fetchWithCache('/map/heatmap', params, false) // 热力图不使用缓存

// 图表数据接口
export const getTrendChart = (params) => fetchWithCache('/charts/trend', params)
export const getTimeDistributionChart = (params) => fetchWithCache('/charts/timeDistribution', params)
export const getBehaviorDistributionChart = (params) => fetchWithCache('/charts/behaviorDistribution', params)
export const getRegionDistributionChart = (params) => fetchWithCache('/charts/regionDistribution', params)

// 驾驶员接口
export const getDriverInfo = (params) => api.get('/driver/info', { params })
export const getDriverTrips = (params) => api.get('/driver/trips', { params })
export const getDriverSafety = (params) => api.get('/driver/safety', { params })

export default api













