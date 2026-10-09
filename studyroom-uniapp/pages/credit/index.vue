<template>
  <!-- FR-UC-053/054 我的信用：信用分 + 爽约记录 + 限制状态 + 变更流水 -->
  <view class="page">
    <!-- 信用分卡片 -->
    <view class="credit-card" :class="{ limited: isLimited }">
      <view class="score-label">信用分</view>
      <view class="score-num">{{ credit.creditScore }}</view>
      <view v-if="!isLimited" class="score-status ok">信用良好，可正常预约</view>
      <view v-else class="score-status bad">
        预约功能受限 · 剩余约 {{ credit.limitRemainingDays }} 天（{{ fmtDate(credit.creditLimitEnd) }} 自动恢复）
      </view>
      <!-- 30 天爽约进度（FR-UC-054：3 次触发限制） -->
      <view class="noshow-progress">
        <view class="progress-text">
          <text>30 天内爽约 {{ credit.noShowCount30d || 0 }}/{{ credit.noShowLimit || 3 }} 次</text>
          <text class="progress-tip">累计 3 次将限制预约 7 天</text>
        </view>
        <view class="progress-bar">
          <view class="progress-inner"
                :class="{ warn: (credit.noShowCount30d || 0) >= (credit.noShowLimit || 3) - 1 }"
                :style="{ width: Math.min((credit.noShowCount30d || 0) / (credit.noShowLimit || 3) * 100, 100) + '%' }">
          </view>
        </view>
      </view>
    </view>

    <!-- 规则说明（FR-UC-053/054） -->
    <view class="card">
      <view class="card-title">信用规则</view>
      <view class="rule-item">· 订单开始后 30 分钟未签到将被自动取消并记为一次爽约</view>
      <view class="rule-item">· 每次爽约扣 10 信用分（下限 0）</view>
      <view class="rule-item">· 30 天内爽约满 3 次，将限制预约 7 天，到期自动恢复</view>
      <view class="rule-item">· 限制期内无法下单，如有疑问请联系门店客服</view>
    </view>

    <!-- 爽约记录（FR-UC-053） -->
    <view class="card">
      <view class="card-title">爽约记录</view>
      <view v-if="!credit.noShows || credit.noShows.length === 0" class="empty-mini">
        暂无爽约记录，继续保持哦
      </view>
      <view v-for="n in credit.noShows" :key="n.orderId" class="noshow-row">
        <view class="noshow-info">
          <view class="noshow-title">爽约 · {{ n.orderNo }}</view>
          <view class="noshow-time">预约开始 {{ fmt(n.startTime) }}</view>
        </view>
        <view class="noshow-tag">已扣分</view>
      </view>
    </view>

    <!-- 信用流水（FR-UC-053/054 变更留痕） -->
    <view class="card">
      <view class="card-title">信用变更</view>
      <view v-if="!credit.logs || credit.logs.length === 0" class="empty-mini">暂无信用变更记录</view>
      <view v-for="(l, i) in credit.logs" :key="i" class="log-row">
        <view class="log-info">
          <view class="log-type">{{ typeLabel(l.type) }}</view>
          <view class="log-remark">{{ l.reason || '-' }}</view>
          <view class="log-time">{{ fmt(l.time) }}</view>
        </view>
        <view class="log-delta" :class="{ minus: (l.scoreDelta || 0) < 0 }">
          {{ (l.scoreDelta || 0) > 0 ? '+' + l.scoreDelta : l.scoreDelta }}
        </view>
      </view>
    </view>

    <view v-if="loading" class="loading">加载中...</view>
  </view>
</template>

<script setup>
/**
 * 信用页 —— FR-UC-053 爽约记录 / FR-UC-054 信用限制（模块8）
 * 展示：信用分、限制状态与到期时间、30 天爽约进度、爽约明细、变更流水
 */
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import dayjs from 'dayjs'
import { getMyCredit } from '@/api/credit'

const credit = ref({})
const loading = ref(false)

const isLimited = computed(() => credit.value.creditStatus === 1)

const fmt = (t) => (t ? dayjs(t).format('MM-DD HH:mm') : '')
const fmtDate = (t) => (t ? dayjs(t).format('YYYY-MM-DD') : '')
const typeLabel = (t) => ({ 1: '爽约扣分', 2: '恢复信用', 3: '限制预约', 4: '解除限制' }[t] || '信用变更')

async function load() {
  loading.value = true
  try {
    credit.value = await getMyCredit()
  } finally {
    loading.value = false
  }
}

onShow(load)
onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f2f3f5; padding: 24rpx; box-sizing: border-box; }

.credit-card {
  background: linear-gradient(135deg, #165dff 0%, #4080ff 100%);
  border-radius: 20rpx; padding: 40rpx 32rpx; color: #fff; margin-bottom: 24rpx;
}
.credit-card.limited { background: linear-gradient(135deg, #f53f3f 0%, #ff7d00 100%); }
.score-label { font-size: 26rpx; opacity: .85; }
.score-num { font-size: 88rpx; font-weight: 700; line-height: 1.2; }
.score-status { font-size: 26rpx; margin-top: 8rpx; opacity: .95; }
.noshow-progress { margin-top: 24rpx; }
.progress-text { display: flex; justify-content: space-between; font-size: 24rpx; opacity: .9; margin-bottom: 12rpx; }
.progress-tip { opacity: .8; }
.progress-bar { height: 12rpx; background: rgba(255,255,255,.25); border-radius: 6rpx; overflow: hidden; }
.progress-inner { height: 100%; background: #fff; border-radius: 6rpx; transition: width .3s; }
.progress-inner.warn { background: #ffe58f; }

.card { background: #fff; border-radius: 20rpx; padding: 28rpx; margin-bottom: 24rpx; }
.card-title { font-size: 30rpx; font-weight: 600; color: #1d2129; margin-bottom: 20rpx; }
.rule-item { font-size: 26rpx; color: #4e5969; line-height: 1.9; }

.empty-mini { text-align: center; color: #86909c; font-size: 26rpx; padding: 30rpx 0; }

.noshow-row { display: flex; align-items: center; justify-content: space-between; padding: 20rpx 0; border-bottom: 1rpx solid #f2f3f5; }
.noshow-row:last-child { border-bottom: none; }
.noshow-title { font-size: 28rpx; color: #1d2129; font-weight: 500; }
.noshow-time { font-size: 24rpx; color: #86909c; margin-top: 6rpx; }
.noshow-tag { font-size: 24rpx; color: #f53f3f; background: #ffece8; border-radius: 8rpx; padding: 6rpx 16rpx; }

.log-row { display: flex; align-items: center; justify-content: space-between; padding: 20rpx 0; border-bottom: 1rpx solid #f2f3f5; }
.log-row:last-child { border-bottom: none; }
.log-type { font-size: 28rpx; color: #1d2129; font-weight: 500; }
.log-remark { font-size: 24rpx; color: #86909c; margin-top: 6rpx; max-width: 480rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.log-time { font-size: 22rpx; color: #c9cdd4; margin-top: 4rpx; }
.log-delta { font-size: 30rpx; font-weight: 600; color: #00b42a; }
.log-delta.minus { color: #f53f3f; }

.loading { text-align: center; color: #86909c; font-size: 26rpx; padding: 24rpx 0; }
</style>
