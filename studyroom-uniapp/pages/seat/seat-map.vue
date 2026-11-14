<template>
  <!-- FR-UC-017 座位选择：HTML 网格渲染（H5/小程序通用，支持鼠标与触摸） -->
  <view class="page">
    <!-- 预约信息卡片 -->
    <view class="info-card">
      <view class="info-row">
        <view class="info-left">
          <text class="info-label">{{ customEnd ? '预约时段' : '开始时间' }}</text>
          <text class="info-time">{{ startTime }}</text>
          <text v-if="customEnd" class="info-end">至 {{ customEndLabel }}</text>
        </view>
        <view class="info-pkg" v-if="packageInfo">
          <text class="info-pkg-name">{{ packageInfo.name }}</text>
        </view>
      </view>
      <view class="legend">
        <view class="lg"><view class="dot idle" />空闲</view>
        <view class="lg"><view class="dot busy" />占用</view>
        <view class="lg"><view class="dot using" />使用中</view>
        <view class="lg"><view class="dot maintain" />维护</view>
        <view class="lg"><view class="dot clean" />打扫</view>
        <view class="lg"><view class="dot locked" />锁定</view>
      </view>
    </view>

    <!-- 教室前方标识 -->
    <view class="room-front">
      <view class="front-bar"></view>
      <text class="front-text">讲台 / 入口</text>
    </view>

    <!-- 座位网格 -->
    <view class="seat-card">
      <view v-if="seatMap" class="seat-grid">
        <view
          v-for="seat in seatMap.seats"
          :key="seat.id"
          class="seat"
          :class="['st-' + (seat.displayStatus ?? 0), { active: selectedSeat && selectedSeat.id === seat.id }]"
          @click="onPickSeat(seat)"
        >
          <view class="seat-back"></view>
          <text class="seat-no">{{ seat.seatNo }}</text>
        </view>
      </view>
      <view v-else class="loading">座位图加载中...</view>
      <view v-if="seatMap && seatMap.seats.length === 0" class="loading">该门店暂未布置座位</view>
    </view>

    <!-- 底部操作栏 -->
    <view class="footer-bar">
      <view class="footer-info">
        <template v-if="selectedSeat">
          <text class="footer-seat">{{ selectedSeat.seatNo }}</text>
          <text class="footer-type">{{ seatTypeName(selectedSeat.seatTypeId) }}</text>
        </template>
        <text v-else class="footer-hint">点击绿色座位进行选择</text>
      </view>
      <button
        class="footer-btn"
        :disabled="!selectedSeat"
        :class="{ disabled: !selectedSeat }"
        @click="goConfirm"
      >{{ selectedSeat ? '去下单' : '请选择座位' }}</button>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { getSeatMap, getPackageList } from '@/api/store'

const storeId = ref(null)
const startTime = ref('')
const packageId = ref(null)
const packageInfo = ref(null)
/** 自定义时段的结束时间（不买套餐时由门店页带入） */
const customEnd = ref('')

const seatMap = ref(null)
const selectedSeat = ref(null)

const customEndLabel = computed(() =>
  customEnd.value ? dayjs(customEnd.value, 'YYYY-MM-DD HH:mm').format('MM-DD HH:mm') : '')

function seatTypeName(typeId) {
  const t = (seatMap.value && seatMap.value.seatTypes || []).find(x => x.id === typeId)
  return t ? t.name : ''
}

/** 计算结束时间（座位图查询窗口）：自定义时段直接取用户选择，否则按套餐时长推算 */
function calcEndTime() {
  const start = dayjs(startTime.value, 'YYYY-MM-DD HH:mm')
  if (customEnd.value) return dayjs(customEnd.value, 'YYYY-MM-DD HH:mm')
  const p = packageInfo.value
  if (!p) return start.add(2, 'hour')
  switch (p.unit) {
    case 'hour': return start.add(p.durationValue, 'hour')
    case 'day': return start.add(p.durationValue, 'day')
    case 'week': return start.add(p.durationValue * 7, 'day')
    case 'month': return start.add(p.durationValue, 'month')
    default: return start.add(p.durationValue, 'hour')
  }
}

/** 选座：仅空闲可选；再次点击已选座位取消 */
function onPickSeat(seat) {
  if (!seat.selectable) {
    uni.showToast({ title: `该座位${seat.statusDesc || '不可选'}，请选择其他座位`, icon: 'none' })
    return
  }
  selectedSeat.value = selectedSeat.value && selectedSeat.value.id === seat.id ? null : seat
}

function goConfirm() {
  if (!selectedSeat.value) return
  let url = `/pages/order/confirm?storeId=${storeId.value}` +
    `&seatId=${selectedSeat.value.id}` +
    `&packageId=${packageId.value}` +
    `&startTime=${encodeURIComponent(startTime.value)}`
  if (customEnd.value) {
    url += `&endTime=${encodeURIComponent(customEnd.value)}`
  }
  uni.navigateTo({ url })
}

onMounted(async () => {
  const pages = getCurrentPages()
  const cur = pages[pages.length - 1]
  storeId.value = cur.options.storeId || cur.options.storeid
  packageId.value = cur.options.packageId || cur.options.packageid
  startTime.value = decodeURIComponent(cur.options.startTime || cur.options.starttime || '')
  customEnd.value = decodeURIComponent(cur.options.endTime || cur.options.endtime || '')

  const pkgs = await getPackageList()
  packageInfo.value = (pkgs || []).find(p => String(p.id) === String(packageId.value))

  const end = calcEndTime().format('YYYY-MM-DD HH:mm')
  seatMap.value = await getSeatMap(storeId.value, startTime.value, end)
})
</script>

<style scoped>
.page {
  padding: 24rpx 24rpx 200rpx;
  background: #f5f6fa;
  min-height: 100vh;
  box-sizing: border-box;
}

/* ---------- 预约信息卡片 ---------- */
.info-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(31, 35, 41, 0.04);
}
.info-row { display: flex; justify-content: space-between; align-items: center; }
.info-left { display: flex; flex-direction: column; gap: 8rpx; }
.info-label { font-size: 22rpx; color: #86909c; }
.info-time { font-size: 34rpx; font-weight: 700; color: #1f2329; }
.info-end { font-size: 24rpx; color: #4e5969; }
.info-pkg {
  background: linear-gradient(135deg, #4a7cf7, #6b96ff);
  border-radius: 999rpx;
  padding: 12rpx 28rpx;
}
.info-pkg-name { font-size: 24rpx; color: #fff; font-weight: 600; }

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx 24rpx;
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 2rpx solid #f2f3f5;
}
.lg { display: flex; align-items: center; font-size: 22rpx; color: #4e5969; }
.dot { width: 20rpx; height: 20rpx; border-radius: 6rpx; margin-right: 8rpx; }
.dot.idle { background: #e8f8ee; border: 2rpx solid #00b42a; box-sizing: border-box; }
.dot.busy { background: #c9cdd4; }
.dot.using { background: #4a7cf7; }
.dot.maintain { background: #ff7d00; }
.dot.clean { background: #fadc19; }
.dot.locked { background: #f53f3f; }

/* ---------- 教室前方 ---------- */
.room-front {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 8rpx 0 28rpx;
}
.front-bar {
  width: 420rpx;
  height: 14rpx;
  border-radius: 999rpx;
  background: linear-gradient(90deg, rgba(74, 124, 247, 0), #4a7cf7 20%, #4a7cf7 80%, rgba(74, 124, 247, 0));
  opacity: 0.35;
}
.front-text { margin-top: 10rpx; font-size: 20rpx; color: #a9aeb8; letter-spacing: 4rpx; }

/* ---------- 座位网格 ---------- */
.seat-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 32rpx 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(31, 35, 41, 0.04);
  min-height: 300rpx;
}
.seat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(108rpx, 1fr));
  gap: 24rpx 16rpx;
  justify-items: center;
}
.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240rpx;
  color: #86909c;
  font-size: 26rpx;
}

/* ---------- 座位块（含椅背造型） ---------- */
.seat {
  position: relative;
  width: 96rpx;
  height: 88rpx;
  border-radius: 16rpx 16rpx 20rpx 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.seat-back {
  position: absolute;
  top: -10rpx;
  left: 12rpx;
  right: 12rpx;
  height: 16rpx;
  border-radius: 8rpx;
  background: inherit;
  opacity: 0.85;
}
.seat-no { font-size: 22rpx; font-weight: 600; }

/* 空闲：浅绿描边，可点击 */
.seat.st-0 {
  background: #e8f8ee;
  border: 2rpx solid #00b42a;
  box-sizing: border-box;
  cursor: pointer;
}
.seat.st-0 .seat-no { color: #00a129; font-weight: 600; }
.seat.st-0:active { transform: scale(0.94); }

/* 占用：灰色 */
.seat.st-1 { background: #e5e6eb; }
.seat.st-1 .seat-no { color: #a9aeb8; }
/* 使用中：浅蓝 */
.seat.st-2 { background: #dbe4fd; }
.seat.st-2 .seat-no { color: #4a7cf7; }
/* 维护：浅橙 */
.seat.st-3 { background: #ffe4ce; }
.seat.st-3 .seat-no { color: #ff7d00; }
/* 打扫：浅黄 */
.seat.st-4 { background: #fdf3c0; }
.seat.st-4 .seat-no { color: #c7a400; }
/* 锁定：浅红 */
.seat.st-5 { background: #fdd9d7; }
.seat.st-5 .seat-no { color: #f53f3f; }

.seat.st-1, .seat.st-2, .seat.st-3, .seat.st-4, .seat.st-5 { opacity: 0.75; }

/* 选中态：实心主色 + 描边光晕 + 轻微放大 */
.seat.active {
  background: #4a7cf7;
  border: 2rpx solid #4a7cf7;
  transform: scale(1.08);
  box-shadow: 0 8rpx 20rpx rgba(74, 124, 247, 0.35);
  opacity: 1;
  z-index: 2;
}
.seat.active .seat-no { color: #fff; font-weight: 700; }
.seat.active .seat-back { background: #3b6ae0; opacity: 1; }

/* ---------- 底部操作栏 ---------- */
.footer-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  gap: 24rpx;
  box-sizing: border-box;
}
.footer-info {
  flex: 1;
  display: flex;
  align-items: baseline;
  gap: 12rpx;
  min-height: 40rpx;
}
.footer-seat { font-size: 36rpx; font-weight: 700; color: #4a7cf7; }
.footer-type { font-size: 24rpx; color: #86909c; }
.footer-hint { font-size: 24rpx; color: #a9aeb8; }
.footer-btn {
  width: 320rpx;
  height: 88rpx;
  line-height: 88rpx;
  background: linear-gradient(135deg, #4a7cf7, #6b96ff);
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 44rpx;
  box-shadow: 0 8rpx 20rpx rgba(74, 124, 247, 0.3);
}
.footer-btn.disabled {
  background: #e5e6eb;
  color: #a9aeb8;
  box-shadow: none;
}
</style>
