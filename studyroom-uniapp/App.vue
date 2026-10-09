<script>
/**
 * 应用入口 —— FR-UC-001 微信静默登录（启动即登录，对用户无感）
 */
import { useUserStore } from '@/store/user'

export default {
  onLaunch() {
    // 静默登录：失败不阻塞首页渲染，页面请求层会在 401 时重试（FR-UC-006）
    const userStore = useUserStore()
    userStore.ensureLogin().catch(() => {
      console.warn('[App] 静默登录失败，待请求层自动重试')
    })
  }
}
</script>

<style>
/* 全局样式（uView Plus 导入后可使用其主题变量） */
page {
  background-color: #f5f6fa;
  font-size: 28rpx;
  color: #303133;
}
</style>
