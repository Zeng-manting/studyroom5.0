import request from '@/utils/request'

/**
 * 管理端门店与座位管理 API —— FR-AD-004 ~ FR-AD-008
 */

/* ============ FR-AD-004 门店信息 ============ */

/** 门店列表（含维护中） */
export const listStores = () => request.get('/admin/store/list')

/** 门店详情 */
export const getStore = (storeId) => request.get(`/admin/store/${storeId}`)

/** 新增门店 */
export const createStore = (data) => request.post('/admin/store', data)

/** 编辑门店信息 */
export const updateStore = (storeId, data) => request.put(`/admin/store/${storeId}`, data)

/* ============ FR-AD-005 时段黑名单 ============ */

/** 黑名单规则列表 */
export const listBlacklistRules = (storeId) =>
  request.get(`/admin/store/${storeId}/blacklist/rules`)

/** 新增黑名单规则 */
export const addBlacklistRule = (storeId, data) =>
  request.post(`/admin/store/${storeId}/blacklist`, data)

/** 编辑黑名单规则 */
export const updateBlacklistRule = (ruleId, data) =>
  request.put(`/admin/blacklist/${ruleId}`, data)

/** 删除黑名单规则 */
export const deleteBlacklistRule = (ruleId) =>
  request.delete(`/admin/blacklist/${ruleId}`)

/** 黑名单月历视图（month: yyyy-MM） */
export const getBlacklistCalendar = (storeId, month) =>
  request.get(`/admin/store/${storeId}/blacklist/calendar`, { params: { month } })

/* ============ FR-AD-006 座位布局 ============ */

/** 座位列表（画布数据：真实状态+类型+进行中订单标记） */
export const listSeats = (storeId) => request.get(`/admin/store/${storeId}/seats`)

/** 座位类型列表 */
export const listSeatTypes = () => request.get('/admin/seat-types')

/** 座位布局全量保存 */
export const saveSeatLayout = (storeId, seats) =>
  request.put(`/admin/store/${storeId}/seats/layout`, { seats })

/* ============ FR-AD-007 座位状态管理 ============ */

/** 手动切换座位状态（targetStatus: 0空闲 2维护 4锁定） */
export const changeSeatStatus = (seatId, targetStatus) =>
  request.post(`/admin/seat/${seatId}/status`, null, { params: { targetStatus } })

/* ============ FR-AD-008 签到二维码 ============ */

/** 生成签到二维码内容（validHours 1~72） */
export const generateQrCode = (storeId, validHours) =>
  request.get(`/admin/store/${storeId}/qrcode`, { params: { validHours } })
