<template>
  <div class="my-courses-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的课程</span>
        </div>
      </template>

      <el-table :data="courses" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="name" label="课程名称" min-width="150" />
        <el-table-column prop="type" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 'PUBLIC' ? '' : 'warning'" size="small">
              {{ row.type === 'PUBLIC' ? '公共课' : '私教' }}
            </el-tag>
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
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
            <el-button type="success" link @click="openLessonDialog(row)">课时管理</el-button>
            <el-button type="warning" link @click="openPriceDialog(row)">改价</el-button>
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

    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>报名记录</span>
        </div>
      </template>

      <div class="toolbar">
        <el-input v-model="enrollKeyword" placeholder="搜索学员姓名/用户名/手机号" clearable style="width: 300px"
          @clear="fetchEnrollments" @keyup.enter="fetchEnrollments">
          <template #append>
            <el-button @click="fetchEnrollments">搜索</el-button>
          </template>
        </el-input>
      </div>

      <el-table :data="enrollments" v-loading="enrollLoading" stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="学员" min-width="120">
          <template #default="{ row }">
            {{ studentNameMap[row.userId] || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="课程" min-width="120">
          <template #default="{ row }">
            {{ courseNameMap[row.courseId] || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="paidAmount" label="支付金额" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.paidAmount > 0" class="price-text">¥ {{ row.paidAmount.toFixed(2) }}</span>
            <span v-else class="free-text">免费</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'TRIAL'" type="warning" size="small">试听中</el-tag>
            <el-tag v-else-if="row.status === 'CONFIRMED'" type="primary" size="small">已确认</el-tag>
            <el-tag v-else-if="row.status === 'PAID'" type="success" size="small">已购买</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="报名时间" width="170" align="center" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="enrollPageNum"
          :total="enrollTotal"
          layout="total, prev, pager, next"
          @current-change="fetchEnrollments"
        />
      </div>
    </el-card>

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

    <el-dialog v-model="lessonDialogVisible" title="课时管理" width="700px">
      <div style="margin-bottom: 16px">
        <el-button type="primary" size="small" @click="openAddLessonDialog">添加课时</el-button>
      </div>
      <el-table :data="lessons" stripe>
        <el-table-column prop="sortOrder" label="序号" width="60" align="center" />
        <el-table-column prop="title" label="课时名称" min-width="150" />
        <el-table-column prop="videoUrl" label="视频链接" min-width="200" show-overflow-tooltip />
        <el-table-column label="试听" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isTrial ? 'success' : 'info'" size="small">
              {{ row.isTrial ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditLessonDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除该课时？" @confirm="handleDeleteLesson(row.id)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="addLessonDialogVisible" :title="isEditLesson ? '编辑课时' : '添加课时'" width="480px">
      <el-form :model="lessonForm" label-position="top">
        <el-form-item label="课时名称" required>
          <el-input v-model="lessonForm.title" placeholder="请输入课时名称" />
        </el-form-item>
        <el-form-item label="视频链接">
          <el-input v-model="lessonForm.videoUrl" placeholder="请输入视频URL" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="lessonForm.sortOrder" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="是否可试听">
          <el-switch v-model="lessonForm.isTrial" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addLessonDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleLessonSubmit">{{ isEditLesson ? '保存' : '添加' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="priceDialogVisible" title="修改价格" width="400px">
      <el-form label-position="top">
        <el-form-item label="当前价格">
          <el-input :model-value="'¥ ' + (priceForm.oldPrice ?? 0).toFixed(2)" disabled />
        </el-form-item>
        <el-form-item label="新价格">
          <el-input-number v-model="priceForm.newPrice" :min="0" :precision="2" :step="50" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="priceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handlePriceChange">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getMyCourses, updateCourse, updateCoursePrice, type Course } from '../../api/course';
import { getLessons, addLesson, updateLesson, deleteLesson, type Lesson } from '../../api/lesson';
import { getCoachEnrollments, type Enrollment } from '../../api/enrollment';
import { getProfile, type UserProfile } from '../../api/auth';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();
const coachId = userStore.user?.id;

const loading = ref(false);
const submitLoading = ref(false);
const pageNum = ref(1);
const total = ref(0);
const courses = ref<Course[]>([]);

const editDialogVisible = ref(false);
const editForm = reactive({ id: 0, name: '', description: '', coverImage: '' });

const lessonDialogVisible = ref(false);
const addLessonDialogVisible = ref(false);
const isEditLesson = ref(false);
const editingLessonId = ref(0);
const currentCourseId = ref(0);
const lessons = ref<Lesson[]>([]);
const lessonForm = reactive({ title: '', videoUrl: '', sortOrder: 0, isTrial: 0 });

const priceDialogVisible = ref(false);
const priceForm = reactive({ courseId: 0, oldPrice: 0, newPrice: 0 });

const enrollLoading = ref(false);
const enrollKeyword = ref('');
const enrollPageNum = ref(1);
const enrollTotal = ref(0);
const enrollments = ref<Enrollment[]>([]);
const courseNameMap = reactive<Record<number, string>>({});
const studentNameMap = reactive<Record<number, string>>({});

async function fetchData() {
  if (!coachId) return;
  loading.value = true;
  try {
    const res = await getMyCourses(coachId, pageNum.value, 10);
    courses.value = res.records;
    total.value = res.total;
    for (const c of res.records) {
      courseNameMap[c.id] = c.name;
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取课程失败');
  } finally {
    loading.value = false;
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

async function openLessonDialog(course: Course) {
  currentCourseId.value = course.id;
  lessons.value = await getLessons(course.id);
  lessonDialogVisible.value = true;
}

function openAddLessonDialog() {
  isEditLesson.value = false;
  editingLessonId.value = 0;
  lessonForm.title = '';
  lessonForm.videoUrl = '';
  lessonForm.sortOrder = 0;
  lessonForm.isTrial = 0;
  addLessonDialogVisible.value = true;
}

function openEditLessonDialog(lesson: Lesson) {
  isEditLesson.value = true;
  editingLessonId.value = lesson.id;
  lessonForm.title = lesson.title;
  lessonForm.videoUrl = lesson.videoUrl || '';
  lessonForm.sortOrder = lesson.sortOrder;
  lessonForm.isTrial = lesson.isTrial;
  addLessonDialogVisible.value = true;
}

async function handleLessonSubmit() {
  if (!coachId || !lessonForm.title) {
    ElMessage.warning('请填写课时名称');
    return;
  }
  submitLoading.value = true;
  try {
    if (isEditLesson.value) {
      await updateLesson(coachId, editingLessonId.value, lessonForm.title, lessonForm.videoUrl, lessonForm.sortOrder, lessonForm.isTrial);
      ElMessage.success('课时更新成功');
    } else {
      await addLesson(coachId, {
        courseId: currentCourseId.value,
        title: lessonForm.title,
        videoUrl: lessonForm.videoUrl,
        sortOrder: lessonForm.sortOrder,
        isTrial: lessonForm.isTrial,
      });
      ElMessage.success('课时添加成功');
    }
    addLessonDialogVisible.value = false;
    lessons.value = await getLessons(currentCourseId.value);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : (isEditLesson.value ? '更新失败' : '添加失败'));
  } finally {
    submitLoading.value = false;
  }
}

async function handleDeleteLesson(lessonId: number) {
  if (!coachId) return;
  try {
    await deleteLesson(coachId, lessonId);
    ElMessage.success('删除成功');
    lessons.value = await getLessons(currentCourseId.value);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败');
  }
}

function openPriceDialog(course: Course) {
  priceForm.courseId = course.id;
  priceForm.oldPrice = course.price;
  priceForm.newPrice = course.price;
  priceDialogVisible.value = true;
}

async function handlePriceChange() {
  if (!coachId) return;
  submitLoading.value = true;
  try {
    await updateCoursePrice(coachId, priceForm.courseId, priceForm.newPrice);
    ElMessage.success('价格修改成功');
    priceDialogVisible.value = false;
    fetchData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '修改失败');
  } finally {
    submitLoading.value = false;
  }
}

async function fetchEnrollments() {
  if (!coachId) return;
  enrollLoading.value = true;
  try {
    const res = await getCoachEnrollments(coachId, enrollKeyword.value || undefined, enrollPageNum.value, 10);
    enrollments.value = res.records;
    enrollTotal.value = res.total;
    for (const e of res.records) {
      if (!studentNameMap[e.userId]) {
        try {
          const profile = await getProfile(e.userId);
          studentNameMap[e.userId] = profile.realName || profile.username;
        } catch { studentNameMap[e.userId] = 'ID:' + e.userId; }
      }
      if (!courseNameMap[e.courseId]) {
        const c = courses.value.find(c => c.id === e.courseId);
        if (c) courseNameMap[e.courseId] = c.name;
      }
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取报名记录失败');
  } finally {
    enrollLoading.value = false;
  }
}

onMounted(() => {
  fetchData();
  fetchEnrollments();
});
</script>

<style scoped>
.my-courses-page { padding: 0; }
.card-header { font-weight: 600; }
.toolbar { margin-bottom: 16px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.free-text { color: #67c23a; }
.price-text { color: #f56c6c; font-weight: bold; }
</style>
