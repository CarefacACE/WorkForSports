<template>
  <view class="page">
    <view class="header">
      <text class="greeting">🛠 管理员 {{ userStore.user?.realName || userStore.user?.username }}</text>
      <text class="sub">{{ todayStr }}</text>
    </view>

    <view class="stats-row">
      <view class="stat">
        <text class="stat-val">{{ stats.users }}</text>
        <text class="stat-lbl">用户数</text>
      </view>
      <view class="stat" @tap="goto('/pages/admin/course-approval')">
        <text class="stat-val warn">{{ stats.pending }}</text>
        <text class="stat-lbl">待审批</text>
      </view>
      <view class="stat">
        <text class="stat-val">{{ stats.courses }}</text>
        <text class="stat-lbl">课程数</text>
      </view>
      <view class="stat" @tap="goto('/pages/profile/wallet')">
        <text class="stat-val">¥{{ stats.revenue }}</text>
        <text class="stat-lbl">总收入</text>
      </view>
    </view>

    <view class="quick-actions">
      <view class="qa-item" @tap="goto('/pages/admin/course-approval')">
        <text class="qa-icon">📝</text><text class="qa-text">课程审批</text>
        <text v-if="stats.pending > 0" class="qa-badge">{{ stats.pending }}</text>
      </view>
      <view class="qa-item" @tap="goto('/pages/admin/user-manage')">
        <text class="qa-icon">👥</text><text class="qa-text">用户管理</text>
      </view>
      <view class="qa-item" @tap="goto('/pages/gym/index')">
        <text class="qa-icon">🏢</text><text class="qa-text">健身房</text>
      </view>
      <view class="qa-item" @tap="goto('/pages/gym/supermarket')">
        <text class="qa-icon">🛒</text><text class="qa-text">超市管理</text>
      </view>
    </view>

    <view class="section">
      <text class="section-title">快捷操作</text>
      <view class="action-list">
        <view class="action" @tap="goto('/pages/courses/public-list')">
          <text>📚 浏览所有课程</text><text>→</text>
        </view>
        <view class="action" @tap="goto('/pages/chat/conversations')">
          <text>💬 消息管理</text><text>→</text>
        </view>
        <view class="action" @tap="goto('/pages/profile/index')">
          <text>⚙️ 个人设置</text><text>→</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { getPendingCourseCount, getAllCoursesForAdmin } from '@/api/course'
import { getUsers } from '@/api/user'

const userStore = useUserStore()

const todayStr = computed(() => new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }))

const stats = ref({ users: 0, pending: 0, courses: 0, revenue: 0 })

function goto(url: string) { uni.navigateTo({ url }) }

async function fetchData() {
  try {
    const [pendingCount, allCourses, users] = await Promise.all([
      getPendingCourseCount().catch(() => 0),
      getAllCoursesForAdmin().catch(() => []),
      getUsers({ pageNum: 1, pageSize: 1, role: 'MEMBER' }).catch(() => ({ total: 0 })),
    ])
    stats.value = {
      pending: typeof pendingCount === 'number' ? pendingCount : 0,
      courses: Array.isArray(allCourses) ? allCourses.length : 0,
      users: (users as { total: number }).total || 0,
      revenue: 0,
    }
  } catch { /* */ }
}

fetchData()
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; padding-bottom: 32rpx; }

.header {
  background: linear-gradient(135deg, #1e40af, #1e3a8a);
  padding: 48rpx 32rpx 56rpx;
  border-radius: 0 0 32rpx 32rpx;
}
.greeting { font-size: 34rpx; font-weight: 700; color: #fff; display: block; margin-bottom: 8rpx; }
.sub { font-size: 24rpx; color: rgba(255,255,255,0.7); }

.stats-row { display: flex; gap: 12rpx; margin: -24rpx 24rpx 16rpx; }
.stat {
  flex: 1; background: #fff; border-radius: 16rpx; padding: 20rpx 8rpx;
  display: flex; flex-direction: column; align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}
.stat-val { font-size: 32rpx; font-weight: 700; color: #2563eb; }
.stat-val.warn { color: #e6a23c; }
.stat-lbl { font-size: 20rpx; color: #999; }

.quick-actions { display: flex; gap: 16rpx; margin: 8rpx 24rpx; }
.qa-item {
  flex: 1; background: #fff; border-radius: 16rpx; padding: 24rpx 8rpx;
  display: flex; flex-direction: column; align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04); position: relative;
}
.qa-icon { font-size: 40rpx; margin-bottom: 8rpx; }
.qa-text { font-size: 22rpx; color: #666; }
.qa-badge {
  position: absolute; top: 12rpx; right: 12rpx;
  min-width: 36rpx; height: 36rpx; line-height: 36rpx; text-align: center;
  background: #ff4d4f; color: #fff; font-size: 20rpx; border-radius: 18rpx; padding: 0 8rpx;
}

.section { margin: 16rpx 24rpx; }
.section-title { font-size: 28rpx; font-weight: 600; color: #333; margin-bottom: 12rpx; display: block; }

.action-list { background: #fff; border-radius: 16rpx; overflow: hidden; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04); }
.action {
  display: flex; justify-content: space-between; align-items: center;
  padding: 28rpx 24rpx; border-bottom: 2rpx solid #f5f6fa;
  font-size: 28rpx; color: #333;
  &:last-child { border-bottom: none; }
}
</style>
