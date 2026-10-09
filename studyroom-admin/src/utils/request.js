import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import router from '@/router'

/**
 * Axios 统一封装（管理端）—— FR-AD-001
 *
 * - 请求拦截：自动注入 Authorization: Bearer {token}
 * - 响应拦截：统一解析 { code, message, data }
 *   - code=200 → 直接返回 data（业务层零包装）
 *   - code=401 → 清除本地凭证并跳转登录页（JWT 2 小时过期，FR-AD-001）
 *   - 其他 → ElMessage 报错并 reject
 * - HTTP 层错误（超时/网络/5xx）同样统一提示
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('ADMIN_TOKEN')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (!res || typeof res.code === 'undefined') {
      return res
    }
    if (res.code === 200) {
      return res.data
    }
    // 401：JWT 过期或无效 —— 清凭证 + 跳登录页（FR-AD-001）
    if (res.code === 401) {
      const authStore = useAuthStore()
      authStore.clearLocal()
      if (router.currentRoute.value.path !== '/login') {
        ElMessage.error(res.message || '登录已过期，请重新登录')
        router.replace({
          path: '/login',
          query: { redirect: router.currentRoute.value.fullPath }
        })
      }
      return Promise.reject(new Error(res.message || '未登录'))
    }
    ElMessage.error(res.message || '操作失败')
    return Promise.reject(new Error(res.message || '操作失败'))
  },
  (error) => {
    // HTTP 状态层错误（含 Security 过滤器直出的 401/403）
    const status = error.response?.status
    const msg = error.response?.data?.message
    if (status === 401) {
      const authStore = useAuthStore()
      authStore.clearLocal()
      if (router.currentRoute.value.path !== '/login') {
        ElMessage.error(msg || '登录已过期，请重新登录')
        router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
      }
    } else {
      ElMessage.error(msg || error.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
