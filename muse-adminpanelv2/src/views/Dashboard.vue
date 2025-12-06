<template>
  <div class="relative w-screen h-screen overflow-hidden text-sm">
    <!-- 背景动效 -->
    <div class="bg-grid"></div>

    <!-- 顶部 Header -->
    <Header />

    <!-- 地图层 (底层) -->
    <div class="absolute inset-0 z-0">
      <div id="map-container" class="map-container"></div>
    </div>

    <!-- 左侧面板 -->
    <div class="absolute left-6 top-24 bottom-6 w-80 flex flex-col gap-4 z-10 pointer-events-none">
      
      <!-- 核心指标 (HUD风格) -->
      <GlassPanel class="p-4 pointer-events-auto">
        <h3 class="text-cyan-400 text-lg font-bold mb-3 border-l-4 border-cyan-400 pl-2">实时概览</h3>
        <div class="grid grid-cols-2 gap-3">
          <div class="bg-blue-900/30 p-3 rounded border border-blue-500/30">
            <div class="text-gray-400 text-xs">在线车辆</div>
            <div class="text-2xl font-tech text-white">
              {{ realtimeData.onlineVehicles }}<span class="text-sm text-gray-500">/{{ realtimeData.totalVehicles }}</span>
            </div>
          </div>
          <div class="bg-red-900/30 p-3 rounded border border-red-500/30 animate-pulse">
            <div class="text-gray-400 text-xs">活跃告警</div>
            <div class="text-2xl font-tech text-red-500">{{ realtimeData.activeAlerts }}</div>
          </div>
        </div>
        <div class="mt-4">
          <div class="flex justify-between text-xs mb-1 text-gray-300">
            <span>系统健康度</span>
            <span class="font-tech">98%</span>
          </div>
          <div class="w-full bg-gray-700 h-1 rounded-full overflow-hidden">
            <div class="bg-gradient-to-r from-green-400 to-cyan-400 h-full w-[98%]"></div>
          </div>
        </div>
      </GlassPanel>

      <!-- 疲劳趋势图表 -->
      <GlassPanel class="p-4 flex-1 pointer-events-auto flex flex-col">
        <h3 class="text-cyan-400 text-lg font-bold mb-2 border-l-4 border-cyan-400 pl-2">疲劳指数趋势</h3>
        <div ref="trendChartRef" class="flex-1 w-full"></div>
      </GlassPanel>

      <!-- 行为分布 -->
      <GlassPanel class="p-4 h-64 pointer-events-auto flex flex-col">
        <h3 class="text-cyan-400 text-lg font-bold mb-2 border-l-4 border-cyan-400 pl-2">异常行为类型</h3>
        <div ref="radarChartRef" class="flex-1 w-full"></div>
      </GlassPanel>
    </div>

    <!-- 右侧面板 -->
    <div class="absolute right-6 top-24 bottom-6 w-80 flex flex-col gap-4 z-10 pointer-events-none">
      
      <!-- 区域风险排行 -->
      <GlassPanel class="p-4 h-64 pointer-events-auto flex flex-col">
        <h3 class="text-cyan-400 text-lg font-bold mb-2 border-l-4 border-cyan-400 pl-2">区域风险分布</h3>
        <div ref="regionChartRef" class="flex-1 w-full"></div>
      </GlassPanel>

      <!-- 实时告警流 -->
      <GlassPanel class="p-4 flex-1 pointer-events-auto overflow-hidden flex flex-col">
        <h3 class="text-cyan-400 text-lg font-bold mb-3 border-l-4 border-cyan-400 pl-2">实时告警监控</h3>
        <div class="overflow-y-auto flex-1 pr-2 space-y-2">
          <div
            v-for="alert in alerts"
            :key="alert.alertId"
            @click="showDriverEventsOnMap(alert.driverId, alert.driverName, alert.deviceId)"
            class="alert-item p-3 rounded bg-gradient-to-r from-gray-800 to-transparent border-l-2 cursor-pointer hover:bg-gray-700/50 transition"
            :class="getLevelColorClass(alert.level)"
          >
            <div class="flex justify-between items-start mb-1">
              <span class="font-bold text-gray-200">{{ alert.driverName }}</span>
              <span
                class="font-tech text-xs px-1 rounded text-black font-bold"
                :class="getLevelBgClass(alert.level)"
              >{{ alert.level }}</span>
            </div>
            <div class="text-xs text-gray-400 mb-1">ID: {{ alert.deviceId }}</div>
            <div class="flex justify-between items-center text-xs">
              <span class="text-white">{{ alert.behavior }}</span>
              <span class="font-tech text-gray-500">{{ formatTime(alert.timestamp) }}</span>
            </div>
          </div>
        </div>
      </GlassPanel>
    </div>

    <!-- 底部控制栏 -->
    <NavigationBar />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import Header from '@/components/Header.vue'
import NavigationBar from '@/components/NavigationBar.vue'
import GlassPanel from '@/components/GlassPanel.vue'
import {
  getRealtimeVehicles,
  getRealtimeAlerts,
  getTrendChart,
  getBehaviorDistributionChart,
  getRegionDistributionChart,
  getMapVehicles,
  getMapTrack
} from '@/api'
import { formatTime, getLevelColorClass, getLevelBgClass, debounce, throttle } from '@/utils'

const router = useRouter()

// 状态数据
const realtimeData = ref({
  totalVehicles: 0,
  onlineVehicles: 0,
  activeAlerts: 0
})
const alerts = ref([])
const trendData = ref([])
const behaviorData = ref({})
const regionData = ref([])

// 图表引用
const trendChartRef = ref(null)
const radarChartRef = ref(null)
const regionChartRef = ref(null)
let trendChart = null
let radarChart = null
let regionChart = null

// 地图实例
let map = null
let markers = []
let eventMarkers = []
let trackPolyline = null
let isMapMoving = false
let lastMapBounds = null
const MAP_UPDATE_DELAY = 800
const MAP_MOVE_THRESHOLD = 0.01

// 事件标注相关
let allEvents = [] // 存储所有事件
let visibleEventMarkers = [] // 当前可见的事件标注
let currentDriverName = '' // 当前查看的驾驶员名称
let mapEventHandlers = [] // 存储地图事件监听器
const MIN_EVENT_DISTANCE = 50 // 最小标注间距（像素）

// 获取实时数据
const fetchRealtimeData = async () => {
  try {
    const vehiclesData = await getRealtimeVehicles()
    if (vehiclesData.code === 200 && vehiclesData.data) {
      realtimeData.value.totalVehicles = vehiclesData.data.totalVehicles || 0
      realtimeData.value.onlineVehicles = vehiclesData.data.onlineVehicles || 0
    }

    const alertsData = await getRealtimeAlerts()
    if (alertsData.code === 200 && alertsData.data) {
      alerts.value = (alertsData.data.alerts || []).slice(0, 20).map(a => ({
        alertId: a.eventId || a.alertId,
        driverId: a.driverId || '',
        driverName: a.driverName || '未知',
        deviceId: a.deviceId || '',
        level: a.level || 'Normal',
        behavior: a.behavior || '未知行为',
        timestamp: a.timestamp || Date.now(),
        location: a.location || null,
        score: a.score || 0
      }))
      realtimeData.value.activeAlerts = alerts.value.length
    }
  } catch (error) {
    console.error('获取实时数据失败:', error)
  }
}

// 获取图表数据
const fetchChartData = async () => {
  try {
    const now = Date.now()
    const startTime = now - 24 * 60 * 60 * 1000

    const trendData_res = await getTrendChart({
      startTime,
      endTime: now,
      interval: 'hour'
    })
    if (trendData_res.code === 200 && trendData_res.data) {
      trendData.value = trendData_res.data.series?.[0]?.data || []
    }

    const behaviorData_res = await getBehaviorDistributionChart({
      startTime,
      endTime: now
    })
    if (behaviorData_res.code === 200 && behaviorData_res.data) {
      // 后端返回的是 { data: [...], statistics: {...} }
      // 将数组转换为对象格式，方便前端使用
      const dataArray = behaviorData_res.data.data || []
      const distributionObj = {}
      dataArray.forEach(item => {
        if (item.behavior) {
          distributionObj[item.behavior] = {
            count: item.count || 0,
            behaviorName: item.behaviorName || item.behavior,
            percentage: item.percentage || 0
          }
        }
      })
      behaviorData.value = distributionObj
      console.log('行为分布数据:', behaviorData.value)
    } else {
      console.warn('获取行为分布数据失败:', behaviorData_res)
    }

    const regionData_res = await getRegionDistributionChart({
      startTime,
      endTime: now
    })
    if (regionData_res.code === 200 && regionData_res.data) {
      regionData.value = regionData_res.data.data || regionData_res.data.distribution || []
    }
  } catch (error) {
    console.error('获取图表数据失败:', error)
  }
}

// 初始化图表
const initCharts = () => {
  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value)
    updateTrendChart()
  }

  if (radarChartRef.value) {
    radarChart = echarts.init(radarChartRef.value)
    updateRadarChart()
  }

  if (regionChartRef.value) {
    regionChart = echarts.init(regionChartRef.value)
    updateRegionChart()
  }

  window.addEventListener('resize', () => {
    trendChart?.resize()
    radarChart?.resize()
    regionChart?.resize()
  })
}

const updateTrendChart = () => {
  if (!trendChart) return
  const xData = trendData.value.map(d => {
    const date = new Date(d.timestamp)
    return `${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
  })
  const yData = trendData.value.map(d => d.value || 0)

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0,0,0,0.7)',
      borderColor: '#00f2ff',
      textStyle: { color: '#fff' }
    },
    grid: { top: 10, right: 10, bottom: 20, left: 30, containLabel: true },
    xAxis: {
      type: 'category',
      data: xData.length > 0 ? xData : ['10:00', '10:05', '10:10', '10:15', '10:20', '10:25'],
      axisLine: { lineStyle: { color: '#334155' } },
      axisLabel: { color: '#94a3b8' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#1e293b', type: 'dashed' } },
      axisLabel: { color: '#94a3b8' }
    },
    series: [{
      data: yData.length > 0 ? yData : [45, 55, 82, 60, 70, 90],
      type: 'line',
      smooth: true,
      symbol: 'none',
      lineStyle: { color: '#00f2ff', width: 3 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(0, 242, 255, 0.5)' },
          { offset: 1, color: 'rgba(0, 242, 255, 0)' }
        ])
      }
    }]
  })
}

const updateRadarChart = () => {
  if (!radarChart) return
  
  const behaviorNames = ['eyes_closed', 'yarning', 'head_down', 'seeing_left', 'seeing_right']
  const behaviorLabels = ['闭眼', '打哈欠', '低头', '左看', '右看']
  
  // 从 behaviorData 中获取数据
  const values = behaviorNames.map(name => {
    if (Array.isArray(behaviorData.value)) {
      const item = behaviorData.value.find(b => b.behavior === name)
      return item?.count || 0
    } else if (behaviorData.value && typeof behaviorData.value === 'object') {
      const item = behaviorData.value[name]
      return item?.count || 0
    }
    return 0
  })
  
  // 计算最大值，用于动态设置雷达图的最大值
  const maxValue = Math.max(...values, 1) // 至少为1，避免除零
  const normalizedMax = Math.ceil(maxValue * 1.2) // 增加20%的余量
  
  // 如果所有值都是0，使用默认值显示
  const hasData = values.some(v => v > 0)
  const displayValues = hasData ? values : [0, 0, 0, 0, 0]
  const displayMax = hasData ? normalizedMax : 100

  radarChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(0,0,0,0.7)',
      borderColor: '#00f2ff',
      textStyle: { color: '#fff' },
      formatter: (params) => {
        const index = params.dataIndex
        const value = displayValues[index]
        const label = behaviorLabels[index]
        const name = behaviorNames[index]
        const item = Array.isArray(behaviorData.value) 
          ? behaviorData.value.find(b => b.behavior === name)
          : behaviorData.value?.[name]
        const percentage = item?.percentage ? item.percentage.toFixed(1) : '0.0'
        return `${label}<br/>数量: ${value}<br/>占比: ${percentage}%`
      }
    },
    radar: {
      indicator: behaviorLabels.map(label => ({ 
        name: label, 
        max: displayMax 
      })),
      splitArea: { 
        areaStyle: { 
          color: ['rgba(13, 25, 48, 0.6)', 'rgba(13, 25, 48, 0.8)'] 
        } 
      },
      axisLine: { lineStyle: { color: '#334155' } },
      splitLine: { lineStyle: { color: '#334155' } },
      name: { 
        textStyle: { 
          color: '#00f2ff',
          fontSize: 12
        } 
      },
      center: ['50%', '55%'],
      radius: '70%'
    },
    series: [{
      type: 'radar',
      data: [{
        value: displayValues,
        name: '行为统计',
        itemStyle: { 
          color: hasData ? '#ff4d4f' : '#666'
        },
        areaStyle: { 
          color: hasData ? 'rgba(255, 77, 79, 0.4)' : 'rgba(100, 100, 100, 0.2)'
        },
        lineStyle: {
          color: hasData ? '#ff4d4f' : '#666',
          width: 2
        }
      }]
    }]
  })
}

const updateRegionChart = () => {
  if (!regionChart) return
  const topRegions = regionData.value.slice(0, 10)
  const names = topRegions.map(r => r.regionName || '未知区域')
  const values = topRegions.map(r => r.count || r.eventCount || 0)

  regionChart.setOption({
    grid: { top: 0, right: 20, bottom: 20, left: 10, containLabel: true },
    xAxis: { type: 'value', show: false },
    yAxis: {
      type: 'category',
      data: names.length > 0 ? names : ['鼓楼区', '秦淮区', '建邺区', '玄武区'],
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#fff' }
    },
    series: [{
      type: 'bar',
      data: values.length > 0 ? values : [45, 60, 75, 82],
      barWidth: 10,
      itemStyle: {
        borderRadius: [0, 5, 5, 0],
        color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
          { offset: 0, color: '#ff4d4f' },
          { offset: 1, color: '#8b0000' }
        ])
      },
      label: { show: true, position: 'right', color: '#fff', formatter: '{c}' }
    }]
  })
}

// 初始化地图
const initMap = () => {
  if (!window.AMap) {
    console.error('高德地图API未加载，请检查Key配置')
    return
  }

  map = new AMap.Map('map-container', {
    resizeEnable: true,
    center: [118.7969, 32.0603], // 南京坐标
    zoom: 11,
    pitch: 50,
    viewMode: '3D',
    mapStyle: 'amap://styles/darkblue'
  })

  const loadVehicles = async (useCache = true) => {
    try {
      if (!map) {
        console.warn('地图对象不存在，跳过加载车辆数据')
        return
      }
      
      // 等待地图完全加载
      if (!map.getBounds || typeof map.getBounds !== 'function') {
        console.warn('地图尚未完全初始化，跳过加载车辆数据')
        return
      }
      
      let bounds
      try {
        bounds = map.getBounds()
      } catch (e) {
        console.warn('获取地图边界失败:', e)
        return
      }
      
      if (!bounds) {
        console.warn('无法获取地图边界，跳过加载车辆数据')
        return
      }
      
      // 检查 bounds 对象的方法是否存在
      if (!bounds.getSouthWest || typeof bounds.getSouthWest !== 'function' ||
          !bounds.getNorthEast || typeof bounds.getNorthEast !== 'function') {
        console.warn('地图边界对象未完全初始化，跳过加载车辆数据')
        return
      }
      
      let sw, ne
      try {
        sw = bounds.getSouthWest()
        ne = bounds.getNorthEast()
      } catch (e) {
        console.warn('获取地图边界坐标失败:', e)
        return
      }
      
      if (!sw || !ne || !sw.lng || !sw.lat || !ne.lng || !ne.lat) {
        console.warn('地图边界坐标无效，跳过加载车辆数据')
        return
      }
      
      const boundsStr = `${sw.lng},${sw.lat},${ne.lng},${ne.lat}`

      if (lastMapBounds && useCache) {
        try {
          const lastSw = lastMapBounds.getSouthWest()
          const lastNe = lastMapBounds.getNorthEast()
          
          if (lastSw && lastNe) {
            const latDiff = Math.abs(sw.lat - lastSw.lat) + Math.abs(ne.lat - lastNe.lat)
            const lngDiff = Math.abs(sw.lng - lastSw.lng) + Math.abs(ne.lng - lastNe.lng)

            if (latDiff < MAP_MOVE_THRESHOLD && lngDiff < MAP_MOVE_THRESHOLD) {
              return
            }
          }
        } catch (e) {
          console.warn('比较地图边界失败:', e)
          // 继续执行，不返回
        }
      }

      lastMapBounds = bounds

      const data = await getMapVehicles({ bounds: boundsStr }, useCache)

      if (data.code === 200 && data.data && data.data.vehicles) {
        markers.forEach(m => map.remove(m))
        markers = []

        data.data.vehicles.forEach(vehicle => {
          if (vehicle.location && vehicle.location.lat && vehicle.location.lng) {
            const content = `
              <div style="position:relative;">
                <div style="width:16px;height:16px;background:#00f2ff;border-radius:50%;box-shadow:0 0 10px #00f2ff; border:2px solid white;"></div>
                <div style="position:absolute;top:-30px;left:-20px;background:rgba(0,0,0,0.7);color:#00f2ff;padding:2px 5px;border:1px solid #00f2ff;font-size:10px;white-space:nowrap;">
                  ${vehicle.deviceId || 'UNKNOWN'}
                </div>
              </div>
            `

            const marker = new AMap.Marker({
              position: [vehicle.location.lng, vehicle.location.lat],
              content: content,
              offset: new AMap.Pixel(-8, -8),
              map: map
            })

            marker.on('click', () => {
              if (vehicle.deviceId) {
                router.push(`/heatmap?deviceId=${vehicle.deviceId}&mode=track`)
              }
            })

            markers.push(marker)
          }
        })
      }
    } catch (error) {
      console.error('加载车辆数据失败:', error)
    }
  }

  const debouncedLoadVehicles = debounce(() => {
    isMapMoving = false
    loadVehicles(true)
  }, MAP_UPDATE_DELAY)

  const throttledLoadVehicles = throttle(() => {
    if (!isMapMoving) {
      isMapMoving = true
    }
  }, 200)

  map.on('movestart', () => {
    isMapMoving = true
  })

  map.on('moveend', () => {
    throttledLoadVehicles()
    debouncedLoadVehicles()
  })

  map.on('zoomend', debouncedLoadVehicles)
  map.on('dragend', debouncedLoadVehicles)

  // 等待地图完全加载后再加载车辆数据
  const tryLoadVehicles = () => {
    try {
      if (!map) return false
      
      if (typeof map.getBounds !== 'function') {
        return false
      }
      
      const bounds = map.getBounds()
      if (!bounds) return false
      
      if (typeof bounds.getSouthWest !== 'function' || 
          typeof bounds.getNorthEast !== 'function') {
        return false
      }
      
      const sw = bounds.getSouthWest()
      const ne = bounds.getNorthEast()
      
      if (!sw || !ne || !sw.lng || !sw.lat || !ne.lng || !ne.lat) {
        return false
      }
      
      // 所有检查通过，可以加载车辆数据
      loadVehicles(false)
      return true
    } catch (e) {
      console.warn('地图尚未完全初始化:', e)
      return false
    }
  }

  // 地图加载完成事件
  map.on('complete', () => {
    console.log('地图加载完成')
    // 延迟一点时间确保地图完全就绪
    setTimeout(() => {
      tryLoadVehicles()
    }, 800)
  })

  // 延迟加载，给地图一些时间初始化
  setTimeout(() => {
    if (!tryLoadVehicles()) {
      // 如果第一次尝试失败，再等一会儿
      setTimeout(() => {
        if (!tryLoadVehicles()) {
          // 如果还是失败，再等一次
          setTimeout(() => {
            tryLoadVehicles()
          }, 1500)
        }
      }, 1500)
    }
  }, 1500)

  // 定期更新车辆数据（仅在地图完全初始化后）
  setInterval(() => {
    if (!isMapMoving && map) {
      // 检查地图是否完全初始化
      try {
        if (typeof map.getBounds === 'function') {
          const bounds = map.getBounds()
          if (bounds && typeof bounds.getSouthWest === 'function') {
            const sw = bounds.getSouthWest()
            if (sw && sw.lng && sw.lat) {
              loadVehicles(false)
            }
          }
        }
      } catch (e) {
        // 地图未完全初始化，跳过本次更新
        console.debug('定时更新车辆数据：地图未完全初始化，跳过')
      }
    }
  }, 30000)
}

// 计算两点之间的像素距离
const getPixelDistance = (pos1, pos2) => {
  try {
    // 确保坐标格式正确 [lng, lat]
    if (!pos1 || !pos2) return Infinity
    if (Array.isArray(pos1) && (isNaN(pos1[0]) || isNaN(pos1[1]))) return Infinity
    if (Array.isArray(pos2) && (isNaN(pos2[0]) || isNaN(pos2[1]))) return Infinity
    
    const pixel1 = map.lngLatToPixel(pos1)
    const pixel2 = map.lngLatToPixel(pos2)
    
    if (!pixel1 || !pixel2 || isNaN(pixel1.x) || isNaN(pixel1.y) || isNaN(pixel2.x) || isNaN(pixel2.y)) {
      return Infinity
    }
    
    return Math.sqrt(Math.pow(pixel1.x - pixel2.x, 2) + Math.pow(pixel1.y - pixel2.y, 2))
  } catch (e) {
    console.warn('计算像素距离失败:', e, pos1, pos2)
    return Infinity
  }
}

// 根据地图缩放级别和视野范围，智能显示事件标注
const updateEventMarkers = () => {
  if (!map || allEvents.length === 0) return

  // 清除当前可见的标注
  visibleEventMarkers.forEach(marker => map.remove(marker))
  visibleEventMarkers = []

  // 获取当前地图视野范围
  let bounds
  try {
    bounds = map.getBounds()
    if (!bounds) {
      console.warn('无法获取地图边界')
      return
    }
  } catch (e) {
    console.warn('获取地图边界失败:', e)
    return
  }

  // 过滤出视野内的事件
  const visibleEvents = allEvents.filter(event => {
    if (!event.location || event.location.lat == null || event.location.lng == null) {
      return false
    }
    
    // 验证坐标有效性
    const lat = parseFloat(event.location.lat)
    const lng = parseFloat(event.location.lng)
    if (isNaN(lat) || isNaN(lng)) {
      return false
    }
    
    try {
      const pos = [lng, lat]
      return bounds.contains(pos)
    } catch (e) {
      console.warn('检查事件是否在视野内失败:', e, event)
      return false
    }
  })

  if (visibleEvents.length === 0) return

  // 按时间排序
  visibleEvents.sort((a, b) => (a.timestamp || 0) - (b.timestamp || 0))

  // 智能标注：根据缩放级别和事件间距决定显示哪些标注
  const displayedEvents = []
  let lastDisplayedPos = null // 使用数组格式 [lng, lat]

  visibleEvents.forEach(event => {
    // 确保坐标格式正确
    const lng = parseFloat(event.location.lng)
    const lat = parseFloat(event.location.lat)
    
    // 验证坐标有效性
    if (isNaN(lng) || isNaN(lat)) {
      console.warn('无效的事件坐标:', event)
      return
    }
    
    const pos = [lng, lat]
    
    // 如果是第一个事件，或者与上一个标注的距离足够远，则显示
    if (lastDisplayedPos === null || 
        getPixelDistance(lastDisplayedPos, pos) >= MIN_EVENT_DISTANCE) {
      displayedEvents.push(event)
      lastDisplayedPos = [lng, lat] // 保存为数组格式
    }
  })

  // 创建标注
  displayedEvents.forEach(event => {
    // 确保坐标格式正确
    const lng = parseFloat(event.location.lng)
    const lat = parseFloat(event.location.lat)
    
    if (isNaN(lng) || isNaN(lat)) {
      console.warn('创建标注时发现无效坐标:', event)
      return
    }
    
    const pos = [lng, lat]
    
    let color = '#ffd700'
    let size = 12
    if (event.level === 'Level 3') {
      color = '#ff0000'
      size = 16
    } else if (event.level === 'Level 2') {
      color = '#ff8c00'
      size = 14
    }

    // 行为名称映射
    const behaviorNames = {
      'eyes_closed': '闭眼',
      'yarning': '打哈欠',
      'head_down': '低头',
      'seeing_left': '左看',
      'seeing_right': '右看'
    }
    const behaviorName = behaviorNames[event.behavior] || event.behavior || '未知行为'

    // 创建标注图标（带文字标签）
    // 使用HTML内容创建更丰富的标注
    const timeStr = formatTime(event.timestamp)
    
    // 创建自定义HTML标记
    const marker = new AMap.Marker({
      position: pos,
      content: `
        <div style="position: relative; cursor: pointer;">
          <div style="
            width: ${size}px;
            height: ${size}px;
            background: ${color};
            border: 2px solid #fff;
            border-radius: 50%;
            box-shadow: 0 0 10px ${color};
            position: absolute;
            top: -${size/2}px;
            left: -${size/2}px;
            z-index: 10;
          "></div>
          <div style="
            position: absolute;
            top: ${size/2 + 5}px;
            left: -70px;
            width: 140px;
            background: rgba(0, 0, 0, 0.9);
            border: 2px solid ${color};
            border-radius: 4px;
            padding: 6px 8px;
            font-size: 11px;
            color: #fff;
            white-space: nowrap;
            box-shadow: 0 2px 8px rgba(0,0,0,0.6);
            pointer-events: none;
            z-index: 20;
          ">
            <div style="font-weight: bold; color: ${color}; margin-bottom: 3px; font-size: 12px;">${behaviorName}</div>
            <div style="font-size: 10px; color: #ccc;">${timeStr}</div>
          </div>
        </div>
      `,
      offset: new AMap.Pixel(0, 0),
      zIndex: 200,
      map: map
    })

    // 创建信息窗口内容
    marker.on('click', () => {
      const infoWindow = new AMap.InfoWindow({
        content: `
          <div style="color: #fff; padding: 10px; min-width: 220px; background: rgba(0,0,0,0.8); border: 1px solid #00f2ff;">
            <div style="font-weight: bold; margin-bottom: 6px; color: #00f2ff; font-size: 14px;">${currentDriverName || '未知'}</div>
            <div style="font-size: 12px; color: #ccc; margin-bottom: 4px;">
              <span style="color: #94a3b8;">行为:</span> <span style="color: #fff;">${behaviorName}</span>
            </div>
            <div style="font-size: 12px; color: #ccc; margin-bottom: 4px;">
              <span style="color: #94a3b8;">级别:</span> <span style="color: ${color};">${event.level || 'Normal'}</span>
            </div>
            <div style="font-size: 12px; color: #ccc; margin-bottom: 4px;">
              <span style="color: #94a3b8;">时间:</span> <span style="color: #fff;">${formatTime(event.timestamp)}</span>
            </div>
            ${event.score ? `<div style="font-size: 12px; color: #ccc;">
              <span style="color: #94a3b8;">分数:</span> <span style="color: #fff;">${event.score.toFixed(1)}</span>
            </div>` : ''}
          </div>
        `,
        offset: new AMap.Pixel(0, -10)
      })
      infoWindow.open(map, marker.getPosition())
    })

    visibleEventMarkers.push(marker)
  })

  console.log(`已显示 ${visibleEventMarkers.length} 个事件标注（共 ${visibleEvents.length} 个事件在视野内）`)
}

// 显示驾驶员事件
const showDriverEventsOnMap = async (driverId, driverName, deviceId) => {
  if (!driverId || !map) {
    alert('无法获取驾驶员信息')
    return
  }

  if (!deviceId) {
    alert('无法获取设备信息')
    return
  }

  try {
    // 清除之前的轨迹和标注
    if (trackPolyline) {
      map.remove(trackPolyline)
      trackPolyline = null
    }
    visibleEventMarkers.forEach(marker => map.remove(marker))
    visibleEventMarkers = []
    allEvents = []
    
    // 清除之前的地图事件监听器
    mapEventHandlers.forEach(handler => {
      map.off('zoomend', handler)
      map.off('moveend', handler)
    })
    mapEventHandlers = []
    
    // 设置当前驾驶员名称
    currentDriverName = driverName || '未知'

    // 获取该驾驶员的所有事件
    const alertsData = await getRealtimeAlerts()
    if (alertsData.code === 200 && alertsData.data && alertsData.data.alerts) {
      const driverEvents = alertsData.data.alerts.filter(a => a.driverId === driverId)

      if (driverEvents.length === 0) {
        alert(`未找到驾驶员 ${driverName} 的最近事件`)
        return
      }

      console.log(`找到 ${driverEvents.length} 个驾驶员事件`)
      
      // 存储所有事件，并确保坐标格式正确
      allEvents = driverEvents.map(event => {
        let location = event.location || null
        
        // 确保坐标格式正确
        if (location) {
          let lat = location.lat
          let lng = location.lng
          
          // 处理可能的对象类型坐标
          if (typeof lat === 'object' && lat !== null) {
            lat = parseFloat(lat.toString())
          }
          if (typeof lng === 'object' && lng !== null) {
            lng = parseFloat(lng.toString())
          }
          
          // 确保是数字类型
          lat = typeof lat === 'number' ? lat : parseFloat(lat)
          lng = typeof lng === 'number' ? lng : parseFloat(lng)
          
          // 验证坐标有效性
          if (isNaN(lat) || isNaN(lng)) {
            console.warn('无效的事件坐标:', event)
            location = null
          } else {
            location = { lat, lng }
          }
        }
        
        return {
          ...event,
          location: location,
          timestamp: event.timestamp || Date.now(),
          behavior: event.behavior || '未知',
          level: event.level || 'Normal',
          score: event.score || 0
        }
      }).filter(event => event.location !== null) // 过滤掉没有有效坐标的事件
      
      console.log(`有效事件数量: ${allEvents.length} (已过滤 ${driverEvents.length - allEvents.length} 个无效坐标事件)`)

      // 确定时间范围（事件时间的前后各扩展30分钟）
      const timestamps = allEvents
        .filter(e => e.timestamp)
        .map(e => e.timestamp)
        .sort((a, b) => a - b)
      
      if (timestamps.length === 0) {
        alert('事件数据中没有时间信息')
        return
      }

      const startTime = timestamps[0] - 30 * 60 * 1000 // 提前30分钟
      const endTime = timestamps[timestamps.length - 1] + 30 * 60 * 1000 // 延后30分钟

      // 获取轨迹数据
      const trackData = await getMapTrack({
        deviceId,
        startTime,
        endTime
      })

      if (trackData.code === 200 && trackData.data && trackData.data.track) {
        const track = trackData.data.track
        
        if (track.length > 0) {
          // 绘制轨迹
          const path = track
            .map(p => {
              if (p.location && p.location.lng && p.location.lat) {
                return [p.location.lng, p.location.lat]
              }
              return null
            })
            .filter(p => p !== null)

          if (path.length > 0) {
            trackPolyline = new AMap.Polyline({
              path: path,
              strokeColor: '#00f2ff',
              strokeWeight: 3,
              strokeOpacity: 0.8,
              zIndex: 100
            })
            map.add(trackPolyline)

            // 调整地图视野以包含轨迹和事件
            const allPositions = [...path]
            allEvents.forEach(event => {
              if (event.location && event.location.lat != null && event.location.lng != null) {
                allPositions.push([event.location.lng, event.location.lat])
              }
            })

            if (allPositions.length > 0) {
              map.setFitView([trackPolyline, ...allPositions.map(pos => new AMap.Marker({ position: pos }))], false, [50, 50, 50, 50])
            }
          }
        }
      }

      // 显示事件标注
      updateEventMarkers()

      // 监听地图缩放和移动，动态更新标注
      const updateHandler = () => {
        updateEventMarkers()
      }
      
      // 添加新的监听器
      map.on('zoomend', updateHandler)
      map.on('moveend', updateHandler)
      
      // 存储监听器以便后续清除
      mapEventHandlers.push(updateHandler)

      console.log(`已加载轨迹和 ${allEvents.length} 个事件`)
    } else {
      alert('获取事件数据失败')
    }
  } catch (error) {
    console.error('获取驾驶员事件失败:', error)
    alert('获取事件数据失败，请稍后重试: ' + error.message)
  }
}

let dataTimer = null

const startDataUpdate = () => {
  fetchRealtimeData()
  fetchChartData()

  dataTimer = setInterval(() => {
    fetchRealtimeData()
    fetchChartData()
    updateTrendChart()
    updateRadarChart()
    updateRegionChart()
  }, 20000)
}

onMounted(() => {
  initMap()
  initCharts()
  startDataUpdate()
})

onUnmounted(() => {
  if (dataTimer) clearInterval(dataTimer)
  if (map) map.destroy()
  trendChart?.dispose()
  radarChart?.dispose()
  regionChart?.dispose()
})
</script>

<style scoped>
/* 使用全局样式 */
</style>



