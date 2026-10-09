/**
 * 消息中心 API（FR-UC-044~047）
 */
import request from '@/utils/request'

/**
 * 消息列表（FR-UC-045：三类消息）
 * @param {number} type 1系统/2订单/3活动，不传查全部
 */
export const getMessages = (type, page = 1, size = 20) =>
  request.get('/message/list', { ...(type ? { type } : {}), page, size })

/** 未读消息数（FR-UC-045：tab 角标） */
export const getUnreadCount = () => request.get('/message/unread-count')

/** 标记单条已读（FR-UC-046） */
export const readMessage = (id) => request.post(`/message/${id}/read`)

/** 全部已读（FR-UC-046） */
export const readAllMessages = () => request.post('/message/read-all')

/** 删除单条消息（FR-UC-046：左滑逻辑删除） */
export const deleteMessage = (id) => request.del(`/message/${id}`)

/**
 * 订阅消息开关（FR-UC-047：六场景 orderSuccess/startSoon/endSoon/cancel/refund/timeout）
 * @param {string} scene 订阅场景
 * @param {number} accept 1订阅 2取消
 */
export const reportSubscribe = (scene, accept) =>
  request.post('/message/subscribe', { scene, accept })
