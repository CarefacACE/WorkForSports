<template>
  <view class="page">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <view class="search-box">
        <text class="search-icon">🔍</text>
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索课程名称"
          placeholder-style="color:#bbb"
          confirm-type="search"
          @confirm="handleSearch"
        />
        <text v-if="keyword" class="search-clear" @tap="clearSearch">✕</text>
      </view>
    </view>

    <!-- 课程列表 -->
    <scroll-view
      class="list"
      scroll-y
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="loadMore"
    >
      <view v-if="courses.length === 0 && !loading" class="empty">
        <text class="empty-icon">📭</text>
        <text class="empty-text">暂无课程</text>
      </view>

      <view
        v-for="course in courses"
        :key="course.id"
        class="course-card"
        @tap="openDetail(course)"
      >
        <view class="course-cover">
          <image v-if="course.coverImage" :src="course.coverImage" mode="aspectFill" class="cover-img" />
          <view v-else class="cover-placeholder">
            <text class="cover-letter">{{ course.name.charAt(0) }}</text>
          </view>
        </view>
        <view class="course-body">
          <view class="course-tags">
            <text class="tag cat">{{ categoryLabel(course.category) }}</text>
            <text class="tag" :class="'diff-' + course.difficulty">{{ difficultyLabel(course.difficulty) }}</text>
          </view>
          <text class="course-name">{{ course.name }}</text>
          <view class="course-foot">
            <view class="course-price">
              <text v-if="course.price > 0" class="price-paid">¥{{ course.price.toFixed(2) }}</text>
              <text v-else class="price-free">免费</text>
            </view>
            <text v-if="course.maxStudents > 0" class="course-capacity">👥 {{ course.maxStudents }}人</text>
          </view>
        </view>
      </view>

      <uni-load-more :status="loadStatus" />
    </scroll-view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { listCourses, type Course } from '@/api/course'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// ---- 常量 ----
const CATEGORIES: Record<string, string> = {
  YOGA: '瑜伽', BOXING: '拳击', SWIMMING: '游泳', RUNNING: '跑步',
  STRENGTH: '力量', DANCE: '舞蹈', OTHER: '其他',
}
const DIFFICULTIES: Record<string, string> = {
  BEGINNER: '初级', INTERMEDIATE: '中级', ADVANCED: '高级',
}
function categoryLabel(v?: string) { return CATEGORIES[v || ''] || '其他' }
function difficultyLabel(v?: string) { return DIFFICULTIES[v || ''] || '初级' }

// ---- State ----
const keyword = ref('')
const pageNum = ref(1)
const pageSize = 12
const total = ref(0)
const courses = ref<Course[]>([])
const loading = ref(false)
const refreshing = ref(false)
const loadStatus = ref<'more' | 'loading' | 'noMore'>('more')

// ---- Methods ----
async function fetchData(reset = false) {
  if (loading.value) return
  loading.value = true
  if (reset) {
    pageNum.value = 1
    loadStatus.value = 'more'
  }

  try {
    const res = await listCourses('PUBLIC', keyword.value || undefined, pageNum.value, pageSize)
    if (reset) {
      courses.value = res.records
    } else {
      courses.value = [...courses.value, ...res.records]
    }
    total.value = res.total
    loadStatus.value = courses.value.length >= total.value ? 'noMore' : 'more'
  } catch {
    uni.showToast({ title: '获取课程失败', icon: 'none' })
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function handleSearch() {
  fetchData(true)
}

function clearSearch() {
  keyword.value = ''
  fetchData(true)
}

function onRefresh() {
  refreshing.value = true
  fetchData(true)
}

function loadMore() {
  if (loadStatus.value === 'noMore' || loading.value) return
  pageNum.value++
  fetchData(false)
}

function openDetail(course: Course) {
  uni.navigateTo({ url: `/pages/courses/detail?id=${course.id}&type=PUBLIC` })
}

fetchData(true)
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f5f6fa;
}

.search-bar {
  background: #fff;
  padding: 16rpx 24rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
}

.search-box {
  display: flex;
  align-items: center;
  background: #f5f6fa;
  border-radius: 32rpx;
  padding: 12rpx 24rpx;
}

.search-icon { font-size: 28rpx; margin-right: 12rpx; }

.search-input {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}

.search-clear {
  font-size: 28rpx;
  color: #999;
  padding: 8rpx;
}

.list {
  padding: 16rpx 24rpx;
}

.course-card {
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 16rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
  display: flex;
}

.course-cover {
  width: 200rpx;
  height: 180rpx;
  flex-shrink: 0;
  background: #f0f2f5;
}

.cover-img {
  width: 100%;
  height: 100%;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
}

.cover-letter {
  font-size: 48rpx;
  color: #fff;
  font-weight: 700;
}

.course-body {
  flex: 1;
  padding: 16rpx 20rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.course-tags {
  display: flex;
  gap: 8rpx;
}

.tag {
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  background: #f0f2f5;
  color: #666;

  &.cat { background: #e8f4fd; color: #2563eb; }
  &.diff-BEGINNER { background: #e8f8e8; color: #67c23a; }
  &.diff-INTERMEDIATE { background: #fef0d0; color: #e6a23c; }
  &.diff-ADVANCED { background: #fde2e2; color: #f56c6c; }
}

.course-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-top: 8rpx;
}

.course-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.price-paid { font-size: 28rpx; color: #f56c6c; font-weight: 700; }
.price-free { font-size: 28rpx; color: #67c23a; font-weight: 700; }
.course-capacity { font-size: 22rpx; color: #999; }

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
}

.empty-icon { font-size: 72rpx; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: #999; }
</style>
