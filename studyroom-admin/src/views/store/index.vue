<template>
  <div class="store-page">
    <el-card shadow="never">
      <div class="toolbar">
        <span class="title">门店列表</span>
        <el-button type="primary" :icon="Plus" @click="openEdit(null)">新增门店</el-button>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="name" label="门店名称" min-width="140" fixed />
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="businessHours" label="营业时间" width="120" />
        <el-table-column label="坐标" width="180">
          <template #default="{ row }">
            <span v-if="row.latitude">{{ row.latitude }}, {{ row.longitude }}</span>
            <span v-else class="muted">未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="设施" min-width="160">
          <template #default="{ row }">
            <el-tag
              v-for="f in (row.facilities || []).slice(0, 3)"
              :key="f"
              size="small"
              type="info"
              class="tag-gap"
            >{{ f }}</el-tag>
            <span v-if="!(row.facilities || []).length" class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="seatCount" label="座位数" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'info'" size="small">
              {{ row.statusDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 门店编辑弹窗 FR-AD-004 -->
    <el-dialog
      v-model="editVisible"
      :title="form.id ? '编辑门店信息' : '新增门店'"
      width="640px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="门店名称" prop="name">
          <el-input v-model="form.name" maxlength="64" placeholder="如：中科智慧自习室" />
        </el-form-item>
        <el-form-item label="门店地址" prop="address">
          <el-input v-model="form.address" maxlength="255" placeholder="省市区 + 详细地址" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" maxlength="32" style="width: 220px" />
        </el-form-item>
        <el-form-item label="营业时间" prop="businessHours">
          <el-input v-model="form.businessHours" placeholder="08:00-23:00" style="width: 180px" />
        </el-form-item>
        <el-form-item label="GPS 坐标">
          <div class="coord-row">
            <el-input v-model="form.latitude" placeholder="纬度，如 31.230400" />
            <el-input v-model="form.longitude" placeholder="经度，如 121.473700" />
          </div>
          <div class="muted tip">签到 GPS 校验依据（FR-UC-030 距离 ≤100 米），为空则跳过校验</div>
        </el-form-item>
        <el-form-item label="设施标签">
          <div class="facilities">
            <el-tag
              v-for="(f, i) in form.facilities"
              :key="f"
              closable
              @close="form.facilities.splice(i, 1)"
              class="tag-gap"
            >{{ f }}</el-tag>
            <el-input
              v-if="facilityInput.visible"
              ref="facilityInputRef"
              v-model="facilityInput.value"
              size="small"
              class="facility-input"
              @keyup.enter="addFacility"
              @blur="addFacility"
            />
            <el-button v-else size="small" @click="showFacilityInput">+ 标签</el-button>
          </div>
        </el-form-item>
        <el-form-item label="环境图片">
          <div class="img-list">
            <div v-for="(img, i) in form.images" :key="i" class="img-box">
              <el-image :src="img" fit="cover" class="img" :preview-src-list="form.images" />
              <el-icon class="img-del" @click="form.images.splice(i, 1)"><CircleClose /></el-icon>
            </div>
          </div>
          <el-input
            v-model="imageUrlInput"
            size="small"
            placeholder="输入图片 URL 回车添加（环境轮播图，FR-UC-015）"
            @keyup.enter="addImage"
          />
        </el-form-item>
        <el-form-item label="门店简介">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="营业状态">
          <el-radio-group v-model="form.status">
            <el-radio-button :label="1">营业中</el-radio-button>
            <el-radio-button :label="0">休息中</el-radio-button>
            <el-radio-button :label="2">维护中</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
          <span class="muted tip">越小越靠前</span>
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
 * 管理端-门店信息 —— FR-AD-004
 * 名称、地址、营业时间、GPS 坐标、设施标签、环境图片编辑；
 * 门店状态（营业/休息/维护）与排序。
 */
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, CircleClose } from '@element-plus/icons-vue'
import { createStore, listStores, updateStore } from '@/api/store'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const editVisible = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  address: '',
  phone: '',
  businessHours: '08:00-23:00',
  latitude: '',
  longitude: '',
  description: '',
  facilities: [],
  images: [],
  status: 1,
  sort: 0
})

const rules = {
  name: [{ required: true, message: '请输入门店名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入门店地址', trigger: 'blur' }],
  businessHours: [{
    pattern: /^([01]\d|2[0-3]):[0-5]\d-([01]\d|2[0-3]):[0-5]\d$/,
    message: '格式应为 HH:mm-HH:mm，如 08:00-23:00',
    trigger: 'blur'
  }]
}

const facilityInput = reactive({ visible: false, value: '' })
const facilityInputRef = ref(null)
const imageUrlInput = ref('')

async function load() {
  loading.value = true
  try {
    list.value = await listStores()
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      address: row.address,
      phone: row.phone || '',
      businessHours: row.businessHours || '08:00-23:00',
      latitude: row.latitude ?? '',
      longitude: row.longitude ?? '',
      description: row.description || '',
      facilities: [...(row.facilities || [])],
      images: [...(row.images || [])],
      status: row.status ?? 1,
      sort: row.sort ?? 0
    })
  } else {
    Object.assign(form, {
      id: null, name: '', address: '', phone: '', businessHours: '08:00-23:00',
      latitude: '', longitude: '', description: '', facilities: [], images: [],
      status: 1, sort: 0
    })
  }
  editVisible.value = true
}

function showFacilityInput() {
  facilityInput.visible = true
  facilityInput.value = ''
  nextTick(() => facilityInputRef.value?.focus())
}

function addFacility() {
  const v = facilityInput.value.trim()
  if (v && !form.facilities.includes(v)) {
    form.facilities.push(v)
  }
  facilityInput.visible = false
  facilityInput.value = ''
}

function addImage() {
  const url = imageUrlInput.value.trim()
  if (url) {
    form.images.push(url)
    imageUrlInput.value = ''
  }
}

async function onSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      ...form,
      latitude: form.latitude === '' ? null : Number(form.latitude),
      longitude: form.longitude === '' ? null : Number(form.longitude),
      facilities: form.facilities,
      images: form.images
    }
    if (form.id) {
      await updateStore(form.id, payload)
      ElMessage.success('门店信息已更新')
    } else {
      await createStore(payload)
      ElMessage.success('门店已创建')
    }
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.title {
  font-size: 15px;
  font-weight: 600;
}
.muted {
  color: #909399;
  font-size: 12px;
}
.tip {
  margin-left: 8px;
}
.tag-gap {
  margin-right: 6px;
}
.coord-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.facilities {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  width: 100%;
}
.facility-input {
  width: 120px;
}
.img-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
  width: 100%;
}
.img-box {
  position: relative;
}
.img {
  width: 72px;
  height: 72px;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}
.img-del {
  position: absolute;
  top: -6px;
  right: -6px;
  cursor: pointer;
  color: #f56c6c;
  background: #fff;
  border-radius: 50%;
}
</style>
