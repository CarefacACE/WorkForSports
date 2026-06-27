<template>
  <div class="profile-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>🏋️ 我的私教主页</span>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </div>
      </template>

      <el-form :model="form" label-width="120px" label-position="right">
        <el-form-item label="封面图">
          <div class="cover-upload">
            <img v-if="form.coverImage" :src="form.coverImage" class="cover-preview" />
            <el-upload
              :show-file-list="false"
              :before-upload="beforeCoverUpload"
              :http-request="handleCoverUpload"
              accept="image/*"
            >
              <el-button>{{ form.coverImage ? '更换封面' : '上传封面' }}</el-button>
            </el-upload>
          </div>
        </el-form-item>

        <el-form-item label="自我介绍">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="介绍一下你的教学风格、经验和理念..." />
        </el-form-item>

        <el-form-item label="专长">
          <div class="specialty-tags">
            <el-tag
              v-for="tag in specialtyList"
              :key="tag"
              closable
              @close="removeSpecialty(tag)"
              class="specialty-tag"
            >{{ tag }}</el-tag>
            <el-input
              v-if="specialtyInputVisible"
              ref="specialtyInputRef"
              v-model="specialtyInputValue"
              size="small"
              style="width: 100px"
              @keyup.enter="addSpecialty"
              @blur="addSpecialty"
            />
            <el-button v-else size="small" @click="showSpecialtyInput">+ 添加</el-button>
          </div>
        </el-form-item>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="单价 (元/节)">
              <el-input-number v-model="form.pricePerSession" :min="0" :step="10" :precision="0" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课时 (分钟)">
              <el-input-number v-model="form.sessionDuration" :min="15" :max="180" :step="15" controls-position="right" class="full-width" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="状态">
          <el-switch v-model="statusActive" active-text="开放" inactive-text="关闭" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue';
import { ElMessage } from 'element-plus';
import { getMyProfile, saveMyProfile, type PrivateCoachProfile } from '../../api/privateCoach';
import { uploadFile } from '../../api/file';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();
const coachId = userStore.user?.id || 0;
const saving = ref(false);

const form = reactive<PrivateCoachProfile>({
  description: '',
  specialties: '',
  pricePerSession: 0,
  sessionDuration: 60,
  coverImage: '',
  status: 'ACTIVE',
});

const statusActive = computed({
  get: () => form.status === 'ACTIVE',
  set: (val) => { form.status = val ? 'ACTIVE' : 'INACTIVE'; },
});

// Specialties management
const specialtyList = computed(() => form.specialties ? form.specialties.split(',').filter(Boolean) : []);
const specialtyInputVisible = ref(false);
const specialtyInputValue = ref('');
const specialtyInputRef = ref();

function removeSpecialty(tag: string) {
  form.specialties = specialtyList.value.filter(t => t !== tag).join(',');
}

function showSpecialtyInput() {
  specialtyInputVisible.value = true;
  nextTick(() => specialtyInputRef.value?.focus());
}

function addSpecialty() {
  const val = specialtyInputValue.value.trim();
  if (val) {
    const list = specialtyList.value;
    if (!list.includes(val)) {
      list.push(val);
      form.specialties = list.join(',');
    }
  }
  specialtyInputVisible.value = false;
  specialtyInputValue.value = '';
}

// Cover upload
function beforeCoverUpload(file: File) {
  if (!file.type.startsWith('image/')) { ElMessage.error('请选择图片'); return false; }
  if (file.size / 1024 / 1024 > 5) { ElMessage.error('图片不超过 5MB'); return false; }
  return true;
}

async function handleCoverUpload(options: { file: File }) {
  try {
    const result = await uploadFile(options.file, coachId, userStore.user?.username || '');
    const base = import.meta.env.VITE_API_BASE_URL || '/api';
    form.coverImage = `${base}/file/download/${result.id}`;
    ElMessage.success('封面上传成功');
  } catch (e) {
    ElMessage.error('上传失败');
  }
}

// Load & Save
async function fetchProfile() {
  try {
    const data = await getMyProfile(coachId);
    if (data) {
      form.description = data.description || '';
      form.specialties = data.specialties || '';
      form.pricePerSession = data.pricePerSession || 0;
      form.sessionDuration = data.sessionDuration || 60;
      form.coverImage = data.coverImage || '';
      form.status = data.status || 'ACTIVE';
    }
  } catch {}
}

async function handleSave() {
  saving.value = true;
  try {
    await saveMyProfile(coachId, { ...form });
    ElMessage.success('保存成功');
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

onMounted(fetchProfile);
</script>

<style scoped>
.profile-page { display: flex; flex-direction: column; flex: 1; min-height: 0; max-width: 800px; }
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }
.cover-upload { display: flex; align-items: center; gap: 16px; }
.cover-preview { width: 160px; height: 100px; object-fit: cover; border-radius: 8px; border: 1px solid #eee; }
.specialty-tags { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.specialty-tag { border-radius: 6px; }
.full-width { width: 100%; }
</style>
