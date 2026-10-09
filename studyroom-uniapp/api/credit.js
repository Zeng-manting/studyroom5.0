/**
 * 信用 API（FR-UC-053/054，模块8）
 */
import request from '@/utils/request'

/**
 * 我的信用（FR-UC-053/054）
 * 信用分、限制状态与到期时间、30 天爽约次数、爽约明细、信用变更流水
 */
export const getMyCredit = () => request.get('/credit/me')
