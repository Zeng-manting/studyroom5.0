<template>
  <div class="blacklist-page">
    <!-- 门店选择 -->
    <el-card shadow="never" class="filter-card">
      <div class="selector">
        <span class="label">门店：</span>
        <el-select v-model="storeId" style="width: 240px" placeholder="选择门店" @change="loadAll">
          <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-button type="primary" :icon="Plus" :disabled="!storeId" class="gap" @click="openEdit(null)">
          新增规则
        </el-button>
      </div>
    </el-card>

    <el-row :gutter="12">
      <!-- 规则列表 -->
      <el-col :span="14">
        <el-card shadow="never" v-loading="loading">
          <template #header>
            <div class="rule-header">
              <span>时段黑名单规则（FR-AD-005）</span>
              <el-radio-group v-model="ruleFilter" size="small">
                <el-radio-button label="all">全部</el-radio-button>
                <el-radio-button label="active">生效中</el-radio-button>
                <el-radio-button label="expired">已过期</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <el-table :data="displayRules" stripe size="small">
            <el-table-column label="类型" width="72">
              <template #default="{ row }">
                <el-tag :type="row.type === 1 ? 'primary' : 'warning'" size="small">
                  {{ row.typeDesc }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="生效日期" min-width="130">
              <template #default="{ row }">
                <div v-if="row.type === 1">{{ row.weekDaysDesc }}</div>
                <div v-else>{{ row.singleDate }}</div>
              </template>
            </el-table-column>
            <el-table-column label="时段" width="130">
              <template #default="{ row }">
                {{ row.startTime ? `${row.startTime.slice(0, 5)}-${row.endTime.slice(0, 5)}` : '全天' }}
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" min-width="110" show-overflow-tooltip />
            <el-table-column label="状态" width="84">
              <template #default="{ row }">
                <el-tag v-if="isExpired(row)" type="info" size="small">已过期</el-tag>
                <el-tag v-else :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.statusDesc }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
                <el-button type="danger" link size="small" @click="onDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && !displayRules.length" description="暂无时段黑名单规则" :image-size="60" />
        </el-card>
      </el-col>

      <!-- 月历视图 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <div class="cal-header">
              <span>日历视图</span>
              <div>
                <el-button :icon="ArrowLeft" circle size="small" @click="shiftMonth(-1)" />
                <span class="cal-month">{{ monthLabel }}</span>
                <el-button :icon="ArrowRight" circle size="small" @click="shiftMonth(1)" />
              </div>
            </div>
          </template>
          <div class="calendar" v-loading="calLoading">
            <div class="cal-week" v-for="w in WEEK_NAMES" :key="w">{{ w }}</div>
            <div
              v-for="(cell, i) in calendarCells"
              :key="i"
              class="cal-day"
              :class="{
                empty: !cell,
                past: cell && cell.date < todayStr,
                closed: cell && cell.allDayClosed,
                partial: cell && !cell.allDayClosed && cell.slots.length,
                today: cell && cell.today
              }"
              :title="cell ? (cell.date < todayStr ? '过去日期不可设置时段黑名单' : dayTitle(cell)) : ''"
              @click="cell && cell.date >= todayStr && openAddForDate(cell)"
            >
              <span v-if="cell" class="day-num">{{ cell.date.slice(8) }}</span>
              <span v-if="cell && cell.allDayClosed" class="day-mark">闭</span>
              <span v-else-if="cell && cell.slots.length" class="day-mark partial-mark">
                {{ cell.slots.length }}段
              </span>
            </div>
          </div>
          <div class="legend">
            <span class="legend-item"><i class="dot closed-dot" />全天不可预约</span>
            <span class="legend-item"><i class="dot partial-dot" />部分时段不可预约</span>
            <span class="legend-item"><i class="dot today-dot" />今日</span>
          </div>
          <div class="cal-hint">点击今天及之后的日期可为该日期新增时段黑名单规则</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 规则编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑时段黑名单规则' : '新增时段黑名单规则'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="规则类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio-button :label="1">周期性</el-radio-button>
            <el-radio-button :label="2">单次</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.type === 1" label="生效周几" prop="weekDays">
          <el-checkbox-group v-model="weekSelection">
            <el-checkbox v-for="(name, i) in WEEK_NAMES" :key="name" :label="i + 1">{{ name }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item v-if="form.type === 2" label="生效日期" prop="singleDate">
          <el-date-picker v-model="form.singleDate" type="date" value-format="YYYY-MM-DD"
                          placeholder="选择日期" :disabled-date="isPastDate" />
        </el-form-item>
        <el-form-item label="禁用时段">
          <div class="time-row">
            <el-time-select v-model="form.startTime" start="00:00" step="00:30" end="23:30" placeholder="开始（空=全天）" />
            <span class="muted">至</span>
            <el-time-select v-model="form.endTime" :start="form.startTime || '00:30'" step="00:30" end="23:59" placeholder="结束" />
          </div>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="form.reason" maxlength="255" placeholder="如：设备检修 / 法定节假日" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 管理端-时段黑名单设置 —— FR-AD-005
 * 周期性规则（按周几）+ 单次规则（按日期），右侧月历视图展示当月命中情况。
 * 规则同时驱动用户端时段置灰（FR-UC-020）与下单后端校验。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import {
  addBlacklistRule, deleteBlacklistRule, getBlacklistCalendar,
  listBlacklistRules, listStores, updateBlacklistRule
} from '@/api/store'

const WEEK_NAMES = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

/** 本地日期 yyyy-MM-dd（避免 toISOString 的时区偏移） */
function fmtDate(d) {
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

const todayStr = fmtDate(new Date())

/** 单次规则的日期选择：禁选今天之前的日期 */
function isPastDate(d) {
  const t = new Date(d)
  t.setHours(0, 0, 0, 0)
  const now = new Date()
  now.setHours(0, 0, 0, 0)
  return t.getTime() < now.getTime()
}

const stores = ref([])
const storeId = ref(null)
const loading = ref(false)
const rules = ref([])
const editVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)

const month = ref(null) // 'YYYY-MM'
const calendar = ref([])
const calLoading = ref(false)

/** 规则筛选：all 全部 / active 生效中 / expired 已过期 */
const ruleFilter = ref('all')

/** 单次规则且日期早于今天视为已过期（周期性规则不会过期） */
function isExpired(row) {
  return row.type === 2 && row.singleDate && row.singleDate < todayStr
}

const displayRules = computed(() => {
  if (ruleFilter.value === 'expired') return rules.value.filter(isExpired)
  if (ruleFilter.value === 'active') return rules.value.filter(r => !isExpired(r))
  return rules.value
})

const weekSelection = ref([])

const form = reactive({
  id: null, type: 1, weekDays: '', singleDate: null,
  startTime: '', endTime: '', reason: '', status: 1
})

const formRules = {
  type: [{ required: true, message: '请选择规则类型' }],
  weekDays: [{
    validator: (r, v, cb) => {
      if (form.type === 1 && !weekSelection.value.length) cb(new Error('周期性规则必须选择周几'))
      else cb()
    },
    trigger: 'change'
  }],
  singleDate: [{
    validator: (r, v, cb) => {
      if (form.type === 2 && !form.singleDate) cb(new Error('请选择生效日期'))
      else cb()
    },
    trigger: 'change'
  }]
}

const monthLabel = computed(() => month.value || '')
const calendarCells = computed(() => {
  if (!calendar.value.length) return []
  const first = calendar.value[0].date
  const jsDay = new Date(first + 'T00:00:00').getDay() // 0=周日
  const offset = (jsDay + 6) % 7 // 周一为首列
  return [...Array(offset).fill(null), ...calendar.value]
})

async function loadStores() {
  stores.value = await listStores()
  if (stores.value.length && !storeId.value) {
    storeId.value = stores.value[0].id
    loadAll()
  }
}

async function loadRules() {
  if (!storeId.value) return
  loading.value = true
  try {
    rules.value = await listBlacklistRules(storeId.value)
  } finally {
    loading.value = false
  }
}

async function loadCalendar() {
  if (!storeId.value) return
  calLoading.value = true
  try {
    calendar.value = await getBlacklistCalendar(storeId.value, month.value)
  } finally {
    calLoading.value = false
  }
}

function loadAll() {
  loadRules()
  loadCalendar()
}

function shiftMonth(delta) {
  const [y, m] = (month.value || new Date().toISOString().slice(0, 7)).split('-').map(Number)
  const d = new Date(y, m - 1 + delta, 1)
  month.value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
  loadCalendar()
}

function openAddForDate(cell) {
  openEdit(null)
  form.type = 2
  form.singleDate = cell.date
}

function openEdit(row) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      type: row.type,
      weekDays: row.weekDays || '',
      singleDate: row.singleDate,
      startTime: row.startTime ? row.startTime.slice(0, 5) : '',
      endTime: row.endTime ? row.endTime.slice(0, 5) : '',
      reason: row.reason || '',
      status: row.status ?? 1
    })
    weekSelection.value = row.weekDays ? row.weekDays.split(',').map(Number) : []
  } else {
    Object.assign(form, {
      id: null, type: 1, weekDays: '', singleDate: null,
      startTime: '', endTime: '', reason: '', status: 1
    })
    weekSelection.value = []
  }
  editVisible.value = true
}

async function onSave() {
  await formRef.value.validate()
  if (form.startTime && form.endTime && form.startTime >= form.endTime) {
    ElMessage.warning('开始时间必须早于结束时间')
    return
  }
  saving.value = true
  try {
    const payload = {
      type: form.type,
      weekDays: form.type === 1 ? weekSelection.value.join(',') : null,
      singleDate: form.type === 2 ? form.singleDate : null,
      startTime: form.startTime || null,
      endTime: form.endTime || null,
      reason: form.reason || null,
      status: form.status
    }
    if (form.id) {
      await updateBlacklistRule(form.id, payload)
      ElMessage.success('规则已更新')
    } else {
      await addBlacklistRule(storeId.value, payload)
      ElMessage.success('规则已创建')
    }
    editVisible.value = false
    loadAll()
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除该时段黑名单规则？删除后对应时段立即恢复可预约。', '删除确认', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteBlacklistRule(row.id)
  ElMessage.success('已删除')
  loadAll()
}

function dayTitle(cell) {
  if (cell.allDayClosed) return '全天不可预约'
  if (cell.slots.length) {
    return cell.slots.map(s => `${s.startTime.slice(0, 5)}-${s.endTime.slice(0, 5)} ${s.reason || ''}`).join('\n')
  }
  return ''
}

onMounted(() => {
  month.value = new Date().toISOString().slice(0, 7)
  loadStores()
})
</script>

<style scoped>
.filter-card {
  margin-bottom: 12px;
}
.rule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.selector {
  display: flex;
  align-items: center;
}
.label {
  font-size: 14px;
  color: #606266;
}
.gap {
  margin-left: 12px;
}
.muted {
  color: #909399;
  font-size: 12px;
}
.cal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.cal-month {
  margin: 0 8px;
  font-weight: 600;
  font-size: 14px;
}
.calendar {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}
.cal-week {
  text-align: center;
  font-size: 12px;
  color: #909399;
  padding-bottom: 4px;
}
.cal-day {
  height: 44px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  position: relative;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cal-day.empty {
  border: none;
}
.cal-day:not(.empty):not(.past) {
  cursor: pointer;
  transition: background 0.15s;
}
.cal-day:not(.empty):not(.closed):not(.partial):hover {
  background: #ecf5ff;
}
.cal-day.closed {
  background: #fef0f0;
  border-color: #fbc4c4;
}
.cal-day.partial {
  background: #fdf6ec;
  border-color: #f3d19e;
}
.cal-day.past {
  background: #f5f7fa;
  border-color: #ebeef5;
  color: #c0c4cc;
}
.cal-day.past .day-mark {
  color: #c0c4cc;
}
.cal-day.today {
  border: 2px solid #409eff;
}
.day-mark {
  position: absolute;
  bottom: 2px;
  right: 4px;
  font-size: 10px;
  color: #f56c6c;
}
.partial-mark {
  color: #e6a23c;
}
.legend {
  margin-top: 10px;
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: #606266;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
.cal-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  display: inline-block;
}
.closed-dot {
  background: #fef0f0;
  border: 1px solid #f56c6c;
}
.partial-dot {
  background: #fdf6ec;
  border: 1px solid #e6a23c;
}
.today-dot {
  background: #fff;
  border: 1px solid #409eff;
}
.time-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
