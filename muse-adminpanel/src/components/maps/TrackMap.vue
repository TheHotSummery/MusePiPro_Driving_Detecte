<template>
  <div class="track-map" ref="trackMapContainer">
    <div class="map-controls">
      <el-button-group>
        <el-button size="small" @click="playTrack" :disabled="!trackData.length">
          <el-icon><VideoPlay /></el-icon>
          {{ isPlaying ? '暂停' : '播放' }}
        </el-button>
        <el-button size="small" @click="resetTrack">
          <el-icon><Refresh /></el-icon>
          重置
        </el-button>
        <el-button size="small" @click="toggleFullscreen">
          <el-icon><FullScreen /></el-icon>
          全屏
        </el-button>
      </el-button-group>
    </div>
    
    <div class="time-controls" v-if="trackData.length">
      <el-slider
        v-model="currentTimeIndex"
        :min="0"
        :max="trackData.length - 1"
        :step="1"
        @change="onTimeChange"
        :format-tooltip="formatTimeTooltip"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'

const props = defineProps({
  deviceId: {
    type: String,
    required: true
  },
  trackData: {
    type: Array,
    default: () => []
  },
  events: {
    type: Array,
    default: () => []
  },
  center: {
    type: Object,
    default: () => ({ lat: 33.553733, lng: 119.030953 })
  },
  zoom: {
    type: Number,
    default: 12
  }
})

const emit = defineEmits(['event-click', 'map-ready'])

const trackMapContainer = ref(null)
let map = null
let trackPolyline = null
let trackMarkers = []
let eventMarkers = []
let infoWindow = null
let isPlaying = ref(false)
let currentTimeIndex = ref(0)
let playInterval = null

// 高德地图API key配置
const amapKey = ref('')
const mapLoading = ref(false)
const mapError = ref('')

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
  if (!amapKey.value) {
    console.error('高德地图API key未配置')
    return
  }

  try {
    // 加载高德地图API
    await amapManager.loadAmapScript(amapKey.value)

    // 创建地图实例
    map = amapManager.createMap(trackMapContainer.value, {
      center: [props.center.lng, props.center.lat],
      zoom: props.zoom
    })

    // 创建信息窗体
    infoWindow = amapManager.createInfoWindow()

    // 地图加载完成
    map.on('complete', () => {
      emit('map-ready', map)
      updateTrack()
    })

  } catch (error) {
    console.error('地图初始化失败:', error)
  }
}


// 更新轨迹
const updateTrack = () => {
  if (!map || !props.trackData.length) return

  // 清除现有轨迹和标记
  clearTrack()

  // 创建轨迹线
  const path = props.trackData.map(point => [point.lng, point.lat])
  
  trackPolyline = amapManager.createPolyline({
    path: path,
    strokeColor: '#409EFF',
    strokeWeight: 4,
    strokeOpacity: 0.8
  })

  map.add(trackPolyline)

  // 添加轨迹点标记
  props.trackData.forEach((point, index) => {
    const marker = amapManager.createMarker({
      position: [point.lng, point.lat],
      title: `轨迹点 ${index + 1}`,
      icon: amapManager.getTrackPointIcon(index, props.trackData.length),
      anchor: 'center'
    })

    marker.on('click', () => {
      showTrackPointInfo(point, marker)
    })

    trackMarkers.push(marker)
    map.add(marker)
  })

  // 添加事件标记
  updateEventMarkers()

  // 自动调整视野
  if (props.trackData.length > 0) {
    map.setFitView([trackPolyline, ...trackMarkers, ...eventMarkers])
  }
}

// 更新事件标记
const updateEventMarkers = () => {
  // 清除现有事件标记
  eventMarkers.forEach(marker => map.remove(marker))
  eventMarkers = []

  // 添加事件标记
  props.events.forEach(event => {
    if (event.location && event.location.lat && event.location.lng) {
      const marker = amapManager.createMarker({
        position: [event.location.lng, event.location.lat],
        title: event.eventType,
        icon: amapManager.getEventIcon(event.eventType, event.severity),
        anchor: 'center'
      })

      marker.on('click', () => {
        showEventInfo(event, marker)
        emit('event-click', event)
      })

      eventMarkers.push(marker)
      map.add(marker)
    }
  })
}


// 显示轨迹点信息
const showTrackPointInfo = (point, marker) => {
  const content = `
    <div class="track-point-info">
      <h4>轨迹点信息</h4>
      <p><strong>时间:</strong> ${formatTime(point.timestamp)}</p>
      <p><strong>位置:</strong> ${point.lat.toFixed(6)}, ${point.lng.toFixed(6)}</p>
      <p><strong>速度:</strong> ${point.speed || 0} km/h</p>
      <p><strong>方向:</strong> ${point.direction || 0}°</p>
    </div>
  `

  infoWindow.setContent(content)
  infoWindow.open(map, marker.getPosition())
}

// 显示事件信息
const showEventInfo = (event, marker) => {
  const content = `
    <div class="event-info">
      <h4>${getEventTypeText(event.eventType)}</h4>
      <p><strong>时间:</strong> ${formatTime(event.timestamp)}</p>
      <p><strong>严重程度:</strong> ${getSeverityText(event.severity)}</p>
      <p><strong>置信度:</strong> ${(event.confidence * 100).toFixed(1)}%</p>
      <p><strong>持续时间:</strong> ${event.duration || 0}秒</p>
      <p><strong>描述:</strong> ${event.context || '无'}</p>
    </div>
  `

  infoWindow.setContent(content)
  infoWindow.open(map, marker.getPosition())
}

// 获取事件类型文本
const getEventTypeText = (eventType) => {
  const typeMap = {
    FATIGUE: '疲劳检测',
    DISTRACTION: '分心行为',
    EMERGENCY: '紧急事件'
  }
  return typeMap[eventType] || '未知事件'
}

// 获取严重程度文本
const getSeverityText = (severity) => {
  const severityMap = {
    LOW: '轻微',
    MEDIUM: '中等',
    HIGH: '严重',
    CRITICAL: '危急'
  }
  return severityMap[severity] || '未知'
}

// 格式化时间
const formatTime = (time) => {
  return amapManager.formatTime(time)
}

// 格式化时间滑块提示
const formatTimeTooltip = (value) => {
  if (props.trackData[value]) {
    return formatTime(props.trackData[value].timestamp)
  }
  return ''
}

// 播放轨迹
const playTrack = () => {
  if (isPlaying.value) {
    pauseTrack()
  } else {
    startTrack()
  }
}

// 开始播放
const startTrack = () => {
  if (!props.trackData.length) return

  isPlaying.value = true
  currentTimeIndex.value = 0

  playInterval = setInterval(() => {
    if (currentTimeIndex.value < props.trackData.length - 1) {
      currentTimeIndex.value++
      onTimeChange(currentTimeIndex.value)
    } else {
      pauseTrack()
    }
  }, 1000) // 每秒播放一个点
}

// 暂停播放
const pauseTrack = () => {
  isPlaying.value = false
  if (playInterval) {
    clearInterval(playInterval)
    playInterval = null
  }
}

// 重置轨迹
const resetTrack = () => {
  pauseTrack()
  currentTimeIndex.value = 0
  updateTrack()
}

// 时间变化处理
const onTimeChange = (index) => {
  if (props.trackData[index]) {
    const point = props.trackData[index]
    map.setCenter([point.lng, point.lat])
    
    // 高亮当前点
    trackMarkers.forEach((marker, i) => {
      if (i === index) {
        marker.setIcon('https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png')
      } else {
        marker.setIcon(amapManager.getTrackPointIcon(i, props.trackData.length))
      }
    })
  }
}

// 清除轨迹
const clearTrack = () => {
  if (trackPolyline) {
    map.remove(trackPolyline)
    trackPolyline = null
  }

  trackMarkers.forEach(marker => map.remove(marker))
  trackMarkers = []

  eventMarkers.forEach(marker => map.remove(marker))
  eventMarkers = []
}

// 全屏切换
const toggleFullscreen = () => {
  if (trackMapContainer.value.requestFullscreen) {
    trackMapContainer.value.requestFullscreen()
  }
}

// 监听轨迹数据变化
watch(() => props.trackData, updateTrack, { deep: true })

// 监听事件数据变化
watch(() => props.events, updateEventMarkers, { deep: true })

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
  pauseTrack()
  if (map) {
    map.destroy()
  }
})

// 暴露方法给父组件
defineExpose({
  playTrack,
  pauseTrack,
  resetTrack,
  updateTrack,
  getMap: () => map
})
</script>

<style scoped lang="scss">
.track-map {
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

.time-controls {
  position: absolute;
  bottom: 20px;
  left: 20px;
  right: 20px;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.9);
  padding: 10px;
  border-radius: 4px;
}

:deep(.track-point-info),
:deep(.event-info) {
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
