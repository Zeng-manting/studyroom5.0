<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h2>无人自习室管理后台</h2>
        <p>Management Console</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @keyup.enter="onSubmit"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入账号"
            :prefix-icon="User"
            autocomplete="username"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>

        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input
              v-model="form.captchaCode"
              placeholder="验证码"
              :prefix-icon="Key"
              maxlength="4"
              @keyup.enter="onSubmit"
            />
            <!-- FR-AD-001：图形验证码，点击刷新 -->
            <img
              v-if="captcha.image"
              :src="captcha.image"
              class="captcha-img"
              title="点击刷新验证码"
              @click="loadCaptcha"
            />
            <div v-else class="captcha-img captcha-loading" @click="loadCaptcha">
              加载中...
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="onSubmit"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-tip">JWT 有效期 2 小时，过期后自动跳转本页重新认证</div>
    </div>
  </div>
</template>

<script setup>
/**
 * 管理端登录页 —— FR-AD-001
 * 账号 + 密码 + 图形验证码；连续失败 5 次锁定 10 分钟（后端校验）。
 * 登录成功跳转 redirect 或首页。
 */
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getCaptcha } from '@/api/auth'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)
const captcha = reactive({ image: '' })

const form = reactive({
  username: '',
  password: '',
  captchaId: '',
  captchaCode: ''
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 4, message: '验证码为 4 位字符', trigger: 'blur' }
  ]
}

async function loadCaptcha() {
  try {
    const data = await getCaptcha()
    captcha.image = data.image
    form.captchaId = data.captchaId
    form.captchaCode = ''
  } catch (e) {
    captcha.image = ''
  }
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await authStore.login({ ...form })
    ElMessage.success('登录成功')
    router.replace(String(route.query.redirect || '/'))
  } catch (e) {
    // 无论成功失败，验证码均已一次性作废，需重新获取
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1d39c4 0%, #2f54eb 50%, #4096ff 100%);
}

.login-card {
  width: 400px;
  padding: 40px 36px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(0, 21, 41, 0.25);
}

.login-header {
  text-align: center;
  margin-bottom: 28px;
}

.login-header h2 {
  font-size: 22px;
  color: #1d2129;
  letter-spacing: 2px;
}

.login-header p {
  margin-top: 6px;
  font-size: 12px;
  color: #a9aeb8;
  letter-spacing: 1px;
}

.captcha-row {
  display: flex;
  width: 100%;
  gap: 10px;
}

.captcha-img {
  width: 130px;
  height: 40px;
  border-radius: 4px;
  cursor: pointer;
  flex-shrink: 0;
}

.captcha-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f2f3f5;
  color: #a9aeb8;
  font-size: 12px;
}

.login-btn {
  width: 100%;
}

.login-tip {
  text-align: center;
  font-size: 12px;
  color: #a9aeb8;
  margin-top: 4px;
}
</style>
