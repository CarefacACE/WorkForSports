<template>
  <view class="page">
    <view class="form-card">
      <text class="form-title">添加运动记录</text>

      <view class="form-item">
        <text class="form-label">运动类型 <text class="required">*</text></text>
        <view class="type-grid">
          <view
            v-for="t in types"
            :key="t"
            class="type-item"
            :class="{ active: form.type === t }"
            @tap="form.type = t"
          >
            <text class="type-icon">{{ typeIcon(t) }}</text>
            <text class="type-text">{{ t }}</text>
          </view>
        </view>
      </view>

      <view class="form-row">
        <view class="form-item half">
          <text class="form-label">时长 (分钟) <text class="required">*</text></text>
          <input v-model.number="form.duration" class="form-input" type="digit" placeholder="0" placeholder-style="color:#bbb" />
        </view>
        <view class="form-item half">
          <text class="form-label">距离 (km)</text>
          <input v-model.number="form.distance" class="form-input" type="digit" placeholder="0" placeholder-style="color:#bbb" />
        </view>
      </view>

      <view class="form-row">
        <view class="form-item half">
          <text class="form-label">卡路里 <text class="required">*</text></text>
          <input v-model.number="form.calories" class="form-input" type="digit" placeholder="0" placeholder-style="color:#bbb" />
        </view>
        <view class="form-item half">
          <text class="form-label">日期 <text class="required">*</text></text>
          <picker mode="date" :value="form.exerciseDate" @change="onDateChange">
            <view class="form-input picker">
              <text :class="{ placeholder: !form.exerciseDate }">{{ form.exerciseDate || '选择日期' }}</text>
            </view>
          </picker>
        </view>
      </view>

      <view class="form-item">
        <text class="form-label">备注</text>
        <textarea
          v-model="form.notes"
          class="form-textarea"
          placeholder="记录一下今天的训练感受..."
          placeholder-style="color:#bbb"
        />
      </view>

      <button class="btn-submit" :disabled="invalid || submitting" @tap="handleSubmit">
        <text v-if="!submitting">保存</text>
        <text v-else>保存中...</text>
      </button>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { reactive, ref, computed } from 'vue'
import { addExerciseRecord, type ExerciseRecord } from '@/api/exercise'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userId = userStore.userId

const types = ['跑步', '游泳', '骑行', '力量训练', '瑜伽', '篮球', '足球', '跳绳', '其他']
const submitting = ref(false)

function typeIcon(type: string): string {
  const map: Record<string, string> = {
    '跑步': '🏃', '游泳': '🏊', '骑行': '🚴', '力量训练': '🏋',
    '瑜伽': '🧘', '篮球': '🏀', '足球': '⚽', '跳绳': '🪢', '其他': '💪',
  }
  return map[type] || '💪'
}

const form = reactive<ExerciseRecord>({
  type: '跑步',
  duration: 0,
  distance: 0,
  calories: 0,
  exerciseDate: new Date().toISOString().split('T')[0],
  notes: '',
})

const invalid = computed(() => !form.type || !form.duration || !form.calories || !form.exerciseDate)

function onDateChange(e: { detail: { value: string } }) {
  form.exerciseDate = e.detail.value
}

async function handleSubmit() {
  if (invalid.value || submitting.value || !userId) return
  submitting.value = true
  try {
    await addExerciseRecord(userId, { ...form })
    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 800)
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '保存失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; padding: 24rpx; }

.form-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}

.form-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #333;
  margin-bottom: 32rpx;
  display: block;
  text-align: center;
}

.form-item { margin-bottom: 24rpx; }
.form-label { font-size: 26rpx; color: #666; margin-bottom: 8rpx; display: block; }
.required { color: #ff4d4f; }

.form-row { display: flex; gap: 16rpx; }
.half { flex: 1; }

.type-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.type-item {
  padding: 12rpx 20rpx;
  border-radius: 12rpx;
  border: 2rpx solid #e5e7eb;
  background: #f9fafb;
  display: flex;
  align-items: center;
  gap: 6rpx;

  &.active {
    border-color: #2563eb;
    background: #e8f4fd;
  }
}

.type-icon { font-size: 28rpx; }
.type-text { font-size: 24rpx; color: #333; .active & { color: #2563eb; font-weight: 600; } }

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

  &.picker { }
}

.placeholder { color: #bbb; }

.form-textarea {
  width: 100%;
  height: 150rpx;
  border: 2rpx solid #e5e7eb;
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  font-size: 28rpx;
  background: #f9fafb;
  box-sizing: border-box;
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
  margin-top: 16rpx;
}
</style>
