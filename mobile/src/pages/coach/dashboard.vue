<template>
  <view class="page">
    <!-- 欢迎 -->
    <view class="header">
      <text class="greeting">👋 教练 {{ userStore.user?.realName || userStore.user?.username }}</text>
      <text class="sub">{{ todayStr }}</text>
    </view>

    <!-- 统计卡片 -->
    <view class="stats-row" v-if="stats">
      <view class="stat" @tap="goto('/pages/coach/my-students')">
        <text class="stat-val">{{ stats.students || 0 }}</text>
        <text class="stat-lbl">学员数</text>
      </view>
      <view class="stat" @tap="goto('/pages/courses/my-courses')">
        <text class="stat-val">{{ stats.courses || 0 }}</text>
        <text class="stat-lbl">课程数</text>
      </view>
      <view class="stat" @tap="goto('/pages/schedule/my-schedule')">
        <text class="stat-val">{{ stats.schedules || 0 }}</text>
        <text class="stat-lbl">今日课</text>
      </view>
      <view class="stat" @tap="goto('/pages/profile/wallet')">
        <text class="stat-val">¥{{ stats.earnings || 0 }}</text>
        <text class="stat-lbl">收益</text>
      </view>
    </view>

    <!-- 我的课程 -->
    <view class="section">
      <view class="section-head">
        <text class="section-title">我的课程</text>
        <text class="section-more" @tap="goto('/pages/courses/my-courses')">查看全部 →</text>
      </view>
      <view v-if="courses.length === 0" class="hint">暂无课程</view>
      <view v-for="c in courses" :key="c.id" class="course-item">
        <view class="c-left">
          <text class="c-name">{{ c.name }}</text>
          <view class="c-tags">
            <text class="c-tag" :class="c.type">{{ c.type === 'PUBLIC' ? '公共课' : '私教' }}</text>
            <text class="c-price">¥{{ c.price }}</text>
          </view>
        </view>
        <text class="c-status" :class="c.status">{{ statusLabel(c.status) }}</text>
      </view>
    </view>

    <!-- 快捷入口 -->
    <view class="quick-actions">
      <view class="qa-item" @tap="goto('/pages/courses/public-list')">
        <text class="qa-icon">📚</text><text class="qa-text">创建课程</text>
      </view>
      <view class="qa-item" @tap="goto('/pages/schedule/my-schedule')">
        <text class="qa-icon">📅</text><text class="qa-text">课表管理</text>
      </view>
      <view class="qa-item" @tap="goto('/pages/checkin/index')">
        <text class="qa-icon">✅</text><text class="qa-text">打卡签到</text>
      </view>
      <view class="qa-item" @tap="goto('/pages/coach/profile')">
        <text class="qa-icon">⚙️</text><text class="qa-text">编辑档案</text>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { getMyCourses } from '@/api/course'
import { getCoachEnrollments } from '@/api/enrollment'

const userStore = useUserStore()
const coachId = userStore.userId || 0

const todayStr = computed(() => new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }))

const stats = ref({ students: 0, courses: 0, schedules: 0, earnings: 0 })
const courses = ref<Array<{ id: number; name: string; type: string; price: number; status: string }>>([])

function statusLabel(s: string) { return { ACTIVE: '进行中', PENDING: '待审批', REJECTED: '已驳回' }[s] || s }

function goto(url: string) { uni.navigateTo({ url }) }

async function fetchData() {
  if (!coachId) return
  try {
    const [c, e] = await Promise.all([
      getMyCourses(coachId, 1, 10),
      getCoachEnrollments(coachId, undefined, 1, 200),
    ])
    courses.value = (c.records || []).slice(0, 5)
    stats.value = {
      students: e.total || 0,
      courses: c.total || 0,
      schedules: 0, // 后续对接
      earnings: 0, // 后续对接
    }
  } catch { /* 静默 */ }
}

fetchData()
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; padding-bottom: 32rpx; }

.header {
  background: linear-gradient(135deg, #1d4ed8, #1e40af);
  padding: 48rpx 32rpx 56rpx;
  border-radius: 0 0 32rpx 32rpx;
}
.greeting { font-size: 36rpx; font-weight: 700; color: #fff; display: block; margin-bottom: 8rpx; }
.sub { font-size: 24rpx; color: rgba(255,255,255,0.7); }

.stats-row { display: flex; gap: 12rpx; margin: -24rpx 24rpx 16rpx; }
.stat {
  flex: 1; background: #fff; border-radius: 16rpx; padding: 20rpx 12rpx;
  display: flex; flex-direction: column; align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}
.stat-val { font-size: 32rpx; font-weight: 700; color: #2563eb; margin-bottom: 4rpx; }
.stat-lbl { font-size: 20rpx; color: #999; }

.section { margin: 16rpx 24rpx; }
.section-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.section-title { font-size: 30rpx; font-weight: 600; color: #333; }
.section-more { font-size: 24rpx; color: #2563eb; }
.hint { text-align: center; padding: 40rpx; font-size: 24rpx; color: #bbb; }

.course-item {
  background: #fff; border-radius: 12rpx; padding: 20rpx 24rpx;
  margin-bottom: 8rpx; display: flex; justify-content: space-between; align-items: center;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
}
.c-name { font-size: 28rpx; font-weight: 600; color: #333; display: block; margin-bottom: 4rpx; }
.c-tags { display: flex; gap: 8rpx; align-items: center; }
.c-tag { font-size: 20rpx; padding: 2rpx 10rpx; border-radius: 6rpx; }
.c-tag.PUBLIC { background: #e8f4fd; color: #2563eb; }
.c-tag.PRIVATE { background: #fef0d0; color: #e6a23c; }
.c-price { font-size: 22rpx; color: #f56c6c; font-weight: 600; }
.c-status { font-size: 22rpx; padding: 4rpx 14rpx; border-radius: 8rpx; }
.c-status.ACTIVE { background: #e8f8e8; color: #67c23a; }
.c-status.PENDING { background: #fef0d0; color: #e6a23c; }
.c-status.REJECTED { background: #fde2e2; color: #f56c6c; }

.quick-actions { display: flex; gap: 16rpx; margin: 8rpx 24rpx; }
.qa-item {
  flex: 1; background: #fff; border-radius: 16rpx; padding: 28rpx 8rpx;
  display: flex; flex-direction: column; align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}
.qa-icon { font-size: 40rpx; margin-bottom: 8rpx; }
.qa-text { font-size: 22rpx; color: #666; }
</style>
