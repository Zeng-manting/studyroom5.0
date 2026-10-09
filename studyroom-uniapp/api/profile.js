/**
 * 个人中心 API（FR-UC-048/049/050/052）
 */
import request from '@/utils/request'

/** 钱包页（FR-UC-048：余额+信用分） */
export const getWallet = () => request.get('/profile/wallet')

/**
 * 充值（FR-UC-048：模拟微信支付直接到账，1~2000 元）
 * request 层自动预取 X-Idempotent-Token（FR-SY-001）
 */
export const recharge = (amount) =>
  request.post('/profile/recharge', { amount }, { idempotent: 'profile:recharge' })

/** 余额流水（FR-UC-048） */
export const getBalanceLogs = (page = 1, size = 20) =>
  request.get('/profile/balance-logs', { page, size })

/**
 * 学习统计（FR-UC-049：累计时长/天数/时段分布 24 数组/近 90 天热力图）
 */
export const getStudyStats = () => request.get('/profile/study-stats')

/** 同意隐私协议（FR-UC-050：幂等更新） */
export const agreePrivacy = () => request.post('/profile/privacy-agree')

/**
 * 提交意见反馈（FR-UC-052：4 类型，内容 5-1000 字）
 * request 层自动预取 X-Idempotent-Token（FR-SY-001）
 */
export const submitFeedback = (data) =>
  request.post('/profile/feedback', data, { idempotent: 'profile:feedback' })
