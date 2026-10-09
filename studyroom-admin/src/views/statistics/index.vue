<template>
  <div class="page">
    <!-- FR-AD-019~023 数据统计：时间范围 + 四大主题 -->
    <div class="toolbar">
      <el-date-picker
        v-model="range"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        :clearable="false"
        style="width: 260px"
        @change="loadTab"
      />
      <el-radio-group v-model="quick" size="default" @change="applyQuick">
        <el-radio-button label="7">近 7 天</el-radio-button>
        <el-radio-button label="30">近 30 天</el-radio-button>
        <el-radio-button label="90">近 90 天</el-radio-button>
      </el-radio-group>
      <div style="flex: 1" />
      <el-button type="primary" :loading="exporting" @click="handleExport">
        导出报表
      </el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="loadTab">
      <!-- ========== 预约统计 FR-AD-019 ========== -->
      <el-tab-pane label="预约统计" name="booking">
        <el-row :gutter="16">
          <el-col :span="14">
            <el-card shadow="never">
              <template #header>预约时段分布（24 小时）</template>
              <div ref="hoursChartRef" class="chart-lg" />
            </el-card>
          </el-col>
          <el-col :span="10">
            <el-card shadow="never">
              <template #header>套餐占比</template>
              <div ref="pkgChartRef" class="chart-lg" />
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- ========== 座位统计 FR-AD-020 ========== -->
      <el-tab-pane label="座位统计" name="seat">
        <el-row :gutter="16">
          <el-col :span="14">
            <el-card shadow="never">
              <template #header>上座率趋势（%）</template>
              <div ref="occChartRef" class="chart-lg" />
            </el-card>
          </el-col>
          <el-col :span="10">
            <el-card shadow="never">
              <template #header>座位类型占用分布</template>
              <div ref="seatTypeChartRef" class="chart-lg" />
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- ========== 营收统计 FR-AD-021 ========== -->
      <el-tab-pane label="营收统计" name="revenue">
        <el-row :gutter="16" style="margin-bottom: 16px">
          <el-col :span="10">
            <el-card shadow="never">
              <template #header>收入构成（按套餐）</template>
              <div ref="revCompChartRef" class="chart-md" />
            </el-card>
          </el-col>
          <el-col :span="14">
            <el-card shadow="never">
              <template #header>退款趋势（金额）</template>
              <div ref="refundTrendChartRef" class="chart-md" />
            </el-card>
          </el-col>
        </el-row>
        <el-card shadow="never">
          <template #header>客单价趋势（元 / 单量）</template>
          <div ref="aovChartRef" class="chart-md" />
        </el-card>
      </el-tab-pane>

      <!-- ========== 用户活跃度 FR-AD-022 ========== -->
      <el-tab-pane label="用户活跃度" name="user">
        <el-row :gutter="16" style="margin-bottom: 16px">
          <el-col :span="14">
            <el-card shadow="never">
              <template #header>DAU 与每日新增</template>
              <div ref="dauChartRef" class="chart-md" />
            </el-card>
          </el-col>
          <el-col :span="10">
            <el-card shadow="never">
              <template #header>学习时长分布</template>
              <div ref="durChartRef" class="chart-md" />
            </el-card>
          </el-col>
        </el-row>
        <el-card shadow="never">
          <template #header>注册留存（逐日：新增 / 次日仍活跃）</template>
          <el-table :data="retentionRows" size="default" border max-height="360">
            <el-table-column prop="date" label="注册日期" width="130" />
            <el-table-column prop="newCount" label="新增用户" width="110" />
            <el-table-column prop="retainedCount" label="次日留存" width="110" />
            <el-table-column prop="retentionRate" label="留存率">
              <template #default="{ row }">{{ row.retentionRate }}%
            </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import {
  bookingHours, packageShare, occupancyTrend, seatTypeDistribution,
  revenueComposition, refundTrend, avgOrderValue,
  userActiveTrend, durationHistogram, retention, exportStats
} from '@/api/statistics'

/* ---------- 时间范围 ---------- */
const quick = ref('30')
const range = ref([])

function applyQuick(days) {
  const end = new Date()
  const begin = new Date(Date.now() - (days - 1) * 86400000)
  const fmt = (d) => d.toISOString().slice(0, 10)
  range.value = [fmt(begin), fmt(end)]
  loadTab()
}

function params() {
  return { beginDate: range.value[0], endDate: range.value[1] }
}

/* ---------- 图表实例 ---------- */
const hoursChartRef = ref()
const pkgChartRef = ref()
const occChartRef = ref()
const seatTypeChartRef = ref()
const revCompChartRef = ref()
const refundTrendChartRef = ref()
const aovChartRef = ref()
const dauChartRef = ref()
const durChartRef = ref()

const charts = reactive({})
const retentionRows = ref([])
const exporting = ref(false)
const activeTab = ref('booking')

function ensureChart(key, refEl) {
  if (!refEl.value) return null
  if (!charts[key]) charts[key] = echarts.init(refEl.value)
  return charts[key]
}

function renderBar(key, refEl, data, name, color) {
  const chart = ensureChart(key, refEl)
  if (!chart) return
  chart.setOption({
    grid: { left: 50, right: 20, top: 30, bottom: 40 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map((d) => d.name || d) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', name, data: data.map((d) => d.value), itemStyle: { color }, barMaxWidth: 30 }]
  }, true)
}

function renderPie(key, refEl, data) {
  const chart = ensureChart(key, refEl)
  if (!chart) return
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{ type: 'pie', radius: ['35%', '65%'], data, label: { formatter: '{b}\n{d}%' } }]
  }, true)
}

function renderTrend(key, refEl, trend, primaryName, secondaryName, primaryColor, secondaryColor) {
  const chart = ensureChart(key, refEl)
  if (!chart) return
  const option = {
    grid: { left: 55, right: secondaryName ? 55 : 25, top: 35, bottom: 40 },
    tooltip: { trigger: 'axis' },
    legend: secondaryName ? { data: [primaryName, secondaryName] } : undefined,
    xAxis: { type: 'category', data: trend.dates },
    yAxis: [{ type: 'value', name: primaryName }],
    series: [{ name: primaryName, type: 'line', smooth: true, data: trend.primary, itemStyle: { color: primaryColor } }]
  }
  if (secondaryName) {
    option.yAxis.push({ type: 'value', name: secondaryName, splitLine: { show: false } })
    option.series.push({ name: secondaryName, type: 'bar', yAxisIndex: 1, data: trend.secondary, itemStyle: { color: secondaryColor } })
  }
  chart.setOption(option, true)
}

/* ---------- 各 Tab 数据加载 ---------- */
const loaders = {
  async booking() {
    await nextTick()
    const [hours, share] = await Promise.all([bookingHours(params()), packageShare(params())])
    renderBar('hours', hoursChartRef, hours.data, '预约量', '#409EFF')
    renderPie('pkg', pkgChartRef, share.data)
  },
  async seat() {
    await nextTick()
    const [trend, dist] = await Promise.all([occupancyTrend(params()), seatTypeDistribution(params())])
    renderTrend('occ', occChartRef, trend.data, '上座率%', null, '#67C23A')
    renderPie('seatType', seatTypeChartRef, dist.data)
  },
  async revenue() {
    await nextTick()
    const [comp, rTrend, aov] = await Promise.all([
      revenueComposition(params()), refundTrend(params()), avgOrderValue(params())
    ])
    renderPie('revComp', revCompChartRef, comp.data)
    renderTrend('refundTrend', refundTrendChartRef, rTrend.data, '退款金额', null, '#F56C6C')
    renderTrend('aov', aovChartRef, aov.data, '客单价', '订单量', '#E6A23C', '#409EFF')
  },
  async user() {
    await nextTick()
    const [dau, dur, ret] = await Promise.all([
      userActiveTrend(params()), durationHistogram(params()), retention(params())
    ])
    renderTrend('dau', dauChartRef, dau.data, 'DAU', '每日新增', '#409EFF', '#67C23A')
    renderBar('dur', durChartRef, dur.data, '人数', '#9B59B6')
    retentionRows.value = ret.data || []
  }
}

async function loadTab() {
  if (!range.value || range.value.length !== 2) return
  try {
    await loaders[activeTab.value]()
  } catch (e) {
    // request 拦截器已统一提示
  }
}

/* ---------- 导出 FR-AD-023 ---------- */
async function handleExport() {
  if (!range.value || range.value.length !== 2) return
  exporting.value = true
  try {
    const blob = await exportStats(params())
    const url = URL.createObjectURL(new Blob([blob], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    }))
    const a = document.createElement('a')
    a.href = url
    a.download = `经营统计_${range.value[0]}_${range.value[1]}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

/* ---------- 生命周期 ---------- */
const handleResize = () => Object.values(charts).forEach((c) => c && c.resize())

onMounted(() => {
  applyQuick('30')
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  Object.values(charts).forEach((c) => c && c.dispose())
})
</script>

<style scoped>
.page { padding: 4px; }
.toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 16px; }
.chart-lg { height: 360px; }
.chart-md { height: 300px; }
</style>
