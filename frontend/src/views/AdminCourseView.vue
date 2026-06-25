<template>
  <div class="admin-course-page">
    <el-card shadow="never">
      <template #header><span>📚 课程审批管理</span></template>

      <el-tabs v-model="tab" @tab-change="onTabChange">
        <el-tab-pane label="⏳ 待审批" name="PENDING">
          <el-table :data="pendingList" v-loading="loading" border stripe empty-text="暂无待审批课程">
            <el-table-column prop="name" label="课程名称" min-width="150" />
            <el-table-column prop="type" label="类型" width="90">
              <template #default="{ row }">
                <el-tag :type="row.type === 'PUBLIC' ? 'success' : 'warning'" size="small">
                  {{ row.type === 'PUBLIC' ? '公共课' : '私教课' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="教练" width="100">
              <template #default="{ row }">{{ row.coachId }}</template>
            </el-table-column>
            <el-table-column label="价格" width="90">
              <template #default="{ row }">¥{{ row.price }}</template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button type="success" size="small" @click="handleApprove(row.id)">✅ 通过</el-button>
                <el-button type="danger" size="small" @click="showRejectDialog(row)">❌ 驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="✅ 已通过" name="ACTIVE">
          <el-table :data="activeList" v-loading="loading" border stripe empty-text="暂无已通过课程">
            <el-table-column prop="name" label="课程名称" min-width="150" />
            <el-table-column prop="type" label="类型" width="90">
              <template #default="{ row }">
                <el-tag :type="row.type === 'PUBLIC' ? 'success' : 'warning'" size="small">
                  {{ row.type === 'PUBLIC' ? '公共课' : '私教课' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="coachId" label="教练ID" width="90" />
            <el-table-column label="价格" width="90">
              <template #default="{ row }">¥{{ row.price }}</template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="❌ 已驳回" name="REJECTED">
          <el-table :data="rejectedList" v-loading="loading" border stripe empty-text="暂无已驳回课程">
            <el-table-column prop="name" label="课程名称" min-width="150" />
            <el-table-column prop="type" label="类型" width="90">
              <template #default="{ row }">
                <el-tag :type="row.type === 'PUBLIC' ? 'success' : 'warning'" size="small">
                  {{ row.type === 'PUBLIC' ? '公共课' : '私教课' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="coachId" label="教练ID" width="90" />
            <el-table-column prop="createTime" label="创建时间" width="170" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 驳回弹窗 -->
    <el-dialog v-model="rejectDialogVisible" title="驳回理由（可选）" width="420px">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="如：课程名称不符规范..." />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="doReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
  getPendingCourses, approveCourse, rejectCourse, getAllCoursesForAdmin,
  type Course,
} from '../api/course';

const tab = ref('PENDING');
const loading = ref(false);
const pendingList = ref<Course[]>([]);
const activeList = ref<Course[]>([]);
const rejectedList = ref<Course[]>([]);

/* ─── 驳回弹窗 ─── */
const rejectDialogVisible = ref(false);
const rejectReason = ref('');
const rejectTargetId = ref(0);
const rejecting = ref(false);

function showRejectDialog(row: Course) {
  rejectTargetId.value = row.id;
  rejectReason.value = '';
  rejectDialogVisible.value = true;
}

async function doReject() {
  rejecting.value = true;
  try {
    await rejectCourse(rejectTargetId.value, rejectReason.value);
    ElMessage.success('已驳回');
    rejectDialogVisible.value = false;
    await fetchPending();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败');
  } finally {
    rejecting.value = false;
  }
}

async function handleApprove(id: number) {
  try {
    await approveCourse(id);
    ElMessage.success('审批通过，课程已上架');
    await fetchPending();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败');
  }
}

async function fetchPending() {
  loading.value = true;
  try {
    pendingList.value = await getPendingCourses() || [];
  } catch { /* ignore */ }
  loading.value = false;
}

async function onTabChange(name: string) {
  if (name === 'PENDING') {
    await fetchPending();
  } else if (name === 'ACTIVE') {
    loading.value = true;
    try { activeList.value = await getAllCoursesForAdmin('ACTIVE') || []; } catch { /**/ }
    loading.value = false;
  } else if (name === 'REJECTED') {
    loading.value = true;
    try { rejectedList.value = await getAllCoursesForAdmin('REJECTED') || []; } catch { /**/ }
    loading.value = false;
  }
}

onMounted(fetchPending);
</script>

<style scoped>
.admin-course-page { display: flex; flex-direction: column; }
</style>
