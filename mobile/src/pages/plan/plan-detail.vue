<template>
  <view class="page">
    <view v-if="loading" class="loading-wrap">
      <text>加载中...</text>
    </view>

    <view v-else-if="plan">
      <!-- 计划概览 -->
      <view class="header-card">
        <view class="plan-banner">
          <text class="plan-goal">{{ plan.goal }}</text>
          <text class="plan-status" :class="plan.status">{{ statusLabel(plan.status) }}</text>
        </view>
        <view class="plan-meta">
          <text>{{ plan.startDate }} → {{ plan.endDate }}</text>
          <text>{{ progress.done }} / {{ details.length }} 天已完成</text>
        </view>
        <view class="progress-bar">
          <view class="fill" :style="{ width: progress.pct + '%' }" />
        </view>
      </view>

      <!-- 每日详情 -->
      <view class="section">
        <text class="section-title">训练日程</text>
      </view>

      <view v-for="d in sortedDetails" :key="d.id" class="day-card" :class="{ done: d.isChecked }">
        <view class="day-header">
          <view class="day-num" :class="{ checked: d.isChecked }">
            <text v-if="d.isChecked">✓</text>
            <text v-else>{{ d.dayNumber }}</text>
          </view>
          <view class="day-info">
            <text class="day-type">{{ d.trainingType }}</text>
            <text class="day-intensity" :class="'intensity-' + d.intensity">
              {{ intensityLabel(d.intensity) }}
            </text>
          </view>
          <text class="day-duration">{{ d.durationMinutes }}min</text>
        </view>

        <text class="day-content" v-if="d.content">{{ d.content }}</text>

        <view class="day-foot">
          <text v-if="d.isChecked" class="checkin-time">✅ 已打卡 · {{ d.checkTime?.split('T')[1]?.slice(0, 5) || d.checkTime }}</text>
          <button
            v-else
            class="btn-checkin"
            @tap="handleCheckIn(d)"
          >
            打卡
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { getPlanDetails, checkInPlanDetail, type PlanDetail } from '@/api/plan'
import { getMyPlans, type TrainingPlan } from '@/api/plan'

const loading = ref(true)
const plan = ref<TrainingPlan | null>(null)
const details = ref<PlanDetail[]>([])
const checkingIn = ref(false)

const sortedDetails = computed(() => [...details.value].sort((a, b) => a.dayNumber - b.dayNumber))

const progress = computed(() => {
  const done = details.value.filter(d => d.isChecked).length
  const total = details.value.length
  return { done, total, pct: total ? Math.round((done / total) * 100) : 0 }
})

function statusLabel(s: string) { return { ACTIVE: '进行中', COMPLETED: '已完成', CANCELLED: '已取消' }[s] || s }
function intensityLabel(s: string) { return { LOW: '低强度', MEDIUM: '中强度', HIGH: '高强度' }[s] || s }

async function fetchData() {
  const pages = getCurrentPages()
  const opts = (pages[pages.length - 1] as unknown as { options: Record<string, string> }).options
  const planId = Number(opts.id)

  try {
    const plans = await getMyPlans((getCurrentPages()[0] as unknown as Record<string, unknown>)._uid as number || 0)
    // planId 已知，直接查详情
    const all = await Promise.all(
      (await getMyPlans(0)).map(async (p) => {
        if (p.id === planId) {
          plan.value = p
          return getPlanDetails(planId)
        }
        return [] as PlanDetail[]
      }),
    )
    // 简化：直接用 planId 查询
    if (!plan.value) {
      try {
        const allPlans = await getMyPlans(0)
        for (const p of allPlans) {
          if (p.id === planId) {
            plan.value = p
            break
          }
        }
      } catch { /* plan not found */ }
    }
    details.value = await getPlanDetails(planId)
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function handleCheckIn(detail: PlanDetail) {
  if (checkingIn.value || !plan.value) return
  checkingIn.value = true
  try {
    await checkInPlanDetail(plan.value.id, detail.id)
    uni.showToast({ title: '打卡成功!', icon: 'success' })
    details.value = await getPlanDetails(plan.value.id)
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '打卡失败', icon: 'none' })
  } finally {
    checkingIn.value = false
  }
}

fetchData()
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; padding-bottom: 32rpx; }

.loading-wrap { display: flex; align-items: center; justify-content: center; padding: 200rpx 0; font-size: 28rpx; color: #999; }

.header-card {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  margin: 24rpx;
  border-radius: 16rpx;
  padding: 32rpx;
  color: #fff;
}

.plan-banner { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.plan-goal { font-size: 34rpx; font-weight: 700; }
.plan-status { font-size: 22rpx; padding: 4rpx 14rpx; border-radius: 8rpx; background: rgba(255,255,255,0.2); }

.plan-meta { display: flex; gap: 24rpx; font-size: 24rpx; opacity: 0.9; margin-bottom: 16rpx; }

.progress-bar { height: 8rpx; background: rgba(255,255,255,0.3); border-radius: 4rpx; overflow: hidden; }
.fill { height: 100%; background: #fff; border-radius: 4rpx; }

.section { margin: 24rpx 24rpx 8rpx; }
.section-title { font-size: 30rpx; font-weight: 600; color: #333; }

.day-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  margin: 12rpx 24rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);

  &.done { opacity: 0.7; }
}

.day-header { display: flex; align-items: center; }

.day-num {
  width: 56rpx; height: 56rpx; border-radius: 28rpx;
  background: #f0f2f5;
  display: flex; align-items: center; justify-content: center;
  font-size: 28rpx; font-weight: 600; color: #999; margin-right: 16rpx;

  &.checked { background: #67c23a; color: #fff; }
}

.day-info { flex: 1; }
.day-type { font-size: 28rpx; font-weight: 600; color: #333; display: block; margin-bottom: 4rpx; }

.day-intensity {
  font-size: 20rpx; padding: 2rpx 10rpx; border-radius: 6rpx;
  &.intensity-LOW { background: #e8f8e8; color: #67c23a; }
  &.intensity-MEDIUM { background: #fef0d0; color: #e6a23c; }
  &.intensity-HIGH { background: #fde2e2; color: #f56c6c; }
}

.day-duration { font-size: 24rpx; color: #2563eb; font-weight: 600; }

.day-content { font-size: 24rpx; color: #666; margin: 12rpx 0; display: block; }

.day-foot { display: flex; justify-content: flex-end; }

.checkin-time { font-size: 22rpx; color: #67c23a; }

.btn-checkin {
  height: 60rpx;
  padding: 0 32rpx;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  border-radius: 12rpx;
  font-size: 24rpx;
  border: none;
}
</style>
