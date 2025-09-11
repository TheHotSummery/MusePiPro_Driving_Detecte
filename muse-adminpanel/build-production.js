#!/usr/bin/env node

// 生产环境构建脚本
// 确保使用正确的环境变量进行构建

import { execSync } from 'child_process'
import fs from 'fs'
import path from 'path'

console.log('🚀 开始生产环境构建...')

// 设置生产环境变量
const envVars = {
  VITE_API_BASE_URL: 'http://spacemit.topcoder.fun',
  VITE_WS_URL: 'ws://spacemit.topcoder.fun/websocket',
  VITE_AMAP_API_KEY: '732a364f70864bb0c4ac3395cc3d5503',
  VITE_AMAP_SECURITY_KEY: 'a0f65e66806bd8b70202e9d270bf74ca',
  VITE_AMAP_VERSION: '1.4.15',
  VITE_APP_TITLE: 'Muse 云端实时检测平台',
  VITE_APP_VERSION: '1.2.0',
  VITE_DEV_MODE: 'production',
  VITE_DEBUG: 'false'
}

// 创建临时环境文件
const envContent = Object.entries(envVars)
  .map(([key, value]) => `${key}=${value}`)
  .join('\n')

fs.writeFileSync('.env.production', envContent)
console.log('✅ 已创建生产环境配置文件')

try {
  // 执行构建
  console.log('📦 开始构建...')
  execSync('npm run build', { 
    stdio: 'inherit',
    env: { ...process.env, ...envVars }
  })
  
  console.log('✅ 构建完成！')
  
  // 验证构建结果
  const distPath = path.join(process.cwd(), 'dist')
  if (fs.existsSync(distPath)) {
    console.log('📁 构建文件已生成到 dist/ 目录')
    
    // 检查关键文件
    const indexHtml = path.join(distPath, 'index.html')
    if (fs.existsSync(indexHtml)) {
      const content = fs.readFileSync(indexHtml, 'utf8')
      if (content.includes('spacemit.topcoder.fun')) {
        console.log('✅ 构建文件中包含正确的域名配置')
      } else {
        console.log('⚠️ 警告：构建文件中可能未包含正确的域名配置')
      }
    }
  }
  
} catch (error) {
  console.error('❌ 构建失败:', error.message)
  process.exit(1)
} finally {
  // 清理临时文件
  if (fs.existsSync('.env.production')) {
    fs.unlinkSync('.env.production')
    console.log('🧹 已清理临时文件')
  }
}

console.log('🎉 生产环境构建完成！')
