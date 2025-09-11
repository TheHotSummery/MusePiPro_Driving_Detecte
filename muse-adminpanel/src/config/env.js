// 环境配置 - 统一管理所有配置项
export const config = {
  // API配置
  api: {
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://spacemit.topcoder.fun',
    timeout: 10000
  },
  
  // WebSocket配置
  websocket: {
    url: import.meta.env.VITE_WS_URL || 'http://spacemit.topcoder.fun/websocket',
    reconnectAttempts: 5,
    reconnectInterval: 5000
  },
  
  // 应用配置
  app: {
    title: import.meta.env.VITE_APP_TITLE || 'Muse 云端实时检测平台',
    version: import.meta.env.VITE_APP_VERSION || '1.2.0'
  },
  
  // 高德地图配置
  amap: {
    apiKey: import.meta.env.VITE_AMAP_API_KEY || 'YOUR_AMAP_API_KEY',
    securityKey: import.meta.env.VITE_AMAP_SECURITY_KEY || 'YOUR_AMAP_SECURITY_KEY',
    version: import.meta.env.VITE_AMAP_VERSION || '1.4.15',
    defaultCenter: {
      lat: 33.553733,
      lng: 119.030953
    },
    defaultZoom: 10
  },
  
  // 地图配置（保持向后兼容）
  map: {
    defaultCenter: {
      lat: 33.553733,
      lng: 119.030953
    },
    defaultZoom: 10
  },
  
  // 刷新间隔配置
  refresh: {
    deviceStatus: 30000,    // 设备状态刷新间隔（毫秒）
    realtimeData: 20000,    // 实时数据刷新间隔（毫秒）
    eventList: 60000        // 事件列表刷新间隔（毫秒）
  },
  
  // 开发配置
  dev: {
    mode: import.meta.env.VITE_DEV_MODE || 'development',
    debug: import.meta.env.VITE_DEBUG === 'true' || false
  }
}

export default config

