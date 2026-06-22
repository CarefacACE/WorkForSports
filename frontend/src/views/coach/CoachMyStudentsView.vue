<template>
  <div class="my-students-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的学员</span>
        </div>
      </template>

      <div class="toolbar">
        <el-select v-model="selectedCourseId" placeholder="选择课程" clearable style="width: 200px; margin-right: 12px" @change="handleCourseChange">
          <el-option v-for="c in myCourses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索用户名/姓名/手机号" clearable style="width: 280px"
          @clear="handleSearch" @keyup.enter="handleSearch">
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>

      <el-table :data="students" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="gender" label="性别" width="70" align="center" />
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewStudentDetail(row)">查看信息</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="pageNum" :total="total" layout="total, prev, pager, next"
          @current-change="fetchStudents" />
      </div>
    </el-card>

    <!-- 学员详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="currentStudent ? `学员信息 — ${currentStudent.realName || currentStudent.username}` : '学员信息'" width="640px" top="5vh">
      <div v-loading="detailLoading">
        <div class="detail-section">
          <div class="detail-section-title">👤 个人信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="用户名">{{ currentStudent?.username }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ currentStudent?.realName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ currentStudent?.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ currentStudent?.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ currentStudent?.gender || '-' }}</el-descriptions-item>
            <el-descriptions-item label="生日">{{ currentStudent?.birthday || '-' }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ currentStudent?.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-section">
          <div class="detail-section-title">❤️ 健康信息</div>
          <template v-if="healthProfile">
            <el-descriptions :column="3" border size="small">
              <el-descriptions-item label="身高">{{ healthProfile.height ? healthProfile.height + ' cm' : '-' }}</el-descriptions-item>
              <el-descriptions-item label="体重">{{ healthProfile.weight ? healthProfile.weight + ' kg' : '-' }}</el-descriptions-item>
              <el-descriptions-item label="BMI">{{ computedBMI }}</el-descriptions-item>
              <el-descriptions-item label="体脂率">{{ healthProfile.bodyFat ? healthProfile.bodyFat + '%' : '-' }}</el-descriptions-item>
              <el-descriptions-item label="肌肉量">{{ healthProfile.muscleMass ? healthProfile.muscleMass + ' kg' : '-' }}</el-descriptions-item>
              <el-descriptions-item label="静息心率">{{ healthProfile.restingHeartRate ? healthProfile.restingHeartRate + ' bpm' : '-' }}</el-descriptions-item>
              <el-descriptions-item label="血压">{{ bloodPressure }}</el-descriptions-item>
              <el-descriptions-item label="血型">{{ bloodTypeLabel }}</el-descriptions-item>
              <el-descriptions-item label="紧急联系人">{{ healthProfile.emergencyContactName || '-' }} {{ healthProfile.emergencyContactPhone || '' }}</el-descriptions-item>
            </el-descriptions>
            <el-descriptions :column="1" border size="small" style="margin-top: 8px">
              <el-descriptions-item label="过敏史">{{ healthProfile.allergies || '-' }}</el-descriptions-item>
              <el-descriptions-item label="既往病史">{{ healthProfile.medicalHistory || '-' }}</el-descriptions-item>
              <el-descriptions-item label="目前用药">{{ healthProfile.currentMedications || '-' }}</el-descriptions-item>
            </el-descriptions>
          </template>
          <el-empty v-else description="该学员暂未填写健康信息" :image-size="40" />
        </div>

        <div class="detail-section">
          <div class="detail-section-title">🎯 训练目标</div>
          <template v-if="healthProfile && (healthProfile.targetWeight || healthProfile.fitnessGoal)">
            <el-descriptions :column="3" border size="small">
              <el-descriptions-item label="目标体重">{{ healthProfile.targetWeight ? healthProfile.targetWeight + ' kg' : '-' }}</el-descriptions-item>
              <el-descriptions-item label="目标体脂率">{{ healthProfile.targetBodyFat ? healthProfile.targetBodyFat + '%' : '-' }}</el-descriptions-item>
              <el-descriptions-item label="目标肌肉量">{{ healthProfile.targetMuscleMass ? healthProfile.targetMuscleMass + ' kg' : '-' }}</el-descriptions-item>
              <el-descriptions-item label="健身目标">{{ fitnessGoalLabel }}</el-descriptions-item>
              <el-descriptions-item label="每周训练">{{ healthProfile.weeklyWorkoutFreq ? healthProfile.weeklyWorkoutFreq + ' 次' : '-' }}</el-descriptions-item>
              <el-descriptions-item label="期望达成">{{ healthProfile.targetDate || '-' }}</el-descriptions-item>
            </el-descriptions>
            <div v-if="healthProfile.goalNotes" class="goal-notes">
              <strong>备注：</strong>{{ healthProfile.goalNotes }}
            </div>
          </template>
          <el-empty v-else description="该学员暂未设置训练目标" :image-size="40" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getMyCourses, type Course } from '../../api/course';
import { getCourseStudents } from '../../api/enrollment';
import { getHealthProfile, type HealthProfile } from '../../api/health';
import { getProfile, type UserProfile } from '../../api/auth';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();
const coachId = userStore.user?.id;

const loading = ref(false);
const myCourses = ref<Course[]>([]);
const selectedCourseId = ref<number | null>(null);
const keyword = ref('');
const pageNum = ref(1);
const total = ref(0);
const students = ref<any[]>([]);

// Detail dialog
const detailVisible = ref(false);
const detailLoading = ref(false);
const currentStudent = ref<UserProfile | null>(null);
const healthProfile = ref<HealthProfile | null>(null);

const fitnessGoalMap: Record<string, string> = {
  MUSCLE_GAIN: '增肌', FAT_LOSS: '减脂', BODY_SHAPING: '塑形', FITNESS: '体能提升',
};

const computedBMI = computed(() => {
  if (!healthProfile.value?.height || !healthProfile.value?.weight) return '-';
  const h = healthProfile.value.height / 100;
  return (healthProfile.value.weight / (h * h)).toFixed(1);
});

const bloodPressure = computed(() => {
  if (!healthProfile.value) return '-';
  const s = healthProfile.value.bpSystolic;
  const d = healthProfile.value.bpDiastolic;
  if (!s && !d) return '-';
  return `${s || '-'}/${d || '-'} mmHg`;
});

const bloodTypeLabel = computed(() => {
  const map: Record<string, string> = { A: 'A型', B: 'B型', AB: 'AB型', O: 'O型', UNKNOWN: '未知' };
  return healthProfile.value?.bloodType ? (map[healthProfile.value.bloodType] || '-') : '-';
});

const fitnessGoalLabel = computed(() => {
  return healthProfile.value?.fitnessGoal ? (fitnessGoalMap[healthProfile.value.fitnessGoal] || '-') : '-';
});

async function fetchCourses() {
  if (!coachId) return;
  try {
    const res = await getMyCourses(coachId, 1, 100);
    myCourses.value = res.records;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取课程失败');
  }
}

async function fetchStudents() {
  if (!coachId || !selectedCourseId.value) return;
  loading.value = true;
  try {
    const res = await getCourseStudents(coachId, selectedCourseId.value, keyword.value || undefined, pageNum.value, 10);
    students.value = res.records;
    total.value = res.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取学员失败');
  } finally {
    loading.value = false;
  }
}

function handleCourseChange() {
  pageNum.value = 1;
  students.value = [];
  if (selectedCourseId.value) {
    fetchStudents();
  }
}

function handleSearch() {
  pageNum.value = 1;
  fetchStudents();
}

async function viewStudentDetail(student: any) {
  currentStudent.value = student;
  healthProfile.value = null;
  detailVisible.value = true;
  detailLoading.value = true;

  try {
    // 获取完整个人资料
    const profile = await getProfile(student.id);
    currentStudent.value = profile;
  } catch {}

  try {
    // 获取健康档案
    const hp = await getHealthProfile(student.id);
    healthProfile.value = hp;
  } catch {}

  detailLoading.value = false;
}

onMounted(() => fetchCourses());
</script>

<style scoped>
.my-students-page { padding: 0; }
.card-header { font-weight: 600; }
.toolbar { display: flex; align-items: center; margin-bottom: 16px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.detail-section { margin-bottom: 20px; }
.detail-section-title { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 10px; padding-bottom: 6px; border-bottom: 1px solid #ebeef5; }
.goal-notes { margin-top: 8px; padding: 8px 12px; background: #f5f7fa; border-radius: 4px; font-size: 13px; color: #606266; }
</style>
