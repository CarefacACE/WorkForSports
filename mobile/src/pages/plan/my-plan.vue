<template>
  <view class="page">
    <scroll-view class="list" scroll-y>
      <view v-if="plans.length === 0 && !loading" class="empty">
        <text class="empty-icon">📋</text>
        <text class="empty-text">暂无训练计划</text>
      </view>

      <view v-for="plan in plans" :key="plan.id" class="plan-card" @tap="gotoDetail(plan.id)">
        <view class="plan-header">
          <text class="plan-goal">{{ plan.goal }}</text>
          <text class="plan-status" :class="'status-' + plan.status">
            {{ statusLabel(plan.status) }}
          </text>
        </view>

        <view class="plan-meta">
          <text class="meta-item">📅 {{ plan.startDate || '--' }} 至 {{ plan.endDate || '--' }}</text>
          <text class="meta-item">⏳ {{ plan.durationDays }}天</text>
        </view>

        <text class="plan-desc" v-if="plan.description">{{ plan.description }}</text>

        <!-- 进度条 -->
        <view class="progress-bar">
          <view class="progress-fill" :style="{ width: progressPercent(plan) + '%' }" />
        </view>
        <text class="progress-text">已完成 {{ completedCount(plan) }} / {{ plan.durationDays }} 天</text>
      </view>

      <uni-load-more :status="loadStatus" />
    </scroll-view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { getMyPlans, getPlanDetails, type TrainingPlan, type PlanDetail } from '@/api/plan'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userId = userStore.userId

const plans = ref<(TrainingPlan & { _details?: PlanDetail[] })[]>([])
const loading = ref(false)
const loadStatus = ref<'more' | 'loading' | 'noMore'>('more')

function statusLabel(s: string): string {
  const map: Record<string, string> = { ACTIVE: '进行中', COMPLETED: '已完成', CANCELLED: '已取消' }
  return map[s] || s
}

function completedCount(plan: TrainingPlan & { _details?: PlanDetail[] }): number {
  return (plan._details || []).filter(d => d.isChecked).length
}

function progressPercent(plan: TrainingPlan & { _details?: PlanDetail[] }): number {
  if (!plan.durationDays) return 0
  return Math.round((completedCount(plan) / plan.durationDays) * 100)
}

async function fetchData() {
  if (loading.value || !userId) return
  loading.value = true
  try {
    const res = await getMyPlans(userId)
    // 获取每个计划的详情以计算进度
    const enriched = await Promise.all(
      (res || []).map(async (plan) => {
        try {
          const details = await getPlanDetails(plan.id)
          return { ...plan, _details: details }
        } catch {
          return plan
        }
      }),
    )
    plans.value = enriched
    loadStatus.value = 'noMore'
  } catch {
    uni.showToast({ title: '获取计划失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function gotoDetail(planId: number) {
  uni.navigateTo({ url: `/pages/plan/plan-detail?id=${planId}` })
}

fetchData()
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; }
.list { padding: 16rpx 24rpx; }

.plan-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}

.plan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.plan-goal {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.plan-status {
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;

  &.status-ACTIVE { background: #e8f4fd; color: #2563eb; }
  &.status-COMPLETED { background: #e8f8e8; color: #67c23a; }
  &.status-CANCELLED { background: #f5f5f5; color: #ccc; }
}

.plan-meta {
  display: flex;
  gap: 24rpx;
  margin-bottom: 12rpx;
}

.meta-item {
  font-size: 24rpx;
  color: #999;
}

.plan-desc {
  font-size: 24rpx;
  color: #666;
  margin-bottom: 16rpx;
  display: block;
}

.progress-bar {
  height: 8rpx;
  background: #f0f2f5;
  border-radius: 4rpx;
  margin-bottom: 8rpx;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #2563eb, #1d4ed8);
  border-radius: 4rpx;
  transition: width 0.3s;
}

.progress-text {
  font-size: 22rpx;
  color: #999;
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
