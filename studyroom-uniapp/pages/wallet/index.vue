<template>
  <!-- FR-UC-048 我的钱包：余额 + 充值（模拟微信支付）+ 流水 -->
  <view class="page">
    <!-- 余额卡片 -->
    <view class="balance-card">
      <view class="balance-label">账户余额（元）</view>
      <view class="balance-num">¥{{ balance }}</view>
      <view class="credit-row">
        <text>信用分</text>
        <text class="credit">{{ creditScore }}</text>
      </view>
      <button class="recharge-btn" @click="showRecharge = true">充值</button>
    </view>

    <!-- 流水 -->
    <view class="card">
      <view class="card-title">余额流水</view>
      <view v-if="logs.length === 0 && !loading" class="empty-mini">暂无流水记录</view>
      <view v-for="l in logs" :key="l.id" class="log-row">
        <view class="log-info">
          <view class="log-type">{{ l.typeDesc }}</view>
          <view class="log-remark">{{ l.remark || '-' }}</view>
          <view class="log-time">{{ fmt(l.createTime) }}</view>
        </view>
        <view class="log-amount" :class="{ in: l.amount > 0, out: l.amount < 0 }">
          {{ l.amount > 0 ? '+' : '' }}{{ l.amount }}
        </view>
      </view>
      <view v-if="loading" class="loading">加载中...</view>
      <view v-else-if="finished && logs.length > 0" class="loading">没有更多了</view>
    </view>

    <!-- 充值弹层（FR-UC-048：1~2000 元，模拟微信支付直接到账） -->
    <view v-if="showRecharge" class="mask" @click="showRecharge = false">
      <view class="recharge-panel" @click.stop>
        <view class="panel-title">余额充值</view>
        <view class="quick-amounts">
          <view v-for="a in quickAmounts" :key="a" class="amount-item"
                :class="{ active: amount === a }" @click="amount = a">¥{{ a }}</view>
        </view>
        <view class="custom-row">
          <text>自定义</text>
          <input v-model="amount" type="digit" class="amount-input" placeholder="1~2000" />
          <text>元</text>
        </view>
        <view class="pay-tip">模拟微信支付，确认后立即到账</view>
        <button class="confirm-btn" :loading="paying" :disabled="paying" @click="onRecharge">确认充值</button>
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 钱包页 —— FR-UC-048（模块7）
 * 充值走幂等 token（FR-SY-001），到账后本地刷新余额与流水
 */
import { ref, computed } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import dayjs from 'dayjs'
import { getWallet, recharge, getBalanceLogs } from '@/api/profile'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

/** 余额（后端直接返回 BigDecimal 数字） */
const balance = ref('0.00')
/** 信用分（来自用户信息 UserVO） */
const creditScore = computed(() => (userStore.userInfo && userStore.userInfo.creditScore) || 100)
const logs = ref([])
const page = ref(1)
const size = 20
const finished = ref(false)
const loading = ref(false)

const showRecharge = ref(false)
const quickAmounts = [50, 100, 200, 500]
const amount = ref(100)
const paying = ref(false)

const fmt = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '')

async function loadWallet() {
  balance.value = await getWallet()
}

async function loadLogs(reset = false) {
  if (loading.value) return
  if (reset) {
    page.value = 1
    finished.value = false
  } else if (finished.value) return
  loading.value = true
  try {
    const res = await getBalanceLogs(page.value, size)
    const list = res.list || []
    logs.value = reset ? list : logs.value.concat(list)
    if (logs.value.length >= (res.total || 0) || list.length < size) finished.value = true
    else page.value++
  } finally {
    loading.value = false
  }
}

/** FR-UC-048 充值：1~2000 元校验 + 模拟支付直接到账 */
async function onRecharge() {
  const val = Number(amount.value)
  if (!val || val < 1 || val > 2000) {
    uni.showToast({ title: '充值金额需在 1~2000 元之间', icon: 'none' })
    return
  }
  paying.value = true
  try {
    await recharge(val)
    uni.showToast({ title: '充值成功', icon: 'success' })
    showRecharge.value = false
    loadWallet()
    loadLogs(true)
  } catch (e) { /* 请求层已提示 */ } finally {
    paying.value = false
  }
}

onShow(() => {
  loadWallet()
  loadLogs(true)
})
onReachBottom(() => loadLogs(false))
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 60rpx; }

.balance-card {
  background: linear-gradient(135deg, #4a7cf7, #6a5af9);
  border-radius: 24rpx; padding: 48rpx 36rpx; color: #fff; margin-bottom: 24rpx;
}
.balance-label { font-size: 24rpx; opacity: .85; }
.balance-num { font-size: 72rpx; font-weight: 700; margin-top: 12rpx; }
.credit-row { margin-top: 16rpx; font-size: 24rpx; opacity: .85; display: flex; gap: 12rpx; }
.recharge-btn {
  margin-top: 32rpx; background: #fff; color: #4a7cf7;
  border-radius: 999rpx; font-size: 28rpx; width: 240rpx;
}

.card { background: #fff; border-radius: 24rpx; padding: 28rpx; }
.card-title { font-size: 30rpx; font-weight: 600; color: #1f2329; margin-bottom: 12rpx; }

.log-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20rpx 0; border-bottom: 1rpx solid #f0f0f0;
}
.log-row:last-child { border-bottom: none; }
.log-type { font-size: 28rpx; color: #303133; }
.log-remark { font-size: 24rpx; color: #86909c; margin-top: 4rpx; }
.log-time { font-size: 22rpx; color: #b0b3ba; margin-top: 4rpx; }
.log-amount { font-size: 32rpx; font-weight: 700; }
.log-amount.in { color: #e74c3c; }   /* 收入（充值/退款）：红 */
.log-amount.out { color: #27ae60; }  /* 支出：绿 */

.empty-mini { text-align: center; color: #b0b3ba; font-size: 26rpx; padding: 60rpx 0; }
.loading { text-align: center; color: #b0b3ba; font-size: 26rpx; padding: 24rpx 0; }

/* 充值弹层 */
.mask {
  position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 99;
  display: flex; align-items: flex-end;
}
.recharge-panel {
  width: 100%; background: #fff; border-radius: 32rpx 32rpx 0 0; padding: 40rpx 36rpx calc(40rpx + env(safe-area-inset-bottom));
}
.panel-title { font-size: 32rpx; font-weight: 600; text-align: center; margin-bottom: 32rpx; }
.quick-amounts { display: flex; gap: 20rpx; }
.amount-item {
  flex: 1; text-align: center; padding: 24rpx 0; border-radius: 16rpx;
  background: #f5f6fa; color: #303133; font-size: 30rpx; font-weight: 600;
  border: 1rpx solid transparent;
}
.amount-item.active { background: #eef3fe; color: #4a7cf7; border-color: #4a7cf7; }
.custom-row {
  display: flex; align-items: center; gap: 16rpx; margin-top: 28rpx;
  font-size: 28rpx; color: #606266;
}
.amount-input {
  flex: 1; background: #f5f6fa; border-radius: 12rpx; padding: 16rpx 24rpx;
  font-size: 30rpx; text-align: right;
}
.pay-tip { margin-top: 20rpx; font-size: 22rpx; color: #b0b3ba; text-align: center; }
.confirm-btn {
  margin-top: 32rpx; background: #4a7cf7; color: #fff;
  border-radius: 999rpx; font-size: 32rpx;
}
.confirm-btn[disabled] { opacity: .6; }
</style>
