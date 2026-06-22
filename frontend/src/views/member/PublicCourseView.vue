<template>
  <div class="course-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>公共课</span>
        </div>
      </template>

      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索课程名称" clearable style="width: 280px"
          @clear="handleSearch" @keyup.enter="handleSearch">
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>

      <el-row :gutter="16">
        <el-col :span="6" v-for="course in courses" :key="course.id" style="margin-bottom: 16px">
          <el-card shadow="hover" class="course-card" @click="openDetail(course)">
            <div class="course-cover">
              <img v-if="course.coverImage" :src="course.coverImage" alt="封面" />
              <div v-else class="cover-placeholder">{{ course.name.charAt(0) }}</div>
            </div>
            <div class="course-info">
              <div class="course-tags">
                <el-tag size="small" type="info">{{ categoryLabel(course.category) }}</el-tag>
                <el-tag size="small" :type="difficultyType(course.difficulty)">{{ difficultyLabel(course.difficulty) }}</el-tag>
              </div>
              <div class="course-name">{{ course.name }}</div>
              <div class="course-price">
                <span v-if="course.price > 0" class="price-paid">¥ {{ course.price.toFixed(2) }}</span>
                <span v-else class="price-free">免费</span>
                <span v-if="course.maxStudents > 0" class="course-capacity">👥 {{ course.maxStudents }}人</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" :title="currentCourse?.name" width="700px">
      <div v-if="currentCourse">
        <p v-if="currentCourse.description" class="course-desc">{{ currentCourse.description }}</p>
        <p v-else class="course-desc-empty">暂无课程介绍</p>

        <div class="detail-info-bar">
          <span>📂 {{ categoryLabel(currentCourse?.category) }}</span>
          <span>📊 {{ difficultyLabel(currentCourse?.difficulty) }}</span>
          <span v-if="currentCourse?.location">📍 {{ currentCourse.location }}</span>
          <span v-if="currentCourse?.startDate">📅 {{ currentCourse.startDate }}</span>
          <span v-if="currentCourse?.maxStudents > 0">👥 限{{ currentCourse.maxStudents }}人</span>
        </div>

        <h4>课时列表</h4>
        <el-table :data="lessons" stripe>
          <el-table-column type="index" width="60" label="#" />
          <el-table-column prop="title" label="课时名称" />
          <el-table-column label="时长" width="100" align="center">
            <template #default="{ row }">
              {{ row.duration ? row.duration + '分钟' : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="试听" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.isTrial" type="success" size="small">可试听</el-tag>
              <el-tag v-else size="small">付费</el-tag>
            </template>
          </el-table-column>
        </el-table>

        <div class="enroll-actions" style="margin-top: 20px; text-align: center">
          <template v-if="!enrollment">
            <el-button v-if="currentCourse.price > 0" type="danger" size="large" @click="handlePay">
              立即购买 ¥{{ currentCourse.price.toFixed(2) }}
            </el-button>
            <el-button v-else type="primary" size="large" @click="handleEnrollFree">
              免费报名
            </el-button>
          </template>
          <el-tag v-else-if="enrollment.status === 'TRIAL'" type="warning" size="large">试听中</el-tag>
          <el-tag v-else-if="enrollment.status === 'PAID'" type="success" size="large">已购买</el-tag>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { listCourses, type Course } from '../../api/course';
import { getLessons, type Lesson } from '../../api/lesson';
import { enroll, payCourse, getMyEnrollments, type Enrollment } from '../../api/enrollment';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();
const userId = userStore.user?.id;

const categoryOptions = [
  { label: '瑜伽', value: 'YOGA' }, { label: '拳击', value: 'BOXING' },
  { label: '游泳', value: 'SWIMMING' }, { label: '跑步', value: 'RUNNING' },
  { label: '力量', value: 'STRENGTH' }, { label: '舞蹈', value: 'DANCE' },
  { label: '其他', value: 'OTHER' },
];
const difficultyOptions = [
  { label: '初级', value: 'BEGINNER' }, { label: '中级', value: 'INTERMEDIATE' }, { label: '高级', value: 'ADVANCED' },
];
function categoryLabel(v?: string) { return categoryOptions.find(c => c.value === v)?.label || '其他'; }
function difficultyLabel(v?: string) { return difficultyOptions.find(d => d.value === v)?.label || '初级'; }
function difficultyType(v?: string) {
  if (v === 'BEGINNER') return 'success';
  if (v === 'INTERMEDIATE') return 'warning';
  if (v === 'ADVANCED') return 'danger';
  return 'info';
}

const keyword = ref('');
const pageNum = ref(1);
const pageSize = ref(12);
const total = ref(0);
const courses = ref<Course[]>([]);

const detailVisible = ref(false);
const currentCourse = ref<Course | null>(null);
const lessons = ref<Lesson[]>([]);
const enrollment = ref<Enrollment | null>(null);

async function fetchData() {
  try {
    const res = await listCourses('PUBLIC', keyword.value || undefined, pageNum.value, pageSize.value);
    courses.value = res.records;
    total.value = res.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取课程失败');
  }
}

function handleSearch() {
  pageNum.value = 1;
  fetchData();
}

async function openDetail(course: Course) {
  currentCourse.value = course;
  detailVisible.value = true;
  lessons.value = await getLessons(course.id);
  if (userId) {
    const res = await getMyEnrollments(userId, 'PUBLIC', 1, 100);
    enrollment.value = res.records.find(e => e.courseId === course.id) || null;
  }
}

async function handleEnrollFree() {
  if (!userId || !currentCourse.value) return;
  try {
    await enroll(userId, currentCourse.value.id);
    ElMessage.success('报名成功');
    await openDetail(currentCourse.value);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '报名失败');
  }
}

async function handlePay() {
  if (!userId || !currentCourse.value) return;
  try {
    await payCourse(userId, currentCourse.value.id);
    ElMessage.success('购买成功，课程已解锁');
    await openDetail(currentCourse.value);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '购买失败');
  }
}

onMounted(() => fetchData());
</script>

<style scoped>
.course-page { padding: 0; }
.card-header { font-weight: 600; }
.toolbar { margin-bottom: 16px; }
.course-card { cursor: pointer; }
.course-cover { height: 120px; overflow: hidden; border-radius: 4px; margin-bottom: 8px; }
.course-cover img { width: 100%; height: 100%; object-fit: cover; }
.cover-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #409eff; color: #fff; font-size: 32px; }
.course-tags { display: flex; gap: 4px; margin-bottom: 4px; }
.course-name { font-weight: 600; margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.price-paid { color: #f56c6c; font-weight: bold; }
.price-free { color: #67c23a; font-weight: bold; }
.course-capacity { font-size: 12px; color: #909399; margin-left: 8px; }
.course-desc { color: #606266; margin-bottom: 16px; }
.course-desc-empty { color: #c0c4cc; margin-bottom: 16px; }
.detail-info-bar { display: flex; gap: 16px; padding: 8px 12px; background: #f5f7fa; border-radius: 4px; margin: 12px 0; font-size: 13px; color: #606266; flex-wrap: wrap; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.enroll-actions { padding: 10px 0; }
</style>
