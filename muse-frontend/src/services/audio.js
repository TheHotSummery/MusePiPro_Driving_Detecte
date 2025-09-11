/**
 * 音频服务模块
 * 负责管理音频播放逻辑，包括自动播放和防重复播放
 */

class AudioService {
  constructor() {
    this.audioElements = {
      low: null,
      mid: null,
      high: null
    }
    
    // 防重复播放的时间间隔（毫秒）
    this.playIntervals = {
      low: 3000,   // 一级提示音3秒内不重复
      mid: 5000,   // 二级提示音5秒内不重复
      high: 2000   // 三级提示音2秒内不重复
    }
    
    // 记录上次播放时间
    this.lastPlayTimes = {
      low: 0,
      mid: 0,
      high: 0
    }
    
    this.initializeAudio()
  }
  
  /**
   * 初始化音频元素
   */
  initializeAudio() {
    try {
      this.audioElements.low = new Audio('/static/audio/alert_low.mp3')
      this.audioElements.mid = new Audio('/static/audio/alert_mid.mp3')
      this.audioElements.high = new Audio('/static/audio/alert_high.mp3')
      
      // 预加载音频
      Object.values(this.audioElements).forEach(audio => {
        if (audio) {
          audio.preload = 'auto'
          audio.volume = 0.8 // 设置音量为80%
        }
      })
      
      console.log('音频服务初始化成功')
    } catch (error) {
      console.error('音频服务初始化失败:', error)
    }
  }
  
  /**
   * 播放指定级别的提示音
   * @param {string} level - 提示音级别 ('low', 'mid', 'high')
   * @param {boolean} force - 是否强制播放（忽略防重复机制）
   * @returns {Promise<boolean>} - 是否播放成功
   */
  async playAlert(level, force = false) {
    if (!this.audioElements[level]) {
      console.error(`音频元素不存在: ${level}`)
      return false
    }
    
    // 检查是否需要防重复播放
    if (!force && !this.canPlay(level)) {
      console.log(`提示音 ${level} 在防重复间隔内，跳过播放`)
      return false
    }
    
    try {
      const audio = this.audioElements[level]
      
      // 重置音频到开始位置
      audio.currentTime = 0
      
      // 播放音频
      await audio.play()
      
      // 更新最后播放时间
      this.lastPlayTimes[level] = Date.now()
      
      console.log(`提示音 ${level} 播放成功`)
      return true
    } catch (error) {
      console.error(`提示音 ${level} 播放失败:`, error)
      return false
    }
  }
  
  /**
   * 检查是否可以播放（防重复机制）
   * @param {string} level - 提示音级别
   * @returns {boolean} - 是否可以播放
   */
  canPlay(level) {
    const now = Date.now()
    const lastPlayTime = this.lastPlayTimes[level]
    const interval = this.playIntervals[level]
    
    return (now - lastPlayTime) >= interval
  }
  
  /**
   * 根据进度值自动播放对应的提示音
   * @param {number} progress - 进度值 (0-100)
   * @returns {Promise<boolean>} - 是否播放成功
   */
  async playAlertByProgress(progress) {
    if (progress >= 95) {
      return await this.playAlert('high')
    } else if (progress >= 75) {
      return await this.playAlert('mid')
    } else if (progress >= 50) {
      return await this.playAlert('low')
    }
    
    return false
  }
  
  /**
   * 手动播放指定级别的提示音（忽略防重复机制）
   * @param {string} level - 提示音级别
   * @returns {Promise<boolean>} - 是否播放成功
   */
  async playManual(level) {
    return await this.playAlert(level, true)
  }
  
  /**
   * 设置音量
   * @param {number} volume - 音量 (0-1)
   */
  setVolume(volume) {
    Object.values(this.audioElements).forEach(audio => {
      if (audio) {
        audio.volume = Math.max(0, Math.min(1, volume))
      }
    })
  }
  
  /**
   * 获取当前音量
   * @returns {number} - 当前音量
   */
  getVolume() {
    const audio = this.audioElements.low
    return audio ? audio.volume : 0.8
  }
}

// 创建单例实例
const audioService = new AudioService()

export default audioService
