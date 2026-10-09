<template>
  <div class="appeal-page">
    <!-- 筛选栏 FR-AD-011 -->
    <el-card shadow="never" class="filter-card">
      <el-form inline @submit.prevent>
        <el-form-item label="申诉状态">
          <el-radio-group v-model="query.status" @change="onSearch">
            <el-radio-button :label="null">全部</el-radio-button>
            <el-radio-button :label="0">待审核</el-radio-button>
            <el-radio-button :label="1">已通过</el-radio-button>
            <el-radio-button :label="2">已驳回</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button :icon="Refresh" @click="onSearch">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 申诉列表 FR-AD-011 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="170" fixed />
        <el-table-column prop="nickname" label="用户" min-width="110" show-overflow-tooltip />
        <el-table-column prop="storeName" label="门店" min-width="110" show-overflow-tooltip />
        <el-table-column prop="seatNo" label="座位" width="80" />
        <el-table-column label="预约开始" width="160">
          <template #default="{ row }">{{ fmt(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="申诉原因" width="130">
          <template #default="{ row }">{{ row.reasonTypeDesc }}</template>
        </el-table-column>
        <el-table-column prop="description" label="申诉说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="凭证" width="90">
          <template #default="{ row }">
            <el-image
              v-if="row.images && row.images.length"
              :src="row.images[0]"
              :preview-src-list="row.images"
              fit="cover"
              class="thumb"
            >
              <template #error>
                <span class="muted">无法加载</span>
              </template>
            </el-image>
            <span v-else class="muted">无</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="160">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button type="success" link @click="openHandle(row, 'approve')">签到成功</el-button>
              <el-button type="danger" link @click="openHandle(row, 'reject')">驳回</el-button>
            </template>
            <el-button v-else type="primary" link @click="openDetail(row)">详情</el-button>
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

    <!-- 处理弹窗 FR-AD-011：标记签到成功 / 驳回 -->
    <el-dialog
      v-model="handleVisible"
      :title="handleMode === 'approve' ? '申诉处理：标记签到成功' : '申诉处理：驳回'"
      width="480px"
      destroy-on-close
    >
      <el-alert
        v-if="handleMode === 'approve'"
        title="通过后：订单仍有效将补记签到并转进行中；爽约订单将撤销爽约记录并恢复信用分。"
        type="success"
        :closable="false"
        class="handle-tip"
      />
      <el-alert
        v-else
        title="驳回后将通过站内消息告知用户处理意见，处理意见必填。"
        type="warning"
        :closable="false"
        class="handle-tip"
      />
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="订单号" :span="2">{{ current?.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ current?.nickname }}</el-descriptions-item>
        <el-descriptions-item label="座位">{{ current?.seatNo }}</el-descriptions-item>
        <el-descriptions-item label="申诉原因" :span="2">{{ current?.reasonTypeDesc }}</el-descriptions-item>
      </el-descriptions>
      <el-input
        v-model="handleRemark"
        type="textarea"
        :rows="3"
        maxlength="200"
        show-word-limit
        :placeholder="handleMode === 'approve' ? '处理意见（可选）' : '处理意见（必填，10~200 字更佳）'"
        class="handle-input"
      />
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button
          :type="handleMode === 'approve' ? 'success' : 'danger'"
          :loading="handling"
          @click="onHandleSubmit"
        >
          {{ handleMode === 'approve' ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 FR-AD-011 -->
    <el-drawer v-model="detailVisible" title="申诉详情" size="420px">
      <template v-if="current">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="订单号">{{ current.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">{{ current.orderStatusDesc }}</el-descriptions-item>
          <el-descriptions-item label="用户">{{ current.nickname }}（ID: {{ current.userId }}）</el-descriptions-item>
          <el-descriptions-item label="门店/座位">{{ current.storeName }} / {{ current.seatNo }}</el-descriptions-item>
          <el-descriptions-item label="预约开始">{{ fmt(current.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="申诉原因">{{ current.reasonTypeDesc }}</el-descriptions-item>
          <el-descriptions-item label="申诉说明">{{ current.description || '—' }}</el-descriptions-item>
          <el-descriptions-item label="处理状态">{{ current.statusDesc }}</el-descriptions-item>
          <el-descriptions-item label="处理意见">{{ current.handleRemark || '—' }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{ fmt(current.handleTime) }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ fmt(current.createTime) }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="current.images && current.images.length" class="img-list">
          <el-image
            v-for="(img, i) in current.images"
            :key="i"
            :src="img"
            :preview-src-list="current.images"
            fit="cover"
            class="img-item"
          />
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
/**
 * 管理端-申诉处理 —— FR-AD-011
 *
 * - 申诉列表（状态筛选 + 分页）：查看申诉详情 + 凭证图片
 * - 标记签到成功：已预约订单补签到转进行中 / 爽约订单撤销爽约恢复信用
 * - 驳回：处理意见必填，站内消息通知用户
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { pageAppeals, approveAppeal, rejectAppeal } from '@/api/order'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 20, status: null })

const handleVisible = ref(false)
const handleMode = ref('approve')
const handleRemark = ref('')
const handling = ref(false)
const current = ref(null)
const detailVisible = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await pageAppeals({ ...query, status: query.status ?? null })
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

function openHandle(row, mode) {
  current.value = row
  handleMode.value = mode
  handleRemark.value = ''
  handleVisible.value = true
}

function openDetail(row) {
  current.value = row
  detailVisible.value = true
}

async function onHandleSubmit() {
  const remark = handleRemark.value.trim()
  if (handleMode.value === 'reject' && !remark) {
    ElMessage.warning('驳回时必须填写处理意见')
    return
  }
  handling.value = true
  try {
    if (handleMode.value === 'approve') {
      await approveAppeal(current.value.appealId, remark || null)
      ElMessage.success('已标记签到成功')
    } else {
      await rejectAppeal(current.value.appealId, remark)
      ElMessage.success('已驳回该申诉')
    }
    handleVisible.value = false
    load()
  } finally {
    handling.value = false
  }
}

function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '—'
}

function statusTag(s) {
  return s === 0 ? 'warning' : s === 1 ? 'success' : 'info'
}

onMounted(load)
</script>

<style scoped>
.filter-card {
  margin-bottom: 12px;
}
.muted {
  color: #909399;
  font-size: 12px;
}
.thumb {
  width: 42px;
  height: 42px;
  border-radius: 4px;
  cursor: pointer;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
.handle-tip {
  margin-bottom: 12px;
}
.handle-input {
  margin-top: 12px;
}
.img-list {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.img-item {
  width: 90px;
  height: 90px;
  border-radius: 6px;
  cursor: pointer;
}
</style>
