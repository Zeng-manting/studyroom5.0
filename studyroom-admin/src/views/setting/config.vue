<template>
  <div class="page">
    <!-- FR-AD-026 系统配置：分组展示 + 批量保存 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>系统配置（退款规则 / 订单超时 / 签到窗口 / 信用阈值等）</span>
          <el-button type="primary" :loading="saving" @click="submit">保存修改</el-button>
        </div>
      </template>

      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
        title="当前版本业务侧退款等规则仍读取编译期常量，本页为运营动态化预留；保存后写入 system_config 表，后续版本切换读取。"
      />

      <el-empty v-if="!groups.length && !loading" description="暂无配置" />

      <el-collapse v-model="activeGroups">
        <el-collapse-item v-for="(items, group) in groups" :key="group" :name="group">
          <template #title>
            <el-tag size="small" style="margin-right: 10px">{{ group }}</el-tag>
            <span style="color: #606266">{{ groupTitle(group) }}（{{ items.length }} 项）</span>
          </template>
          <el-table :data="items" border size="default">
            <el-table-column prop="configKey" label="配置键" width="260" />
            <el-table-column prop="configValue" label="配置值" width="220">
              <template #default="{ row }">
                <el-input v-model="row.configValue" size="small" :data-key="row.configKey" />
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="200" />
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listConfigs, saveConfigs } from '@/api/setting'

const groups = ref({})
const loading = ref(false)
const saving = ref(false)
const activeGroups = reactive(['refund'])

const groupTitles = {
  refund: '退款规则',
  order: '订单超时',
  sign: '签到窗口',
  pause: '暂停上限',
  credit: '信用阈值',
  limiter: '接口限流',
  jwt: '登录有效期'
}

function groupTitle(group) {
  return groupTitles[group] || group
}

async function load() {
  loading.value = true
  try {
    const { data } = await listConfigs()
    groups.value = data || {}
    activeGroups.length = 0
    Object.keys(groups.value).forEach((g) => activeGroups.push(g))
  } finally {
    loading.value = false
  }
}

async function submit() {
  const values = {}
  Object.values(groups.value).forEach((items) => {
    items.forEach((c) => {
      values[c.configKey] = String(c.configValue ?? '').trim()
    })
  })
  if (!Object.keys(values).length) return ElMessage.warning('没有可保存的配置')
  saving.value = true
  try {
    await saveConfigs(values)
    ElMessage.success('保存成功')
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page { padding: 4px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
</style>
