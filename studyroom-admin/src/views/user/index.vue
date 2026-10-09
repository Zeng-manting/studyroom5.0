<template>
  <div class="user-page">
    <!-- 筛选栏 FR-AD-014 -->
    <el-card shadow="never" class="filter-card">
      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="昵称">
          <el-input v-model="query.keyword" placeholder="模糊匹配" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="账号状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="信用状态">
          <el-select v-model="query.creditStatus" clearable placeholder="全部" style="width: 120px">
            <el-option label="正常" :value="0" />
            <el-option label="受限" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户表格 FR-AD-014 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="用户" min-width="180" fixed>
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.avatar">{{ row.nickname?.[0] || '?' }}</el-avatar>
              <div>
                <div class="nickname">{{ row.nickname || '未设置昵称' }}</div>
                <div class="muted">ID: {{ row.id }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="手机号" width="130">
          <template #default="{ row }">{{ row.phoneMasked || '—' }}</template>
        </el-table-column>
        <el-table-column label="邮箱" width="160">
          <template #default="{ row }">
            <span class="muted">{{ row.emailMasked || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="余额" width="100">
          <template #default="{ row }">¥{{ row.balance ?? '0.00' }}</template>
        </el-table-column>
        <el-table-column label="信用" width="90">
          <template #default="{ row }">
            <span :class="{ 'credit-low': row.creditScore < 80 }">{{ row.creditScore ?? '—' }}</span>
            <el-tag v-if="row.creditStatus === 1" type="danger" size="small" effect="plain" class="tag-gap">
              受限
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单数" prop="orderCount" width="80" />
        <el-table-column label="学习时长" width="100">
          <template #default="{ row }">{{ fmtHours(row.totalStudyMinutes) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="160">
          <template #default="{ row }">{{ fmt(row.lastLoginTime) || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 1" type="danger" link @click="onToggle(row, true)">
              禁用
            </el-button>
            <el-button v-else type="success" link @click="onToggle(row, false)">启用</el-button>
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

    <!-- 用户详情抽屉 FR-AD-014：基本信息 + 学习记录 + 评价记录 -->
    <el-drawer v-model="detailVisible" title="用户详情" size="720px" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="昵称" :span="2">
              {{ detail.baseInfo.nickname || '未设置' }}
            </el-descriptions-item>
            <el-descriptions-item label="账号状态">
              <el-tag :type="detail.baseInfo.status === 1 ? 'success' : 'danger'" size="small">
                {{ detail.baseInfo.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="手机号">{{ detail.baseInfo.phoneMasked || '—' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ detail.baseInfo.emailMasked || '—' }}</el-descriptions-item>
            <el-descriptions-item label="性别">
              {{ ['未知', '男', '女'][detail.baseInfo.gender ?? 0] }}
            </el-descriptions-item>
            <el-descriptions-item label="余额">¥{{ detail.baseInfo.balance ?? '0.00' }}</el-descriptions-item>
            <el-descriptions-item label="信用分">{{ detail.baseInfo.creditScore ?? '—' }}</el-descriptions-item>
            <el-descriptions-item label="信用状态">
              {{ detail.baseInfo.creditStatus === 1 ? '受限' : '正常' }}
            </el-descriptions-item>
            <el-descriptions-item label="累计订单">{{ detail.baseInfo.orderCount }} 笔</el-descriptions-item>
            <el-descriptions-item label="累计学习">
              {{ fmtHours(detail.baseInfo.totalStudyMinutes) }}
            </el-descriptions-item>
            <el-descriptions-item label="注册时间">
              {{ fmt(detail.baseInfo.createTime) }}
            </el-descriptions-item>
          </el-descriptions>

          <h4 class="section-title">学习记录（最近 20 笔）</h4>
          <el-table :data="detail.studyRecords" size="small" max-height="320" border>
            <el-table-column prop="orderNo" label="订单号" min-width="160" show-overflow-tooltip />
            <el-table-column prop="storeName" label="门店" min-width="90" show-overflow-tooltip />
            <el-table-column prop="seatNo" label="座位" width="70" />
            <el-table-column prop="packageName" label="套餐" min-width="90" show-overflow-tooltip />
            <el-table-column label="时段" min-width="170">
              <template #default="{ row }">
                <div class="time-cell">{{ fmt(row.startTime) }}</div>
                <div class="time-cell muted">至 {{ fmt(row.endTime) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="86">
              <template #default="{ row }">
                <el-tag size="small" :type="orderTag(row.status)">{{ row.statusDesc }}</el-tag>
                <el-tag v-if="row.noShow" type="danger" size="small" effect="plain">爽约</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="实付" width="80">
              <template #default="{ row }">¥{{ row.payAmount }}</template>
            </el-table-column>
            <el-table-column label="用时" width="70">
              <template #default="{ row }">
                {{ row.actualDurationMinutes ? row.actualDurationMinutes + '分' : '—' }}
              </template>
            </el-table-column>
          </el-table>

          <h4 class="section-title">评价记录</h4>
          <el-empty v-if="!detail.reviewRecords.length" description="暂无评价" :image-size="60" />
          <div v-else class="review-list">
            <div v-for="r in detail.reviewRecords" :key="r.reviewId" class="review-item">
              <div class="review-head">
                <el-rate :model-value="r.score" disabled size="small" />
                <span class="muted">{{ r.storeName || '门店' }} · {{ fmt(r.createTime) }}</span>
                <el-tag v-if="r.status === 0" type="info" size="small">已隐藏</el-tag>
              </div>
              <div class="review-content">{{ r.content }}</div>
              <div v-if="r.replyContent" class="review-reply">商家回复：{{ r.replyContent }}</div>
            </div>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { pageUsers, getUserDetail, disableUser, enableUser } from '@/api/user'

defineOptions({ name: 'UserList' })

const query = reactive({ keyword: '', status: null, creditStatus: null, page: 1, size: 20 })
const list = ref([])
const total = ref(0)
const loading = ref(false)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const fmt = (v) => (v ? String(v).replace('T', ' ').slice(0, 16) : '')

/** 分钟 → x小时y分 */
const fmtHours = (minutes) => {
  if (!minutes) return '0小时'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h ? `${h}小时${m ? m + '分' : ''}` : `${m}分`
}

const orderTag = (status) =>
  ({ 0: 'info', 1: 'primary', 2: 'warning', 3: 'success', 4: 'info', 5: 'warning', 6: 'info', 7: 'danger' })[status] || 'info'

const load = async () => {
  loading.value = true
  try {
    const { data } = await pageUsers(query)
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
  query.keyword = ''
  query.status = null
  query.creditStatus = null
  onSearch()
}

const openDetail = async (row) => {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const { data } = await getUserDetail(row.id)
    detail.value = data
  } finally {
    detailLoading.value = false
  }
}

/** 禁用/启用 FR-AD-014：禁用前二次确认（后端还会校验进行中订单） */
const onToggle = async (row, disable) => {
  const action = disable ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      disable
        ? `确定禁用用户「${row.nickname || row.id}」吗？禁用后该用户将无法登录使用。`
        : `确定启用用户「${row.nickname || row.id}」吗？`,
      `${action}账号`,
      { type: disable ? 'warning' : 'info', confirmButtonText: action, cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await (disable ? disableUser(row.id) : enableUser(row.id))
    ElMessage.success(`${action}成功`)
    load()
  } catch (e) {
    // 后端校验失败（如存在进行中订单）展示具体原因
    ElMessage.error(e?.message || `${action}失败`)
  }
}

onMounted(load)
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.nickname {
  font-weight: 500;
}
.muted {
  color: #909399;
  font-size: 12px;
}
.time-cell {
  line-height: 1.4;
  font-size: 12px;
}
.credit-low {
  color: #e6a23c;
  font-weight: 600;
}
.tag-gap {
  margin-left: 4px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.section-title {
  margin: 20px 0 10px;
  font-size: 14px;
  color: #303133;
}
.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.review-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}
.review-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.review-content {
  margin-top: 6px;
  font-size: 13px;
  color: #303133;
}
.review-reply {
  margin-top: 8px;
  padding: 8px 10px;
  background: #ecf5ff;
  border-radius: 4px;
  font-size: 12px;
  color: #409eff;
}
</style>
