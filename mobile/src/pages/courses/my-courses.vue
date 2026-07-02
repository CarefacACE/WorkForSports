<template>
  <view class="page">
    <!-- Tab 切换 -->
    <view class="tabs">
      <view
        v-for="t in tabs"
        :key="t.value"
        class="tab"
        :class="{ active: activeTab === t.value }"
        @tap="switchTab(t.value)"
      >
        <text>{{ t.label }}</text>
      </view>
    </view>

    <!-- 公共课列表 -->
    <view v-if="activeTab === 'PUBLIC'" class="list">
      <view v-if="enrollments.length === 0 && !loading" class="empty">
        <text class="empty-icon">📚</text>
        <text class="empty-text">还没有报名任何公共课</text>
      </view>

      <view
        v-for="e in enrollments"
        :key="e.id"
        class="card"
      >
        <view class="card-left">
          <text class="card-title">{{ courseNameMap[e.courseId] || '课程 #' + e.courseId }}</text>
          <view class="card-meta">
            <text class="meta-tag" :class="'status-' + e.status">{{ statusLabel(e.status) }}</text>
            <text class="meta-price">¥{{ (e.paidAmount ?? 0).toFixed(0) }}</text>
            <text class="meta-date">{{ e.createTime?.split(' ')[0] }}</text>
          </view>
        </view>
        <view class="card-right" v-if="e.status === 'PAID' || e.status === 'CONFIRMED'">
          <text class="btn-quit" @tap="handleQuit(e)">退出</text>
        </view>
      </view>
    </view>

    <!-- 私教课列表 -->
    <view v-if="activeTab === 'PRIVATE'" class="list">
      <view v-if="enrollments.length === 0 && !loading" class="empty">
        <text class="empty-icon">🧑‍🏫</text>
        <text class="empty-text">还没有报名任何私教课</text>
      </view>

      <view
        v-for="e in enrollments"
        :key="e.id"
        class="card"
      >
        <view class="card-left">
          <text class="card-title">{{ coachNameMap[e.coachId!] || '私教 #' + e.coachId }}</text>
          <view class="card-meta">
            <text class="meta-tag" :class="'status-' + e.status">{{ e.status === 'PAID' ? '在读' : e.status === 'CANCELLED' ? '已退出' : e.status }}</text>
            <text class="meta-tag auto" :class="{ on: e.autoDeductAgreed }">自动扣费: {{ e.autoDeductAgreed ? '已开' : '未开' }}</text>
          </view>
        </view>
        <view class="card-right" v-if="e.status === 'PAID'">
          <text class="btn-quit" @tap="handleQuit(e)">退出</text>
        </view>
      </view>
    </view>

    <uni-load-more :status="loadStatus" />
  </view>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue'
import { getMyEnrollments, quitEnrollment, type Enrollment } from '@/api/enrollment'
import { getCourseDetail } from '@/api/course'
import { getCoachDetail } from '@/api/privateCoach'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userId = userStore.userId

const tabs = [
  { value: 'PUBLIC', label: '公共课' },
  { value: 'PRIVATE', label: '私教课' },
]
const activeTab = ref('PUBLIC')
const loading = ref(false)
const loadStatus = ref<'more' | 'loading' | 'noMore'>('more')
const enrollments = ref<Enrollment[]>([])
const courseNameMap = reactive<Record<number, string>>({})
const coachNameMap = reactive<Record<number, string>>({})

function statusLabel(s: string): string {
  const map: Record<string, string> = {
    TRIAL: '试听中', PAID: '已购买', CONFIRMED: '已确认', CANCELLED: '已退出',
  }
  return map[s] || s
}

async function fetchData() {
  if (loading.value || !userId) return
  loading.value = true
  try {
    const tab = activeTab.value
    const res = await getMyEnrollments(userId, tab, 1, 100)
    enrollments.value = res.records

    if (tab === 'PUBLIC') {
      for (const e of res.records) {
        if (e.courseId && !courseNameMap[e.courseId]) {
          try {
            const course = await getCourseDetail(e.courseId)
            courseNameMap[e.courseId] = course.name
          } catch { courseNameMap[e.courseId] = '课程 #' + e.courseId }
        }
      }
    } else {
      for (const e of res.records) {
        if (e.coachId && !coachNameMap[e.coachId]) {
          try {
            const detail = await getCoachDetail(e.coachId)
            coachNameMap[e.coachId] = (detail as unknown as { realName?: string }).realName || '私教 #' + e.coachId
          } catch { coachNameMap[e.coachId] = '私教 #' + e.coachId }
        }
      }
    }

    loadStatus.value = 'noMore'
  } catch {
    uni.showToast({ title: '获取数据失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function handleQuit(row: Enrollment) {
  const res = await new Promise<boolean>((resolve) => {
    uni.showModal({
      title: '确认退出',
      content: '确定退出该课程吗？',
      success: (r) => resolve(r.confirm),
    })
  })
  if (!res) return

  try {
    await quitEnrollment(row.id)
    uni.showToast({ title: '已退出', icon: 'success' })
    fetchData()
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '退出失败', icon: 'none' })
  }
}

function switchTab(v: string) {
  activeTab.value = v
  fetchData()
}

fetchData()
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; }

.tabs {
  display: flex;
  background: #fff;
  border-bottom: 2rpx solid #f0f2f5;
  position: sticky;
  top: 0;
  z-index: 10;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  font-size: 28rpx;
  color: #666;
  border-bottom: 4rpx solid transparent;

  &.active {
    color: #2563eb;
    font-weight: 600;
    border-bottom-color: #2563eb;
  }
}

.list { padding: 16rpx 24rpx; }

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-left { flex: 1; }
.card-title { font-size: 30rpx; font-weight: 600; color: #333; margin-bottom: 8rpx; display: block; }

.card-meta { display: flex; gap: 12rpx; align-items: center; }

.meta-tag {
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  background: #f0f2f5;
  color: #666;

  &.status-TRIAL { background: #fef0d0; color: #e6a23c; }
  &.status-PAID { background: #e8f8e8; color: #67c23a; }
  &.status-CONFIRMED { background: #e8f4fd; color: #2563eb; }
  &.status-CANCELLED { background: #f5f5f5; color: #ccc; }

  &.auto.on { background: #e8f4fd; color: #2563eb; }
}

.meta-price { font-size: 22rpx; color: #f56c6c; }
.meta-date { font-size: 22rpx; color: #999; }

.btn-quit {
  font-size: 24rpx;
  color: #f56c6c;
  padding: 8rpx 20rpx;
  border: 2rpx solid #f56c6c;
  border-radius: 8rpx;
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
