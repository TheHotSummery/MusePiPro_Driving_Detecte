<template>
  <div class="map-config-help">
    <el-card>
      <template #header>
        <span>高德地图配置帮助</span>
      </template>
      
      <div class="help-content">
        <el-alert
          title="重要提示"
          type="warning"
          :closable="false"
          style="margin-bottom: 20px;"
        >
          <p>2021年后申请的高德地图API Key需要配置安全密钥才能正常使用。</p>
        </el-alert>

        <el-steps :active="currentStep" finish-status="success">
          <el-step title="申请API Key" description="在高德开放平台申请Web服务API Key" />
          <el-step title="配置安全密钥" description="生成并配置安全密钥" />
          <el-step title="配置应用" description="在应用中配置API Key和安全密钥" />
        </el-steps>

        <div class="step-content">
          <!-- 步骤1：申请API Key -->
          <div v-if="currentStep === 0" class="step-panel">
            <h3>1. 申请API Key</h3>
            <ol>
              <li>访问 <a href="https://console.amap.com/" target="_blank">高德开放平台控制台</a></li>
              <li>登录或注册账号</li>
              <li>创建应用，选择"Web端(JS API)"</li>
              <li>在应用配置中启用以下服务：
                <ul>
                  <li>基础地图服务</li>
                  <li>基础定位服务</li>
                  <li>基础搜索服务</li>
                </ul>
              </li>
              <li>获取API Key</li>
            </ol>
          </div>

          <!-- 步骤2：配置安全密钥 -->
          <div v-if="currentStep === 1" class="step-panel">
            <h3>2. 配置安全密钥</h3>
            <ol>
              <li>在应用详情页面，找到"安全密钥"配置</li>
              <li>点击"生成安全密钥"</li>
              <li>记录生成的安全密钥（只显示一次）</li>
              <li>配置应用域名白名单（可选，建议配置）</li>
            </ol>
            
            <el-alert
              title="注意"
              type="info"
              :closable="false"
              style="margin-top: 15px;"
            >
              <p>安全密钥只显示一次，请妥善保存。如果丢失，需要重新生成。</p>
            </el-alert>
          </div>

          <!-- 步骤3：配置应用 -->
          <div v-if="currentStep === 2" class="step-panel">
            <h3>3. 在应用中配置</h3>
            <ol>
              <li>在平台中点击"地图配置"按钮</li>
              <li>输入API Key</li>
              <li>输入安全密钥（如果有）</li>
              <li>选择API版本：
                <ul>
                  <li><strong>基础版 v1.4.15</strong>：功能较少但调用限制较少，适合基础使用</li>
                  <li><strong>标准版 v2.0</strong>：功能完整但有限制，适合高级功能</li>
                </ul>
              </li>
              <li>点击保存</li>
            </ol>
          </div>
        </div>

        <div class="step-actions">
          <el-button @click="prevStep" :disabled="currentStep === 0">上一步</el-button>
          <el-button type="primary" @click="nextStep" :disabled="currentStep === 2">下一步</el-button>
          <el-button type="success" @click="goToConfig">去配置</el-button>
        </div>

        <el-divider />

        <div class="troubleshooting">
          <h3>常见问题</h3>
          <el-collapse>
            <el-collapse-item title="API Key错误" name="1">
              <p>请检查：</p>
              <ul>
                <li>API Key是否正确复制</li>
                <li>是否启用了Web端(JS API)服务</li>
                <li>是否配置了安全密钥（2021年后申请的Key）</li>
              </ul>
            </el-collapse-item>
            <el-collapse-item title="地图不显示" name="2">
              <p>请检查：</p>
              <ul>
                <li>网络连接是否正常</li>
                <li>浏览器控制台是否有错误信息</li>
                <li>API Key是否有效</li>
                <li>是否配置了域名白名单</li>
              </ul>
            </el-collapse-item>
            <el-collapse-item title="调用超限" name="3">
              <p>解决方案：</p>
              <ul>
                <li>使用基础版API（v1.4.15）减少调用量</li>
                <li>升级到付费版本</li>
                <li>优化应用，减少不必要的API调用</li>
              </ul>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const currentStep = ref(0)

const nextStep = () => {
  if (currentStep.value < 2) {
    currentStep.value++
  }
}

const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

const goToConfig = () => {
  router.push('/')
  // 这里可以触发地图配置对话框
  setTimeout(() => {
    // 可以通过事件总线或其他方式触发配置对话框
    window.dispatchEvent(new CustomEvent('show-map-config'))
  }, 100)
}
</script>

<style scoped lang="scss">
.map-config-help {
  padding: 20px;
  height: calc(100vh - 60px);
  overflow-y: auto;
}

.help-content {
  max-width: 800px;
  margin: 0 auto;
}

.step-content {
  margin: 30px 0;
  min-height: 300px;
}

.step-panel {
  h3 {
    color: #303133;
    margin-bottom: 15px;
  }

  ol {
    line-height: 1.8;
    
    li {
      margin-bottom: 8px;
    }
    
    ul {
      margin-top: 8px;
      margin-left: 20px;
      
      li {
        margin-bottom: 4px;
      }
    }
  }
}

.step-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin: 30px 0;
}

.troubleshooting {
  h3 {
    color: #303133;
    margin-bottom: 15px;
  }
}

a {
  color: #409EFF;
  text-decoration: none;
  
  &:hover {
    text-decoration: underline;
  }
}
</style>

