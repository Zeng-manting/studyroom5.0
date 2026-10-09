<template>
  <!-- FR-UC-041 公告列表：类型筛选 + 重要标识 -->
  <view class="page">
    <!-- 类型筛选 -->
    <view class="filter-bar">
      <view v-for="t in types" :key="t.value" class="filter-item"
            :class="{ active: currentType === t.value }" @click="switchType(t.value)">{{ t.label }}</view>
    </view>

    <view v-if="notices.length === 0 && !loading" class="empty">
      <text class="empty-icon">📢</text>
      <text>暂无公告</text>
    </view>

    <view v-for="n in notices" :key="n.id" class="notice-card" @click="goDetail(n.id)">
      <view class="notice-head">
        <text v-if="n.important" class="important-tag">重要</text>
        <text class="notice-title">{{ n.title }}</text>
      </view>
      <view class="notice-meta">
        <text class="notice-type">{{ typeLabel(n.type) }}</text>
        <text class="notice-time">{{ fmt(n.publishTime) }}</text>
        <text v-if="!n.hasRead" class="unread-dot">未读</text>
      </view>
    </view>

    <view v-if="loading" class="loading">加载中...</view>
    <view v-else-if="finished && notices.length > 0" class="loading">没有更多了</view>
  </view>
</template>

<script setup>
/**
 * 公告列表页 —— FR-UC-041（模块7）
 * 仅展示已发布且 publish_time <= now 的公告；hasRead 标识由后端按当前用户计算
 */
import { ref } from 'vue'
import { onPullDownRefresh, onShow, onReachBottom } from '@dcloudio/uni-app'
import dayjs from 'dayjs'
import { getNotices } from '@/api/notice'

const types = [
  { value: 0, label: '全部' },
  { value: 1, label: '制度' },
  { value: 2, label: '停业公告' },
  { value: 3, label: '活动' },
  { value: 4, label: '其他' }
]
const TYPE_LABELS = { 1: '制度', 2: '停业公告', 3: '活动', 4: '其他' }

const currentType = ref(0)
const notices = ref([])
const page = ref(1)
const size = 10
const total = ref(0)
const loading = ref(false)
const finished = ref(false)

const fmt = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '')
const typeLabel = (t) => TYPE_LABELS[t] || '其他'

async function load(reset = false) {
  if (loading.value) return
  if (reset) {
    page.value = 1
    finished.value = false
  } else if (finished.value) return
  loading.value = true
  try {
    const res = await getNotices(currentType.value || undefined, page.value, size)
    const list = res.list || []
    notices.value = reset ? list : notices.value.concat(list)
    total.value = res.total || 0
    if (notices.value.length >= total.value || list.length < size) finished.value = true
    else page.value++
  } finally {
    loading.value = false
  }
}

function switchType(v) {
  if (currentType.value === v) return
  currentType.value = v
  notices.value = []
  load(true)
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/notice/detail?id=${id}` })
}

onPullDownRefresh(async () => {
  await load(true)
  uni.stopPullDownRefresh()
})
onShow(() => load(true))
onReachBottom(() => load(false))
</script>

<style scoped>
.page { padding: 24rpx; }

.filter-bar {
  display: flex; gap: 16rpx; margin-bottom: 24rpx; flex-wrap: wrap;
}
.filter-item {
  padding: 10rpx 32rpx; border-radius: 999rpx; font-size: 26rpx;
  background: #fff; color: #606266;
}
.filter-item.active { background: #4a7cf7; color: #fff; }

.notice-card { background: #fff; border-radius: 24rpx; padding: 28rpx; margin-bottom: 20rpx; }
.notice-head { display: flex; align-items: center; gap: 12rpx; }
.important-tag {
  flex-shrink: 0; background: #fdeaea; color: #e74c3c;
  font-size: 20rpx; padding: 4rpx 12rpx; border-radius: 8rpx;
}
.notice-title { font-size: 30rpx; font-weight: 600; color: #1f2329; }
.notice-meta {
  display: flex; align-items: center; gap: 20rpx;
  margin-top: 12rpx; font-size: 24rpx; color: #909399;
}
.unread-dot { color: #e74c3c; font-size: 22rpx; }

.empty { text-align: center; padding: 160rpx 0; color: #86909c; }
.empty-icon { display: block; font-size: 80rpx; margin-bottom: 16rpx; }
.loading { text-align: center; color: #b0b3ba; font-size: 26rpx; padding: 24rpx 0; }
</style>
