<template>
  <div class="qrcode-page">
    <el-row :gutter="12">
      <!-- 生成配置 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>签到二维码生成（FR-AD-008）</template>
          <el-form label-width="90px">
            <el-form-item label="门店">
              <el-select v-model="storeId" style="width: 100%" placeholder="选择门店">
                <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="有效期">
              <el-select v-model="validHours" style="width: 100%">
                <el-option :label="1" :value="1" />
                <el-option :label="2" :value="2" />
                <el-option :label="4" :value="4" />
                <el-option :label="8" :value="8" />
                <el-option :label="12" :value="12" />
                <el-option :label="24" :value="24" />
                <el-option :label="72" :value="72" />
              </el-select>
              <div class="muted">小时；过期后扫码将提示"签到码已过期，请联系店员刷新"</div>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :icon="Refresh"
                :loading="generating"
                :disabled="!storeId"
                @click="onGenerate"
              >
                {{ qrData ? '刷新签到码' : '生成签到码' }}
              </el-button>
            </el-form-item>
          </el-form>

          <el-alert
            v-if="qrData"
            type="success"
            :closable="false"
            :title="`有效期至 ${fmt(qrData.expireAt)}`"
          />

          <el-divider content-position="left">安全说明</el-divider>
          <div class="security muted">
            二维码内容为 <code>SR-SIGN:{AES-256密文}:{HMAC-SHA256签名}</code>，
            与用户端签到五重校验（FR-UC-030）同源：防伪造（AES）、防篡改（HMAC 常量时间比较）、
            防重放（exp 有效期）。每次生成均为新随机 nonce，旧码在过期前仍有效，建议定期刷新张贴。
          </div>
        </el-card>
      </el-col>

      <!-- 预览 / 下载 / 打印 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>打印预览</template>
          <div v-if="qrData" class="preview">
            <div class="poster" ref="posterRef">
              <div class="poster-title">{{ qrData.storeName }} · 扫码签到</div>
              <img v-if="qrDataUrl" :src="qrDataUrl" class="qr-img" alt="签到二维码" />
              <div class="poster-sub">到店后使用微信扫码，按提示完成签到</div>
              <div class="poster-exp">本码有效期至：{{ fmt(qrData.expireAt) }}</div>
            </div>
            <div class="actions">
              <el-button type="primary" :icon="Download" @click="onDownload">下载 PNG</el-button>
              <el-button :icon="Printer" @click="onPrint">打印</el-button>
            </div>
          </div>
          <el-empty v-else description="先生成签到码查看预览" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
/**
 * 管理端-签到二维码管理 —— FR-AD-008
 * 生成（AES+HMAC）、刷新、下载 PNG、打印。
 * QR 图片由前端 qrcode 库从内容字符串渲染（内容本身已加密签名，无泄露风险）。
 */
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Download, Printer } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import { generateQrCode, listStores } from '@/api/store'

const stores = ref([])
const storeId = ref(null)
const validHours = ref(4)
const generating = ref(false)
const qrData = ref(null)
const qrDataUrl = ref('')
const posterRef = ref(null)

async function loadStores() {
  stores.value = await listStores()
  if (stores.value.length) {
    storeId.value = stores.value[0].id
  }
}

async function onGenerate() {
  generating.value = true
  try {
    const data = await generateQrCode(storeId.value, validHours.value)
    qrData.value = data
    await nextTick()
    qrDataUrl.value = await QRCode.toDataURL(data.code, {
      width: 480,
      margin: 2,
      errorCorrectionLevel: 'M'
    })
  } finally {
    generating.value = false
  }
}

function onDownload() {
  const a = document.createElement('a')
  a.href = qrDataUrl.value
  a.download = `签到码_${qrData.value.storeName}_${new Date().toISOString().slice(0, 10)}.png`
  a.click()
}

function onPrint() {
  const w = window.open('', '_blank', 'width=640,height=800')
  if (!w) {
    ElMessage.warning('浏览器拦截了打印窗口，请允许弹窗后重试')
    return
  }
  w.document.write(`<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><title>签到二维码 - ${qrData.value.storeName}</title>
<style>
  body { font-family: "Microsoft YaHei", sans-serif; display: flex; flex-direction: column;
         align-items: center; padding-top: 40px; }
  h1 { font-size: 26px; }
  img { width: 380px; height: 380px; }
  .sub { font-size: 16px; color: #555; margin: 12px 0 4px; }
  .exp { font-size: 13px; color: #999; }
</style></head>
<body>
  <h1>${qrData.value.storeName} · 扫码签到</h1>
  <img src="${qrDataUrl.value}" />
  <div class="sub">到店后使用微信扫码，按提示完成签到</div>
  <div class="exp">本码有效期至：${fmt(qrData.value.expireAt)}</div>
</body></html>`)
  w.document.close()
  w.focus()
  w.print()
}

function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : ''
}

onMounted(loadStores)
</script>

<style scoped>
.muted {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}
.security code {
  background: #f5f7fa;
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 11px;
}
.preview {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.poster {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 24px 32px;
  text-align: center;
  background: #fff;
}
.poster-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 14px;
}
.qr-img {
  width: 300px;
  height: 300px;
}
.poster-sub {
  margin-top: 10px;
  font-size: 14px;
  color: #606266;
}
.poster-exp {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
.actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}
</style>
