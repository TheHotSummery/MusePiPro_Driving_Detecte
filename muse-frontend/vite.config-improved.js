import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ command, mode }) => {
  // 加载环境变量
  const env = loadEnv(mode, process.cwd(), '')
  
  return {
    plugins: [
      vue(),
      vueDevTools(),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
    
    // 开发服务器配置
    server: {
      host: '0.0.0.0', // 允许外部访问
      port: 3000,
      proxy: {
        // 代理API请求到后端
        '/api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:5200',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '')
        },
        // 代理WebSocket连接
        '/socket.io': {
          target: env.VITE_WS_URL || 'ws://localhost:5200',
          ws: true,
          changeOrigin: true
        }
      }
    },
    
    // 构建配置
    build: {
      outDir: 'dist',
      assetsDir: 'static', // 静态资源文件夹
      sourcemap: false, // 生产环境不生成sourcemap
      rollupOptions: {
        output: {
          // 分包策略
          manualChunks: {
            vendor: ['vue', 'vue-router', 'pinia'],
            element: ['element-plus'],
            socket: ['socket.io-client']
          }
        }
      }
    },
    
    // 环境变量配置
    define: {
      __APP_VERSION__: JSON.stringify(process.env.npm_package_version || '1.0.0')
    }
  }
})
