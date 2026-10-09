import App from './App'
import { createPinia } from 'pinia'
import { createSSRApp } from 'vue'

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
