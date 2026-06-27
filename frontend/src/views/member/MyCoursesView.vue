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
        <el-tab-pane label="私教课" name="PRIVATE" />
      </el-tabs>

      <!-- ═══ 公共课列表 ═══ -->
      <el-table v-if="activeTab === 'PUBLIC'" :data="enrollments" v-loading="loading" stripe>
        <el-table-column label="课程名称" min-width="160">
          <template #default="{ row }">
            {{ courseNameMap[row.courseId] || '课程 #' + row.courseId }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'TRIAL'" type="warning" size="small">试听中</el-tag>
            <el-tag v-else-if="row.status === 'CONFIRMED'" type="primary" size="small">已确认</el-tag>
            <el-tag v-else-if="row.status === 'PAID'" type="success" size="small">已购买</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paidAmount" label="已付金额" width="100" align="center">
          <template #default="{ row }">
            ¥ {{ (row.paidAmount ?? 0).toFixed(0) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="报名时间" width="170" align="center" />
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-popconfirm title="确定退出该课程吗？" @confirm="handleQuit(row)">
              <template #reference>
                <el-button v-if="row.status === 'PAID' || row.status === 'CONFIRMED'"
                           type="danger" size="small">退出</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- ═══ 私教课列表 ═══ -->
      <el-table v-if="activeTab === 'PRIVATE'" :data="enrollments" v-loading="loading" stripe>
        <el-table-column label="教练" min-width="140">
          <template #default="{ row }">
            {{ coachNameMap[row.coachId] || '私教 #' + row.coachId }}
          </template>
        </el-table-column>
        <el-table-column label="自动扣费" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.autoDeductAgreed" type="success" size="small">已开通</el-tag>
            <el-tag v-else type="info" size="small">未开通</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PAID'" type="success" size="small">在读</el-tag>
            <el-tag v-else-if="row.status === 'CANCELLED'" type="info" size="small">已退出</el-tag>
            <el-tag v-else type="warning" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="加入时间" width="170" align="center" />
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-popconfirm title="确定退出该私教课程吗？未审批的预约将被取消。" @confirm="handleQuit(row)">
              <template #reference>
                <el-button v-if="row.status === 'PAID'" type="danger" size="small">退出</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getMyEnrollments, quitEnrollment, type Enrollment } from '../../api/enrollment';
import { getCourseDetail } from '../../api/course';
import { getCoachDetail } from '../../api/privateCoach';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();
const userId = userStore.user?.id;

const activeTab = ref('PUBLIC');
const loading = ref(false);
const enrollments = ref<Enrollment[]>([]);
const courseNameMap = reactive<Record<number, string>>({});
const coachNameMap = reactive<Record<number, string>>({});

async function fetchData() {
  if (!userId) return;
  loading.value = true;
  try {
    const tab = activeTab.value;
    const res = await getMyEnrollments(userId, tab, 1, 100);
    enrollments.value = res.records;

    if (tab === 'PUBLIC') {
      for (const e of res.records) {
        if (!courseNameMap[e.courseId]) {
          try {
            const course = await getCourseDetail(e.courseId);
            courseNameMap[e.courseId] = course.name;
          } catch { /* course may be deleted */ }
        }
      }
    } else {
      // PRIVATE: map coach names from coachId
      for (const e of res.records) {
        if (e.coachId && !coachNameMap[e.coachId]) {
          try {
            const detail = await getCoachDetail(e.coachId);
            coachNameMap[e.coachId] = detail.realName || ('私教 #' + e.coachId);
          } catch { coachNameMap[e.coachId] = '私教 #' + e.coachId; }
        }
      }
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取数据失败');
  } finally {
    loading.value = false;
  }
}

async function handleQuit(row: Enrollment) {
  try {
    await quitEnrollment(row.id);
    ElMessage.success('已退出课程');
    await fetchData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '退出失败');
  }
}

function handleTabChange() {
  fetchData();
}

onMounted(() => fetchData());
</script>

<style scoped>
.my-courses-page { display: flex; flex-direction: column; flex: 1; min-height: 0; }
.card-header { font-weight: 600; }
</style>
