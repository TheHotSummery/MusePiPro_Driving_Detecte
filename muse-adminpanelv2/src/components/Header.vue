<template>
  <header class="absolute top-0 left-0 w-full h-20 z-50 flex justify-between items-center px-8 bg-gradient-to-b from-black/80 to-transparent pointer-events-none">
    <div class="flex items-center gap-4 pointer-events-auto">
      <!-- 简易Logo -->
      <div class="w-10 h-10 border-2 border-cyan-400 rounded-full flex items-center justify-center animate-pulse shadow-[0_0_15px_#00f2ff]">
        <span class="font-tech font-bold text-xl text-cyan-400">R</span>
      </div>
      <div>
        <h1 class="text-3xl font-bold header-title tracking-wider">RISC-V 疲劳驾驶检测平台</h1>
        <p class="text-xs text-cyan-200/60 tracking-[0.2em] uppercase">Real-time Driver Monitoring System</p>
      </div>
    </div>
    
    <div class="flex items-center gap-6 pointer-events-auto">
      <div class="text-right">
        <div class="text-2xl font-tech text-white">{{ currentTime }}</div>
        <div class="text-xs text-gray-400">{{ currentDate }}</div>
      </div>
      <!-- 状态指示灯 -->
      <div class="flex gap-2">
        <span class="px-3 py-1 rounded bg-green-500/20 border border-green-500 text-green-400 text-xs">SYSTEM ONLINE</span>
        <span class="px-3 py-1 rounded bg-blue-500/20 border border-blue-500 text-blue-400 text-xs">MAP READY</span>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { formatTime, formatDate } from '@/utils'

const currentTime = ref('')
const currentDate = ref('')

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('en-GB') // 24h format
  currentDate.value = formatDate(now.getTime())
}

let timeInterval = null

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval)
  }
})
</script>













