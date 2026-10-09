<template>
  <!-- FR-UC-035 六 Tab 订单列表 / FR-UC-036 倒计时三态 / FR-UC-037 动态操作按钮 -->
  <view class="page">
    <!-- 六 Tab（FR-UC-035） -->
    <view class="tabs">
      <view v-for="t in tabs" :key="t.value" class="tab" :class="{ active: currentTab === t.value }"
            @click="switchTab(t.value)">{{ t.label }}</view>
    </view>

    <!-- 订单列表 -->
    <view v-if="orders.length === 0 && !loading" class="empty">
      <text class="empty-icon">📋</text>
      <text>暂无订单</text>
    </view>

    <view v-for="o in orders" :key="o.orderId" class="order-card" @click="goDetail(o.orderId)">
      <view class="order-head">
        <text class="store-name">{{ o.storeName }}</text>
        <text class="status" :class="`st-${o.status}`">{{ o.statusDesc }}</text>
      </view>

      <view class="order-body">
        <view class="info-line">
          <text class="seat">{{ o.seatNo }} 号座位 · {{ o.packageName }}</text>
        </view>
        <view class="info-line time">
          {{ fmt(o.startTime) }} ~ {{ fmt(o.endTime) }}
        </view>

        <!-- FR-UC-036 倒计时三态 -->
        <view v-if="countdownText(o)" class="countdown" :class="countdownClass(o)">
          {{ countdownText(o) }}
        </view>

        <view class="order-foot">
          <view class="amount">
            <text v-if="o.refundAmount > 0" class="refund">已退 ¥{{ o.refundAmount }}</text>
            <text class="pay">¥{{ o.payAmount }}</text>
          </view>
          <!-- FR-UC-037 动态操作按钮：后端计算 actions，前端零逻辑渲染 -->
          <view class="actions">
            <view v-for="a in o.actions" :key="a" class="action-btn"
                  :class="`btn-${a}`" @click.stop="onAction(a, o)">{{ actionLabel(a) }}</view>
          </view>
        </view>
      </view>
    </view>

    <view v-if="loading" class="loading">加载中...</view>
    <view v-else-if="finished && orders.length > 0" class="loading">没有更多了</view>
  </view>
</template>

<script setup>
/**
 * 订单列表页 —— FR-UC-035/036/037（模块7）
 * 六 Tab：0全部/1待支付/2已预约/3进行中/4已完成/5已取消（已取消含退款中/已退款/申诉中）
 * 倒计时三态：待支付(红)=payDeadline、已预约(蓝)=startTime、进行中(绿)=endTime
 * 操作按钮：后端 actions 数组驱动，前端只做映射与跳转
 */
import { ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow, onHide } from '@dcloudio/uni-app'
import dayjs from 'dayjs'
import { getOrderList, cancelOrder } from '@/api/order'

const tabs = [
  { value: 0, label: '全部' },
  { value: 1, label: '待支付' },
  { value: 2, label: '已预约' },
  { value: 3, label: '进行中' },
  { value: 4, label: '已完成' },
  { value: 5, label: '已取消' }
]

const currentTab = ref(0)
const orders = ref([])
const page = ref(1)
const size = 10
const total = ref(0)
const loading = ref(false)
const finished = ref(false)
/** 秒级 tick 驱动倒计时刷新 */
const now = ref(Date.now())
let timer = null

const fmt = (t) => (t ? dayjs(t).format('MM-DD HH:mm') : '')

/* ---------- FR-UC-036 倒计时三态 ---------- */
function countdownTarget(o) {
  if (o.status === 0 && o.payDeadline) return { target: o.payDeadline, prefix: '支付剩余', cls: 'cd-red' }
  if (o.status === 1 && o.signTime == null && o.startTime) return { target: o.startTime, prefix: '开始于', cls: 'cd-blue' }
  if (o.status === 2 && o.endTime) return { target: o.endTime, prefix: '结束剩余', cls: 'cd-green' }
  return null
}

function countdownText(o) {
  const c = countdownTarget(o)
  if (!c) return ''
  const diff = dayjs(c.target).valueOf() - now.value
  if (c.prefix === '开始于') return diff > 0 ? `${c.prefix} ${fmt(c.target)}` : '已到开始时间，请签到'
  if (diff <= 0) return c.cls === 'cd-red' ? '支付已超时' : '已结束'
  const min = Math.floor(diff / 60000)
  if (min >= 60) {
    const h = Math.floor(min / 60)
    return `${c.prefix} ${h}小时${min % 60}分`
  }
  if (min >= 1) return `${c.prefix} ${min}分${Math.floor((diff % 60000) / 1000)}秒`
  return `${c.prefix} ${Math.floor(diff / 1000)}秒`
}

function countdownClass(o) {
  const c = countdownTarget(o)
  return c ? c.cls : ''
}

/* ---------- FR-UC-037 操作按钮 ---------- */
const ACTION_LABELS = {
  pay: '去支付', cancel: '取消订单', sign: '扫码签到', pause: '暂停', resume: '回座', end: '结束使用',
  review: '评价', appeal: '申诉'
}
const actionLabel = (a) => ACTION_LABELS[a] || a

function onAction(action, o) {
  switch (action) {
    case 'pay':
      goDetail(o.orderId)
      break
    case 'cancel':
      confirmCancel(o)
      break
    case 'sign':
      uni.navigateTo({ url: '/pages/sign/checkin' })
      break
    case 'pause':
    case 'resume':
    case 'end':
      uni.navigateTo({ url: `/pages/order/usage?orderId=${o.orderId}` })
      break
    case 'review':
      uni.navigateTo({ url: `/pages/review/create?orderId=${o.orderId}&storeName=${encodeURIComponent(o.storeName)}` })
      break
    case 'appeal':
      // 爽约申诉（FR-UC-032 申诉状态在详情页展示，提交入口在详情页）
      goDetail(o.orderId)
      break
  }
}

/** FR-UC-027 取消订单（阶梯退款提示） */
function confirmCancel(o) {
  uni.showModal({
    title: '取消订单',
    content: '开始前 2 小时以上取消全额退款，2 小时内按比例退款，确认取消吗？',
    confirmColor: '#e74c3c',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await cancelOrder(o.orderId)
        uni.showToast({ title: '已取消', icon: 'success' })
        refresh()
      } catch (e) { /* 请求层已提示 */ }
    }
  })
}

/* ---------- 分页加载 ---------- */
async function load(reset = false) {
  if (loading.value) return
  if (reset) {
    page.value = 1
    finished.value = false
  } else if (finished.value) return
  loading.value = true
  try {
    const res = await getOrderList(currentTab.value, page.value, size)
    const list = res.list || []
    orders.value = reset ? list : orders.value.concat(list)
    total.value = res.total || 0
    if (orders.value.length >= total.value || list.length < size) finished.value = true
    else page.value++
  } finally {
    loading.value = false
  }
}

function refresh() { load(true) }

function switchTab(v) {
  if (currentTab.value === v) return
  currentTab.value = v
  orders.value = []
  refresh()
}

function goDetail(orderId) {
  uni.navigateTo({ url: `/pages/order/detail?orderId=${orderId}` })
}

onPullDownRefresh(async () => {
  await load(true)
  uni.stopPullDownRefresh()
})

onReachBottom(() => load(false))

onShow(() => {
  // 从详情/支付/评价页返回时刷新当前 Tab
  refresh()
  timer = setInterval(() => { now.value = Date.now() }, 1000)
})
onHide(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 60rpx; }

.tabs {
  display: flex; background: #fff; border-radius: 999rpx;
  padding: 8rpx; margin-bottom: 24rpx;
}
.tab {
  flex: 1; text-align: center; padding: 14rpx 0;
  font-size: 26rpx; color: #606266; border-radius: 999rpx;
}
.tab.active { background: #4a7cf7; color: #fff; font-weight: 600; }

.order-card { background: #fff; border-radius: 24rpx; padding: 28rpx; margin-bottom: 24rpx; }
.order-head { display: flex; justify-content: space-between; align-items: center; }
.store-name { font-size: 30rpx; font-weight: 600; color: #1f2329; }
.status { font-size: 26rpx; }
.st-0 { color: #e74c3c; } .st-1 { color: #4a7cf7; } .st-2 { color: #27ae60; }
.st-3 { color: #909399; } .st-4, .st-5, .st-6, .st-7 { color: #b0b3ba; }

.order-body { margin-top: 16rpx; }
.info-line { font-size: 26rpx; color: #606266; line-height: 44rpx; }
.seat { color: #303133; }
.time { color: #86909c; }

.countdown { margin-top: 12rpx; font-size: 26rpx; font-weight: 600; }
.cd-red { color: #e74c3c; }
.cd-blue { color: #4a7cf7; }
.cd-green { color: #27ae60; }

.order-foot {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 20rpx; padding-top: 20rpx; border-top: 1rpx solid #f0f0f0;
}
.amount { display: flex; align-items: baseline; gap: 12rpx; }
.refund { font-size: 24rpx; color: #27ae60; }
.pay { font-size: 34rpx; font-weight: 700; color: #e74c3c; }

.actions { display: flex; gap: 16rpx; }
.action-btn {
  padding: 10rpx 28rpx; border-radius: 999rpx; font-size: 26rpx;
  border: 1rpx solid #dcdfe6; color: #606266; background: #fff;
}
.btn-pay { background: #e74c3c; border-color: #e74c3c; color: #fff; }
.btn-sign { background: #27ae60; border-color: #27ae60; color: #fff; }
.btn-review { background: #4a7cf7; border-color: #4a7cf7; color: #fff; }

.empty { text-align: center; padding: 160rpx 0; color: #86909c; }
.empty-icon { display: block; font-size: 80rpx; margin-bottom: 16rpx; }
.loading { text-align: center; color: #b0b3ba; font-size: 26rpx; padding: 24rpx 0; }
</style>
