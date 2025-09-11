// 配置管理工具 - 提供统一的配置访问和修改接口
import { config } from './env.js'

class ConfigManager {
  constructor() {
    this.config = config
  }

  // 获取API基础地址
  getApiBaseUrl() {
    return this.config.api.baseURL
  }

  // 获取WebSocket地址
  getWebSocketUrl() {
    return this.config.websocket.url
  }

  // 获取高德地图配置
  getAmapConfig() {
    return {
      apiKey: this.config.amap.apiKey || localStorage.getItem('amap_api_key') || '',
      securityKey: this.config.amap.securityKey || localStorage.getItem('amap_security_key') || '',
      version: this.config.amap.version || localStorage.getItem('amap_version') || '1.4.15',
      defaultCenter: this.config.amap.defaultCenter,
      defaultZoom: this.config.amap.defaultZoom
    }
  }

  // 设置高德地图配置
  setAmapConfig(apiKey, securityKey = '', version = '1.4.15') {
    localStorage.setItem('amap_api_key', apiKey)
    localStorage.setItem('amap_security_key', securityKey)
    localStorage.setItem('amap_version', version)
  }

  // 获取应用配置
  getAppConfig() {
    return this.config.app
  }

  // 获取刷新间隔配置
  getRefreshConfig() {
    return this.config.refresh
  }

  // 获取开发配置
  getDevConfig() {
    return this.config.dev
  }

  // 检查配置完整性
  validateConfig() {
    const issues = []
    
    // 检查API地址
    if (!this.config.api.baseURL || this.config.api.baseURL === 'http://spacemit.topcoder.fun') {
      issues.push('API地址使用默认值，请确认是否正确')
    }
    
    // 检查WebSocket地址
    if (!this.config.websocket.url || this.config.websocket.url === 'http://spacemit.topcoder.fun/websocket') {
      issues.push('WebSocket地址使用默认值，请确认是否正确')
    }
    
    // 检查高德地图配置
    const amapConfig = this.getAmapConfig()
    if (!amapConfig.apiKey) {
      issues.push('高德地图API Key未配置')
    }
    
    return {
      isValid: issues.length === 0,
      issues
    }
  }

  // 生成配置报告
  generateConfigReport() {
    const validation = this.validateConfig()
    const amapConfig = this.getAmapConfig()
    
    return {
      timestamp: new Date().toISOString(),
      api: {
        baseURL: this.config.api.baseURL,
        timeout: this.config.api.timeout
      },
      websocket: {
        url: this.config.websocket.url,
        reconnectAttempts: this.config.websocket.reconnectAttempts
      },
      amap: {
        apiKey: amapConfig.apiKey ? `${amapConfig.apiKey.substring(0, 8)}...` : '未配置',
        securityKey: amapConfig.securityKey ? '已配置' : '未配置',
        version: amapConfig.version
      },
      app: this.config.app,
      validation
    }
  }

  // 快速配置方法 - 一键设置常用配置
  quickSetup(options = {}) {
    const {
      apiBaseUrl = 'http://spacemit.topcoder.fun',
      amapApiKey = '',
      amapSecurityKey = '',
      amapVersion = '1.4.15'
    } = options

    // 设置高德地图配置
    if (amapApiKey) {
      this.setAmapConfig(amapApiKey, amapSecurityKey, amapVersion)
    }

    // 返回当前配置状态
    return this.generateConfigReport()
  }
}

// 创建单例实例
const configManager = new ConfigManager()

export default configManager
