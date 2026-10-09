<template>
  <div class="page">
    <!-- FR-AD-024 财务总览：汇总卡片 + 营收/退款趋势 + 退款明细 + 导出 -->
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
        @change="loadAll"
      />
      <el-radio-group v-model="quick" @change="applyQuick">
        <el-radio-button label="7">近 7 天</el-radio-button>
        <el-radio-button label="30">近 30 天</el-radio-button>
        <el-radio-button label="90">近 90 天</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 汇总卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="6" v-for="card in summaryCards" :key="card.label">
        <el-card shadow="never">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图 -->
    <el-card shadow="never" style="margin-bottom: 16px">
      <template #header>营收与退款趋势（元）</template>
      <div ref="trendChartRef" class="chart" />
    </el-card>

    <!-- 退款记录 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>退款记录</span>
          <div class="filters">
            <el-select v-model="query.type" placeholder="退款类型" clearable style="width: 150px" @change="loadRefunds">
              <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
            <el-input v-model="query.keyword" placeholder="订单号" clearable style="width: 180px" @keyup.enter="loadRefunds" />
            <el-button type="primary" plain @click="loadRefunds">查询</el-button>
            <el-button type="success" plain :loading="exporting" @click="handleExport">导出 Excel</el-button>
          </div>
        </div>
      </template>
      <el-table :data="refunds" v-loading="loading" border stripe>
        <el-table-column prop="createTime" label="退款时间" width="160">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="nickname" label="用户" width="130" />
        <el-table-column prop="typeDesc" label="退款类型" width="120" />
        <el-table-column prop="amount" label="退款金额" width="110">
          <template #default="{ row }">
            <span style="color: #F56C6C">¥{{ row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
      </el-table>
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        layout="total, prev, pager, next, sizes"
        :page-sizes="[10, 20, 50]"
        style="margin-top: 14px; justify-content: flex-end"
        @current-change="loadRefunds"
        @size-change="loadRefunds"
      />
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { financeSummary, refundPage, refundExport } from '@/api/finance'

const quick = ref('30')
const range = ref([])
const summary = ref({})
const refunds = ref([])
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)
const trendChartRef = ref()
let trendChart = null

const query = reactive({ type: null, keyword: '', page: 1, size: 20 })

const typeOptions = [
  { value: 1, label: '阶梯取消退款' },
  { value: 2, label: '提前离场退差' },
  { value: 3, label: '申诉退款' },
  { value: 4, label: '管理补偿' },
  { value: 5, label: '手动退款' }
]

const summaryCards = computed(() => [
  { label: '总营收', value: `¥${summary.value.totalRevenue ?? 0}`, color: '#409EFF' },
  { label: '总退款', value: `¥${summary.value.totalRefund ?? 0}`, color: '#F56C6C' },
  { label: '净营收', value: `¥${summary.value.netRevenue ?? 0}`, color: '#67C23A' },
  { label: `客单价（${summary.value.orderCount ?? 0} 单）`, value: `¥${summary.value.avgOrderAmount ?? 0}`, color: '#E6A23C' }
])

function applyQuick(days) {
  const end = new Date()
  const begin = new Date(Date.now() - (days - 1) * 86400000)
  const fmt = (d) => d.toISOString().slice(0, 10)
  range.value = [fmt(begin), fmt(end)]
  loadAll()
}

function fmtTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '-'
}

async function loadSummary() {
  const { data } = await financeSummary({ beginDate: range.value[0], endDate: range.value[1] })
  summary.value = data
  await nextTick()
  if (!trendChart && trendChartRef.value) trendChart = echarts.init(trendChartRef.value)
  const trend = data.dailyTrend || []
  trendChart && trendChart.setOption({
    grid: { left: 60, right: 20, top: 40, bottom: 40 },
    tooltip: { trigger: 'axis' },
    legend: { data: ['营收', '退款', '净营收'] },
    xAxis: { type: 'category', data: trend.map((d) => d.date) },
    yAxis: { type: 'value' },
    series: [
      { name: '营收', type: 'line', smooth: true, data: trend.map((d) => d.revenue), itemStyle: { color: '#409EFF' } },
      { name: '退款', type: 'line', smooth: true, data: trend.map((d) => d.refund), itemStyle: { color: '#F56C6C' } },
      { name: '净营收', type: 'bar', data: trend.map((d) => d.net), itemStyle: { color: '#67C23A' } }
    ]
  }, true)
}

async function loadRefunds() {
  loading.value = true
  try {
    const { data } = await refundPage({
      beginDate: range.value[0], endDate: range.value[1],
      type: query.type, keyword: query.keyword,
      page: query.page, size: query.size
    })
    refunds.value = data.list || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const blob = await refundExport({
      beginDate: range.value[0], endDate: range.value[1],
      type: query.type, keyword: query.keyword
    })
    const url = URL.createObjectURL(new Blob([blob], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    }))
    const a = document.createElement('a')
    a.href = url
    a.download = `退款记录_${range.value[0]}_${range.value[1]}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

function loadAll() {
  loadSummary()
  loadRefunds()
}

const handleResize = () => trendChart && trendChart.resize()

onMounted(() => {
  applyQuick('30')
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart && trendChart.dispose()
})
</script>

<style scoped>
.page { padding: 4px; }
.toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 16px; }
.stat-label { color: #909399; font-size: 13px; margin-bottom: 8px; }
.stat-value { font-size: 24px; font-weight: 600; }
.chart { height: 320px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.filters { display: flex; gap: 10px; }
</style>
