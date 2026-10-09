<template>
  <div class="review-page">
    <!-- 筛选栏 FR-AD-017：评分 / 时间 / 状态 -->
    <el-card shadow="never" class="filter-card">
      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="评分">
          <el-select v-model="query.score" clearable placeholder="全部" style="width: 110px">
            <el-option v-for="s in [5, 4, 3, 2, 1]" :key="s" :label="`${s} 星`" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="评价时间">
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
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 110px">
            <el-option label="显示中" :value="1" />
            <el-option label="已隐藏" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="评价内容" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 评价表格 FR-AD-017 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="评价" min-width="280">
          <template #default="{ row }">
            <div class="review-cell">
              <div class="review-head">
                <el-rate :model-value="row.score" disabled size="small" />
                <el-tag v-if="row.anonymous === 1" size="small" effect="plain" type="info">匿名</el-tag>
                <span class="muted">{{ fmt(row.createTime) }}</span>
              </div>
              <div v-if="row.tags?.length" class="tags">
                <el-tag v-for="t in row.tags" :key="t" size="small" effect="plain">{{ t }}</el-tag>
              </div>
              <div class="content">{{ row.content }}</div>
              <div v-if="row.images?.length" class="images">
                <el-image
                  v-for="(img, i) in row.images"
                  :key="i"
                  :src="img"
                  :preview-src-list="row.images"
                  :initial-index="i"
                  fit="cover"
                  class="thumb"
                  preview-teleported
                />
              </div>
              <div v-if="row.replyContent" class="reply">
                <span class="reply-label">已回复：</span>{{ row.replyContent }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="用户" width="130">
          <template #default="{ row }">
            <div>{{ row.nickname }}</div>
            <div class="muted">ID: {{ row.userId }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单号" min-width="160" show-overflow-tooltip />
        <el-table-column label="门店/座位" min-width="130">
          <template #default="{ row }">
            <div>{{ row.storeName || '—' }}</div>
            <div class="muted">{{ row.seatNo || '按门店评价' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="86">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '显示中' : '已隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openReply(row)">
              {{ row.replyContent ? '编辑回复' : '回复' }}
            </el-button>
            <el-button v-if="row.status === 1" type="warning" link @click="onToggle(row, true)">
              隐藏
            </el-button>
            <el-button v-else type="success" link @click="onToggle(row, false)">显示</el-button>
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

    <!-- 回复弹窗 FR-AD-018：10-200 字，已有回复则编辑 -->
    <el-dialog
      v-model="replyVisible"
      :title="replyTarget?.replyContent ? '编辑回复' : '回复评价'"
      width="560px"
      destroy-on-close
    >
      <div v-if="replyTarget" class="reply-dialog">
        <div class="review-quote">
          <div class="review-head">
            <el-rate :model-value="replyTarget.score" disabled size="small" />
            <span class="muted">{{ replyTarget.nickname }} · {{ fmt(replyTarget.createTime) }}</span>
          </div>
          <div class="content">{{ replyTarget.content }}</div>
        </div>
        <el-input
          v-model="replyContent"
          type="textarea"
          :rows="4"
          maxlength="200"
          show-word-limit
          placeholder="输入回复内容（10-200 字），将展示在用户端评价下方"
        />
        <div v-if="replyTarget.replyContent" class="muted old-reply">
          当前回复：{{ replyTarget.replyContent }}
        </div>
      </div>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="onReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { pageReviews, replyReview, hideReview, showReview } from '@/api/review'

defineOptions({ name: 'ReviewList' })

const query = reactive({ score: null, status: null, keyword: '', page: 1, size: 20 })
const dateRange = ref(null)
const list = ref([])
const total = ref(0)
const loading = ref(false)

const replyVisible = ref(false)
const replying = ref(false)
const replyTarget = ref(null)
const replyContent = ref('')

const fmt = (v) => (v ? String(v).replace('T', ' ').slice(0, 16) : '')

const params = computed(() => ({
  ...query,
  beginDate: dateRange.value?.[0] || undefined,
  endDate: dateRange.value?.[1] || undefined
}))

const load = async () => {
  loading.value = true
  try {
    const { data } = await pageReviews(params.value)
    list.value = data.list || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

const onSearch = () => {
  query.page = 1
  load()
}

const onReset = () => {
  query.score = null
  query.status = null
  query.keyword = ''
  dateRange.value = null
  onSearch()
}

/* ---------- 回复 FR-AD-018 ---------- */

const openReply = (row) => {
  replyTarget.value = row
  replyContent.value = row.replyContent || ''
  replyVisible.value = true
}

const onReply = async () => {
  const content = replyContent.value.trim()
  if (content.length < 10 || content.length > 200) {
    ElMessage.warning('回复内容需 10-200 字')
    return
  }
  replying.value = true
  try {
    const { data } = await replyReview(replyTarget.value.id, content)
    // 局部更新，避免整页刷新
    const idx = list.value.findIndex((r) => r.id === data.id)
    if (idx >= 0) list.value[idx] = data
    ElMessage.success('回复成功')
    replyVisible.value = false
  } finally {
    replying.value = false
  }
}

/* ---------- 隐藏/显示 FR-AD-018 ---------- */

const onToggle = async (row, hide) => {
  const action = hide ? '隐藏' : '显示'
  try {
    await ElMessageBox.confirm(
      hide
        ? '隐藏后该评价在用户端门店评价列表中不可见，确定隐藏吗？'
        : '确定将该评价恢复显示吗？',
      `${action}评价`,
      { type: hide ? 'warning' : 'info', confirmButtonText: action, cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await (hide ? hideReview(row.id) : showReview(row.id))
  row.status = hide ? 0 : 1
  ElMessage.success(`已${action}`)
}

onMounted(load)
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}
.muted {
  color: #909399;
  font-size: 12px;
}
.review-cell {
  padding: 4px 0;
}
.review-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  margin-top: 4px;
}
.content {
  margin-top: 6px;
  font-size: 13px;
  color: #303133;
  line-height: 1.6;
  white-space: pre-wrap;
}
.images {
  display: flex;
  gap: 6px;
  margin-top: 6px;
  flex-wrap: wrap;
}
.thumb {
  width: 56px;
  height: 56px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}
.reply {
  margin-top: 8px;
  padding: 8px 10px;
  background: #ecf5ff;
  border-radius: 4px;
  font-size: 12px;
  color: #409eff;
}
.reply-label {
  font-weight: 600;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.reply-dialog .review-quote {
  padding: 10px 12px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 12px;
}
.old-reply {
  margin-top: 8px;
}
</style>
