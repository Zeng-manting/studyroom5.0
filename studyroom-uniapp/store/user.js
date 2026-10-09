/**
 * 用户全局状态 —— Pinia（技术选型 1.2：状态管理用户信息/选座临时数据/订单状态）
 */
import { defineStore } from 'pinia'
import { silentLogin } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    /** 用户信息（UserVO） */
    userInfo: null,
    /** 是否新用户（首次自动注册，FR-UC-002） */
    isNewUser: false
  }),

  getters: {
    isLogged: (state) => !!state.userInfo,
    emailBound: (state) => !!(state.userInfo && state.userInfo.emailBound)
  },

  actions: {
    setUser(user, isNewUser = false) {
      this.userInfo = user
      this.isNewUser = isNewUser
    },

    /** 拉取最新用户信息（GET /user/me） */
    async fetchUserInfo() {
      const request = (await import('@/utils/request')).default
      const user = await request.get('/user/me')
      this.userInfo = user
      return user
    },

    /** 应用启动：静默登录（FR-UC-001，对用户无感） */
    async ensureLogin() {
      if (this.userInfo) return this.userInfo
      await silentLogin()
      return this.fetchUserInfo()
    }
  }
})

export default useUserStore
