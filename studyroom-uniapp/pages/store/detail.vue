<template>
  <!-- 门店详情：FR-UC-015 轮播/信息/设施 + FR-UC-016 日期时段选择 + FR-UC-020 黑名单置灰 + FR-UC-019 套餐 -->
  <view class="page">
    <!-- 轮播（FR-UC-015） -->
    <swiper v-if="detail" class="banner" indicator-dots circular autoplay :interval="4000">
      <swiper-item v-for="(img, i) in detail.imageList" :key="i">
        <image class="banner-img" :src="img" mode="aspectFill" />
      </swiper-item>
      <swiper-item v-if="!detail.imageList || detail.imageList.length === 0">
        <view class="banner-img banner-empty">📖</view>
      </swiper-item>
    </swiper>

    <view v-if="detail" class="body">
      <!-- 基本信息 -->
      <view class="card">
        <view class="store-name">
          {{ detail.name }}
          <text class="store-status" :class="`st-${detail.status}`">{{ detail.statusDesc }}</text>
        </view>
        <view class="row" @click="openMap">
          <text class="row-icon">📍</text>
          <text class="row-text">{{ detail.address }}</text>
          <text class="row-arrow">导航 ›</text>
        </view>
        <view class="row">
          <text class="row-icon">🕐</text>
          <text class="row-text">营业时间 {{ detail.businessHours }}</text>
        </view>
        <view v-if="detail.phone" class="row" @click="callPhone">
          <text class="row-icon">📞</text>
          <text class="row-text">{{ detail.phone }}</text>
        </view>
        <view class="row">
          <text class="row-icon">⭐</text>
          <text class="row-text">综合评分 {{ detail.rating || '5.0' }}</text>
        </view>
        <view v-if="detail.description" class="desc">{{ detail.description }}</view>
        <view v-if="detail.facilityList && detail.facilityList.length" class="tags">
          <text v-for="(f, i) in detail.facilityList" :key="i" class="tag">{{ f }}</text>
        </view>
      </view>

      <!-- 7 天日期条（FR-UC-016） -->
      <view class="card">
        <view class="card-title">选择日期</view>
        <scroll-view scroll-x class="date-bar">
          <view v-for="d in dateList" :key="d.value" class="date-item"
                :class="{ active: d.value === selectedDate }" @click="selectDate(d.value)">
            <text class="date-week">{{ d.week }}</text>
            <text class="date-day">{{ d.label }}</text>
          </view>
        </scroll-view>
      </view>

      <!-- 时段选择（FR-UC-016 30分钟粒度 + FR-UC-020 黑名单置灰） -->
      <view class="card">
        <view class="card-title">选择开始时段</view>
        <view v-if="blacklist && blacklist.allDayClosed" class="closed-tip">
          当日全天闭店休息，请选择其他日期
        </view>
        <view v-else class="slot-grid">
          <view v-for="s in timeSlots" :key="s.time" class="slot"
                :class="{ disabled: s.disabled, active: s.time === selectedTime, past: s.past }"
                @click="selectTime(s)">
            {{ s.time }}
          </view>
        </view>
        <view class="slot-legend">
          <text class="lg lg-ok">可选</text>
          <text class="lg lg-grey">闭店/占用</text>
          <text class="lg lg-past">已过</text>
        </view>
      </view>

      <!-- 计费方式：套餐 / 自定义时长（FR-UC-019） -->
      <view class="card">
        <view class="card-title">计费方式</view>
        <view class="mode-tabs">
          <view class="mode-tab" :class="{ active: billingMode === 'package' }"
                @click="switchMode('package')">按套餐</view>
          <view class="mode-tab" :class="{ active: billingMode === 'custom' }"
                @click="switchMode('custom')">自定义时长</view>
        </view>

        <block v-if="billingMode === 'package'">
          <view class="pkg-tabs">
            <view v-for="t in pkgTabs" :key="t.type" class="pkg-tab"
                  :class="{ active: activeTab === t.type }" @click="switchTab(t.type)">
              {{ t.label }}
            </view>
          </view>
          <view class="pkg-list">
            <view v-for="p in currentPackages" :key="p.id" class="pkg-item"
                  :class="{ active: selectedPackage && selectedPackage.id === p.id }"
                  @click="selectPackage(p)">
              <view class="pkg-info">
                <view class="pkg-name">{{ p.name }}</view>
                <view class="pkg-desc">{{ p.durationDesc }} · {{ p.description || '自习套餐' }}</view>
              </view>
              <view class="pkg-price">
                <text class="pkg-now">¥{{ p.price }}</text>
                <text v-if="p.originalPrice" class="pkg-origin">¥{{ p.originalPrice }}</text>
              </view>
            </view>
            <view v-if="currentPackages.length === 0" class="pkg-empty">该分类暂无在售套餐</view>
          </view>
        </block>

        <block v-else>
          <view class="custom-tip">
            按 {{ customHourly }} 元/小时计价，最短 30 分钟，最长 24 小时
          </view>
          <view v-if="durationOptions.length" class="dur-grid">
            <view v-for="d in durationOptions" :key="d.minutes" class="dur-item"
                  :class="{ active: customMinutes === d.minutes, disabled: d.disabled }"
                  @click="selectDuration(d)">
              {{ d.label }}
            </view>
          </view>
          <view v-else class="pkg-empty">请先选择开始时段</view>
          <view v-if="customMinutes" class="custom-sum">
            结束时间 {{ customEndText }} · 预计 ¥{{ customAmount }}
          </view>
          <view v-if="!customPkg" class="pkg-empty">门店暂未开放自定义时长</view>
        </block>
      </view>

      <!-- 底部按钮 -->
      <view class="footer-bar">
        <view class="footer-info">
          <view v-if="footerSummary" class="footer-sum">{{ footerSummary }}</view>
          <view v-else class="footer-hint">请选择时段与{{ billingMode === 'package' ? '套餐' : '时长' }}</view>
        </view>
        <button class="footer-btn" :disabled="!canGoSeat" :class="{ disabled: !canGoSeat }"
                @click="goSeatMap">去选座</button>
      </view>
    </view>

    <view v-else class="loading">加载中...</view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { getStoreDetail, getBlacklist, getPackageList } from '@/api/store'

const props = defineProps({}) // eslint-disable-line

const storeId = ref(null)
const detail = ref(null)
const blacklist = ref(null)
const packages = ref([])

// FR-UC-016：7 天日期条
const dateList = computed(() => {
  const weeks = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const list = []
  for (let i = 0; i < 7; i++) {
    const d = dayjs().add(i, 'day')
    list.push({
      value: d.format('YYYY-MM-DD'),
      label: i === 0 ? '今天' : d.format('MM/DD'),
      week: i === 0 ? '今天' : weeks[d.day()]
    })
  }
  return list
})
const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const selectedTime = ref('')

// FR-UC-020：黑名单时段 + 已过时段 → 置灰
const timeSlots = computed(() => {
  const slots = []
  if (!detail.value) return slots
  // 解析营业时间，如 08:00-23:00
  const m = (detail.value.businessHours || '08:00-22:00').match(/(\d{1,2}):(\d{2})\s*-\s*(\d{1,2}):(\d{2})/)
  const open = m ? Number(m[1]) * 60 + Number(m[2]) : 8 * 60
  const close = m ? Number(m[3]) * 60 + Number(m[4]) : 22 * 60
  const now = dayjs()
  const isToday = selectedDate.value === now.format('YYYY-MM-DD')
  const nowMin = now.hour() * 60 + now.minute()

  for (let t = open; t <= close - 30; t += 30) {
    const time = `${String(Math.floor(t / 60)).padStart(2, '0')}:${String(t % 60).padStart(2, '0')}`
    const past = isToday && t <= nowMin
    let disabled = past
    // 黑名单重叠判定（FR-UC-020）
    if (blacklist.value && !blacklist.value.allDayClosed) {
      const [h, mi] = time.split(':').map(Number)
      const start = h * 60 + mi
      for (const b of blacklist.value.slots || []) {
        const bs = toMin(b.startTime)
        const be = toMin(b.endTime)
        if (start < be && start + 30 > bs) { disabled = true; break }
      }
    }
    slots.push({ time, disabled, past })
  }
  return slots
})

// FR-UC-019：套餐选项卡
const pkgTabs = [
  { type: 1, label: '按时长' },
  { type: 2, label: '日卡' },
  { type: 3, label: '周卡' },
  { type: 4, label: '月卡' }
]
const activeTab = ref(1)
const selectedPackage = ref(null)
const currentPackages = computed(() => packages.value.filter(p => p.type === activeTab.value))

// 自定义时长：不买套餐，按实际选择的起止时间计费（type=5 的自定义套餐提供小时单价）
const billingMode = ref('package')
const customMinutes = ref(0)
const customPkg = computed(() => packages.value.find(p => p.type === 5) || null)
const customHourly = computed(() => customPkg.value ? Number(customPkg.value.price).toFixed(2) : '0.00')

const DURATION_PRESETS = [
  { minutes: 30, label: '半小时' },
  { minutes: 60, label: '1小时' },
  { minutes: 90, label: '1.5小时' },
  { minutes: 120, label: '2小时' },
  { minutes: 180, label: '3小时' },
  { minutes: 240, label: '4小时' },
  { minutes: 360, label: '6小时' },
  { minutes: 480, label: '8小时' }
]

// 可选时长：不得超出当日闭店时间，也不得与黑名单时段交叉
const durationOptions = computed(() => {
  if (!selectedTime.value || !detail.value) return []
  const m = (detail.value.businessHours || '08:00-22:00').match(/(\d{1,2}):(\d{2})\s*-\s*(\d{1,2}):(\d{2})/)
  const close = m ? Number(m[3]) * 60 + Number(m[4]) : 22 * 60
  const start = toMin(selectedTime.value)
  return DURATION_PRESETS.map(d => ({
    ...d,
    disabled: start + d.minutes > close || overlapsBlacklist(start, start + d.minutes)
  }))
})

const customEndText = computed(() => {
  if (!selectedTime.value || !customMinutes.value) return ''
  const end = toMin(selectedTime.value) + customMinutes.value
  return `${String(Math.floor(end / 60)).padStart(2, '0')}:${String(end % 60).padStart(2, '0')}`
})

const customAmount = computed(() =>
  (Number(customHourly.value) * customMinutes.value / 60).toFixed(2))

const canGoSeat = computed(() => {
  if (!selectedTime.value) return false
  return billingMode.value === 'custom'
    ? !!(customMinutes.value && customPkg.value)
    : !!selectedPackage.value
})

const footerSummary = computed(() => {
  if (billingMode.value === 'custom') {
    if (!selectedTime.value || !customMinutes.value) return ''
    const item = DURATION_PRESETS.find(d => d.minutes === customMinutes.value)
    return `${selectedDate.value} ${selectedTime.value}~${customEndText.value} · 自定义${item ? item.label : ''}`
  }
  return selectedTime.value && selectedPackage.value
    ? `${selectedDate.value} ${selectedTime.value} 起 · ${selectedPackage.value.durationDesc}`
    : ''
})

function overlapsBlacklist(start, end) {
  if (!blacklist.value || blacklist.value.allDayClosed) return false
  for (const b of blacklist.value.slots || []) {
    if (start < toMin(b.endTime) && end > toMin(b.startTime)) return true
  }
  return false
}

function switchMode(mode) {
  billingMode.value = mode
  customMinutes.value = 0
  selectedPackage.value = null
}

function selectDuration(d) {
  if (d.disabled) return
  customMinutes.value = d.minutes
}

function toMin(hhmm) {
  if (!hhmm) return 0
  const [h, m] = String(hhmm).split(':').map(Number)
  return h * 60 + (m || 0)
}

function selectDate(v) {
  selectedDate.value = v
  selectedTime.value = ''
  customMinutes.value = 0
  loadBlacklist()
}

function selectTime(s) {
  if (s.disabled) return
  selectedTime.value = s.time
  customMinutes.value = 0
}

function switchTab(type) {
  activeTab.value = type
}

function selectPackage(p) {
  selectedPackage.value = p
}

function openMap() {
  const d = detail.value
  if (d && d.latitude && d.longitude) {
    uni.openLocation({
      latitude: Number(d.latitude),
      longitude: Number(d.longitude),
      name: d.name,
      address: d.address
    })
  }
}

function callPhone() {
  uni.makePhoneCall({ phoneNumber: detail.value.phone })
}

async function loadBlacklist() {
  blacklist.value = await getBlacklist(storeId.value, selectedDate.value)
}

function goSeatMap() {
  if (!canGoSeat.value) return
  const startTime = `${selectedDate.value} ${selectedTime.value}`
  let url = `/pages/seat/seat-map?storeId=${storeId.value}&startTime=${encodeURIComponent(startTime)}`
  if (billingMode.value === 'custom') {
    const endTime = `${selectedDate.value} ${customEndText.value}`
    url += `&packageId=${customPkg.value.id}&endTime=${encodeURIComponent(endTime)}`
  } else {
    url += `&packageId=${selectedPackage.value.id}`
  }
  uni.navigateTo({ url })
}

onMounted(async () => {
  const pages = getCurrentPages()
  const cur = pages[pages.length - 1]
  storeId.value = cur.options.storeId || cur.options.storeid

  const [d, pkgs] = await Promise.all([
    getStoreDetail(storeId.value),
    getPackageList()
  ])
  detail.value = d
  packages.value = pkgs || []
  await loadBlacklist()
})
</script>

<style scoped>
.page { padding-bottom: 160rpx; background: #f5f6fa; min-height: 100vh; }
.banner { height: 360rpx; }
.banner-img { width: 100%; height: 360rpx; }
.banner-empty { display: flex; align-items: center; justify-content: center; font-size: 100rpx; background: #4a7cf7; }

.body { padding: 24rpx; }
.card { background: #fff; border-radius: 20rpx; padding: 24rpx; margin-bottom: 24rpx; }
.card-title { font-size: 30rpx; font-weight: 600; color: #1f2329; margin-bottom: 20rpx; }

.store-name { font-size: 36rpx; font-weight: 700; color: #1f2329; margin-bottom: 16rpx; display: flex; align-items: center; gap: 12rpx; }
.store-status { font-size: 22rpx; padding: 2rpx 12rpx; border-radius: 8rpx; }
.st-1 { color: #00b42a; background: rgba(0, 180, 42, .1); }
.st-0 { color: #86909c; background: #f2f3f5; }
.st-2 { color: #ff7d00; background: rgba(255, 125, 0, .1); }
.row { display: flex; align-items: center; padding: 12rpx 0; }
.row-icon { margin-right: 14rpx; }
.row-text { flex: 1; font-size: 26rpx; color: #4e5969; }
.row-arrow { font-size: 24rpx; color: #4a7cf7; }
.desc { margin-top: 12rpx; font-size: 26rpx; color: #86909c; line-height: 1.6; }
.tags { margin-top: 16rpx; display: flex; flex-wrap: wrap; gap: 12rpx; }
.tag { padding: 4rpx 16rpx; background: #f2f3f5; border-radius: 8rpx; font-size: 22rpx; color: #4e5969; }

.date-bar { white-space: nowrap; }
.date-item { display: inline-flex; flex-direction: column; align-items: center; width: 120rpx; padding: 16rpx 0; margin-right: 16rpx; border-radius: 14rpx; background: #f5f6fa; }
.date-item.active { background: #4a7cf7; }
.date-item.active .date-week, .date-item.active .date-day { color: #fff; }
.date-week { font-size: 22rpx; color: #86909c; }
.date-day { margin-top: 6rpx; font-size: 28rpx; font-weight: 600; color: #1f2329; }

.slot-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.slot { width: calc((100% - 64rpx) / 5); text-align: center; padding: 16rpx 0; border-radius: 12rpx; background: #f5f6fa; font-size: 26rpx; color: #1f2329; }
.slot.active { background: #4a7cf7; color: #fff; font-weight: 600; }
.slot.disabled { background: #e5e6eb; color: #c9cdd4; text-decoration: line-through; }
.slot.past { background: #f2f3f5; color: #c9cdd4; }
.slot-legend { margin-top: 20rpx; display: flex; gap: 24rpx; font-size: 22rpx; }
.lg::before { content: ''; display: inline-block; width: 16rpx; height: 16rpx; border-radius: 4rpx; margin-right: 8rpx; vertical-align: middle; }
.lg-ok::before { background: #4a7cf7; }
.lg-grey::before { background: #e5e6eb; }
.lg-past::before { background: #f2f3f5; }
.closed-tip { padding: 40rpx 0; text-align: center; color: #ff7d00; font-size: 26rpx; }

.pkg-tabs { display: flex; gap: 16rpx; margin-bottom: 20rpx; }
.pkg-tab { flex: 1; text-align: center; padding: 14rpx 0; border-radius: 12rpx; background: #f5f6fa; font-size: 26rpx; color: #4e5969; }
.pkg-tab.active { background: rgba(74, 124, 247, .12); color: #4a7cf7; font-weight: 600; }

/* 计费方式切换 + 自定义时长 */
.mode-tabs { display: flex; gap: 16rpx; margin-bottom: 20rpx; }
.mode-tab { flex: 1; text-align: center; padding: 16rpx 0; border-radius: 12rpx; border: 2rpx solid #e5e6eb; font-size: 28rpx; color: #4e5969; }
.mode-tab.active { border-color: #4a7cf7; background: rgba(74, 124, 247, .08); color: #4a7cf7; font-weight: 600; }
.custom-tip { font-size: 24rpx; color: #86909c; margin-bottom: 16rpx; }
.dur-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.dur-item { width: calc((100% - 48rpx) / 4); text-align: center; padding: 18rpx 0; border-radius: 12rpx; background: #f5f6fa; font-size: 26rpx; color: #1f2329; }
.dur-item.active { background: #4a7cf7; color: #fff; font-weight: 600; }
.dur-item.disabled { background: #f2f3f5; color: #c9cdd4; text-decoration: line-through; }
.custom-sum { margin-top: 20rpx; padding: 20rpx; border-radius: 12rpx; background: rgba(74, 124, 247, .06); font-size: 26rpx; color: #4a7cf7; }
.pkg-item { display: flex; align-items: center; padding: 24rpx; border: 2rpx solid #f2f3f5; border-radius: 14rpx; margin-bottom: 16rpx; }
.pkg-item.active { border-color: #4a7cf7; background: rgba(74, 124, 247, .04); }
.pkg-info { flex: 1; }
.pkg-name { font-size: 28rpx; font-weight: 600; color: #1f2329; }
.pkg-desc { margin-top: 6rpx; font-size: 24rpx; color: #86909c; }
.pkg-price { text-align: right; }
.pkg-now { font-size: 32rpx; font-weight: 700; color: #ff7d00; }
.pkg-origin { margin-left: 10rpx; font-size: 22rpx; color: #c9cdd4; text-decoration: line-through; }
.pkg-empty { padding: 40rpx 0; text-align: center; color: #86909c; font-size: 26rpx; }

.footer-bar { position: fixed; left: 0; right: 0; bottom: 0; display: flex; align-items: center; padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom)); background: #fff; box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, .04); }
.footer-info { flex: 1; }
.footer-sum { font-size: 26rpx; color: #1f2329; }
.footer-hint { font-size: 26rpx; color: #86909c; }
.footer-btn { width: 260rpx; height: 88rpx; line-height: 88rpx; background: #4a7cf7; color: #fff; font-size: 30rpx; border-radius: 44rpx; }
.footer-btn.disabled { background: #c9cdd4; }

.loading { padding: 120rpx 0; text-align: center; color: #86909c; }
</style>
