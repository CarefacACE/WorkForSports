<template>
  <div class="admin-repair-page">
    <el-card shadow="never">
      <template #header>
        <span>🔧 器材保修管理</span>
      </template>

      <!-- 筛选栏 -->
      <div class="toolbar">
        <el-radio-group v-model="statusFilter" @change="fetchData">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="PENDING">待处理</el-radio-button>
          <el-radio-button value="PROCESSING">处理中</el-radio-button>
          <el-radio-button value="RESOLVED">已解决</el-radio-button>
          <el-radio-button value="REJECTED">已驳回</el-radio-button>
        </el-radio-group>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="username" label="报修教练" width="120" />
        <el-table-column prop="equipmentName" label="器材名称" width="140" />
        <el-table-column prop="equipmentLocation" label="位置/编号" width="120" />
        <el-table-column prop="description" label="故障描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="170" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING' || row.status === 'PROCESSING'" type="primary" link @click="openProcessDialog(row)">处理</el-button>
            <el-button type="info" link @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 处理对话框 -->
    <el-dialog v-model="processDialogVisible" title="处理报修" width="500px">
      <div v-if="currentItem" class="process-detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="报修教练">{{ currentItem.username }}</el-descriptions-item>
          <el-descriptions-item label="器材名称">{{ currentItem.equipmentName }}</el-descriptions-item>
          <el-descriptions-item label="位置/编号">{{ currentItem.equipmentLocation || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="statusTag(currentItem.status)" size="small">{{ statusLabel(currentItem.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="故障描述" :span="2">{{ currentItem.description }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <el-form label-position="top" style="margin-top: 16px;">
        <el-form-item label="处理状态" required>
          <el-radio-group v-model="processForm.status">
            <el-radio value="PROCESSING">处理中</el-radio>
            <el-radio value="RESOLVED">已解决</el-radio>
            <el-radio value="REJECTED">已驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="反馈意见" required>
          <el-input v-model="processForm.feedback" type="textarea" :rows="4" placeholder="请输入反馈意见..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="processing" @click="submitProcess">确认处理</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="报修详情" width="500px">
      <div v-if="currentItem">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="报修教练">{{ currentItem.username }}</el-descriptions-item>
          <el-descriptions-item label="器材名称">{{ currentItem.equipmentName }}</el-descriptions-item>
          <el-descriptions-item label="位置/编号">{{ currentItem.equipmentLocation || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(currentItem.status)" size="small">{{ statusLabel(currentItem.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="故障描述" :span="2">{{ currentItem.description }}</el-descriptions-item>
          <el-descriptions-item v-if="currentItem.feedback" label="反馈意见" :span="2">{{ currentItem.feedback }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ currentItem.createTime }}</el-descriptions-item>
          <el-descriptions-item v-if="currentItem.processedTime" label="处理时间">{{ currentItem.processedTime }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { listRepairs, processRepair, type EquipmentRepair } from '../api/feedback';

const loading = ref(false);
const processing = ref(false);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableData = ref<EquipmentRepair[]>([]);
const statusFilter = ref('');

const processDialogVisible = ref(false);
const detailDialogVisible = ref(false);
const currentItem = ref<EquipmentRepair | null>(null);

const processForm = reactive({
  status: 'PROCESSING',
  feedback: '',
});

function statusTag(status: string): string {
  const map: Record<string, string> = { PENDING: 'warning', PROCESSING: '', RESOLVED: 'success', REJECTED: 'danger' };
  return map[status] || 'info';
}

function statusLabel(status: string): string {
  const map: Record<string, string> = { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已解决', REJECTED: '已驳回' };
  return map[status] || status;
}

async function fetchData() {
  loading.value = true;
  try {
    const result = await listRepairs(pageNum.value, pageSize.value, statusFilter.value || undefined);
    tableData.value = result.records || [];
    total.value = result.total || 0;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取数据失败');
  } finally {
    loading.value = false;
  }
}

function openProcessDialog(row: EquipmentRepair) {
  currentItem.value = row;
  processForm.status = 'PROCESSING';
  processForm.feedback = '';
  processDialogVisible.value = true;
}

function viewDetail(row: EquipmentRepair) {
  currentItem.value = row;
  detailDialogVisible.value = true;
}

async function submitProcess() {
  if (!currentItem.value) return;
  if (!processForm.feedback.trim()) {
    ElMessage.warning('请填写反馈意见');
    return;
  }

  processing.value = true;
  try {
    await processRepair(currentItem.value.id, processForm.status, processForm.feedback.trim());
    ElMessage.success('处理成功');
    processDialogVisible.value = false;
    fetchData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '处理失败');
  } finally {
    processing.value = false;
  }
}

// 初始加载
fetchData();
</script>

<style scoped>
.admin-repair-page {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.toolbar {
  margin-bottom: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.process-detail {
  margin-bottom: 4px;
}
</style>
