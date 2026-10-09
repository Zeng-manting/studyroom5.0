<template>
  <!-- FR-UC-038 订单详情：信息 + 费用 + 状态时间轴 -->
  <view class="page">
    <view v-if="!order" class="loading">加载中...</view>
    <template v-else>
      <!-- 状态卡片 -->
      <view class="card status-card">
        <view class="status-title">{{ order.statusDesc }}</view>
        <view v-if="order.status === 0 && order.payDeadline" class="status-sub cd-red">
          支付剩余 {{ fromNow(order.payDeadline) }}
        </view>
        <view v-if="order.noShow" class="no-show-tip">该订单因超时未签到已爽约取消</view>

        <!-- 待支付：去支付 -->
        <button v-if="order.status === 0" class="primary-btn" @click="onPay">去支付 ¥{{ order.payAmount }}</button>
        <!-- 已完成未评价：去评价 -->
        <button v-if="order.status === 3 && !order.reviewed" class="primary-btn" @click="goReview">评价本次学习</button>
      </view>

      <!-- 预约信息 -->
      <view class="card">
        <view class="card-title">预约信息</view>
        <view class="info-row"><text class="label">门店</text><text>{{ order.storeName }}</text></view>
        <view class="info-row"><text class="label">地址</text><text>{{ order.storeAddress }}</text></view>
        <view class="info-row"><text class="label">座位</text><text>{{ order.seatNo }} 号（{{ order.seatTypeName }}）</text></view>
        <view class="info-row"><text class="label">套餐</text><text>{{ order.packageName }}</text></view>
        <view class="info-row"><text class="label">时间</text><text>{{ fmtFull(order.startTime) }} ~ {{ fmtFull(order.endTime) }}</text></view>
        <view v-if="order.actualDurationMinutes != null" class="info-row">
          <text class="label">实际时长</text><text>{{ order.actualDurationMinutes }} 分钟</text>
        </view>
        <view v-if="order.signGpsDistance != null" class="info-row">
          <text class="label">签到定位</text><text>距门店 {{ order.signGpsDistance }} 米</text>
        </view>
      </view>

      <!-- 费用明细 -->
      <view class="card">
        <view class="card-title">费用明细</view>
        <view class="info-row"><text class="label">原价</text><text>¥{{ order.originalPrice }}</text></view>
        <view v-if="order.discountAmount > 0" class="info-row">
          <text class="label">套餐优惠</text><text class="discount">-¥{{ order.discountAmount }}</text>
        </view>
        <view v-if="order.couponAmount > 0" class="info-row">
          <text class="label">优惠券</text><text class="discount">-¥{{ order.couponAmount }}</text>
        </view>
        <view class="info-row"><text class="label">实付</text><text class="pay-amount">¥{{ order.payAmount }}</text></view>
        <view v-if="order.refundAmount > 0" class="info-row">
          <text class="label">退款</text><text class="refund-amount">¥{{ order.refundAmount }}</text>
        </view>
        <view class="info-row"><text class="label">订单号</text><text class="order-no">{{ order.orderNo }}</text></view>
      </view>

      <!-- FR-UC-038 状态时间轴 -->
      <view class="card">
        <view class="card-title">订单进度</view>
        <view class="timeline">
          <view v-for="(n, i) in order.timeline" :key="i" class="tl-node" :class="{ last: i === order.timeline.length - 1 }">
            <view class="tl-dot" :class="{ done: n.done }"></view>
            <view class="tl-body">
              <view class="tl-title">{{ n.title }}</view>
              <view v-if="n.desc" class="tl-desc">{{ n.desc }}</view>
              <view class="tl-time">{{ fmtFull(n.time) }}</view>
            </view>
          </view>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup>
/**
 * 订单详情页 —— FR-UC-038（模块7）
 * 时间轴节点：create/pay/sign/pause/end/cancel/refund，后端 buildTimeline 正序生成
 */
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import dayjs from 'dayjs'
import { getOrderDetail, payOrder } from '@/api/order'

const order = ref(null)
let orderId = null

const fmtFull = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '')
const fromNow = (t) => {
  const diff = dayjs(t).valueOf() - Date.now()
  if (diff <= 0) return '已超时'
  const min = Math.floor(diff / 60000)
  return min >= 60 ? `${Math.floor(min / 60)}小时${min % 60}分` : `${min}分`
}

async function load() {
  order.value = await getOrderDetail(orderId)
}

/** 待支付订单支付（FR-UC-023：默认余额，余额不足提示去充值） */
function onPay() {
  uni.showActionSheet({
    itemList: ['余额支付', '模拟微信支付'],
    success: async (res) => {
      try {
        await payOrder(orderId, res.tapIndex + 1)
        uni.showToast({ title: '支付成功', icon: 'success' })
        load()
      } catch (e) { /* 请求层已提示（余额不足引导去钱包充值） */ }
    }
  })
}

function goReview() {
  uni.navigateTo({
    url: `/pages/review/create?orderId=${orderId}&storeName=${encodeURIComponent(order.value.storeName)}`
  })
}

onLoad((query) => {
  orderId = query.orderId
  load()
})
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 60rpx; }
.card { background: #fff; border-radius: 24rpx; padding: 28rpx; margin-bottom: 24rpx; }
.card-title { font-size: 30rpx; font-weight: 600; color: #1f2329; margin-bottom: 16rpx; }

.status-card { text-align: center; padding: 48rpx 28rpx; }
.status-title { font-size: 40rpx; font-weight: 700; color: #1f2329; }
.status-sub { margin-top: 12rpx; font-size: 26rpx; }
.cd-red { color: #e74c3c; }
.no-show-tip { margin-top: 12rpx; font-size: 24rpx; color: #e6a23c; }
.primary-btn {
  margin-top: 32rpx; background: #4a7cf7; color: #fff;
  border-radius: 999rpx; font-size: 30rpx;
}

.info-row {
  display: flex; justify-content: space-between; gap: 24rpx;
  padding: 16rpx 0; font-size: 26rpx; color: #303133;
}
.label { color: #86909c; flex-shrink: 0; }
.discount { color: #27ae60; }
.pay-amount { font-weight: 700; color: #e74c3c; }
.refund-amount { color: #27ae60; }
.order-no { font-size: 22rpx; color: #909399; word-break: break-all; text-align: right; }

/* 时间轴 */
.timeline { padding-left: 8rpx; }
.tl-node { display: flex; gap: 20rpx; padding-bottom: 32rpx; position: relative; }
.tl-node::before {
  content: ''; position: absolute; left: 9rpx; top: 24rpx; bottom: 0;
  width: 2rpx; background: #e4e7ed;
}
.tl-node.last::before { display: none; }
.tl-node.last { padding-bottom: 4rpx; }
.tl-dot {
  width: 20rpx; height: 20rpx; border-radius: 50%;
  background: #dcdfe6; margin-top: 6rpx; flex-shrink: 0;
  border: 4rpx solid #f0f0f0; box-sizing: content-box;
}
.tl-dot.done { background: #4a7cf7; }
.tl-title { font-size: 28rpx; color: #303133; font-weight: 500; }
.tl-desc { margin-top: 4rpx; font-size: 24rpx; color: #86909c; }
.tl-time { margin-top: 4rpx; font-size: 22rpx; color: #b0b3ba; }

.loading { text-align: center; color: #b0b3ba; padding: 160rpx 0; }
</style>
