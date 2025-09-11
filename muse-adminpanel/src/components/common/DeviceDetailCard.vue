<template>
  <el-card class="device-detail-card" shadow="hover">
    <template #header>
      <div class="card-header">
        <span>设备详情</span>
        <el-tag :type="getStatusType(device.status)" size="small">
          {{ getStatusText(device.status) }}
        </el-tag>
      </div>
    </template>
    
    <div class="device-info">
      <!-- 基本信息 -->
      <div class="info-section">
        <h4>基本信息</h4>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">设备ID:</span>
            <span class="value">{{ device.deviceId }}</span>
          </div>
          <div class="info-item">
            <span class="label">设备类型:</span>
            <span class="value">{{ device.deviceType }}</span>
          </div>
          <div class="info-item">
            <span class="label">版本:</span>
            <span class="value">{{ device.version }}</span>
          </div>
          <div class="info-item">
            <span class="label">网络健康状态:</span>
            <span class="value">
              <el-progress 
                :percentage="device.healthScore || 0" 
                :color="getHealthColor(device.healthScore)"
                :stroke-width="8"
                :show-text="true"
              />
            </span>
          </div>
        </div>
      </div>

      <!-- 用户信息 -->
      <div class="info-section" v-if="device.user">
        <h4>用户信息</h4>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">用户名:</span>
            <span class="value">{{ device.user.username }}</span>
          </div>
          <div class="info-item">
            <span class="label">邮箱:</span>
            <span class="value">{{ device.user.email }}</span>
          </div>
          <div class="info-item">
            <span class="label">手机:</span>
            <span class="value">{{ device.user.phone }}</span>
          </div>
        </div>
      </div>

      <!-- 位置信息 -->
      <div class="info-section" v-if="device.currentLocation">
        <h4>位置信息</h4>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">经度:</span>
            <span class="value">{{ device.currentLocation.lng?.toFixed(6) }}</span>
          </div>
          <div class="info-item">
            <span class="label">纬度:</span>
            <span class="value">{{ device.currentLocation.lat?.toFixed(6) }}</span>
          </div>
          <div class="info-item">
            <span class="label">海拔:</span>
            <span class="value">{{ device.currentLocation.altitude?.toFixed(2) }} m</span>
          </div>
          <div class="info-item">
            <span class="label">精度:</span>
            <span class="value">{{ device.currentLocation.hdop?.toFixed(2) }}</span>
          </div>
        </div>
      </div>

      <!-- 实时信息（仅在线设备显示） -->
      <div class="info-section" v-if="isOnline && device.currentLocation">
        <h4>实时信息</h4>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">速度:</span>
            <span class="value speed-value">{{ device.currentLocation.speed?.toFixed(1) }} km/h</span>
          </div>
          <div class="info-item">
            <span class="label">方向:</span>
            <span class="value direction-value">{{ device.currentLocation.direction?.toFixed(1) }}°</span>
          </div>
          <div class="info-item">
            <span class="label">卫星数:</span>
            <span class="value satellite-value">{{ device.currentLocation.satellites }}</span>
          </div>
          <div class="info-item">
            <span class="label">最后更新:</span>
            <span class="value">{{ formatLastSeen(device.lastSeen) }}</span>
          </div>
        </div>
      </div>

      <!-- 离线提示 -->
      <div class="info-section" v-if="!isOnline">
        <el-alert
          title="设备离线"
          description="设备当前处于离线状态，实时信息不可用"
          type="warning"
          :closable="false"
          show-icon
        />
      </div>

      <!-- 统计信息 -->
      <div class="info-section" v-if="device.statistics">
        <h4>统计信息</h4>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">疲劳事件:</span>
            <span class="value">{{ device.statistics.fatigueEvents || 0 }}</span>
          </div>
          <div class="info-item">
            <span class="label">分心事件:</span>
            <span class="value">{{ device.statistics.distractionEvents || 0 }}</span>
          </div>
          <div class="info-item">
            <span class="label">总驾驶时间:</span>
            <span class="value">{{ formatDuration(device.statistics.totalDrivingTime) }}</span>
          </div>
          <div class="info-item">
            <span class="label">总行驶距离:</span>
            <span class="value">{{ device.statistics.totalDistance?.toFixed(2) || 0 }} km</span>
          </div>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  device: {
    type: Object,
    required: true
  }
})

// 计算属性
const isOnline = computed(() => props.device.status === 'ONLINE')

// 方法
const getStatusType = (status) => {
  const typeMap = {
    ONLINE: 'success',
    OFFLINE: 'info',
    LOST: 'danger'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status) => {
  const textMap = {
    ONLINE: '在线',
    OFFLINE: '离线',
    LOST: '失联'
  }
  return textMap[status] || '未知'
}

const getHealthColor = (score) => {
  if (score >= 80) return '#67C23A'
  if (score >= 60) return '#E6A23C'
  return '#F56C6C'
}

const formatLastSeen = (lastSeen) => {
  if (!lastSeen) return '未知'
  
  const date = new Date(lastSeen)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  return date.toLocaleDateString()
}

const formatDuration = (seconds) => {
  if (!seconds) return '0分钟'
  
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  
  if (hours > 0) {
    return `${hours}小时${minutes}分钟`
  }
  return `${minutes}分钟`
}
</script>

<style scoped lang="scss">
.device-detail-card {
  margin-bottom: 20px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .device-info {
    .info-section {
      margin-bottom: 20px;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      h4 {
        margin: 0 0 12px 0;
        color: #303133;
        font-size: 14px;
        font-weight: 600;
        border-bottom: 1px solid #EBEEF5;
        padding-bottom: 8px;
      }
      
      .info-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 12px;
        
        .info-item {
          display: flex;
          align-items: center;
          
          .label {
            color: #606266;
            font-size: 13px;
            margin-right: 8px;
            min-width: 80px;
          }
          
          .value {
            color: #303133;
            font-size: 13px;
            font-weight: 500;
            
            &.speed-value {
              color: #67C23A;
              font-weight: 600;
            }
            
            &.direction-value {
              color: #409EFF;
              font-weight: 600;
            }
            
            &.satellite-value {
              color: #E6A23C;
              font-weight: 600;
            }
          }
        }
      }
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .device-detail-card {
    .device-info {
      .info-section {
        .info-grid {
          grid-template-columns: 1fr;
        }
      }
    }
  }
}
</style>

