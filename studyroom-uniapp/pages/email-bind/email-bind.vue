<template>
  <!-- FR-UC-004 邮箱绑定：SMTP 验证码 + AES-256-CBC 加密存储 -->
  <view class="page">
    <view v-if="bound" class="card bound">
      <view class="bound-icon">✅</view>
      <view class="bound-text">已绑定邮箱</view>
      <view class="bound-email">{{ emailMasked }}</view>
      <view class="bound-tip">关键通知（预约成功 / 退款到账）将发送到该邮箱</view>
    </view>

    <view v-else class="card">
      <view class="form-title">绑定邮箱</view>
      <view class="form-tip">绑定后可通过邮件接收预约成功、退款到账等关键通知</view>

      <view class="form-item">
        <input
          class="input"
          type="text"
          v-model="email"
          placeholder="请输入邮箱地址"
          :maxlength="64"
        />
      </view>

      <view class="form-item code-item">
        <input
          class="input code-input"
          type="number"
          v-model="code"
          placeholder="6 位验证码"
          :maxlength="6"
        />
        <button
          class="code-btn"
          :disabled="countdown > 0 || sending"
          @click="onSendCode"
        >
          {{ countdown > 0 ? `${countdown}s 后重发` : (sending ? '发送中...' : '获取验证码') }}
        </button>
      </view>

      <button class="bind-btn" :loading="binding" :disabled="binding" @click="onBind">
        确认绑定
      </button>
    </view>
  </view>
</template>

<script setup>
/**
 * 邮箱绑定页 —— FR-UC-004
 * 两步流程：
 * 1. POST /user/email/code  发送验证码（60s 冷却 + 每日 5 次，后端控制）
 * 2. POST /user/email/bind  验证码校验 → AES-256-CBC 加密落库
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

const userStore = useUserStore()

const bound = ref(false)
const emailMasked = ref('')

const email = ref('')
const code = ref('')
const sending = ref(false)
const binding = ref(false)
const countdown = ref(0)

let timer = null

onMounted(async () => {
  try {
    const status = await request.get('/user/email')
    bound.value = status.bound
    emailMasked.value = status.emailMasked || ''
  } catch (e) { /* 请求层已提示 */ }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

/** 发送验证码（FR-UC-004 第一步） */
async function onSendCode() {
  if (!email.value || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
    uni.showToast({ title: '请输入正确的邮箱地址', icon: 'none' })
    return
  }
  sending.value = true
  try {
    await request.post('/user/email/code', { email: email.value })
    startCountdown(60)
    uni.showToast({ title: '验证码已发送', icon: 'success' })
  } catch (e) {
    /* 请求层已提示 */
  } finally {
    sending.value = false
  }
}

/** 绑定邮箱（FR-UC-004 第二步） */
async function onBind() {
  if (!/^\d{6}$/.test(code.value)) {
    uni.showToast({ title: '请输入 6 位验证码', icon: 'none' })
    return
  }
  binding.value = true
  try {
    await request.post('/user/email/bind', { email: email.value, code: code.value })
    // 刷新状态
    const status = await request.get('/user/email')
    bound.value = status.bound
    emailMasked.value = status.emailMasked || ''
    await userStore.fetchUserInfo()
    uni.showToast({ title: '绑定成功', icon: 'success' })
  } catch (e) {
    /* 请求层已提示 */
  } finally {
    binding.value = false
  }
}

function startCountdown(seconds) {
  countdown.value = seconds
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { background: #fff; border-radius: 24rpx; padding: 40rpx 32rpx; }
.bound { text-align: center; padding: 80rpx 32rpx; }
.bound-icon { font-size: 80rpx; }
.bound-text { margin-top: 16rpx; font-size: 34rpx; font-weight: bold; }
.bound-email { margin-top: 12rpx; color: #4a7cf7; }
.bound-tip { margin-top: 24rpx; color: #909399; font-size: 24rpx; }
.form-title { font-size: 34rpx; font-weight: bold; }
.form-tip { margin-top: 8rpx; color: #909399; font-size: 24rpx; margin-bottom: 32rpx; }
.form-item {
  display: flex; align-items: center;
  border: 1rpx solid #dcdfe6; border-radius: 16rpx;
  padding: 0 24rpx; margin-bottom: 24rpx;
}
.input { height: 88rpx; flex: 1; }
.code-item { padding-right: 8rpx; }
.code-btn {
  font-size: 26rpx; color: #4a7cf7; background: transparent;
  line-height: normal; padding: 0; margin: 0; white-space: nowrap;
}
.code-btn::after { border: none; }
.code-btn[disabled] { color: #909399; background: transparent; }
.bind-btn {
  margin-top: 24rpx; background: #4a7cf7; color: #fff;
  border-radius: 999rpx; font-size: 32rpx;
}
.bind-btn[disabled] { opacity: .6; }
</style>
