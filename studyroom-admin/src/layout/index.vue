<template>
  <el-container class="layout">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="aside">
      <div class="logo">
        <span class="logo-icon">📚</span>
        <span class="logo-text">自习室后台</span>
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#001529"
        text-color="rgba(255, 255, 255, 0.65)"
        active-text-color="#fff"
        class="menu"
      >
        <!-- 模块 9~14 全量菜单：仪表盘/订单/门店/用户/内容/统计/财务/营销/系统 -->
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-sub-menu index="/order">
          <template #title>
            <el-icon><Tickets /></el-icon>
            <span>订单管理</span>
          </template>
          <el-menu-item index="/order">订单列表</el-menu-item>
          <el-menu-item index="/order/appeal">申诉处理</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/store">
          <template #title>
            <el-icon><OfficeBuilding /></el-icon>
            <span>门店管理</span>
          </template>
          <el-menu-item index="/store">门店信息</el-menu-item>
          <el-menu-item index="/store/blacklist">时段黑名单</el-menu-item>
          <el-menu-item index="/store/seats">座位管理</el-menu-item>
          <el-menu-item index="/store/qrcode">签到二维码</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/user">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-sub-menu index="/content">
          <template #title>
            <el-icon><Bell /></el-icon>
            <span>内容管理</span>
          </template>
          <el-menu-item index="/notice">公告管理</el-menu-item>
          <el-menu-item index="/review">评价管理</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/statistics">
          <el-icon><TrendCharts /></el-icon>
          <span>数据统计</span>
        </el-menu-item>
        <el-menu-item index="/finance">
          <el-icon><Money /></el-icon>
          <span>财务总览</span>
        </el-menu-item>
        <el-menu-item index="/coupon">
          <el-icon><Present /></el-icon>
          <span>优惠券管理</span>
        </el-menu-item>
        <el-sub-menu index="/setting">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </template>
          <el-menu-item index="/setting/price">价格配置</el-menu-item>
          <el-menu-item index="/setting/config">系统配置</el-menu-item>
          <el-menu-item index="/log">操作日志</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta?.title">
              {{ route.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <el-dropdown trigger="click" @command="onCommand">
          <span class="user-box">
            <el-avatar :size="30" class="user-avatar">
              {{ authStore.displayName.slice(0, 1) }}
            </el-avatar>
            <span class="user-name">{{ authStore.displayName }}</span>
            <el-tag size="small" type="primary" effect="plain">
              {{ authStore.roleName }}
            </el-tag>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
/**
 * 管理端主布局 —— FR-AD-001
 * 侧边菜单 + 顶栏面包屑/用户区 + 主内容区；
 * 后续模块在 el-menu 与路由 children 中追加对应入口。
 */
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

async function onCommand(cmd) {
  if (cmd !== 'logout') return
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await authStore.logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}

onMounted(() => {
  // 刷新管理员信息（角色/最近登录时间，失败静默）
  authStore.fetchAdminInfo().catch(() => {})
})
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background: #001529;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
}

.logo-icon {
  font-size: 22px;
}

.logo-text {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 2px;
}

.menu {
  border-right: none;
}

.header {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.user-box {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.user-avatar {
  background: #2f54eb;
  color: #fff;
  font-size: 14px;
}

.user-name {
  font-size: 14px;
  color: #1d2129;
}

.main {
  background: #f5f7fa;
  overflow-y: auto;
}
</style>
