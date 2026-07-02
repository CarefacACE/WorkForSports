<template>
  <view class="page">
    <view class="brand">
      <text class="brand-title">创建账号</text>
      <text class="brand-sub">加入智训业财云</text>
    </view>

    <!-- 角色选择 -->
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

    <!-- 注册表单 -->
    <view class="form-card">
      <view class="form-item">
        <text class="form-label">用户名 <text class="required">*</text></text>
        <input
          v-model="form.username"
          class="form-input"
          placeholder="字母开头，4-20位"
          placeholder-style="color:#bbb"
          :disabled="loading"
        />
      </view>

      <view class="form-item">
        <text class="form-label">真实姓名 <text class="required">*</text></text>
        <input
          v-model="form.realName"
          class="form-input"
          placeholder="请输入真实姓名"
          placeholder-style="color:#bbb"
          :disabled="loading"
        />
      </view>

      <view class="form-item">
        <text class="form-label">手机号</text>
        <input
          v-model="form.phone"
          class="form-input"
          type="number"
          maxlength="11"
          placeholder="选填"
          placeholder-style="color:#bbb"
          :disabled="loading"
        />
      </view>

      <view class="form-item">
        <text class="form-label">邮箱</text>
        <input
          v-model="form.email"
          class="form-input"
          placeholder="选填"
          placeholder-style="color:#bbb"
          :disabled="loading"
        />
      </view>

      <view class="form-item">
        <text class="form-label">密码 <text class="required">*</text></text>
        <input
          v-model="form.password"
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

      <button class="btn-submit" :class="{ loading }" :disabled="invalid" @tap="handleRegister">
        <text v-if="!loading">注 册</text>
        <text v-else>注册中...</text>
      </button>

      <view class="form-foot">
        <text class="link" @tap="gotoLogin">已有账号？立即登录</text>
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
import { register, type UserRole } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const showPwd = ref(false)
const error = ref('')

const roles = [
  { value: 'MEMBER' as UserRole, label: '会员', icon: '🏃' },
  { value: 'COACH' as UserRole, label: '教练', icon: '🧑‍🏫' },
]

const form = reactive({
  role: 'MEMBER' as UserRole,
  username: '',
  realName: '',
  phone: '',
  email: '',
  password: '',
})

const invalid = computed(() => !form.username.trim() || !form.realName.trim() || form.password.length < 6)

async function handleRegister() {
  if (invalid.value || loading.value) return
  loading.value = true
  error.value = ''

  try {
    const result = await register({
      username: form.username.trim(),
      realName: form.realName.trim(),
      role: form.role,
      phone: form.phone.trim() || undefined,
      email: form.email.trim() || undefined,
      password: form.password,
    })
    userStore.setUser(result)

    uni.showToast({ title: '注册成功', icon: 'success' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/index/index' })
    }, 600)
  } catch (e) {
    error.value = (e as Error).message || '注册失败，请重试'
  } finally {
    loading.value = false
  }
}

function gotoLogin() {
  uni.navigateBack()
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
  margin-bottom: 32rpx;
}

.brand-title {
  font-size: 40rpx;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8rpx;
}

.brand-sub {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.7);
}

/* 角色 */
.role-tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.role-tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12rpx 36rpx;
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.12);

  &.active {
    background: #fff;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.1);
  }
}

.role-tab-icon {
  font-size: 32rpx;
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
  padding: 40rpx 36rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.12);
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

.required {
  color: #ff4d4f;
}

.form-input {
  width: 100%;
  height: 80rpx;
  border: 2rpx solid #e5e7eb;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
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
  right: 16rpx;
  bottom: 16rpx;
  font-size: 34rpx;
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

  &.loading {
    opacity: 0.7;
  }
}

.form-foot {
  margin-top: 20rpx;
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
</style>
