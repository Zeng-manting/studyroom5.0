<template>
  <div class="order-page">
    <!-- 筛选栏 FR-AD-010：时间 / 状态 / 座位号 / 用户 -->
    <el-card shadow="never" class="filter-card">
      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="下单时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 130px">
            <el-option v-for="(label, s) in STATUS" :key="s" :label="label" :value="Number(s)" />
          </el-select>
        </el-form-item>
        <el-form-item label="座位号">
          <el-input v-model="query.seatNo" placeholder="模糊匹配" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="query.userId" placeholder="精确匹配" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="订单号/昵称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
          <el-button type="success" :icon="Download" :loading="exporting" @click="onExport">
            导出 Excel
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 订单表格 FR-AD-010 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="170" fixed />
        <el-table-column prop="nickname" label="用户" min-width="110" show-overflow-tooltip />
        <el-table-column prop="storeName" label="门店" min-width="110" show-overflow-tooltip />
        <el-table-column prop="seatNo" label="座位" width="80" />
        <el-table-column prop="packageName" label="套餐" min-width="100" show-overflow-tooltip />
        <el-table-column label="预约时段" min-width="200">
          <template #default="{ row }">
            <div class="time-cell">{{ fmt(row.startTime) }}</div>
            <div class="time-cell muted">至 {{ fmt(row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="实付/退款" width="130">
          <template #default="{ row }">
            <div>¥{{ row.payAmount ?? '0.00' }}</div>
            <div v-if="row.refundAmount > 0" class="refund-cell">退 ¥{{ row.refundAmount }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.statusDesc }}</el-tag>
            <el-tag v-if="row.noShow" type="danger" size="small" effect="plain" class="tag-gap">
              爽约
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payTypeDesc" label="支付方式" width="90" />
        <el-table-column prop="createTime" label="下单时间" width="160">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1 || row.status === 3"
              type="primary"
              link
              @click="openRefund(row)"
            >
              退款
            </el-button>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pager"
        @current-change="load"
        @size-change="onSearch"
      />
    </el-card>

    <!-- 退款审核弹窗 FR-AD-012 -->
    <el-dialog v-model="refundVisible" title="退款审核" width="480px" destroy-on-close>
      <div v-loading="refundLoading">
        <template v-if="preview">
          <el-descriptions :column="2" border size="small" class="refund-desc">
            <el-descriptions-item label="订单号" :span="2">{{ preview.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="用户">{{ preview.nickname }}</el-descriptions-item>
            <el-descriptions-item label="订单状态">{{ preview.statusDesc }}</el-descriptions-item>
            <el-descriptions-item label="实付金额">¥{{ preview.payAmount }}</el-descriptions-item>
            <el-descriptions-item label="已退金额">¥{{ preview.refundedAmount }}</el-descriptions-item>
            <el-descriptions-item label="可退上限">¥{{ preview.refundableAmount }}</el-descriptions-item>
            <el-descriptions-item label="系统建议">
              <span class="suggest">¥{{ preview.suggestedAmount }}</span>
            </el-descriptions-item>
          </el-descriptions>
          <el-alert :title="preview.suggestionDesc" type="info" :closable="false" class="refund-tip" />

          <el-form label-width="80px">
            <el-form-item label="退款金额">
              <el-input-number
                v-model="refundForm.amount"
                :min="0.01"
                :max="Number(preview.refundableAmount)"
                :precision="2"
                :step="1"
                style="width: 200px"
              />
            </el-form-item>
            <el-form-item label="退款原因" required>
              <el-input
                v-model="refundForm.reason"
                type="textarea"
                :rows="2"
                maxlength="200"
                show-word-limit
                placeholder="必填，将通知用户并落退款流水"
              />
            </el-form-item>
          </el-form>
        </template>
      </div>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="refundSubmitting"
          :disabled="!preview || !preview.refundable"
          @click="onRefundSubmit"
        >
          确认退款
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 管理端-订单列表 —— FR-AD-010 / FR-AD-012 / FR-AD-013
 *
 * - 多条件筛选：时间范围 / 状态 / 座位号 / 用户ID / 关键字（订单号、昵称）
 * - 分页默认 20 条（与需求一致）
 * - 已预约 / 已完成订单可发起退款审核（系统建议金额 + 手动调整）
 * - 导出 Excel：与列表同条件，浏览器直接下载
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import { pageOrders, exportOrders, refundPreview, submitRefund } from '@/api/order'

const STATUS = {
  0: '待支付', 1: '已预约', 2: '进行中', 3: '已完成',
  4: '已取消', 5: '退款中', 6: '已退款', 7: '申诉中'
}
const STATUS_TAG = { 0: 'info', 1: 'primary', 2: 'success', 3: 'success', 4: 'info', 5: 'warning', 6: 'warning', 7: 'danger' }

const loading = ref(false)
const exporting = ref(false)
const list = ref([])
const total = ref(0)
const dateRange = ref(null)
const query = reactive({
  page: 1,
  size: 20,
  status: null,
  seatNo: '',
  userId: '',
  keyword: ''
})

const refundVisible = ref(false)
const refundLoading = ref(false)
const refundSubmitting = ref(false)
const preview = ref(null)
const refundForm = reactive({ amount: 0, reason: '' })

function buildParams() {
  return {
    ...query,
    userId: query.userId || null,
    status: query.status ?? null,
    beginDate: dateRange.value?.[0] || null,
    endDate: dateRange.value?.[1] || null
  }
}

async function load() {
  loading.value = true
  try {
    const data = await pageOrders(buildParams())
    list.value = data.list || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  load()
}

function onReset() {
  dateRange.value = null
  query.status = null
  query.seatNo = ''
  query.userId = ''
  query.keyword = ''
  onSearch()
}

/** FR-AD-013：导出 Excel（Blob 下载） */
async function onExport() {
  exporting.value = true
  try {
    const blob = await exportOrders(buildParams())
    const url = URL.createObjectURL(
      blob instanceof Blob ? blob : new Blob([blob])
    )
    const a = document.createElement('a')
    a.href = url
    a.download = `订单导出_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

/** FR-AD-012：打开退款弹窗（先拉系统建议金额） */
async function openRefund(row) {
  refundVisible.value = true
  refundLoading.value = true
  preview.value = null
  try {
    const data = await refundPreview(row.orderId)
    preview.value = data
    refundForm.amount = Number(data.suggestedAmount) > 0 ? Number(data.suggestedAmount) : undefined
    refundForm.reason = ''
  } finally {
    refundLoading.value = false
  }
}

async function onRefundSubmit() {
  if (!refundForm.amount || refundForm.amount <= 0) {
    ElMessage.warning('请填写退款金额')
    return
  }
  if (!refundForm.reason?.trim()) {
    ElMessage.warning('请填写退款原因')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认退款 ¥${Number(refundForm.amount).toFixed(2)} 至用户余额？`,
      '退款确认',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  refundSubmitting.value = true
  try {
    await submitRefund(preview.value.orderId, refundForm.amount, refundForm.reason.trim())
    ElMessage.success('退款成功，已回充用户余额并通知')
    refundVisible.value = false
    load()
  } finally {
    refundSubmitting.value = false
  }
}

function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '—'
}

function statusTag(s) {
  return STATUS_TAG[s] || 'info'
}

onMounted(load)
</script>

<style scoped>
.filter-card {
  margin-bottom: 12px;
}
.time-cell {
  font-size: 13px;
  line-height: 1.5;
}
.muted {
  color: #909399;
  font-size: 12px;
}
.refund-cell {
  color: #e6a23c;
  font-size: 12px;
}
.tag-gap {
  margin-left: 4px;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
.refund-desc {
  margin-bottom: 10px;
}
.refund-tip {
  margin-bottom: 14px;
}
.suggest {
  color: #f56c6c;
  font-weight: 600;
}
</style>
