<template>
  <div class="dashboard">
    <!-- ============ FR-AD-002 核心指标卡片 ============ -->
    <el-row :gutter="16" class="card-row">
      <el-col v-for="card in cards" :key="card.key" :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric">
            <div class="metric-icon" :style="{ background: card.bg, color: card.color }">
              <el-icon :size="26"><component :is="card.icon" /></el-icon>
            </div>
            <div class="metric-body">
              <div class="metric-label">{{ card.label }}</div>
              <div class="metric-value">
                {{ card.value }}<span v-if="card.unit" class="metric-unit">{{ card.unit }}</span>
              </div>
              <div class="metric-sub">
                <template v-if="card.delta !== null">
                  <span :class="['delta', card.delta >= 0 ? 'up' : 'down']">
                    {{ card.delta >= 0 ? '↑' : '↓' }}{{ Math.abs(card.delta) }}%
                  </span>
                  <span class="yesterday">较昨日</span>
                </template>
                <span v-else class="yesterday">{{ card.sub }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- ============ FR-AD-003 趋势折线图（7天/30天） ============ -->
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">预约量与营收趋势</span>
              <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
                <el-radio-button :label="7">近 7 天</el-radio-button>
                <el-radio-button :label="30">近 30 天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="trend-chart" v-loading="trendLoading" />
        </el-card>
      </el-col>

      <!-- ============ FR-AD-003 座位状态总览网格 ============ -->
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">座位状态总览</span>
              <span class="card-extra">共 {{ seatTotal }} 个座位</span>
            </div>
          </template>
          <div v-loading="seatLoading" class="seat-grid">
            <div v-for="item in seatStatusList" :key="item.status" class="seat-cell">
              <div class="seat-dot" :style="{ background: item.color }" />
              <div class="seat-count" :style="{ color: item.color }">{{ item.count }}</div>
              <div class="seat-name">{{ item.name }}</div>
              <div class="seat-percent">{{ item.percent }}%</div>
            </div>
          </div>
          <div class="seat-legend">
            上座率 = 使用中 ÷ 可用座位（总数 − 维护中），当前可用 {{ summary?.seatUsable ?? '-' }} 个
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ============ FR-AD-003 座位利用率 Top10 ============ -->
    <el-card shadow="never" class="top-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">座位利用率 Top10</span>
          <span class="card-extra">近 30 天 · 按累计使用时长排序</span>
        </div>
      </template>
      <el-table :data="seatTopList" v-loading="topLoading" stripe>
        <el-table-column label="排名" width="70" align="center">
          <template #default="{ $index }">
            <span :class="['rank', `rank-${$index + 1}`]">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="seatNo" label="座位号" min-width="100" />
        <el-table-column prop="typeName" label="类型" min-width="100" />
        <el-table-column prop="orderCount" label="订单数" min-width="80" align="center" />
        <el-table-column label="累计使用时长" min-width="120" align="center">
          <template #default="{ row }">{{ row.usedHours }} 小时</template>
        </el-table-column>
        <el-table-column label="营收" min-width="110" align="right">
          <template #default="{ row }">¥{{ row.revenue }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 管理端仪表盘 —— FR-AD-002（核心指标卡片）/ FR-AD-003（趋势图表）
 *
 * - 四张指标卡：今日预约量 / 营收 / 当前上座率 / 活跃用户（较昨日环比）
 * - 趋势折线图：ECharts 双 Y 轴（预约量柱 + 营收折线），7/30 天切换
 * - 座位状态总览：六色状态实时快照网格（FR-UC-017 同色系）
 * - 座位利用率 Top10：近 30 天累计使用时长排行
 */
import { computed, onBeforeUnmount, onMounted, reactive, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getSummary, getTrend, getSeatStatus, getSeatTop } from '@/api/dashboard'

/* ==================== 座位六色状态字典（FR-UC-017） ==================== */
const SEAT_STATUS_META = {
  0: { name: '空闲', color: '#67C23A' },
  1: { name: '使用中', color: '#409EFF' },
  2: { name: '维护中', color: '#E6A23C' },
  3: { name: '打扫中', color: '#F2C94C' },
  4: { name: '锁定', color: '#F56C6C' }
}

/* ==================== 状态 ==================== */
const summary = ref(null)
const trendDays = ref(7)
const trendLoading = ref(false)
const seatLoading = ref(false)
const topLoading = ref(false)
const seatTopList = ref([])
const trendChartRef = ref(null)
let trendChart = null

/* ==================== 环比计算（昨日为 0 时：今日>0 记 100%） ==================== */
function deltaPercent(today, yesterday) {
  const t = Number(today) || 0
  const y = Number(yesterday) || 0
  if (y === 0) return t > 0 ? 100 : 0
  return Math.round(((t - y) / y) * 100)
}

/* ==================== FR-AD-002 指标卡片 ==================== */
const cards = computed(() => {
  const s = summary.value
  if (!s) return []
  const rate = s.seatUsable > 0 ? (s.seatInUse / s.seatUsable) * 100 : 0
  return [
    {
      key: 'orders',
      label: '今日预约量',
      value: s.todayOrders,
      icon: 'Calendar',
      color: '#409EFF',
      bg: '#ecf5ff',
      delta: deltaPercent(s.todayOrders, s.yesterdayOrders)
    },
    {
      key: 'revenue',
      label: '今日营收',
      value: `¥${Number(s.todayRevenue).toFixed(2)}`,
      icon: 'Wallet',
      color: '#F56C6C',
      bg: '#fef0f0',
      delta: deltaPercent(s.todayRevenue, s.yesterdayRevenue)
    },
    {
      key: 'occupancy',
      label: '当前上座率',
      value: rate.toFixed(1),
      unit: '%',
      icon: 'DataLine',
      color: '#67C23A',
      bg: '#f0f9eb',
      delta: null,
      sub: `使用中 ${s.seatInUse} / 可用 ${s.seatUsable}`
    },
    {
      key: 'active',
      label: '今日活跃用户',
      value: s.activeUsers,
      icon: 'User',
      color: '#E6A23C',
      bg: '#fdf6ec',
      delta: deltaPercent(s.activeUsers, s.yesterdayActiveUsers)
    }
  ]
})

/* ==================== 座位状态总览 ==================== */
const seatTotal = computed(() =>
  seatStatusList.value.reduce((sum, i) => sum + i.count, 0)
)
const seatStatusList = ref([])

/* ==================== FR-AD-003 趋势折线图 ==================== */
async function loadTrend() {
  trendLoading.value = true
  try {
    const list = await getTrend(trendDays.value)
    renderTrendChart(list || [])
  } finally {
    trendLoading.value = false
  }
}

function renderTrendChart(list) {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  trendChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['预约量', '营收'], top: 0 },
    grid: { left: 10, right: 14, top: 36, bottom: 6, containLabel: true },
    xAxis: {
      type: 'category',
      data: list.map((i) => i.date.slice(5)),
      axisLabel: { color: '#909399' },
      axisLine: { lineStyle: { color: '#DCDFE6' } }
    },
    yAxis: [
      {
        type: 'value',
        name: '预约量',
        minInterval: 1,
        axisLabel: { color: '#909399' },
        splitLine: { lineStyle: { type: 'dashed', color: '#EBEEF5' } }
      },
      {
        type: 'value',
        name: '营收(元)',
        axisLabel: { color: '#909399', formatter: '{value}' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '预约量',
        type: 'bar',
        barMaxWidth: 18,
        itemStyle: { color: '#409EFF', borderRadius: [3, 3, 0, 0] },
        data: list.map((i) => i.orderCount)
      },
      {
        name: '营收',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: { color: '#F56C6C' },
        lineStyle: { width: 2, color: '#F56C6C' },
        data: list.map((i) => Number(i.revenue))
      }
    ]
  })
}

/* ==================== 数据加载 ==================== */
async function loadSummary() {
  summary.value = await getSummary()
}

async function loadSeatStatus() {
  seatLoading.value = true
  try {
    const list = await getSeatStatus()
    const total = (list || []).reduce((sum, i) => sum + Number(i.count), 0) || 1
    seatStatusList.value = (list || []).map((i) => ({
      status: i.status,
      count: i.count,
      ...(SEAT_STATUS_META[i.status] || { name: '未知', color: '#909399' }),
      percent: ((Number(i.count) / total) * 100).toFixed(1)
    }))
  } finally {
    seatLoading.value = false
  }
}

async function loadSeatTop() {
  topLoading.value = true
  try {
    seatTopList.value = (await getSeatTop(30, 10)) || []
  } finally {
    topLoading.value = false
  }
}

function handleResize() {
  trendChart && trendChart.resize()
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  // 指标卡与座位状态并行加载
  loadSummary()
  loadSeatStatus()
  loadSeatTop()
  await nextTick()
  loadTrend()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
})
</script>

<style scoped>
.dashboard {
  padding: 4px;
}

.card-row {
  margin-bottom: 16px;
}

/* ============ 指标卡片 ============ */
.metric-card :deep(.el-card__body) {
  padding: 18px 20px;
}

.metric {
  display: flex;
  align-items: center;
  gap: 14px;
}

.metric-icon {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.metric-label {
  font-size: 13px;
  color: #909399;
}

.metric-value {
  font-size: 26px;
  font-weight: 600;
  color: #303133;
  line-height: 1.3;
  margin-top: 2px;
}

.metric-unit {
  font-size: 14px;
  margin-left: 2px;
}

.metric-sub {
  font-size: 12px;
  margin-top: 2px;
}

.delta.up {
  color: #f56c6c;
  font-weight: 600;
}

.delta.down {
  color: #67c23a;
  font-weight: 600;
}

.yesterday {
  color: #c0c4cc;
  margin-left: 6px;
}

/* ============ 通用卡片头 ============ */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.card-extra {
  font-size: 12px;
  color: #909399;
}

.chart-card {
  margin-bottom: 16px;
}

/* ============ 趋势图 ============ */
.trend-chart {
  height: 340px;
  width: 100%;
}

/* ============ 座位状态网格 ============ */
.seat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  min-height: 220px;
  align-content: center;
}

.seat-cell {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px 10px;
  text-align: center;
}

.seat-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin: 0 auto 6px;
}

.seat-count {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}

.seat-name {
  font-size: 13px;
  color: #606266;
  margin-top: 2px;
}

.seat-percent {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 2px;
}

.seat-legend {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}

/* ============ Top10 ============ */
.rank {
  display: inline-block;
  width: 22px;
  height: 22px;
  line-height: 22px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
  color: #909399;
  background: #f4f4f5;
}

.rank-1 {
  color: #fff;
  background: #f56c6c;
}

.rank-2 {
  color: #fff;
  background: #e6a23c;
}

.rank-3 {
  color: #fff;
  background: #409eff;
}

@media (max-width: 992px) {
  .seat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
