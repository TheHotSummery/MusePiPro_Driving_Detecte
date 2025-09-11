<template>
  <div class="video-stream-container">
    <el-card class="video-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <el-icon><VideoCamera /></el-icon>
          <span>实时视频流</span>
          <div class="video-controls">
            <el-button 
              :type="isPlaying ? 'danger' : 'success'" 
              :icon="isPlaying ? 'VideoPause' : 'VideoPlay'"
              size="small"
              @click="toggleVideo"
            >
              {{ isPlaying ? '暂停' : '播放' }}
            </el-button>
            <el-button 
              type="warning" 
              icon="Refresh" 
              size="small"
              @click="reconnectVideo"
            >
              重连
            </el-button>
          </div>
        </div>
      </template>
      
      <div class="video-wrapper">
        <img 
          ref="videoElement"
          class="video-feed"
          :src="videoUrl"
          alt="视频流"
        />
        
        <!-- 视频状态指示器 -->
        <div class="video-status">
          <el-tag 
            :type="statusType" 
            size="small"
            class="status-tag"
          >
            {{ statusText }}
          </el-tag>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoCamera, VideoPlay, VideoPause, Refresh } from '@element-plus/icons-vue'
import videoService from '@/services/video'
import apiConfig from '@/config/api'

// 响应式数据
const videoElement = ref(null)
const videoUrl = ref(apiConfig.getVideoUrl())

// 计算属性 - 从videoService获取状态
const isPlaying = computed(() => videoService.getStatus().isPlaying)
const hasError = computed(() => videoService.getStatus().hasError)
const retryCount = computed(() => videoService.getStatus().retryCount)

const statusType = computed(() => {
  if (hasError.value) return 'danger'
  if (isPlaying.value) return 'success'
  return 'warning'
})

const statusText = computed(() => {
  if (hasError.value) return '连接失败'
  if (isPlaying.value) return '正常播放'
  return '连接中...'
})

// 方法
const toggleVideo = () => {
  if (isPlaying.value) {
    videoService.stop()
  } else {
    videoService.reconnect()
  }
}

const reconnectVideo = () => {
  videoService.reconnect()
  ElMessage.info('正在重新连接视频流...')
}

// 生命周期
onMounted(() => {
  // 初始化视频服务
  if (videoElement.value) {
    videoService.initVideo(videoElement.value, videoUrl.value)
  }
})

onUnmounted(() => {
  videoService.stop()
})
</script>

<style scoped lang="scss">
.video-stream-container {
  width: 100%;
}

.video-card {
  background: rgba(6, 78, 59, 0.8);
  border: 1px solid #10b981;
  backdrop-filter: blur(10px);
  color: white;
  
  :deep(.el-card__header) {
    background: rgba(6, 78, 59, 0.9);
    border-bottom: 1px solid #10b981;
  }
  
  :deep(.el-card__body) {
    padding: 0;
  }
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: white;
  
  .video-controls {
    margin-left: auto;
    display: flex;
    gap: 8px;
  }
}

.video-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1; /* 1:1 比例 */
  overflow: hidden;
}

.video-feed {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3);
  transition: all 0.3s ease;
  
  &:hover {
    box-shadow: 0 0 20px rgba(0, 255, 255, 0.5);
  }
}

.video-status {
  position: absolute;
  top: 10px;
  right: 10px;
  
  .status-tag {
    backdrop-filter: blur(10px);
    background: rgba(0, 0, 0, 0.7);
  }
}

// 响应式设计
@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    
    .video-controls {
      margin-left: 0;
      width: 100%;
      justify-content: space-between;
    }
  }
}
</style>
