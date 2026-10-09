<template>
  <!-- FR-UC-043 公告详情：富文本 + 附件下载；首次访问自动落已读 -->
  <view class="page">
    <view v-if="!notice" class="loading">加载中...</view>
    <template v-else>
      <view class="card">
        <view class="notice-title-row">
          <text v-if="notice.important" class="important-tag">重要</text>
          <text class="title">{{ notice.title }}</text>
        </view>
        <view class="meta">
          {{ typeLabel }} · {{ fmt(notice.publishTime) }} · 阅读 {{ notice.readCount }}
        </view>

        <!-- 富文本正文（管理端发布） -->
        <view class="content">
          <rich-text :nodes="notice.content" />
        </view>
      </view>

      <!-- 附件 -->
      <view v-if="notice.attachments && notice.attachments.length" class="card">
        <view class="card-title">附件（{{ notice.attachments.length }}）</view>
        <view v-for="(a, i) in notice.attachments" :key="i" class="attach-item" @click="download(a)">
          <text class="attach-icon">📎</text>
          <text class="attach-name">{{ a.fileName }}</text>
          <text class="attach-arrow">下载</text>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup>
/**
 * 公告详情页 —— FR-UC-043（模块7）
 * 首次访问后端自动落已读（notice_read）并自增 read_count
 */
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import dayjs from 'dayjs'
import { getNoticeDetail } from '@/api/notice'

const notice = ref(null)
const TYPE_LABELS = { 1: '制度', 2: '停业公告', 3: '活动', 4: '其他' }

const fmt = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '')

function download(a) {
  uni.downloadFile({
    url: a.fileUrl,
    success: (res) => {
      uni.openDocument({
        filePath: res.tempFilePath,
        showMenu: true,
        fail: () => uni.showToast({ title: '该文件类型暂不支持预览', icon: 'none' })
      })
    },
    fail: () => uni.showToast({ title: '下载失败', icon: 'none' })
  })
}

onLoad(async (query) => {
  notice.value = await getNoticeDetail(query.id)
})
</script>

<style scoped>
.page { padding: 24rpx; }
.card { background: #fff; border-radius: 24rpx; padding: 32rpx; margin-bottom: 24rpx; }
.card-title { font-size: 30rpx; font-weight: 600; color: #1f2329; margin-bottom: 16rpx; }

.notice-title-row { display: flex; align-items: center; gap: 12rpx; }
.important-tag {
  flex-shrink: 0; background: #fdeaea; color: #e74c3c;
  font-size: 20rpx; padding: 4rpx 12rpx; border-radius: 8rpx;
}
.title { font-size: 36rpx; font-weight: 700; color: #1f2329; }
.meta { margin-top: 16rpx; font-size: 24rpx; color: #909399; padding-bottom: 20rpx; border-bottom: 1rpx solid #f0f0f0; }

.content { margin-top: 24rpx; font-size: 28rpx; color: #303133; line-height: 1.8; }

.attach-item {
  display: flex; align-items: center; gap: 16rpx;
  padding: 20rpx 0; border-bottom: 1rpx solid #f0f0f0;
}
.attach-item:last-child { border-bottom: none; }
.attach-icon { font-size: 32rpx; }
.attach-name { flex: 1; font-size: 26rpx; color: #303133; word-break: break-all; }
.attach-arrow { font-size: 24rpx; color: #4a7cf7; }

.loading { text-align: center; color: #b0b3ba; padding: 160rpx 0; }
</style>
