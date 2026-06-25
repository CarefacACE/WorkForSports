<template>
  <div class="admin-notification-page">
    <el-card shadow="never">
      <template #header><span>📢 发布通知</span></template>

      <el-tabs v-model="tab" type="border-card">
        <!-- ═══ 广播通知 ═══ -->
        <el-tab-pane label="📣 广播通知（全体用户）" name="broadcast">
          <el-form label-position="top" style="max-width: 600px;">
            <el-form-item label="通知标题" required>
              <el-input v-model="broadcastTitle" placeholder="如：系统维护通知" />
            </el-form-item>
            <el-form-item label="通知内容" required>
              <el-input v-model="broadcastContent" type="textarea" :rows="4" placeholder="通知正文..." />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="broadcasting" @click="doBroadcast">
                发送给全体用户
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- ═══ 定向通知 ═══ -->
        <el-tab-pane label="🎯 定向通知（指定用户）" name="target">
          <el-form label-position="top" style="max-width: 600px;">
            <el-form-item label="搜索目标用户">
              <el-select
                v-model="selectedUserId"
                filterable
                remote
                reserve-keyword
                placeholder="输入用户名或姓名搜索"
                :remote-method="searchUsers"
                :loading="searchingUsers"
                clearable
                style="width: 100%;"
              >
                <el-option
                  v-for="u in userOptions"
                  :key="u.id"
                  :label="`${u.realName || u.username} (${u.role})`"
                  :value="u.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="通知标题" required>
              <el-input v-model="targetTitle" placeholder="如：私教课程通知" />
            </el-form-item>
            <el-form-item label="通知内容" required>
              <el-input v-model="targetContent" type="textarea" :rows="4" placeholder="通知正文..." />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="sending"
                :disabled="!selectedUserId"
                @click="doSendToUser"
              >
                发送给该用户
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { broadcastNotification, sendNotificationToUser } from '../api/notification';
import { getAllUsersForAdmin, type AdminUser } from '../api/chat';

const tab = ref('broadcast');

/* ─── 广播通知 ─── */
const broadcastTitle = ref('');
const broadcastContent = ref('');
const broadcasting = ref(false);

async function doBroadcast() {
  if (!broadcastTitle.value.trim()) { ElMessage.warning('请输入标题'); return; }
  if (!broadcastContent.value.trim()) { ElMessage.warning('请输入内容'); return; }
  broadcasting.value = true;
  try {
    await broadcastNotification(broadcastTitle.value.trim(), broadcastContent.value.trim());
    ElMessage.success('广播通知已发送给全体用户');
    broadcastTitle.value = '';
    broadcastContent.value = '';
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '发送失败');
  } finally {
    broadcasting.value = false;
  }
}

/* ─── 定向通知 ─── */
const targetTitle = ref('');
const targetContent = ref('');
const selectedUserId = ref<number | null>(null);
const sending = ref(false);
const searchingUsers = ref(false);
const userOptions = ref<AdminUser[]>([]);

async function searchUsers(query: string) {
  if (!query) { userOptions.value = []; return; }
  searchingUsers.value = true;
  try {
    const all = await getAllUsersForAdmin();
    const q = query.toLowerCase();
    userOptions.value = all.filter(
      u => u.username.toLowerCase().includes(q) || (u.realName || '').toLowerCase().includes(q)
    ).slice(0, 20);
  } catch { /* ignore */ }
  searchingUsers.value = false;
}

async function doSendToUser() {
  if (!selectedUserId.value) { ElMessage.warning('请选择目标用户'); return; }
  if (!targetTitle.value.trim()) { ElMessage.warning('请输入标题'); return; }
  if (!targetContent.value.trim()) { ElMessage.warning('请输入内容'); return; }
  sending.value = true;
  try {
    await sendNotificationToUser(selectedUserId.value, targetTitle.value.trim(), targetContent.value.trim());
    ElMessage.success('通知已发送');
    targetTitle.value = '';
    targetContent.value = '';
    selectedUserId.value = null;
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '发送失败');
  } finally {
    sending.value = false;
  }
}
</script>

<style scoped>
.admin-notification-page { display: flex; flex-direction: column; }
</style>
