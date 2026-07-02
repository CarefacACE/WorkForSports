<template>
  <view class="page">
    <view class="section">
      <text class="section-title">我的会员卡</text>
      <view v-if="membership" class="member-card">
        <view class="mc-badge">{{ membershipType }}</view>
        <text class="mc-name">{{ membership.cardName || '健身卡' }}</text>
        <view class="mc-dates">
          <text>{{ membership.startDate }} → {{ membership.endDate }}</text>
        </view>
        <view class="mc-status" v-if="membership.remainingVisits">
          <text>剩余次数: {{ membership.remainingVisits }}</text>
        </view>
      </view>
      <view v-else class="hint">
        <text>暂未开通会员，请联系管理员</text>
      </view>
    </view>

    <view class="section">
      <text class="section-title">可用会员卡</text>
      <view v-if="cards.length === 0" class="hint">
        <text>暂无可用健身卡</text>
      </view>
      <view v-for="c in cards" :key="c.id" class="card-item">
        <view class="ci-left">
          <text class="ci-name">{{ c.name }}</text>
          <view class="ci-meta">
            <text class="ci-tag">{{ cardTypeLabel(c.cardCategory) }}</text>
            <text class="ci-tag">{{ durationLabel(c.type) }}</text>
          </view>
        </view>
        <view class="ci-right">
          <text class="ci-price">¥{{ c.price }}</text>
          <button class="ci-btn" @tap="handlePurchase(c.id)">购买</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { listGymCards, getMyGymMembership, purchaseGymCard, type GymCard, type GymMembership, type SubCardInfo } from '@/api/gym'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userId = userStore.userId || 0

const cards = ref<GymCard[]>([])
const membership = ref<GymMembership | null>(null)

const membershipType = ref('')

function cardTypeLabel(v?: string) { return { SESSION: '次卡', TIME: '时间卡' }[v || ''] || v || '' }
function durationLabel(v?: string) {
  return { VISIT: '按次', MONTHLY: '月卡', QUARTERLY: '季卡', YEARLY: '年卡', TRIAL: '体验' }[v || ''] || v || ''
}

async function fetchData() {
  try {
    const [c, m] = await Promise.all([
      listGymCards(),
      getMyGymMembership(userId!),
    ])
    cards.value = c || []
    membership.value = m || null
    if (m) membershipType.value = cardTypeLabel((m as unknown as { cardCategory?: string }).cardCategory)
  } catch { /* */ }
}

async function handlePurchase(cardId: number | undefined) {
  if (!cardId) return
  const res = await new Promise<boolean>((resolve) => {
    uni.showModal({ title: '确认购买', content: '确定购买该会员卡？', success: (r) => resolve(r.confirm) })
  })
  if (!res) return
  try {
    await purchaseGymCard(userId!, cardId)
    uni.showToast({ title: '购买成功', icon: 'success' })
    fetchData()
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '购买失败', icon: 'none' })
  }
}

fetchData()
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; padding: 24rpx; }

.section { margin-bottom: 24rpx; }
.section-title { font-size: 30rpx; font-weight: 600; color: #333; margin-bottom: 12rpx; display: block; }

.member-card {
  background: linear-gradient(135deg, #2563eb, #1d4ed8); border-radius: 16rpx;
  padding: 32rpx; color: #fff; position: relative; overflow: hidden;
}
.mc-badge { position: absolute; top: 16rpx; right: 16rpx; font-size: 20rpx; padding: 4rpx 14rpx; background: rgba(255,255,255,0.2); border-radius: 8rpx; }
.mc-name { font-size: 34rpx; font-weight: 700; margin-bottom: 8rpx; display: block; }
.mc-dates { font-size: 24rpx; opacity: 0.85; margin-bottom: 8rpx; }
.mc-status { font-size: 24rpx; opacity: 0.85; }

.hint { text-align: center; padding: 60rpx 0; font-size: 26rpx; color: #bbb; }

.card-item {
  background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 12rpx;
  display: flex; justify-content: space-between; align-items: center;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
}
.ci-name { font-size: 28rpx; font-weight: 600; color: #333; margin-bottom: 4rpx; display: block; }
.ci-meta { display: flex; gap: 8rpx; }
.ci-tag { font-size: 20rpx; padding: 2rpx 10rpx; background: #f0f2f5; color: #666; border-radius: 6rpx; }
.ci-right { text-align: right; }
.ci-price { font-size: 32rpx; font-weight: 700; color: #f56c6c; display: block; margin-bottom: 8rpx; }
.ci-btn { height: 56rpx; padding: 0 28rpx; background: #2563eb; color: #fff; border-radius: 12rpx; font-size: 24rpx; border: none; }
</style>
