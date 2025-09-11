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
    const wsUrl = import.meta.env.VITE_WS_URL || 'ws://spacemit.topcoder.fun/websocket'
    
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

  // 设置事件处理器
  setupEventHandlers() {
    this.socket.onopen = () => {
      console.log('WebSocket连接成功')
      this.reconnectAttempts = 0
      this.emit('connected')
    }

    this.socket.onclose = (event) => {
      console.log('WebSocket连接断开:', event.code, event.reason)
      this.emit('disconnected', event.reason)
      
      // 自动重连
      if (this.reconnectAttempts < this.maxReconnectAttempts) {
        this.reconnectAttempts++
        setTimeout(() => {
          console.log(`尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
          this.connect()
        }, this.reconnectInterval)
      }
    }

    this.socket.onerror = (error) => {
      console.error('WebSocket连接错误:', error)
      this.emit('error', error)
    }

    this.socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        const { type, payload } = data
        
        // 根据消息类型分发事件
        switch (type) {
          case 'realtime_data':
            this.emit('realtime_data', payload)
            break
          case 'alert':
            this.emit('alert', payload)
            break
          case 'device_status':
            this.emit('device_status', payload)
            break
          case 'event':
            this.emit('event', payload)
            break
          default:
            console.log('未知消息类型:', type, payload)
        }
      } catch (error) {
        console.error('解析WebSocket消息失败:', error)
      }
    }
  }

  // 断开连接
  disconnect() {
    if (this.socket) {
      this.socket.close()
      this.socket = null
    }
  }

  // 添加事件监听器
  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event).push(callback)
  }

  // 移除事件监听器
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
          console.error(`事件监听器错误 (${event}):`, error)
        }
      })
    }
  }

  // 发送消息
  send(type, data) {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      const message = JSON.stringify({ type, payload: data })
      this.socket.send(message)
    } else {
      console.warn('WebSocket未连接，无法发送消息')
    }
  }

  // 获取连接状态
  isConnected() {
    return this.socket && this.socket.readyState === WebSocket.OPEN
  }
}

// 创建单例实例
const wsManager = new WebSocketManager()

export default wsManager
