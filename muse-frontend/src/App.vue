<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <RouterView />
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  font-family: 'Roboto', 'Helvetica Neue', Arial, sans-serif;
}

#app {
  min-height: 100vh;
  width: 100%;
  background: linear-gradient(135deg, #064e3b 0%, #065f46 50%, #047857 100%);
}

// 全局Element Plus样式覆盖
.el-message {
  z-index: 9999;
}

.el-message-box {
  z-index: 9999;
}

// 自定义滚动条
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: rgba(31, 41, 55, 0.3);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #10b981;
  border-radius: 4px;
  
  &:hover {
    background: #059669;
  }
}

::-webkit-scrollbar-corner {
  background: rgba(31, 41, 55, 0.3);
}
</style>
