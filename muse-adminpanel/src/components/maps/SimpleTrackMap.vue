<template>
  <div class="simple-track-map" ref="mapContainer">
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
        <el-button size="small" @click="centerMap">
          <el-icon><Aim /></el-icon>
          居中
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
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { VideoPlay, Refresh, Aim, Loading } from '@element-plus/icons-vue'

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

const mapContainer = ref(null)
const mapLoading = ref(false)
const mapError = ref('')
const isPlaying = ref(false)
const currentTimeIndex = ref(0)

let map = null
let trackPolyline = null
let trackMarkers = []
let eventMarkers = []
let currentMarker = null
let infoWindow = null
let playInterval = null

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
  console.log('开始初始化SimpleTrackMap...')
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

    console.log('AMap已加载，开始创建SimpleTrackMap实例...')
    console.log('地图容器尺寸:', mapContainer.value.offsetWidth, 'x', mapContainer.value.offsetHeight)

    // 创建地图实例
    map = new window.AMap.Map(mapContainer.value, {
      center: [props.center.lng, props.center.lat],
      zoom: props.zoom,
      mapStyle: 'amap://styles/normal'
    })

    console.log('SimpleTrackMap实例创建成功')

    // 创建信息窗体
    infoWindow = new window.AMap.InfoWindow({
      offset: new window.AMap.Pixel(0, -30)
    })

    // 地图加载完成
    map.on('complete', () => {
      console.log('SimpleTrackMap地图加载完成')
      emit('map-ready', map)
      updateTrackDisplay()
      updateEventMarkers()
      mapLoading.value = false
    })

  } catch (error) {
    console.error('SimpleTrackMap地图初始化失败:', error)
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

// 更新轨迹显示
const updateTrackDisplay = () => {
  if (!map || !props.trackData.length) return

  // 清除现有轨迹
  if (trackPolyline) {
    map.remove(trackPolyline)
  }
  trackMarkers.forEach(marker => map.remove(marker))
  trackMarkers = []

  // 创建轨迹点数组
  const path = props.trackData.map(point => [point.lng, point.lat])
  
  // 创建轨迹线
  trackPolyline = new window.AMap.Polyline({
    path: path,
    strokeColor: '#409EFF',
    strokeWeight: 3,
    strokeOpacity: 0.8
  })
  map.add(trackPolyline)

  // 创建轨迹点标记
  props.trackData.forEach((point, index) => {
    const marker = new window.AMap.Marker({
      position: [point.lng, point.lat],
      title: `轨迹点 ${index + 1}`,
      icon: getTrackPointIcon(index),
      anchor: 'center'
    })

    // 点击事件
    marker.on('click', () => {
      showTrackPointInfo(point, marker)
    })

    trackMarkers.push(marker)
    map.add(marker)
  })

  // 自动调整视野
  if (path.length > 0) {
    map.setFitView(trackMarkers)
  }
}

// 更新事件标记
const updateEventMarkers = () => {
  if (!map || !props.events.length) return

  // 清除现有事件标记
  eventMarkers.forEach(marker => map.remove(marker))
  eventMarkers = []

  // 创建事件标记
  props.events.forEach(event => {
    if (event.location && event.location.lat && event.location.lng) {
      const marker = new window.AMap.Marker({
        position: [event.location.lng, event.location.lat],
        title: getEventTypeText(event.eventType),
        icon: getEventIcon(event.severity),
        anchor: 'center'
      })

      // 点击事件
      marker.on('click', () => {
        showEventInfo(event, marker)
        emit('event-click', event)
      })

      eventMarkers.push(marker)
      map.add(marker)
    }
  })
}

// 获取轨迹点图标
const getTrackPointIcon = (index) => {
  if (index === 0) {
    return 'https://webapi.amap.com/theme/v1.3/markers/n/mark_g.png' // 起点
  } else if (index === props.trackData.length - 1) {
    return 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png' // 终点
  } else {
    return 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png' // 中间点
  }
}

// 获取事件图标
const getEventIcon = (severity) => {
  const iconMap = {
    LOW: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_g.png',
    MEDIUM: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_y.png',
    HIGH: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png',
    CRITICAL: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png'
  }
  return iconMap[severity] || iconMap.MEDIUM
}

// 显示轨迹点信息
const showTrackPointInfo = (point, marker) => {
  const content = `
    <div class="track-point-info">
      <h4>轨迹点</h4>
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
      <p><strong>严重程度:</strong> ${getSeverityText(event.severity)}</p>
      <p><strong>置信度:</strong> ${(event.confidence * 100).toFixed(1)}%</p>
      <p><strong>时间:</strong> ${formatTime(event.timestamp)}</p>
      <p><strong>描述:</strong> ${event.context || '无'}</p>
    </div>
  `

  infoWindow.setContent(content)
  infoWindow.open(map, marker.getPosition())
}

// 播放轨迹
const playTrack = () => {
  if (!props.trackData.length) return

  if (isPlaying.value) {
    // 暂停
    if (playInterval) {
      clearInterval(playInterval)
      playInterval = null
    }
    isPlaying.value = false
  } else {
    // 播放
    isPlaying.value = true
    currentTimeIndex.value = 0
    
    playInterval = setInterval(() => {
      if (currentTimeIndex.value < props.trackData.length - 1) {
        currentTimeIndex.value++
        onTimeChange(currentTimeIndex.value)
      } else {
        // 播放结束
        if (playInterval) {
          clearInterval(playInterval)
          playInterval = null
        }
        isPlaying.value = false
      }
    }, 1000) // 每秒播放一个点
  }
}

// 重置轨迹
const resetTrack = () => {
  if (playInterval) {
    clearInterval(playInterval)
    playInterval = null
  }
  isPlaying.value = false
  currentTimeIndex.value = 0
  updateTrackDisplay()
}

// 时间变化处理
const onTimeChange = (index) => {
  if (!map || !props.trackData[index]) return

  const point = props.trackData[index]
  
  // 移动地图中心到当前点
  map.setCenter([point.lng, point.lat])
  
  // 更新当前标记
  if (currentMarker) {
    map.remove(currentMarker)
  }
  
  currentMarker = new window.AMap.Marker({
    position: [point.lng, point.lat],
    title: '当前位置',
    icon: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png',
    anchor: 'center'
  })
  
  map.add(currentMarker)
}

// 居中地图
const centerMap = () => {
  if (map && props.trackData.length > 0) {
    map.setFitView(trackMarkers)
  } else if (map) {
    map.setCenter([props.center.lng, props.center.lat])
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

// 格式化时间提示
const formatTimeTooltip = (value) => {
  if (!props.trackData[value]) return ''
  return formatTime(props.trackData[value].timestamp)
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '未知'
  return new Date(time).toLocaleString()
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

// 监听数据变化
watch(() => props.trackData, updateTrackDisplay, { deep: true })
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
  if (playInterval) {
    clearInterval(playInterval)
  }
  if (map) {
    map.destroy()
  }
})

// 暴露方法给父组件
defineExpose({
  centerMap,
  updateTrackDisplay,
  updateEventMarkers,
  getMap: () => map
})
</script>

<style scoped lang="scss">
.simple-track-map {
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
