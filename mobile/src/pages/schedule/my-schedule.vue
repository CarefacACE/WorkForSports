<template>
  <view class="page">
    <!-- 日期切换 -->
    <view class="date-bar">
      <text class="date-arrow" @tap="prevWeek">◀</text>
      <text class="date-label">{{ weekLabel }}</text>
      <text class="date-arrow" @tap="nextWeek">▶</text>
    </view>

    <!-- 课程卡片列表 -->
    <scroll-view class="list" scroll-y>
      <view v-if="schedules.length === 0 && !loading" class="empty">
        <text class="empty-icon">📅</text>
        <text class="empty-text">暂无课程安排</text>
      </view>

      <view v-for="s in schedules" :key="s.id" class="schedule-card" :style="{ borderLeftColor: s.color || '#2563eb' }">
        <view class="s-time">
          <text class="time-start">{{ s.startTime?.slice(11, 16) || '--:--' }}</text>
          <text class="time-end">{{ s.endTime?.slice(11, 16) || '--:--' }}</text>
        </view>
        <view class="s-dot">●</view>
        <view class="s-content">
          <text class="s-title">{{ s.title }}</text>
          <text class="s-location" v-if="s.location">📍 {{ s.location }}</text>
          <text class="s-coach" v-if="(s as any).coachName">🧑‍🏫 {{ (s as any).coachName }}</text>
        </view>
        <view class="s-status">
          <text class="status-badge" :class="bookingClass(s.bookingStatus)">
            {{ bookingLabel(s.bookingStatus) }}
          </text>
        </view>
      </view>

      <uni-load-more :status="loadStatus" />
    </scroll-view>
  </view>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { getMemberSchedules, type ScheduleEvent } from '@/api/schedule'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userId = userStore.userId

const schedules = ref<ScheduleEvent[]>([])
const loading = ref(false)
const loadStatus = ref<'more' | 'loading' | 'noMore'>('more')
const weekOffset = ref(0)

const weekLabel = computed(() => {
  const now = new Date()
  now.setDate(now.getDate() + weekOffset.value * 7)
  const monday = new Date(now)
  monday.setDate(now.getDate() - now.getDay() + 1)
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  return `${monday.getMonth() + 1}/${monday.getDate()} - ${sunday.getMonth() + 1}/${sunday.getDate()}`
})

function bookingClass(s?: string): string {
  const map: Record<string, string> = {
    CONFIRMED: 'confirmed', PENDING: 'pending', CANCELLED: 'cancelled',
  }
  return map[s || ''] || ''
}

function bookingLabel(s?: string): string {
  const map: Record<string, string> = {
    CONFIRMED: '已确认', PENDING: '待确认', CANCELLED: '已取消',
  }
  return map[s || ''] || s || '--'
}

async function fetchData() {
  if (loading.value || !userId) return
  loading.value = true
  try {
    const res = await getMemberSchedules(userId)
    schedules.value = (res as unknown as ScheduleEvent[]) || []
    loadStatus.value = 'noMore'
  } catch {
    uni.showToast({ title: '获取课表失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function prevWeek() { weekOffset.value--; fetchData() }
function nextWeek() { weekOffset.value++; fetchData() }

fetchData()
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; }

.date-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 32rpx;
  background: #fff;
  border-bottom: 2rpx solid #f0f2f5;
}

.date-arrow { font-size: 28rpx; color: #2563eb; padding: 8rpx; }

.date-label { font-size: 28rpx; font-weight: 600; color: #333; }

.list { padding: 16rpx 24rpx; }

.schedule-card {
  background: #fff;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 12rpx;
  border-left: 6rpx solid #2563eb;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
  display: flex;
  align-items: center;
}

.s-time {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 16rpx;
}

.time-start { font-size: 28rpx; font-weight: 600; color: #333; }
.time-end { font-size: 22rpx; color: #999; }

.s-dot {
  font-size: 24rpx;
  color: #2563eb;
  margin: 0 12rpx;
}

.s-content { flex: 1; }
.s-title { font-size: 28rpx; font-weight: 600; color: #333; margin-bottom: 4rpx; display: block; }
.s-location { font-size: 22rpx; color: #999; }
.s-coach { font-size: 22rpx; color: #999; margin-top: 2rpx; }

.status-badge {
  font-size: 20rpx;
  padding: 6rpx 16rpx;
  border-radius: 8rpx;

  &.confirmed { background: #e8f8e8; color: #67c23a; }
  &.pending { background: #fef0d0; color: #e6a23c; }
  &.cancelled { background: #f5f5f5; color: #ccc; }
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 0;
}

.empty-icon { font-size: 72rpx; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: #999; }
</style>
