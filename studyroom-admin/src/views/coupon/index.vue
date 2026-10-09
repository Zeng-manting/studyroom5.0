<template>
  <div class="page">
    <!-- FR-AD-027 优惠券配置：列表 + 新建/编辑 + 定向发放 + 上下架 + 领取明细 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>优惠券管理</span>
          <div class="filters">
            <el-select v-model="query.type" placeholder="券类型" clearable style="width: 140px" @change="load">
              <el-option label="新人券" :value="1" />
              <el-option label="时长抵扣券" :value="2" />
              <el-option label="满减券" :value="3" />
            </el-select>
            <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px" @change="load">
              <el-option label="上架" :value="1" />
              <el-option label="下架" :value="0" />
            </el-select>
            <el-button type="primary" plain @click="load">查询</el-button>
            <el-button type="primary" @click="openSave()">新建优惠券</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="name" label="券名称" min-width="150" />
        <el-table-column prop="typeDesc" label="类型" width="110" />
        <el-table-column label="优惠内容" min-width="170">
          <template #default="{ row }">{{ couponText(row) }}</template>
        </el-table-column>
        <el-table-column label="有效期" width="150">
          <template #default="{ row }">{{ validText(row) }}</template>
        </el-table-column>
        <el-table-column label="领取 / 发行" width="120">
          <template #default="{ row }">
            {{ row.claimCount }} / {{ row.totalQuota === 0 ? '不限' : row.totalQuota }}
          </template>
        </el-table-column>
        <el-table-column prop="usedCount" label="已使用" width="80" />
        <el-table-column prop="perUserLimit" label="限领" width="70" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openSave(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'info' : 'success'" @click="toggle(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button link type="warning" @click="openGrant(row)">发放</el-button>
            <el-button link type="success" @click="openClaims(row)">领取明细</el-button>
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

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="saveVisible" :title="form.id ? '编辑优惠券' : '新建优惠券'" width="560px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="券名称" required>
          <el-input v-model="form.name" maxlength="64" placeholder="如：新客立减 5 元" />
        </el-form-item>
        <el-form-item label="券类型" required>
          <el-radio-group v-model="form.type">
            <el-radio :label="1">新人券</el-radio>
            <el-radio :label="2">时长抵扣券</el-radio>
            <el-radio :label="3">满减券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优惠方式" required>
          <el-radio-group v-model="form.discountType">
            <el-radio :label="1">定额</el-radio>
            <el-radio :label="2">折扣</el-radio>
            <el-radio :label="3">满减</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="form.discountType === 3">
          <el-form-item label="满减门槛(元)" required>
            <el-input-number v-model="form.thresholdAmount" :min="0" :precision="2" />
          </el-form-item>
        </template>
        <template v-if="form.discountType === 1 || form.discountType === 3">
          <el-form-item :label="form.discountType === 3 ? '减免金额(元)' : '抵扣金额(元)'" required>
            <el-input-number v-model="form.discountAmount" :min="0" :precision="2" />
          </el-form-item>
        </template>
        <template v-if="form.discountType === 2">
          <el-form-item label="折扣比例" required>
            <el-input-number v-model="form.discountRatio" :min="0.01" :max="1" :step="0.05" :precision="2" />
            <span class="tip">如 0.85 = 85 折</span>
          </el-form-item>
        </template>
        <el-form-item label="有效期">
          <el-radio-group v-model="validMode">
            <el-radio label="days">领取后有效天数</el-radio>
            <el-radio label="fixed">固定时间段</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="validMode === 'days'" label="有效天数">
          <el-input-number v-model="form.validDays" :min="1" :max="365" />
        </el-form-item>
        <template v-else>
          <el-form-item label="起止时间">
            <el-date-picker
              v-model="fixedRange"
              type="datetimerange"
              value-format="YYYY-MM-DDTHH:mm:ss"
              start-placeholder="起"
              end-placeholder="止"
            />
          </el-form-item>
        </template>
        <el-form-item label="总发行量">
          <el-input-number v-model="form.totalQuota" :min="0" />
          <span class="tip">0 = 不限量</span>
        </el-form-item>
        <el-form-item label="每人限领">
          <el-input-number v-model="form.perUserLimit" :min="1" />
        </el-form-item>
        <el-form-item label="使用说明">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
        <el-form-item label="上架">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 发放弹窗 -->
    <el-dialog v-model="grantVisible" title="定向发放优惠券" width="480px">
      <p style="margin-bottom: 12px">
        将向以下用户发放「{{ grantTarget.name }}」，已领取达限领数的用户将自动跳过。
      </p>
      <el-input
        v-model="grantUserIds"
        type="textarea"
        :rows="6"
        placeholder="输入用户 ID，多个用逗号或换行分隔，如：1001, 1002, 1003"
      />
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" :loading="granting" @click="submitGrant">确认发放</el-button>
      </template>
    </el-dialog>

    <!-- 领取明细抽屉 -->
    <el-drawer v-model="claimsVisible" :title="`领取明细 - ${claimsTarget?.name || ''}`" size="600px">
      <el-table :data="claims" border stripe size="default">
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="statusDesc" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'info' : 'primary'" size="small">
              {{ row.statusDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="claimTime" label="领取时间" width="160">
          <template #default="{ row }">{{ fmtTime(row.claimTime) }}</template>
        </el-table-column>
        <el-table-column prop="expireTime" label="过期时间" width="160">
          <template #default="{ row }">{{ fmtTime(row.expireTime) }}</template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageCoupons, saveCoupon, toggleCouponStatus, grantCoupon, listClaims } from '@/api/coupon'

const query = reactive({ type: null, status: null, page: 1, size: 20 })
const list = ref([])
const total = ref(0)
const loading = ref(false)

const saveVisible = ref(false)
const saving = ref(false)
const validMode = ref('days')
const fixedRange = ref([])
const form = reactive({})

const grantVisible = ref(false)
const granting = ref(false)
const grantTarget = ref({})
const grantUserIds = ref('')

const claimsVisible = ref(false)
const claimsTarget = ref({})
const claims = ref([])

function couponText(row) {
  if (row.discountType === 2) return `${(row.discountRatio * 10).toFixed(1)} 折`
  if (row.discountType === 3) return `满 ${row.thresholdAmount} 减 ${row.discountAmount}`
  return `抵扣 ¥${row.discountAmount}`
}

function validText(row) {
  if (row.validStart) {
    return `${fmtDate(row.validStart)} ~ ${fmtDate(row.validEnd)}`
  }
  return `领取后 ${row.validDays} 天`
}

function fmtDate(t) {
  return t ? String(t).replace('T', ' ').slice(0, 10) : '-'
}

function fmtTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '-'
}

async function load() {
  loading.value = true
  try {
    const { data } = await pageCoupons(query)
    list.value = data.list || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

/* ---------- 新建/编辑 ---------- */
function openSave(row) {
  Object.assign(form, {
    id: row?.id || null,
    name: row?.name || '',
    type: row?.type || 1,
    discountType: row?.discountType || 1,
    thresholdAmount: row?.thresholdAmount ?? 0,
    discountAmount: row?.discountAmount ?? 0,
    discountRatio: row?.discountRatio ?? 0.85,
    validDays: row?.validDays ?? 30,
    totalQuota: row?.totalQuota ?? 0,
    perUserLimit: row?.perUserLimit ?? 1,
    description: row?.description || '',
    status: row?.status ?? 1
  })
  validMode.value = row?.validStart ? 'fixed' : 'days'
  fixedRange.value = row?.validStart ? [row.validStart, row.validEnd] : []
  saveVisible.value = true
}

async function submitSave() {
  if (!form.name) return ElMessage.warning('请填写券名称')
  saving.value = true
  try {
    const payload = { ...form }
    if (validMode.value === 'fixed' && fixedRange.value?.length === 2) {
      payload.validStart = fixedRange.value[0]
      payload.validEnd = fixedRange.value[1]
    } else {
      payload.validStart = null
      payload.validEnd = null
    }
    await saveCoupon(payload)
    ElMessage.success('保存成功')
    saveVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

/* ---------- 上下架 ---------- */
async function toggle(row) {
  const online = row.status !== 1
  await ElMessageBox.confirm(
    `确定将「${row.name}」${online ? '上架' : '下架'}吗？${online ? '' : '下架后用户端不可再领取，已领取的继续可用。'}`,
    '提示', { type: 'warning' }
  )
  await toggleCouponStatus(row.id, online)
  ElMessage.success(online ? '已上架' : '已下架')
  load()
}

/* ---------- 定向发放 ---------- */
function openGrant(row) {
  grantTarget.value = row
  grantUserIds.value = ''
  grantVisible.value = true
}

async function submitGrant() {
  const ids = grantUserIds.value.split(/[\s,，]+/).filter((s) => /^\d+$/.test(s)).map(Number)
  if (!ids.length) return ElMessage.warning('请输入至少一个有效的用户 ID')
  granting.value = true
  try {
    const { data } = await grantCoupon({ couponId: grantTarget.value.id, userIds: ids })
    ElMessage.success(`发放完成：成功 ${data} 人（超限/无效用户已跳过）`)
    grantVisible.value = false
    load()
  } finally {
    granting.value = false
  }
}

/* ---------- 领取明细 ---------- */
async function openClaims(row) {
  claimsTarget.value = row
  claimsVisible.value = true
  const { data } = await listClaims(row.id)
  claims.value = data || []
}

onMounted(load)
</script>

<style scoped>
.page { padding: 4px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.filters { display: flex; gap: 10px; }
.tip { margin-left: 10px; color: #909399; font-size: 12px; }
</style>
