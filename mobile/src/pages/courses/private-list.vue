<template>
  <view class="page">
    <scroll-view
      class="list"
      scroll-y
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="loadMore"
    >
      <view v-if="coaches.length === 0 && !loading" class="empty">
        <text class="empty-icon">🧑‍🏫</text>
        <text class="empty-text">暂无教练</text>
      </view>

      <view
        v-for="coach in coaches"
        :key="coach.coachId"
        class="coach-card"
        @tap="gotoDetail(coach.coachId)"
      >
        <view class="coach-header">
          <view class="coach-avatar">
            <text class="avatar-text">{{ (coach.realName || '教练').charAt(0) }}</text>
          </view>
          <view class="coach-info">
            <text class="coach-name">{{ coach.realName || '私教' }}</text>
            <view class="coach-spec" v-if="coach.specialties">
              <text
                v-for="(s, i) in coach.specialties.split(',')"
                :key="i"
                class="spec-tag"
              >{{ s.trim() }}</text>
            </view>
          </view>
          <view class="coach-price">
            <text class="price-num">¥{{ coach.pricePerSession || 0 }}</text>
            <text class="price-unit">/节</text>
          </view>
        </view>
        <view class="coach-desc" v-if="coach.description">
          <text>{{ coach.description }}</text>
        </view>
        <view class="coach-meta">
          <text class="meta-item">⏱ {{ coach.sessionDuration || 60 }}分钟/节</text>
          <text class="meta-item">📍 线上+线下</text>
        </view>
      </view>

      <uni-load-more :status="loadStatus" />
    </scroll-view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { listCoaches, type CoachListItem } from '@/api/privateCoach'

interface CoachItem extends CoachListItem {}

const coaches = ref<CoachItem[]>([])
const loading = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const loadStatus = ref<'more' | 'loading' | 'noMore'>('more')

async function fetchData(reset = false) {
  if (loading.value) return
  loading.value = true
  if (reset) { pageNum.value = 1; loadStatus.value = 'more' }

  try {
    const res = await listCoaches(undefined, pageNum.value, pageSize)
    const items = (res.records || []) as unknown as CoachItem[]
    if (reset) {
      coaches.value = items
    } else {
      coaches.value = [...coaches.value, ...items]
    }
    total.value = (res as unknown as { total: number }).total || 0
    loadStatus.value = coaches.value.length >= total.value ? 'noMore' : 'more'
  } catch {
    uni.showToast({ title: '获取教练列表失败', icon: 'none' })
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function onRefresh() { refreshing.value = true; fetchData(true) }
function loadMore() { if (loadStatus.value !== 'noMore' && !loading.value) { pageNum.value++; fetchData(false) } }
function gotoDetail(coachId: number) {
  uni.navigateTo({ url: `/pages/courses/detail?id=${coachId}&type=PRIVATE` })
}

fetchData(true)
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; }
.list { padding: 16rpx 24rpx; }

.coach-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}

.coach-header {
  display: flex;
  align-items: center;
}

.coach-avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 44rpx;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
}

.avatar-text {
  font-size: 36rpx;
  color: #fff;
  font-weight: 600;
}

.coach-info {
  flex: 1;
}

.coach-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 8rpx;
}

.coach-spec {
  display: flex;
  gap: 8rpx;
  flex-wrap: wrap;
}

.spec-tag {
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  background: #e8f4fd;
  color: #2563eb;
  border-radius: 8rpx;
}

.coach-price {
  text-align: right;
}

.price-num {
  font-size: 36rpx;
  font-weight: 700;
  color: #f56c6c;
}

.price-unit {
  font-size: 22rpx;
  color: #999;
}

.coach-desc {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.coach-meta {
  margin-top: 12rpx;
  display: flex;
  gap: 24rpx;
}

.meta-item {
  font-size: 22rpx;
  color: #999;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 0;
}

.empty-icon { font-size: 80rpx; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: #999; }
</style>
