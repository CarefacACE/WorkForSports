<template>
  <view class="page">
    <view v-if="loading" class="loading-wrap">
      <text class="loading-text">加载中...</text>
    </view>

    <view v-else-if="detail">
      <!-- 封面 -->
      <view class="cover">
        <image v-if="detail.coverImage" :src="detail.coverImage" mode="aspectFill" class="cover-img" />
        <view v-else class="cover-placeholder">
          <text class="cover-letter">{{ (detail.name || '课程').charAt(0) }}</text>
        </view>
      </view>

      <!-- 基本信息 -->
      <view class="section info-card">
        <text class="course-name">{{ detail.name || '私教课程' }}</text>
        <view class="info-tags">
          <text class="tag">{{ categoryLabel(detail.category) }}</text>
          <text class="tag diff">{{ difficultyLabel(detail.difficulty) }}</text>
          <text v-if="detail.type === 'PRIVATE'" class="tag private">私教</text>
        </view>

        <view class="info-price" v-if="type === 'PUBLIC'">
          <text v-if="(detail.price ?? 0) > 0" class="price-paid">¥{{ (detail.price ?? 0).toFixed(2) }}</text>
          <text v-else class="price-free">免费</text>
        </view>

        <view class="info-meta">
          <text v-if="detail.location" class="meta-item">📍 {{ detail.location }}</text>
          <text v-if="detail.startDate" class="meta-item">📅 {{ detail.startDate }}</text>
          <text v-if="(detail.maxStudents ?? 0) > 0" class="meta-item">👥 限{{ detail.maxStudents }}人</text>
        </view>

        <!-- 私教信息 -->
        <view v-if="type === 'PRIVATE' && coachProfile" class="coach-info">
          <text class="section-subtitle">教练信息</text>
          <view class="coach-row">
            <text class="coach-name">{{ coachProfile.realName || '教练' }}</text>
            <text class="coach-price">¥{{ (coachProfile.pricePerSession ?? 0) }} / 节</text>
          </view>
          <text v-if="coachProfile.specialties" class="coach-spec">{{ coachProfile.specialties }}</text>
          <text v-if="coachProfile.description" class="coach-desc">{{ coachProfile.description }}</text>
        </view>
      </view>

      <!-- 课程描述 -->
      <view class="section" v-if="detail.description">
        <text class="section-title">课程介绍</text>
        <view class="card">
          <text class="desc-text">{{ detail.description }}</text>
        </view>
      </view>

      <!-- 课时列表（仅公共课） -->
      <view class="section" v-if="type === 'PUBLIC' && lessons.length > 0">
        <text class="section-title">课时列表 ({{ lessons.length }})</text>
        <view class="card">
          <view v-for="(lesson, i) in lessons" :key="lesson.id" class="lesson-item">
            <view class="lesson-num">{{ i + 1 }}</view>
            <view class="lesson-info">
              <text class="lesson-title">{{ lesson.title }}</text>
              <text class="lesson-duration">{{ lesson.duration ? lesson.duration + '分钟' : '-' }}</text>
            </view>
            <text class="lesson-trial" :class="{ isTrial: lesson.isTrial }">
              {{ lesson.isTrial ? '可试听' : '付费' }}
            </text>
          </view>
        </view>
      </view>

      <!-- 底部操作栏 -->
      <view class="bottom-bar" v-if="type === 'PUBLIC'">
        <template v-if="!enrollment">
          <button v-if="(detail.price ?? 0) > 0" class="btn-pay" @tap="handlePay">
            立即购买 ¥{{ (detail.price ?? 0).toFixed(2) }}
          </button>
          <button v-else class="btn-enroll" @tap="handleEnrollFree">
            免费报名
          </button>
        </template>
        <view v-else class="enrollment-badge">
          <text v-if="enrollment.status === 'TRIAL'" class="badge trial">试听中</text>
          <text v-else-if="enrollment.status === 'PAID'" class="badge paid">已购买</text>
          <text v-else-if="enrollment.status === 'CONFIRMED'" class="badge confirmed">已确认</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { getCourseDetail, type Course } from '@/api/course'
import { getLessons, type Lesson } from '@/api/lesson'
import { enroll, payCourse, getMyEnrollments, type Enrollment } from '@/api/enrollment'
import { getCoachDetail, type CoachListItem } from '@/api/privateCoach'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userId = userStore.userId

const loading = ref(true)
const type = ref<'PUBLIC' | 'PRIVATE'>('PUBLIC')
const detail = ref<Course | null>(null)
const lessons = ref<Lesson[]>([])
const enrollment = ref<Enrollment | null>(null)
const coachProfile = ref<CoachListItem | null>(null)

const CATEGORIES: Record<string, string> = {
  YOGA: '瑜伽', BOXING: '拳击', SWIMMING: '游泳', RUNNING: '跑步',
  STRENGTH: '力量', DANCE: '舞蹈', OTHER: '其他',
}
const DIFFICULTIES: Record<string, string> = {
  BEGINNER: '初级', INTERMEDIATE: '中级', ADVANCED: '高级',
}
function categoryLabel(v?: string) { return CATEGORIES[v || ''] || '其他' }
function difficultyLabel(v?: string) { return DIFFICULTIES[v || ''] || '初级' }

onMounted(async () => {
  // 获取页面参数
  const pages = getCurrentPages()
  const page = pages[pages.length - 1]
  const opts = (page as unknown as { options: Record<string, string> }).options

  const id = Number(opts.id)
  type.value = (opts.type as 'PUBLIC' | 'PRIVATE') || 'PUBLIC'

  try {
    if (type.value === 'PUBLIC') {
      detail.value = await getCourseDetail(id)
      lessons.value = await getLessons(id)
      if (userId) {
        const res = await getMyEnrollments(userId, 'PUBLIC', 1, 100)
        enrollment.value = res.records.find(e => e.courseId === id) || null
      }
    } else {
      // 私教
      coachProfile.value = await getCoachDetail(id)
      detail.value = {
        id,
        name: coachProfile.value.realName || '私教',
        description: coachProfile.value.description,
        type: 'PRIVATE',
        price: coachProfile.value.pricePerSession,
        category: (coachProfile.value as unknown as { category?: string }).category || '',
        difficulty: '',
        coachId: (coachProfile.value as unknown as { coachId: number }).coachId || id,
        status: 'ACTIVE',
      } as unknown as Course
    }
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
})

async function handleEnrollFree() {
  if (!userId || !detail.value) return
  try {
    await enroll(userId, detail.value.id)
    uni.showToast({ title: '报名成功', icon: 'success' })
    // 刷新报名状态
    const res = await getMyEnrollments(userId, 'PUBLIC', 1, 100)
    enrollment.value = res.records.find(e => e.courseId === detail.value!.id) || null
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '报名失败', icon: 'none' })
  }
}

async function handlePay() {
  if (!userId || !detail.value) return
  try {
    await payCourse(userId, detail.value.id)
    uni.showToast({ title: '购买成功', icon: 'success' })
    const res = await getMyEnrollments(userId, 'PUBLIC', 1, 100)
    enrollment.value = res.records.find(e => e.courseId === detail.value!.id) || null
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '购买失败', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; padding-bottom: 120rpx; }

.loading-wrap {
  display: flex; align-items: center; justify-content: center;
  padding: 200rpx 0;
}
.loading-text { font-size: 28rpx; color: #999; }

/* 封面 */
.cover {
  width: 100%;
  height: 400rpx;
  background: #f0f2f5;
}
.cover-img { width: 100%; height: 100%; }
.cover-placeholder {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
}
.cover-letter { font-size: 80rpx; color: #fff; font-weight: 700; }

/* 信息卡片 */
.section {
  margin: 24rpx;
}

.info-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-top: -32rpx;
  position: relative;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.06);
}

.course-name {
  font-size: 36rpx;
  font-weight: 700;
  color: #333;
  margin-bottom: 12rpx;
}

.info-tags {
  display: flex;
  gap: 8rpx;
  margin-bottom: 16rpx;
}

.tag {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  background: #f0f2f5;
  color: #666;

  &.private { background: #fef0d0; color: #e6a23c; }
  &.diff { background: #e8f4fd; color: #2563eb; }
}

.info-price {
  margin-bottom: 16rpx;
}

.price-paid { font-size: 40rpx; font-weight: 700; color: #f56c6c; }
.price-free { font-size: 40rpx; font-weight: 700; color: #67c23a; }

.info-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.meta-item {
  font-size: 24rpx;
  color: #999;
}

/* 教练信息 */
.coach-info {
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 2rpx solid #f0f2f5;
}

.section-subtitle {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 12rpx;
}

.coach-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}

.coach-name { font-size: 30rpx; font-weight: 600; color: #333; }
.coach-price { font-size: 28rpx; color: #f56c6c; font-weight: 600; }
.coach-spec { font-size: 24rpx; color: #666; }
.coach-desc { font-size: 24rpx; color: #999; margin-top: 8rpx; }

/* 介绍 */
.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 12rpx;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}

.desc-text {
  font-size: 28rpx;
  color: #666;
  line-height: 1.8;
}

/* 课时列表 */
.lesson-item {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 2rpx solid #f5f6fa;

  &:last-child { border-bottom: none; }
}

.lesson-num {
  width: 48rpx;
  height: 48rpx;
  border-radius: 24rpx;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  color: #666;
  margin-right: 16rpx;
}

.lesson-info {
  flex: 1;
}

.lesson-title {
  font-size: 28rpx;
  color: #333;
  display: block;
  margin-bottom: 4rpx;
}

.lesson-duration {
  font-size: 22rpx;
  color: #999;
}

.lesson-trial {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  background: #f0f2f5;
  color: #666;

  &.isTrial {
    background: #e8f8e8;
    color: #67c23a;
  }
}

/* 底部栏 */
.bottom-bar {
  position: fixed;
  bottom: 0; left: 0; right: 0;
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2rpx 12rpx rgba(0,0,0,0.06);
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-enroll {
  width: 100%;
  height: 88rpx;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  border-radius: 16rpx;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
}

.btn-pay {
  width: 100%;
  height: 88rpx;
  background: linear-gradient(135deg, #f56c6c, #e04040);
  color: #fff;
  border-radius: 16rpx;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
}

.enrollment-badge {
  display: flex;
  align-items: center;
  justify-content: center;
}

.badge {
  padding: 12rpx 48rpx;
  border-radius: 16rpx;
  font-size: 28rpx;
  font-weight: 600;

  &.trial { background: #fef0d0; color: #e6a23c; }
  &.paid { background: #e8f8e8; color: #67c23a; }
  &.confirmed { background: #e8f4fd; color: #2563eb; }
}
</style>
