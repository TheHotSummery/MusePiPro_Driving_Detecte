<template>
  <div class="event-trend-chart">
    <div class="chart-header">
      <h3>事件趋势分析</h3>
      <el-select v-model="selectedPeriod" @change="onPeriodChange" size="small">
        <el-option label="今日" value="today" />
        <el-option label="本周" value="week" />
        <el-option label="本月" value="month" />
      </el-select>
    </div>
    <v-chart 
      class="chart" 
      :option="chartOption" 
      :loading="loading"
      autoresize
      :canvas-id="'event-trend-chart'"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DataZoomComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DataZoomComponent
])

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['period-change'])

const selectedPeriod = ref('today')

// 图表配置
const chartOption = computed(() => {
  const eventTypes = ['FATIGUE', 'DISTRACTION', 'EMERGENCY']
  // 增强颜色对比度
  const colors = ['#FF8C00', '#DC143C', '#32CD32'] // 橙色、深红、绿色
  
  // 处理数据
  const processedData = processData()
  
  return {
    title: {
      text: '事件趋势',
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'bold',
        color: '#303133'
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        lineStyle: {
          color: '#999',
          width: 1
        }
      },
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: '#ccc',
      borderWidth: 1,
      textStyle: {
        color: '#fff'
      },
      formatter: function(params) {
        let result = `<div style="margin-bottom: 8px; font-weight: bold; font-size: 14px; color: #fff;">${params[0].axisValue}</div>`
        params.forEach(param => {
          const value = param.value || 0
          result += `<div style="margin: 4px 0; display: flex; align-items: center;">
            <span style="display: inline-block; width: 12px; height: 12px; background-color: ${param.color}; border-radius: 50%; margin-right: 8px;"></span>
            <span style="color: #fff; font-weight: 500;">${param.seriesName}:</span>
            <span style="color: #fff; font-weight: bold; margin-left: 8px;">${value}</span>
          </div>`
        })
        return result
      }
    },
    legend: {
      data: ['疲劳事件', '分心事件', '紧急事件'],
      top: 30,
      textStyle: {
        fontSize: 12,
        fontWeight: 'bold'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '20%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: processedData.timeLabels,
      axisLabel: {
        rotate: 45,
        color: '#666',
        fontSize: 11
      },
      axisLine: {
        lineStyle: {
          color: '#ddd'
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '事件数量',
      nameTextStyle: {
        color: '#666',
        fontSize: 12
      },
      axisLabel: {
        formatter: '{value}',
        color: '#666',
        fontSize: 11
      },
      axisLine: {
        lineStyle: {
          color: '#ddd'
        }
      },
      splitLine: {
        lineStyle: {
          color: '#f0f0f0',
          type: 'dashed'
        }
      }
    },
    dataZoom: [
      {
        type: 'inside',
        start: 0,
        end: 100
      }
    ],
    series: eventTypes.map((type, index) => ({
      name: getEventTypeName(type),
      type: 'line',
      data: processedData.seriesData[type] || [],
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: {
        width: 4
      },
      itemStyle: {
        color: colors[index],
        borderColor: '#fff',
        borderWidth: 2
      },
      areaStyle: {
        opacity: 0.2,
        color: colors[index]
      },
      // 添加数值标注
      label: {
        show: false,
        position: 'top',
        formatter: '{c}',
        fontSize: 10,
        fontWeight: 'bold',
        color: colors[index]
      },
      // 高亮时的样式
      emphasis: {
        focus: 'series',
        itemStyle: {
          borderColor: '#fff',
          borderWidth: 3,
          shadowBlur: 10,
          shadowColor: colors[index]
        }
      }
    }))
  }
})

// 处理数据
const processData = () => {
  if (!props.data.length) {
    return {
      timeLabels: [],
      seriesData: {}
    }
  }

  // 按时间分组数据
  const timeGroups = {}
  const eventTypes = ['FATIGUE', 'DISTRACTION', 'EMERGENCY']
  
  props.data.forEach(event => {
    const timeKey = formatTimeKey(event.timestamp)
    if (!timeGroups[timeKey]) {
      timeGroups[timeKey] = {}
    }
    if (!timeGroups[timeKey][event.eventType]) {
      timeGroups[timeKey][event.eventType] = 0
    }
    timeGroups[timeKey][event.eventType]++
  })

  // 生成时间标签和数据
  const timeLabels = Object.keys(timeGroups).sort()
  const seriesData = {}
  
  eventTypes.forEach(type => {
    seriesData[type] = timeLabels.map(time => timeGroups[time]?.[type] || 0)
  })

  return {
    timeLabels,
    seriesData
  }
}

// 格式化时间键
const formatTimeKey = (timestamp) => {
  const date = new Date(timestamp)
  
  switch (selectedPeriod.value) {
    case 'today':
      return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
    case 'week':
      return `${date.getMonth() + 1}/${date.getDate()}`
    case 'month':
      return `${date.getMonth() + 1}/${date.getDate()}`
    default:
      return date.toLocaleDateString()
  }
}

// 获取事件类型名称
const getEventTypeName = (type) => {
  const typeMap = {
    FATIGUE: '疲劳事件',
    DISTRACTION: '分心事件',
    EMERGENCY: '紧急事件'
  }
  return typeMap[type] || type
}

// 周期变化处理
const onPeriodChange = (period) => {
  emit('period-change', period)
}

// 监听数据变化
watch(() => props.data, () => {
  // 数据变化时重新计算图表
}, { deep: true })
</script>

<style scoped lang="scss">
.event-trend-chart {
  width: 100%;
  height: 100%;
  min-height: 350px;
  background: #fff;
  border-radius: 8px;
  padding: 15px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  min-height: 32px;

  h3 {
    margin: 0;
    font-size: 14px;
    color: #303133;
    white-space: nowrap;
    flex-shrink: 0;
  }
}

.chart {
  width: 100%;
  flex: 1;
  min-height: 280px;
}
</style>

