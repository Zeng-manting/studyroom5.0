<template>
  <!-- 订单确认页：FR-UC-022 费用明细 + FR-UC-023 支付方式 + FR-UC-024 优惠券 -->
  <view class="page">
    <view v-if="preview" class="body">
      <!-- 座位与时段信息（FR-UC-022） -->
      <view class="card">
        <view class="store-name">{{ preview.storeName }}</view>
        <view class="seat-row">
          <view class="seat-no">{{ preview.seatNo }}</view>
          <view class="seat-type">{{ preview.seatTypeName }} · {{ preview.packageName }}</view>
        </view>
        <view class="time-row">
          <view class="time-block">
            <text class="time-label">开始</text>
            <text class="time-val">{{ fmt(preview.startTime) }}</text>
          </view>
          <view class="time-arrow">→</view>
          <view class="time-block">
            <text class="time-label">结束</text>
            <text class="time-val">{{ fmt(preview.endTime) }}</text>
          </view>
        </view>
      </view>

      <!-- 优惠券（FR-UC-024） -->
      <view class="card" @click="showCoupons = true">
        <view class="row">
          <text class="row-label">优惠券</text>
          <text v-if="coupon" class="row-value highlight">-¥{{ coupon.deductibleAmount }}（{{ coupon.discountDesc }}）</text>
          <text v-else-if="coupons.length" class="row-value link">{{ coupons.length }} 张可用 ›</text>
          <text v-else class="row-value grey">暂无可用</text>
        </view>
      </view>

      <!-- 费用明细（FR-UC-022：原价 - 优惠 - 实付） -->
      <view class="card">
        <view class="row">
          <text class="row-label">原价</text>
          <text class="row-value">¥{{ preview.originalPrice }}</text>
        </view>
        <view v-if="Number(preview.discountAmount) > 0" class="row">
          <text class="row-label">套餐优惠</text>
          <text class="row-value highlight">-¥{{ preview.discountAmount }}</text>
        </view>
        <view v-if="Number(preview.couponAmount) > 0" class="row">
          <text class="row-label">券抵扣</text>
          <text class="row-value highlight">-¥{{ preview.couponAmount }}</text>
        </view>
        <view class="row total-row">
          <text class="row-label">实付</text>
          <text class="total">¥{{ payAmount }}</text>
        </view>
      </view>

      <!-- 支付方式（FR-UC-023：余额优先 + 模拟微信支付） -->
      <view class="card">
        <view class="card-title">支付方式</view>
        <view class="pay-item" :class="{ active: payType === 1 }" @click="payType = 1">
          <text class="pay-icon">💰</text>
          <view class="pay-info">
            <text class="pay-name">余额支付</text>
            <text class="pay-sub">余额 ¥{{ preview.userBalance }}</text>
          </view>
          <text v-if="insufficient" class="pay-warn">余额不足</text>
          <view class="radio" :class="{ on: payType === 1 }" />
        </view>
        <view class="pay-item" :class="{ active: payType === 2 }" @click="payType = 2">
          <text class="pay-icon">💚</text>
          <view class="pay-info">
            <text class="pay-name">微信支付</text>
            <text class="pay-sub">模拟支付（联调环境）</text>
          </view>
          <view class="radio" :class="{ on: payType === 2 }" />
        </view>
      </view>
    </view>

    <view v-else class="loading">订单确认信息加载中...</view>

    <!-- 底部支付按钮 -->
    <view class="footer-bar">
      <view class="footer-amount">
        <text class="footer-label">应付</text>
        <text class="footer-num">¥{{ payAmount }}</text>
      </view>
      <button class="footer-btn" :disabled="submitting || !preview" :class="{ disabled: submitting || !preview }"
              @click="submitPay">立即支付</button>
    </view>

    <!-- 优惠券弹层（FR-UC-024） -->
    <view v-if="showCoupons" class="mask" @click="showCoupons = false">
      <view class="sheet" @click.stop>
        <view class="sheet-title">选择优惠券</view>
        <scroll-view scroll-y class="sheet-body">
          <view class="coupon-none" @click="pickCoupon(null)">不使用优惠券</view>
          <view v-for="c in coupons" :key="c.claimId" class="coupon-item"
                :class="{ active: coupon && coupon.claimId === c.claimId }"
                @click="pickCoupon(c)">
            <view class="coupon-left">
              <text class="coupon-amount">¥{{ c.deductibleAmount }}</text>
            </view>
            <view class="coupon-right">
              <text class="coupon-name">{{ c.name }}（{{ c.discountDesc }}）</text>
              <text class="coupon-expire">{{ c.expireTime }} 前有效</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { previewOrder, getUsableCoupons, createOrder, payOrder } from '@/api/order'

const storeId = ref(null)
const seatId = ref(null)
const packageId = ref(null)
const startTime = ref('')

const preview = ref(null)
const coupons = ref([])
const coupon = ref(null)
const payType = ref(1) // FR-UC-023 余额优先
const submitting = ref(false)
const showCoupons = ref(false)

const payAmount = computed(() => preview.value ? preview.value.payAmount : '0.00')
const insufficient = computed(() =>
  preview.value && payType.value === 1 &&
  Number(preview.value.userBalance) < Number(preview.value.payAmount))

function fmt(t) {
  return t ? dayjs(t).format('MM-DD HH:mm') : ''
}

async function loadPreview() {
  preview.value = await previewOrder({
    storeId: storeId.value,
    seatId: seatId.value,
    packageId: packageId.value,
    startTime: startTime.value,
    couponClaimId: coupon.value ? coupon.value.claimId : undefined
  })
}

async function loadCoupons() {
  coupons.value = await getUsableCoupons({
    storeId: storeId.value,
    seatId: seatId.value,
    packageId: packageId.value,
    startTime: startTime.value
  })
}

/** 选券后重算费用（FR-UC-024） */
async function pickCoupon(c) {
  coupon.value = c
  showCoupons.value = false
  await loadPreview()
}

/** FR-UC-018 创建订单（原子锁座）→ FR-UC-023 支付 */
async function submitPay() {
  if (submitting.value) return
  if (insufficient.value) {
    uni.showToast({ title: '余额不足，请选择微信支付', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    // 1. FR-UC-018 创建订单（Redisson 原子锁座 + 幂等 token）
    const created = await createOrder({
      storeId: storeId.value,
      seatId: seatId.value,
      packageId: packageId.value,
      startTime: startTime.value,
      couponClaimId: coupon.value ? coupon.value.claimId : null
    })
    // 2. FR-UC-023 支付
    try {
      await payOrder(created.orderId, payType.value)
      // 3. FR-UC-028 支付成功页
      uni.redirectTo({ url: `/pages/order/pay-success?orderId=${created.orderId}` })
    } catch (e) {
      // 支付失败留在当前页，订单进入待支付（15 分钟内可重新支付，FR-UC-025）
      uni.showModal({
        title: '支付未完成',
        content: '订单已保留 15 分钟，请尽快完成支付，超时将自动取消',
        showCancel: false
      })
    }
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const pages = getCurrentPages()
  const cur = pages[pages.length - 1]
  storeId.value = cur.options.storeId || cur.options.storeid
  seatId.value = cur.options.seatId || cur.options.seatid
  packageId.value = cur.options.packageId || cur.options.packageid
  startTime.value = decodeURIComponent(cur.options.startTime || cur.options.starttime || '')

  await Promise.all([loadPreview(), loadCoupons()])
})
</script>

<style scoped>
.page { padding: 24rpx 24rpx 160rpx; background: #f5f6fa; min-height: 100vh; }
.body { display: flex; flex-direction: column; gap: 24rpx; }
.card { background: #fff; border-radius: 20rpx; padding: 24rpx; }
.card-title { font-size: 30rpx; font-weight: 600; color: #1f2329; margin-bottom: 16rpx; }

.store-name { font-size: 30rpx; font-weight: 600; color: #1f2329; margin-bottom: 16rpx; }
.seat-row { display: flex; align-items: center; gap: 16rpx; margin-bottom: 20rpx; }
.seat-no { font-size: 40rpx; font-weight: 700; color: #4a7cf7; }
.seat-type { font-size: 24rpx; color: #86909c; }
.time-row { display: flex; align-items: center; justify-content: space-between; background: #f5f6fa; border-radius: 14rpx; padding: 20rpx; }
.time-block { flex: 1; text-align: center; }
.time-label { display: block; font-size: 22rpx; color: #86909c; margin-bottom: 6rpx; }
.time-val { font-size: 30rpx; font-weight: 600; color: #1f2329; }
.time-arrow { color: #c9cdd4; font-size: 32rpx; }

.row { display: flex; align-items: center; justify-content: space-between; padding: 14rpx 0; }
.row-label { font-size: 27rpx; color: #4e5969; }
.row-value { font-size: 27rpx; color: #1f2329; }
.row-value.highlight { color: #ff7d00; }
.row-value.link { color: #4a7cf7; }
.row-value.grey { color: #c9cdd4; }
.total-row { border-top: 1rpx solid #f2f3f5; margin-top: 8rpx; padding-top: 20rpx; }
.total { font-size: 40rpx; font-weight: 700; color: #ff7d00; }

.pay-item { display: flex; align-items: center; padding: 24rpx 16rpx; border: 2rpx solid #f2f3f5; border-radius: 14rpx; margin-bottom: 16rpx; }
.pay-item.active { border-color: #4a7cf7; background: rgba(74, 124, 247, .04); }
.pay-icon { font-size: 40rpx; margin-right: 20rpx; }
.pay-info { flex: 1; display: flex; flex-direction: column; }
.pay-name { font-size: 28rpx; color: #1f2329; font-weight: 500; }
.pay-sub { margin-top: 4rpx; font-size: 22rpx; color: #86909c; }
.pay-warn { font-size: 22rpx; color: #f53f3f; margin-right: 12rpx; }
.radio { width: 32rpx; height: 32rpx; border-radius: 50%; border: 2rpx solid #c9cdd4; }
.radio.on { border-color: #4a7cf7; background: #4a7cf7; box-shadow: inset 0 0 0 6rpx #fff; }

.footer-bar { position: fixed; left: 0; right: 0; bottom: 0; display: flex; align-items: center; padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom)); background: #fff; box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, .04); }
.footer-amount { flex: 1; display: flex; align-items: baseline; gap: 10rpx; }
.footer-label { font-size: 26rpx; color: #4e5969; }
.footer-num { font-size: 44rpx; font-weight: 700; color: #ff7d00; }
.footer-btn { width: 260rpx; height: 88rpx; line-height: 88rpx; background: #4a7cf7; color: #fff; font-size: 30rpx; border-radius: 44rpx; }
.footer-btn.disabled { background: #c9cdd4; }

.loading { padding: 120rpx 0; text-align: center; color: #86909c; }

.mask { position: fixed; inset: 0; background: rgba(0, 0, 0, .45); z-index: 99; display: flex; align-items: flex-end; }
.sheet { width: 100%; background: #fff; border-radius: 32rpx 32rpx 0 0; padding: 32rpx 24rpx calc(32rpx + env(safe-area-inset-bottom)); max-height: 70vh; display: flex; flex-direction: column; }
.sheet-title { text-align: center; font-size: 32rpx; font-weight: 600; color: #1f2329; margin-bottom: 24rpx; }
.sheet-body { max-height: 50vh; }
.coupon-none { text-align: center; padding: 26rpx; color: #86909c; font-size: 27rpx; border-bottom: 1rpx solid #f2f3f5; }
.coupon-item { display: flex; align-items: center; padding: 24rpx 16rpx; border: 2rpx solid #f2f3f5; border-radius: 14rpx; margin-bottom: 16rpx; }
.coupon-item.active { border-color: #ff7d00; background: rgba(255, 125, 0, .04); }
.coupon-left { width: 140rpx; text-align: center; }
.coupon-amount { font-size: 38rpx; font-weight: 700; color: #ff7d00; }
.coupon-right { flex: 1; display: flex; flex-direction: column; }
.coupon-name { font-size: 27rpx; color: #1f2329; }
.coupon-expire { margin-top: 6rpx; font-size: 22rpx; color: #86909c; }
</style>
