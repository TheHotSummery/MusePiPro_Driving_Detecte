<template>
  <div class="fatigue-analysis-chart">
    <div class="chart-header">
      <h3>疲劳行为分析</h3>
      <el-select v-model="selectedType" @change="onTypeChange" size="small">
        <el-option label="小时分布" value="hourly" />
        <el-option label="严重程度" value="severity" />
        <el-option label="行为类型" value="behavior" />
      </el-select>
    </div>
    <v-chart 
      class="chart" 
      :option="chartOption" 
      :loading="loading"
      autoresize
      :canvas-id="'fatigue-analysis-chart'"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  BarChart,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
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

const emit = defineEmits(['type-change'])

const selectedType = ref('hourly')

// 图表配置
const chartOption = computed(() => {
  switch (selectedType.value) {
    case 'hourly':
      return getHourlyChartOption()
    case 'severity':
      return getSeverityChartOption()
    case 'behavior':
      return getBehaviorChartOption()
    default:
      return getHourlyChartOption()
  }
})

// 小时分布图表
const getHourlyChartOption = () => {
  const hourlyData = getHourlyData()
  
  return {
    title: {
      text: '疲劳事件小时分布',
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
        type: 'shadow',
        shadowStyle: {
          color: 'rgba(0, 0, 0, 0.1)'
        }
      },
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: '#ccc',
      borderWidth: 1,
      textStyle: {
        color: '#fff'
      },
      formatter: function(params) {
        const param = params[0]
        return `<div style="color: #fff; font-weight: bold;">${param.axisValue}:00</div>
                <div style="margin-top: 4px; color: #fff;">
                  <span style="display: inline-block; width: 12px; height: 12px; background-color: ${param.color}; border-radius: 50%; margin-right: 8px;"></span>
                  疲劳事件: <span style="font-weight: bold;">${param.value}</span>
                </div>`
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
      data: hourlyData.labels,
      axisLabel: {
        formatter: '{value}:00',
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
    series: [{
      name: '疲劳事件',
      type: 'bar',
      data: hourlyData.values,
      itemStyle: {
        color: '#FF8C00',
        borderRadius: [4, 4, 0, 0]
      },
      emphasis: {
        itemStyle: {
          color: '#FF7F00',
          shadowBlur: 10,
          shadowColor: '#FF8C00'
        }
      },
      // 添加数值标注
      label: {
        show: true,
        position: 'top',
        formatter: '{c}',
        fontSize: 10,
        fontWeight: 'bold',
        color: '#FF8C00'
      }
    }]
  }
}

// 严重程度图表
const getSeverityChartOption = () => {
  const severityData = getSeverityData()
  
  return {
    title: {
      text: '疲劳事件严重程度分布',
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'bold',
        color: '#303133'
      }
    },
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: '#ccc',
      borderWidth: 1,
      textStyle: {
        color: '#fff'
      },
      formatter: function(params) {
        return `<div style="color: #fff; font-weight: bold;">${params.name}</div>
                <div style="margin-top: 4px; color: #fff;">
                  <span style="display: inline-block; width: 12px; height: 12px; background-color: ${params.color}; border-radius: 50%; margin-right: 8px;"></span>
                  数量: <span style="font-weight: bold;">${params.value}</span>
                </div>
                <div style="color: #fff; margin-top: 2px;">占比: <span style="font-weight: bold;">${params.percent}%</span></div>`
      }
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle',
      textStyle: {
        fontSize: 12,
        fontWeight: 'bold'
      }
    },
    series: [{
      name: '严重程度',
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['60%', '50%'],
      data: severityData,
      emphasis: {
        itemStyle: {
          shadowBlur: 15,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.3)'
        },
        label: {
          show: true,
          fontSize: 14,
          fontWeight: 'bold'
        }
      },
      label: {
        show: true,
        formatter: '{b}\n{c} ({d}%)',
        fontSize: 11,
        fontWeight: 'bold'
      },
      labelLine: {
        show: true,
        lineStyle: {
          width: 2
        }
      }
    }]
  }
}

// 行为类型图表
const getBehaviorChartOption = () => {
  const behaviorData = getBehaviorData()
  
  return {
    title: {
      text: '疲劳行为类型分布',
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
        type: 'shadow',
        shadowStyle: {
          color: 'rgba(0, 0, 0, 0.1)'
        }
      },
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: '#ccc',
      borderWidth: 1,
      textStyle: {
        color: '#fff'
      },
      formatter: function(params) {
        const param = params[0]
        return `<div style="color: #fff; font-weight: bold;">${param.axisValue}</div>
                <div style="margin-top: 4px; color: #fff;">
                  <span style="display: inline-block; width: 12px; height: 12px; background-color: ${param.color}; border-radius: 50%; margin-right: 8px;"></span>
                  行为类型: <span style="font-weight: bold;">${param.value}</span>
                </div>`
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
      data: behaviorData.labels,
      axisLabel: {
        color: '#666',
        fontSize: 11,
        rotate: 45
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
    series: [{
      name: '行为类型',
      type: 'bar',
      data: behaviorData.values,
      itemStyle: {
        color: '#409EFF',
        borderRadius: [4, 4, 0, 0]
      },
      emphasis: {
        itemStyle: {
          color: '#337ECC',
          shadowBlur: 10,
          shadowColor: '#409EFF'
        }
      },
      // 添加数值标注
      label: {
        show: true,
        position: 'top',
        formatter: '{c}',
        fontSize: 10,
        fontWeight: 'bold',
        color: '#409EFF'
      }
    }]
  }
}

// 获取小时分布数据
const getHourlyData = () => {
  const hourlyCount = new Array(24).fill(0)
  
  props.data.forEach(event => {
    if (event.eventType === 'FATIGUE') {
      const hour = new Date(event.timestamp).getHours()
      hourlyCount[hour]++
    }
  })
  
  return {
    labels: Array.from({ length: 24 }, (_, i) => i),
    values: hourlyCount
  }
}

// 获取严重程度数据
const getSeverityData = () => {
  const severityCount = {
    LOW: 0,
    MEDIUM: 0,
    HIGH: 0,
    CRITICAL: 0
  }
  
  props.data.forEach(event => {
    if (event.eventType === 'FATIGUE' && severityCount.hasOwnProperty(event.severity)) {
      severityCount[event.severity]++
    }
  })
  
  const colors = {
    LOW: '#67C23A',
    MEDIUM: '#E6A23C',
    HIGH: '#F56C6C',
    CRITICAL: '#F56C6C'
  }
  
  const severityNames = {
    LOW: '轻微',
    MEDIUM: '中等',
    HIGH: '严重',
    CRITICAL: '危急'
  }
  
  return Object.entries(severityCount)
    .filter(([_, count]) => count > 0)
    .map(([severity, count]) => ({
      value: count,
      name: severityNames[severity],
      itemStyle: {
        color: colors[severity]
      }
    }))
}

// 获取行为类型数据
const getBehaviorData = () => {
  const behaviorCount = {}
  
  props.data.forEach(event => {
    if (event.eventType === 'FATIGUE' && event.behavior) {
      behaviorCount[event.behavior] = (behaviorCount[event.behavior] || 0) + 1
    }
  })
  
  const behaviorNames = {
    EYE_CLOSURE: '闭眼',
    YAWNING: '打哈欠',
    HEAD_NODDING: '点头',
    DISTRACTED: '分心',
    SLEEPY: '困倦'
  }
  
  return {
    labels: Object.keys(behaviorCount).map(behavior => behaviorNames[behavior] || behavior),
    values: Object.values(behaviorCount)
  }
}

// 类型变化处理
const onTypeChange = (type) => {
  emit('type-change', type)
}

// 监听数据变化
watch(() => props.data, () => {
  // 数据变化时重新计算图表
}, { deep: true })
</script>

<style scoped lang="scss">
.fatigue-analysis-chart {
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

