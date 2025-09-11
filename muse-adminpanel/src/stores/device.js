import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { deviceApi } from '@/api/device'

export const useDeviceStore = defineStore('device', () => {
  // 状态
  const devices = ref([])
  const currentDevice = ref(null)
  const realtimeDevices = ref([])
  const loading = ref(false)
  const error = ref(null)

  // 计算属性
  const onlineDevices = computed(() => 
    devices.value.filter(d => d.status === 'ONLINE')
  )

  const offlineDevices = computed(() => 
    devices.value.filter(d => d.status === 'OFFLINE')
  )

  const deviceCount = computed(() => ({
    total: devices.value.length,
    online: onlineDevices.value.length,
    offline: offlineDevices.value.length
  }))

  // 方法
  const fetchDevices = async (params = {}) => {
    loading.value = true
    error.value = null
    console.log('=== 设备Store: 开始获取设备列表 ===')
    console.log('参数:', params)
    try {
      const response = await deviceApi.getDevices(params)
      console.log('设备API响应:', response)
      console.log('响应中的设备数组:', response.devices)
      console.log('设备数组长度:', response.devices?.length)
      
      if (response.devices && response.devices.length > 0) {
        devices.value = response.devices
        console.log('设置设备列表:', devices.value.length, '个设备')
        console.log('最终设备列表:', devices.value)
        console.log('第一个设备的用户名:', devices.value[0]?.username)
        console.log('第一个设备的设备ID:', devices.value[0]?.deviceId)
      } else {
        console.log('API返回的设备数组为空，使用空数组')
        devices.value = []
      }
      
      // 如果没有设备数据，添加一些模拟数据用于测试
      if (devices.value.length === 0) {
        console.log('⚠️ 警告：使用模拟数据！')
        devices.value = [
          {
            deviceId: 'MUSE_PI_PRO_001',
            deviceType: 'MUSE_PI_PRO',
            version: '1.0.0',
            status: 'ONLINE',
            healthScore: 85,
            lastSeen: new Date().toISOString(),
            username: '张三',
            phone: '13800138001',
            location: {
              lat: 33.553733,
              lng: 119.030953,
              speed: 60,
              direction: 45,
              altitude: 10,
              hdop: 1.2,
              satellites: 8
            }
          },
          {
            deviceId: 'MUSE_PI_PRO_002',
            deviceType: 'MUSE_PI_PRO',
            version: '1.0.0',
            status: 'ONLINE',
            healthScore: 92,
            lastSeen: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
            username: '李四',
            phone: '13800138002',
            location: {
              lat: 33.563733,
              lng: 119.040953,
              speed: 45,
              direction: 90,
              altitude: 15,
              hdop: 0.8,
              satellites: 10
            }
          },
          {
            deviceId: 'MUSE_PI_PRO_003',
            deviceType: 'MUSE_PI_PRO',
            version: '1.0.0',
            status: 'OFFLINE',
            healthScore: 78,
            lastSeen: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
            username: '王五',
            phone: '13800138003',
            location: {
              lat: 33.543733,
              lng: 119.020953,
              speed: 0,
              direction: 0,
              altitude: 8,
              hdop: 2.1,
              satellites: 6
            }
          }
        ]
      }
      
      return response
    } catch (err) {
      console.error('获取设备列表失败:', err)
      error.value = err.message
      // 如果API调用失败，使用模拟数据
      console.log('API调用失败，使用模拟数据')
      devices.value = [
        {
          deviceId: 'MUSE_PI_PRO_001',
          deviceType: 'MUSE_PI_PRO',
          version: '1.0.0',
          status: 'ONLINE',
          healthScore: 85,
          lastSeen: new Date().toISOString(),
          username: '张三',
          phone: '13800138001',
          location: {
            lat: 33.553733,
            lng: 119.030953,
            speed: 60,
            direction: 45,
            altitude: 10,
            hdop: 1.2,
            satellites: 8
          }
        },
        {
          deviceId: 'MUSE_PI_PRO_002',
          deviceType: 'MUSE_PI_PRO',
          version: '1.0.0',
          status: 'ONLINE',
          healthScore: 92,
          lastSeen: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
          username: '李四',
          phone: '13800138002',
          location: {
            lat: 33.563733,
            lng: 119.040953,
            speed: 45,
            direction: 90,
            altitude: 15,
            hdop: 0.8,
            satellites: 10
          }
        },
        {
          deviceId: 'MUSE_PI_PRO_003',
          deviceType: 'MUSE_PI_PRO',
          version: '1.0.0',
          status: 'OFFLINE',
          healthScore: 78,
          lastSeen: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
          username: '王五',
          phone: '13800138003',
          location: {
            lat: 33.543733,
            lng: 119.020953,
            speed: 0,
            direction: 0,
            altitude: 8,
            hdop: 2.1,
            satellites: 6
          }
        }
      ]
    } finally {
      loading.value = false
    }
  }

  const fetchDeviceDetail = async (deviceId) => {
    loading.value = true
    error.value = null
    try {
      const response = await deviceApi.getDeviceDetail(deviceId)
      currentDevice.value = response
      return response
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const fetchDeviceTrack = async (deviceId, params = {}) => {
    try {
      const response = await deviceApi.getDeviceTrack(deviceId, params)
      return response
    } catch (err) {
      error.value = err.message
      // 如果API调用失败，返回模拟轨迹数据
      const mockTrack = generateMockTrack(deviceId, params)
      return { track: mockTrack }
    }
  }

  // 生成模拟轨迹数据
  const generateMockTrack = (deviceId, params) => {
    const track = []
    const startTime = new Date(params.startTime || Date.now() - 24 * 60 * 60 * 1000)
    const endTime = new Date(params.endTime || Date.now())
    
    // 基础位置（江苏盐城附近）
    let baseLat = 33.553733
    let baseLng = 119.030953
    
    // 生成24小时的轨迹点，每小时一个点
    const hours = Math.ceil((endTime - startTime) / (1000 * 60 * 60))
    for (let i = 0; i < hours; i++) {
      const timestamp = new Date(startTime.getTime() + i * 60 * 60 * 1000)
      
      // 模拟车辆移动轨迹
      const lat = baseLat + (Math.random() - 0.5) * 0.01
      const lng = baseLng + (Math.random() - 0.5) * 0.01
      
      track.push({
        timestamp: timestamp.toISOString(),
        lat: lat,
        lng: lng,
        speed: Math.random() * 80 + 20, // 20-100 km/h
        direction: Math.random() * 360,
        altitude: Math.random() * 20 + 5,
        hdop: Math.random() * 2 + 0.5,
        satellites: Math.floor(Math.random() * 5) + 6
      })
    }
    
    return track
  }

  const fetchRealtimeStream = async () => {
    try {
      const response = await deviceApi.getRealtimeStream()
      realtimeDevices.value = response.devices || []
      return response
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const updateDeviceStatus = (deviceId, status) => {
    const device = devices.value.find(d => d.deviceId === deviceId)
    if (device) {
      device.status = status
      device.lastSeen = new Date().toISOString()
    }
  }

  const updateDeviceLocation = (deviceId, location) => {
    const device = devices.value.find(d => d.deviceId === deviceId)
    if (device) {
      device.location = location
    }
  }

  const updateDevice = async (deviceId, deviceData) => {
    loading.value = true
    error.value = null
    console.log('=== 设备Store: 开始更新设备信息 ===')
    console.log('设备ID:', deviceId)
    console.log('更新数据:', deviceData)
    
    try {
      const response = await deviceApi.updateDevice(deviceId, deviceData)
      console.log('更新设备API响应:', response)
      
      // 更新本地设备列表
      const device = devices.value.find(d => d.deviceId === deviceId)
      if (device) {
        Object.assign(device, deviceData)
      }
      
      return response
    } catch (err) {
      console.error('更新设备失败:', err)
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
    devices,
    currentDevice,
    realtimeDevices,
    loading,
    error,
    
    // 计算属性
    onlineDevices,
    offlineDevices,
    deviceCount,
    
    // 方法
    fetchDevices,
    fetchDeviceDetail,
    fetchDeviceTrack,
    fetchRealtimeStream,
    updateDeviceStatus,
    updateDeviceLocation,
    updateDevice,
    clearError
  }
})

