/**
 * 统一请求封装 —— FR-UC-005 Token 自动注入 / FR-UC-006 401 静默重登与请求重放
 *
 * 核心机制：
 * 1. 每个请求自动携带 Authorization: Bearer <token>（FR-UC-005）
 * 2. 收到 401 时：静默调用 silentLogin 重新登录（FR-UC-006）
 *    - 并发 401 请求进入等待队列，登录成功后统一重放（只触发一次登录）
 *    - 重放仍 401 → 清除本地态并提示
 * 3. 统一解析 { code, message, data }，业务失败直接 reject
 *
 * 幂等接口（下单/签到等）通过 options.idempotent = 'order:create' 声明，
 * 请求前自动预取 X-Idempotent-Token（FR-SY-001，服务端一次性消费）。
 */
import config from './config'
import { getToken, silentLogin } from './auth'

/** 是否正在静默重登 */
let refreshing = false
/** 重登期间到达的 401 请求等待队列 */
let pendingQueue = []

function onRefreshed(newToken) {
  pendingQueue.forEach(cb => cb(newToken))
  pendingQueue = []
}

/** 预取幂等 token（FR-SY-001） */
async function fetchIdempotentToken(biz) {
  return new Promise((resolve) => {
    uni.request({
      url: `${config.BASE_URL}/idempotent/token?biz=${encodeURIComponent(biz)}&ttl=60`,
      header: { Authorization: `Bearer ${getToken()}` },
      success: (res) => {
        const body = res.data || {}
        resolve(body.code === 200 ? body.data.token : '')
      },
      fail: () => resolve('')
    })
  })
}

/**
 * 发起请求
 * @param {Object} options uni.request 参数 + { idempotent?: string }
 */
function request(options) {
  return new Promise(async (resolve, reject) => {
    const doRequest = (token) => {
      const header = Object.assign({ Authorization: `Bearer ${token}` }, options.header || {})
      uni.request({
        ...options,
        url: `${config.BASE_URL}${options.url}`,
        header,
        success: async (res) => {
          const body = res.data || {}
          // ---------- FR-UC-006：401 静默重登 + 重放 ----------
          if (res.statusCode === 401) {
            handle401(options, resolve, reject)
            return
          }
          if (res.statusCode === 429) {
            uni.showToast({ title: body.message || '操作过于频繁', icon: 'none' })
            reject({ code: 429, message: body.message })
            return
          }
          if (res.statusCode >= 400) {
            uni.showToast({ title: body.message || `请求失败(${res.statusCode})`, icon: 'none' })
            reject({ code: res.statusCode, message: body.message })
            return
          }
          if (body.code === 200) {
            resolve(body.data)
          } else {
            // 业务错误（600 等）
            uni.showToast({ title: body.message || '操作失败', icon: 'none' })
            reject({ code: body.code, message: body.message })
          }
        },
        fail: () => {
          uni.showToast({ title: '网络异常，请检查网络', icon: 'none' })
          reject({ code: -1, message: '网络异常' })
        }
      })
    }

    // 幂等 token 预取（FR-SY-001）
    if (options.idempotent) {
      const t = await fetchIdempotentToken(options.idempotent)
      if (t) {
        options = {
          ...options,
          header: { ...(options.header || {}), 'X-Idempotent-Token': t }
        }
      }
    }
    doRequest(getToken())
  })
}

/** 401 处理：静默重登后重放（FR-UC-006） */
function handle401(options, resolve, reject) {
  if (refreshing) {
    // 已有登录进行中：挂入队列等待重放
    pendingQueue.push((newToken) => {
      options.header = { ...(options.header || {}), Authorization: `Bearer ${newToken}` }
      request(options).then(resolve).catch(reject)
    })
    return
  }
  refreshing = true
  silentLogin()
    .then((newToken) => {
      refreshing = false
      onRefreshed(newToken)
      // 重放当前请求
      options.header = { ...(options.header || {}), Authorization: `Bearer ${newToken}` }
      request(options).then(resolve).catch(reject)
    })
    .catch(() => {
      refreshing = false
      // 重登失败：清空队列
      pendingQueue = []
      // 从未登录过（本地无 token）：静默引导到登录页，不提示"登录失效"
      if (!getToken()) {
        reject({ code: 401, message: '请先登录' })
        return
      }
      uni.showToast({ title: '登录失效，请重新进入小程序', icon: 'none' })
      reject({ code: 401, message: '登录失效' })
    })
}

/* ============ 便捷方法 ============ */
request.get = (url, data, extra = {}) => request({ url, method: 'GET', data, ...extra })
request.post = (url, data, extra = {}) => request({ url, method: 'POST', data, ...extra })
request.put = (url, data, extra = {}) => request({ url, method: 'PUT', data, ...extra })
request.del = (url, data, extra = {}) => request({ url, method: 'DELETE', data, ...extra })

/**
 * 文件上传（FR-UC-003 头像 / FR-UC-039 评价图片）
 * @param {string} filePath 本地临时文件路径
 * @param {string} scene avatar / review / feedback
 * @returns {Promise<string>} 图片 URL
 */
request.upload = (filePath, scene) => {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${config.BASE_URL}/file/upload`,
      filePath,
      name: 'file',
      formData: { scene },
      header: { Authorization: `Bearer ${getToken()}` },
      success: (res) => {
        const body = JSON.parse(res.data || '{}')
        if (res.statusCode === 200 && body.code === 200) {
          resolve(body.data)
        } else {
          uni.showToast({ title: body.message || '上传失败', icon: 'none' })
          reject(body)
        }
      },
      fail: () => {
        uni.showToast({ title: '上传失败', icon: 'none' })
        reject({ code: -1 })
      }
    })
  })
}

export default request
