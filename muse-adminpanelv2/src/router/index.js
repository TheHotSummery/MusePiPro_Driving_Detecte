import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue')
  },
  {
    path: '/monitor',
    name: 'Monitor',
    component: () => import('@/views/Monitor.vue')
  },
  {
    path: '/report',
    name: 'Report',
    component: () => import('@/views/Report.vue')
  },
  {
    path: '/heatmap',
    name: 'Heatmap',
    component: () => import('@/views/Heatmap.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router













