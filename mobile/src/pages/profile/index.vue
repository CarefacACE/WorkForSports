<template>
  <view class="page">
    <!-- 头像区 -->
    <view class="profile-header">
      <view class="avatar-box">
        <image v-if="userStore.user?.avatar" :src="userStore.user.avatar" mode="aspectFill" class="avatar-img" />
        <view v-else class="avatar-placeholder">
          <text>{{ (userStore.user?.realName || '用户').charAt(0) }}</text>
        </view>
      </view>
      <text class="profile-name">{{ userStore.user?.realName || '未设置姓名' }}</text>
      <text class="profile-role">{{ roleLabel }}</text>
    </view>

    <!-- 快捷入口 -->
    <view class="quick-links">
      <view class="link-item" @tap="goto('/pages/profile/edit')">
        <text class="link-icon">👤</text>
        <text class="link-text">编辑资料</text>
        <text class="link-arrow">→</text>
      </view>
      <view class="link-item" @tap="goto('/pages/profile/health')">
        <text class="link-icon">❤️</text>
        <text class="link-text">健康档案</text>
        <text class="link-arrow">→</text>
      </view>
      <view class="link-item" @tap="goto('/pages/profile/wallet')">
        <text class="link-icon">💰</text>
        <text class="link-text">我的钱包</text>
        <view class="link-right">
          <text class="link-balance">¥{{ balance }}</text>
          <text class="link-arrow">→</text>
        </view>
      </view>
    </view>

    <!-- 功能入口 -->
    <view class="section">
      <text class="section-title">运动与训练</text>
      <view class="link-item" @tap="goto('/pages/exercise/index')">
        <text class="link-icon">📊</text>
        <text class="link-text">运动记录</text>
        <text class="link-arrow">→</text>
      </view>
      <view class="link-item" @tap="goto('/pages/plan/my-plan')">
        <text class="link-icon">📋</text>
        <text class="link-text">训练计划</text>
        <text class="link-arrow">→</text>
      </view>
      <view class="link-item" @tap="goto('/pages/checkin/index')">
        <text class="link-icon">✅</text>
        <text class="link-text">打卡</text>
        <text class="link-arrow">→</text>
      </view>
    </view>

    <view class="section">
      <text class="section-title">其他</text>
      <view class="link-item" @tap="goto('/pages/gym/index')">
        <text class="link-icon">🏢</text>
        <text class="link-text">健身房</text>
        <text class="link-arrow">→</text>
      </view>
      <view class="link-item" @tap="goto('/pages/gym/supermarket')">
        <text class="link-icon">🛒</text>
        <text class="link-text">运动超市</text>
        <text class="link-arrow">→</text>
      </view>
      <view class="link-item" @tap="goto('/pages/video/channel')">
        <text class="link-icon">🎬</text>
        <text class="link-text">健身视频</text>
        <text class="link-arrow">→</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-area">
      <button class="btn-logout" @tap="handleLogout">退出登录</button>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const balance = ref('0.00')

const roleLabel = computed(() => {
  const map: Record<string, string> = { MEMBER: '会员', COACH: '教练', ADMIN: '管理员' }
  return map[userStore.userRole || ''] || ''
})

function goto(url: string) {
  uni.navigateTo({ url })
}

function handleLogout() {
  uni.showModal({
    title: '退出登录',
    content: '确定要退出吗？',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
        uni.reLaunch({ url: '/pages/auth/login' })
      }
    },
  })
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; padding-bottom: 48rpx; }

.profile-header {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  padding: 60rpx 32rpx 48rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-radius: 0 0 32rpx 32rpx;
}

.avatar-box {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  overflow: hidden;
  margin-bottom: 16rpx;
  border: 4rpx solid rgba(255,255,255,0.3);
}

.avatar-img { width: 100%; height: 100%; }

.avatar-placeholder {
  width: 100%; height: 100%;
  background: rgba(255,255,255,0.2);
  display: flex; align-items: center; justify-content: center;
  font-size: 48rpx; color: #fff; font-weight: 700;
}

.profile-name { font-size: 36rpx; font-weight: 700; color: #fff; margin-bottom: 4rpx; }
.profile-role { font-size: 24rpx; color: rgba(255,255,255,0.7); }

/* 快捷入口 */
.quick-links {
  background: #fff;
  margin: 16rpx 24rpx;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}

.link-item {
  display: flex;
  align-items: center;
  padding: 28rpx 24rpx;
  border-bottom: 2rpx solid #f5f6fa;

  &:last-child { border-bottom: none; }
}

.link-icon { font-size: 36rpx; margin-right: 16rpx; width: 48rpx; text-align: center; }
.link-text { flex: 1; font-size: 28rpx; color: #333; }
.link-arrow { font-size: 24rpx; color: #ccc; }
.link-right { display: flex; align-items: center; gap: 12rpx; }
.link-balance { font-size: 26rpx; color: #f56c6c; font-weight: 600; }

.section {
  margin: 16rpx 24rpx;
}

.section-title {
  font-size: 24rpx;
  color: #999;
  padding: 8rpx 24rpx 12rpx;
  display: block;
}

.section .link-item {
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 8rpx;
  border-bottom: none;
}

.logout-area {
  padding: 48rpx 24rpx;
}

.btn-logout {
  width: 100%;
  height: 88rpx;
  background: #fff;
  color: #f56c6c;
  border-radius: 16rpx;
  font-size: 28rpx;
  border: 2rpx solid #fde2e2;
}
</style>
