<template>
  <div class="member-profile-page">
    <!-- 头像上传 -->
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="card-header">
          <el-icon :size="18"><User /></el-icon>
          <span>个人头像</span>
        </div>
      </template>

      <div class="avatar-upload-area">
        <div class="avatar-preview">
          <img v-if="avatarUrl" :src="avatarUrl" class="avatar-img" alt="头像" />
          <div v-else class="avatar-placeholder">
            <el-icon :size="36"><User /></el-icon>
          </div>
        </div>
        <div class="avatar-actions">
          <el-upload
            :show-file-list="false"
            :before-upload="beforeAvatarUpload"
            :http-request="handleAvatarUpload"
            accept="image/*"
          >
            <el-button type="primary" :loading="avatarLoading">
              {{ avatarUrl ? '更换头像' : '上传头像' }}
            </el-button>
          </el-upload>
          <p class="avatar-hint">支持 JPG、PNG 格式，建议尺寸不超过 2MB</p>
        </div>
      </div>
    </el-card>

    <!-- 健康信息 -->
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="card-header">
          <el-icon :size="18"><FirstAidKit /></el-icon>
          <span>健康信息</span>
        </div>
      </template>

      <el-form :model="form" label-width="130px" label-position="right">
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="身高 (cm)">
              <el-input-number v-model="form.height" :min="50" :max="250" :precision="1" :step="0.5" controls-position="right" placeholder="请输入身高" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="体重 (kg)">
              <el-input-number v-model="form.weight" :min="20" :max="300" :precision="1" :step="0.5" controls-position="right" placeholder="请输入体重" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="BMI">
              <el-input :model-value="computedBMI" disabled>
                <template #suffix>
                  <el-tag v-if="bmiLabel" :type="bmiTagType" size="small">{{ bmiLabel }}</el-tag>
                </template>
              </el-input>
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="体脂率 (%)">
              <el-input-number v-model="form.bodyFat" :min="1" :max="70" :precision="1" :step="0.1" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="肌肉量 (kg)">
              <el-input-number v-model="form.muscleMass" :min="10" :max="100" :precision="1" :step="0.1" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="静息心率 (bpm)">
              <el-input-number v-model="form.restingHeartRate" :min="30" :max="200" :step="1" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="收缩压 (mmHg)">
              <el-input-number v-model="form.bpSystolic" :min="60" :max="260" :step="1" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="舒张压 (mmHg)">
              <el-input-number v-model="form.bpDiastolic" :min="30" :max="160" :step="1" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="血型">
              <el-select v-model="form.bloodType" placeholder="请选择血型" clearable class="full-width">
                <el-option label="A 型" value="A" />
                <el-option label="B 型" value="B" />
                <el-option label="AB 型" value="AB" />
                <el-option label="O 型" value="O" />
                <el-option label="未知" value="UNKNOWN" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="过敏史">
              <el-input v-model="form.allergies" type="textarea" :rows="2" placeholder="请描述过敏情况，无则留空" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="既往病史">
              <el-input v-model="form.medicalHistory" type="textarea" :rows="2" placeholder="请描述既往病史，无则留空" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="目前用药">
              <el-input v-model="form.currentMedications" type="textarea" :rows="2" placeholder="请描述目前用药情况，无则留空" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="紧急联系人">
              <el-input v-model="form.emergencyContactName" placeholder="请输入紧急联系人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急联系人电话">
              <el-input v-model="form.emergencyContactPhone" placeholder="请输入紧急联系人电话" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 达成目标 -->
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="card-header">
          <el-icon :size="18"><TrendCharts /></el-icon>
          <span>达成目标</span>
        </div>
      </template>

      <el-form :model="form" label-width="130px" label-position="right">
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="目标体重 (kg)">
              <el-input-number v-model="form.targetWeight" :min="20" :max="300" :precision="1" :step="0.5" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="目标体脂率 (%)">
              <el-input-number v-model="form.targetBodyFat" :min="1" :max="70" :precision="1" :step="0.1" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="目标肌肉量 (kg)">
              <el-input-number v-model="form.targetMuscleMass" :min="10" :max="100" :precision="1" :step="0.1" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="健身目标">
              <el-select v-model="form.fitnessGoal" placeholder="请选择" clearable class="full-width">
                <el-option label="增肌" value="MUSCLE_GAIN" />
                <el-option label="减脂" value="FAT_LOSS" />
                <el-option label="塑形" value="BODY_SHAPING" />
                <el-option label="体能提升" value="FITNESS" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="每周训练次数">
              <el-input-number v-model="form.weeklyWorkoutFreq" :min="1" :max="7" :step="1" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="期望达成日期">
              <el-date-picker v-model="form.targetDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择日期" class="full-width" />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="目标备注">
              <el-input v-model="form.goalNotes" type="textarea" :rows="3" placeholder="补充说明您的健身目标或计划" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 保存按钮 -->
    <div class="save-area">
      <el-button type="primary" size="large" :loading="saving" @click="handleSave">
        保存健康档案
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { FirstAidKit, TrendCharts, User } from '@element-plus/icons-vue';
import { getHealthProfile, saveHealthProfile, type HealthProfile } from '../../api/health';
import { uploadFile } from '../../api/file';
import { updateProfile } from '../../api/auth';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();
const userId = userStore.user?.id;

// ====== 头像上传 ======
const avatarUrl = ref(userStore.user?.avatar || '');
const avatarLoading = ref(false);

function beforeAvatarUpload(file: File) {
  const isImage = file.type.startsWith('image/');
  const isLt2M = file.size / 1024 / 1024 < 2;
  if (!isImage) { ElMessage.error('只能上传图片文件'); return false; }
  if (!isLt2M) { ElMessage.error('图片大小不能超过 2MB'); return false; }
  return true;
}

async function handleAvatarUpload(options: { file: File }) {
  if (!userId) return;
  avatarLoading.value = true;
  try {
    const result = await uploadFile(options.file, userId, userStore.user?.username || '');
    // Build download URL
    const base = import.meta.env.VITE_API_BASE_URL || '/api';
    const url = `${base}/file/download/${result.id}`;
    avatarUrl.value = url;
    // Update user profile avatar
    if (userStore.user) {
      await updateProfile({
        id: userStore.user.id,
        realName: userStore.user.realName || '',
        avatar: url,
      });
      userStore.user.avatar = url;
    }
    ElMessage.success('头像上传成功');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '上传失败');
  } finally {
    avatarLoading.value = false;
  }
}

const saving = ref(false);

const form = reactive<HealthProfile>({
  height: undefined,
  weight: undefined,
  bodyFat: undefined,
  muscleMass: undefined,
  bpSystolic: undefined,
  bpDiastolic: undefined,
  restingHeartRate: undefined,
  bloodType: undefined,
  allergies: undefined,
  medicalHistory: undefined,
  currentMedications: undefined,
  emergencyContactName: undefined,
  emergencyContactPhone: undefined,
  targetWeight: undefined,
  targetBodyFat: undefined,
  targetMuscleMass: undefined,
  fitnessGoal: undefined,
  weeklyWorkoutFreq: undefined,
  targetDate: undefined,
  goalNotes: undefined,
});

const computedBMI = computed(() => {
  if (!form.height || !form.weight) return '--';
  const heightM = form.height / 100;
  const bmi = form.weight / (heightM * heightM);
  return bmi.toFixed(1);
});

const bmiLabel = computed(() => {
  if (!form.height || !form.weight) return '';
  const bmi = parseFloat(computedBMI.value);
  if (isNaN(bmi)) return '';
  if (bmi < 18.5) return '偏瘦';
  if (bmi < 24) return '正常';
  if (bmi < 28) return '偏胖';
  return '肥胖';
});

const bmiTagType = computed(() => {
  if (!form.height || !form.weight) return 'info';
  const bmi = parseFloat(computedBMI.value);
  if (isNaN(bmi)) return 'info';
  if (bmi < 18.5) return 'warning';
  if (bmi < 24) return 'success';
  if (bmi < 28) return 'warning';
  return 'danger';
});

async function fetchProfile() {
  if (!userId) return;
  try {
    const data = await getHealthProfile(userId);
    if (data) {
      Object.assign(form, data);
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取健康档案失败');
  }
}

async function handleSave() {
  if (!userId) {
    ElMessage.warning('请先登录');
    return;
  }
  saving.value = true;
  try {
    await saveHealthProfile(userId, { ...form });
    ElMessage.success('健康档案保存成功');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  fetchProfile();
});
</script>

<style scoped>
.member-profile-page {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  max-width: 1100px;
  margin: 0 auto;
}

.section-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.full-width {
  width: 100%;
}

.save-area {
  text-align: center;
  padding: 16px 0 32px;
}

/* Avatar Upload */
.avatar-upload-area {
  display: flex;
  align-items: center;
  gap: 24px;
}
.avatar-preview {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  border: 3px solid var(--el-color-primary-light-7, #c6e2ff);
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light, #f5f7fa);
  color: var(--el-text-color-placeholder, #a8abb2);
}
.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.avatar-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
  margin: 0;
}
</style>
