<template>
  <!-- 支付成功页：FR-UC-028 绿色勾动画 + 订单摘要 + 签到码入口 -->
  <view class="page">
    <!-- 绿色勾动画（FR-UC-028） -->
    <view class="success-anim">
      <view class="circle">
        <view class="check" />
      </view>
      <view class="success-text">支付成功</view>
      <view class="success-sub">已为你锁定座位，请按时到店签到</view>
    </view>

    <!-- 订单摘要 -->
    <view v-if="order" class="card">
      <view class="summary-row">
        <text class="s-label">订单号</text>
        <text class="s-value">{{ order.orderNo }}</text>
      </view>
      <view class="summary-row">
        <text class="s-label">门店</text>
        <text class="s-value">{{ order.storeName }}</text>
      </view>
      <view class="summary-row">
        <text class="s-label">座位</text>
        <text class="s-value">{{ order.seatNo }}（{{ order.seatTypeName }}）</text>
      </view>
      <view class="summary-row">
        <text class="s-label">时段</text>
        <text class="s-value">{{ fmt(order.startTime) }} ~ {{ fmt(order.endTime) }}</text>
      </view>
      <view class="summary-row">
        <text class="s-label">支付金额</text>
        <text class="s-value pay">¥{{ order.payAmount }}</text>
      </view>
    </view>

    <view class="tips">
      <view class="tip">· 请在预约开始时间后 30 分钟内到店扫码签到，超时订单将自动取消并按比例退款（FR-UC-026）</view>
      <view class="tip">· 开始前取消订单支持阶梯退款：提前 60 分钟以上全退（FR-UC-027）</view>
    </view>

    <!-- 操作入口：扫码签到（FR-UC-028/029 模块6） -->
    <view class="btn-group">
      <button class="btn primary" @click="goCheckIn">到店扫码签到</button>
      <button class="btn plain" @click="goOrders">查看我的预约</button>
      <button class="btn plain" @click="goHome">返回首页</button>
    </view>
  </view>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { getOrderDetail } from '@/api/order'

const orderId = ref(null)
const order = ref(null)

function fmt(t) {
  return t ? dayjs(t).format('MM-DD HH:mm') : ''
}

function goCheckIn() {
  // 到店后扫码签到（FR-UC-029 五重校验，模块6）
  uni.navigateTo({ url: '/pages/sign/checkin' })
}

function goOrders() {
  // 模块7（订单列表 FR-UC-035）上线后替换为订单列表页
  uni.showToast({ title: '订单列表将在模块7上线', icon: 'none' })
}

function goHome() {
  uni.switchTab ? uni.switchTab({ url: '/pages/index/index' }) : uni.reLaunch({ url: '/pages/index/index' })
}

onMounted(async () => {
  const pages = getCurrentPages()
  const cur = pages[pages.length - 1]
  orderId.value = cur.options.orderId || cur.options.orderid
  order.value = await getOrderDetail(orderId.value)
})
</script>

<style scoped>
.page { padding: 60rpx 32rpx; background: #f5f6fa; min-height: 100vh; }

/* 绿色勾动画（FR-UC-028） */
.success-anim { display: flex; flex-direction: column; align-items: center; padding: 40rpx 0 60rpx; }
.circle { width: 140rpx; height: 140rpx; border-radius: 50%; background: #00b42a; display: flex; align-items: center; justify-content: center; animation: pop .45s cubic-bezier(.68, -.55, .27, 1.55); }
.check { width: 64rpx; height: 36rpx; border-left: 10rpx solid #fff; border-bottom: 10rpx solid #fff; transform: rotate(-45deg) translate(2rpx, -6rpx); }
@keyframes pop { 0% { transform: scale(0); } 80% { transform: scale(1.12); } 100% { transform: scale(1); } }
.success-text { margin-top: 28rpx; font-size: 38rpx; font-weight: 700; color: #1f2329; }
.success-sub { margin-top: 10rpx; font-size: 26rpx; color: #86909c; }

.card { background: #fff; border-radius: 20rpx; padding: 8rpx 24rpx; }
.summary-row { display: flex; justify-content: space-between; padding: 22rpx 0; border-bottom: 1rpx solid #f7f8fa; }
.summary-row:last-child { border-bottom: none; }
.s-label { font-size: 27rpx; color: #86909c; }
.s-value { font-size: 27rpx; color: #1f2329; }
.s-value.pay { color: #ff7d00; font-weight: 700; }

.tips { margin-top: 24rpx; padding: 0 8rpx; }
.tip { font-size: 22rpx; color: #c9cdd4; line-height: 1.8; }

.btn-group { margin-top: 60rpx; display: flex; flex-direction: column; gap: 24rpx; }
.btn { height: 88rpx; line-height: 88rpx; border-radius: 44rpx; font-size: 30rpx; }
.btn.primary { background: #4a7cf7; color: #fff; }
.btn.plain { background: #fff; color: #4a7cf7; border: 2rpx solid #4a7cf7; }
</style>
