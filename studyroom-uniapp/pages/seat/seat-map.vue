<template>
  <!-- Canvas 座位平面图：FR-UC-017 六色状态（绿/灰/蓝/橙/黄/红） -->
  <view class="page">
    <view class="header-card">
      <view class="header-row">
        <text class="header-time">{{ startTime }}</text>
        <text class="header-pkg">{{ packageInfo ? packageInfo.name : '' }}</text>
      </view>
      <view class="legend">
        <view class="lg"><view class="dot" style="background:#00b42a" />空闲</view>
        <view class="lg"><view class="dot" style="background:#c9cdd4" />占用</view>
        <view class="lg"><view class="dot" style="background:#4a7cf7" />使用中</view>
        <view class="lg"><view class="dot" style="background:#ff7d00" />维护</view>
        <view class="lg"><view class="dot" style="background:#fadc19" />打扫</view>
        <view class="lg"><view class="dot" style="background:#f53f3f" />锁定</view>
      </view>
    </view>

    <!-- FR-UC-017：uni.createCanvasContext 绘制座位平面图 -->
    <view class="canvas-wrap">
      <canvas :canvas-id="CANVAS_ID" id="seat-canvas" class="seat-canvas"
              :style="{ width: canvasStyleWidth + 'px', height: canvasStyleHeight + 'px' }"
              @touchstart="onTouchSeat" />
      <view v-if="!seatMap" class="loading">座位图加载中...</view>
    </view>

    <view v-if="selectedSeat" class="selected-bar">
      已选座位：<text class="selected-no">{{ selectedSeat.seatNo }}</text>
      <text class="selected-type">（{{ seatTypeName(selectedSeat.seatTypeId) }}）</text>
    </view>

    <view class="footer-bar">
      <button class="footer-btn" :disabled="!selectedSeat" :class="{ disabled: !selectedSeat }"
              @click="goConfirm">去下单</button>
    </view>
  </view>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { getSeatMap, getPackageList } from '@/api/store'

const CANVAS_ID = 'seat-canvas'
/** 座位方块绘制尺寸（rpx → 按屏幕比例换算） */
const SEAT_W = 44
const SEAT_H = 44
const SEAT_GAP = 16
const PADDING = 16

/** 六色映射（FR-UC-017）：0空闲绿 1占用灰 2使用中蓝 3维护橙 4打扫黄 5锁定红 */
const STATUS_COLORS = ['#00b42a', '#c9cdd4', '#4a7cf7', '#ff7d00', '#fadc19', '#f53f3f']

const storeId = ref(null)
const startTime = ref('')
const packageId = ref(null)
const packageInfo = ref(null)

const seatMap = ref(null)
const selectedSeat = ref(null)
const scale = ref(0.5)

const canvasStyleWidth = ref(375)
const canvasStyleHeight = ref(400)

function seatTypeName(typeId) {
  const t = (seatMap.value && seatMap.value.seatTypes || []).find(x => x.id === typeId)
  return t ? t.name : ''
}

/** 计算套餐结束时间（座位图查询窗口） */
function calcEndTime() {
  const p = packageInfo.value
  const start = dayjs(startTime.value, 'YYYY-MM-DD HH:mm')
  if (!p) return start.add(2, 'hour')
  switch (p.unit) {
    case 'hour': return start.add(p.durationValue, 'hour')
    case 'day': return start.add(p.durationValue, 'day')
    case 'week': return start.add(p.durationValue * 7, 'day')
    case 'month': return start.add(p.durationValue, 'month')
    default: return start.add(p.durationValue, 'hour')
  }
}

/** 绘制座位平面图（FR-UC-017） */
function drawSeatMap() {
  const map = seatMap.value
  if (!map) return
  const ctx = uni.createCanvasContext(CANVAS_ID)
  const w = SEAT_W * scale.value
  const h = SEAT_H * scale.value

  // 背景
  ctx.setFillStyle('#ffffff')
  ctx.fillRect(0, 0, canvasStyleWidth.value, canvasStyleHeight.value)

  for (const seat of map.seats) {
    const x = (seat.xCoord || 0) * scale.value
    const y = (seat.yCoord || 0) * scale.value
    const color = STATUS_COLORS[seat.displayStatus] || '#c9cdd4'

    // 座位方块（选中态描边）
    ctx.setFillStyle(color)
    ctx.fillRect(x, y, w, h)
    if (selectedSeat.value && selectedSeat.value.id === seat.id) {
      ctx.setStrokeStyle('#1f2329')
      ctx.setLineWidth(3)
      ctx.strokeRect(x - 3, y - 3, w + 6, h + 6)
    }

    // 座位编号
    ctx.setFillStyle('#ffffff')
    ctx.setFontSize(9)
    ctx.setTextAlign('center')
    ctx.setTextBaseline('middle')
    ctx.fillText(seat.seatNo, x + w / 2, y + h / 2)
  }
  ctx.draw()
}

/** 点击选座：命中检测 → 仅空闲可选 */
function onTouchSeat(e) {
  const touch = e.touches && e.touches[0]
  if (!touch || !seatMap.value) return
  const x = touch.x
  const y = touch.y
  const w = SEAT_W * scale.value
  const h = SEAT_H * scale.value

  for (const seat of seatMap.value.seats) {
    const sx = (seat.xCoord || 0) * scale.value
    const sy = (seat.yCoord || 0) * scale.value
    if (x >= sx && x <= sx + w && y >= sy && y <= sy + h) {
      if (!seat.selectable) {
        uni.showToast({ title: `该座位${seat.statusDesc}，不可选择`, icon: 'none' })
        return
      }
      selectedSeat.value = selectedSeat.value && selectedSeat.value.id === seat.id ? null : seat
      drawSeatMap()
      return
    }
  }
}

function goConfirm() {
  if (!selectedSeat.value) return
  uni.navigateTo({
    url: `/pages/order/confirm?storeId=${storeId.value}` +
      `&seatId=${selectedSeat.value.id}` +
      `&packageId=${packageId.value}` +
      `&startTime=${encodeURIComponent(startTime.value)}`
  })
}

onMounted(async () => {
  const pages = getCurrentPages()
  const cur = pages[pages.length - 1]
  storeId.value = cur.options.storeId || cur.options.storeid
  packageId.value = cur.options.packageId || cur.options.packageid
  startTime.value = decodeURIComponent(cur.options.startTime || cur.options.starttime || '')

  // 屏幕缩放比例（坐标为设计像素，rpx 坐标按 750 设计稿换算）
  const sys = uni.getSystemInfoSync()
  scale.value = sys.windowWidth / 375

  const pkgs = await getPackageList()
  packageInfo.value = (pkgs || []).find(p => String(p.id) === String(packageId.value))

  const end = calcEndTime().format('YYYY-MM-DD HH:mm')
  const map = await getSeatMap(storeId.value, startTime.value, end)
  seatMap.value = map

  // 画布尺寸
  canvasStyleWidth.value = (map.canvasWidth || 375) * scale.value
  canvasStyleHeight.value = (map.canvasHeight || 400) * scale.value

  // 等 canvas 节点就绪后绘制
  setTimeout(drawSeatMap, 100)
})
</script>

<style scoped>
.page { padding: 24rpx 24rpx 160rpx; background: #f5f6fa; min-height: 100vh; }
.header-card { background: #fff; border-radius: 20rpx; padding: 24rpx; margin-bottom: 24rpx; }
.header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; }
.header-time { font-size: 30rpx; font-weight: 600; color: #1f2329; }
.header-pkg { font-size: 26rpx; color: #4a7cf7; }
.legend { display: flex; flex-wrap: wrap; gap: 20rpx; }
.lg { display: flex; align-items: center; font-size: 22rpx; color: #4e5969; }
.dot { width: 18rpx; height: 18rpx; border-radius: 6rpx; margin-right: 8rpx; }

.canvas-wrap { background: #fff; border-radius: 20rpx; padding: 16rpx; position: relative; }
.seat-canvas { display: block; }
.loading { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; color: #86909c; font-size: 26rpx; }

.selected-bar { margin-top: 24rpx; background: rgba(74, 124, 247, .08); border: 2rpx solid rgba(74, 124, 247, .3); border-radius: 14rpx; padding: 24rpx; font-size: 28rpx; color: #1f2329; }
.selected-no { color: #4a7cf7; font-weight: 700; font-size: 32rpx; }
.selected-type { color: #86909c; font-size: 24rpx; }

.footer-bar { position: fixed; left: 0; right: 0; bottom: 0; padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom)); background: #fff; box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, .04); }
.footer-btn { height: 88rpx; line-height: 88rpx; background: #4a7cf7; color: #fff; font-size: 30rpx; border-radius: 44rpx; }
.footer-btn.disabled { background: #c9cdd4; }
</style>
