<template>
  <div class="page">
    <!-- FR-AD-028 操作日志：筛选 + 表格 + 详情 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>管理员操作日志（保留 90 天，每日凌晨自动清理）</span>
          <div class="filters">
            <el-date-picker
              v-model="range"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 250px"
              @change="load"
            />
            <el-input v-model="query.username" placeholder="管理员账号" clearable style="width: 140px" @keyup.enter="load" />
            <el-input v-model="query.module" placeholder="模块" clearable style="width: 120px" @keyup.enter="load" />
            <el-select v-model="query.status" placeholder="状态" clearable style="width: 100px" @change="load">
              <el-option label="成功" :value="1" />
              <el-option label="失败" :value="0" />
            </el-select>
            <el-button type="primary" plain @click="load">查询</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="createTime" label="时间" width="160">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="110" />
        <el-table-column prop="operation" label="操作" min-width="160" show-overflow-tooltip />
        <el-table-column prop="requestUrl" label="请求" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costMs" label="耗时" width="90">
          <template #default="{ row }">{{ row.costMs }}ms</template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top: 14px; justify-content: flex-end"
        @current-change="load"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="操作日志详情" width="640px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="操作人">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ fmtTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ detail.module }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status === 1 ? '成功' : '失败' }}</el-descriptions-item>
        <el-descriptions-item label="方法" :span="2">{{ detail.method }}</el-descriptions-item>
        <el-descriptions-item label="请求 URL" :span="2">{{ detail.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail.ip }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.costMs }}ms</el-descriptions-item>
        <el-descriptions-item label="User-Agent" :span="2">{{ detail.userAgent || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求参数（脱敏）" :span="2">
          <pre class="params">{{ detail.params || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.errorMsg" label="错误信息" :span="2">
          <span style="color: #F56C6C">{{ detail.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { pageLogs } from '@/api/log'

const range = ref([])
const query = reactive({ username: '', module: '', status: null, page: 1, size: 20 })
const list = ref([])
const total = ref(0)
const loading = ref(false)

const detailVisible = ref(false)
const detail = ref({})

function fmtTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 19) : '-'
}

async function load() {
  loading.value = true
  try {
    const { data } = await pageLogs({
      beginDate: range.value?.[0] || null,
      endDate: range.value?.[1] || null,
      username: query.username || null,
      module: query.module || null,
      status: query.status,
      page: query.page,
      size: query.size
    })
    list.value = data.list || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.page { padding: 4px; }
.card-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.filters { display: flex; gap: 10px; flex-wrap: wrap; }
.params { margin: 0; white-space: pre-wrap; word-break: break-all; font-size: 12px; }
</style>
