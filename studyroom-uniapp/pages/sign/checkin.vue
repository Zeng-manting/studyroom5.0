<template>
  <!-- 扫码签到页 —— FR-UC-029 扫码 + GPS / FR-UC-030 五重校验 / FR-UC-031 成功跳使用中 / FR-UC-032 失败申诉 -->
  <view class="page">
    <!-- ==================== 待扫码状态 ==================== -->
    <view v-if="step === 'scan'" class="scan-card">
      <view class="scan-icon">📷</view>
      <view class="scan-title">扫码签到</view>
      <view class="scan-desc">
        到达门店后，扫描桌面/入口处张贴的签到码即可完成签到。
        签到需满足：门店签到码有效、有已支付订单、预约开始前后15分钟内、距门店100米以内。
      </view>
      <view class="tip-card">
        <view class="tip-title">签到五重校验</view>
        <view class="tip-item">① 二维码 AES 解密</view>
        <view class="tip-item">② HMAC 防篡改验签</view>
        <view class="tip-item">③ 有效订单校验</view>
        <view class="tip-item">④ 时间窗口（开始±15分钟）</view>
        <view class="tip-item">⑤ GPS 距离（≤100米）</view>
      </view>
      <button class="btn-primary" :loading="locating" @click="startCheckIn">
        {{ locating ? '定位中…' : '扫码并签到' }}
      </button>
      <view v-if="locError" class="loc-error">{{ locError }}</view>
    </view>

    <!-- ==================== 签到成功（FR-UC-031） ==================== -->
    <view v-else-if="step === 'success'" class="result-card">
      <view class="result-icon success">✓</view>
      <view class="result-title">签到成功</view>
      <view v-if="result.late" class="late-badge">迟到 {{ result.lateMinutes }} 分钟</view>
      <view class="info-card">
        <view class="info-row"><text class="label">门店</text><text>{{ result.storeName }}</text></view>
        <view class="info-row"><text class="label">座位</text><text>{{ result.seatNo }}</text></view>
        <view class="info-row"><text class="label">时段</text><text>{{ fmt(result.startTime) }} ~ {{ fmt(result.endTime) }}</text></view>
        <view class="info-row"><text class="label">签到时间</text><text>{{ fmt(result.signTime) }}</text></view>
        <view class="info-row" v-if="result.gpsDistance !== null && result.gpsDistance !== undefined">
          <text class="label">距门店</text><text>{{ result.gpsDistance }} 米</text>
        </view>
      </view>
      <button class="btn-primary" @click="goUsage">进入使用中</button>
    </view>

    <!-- ==================== 签到失败 + 申诉（FR-UC-032） ==================== -->
    <view v-else-if="step === 'fail'" class="result-card">
      <view class="result-icon fail">✕</view>
      <view class="result-title">签到失败</view>
      <view class="fail-reason">{{ result.failReason }}</view>

      <!-- 申诉表单 -->
      <view v-if="!appealSubmitted" class="appeal-form">
        <view class="form-title">提交申诉</view>
        <view class="form-desc">若确已到店，可提交申诉，管理员核实后会为您补记签到。</view>

        <view class="field-label">申诉原因</view>
        <picker :range="reasonTexts" @change="onReasonChange">
          <view class="picker-value">{{ reasonText || '请选择申诉原因' }}</view>
        </picker>

        <view class="field-label">情况说明</view>
        <textarea
          v-model="appealDesc"
          class="appeal-textarea"
          placeholder="请描述当时的情况（≤500字）"
          :maxlength="500"
        />

        <view class="field-label">凭证图片（最多3张）</view>
        <view class="img-list">
          <view v-for="(img, i) in appealImages" :key="i" class="img-item">
            <image :src="img" mode="aspectFill" />
            <view class="img-del" @click="removeImage(i)">×</view>
          </view>
          <view v-if="appealImages.length < 3" class="img-add" @click="chooseImage">＋</view>
        </view>

        <button class="btn-primary" :loading="submitting" @click="doAppeal">提交申诉</button>
        <button class="btn-plain" @click="resetToScan">重新扫码</button>
      </view>

      <!-- 已提交 -->
      <view v-else class="appeal-done">
        <view class="done-icon">📨</view>
        <view class="done-title">申诉已提交</view>
        <view class="done-desc">管理员将尽快核实处理，结果将通过消息通知您。</view>
        <button class="btn-plain" @click="goBack">返回</button>
      </view>
    </view>
  </view>
</template>

<script>
/**
 * 扫码签到页（FR-UC-029 ~ FR-UC-032）
 *
 * 流程：
 * 1. uni.getLocation(type=gcj02) 获取 GPS（FR-UC-029）
 * 2. uni.scanCode 扫门店签到码（FR-UC-029）
 * 3. POST /sign/checkin 提交五重校验（FR-UC-030）
 * 4. 成功 → 展示签到信息 + 进入使用中页（FR-UC-031）
 * 5. 失败 → 显示失败原因 + 申诉表单（FR-UC-032）
 */
import { checkIn, submitAppeal, uploadAppealImage } from '@/api/checkin'

/** 申诉原因枚举（与后端 AppealCreateDTO.reasonType 对应） */
const REASONS = [
  { type: 'QR_EXPIRED', text: '签到码过期/无法扫描' },
  { type: 'GPS_FAIL', text: '定位不准/距离误判' },
  { type: 'TIME_WINDOW', text: '时间窗口判定有误' },
  { type: 'ORDER_INVALID', text: '订单状态异常' },
  { type: 'OTHER', text: '其他原因' }
]

export default {
  data() {
    return {
      step: 'scan',            // scan | success | fail
      locating: false,
      locError: '',
      result: {},
      // 申诉表单（FR-UC-032）
      reasons: REASONS,
      reasonTexts: REASONS.map((r) => r.text),
      reasonText: '',
      appealDesc: '',
      appealImages: [],
      submitting: false,
      appealSubmitted: false
    }
  },
  methods: {
    /** 扫码签到主流程（FR-UC-029：先定位后扫码） */
    async startCheckIn() {
      this.locError = ''
      this.locating = true
      try {
        // 1. 获取 GPS 定位（uni.getLocation，gcj02 与国内地图坐标系一致）
        const loc = await new Promise((resolve, reject) => {
          uni.getLocation({
            type: 'gcj02',
            success: resolve,
            fail: () => reject(new Error('定位失败'))
          })
        })
        // 2. 扫码（FR-UC-029）
        const scan = await new Promise((resolve, reject) => {
          uni.scanCode({
            onlyFromCamera: true,
            success: resolve,
            fail: () => reject(new Error('cancel'))
          })
        })
        // 3. 提交五重校验（FR-UC-030）
        const result = await checkIn({
          qrcode: scan.result,
          latitude: loc.latitude,
          longitude: loc.longitude
        })
        this.result = result
        this.step = result.success ? 'success' : 'fail'
        if (result.success) {
          uni.showToast({ title: '签到成功', icon: 'success' })
        }
      } catch (e) {
        if (e && e.message === '定位失败') {
          this.locError = '获取定位失败，请检查手机定位授权后重试'
        } else if (e && e.message === 'cancel') {
          // 用户取消扫码，静默返回
        }
        // 接口业务错误由 request 层统一 toast，此处仅复位状态
      } finally {
        this.locating = false
      }
    },

    /** 进入使用中页（FR-UC-031） */
    goUsage() {
      uni.redirectTo({ url: `/pages/order/usage?orderId=${this.result.orderId}` })
    },

    goBack() {
      uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/index/index' }) })
    },

    /** 重置回扫码状态 */
    resetToScan() {
      this.step = 'scan'
      this.result = {}
      this.appealDesc = ''
      this.appealImages = []
      this.reasonText = ''
      this.appealSubmitted = false
    },

    /** 选择申诉原因 */
    onReasonChange(e) {
      this.reasonText = this.reasonTexts[Number(e.detail.value)]
    },

    /** 选择凭证图片（FR-UC-032 + FR-CC-003） */
    chooseImage() {
      uni.chooseImage({
        count: 3 - this.appealImages.length,
        sizeType: ['compressed'],
        success: async (res) => {
          uni.showLoading({ title: '上传中…' })
          try {
            for (const path of res.tempFilePaths) {
              const url = await uploadAppealImage(path)
              this.appealImages.push(url)
            }
          } finally {
            uni.hideLoading()
          }
        }
      })
    },

    removeImage(i) {
      this.appealImages.splice(i, 1)
    },

    /** 提交申诉（FR-UC-032） */
    async doAppeal() {
      if (!this.reasonText) {
        uni.showToast({ title: '请选择申诉原因', icon: 'none' })
        return
      }
      if (!this.appealDesc.trim()) {
        uni.showToast({ title: '请填写情况说明', icon: 'none' })
        return
      }
      const reason = this.reasons.find((r) => r.text === this.reasonText)
      this.submitting = true
      try {
        await submitAppeal({
          orderId: this.result.orderId,
          reasonType: reason.type,
          description: this.appealDesc.trim(),
          images: this.appealImages
        })
        this.appealSubmitted = true
        uni.showToast({ title: '申诉已提交', icon: 'success' })
      } finally {
        this.submitting = false
      }
    },

    /** 时间格式化 */
    fmt(t) {
      if (!t) return ''
      const d = new Date(String(t).replace(/-/g, '/'))
      const p = (n) => String(n).padStart(2, '0')
      return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f5f6fa;
  padding: 30rpx;
}

/* ---------- 扫码卡片 ---------- */
.scan-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 60rpx 40rpx;
  text-align: center;
}
.scan-icon { font-size: 100rpx; }
.scan-title {
  font-size: 40rpx;
  font-weight: 600;
  margin: 20rpx 0 16rpx;
  color: #1f2329;
}
.scan-desc {
  font-size: 26rpx;
  color: #86909c;
  line-height: 1.7;
  text-align: left;
  margin-bottom: 30rpx;
}
.tip-card {
  background: #f7f8fa;
  border-radius: 16rpx;
  padding: 24rpx;
  text-align: left;
  margin-bottom: 40rpx;
}
.tip-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #4e5969;
  margin-bottom: 12rpx;
}
.tip-item {
  font-size: 24rpx;
  color: #86909c;
  line-height: 1.9;
}
.loc-error {
  margin-top: 20rpx;
  font-size: 24rpx;
  color: #e54d42;
}

/* ---------- 结果卡片（成功/失败共用） ---------- */
.result-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 60rpx 40rpx;
  text-align: center;
}
.result-icon {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  margin: 0 auto;
  font-size: 64rpx;
  font-weight: 700;
  line-height: 120rpx;
  animation: pop 0.4s ease-out;
}
.result-icon.success {
  background: #e8f7e8;
  color: #00b578;
}
.result-icon.fail {
  background: #fdeceb;
  color: #e54d42;
}
@keyframes pop {
  0% { transform: scale(0.3); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}
.result-title {
  font-size: 38rpx;
  font-weight: 600;
  color: #1f2329;
  margin: 28rpx 0 12rpx;
}
.late-badge {
  display: inline-block;
  background: #fff7e8;
  color: #ff976a;
  font-size: 24rpx;
  border-radius: 8rpx;
  padding: 6rpx 16rpx;
  margin-bottom: 12rpx;
}
.fail-reason {
  font-size: 28rpx;
  color: #e54d42;
  background: #fdeceb;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  line-height: 1.6;
  margin-bottom: 30rpx;
}

/* ---------- 信息卡片 ---------- */
.info-card {
  background: #f7f8fa;
  border-radius: 16rpx;
  padding: 10rpx 24rpx;
  margin-bottom: 36rpx;
  text-align: left;
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

/* ---------- 申诉表单 ---------- */
.appeal-form { text-align: left; }
.form-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 8rpx;
}
.form-desc {
  font-size: 24rpx;
  color: #86909c;
  margin-bottom: 28rpx;
}
.field-label {
  font-size: 26rpx;
  color: #4e5969;
  margin: 24rpx 0 12rpx;
}
.picker-value {
  background: #f7f8fa;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  font-size: 28rpx;
  color: #1f2329;
}
.appeal-textarea {
  width: 100%;
  box-sizing: border-box;
  background: #f7f8fa;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  font-size: 28rpx;
  min-height: 160rpx;
}
.img-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 32rpx;
}
.img-item {
  position: relative;
  width: 180rpx;
  height: 180rpx;
  border-radius: 12rpx;
  overflow: hidden;
}
.img-item image { width: 100%; height: 100%; }
.img-del {
  position: absolute;
  top: 0;
  right: 0;
  width: 44rpx;
  height: 44rpx;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  text-align: center;
  line-height: 44rpx;
  font-size: 28rpx;
  border-bottom-left-radius: 12rpx;
}
.img-add {
  width: 180rpx;
  height: 180rpx;
  border: 2rpx dashed #c9cdd4;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 56rpx;
  color: #c9cdd4;
}

/* ---------- 申诉已提交 ---------- */
.appeal-done { text-align: center; }
.done-icon { font-size: 90rpx; }
.done-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #1f2329;
  margin: 20rpx 0 10rpx;
}
.done-desc {
  font-size: 26rpx;
  color: #86909c;
  line-height: 1.6;
  margin-bottom: 36rpx;
}

/* ---------- 按钮 ---------- */
.btn-primary {
  background: #4a7cf7;
  color: #fff;
  font-size: 30rpx;
  border-radius: 44rpx;
  margin-top: 10rpx;
}
.btn-plain {
  background: #fff;
  color: #4a7cf7;
  font-size: 30rpx;
  border-radius: 44rpx;
  border: 2rpx solid #4a7cf7;
  margin-top: 20rpx;
}
</style>
