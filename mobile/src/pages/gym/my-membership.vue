<template>
  <view class="page">
    <view v-if="membership" class="member-card">
      <view class="mc-badge">{{ cardTypeLabel(membership.cardCategory) }}</view>
      <text class="mc-name">{{ membership.cardName || '会员卡' }}</text>
      <view class="mc-dates">
        <text>{{ membership.startDate }} → {{ membership.endDate }}</text>
      </view>
      <view class="mc-info" v-if="membership.remainingVisits">
        <text>剩余次数: {{ membership.remainingVisits }}</text>
      </view>
      <view class="mc-info">
        <text>状态: {{ membership.endDate && new Date(membership.endDate) > new Date() ? '有效' : '已过期' }}</text>
      </view>
    </view>
    <view v-else class="hint">
      <text>暂未开通会员</text>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { getMyGymMembership, type GymMembership } from '@/api/gym'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userId = userStore.userId || 0

const membership = ref<GymMembership | null>(null)

function cardTypeLabel(v?: string) { return { SESSION: '次卡', TIME: '时间卡' }[v || ''] || v || '' }

async function fetchData() {
  try {
    membership.value = await getMyGymMembership(userId!)
  } catch { /* */ }
}

fetchData()
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; padding: 24rpx; }

.member-card {
  background: linear-gradient(135deg, #2563eb, #1d4ed8); border-radius: 16rpx;
  padding: 32rpx; color: #fff; position: relative;
}
.mc-badge { position: absolute; top: 16rpx; right: 16rpx; font-size: 20rpx; padding: 4rpx 14rpx; background: rgba(255,255,255,0.2); border-radius: 8rpx; }
.mc-name { font-size: 34rpx; font-weight: 700; margin-bottom: 8rpx; display: block; }
.mc-dates { font-size: 24rpx; opacity: 0.85; margin-bottom: 8rpx; }
.mc-info { font-size: 24rpx; opacity: 0.85; }

.hint { text-align: center; padding: 160rpx 0; font-size: 28rpx; color: #999; }
</style>
