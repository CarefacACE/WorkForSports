<template>
  <view class="page">
    <view class="form-card">
      <!-- 头像 -->
      <view class="avatar-row" @tap="handleChooseAvatar">
        <image v-if="form.avatar" :src="form.avatar" mode="aspectFill" class="avatar-img" />
        <view v-else class="avatar-placeholder">
          <text>{{ (form.realName || '用户').charAt(0) }}</text>
        </view>
        <text class="avatar-hint">点击更换头像</text>
      </view>

      <view class="form-item">
        <text class="form-label">真实姓名 <text class="required">*</text></text>
        <input v-model="form.realName" class="form-input" placeholder="请输入" placeholder-style="color:#bbb" />
      </view>

      <view class="form-row">
        <view class="form-item half">
          <text class="form-label">性别</text>
          <picker :range="genders" :value="genderIndex" @change="onGenderChange">
            <view class="form-input picker">
              <text>{{ form.gender || '选择性别' }}</text>
            </view>
          </picker>
        </view>
        <view class="form-item half">
          <text class="form-label">生日</text>
          <picker mode="date" :value="form.birthday" @change="onBirthdayChange">
            <view class="form-input picker">
              <text>{{ form.birthday || '选择日期' }}</text>
            </view>
          </picker>
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">手机号</text>
        <input v-model="form.phone" class="form-input" type="number" maxlength="11" placeholder="选填" placeholder-style="color:#bbb" />
      </view>

      <view class="form-item">
        <text class="form-label">邮箱</text>
        <input v-model="form.email" class="form-input" placeholder="选填" placeholder-style="color:#bbb" />
      </view>

      <button class="btn-submit" :disabled="submitting" @tap="handleSave">
        <text>{{ submitting ? '保存中...' : '保存修改' }}</text>
      </button>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { reactive, ref, computed } from 'vue'
import { updateProfile } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const u = userStore.user
const submitting = ref(false)
const genders = ['男', '女']

const form = reactive({
  realName: u?.realName || '',
  phone: u?.phone || '',
  email: u?.email || '',
  gender: u?.gender || '',
  birthday: u?.birthday || '',
  avatar: u?.avatar || '',
})

const genderIndex = computed(() => (form.gender === '女' ? 1 : 0))

function onGenderChange(e: { detail: { value: number } }) {
  form.gender = genders[e.detail.value]
}

function onBirthdayChange(e: { detail: { value: string } }) {
  form.birthday = e.detail.value
}

function handleChooseAvatar() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: (res) => {
      form.avatar = res.tempFilePaths[0]
    },
  })
}

async function handleSave() {
  if (submitting.value || !u?.id) return
  submitting.value = true
  try {
    const updated = await updateProfile({
      id: u.id,
      realName: form.realName,
      phone: form.phone || undefined,
      email: form.email || undefined,
      gender: form.gender || undefined,
      birthday: form.birthday || undefined,
      avatar: form.avatar || undefined,
    })
    userStore.updateProfile(updated)
    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 800)
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '保存失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; padding: 24rpx; }

.form-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 40rpx 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}

.avatar-row {
  display: flex;
  align-items: center;
  margin-bottom: 32rpx;
}

.avatar-img, .avatar-placeholder {
  width: 96rpx; height: 96rpx; border-radius: 48rpx; overflow: hidden;
}
.avatar-img { }
.avatar-placeholder {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  display: flex; align-items: center; justify-content: center;
  font-size: 40rpx; color: #fff; font-weight: 700;
}

.avatar-hint {
  margin-left: 24rpx;
  font-size: 26rpx;
  color: #2563eb;
}

.form-item { margin-bottom: 24rpx; }
.form-label { font-size: 26rpx; color: #666; margin-bottom: 8rpx; display: block; }
.required { color: #ff4d4f; }

.form-row { display: flex; gap: 16rpx; }
.half { flex: 1; }

.form-input {
  height: 80rpx;
  border: 2rpx solid #e5e7eb;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  color: #333;
  background: #f9fafb;
  display: flex;
  align-items: center;
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
  margin-top: 16rpx;
}
</style>
