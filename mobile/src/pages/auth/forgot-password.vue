<template>
  <view class="page">
    <view class="form-card">
      <text class="form-title">忘记密码</text>
      <text class="form-desc">通过邮箱/手机验证码重置密码</text>

      <!-- 步骤 1：发送验证码 -->
      <view v-if="step === 1" class="step">
        <view class="form-item">
          <text class="form-label">邮箱或手机号</text>
          <input
            v-model="contact"
            class="form-input"
            placeholder="请输入注册时的邮箱或手机号"
            placeholder-style="color:#bbb"
            :disabled="sending"
          />
        </view>
        <button class="btn-submit" :disabled="!contact.trim() || sending" @tap="handleSendCode">
          <text v-if="!sending">发送验证码</text>
          <text v-else>{{ countdown }}s 后重发</text>
        </button>
      </view>

      <!-- 步骤 2：输入验证码和新密码 -->
      <view v-else class="step">
        <view class="form-item">
          <text class="form-label">验证码</text>
          <input
            v-model="code"
            class="form-input"
            type="number"
            maxlength="6"
            placeholder="请输入6位验证码"
            placeholder-style="color:#bbb"
            :disabled="loading"
          />
        </view>
        <view class="form-item">
          <text class="form-label">新密码</text>
          <input
            v-model="newPassword"
            class="form-input"
            placeholder="至少6位"
            placeholder-style="color:#bbb"
            :password="!showPwd"
            :disabled="loading"
          />
          <view class="pwd-toggle" @tap="showPwd = !showPwd">
            <text>{{ showPwd ? '🙈' : '👁' }}</text>
          </view>
        </view>
        <button class="btn-submit" :disabled="!validReset || loading" @tap="handleReset">
          <text v-if="!loading">重置密码</text>
          <text v-else>重置中...</text>
        </button>
      </view>

      <view class="form-foot">
        <text class="link" @tap="gotoLogin">返回登录</text>
      </view>
    </view>

    <view v-if="error" class="error-box">
      <text>{{ error }}</text>
    </view>
    <view v-if="success" class="success-box">
      <text>{{ success }}</text>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { sendCode, resetPasswordByCode } from '@/api/auth'

const step = ref(1)
const contact = ref('')
const code = ref('')
const newPassword = ref('')
const showPwd = ref(false)
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
const error = ref('')
const success = ref('')

const validReset = computed(() => code.value.trim().length >= 4 && newPassword.value.length >= 6)

let timer: ReturnType<typeof setInterval> | null = null

async function handleSendCode() {
  if (!contact.value.trim() || sending.value) return
  sending.value = true
  error.value = ''

  try {
    await sendCode(contact.value.trim())
    step.value = 2
    success.value = '验证码已发送，请查收'

    // 60 秒倒计时
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        if (timer) clearInterval(timer)
        sending.value = false
      }
    }, 1000)
  } catch (e) {
    error.value = (e as Error).message || '发送失败'
    sending.value = false
  }

  setTimeout(() => { success.value = '' }, 3000)
}

async function handleReset() {
  if (!validReset.value || loading.value) return
  loading.value = true
  error.value = ''

  try {
    await resetPasswordByCode(contact.value.trim(), code.value.trim(), newPassword.value)
    success.value = '密码重置成功，请登录'
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  } catch (e) {
    error.value = (e as Error).message || '重置失败'
  } finally {
    loading.value = false
  }

  setTimeout(() => { success.value = '' }, 3000)
}

function gotoLogin() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  padding: 120rpx 48rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.form-card {
  width: 100%;
  background: #fff;
  border-radius: 24rpx;
  padding: 48rpx 40rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.12);
}

.form-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #333;
  display: block;
  text-align: center;
  margin-bottom: 8rpx;
}

.form-desc {
  font-size: 24rpx;
  color: #999;
  display: block;
  text-align: center;
  margin-bottom: 32rpx;
}

.step {
  display: flex;
  flex-direction: column;
}

.form-item {
  margin-bottom: 24rpx;
  position: relative;
}

.form-label {
  font-size: 26rpx;
  color: #666;
  margin-bottom: 8rpx;
  display: block;
}

.form-input {
  width: 100%;
  height: 88rpx;
  border: 2rpx solid #e5e7eb;
  border-radius: 16rpx;
  padding: 0 24rpx;
  font-size: 30rpx;
  color: #333;
  background: #f9fafb;
  box-sizing: border-box;

  &:focus {
    border-color: #2563eb;
    background: #fff;
  }
}

.pwd-toggle {
  position: absolute;
  right: 20rpx;
  bottom: 20rpx;
  font-size: 36rpx;
}

.btn-submit {
  width: 100%;
  height: 88rpx;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  border-radius: 16rpx;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 8rpx;
}

.form-foot {
  margin-top: 24rpx;
  text-align: center;
}

.link {
  font-size: 26rpx;
  color: #2563eb;
}

.error-box {
  margin-top: 24rpx;
  padding: 16rpx 24rpx;
  background: rgba(255, 77, 79, 0.15);
  border-radius: 12rpx;

  text {
    color: #ff4d4f;
    font-size: 26rpx;
  }
}

.success-box {
  margin-top: 24rpx;
  padding: 16rpx 24rpx;
  background: rgba(82, 196, 26, 0.15);
  border-radius: 12rpx;

  text {
    color: #52c41a;
    font-size: 26rpx;
  }
}
</style>
