<template>
  <div class="icon-test">
    <h3>图标测试</h3>
    <div class="icon-grid">
      <div v-for="(icon, name) in testIcons" :key="name" class="icon-item">
        <el-icon><component :is="icon" /></el-icon>
        <span>{{ name }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { 
  Monitor, 
  Clock, 
  Setting, 
  Connection, 
  Close,
  VideoCamera,
  VideoPlay,
  VideoPause,
  Refresh,
  Upload,
  Download,
  Delete,
  Check,
  Warning,
  InfoFilled,
  List,
  View,
  Tools,
  Bell,
  Microphone,
  ArrowDownBold,
  Cpu,
  Timer
} from '@element-plus/icons-vue'

const testIcons = {
  Monitor,
  Clock,
  Setting,
  Connection,
  Close,
  VideoCamera,
  VideoPlay,
  VideoPause,
  Refresh,
  Upload,
  Download,
  Delete,
  Check,
  Warning,
  InfoFilled,
  List,
  View,
  Tools,
  Bell,
  Microphone,
  ArrowDownBold,
  Cpu,
  Timer
}
</script>

<style scoped>
.icon-test {
  padding: 20px;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.icon-item span {
  font-size: 12px;
  text-align: center;
}
</style>
