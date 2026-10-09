<template>
  <!-- 首页：门店列表（FR-UC-007~010）+ 快捷入口 -->
  <view class="page">
    <view class="hero">
      <view class="hero-title">无人自习室</view>
      <view class="hero-sub">{{ greeting }}，{{ userInfo ? userInfo.nickname : '同学' }}</view>
    </view>

    <!-- 搜索栏 -->
    <view class="search-bar">
      <input v-model="keyword" class="search-input" placeholder="搜索门店名称 / 地址"
             confirm-type="search" @confirm="loadStores" />
      <view class="search-btn" @click="loadStores">搜索</view>
    </view>

    <!-- 门店列表 -->
    <view v-if="loading" class="loading">加载中...</view>
    <view v-else-if="stores.length === 0" class="empty">
      <text class="empty-icon">🏫</text>
      <text>暂无门店</text>
    </view>
    <view v-else class="store-list">
      <view v-for="s in stores" :key="s.id" class="store-card" @click="goStore(s.id)">
        <image v-if="s.coverImage" class="store-cover" :src="s.coverImage" mode="aspectFill" />
        <view v-else class="store-cover store-cover-empty">📖</view>
        <view class="store-info">
          <view class="store-name">{{ s.name }}</view>
          <view class="store-addr">{{ s.address }}</view>
          <view class="store-meta">
            <text class="store-rating">★ {{ s.rating || '5.0' }}</text>
            <text class="store-hours">{{ s.businessHours }}</text>
            <text class="store-status" :class="`st-${s.status}`">{{ s.statusDesc }}</text>
          </view>
          <view v-if="s.facilityList && s.facilityList.length" class="store-tags">
            <text v-for="(f, i) in s.facilityList.slice(0, 3)" :key="i" class="tag">{{ f }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 快捷入口 -->
    <view class="card">
      <view class="card-title">学习服务</view>
      <view class="entry" @click="goCheckIn">
        <text class="entry-icon">📷</text>
        <text class="entry-label">到店扫码签到</text>
        <text class="entry-arrow">›</text>
      </view>
      <view class="entry" @click="goOrders">
        <text class="entry-icon">📋</text>
        <text class="entry-label">我的订单</text>
        <text class="entry-arrow">›</text>
      </view>
      <view class="entry" @click="goMessages">
        <text class="entry-icon">🔔</text>
        <text class="entry-label">消息中心</text>
        <text v-if="unreadTotal > 0" class="entry-badge">{{ unreadTotal > 99 ? '99+' : unreadTotal }}</text>
        <text class="entry-arrow">›</text>
      </view>
      <view class="entry" @click="goNotices">
        <text class="entry-icon">📢</text>
        <text class="entry-label">公告中心</text>
        <text class="entry-arrow">›</text>
      </view>
      <view class="entry" @click="goProfile">
        <text class="entry-icon">👤</text>
        <text class="entry-label">个人资料</text>
        <text class="entry-arrow">›</text>
      </view>
      <view class="entry" @click="goEmailBind">
        <text class="entry-icon">📧</text>
        <text class="entry-label">绑定邮箱</text>
        <text class="entry-tag" :class="{ ok: emailBound }">{{ emailBound ? '已绑定' : '未绑定' }}</text>
        <text class="entry-arrow">›</text>
      </view>
      <view class="entry" @click="goWallet">
        <text class="entry-icon">💰</text>
        <text class="entry-label">我的钱包</text>
        <text class="entry-arrow">›</text>
      </view>
      <view class="entry" @click="goCredit">
        <text class="entry-icon">🛡️</text>
        <text class="entry-label">我的信用</text>
        <text class="entry-tag" :class="{ warn: creditLimited }">
          {{ creditLimited ? '受限中' : '信用良好' }}
        </text>
        <text class="entry-arrow">›</text>
      </view>
      <view class="entry" @click="goStats">
        <text class="entry-icon">📊</text>
        <text class="entry-label">学习统计</text>
        <text class="entry-arrow">›</text>
      </view>
      <view class="entry" @click="goSettings">
        <text class="entry-icon">⚙️</text>
        <text class="entry-label">设置</text>
        <text class="entry-arrow">›</text>
      </view>
    </view>

    <!-- FR-UC-050 首次进入隐私协议弹窗 -->
    <privacy-popup v-model:visible="showPrivacy" />
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/user'
import { getStoreList } from '@/api/store'
import { getUnreadCount } from '@/api/message'
import { getNoticePopup } from '@/api/notice'
import PrivacyPopup from '@/components/privacy-popup.vue'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const emailBound = computed(() => userStore.emailBound)
/** FR-UC-054 信用受限标记（入口提示） */
const creditLimited = computed(() => (userStore.userInfo && userStore.userInfo.creditStatus) === 1)

/** FR-UC-050 隐私协议弹窗：未同意过则首次进入弹出 */
const showPrivacy = ref(false)
/** FR-UC-045 消息未读总数（角标） */
const unreadTotal = ref(0)
/** FR-UC-042 重要公告弹窗（已读后不再弹） */
const importantNotice = ref(null)

const keyword = ref('')
const stores = ref([])
const loading = ref(false)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 18) return '下午好'
  return '晚上好'
})

async function loadStores() {
  loading.value = true
  try {
    stores.value = await getStoreList(keyword.value || undefined)
  } finally {
    loading.value = false
  }
}

function goStore(id) {
  uni.navigateTo({ url: `/pages/store/detail?storeId=${id}` })
}
function goProfile() {
  uni.navigateTo({ url: '/pages/profile/profile' })
}
function goCheckIn() {
  // 到店扫码签到（FR-UC-029，模块6）
  uni.navigateTo({ url: '/pages/sign/checkin' })
}
function goEmailBind() {
  uni.navigateTo({ url: '/pages/email-bind/email-bind' })
}
/* ---- 模块7 快捷入口 ---- */
function goOrders() { uni.navigateTo({ url: '/pages/order/list' }) }
function goMessages() { uni.navigateTo({ url: '/pages/message/list' }) }
function goNotices() { uni.navigateTo({ url: '/pages/notice/list' }) }
function goWallet() { uni.navigateTo({ url: '/pages/wallet/index' }) }
/* ---- 模块8 信用入口（FR-UC-053/054） ---- */
function goCredit() { uni.navigateTo({ url: '/pages/credit/index' }) }
function goStats() { uni.navigateTo({ url: '/pages/stats/index' }) }
function goSettings() { uni.navigateTo({ url: '/pages/profile/settings' }) }

onMounted(async () => {
  loadStores()
  // 若已登录则静默刷新用户信息（store 中方法名为 fetchUserInfo）
  if (userStore.isLogged) userStore.fetchUserInfo().catch(() => {})
})

onShow(() => {
  // 首次进入：隐私协议未同意 → 弹窗（FR-UC-050）
  if (userStore.userInfo && userStore.userInfo.privacyAgreed === false) {
    showPrivacy.value = true
  }
  // 消息未读角标（FR-UC-045，失败静默；后端返回总数 Long）
  getUnreadCount().then((n) => {
    unreadTotal.value = Number(n || 0)
  }).catch(() => {})
  // 重要公告弹窗（FR-UC-042：最新一条重要且未读）
  getNoticePopup().then((n) => {
    importantNotice.value = n
    if (n) {
      uni.showModal({
        title: n.important ? '【重要】' + n.title : n.title,
        content: '有新的重要公告，点击查看详情',
        confirmText: '查看',
        success: (r) => {
          if (r.confirm) uni.navigateTo({ url: `/pages/notice/detail?id=${n.id}` })
        }
      })
    }
  }).catch(() => {})
})
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 60rpx; }
.hero { padding: 20rpx 8rpx 28rpx; }
.hero-title { font-size: 44rpx; font-weight: 700; color: #1f2329; }
.hero-sub { margin-top: 8rpx; font-size: 26rpx; color: #86909c; }

.search-bar { display: flex; align-items: center; gap: 16rpx; margin-bottom: 24rpx; }
.search-input { flex: 1; height: 76rpx; padding: 0 28rpx; background: #fff; border-radius: 40rpx; font-size: 28rpx; }
.search-btn { width: 120rpx; height: 76rpx; line-height: 76rpx; text-align: center; background: #4a7cf7; color: #fff; border-radius: 40rpx; font-size: 28rpx; }

.loading, .empty { padding: 80rpx 0; text-align: center; color: #86909c; font-size: 26rpx; }
.empty-icon { display: block; font-size: 80rpx; margin-bottom: 16rpx; }

.store-list { display: flex; flex-direction: column; gap: 20rpx; margin-bottom: 32rpx; }
.store-card { display: flex; gap: 20rpx; background: #fff; border-radius: 20rpx; padding: 20rpx; }
.store-cover { width: 180rpx; height: 180rpx; border-radius: 14rpx; background: #f2f3f5; flex-shrink: 0; }
.store-cover-empty { display: flex; align-items: center; justify-content: center; font-size: 60rpx; }
.store-info { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.store-name { font-size: 32rpx; font-weight: 600; color: #1f2329; }
.store-addr { margin-top: 6rpx; font-size: 24rpx; color: #86909c; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.store-meta { margin-top: 10rpx; display: flex; align-items: center; gap: 16rpx; font-size: 24rpx; }
.store-rating { color: #ff9c38; }
.store-hours { color: #86909c; }
.store-status { padding: 2rpx 12rpx; border-radius: 8rpx; font-size: 22rpx; }
.st-1 { color: #00b42a; background: rgba(0, 180, 42, .1); }
.st-0 { color: #86909c; background: #f2f3f5; }
.st-2 { color: #ff7d00; background: rgba(255, 125, 0, .1); }
.store-tags { margin-top: 10rpx; display: flex; gap: 10rpx; }
.tag { padding: 2rpx 12rpx; background: #f2f3f5; border-radius: 6rpx; font-size: 22rpx; color: #4e5969; }

.card { background: #fff; border-radius: 20rpx; padding: 24rpx; margin-top: 8rpx; }
.card-title { font-size: 30rpx; font-weight: 600; color: #1f2329; margin-bottom: 12rpx; }
.entry { display: flex; align-items: center; padding: 22rpx 8rpx; border-bottom: 1rpx solid #f2f3f5; }
.entry:last-child { border-bottom: none; }
.entry-icon { font-size: 36rpx; margin-right: 18rpx; }
.entry-label { flex: 1; font-size: 28rpx; color: #1f2329; }
.entry-tag { font-size: 24rpx; color: #ff7d00; }
.entry-tag.ok { color: #00b42a; }
.entry-tag.warn { color: #f53f3f; font-weight: 600; }
.entry-arrow { color: #c9cdd4; font-size: 32rpx; }
.entry-badge {
  min-width: 36rpx; padding: 0 10rpx; margin-left: auto; margin-right: 8rpx;
  background: #e74c3c; color: #fff; border-radius: 999rpx;
  font-size: 20rpx; line-height: 36rpx; text-align: center;
}
</style>
