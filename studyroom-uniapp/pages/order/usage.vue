<template>
  <!-- 使用中页 —— FR-UC-031 签到后进入 / FR-UC-033 暂停与恢复 / FR-UC-034 离场结算 -->
  <view class="page">
    <!-- ==================== 进行中：倒计时卡片 ==================== -->
    <view v-if="usage.status === 2" class="card">
      <!-- 暂停提示（FR-UC-033） -->
      <view v-if="usage.paused" class="pause-banner">
        <view class="pause-title">⏸ 暂停中 · 座位保留</view>
        <view class="pause-desc">
          请在 {{ fmt(usage.pauseDeadline) }} 前回座恢复，超时将自动结算并退还差额
          <text v-if="usage.pauseRemainingMinutes !== null" class="pause-count">
            （剩 {{ usage.pauseRemainingMinutes }} 分钟）
          </text>
        </view>
      </view>

      <view class="status-line">
        <view class="status-dot" :class="usage.paused ? 'paused' : 'active'" />
        <text>{{ usage.paused ? '已暂停' : '使用中' }}</text>
      </view>

      <!-- 剩余时间倒计时 -->
      <view class="countdown">
        <text class="cd-num">{{ cdText }}</text>
        <text class="cd-unit">{{ usage.paused ? '暂停计时不流逝' : '距离预约结束' }}</text>
      </view>

      <view class="meta-grid">
        <view class="meta-item">
          <view class="meta-label">门店</view>
          <view class="meta-value">{{ usage.storeName || '-' }}</view>
        </view>
        <view class="meta-item">
          <view class="meta-label">座位</view>
          <view class="meta-value">{{ usage.seatNo || '-' }}</view>
        </view>
        <view class="meta-item">
          <view class="meta-label">签到时间</view>
          <view class="meta-value">{{ fmt(usage.signTime) }}</view>
        </view>
        <view class="meta-item">
          <view class="meta-label">预约时段</view>
          <view class="meta-value small">{{ fmt(usage.startTime) }}~{{ fmt(usage.endTime) }}</view>
        </view>
      </view>

      <!-- 操作按钮：按状态切换（FR-UC-037 同款逻辑，此处先覆盖签到场景） -->
      <view class="btn-area">
        <button v-if="!usage.paused" class="btn-warn" :loading="acting" @click="doPause">
          临时离座（暂停）
        </button>
        <button v-else class="btn-primary" :loading="acting" @click="doResume">
          回座恢复
        </button>
        <!-- FR-UC-055 进行中续时（后端 extendable：进行中且未暂停） -->
        <button v-if="usage.extendable" class="btn-extend" @click="openExtend">
          续时延时{{ usage.extendCount ? '（已续 ' + usage.extendCount + ' 次）' : '' }}
        </button>
        <button class="btn-end" :loading="acting" @click="confirmEnd">结束使用</button>
      </view>

      <view class="hint">
        已暂停 {{ usage.pauseCount || 0 }} 次<text v-if="usage.totalPausedMinutes"> · 累计 {{ usage.totalPausedMinutes }} 分钟</text>；
        暂停每次最长保留 30 分钟
      </view>
    </view>

    <!-- ==================== 已完成：结算结果（FR-UC-034） ==================== -->
    <view v-else-if="usage.status === 3" class="card">
      <view class="result-icon">✓</view>
      <view class="result-title">使用结束</view>
      <view class="settle-card">
        <view class="info-row"><text class="label">实际使用</text><text class="strong">{{ usage.actualDurationMinutes }} 分钟</text></view>
        <view class="info-row"><text class="label">结束时间</text><text>{{ fmt(usage.actualEndTime) }}</text></view>
        <view class="info-row">
          <text class="label">退还差额</text>
          <text class="refund">¥ {{ (usage.refundAmount || 0).toFixed(2) }}（已退至余额）</text>
        </view>
      </view>
      <view class="hint">座位已转"打扫中"，感谢使用，欢迎评价本次体验～</view>
      <button class="btn-plain" @click="goBack">返回首页</button>
    </view>

    <!-- 加载失败 -->
    <view v-else class="card">
      <view class="result-title">订单不在使用中</view>
      <button class="btn-plain" @click="goBack">返回</button>
    </view>

    <!-- ==================== FR-UC-055 续时弹层 ==================== -->
    <view v-if="showExtend" class="extend-mask" @click="showExtend = false">
      <view class="extend-panel" @click.stop>
        <view class="extend-title">续时延时</view>
        <view class="extend-sub">当前套餐：{{ extendInfo.packageName || '-' }}（¥{{ extendInfo.unitPrice }}/小时）</view>

        <!-- 时长选择 -->
        <view class="hour-grid">
          <view v-for="h in hourOptions" :key="h" class="hour-item"
                :class="{ active: extendHours === h }" @click="chooseHours(h)">
            {{ h }}小时
          </view>
        </view>

        <!-- 费用明细（FR-UC-055 预览结果） -->
        <view class="extend-fee" v-if="extendInfo.amount != null">
          <view class="fee-row">
            <text>续时费用</text>
            <text class="fee-amount">¥{{ extendInfo.amount }}</text>
          </view>
          <view class="fee-row">
            <text>新结束时间</text>
            <text>{{ fmt(extendInfo.newEndTime) }}</text>
          </view>
          <view class="fee-row">
            <text>账户余额</text>
            <text>¥{{ extendInfo.balance }}</text>
          </view>
        </view>

        <!-- 支付方式（FR-UC-023 同款） -->
        <view class="pay-types">
          <view class="pay-item" :class="{ active: extendPayType === 1 }" @click="extendPayType = 1">
            <text>余额支付</text>
            <text class="pay-sub">¥{{ extendInfo.balance }}</text>
          </view>
          <view class="pay-item" :class="{ active: extendPayType === 2 }" @click="extendPayType = 2">
            <text>微信支付</text>
            <text class="pay-sub">模拟支付</text>
          </view>
        </view>

        <button class="btn-primary extend-btn" :loading="extending" :disabled="extending" @click="doExtend">
          确认续时 ¥{{ extendInfo.amount || '0.00' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script>
/**
 * 使用中页（FR-UC-031 / FR-UC-033 / FR-UC-034 / FR-UC-055）
 *
 * - 进入页面拉取 GET /order/{id}/usage，展示剩余时间倒计时（每秒本地刷新，每分钟对齐服务端）
 * - 暂停/恢复：FR-UC-033（暂停计时保留 30 分钟，超时自动结算）
 * - 结束使用：FR-UC-034（实际时长结算 + 未使用差额退余额，座位转打扫中）
 * - 续时延时：FR-UC-055（按套餐单价折算，支付后顺延结束时间并续占座位）
 */
import { getUsage, pauseOrder, resumeOrder, endOrder, extendPreview, extendOrder } from '@/api/order'

export default {
  data() {
    return {
      orderId: null,
      usage: {},
      acting: false,
      remainSeconds: 0,
      timer: null,
      // FR-UC-055 续时弹层
      showExtend: false,
      hourOptions: [1, 2, 3, 4],
      extendHours: 1,
      extendInfo: {},
      extendPayType: 1,
      extending: false
    }
  },
  computed: {
    /** 剩余时间 mm:ss / hh:mm:ss */
    cdText() {
      const s = Math.max(this.remainSeconds, 0)
      const h = Math.floor(s / 3600)
      const m = Math.floor((s % 3600) / 60)
      const sec = s % 60
      const p = (n) => String(n).padStart(2, '0')
      return h > 0 ? `${p(h)}:${p(m)}:${p(sec)}` : `${p(m)}:${p(sec)}`
    }
  },
  onLoad(query) {
    this.orderId = Number(query.orderId)
    this.fetchUsage()
  },
  onShow() {
    // 从其他页面返回时刷新（如申诉后回来）
    if (this.orderId) this.fetchUsage()
  },
  onUnload() {
    if (this.timer) clearInterval(this.timer)
  },
  methods: {
    /** 拉取使用状态并启动倒计时（FR-UC-031） */
    async fetchUsage() {
      if (!this.orderId) return
      this.usage = await getUsage(this.orderId)
      this.remainSeconds = (this.usage.remainingMinutes || 0) * 60
      if (this.timer) clearInterval(this.timer)
      // 每秒本地递减；暂停中不流逝
      this.timer = setInterval(() => {
        if (this.usage.status === 2 && !this.usage.paused && this.remainSeconds > 0) {
          this.remainSeconds -= 1
        }
      }, 1000)
      // 每 60 秒对齐服务端（暂停截止/状态可能被延迟任务改变）
      setTimeout(() => {
        const align = setInterval(async () => {
          try {
            if (this.usage.status === 2) {
              this.usage = await getUsage(this.orderId)
              this.remainSeconds = (this.usage.remainingMinutes || 0) * 60
            } else {
              clearInterval(align)
            }
          } catch (e) { /* 静默，下轮重试 */ }
        }, 60000)
      }, 60000)
    },

    /** 临时离座（FR-UC-033） */
    async doPause() {
      this.acting = true
      try {
        this.usage = await pauseOrder(this.orderId)
        uni.showToast({ title: '已暂停，座位保留30分钟', icon: 'none' })
      } finally {
        this.acting = false
      }
    },

    /** 回座恢复（FR-UC-033） */
    async doResume() {
      this.acting = true
      try {
        this.usage = await resumeOrder(this.orderId)
        this.remainSeconds = (this.usage.remainingMinutes || 0) * 60
        uni.showToast({ title: '已恢复使用', icon: 'success' })
      } finally {
        this.acting = false
      }
    },

    /** 结束使用二次确认（FR-UC-034） */
    confirmEnd() {
      uni.showModal({
        title: '结束使用',
        content: '确定要离场结束使用吗？未使用部分将按比例退还至余额。',
        confirmText: '结束使用',
        success: async (res) => {
          if (!res.confirm) return
          this.acting = true
          try {
            this.usage = await endOrder(this.orderId)
            uni.showToast({ title: '已结算', icon: 'success' })
          } finally {
            this.acting = false
          }
        }
      })
    },

    /* ==================== FR-UC-055 续时 ==================== */

    /** 打开续时弹层并拉取默认时长（1 小时）费用预览 */
    async openExtend() {
      this.showExtend = true
      this.extendHours = 1
      this.extendPayType = 1
      await this.loadExtendPreview()
    },

    /** 切换续时时长，重新预览费用 */
    async chooseHours(h) {
      if (this.extendHours === h) return
      this.extendHours = h
      await this.loadExtendPreview()
    },

    /** 续时费用预览（FR-UC-055：按当前套餐单价折算） */
    async loadExtendPreview() {
      try {
        this.extendInfo = await extendPreview(this.orderId, this.extendHours)
      } catch (e) {
        // 座位顺延时段冲突等业务错误：提示并关闭弹层
        this.showExtend = false
      }
    },

    /** 确认续时（FR-UC-055：支付后顺延结束时间并续占座位） */
    async doExtend() {
      if (this.extendPayType === 1 &&
          Number(this.extendInfo.balance) < Number(this.extendInfo.amount)) {
        uni.showToast({ title: '余额不足，请先充值或改用微信支付', icon: 'none' })
        return
      }
      this.extending = true
      try {
        this.usage = await extendOrder(this.orderId, {
          hours: this.extendHours,
          payType: this.extendPayType
        })
        this.remainSeconds = (this.usage.remainingMinutes || 0) * 60
        this.showExtend = false
        uni.showToast({ title: '续时成功', icon: 'success' })
      } finally {
        this.extending = false
      }
    },

    fmt(t) {
      if (!t) return '-'
      const d = new Date(String(t).replace(/-/g, '/'))
      const p = (n) => String(n).padStart(2, '0')
      return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
    },

    goBack() {
      uni.switchTab ? uni.switchTab({ url: '/pages/index/index' }) : uni.navigateTo({ url: '/pages/index/index' })
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f6fa;
  padding: 30rpx;
}
.card {
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx 36rpx;
}

/* ---------- 暂停横幅 ---------- */
.pause-banner {
  background: #fff7e8;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 30rpx;
}
.pause-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #ff976a;
  margin-bottom: 8rpx;
}
.pause-desc {
  font-size: 24rpx;
  color: #b8814f;
  line-height: 1.6;
}
.pause-count { font-weight: 600; }

/* ---------- 状态与倒计时 ---------- */
.status-line {
  display: flex;
  align-items: center;
  gap: 12rpx;
  font-size: 28rpx;
  color: #4e5969;
}
.status-dot {
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
}
.status-dot.active {
  background: #00b578;
  box-shadow: 0 0 12rpx rgba(0, 181, 120, 0.5);
}
.status-dot.paused { background: #ff976a; }

.countdown {
  text-align: center;
  margin: 36rpx 0;
}
.cd-num {
  display: block;
  font-size: 96rpx;
  font-weight: 700;
  color: #1f2329;
  font-family: 'DIN Alternate', 'Menlo', monospace;
  letter-spacing: 4rpx;
}
.cd-unit {
  font-size: 24rpx;
  color: #86909c;
}

/* ---------- 信息网格 ---------- */
.meta-grid {
  display: flex;
  flex-wrap: wrap;
  background: #f7f8fa;
  border-radius: 16rpx;
  padding: 12rpx 24rpx;
  margin-bottom: 36rpx;
}
.meta-item {
  width: 50%;
  padding: 14rpx 0;
}
.meta-label {
  font-size: 24rpx;
  color: #86909c;
  margin-bottom: 6rpx;
}
.meta-value {
  font-size: 28rpx;
  color: #1f2329;
}
.meta-value.small { font-size: 24rpx; }

/* ---------- 按钮区 ---------- */
.btn-area {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.btn-primary {
  background: #4a7cf7;
  color: #fff;
  font-size: 30rpx;
  border-radius: 44rpx;
}
.btn-warn {
  background: #fff7e8;
  color: #ff976a;
  font-size: 30rpx;
  border-radius: 44rpx;
}
.btn-end {
  background: #fdeceb;
  color: #e54d42;
  font-size: 30rpx;
  border-radius: 44rpx;
}
.btn-plain {
  background: #fff;
  color: #4a7cf7;
  border: 2rpx solid #4a7cf7;
  font-size: 30rpx;
  border-radius: 44rpx;
  margin-top: 24rpx;
}
.btn-extend {
  background: #eef3ff;
  color: #4a7cf7;
  font-size: 30rpx;
  border-radius: 44rpx;
}

/* ---------- FR-UC-055 续时弹层 ---------- */
.extend-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  z-index: 999;
}
.extend-panel {
  width: 100%;
  background: #fff;
  border-radius: 32rpx 32rpx 0 0;
  padding: 40rpx 36rpx calc(40rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}
.extend-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #1f2329;
  text-align: center;
}
.extend-sub {
  font-size: 24rpx;
  color: #86909c;
  text-align: center;
  margin: 12rpx 0 30rpx;
}
.hour-grid {
  display: flex;
  gap: 20rpx;
  margin-bottom: 30rpx;
}
.hour-item {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  background: #f7f8fa;
  border-radius: 14rpx;
  font-size: 28rpx;
  color: #4e5969;
  border: 2rpx solid transparent;
}
.hour-item.active {
  background: #eef3ff;
  color: #4a7cf7;
  border-color: #4a7cf7;
  font-weight: 600;
}
.extend-fee {
  background: #f7f8fa;
  border-radius: 16rpx;
  padding: 6rpx 24rpx;
  margin-bottom: 24rpx;
}
.fee-row {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 0;
  font-size: 26rpx;
  color: #4e5969;
  border-bottom: 1rpx solid #f0f1f3;
}
.fee-row:last-child { border-bottom: none; }
.fee-amount {
  color: #e54d42;
  font-weight: 700;
  font-size: 30rpx;
}
.pay-types {
  display: flex;
  gap: 20rpx;
  margin-bottom: 30rpx;
}
.pay-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  padding: 18rpx 0;
  background: #f7f8fa;
  border-radius: 14rpx;
  font-size: 28rpx;
  color: #4e5969;
  border: 2rpx solid transparent;
}
.pay-item.active {
  background: #eef3ff;
  color: #4a7cf7;
  border-color: #4a7cf7;
  font-weight: 600;
}
.pay-sub {
  font-size: 22rpx;
  color: #86909c;
}
.extend-btn { margin-top: 0; }
.hint {
  margin-top: 24rpx;
  font-size: 22rpx;
  color: #c9cdd4;
  text-align: center;
  line-height: 1.6;
}

/* ---------- 结算结果 ---------- */
.result-icon {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: #e8f7e8;
  color: #00b578;
  font-size: 64rpx;
  font-weight: 700;
  line-height: 120rpx;
  text-align: center;
  margin: 0 auto;
  animation: pop 0.4s ease-out;
}
@keyframes pop {
  0% { transform: scale(0.3); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}
.result-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #1f2329;
  text-align: center;
  margin: 24rpx 0 30rpx;
}
.settle-card {
  background: #f7f8fa;
  border-radius: 16rpx;
  padding: 10rpx 24rpx;
}
.info-row {
  display: flex;
  justify-content: space-between;
  padding: 18rpx 0;
  font-size: 28rpx;
  color: #1f2329;
  border-bottom: 1rpx solid #f0f1f3;
}
.info-row:last-child { border-bottom: none; }
.label { color: #86909c; }
.strong { font-weight: 600; }
.refund { color: #e54d42; font-weight: 600; }
</style>
