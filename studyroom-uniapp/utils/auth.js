/**
 * 登录工具 —— 双端适配
 *
 * 微信小程序：uni.login 获取 code → POST /user/wx-login → 后端 code2Session（FR-UC-001）
 * H5：无微信登录能力，使用账号登录 → POST /user/h5-login（首次自动注册）
 */
import config from './config'
import { useUserStore } from '@/store/user'

/** 登录锁：并发触发静默登录时复用同一个 Promise */
let loginPromise = null

/** 登录成功后的统一落地：token / 过期时间 / 用户状态 */
function applyLoginResult(data) {
  const { token, expireSeconds, newUser, user } = data
  // FR-UC-005：token 持久化，请求层自动注入
  uni.setStorageSync(config.TOKEN_KEY, token)
  uni.setStorageSync('studyroom_token_expire_at', Date.now() + expireSeconds * 1000)
  useUserStore().setUser(user, newUser)
  return token
}

/** 登录接口请求（两端共用） */
function postLogin(path, data) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${config.BASE_URL}${path}`,
      method: 'POST',
      data,
      success: (res) => {
        const body = res.data || {}
        if (res.statusCode === 200 && body.code === 200) {
          resolve(applyLoginResult(body.data))
        } else {
          reject(new Error(body.message || '登录失败'))
        }
      },
      fail: () => reject(new Error('网络异常，登录失败'))
    })
  })
}

/**
 * 执行静默登录（对用户无感）
 * @returns {Promise<string>} 新 token
 */
export function silentLogin() {
  if (loginPromise) return loginPromise

  loginPromise = doSilentLogin().finally(() => {
    loginPromise = null
  })

  return loginPromise
}

// #ifdef H5
/** 登录页跳转去重：并发 401 时只跳一次 */
let redirectingToLogin = false

function gotoLoginPage() {
  if (redirectingToLogin) return
  const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : []
  const current = pages.length ? (pages[pages.length - 1].route || '') : ''
  if (current.indexOf('pages/login/index') === 0) return
  redirectingToLogin = true
  uni.reLaunch({
    url: '/pages/login/index',
    complete: () => {
      redirectingToLogin = false
    }
  })
}

/** H5：用本地保存的账号静默登录；没有账号则跳转登录页 */
function doSilentLogin() {
  const account = uni.getStorageSync(config.H5_ACCOUNT_KEY)
  if (!account) {
    gotoLoginPage()
    return Promise.reject(new Error('请先登录'))
  }
  return postLogin('/user/h5-login', { account })
}

/** H5 登录页调用：账号登录并记住账号（下次静默登录复用） */
export function loginByAccount(account) {
  return postLogin('/user/h5-login', { account }).then((token) => {
    uni.setStorageSync(config.H5_ACCOUNT_KEY, account)
    return token
  })
}
// #endif

// #ifndef H5
/** 微信小程序：静默登录 */
function doSilentLogin() {
  return new Promise((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success: (loginRes) => {
        postLogin('/user/wx-login', { code: loginRes.code }).then(resolve).catch(reject)
      },
      fail: () => reject(new Error('uni.login 调用失败'))
    })
  })
}
// #endif

/** 获取本地 token */
export function getToken() {
  return uni.getStorageSync(config.TOKEN_KEY) || ''
}

/** 清除本地 token（登出） */
export function clearToken() {
  uni.removeStorageSync(config.TOKEN_KEY)
  uni.removeStorageSync('studyroom_token_expire_at')
  // #ifdef H5
  // H5 登出同时清除账号，下次进入重新选择账号
  uni.removeStorageSync(config.H5_ACCOUNT_KEY)
  // #endif
}
