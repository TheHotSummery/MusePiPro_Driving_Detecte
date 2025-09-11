// 改进的API配置文件
// 支持环境变量和动态配置

// 从环境变量获取配置，如果没有则使用默认值
const getEnvVar = (key, defaultValue) => {
  // Vite环境变量以VITE_开头
  return import.meta.env[key] || defaultValue
}

// 动态获取当前主机地址
const getCurrentHost = () => {
  if (typeof window !== 'undefined') {
    return window.location.hostname
  }
  return 'localhost'
}


// 基础配置
const baseConfig = {
  // 从环境变量获取，如果没有则使用当前主机
  API_BASE_URL: getEnvVar('VITE_API_BASE_URL', `http://${getCurrentHost()}:5200`),
  WS_URL: getEnvVar('VITE_WS_URL', `ws://${getCurrentHost()}:5200`),
  VIDEO_URL: getEnvVar('VITE_VIDEO_URL', '/feed/webcam/'),
  IMAGE_URL: getEnvVar('VITE_IMAGE_URL', '/feed/image/'),
  
  // 应用信息
  APP_TITLE: getEnvVar('VITE_APP_TITLE', '疲劳驾驶检测系统'),
  APP_VERSION: getEnvVar('VITE_APP_VERSION', '1.0.0'),
}

// 环境特定配置
const config = {
  // 开发环境
  development: {
    ...baseConfig,
    API_BASE_URL: getEnvVar('VITE_API_BASE_URL', `http://${getCurrentHost()}:5200`),
    WS_URL: getEnvVar('VITE_WS_URL', `ws://${getCurrentHost()}:5200`),
  },
  
  // 生产环境
  production: {
    ...baseConfig,
    // 生产环境使用相对路径，自动适配当前域名
    API_BASE_URL: getEnvVar('VITE_API_BASE_URL', `http://${getCurrentHost()}:5200`),
    WS_URL: getEnvVar('VITE_WS_URL', `ws://${getCurrentHost()}:5200`),
  },
  
  // 本地测试
  local: {
    ...baseConfig,
    API_BASE_URL: getEnvVar('VITE_API_BASE_URL', 'http://localhost:5200'),
    WS_URL: getEnvVar('VITE_WS_URL', 'ws://localhost:5200'),
  }
}

// 获取当前环境
const getCurrentEnv = () => {
  // 1. 优先从URL参数获取
  if (typeof window !== 'undefined') {
    const urlParams = new URLSearchParams(window.location.search)
    const envParam = urlParams.get('env')
    if (envParam && config[envParam]) {
      return envParam
    }
    
    // 2. 根据域名判断环境
    const hostname = window.location.hostname
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
      return 'local'
    }
    
    // 3. 根据端口判断
    const port = window.location.port
    if (port === '3000' || port === '5173') {
      return 'development'
    }
  }
  
  // 4. 根据Vite环境变量判断
  const mode = import.meta.env.MODE
  if (mode === 'production') {
    return 'production'
  }
  
  return 'development'
}

// 导出当前环境的配置
const currentEnv = getCurrentEnv()
const currentConfig = config[currentEnv]

// 调试信息
if (import.meta.env.DEV) {
  console.log(`当前环境: ${currentEnv}`)
  console.log('API配置:', currentConfig)
  console.log('环境变量:', {
    VITE_API_BASE_URL: import.meta.env.VITE_API_BASE_URL,
    VITE_WS_URL: import.meta.env.VITE_WS_URL,
    MODE: import.meta.env.MODE
  })
}

export default {
  ...currentConfig,
  
  // 工具方法
  getApiUrl: (path) => {
    return `${currentConfig.API_BASE_URL}${path}`
  },
  
  getVideoUrl: () => {
    return `${currentConfig.API_BASE_URL}${currentConfig.VIDEO_URL}`
  },
  
  getImageUrl: () => {
    return `${currentConfig.API_BASE_URL}${currentConfig.IMAGE_URL}`
  },
  
  // 获取所有可用环境
  getAvailableEnvs: () => {
    return Object.keys(config)
  },
  
  // 切换环境（通过URL参数）
  switchEnv: (env) => {
    if (config[env] && typeof window !== 'undefined') {
      const url = new URL(window.location)
      url.searchParams.set('env', env)
      window.location.href = url.toString()
    }
  },
  
  // 获取当前环境
  getCurrentEnv: () => currentEnv,
  
  // 检查是否为生产环境
  isProduction: () => currentEnv === 'production',
  
  // 检查是否为开发环境
  isDevelopment: () => currentEnv === 'development'
}
