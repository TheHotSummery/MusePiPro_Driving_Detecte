<template>
  <div class="nav-bar">
    <router-link to="/monitor" class="nav-item">
      <span class="nav-icon nav-icon-cyan">🚗</span>
      <span class="nav-label">车辆监控</span>
    </router-link>
    <router-link to="/heatmap" class="nav-item">
      <span class="nav-icon nav-icon-red">🔥</span>
      <span class="nav-label">疲劳热力</span>
    </router-link>
    <router-link to="/report" class="nav-item">
      <span class="nav-icon nav-icon-green">📊</span>
      <span class="nav-label">数据报表</span>
    </router-link>
  </div>
</template>

<script setup>
// 导航栏组件 - 底部三个按钮：车辆监控、疲劳热力、数据报表
</script>

<style scoped>
.nav-bar {
  position: absolute;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  background: rgba(13, 25, 48, 0.6);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(0, 242, 255, 0.1);
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.5);
  padding: 8px 32px;
  border-radius: 9999px;
  display: flex;
  gap: 32px;
  pointer-events: auto;
  clip-path: polygon(
    0 0,
    100% 0,
    100% calc(100% - 20px),
    calc(100% - 20px) 100%,
    0 100%
  );
}

.nav-bar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, transparent, #00f2ff, transparent);
}

.nav-bar::after {
  content: '';
  position: absolute;
  bottom: 0;
  right: 0;
  width: 20px;
  height: 20px;
  border-bottom: 2px solid #00f2ff;
  border-right: 2px solid #00f2ff;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  text-decoration: none;
  color: inherit;
  transition: all 0.3s;
}

.nav-item:hover .nav-icon {
  color: white;
}

.nav-icon {
  font-size: 20px;
  transition: color 0.3s;
  display: block;
}

.nav-icon-cyan {
  color: #22d3ee;
}

.nav-icon-red {
  color: #f87171;
}

.nav-icon-green {
  color: #34d399;
}

.nav-label {
  font-size: 10px;
  color: #9ca3af;
  margin-top: 4px;
}
</style>

