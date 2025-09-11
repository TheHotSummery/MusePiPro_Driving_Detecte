import request from './index'

// 告警管理相关API
export const alertApi = {
  // 获取实时告警
  getRealtimeAlerts() {
    return request.get('/api/v1/platform/alerts/realtime')
  },

  // 获取告警历史
  getAlertHistory(params = {}) {
    return request.get('/api/v1/platform/alerts/history', { params })
  },

  // 确认告警
  acknowledgeAlert(alertId) {
    return request.post(`/api/v1/platform/alerts/${alertId}/acknowledge`)
  },

  // 处理告警
  handleAlert(alertId, data) {
    return request.post(`/api/v1/platform/alerts/${alertId}/handle`, data)
  }
}

