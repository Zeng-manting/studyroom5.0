<template>
  <div class="seats-page">
    <!-- 工具栏 -->
    <el-card shadow="never" class="filter-card">
      <div class="toolbar">
        <div class="left">
          <span class="label">门店：</span>
          <el-select v-model="storeId" style="width: 220px" placeholder="选择门店" @change="loadAll">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
          <el-divider direction="vertical" />
          <el-button type="primary" :icon="Plus" :disabled="!storeId" @click="startAdd">
            添加座位
          </el-button>
          <el-button :icon="Delete" :disabled="!selected" @click="removeSelected">
            删除选中
          </el-button>
          <el-button type="success" :icon="Check" :loading="saving" :disabled="!storeId" @click="onSave">
            保存布局
          </el-button>
          <span v-if="dirty" class="dirty-tip">● 有未保存的修改</span>
        </div>
        <div class="legend">
          <span class="legend-item"><i :style="dot(0)" />空闲</span>
          <span class="legend-item"><i :style="dot(1)" />使用中</span>
          <span class="legend-item"><i :style="dot(2)" />维护</span>
          <span class="legend-item"><i :style="dot(3)" />打扫中</span>
          <span class="legend-item"><i :style="dot(4)" />锁定</span>
        </div>
      </div>
      <div v-if="adding" class="adding-tip">
        <el-alert title="点击画布空白处放置新座位，按 Esc 取消" type="warning" :closable="false" />
      </div>
    </el-card>

    <el-row :gutter="12">
      <!-- 画布 -->
      <el-col :span="17">
        <el-card shadow="never" v-loading="loading" class="canvas-card">
          <template #header>
            <span>座位布局（拖拽调整位置，点击选中编辑；FR-AD-006）</span>
          </template>
          <div
            ref="canvasRef"
            class="canvas"
            :style="{ width: canvasWidth + 'px', height: canvasHeight + 'px' }"
            @mousedown="onCanvasMouseDown"
          >
            <div
              v-for="seat in seats"
              :key="seat._key"
              class="seat"
              :class="{ selected: selected === seat, dragging: draggingSeat === seat }"
              :style="{
                left: seat.xCoord + 'px',
                top: seat.yCoord + 'px',
                borderColor: seatColor(seat.status)
              }"
              @mousedown.stop="onSeatMouseDown($event, seat)"
            >
              <span class="seat-no">{{ seat.seatNo }}</span>
              <span class="seat-status" :style="{ color: seatColor(seat.status) }">
                {{ seat.statusDesc }}
              </span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 属性 / 状态面板 -->
      <el-col :span="7">
        <el-card shadow="never" class="panel-card">
          <template #header>座位属性 / 状态管理</template>
          <template v-if="selected">
            <el-form label-width="70px" size="small">
              <el-form-item label="座位编号">
                <el-input v-model="selected.seatNo" maxlength="32" @input="markDirty" />
              </el-form-item>
              <el-form-item label="座位类型">
                <el-select v-model="selected.seatTypeId" @change="markDirty">
                  <el-option v-for="t in seatTypes" :key="t.id" :label="t.name" :value="t.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="楼层">
                <el-input v-model="selected.floor" maxlength="16" @input="markDirty" />
              </el-form-item>
              <el-form-item label="区域">
                <el-select v-model="selected.areaCode" clearable @change="markDirty">
                  <el-option label="靠窗 window" value="window" />
                  <el-option label="角落 corner" value="corner" />
                  <el-option label="中央 center" value="center" />
                </el-select>
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="selected.description" maxlength="255" @input="markDirty" />
              </el-form-item>
              <el-form-item label="画布坐标">
                <span class="muted">X: {{ selected.xCoord }} / Y: {{ selected.yCoord }}</span>
              </el-form-item>
            </el-form>

            <el-divider>状态管理（FR-AD-007）</el-divider>
            <div class="status-actions">
              <el-button
                v-if="selected.status === 0"
                type="warning" size="small"
                :disabled="selected.hasActiveOrder"
                @click="changeStatus(selected, 2)"
              >置为维护</el-button>
              <el-button
                v-if="selected.status === 2 || selected.status === 3 || selected.status === 4"
                type="success" size="small"
                @click="changeStatus(selected, 0)"
              >恢复空闲</el-button>
              <el-button
                v-if="selected.status === 0"
                type="danger" size="small"
                :disabled="selected.hasActiveOrder"
                @click="changeStatus(selected, 4)"
              >锁定</el-button>
              <div v-if="selected.status === 1" class="muted status-tip">
                使用中：状态由订单驱动（签到/离场自动流转），不可手动切换
              </div>
              <div v-if="selected.hasActiveOrder && selected.status === 0" class="muted status-tip">
                该座位有未完成订单（含待支付锁座），暂不能置维护/锁定
              </div>
            </div>
          </template>
          <el-empty v-else description="点击画布上的座位进行编辑" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
/**
 * 管理端-座位布局可视化编辑 + 状态管理 —— FR-AD-006 / FR-AD-007
 *
 * - 拖拽编辑画布：mousedown/mousemove 拖动座位（10px 网格吸附），点击选中编辑属性
 * - 全量同步保存：带 id 更新、不带 id 新增、画布缺失座位删除（后端校验进行中订单）
 * - 状态管理：空闲↔维护/锁定、打扫→空闲即时切换（后端 CAS），使用中由订单驱动
 */
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete, Check } from '@element-plus/icons-vue'
import {
  changeSeatStatus, listSeatTypes, listSeats, listStores, saveSeatLayout
} from '@/api/store'

const SEAT_W = 76
const SEAT_H = 44
const SNAP = 10

const stores = ref([])
const storeId = ref(null)
const seatTypes = ref([])
const seats = ref([])
const loading = ref(false)
const saving = ref(false)
const dirty = ref(false)
const selected = ref(null)

const canvasRef = ref(null)
const adding = ref(false)
const draggingSeat = ref(null)
const dragState = reactive({ startX: 0, startY: 0, originX: 0, originY: 0, moved: false })
const keySeq = ref(0)

const canvasWidth = computed(() =>
  Math.max(800, ...seats.value.map(s => s.xCoord + SEAT_W + 60)))
const canvasHeight = computed(() =>
  Math.max(420, ...seats.value.map(s => s.yCoord + SEAT_H + 60)))

function seatColor(status) {
  return { 0: '#67c23a', 1: '#409eff', 2: '#e6a23c', 3: '#fadb14', 4: '#f56c6c' }[status] || '#909399'
}
function dot(status) {
  return { background: seatColor(status), display: 'inline-block', width: '10px', height: '10px', borderRadius: '2px' }
}
function markDirty() {
  dirty.value = true
}

async function loadStores() {
  stores.value = await listStores()
  if (stores.value.length && !storeId.value) {
    storeId.value = stores.value[0].id
    loadAll()
  }
}

async function loadAll() {
  if (!storeId.value) return
  loading.value = true
  selected.value = null
  dirty.value = false
  try {
    const [seatList, typeList] = await Promise.all([
      listSeats(storeId.value),
      listSeatTypes()
    ])
    seatTypes.value = typeList
    seats.value = seatList.map(s => ({ ...s, _key: 's' + (s.id ?? 'n' + keySeq.value++) }))
  } finally {
    loading.value = false
  }
}

/* ============ 添加座位（FR-AD-006） ============ */

function startAdd() {
  adding.value = true
}

function onCanvasMouseDown(e) {
  if (!adding.value) {
    selected.value = null
    return
  }
  const rect = canvasRef.value.getBoundingClientRect()
  const x = snap(e.clientX - rect.left - SEAT_W / 2)
  const y = snap(e.clientY - rect.top - SEAT_H / 2)
  const nextNo = autoSeatNo()
  const seat = {
    id: null,
    _key: 'n' + keySeq.value++,
    seatNo: nextNo,
    seatTypeId: seatTypes.value[0]?.id,
    seatTypeName: seatTypes.value[0]?.name,
    areaCode: null,
    xCoord: Math.max(0, x),
    yCoord: Math.max(0, y),
    floor: '1F',
    description: null,
    status: 0,
    statusDesc: '空闲',
    hasActiveOrder: false
  }
  seats.value.push(seat)
  selected.value = seat
  dirty.value = true
  adding.value = false
  ElMessage.success(`已添加座位 ${nextNo}，可在右侧编辑属性`)
}

/** 自动编号：A-01、A-02…B-01（跳过已占用编号） */
function autoSeatNo() {
  const existing = new Set(seats.value.map(s => s.seatNo))
  for (let i = 0; i < 26; i++) {
    const prefix = String.fromCharCode(65 + i)
    for (let n = 1; n <= 99; n++) {
      const no = `${prefix}-${String(n).padStart(2, '0')}`
      if (!existing.has(no)) return no
    }
  }
  return 'NEW-' + Date.now()
}

function snap(v) {
  return Math.round(v / SNAP) * SNAP
}

/* ============ 拖拽（FR-AD-006） ============ */

function onSeatMouseDown(e, seat) {
  selected.value = seat
  draggingSeat.value = seat
  dragState.startX = e.clientX
  dragState.startY = e.clientY
  dragState.originX = seat.xCoord
  dragState.originY = seat.yCoord
  dragState.moved = false
  document.addEventListener('mousemove', onDocMouseMove)
  document.addEventListener('mouseup', onDocMouseUp)
}

function onDocMouseMove(e) {
  if (!draggingSeat.value) return
  const dx = e.clientX - dragState.startX
  const dy = e.clientY - dragState.startY
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
    dragState.moved = true
  }
  draggingSeat.value.xCoord = Math.max(0, snap(dragState.originX + dx))
  draggingSeat.value.yCoord = Math.max(0, snap(dragState.originY + dy))
}

function onDocMouseUp() {
  if (draggingSeat.value && dragState.moved) {
    dirty.value = true
  }
  draggingSeat.value = null
  document.removeEventListener('mousemove', onDocMouseMove)
  document.removeEventListener('mouseup', onDocMouseUp)
}

function onKeydown(e) {
  if (e.key === 'Escape') {
    adding.value = false
  }
}

/* ============ 删除 / 保存（FR-AD-006） ============ */

function removeSelected() {
  if (!selected.value) return
  const idx = seats.value.indexOf(selected.value)
  if (idx > -1) {
    seats.value.splice(idx, 1)
  }
  selected.value = null
  dirty.value = true
}

async function onSave() {
  // 前置校验：编号非空 + 不重复
  const nos = seats.value.map(s => (s.seatNo || '').trim())
  if (nos.some(n => !n)) {
    ElMessage.warning('存在未填写编号的座位')
    return
  }
  if (new Set(nos).size !== nos.length) {
    ElMessage.warning('存在重复的座位编号，请检查')
    return
  }
  saving.value = true
  try {
    await saveSeatLayout(
      storeId.value,
      seats.value.map(s => ({
        id: s.id,
        seatNo: s.seatNo.trim(),
        seatTypeId: s.seatTypeId,
        areaCode: s.areaCode,
        xCoord: s.xCoord,
        yCoord: s.yCoord,
        floor: s.floor,
        description: s.description
      }))
    )
    ElMessage.success('座位布局已保存')
    loadAll()
  } finally {
    saving.value = false
  }
}

/* ============ 状态管理（FR-AD-007） ============ */

async function changeStatus(seat, target) {
  await changeSeatStatus(seat.id, target)
  ElMessage.success(target === 0 ? `座位 ${seat.seatNo} 已恢复空闲` : '状态已更新')
  loadAll()
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
  loadStores()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  document.removeEventListener('mousemove', onDocMouseMove)
  document.removeEventListener('mouseup', onDocMouseUp)
})
</script>

<style scoped>
.filter-card {
  margin-bottom: 12px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.left {
  display: flex;
  align-items: center;
  gap: 4px;
}
.label {
  font-size: 14px;
  color: #606266;
}
.dirty-tip {
  color: #e6a23c;
  font-size: 12px;
  margin-left: 10px;
}
.legend {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #606266;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
.adding-tip {
  margin-top: 8px;
}
.canvas {
  position: relative;
  background-image:
    linear-gradient(#f0f2f5 1px, transparent 1px),
    linear-gradient(90deg, #f0f2f5 1px, transparent 1px);
  background-size: 20px 20px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  overflow: hidden;
  cursor: default;
  user-select: none;
}
.canvas:has(.seat:hover) {
  cursor: pointer;
}
.seat {
  position: absolute;
  width: 76px;
  height: 44px;
  border: 2px solid #67c23a;
  border-radius: 6px;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: grab;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}
.seat:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.16);
}
.seat.selected {
  outline: 2px solid #409eff;
  outline-offset: 1px;
}
.seat.dragging {
  cursor: grabbing;
  opacity: 0.85;
  z-index: 10;
}
.seat-no {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}
.seat-status {
  font-size: 10px;
  line-height: 1.2;
}
.muted {
  color: #909399;
  font-size: 12px;
}
.status-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.status-tip {
  width: 100%;
  line-height: 1.5;
}
.panel-card {
  position: sticky;
  top: 8px;
}
</style>
