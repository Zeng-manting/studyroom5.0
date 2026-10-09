import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'

/**
 * 管理端路由 + 守卫 —— FR-AD-001
 *
 * - 未登录访问任何页面 → 跳转 /login（携带 redirect 回跳）
 * - 已登录访问 /login → 跳转首页
 * - 后续模块（10~14）在 layout 子路由下追加各管理页
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', icon: 'Odometer' }
      },
      {
        path: 'order',
        name: 'OrderList',
        component: () => import('@/views/order/index.vue'),
        meta: { title: '订单列表' }
      },
      {
        path: 'order/appeal',
        name: 'OrderAppeal',
        component: () => import('@/views/order/appeal.vue'),
        meta: { title: '申诉处理' }
      },
      {
        path: 'store',
        name: 'StoreInfo',
        component: () => import('@/views/store/index.vue'),
        meta: { title: '门店信息' }
      },
      {
        path: 'store/blacklist',
        name: 'StoreBlacklist',
        component: () => import('@/views/store/blacklist.vue'),
        meta: { title: '时段黑名单' }
      },
      {
        path: 'store/seats',
        name: 'StoreSeats',
        component: () => import('@/views/store/seats.vue'),
        meta: { title: '座位管理' }
      },
      {
        path: 'store/qrcode',
        name: 'StoreQrcode',
        component: () => import('@/views/store/qrcode.vue'),
        meta: { title: '签到二维码' }
      },
      {
        path: 'user',
        name: 'UserList',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'notice',
        name: 'NoticeList',
        component: () => import('@/views/notice/index.vue'),
        meta: { title: '公告管理' }
      },
      {
        path: 'review',
        name: 'ReviewList',
        component: () => import('@/views/review/index.vue'),
        meta: { title: '评价管理' }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/statistics/index.vue'),
        meta: { title: '数据统计' }
      },
      {
        path: 'finance',
        name: 'Finance',
        component: () => import('@/views/finance/index.vue'),
        meta: { title: '财务总览' }
      },
      {
        path: 'coupon',
        name: 'Coupon',
        component: () => import('@/views/coupon/index.vue'),
        meta: { title: '优惠券管理' }
      },
      {
        path: 'setting/price',
        name: 'PriceSetting',
        component: () => import('@/views/setting/price.vue'),
        meta: { title: '价格配置' }
      },
      {
        path: 'setting/config',
        name: 'SystemConfig',
        component: () => import('@/views/setting/config.vue'),
        meta: { title: '系统配置' }
      },
      {
        path: 'log',
        name: 'AdminLog',
        component: () => import('@/views/log/index.vue'),
        meta: { title: '操作日志' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.path !== '/login' && !authStore.isLogged) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && authStore.isLogged) {
    return { path: '/' }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta?.title
    ? `${to.meta.title} - 无人自习室管理后台`
    : '无人自习室管理后台'
})

export default router
