import request from './index'

// 设备管理相关API
export const deviceApi = {
  // 获取设备列表
  getDevices(params = {}) {
    console.log('=== API调用: 获取设备列表 ===')
    console.log('请求参数:', params)
    return request.get('/api/v1/platform/devices', { params })
  },

  // 获取设备详情
  getDeviceDetail(deviceId) {
    return request.get(`/api/v1/platform/devices/${deviceId}`)
  },

  // 获取设备历史轨迹
  getDeviceTrack(deviceId, params = {}) {
    return request.get(`/api/v1/platform/devices/${deviceId}/track`, { params })
  },

  // 获取设备实时数据流
  getRealtimeStream() {
    return request.get('/api/v1/platform/realtime/stream')
  },

  // 更新设备信息（用于绑定/解绑用户）
  updateDevice(deviceId, deviceData) {
    console.log('=== API调用: 更新设备信息 ===')
    console.log('设备ID:', deviceId)
    console.log('更新数据:', deviceData)
    return request.put(`/api/v1/platform/devices/${deviceId}`, deviceData)
  }
}

