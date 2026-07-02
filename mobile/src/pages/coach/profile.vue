<template>
  <view class="page">
    <view class="form-card" v-if="!loading">
      <!-- 封面 -->
      <view class="cover-area" @tap="selectCover">
        <image v-if="form.coverImage" :src="form.coverImage" mode="aspectFill" class="cover-img" />
        <view v-else class="cover-placeholder">
          <text class="cover-icon">📷</text>
          <text class="cover-hint">点击上传封面图</text>
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">自我介绍</text>
        <textarea v-model="form.description" class="form-textarea" placeholder="介绍你的教学风格、经验和理念..." placeholder-style="color:#bbb" />
      </view>

      <view class="form-item">
        <text class="form-label">专长</text>
        <view class="spec-list">
          <text v-for="t in specList" :key="t" class="spec" @tap="removeSpec(t)">{{ t }} ✕</text>
          <view class="spec-add" v-if="specInputVisible">
            <input v-model="specInput" class="spec-input" placeholder="输入专长" placeholder-style="color:#bbb" confirm-type="done" @confirm="addSpec" />
          </view>
          <text v-else class="spec-add-btn" @tap="specInputVisible = true">+ 添加</text>
        </view>
      </view>

      <view class="form-row">
        <view class="form-item half">
          <text class="form-label">单价 (元/节)</text>
          <input v-model.number="form.pricePerSession" class="form-input" type="digit" placeholder="0" placeholder-style="color:#bbb" />
        </view>
        <view class="form-item half">
          <text class="form-label">课时 (分钟)</text>
          <input v-model.number="form.sessionDuration" class="form-input" type="digit" placeholder="60" placeholder-style="color:#bbb" />
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">状态</text>
        <switch :checked="form.status === 'ACTIVE'" @change="onStatusChange" />
      </view>

      <button class="btn-save" :disabled="saving" @tap="handleSave">
        <text>{{ saving ? '保存中...' : '保存' }}</text>
      </button>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { reactive, ref, computed } from 'vue'
import { getMyProfile, saveMyProfile, type PrivateCoachProfile } from '@/api/privateCoach'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const coachId = userStore.userId || 0

const loading = ref(true)
const saving = ref(false)
const specInput = ref('')
const specInputVisible = ref(false)

const form = reactive<PrivateCoachProfile>({
  description: '',
  specialties: '',
  pricePerSession: 0,
  sessionDuration: 60,
  coverImage: '',
  status: 'ACTIVE',
})

const specList = computed({
  get: () => form.specialties ? form.specialties.split(',').filter(Boolean) : [],
  set: (vals: string[]) => { form.specialties = vals.join(','); },
})

function addSpec() {
  const v = specInput.value.trim()
  if (!v) return
  const list = [...specList.value, v]
  specList.value = list
  specInput.value = ''
  specInputVisible.value = false
}

function removeSpec(t: string) {
  specList.value = specList.value.filter(s => s !== t)
}

function onStatusChange(e: { detail: { value: boolean } }) {
  form.status = e.detail.value ? 'ACTIVE' : 'INACTIVE'
}

function selectCover() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: (res) => { form.coverImage = res.tempFilePaths[0]; },
  })
}

async function handleSave() {
  if (saving.value) return
  saving.value = true
  try {
    await saveMyProfile(coachId, { ...form })
    uni.showToast({ title: '保存成功', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

async function fetchData() {
  if (!coachId) { loading.value = false; return }
  try {
    const p = await getMyProfile(coachId)
    if (p) Object.assign(form, p)
  } catch { /* */ }
  loading.value = false
}

fetchData()
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; padding: 24rpx; }

.form-card { background: #fff; border-radius: 16rpx; padding: 32rpx 24rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04); }

.cover-area {
  width: 100%; height: 320rpx; border-radius: 16rpx; overflow: hidden;
  margin-bottom: 32rpx; background: #f0f2f5;
}
.cover-img { width: 100%; height: 100%; }
.cover-placeholder { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.cover-icon { font-size: 56rpx; margin-bottom: 8rpx; }
.cover-hint { font-size: 26rpx; color: #999; }

.form-item { margin-bottom: 24rpx; }
.form-label { font-size: 26rpx; color: #666; margin-bottom: 8rpx; display: block; }
.form-row { display: flex; gap: 16rpx; }
.half { flex: 1; }

.form-textarea {
  width: 100%; height: 160rpx; border: 2rpx solid #e5e7eb;
  border-radius: 12rpx; padding: 16rpx; font-size: 28rpx; box-sizing: border-box;
  background: #f9fafb;
}

.form-input {
  height: 80rpx; border: 2rpx solid #e5e7eb; border-radius: 12rpx;
  padding: 0 20rpx; font-size: 28rpx; color: #333; background: #f9fafb;
  display: flex; align-items: center;
}

.spec-list { display: flex; flex-wrap: wrap; gap: 12rpx; }
.spec {
  padding: 8rpx 20rpx; background: #e8f4fd; color: #2563eb; border-radius: 8rpx; font-size: 24rpx;
}
.spec-add { }
.spec-input { height: 56rpx; border: 2rpx dashed #ccc; border-radius: 8rpx; padding: 0 12rpx; font-size: 24rpx; width: 140rpx; }
.spec-add-btn { font-size: 24rpx; color: #2563eb; padding: 8rpx 16rpx; border: 2rpx dashed #2563eb; border-radius: 8rpx; }

.btn-save {
  width: 100%; height: 88rpx; background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff; border-radius: 16rpx; font-size: 30rpx; font-weight: 600;
  border: none; margin-top: 8rpx;
}
</style>
