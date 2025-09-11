<template>
  <div class="device-map" ref="mapContainer">
    <div class="map-controls">
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
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import amapManager from '@/utils/map'

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
let map = null
let markers = []
let infoWindow = null

// 高德地图API key配置
const amapKey = ref('')

// 从localStorage获取API key
const loadAmapKey = () => {
  const key = localStorage.getItem('amap_api_key')
  if (key) {
    amapKey.value = key
  } else {
    console.warn('高德地图API key未配置，请在地图配置中设置')
  }
}

// 初始化地图
const initMap = async () => {
  if (!amapKey.value) {
    console.error('高德地图API key未配置')
    return
  }

  try {
    // 加载高德地图API
    await amapManager.loadAmapScript(amapKey.value)

    // 创建地图实例
    map = amapManager.createMap(mapContainer.value, {
      center: [props.center.lng, props.center.lat],
      zoom: props.zoom
    })

    // 创建信息窗体
    infoWindow = amapManager.createInfoWindow()

    // 地图加载完成
    map.on('complete', () => {
      emit('map-ready', map)
      updateMarkers()
    })

  } catch (error) {
    console.error('地图初始化失败:', error)
  }
}


// 更新设备标记
const updateMarkers = () => {
  if (!map) return

  // 清除现有标记
  markers.forEach(marker => map.remove(marker))
  markers = []

  // 添加新标记
  props.devices.forEach(device => {
    if (device.location && device.location.lat && device.location.lng) {
      const marker = amapManager.createMarker({
        position: [device.location.lng, device.location.lat],
        title: device.deviceId,
        icon: amapManager.getDeviceIcon(device.status),
        anchor: 'center'
      })

      // 点击事件
      marker.on('click', () => {
        showDeviceInfo(device, marker)
        emit('device-click', device)
      })

      markers.push(marker)
      map.add(marker)
    }
  })

  // 如果有设备，自动调整视野
  if (props.devices.length > 0) {
    map.setFitView(markers)
  }
}


// 显示设备信息
const showDeviceInfo = (device, marker) => {
  const content = `
    <div class="device-info-window">
      <h4>${device.deviceId}</h4>
      <p><strong>驾驶员:</strong> ${device.username || '未知'}</p>
      <p><strong>状态:</strong> ${getStatusText(device.status)}</p>
      <p><strong>网络健康状态:</strong> ${device.healthScore || 0}%</p>
      <p><strong>最后心跳:</strong> ${formatTime(device.lastSeen)}</p>
    </div>
  `

  infoWindow.setContent(content)
  infoWindow.open(map, marker.getPosition())
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
  getMap: () => map
})
</script>

<style scoped lang="scss">
.device-map {
  width: 100%;
  height: 100%;
  position: relative;
  border-radius: 8px;
  overflow: hidden;
}

.map-controls {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 1000;
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
