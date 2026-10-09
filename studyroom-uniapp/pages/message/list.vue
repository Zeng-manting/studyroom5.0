<template>
  <!-- FR-UC-045/046 消息中心：三分类 + 未读角标 + 左滑删除 + 全部已读 -->
  <view class="page">
    <!-- 三分类 Tab + 全部已读 -->
    <view class="top-bar">
      <view class="tabs">
        <view v-for="t in tabs" :key="t.value" class="tab" :class="{ active: currentType === t.value }"
              @click="switchType(t.value)">
          {{ t.label }}
          <text v-if="t.value === 0 && unreadTotal > 0" class="badge">{{ unreadTotal > 99 ? '99+' : unreadTotal }}</text>
        </view>
      </view>
      <view class="read-all" @click="onReadAll">全部已读</view>
    </view>

    <view v-if="messages.length === 0 && !loading" class="empty">
      <text class="empty-icon">🔔</text>
      <text>暂无消息</text>
    </view>

    <!-- 左滑删除（FR-UC-046）：touch 事件手写滑动，露出红色删除按钮 -->
    <view class="msg-list">
      <view v-for="m in messages" :key="m.id" class="swipe-wrap">
        <view class="swipe-del" @click.stop="onDelete(m)">删除</view>
        <view class="swipe-content" :style="{ transform: `translateX(${offsets[m.id] || 0}rpx)` }"
              @touchstart="onTouchStart($event, m)" @touchmove="onTouchMove($event, m)" @touchend="onTouchEnd(m)">
          <view class="msg-card" :class="{ unread: !m.isRead }" @click="onMsgClick(m)">
            <view class="msg-head">
              <text class="msg-type" :class="`mt-${m.type}`">{{ typeLabel(m.type) }}</text>
              <text class="msg-time">{{ fmt(m.createTime) }}</text>
            </view>
            <view class="msg-title">
              <text v-if="!m.isRead" class="dot"></text>{{ m.title }}
            </view>
            <view class="msg-content">{{ m.content }}</view>
          </view>
        </view>
      </view>
    </view>

    <view v-if="loading" class="loading">加载中...</view>
    <view v-else-if="finished && messages.length > 0" class="loading">没有更多了</view>
  </view>
</template>

<script setup>
/**
 * 消息中心页 —— FR-UC-045/046（模块7）
 * 三分类：1系统/2订单/3活动；未读数角标；点击已读；左滑删除（逻辑删除）
 * 左滑用 touch 事件手写（translateX），不依赖第三方组件库
 */
import { ref } from 'vue'
import { onShow, onReachBottom, onPullDownRefresh } from '@dcloudio/uni-app'
import dayjs from 'dayjs'
import { getMessages, getUnreadCount, readMessage, readAllMessages, deleteMessage } from '@/api/message'

const tabs = [
  { value: 0, label: '全部' },
  { value: 1, label: '系统' },
  { value: 2, label: '订单' },
  { value: 3, label: '活动' }
]
const TYPE_LABELS = { 1: '系统', 2: '订单', 3: '活动' }

const currentType = ref(0)
const messages = ref([])
const page = ref(1)
const size = 20
const total = ref(0)
const loading = ref(false)
const finished = ref(false)

/** 未读总数（后端返回 Long） */
const unreadTotal = ref(0)

const fmt = (t) => (t ? dayjs(t).format('MM-DD HH:mm') : '')
const typeLabel = (t) => TYPE_LABELS[t] || '系统'

/* ---------- 左滑删除 ---------- */
const DEL_WIDTH = -140 // 删除按钮宽度（rpx）
const offsets = ref({})       // 每条消息当前偏移
let touchStartX = 0
let touchStartY = 0
let touchMsg = null
let touchOffset = 0
let swiping = false

function onTouchStart(e, m) {
  touchStartX = e.touches[0].clientX
  touchStartY = e.touches[0].clientY
  touchMsg = m
  touchOffset = offsets.value[m.id] || 0
  swiping = false
}

function onTouchMove(e, m) {
  const dx = e.touches[0].clientX - touchStartX
  const dy = e.touches[0].clientY - touchStartY
  // 首次判定方向：横向位移大于纵向才视为左滑
  if (!swiping) {
    if (Math.abs(dx) < 10) return
    swiping = Math.abs(dx) > Math.abs(dy)
    if (!swiping) return
  }
  // rpx 换算：以 750 设计稿宽近似
  const scale = 750 / uni.getSystemInfoSync().windowWidth
  let next = touchOffset + dx * scale
  if (next > 0) next = 0
  if (next < DEL_WIDTH) next = DEL_WIDTH
  offsets.value = { ...offsets.value, [m.id]: next }
}

function onTouchEnd(m) {
  if (!swiping) return
  const cur = offsets.value[m.id] || 0
  // 超过一半即展开，否则收回
  offsets.value = { ...offsets.value, [m.id]: cur < DEL_WIDTH / 2 ? DEL_WIDTH : 0 }
}

function onDelete(m) {
  uni.showModal({
    title: '删除消息',
    content: '确定删除这条消息吗？',
    confirmColor: '#e74c3c',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await deleteMessage(m.id)
        messages.value = messages.value.filter((x) => x.id !== m.id)
        if (!m.isRead && unreadTotal.value > 0) unreadTotal.value--
        uni.showToast({ title: '已删除', icon: 'success' })
      } catch (e) { /* 请求层已提示 */ }
    }
  })
}

/* ---------- 数据加载 ---------- */
async function loadUnread() {
  try {
    unreadTotal.value = Number((await getUnreadCount()) || 0)
  } catch (e) { /* 角标失败静默 */ }
}

async function load(reset = false) {
  if (loading.value) return
  if (reset) {
    page.value = 1
    finished.value = false
  } else if (finished.value) return
  loading.value = true
  try {
    const res = await getMessages(currentType.value || undefined, page.value, size)
    const list = res.list || []
    messages.value = reset ? list : messages.value.concat(list)
    total.value = res.total || 0
    if (messages.value.length >= total.value || list.length < size) finished.value = true
    else page.value++
  } finally {
    loading.value = false
  }
}

function switchType(v) {
  if (currentType.value === v) return
  currentType.value = v
  messages.value = []
  offsets.value = {}
  load(true)
}

/** 点击消息：标记已读 + 按业务类型跳转（订单→详情） */
async function onMsgClick(m) {
  const opened = (offsets.value[m.id] || 0) < 0
  offsets.value = { ...offsets.value, [m.id]: 0 } // 展开态点击先收回
  if (opened) return
  if (!m.isRead) {
    try {
      await readMessage(m.id)
      m.isRead = true
      if (unreadTotal.value > 0) unreadTotal.value--
    } catch (e) { /* 静默 */ }
  }
  if (m.bizType === 'order' && m.bizId) {
    uni.navigateTo({ url: `/pages/order/detail?orderId=${m.bizId}` })
  }
}

/** 全部已读（FR-UC-046） */
async function onReadAll() {
  try {
    await readAllMessages()
    messages.value.forEach((m) => { m.isRead = true })
    unreadTotal.value = 0
    uni.showToast({ title: '已全部标记已读', icon: 'success' })
  } catch (e) { /* 请求层已提示 */ }
}

onShow(() => {
  load(true)
  loadUnread()
})
onReachBottom(() => load(false))
onPullDownRefresh(async () => {
  await load(true)
  uni.stopPullDownRefresh()
})
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 60rpx; }

.top-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24rpx; }
.tabs { display: flex; gap: 8rpx; background: #fff; border-radius: 999rpx; padding: 8rpx; }
.tab {
  position: relative; padding: 12rpx 28rpx; border-radius: 999rpx;
  font-size: 26rpx; color: #606266;
}
.tab.active { background: #4a7cf7; color: #fff; font-weight: 600; }
.badge {
  display: inline-block; min-width: 32rpx; padding: 0 8rpx; margin-left: 6rpx;
  background: #e74c3c; color: #fff; border-radius: 999rpx;
  font-size: 20rpx; text-align: center; line-height: 32rpx;
}
.read-all { font-size: 26rpx; color: #4a7cf7; padding: 0 8rpx; }

/* 左滑删除 */
.msg-list { border-radius: 24rpx; overflow: hidden; }
.swipe-wrap { position: relative; overflow: hidden; }
.swipe-del {
  position: absolute; top: 0; right: 0; bottom: 0; width: 140rpx;
  background: #e74c3c; color: #fff; font-size: 26rpx;
  display: flex; align-items: center; justify-content: center;
}
.swipe-content { position: relative; z-index: 1; transition: transform .15s ease-out; background: #fff; }

.msg-card { padding: 24rpx 28rpx; border-bottom: 1rpx solid #f0f0f0; }
.msg-card.unread { background: #f8faff; }
.swipe-wrap:last-child .msg-card { border-bottom: none; }

.msg-head { display: flex; justify-content: space-between; align-items: center; }
.msg-type { font-size: 20rpx; padding: 2rpx 12rpx; border-radius: 8rpx; }
.mt-1 { background: #eef3fe; color: #4a7cf7; }
.mt-2 { background: #e8f7ee; color: #27ae60; }
.mt-3 { background: #fdf0e6; color: #e6a23c; }
.msg-time { font-size: 22rpx; color: #b0b3ba; }

.msg-title {
  display: flex; align-items: center; gap: 10rpx;
  margin-top: 12rpx; font-size: 28rpx; font-weight: 600; color: #1f2329;
}
.dot { width: 14rpx; height: 14rpx; border-radius: 50%; background: #e74c3c; flex-shrink: 0; }

.msg-content {
  margin-top: 8rpx; font-size: 26rpx; color: #86909c;
  overflow: hidden; text-overflow: ellipsis; display: -webkit-box;
  -webkit-line-clamp: 2; -webkit-box-orient: vertical;
}

.empty { text-align: center; padding: 160rpx 0; color: #86909c; }
.empty-icon { display: block; font-size: 80rpx; margin-bottom: 16rpx; }
.loading { text-align: center; color: #b0b3ba; font-size: 26rpx; padding: 24rpx 0; }
</style>
