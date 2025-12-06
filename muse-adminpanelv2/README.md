# Muse Admin Panel v2

基于 Vue 3 + Vite + ECharts + 高德地图的管理端数据可视化平台

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Vite** - 下一代前端构建工具
- **Vue Router** - 官方路由管理器
- **Pinia** - Vue 状态管理
- **ECharts** - 数据可视化图表库
- **高德地图 API** - 地图展示和交互
- **Axios** - HTTP 客户端

## 项目结构

```
muse-adminpanelv2/
├── src/
│   ├── api/              # API 接口封装
│   ├── assets/           # 静态资源
│   │   └── styles/       # 全局样式
│   ├── components/       # 公共组件
│   │   ├── GlassPanel.vue
│   │   ├── Header.vue
│   │   └── NavigationBar.vue
│   ├── router/           # 路由配置
│   ├── utils/            # 工具函数
│   ├── views/            # 页面组件
│   │   ├── Dashboard.vue    # 数据大屏
│   │   ├── Monitor.vue      # 车辆监控
│   │   ├── Report.vue       # 数据报表
│   │   └── Heatmap.vue      # 热力图/轨迹回放
│   ├── App.vue
│   └── main.js
├── index.html
├── package.json
└── vite.config.js
```

## 安装依赖

```bash
npm install
```

## 开发

```bash
npm run dev
```

访问 http://localhost:3000

## 构建

```bash
npm run build
```

## 页面说明

### 1. Dashboard (数据大屏)
- 实时车辆监控
- 地图展示
- 疲劳趋势图表
- 行为分布雷达图
- 区域风险排行
- 实时告警流

### 2. Monitor (车辆监控)
- 车辆列表
- 单车轨迹展示
- 疲劳监测仪表盘
- 车辆状态遥测

### 3. Report (数据报表)
- 总体概览统计
- 月度趋势分析
- 行为类型占比
- 驾驶员排行榜

### 4. Heatmap (热力图/轨迹回放)
- 疲劳事件热力图
- 车辆轨迹回放
- 时间轴控制

## API 配置

API 基础路径配置在 `src/api/index.js` 中：

```javascript
const API_BASE = '/api/v2/dashboard'
```

开发环境代理配置在 `vite.config.js` 中：

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

## 高德地图配置

高德地图 Key 配置在 `index.html` 中：

```html
<script type="text/javascript">
  window._AMapSecurityConfig = {
    securityJsCode: 'your-security-js-code',
  };
</script>
<script src="https://webapi.amap.com/maps?v=2.0&key=your-api-key"></script>
```

## 样式说明

项目使用全局 CSS 样式，主要特点：

- 科技感深色主题
- 玻璃拟态效果
- 响应式布局
- 动画效果

样式文件位于 `src/assets/styles/main.css`

## 注意事项

1. 确保后端 API 服务已启动（默认端口 8080）
2. 配置正确的高德地图 API Key
3. 浏览器需要支持 ES6+ 特性













