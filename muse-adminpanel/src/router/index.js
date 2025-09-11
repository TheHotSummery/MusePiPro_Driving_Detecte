import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '@/views/Dashboard.vue'
import DeviceDetail from '@/views/DeviceDetail.vue'
import EventManagement from '@/views/EventManagement.vue'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: {
      title: '实时监控仪表板'
    }
  },
  {
    path: '/devices/:deviceId',
    name: 'DeviceDetail',
    component: DeviceDetail,
    meta: {
      title: '设备详情'
    }
  },
  {
    path: '/events',
    name: 'EventManagement',
    component: EventManagement,
    meta: {
      title: '事件管理'
    }
  },
  {
    path: '/alerts',
    name: 'AlertCenter',
    component: () => import('@/views/AlertCenter.vue'),
    meta: {
      title: '告警中心'
    }
  },
  {
    path: '/analytics',
    name: 'Analytics',
    component: () => import('@/views/Analytics.vue'),
    meta: {
      title: '数据分析'
    }
  },
  {
    path: '/users',
    name: 'UserManagement',
    component: () => import('@/views/UserManagement.vue'),
    meta: {
      title: '用户管理'
    }
  },
  {
    path: '/websocket-test',
    name: 'WebSocketTest',
    component: () => import('@/views/WebSocketTest.vue'),
    meta: {
      title: 'WebSocket测试'
    }
  },
  {
    path: '/map-config-help',
    name: 'MapConfigHelp',
    component: () => import('@/views/MapConfigHelp.vue'),
    meta: {
      title: '地图配置帮助'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - Muse 云端实时检测平台`
  }
  next()
})

export default router