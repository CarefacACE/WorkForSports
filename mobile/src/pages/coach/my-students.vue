<template>
  <view class="page">
    <!-- 搜索 + 课程筛选 -->
    <view class="toolbar">
      <view class="search-box">
        <input v-model="keyword" placeholder="搜索学员..." placeholder-style="color:#bbb" confirm-type="search" @confirm="doSearch" />
        <text v-if="keyword" class="clear" @tap="clearSearch">✕</text>
      </view>
      <picker :range="courseOptions" range-key="name" :value="courseIdx" @change="onCourseChange">
        <view class="picker-btn">
          <text>{{ selectedCourse || '全部课程' }}</text><text class="arrow">▼</text>
        </view>
      </picker>
    </view>

    <scroll-view class="list" scroll-y>
      <view v-if="students.length === 0 && !loading" class="empty">
        <text class="empty-icon">👥</text><text class="empty-text">暂无学员</text>
      </view>

      <view v-for="s in students" :key="s.id" class="student-card" @tap="viewDetail(s)">
        <view class="s-avatar">
          <text>{{ (s.realName || s.username || '?').charAt(0) }}</text>
        </view>
        <view class="s-info">
          <text class="s-name">{{ s.realName || s.username }}</text>
          <view class="s-meta">
            <text v-if="s.phone">📱 {{ s.phone }}</text>
            <text v-if="s.gender">{{ s.gender }}</text>
          </view>
        </view>
        <text class="s-arrow">→</text>
      </view>

      <uni-load-more :status="loadStatus" />
    </scroll-view>
  </view>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { getCourseStudents, type StudentPage } from '@/api/enrollment'
import { getMyCourses } from '@/api/course'
import { getHealthProfile } from '@/api/health'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const coachId = userStore.userId || 0

const keyword = ref('')
const students = ref<Array<{ id: number; username: string; realName: string; phone: string; gender?: string }>>([])
const courses = ref<Array<{ id: number; name: string }>>([])
const selectedCourse = ref('')
const selectedCourseId = ref<number | undefined>(undefined)
const courseIdx = ref(0)
const loading = ref(false)
const loadStatus = ref<'more' | 'loading' | 'noMore'>('more')
const pageNum = ref(1)

const courseOptions = computed(() => [{ id: 0, name: '全部课程' }, ...courses.value])

function onCourseChange(e: { detail: { value: number } }) {
  const item = courseOptions.value[e.detail.value]
  selectedCourse.value = item.name
  selectedCourseId.value = item.id || undefined
  fetchStudents(true)
}

function doSearch() { fetchStudents(true) }
function clearSearch() { keyword.value = ''; fetchStudents(true) }

async function fetchStudents(reset = false) {
  if (loading.value) return
  loading.value = true
  if (reset) { pageNum.value = 1; loadStatus.value = 'more' }

  try {
    // 先拉课程列表
    if (courses.value.length === 0) {
      try {
        const c = await getMyCourses(coachId, 1, 50)
        courses.value = c.records.map(({ id, name }) => ({ id, name }))
      } catch { /* */ }
    }

    const res = await getCourseStudents(coachId, selectedCourseId.value || undefined, keyword.value || undefined, pageNum.value, 20)
    const records = (res.records || []) as unknown as typeof students.value
    if (reset) students.value = records
    else students.value = [...students.value, ...records]

    loadStatus.value = records.length < 20 ? 'noMore' : 'more'
  } catch {
    uni.showToast({ title: '获取学员失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function viewDetail(s: typeof students.value[0]) {
  let healthInfo = ''
  try {
    const h = await getHealthProfile(s.id)
    if (h) {
      healthInfo = [
        h.height ? `身高: ${h.height}cm` : '',
        h.weight ? `体重: ${h.weight}kg` : '',
        h.bodyFat ? `体脂率: ${h.bodyFat}%` : '',
        h.fitnessGoal ? `目标: ${h.fitnessGoal}` : '',
      ].filter(Boolean).join(' | ')
    }
  } catch { /* */ }

  uni.showModal({
    title: `学员信息 — ${s.realName || s.username}`,
    content: [
      `用户名: ${s.username}`,
      `手机: ${s.phone || '-'}`,
      `性别: ${s.gender || '-'}`,
      healthInfo ? `\n健康信息:\n${healthInfo}` : '',
    ].filter(Boolean).join('\n'),
    showCancel: false,
    confirmText: '好的',
  })
}

fetchStudents(true)
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; }

.toolbar {
  background: #fff; padding: 16rpx 24rpx; display: flex; gap: 12rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
  position: sticky; top: 0; z-index: 10;
}
.search-box {
  flex: 1; height: 68rpx; background: #f5f6fa; border-radius: 34rpx;
  padding: 0 24rpx; display: flex; align-items: center;
}
.search-box input { flex: 1; font-size: 26rpx; color: #333; }
.clear { font-size: 24rpx; color: #999; padding: 8rpx; }

.picker-btn {
  height: 68rpx; padding: 0 20rpx; background: #f5f6fa; border-radius: 34rpx;
  display: flex; align-items: center; gap: 8rpx; font-size: 24rpx; color: #666;
}
.arrow { font-size: 18rpx; color: #bbb; }

.list { padding: 16rpx 24rpx; }

.student-card {
  background: #fff; border-radius: 16rpx; padding: 20rpx 24rpx;
  margin-bottom: 12rpx; display: flex; align-items: center;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
}
.s-avatar {
  width: 80rpx; height: 80rpx; border-radius: 40rpx;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  display: flex; align-items: center; justify-content: center;
  font-size: 32rpx; color: #fff; font-weight: 600; margin-right: 20rpx;
}
.s-info { flex: 1; }
.s-name { font-size: 30rpx; font-weight: 600; color: #333; margin-bottom: 4rpx; display: block; }
.s-meta { font-size: 22rpx; color: #999; display: flex; gap: 16rpx; }
.s-arrow { font-size: 28rpx; color: #ccc; }

.empty { display: flex; flex-direction: column; align-items: center; padding: 160rpx 0; }
.empty-icon { font-size: 72rpx; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: #999; }
</style>
