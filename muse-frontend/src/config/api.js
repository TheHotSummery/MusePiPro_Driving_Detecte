// API配置文件
// 在这里修改API地址，方便在不同环境间切换

const config = {
  // 开发环境配置
  development: {
    API_BASE_URL: 'http://192.168.110.31:5200',
    WS_URL: 'ws://192.168.110.31:5200',
    VIDEO_URL: '/feed/webcam/',
    IMAGE_URL: '/feed/image/'
  },
  
  // 生产环境配置
  production: {
    API_BASE_URL: 'http://192.168.110.31:5200',
    WS_URL: 'ws://192.168.110.31:5200',
    VIDEO_URL: '/feed/webcam/',
    IMAGE_URL: '/feed/image/'
  },
  
  // 本地测试配置
  local: {
    API_BASE_URL: 'http://192.168.110.31:5200',
    WS_URL: 'ws://192.168.110.31:5200',
    VIDEO_URL: '/feed/webcam/',
    IMAGE_URL: '/feed/image/'
  }
}

// 获取当前环境
const getCurrentEnv = () => {
  // 可以通过环境变量或URL参数来切换环境
  const urlParams = new URLSearchParams(window.location.search)
  const envParam = urlParams.get('env')
  
  if (envParam && config[envParam]) {
    return envParam
  }
  
  // 默认根据域名判断环境
  if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
    return 'local'
  }
  
  return 'development'
}

// 导出当前环境的配置
const currentEnv = getCurrentEnv()
const currentConfig = config[currentEnv]

console.log(`当前环境: ${currentEnv}`)
console.log('API配置:', currentConfig)

export default {
  ...currentConfig,
  // 添加一些工具方法
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
    if (config[env]) {
      const url = new URL(window.location)
      url.searchParams.set('env', env)
      window.location.href = url.toString()
    }
  }
}




