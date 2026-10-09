/**
 * 静默登录工具 —— FR-UC-001 / FR-UC-002
 *
 * 流程：uni.login 获取 code → POST /user/wx-login → 后端 code2Session
 * 换 openid → 自动注册（首次）→ 返回 JWT（7 天，FR-UC-005）
 */
import config from './config'
import { useUserStore } from '@/store/user'

/** 登录锁：并发触发静默登录时复用同一个 Promise */
let loginPromise = null

/**
 * 执行微信静默登录（对用户无感）
 * @returns {Promise<string>} 新 token
 */
export function silentLogin() {
  if (loginPromise) return loginPromise

  loginPromise = new Promise((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success: (loginRes) => {
        uni.request({
          url: `${config.BASE_URL}/user/wx-login`,
          method: 'POST',
          data: { code: loginRes.code },
          success: (res) => {
            const body = res.data || {}
            if (res.statusCode === 200 && body.code === 200) {
              const { token, expireSeconds, newUser, user } = body.data
              // FR-UC-005：token 持久化，请求层自动注入
              uni.setStorageSync(config.TOKEN_KEY, token)
              uni.setStorageSync('studyroom_token_expire_at', Date.now() + expireSeconds * 1000)
              useUserStore().setUser(user, newUser)
              resolve(token)
            } else {
              reject(new Error(body.message || '登录失败'))
            }
          },
          fail: (err) => reject(new Error('网络异常，登录失败'))
        })
      },
      fail: () => reject(new Error('uni.login 调用失败'))
    })
  }).finally(() => {
    loginPromise = null
  })

  return loginPromise
}

/** 获取本地 token */
export function getToken() {
  return uni.getStorageSync(config.TOKEN_KEY) || ''
}

/** 清除本地 token（登出） */
export function clearToken() {
  uni.removeStorageSync(config.TOKEN_KEY)
  uni.removeStorageSync('studyroom_token_expire_at')
}
