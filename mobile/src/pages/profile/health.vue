<template>
  <view class="page">
    <view v-if="loading" class="loading-wrap">
      <text>加载中...</text>
    </view>

    <view v-else class="form-card">
      <view class="grp">
        <text class="grp-title">身体指标</text>

        <view class="form-row">
          <view class="form-item half">
            <text class="form-label">身高 (cm)</text>
            <input v-model.number="form.height" class="form-input" type="digit" placeholder="--" placeholder-style="color:#bbb" />
          </view>
          <view class="form-item half">
            <text class="form-label">体重 (kg)</text>
            <input v-model.number="form.weight" class="form-input" type="digit" placeholder="--" placeholder-style="color:#bbb" />
          </view>
        </view>

        <view class="form-row">
          <view class="form-item half">
            <text class="form-label">体脂率 (%)</text>
            <input v-model.number="form.bodyFat" class="form-input" type="digit" placeholder="--" placeholder-style="color:#bbb" />
          </view>
          <view class="form-item half">
            <text class="form-label">肌肉量 (kg)</text>
            <input v-model.number="form.muscleMass" class="form-input" type="digit" placeholder="--" placeholder-style="color:#bbb" />
          </view>
        </view>
      </view>

      <view class="grp">
        <text class="grp-title">血压与心率</text>

        <view class="form-row">
          <view class="form-item half">
            <text class="form-label">收缩压</text>
            <input v-model.number="form.bpSystolic" class="form-input" type="digit" placeholder="--" placeholder-style="color:#bbb" />
          </view>
          <view class="form-item half">
            <text class="form-label">舒张压</text>
            <input v-model.number="form.bpDiastolic" class="form-input" type="digit" placeholder="--" placeholder-style="color:#bbb" />
          </view>
        </view>

        <view class="form-item">
          <text class="form-label">静息心率</text>
          <input v-model.number="form.restingHeartRate" class="form-input" type="digit" placeholder="--" placeholder-style="color:#bbb" />
        </view>
      </view>

      <view class="grp">
        <text class="grp-title">健身目标</text>

        <view class="form-item">
          <text class="form-label">目标体重 (kg)</text>
          <input v-model.number="form.targetWeight" class="form-input" type="digit" placeholder="--" placeholder-style="color:#bbb" />
        </view>
        <view class="form-item">
          <text class="form-label">健身目标</text>
          <picker :range="goals" :value="goalIndex" @change="onGoalChange">
            <view class="form-input picker">
              <text>{{ form.fitnessGoal || '选择目标' }}</text>
            </view>
          </picker>
        </view>
        <view class="form-item">
          <text class="form-label">每周训练频率</text>
          <input v-model.number="form.weeklyWorkoutFreq" class="form-input" type="digit" placeholder="次/周" placeholder-style="color:#bbb" />
        </view>
      </view>

      <button class="btn-submit" :disabled="submitting" @tap="handleSave">
        <text>{{ submitting ? '保存中...' : '保存健康档案' }}</text>
      </button>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { reactive, ref, computed } from 'vue'
import { getHealthProfile, saveHealthProfile, type HealthProfile } from '@/api/health'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userId = userStore.userId
const loading = ref(true)
const submitting = ref(false)
const goals = ['减脂', '增肌', '塑形', '增强体能', '保持健康', '其他']

const form = reactive<HealthProfile>({
  height: undefined, weight: undefined, bodyFat: undefined, muscleMass: undefined,
  bpSystolic: undefined, bpDiastolic: undefined, restingHeartRate: undefined,
  targetWeight: undefined, targetBodyFat: undefined, targetMuscleMass: undefined,
  fitnessGoal: '',
  weeklyWorkoutFreq: undefined,
})

const goalIndex = computed(() => {
  if (!form.fitnessGoal) return 0
  const idx = goals.indexOf(form.fitnessGoal)
  return idx >= 0 ? idx : 0
})

function onGoalChange(e: { detail: { value: number } }) {
  form.fitnessGoal = goals[e.detail.value]
}

async function fetchData() {
  if (!userId) return
  try {
    const p = await getHealthProfile(userId)
    if (p) Object.assign(form, p)
  } catch { /* no profile yet */ }
  loading.value = false
}

async function handleSave() {
  if (submitting.value || !userId) return
  submitting.value = true
  try {
    await saveHealthProfile(userId, { ...form })
    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 800)
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '保存失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

fetchData()
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; padding: 24rpx; }

.loading-wrap { display: flex; align-items: center; justify-content: center; padding: 200rpx 0; font-size: 28rpx; color: #999; }

.form-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}

.grp {
  margin-bottom: 32rpx;
  padding-bottom: 32rpx;
  border-bottom: 2rpx solid #f0f2f5;

  &:last-of-type { border-bottom: none; }
}

.grp-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 20rpx;
  display: block;
}

.form-item { margin-bottom: 20rpx; }
.form-label { font-size: 26rpx; color: #666; margin-bottom: 8rpx; display: block; }

.form-row { display: flex; gap: 16rpx; }
.half { flex: 1; }

.form-input {
  height: 80rpx;
  border: 2rpx solid #e5e7eb;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  color: #333;
  background: #f9fafb;
  display: flex;
  align-items: center;
}

.btn-submit {
  width: 100%;
  height: 88rpx;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  border-radius: 16rpx;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
  margin-top: 8rpx;
}
</style>
