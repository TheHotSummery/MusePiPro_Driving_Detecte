<template>
  <div class="simple-map" ref="mapContainer">
    <div class="map-controls">
      <el-select 
        v-model="statusFilter" 
        placeholder="筛选设备状态" 
        size="small"
        clearable
        style="margin-right: 10px; width: 150px;"
        @change="updateMarkers"
      >
        <el-option label="全部设备" value="" />
        <el-option label="在线设备" value="ONLINE" />
        <el-option label="离线设备" value="OFFLINE" />
        <el-option label="失联设备" value="LOST" />
      </el-select>
      
      <el-button-group>
        <el-button size="small" @click="centerMap">
          <el-icon><Aim /></el-icon>
          居中
        </el-button>
        <el-button size="small" @click="toggleFullscreen">
          <el-icon><FullScreen /></el-icon>
          全屏
        </el-button>
      </el-button-group>
    </div>
    
    <!-- 地图加载失败提示 -->
    <div v-if="mapError" class="map-error">
      <el-alert
        title="地图加载失败"
        :description="mapError"
        type="error"
        show-icon
        :closable="false"
      />
      <el-button @click="retryLoad" style="margin-top: 10px;">重试</el-button>
    </div>
    
    <!-- 地图加载中提示 -->
    <div v-if="mapLoading" class="map-loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>地图加载中...</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick, toRaw } from 'vue'
import { Aim, FullScreen, Loading } from '@element-plus/icons-vue'

const props = defineProps({
  devices: {
    type: Array,
    default: () => []
  },
  center: {
    type: Object,
    default: () => ({ lat: 33.553733, lng: 119.030953 })
  },
  zoom: {
    type: Number,
    default: 10
  }
})

const emit = defineEmits(['device-click', 'map-ready'])

const mapContainer = ref(null)
const mapLoading = ref(false)
const mapError = ref('')
const statusFilter = ref('')
let map = null
let markers = []
let infoWindow = null
let lastDevicePositions = new Map() // 存储设备上次位置，用于检测位置变化
let positionChangeThreshold = 0.0001 // 位置变化阈值（约10米）

// 坐标校准算法
const PI = 3.141592653589793
const a = 6378245
const ee = 0.006693421622965943

function transformlat(lng, lat) {
  let ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng))
  ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(lat * PI) + 40.0 * Math.sin(lat / 3.0 * PI)) * 2.0 / 3.0
  ret += (160.0 * Math.sin(lat / 12.0 * PI) + 320 * Math.sin(lat * PI / 30.0)) * 2.0 / 3.0
  return ret
}

function transformlng(lng, lat) {
  let ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng))
  ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(lng * PI) + 40.0 * Math.sin(lng / 3.0 * PI)) * 2.0 / 3.0
  ret += (150.0 * Math.sin(lng / 12.0 * PI) + 300.0 * Math.sin(lng / 30.0 * PI)) * 2.0 / 3.0
  return ret
}

function out_of_china(lng, lat) {
  return (lng < 72.004 || lng > 137.8347) || ((lat < 0.8293 || lat > 55.8271) || false)
}

function wgs84togcj02(lng, lat) {
  if (out_of_china(lng, lat)) {
    return [lng, lat]
  } else {
    let dlat = transformlat(lng - 105.0, lat - 35.0)
    let dlng = transformlng(lng - 105.0, lat - 35.0)
    const radlat = lat / 180.0 * PI
    let magic = Math.sin(radlat)
    magic = 1 - ee * magic * magic
    const sqrtmagic = Math.sqrt(magic)
    dlat = (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * PI)
    dlng = (dlng * 180.0) / (a / sqrtmagic * Math.cos(radlat) * PI)
    const mglat = lat + dlat
    const mglng = lng + dlng
    return [mglng, mglat]
  }
}

// 高德地图API key配置
const amapKey = ref('')

// 从环境变量或localStorage获取API key
const loadAmapKey = () => {
  // 优先从环境变量获取
  const envKey = import.meta.env.VITE_AMAP_API_KEY
  const localKey = localStorage.getItem('amap_api_key')
  
  if (envKey && envKey !== 'your_amap_api_key_here') {
    amapKey.value = envKey
  } else if (localKey) {
    amapKey.value = localKey
  } else {
    // 使用测试API key（仅用于开发测试）
    amapKey.value = 'test_key_for_development'
    mapError.value = '使用测试API key，地图功能可能受限。请配置正式的高德地图API key'
  }
}

// 初始化地图
const initMap = async () => {
  console.log('开始初始化地图...')
  console.log('API Key:', amapKey.value ? '已配置' : '未配置')
  console.log('地图容器:', mapContainer.value)
  
  if (!amapKey.value) {
    mapError.value = '高德地图API key未配置'
    return
  }

  if (!mapContainer.value) {
    mapError.value = '地图容器未找到'
    return
  }

  mapLoading.value = true
  mapError.value = ''

  try {
    // 动态加载高德地图API
    if (!window.AMap) {
      console.log('加载高德地图API...')
      await loadAmapScript()
    }

    // 检查AMap是否加载成功
    if (!window.AMap) {
      throw new Error('高德地图API加载失败')
    }

    console.log('AMap已加载，开始创建地图实例...')
    console.log('地图容器尺寸:', mapContainer.value.offsetWidth, 'x', mapContainer.value.offsetHeight)

    // 创建地图实例
    map = new window.AMap.Map(mapContainer.value, {
      center: [props.center.lng, props.center.lat],
      zoom: props.zoom,
      mapStyle: 'amap://styles/normal'
    })

    console.log('地图实例创建成功')

    // 创建信息窗体
    infoWindow = new window.AMap.InfoWindow({
      offset: new window.AMap.Pixel(0, -30)
    })

    // 地图加载完成
    map.on('complete', () => {
      console.log('地图加载完成')
      
      // 添加地图点击事件，点击空白处关闭信息窗口
      map.on('click', () => {
        if (infoWindow) {
          infoWindow.close()
        }
      })
      
      emit('map-ready', map)
      updateMarkers()
      mapLoading.value = false
    })

  } catch (error) {
    console.error('地图初始化失败:', error)
    mapError.value = error.message || '地图初始化失败'
    mapLoading.value = false
  }
}

// 动态加载高德地图脚本
const loadAmapScript = () => {
  return new Promise((resolve, reject) => {
    if (window.AMap) {
      resolve()
      return
    }

    const version = localStorage.getItem('amap_version') || '1.4.15'
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=${version}&key=${amapKey.value}`
    script.onload = () => {
      // 等待AMap对象完全初始化
      const checkAMap = () => {
        if (window.AMap && window.AMap.Map) {
          resolve()
        } else {
          setTimeout(checkAMap, 100)
        }
      }
      checkAMap()
    }
    script.onerror = () => {
      reject(new Error('高德地图API加载失败'))
    }
    document.head.appendChild(script)
  })
}

// 检测设备位置变化
const detectPositionChanges = (devices) => {
  const changedDevices = []
  const onlineDevices = []
  
  devices.forEach(device => {
    const deviceData = toRaw(device) || device
    
    if (deviceData.location && deviceData.location.lat && deviceData.location.lng) {
      const deviceId = deviceData.deviceId
      const currentPos = {
        lat: deviceData.location.lat,
        lng: deviceData.location.lng
      }
      
      // 检查是否为在线设备
      if (deviceData.status === 'ONLINE') {
        onlineDevices.push(deviceData)
      }
      
      // 检查位置是否发生变化
      if (lastDevicePositions.has(deviceId)) {
        const lastPos = lastDevicePositions.get(deviceId)
        const latDiff = Math.abs(currentPos.lat - lastPos.lat)
        const lngDiff = Math.abs(currentPos.lng - lastPos.lng)
        
        if (latDiff > positionChangeThreshold || lngDiff > positionChangeThreshold) {
          console.log(`设备 ${deviceId} 位置发生变化:`, {
            from: lastPos,
            to: currentPos,
            latDiff,
            lngDiff
          })
          changedDevices.push(deviceData)
        }
      } else {
        // 新设备，记录为位置变化
        console.log(`新设备 ${deviceId} 首次出现`)
        changedDevices.push(deviceData)
      }
      
      // 更新位置记录
      lastDevicePositions.set(deviceId, currentPos)
    }
  })
  
  return { changedDevices, onlineDevices }
}

// 自动聚焦到位置变化的在线设备
const focusOnChangedDevices = (changedDevices) => {
  if (changedDevices.length === 0) return
  
  // 筛选出在线且位置变化的设备
  const onlineChangedDevices = changedDevices.filter(device => device.status === 'ONLINE')
  
  if (onlineChangedDevices.length > 0) {
    console.log('自动聚焦到位置变化的在线设备:', onlineChangedDevices.map(d => d.deviceId))
    
    // 找到对应的标记
    const changedMarkers = markers.filter(marker => {
      const deviceId = marker.getTitle().split(' - ')[0]
      return onlineChangedDevices.some(device => device.deviceId === deviceId)
    })
    
    if (changedMarkers.length > 0) {
      // 聚焦到变化的设备
      map.setFitView(changedMarkers, false, [50, 50, 50, 50])
      
      // 高亮显示变化的设备（可选）
      changedMarkers.forEach(marker => {
        marker.setAnimation('AMAP_ANIMATION_BOUNCE')
        setTimeout(() => {
          marker.setAnimation(null)
        }, 2000)
      })
    }
  }
}

// 更新设备标记
const updateMarkers = () => {
  if (!map) return

  console.log('=== 更新地图标记 ===')
  console.log('所有设备:', props.devices)
  console.log('当前筛选状态:', statusFilter.value)

  // 检测位置变化
  const { changedDevices } = detectPositionChanges(props.devices)

  // 清除现有标记
  markers.forEach(marker => map.remove(marker))
  markers = []

  // 筛选设备
  const filteredDevices = props.devices.filter(device => {
    // 安全访问Proxy对象
    const deviceData = toRaw(device) || device
    console.log(`检查设备 ${deviceData.deviceId}:`, {
      status: deviceData.status,
      hasLocation: !!(deviceData.location && deviceData.location.lat && deviceData.location.lng),
      location: deviceData.location
    })
    
    // 筛选状态
    if (statusFilter.value && deviceData.status !== statusFilter.value) {
      console.log(`设备 ${deviceData.deviceId} 状态不匹配，跳过`)
      return false
    }
    // 只显示有位置信息的设备
    const hasValidLocation = deviceData.location && deviceData.location.lat && deviceData.location.lng
    if (!hasValidLocation) {
      console.log(`设备 ${deviceData.deviceId} 没有有效位置信息，跳过`)
    }
    return hasValidLocation
  })

  console.log('筛选后的设备:', filteredDevices)

  // 添加新标记
  filteredDevices.forEach((device, index) => {
    // 安全访问Proxy对象
    const deviceData = toRaw(device) || device
    
    if (deviceData.location && deviceData.location.lat && deviceData.location.lng) {
      // 坐标校准：WGS-84 转 GCJ-02
      const [calibratedLng, calibratedLat] = wgs84togcj02(deviceData.location.lng, deviceData.location.lat)
      
      // 检查是否为位置变化的设备，使用特殊图标
      const isChangedDevice = changedDevices.some(changed => changed.deviceId === deviceData.deviceId)
      
      const marker = new window.AMap.Marker({
        position: [calibratedLng, calibratedLat],
        title: `${deviceData.deviceId} - ${deviceData.username || '未知用户'}`,
        icon: getDeviceIcon(deviceData.status, index, isChangedDevice),
        anchor: 'center',
        label: {
          content: `${deviceData.deviceId}<br/>${deviceData.username || '未知用户'}<br/>${getStatusText(deviceData.status)}${isChangedDevice ? '<br/>📍 位置更新' : ''}`,
          direction: 'bottom',
          offset: new window.AMap.Pixel(0, 10),
          style: {
            backgroundColor: isChangedDevice ? '#FF6B6B' : getStatusColor(deviceData.status),
            color: '#fff',
            fontSize: '12px',
            padding: '4px 8px',
            borderRadius: '4px',
            border: '1px solid #fff'
          }
        }
      })

      // 点击事件
      marker.on('click', () => {
        showDeviceInfo(deviceData, marker)
        emit('device-click', deviceData)
      })

      markers.push(marker)
      map.add(marker)
    }
  })

  // 自动聚焦到位置变化的在线设备
  focusOnChangedDevices(changedDevices)

  // 如果没有设备变化，保持当前视野或调整到所有设备
  if (changedDevices.length === 0 && filteredDevices.length > 0) {
    map.setFitView(markers)
  }
}

// 获取设备图标 - 不同颜色标记
const getDeviceIcon = (status, index, isChanged = false) => {
  const colors = ['#67C23A', '#E6A23C', '#F56C6C', '#409EFF', '#909399', '#9C27B0', '#FF9800', '#4CAF50']
  let color = colors[index % colors.length]
  
  // 如果设备位置发生变化，使用特殊颜色
  if (isChanged) {
    color = '#FF6B6B' // 红色高亮
  }
  
  // 创建自定义图标
  const icon = new window.AMap.Icon({
    size: new window.AMap.Size(32, 32),
    image: `data:image/svg+xml;base64,${btoa(`
      <svg width="32" height="32" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
        <circle cx="16" cy="16" r="12" fill="${color}" stroke="#fff" stroke-width="2"/>
        <circle cx="16" cy="16" r="4" fill="#fff"/>
        <text x="16" y="20" text-anchor="middle" fill="#fff" font-size="8" font-weight="bold">${index + 1}</text>
        ${isChanged ? '<circle cx="16" cy="16" r="14" fill="none" stroke="#FF6B6B" stroke-width="3" opacity="0.8"/>' : ''}
      </svg>
    `)}`,
    imageSize: new window.AMap.Size(32, 32)
  })
  
  return icon
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    ONLINE: '在线',
    OFFLINE: '离线',
    LOST: '失联'
  }
  return statusMap[status] || '未知'
}

// 获取状态颜色
const getStatusColor = (status) => {
  const colorMap = {
    ONLINE: '#67C23A',
    OFFLINE: '#909399',
    LOST: '#F56C6C'
  }
  return colorMap[status] || '#909399'
}

// 显示设备信息
const showDeviceInfo = (device, marker) => {
  // 计算校准后的坐标
  const [calibratedLng, calibratedLat] = wgs84togcj02(device.location.lng, device.location.lat)
  
  // 根据设备状态显示不同的信息
  const isOnline = device.status === 'ONLINE'
  
  const content = `
    <div class="device-info-window">
      <h4>${device.deviceId}</h4>
      <p><strong>驾驶员:</strong> ${device.username || '未知'}</p>
      <p><strong>状态:</strong> <span style="color: ${getStatusColor(device.status)}">${getStatusText(device.status)}</span></p>
      <p><strong>网络健康状态:</strong> ${device.healthScore || 0}%</p>
      <p><strong>最后心跳:</strong> ${formatTime(device.lastSeen)}</p>
      
      ${isOnline ? `
        <hr style="margin: 8px 0;">
        <p><strong>实时信息:</strong></p>
        <p><strong>速度:</strong> ${device.location.speed || 0} km/h</p>
        <p><strong>方向:</strong> ${device.location.direction || 0}°</p>
        <p><strong>卫星数:</strong> ${device.location.satellites || 0}</p>
        <p><strong>海拔:</strong> ${device.location.altitude || 0} m</p>
        <p><strong>精度:</strong> ${device.location.hdop || 0}</p>
      ` : `
        <hr style="margin: 8px 0;">
        <p style="color: #909399; font-style: italic;">设备离线，实时信息不可用</p>
      `}
      
      <hr style="margin: 8px 0;">
      <p><strong>原始坐标 (WGS-84):</strong></p>
      <p>经度: ${device.location.lng.toFixed(6)}</p>
      <p>纬度: ${device.location.lat.toFixed(6)}</p>
      <p><strong>校准坐标 (GCJ-02):</strong></p>
      <p>经度: ${calibratedLng.toFixed(6)}</p>
      <p>纬度: ${calibratedLat.toFixed(6)}</p>
    </div>
  `

  infoWindow.setContent(content)
  infoWindow.open(map, marker.getPosition())
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '未知'
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  return date.toLocaleDateString()
}

// 居中地图
const centerMap = () => {
  if (map && props.devices.length > 0) {
    map.setFitView(markers)
  } else if (map) {
    map.setCenter([props.center.lng, props.center.lat])
  }
}

// 全屏切换
const toggleFullscreen = () => {
  if (mapContainer.value.requestFullscreen) {
    mapContainer.value.requestFullscreen()
  }
}

// 重试加载
const retryLoad = () => {
  mapError.value = ''
  if (map) {
    map.destroy()
    map = null
  }
  initMap()
}

// 强制刷新地图
const forceRefresh = () => {
  console.log('强制刷新地图...')
  if (map) {
    map.destroy()
    map = null
  }
  markers = []
  setTimeout(() => {
    initMap()
  }, 100)
}

// 监听设备变化
watch(() => props.devices, updateMarkers, { deep: true })

// 监听API key变化
watch(() => amapKey.value, (newKey) => {
  if (newKey && !map) {
    initMap()
  }
})

onMounted(() => {
  loadAmapKey()
  nextTick(() => {
    if (amapKey.value) {
      initMap()
    }
  })
})

onUnmounted(() => {
  if (map) {
    map.destroy()
  }
})

// 暴露方法给父组件
defineExpose({
  centerMap,
  updateMarkers,
  forceRefresh,
  getMap: () => map
})
</script>

<style scoped lang="scss">
.simple-map {
  width: 100%;
  height: 100%;
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
  min-height: 400px;
  border: 2px solid #e4e7ed;
  
}

.map-controls {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.9);
  padding: 8px 12px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(4px);
}

.map-error,
.map-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 1000;
  text-align: center;
}

.map-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: #409EFF;
  
  .el-icon {
    font-size: 24px;
  }
}

:deep(.device-info-window) {
  h4 {
    margin: 0 0 8px 0;
    color: #303133;
    font-size: 14px;
  }

  p {
    margin: 4px 0;
    font-size: 12px;
    color: #606266;
  }
}
</style>
