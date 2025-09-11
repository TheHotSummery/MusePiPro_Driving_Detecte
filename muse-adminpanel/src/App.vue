<template>
  <div id="app">
    <!-- 顶部导航栏 -->
    <el-header class="app-header">
      <div class="header-content">
        <div class="logo">
          <h1>Muse 云端实时检测平台</h1>
        </div>
        <el-menu
          :default-active="activeMenu"
          mode="horizontal"
          router
          class="header-menu"
        >
          <el-menu-item index="/dashboard">
            <el-icon><Monitor /></el-icon>
            <span>实时监控</span>
          </el-menu-item>
          <el-menu-item index="/events">
            <el-icon><Warning /></el-icon>
            <span>事件中心</span>
          </el-menu-item>
          <el-menu-item index="/alerts">
            <el-icon><Bell /></el-icon>
            <span>告警中心</span>
          </el-menu-item>
        </el-menu>
        <div class="header-actions">
          <el-button size="small" @click="toggleTheme">
            <el-icon><Moon v-if="isDark" /><Sunny v-else /></el-icon>
          </el-button>
          <el-button size="small" @click="showSystemInfo">
            <el-icon><InfoFilled /></el-icon>
          </el-button>
          <el-dropdown @command="handleAdminCommand" trigger="click">
            <el-button size="small">
              <el-icon><Setting /></el-icon>
              管理
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="analytics">
                  <el-icon><DataAnalysis /></el-icon>
                  数据分析
                </el-dropdown-item>
                <el-dropdown-item command="users">
                  <el-icon><User /></el-icon>
                  用户管理
                </el-dropdown-item>
                <el-dropdown-item command="websocket-test">
                  <el-icon><Connection /></el-icon>
                  连接测试
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <!-- 主要内容区域 -->
    <el-main class="app-main">
      <RouterView />
    </el-main>

    <!-- 系统信息对话框 -->
    <el-dialog
      v-model="systemInfoVisible"
      title="系统信息"
      width="500px"
    >
      <el-descriptions :column="1" border>
        <el-descriptions-item label="系统版本">v1.0.0</el-descriptions-item>
        <el-descriptions-item label="构建时间">{{ buildTime }}</el-descriptions-item>
        <el-descriptions-item label="运行环境">{{ environment }}</el-descriptions-item>
        <el-descriptions-item label="API地址">{{ apiBaseUrl }}</el-descriptions-item>
        <el-descriptions-item label="WebSocket状态">
          <el-tag :type="wsConnected ? 'success' : 'danger'">
            {{ wsConnected ? '已连接' : '未连接' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { 
  Monitor, 
  Warning, 
  Bell, 
  DataAnalysis, 
  User, 
  Moon, 
  Sunny, 
  InfoFilled,
  Connection,
  Setting
} from '@element-plus/icons-vue'

import wsManager from '@/utils/websocket'

const route = useRoute()

// 响应式数据
const isDark = ref(false)
const systemInfoVisible = ref(false)
const wsConnected = ref(false)

// 计算属性
const activeMenu = computed(() => {
  // 如果路径是根路径，默认选中dashboard
  return route.path === '/' ? '/dashboard' : route.path
})

const buildTime = ref(new Date().toLocaleString())
const environment = ref(import.meta.env.MODE)
const apiBaseUrl = ref(import.meta.env.VITE_API_BASE_URL || 'http://spacemit.topcoder.fun')

// 方法
const toggleTheme = () => {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

const showSystemInfo = () => {
  systemInfoVisible.value = true
}

// 处理管理命令
const handleAdminCommand = (command) => {
  const routeMap = {
    'analytics': '/analytics',
    'users': '/users',
    'websocket-test': '/websocket-test'
  }
  
  if (routeMap[command]) {
    window.location.href = routeMap[command]
  }
}

const handleWebSocketStatus = (connected) => {
  wsConnected.value = connected
}

// 生命周期
onMounted(() => {
  // 恢复主题设置
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme === 'dark') {
    isDark.value = true
    document.documentElement.classList.add('dark')
  }

  // 监听WebSocket连接状态
  wsManager.on('connected', () => handleWebSocketStatus(true))
  wsManager.on('disconnected', () => handleWebSocketStatus(false))
  
  // 初始化WebSocket连接状态
  wsConnected.value = wsManager.getConnectionStatus().isConnected
})

onUnmounted(() => {
  wsManager.off('connected', handleWebSocketStatus)
  wsManager.off('disconnected', handleWebSocketStatus)
})
</script>

<style scoped lang="scss">
#app {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0;
  height: 60px;
  line-height: 60px;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  width: 100%;
  padding: 0 20px;
}

.logo {
  h1 {
    margin: 0;
    font-size: 20px;
    color: #409EFF;
    font-weight: 600;
  }
}

.header-menu {
  flex: 1;
  justify-content: center;
  border-bottom: none;
  
  .el-menu-item {
    height: 60px;
    line-height: 60px;
    
    &:hover {
      background-color: #ecf5ff;
    }
    
    &.is-active {
      background-color: #ecf5ff;
      color: #409EFF;
    }
  }
}

.header-actions {
  display: flex;
  gap: 8px;
}

.app-main {
  flex: 1;
  padding: 0;
  background: #f5f7fa;
  overflow-y: auto;
}

// 响应式布局
@media (max-width: 768px) {
  .header-content {
    padding: 0 10px;
  }
  
  .logo h1 {
    font-size: 16px;
  }
  
  .header-menu {
    display: none;
  }
  
  .header-actions {
    gap: 4px;
  }
}

// 暗色主题
:global(.dark) {
  .app-header {
    background: #1d1e1f;
    border-bottom-color: #414243;
  }
  
  .app-main {
    background: #141414;
  }
  
  .logo h1 {
    color: #409EFF;
  }
}
</style>