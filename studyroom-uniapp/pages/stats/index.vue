<template>
  <!-- FR-UC-049 学习统计：累计数据 + 24 小时时段分布柱状图 + 近 90 天热力图 -->
  <view class="page">
    <!-- 累计概览 -->
    <view class="stat-grid">
      <view class="stat-item">
        <view class="stat-num">{{ hoursText }}</view>
        <view class="stat-label">累计学习（小时）</view>
      </view>
      <view class="stat-item">
        <view class="stat-num">{{ stat.totalDays || 0 }}</view>
        <view class="stat-label">学习天数</view>
      </view>
      <view class="stat-item">
        <view class="stat-num">{{ stat.totalOrders || 0 }}</view>
        <view class="stat-label">累计订单</view>
      </view>
      <view class="stat-item">
        <view class="stat-num">¥{{ stat.totalConsume || 0 }}</view>
        <view class="stat-label">累计消费</view>
      </view>
    </view>

    <!-- 24 小时时段分布（后端返回 24 元素定长数组，下标=小时） -->
    <view class="card">
      <view class="card-title">常用学习时段</view>
      <view class="chart">
        <view class="bars">
          <view v-for="(v, h) in stat.hourDistribution || []" :key="h" class="bar-col">
            <view class="bar" :style="{ height: barHeight(v) + 'rpx' }"></view>
          </view>
        </view>
        <view class="axis">
          <text v-for="h in [0, 6, 12, 18, 23]" :key="h" class="axis-label">{{ h }}时</text>
        </view>
      </view>
    </view>

    <!-- 近 90 天热力图（后端按日零填充，minutes=0 表示未打卡） -->
    <view class="card">
      <view class="card-title">近 90 天打卡</view>
      <view class="heatmap">
        <view v-for="d in stat.heatmap || []" :key="d.date" class="heat-cell"
              :class="heatClass(d.minutes)" @click="onHeatClick(d)"></view>
      </view>
      <view class="heat-legend">
        <text class="legend-label">少</text>
        <view class="heat-cell lv0"></view>
        <view class="heat-cell lv1"></view>
        <view class="heat-cell lv2"></view>
        <view class="heat-cell lv3"></view>
        <text class="legend-label">多</text>
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 学习统计页 —— FR-UC-049（模块7）
 * hourDistribution：24 元素定长数组（无数据小时补 0），柱高按最大值归一化
 * heatmap：近 90 天 DayHeat{date, minutes}，零填充，四级热力着色
 */
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getStudyStats } from '@/api/profile'

const stat = ref({})

const hoursText = computed(() => {
  const min = stat.value.totalMinutes || 0
  return (min / 60).toFixed(1)
})

/** 柱高：按 24 小时中最大计数归一化（最大 200rpx） */
function barHeight(v) {
  const max = Math.max(...(stat.value.hourDistribution || [0]), 1)
  return Math.max(((v || 0) / max) * 200, v > 0 ? 8 : 4)
}

/** 热力分级：0 / <60 / <180 / >=180 分钟 */
function heatClass(minutes) {
  const m = minutes || 0
  if (m <= 0) return 'lv0'
  if (m < 60) return 'lv1'
  if (m < 180) return 'lv2'
  return 'lv3'
}

function onHeatClick(d) {
  if (!d.minutes) return
  uni.showToast({ title: `${d.date} 学习 ${(d.minutes / 60).toFixed(1)} 小时`, icon: 'none' })
}

onShow(async () => {
  try {
    stat.value = await getStudyStats()
  } catch (e) { /* 请求层已提示 */ }
})
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 60rpx; }

.stat-grid {
  display: flex; background: #fff; border-radius: 24rpx;
  padding: 32rpx 0; margin-bottom: 24rpx;
}
.stat-item { flex: 1; text-align: center; }
.stat-num { font-size: 36rpx; font-weight: 700; color: #1f2329; }
.stat-label { margin-top: 8rpx; font-size: 22rpx; color: #86909c; }

.card { background: #fff; border-radius: 24rpx; padding: 28rpx; margin-bottom: 24rpx; }
.card-title { font-size: 30rpx; font-weight: 600; color: #1f2329; margin-bottom: 24rpx; }

/* 24 小时柱状图 */
.chart { padding: 0 8rpx; }
.bars {
  display: flex; align-items: flex-end; gap: 4rpx;
  height: 220rpx;
}
.bar-col { flex: 1; display: flex; align-items: flex-end; justify-content: center; height: 100%; }
.bar {
  width: 100%; border-radius: 6rpx 6rpx 0 0;
  background: linear-gradient(180deg, #7ea4f9, #4a7cf7);
  min-height: 4rpx;
}
.axis {
  display: flex; justify-content: space-between; margin-top: 8rpx;
}
.axis-label { font-size: 20rpx; color: #b0b3ba; }

/* 热力图：7 列 × ~13 行 */
.heatmap {
  display: flex; flex-wrap: wrap; gap: 8rpx;
}
.heat-cell {
  width: 36rpx; height: 36rpx; border-radius: 8rpx;
}
.lv0 { background: #f0f1f5; }
.lv1 { background: #c3d5fc; }
.lv2 { background: #7ea4f9; }
.lv3 { background: #4a7cf7; }

.heat-legend {
  display: flex; align-items: center; gap: 10rpx;
  justify-content: flex-end; margin-top: 20rpx;
}
.legend-label { font-size: 20rpx; color: #b0b3ba; }
.heat-legend .heat-cell { width: 24rpx; height: 24rpx; }
</style>
