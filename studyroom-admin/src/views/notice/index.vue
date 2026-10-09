<template>
  <div class="notice-page">
    <!-- 筛选栏 FR-AD-016 -->
    <el-card shadow="never" class="filter-card">
      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="类型">
          <el-select v-model="query.type" clearable placeholder="全部" style="width: 130px">
            <el-option v-for="(label, t) in TYPES" :key="t" :label="label" :value="Number(t)" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
            <el-option v-for="(label, s) in STATUS" :key="s" :label="label" :value="Number(s)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
          <el-button type="success" :icon="Plus" @click="openEdit(null)">新建公告</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 公告表格 FR-AD-016 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip fixed />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small">{{ row.typeDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="storeName" label="可见范围" width="120" show-overflow-tooltip />
        <el-table-column label="重要" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.isImportant === 1" type="danger" size="small" effect="plain">重要</el-tag>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="86">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="阅读量" width="80">
          <template #default="{ row }">
            <span class="read-count">{{ row.readCount ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">{{ fmt(row.publishTime) || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status !== 1" type="success" link @click="onPublish(row)">
              发布
            </el-button>
            <el-button v-if="row.status === 1" type="warning" link @click="onOffline(row)">
              下架
            </el-button>
            <el-button type="danger" link @click="onDelete(row)">删除</el-button>
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

    <!-- 新建/编辑弹窗 FR-AD-015 -->
    <el-dialog
      v-model="editVisible"
      :title="form.id ? '编辑公告' : '新建公告'"
      width="760px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="60" show-word-limit placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio-button v-for="(label, t) in TYPES" :key="t" :label="Number(t)">
              {{ label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="可见范围">
          <el-select v-model="form.storeId" clearable placeholder="全部门店" style="width: 220px">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
          <span class="muted tip-gap">不选择 = 对全部门店可见</span>
        </el-form-item>
        <el-form-item label="重要公告">
          <el-switch v-model="form.isImportant" :active-value="1" :inactive-value="0" />
          <span class="muted tip-gap">开启后用户端将弹窗提醒（FR-UC-042）</span>
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <!-- 轻量富文本：contenteditable + execCommand 工具栏 -->
          <div class="editor-wrap">
            <div class="editor-toolbar">
              <button type="button" @mousedown.prevent @click="execCmd('bold')"><b>B</b></button>
              <button type="button" @mousedown.prevent @click="execCmd('italic')"><i>I</i></button>
              <button type="button" @mousedown.prevent @click="execCmd('underline')"><u>U</u></button>
              <span class="sep" />
              <button type="button" @mousedown.prevent @click="execCmd('formatBlock', 'h3')">标题</button>
              <button type="button" @mousedown.prevent @click="execCmd('formatBlock', 'blockquote')">引用</button>
              <button type="button" @mousedown.prevent @click="execCmd('insertUnorderedList')">列表</button>
              <span class="sep" />
              <button type="button" @mousedown.prevent @click="insertImage">图片</button>
              <button type="button" @mousedown.prevent @click="clearFormat">清除格式</button>
            </div>
            <div
              ref="editorRef"
              class="editor-body"
              contenteditable="true"
              data-placeholder="请输入公告正文（支持加粗 / 标题 / 列表 / 图片）"
              @input="onEditorInput"
            />
          </div>
        </el-form-item>
        <el-form-item label="附件">
          <div class="attach-list">
            <div v-for="(a, i) in form.attachments" :key="i" class="attach-item">
              <el-input v-model="a.fileName" placeholder="文件名" style="width: 160px" size="small" />
              <el-input v-model="a.fileUrl" placeholder="文件URL" style="flex: 1" size="small" />
              <el-button type="danger" :icon="Delete" circle size="small" @click="form.attachments.splice(i, 1)" />
            </div>
            <el-button :icon="Plus" size="small" @click="form.attachments.push({ fileName: '', fileUrl: '' })">
              添加附件
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button :loading="saving" @click="onSave(false)">存草稿</el-button>
        <el-button type="primary" :loading="saving" @click="onSave(true)">保存并发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import { pageNotices, getNotice, saveNotice, publishNotice, offlineNotice, deleteNotice } from '@/api/notice'
import { listStores } from '@/api/store'

defineOptions({ name: 'NoticeList' })

const TYPES = { 1: '门店规则', 2: '停业通知', 3: '活动公告', 4: '其他' }
const STATUS = { 0: '草稿', 1: '已发布', 2: '已下架' }

const query = reactive({ type: null, status: null, page: 1, size: 20 })
const list = ref([])
const total = ref(0)
const loading = ref(false)

const stores = ref([])
const editVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const editorRef = ref(null)

const form = reactive({
  id: null,
  storeId: null,
  title: '',
  type: 1,
  content: '',
  isImportant: 0,
  publishNow: false,
  attachments: []
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }]
}

const fmt = (v) => (v ? String(v).replace('T', ' ').slice(0, 16) : '')

const typeTag = (t) => ({ 1: 'primary', 2: 'danger', 3: 'success', 4: 'info' })[t] || 'info'
const statusTag = (s) => ({ 0: 'info', 1: 'success', 2: 'warning' })[s] || 'info'

const load = async () => {
  loading.value = true
  try {
    const { data } = await pageNotices(query)
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
  query.type = null
  query.status = null
  onSearch()
}

/* ---------- 富文本编辑器（contenteditable 轻量实现） ---------- */

const execCmd = (cmd, value) => {
  editorRef.value?.focus()
  document.execCommand(cmd, false, value)
  onEditorInput()
}

const clearFormat = () => execCmd('removeFormat')

/** 插入图片：弹窗输入 URL 后插入 img 标签 */
const insertImage = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入图片 URL', '插入图片', {
      inputPlaceholder: 'https://...'
    })
    if (value) execCmd('insertImage', value.trim())
  } catch {
    /* 用户取消 */
  }
}

const onEditorInput = () => {
  form.content = editorRef.value?.innerHTML || ''
}

/* ---------- 新建/编辑 ---------- */

const openEdit = async (row) => {
  editVisible.value = true
  if (row) {
    // 编辑：拉详情回显
    try {
      const { data } = await getNotice(row.id)
      form.id = data.id
      form.storeId = data.storeId
      form.title = data.title
      form.type = data.type
      form.isImportant = data.isImportant
      form.content = data.content || ''
      form.attachments = (data.attachments || []).map((a) => ({
        fileName: a.fileName,
        fileUrl: a.fileUrl
      }))
    } catch (e) {
      ElMessage.error('加载公告详情失败')
    }
  } else {
    form.id = null
    form.storeId = null
    form.title = ''
    form.type = 1
    form.isImportant = 0
    form.content = ''
    form.attachments = []
  }
  // 弹窗渲染后回填富文本
  await nextTick()
  if (editorRef.value) {
    editorRef.value.innerHTML = form.content
  }
}

const onSave = async (publishNow) => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (!form.content || !form.content.replace(/<[^>]+>/g, '').trim()) {
    ElMessage.warning('请输入公告正文')
    return
  }
  saving.value = true
  try {
    await saveNotice({ ...form, publishNow, title: form.title.trim() })
    ElMessage.success(publishNow ? '已保存并发布' : '已存为草稿')
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

/* ---------- 发布/下架/删除 ---------- */

const onPublish = async (row) => {
  try {
    await ElMessageBox.confirm(`确定发布公告「${row.title}」吗？发布后用户端即刻可见。`, '发布公告', {
      type: 'info'
    })
  } catch {
    return
  }
  await publishNotice(row.id)
  ElMessage.success('已发布')
  load()
}

const onOffline = async (row) => {
  try {
    await ElMessageBox.confirm(`确定下架公告「${row.title}」吗？用户端将即刻不可见。`, '下架公告', {
      type: 'warning'
    })
  } catch {
    return
  }
  await offlineNotice(row.id)
  ElMessage.success('已下架')
  load()
}

const onDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？删除后不可恢复。`, '删除公告', {
      type: 'warning',
      confirmButtonText: '删除'
    })
  } catch {
    return
  }
  await deleteNotice(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(() => {
  load()
  listStores().then(({ data }) => (stores.value = data || [])).catch(() => {})
})
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}
.muted {
  color: #909399;
  font-size: 12px;
}
.tip-gap {
  margin-left: 8px;
}
.read-count {
  color: #409eff;
  font-weight: 600;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.editor-wrap {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}
.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 8px;
  border-bottom: 1px solid #ebeef5;
  background: #f5f7fa;
  flex-wrap: wrap;
}
.editor-toolbar button {
  border: 1px solid #dcdfe6;
  background: #fff;
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 12px;
  cursor: pointer;
  color: #606266;
}
.editor-toolbar button:hover {
  color: #409eff;
  border-color: #409eff;
}
.sep {
  width: 1px;
  height: 14px;
  background: #dcdfe6;
  margin: 0 4px;
}
.editor-body {
  min-height: 220px;
  max-height: 360px;
  overflow-y: auto;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.7;
  outline: none;
}
.editor-body:empty::before {
  content: attr(data-placeholder);
  color: #c0c4cc;
}
.editor-body :deep(blockquote) {
  margin: 8px 0;
  padding: 6px 12px;
  border-left: 4px solid #dcdfe6;
  background: #fafafa;
  color: #606266;
}
.editor-body :deep(img) {
  max-width: 100%;
}
.attach-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
}
.attach-item {
  display: flex;
  gap: 8px;
  width: 100%;
  align-items: center;
}
</style>
