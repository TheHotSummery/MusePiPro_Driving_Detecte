import apiConfig from '@/config/api'

class VideoService {
  constructor() {
    this.videoElement = null
    this.isPlaying = false
    this.retryCount = 0
    this.maxRetries = 3
    this.retryDelay = 2000
    this.stopRetrying = false
  }

  // 初始化视频流
  initVideo(element, videoUrl = null) {
    this.videoElement = element
    this.videoUrl = videoUrl || apiConfig.getVideoUrl()
    this.setupVideoElement()
  }

  setupVideoElement() {
    if (!this.videoElement) return

    // 设置视频源
    this.videoElement.src = this.videoUrl
    this.videoElement.alt = '视频流'

    // 错误处理
    this.videoElement.onerror = () => {
      console.error('视频流加载失败')
      this.handleVideoError()
    }

    // 加载成功
    this.videoElement.onload = () => {
      console.log('视频流加载成功')
      this.retryCount = 0
      this.isPlaying = true
    }

    // 开始播放
    this.videoElement.onloadstart = () => {
      console.log('开始加载视频流')
    }
  }

  // 处理视频错误
  handleVideoError() {
    this.retryCount++
    this.isPlaying = false

    if (this.retryCount < this.maxRetries) {
      console.log(`视频流重试 ${this.retryCount}/${this.maxRetries}`)
      setTimeout(() => {
        this.retryVideo()
      }, this.retryDelay)
    } else {
      console.error('视频流重试次数已达上限，显示占位图')
      this.showPlaceholder()
      // 停止重试，避免无限循环
      this.stopRetrying = true
    }
  }

  // 重试视频流
  retryVideo() {
    if (this.videoElement && !this.stopRetrying) {
      // 添加时间戳避免缓存
      const timestamp = new Date().getTime()
      this.videoElement.src = `${this.videoUrl}?t=${timestamp}`
    }
  }

  // 显示占位图
  showPlaceholder() {
    if (this.videoElement) {
      // 使用相对路径，避免跨域问题
      this.videoElement.src = '/static/placeholder.jpg'
    }
  }

  // 重新连接视频流
  reconnect() {
    this.retryCount = 0
    this.isPlaying = false
    this.stopRetrying = false
    if (this.videoElement) {
      this.setupVideoElement()
    }
  }

  // 停止视频流
  stop() {
    this.isPlaying = false
    if (this.videoElement) {
      this.videoElement.src = ''
    }
  }

  // 获取视频状态
  getStatus() {
    return {
      isPlaying: this.isPlaying,
      retryCount: this.retryCount,
      hasError: this.retryCount >= this.maxRetries
    }
  }

  // 设置视频URL
  setVideoUrl(url) {
    this.videoUrl = url
    if (this.videoElement) {
      this.setupVideoElement()
    }
  }
}

// 创建单例实例
const videoService = new VideoService()

export default videoService
