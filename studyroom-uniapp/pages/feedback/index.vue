<template>
  <!-- FR-UC-052 意见反馈：4 类型 + 内容 5-1000 字 + 图片 -->
  <view class="page">
    <view class="card">
      <!-- 反馈类型 -->
      <view class="type-block">
        <view v-for="t in types" :key="t.value" class="type-item"
              :class="{ active: form.type === t.value }" @click="form.type = t.value">
          {{ t.label }}
        </view>
      </view>

      <!-- 反馈内容 5-1000 字 -->
      <textarea v-model="form.content" class="content-input"
                placeholder="请详细描述您的问题或建议（至少 5 个字）" :maxlength="1000" />
      <view class="word-count">{{ form.content.length }}/1000</view>

      <!-- 截图（选填，≤6 张） -->
      <view class="img-block">
        <view v-for="(img, i) in form.images" :key="i" class="img-item">
          <image :src="img" mode="aspectFill" class="img" @click="previewImg(i)" />
          <view class="img-del" @click="form.images.splice(i, 1)">×</view>
        </view>
        <view v-if="form.images.length < 6" class="img-add" @click="chooseImg">＋</view>
      </view>
    </view>

    <button class="submit-btn" :loading="submitting" :disabled="submitting" @click="onSubmit">提交反馈</button>
    <view class="contact-tip">提交后可在消息中心收到处理结果通知</view>
  </view>
</template>

<script setup>
/**
 * 意见反馈页 —— FR-UC-052（模块7）
 * 类型：1功能建议 2问题反馈 3投诉 4其他；提交走幂等 token（FR-SY-001）
 */
import { reactive, ref } from 'vue'
import { submitFeedback } from '@/api/profile'
import request from '@/utils/request'

const types = [
  { value: 1, label: '功能建议' },
  { value: 2, label: '问题反馈' },
  { value: 3, label: '投诉' },
  { value: 4, label: '其他' }
]

const form = reactive({ type: 1, content: '', images: [] })
const submitting = ref(false)

function chooseImg() {
  uni.chooseImage({
    count: 6 - form.images.length,
    sizeType: ['compressed'],
    success: async (res) => {
      uni.showLoading({ title: '上传中' })
      try {
        for (const p of res.tempFilePaths) {
          const url = await request.upload(p, 'feedback')
          form.images.push(url)
        }
      } finally {
        uni.hideLoading()
      }
    }
  })
}

function previewImg(i) {
  uni.previewImage({ urls: form.images, current: i })
}

async function onSubmit() {
  if (form.content.trim().length < 5) {
    uni.showToast({ title: '反馈内容至少 5 个字', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await submitFeedback({ type: form.type, content: form.content.trim(), images: form.images })
    uni.showToast({ title: '提交成功，感谢反馈', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 800)
  } catch (e) { /* 请求层已提示 */ } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 60rpx; }
.card { background: #fff; border-radius: 24rpx; padding: 32rpx; }

.type-block { display: flex; gap: 16rpx; flex-wrap: wrap; }
.type-item {
  padding: 12rpx 32rpx; border-radius: 999rpx; font-size: 26rpx;
  background: #f5f6fa; color: #606266; border: 1rpx solid transparent;
}
.type-item.active { background: #eef3fe; color: #4a7cf7; border-color: #4a7cf7; }

.content-input {
  width: 100%; box-sizing: border-box; margin-top: 32rpx;
  background: #f5f6fa; border-radius: 16rpx; padding: 24rpx;
  font-size: 28rpx; min-height: 240rpx;
}
.word-count { text-align: right; font-size: 22rpx; color: #b0b3ba; margin-top: 8rpx; }

.img-block { display: flex; flex-wrap: wrap; gap: 16rpx; margin-top: 24rpx; }
.img-item { position: relative; }
.img { width: 160rpx; height: 160rpx; border-radius: 12rpx; }
.img-del {
  position: absolute; top: -12rpx; right: -12rpx;
  width: 40rpx; height: 40rpx; border-radius: 50%;
  background: rgba(0,0,0,.6); color: #fff; text-align: center;
  line-height: 38rpx; font-size: 28rpx;
}
.img-add {
  width: 160rpx; height: 160rpx; border-radius: 12rpx;
  border: 2rpx dashed #dcdfe6; display: flex; align-items: center; justify-content: center;
  font-size: 52rpx; color: #b0b3ba;
}

.submit-btn {
  margin-top: 40rpx; background: #4a7cf7; color: #fff;
  border-radius: 999rpx; font-size: 32rpx;
}
.submit-btn[disabled] { opacity: .6; }
.contact-tip { margin-top: 20rpx; text-align: center; font-size: 22rpx; color: #b0b3ba; }
</style>
