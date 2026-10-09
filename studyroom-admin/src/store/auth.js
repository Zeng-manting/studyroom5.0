import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getMe } from '@/api/auth'

const TOKEN_KEY = 'ADMIN_TOKEN'
const ADMIN_KEY = 'ADMIN_INFO'

/**
 * 管理端认证状态 —— FR-AD-001
 * token 存 localStorage（JWT 2 小时，过期由后端 401 + 前端拦截器兜底）
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    admin: JSON.parse(localStorage.getItem(ADMIN_KEY) || 'null')
  }),

  getters: {
    isLogged: (state) => !!state.token,
    displayName: (state) =>
      state.admin?.realName || state.admin?.username || '管理员',
    roleName: (state) => state.admin?.roleName || ''
  },

  actions: {
    /** 登录（FR-AD-001）：token + 管理员信息落地 */
    async login(loginForm) {
      const data = await loginApi(loginForm)
      this.token = data.token
      this.admin = data.admin
      localStorage.setItem(TOKEN_KEY, data.token)
      localStorage.setItem(ADMIN_KEY, JSON.stringify(data.admin))
      return data
    },

    /** 刷新管理员信息（顶栏展示） */
    async fetchAdminInfo() {
      this.admin = await getMe()
      localStorage.setItem(ADMIN_KEY, JSON.stringify(this.admin))
    },

    /** 退出登录（FR-AD-001：后端 token 拉黑 + 本地清凭证） */
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // 后端拉黑失败不阻断本地退出
      }
      this.clearLocal()
    },

    /** 仅清除本地凭证（401 被动登出时使用） */
    clearLocal() {
      this.token = ''
      this.admin = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(ADMIN_KEY)
    }
  }
})
