<template>
  <div class="course-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>私教课</span>
          <el-button type="primary" @click="createDialogVisible = true">创建私教课</el-button>
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
        <el-table-column label="分类" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="difficultyType(row.difficulty)" size="small">{{ difficultyLabel(row.difficulty) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="90" align="center">
          <template #default="{ row }">
            <span class="price-text">¥{{ row.price.toFixed(0) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.coachId === coachId" type="primary" link @click="openEditDialog(row)">编辑</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="pageNum" :total="total" layout="total, prev, pager, next"
          @current-change="fetchData" />
      </div>
    </el-card>

    <!-- 创建私教课对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建私教课" width="560px" top="5vh">
      <el-form :model="createForm" label-position="top">
        <div class="form-section-title">基本信息</div>
        <el-row :gutter="16">
          <el-col :span="16">
            <el-form-item label="课程名称" required>
              <el-input v-model="createForm.name" placeholder="请输入课程名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="价格">
              <el-input-number v-model="createForm.price" :min="0" :precision="2" :step="50" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="运动分类">
              <el-select v-model="createForm.category" placeholder="请选择" style="width:100%">
                <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="难度等级">
              <el-select v-model="createForm.difficulty" placeholder="请选择" style="width:100%">
                <el-option v-for="d in difficultyOptions" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="课程介绍">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="请输入课程介绍" />
        </el-form-item>
        <el-form-item label="封面图片">
          <div class="cover-upload-area">
            <div v-if="createForm.coverImage" class="cover-preview">
              <img :src="createForm.coverImage" alt="封面预览" />
              <div class="cover-preview-mask">
                <el-button type="danger" link @click="createForm.coverImage = ''">移除</el-button>
              </div>
            </div>
            <el-upload v-else class="cover-uploader" drag :show-file-list="false"
              accept="image/*" :http-request="(opt: any) => handleCoverUpload(opt, 'create')">
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <div class="el-upload__text">将图片拖到此处，或<em>点击上传</em></div>
            </el-upload>
          </div>
        </el-form-item>

        <div class="form-section-title">课程设置</div>
        <el-form-item label="标签">
          <div class="tag-input-area">
            <el-tag v-for="tag in createTags" :key="tag" closable @close="removeCreateTag(tag)" style="margin-right:6px;margin-bottom:4px;">{{ tag }}</el-tag>
            <el-input v-if="createTagInputVisible" ref="createTagInputRef" v-model="createTagInput" size="small"
              style="width:100px" @keyup.enter="addCreateTag" @blur="addCreateTag" />
            <el-button v-else size="small" @click="showCreateTagInput">+ 添加标签</el-button>
          </div>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="最大学员数">
              <el-input-number v-model="createForm.maxStudents" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="上课地点">
              <el-input v-model="createForm.location" placeholder="如：A区操房" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="开课日期">
          <el-date-picker v-model="createForm.startDate" type="date" value-format="YYYY-MM-DD"
            placeholder="请选择开课日期" style="width:100%" />
        </el-form-item>

        <div class="form-section-title">排课设置</div>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="总课时数">
              <el-input-number v-model="createForm.totalLessons" :min="0" :max="200" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="上课频率">
              <el-select v-model="createForm.frequency" style="width:100%">
                <el-option v-for="f in frequencyOptions" :key="f.value" :label="f.label" :value="f.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="上课时间段">
              <el-select v-model="createForm.defaultTimeSlot" style="width:100%">
                <el-option v-for="t in timeSlotOptions" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="排课方式">
          <el-radio-group v-model="createForm.scheduleMode">
            <el-radio value="MANUAL">手动排课</el-radio>
            <el-radio value="AUTO">自动排课</el-radio>
          </el-radio-group>
          <div v-if="createForm.scheduleMode === 'AUTO'" class="schedule-hint">
            创建课程后系统将自动生成 {{ createForm.totalLessons }} 节课的课表
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleCreate">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑私教课对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑私教课" width="560px" top="5vh">
      <el-form :model="editForm" label-position="top">
        <div class="form-section-title">基本信息</div>
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="课程名称">
              <el-input v-model="editForm.name" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="运动分类">
              <el-select v-model="editForm.category" placeholder="请选择" style="width:100%">
                <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="难度等级">
              <el-select v-model="editForm.difficulty" placeholder="请选择" style="width:100%">
                <el-option v-for="d in difficultyOptions" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="课程介绍">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="封面图片">
          <div class="cover-upload-area">
            <div v-if="editForm.coverImage" class="cover-preview">
              <img :src="editForm.coverImage" alt="封面预览" />
              <div class="cover-preview-mask">
                <el-button type="danger" link @click="editForm.coverImage = ''">移除</el-button>
              </div>
            </div>
            <el-upload v-else class="cover-uploader" drag :show-file-list="false"
              accept="image/*" :http-request="(opt: any) => handleCoverUpload(opt, 'edit')">
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <div class="el-upload__text">将图片拖到此处，或<em>点击上传</em></div>
            </el-upload>
          </div>
        </el-form-item>

        <div class="form-section-title">课程设置</div>
        <el-form-item label="标签">
          <div class="tag-input-area">
            <el-tag v-for="tag in editTags" :key="tag" closable @close="removeEditTag(tag)" style="margin-right:6px;margin-bottom:4px;">{{ tag }}</el-tag>
            <el-input v-if="editTagInputVisible" ref="editTagInputRef" v-model="editTagInput" size="small"
              style="width:100px" @keyup.enter="addEditTag" @blur="addEditTag" />
            <el-button v-else size="small" @click="showEditTagInput">+ 添加标签</el-button>
          </div>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="最大学员数">
              <el-input-number v-model="editForm.maxStudents" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="上课地点">
              <el-input v-model="editForm.location" placeholder="如：A区操房" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="开课日期">
          <el-date-picker v-model="editForm.startDate" type="date" value-format="YYYY-MM-DD"
            placeholder="请选择开课日期" style="width:100%" />
        </el-form-item>

        <div class="form-section-title">排课设置</div>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="总课时数">
              <el-input-number v-model="editForm.totalLessons" :min="0" :max="200" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="上课频率">
              <el-select v-model="editForm.frequency" style="width:100%">
                <el-option v-for="f in frequencyOptions" :key="f.value" :label="f.label" :value="f.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="上课时间段">
              <el-select v-model="editForm.defaultTimeSlot" style="width:100%">
                <el-option v-for="t in timeSlotOptions" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="排课方式">
          <el-radio-group v-model="editForm.scheduleMode">
            <el-radio value="MANUAL">手动排课</el-radio>
            <el-radio value="AUTO">自动排课</el-radio>
          </el-radio-group>
          <div v-if="editForm.scheduleMode === 'AUTO'" class="schedule-hint">
            创建课程后系统将自动生成 {{ editForm.totalLessons }} 节课的课表
          </div>
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
import { ref, reactive, nextTick, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { UploadFilled } from '@element-plus/icons-vue';
import type { UploadRequestOptions } from 'element-plus';
import { listCourses, createCourse, updateCourse, type Course } from '../../api/course';
import { uploadFile, getDownloadUrl } from '../../api/file';
import { autoSchedule } from '../../api/schedule';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();
const coachId = userStore.user?.id;

const categoryOptions = [
  { label: '瑜伽', value: 'YOGA' },
  { label: '拳击', value: 'BOXING' },
  { label: '游泳', value: 'SWIMMING' },
  { label: '跑步', value: 'RUNNING' },
  { label: '力量', value: 'STRENGTH' },
  { label: '舞蹈', value: 'DANCE' },
  { label: '其他', value: 'OTHER' },
];
const difficultyOptions = [
  { label: '初级', value: 'BEGINNER' },
  { label: '中级', value: 'INTERMEDIATE' },
  { label: '高级', value: 'ADVANCED' },
];

const frequencyOptions = [
  { label: '每天', value: 'DAILY' },
  { label: '每周1次', value: 'WEEKLY_1' },
  { label: '每周2次', value: 'WEEKLY_2' },
  { label: '每周3次', value: 'WEEKLY_3' },
  { label: '每两周1次', value: 'BIWEEKLY' },
];

const timeSlotOptions = [
  '08:00','09:00','10:00','11:00','12:00','13:00',
  '14:00','15:00','16:00','17:00','18:00','19:00','20:00','21:00',
];

function categoryLabel(v: string) {
  return categoryOptions.find(c => c.value === v)?.label || '其他';
}
function difficultyLabel(v: string) {
  return difficultyOptions.find(d => d.value === v)?.label || '初级';
}
function difficultyType(v: string) {
  if (v === 'BEGINNER') return '';
  if (v === 'INTERMEDIATE') return 'warning';
  if (v === 'ADVANCED') return 'danger';
  return 'info';
}

const keyword = ref('');
const pageNum = ref(1);
const total = ref(0);
const loading = ref(false);
const courses = ref<Course[]>([]);

// ---- Create form ----
const createDialogVisible = ref(false);
const submitLoading = ref(false);
const createForm = reactive({
  name: '', description: '', price: 0, coverImage: '',
  category: 'OTHER', difficulty: 'BEGINNER', maxStudents: 0,
  location: '', startDate: '', tags: '',
  totalLessons: 0,
  frequency: 'WEEKLY_2',
  scheduleMode: 'MANUAL',
  defaultTimeSlot: '10:00',
});
const createTags = ref<string[]>([]);
const createTagInput = ref('');
const createTagInputVisible = ref(false);
const createTagInputRef = ref();

function showCreateTagInput() {
  createTagInputVisible.value = true;
  nextTick(() => createTagInputRef.value?.input?.focus());
}
function addCreateTag() {
  const v = createTagInput.value.trim();
  if (v && !createTags.value.includes(v)) createTags.value.push(v);
  createTagInput.value = '';
  createTagInputVisible.value = false;
}
function removeCreateTag(tag: string) {
  createTags.value = createTags.value.filter(t => t !== tag);
}

// ---- Edit form ----
const editDialogVisible = ref(false);
const editForm = reactive({
  id: 0, name: '', description: '', coverImage: '',
  category: 'OTHER', difficulty: 'BEGINNER', maxStudents: 0,
  location: '', startDate: '', tags: '',
  totalLessons: 0,
  frequency: 'WEEKLY_2',
  scheduleMode: 'MANUAL',
  defaultTimeSlot: '10:00',
});
const editTags = ref<string[]>([]);
const editTagInput = ref('');
const editTagInputVisible = ref(false);
const editTagInputRef = ref();

function showEditTagInput() {
  editTagInputVisible.value = true;
  nextTick(() => editTagInputRef.value?.input?.focus());
}
function addEditTag() {
  const v = editTagInput.value.trim();
  if (v && !editTags.value.includes(v)) editTags.value.push(v);
  editTagInput.value = '';
  editTagInputVisible.value = false;
}
function removeEditTag(tag: string) {
  editTags.value = editTags.value.filter(t => t !== tag);
}

// ---- Data ----
async function fetchData() {
  loading.value = true;
  try {
    const res = await listCourses('PRIVATE', keyword.value || undefined, pageNum.value, 10);
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

function resetCreateForm() {
  createForm.name = '';
  createForm.description = '';
  createForm.price = 0;
  createForm.coverImage = '';
  createForm.category = 'OTHER';
  createForm.difficulty = 'BEGINNER';
  createForm.maxStudents = 0;
  createForm.location = '';
  createForm.startDate = '';
  createForm.totalLessons = 0;
  createForm.frequency = 'WEEKLY_2';
  createForm.scheduleMode = 'MANUAL';
  createForm.defaultTimeSlot = '10:00';
  createTags.value = [];
}

async function handleCreate() {
  if (!coachId || !createForm.name || !createForm.price) {
    ElMessage.warning('请填写完整信息');
    return;
  }
  submitLoading.value = true;
  try {
    const result = await createCourse(coachId, {
      name: createForm.name,
      description: createForm.description,
      type: 'PRIVATE',
      price: createForm.price,
      coverImage: createForm.coverImage,
      category: createForm.category,
      difficulty: createForm.difficulty,
      maxStudents: createForm.maxStudents,
      location: createForm.location,
      startDate: createForm.startDate || undefined,
      tags: createTags.value.join(','),
      totalLessons: createForm.totalLessons,
      frequency: createForm.frequency,
      scheduleMode: createForm.scheduleMode,
      defaultTimeSlot: createForm.defaultTimeSlot,
    });
    ElMessage.success('私教课创建成功');
    // 自动排课
    if (createForm.scheduleMode === 'AUTO' && createForm.totalLessons > 0) {
      try {
        await autoSchedule(coachId, result.id);
        ElMessage.success(`已自动生成 ${createForm.totalLessons} 节课`);
      } catch (e) {
        ElMessage.warning('自动排课失败，请手动排课');
      }
    }
    createDialogVisible.value = false;
    resetCreateForm();
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
  editForm.category = course.category || 'OTHER';
  editForm.difficulty = course.difficulty || 'BEGINNER';
  editForm.maxStudents = course.maxStudents || 0;
  editForm.location = course.location || '';
  editForm.startDate = course.startDate || '';
  editForm.totalLessons = course.totalLessons || 0;
  editForm.frequency = course.frequency || 'WEEKLY_2';
  editForm.scheduleMode = course.scheduleMode || 'MANUAL';
  editForm.defaultTimeSlot = course.defaultTimeSlot || '10:00';
  editTags.value = course.tags ? course.tags.split(',').filter(Boolean) : [];
  editDialogVisible.value = true;
}

async function handleEdit() {
  if (!coachId) return;
  submitLoading.value = true;
  try {
    await updateCourse(coachId, {
      id: editForm.id,
      name: editForm.name,
      description: editForm.description,
      coverImage: editForm.coverImage,
      category: editForm.category,
      difficulty: editForm.difficulty,
      maxStudents: editForm.maxStudents,
      location: editForm.location,
      startDate: editForm.startDate || undefined,
      tags: editTags.value.join(','),
      totalLessons: editForm.totalLessons,
      frequency: editForm.frequency,
      scheduleMode: editForm.scheduleMode,
      defaultTimeSlot: editForm.defaultTimeSlot,
    });
    ElMessage.success('更新成功');
    editDialogVisible.value = false;
    fetchData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新失败');
  } finally {
    submitLoading.value = false;
  }
}

async function handleCoverUpload(options: UploadRequestOptions, target: 'create' | 'edit') {
  if (!coachId || !userStore.user) return;
  try {
    const result = await uploadFile(options.file, coachId, userStore.user.username);
    const url = getDownloadUrl(result.id);
    if (target === 'create') {
      createForm.coverImage = url;
    } else {
      editForm.coverImage = url;
    }
    ElMessage.success('封面上传成功');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '封面上传失败');
  }
}

onMounted(() => fetchData());
</script>

<style scoped>
.course-page { padding: 0; }
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }
.toolbar { margin-bottom: 16px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.price-text { color: #f56c6c; font-weight: bold; }
.form-section-title { font-size: 14px; font-weight: 600; color: #303133; margin: 8px 0 12px; padding-bottom: 8px; border-bottom: 1px solid #ebeef5; }
.tag-input-area { display: flex; flex-wrap: wrap; align-items: center; }
.cover-upload-area { width: 100%; }
.cover-uploader :deep(.el-upload-dragger) { width: 100%; height: 120px; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.cover-preview { position: relative; width: 100%; height: 120px; border-radius: 6px; overflow: hidden; border: 1px solid #dcdfe6; }
.cover-preview img { width: 100%; height: 100%; object-fit: cover; }
.cover-preview-mask { position: absolute; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity 0.2s; }
.cover-preview:hover .cover-preview-mask { opacity: 1; }
.cover-preview-mask .el-button { color: #fff; font-size: 14px; }
.schedule-hint { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
