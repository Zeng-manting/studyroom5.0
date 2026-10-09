<template>
  <!-- FR-UC-039 提交评价：5星 + 预设标签 + 文字 10-500 字 + 图片 ≤6 张 + 匿名开关 -->
  <view class="page">
    <view class="card">
      <view class="store">{{ storeName }}</view>

      <!-- 评分 -->
      <view class="score-block">
        <view class="stars">
          <text v-for="i in 5" :key="i" class="star" :class="{ active: i <= score }"
                @click="score = i">★</text>
        </view>
        <text class="score-text">{{ scoreText }}</text>
      </view>

      <!-- 预设标签（可多选，≤10 个） -->
      <view class="tag-block">
        <view v-for="t in presetTags" :key="t" class="tag" :class="{ active: tags.includes(t) }"
              @click="toggleTag(t)">{{ t }}</view>
      </view>

      <!-- 文字评价 10-500 字 -->
      <textarea v-model="content" class="content-input" placeholder="说说本次学习体验（至少 10 个字）"
                :maxlength="500" />
      <view class="word-count">{{ content.length }}/500</view>

      <!-- 图片 ≤6 张 -->
      <view class="img-block">
        <view v-for="(img, i) in images" :key="i" class="img-item">
          <image :src="img" mode="aspectFill" class="img" @click="previewImg(i)" />
          <view class="img-del" @click="removeImg(i)">×</view>
        </view>
        <view v-if="images.length < 6" class="img-add" @click="chooseImg">＋</view>
      </view>

      <!-- 匿名开关 -->
      <view class="anon-row">
        <text>匿名评价</text>
        <switch :checked="anonymous === 1" color="#4a7cf7" @change="onAnonChange" />
      </view>
    </view>

    <button class="submit-btn" :loading="submitting" :disabled="submitting || score === 0"
            @click="onSubmit">提交评价</button>
  </view>
</template>

<script setup>
/**
 * 评价创建页 —— FR-UC-039（模块7）
 * 一单一评（后端 uk_order 兜底）；提交走幂等 token（FR-SY-001）
 */
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { submitReview } from '@/api/review'
import request from '@/utils/request'

const presetTags = ['环境安静', '座位舒适', '灯光适宜', '设施齐全', '卫生干净', '空调给力',
  '网络流畅', '性价比高', '停车方便', '会再来']

const orderId = ref(null)
const storeName = ref('')
const score = ref(0)
const tags = ref([])
const content = ref('')
const images = ref([])
const anonymous = ref(0)
const submitting = ref(false)

const SCORE_TEXTS = ['', '很差', '较差', '一般', '满意', '超赞']
const scoreText = computed(() => SCORE_TEXTS[score.value] || '点击星星评分')

function toggleTag(t) {
  const i = tags.value.indexOf(t)
  if (i >= 0) tags.value.splice(i, 1)
  else if (tags.value.length < 10) tags.value.push(t)
  else uni.showToast({ title: '最多选择 10 个标签', icon: 'none' })
}

function onAnonChange(e) {
  anonymous.value = e.detail.value ? 1 : 0
}

/** 选图上传（FR-UC-039 图片 ≤6 张） */
function chooseImg() {
  uni.chooseImage({
    count: 6 - images.value.length,
    sizeType: ['compressed'],
    success: async (res) => {
      uni.showLoading({ title: '上传中' })
      try {
        for (const p of res.tempFilePaths) {
          const url = await request.upload(p, 'review')
          images.value.push(url)
        }
      } finally {
        uni.hideLoading()
      }
    }
  })
}

function removeImg(i) { images.value.splice(i, 1) }

function previewImg(i) {
  uni.previewImage({ urls: images.value, current: i })
}

async function onSubmit() {
  if (score.value === 0) {
    uni.showToast({ title: '请先评分', icon: 'none' })
    return
  }
  if (content.value.trim().length < 10) {
    uni.showToast({ title: '评价内容至少 10 个字', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await submitReview({
      orderId: orderId.value,
      score: score.value,
      tags: tags.value,
      content: content.value.trim(),
      images: images.value,
      anonymous: anonymous.value
    })
    uni.showToast({ title: '评价成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 800)
  } catch (e) { /* 请求层已提示 */ } finally {
    submitting.value = false
  }
}

onLoad((query) => {
  orderId.value = Number(query.orderId)
  storeName.value = decodeURIComponent(query.storeName || '')
})
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 60rpx; }
.card { background: #fff; border-radius: 24rpx; padding: 32rpx; }

.store { font-size: 32rpx; font-weight: 600; color: #1f2329; text-align: center; }

.score-block { display: flex; flex-direction: column; align-items: center; margin-top: 32rpx; }
.stars { display: flex; gap: 16rpx; }
.star { font-size: 64rpx; color: #dcdfe6; }
.star.active { color: #f7b500; }
.score-text { margin-top: 12rpx; font-size: 26rpx; color: #86909c; }

.tag-block {
  display: flex; flex-wrap: wrap; gap: 16rpx;
  margin-top: 40rpx;
}
.tag {
  padding: 10rpx 28rpx; border-radius: 999rpx; font-size: 24rpx;
  background: #f5f6fa; color: #606266; border: 1rpx solid transparent;
}
.tag.active { background: #eef3fe; color: #4a7cf7; border-color: #4a7cf7; }

.content-input {
  width: 100%; box-sizing: border-box; margin-top: 32rpx;
  background: #f5f6fa; border-radius: 16rpx; padding: 24rpx;
  font-size: 28rpx; min-height: 200rpx;
}
.word-count { text-align: right; font-size: 22rpx; color: #b0b3ba; margin-top: 8rpx; }

.img-block { display: flex; flex-wrap: wrap; gap: 16rpx; margin-top: 24rpx; }
.img-item { position: relative; }
.img { width: 180rpx; height: 180rpx; border-radius: 12rpx; }
.img-del {
  position: absolute; top: -12rpx; right: -12rpx;
  width: 40rpx; height: 40rpx; border-radius: 50%;
  background: rgba(0,0,0,.6); color: #fff; text-align: center;
  line-height: 38rpx; font-size: 28rpx;
}
.img-add {
  width: 180rpx; height: 180rpx; border-radius: 12rpx;
  border: 2rpx dashed #dcdfe6; display: flex; align-items: center; justify-content: center;
  font-size: 56rpx; color: #b0b3ba;
}

.anon-row {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 32rpx; padding-top: 24rpx; border-top: 1rpx solid #f0f0f0;
  font-size: 28rpx; color: #303133;
}

.submit-btn {
  margin-top: 40rpx; background: #4a7cf7; color: #fff;
  border-radius: 999rpx; font-size: 32rpx;
}
.submit-btn[disabled] { opacity: .5; }
</style>
