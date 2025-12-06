<template>
  <div class="relative w-full h-screen">
    
    <!-- 返回按钮 -->
    <router-link
      to="/"
      class="absolute top-4 left-4 z-50 glass-control w-10 h-10 flex items-center justify-center hover:bg-slate-700 transition text-cyan-400"
    >
      <i class="ri-arrow-left-line text-xl"></i>
    </router-link>

    <!-- 标题悬浮窗 -->
    <div class="absolute top-4 left-16 z-50 glass-control px-6 py-2">
      <h1 class="text-lg font-bold tracking-wider">{{ isTrackMode ? `车辆轨迹回放 - ${deviceId}` : '疲劳事件时空分布热力图' }}</h1>
      <p class="text-xs text-gray-400">DATA SOURCE: RISC-V EDGE DEVICES</p>
    </div>

    <!-- 右侧图例 -->
    <div v-if="!isTrackMode" class="absolute bottom-32 right-4 z-50 glass-control p-4 w-48">
      <h3 class="text-xs font-bold text-gray-400 mb-2">风险等级 (RISK LEVEL)</h3>
      <div class="flex items-center gap-2 text-xs mb-1">
        <div class="w-3 h-3 rounded-full bg-red-600"></div>
        高风险 (Level 3)
      </div>
      <div class="flex items-center gap-2 text-xs mb-1">
        <div class="w-3 h-3 rounded-full bg-yellow-400"></div>
        中风险 (Level 2)
      </div>
      <div class="flex items-center gap-2 text-xs">
        <div class="w-3 h-3 rounded-full bg-blue-400"></div>
        低风险 (Level 1)
      </div>
    </div>

    <!-- 底部时间轴控制器 -->
    <div class="absolute bottom-8 left-8 right-8 z-50 glass-control p-4 flex items-center gap-4">
      <button
        @click="togglePlay"
        class="w-10 h-10 rounded-full bg-cyan-500 text-black flex items-center justify-center hover:bg-cyan-400 transition"
      >
        <i :class="isPlaying ? 'ri-pause-line' : 'ri-play-fill'"></i>
      </button>
      <div class="flex-1">
        <div class="flex justify-between text-xs text-gray-400 mb-2">
          <span v-if="!isTrackMode">00:00</span>
          <span v-if="isTrackMode">起点</span>
          <span class="text-cyan-400 font-bold text-lg">{{ timeDisplay }}</span>
          <span v-if="!isTrackMode">23:59</span>
          <span v-if="isTrackMode">终点</span>
        </div>
        <input
          v-if="!isTrackMode"
          v-model.number="currentTime"
          type="range"
          min="0"
          max="23"
          step="1"
          @input="updateHeatmap"
          class="w-full"
        />
        <input
          v-if="isTrackMode"
          v-model.number="currentTrackIndex"
          type="range"
          :min="0"
          :max="trackDataLength > 0 ? trackDataLength - 1 : 0"
          step="1"
          @input="onTrackTimeChange"
          class="w-full"
        />
      </div>
    </div>

    <!-- 地图容器 -->
    <div ref="heatmapContainerRef" class="w-full h-full"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { getMapHeatmap, getMapTrack } from '@/api'
import { debounce } from '@/utils'

const route = useRoute()

// 检查URL参数，判断是否为轨迹回放模式
const deviceId = route.query.deviceId
const mode = route.query.mode
const isTrackMode = mode === 'track' && deviceId

const currentTime = ref(12)
const isPlaying = ref(false)
const timeDisplay = ref('12:00 - 13:00')
const currentTrackIndex = ref(0)
const trackData = ref([])

let heatmapInstance = null
let map = null
let timer = null
let trackTimer = null
let trackPolyline = null
let trackMarkers = []
let eventMarkers = []
let isPlayingTrack = false

const heatmapContainerRef = ref(null)

const trackDataLength = computed(() => trackData.value.length)

// 从API获取热力数据
const fetchHeatmapData = async (hour, useCache = true) => {
  try {
    const now = Date.now()
    const startTime = now - (24 - hour) * 60 * 60 * 1000
    const endTime = startTime + 60 * 60 * 1000

    const data = await getMapHeatmap({
      startTime,
      endTime
    })

    if (data.code === 200 && data.data && data.data.points) {
      const points = data.data.points.map(p => ({
        lng: p.lng,
        lat: p.lat,
        count: Math.floor((p.intensity || 0) * 100)
      }))
      return { points, max: 100 }
    }
  } catch (error) {
    console.error('获取热力图数据失败:', error)
  }

  return { points: [], max: 100 }
}

// 防抖版本的更新函数
const debouncedUpdateHeatmap = debounce(async () => {
  timeDisplay.value = `${String(currentTime.value).padStart(2, '0')}:00 - ${String(Number(currentTime.value) + 1).padStart(2, '0')}:00`
  if (heatmapInstance) {
    const heatmapData = await fetchHeatmapData(currentTime.value, true)
    heatmapInstance.setData({
      data: heatmapData.points,
      max: heatmapData.max
    })
  }
}, 500)

const updateHeatmap = async () => {
  debouncedUpdateHeatmap()
}

const togglePlay = () => {
  if (isTrackMode) {
    if (isPlayingTrack) {
      pauseTrack()
    } else {
      playTrack()
    }
  } else {
    isPlaying.value = !isPlaying.value
    if (isPlaying.value) {
      timer = setInterval(async () => {
        if (currentTime.value >= 23) currentTime.value = 0
        else currentTime.value = Number(currentTime.value) + 1
        await updateHeatmap()
      }, 3000)
    } else {
      if (timer) clearInterval(timer)
    }
  }
}

// 获取轨迹数据
const fetchTrackData = async (deviceId, startTime, endTime) => {
  try {
    const data = await getMapTrack({
      deviceId,
      startTime,
      endTime
    })
    if (data.code === 200 && data.data && data.data.track) {
      return data.data.track
    }
  } catch (error) {
    console.error('获取轨迹数据失败:', error)
  }
  return []
}

// 显示轨迹
const displayTrack = track => {
  if (!map || !track || track.length === 0) return

  if (trackPolyline) map.remove(trackPolyline)
  trackMarkers.forEach(m => map.remove(m))
  eventMarkers.forEach(m => map.remove(m))
  trackMarkers = []
  eventMarkers = []

  const path = track.map(p => [p.location.lng, p.location.lat]).filter(p => p[0] && p[1])
  if (path.length > 0) {
    trackPolyline = new AMap.Polyline({
      path: path,
      strokeColor: '#00f2ff',
      strokeWeight: 3,
      strokeOpacity: 0.8
    })
    map.add(trackPolyline)

    track.forEach((point, index) => {
      if (point.location && point.location.lat && point.location.lng) {
        const marker = new AMap.Marker({
          position: [point.location.lng, point.location.lat],
          title: `轨迹点 ${index + 1}`,
          icon: new AMap.Icon({
            size: new AMap.Size(8, 8),
            image: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iOCIgheight="8IiB2aWV3Qm94PSIwIDAgOCA4IiBmaWxsPSJub25lIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxjaXJjbGUgY3g9IjQiIGN5PSI0IiByPSIzIiBmaWxsPSIjMDBmMmZmIi8+PC9zdmc+',
            imageSize: new AMap.Size(8, 8)
          }),
          zIndex: 100
        })
        trackMarkers.push(marker)
        map.add(marker)
      }

      if (point.events && point.events.length > 0) {
        point.events.forEach(event => {
          if (event.location && event.location.lat && event.location.lng) {
            const eventMarker = new AMap.Marker({
              position: [event.location.lng, event.location.lat],
              title: `${event.behavior} - ${event.level}`,
              icon: new AMap.Icon({
                size: new AMap.Size(16, 16),
                image: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTYiIGhlaWdodD0iMTYiIHZpZXdCb3g9IjAgMCAxNiAxNiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSI4IiBjeT0iOCIgcj0iNyIgZmlsbD0iI2ZmNGQ0ZiIgc3Ryb2tlPSIjZmZmIiBzdHJva2Utd2lkdGg9IjIiLz48L3N2Zz4=',
                imageSize: new AMap.Size(16, 16)
              }),
              zIndex: 200
            })
            eventMarkers.push(eventMarker)
            map.add(eventMarker)
          }
        })
      }
    })

    map.setFitView([trackPolyline, ...trackMarkers, ...eventMarkers])
  }
}

// 播放轨迹
const playTrack = () => {
  if (trackData.value.length === 0) {
    alert('没有轨迹数据')
    return
  }
  isPlayingTrack = true
  isPlaying.value = true
  if (trackTimer) {
    clearInterval(trackTimer)
  }
  trackTimer = setInterval(() => {
    if (currentTrackIndex.value < trackData.value.length - 1) {
      currentTrackIndex.value++
      onTrackTimeChange()
    } else {
      pauseTrack()
    }
  }, 1000)
}

// 暂停轨迹
const pauseTrack = () => {
  isPlayingTrack = false
  isPlaying.value = false
  if (trackTimer) {
    clearInterval(trackTimer)
    trackTimer = null
  }
}

// 轨迹时间变化
const onTrackTimeChange = () => {
  if (trackData.value.length === 0) return
  const point = trackData.value[currentTrackIndex.value]
  if (point && point.location) {
    map.setCenter([point.location.lng, point.location.lat])
    const date = new Date(point.timestamp)
    timeDisplay.value = `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  }
}

onMounted(async () => {
  if (!heatmapContainerRef.value) return

  // 等待高德地图和热力图插件加载完成
  if (!window.AMap) {
    console.error('高德地图API未加载')
    return
  }

  map = new AMap.Map(heatmapContainerRef.value, {
    resizeEnable: true,
    center: [118.7969, 32.0603],
    zoom: 13,
    mapStyle: 'amap://styles/darkblue'
  })

  if (isTrackMode) {
    const now = Date.now()
    const startTime = now - 24 * 60 * 60 * 1000
    trackData.value = await fetchTrackData(deviceId, startTime, now)
    displayTrack(trackData.value)

    if (trackData.value.length > 0) {
      currentTrackIndex.value = 0
      onTrackTimeChange()
    }
  } else {
    // 等待地图加载完成后初始化热力图
    map.on('complete', () => {
      // 使用插件方式加载热力图
      AMap.plugin('AMap.Heatmap', () => {
        initHeatmap()
      })
    })
    
    // 如果地图已经加载完成，直接初始化
    if (map.getStatus && map.getStatus() === 'complete') {
      AMap.plugin('AMap.Heatmap', () => {
        initHeatmap()
      })
    }
  }
})

const initHeatmap = async () => {
  try {
    // 高德地图v2.0中，热力图类名是AMap.Heatmap（注意大小写）
    if (typeof AMap.Heatmap === 'undefined' && typeof AMap.HeatMap === 'undefined') {
      console.error('热力图插件未加载，请检查插件配置')
      return
    }
    
    // 尝试使用AMap.Heatmap（v2.0标准）或AMap.HeatMap（兼容）
    const HeatmapClass = AMap.Heatmap || AMap.HeatMap
    
    heatmapInstance = new HeatmapClass(map, {
      radius: 25,
      opacity: [0, 0.8],
      gradient: {
        0.5: 'blue',
        0.65: 'rgb(117,211,248)',
        0.7: 'rgb(0, 255, 0)',
        0.9: '#ffea00',
        1.0: 'red'
      }
    })

    const heatmapData = await fetchHeatmapData(currentTime.value, false)
    if (heatmapInstance && heatmapData.points && heatmapData.points.length > 0) {
      heatmapInstance.setData({
        data: heatmapData.points,
        max: heatmapData.max
      })
      console.log('热力图数据已加载:', heatmapData.points.length, '个点')
    } else {
      console.warn('热力图数据为空')
    }
  } catch (error) {
    console.error('初始化热力图失败:', error)
  }
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (trackTimer) {
    clearInterval(trackTimer)
    trackTimer = null
  }
  if (map) {
    map.destroy()
  }
})
</script>

<style scoped>
/* 使用全局样式 */
</style>

