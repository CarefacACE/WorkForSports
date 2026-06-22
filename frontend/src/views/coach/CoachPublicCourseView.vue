<template>
  <div class="course-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>公共课</span>
          <el-button type="primary" @click="createDialogVisible = true">创建课程</el-button>
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

      <el-table :data="courses" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="name" label="课程名称" min-width="150" />
        <el-table-column label="教练" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.coachId === coachId" type="success" size="small">我的</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.price > 0">¥ {{ row.price.toFixed(2) }}</span>
            <span v-else class="free-text">免费</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.coachId === coachId" type="primary" link @click="openEditDialog(row)">编辑</el-button>
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

    <el-dialog v-model="createDialogVisible" title="创建公共课" width="500px">
      <el-form :model="createForm" label-position="top">
        <el-form-item label="课程名称" required>
          <el-input v-model="createForm.name" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="课程介绍">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="请输入课程介绍" />
        </el-form-item>
        <el-form-item label="价格（0表示免费）">
          <el-input-number v-model="createForm.price" :min="0" :precision="2" :step="50" style="width: 100%" />
        </el-form-item>
        <el-form-item label="封面图片URL">
          <el-input v-model="createForm.coverImage" placeholder="请输入封面图片URL" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleCreate">确认创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="编辑课程" width="500px">
      <el-form :model="editForm" label-position="top">
        <el-form-item label="课程名称">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="课程介绍">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="封面图片URL">
          <el-input v-model="editForm.coverImage" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { listCourses, createCourse, updateCourse, type Course } from '../../api/course';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();
const coachId = userStore.user?.id;

const keyword = ref('');
const pageNum = ref(1);
const total = ref(0);
const loading = ref(false);
const courses = ref<Course[]>([]);

const createDialogVisible = ref(false);
const submitLoading = ref(false);
const createForm = reactive({
  name: '',
  description: '',
  price: 0,
  coverImage: '',
});

const editDialogVisible = ref(false);
const editForm = reactive({ id: 0, name: '', description: '', coverImage: '' });

async function fetchData() {
  loading.value = true;
  try {
    const res = await listCourses('PUBLIC', keyword.value || undefined, pageNum.value, 10);
    courses.value = res.records;
    total.value = res.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取课程失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pageNum.value = 1;
  fetchData();
}

async function handleCreate() {
  if (!coachId || !createForm.name) {
    ElMessage.warning('请填写课程名称');
    return;
  }
  submitLoading.value = true;
  try {
    await createCourse(coachId, {
      name: createForm.name,
      description: createForm.description,
      type: 'PUBLIC',
      price: createForm.price,
      coverImage: createForm.coverImage,
    });
    ElMessage.success('课程创建成功');
    createDialogVisible.value = false;
    createForm.name = '';
    createForm.description = '';
    createForm.price = 0;
    createForm.coverImage = '';
    fetchData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败');
  } finally {
    submitLoading.value = false;
  }
}

function openEditDialog(course: Course) {
  editForm.id = course.id;
  editForm.name = course.name;
  editForm.description = course.description || '';
  editForm.coverImage = course.coverImage || '';
  editDialogVisible.value = true;
}

async function handleEdit() {
  if (!coachId) return;
  submitLoading.value = true;
  try {
    await updateCourse(coachId, editForm);
    ElMessage.success('更新成功');
    editDialogVisible.value = false;
    fetchData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新失败');
  } finally {
    submitLoading.value = false;
  }
}

onMounted(() => fetchData());
</script>

<style scoped>
.course-page { padding: 0; }
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }
.toolbar { margin-bottom: 16px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.free-text { color: #67c23a; }
</style>
