# Muse Pi Pro Plus 前端系统

基于 Vue 3 + Element Plus 的疲劳驾驶检测系统前端界面。

## 项目特性

- 🚀 **Vue 3** - 使用最新的 Vue 3 Composition API
- 🎨 **Element Plus** - 现代化的 UI 组件库
- 📦 **Pinia** - 轻量级状态管理
- 🔄 **WebSocket** - 实时数据通信
- 📱 **响应式设计** - 支持桌面端和移动端
- 🎯 **TypeScript 支持** - 类型安全（可选）

## 技术栈

- **框架**: Vue 3.5.18
- **UI库**: Element Plus 2.11.1
- **状态管理**: Pinia 3.0.3
- **路由**: Vue Router 4.5.1
- **HTTP客户端**: Axios 1.11.0
- **WebSocket**: Socket.IO Client 4.8.1
- **构建工具**: Vite 7.0.6
- **样式**: SCSS

## 项目结构

```
muse-frontend/
├── src/
│   ├── components/          # 组件目录
│   │   ├── VideoStream.vue  # 视频流组件
│   │   ├── DetectionPanel.vue # 检测结果面板
│   │   ├── StatusMonitor.vue  # 状态监控
│   │   ├── EventList.vue    # 事件列表
│   │   ├── ConfigPanel.vue  # 配置面板
│   │   ├── WeightPanel.vue  # 权重面板
│   │   ├── GPIOControl.vue  # GPIO控制
│   │   └── Navigation.vue   # 导航组件
│   ├── views/              # 页面视图
│   │   ├── HomeView.vue    # 主仪表板
│   │   ├── HistoryView.vue # 历史记录
│   │   └── SettingsView.vue # 设置页面
│   ├── stores/             # Pinia状态管理
│   │   ├── detection.js    # 检测数据状态
│   │   ├── system.js       # 系统状态
│   │   └── config.js       # 配置状态
│   ├── services/           # 服务层
│   │   ├── socket.js       # WebSocket服务
│   │   ├── api.js          # HTTP API服务
│   │   └── video.js        # 视频流服务
│   ├── router/             # 路由配置
│   └── assets/             # 静态资源
├── public/                 # 公共资源
│   └── static/            # 静态文件（音频、图片等）
└── package.json
```

## 核心功能

### 1. 实时视频流
- MJPEG视频流显示
- 视频连接状态监控
- 自动重连机制
- 错误处理和占位图显示

### 2. 检测结果展示
- 实时行为检测框和标签
- 疲劳等级可视化进度条
- 检测置信度显示
- 多级警报系统

### 3. 状态监控
- 疲劳等级实时更新
- 进度评分动态显示
- 分心次数统计
- CPU使用率监控
- FPS显示

### 4. 事件记录
- 实时事件列表
- 历史记录查看
- 事件筛选和搜索
- 数据导出功能

### 5. 配置管理
- 参数实时调整
- 权重动态修改
- 配置保存和恢复
- 表单验证

### 6. GPIO控制
- 手动触发硬件设备
- 音频提示测试
- 操作历史记录
- 自定义GPIO控制

## 安装和运行

### 环境要求
- Node.js >= 20.19.0
- npm 或 yarn

### 安装依赖
```bash
npm install
# 或
yarn install
```

### 开发模式
```bash
npm run dev
# 或
yarn dev
```

### 构建生产版本
```bash
npm run build
# 或
yarn build
```

### 预览生产版本
```bash
npm run preview
# 或
yarn preview
```

## 配置说明

### WebSocket连接
默认连接到 `ws://localhost:5200`，可在设置页面修改。

### 视频流地址
默认视频流地址为 `/feed/webcam/`，可在设置页面修改。

### 静态资源
所有静态资源（音频文件、图片等）放在 `public/static/` 目录下。

## API接口

### WebSocket事件

#### 接收事件
- `detection_update` - 检测结果更新
- `level_update` - 疲劳等级更新
- `events_update` - 事件列表更新
- `config_update` - 配置更新
- `weights_update` - 权重更新
- `error` - 错误信息
- `status` - 连接状态

#### 发送事件
- `update_config` - 更新配置
- `update_weights` - 更新权重
- `clear_events` - 清空事件
- `trigger_gpio` - 触发GPIO

### HTTP接口
- `GET /` - 主页面
- `GET /feed/webcam/` - 视频流
- `GET /feed/image/` - 图片流

## 状态管理

### Detection Store
管理检测相关的状态：
- 检测结果
- 疲劳状态
- 分心状态
- 事件列表
- 系统性能指标

### System Store
管理系统状态：
- 连接状态
- 错误信息
- 最后更新时间

### Config Store
管理配置信息：
- 检测参数
- 权重配置
- 默认值管理

## 响应式设计

支持多种屏幕尺寸：
- 桌面端 (1920x1080)
- 平板端 (1024x768)
- 移动端 (375x667)

## 浏览器兼容性

- Chrome >= 88
- Firefox >= 85
- Safari >= 14
- Edge >= 88

## 开发规范

### 代码规范
- 使用 ESLint + Prettier
- 遵循 Vue 3 最佳实践
- 组件命名使用 PascalCase
- 文件命名使用 kebab-case

### 性能优化
- 组件懒加载
- 图片懒加载
- 虚拟滚动（大数据列表）
- 防抖和节流

## 故障排除

### 常见问题

1. **WebSocket连接失败**
   - 检查后端服务是否启动
   - 确认端口号是否正确
   - 检查防火墙设置

2. **视频流无法显示**
   - 检查视频流地址是否正确
   - 确认后端视频服务是否正常
   - 查看浏览器控制台错误信息

3. **样式显示异常**
   - 清除浏览器缓存
   - 检查CSS文件是否正确加载
   - 确认Element Plus版本兼容性

## 更新日志

### v1.0.0 (2024-01-XX)
- 初始版本发布
- 实现所有核心功能
- 支持响应式设计
- 完整的WebSocket通信

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request 来改进项目。

## 联系方式

如有问题，请联系开发团队。