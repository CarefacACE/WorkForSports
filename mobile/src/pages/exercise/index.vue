<template>
  <view class="page">
    <!-- 统计卡片 -->
    <view class="stats-row" v-if="stats">
      <view class="stat-card">
        <text class="stat-value">{{ (stats.totalDuration || 0) }}</text>
        <text class="stat-unit">分钟</text>
        <text class="stat-label">总时长</text>
      </view>
      <view class="stat-card">
        <text class="stat-value">{{ (stats.totalCalories || 0) }}</text>
        <text class="stat-unit">kcal</text>
        <text class="stat-label">消耗</text>
      </view>
      <view class="stat-card">
        <text class="stat-value">{{ stats.totalDays || 0 }}</text>
        <text class="stat-unit">天</text>
        <text class="stat-label">坚持</text>
      </view>
      <view class="stat-card">
        <text class="stat-value">{{ stats.totalSessions || 0 }}</text>
        <text class="stat-unit">次</text>
        <text class="stat-label">训练</text>
      </view>
    </view>

    <!-- 添加按钮 -->
    <view class="action-bar">
      <button class="btn-add" @tap="gotoAdd">+ 添加运动记录</button>
    </view>

    <!-- 记录列表 -->
    <scroll-view
      class="list"
      scroll-y
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view v-if="records.length === 0 && !loading" class="empty">
        <text class="empty-icon">🏃</text>
        <text class="empty-text">还没有运动记录</text>
      </view>

      <view v-for="r in records" :key="r.id" class="record-card">
        <view class="record-left">
          <view class="record-icon">{{ typeIcon(r.type) }}</view>
        </view>
        <view class="record-main">
          <text class="record-type">{{ r.type }}</text>
          <view class="record-stats">
            <text class="rs-item">⏱ {{ r.duration }}min</text>
            <text class="rs-item" v-if="r.distance">📏 {{ r.distance }}km</text>
            <text class="rs-item">🔥 {{ r.calories }}kcal</text>
          </view>
          <text class="record-date">{{ r.exerciseDate }}</text>
        </view>
        <view class="record-right">
          <text class="btn-delete" @tap="handleDelete(r)">🗑</text>
        </view>
      </view>

      <uni-load-more :status="loadStatus" />
    </scroll-view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { getExerciseRecords, getExerciseStats, deleteExerciseRecord, type ExerciseRecord, type ExerciseStats } from '@/api/exercise'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userId = userStore.userId

const records = ref<ExerciseRecord[]>([])
const stats = ref<ExerciseStats | null>(null)
const loading = ref(false)
const refreshing = ref(false)
const loadStatus = ref<'more' | 'loading' | 'noMore'>('more')

function typeIcon(type: string): string {
  const map: Record<string, string> = {
    '跑步': '🏃', '游泳': '🏊', '骑行': '🚴', '力量训练': '🏋',
    '瑜伽': '🧘', '篮球': '🏀', '足球': '⚽', '跳绳': '🪢',
  }
  return map[type] || '💪'
}

async function fetchData() {
  if (loading.value || !userId) return
  loading.value = true
  try {
    const [r, s] = await Promise.all([
      getExerciseRecords(userId),
      getExerciseStats(userId),
    ])
    records.value = r || []
    stats.value = s
    loadStatus.value = 'noMore'
  } catch {
    uni.showToast({ title: '获取数据失败', icon: 'none' })
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function onRefresh() { refreshing.value = true; fetchData() }

async function handleDelete(record: ExerciseRecord) {
  const res = await new Promise<boolean>((resolve) => {
    uni.showModal({ title: '删除记录', content: '确定删除？', success: (r) => resolve(r.confirm) })
  })
  if (!res || !userId) return

  try {
    await deleteExerciseRecord(userId, record.id!)
    uni.showToast({ title: '已删除', icon: 'success' })
    fetchData()
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '删除失败', icon: 'none' })
  }
}

function gotoAdd() {
  uni.navigateTo({ url: '/pages/exercise/add-record' })
}

fetchData()
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; padding-bottom: 32rpx; }

/* 统计行 */
.stats-row {
  display: flex;
  gap: 12rpx;
  padding: 16rpx 24rpx;
}

.stat-card {
  flex: 1;
  background: #fff;
  border-radius: 16rpx;
  padding: 20rpx 8rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}

.stat-value { font-size: 32rpx; font-weight: 700; color: #2563eb; }
.stat-unit { font-size: 20rpx; color: #999; }
.stat-label { font-size: 20rpx; color: #999; margin-top: 4rpx; }

/* 操作栏 */
.action-bar { padding: 0 24rpx 16rpx; }

.btn-add {
  width: 100%;
  height: 80rpx;
  background: #fff;
  color: #2563eb;
  border: 2rpx dashed #2563eb;
  border-radius: 16rpx;
  font-size: 28rpx;
}

/* 列表 */
.list { padding: 0 24rpx; }

.record-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 12rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
  display: flex;
  align-items: center;
}

.record-left { margin-right: 16rpx; }
.record-icon { font-size: 44rpx; }

.record-main { flex: 1; }

.record-type { font-size: 28rpx; font-weight: 600; color: #333; margin-bottom: 6rpx; display: block; }

.record-stats { display: flex; gap: 16rpx; margin-bottom: 4rpx; }
.rs-item { font-size: 22rpx; color: #666; }

.record-date { font-size: 20rpx; color: #bbb; }

.btn-delete { font-size: 32rpx; padding: 8rpx; }

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 0;
}

.empty-icon { font-size: 72rpx; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: #999; }
</style>
