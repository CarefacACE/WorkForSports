<template>
  <el-popover
    placement="bottom"
    :width="440"
    trigger="click"
    @show="loadMyComplaints"
  >
    <template #reference>
      <el-badge :value="myComplaintCount > 0 ? myComplaintCount : undefined" :max="99">
        <el-button circle size="small" title="投诉">
          <el-icon><WarningFilled /></el-icon>
        </el-button>
      </el-badge>
    </template>

    <div class="complaint-popover">
      <div class="popover-header">
        <span>📝 投诉建议</span>
        <el-button type="primary" link size="small" @click="showForm = !showForm">
          {{ showForm ? '收起' : '我要投诉' }}
        </el-button>
      </div>

      <!-- 新增投诉表单 -->
      <div v-if="showForm" class="complaint-form">
        <el-form label-position="top" size="small">
          <el-form-item label="投诉教练" required>
            <el-select
              v-model="form.coachId"
              filterable
              placeholder="搜索教练"
              :loading="loadingCoaches"
              style="width: 100%"
              @focus="loadCoaches"
            >
              <el-option
                v-for="coach in coachOptions"
                :key="coach.id"
                :label="coach.realName ? `${coach.realName} (${coach.username})` : coach.username"
                :value="coach.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="投诉内容" required>
            <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请详细描述您的投诉内容..." />
          </el-form-item>
          <el-form-item>
            <el-button type="danger" :loading="submitting" @click="submitComplaintForm">提交投诉</el-button>
          </el-form-item>
        </el-form>
        <el-divider />
      </div>

      <!-- 我的投诉列表 -->
      <div class="complaint-list">
        <div v-if="myComplaints.length === 0" class="empty-hint">暂无投诉记录</div>
        <div v-for="item in myComplaints" :key="item.id" class="complaint-item">
          <div class="complaint-item-header">
            <span class="complaint-coach">👨‍🏫 {{ item.coachUsername }}</span>
            <el-tag :type="statusTag(item.status)" size="small">{{ statusLabel(item.status) }}</el-tag>
          </div>
          <div class="complaint-content">{{ item.content }}</div>
          <div v-if="item.feedback" class="complaint-feedback">
            <strong>管理员回复：</strong>{{ item.feedback }}
          </div>
          <div class="complaint-time">{{ item.createTime }}</div>
        </div>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { WarningFilled } from '@element-plus/icons-vue';
import { submitComplaint, getMyComplaints, type CoachComplaint } from '../api/feedback';
import { getUsers, type UserItem } from '../api/user';
import { useUserStore } from '../stores/user';

const userStore = useUserStore();
const user = userStore.user;

const showForm = ref(false);
const submitting = ref(false);
const myComplaints = ref<CoachComplaint[]>([]);
const myComplaintCount = ref(0);
const coachOptions = ref<UserItem[]>([]);
const loadingCoaches = ref(false);

const form = reactive({
  coachId: null as number | null,
  content: '',
});

function statusTag(status: string): string {
  const map: Record<string, string> = { PENDING: 'warning', PROCESSING: '', RESOLVED: 'success', REJECTED: 'danger' };
  return map[status] || 'info';
}

function statusLabel(status: string): string {
  const map: Record<string, string> = { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已解决', REJECTED: '已驳回' };
  return map[status] || status;
}

async function loadCoaches() {
  if (coachOptions.value.length > 0) return;
  loadingCoaches.value = true;
  try {
    const result = await getUsers({ role: 'COACH', pageNum: 1, pageSize: 100 });
    coachOptions.value = result.records || [];
  } catch { /* ignore */ } finally {
    loadingCoaches.value = false;
  }
}

async function loadMyComplaints() {
  if (!user?.id) return;
  try {
    const result = await getMyComplaints(user.id, 1, 20);
    myComplaints.value = result.records || [];
    myComplaintCount.value = myComplaints.value.filter(c => c.status === 'PROCESSING' || c.status === 'PENDING').length;
  } catch { /* ignore */ }
}

async function submitComplaintForm() {
  if (!user?.id) {
    ElMessage.warning('请先登录');
    return;
  }
  if (!form.coachId) {
    ElMessage.warning('请选择要投诉的教练');
    return;
  }
  if (!form.content.trim()) {
    ElMessage.warning('请填写投诉内容');
    return;
  }

  const selectedCoach = coachOptions.value.find(c => c.id === form.coachId);

  submitting.value = true;
  try {
    await submitComplaint({
      userId: user.id,
      username: user.username,
      coachId: form.coachId,
      coachUsername: selectedCoach?.username || '',
      content: form.content.trim(),
    });
    ElMessage.success('投诉已提交');
    form.coachId = null;
    form.content = '';
    showForm.value = false;
    await loadMyComplaints();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败');
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.complaint-popover {
  max-height: 440px;
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

.complaint-form {
  margin-bottom: 4px;
}

.complaint-list {
  flex: 1;
  overflow-y: auto;
  max-height: 320px;
}

.empty-hint {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding: 16px 0;
}

.complaint-item {
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.complaint-item:last-child {
  border-bottom: none;
}

.complaint-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.complaint-coach {
  font-weight: 600;
  font-size: 13px;
}

.complaint-content {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
  margin-bottom: 4px;
}

.complaint-feedback {
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  padding: 6px 8px;
  border-radius: 4px;
  margin-top: 6px;
}

.complaint-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 4px;
}
</style>
