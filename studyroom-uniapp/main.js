import App from './App'
import { createPinia } from 'pinia'
import { createSSRApp } from 'vue'

// #ifdef H5
/**
 * H5 宽屏适配：桌面浏览器窗口过宽时，将 uni-app 的 rpx 换算基准固定为手机宽度（480px），
 * 内容居中显示。必须在应用初始化前执行（rpx→px 在组件渲染时按 windowWidth 计算）。
 */
;(function applyH5MaxWidth() {
  const MAX_WIDTH = 480
  if (window.innerWidth > MAX_WIDTH) {
    Object.defineProperty(window, 'innerWidth', {
      get: function () { return MAX_WIDTH },
      configurable: true
    })
    document.documentElement.style.maxWidth = MAX_WIDTH + 'px'
    document.documentElement.style.margin = '0 auto'
    document.documentElement.style.boxShadow = '0 0 24px rgba(0,0,0,.12)'
    document.documentElement.style.minHeight = '100vh'
    document.body.style.background = '#ebedf0'
  }
})()
// #endif

/**
 * uni-app Vue 3 入口
 * 技术选型：uni-app 3.x + Vue 3.4 + Pinia 2.x + uView Plus 3.x（HBuilderX 创建）
 */
export function createApp() {
  const app = createSSRApp(App)
  app.use(createPinia())
  // uView Plus：HBuilderX 插件市场导入 uni_modules 后取消注释
  // import uviewPlus from '@/uni_modules/uview-plus'
  // app.use(uviewPlus)
  return { app }
}
