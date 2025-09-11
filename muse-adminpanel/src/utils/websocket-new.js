class WebSocketManager {
  constructor() {
    this.socket = null
    this.stompClient = null
    this.isConnected = false
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectInterval = 5000
    this.listeners = new Map()
  }

  // 连接WebSocket
  connect() {
    const wsUrl = import.meta.env.VITE_WS_URL || 'http://spacemit.topcoder.fun/websocket'
    
    try {
      // 动态导入SockJS和STOMP
      this.loadDependencies().then(() => {
        this.createSTOMPConnection(wsUrl)
      }).catch(error => {
        console.error('加载依赖失败:', error)
        this.emit('error', error)
      })
    } catch (error) {
      console.error('WebSocket连接失败:', error)
      this.emit('error', error)
      return
    }
  }

  // 加载依赖
  loadDependencies() {
    return new Promise((resolve, reject) => {
      const dependencies = []
      
      if (!window.SockJS) {
        dependencies.push(this.loadScript('https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js'))
      }
      
      if (!window.Stomp) {
        dependencies.push(this.loadScript('https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js'))
      }
      
      if (dependencies.length === 0) {
        resolve()
        return
      }
      
      Promise.all(dependencies).then(resolve).catch(reject)
    })
  }

  // 加载脚本
  loadScript(src) {
    return new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = src
      script.onload = resolve
      script.onerror = reject
      document.head.appendChild(script)
    })
  }

  // 创建STOMP连接
  createSTOMPConnection(wsUrl) {
    try {
      console.log('创建STOMP连接:', wsUrl)
      this.socket = new window.SockJS(wsUrl)
      this.stompClient = window.Stomp.over(this.socket)
      
      // 禁用调试信息（生产环境）
      this.stompClient.debug = null
      
      this.stompClient.connect({}, (frame) => {
        console.log('WebSocket连接成功:', frame)
        this.isConnected = true
        this.reconnectAttempts = 0
        this.subscribeToTopics()
        this.emit('connected', frame)
      }, (error) => {
        console.error('WebSocket连接失败:', error)
        this.isConnected = false
        this.handleReconnect()
      })
    } catch (error) {
      console.error('STOMP连接失败:', error)
      this.emit('error', error)
    }
  }

  // 订阅主题
  subscribeToTopics() {
    console.log('开始订阅WebSocket主题...')
    
    // 订阅实时数据推送
    this.stompClient.subscribe('/topic/realtime_data', (message) => {
      try {
        const data = JSON.parse(message.body)
        console.log('收到实时数据推送:', data)
        this.emit('realtime_data', data)
      } catch (error) {
        console.error('解析实时数据失败:', error)
      }
    })

    // 订阅设备状态更新
    this.stompClient.subscribe('/topic/device_status', (message) => {
      try {
        const data = JSON.parse(message.body)
        console.log('收到设备状态更新:', data)
        this.emit('device_status', data)
      } catch (error) {
        console.error('解析设备状态更新失败:', error)
      }
    })

    // 订阅告警推送
    this.stompClient.subscribe('/topic/alert', (message) => {
      try {
        const data = JSON.parse(message.body)
        console.log('收到告警推送:', data)
        this.emit('alert', data)
      } catch (error) {
        console.error('解析告警推送失败:', error)
      }
    })

    // 订阅事件通知
    this.stompClient.subscribe('/topic/event', (message) => {
      try {
        const data = JSON.parse(message.body)
        console.log('收到事件通知:', data)
        this.emit('event', data)
      } catch (error) {
        console.error('解析事件通知失败:', error)
      }
    })

    console.log('WebSocket主题订阅完成')
  }

  // 处理重连
  handleReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      console.log(`尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
      setTimeout(() => {
        this.connect()
      }, this.reconnectInterval)
    } else {
      console.error('达到最大重连次数，停止重连')
      this.emit('max_reconnect_attempts')
    }
  }

  // 断开连接
  disconnect() {
    if (this.stompClient && this.isConnected) {
      this.stompClient.disconnect(() => {
        console.log('WebSocket连接已断开')
        this.isConnected = false
        this.emit('disconnected')
      })
    }
  }

  // 发送消息
  send(destination, body, headers = {}) {
    if (this.stompClient && this.isConnected) {
      this.stompClient.send(destination, headers, JSON.stringify(body))
    } else {
      console.warn('WebSocket未连接，无法发送消息')
    }
  }

  // 事件监听
  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event).push(callback)
  }

  // 移除事件监听
  off(event, callback) {
    if (this.listeners.has(event)) {
      const callbacks = this.listeners.get(event)
      const index = callbacks.indexOf(callback)
      if (index > -1) {
        callbacks.splice(index, 1)
      }
    }
  }

  // 触发事件
  emit(event, data) {
    if (this.listeners.has(event)) {
      this.listeners.get(event).forEach(callback => {
        try {
          callback(data)
        } catch (error) {
          console.error(`事件处理器执行失败 (${event}):`, error)
        }
      })
    }
  }

  // 获取连接状态
  getConnectionStatus() {
    return {
      isConnected: this.isConnected,
      reconnectAttempts: this.reconnectAttempts,
      maxReconnectAttempts: this.maxReconnectAttempts
    }
  }
}

// 创建单例实例
const wsManager = new WebSocketManager()

export default wsManager
