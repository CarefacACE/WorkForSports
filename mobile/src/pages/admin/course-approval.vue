<template>
  <view class="page">
    <view class="tabs">
      <view
        v-for="t in tabs"
        :key="t.value"
        class="tab"
        :class="{ active: activeTab === t.value }"
        @tap="activeTab = t.value; fetchList()"
      >
        <text>{{ t.label }}</text>
      </view>
    </view>

    <scroll-view class="list" scroll-y>
      <view v-if="list.length === 0 && !loading" class="empty">
        <text class="empty-icon">📭</text>
        <text class="empty-text">暂无 {{ activeTab === 'PENDING' ? '待审批' : activeTab === 'ACTIVE' ? '已通过' : '已驳回' }} 课程</text>
      </view>

      <view v-for="c in list" :key="c.id" class="card">
        <view class="c-header">
          <text class="c-name">{{ c.name }}</text>
          <text class="c-type" :class="c.type">{{ c.type === 'PUBLIC' ? '公共课' : '私教' }}</text>
        </view>
        <view class="c-meta">
          <text>教练ID: {{ c.coachId }}</text>
          <text>¥{{ c.price }}</text>
          <text>{{ c.createTime?.split(' ')[0] }}</text>
        </view>

        <view class="c-actions" v-if="activeTab === 'PENDING'">
          <button class="btn-approve" @tap="handleApprove(c.id)">✅ 通过</button>
          <button class="btn-reject" @tap="openReject(c)">❌ 驳回</button>
        </view>
        <view class="c-actions" v-else-if="activeTab === 'ACTIVE'">
          <button class="btn-reject" @tap="openReject(c)">改为驳回</button>
        </view>
        <view class="c-actions" v-else>
          <button class="btn-approve" @tap="handleApprove(c.id)">改为通过</button>
        </view>
      </view>
    </scroll-view>

    <!-- 驳回弹窗 -->
    <uni-popup ref="popup" type="center">
      <view class="popup-card">
        <text class="popup-title">驳回理由</text>
        <textarea v-model="rejectReason" class="popup-textarea" placeholder="驳回原因将通知教练，教练修改后可重新申请" placeholder-style="color:#bbb" />
        <view class="popup-btns">
          <button class="btn-cancel" @tap="closeReject">取消</button>
          <button class="btn-confirm-reject" @tap="doReject">确认驳回</button>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { getPendingCourses, getAllCoursesForAdmin, approveCourse, rejectCourse, type Course } from '@/api/course'

const tabs = [
  { value: 'PENDING', label: '⏳ 待审批' },
  { value: 'ACTIVE', label: '✅ 已通过' },
  { value: 'REJECTED', label: '❌ 已驳回' },
]

const activeTab = ref('PENDING')
const list = ref<Course[]>([])
const loading = ref(false)
const rejectId = ref(0)
const rejectReason = ref('')

const popup = ref<{ open: () => void; close: () => void }>()

async function fetchList() {
  loading.value = true
  try {
    if (activeTab.value === 'PENDING') {
      list.value = await getPendingCourses()
    } else {
      const all = await getAllCoursesForAdmin(activeTab.value === 'ACTIVE' ? 'ACTIVE' : 'REJECTED')
      list.value = all || []
    }
  } catch {
    uni.showToast({ title: '获取数据失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function handleApprove(id: number) {
  try {
    await approveCourse(id)
    uni.showToast({ title: '已通过', icon: 'success' })
    fetchList()
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '操作失败', icon: 'none' })
  }
}

function openReject(c: Course) {
  rejectId.value = c.id
  rejectReason.value = ''
  popup.value?.open()
}

function closeReject() {
  rejectId.value = 0
  rejectReason.value = ''
  popup.value?.close()
}

async function doReject() {
  if (!rejectId.value) return
  try {
    await rejectCourse(rejectId.value, rejectReason.value)
    uni.showToast({ title: '已驳回', icon: 'success' })
    closeReject()
    fetchList()
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '操作失败', icon: 'none' })
  }
}

fetchList()
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; }

.tabs { display: flex; background: #fff; border-bottom: 2rpx solid #f0f2f5; position: sticky; top: 0; z-index: 10; }
.tab { flex: 1; text-align: center; padding: 24rpx 0; font-size: 26rpx; color: #666; border-bottom: 4rpx solid transparent; }
.tab.active { color: #2563eb; font-weight: 600; border-bottom-color: #2563eb; }

.list { padding: 16rpx 24rpx; }

.card {
  background: #fff; border-radius: 16rpx; padding: 24rpx;
  margin-bottom: 12rpx; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
}

.c-header { display: flex; align-items: center; gap: 12rpx; margin-bottom: 12rpx; }
.c-name { font-size: 30rpx; font-weight: 600; color: #333; flex: 1; }
.c-type { font-size: 22rpx; padding: 4rpx 14rpx; border-radius: 8rpx; }
.c-type.PUBLIC { background: #e8f4fd; color: #2563eb; }
.c-type.PRIVATE { background: #fef0d0; color: #e6a23c; }

.c-meta { display: flex; gap: 20rpx; font-size: 22rpx; color: #999; margin-bottom: 16rpx; }

.c-actions { display: flex; gap: 16rpx; }

.btn-approve, .btn-reject {
  flex: 1; height: 68rpx; border-radius: 12rpx; font-size: 24rpx; border: none; display: flex; align-items: center; justify-content: center;
}
.btn-approve { background: #e8f8e8; color: #67c23a; }
.btn-reject { background: #fde2e2; color: #f56c6c; }

.empty { display: flex; flex-direction: column; align-items: center; padding: 160rpx 0; }
.empty-icon { font-size: 72rpx; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: #999; }

/* 弹窗 */
.popup-card {
  width: 560rpx; background: #fff; border-radius: 24rpx; padding: 40rpx 32rpx;
}
.popup-title { font-size: 32rpx; font-weight: 700; color: #333; margin-bottom: 20rpx; display: block; text-align: center; }
.popup-textarea {
  width: 100%; height: 160rpx; border: 2rpx solid #e5e7eb; border-radius: 12rpx;
  padding: 16rpx; font-size: 26rpx; box-sizing: border-box; margin-bottom: 24rpx; background: #f9fafb;
}
.popup-btns { display: flex; gap: 16rpx; }
.btn-cancel, .btn-confirm-reject {
  flex: 1; height: 80rpx; border-radius: 12rpx; font-size: 28rpx; border: none; display: flex; align-items: center; justify-content: center;
}
.btn-cancel { background: #f5f6fa; color: #666; }
.btn-confirm-reject { background: #f56c6c; color: #fff; }
</style>
