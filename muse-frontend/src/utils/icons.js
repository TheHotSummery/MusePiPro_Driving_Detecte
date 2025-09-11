// Element Plus 图标工具类
// 提供安全的图标导入和备用方案

// 常用图标映射
export const iconMap = {
  // 连接相关
  connection: 'Connection',
  disconnect: 'Close', // 使用Close代替Disconnect
  
  // 导航相关
  monitor: 'Monitor',
  clock: 'Clock',
  setting: 'Setting',
  
  // 视频相关
  videoCamera: 'VideoCamera',
  videoPlay: 'VideoPlay',
  videoPause: 'VideoPause',
  
  // 操作相关
  refresh: 'Refresh',
  upload: 'Upload',
  download: 'Download',
  delete: 'Delete',
  check: 'Check',
  close: 'Close',
  
  // 状态相关
  warning: 'Warning',
  info: 'InfoFilled',
  success: 'SuccessFilled',
  error: 'CircleCloseFilled',
  
  // 数据相关
  list: 'List',
  view: 'View',
  trend: 'TrendCharts',
  scale: 'Scale',
  
  // 工具相关
  tools: 'Tools',
  bell: 'Bell',
  light: 'Light',
  vibration: 'Vibration',
  volume: 'VolumeUp',
  play: 'VideoPlay',
  
  // 箭头相关
  arrowDown: 'ArrowDownBold',
  arrowUp: 'ArrowUpBold',
  arrowLeft: 'ArrowLeftBold',
  arrowRight: 'ArrowRightBold',
  
  // 系统相关
  cpu: 'Cpu',
  timer: 'Timer'
}

// 获取图标名称
export const getIconName = (key) => {
  return iconMap[key] || 'QuestionFilled'
}

// 检查图标是否存在
export const isIconAvailable = (iconName) => {
  return Object.values(iconMap).includes(iconName)
}




