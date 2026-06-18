<template>
  <div class="my-courses-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的课程</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="公共课" name="PUBLIC" />
        <el-tab-pane label="私教" name="PRIVATE" />
      </el-tabs>

      <el-table :data="enrollments" v-loading="loading" stripe>
        <el-table-column prop="courseId" label="课程ID" width="80" align="center" />
        <el-table-column label="课程名称" min-width="150">
          <template #default="{ row }">
            {{ courseNameMap[row.courseId] || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'TRIAL'" type="warning" size="small">试听中</el-tag>
            <el-tag v-else-if="row.status === 'CONFIRMED'" type="primary" size="small">已确认</el-tag>
            <el-tag v-else-if="row.status === 'PAID'" type="success" size="small">已购买</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paidAmount" label="已付金额" width="120" align="center">
          <template #default="{ row }">
            ¥ {{ (row.paidAmount ?? 0).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="报名时间" width="180" align="center" />
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === 'TRIAL' || row.status === 'CONFIRMED'" type="success" link @click="handlePay(row.courseId)">
              去付费
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getMyEnrollments, payCourse, type Enrollment } from '../../api/enrollment';
import { getCourseDetail } from '../../api/course';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();
const userId = userStore.user?.id;

const activeTab = ref('PUBLIC');
const loading = ref(false);
const pageNum = ref(1);
const total = ref(0);
const enrollments = ref<Enrollment[]>([]);
const courseNameMap = reactive<Record<number, string>>({});
const coursePriceMap = reactive<Record<number, number>>({});

async function fetchData() {
  if (!userId) return;
  loading.value = true;
  try {
    const res = await getMyEnrollments(userId, activeTab.value, pageNum.value, 10);
    enrollments.value = res.records;
    total.value = res.total;
    for (const e of res.records) {
      if (!courseNameMap[e.courseId]) {
        try {
          const course = await getCourseDetail(e.courseId);
          courseNameMap[e.courseId] = course.name;
          coursePriceMap[e.courseId] = course.price;
        } catch { /* ignore */ }
      }
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取数据失败');
  } finally {
    loading.value = false;
  }
}

function handleTabChange() {
  pageNum.value = 1;
  fetchData();
}

async function handlePay(courseId: number) {
  if (!userId) return;
  const courseName = courseNameMap[courseId] || '未知课程';
  const coursePrice = coursePriceMap[courseId] ?? 0;
  try {
    await ElMessageBox.confirm(
      `确认购买课程「${courseName}」？价格：¥${coursePrice.toFixed(2)}`,
      '付费确认',
      { confirmButtonText: '确认支付', cancelButtonText: '取消', type: 'warning' }
    );
  } catch {
    return; // 用户点击取消
  }
  try {
    await payCourse(userId, courseId);
    ElMessage.success('付费成功');
    fetchData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '付费失败');
  }
}

onMounted(() => fetchData());
</script>

<style scoped>
.my-courses-page { padding: 0; }
.card-header { font-weight: 600; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
