<template>
  <!-- FR-UC-003 用户信息授权：微信头像昵称填写能力 -->
  <view class="page">
    <view class="card">
      <view class="row">
        <text class="label">头像</text>
        <!-- #ifdef MP-WEIXIN -->
        <!-- 微信头像填写能力：open-type="chooseAvatar" -->
        <button class="avatar-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
          <image class="avatar" :src="form.avatar || defaultAvatar" mode="aspectFill" />
          <text class="hint">点击更换</text>
        </button>
        <!-- #endif -->
        <!-- #ifdef H5 -->
        <view class="avatar-btn" @click="onChooseAvatarH5">
          <image class="avatar" :src="form.avatar || defaultAvatar" mode="aspectFill" />
          <text class="hint">点击更换</text>
        </view>
        <!-- #endif -->
      </view>

      <view class="row">
        <text class="label">昵称</text>
        <!-- 微信昵称填写能力：type="nickname"（H5 降级为普通文本输入） -->
        <input
          class="nickname-input"
          :type="nicknameType"
          v-model="form.nickname"
          placeholder="请输入昵称"
          :maxlength="64"
          @blur="onNicknameBlur"
        />
      </view>

      <view class="row">
        <text class="label">性别</text>
        <picker class="gender-picker" :range="genderOptions" :value="genderIndex" @change="onGenderChange">
          <view>{{ genderOptions[genderIndex] }}</view>
        </picker>
      </view>
    </view>

    <button class="save-btn" :loading="saving" :disabled="saving" @click="onSave">保存</button>
  </view>
</template>

<script setup>
/**
 * 个人资料编辑页 —— FR-UC-003
 * 头像：chooseAvatar 临时文件 → /file/upload 上传 → PUT /user/profile
 * 昵称：input type="nickname" 微信官方填写能力
 */
import { reactive, ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

const userStore = useUserStore()

const defaultAvatar = 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0'

// #ifdef H5
/** H5 无微信昵称填写能力，降级为普通文本输入 */
const nicknameType = 'text'
// #endif
// #ifndef H5
const nicknameType = 'nickname'
// #endif

const form = reactive({ avatar: '', nickname: '', gender: 0 })
const genderOptions = ['未设置', '男', '女']
const genderIndex = computed(() => form.gender || 0)
const saving = ref(false)

onMounted(() => {
  const u = userStore.userInfo
  if (u) {
    form.avatar = u.avatar || ''
    form.nickname = u.nickname || ''
    form.gender = u.gender || 0
  }
})

/** 选中微信头像（临时文件），立即上传换取 URL */
async function onChooseAvatar(e) {
  const tempPath = e.detail.avatarUrl
  uni.showLoading({ title: '上传中' })
  try {
    // FR-CC-003 当前替代方案：本地存储，接口不变，后续可切 COS
    const url = await request.upload(tempPath, 'avatar')
    form.avatar = url
  } finally {
    uni.hideLoading()
  }
}

// #ifdef H5
/** H5：使用浏览器文件选择上传头像 */
function onChooseAvatarH5() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: async (res) => {
      const tempPath = res.tempFilePaths[0]
      uni.showLoading({ title: '上传中' })
      try {
        form.avatar = await request.upload(tempPath, 'avatar')
      } finally {
        uni.hideLoading()
      }
    }
  })
}
// #endif

function onNicknameBlur(e) {
  form.nickname = (e.detail.value || '').trim()
}

function onGenderChange(e) {
  form.gender = Number(e.detail.value)
}

/** 保存资料：PUT /user/profile（FR-UC-003） */
async function onSave() {
  if (!form.nickname) {
    uni.showToast({ title: '请填写昵称', icon: 'none' })
    return
  }
  saving.value = true
  try {
    const user = await request.put('/user/profile', {
      avatar: form.avatar,
      nickname: form.nickname,
      gender: form.gender
    })
    userStore.setUser(user)
    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (e) {
    /* 错误提示由请求层统一处理 */
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { background: #fff; border-radius: 24rpx; padding: 8rpx 32rpx; }
.row {
  display: flex; align-items: center;
  padding: 28rpx 0; border-bottom: 1rpx solid #f0f0f0;
}
.row:last-child { border-bottom: none; }
.label { width: 140rpx; color: #606266; }
.avatar-btn {
  display: flex; align-items: center; background: transparent; padding: 0; margin: 0;
  line-height: normal;
}
.avatar-btn::after { border: none; }
.avatar { width: 108rpx; height: 108rpx; border-radius: 50%; background: #f0f0f0; }
.hint { margin-left: 24rpx; color: #909399; font-size: 26rpx; }
.nickname-input { flex: 1; text-align: right; }
.gender-picker { flex: 1; text-align: right; color: #303133; }
.save-btn {
  margin-top: 48rpx; background: #4a7cf7; color: #fff;
  border-radius: 999rpx; font-size: 32rpx;
}
.save-btn[disabled] { opacity: .6; }
</style>
