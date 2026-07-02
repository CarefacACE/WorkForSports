<template>
  <view class="page">
    <!-- 顶部问候 -->
    <view class="header">
      <view class="header-left">
        <text class="greeting">{{ greeting }}</text>
        <text class="username">{{ userStore.user?.realName || '用户' }}</text>
      </view>
      <view class="header-right">
        <view class="role-badge" :class="roleClass">
          {{ roleLabel }}
        </view>
      </view>
    </view>

    <!-- 快捷入口 -->
    <view class="quick-actions">
      <view class="action-card" @tap="goto('/pages/courses/public-list')">
        <text class="action-icon">📚</text>
        <text class="action-text">公共课程</text>
      </view>
      <view class="action-card" @tap="goto('/pages/courses/private-list')">
        <text class="action-icon">🧑‍🏫</text>
        <text class="action-text">私教课程</text>
      </view>
      <view class="action-card" @tap="goto('/pages/schedule/my-schedule')">
        <text class="action-icon">📅</text>
        <text class="action-text">我的课表</text>
      </view>
      <view class="action-card" @tap="goto('/pages/exercise/index')">
        <text class="action-icon">📊</text>
        <text class="action-text">运动记录</text>
      </view>
    </view>

    <!-- 今日概览 -->
    <view class="section">
      <text class="section-title">今日概览</text>
      <view class="stats-row">
        <view class="stat-card">
          <text class="stat-value">--</text>
          <text class="stat-label">今日课程</text>
        </view>
        <view class="stat-card">
          <text class="stat-value">--</text>
          <text class="stat-label">运动时长</text>
        </view>
        <view class="stat-card">
          <text class="stat-value">--</text>
          <text class="stat-label">消耗卡路里</text>
        </view>
      </view>
    </view>

    <!-- 最近课程 -->
    <view class="section">
      <view class="section-head">
        <text class="section-title">最近课程</text>
        <text class="section-more" @tap="goto('/pages/courses/my-courses')">查看全部 →</text>
      </view>
      <view class="empty">
        <text class="empty-icon">📭</text>
        <text class="empty-text">暂无课程记录</text>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const roleLabel = computed(() => {
  const map: Record<string, string> = { MEMBER: '会员', COACH: '教练', ADMIN: '管理' }
  return map[userStore.user?.role || ''] || ''
})

const roleClass = computed(() => {
  const map: Record<string, string> = { MEMBER: 'member', COACH: 'coach', ADMIN: 'admin' }
  return map[userStore.user?.role || ''] || ''
})

function goto(url: string) {
  uni.navigateTo({ url })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f5f6fa;
  padding-bottom: 32rpx;
}

.header {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  padding: 48rpx 32rpx 56rpx;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border-radius: 0 0 32rpx 32rpx;
}

.header-left {
  display: flex;
  flex-direction: column;
}

.greeting {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 8rpx;
}

.username {
  font-size: 40rpx;
  font-weight: 700;
  color: #fff;
}

.role-badge {
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 24rpx;

  &.member {
    background: rgba(255, 255, 255, 0.2);
    color: #fff;
  }
  &.coach {
    background: rgba(255, 255, 255, 0.25);
    color: #fff;
  }
  &.admin {
    background: rgba(255, 77, 79, 0.2);
    color: #ffd6d6;
  }
}

/* 快捷入口 */
.quick-actions {
  display: flex;
  gap: 16rpx;
  margin: -24rpx 24rpx 24rpx;
}

.action-card {
  flex: 1;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx 12rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.action-icon {
  font-size: 40rpx;
  margin-bottom: 8rpx;
}

.action-text {
  font-size: 22rpx;
  color: #666;
}

/* 区域 */
.section {
  margin: 0 24rpx 24rpx;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 16rpx;
  display: block;
}

.section-more {
  font-size: 24rpx;
  color: #2563eb;
}

/* 统计行 */
.stats-row {
  display: flex;
  gap: 16rpx;
}

.stat-card {
  flex: 1;
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx 16rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.stat-value {
  font-size: 36rpx;
  font-weight: 700;
  color: #2563eb;
  margin-bottom: 4rpx;
}

.stat-label {
  font-size: 22rpx;
  color: #999;
}

/* 空状态 */
.empty {
  background: #fff;
  border-radius: 16rpx;
  padding: 64rpx 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.empty-icon {
  font-size: 64rpx;
  margin-bottom: 12rpx;
}

.empty-text {
  font-size: 26rpx;
  color: #999;
}
</style>
