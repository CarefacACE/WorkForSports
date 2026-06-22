<template>
  <div class="file-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>文件管理</span>
          <div class="header-actions">
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :on-change="handleFileChange"
              :show-file-list="false"
            >
              <el-button type="primary">
                <el-icon><Upload /></el-icon>
                上传文件
              </el-button>
            </el-upload>
          </div>
        </div>
      </template>

      <el-table :data="fileList" v-loading="loading" stripe>
        <el-table-column prop="originalName" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column label="文件大小" width="120" align="center">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="文件类型" width="150" align="center" show-overflow-tooltip />
        <el-table-column prop="uploadUsername" label="上传人" width="120" align="center" />
        <el-table-column label="上传时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDownload(row)">
              <el-icon><Download /></el-icon>
              下载
            </el-button>
            <el-button type="danger" link @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Upload, Download, Delete } from '@element-plus/icons-vue';
import { uploadFile, getFileList, deleteFile, getDownloadUrl, type FileInfo } from '../api/file';
import { useUserStore } from '../stores/user';

const userStore = useUserStore();
const loading = ref(false);
const fileList = ref<FileInfo[]>([]);

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatTime(time: string): string {
  if (!time) return '';
  return time.replace('T', ' ').substring(0, 19);
}

function getUserId(): number | undefined {
  return userStore.user?.role === 'MEMBER' ? userStore.user.id : undefined;
}

async function loadFileList() {
  loading.value = true;
  try {
    fileList.value = await getFileList(getUserId());
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取文件列表失败');
  } finally {
    loading.value = false;
  }
}

async function handleFileChange(file: any) {
  if (!userStore.user) {
    ElMessage.warning('请先登录');
    return;
  }

  const maxSize = 50 * 1024 * 1024;
  if (file.raw.size > maxSize) {
    ElMessage.warning('文件大小不能超过50MB');
    return;
  }

  loading.value = true;
  try {
    await uploadFile(file.raw, userStore.user.id, userStore.user.username);
    ElMessage.success('上传成功');
    await loadFileList();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '上传失败');
  } finally {
    loading.value = false;
  }
}

function handleDownload(file: FileInfo) {
  const url = getDownloadUrl(file.id);
  const link = document.createElement('a');
  link.href = url;
  link.download = file.originalName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

async function handleDelete(file: FileInfo) {
  try {
    await ElMessageBox.confirm('确定要删除该文件吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });

    loading.value = true;
    await deleteFile(file.id);
    ElMessage.success('删除成功');
    await loadFileList();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败');
    }
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadFileList();
});
</script>

<style scoped>
.file-page {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 12px;
}


</style>
