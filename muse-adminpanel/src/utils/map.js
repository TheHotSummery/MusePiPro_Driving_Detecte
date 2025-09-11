// 高德地图工具类
class AmapManager {
  constructor() {
    this.isLoaded = false
    this.loadingPromise = null
  }

  // 加载高德地图API
  async loadAmapScript(apiKey, securityKey = '') {
    if (this.isLoaded) {
      return Promise.resolve()
    }

    if (this.loadingPromise) {
      return this.loadingPromise
    }

    this.loadingPromise = new Promise((resolve, reject) => {
      if (window.AMap) {
        this.isLoaded = true
        resolve()
        return
      }

      // 根据配置选择API版本
      const version = localStorage.getItem('amap_version') || '1.4.15'
      const script = document.createElement('script')
      script.src = `https://webapi.amap.com/maps?v=${version}&key=${apiKey}`
      script.onload = () => {
        // 等待AMap对象完全初始化
        const checkAMap = () => {
          if (window.AMap && window.AMap.Map) {
            this.isLoaded = true
            resolve()
          } else {
            setTimeout(checkAMap, 100)
          }
        }
        checkAMap()
      }
      script.onerror = (error) => {
        this.loadingPromise = null
        console.error('高德地图API加载失败:', error)
        reject(error)
      }
      document.head.appendChild(script)
    })

    return this.loadingPromise
  }

  // 创建地图实例
  createMap(container, options = {}) {
    if (!window.AMap) {
      throw new Error('高德地图API未加载')
    }

    const defaultOptions = {
      center: [116.397428, 39.90923], // 北京
      zoom: 10,
      mapStyle: 'amap://styles/normal'
    }

    return new window.AMap.Map(container, { ...defaultOptions, ...options })
  }

  // 创建标记
  createMarker(options) {
    if (!window.AMap) {
      throw new Error('高德地图API未加载')
    }

    return new window.AMap.Marker(options)
  }

  // 创建信息窗体
  createInfoWindow(options = {}) {
    if (!window.AMap) {
      throw new Error('高德地图API未加载')
    }

    const defaultOptions = {
      offset: new window.AMap.Pixel(0, -30)
    }

    return new window.AMap.InfoWindow({ ...defaultOptions, ...options })
  }

  // 创建轨迹线
  createPolyline(options) {
    if (!window.AMap) {
      throw new Error('高德地图API未加载')
    }

    return new window.AMap.Polyline(options)
  }

  // 获取设备图标
  getDeviceIcon(status) {
    const iconMap = {
      ONLINE: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_g.png',
      OFFLINE: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png',
      LOST: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png'
    }
    return iconMap[status] || iconMap.OFFLINE
  }

  // 获取轨迹点图标
  getTrackPointIcon(index, total) {
    if (index === 0) {
      return 'https://webapi.amap.com/theme/v1.3/markers/n/start.png'
    } else if (index === total - 1) {
      return 'https://webapi.amap.com/theme/v1.3/markers/n/end.png'
    } else {
      return 'https://webapi.amap.com/theme/v1.3/markers/n/mark_g.png'
    }
  }

  // 获取事件图标
  getEventIcon(eventType, severity) {
    const iconMap = {
      FATIGUE: {
        HIGH: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png',
        MEDIUM: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_o.png',
        LOW: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_y.png'
      },
      DISTRACTION: {
        HIGH: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png',
        MEDIUM: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_o.png',
        LOW: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_y.png'
      },
      EMERGENCY: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png'
    }

    if (eventType === 'EMERGENCY') {
      return iconMap.EMERGENCY
    }

    return iconMap[eventType]?.[severity] || iconMap.FATIGUE.LOW
  }

  // 格式化位置信息
  formatLocation(location) {
    if (!location) return '未知'
    return `${location.lat.toFixed(6)}, ${location.lng.toFixed(6)}`
  }

  // 格式化时间
  formatTime(time) {
    if (!time) return '未知'
    return new Date(time).toLocaleString()
  }

  // 获取GPS质量描述
  getGPSQuality(hdop) {
    if (!hdop) return '未知'
    if (hdop < 1) return '优秀'
    if (hdop < 2) return '良好'
    if (hdop < 5) return '一般'
    return '较差'
  }
}

// 创建单例实例
const amapManager = new AmapManager()

export default amapManager
