<template>
  <el-popover
    placement="bottom"
    :width="420"
    trigger="click"
    @show="loadMyRepairs"
  >
    <template #reference>
      <el-badge :value="myRepairCount > 0 ? myRepairCount : undefined" :max="99">
        <el-button circle size="small" title="器材报修">
          <el-icon><Tools /></el-icon>
        </el-button>
      </el-badge>
    </template>

    <div class="repair-popover">
      <div class="popover-header">
        <span>🔧 器材报修</span>
        <el-button type="primary" link size="small" @click="showForm = !showForm">
          {{ showForm ? '收起' : '新增报修' }}
        </el-button>
      </div>

      <!-- 新增报修表单 -->
      <div v-if="showForm" class="repair-form">
        <el-form label-position="top" size="small">
          <el-form-item label="器材名称" required>
            <el-input v-model="form.equipmentName" placeholder="如：跑步机、龙门架" />
          </el-form-item>
          <el-form-item label="器材位置/编号">
            <el-input v-model="form.equipmentLocation" placeholder="如：A区3号、B栋101室" />
          </el-form-item>
          <el-form-item label="故障描述" required>
            <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请详细描述器材故障情况..." />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="submitRepairForm">提交报修</el-button>
          </el-form-item>
        </el-form>
        <el-divider />
      </div>

      <!-- 我的报修列表 -->
      <div class="repair-list">
        <div v-if="myRepairs.length === 0" class="empty-hint">暂无报修记录</div>
        <div v-for="item in myRepairs" :key="item.id" class="repair-item">
          <div class="repair-item-header">
            <span class="repair-equipment">{{ item.equipmentName }}</span>
            <el-tag :type="statusTag(item.status)" size="small">{{ statusLabel(item.status) }}</el-tag>
          </div>
          <div class="repair-item-body">
            <div v-if="item.equipmentLocation" class="repair-location">📍 {{ item.equipmentLocation }}</div>
            <div class="repair-desc">{{ item.description }}</div>
          </div>
          <div v-if="item.feedback" class="repair-feedback">
            <strong>管理员回复：</strong>{{ item.feedback }}
          </div>
          <div class="repair-time">{{ item.createTime }}</div>
        </div>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { Tools } from '@element-plus/icons-vue';
import { submitRepair, getMyRepairs, type EquipmentRepair } from '../api/feedback';
import { useUserStore } from '../stores/user';

const userStore = useUserStore();
const user = userStore.user;

const showForm = ref(false);
const submitting = ref(false);
const myRepairs = ref<EquipmentRepair[]>([]);
const myRepairCount = ref(0);

const form = reactive({
  equipmentName: '',
  equipmentLocation: '',
  description: '',
});

function statusTag(status: string): string {
  const map: Record<string, string> = { PENDING: 'warning', PROCESSING: '', RESOLVED: 'success', REJECTED: 'danger' };
  return map[status] || 'info';
}

function statusLabel(status: string): string {
  const map: Record<string, string> = { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已解决', REJECTED: '已驳回' };
  return map[status] || status;
}

async function loadMyRepairs() {
  if (!user?.id) return;
  try {
    const result = await getMyRepairs(user.id, 1, 20);
    myRepairs.value = result.records || [];
    myRepairCount.value = myRepairs.value.filter(r => r.status === 'PROCESSING' || r.status === 'PENDING').length;
  } catch { /* ignore */ }
}

async function submitRepairForm() {
  if (!user?.id) {
    ElMessage.warning('请先登录');
    return;
  }
  if (!form.equipmentName.trim()) {
    ElMessage.warning('请填写器材名称');
    return;
  }
  if (!form.description.trim()) {
    ElMessage.warning('请填写故障描述');
    return;
  }

  submitting.value = true;
  try {
    await submitRepair({
      userId: user.id,
      username: user.username,
      equipmentName: form.equipmentName.trim(),
      equipmentLocation: form.equipmentLocation.trim(),
      description: form.description.trim(),
    });
    ElMessage.success('报修已提交');
    form.equipmentName = '';
    form.equipmentLocation = '';
    form.description = '';
    showForm.value = false;
    await loadMyRepairs();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败');
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.repair-popover {
  max-height: 420px;
  display: flex;
  flex-direction: column;
}

.popover-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  font-weight: 600;
  font-size: 14px;
}

.repair-form {
  margin-bottom: 4px;
}

.repair-list {
  flex: 1;
  overflow-y: auto;
  max-height: 300px;
}

.empty-hint {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding: 16px 0;
}

.repair-item {
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.repair-item:last-child {
  border-bottom: none;
}

.repair-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.repair-equipment {
  font-weight: 600;
  font-size: 13px;
}

.repair-item-body {
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;
}

.repair-location {
  color: #909399;
  margin-bottom: 2px;
}

.repair-desc {
  line-height: 1.5;
}

.repair-feedback {
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  padding: 6px 8px;
  border-radius: 4px;
  margin-top: 6px;
}

.repair-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 4px;
}
</style>
