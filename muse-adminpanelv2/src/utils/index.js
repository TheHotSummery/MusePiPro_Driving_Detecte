// 防抖函数
export const debounce = (func, wait) => {
  let timeout
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout)
      func(...args)
    }
    clearTimeout(timeout)
    timeout = setTimeout(later, wait)
  }
}

// 节流函数
export const throttle = (func, limit) => {
  let inThrottle
  return function(...args) {
    if (!inThrottle) {
      func.apply(this, args)
      inThrottle = true
      setTimeout(() => inThrottle = false, limit)
    }
  }
}

// 格式化时间
export const formatTime = (timestamp) => {
  if (!timestamp) return '--:--'
  const d = new Date(timestamp)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

// 格式化日期
export const formatDate = (timestamp) => {
  if (!timestamp) return ''
  const d = new Date(timestamp)
  return d.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
}

// 格式化数字
export const formatNumber = (num) => {
  if (num === null || num === undefined) return '0'
  return num.toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

// 获取告警级别颜色类
export const getLevelColorClass = (level) => {
  if (level === 'Level 3') return 'border-red-500'
  if (level === 'Level 2') return 'border-orange-500'
  return 'border-yellow-400'
}

// 获取告警级别背景类
export const getLevelBgClass = (level) => {
  if (level === 'Level 3') return 'bg-red-500'
  if (level === 'Level 2') return 'bg-orange-500'
  return 'bg-yellow-400'
}

// 获取级别颜色
export const getLevelColor = (level) => {
  if (level === 'Level 3') return 'text-red-400'
  if (level === 'Level 2') return 'text-orange-400'
  if (level === 'Level 1') return 'text-yellow-400'
  return 'text-green-400'
}

// 获取评分样式类
export const getScoreClass = (score) => {
  if (score >= 90) return 'score-high'
  if (score >= 75) return 'score-mid'
  return 'score-low'
}













