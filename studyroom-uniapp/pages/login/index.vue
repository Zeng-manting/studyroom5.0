<template>
  <!-- H5 登录页（小程序端走静默登录，无需进入本页） -->
  <view class="page">
    <view class="brand">
      <view class="logo">📚</view>
      <view class="brand-title">无人自习室</view>
      <view class="brand-sub">输入账号即可开始体验</view>
    </view>

    <view class="card">
      <view class="field-label">登录账号</view>
      <input
        class="account-input"
        v-model="account"
        placeholder="2-32 位字母 / 数字 / 中文"
        :maxlength="32"
        confirm-type="done"
        @confirm="onLogin"
      />
      <view class="tips">首次登录自动注册，同一账号数据互通。</view>
      <button class="btn-primary" :loading="loading" :disabled="loading" @click="onLogin">
        {{ loading ? '登录中…' : '登录' }}
      </button>
    </view>
  </view>
</template>

<script>
/**
 * H5 端账号登录页
 *
 * H5 环境无法使用微信 code2Session，改为账号标识用户：
 * 提交账号 → POST /user/h5-login → 后端建档/复用并签发 JWT（7 天）
 */
import { loginByAccount } from '@/utils/auth'

export default {
  data() {
    return {
      account: '',
      loading: false
    }
  },
  methods: {
    async onLogin() {
      const account = (this.account || '').trim()
      if (account.length < 2) {
        uni.showToast({ title: '账号至少 2 个字符', icon: 'none' })
        return
      }
      this.loading = true
      try {
        await loginByAccount(account)
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 500)
      } catch (e) {
        uni.showToast({ title: e.message || '登录失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 120rpx 48rpx 0;
  box-sizing: border-box;
  background: linear-gradient(180deg, #4a7cf7 0%, #f5f6fa 42%);
}

.brand {
  text-align: center;
  margin-bottom: 64rpx;
}

.logo {
  font-size: 110rpx;
  line-height: 1;
}

.brand-title {
  margin-top: 20rpx;
  font-size: 48rpx;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 2rpx;
}

.brand-sub {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
}

.card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 48rpx 40rpx;
  box-shadow: 0 12rpx 40rpx rgba(29, 61, 148, 0.12);
}

.field-label {
  font-size: 26rpx;
  color: #606266;
  margin-bottom: 16rpx;
}

.account-input {
  height: 92rpx;
  padding: 0 24rpx;
  background: #f5f6fa;
  border-radius: 16rpx;
  font-size: 30rpx;
  color: #303133;
}

.tips {
  margin-top: 20rpx;
  font-size: 24rpx;
  color: #909399;
}

.btn-primary {
  margin-top: 48rpx;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 46rpx;
  background: #4a7cf7;
  color: #ffffff;
  font-size: 32rpx;
  border: none;
}

.btn-primary::after {
  border: none;
}
</style>
