/**
 * 全局配置
 * 技术选型：uni-app 3.x + Vue 3 + uView Plus + Pinia + dayjs
 */
export default {
  /** 后端接口基地址（本机调试用局域网 IP，微信开发者工具需勾选"不校验合法域名"） */
  BASE_URL: 'http://127.0.0.1:8080/api',
  /** Token 存储键 */
  TOKEN_KEY: 'studyroom_token',
  /** H5 端登录账号存储键（小程序端不使用） */
  H5_ACCOUNT_KEY: 'studyroom_h5_account',
  /** 是否已同意隐私协议（FR-UC-050） */
  PRIVACY_KEY: 'studyroom_privacy_agreed'
}
