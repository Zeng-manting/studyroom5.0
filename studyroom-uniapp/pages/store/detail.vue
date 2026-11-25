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

      <!-- 预约面板：模式 → 日期 → 时长/套餐 → 起止时间 -->
      <view class="card booking-card">
        <view class="mode-tabs">
          <view class="mode-tab" :class="{ active: billingMode === 'custom' }"
                @click="switchMode('custom')">小时模式</view>
          <view class="mode-tab" :class="{ active: billingMode === 'package' }"
                @click="switchMode('package')">套餐模式</view>
        </view>

        <scroll-view scroll-x class="date-bar" :show-scrollbar="false">
          <view v-for="d in dateList" :key="d.value" class="date-item"
                :class="{ active: d.value === selectedDate }" @click="selectDate(d.value)">
            <text class="date-week">{{ d.week }}</text>
            <text class="date-day">{{ d.label }}</text>
          </view>
        </scroll-view>

        <block v-if="billingMode === 'custom'">
          <view class="section-head">
            <text>选择时长</text>
            <text class="section-note">¥{{ customHourly }}/小时</text>
          </view>
          <view v-if="customPkg" class="dur-grid">
            <view v-for="d in durationOptions" :key="d.minutes" class="dur-item"
                  :class="{ active: customMinutes === d.minutes, disabled: d.disabled }"
                  @click="selectDuration(d)">
              {{ d.label }}
            </view>
          </view>
          <view v-else class="pkg-empty">门店暂未开放自定义时长</view>
        </block>

        <block v-else>
          <view class="section-head">
            <text>选择套餐</text>
            <text class="section-note">按套餐固定时长计费</text>
          </view>
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
                <view class="pkg-desc">{{ packageDurationText(p) }} · {{ p.description || '自习套餐' }}</view>
              </view>
              <view class="pkg-price">
                <text class="pkg-now">¥{{ p.price }}</text>
                <text v-if="p.originalPrice" class="pkg-origin">¥{{ p.originalPrice }}</text>
              </view>
            </view>
            <view v-if="currentPackages.length === 0" class="pkg-empty">该分类暂无在售套餐</view>
          </view>
        </block>

        <view class="time-section">
          <view class="section-head">
            <text>预约时间</text>
            <text class="section-note">5分钟取整 · 30分钟起订</text>
          </view>
          <view v-if="blacklist && blacklist.allDayClosed" class="closed-tip">
            当日全天闭店休息，请选择其他日期
          </view>
          <template v-else>
            <view class="time-picker-row">
              <picker class="time-picker" mode="time" :value="selectedTime || defaultStartTime"
                      :start="businessRange.openText" :end="businessRange.closeText"
                      @change="onTimePickerChange">
                <view class="time-field" :class="{ placeholder: !selectedTime }">
                  <view class="time-label">开始时间</view>
                  <view class="time-value">{{ selectedTime || '请选择' }}</view>
                </view>
              </picker>
              <view class="time-arrow">→</view>
              <view class="time-field end" :class="{ placeholder: !endTimeText }">
                <view class="time-label">结束时间</view>
                <view class="time-value">{{ endTimeText || '自动计算' }}</view>
              </view>
            </view>
            <view class="time-tip">
              {{ selectedTime ? `${selectedDate} · ${billingMode === 'custom' ? '选择时长后生成完整时段' : '选择套餐后生成完整时段'}` : '点击开始时间，选择可预约时段' }}
            </view>
          </template>
        </view>
      </view>

      <!-- 底部预约汇总 -->
      <view class="footer-bar">
        <view class="footer-info">
          <view v-if="footerSummary" class="footer-sum">{{ footerSummary }}</view>
          <view v-else class="footer-hint">请选择{{ billingMode === 'package' ? '套餐' : '时长' }}与开始时间</view>
          <view v-if="canGoSeat" class="footer-price">合计 <text>¥{{ footerPrice }}</text></view>
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
      label: d.format('MM/DD'),
      week: i === 0 ? '今天' : i === 1 ? '明天' : weeks[d.day()]
    })
  }
  return list
})
const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const selectedTime = ref('')

// 营业时间区间（分钟），供时间选择器限定范围
const businessRange = computed(() => {
  const m = (detail.value?.businessHours || '08:00-22:00').match(/(\d{1,2}):(\d{2})\s*-\s*(\d{1,2}):(\d{2})/)
  const open = m ? Number(m[1]) * 60 + Number(m[2]) : 8 * 60
  const close = m ? Number(m[3]) * 60 + Number(m[4]) : 22 * 60
  return { open, close, openText: toText(open), closeText: toText(close) }
})

// 未选择时，选择器默认落在开门时间或今天下一个 5 分钟刻度
const defaultStartTime = computed(() => {
  const now = dayjs()
  let min = businessRange.value.open
  if (selectedDate.value === now.format('YYYY-MM-DD')) {
    min = Math.max(min, Math.ceil((now.hour() * 60 + now.minute()) / 5) * 5)
  }
  return min + 30 > businessRange.value.close ? businessRange.value.openText : toText(min)
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
const billingMode = ref('custom')
const customMinutes = ref(180)
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

// 可选时长：未选开始时间时也可先选时长；确定时段后校验闭店与黑名单
const durationOptions = computed(() => {
  if (!detail.value) return []
  if (!selectedTime.value) {
    return DURATION_PRESETS.map(d => ({ ...d, disabled: !!blacklist.value?.allDayClosed }))
  }
  const close = businessRange.value.close
  const start = toMin(selectedTime.value)
  return DURATION_PRESETS.map(d => ({
    ...d,
    disabled: start + d.minutes > close || overlapsBlacklist(start, start + d.minutes)
  }))
})

const customEndText = computed(() => {
  if (!selectedTime.value || !customMinutes.value) return ''
  const end = toMin(selectedTime.value) + customMinutes.value
  return toText(end)
})

const customAmount = computed(() =>
  (Number(customHourly.value) * customMinutes.value / 60).toFixed(2))

// 与后端 calcEndTime 使用相同的时长单位，实时展示预约结束时间
const endTimeText = computed(() => {
  if (!selectedTime.value) return ''
  if (billingMode.value === 'custom') return customEndText.value
  if (!selectedPackage.value) return ''
  const start = dayjs(`${selectedDate.value} ${selectedTime.value}`)
  const p = selectedPackage.value
  const end = start.add(p.durationValue || 1, p.unit || 'hour')
  return end.format('YYYY-MM-DD') === selectedDate.value
    ? end.format('HH:mm')
    : end.format('MM-DD HH:mm')
})

const canGoSeat = computed(() => {
  if (!selectedTime.value) return false
  return billingMode.value === 'custom'
    ? !!(customMinutes.value && customPkg.value)
    : !!selectedPackage.value
})

const footerPrice = computed(() => billingMode.value === 'custom'
  ? customAmount.value
  : Number(selectedPackage.value?.price || 0).toFixed(2))

const footerSummary = computed(() => {
  if (!selectedTime.value || !endTimeText.value) return ''
  const date = selectedDate.value.slice(5).replace('-', '/')
  if (billingMode.value === 'custom') {
    const item = DURATION_PRESETS.find(d => d.minutes === customMinutes.value)
    return `${date} ${selectedTime.value}~${endTimeText.value} · ${item ? item.label : ''}`
  }
  return `${date} ${selectedTime.value}~${endTimeText.value} · ${packageDurationText(selectedPackage.value)}`
})

function overlapsBlacklist(start, end) {
  if (!blacklist.value || blacklist.value.allDayClosed) return false
  for (const b of blacklist.value.slots || []) {
    if (start < toMin(b.endTime) && end > toMin(b.startTime)) return true
  }
  return false
}

function packageDurationText(p) {
  if (!p) return ''
  const unitLabels = { hour: '小时', day: '天', week: '周', month: '个月' }
  return `${p.durationValue || 1}${unitLabels[p.unit || 'hour'] || ''}`
}

function switchMode(mode) {
  if (billingMode.value === mode) return
  billingMode.value = mode
  customMinutes.value = mode === 'custom' ? 180 : 0
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

/** 分钟数 → HH:mm */
function toText(min) {
  return `${String(Math.floor(min / 60)).padStart(2, '0')}:${String(min % 60).padStart(2, '0')}`
}

function selectDate(v) {
  selectedDate.value = v
  selectedTime.value = ''
  loadBlacklist()
}

function onTimePickerChange(e) {
  const [h, m] = String(e.detail.value || '').split(':').map(Number)
  if (Number.isNaN(h) || Number.isNaN(m)) return
  // 5 分钟向上取整
  const rounded = Math.ceil((h * 60 + m) / 5) * 5
  const now = dayjs()
  if (selectedDate.value === now.format('YYYY-MM-DD')
      && rounded <= now.hour() * 60 + now.minute()) {
    uni.showToast({ title: '该时间已过，请选择更晚的时间', icon: 'none' })
    return
  }
  if (rounded + 30 > businessRange.value.close) {
    uni.showToast({ title: '闭店前需预留至少 30 分钟', icon: 'none' })
    return
  }
  if (overlapsBlacklist(rounded, rounded + 30)) {
    uni.showToast({ title: '该时段门店闭店休息，请更换时间', icon: 'none' })
    return
  }
  selectedTime.value = toText(rounded)
  const current = durationOptions.value.find(d => d.minutes === customMinutes.value)
  if (current && current.disabled) customMinutes.value = 0
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
.page { padding-bottom: 200rpx; background: #f5f6fa; min-height: 100vh; }
.banner { height: 360rpx; }
.banner-img { width: 100%; height: 360rpx; }
.banner-empty { display: flex; align-items: center; justify-content: center; font-size: 100rpx; background: #4a7cf7; }

.body { padding: 24rpx; }
.card { background: #fff; border-radius: 20rpx; padding: 28rpx; margin-bottom: 24rpx; }
.booking-card { padding-bottom: 30rpx; }

.store-name { font-size: 36rpx; font-weight: 700; color: #1f2329; margin-bottom: 16rpx; display: flex; align-items: center; gap: 12rpx; }
.store-status { font-size: 22rpx; padding: 2rpx 12rpx; border-radius: 8rpx; }
.st-1 { color: #00b42a; background: rgba(0, 180, 42, .1); }
.st-0 { color: #86909c; background: #f2f3f5; }
.st-2 { color: #ff7d00; background: rgba(255, 125, 0, .1); }
.row { display: flex; align-items: center; padding: 12rpx 0; }
.row-icon { margin-right: 14rpx; }
.row-text { flex: 1; font-size: 26rpx; color: #4e5969; }
.row-arrow { font-size: 24rpx; color: #1677ff; }
.desc { margin-top: 12rpx; font-size: 26rpx; color: #86909c; line-height: 1.6; }
.tags { margin-top: 16rpx; display: flex; flex-wrap: wrap; gap: 12rpx; }
.tag { padding: 4rpx 16rpx; background: #f2f3f5; border-radius: 8rpx; font-size: 22rpx; color: #4e5969; }

/* 小时 / 套餐分段切换 */
.mode-tabs { display: flex; padding: 6rpx; margin-bottom: 28rpx; border-radius: 16rpx; background: #f2f4f8; }
.mode-tab { flex: 1; padding: 18rpx 0; text-align: center; border-radius: 12rpx; font-size: 28rpx; color: #4e5969; transition: all .2s ease; }
.mode-tab.active { background: #fff; color: #1677ff; font-weight: 600; box-shadow: 0 2rpx 10rpx rgba(31, 35, 41, .06); }

/* 横向日期 */
.date-bar { white-space: nowrap; margin: 0 -4rpx; }
.date-item { display: inline-flex; flex-direction: column; align-items: center; width: 104rpx; padding: 18rpx 0; margin: 0 4rpx; border-radius: 16rpx; background: #f7f8fa; transition: all .2s ease; }
.date-item.active { background: #1677ff; box-shadow: 0 6rpx 16rpx rgba(22, 119, 255, .18); }
.date-item.active .date-week, .date-item.active .date-day { color: #fff; }
.date-week { font-size: 22rpx; color: #86909c; }
.date-day { margin-top: 8rpx; font-size: 26rpx; font-weight: 600; color: #1f2329; }

.section-head { display: flex; align-items: center; justify-content: space-between; margin: 32rpx 0 20rpx; font-size: 29rpx; font-weight: 600; color: #1f2329; }
.section-note { font-size: 23rpx; font-weight: 400; color: #86909c; }
.closed-tip { padding: 36rpx 0; text-align: center; color: #ff7d00; font-size: 26rpx; }

/* 时长选项：参考图的描边按钮，选中项仅用品牌蓝强调 */
.dur-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.dur-item { width: calc((100% - 48rpx) / 4); box-sizing: border-box; text-align: center; padding: 20rpx 0; border: 2rpx solid #e5e8ef; border-radius: 12rpx; background: #fff; font-size: 25rpx; color: #1f2329; transition: all .2s ease; }
.dur-item.active { border-color: #1677ff; background: #eef5ff; color: #1677ff; font-weight: 600; }
.dur-item.disabled { border-color: #f0f1f5; background: #f5f6fa; color: #c9cdd4; text-decoration: line-through; }

.pkg-tabs { display: flex; gap: 12rpx; margin-bottom: 20rpx; }
.pkg-tab { flex: 1; text-align: center; padding: 14rpx 0; border-radius: 12rpx; background: #f7f8fa; font-size: 25rpx; color: #4e5969; }
.pkg-tab.active { background: #eef5ff; color: #1677ff; font-weight: 600; }
.pkg-item { display: flex; align-items: center; padding: 22rpx; border: 2rpx solid #edf0f5; border-radius: 14rpx; margin-bottom: 16rpx; transition: all .2s ease; }
.pkg-item.active { border-color: #1677ff; background: #f5f9ff; }
.pkg-info { flex: 1; min-width: 0; }
.pkg-name { font-size: 28rpx; font-weight: 600; color: #1f2329; }
.pkg-desc { margin-top: 6rpx; font-size: 23rpx; color: #86909c; }
.pkg-price { text-align: right; padding-left: 16rpx; }
.pkg-now { font-size: 32rpx; font-weight: 700; color: #1677ff; }
.pkg-origin { margin-left: 10rpx; font-size: 22rpx; color: #c9cdd4; text-decoration: line-through; }
.pkg-empty { padding: 36rpx 0; text-align: center; color: #86909c; font-size: 26rpx; }

/* 起止时间：开始时间可选，结束时间由时长或套餐自动生成 */
.time-section { margin-top: 30rpx; padding-top: 2rpx; border-top: 2rpx solid #f3f4f6; }
.time-picker-row { display: flex; align-items: center; }
.time-picker { flex: 1; min-width: 0; }
.time-field { height: 132rpx; box-sizing: border-box; padding: 20rpx 22rpx; border: 2rpx solid #e5e8ef; border-radius: 16rpx; background: #f8f9fb; }
.time-field.end { flex: 1; min-width: 0; }
.time-label { font-size: 22rpx; color: #86909c; }
.time-value { margin-top: 10rpx; font-size: 34rpx; line-height: 1.2; font-weight: 600; color: #1f2329; }
.time-field.placeholder .time-value { font-size: 27rpx; font-weight: 400; color: #a9b0bc; }
.time-arrow { width: 56rpx; flex-shrink: 0; text-align: center; font-size: 30rpx; color: #a9b0bc; }
.time-tip { margin-top: 16rpx; font-size: 22rpx; line-height: 1.5; color: #86909c; }

.footer-bar { position: fixed; z-index: 20; left: 0; right: 0; bottom: 0; display: flex; align-items: center; padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom)); background: #fff; box-shadow: 0 -6rpx 24rpx rgba(31, 35, 41, .06); }
.footer-info { flex: 1; min-width: 0; padding-right: 20rpx; }
.footer-sum { font-size: 23rpx; line-height: 1.4; color: #4e5969; }
.footer-hint { font-size: 25rpx; color: #86909c; }
.footer-price { margin-top: 4rpx; font-size: 22rpx; color: #4e5969; }
.footer-price text { margin-left: 6rpx; font-size: 34rpx; font-weight: 700; color: #1677ff; }
.footer-btn { width: 220rpx; height: 84rpx; line-height: 84rpx; margin: 0; padding: 0; background: #1677ff; color: #fff; font-size: 29rpx; border-radius: 42rpx; }
.footer-btn::after { border: none; }
.footer-btn.disabled { background: #c9cdd4; }

.loading { padding: 120rpx 0; text-align: center; color: #86909c; }
</style>
