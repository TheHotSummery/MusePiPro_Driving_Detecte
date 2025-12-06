<template>
  <div class="w-screen h-screen flex flex-col">
    <!-- 顶部导航 -->
    <header class="h-16 border-b border-slate-800 flex items-center px-6 bg-slate-900/50 backdrop-blur justify-between">
      <div class="flex items-center gap-4">
        <router-link to="/" class="text-gray-400 hover:text-white transition">
          <i class="ri-arrow-left-line text-xl"></i>
        </router-link>
        <h1 class="text-xl font-bold tracking-wider text-cyan-400">
          单车监控中心 <span class="text-xs text-gray-500 ml-2">VEHICLE MONITORING</span>
        </h1>
      </div>
      <div class="flex gap-4">
        <button
          @click="showHistoryTrack"
          class="px-4 py-1 border border-cyan-500/50 text-cyan-400 text-xs rounded hover:bg-cyan-500/20"
        >
          历史回放
        </button>
      </div>
    </header>

    <div class="flex-1 flex overflow-hidden">
      <!-- 左侧车辆列表 -->
      <div class="w-64 border-r border-slate-800 bg-slate-900/30 flex flex-col">
        <div class="p-4 border-b border-slate-800">
          <input
            v-model="searchText"
            type="text"
            placeholder="搜索设备/司机..."
            class="w-full bg-slate-800 border-none rounded px-3 py-2 text-xs text-white outline-none focus:ring-1 focus:ring-cyan-400"
          />
        </div>
        <div class="flex-1 overflow-y-auto">
          <div
            v-for="v in filteredVehicles"
            :key="v.deviceId"
            @click="selectVehicle(v)"
            class="vehicle-item p-4 border-b border-slate-800/50 cursor-pointer hover:bg-slate-800/50 transition"
            :class="{ 'active': currentVehicle && currentVehicle.deviceId === v.deviceId }"
          >
            <div class="flex justify-between mb-1">
              <span class="font-bold">{{ v.deviceId }}</span>
              <span
                class="text-xs px-1 rounded"
                :class="v.status === 'online' ? 'bg-green-900 text-green-400' : 'bg-gray-700 text-gray-400'"
              >{{ v.status }}</span>
            </div>
            <div class="flex justify-between text-xs text-gray-400">
              <span>{{ v.driverName || '未绑定' }}</span>
              <span class="font-tech">Score: {{ v.score.toFixed(1) }}</span>
            </div>
          </div>
          <div v-if="filteredVehicles.length === 0" class="p-4 text-center text-gray-500 text-xs">
            暂无车辆数据
          </div>
        </div>
      </div>

      <!-- 中间主控区 - 轨迹地图（全屏） -->
      <div class="flex-1 flex flex-col bg-black relative">
        <div ref="trackMapRef" class="w-full h-full"></div>
      </div>

      <!-- 右侧数据遥测 -->
      <div class="w-72 border-l border-slate-800 bg-slate-900/30 p-4 flex flex-col gap-6">
        <div>
          <h3 class="text-cyan-400 text-sm font-bold mb-4 border-l-2 border-cyan-400 pl-2">车辆状态 Telemetry</h3>
          <div class="grid grid-cols-2 gap-4">
            <div class="text-center p-2 bg-slate-800/50 rounded">
              <div class="text-xs text-gray-400">时速 (km/h)</div>
              <div class="text-2xl font-tech text-white">
                {{ currentVehicle ? (currentVehicle.speed || 0).toFixed(1) : '0' }}
              </div>
            </div>
            <div class="text-center p-2 bg-slate-800/50 rounded">
              <div class="text-xs text-gray-400">状态</div>
              <div
                class="text-lg font-tech"
                :class="currentVehicle && currentVehicle.status === 'online' ? 'text-green-400' : 'text-gray-400'"
              >
                {{ currentVehicle ? (currentVehicle.status === 'online' ? '在线' : '离线') : '--' }}
              </div>
            </div>
          </div>
        </div>

        <div class="flex-1">
          <h3 class="text-cyan-400 text-sm font-bold mb-2 border-l-2 border-cyan-400 pl-2">疲劳监测指数</h3>
          <div ref="gaugeChartRef" class="w-full h-48"></div>
          <div class="mt-4 space-y-2">
            <div class="text-xs flex justify-between text-gray-400">
              <span>当前级别</span>
              <span class="font-tech" :class="getLevelColor(currentVehicle?.level)">
                {{ currentVehicle?.level || 'Normal' }}
              </span>
            </div>
            <div class="text-xs flex justify-between text-gray-400">
              <span>疲劳分数</span>
              <span class="font-tech">{{ currentVehicle ? currentVehicle.score.toFixed(1) : '0' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getRealtimeVehicles, getMapTrack } from '@/api'
import { getLevelColor } from '@/utils'

const router = useRouter()

const searchText = ref('')
const vehicles = ref([])
const currentVehicle = ref(null)
let map = null
let trackPolyline = null
let trackMarkers = []
let eventMarkers = []
let gaugeChart = null

const trackMapRef = ref(null)
const gaugeChartRef = ref(null)

const filteredVehicles = computed(() => {
  if (!searchText.value) return vehicles.value
  const search = searchText.value.toLowerCase()
  return vehicles.value.filter(
    v =>
      v.deviceId.toLowerCase().includes(search) ||
      (v.driverName && v.driverName.toLowerCase().includes(search))
  )
})

const fetchVehicles = async () => {
  try {
    const data = await getRealtimeVehicles()
    if (data.code === 200 && data.data && data.data.vehicles) {
      vehicles.value = data.data.vehicles.map(v => ({
        deviceId: v.deviceId || '',
        driverId: v.driverId || '',
        driverName: v.driverName || '未绑定',
        status: v.status || 'offline',
        score: v.currentScore || 0,
        level: v.currentLevel || 'Normal',
        speed: v.location?.speed ? v.location.speed : 0,
        location: v.location
      }))

      if (vehicles.value.length > 0 && !currentVehicle.value) {
        selectVehicle(vehicles.value[0])
      }
    }
  } catch (error) {
    console.error('获取车辆列表失败:', error)
  }
}

const selectVehicle = async vehicle => {
  currentVehicle.value = vehicle
  updateGaugeChart(vehicle.score, vehicle.level)

  if (vehicle.deviceId) {
    await loadVehicleTrack(vehicle.deviceId)
  }
}

const loadVehicleTrack = async deviceId => {
  if (!map) return

  try {
    if (trackPolyline) map.remove(trackPolyline)
    trackMarkers.forEach(m => map.remove(m))
    eventMarkers.forEach(m => map.remove(m))
    trackMarkers = []
    eventMarkers = []

    const now = Date.now()
    const startTime = now - 24 * 60 * 60 * 1000
    const data = await getMapTrack({
      deviceId,
      startTime,
      endTime: now
    })

    if (data.code === 200 && data.data && data.data.track && data.data.track.length > 0) {
      const track = data.data.track

      const path = track
        .filter(p => p.location && p.location.lat && p.location.lng)
        .map(p => [p.location.lng, p.location.lat])

      if (path.length > 0) {
        trackPolyline = new AMap.Polyline({
          path: path,
          strokeColor: '#00f2ff',
          strokeWeight: 3,
          strokeOpacity: 0.8,
          map: map
        })

        if (path.length > 1) {
          const startMarker = new AMap.Marker({
            position: path[0],
            icon: new AMap.Icon({
              size: new AMap.Size(20, 20),
              image: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_g.png',
              imageSize: new AMap.Size(20, 20)
            }),
            title: '起点',
            map: map
          })
          trackMarkers.push(startMarker)

          const endMarker = new AMap.Marker({
            position: path[path.length - 1],
            icon: new AMap.Icon({
              size: new AMap.Size(20, 20),
              image: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png',
              imageSize: new AMap.Size(20, 20)
            }),
            title: '终点',
            map: map
          })
          trackMarkers.push(endMarker)
        }

        track.forEach(point => {
          if (point.events && point.events.length > 0) {
            point.events.forEach(event => {
              if (point.location && point.location.lat && point.location.lng) {
                let color = '#ffd700'
                let size = 12
                if (event.level === 'Level 3') {
                  color = '#ff0000'
                  size = 16
                } else if (event.level === 'Level 2') {
                  color = '#ff8c00'
                  size = 14
                }

                const eventMarker = new AMap.Marker({
                  position: [point.location.lng, point.location.lat],
                  icon: new AMap.Icon({
                    size: new AMap.Size(size, size),
                    image: `data:image/svg+xml;base64,${btoa(`<svg width="${size}" height="${size}" xmlns="http://www.w3.org/2000/svg"><circle cx="${size/2}" cy="${size/2}" r="${size/2-1}" fill="${color}" stroke="#fff" stroke-width="1"/></svg>`)}`,
                    imageSize: new AMap.Size(size, size)
                  }),
                  title: `${event.behavior} - ${event.level}`,
                  zIndex: 200,
                  map: map
                })

                eventMarkers.push(eventMarker)
              }
            })
          }
        })

        const allMarkers = [trackPolyline, ...trackMarkers, ...eventMarkers].filter(m => m)
        if (allMarkers.length > 0) {
          map.setFitView(allMarkers)
        }
      }
    }
  } catch (error) {
    console.error('加载轨迹失败:', error)
  }
}

const showHistoryTrack = () => {
  if (currentVehicle.value && currentVehicle.value.deviceId) {
    router.push(`/heatmap?deviceId=${currentVehicle.value.deviceId}&mode=track`)
  } else {
    alert('请先选择一辆车辆')
  }
}

const initMap = () => {
  if (!trackMapRef.value) return
  map = new AMap.Map(trackMapRef.value, {
    center: [118.7969, 32.0603],
    zoom: 13,
    mapStyle: 'amap://styles/darkblue',
    viewMode: '3D'
  })
}

const initGauge = () => {
  if (!gaugeChartRef.value) return
  gaugeChart = echarts.init(gaugeChartRef.value)
  updateGaugeChart(0, 'Normal')
}

const updateGaugeChart = (score, level) => {
  if (!gaugeChart) return

  let color = '#10b981'
  if (level === 'Level 3') {
    color = '#ef4444'
  } else if (level === 'Level 2') {
    color = '#f59e0b'
  } else if (level === 'Level 1') {
    color = '#eab308'
  }

  gaugeChart.setOption({
    series: [{
      type: 'gauge',
      startAngle: 180,
      endAngle: 0,
      min: 0,
      max: 100,
      splitNumber: 5,
      itemStyle: { color: color },
      progress: { show: true, width: 10 },
      pointer: { show: false },
      axisLine: {
        lineStyle: {
          width: 10,
          color: [[1, color]]
        }
      },
      axisTick: { show: false },
      splitLine: { length: 5, lineStyle: { width: 1, color: '#999' } },
      detail: {
        fontSize: 20,
        offsetCenter: [0, '0%'],
        valueAnimation: true,
        formatter: '{value}',
        color: color
      },
      data: [{ value: score, name: '疲劳值' }]
    }]
  })
}

onMounted(() => {
  initMap()
  initGauge()
  fetchVehicles()

  setInterval(fetchVehicles, 30000)
})

onUnmounted(() => {
  if (map) map.destroy()
  if (gaugeChart) gaugeChart.dispose()
})
</script>

<style scoped>
.vehicle-item.active {
  background: linear-gradient(90deg, rgba(0, 242, 255, 0.2), transparent);
  border-left: 4px solid #00f2ff;
}
</style>













