<template>
  <div class="min-h-screen p-8">
    <!-- 头部 -->
    <div class="flex justify-between items-end mb-8">
      <div>
        <router-link to="/" class="text-cyan-400 hover:text-white flex items-center gap-2 mb-2 transition">
          <i class="ri-arrow-left-line"></i> 返回大屏
        </router-link>
        <h1 class="text-3xl font-bold text-white">运营数据统计报表</h1>
        <p class="text-gray-400 text-sm mt-1">统计周期: {{ periodText }}</p>
      </div>
      <button class="bg-cyan-600 hover:bg-cyan-500 text-white px-6 py-2 rounded font-bold flex items-center gap-2 transition">
        <i class="ri-download-line"></i> 导出 PDF 报告
      </button>
    </div>

    <!-- 主要内容网格 -->
    <div class="grid grid-cols-12 gap-6">
      
      <!-- 1. 总体概览 (占据一整行) -->
      <div class="col-span-12 grid grid-cols-4 gap-6 mb-2">
        <div class="tech-card p-6 flex items-center justify-between">
          <div>
            <div class="text-gray-400 text-xs uppercase">总行驶里程</div>
            <div class="text-2xl font-bold text-white mt-1">
              {{ formatNumber(summaryData.totalDistance) }} <span class="text-sm font-normal text-gray-500">km</span>
            </div>
          </div>
          <i class="ri-road-map-line text-3xl text-cyan-500"></i>
        </div>
        <div class="tech-card p-6 flex items-center justify-between">
          <div>
            <div class="text-gray-400 text-xs uppercase">疲劳告警总数</div>
            <div class="text-2xl font-bold text-red-400 mt-1">
              {{ summaryData.totalEvents }} <span class="text-sm font-normal text-gray-500">次</span>
            </div>
          </div>
          <i class="ri-alarm-warning-line text-3xl text-red-500"></i>
        </div>
        <div class="tech-card p-6 flex items-center justify-between">
          <div>
            <div class="text-gray-400 text-xs uppercase">平均安全评分</div>
            <div class="text-2xl font-bold text-green-400 mt-1">{{ summaryData.avgSafetyScore.toFixed(1) }}</div>
          </div>
          <i class="ri-shield-check-line text-3xl text-green-500"></i>
        </div>
        <div class="tech-card p-6 flex items-center justify-between">
          <div>
            <div class="text-gray-400 text-xs uppercase">车队在线率</div>
            <div class="text-2xl font-bold text-blue-400 mt-1">{{ summaryData.onlineRate.toFixed(1) }}%</div>
          </div>
          <i class="ri-signal-tower-line text-3xl text-blue-500"></i>
        </div>
      </div>

      <!-- 2. 月度趋势图 (占8列) -->
      <div class="col-span-8 tech-card p-6 min-h-[400px]">
        <h3 class="text-lg font-bold text-white mb-4 border-l-4 border-cyan-400 pl-3">疲劳事件月度趋势分析</h3>
        <div ref="lineChartRef" class="w-full h-[320px]"></div>
      </div>

      <!-- 3. 告警类型占比 (占4列) -->
      <div class="col-span-4 tech-card p-6 min-h-[400px]">
        <h3 class="text-lg font-bold text-white mb-4 border-l-4 border-cyan-400 pl-3">违规行为类型占比</h3>
        <div ref="pieChartRef" class="w-full h-[320px]"></div>
      </div>

      <!-- 4. 驾驶员排行榜 (占12列) -->
      <div class="col-span-12 tech-card p-6">
        <h3 class="text-lg font-bold text-white mb-4 border-l-4 border-cyan-400 pl-3">驾驶员安全绩效排行榜 (Top 10)</h3>
        <table class="tech-table">
          <thead>
            <tr>
              <th>排名</th>
              <th>姓名</th>
              <th>工号</th>
              <th>所属车队</th>
              <th>行驶时长 (h)</th>
              <th>告警次数</th>
              <th>安全评分</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(d, index) in drivers" :key="index">
              <td>
                <span
                  v-if="index < 3"
                  class="w-6 h-6 rounded-full bg-yellow-500/20 text-yellow-500 flex items-center justify-center text-xs font-bold"
                >{{ index + 1 }}</span>
                <span v-else class="text-gray-500 pl-2">{{ index + 1 }}</span>
              </td>
              <td class="font-bold">{{ d.name }}</td>
              <td class="text-gray-400 font-mono">{{ d.id }}</td>
              <td>{{ d.team }}</td>
              <td>{{ d.hours }}</td>
              <td :class="d.alerts > 5 ? 'text-red-400' : 'text-gray-300'">{{ d.alerts }}</td>
              <td>
                <span class="score-badge" :class="getScoreClass(d.score)">{{ d.score }}</span>
              </td>
              <td>
                <button class="text-cyan-400 hover:text-white text-xs underline">查看详情</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import {
  getDriverStatistics,
  getEventStatistics,
  getRealtimeSystem,
  getTrendChart,
  getBehaviorDistributionChart
} from '@/api'
import { formatNumber, getScoreClass } from '@/utils'

const drivers = ref([])
const summaryData = ref({
  totalDistance: 0,
  totalEvents: 0,
  avgSafetyScore: 0,
  onlineRate: 0
})
const trendData = ref([])
const behaviorData = ref([])
const periodText = ref('最近30天')

const lineChartRef = ref(null)
const pieChartRef = ref(null)
let lineChart = null
let pieChart = null

const fetchData = async () => {
  try {
    const now = Date.now()
    const startTime = now - 30 * 24 * 60 * 60 * 1000

    const driversData = await getDriverStatistics({
      startTime,
      endTime: now
    })
    if (driversData.code === 200 && driversData.data) {
      drivers.value = (driversData.data.drivers || []).slice(0, 10).map(d => ({
        name: d.driverName || '未知',
        id: d.driverId || '',
        team: d.teamName || '未分配',
        hours: Math.floor((d.statistics?.totalDuration || 0) / 3600),
        alerts: d.statistics?.totalEvents || 0,
        score: d.statistics?.safetyScore || 0
      }))

      const totalDrivers = driversData.data.drivers?.length || 0
      if (totalDrivers > 0) {
        summaryData.value.totalDistance = driversData.data.drivers.reduce(
          (sum, d) => sum + (d.statistics?.totalDistance || 0),
          0
        )
        summaryData.value.totalEvents = driversData.data.drivers.reduce(
          (sum, d) => sum + (d.statistics?.totalEvents || 0),
          0
        )
        summaryData.value.avgSafetyScore =
          driversData.data.drivers.reduce((sum, d) => sum + (d.statistics?.safetyScore || 0), 0) /
          totalDrivers
      }
    }

    const eventsData = await getEventStatistics({
      startTime,
      endTime: now
    })
    if (eventsData.code === 200 && eventsData.data) {
      summaryData.value.totalEvents = eventsData.data.summary?.totalEvents || 0
    }

    const systemData = await getRealtimeSystem()
    if (systemData.code === 200 && systemData.data) {
      const total = systemData.data.totalDevices || 1
      const online = systemData.data.healthyDevices || 0
      summaryData.value.onlineRate = (online / total) * 100
    }

    const trendData_res = await getTrendChart({
      startTime,
      endTime: now,
      interval: 'day'
    })
    if (trendData_res.code === 200 && trendData_res.data) {
      trendData.value = trendData_res.data.series?.[0]?.data || []
    }

    const behaviorData_res = await getBehaviorDistributionChart({
      startTime,
      endTime: now
    })
    if (behaviorData_res.code === 200 && behaviorData_res.data) {
      behaviorData.value = behaviorData_res.data.distribution || []
    }

    initCharts()
  } catch (error) {
    console.error('获取数据失败:', error)
  }
}

const initCharts = () => {
  if (lineChartRef.value) {
    lineChart = echarts.init(lineChartRef.value)
    const xData = trendData.value.map(d => {
      const date = new Date(d.timestamp)
      return `${date.getMonth() + 1}/${date.getDate()}`
    })
    const yData = trendData.value.map(d => d.value || 0)

    lineChart.setOption({
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(0,0,0,0.8)',
        textStyle: { color: '#fff' }
      },
      xAxis: {
        type: 'category',
        data: xData.length > 0 ? xData : ['1日', '5日', '10日', '15日', '20日', '25日', '30日'],
        axisLine: { lineStyle: { color: '#475569' } }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: '#1e293b' } }
      },
      series: [{
        data: yData.length > 0 ? yData : [12, 15, 8, 24, 18, 10, 5],
        type: 'line',
        smooth: true,
        itemStyle: { color: '#00f2ff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0,242,255,0.5)' },
            { offset: 1, color: 'rgba(0,242,255,0)' }
          ])
        }
      }]
    })
  }

  if (pieChartRef.value) {
    pieChart = echarts.init(pieChartRef.value)
    const pieData = behaviorData.value.map(b => ({
      value: b.count || 0,
      name: b.label || b.behavior || '未知',
      itemStyle: {
        color: ['#ff4d4f', '#fbbf24', '#34d399', '#60a5fa', '#a855f7'][behaviorData.value.indexOf(b) % 5]
      }
    }))

    pieChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: '0%', textStyle: { color: '#fff' } },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        itemStyle: { borderRadius: 5, borderColor: '#0f172a', borderWidth: 2 },
        data:
          pieData.length > 0
            ? pieData
            : [
                { value: 40, name: '闭眼 (Fatigue)', itemStyle: { color: '#ff4d4f' } },
                { value: 30, name: '低头 (Distraction)', itemStyle: { color: '#fbbf24' } },
                { value: 20, name: '打哈欠 (Yarning)', itemStyle: { color: '#34d399' } },
                { value: 10, name: '其他', itemStyle: { color: '#60a5fa' } }
              ]
      }]
    })
  }

  window.addEventListener('resize', () => {
    lineChart?.resize()
    pieChart?.resize()
  })
}

onMounted(() => {
  fetchData()
})

onUnmounted(() => {
  lineChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
/* 使用全局样式 */
</style>













