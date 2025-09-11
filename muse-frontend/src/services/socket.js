import { io } from 'socket.io-client'
import { useDetectionStore } from '@/stores/detection'
import { useSystemStore } from '@/stores/system'
import { useConfigStore } from '@/stores/config'
import apiConfig from '@/config/api'

class SocketService {
  constructor() {
    this.socket = null
    this.detectionStore = null
    this.systemStore = null
    this.configStore = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 1000
  }

  // 初始化stores
  initStores() {
    if (!this.detectionStore) {
      this.detectionStore = useDetectionStore()
    }
    if (!this.systemStore) {
      this.systemStore = useSystemStore()
    }
    if (!this.configStore) {
      this.configStore = useConfigStore()
    }
  }

  connect() {
    // 确保stores已初始化
    this.initStores()
    
    if (this.socket) {
      this.disconnect()
    }

    this.socket = io(apiConfig.WS_URL, {
      reconnection: true,
      reconnectionAttempts: this.maxReconnectAttempts,
      reconnectionDelay: this.reconnectDelay,
      timeout: 20000
    })

    this.setupEventListeners()
  }

  disconnect() {
    if (this.socket) {
      this.socket.disconnect()
      this.socket = null
    }
  }

  setupEventListeners() {
    if (!this.socket) return

    // 连接事件
    this.socket.on('connect', () => {
      console.log('WebSocket连接成功')
      this.systemStore.setConnected(true)
      this.systemStore.setConnectionStatus('已连接，等待检测结果...')
      this.reconnectAttempts = 0
    })

    this.socket.on('disconnect', () => {
      console.log('WebSocket连接断开')
      this.systemStore.setConnected(false)
      this.systemStore.setConnectionStatus('连接断开，重连中...')
    })

    this.socket.on('connect_error', (error) => {
      console.error('WebSocket连接错误:', error)
      this.systemStore.setError(`连接错误: ${error.message}`)
      this.reconnectAttempts++
    })

    // 业务事件
    this.socket.on('detection_update', (data) => {
      this.detectionStore.updateDetection(data)
      this.systemStore.updateLastUpdateTime()
    })

    this.socket.on('status', (data) => {
      this.detectionStore.updateStatus(data.message)
    })

    this.socket.on('config_update', (data) => {
      this.configStore.updateConfig(data)
    })

    this.socket.on('weights_update', (data) => {
      this.configStore.updateWeights(data)
    })

    this.socket.on('error', (data) => {
      this.systemStore.setError(data.message)
    })
  }

  // 发送事件方法
  updateConfig() {
    this.initStores()
    if (this.socket && this.socket.connected) {
      this.socket.emit('update_config', this.configStore.getConfigForSocket())
    }
  }

  updateWeights() {
    this.initStores()
    if (this.socket && this.socket.connected) {
      this.socket.emit('update_weights', this.configStore.getWeightsForSocket())
    }
  }

  clearEvents() {
    this.initStores()
    if (this.socket && this.socket.connected) {
      this.socket.emit('clear_events')
      this.detectionStore.clearEvents()
    }
  }

  triggerGPIO(gpio, duration = 1) {
    if (this.socket && this.socket.connected) {
      this.socket.emit('trigger_gpio', { gpio, duration })
    }
  }

  // 网络测试相关方法
  sendNetworkTest(testType) {
    if (this.socket && this.socket.connected) {
      this.socket.emit('network_test', { test_type: testType })
    } else {
      throw new Error('WebSocket未连接')
    }
  }

  getNetworkStatus() {
    if (this.socket && this.socket.connected) {
      this.socket.emit('get_network_status')
    } else {
      throw new Error('WebSocket未连接')
    }
  }

  // 获取连接状态
  isConnected() {
    return this.socket && this.socket.connected
  }

  // 获取socket实例（用于其他服务）
  getSocket() {
    return this.socket
  }
}

// 创建单例实例
const socketService = new SocketService()

export default socketService
