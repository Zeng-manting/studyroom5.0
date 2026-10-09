/**
 * 自助签到 API（FR-UC-029 ~ FR-UC-032）
 */
import request from '@/utils/request'
import { getToken } from '@/utils/auth'
import config from '@/utils/config'

/**
 * 扫码签到（FR-UC-029/030/031 五重校验）
 * request 层自动预取 X-Idempotent-Token（FR-SY-001）
 *
 * @param {Object} data { qrcode, latitude, longitude }
 */
export const checkIn = (data) =>
  request.post('/sign/checkin', data, { idempotent: 'sign:checkin' })

/**
 * 提交签到申诉（FR-UC-032：原因 + 文字说明 + 凭证图片URL列表）
 */
export const submitAppeal = (data) => request.post('/sign/appeal', data)

/**
 * 上传申诉凭证图片（FR-CC-003，scene=feedback）
 *
 * @param {String} filePath 本地临时文件路径（uni.chooseImage 获得）
 * @returns {Promise<String>} 图片访问 URL
 */
export const uploadAppealImage = (filePath) =>
  new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${config.BASE_URL}/file/upload?scene=feedback`,
      filePath,
      name: 'file',
      header: { Authorization: `Bearer ${getToken()}` },
      success: (res) => {
        try {
          const body = JSON.parse(res.data)
          if (body.code === 200) {
            resolve(body.data)
          } else {
            uni.showToast({ title: body.message || '上传失败', icon: 'none' })
            reject(body)
          }
        } catch (e) {
          reject(e)
        }
      },
      fail: () => {
        uni.showToast({ title: '上传失败，请重试', icon: 'none' })
        reject(new Error('upload fail'))
      }
    })
  })
