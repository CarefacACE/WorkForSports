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
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchStudents"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getMyCourses, type Course } from '../../api/course';
import { getCourseStudents } from '../../api/enrollment';
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

onMounted(() => fetchCourses());
</script>

<style scoped>
.my-students-page { padding: 0; }
.card-header { font-weight: 600; }
.toolbar { display: flex; align-items: center; margin-bottom: 16px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
