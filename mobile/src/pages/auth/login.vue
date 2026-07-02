<template>
  <view class="page">
    <!-- 品牌区 -->
    <view class="brand">
      <view class="brand-icon">💪</view>
      <text class="brand-title">智训业财云</text>
      <text class="brand-sub">一站式健身房智能管理平台</text>
    </view>

    <!-- 角色切换 -->
    <view class="role-tabs">
      <view
        v-for="r in roles"
        :key="r.value"
        class="role-tab"
        :class="{ active: form.role === r.value }"
        @tap="form.role = r.value"
      >
        <text class="role-tab-icon">{{ r.icon }}</text>
        <text class="role-tab-text">{{ r.label }}</text>
      </view>
    </view>

    <!-- 表单卡片 -->
    <view class="form-card">
      <view class="form-item">
        <text class="form-label">用户名</text>
        <input
          v-model="form.username"
          class="form-input"
          placeholder="请输入用户名"
          placeholder-style="color:#bbb"
          :disabled="loading"
        />
      </view>

      <view class="form-item">
        <text class="form-label">密码</text>
        <input
          v-model="form.password"
          class="form-input"
          placeholder="请输入密码"
          placeholder-style="color:#bbb"
          :password="!showPwd"
          :disabled="loading"
        />
        <view class="pwd-toggle" @tap="showPwd = !showPwd">
          <text>{{ showPwd ? '🙈' : '👁' }}</text>
        </view>
      </view>

      <button class="btn-login" :class="{ loading }" :disabled="invalid" @tap="handleLogin">
        <text v-if="!loading">登 录</text>
        <text v-else>登录中...</text>
      </button>

      <view class="form-foot">
        <text class="link" @tap="gotoRegister">快速注册</text>
        <text class="link" @tap="gotoForgot">忘记密码</text>
      </view>
    </view>

    <!-- 错误提示 -->
    <view v-if="error" class="error-box">
      <text>{{ error }}</text>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { reactive, ref, computed } from 'vue'
import { login, type UserRole } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const showPwd = ref(false)
const error = ref('')

const roles = [
  { value: 'MEMBER' as UserRole, label: '会员', icon: '🏃' },
  { value: 'COACH' as UserRole, label: '教练', icon: '🧑‍🏫' },
  { value: 'ADMIN' as UserRole, label: '管理', icon: '🛠' },
]

const form = reactive({
  role: 'MEMBER' as UserRole,
  username: '',
  password: '',
})

const invalid = computed(() => !form.username.trim() || !form.password.trim())

async function handleLogin() {
  if (invalid.value || loading.value) return
  loading.value = true
  error.value = ''

  try {
    const result = await login({
      username: form.username.trim(),
      password: form.password,
      role: form.role,
    })
    userStore.setUser(result)

    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/index/index' })
    }, 600)
  } catch (e) {
    error.value = (e as Error).message || '登录失败，请重试'
  } finally {
    loading.value = false
  }
}

function gotoRegister() {
  uni.navigateTo({ url: '/pages/auth/register' })
}

function gotoForgot() {
  uni.navigateTo({ url: '/pages/auth/forgot-password' })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  padding: 80rpx 48rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 48rpx;
}

.brand-icon {
  font-size: 64rpx;
  margin-bottom: 16rpx;
}

.brand-title {
  font-size: 44rpx;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8rpx;
}

.brand-sub {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.7);
}

/* 角色切换 */
.role-tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 32rpx;
}

.role-tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16rpx 32rpx;
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.12);
  transition: all 0.3s;

  &.active {
    background: #fff;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.1);
  }
}

.role-tab-icon {
  font-size: 36rpx;
  margin-bottom: 4rpx;
}

.role-tab-text {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);

  .active & {
    color: #2563eb;
    font-weight: 600;
  }
}

/* 表单 */
.form-card {
  width: 100%;
  background: #fff;
  border-radius: 24rpx;
  padding: 48rpx 40rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.12);
}

.form-item {
  margin-bottom: 32rpx;
  position: relative;
}

.form-label {
  font-size: 28rpx;
  color: #666;
  margin-bottom: 12rpx;
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

.btn-login {
  width: 100%;
  height: 96rpx;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  border-radius: 16rpx;
  font-size: 32rpx;
  font-weight: 600;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 8rpx;

  &.loading {
    opacity: 0.7;
  }
}

.form-foot {
  display: flex;
  justify-content: space-between;
  margin-top: 24rpx;
}

.link {
  font-size: 26rpx;
  color: #2563eb;
  padding: 8rpx 0;
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
</style>
