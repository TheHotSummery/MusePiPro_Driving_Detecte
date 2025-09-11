import request from './index'

// 事件管理相关API
export const eventApi = {
  // 获取事件列表
  getEvents(params = {}) {
    console.log('=== API调用: 获取事件列表 ===')
    console.log('请求参数:', params)
    return request.get('/api/v1/platform/events', { params })
  },

  // 获取事件统计
  getEventStatistics(params = {}) {
    return request.get('/api/v1/platform/events/statistics', { params })
  },

  // 获取驾驶行为分析
  getDrivingBehaviorAnalysis(deviceId, params = {}) {
    return request.get(`/api/v1/platform/analysis/driving-behavior`, {
      params: { deviceId, ...params }
    })
  }
}

