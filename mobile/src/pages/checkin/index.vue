<template>
  <view class="page">
    <!-- 今日状态 -->
    <view class="status-card">
      <view class="status-icon">{{ checked ? '✅' : '⏳' }}</view>
      <text class="status-text">{{ checked ? '今日已打卡' : '今日未打卡' }}</text>
      <text v-if="checked" class="status-time">签到时间: {{ checkTime }}</text>
    </view>

    <!-- 打卡按钮 -->
    <view class="checkin-area" v-if="!checked">
      <button class="btn-checkin" @tap="handleCheckin">
        <text class="btn-icon">📲</text>
        <text class="btn-text">签到打卡</text>
        <text class="btn-sub">点击确认到达训练场地</text>
      </button>
    </view>

    <!-- 历史记录 -->
    <view class="section">
      <text class="section-title">打卡历史</text>
    </view>

    <scroll-view class="list" scroll-y>
      <view v-if="history.length === 0 && !loading" class="empty">
        <text class="empty-text">暂无打卡记录</text>
      </view>

      <view v-for="h in history" :key="h.id" class="history-card">
        <view class="h-left">
          <text class="h-date">{{ h.checkInTime?.split(' ')[0] }}</text>
          <text class="h-time">{{ h.checkInTime?.split(' ')[1] }}</text>
        </view>
        <view class="h-right">
          <text class="h-status" :class="h.status">{{ statusLabel(h.status) }}</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { checkIn, getCheckInHistory, getCheckInStatus, type CheckInRecord } from '@/api/checkin'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userId = userStore.userId
const role = userStore.userRole === 'MEMBER' ? 'MEMBER' : 'COACH'

const checked = ref(false)
const checkTime = ref('')
const history = ref<CheckInRecord[]>([])
const loading = ref(false)

function statusLabel(s: string): string {
  return { SIGNED: '已签到', PENDING: '待签到', ABSENT: '缺勤' }[s] || s
}

async function fetchStatus() {
  if (!userId) return
  try {
    // 查最新课程安排
    const statusRes = await getCheckInStatus(0, userId, role).catch(() => null)
    if (statusRes && (statusRes as unknown as { checked?: boolean }).checked) {
      checked.value = true
      checkTime.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    }
  } catch { /* no schedule */ }
}

async function fetchHistory() {
  if (!userId) return
  loading.value = true
  try {
    history.value = await getCheckInHistory(userId, role) as unknown as CheckInRecord[]
  } catch {
    // 静默失败
  } finally {
    loading.value = false
  }
}

async function handleCheckin() {
  if (!userId) return
  try {
    await checkIn(0, userId, role)
    checked.value = true
    checkTime.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    uni.showToast({ title: '打卡成功!', icon: 'success' })
    fetchHistory()
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '打卡失败', icon: 'none' })
  }
}

fetchStatus()
fetchHistory()
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; }

.status-card {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  margin: 24rpx;
  border-radius: 16rpx;
  padding: 48rpx 32rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.status-icon { font-size: 64rpx; margin-bottom: 12rpx; }
.status-text { font-size: 32rpx; font-weight: 600; color: #fff; }
.status-time { font-size: 24rpx; color: rgba(255,255,255,0.8); margin-top: 8rpx; }

.checkin-area {
  padding: 24rpx;
  display: flex;
  justify-content: center;
}

.btn-checkin {
  width: 100%;
  height: 200rpx;
  background: #fff;
  border-radius: 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2rpx solid #2563eb;
  box-shadow: 0 4rpx 24rpx rgba(37, 99, 235, 0.12);
}

.btn-icon { font-size: 56rpx; margin-bottom: 8rpx; }
.btn-text { font-size: 36rpx; font-weight: 700; color: #2563eb; }
.btn-sub { font-size: 22rpx; color: #999; margin-top: 4rpx; }

.section { margin: 16rpx 24rpx 8rpx; }
.section-title { font-size: 30rpx; font-weight: 600; color: #333; }

.list { padding: 0 24rpx; }

.history-card {
  background: #fff;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 8rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
}

.h-date { font-size: 26rpx; color: #333; font-weight: 500; margin-right: 12rpx; }
.h-time { font-size: 24rpx; color: #999; }

.h-status {
  font-size: 24rpx;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;

  &.SIGNED { color: #67c23a; background: #e8f8e8; }
  &.PENDING { color: #e6a23c; background: #fef0d0; }
  &.ABSENT { color: #f56c6c; background: #fde2e2; }
}

.empty { padding: 80rpx 0; text-align: center; }
.empty-text { font-size: 28rpx; color: #999; }
</style>
