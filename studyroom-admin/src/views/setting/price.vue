<template>
  <div class="page">
    <!-- FR-AD-025 价格配置：套餐 / 时段加价规则 / 座位类型 -->
    <el-tabs v-model="tab" @tab-change="loadTab">
      <!-- ========== 套餐 ========== -->
      <el-tab-pane label="套餐管理" name="pkg">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>套餐列表（用户端 FR-UC-019 数据源）</span>
              <el-button type="primary" @click="openPkg()">新建套餐</el-button>
            </div>
          </template>
          <el-table :data="packages" v-loading="loading" border stripe>
            <el-table-column prop="packageName" label="名称" min-width="150" />
            <el-table-column prop="packageTypeDesc" label="类型" width="100" />
            <el-table-column label="时长" width="130">
              <template #default="{ row }">{{ durationText(row) }}</template>
            </el-table-column>
            <el-table-column label="售价" width="110">
              <template #default="{ row }">
                <span style="color: #409EFF; font-weight: 600">¥{{ row.price }}</span>
                <span v-if="row.originalPrice" style="color: #C0C4CC; text-decoration: line-through; margin-left: 6px; font-size: 12px">¥{{ row.originalPrice }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
            <el-table-column prop="sort" label="排序" width="70" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openPkg(row)">编辑</el-button>
                <el-button link :type="row.status === 1 ? 'info' : 'success'" @click="togglePkg(row)">
                  {{ row.status === 1 ? '下架' : '上架' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ========== 时段加价规则 ========== -->
      <el-tab-pane label="时段加价规则" name="rule">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>时段加价规则（PricingService 计价引擎数据源）</span>
              <el-button type="primary" @click="openRule()">新建规则</el-button>
            </div>
          </template>
          <el-table :data="rules" v-loading="loading" border stripe>
            <el-table-column prop="timeSlotStart" label="时段起" width="100" />
            <el-table-column prop="timeSlotEnd" label="时段止" width="100" />
            <el-table-column prop="seatTypeName" label="座位类型" width="120">
              <template #default="{ row }">{{ row.seatTypeName || '全部类型' }}</template>
            </el-table-column>
            <el-table-column prop="rulePackageName" label="适用套餐" width="130">
              <template #default="{ row }">{{ row.rulePackageName || '全部套餐' }}</template>
            </el-table-column>
            <el-table-column label="加价方式" width="140">
              <template #default="{ row }">
                <span v-if="row.extraPrice">定额 +¥{{ row.extraPrice }}</span>
                <span v-else-if="row.extraRatio">倍数 ×{{ row.extraRatio }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="priority" label="优先级" width="80" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.ruleStatus === 1 ? 'success' : 'info'">{{ row.ruleStatus === 1 ? '启用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openRule(row)">编辑</el-button>
                <el-button link type="danger" @click="removeRule(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ========== 座位类型 ========== -->
      <el-tab-pane label="座位类型" name="seatType">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>座位类型（补模块 12 遗留；基础价通过加价规则表达）</span>
              <el-button type="primary" @click="openSeatType()">新建类型</el-button>
            </div>
          </template>
          <el-table :data="seatTypes" v-loading="loading" border stripe>
            <el-table-column prop="typeName" label="名称" min-width="120" />
            <el-table-column prop="typeCode" label="编码" width="120" />
            <el-table-column prop="seatCount" label="座位数" width="90" />
            <el-table-column prop="typeDescription" label="描述" min-width="180" show-overflow-tooltip />
            <el-table-column prop="typeSort" label="排序" width="70" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.typeStatus === 1 ? 'success' : 'info'">{{ row.typeStatus === 1 ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openSeatType(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 套餐编辑弹窗 -->
    <el-dialog v-model="pkgVisible" :title="pkgForm.id ? '编辑套餐' : '新建套餐'" width="520px">
      <el-form :model="pkgForm" label-width="100px">
        <el-form-item label="套餐名称" required>
          <el-input v-model="pkgForm.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="套餐类型" required>
          <el-select v-model="pkgForm.type" @change="onPkgTypeChange">
            <el-option label="按小时" :value="1" />
            <el-option label="日卡" :value="2" />
            <el-option label="周卡" :value="3" />
            <el-option label="月卡" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item :label="durationLabel" required>
          <el-input-number v-model="pkgForm.durationValue" :min="1" />
        </el-form-item>
        <el-form-item label="售价（元）" required>
          <el-input-number v-model="pkgForm.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="划线原价">
          <el-input-number v-model="pkgForm.originalPrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="pkgForm.description" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="pkgForm.sort" :min="0" />
        </el-form-item>
        <el-form-item label="上架">
          <el-switch v-model="pkgForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pkgVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPkg">保存</el-button>
      </template>
    </el-dialog>

    <!-- 加价规则弹窗 -->
    <el-dialog v-model="ruleVisible" :title="ruleForm.id ? '编辑加价规则' : '新建加价规则'" width="520px">
      <el-form :model="ruleForm" label-width="100px">
        <el-form-item label="时段" required>
          <el-time-picker v-model="ruleRange" is-range format="HH:mm" value-format="HH:mm"
            start-placeholder="起" end-placeholder="止" style="width: 100%" />
        </el-form-item>
        <el-form-item label="座位类型">
          <el-select v-model="ruleForm.seatTypeId" placeholder="全部类型" clearable>
            <el-option v-for="t in seatTypes" :key="t.seatTypeId" :label="t.typeName" :value="t.seatTypeId" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用套餐">
          <el-select v-model="ruleForm.packageId" placeholder="全部套餐" clearable>
            <el-option v-for="p in packages" :key="p.packageId" :label="p.packageName" :value="p.packageId" />
          </el-select>
        </el-form-item>
        <el-form-item label="加价方式">
          <el-radio-group v-model="ruleMode">
            <el-radio label="price">定额加价</el-radio>
            <el-radio label="ratio">倍数加价</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="ruleMode === 'price'" label="定额加价(元)">
          <el-input-number v-model="ruleForm.extraPrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item v-if="ruleMode === 'ratio'" label="倍数">
          <el-input-number v-model="ruleForm.extraRatio" :min="1" :max="3" :step="0.1" :precision="2" />
          <span class="tip">1.20 = 上浮 20%</span>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="ruleForm.priority" :min="0" :max="100" />
          <span class="tip">越大越优先</span>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="ruleForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRule">保存</el-button>
      </template>
    </el-dialog>

    <!-- 座位类型弹窗 -->
    <el-dialog v-model="seatTypeVisible" :title="seatTypeForm.id ? '编辑座位类型' : '新建座位类型'" width="480px">
      <el-form :model="seatTypeForm" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="seatTypeForm.name" maxlength="32" placeholder="如 普通区 / VIP" />
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input v-model="seatTypeForm.code" maxlength="32" placeholder="如 NORMAL / VIP（大写唯一）" :disabled="!!seatTypeForm.id" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="seatTypeForm.description" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="seatTypeForm.sort" :min="0" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="seatTypeForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="seatTypeVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitSeatType">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listPackages, savePackage, togglePackageStatus,
  listPriceRules, savePriceRule, deletePriceRule,
  listSeatTypes, saveSeatType
} from '@/api/setting'

const tab = ref('pkg')
const loading = ref(false)
const saving = ref(false)

const packages = ref([])
const rules = ref([])
const seatTypes = ref([])

/* ---------- 套餐 ---------- */
const pkgVisible = ref(false)
const pkgForm = reactive({})
const durationLabel = computed(() => {
  const labels = { 1: '时长（小时）', 2: '时长（天）', 3: '时长（周）', 4: '时长（月）' }
  return labels[pkgForm.type] || '时长'
})

function durationText(row) {
  return `${row.durationValue} ${unitText(row.unit)}`
}

function unitText(unit) {
  const map = { hour: '小时', day: '天', week: '周', month: '月' }
  return map[unit] || unit
}

function onPkgTypeChange(type) {
  pkgForm.unit = { 1: 'hour', 2: 'day', 3: 'week', 4: 'month' }[type]
  if ([2, 3, 4].includes(type)) {
    pkgForm.durationValue = { 2: 1, 3: 1, 4: 1 }[type]
  }
}

function openPkg(row) {
  Object.assign(pkgForm, {
    id: row?.packageId || null,
    name: row?.packageName || '',
    type: row?.packageType || 1,
    durationValue: row?.durationValue ?? 1,
    unit: row?.unit || 'hour',
    price: row?.price ?? 0,
    originalPrice: row?.originalPrice ?? null,
    description: row?.description || '',
    sort: row?.sort ?? 0,
    status: row?.status ?? 1
  })
  pkgVisible.value = true
}

async function submitPkg() {
  if (!pkgForm.name) return ElMessage.warning('请填写套餐名称')
  saving.value = true
  try {
    await savePackage({ ...pkgForm, originalPrice: pkgForm.originalPrice || null })
    ElMessage.success('保存成功')
    pkgVisible.value = false
    loadTab()
  } finally {
    saving.value = false
  }
}

async function togglePkg(row) {
  const online = row.status !== 1
  await ElMessageBox.confirm(`确定${online ? '上架' : '下架'}套餐「${row.packageName}」吗？`, '提示', { type: 'warning' })
  await togglePackageStatus(row.packageId, online)
  ElMessage.success(online ? '已上架' : '已下架')
  loadTab()
}

/* ---------- 加价规则 ---------- */
const ruleVisible = ref(false)
const ruleForm = reactive({})
const ruleMode = ref('price')
const ruleRange = ref(['18:00', '22:00'])

function openRule(row) {
  Object.assign(ruleForm, {
    id: row?.ruleId || null,
    seatTypeId: row?.seatTypeId || null,
    packageId: row?.rulePackageId || null,
    timeSlotStart: row?.timeSlotStart || '18:00',
    timeSlotEnd: row?.timeSlotEnd || '22:00',
    extraPrice: row?.extraPrice ?? 0,
    extraRatio: row?.extraRatio ?? 1,
    priority: row?.priority ?? 0,
    status: row?.ruleStatus ?? 1
  })
  ruleMode.value = row?.extraPrice ? 'price' : 'ratio'
  ruleRange.value = [ruleForm.timeSlotStart, ruleForm.timeSlotEnd]
  ruleVisible.value = true
}

async function submitRule() {
  if (!ruleRange.value || ruleRange.value.length !== 2) return ElMessage.warning('请选择时段')
  saving.value = true
  try {
    const payload = { ...ruleForm, timeSlotStart: ruleRange.value[0], timeSlotEnd: ruleRange.value[1] }
    if (ruleMode.value === 'price') payload.extraRatio = null
    else payload.extraPrice = null
    await savePriceRule(payload)
    ElMessage.success('保存成功')
    ruleVisible.value = false
    loadTab()
  } finally {
    saving.value = false
  }
}

async function removeRule(row) {
  await ElMessageBox.confirm('确定删除该加价规则吗？删除后立即生效。', '警告', { type: 'warning' })
  await deletePriceRule(row.ruleId)
  ElMessage.success('已删除')
  loadTab()
}

/* ---------- 座位类型 ---------- */
const seatTypeVisible = ref(false)
const seatTypeForm = reactive({})

function openSeatType(row) {
  Object.assign(seatTypeForm, {
    id: row?.seatTypeId || null,
    name: row?.typeName || '',
    code: row?.typeCode || '',
    description: row?.typeDescription || '',
    sort: row?.typeSort ?? 0,
    status: row?.typeStatus ?? 1
  })
  seatTypeVisible.value = true
}

async function submitSeatType() {
  if (!seatTypeForm.name) return ElMessage.warning('请填写类型名称')
  if (!seatTypeForm.code) return ElMessage.warning('请填写类型编码')
  saving.value = true
  try {
    await saveSeatType({ ...seatTypeForm })
    ElMessage.success('保存成功')
    seatTypeVisible.value = false
    loadTab()
  } finally {
    saving.value = false
  }
}

/* ---------- 加载 ---------- */
async function loadTab() {
  loading.value = true
  try {
    if (tab.value === 'pkg') {
      const { data } = await listPackages()
      packages.value = data || []
    } else if (tab.value === 'rule') {
      const [ruleRes, typeRes] = await Promise.all([listPriceRules(), listSeatTypes()])
      rules.value = ruleRes.data || []
      seatTypes.value = typeRes.data || []
    } else {
      const { data } = await listSeatTypes()
      seatTypes.value = data || []
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadTab)
</script>

<style scoped>
.page { padding: 4px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.tip { margin-left: 10px; color: #909399; font-size: 12px; }
</style>
