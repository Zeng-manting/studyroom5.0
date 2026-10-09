<template>
  <!-- 设置页：消息订阅设置（FR-UC-047）+ 隐私协议 + 缓存清理 -->
  <view class="page">
    <!-- FR-UC-047 订阅消息设置：六场景开关 -->
    <view class="card">
      <view class="card-title">订阅消息提醒</view>
      <view v-for="s in scenes" :key="s.key" class="scene-row">
        <view class="scene-info">
          <view class="scene-name">{{ s.label }}</view>
          <view class="scene-desc">{{ s.desc }}</view>
        </view>
        <switch :checked="subscribed[s.key] !== false" color="#4a7cf7" @change="(e) =>onSwitch(e, s.key)" />
      </view>
    </view>

    <!-- 通用设置 -->
    <view class="card">
      <view class="card-title">通用</view>
      <view class="cell" @click="goNotice">
        <text>公告中心</text><text class="arrow">›</text>
      </view>
      <view class="cell" @click="goPrivacy">
        <text>隐私政策</text><text class="arrow">›</text>
      </view>
      <view class="cell" @click="clearCache">
        <text>清理缓存</text>
        <text class="cache-size">{{ cacheSize }}</text>
      </view>
      <view class="cell" @click="goFeedback">
        <text>意见反馈</text><text class="arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 设置页 —— FR-UC-047（模块7）
 * 六场景订阅开关：orderSuccess/startSoon/endSoon/cancel/refund/timeout
 * upsert 后端 user_subscribe_message（uk_user_scene）
 */
import { ref, onMounted } from 'vue'
import { reportSubscribe } from '@/api/message'

const scenes = [
  { key: 'orderSuccess', label: '预约成功', desc: '支付成功后通知' },
  { key: 'startSoon', label: '即将开始', desc: '预约开始前 15 分钟提醒' },
  { key: 'endSoon', label: '即将结束', desc: '预约结束前 15 分钟提醒' },
  { key: 'cancel', label: '订单取消', desc: '订单取消/退款通知' },
  { key: 'refund', label: '退款到账', desc: '退款完成后通知' },
  { key: 'timeout', label: '爽约提醒', desc: '超时未签到取消通知' }
]

/** 已订阅场景集合（默认全开） */
const subscribed = ref({})

const cacheSize = ref('')

onMounted(() => {
  loadCache()
})

/** FR-UC-047 场景开关上报（accept 1订阅 2取消） */
async function onSwitch(e, key) {
  const accept = e.detail.value ? 1 : 2
  // 乐观更新，失败回滚
  const prev = subscribed.value[key]
  subscribed.value = { ...subscribed.value, [key]: e.detail.value }
  try {
    await reportSubscribe(key, accept)
  } catch (err) {
    subscribed.value = { ...subscribed.value, [key]: prev }
  }
}

function loadCache() {
  try {
    const info = uni.getStorageInfoSync()
    cacheSize.value = `${(info.currentSize / 1024).toFixed(2)} MB`
  } catch (e) {
    cacheSize.value = '0 MB'
  }
}

/** 清理缓存（保留登录态相关 key） */
function clearCache() {
  uni.showModal({
    title: '清理缓存',
    content: `当前缓存 ${cacheSize.value}，清理后不影响登录状态，确认清理吗？`,
    success: (res) => {
      if (!res.confirm) return
      const KEEP = ['token', 'userInfo']
      try {
        const info = uni.getStorageInfoSync()
        info.keys.filter((k) => !KEEP.includes(k)).forEach((k) => uni.removeStorageSync(k))
        loadCache()
        uni.showToast({ title: '清理完成', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: '清理失败', icon: 'none' })
      }
    }
  })
}

function goNotice() {
  uni.navigateTo({ url: '/pages/notice/list' })
}
function goPrivacy() {
  // FR-UC-050 隐私政策说明弹窗
  uni.showModal({
    title: '隐私政策',
    content: '我们仅收集为您提供服务所必需的信息（微信昵称头像、预约记录、签到定位），数据加密存储，不会向第三方提供。可随时在个人资料中修改或解绑。',
    showCancel: false,
    confirmText: '我知道了'
  })
}
function goFeedback() {
  uni.navigateTo({ url: '/pages/feedback/index' })
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 60rpx; }
.card { background: #fff; border-radius: 24rpx; padding: 8rpx 32rpx; margin-bottom: 24rpx; }
.card-title { font-size: 30rpx; font-weight: 600; color: #1f2329; padding: 24rpx 0 8rpx; }

.scene-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24rpx 0; border-bottom: 1rpx solid #f0f0f0;
}
.scene-row:last-child { border-bottom: none; }
.scene-name { font-size: 28rpx; color: #303133; }
.scene-desc { margin-top: 4rpx; font-size: 22rpx; color: #b0b3ba; }

.cell {
  display: flex; justify-content: space-between; align-items: center;
  padding: 28rpx 0; border-bottom: 1rpx solid #f0f0f0;
  font-size: 28rpx; color: #303133;
}
.cell:last-child { border-bottom: none; }
.arrow { color: #b0b3ba; font-size: 32rpx; }
.cache-size { font-size: 24rpx; color: #86909c; }
</style>
