<template>
  <!-- FR-UC-050 首次进入隐私协议弹窗：同意后才可继续使用 -->
  <view v-if="visible" class="mask">
    <view class="popup">
      <view class="popup-title">用户隐私协议</view>
      <scroll-view scroll-y class="popup-body">
        <view class="clause">
          欢迎使用无人自习室小程序。为了向您提供预约、签到、消息通知等服务，我们需要收集和使用以下信息：
        </view>
        <view class="clause">1. 微信昵称与头像：用于展示您的个人资料；</view>
        <view class="clause">2. 预约与订单信息：用于座位预约、签到校验与结算；</view>
        <view class="clause">3. 位置信息：仅在您主动扫码签到时用于到店距离校验；</view>
        <view class="clause">4. 邮箱（选填）：用于订单与退款通知兜底提醒。</view>
        <view class="clause">
          上述信息均加密存储，不会向任何第三方提供。您可以随时在「设置-隐私政策」中查看详情。
        </view>
      </scroll-view>
      <view class="popup-btns">
        <button class="btn-reject" @click="onReject">不同意</button>
        <button class="btn-agree" @click="onAgree">同意并继续</button>
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 隐私协议弹窗 —— FR-UC-050（模块7）
 * 由父组件传入 v-model:visible；点击同意调用 POST /profile/privacy-agree（幂等）
 */
import { defineProps, defineEmits } from 'vue'
import { agreePrivacy } from '@/api/profile'
import { useUserStore } from '@/store/user'

const props = defineProps({
  visible: { type: Boolean, default: false }
})
const emit = defineEmits(['update:visible'])

const userStore = useUserStore()

/** 同意：幂等更新 user.privacy_agreed 并关闭弹窗 */
async function onAgree() {
  try {
    await agreePrivacy()
  } catch (e) { /* 失败不阻断（下次仍会提示） */ }
  if (userStore.userInfo) userStore.userInfo.privacyAgreed = true
  emit('update:visible', false)
}

/** 拒绝：提示后退出（小程序场景引导返回） */
function onReject() {
  uni.showModal({
    title: '提示',
    content: '需同意隐私协议后才能使用小程序服务',
    showCancel: false,
    confirmText: '重新查看',
    success: () => { /* 弹窗保持，用户可再次选择同意 */ }
  })
}
</script>

<style scoped>
.mask {
  position: fixed; inset: 0; background: rgba(0,0,0,.55); z-index: 999;
  display: flex; align-items: center; justify-content: center;
}
.popup {
  width: 620rpx; background: #fff; border-radius: 24rpx; padding: 40rpx 36rpx;
  display: flex; flex-direction: column;
}
.popup-title { font-size: 34rpx; font-weight: 700; color: #1f2329; text-align: center; }
.popup-body { max-height: 480rpx; margin-top: 24rpx; }
.clause { font-size: 26rpx; color: #606266; line-height: 1.8; margin-bottom: 8rpx; }
.popup-btns { display: flex; gap: 24rpx; margin-top: 32rpx; }
.btn-reject {
  flex: 1; background: #f5f6fa; color: #606266;
  border-radius: 999rpx; font-size: 28rpx;
}
.btn-agree {
  flex: 1; background: #4a7cf7; color: #fff;
  border-radius: 999rpx; font-size: 28rpx;
}
</style>
