# API配置说明

## 配置文件位置

API地址配置位于：`src/config/api.js`

## 当前配置

默认配置为您的后端地址：`http://192.168.110.31:5200`

## 如何修改API地址

### 方法1：直接修改配置文件

编辑 `src/config/api.js` 文件中的配置：

```javascript
const config = {
  // 开发环境配置
  development: {
    API_BASE_URL: 'http://192.168.110.31:5200',  // 修改这里
    WS_URL: 'ws://192.168.110.31:5200',          // 修改这里
    VIDEO_URL: '/feed/webcam/',
    IMAGE_URL: '/feed/image/'
  },
  
  // 生产环境配置
  production: {
    API_BASE_URL: 'http://192.168.110.31:5200',  // 修改这里
    WS_URL: 'ws://192.168.110.31:5200',          // 修改这里
    VIDEO_URL: '/feed/webcam/',
    IMAGE_URL: '/feed/image/'
  },
  
  // 本地测试配置
  local: {
    API_BASE_URL: 'http://localhost:5200',        // 本地地址
    WS_URL: 'ws://localhost:5200',                // 本地地址
    VIDEO_URL: '/feed/webcam/',
    IMAGE_URL: '/feed/image/'
  }
}
```

### 方法2：使用环境切换器

在页面右上角有一个环境切换器，可以快速切换不同的API地址：

- **开发环境**: `192.168.110.31:5200`
- **生产环境**: `192.168.110.31:5200` 
- **本地环境**: `localhost:5200`

### 方法3：通过URL参数切换

在浏览器地址栏添加 `?env=环境名` 参数：

- `http://localhost:5173/?env=development` - 使用开发环境
- `http://localhost:5173/?env=production` - 使用生产环境  
- `http://localhost:5173/?env=local` - 使用本地环境

## 配置项说明

- `API_BASE_URL`: HTTP API的基础地址
- `WS_URL`: WebSocket连接地址
- `VIDEO_URL`: 视频流路径
- `IMAGE_URL`: 图片流路径

## 添加新的环境配置

1. 在 `config` 对象中添加新的环境配置
2. 在 `getEnvDisplayName` 方法中添加显示名称
3. 重启开发服务器

## 注意事项

- 修改配置后需要重启开发服务器才能生效
- WebSocket地址和HTTP地址需要保持一致的主机和端口
- 确保后端服务正在运行且地址正确




