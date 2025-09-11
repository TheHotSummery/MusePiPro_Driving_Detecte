<template>
  <div class="websocket-test">
    <el-card>
      <template #header>
        <span>WebSocket连接测试</span>
      </template>
      
      <div class="test-content">
        <div class="connection-status">
          <el-tag :type="wsConnected ? 'success' : 'danger'" size="large">
            {{ wsConnected ? '已连接' : '未连接' }}
          </el-tag>
        </div>
        
        <div class="test-actions">
          <el-button @click="connect" :disabled="wsConnected">连接</el-button>
          <el-button @click="disconnect" :disabled="!wsConnected">断开</el-button>
          <el-button @click="sendTestMessage" :disabled="!wsConnected">发送测试消息</el-button>
        </div>
        
        <div class="message-log">
          <h4>消息日志</h4>
          <div class="log-content">
            <div 
              v-for="(message, index) in messages" 
              :key="index"
              class="log-item"
            >
              <span class="log-time">{{ message.time }}</span>
              <span class="log-type">{{ message.type }}</span>
              <span class="log-content">{{ message.content }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import wsManager from '@/utils/websocket'

const wsConnected = ref(false)
const messages = ref([])

const addMessage = (type, content) => {
  messages.value.unshift({
    time: new Date().toLocaleTimeString(),
    type,
    content
  })
  
  // 限制日志数量
  if (messages.value.length > 50) {
    messages.value = messages.value.slice(0, 50)
  }
}

const connect = () => {
  wsManager.connect()
}

const disconnect = () => {
  wsManager.disconnect()
}

const sendTestMessage = () => {
  wsManager.send('test', { message: 'Hello WebSocket!' })
  addMessage('发送', '测试消息')
}

const handleConnected = () => {
  wsConnected.value = true
  addMessage('系统', 'WebSocket连接成功')
}

const handleDisconnected = (reason) => {
  wsConnected.value = false
  addMessage('系统', `WebSocket连接断开: ${reason}`)
}

const handleError = (error) => {
  addMessage('错误', `连接错误: ${error.message || error}`)
}

const handleRealtimeData = (data) => {
  addMessage('数据', `实时数据: ${JSON.stringify(data)}`)
}

const handleAlert = (alert) => {
  addMessage('告警', `新告警: ${alert.message}`)
}

onMounted(() => {
  // 监听WebSocket事件
  wsManager.on('connected', handleConnected)
  wsManager.on('disconnected', handleDisconnected)
  wsManager.on('error', handleError)
  wsManager.on('realtime_data', handleRealtimeData)
  wsManager.on('alert', handleAlert)
  
  // 初始化连接状态
  wsConnected.value = wsManager.getConnectionStatus().isConnected
  
  addMessage('系统', 'WebSocket测试页面已加载')
})

onUnmounted(() => {
  wsManager.off('connected', handleConnected)
  wsManager.off('disconnected', handleDisconnected)
  wsManager.off('error', handleError)
  wsManager.off('realtime_data', handleRealtimeData)
  wsManager.off('alert', handleAlert)
})
</script>

<style scoped lang="scss">
.websocket-test {
  padding: 20px;
  height: calc(100vh - 60px);
  overflow-y: auto;
}

.test-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.connection-status {
  text-align: center;
}

.test-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.message-log {
  .log-content {
    max-height: 400px;
    overflow-y: auto;
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    padding: 10px;
    background: #f8f9fa;
  }
  
  .log-item {
    display: flex;
    gap: 10px;
    margin-bottom: 8px;
    font-size: 12px;
    
    .log-time {
      color: #909399;
      min-width: 80px;
    }
    
    .log-type {
      color: #409EFF;
      min-width: 60px;
      font-weight: 500;
    }
    
    .log-content {
      flex: 1;
      color: #303133;
    }
  }
}
</style>

