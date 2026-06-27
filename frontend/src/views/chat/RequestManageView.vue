<template>
  <div class="request-manage-full">
    <el-card class="request-card">
      <template #header>
        <div class="card-header">
          <span>好友/加群申请</span>
          <el-button type="primary" @click="showSearchDialog = true">
            <el-icon><Search /></el-icon>
            添加好友/发起会话
          </el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="loadRequests">
        <el-tab-pane label="收到的申请" name="received">
          <el-table :data="receivedRequests" v-loading="loading" border stripe>
            <el-table-column prop="fromUserId" label="申请人ID" width="80" align="center" />
            <el-table-column label="申请类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.requestType === 'FRIEND' ? 'primary' : row.requestType === 'JOIN_GROUP' ? 'success' : 'warning'" size="small">
                  {{ requestTypeLabel(row.requestType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="留言" min-width="150" />
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'APPROVED' ? 'success' : 'danger'" size="small">
                  {{ statusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="申请时间" width="170" align="center" />
            <el-table-column label="操作" width="150" align="center" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 'PENDING'">
                  <el-button type="primary" link @click="handleApprove(row.id)">同意</el-button>
                  <el-button type="danger" link @click="handleReject(row.id)">拒绝</el-button>
                </template>
                <span v-else class="processed-text">已处理</span>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrapper">
            <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize"
              :page-sizes="[10, 20, 50]" :total="total"
              layout="total, sizes, prev, pager, next" @size-change="loadRequests"
              @current-change="loadRequests" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="发出的申请" name="sent">
          <el-table :data="sentRequests" v-loading="loading" border stripe>
            <el-table-column prop="toUserId" label="对方ID" width="80" align="center" />
            <el-table-column label="申请类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.requestType === 'FRIEND' ? 'primary' : row.requestType === 'JOIN_GROUP' ? 'success' : 'warning'" size="small">
                  {{ requestTypeLabel(row.requestType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="留言" min-width="150" />
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'APPROVED' ? 'success' : 'danger'" size="small">
                  {{ statusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="申请时间" width="170" align="center" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="showSearchDialog" title="添加好友/发起会话" width="520px">
      <el-tabs v-model="dialogTab">
        <el-tab-pane label="添加好友" name="friend">
          <el-form label-position="top">
            <el-form-item label="搜索用户（输入用户名）">
              <el-input v-model="searchKeyword" placeholder="请输入用户名" clearable @keyup.enter="handleSearch">
                <template #append>
                  <el-button @click="handleSearch">搜索</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-form>
          <div v-if="searchResult" class="search-result">
            <div class="result-info">
              <span class="result-name">{{ searchResult.realName || searchResult.username }}</span>
              <el-tag size="small">{{ searchResult.role === 'COACH' ? '教练' : '会员' }}</el-tag>
            </div>
            <div class="result-actions">
              <el-button type="primary" size="small" @click="handleSendFriendRequest">添加好友</el-button>
              <el-button type="success" size="small" @click="handleSendTempChat">发起临时会话</el-button>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="加入群聊" name="group">
          <el-form label-position="top">
            <el-form-item label="搜索群聊（输入群聊名称）">
              <el-input v-model="groupSearchKeyword" placeholder="请输入群聊名称" clearable @keyup.enter="handleSearchGroups">
                <template #append>
                  <el-button @click="handleSearchGroups">搜索</el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="可加入的群聊">
              <div class="group-list">
                <div v-for="group in displayGroups" :key="group.id" class="group-item"
                  :class="{ active: selectedGroupId === group.id }" @click="selectedGroupId = group.id">
                  <div class="group-name">{{ group.name }}</div>
                  <div class="group-id">ID: {{ group.id }}</div>
                </div>
                <div v-if="displayGroups.length === 0" class="empty-hint">暂无可加入的群聊</div>
              </div>
            </el-form-item>
            <el-form-item label="申请留言">
              <el-input v-model="requestMessage" placeholder="可选" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="showSearchDialog = false">取消</el-button>
        <el-button v-if="dialogTab === 'group'" type="primary" @click="handleJoinGroup" :disabled="!selectedGroupId">发送加群申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { searchUser, searchUserById, startTempChat, sendFriendRequest, sendJoinGroupRequest, approveRequest, rejectRequest, getMyRequests, getSentRequests, getAvailableGroups, searchGroups, searchGroupById, type FriendRequest, type SearchResult, type ChatConversation } from '../../api/chat';

const router = useRouter();
const activeTab = ref('received');
const loading = ref(false);
const receivedRequests = ref<FriendRequest[]>([]);
const sentRequests = ref<FriendRequest[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const currentUserId = ref(0);

const showSearchDialog = ref(false);
const dialogTab = ref('friend');
const searchKeyword = ref('');
const searchResult = ref<SearchResult | null>(null);
const groupSearchKeyword = ref('');
const availableGroups = ref<ChatConversation[]>([]);
const displayGroups = ref<ChatConversation[]>([]);
const selectedGroupId = ref<number | null>(null);
const requestMessage = ref('');

function requestTypeLabel(type: string) {
  const map: Record<string, string> = { FRIEND: '好友', TEMP_CHAT: '临时会话', JOIN_GROUP: '加群' };
  return map[type] || type;
}

function statusLabel(status: string) {
  const map: Record<string, string> = { PENDING: '待处理', APPROVED: '已同意', REJECTED: '已拒绝' };
  return map[status] || status;
}

async function loadRequests() {
  loading.value = true;
  try {
    if (activeTab.value === 'received') {
      const res = await getMyRequests(currentUserId.value, undefined, pageNum.value, pageSize.value);
      receivedRequests.value = res.records;
      total.value = res.total;
    } else {
      const res = await getSentRequests(currentUserId.value, pageNum.value, pageSize.value);
      sentRequests.value = res.records;
      total.value = res.total;
    }
  } catch (error) {
    ElMessage.error('加载申请列表失败');
  } finally {
    loading.value = false;
  }
}

async function handleSearch() {
  if (!searchKeyword.value.trim()) return;
  try {
    const keyword = searchKeyword.value.trim();
    if (/^\d+$/.test(keyword)) {
      searchResult.value = await searchUserById(Number(keyword));
    } else {
      searchResult.value = await searchUser(keyword);
    }
  } catch {
    searchResult.value = null;
    ElMessage.error('用户不存在');
  }
}

async function handleSendFriendRequest() {
  if (!searchResult.value) return;
  try {
    await sendFriendRequest(currentUserId.value, searchResult.value.id, 'FRIEND', requestMessage.value);
    ElMessage.success('好友申请已发送');
    showSearchDialog.value = false;
    searchResult.value = null;
    searchKeyword.value = '';
    requestMessage.value = '';
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发送失败');
  }
}

async function handleSendTempChat() {
  if (!searchResult.value) return;
  try {
    const conversation = await startTempChat(currentUserId.value, searchResult.value.id);
    ElMessage.success('会话已创建');
    showSearchDialog.value = false;
    searchResult.value = null;
    searchKeyword.value = '';
    const userInfo = JSON.parse(localStorage.getItem('user_info') || '{}');
    if (userInfo.role === 'COACH') {
      router.push('/coach/chat-private');
    } else {
      router.push('/member/chat-private');
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败');
  }
}

async function handleJoinGroup() {
  if (!selectedGroupId.value) return;
  try {
    await sendJoinGroupRequest(currentUserId.value, selectedGroupId.value, requestMessage.value);
    ElMessage.success('加群申请已发送');
    showSearchDialog.value = false;
    selectedGroupId.value = null;
    requestMessage.value = '';
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发送失败');
  }
}

async function loadAvailableGroups() {
  try {
    availableGroups.value = await getAvailableGroups(currentUserId.value);
    displayGroups.value = availableGroups.value;
  } catch {}
}

async function handleSearchGroups() {
  if (!groupSearchKeyword.value.trim()) {
    displayGroups.value = availableGroups.value;
    return;
  }
  try {
    const keyword = groupSearchKeyword.value.trim();
    if (/^\d+$/.test(keyword)) {
      const group = await searchGroupById(Number(keyword), currentUserId.value);
      displayGroups.value = group ? [group] : [];
    } else {
      displayGroups.value = await searchGroups(keyword, currentUserId.value);
    }
  } catch {
    displayGroups.value = [];
  }
}

async function handleApprove(id: number) {
  try {
    await approveRequest(id, currentUserId.value);
    ElMessage.success('已同意');
    loadRequests();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败');
  }
}

async function handleReject(id: number) {
  try {
    await rejectRequest(id, currentUserId.value);
    ElMessage.success('已拒绝');
    loadRequests();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败');
  }
}

onMounted(() => {
  const userInfo = JSON.parse(localStorage.getItem('user_info') || '{}');
  currentUserId.value = userInfo.id || 0;
  loadRequests();
  loadAvailableGroups();
});
</script>

<style scoped>
.request-manage-full {
  display: flex;
  flex-direction: row !important;
  flex: 1;
  margin: -24px;
  background: #f8fafc;
  overflow: hidden;
}

.request-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-radius: 0;
}

.request-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.request-card :deep(.el-tabs) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.request-card :deep(.el-tabs__content) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.request-card :deep(.el-tab-pane) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.request-card :deep(.el-table) {
  flex: 1;
}

.request-card :deep(.el-table__body-wrapper) {
  flex: 1;
  overflow-y: auto;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.search-result {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-top: 8px;
}

.result-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.result-name {
  font-weight: 500;
  font-size: 15px;
}

.result-actions {
  display: flex;
  gap: 8px;
}

.processed-text {
  font-size: 12px;
  color: #909399;
}

.group-list {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  width: 100%;
}

.group-item {
  padding: 10px 12px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.group-item:hover {
  background: #f5f7fa;
}

.group-item.active {
  background: #ecf5ff;
}

.group-name {
  font-size: 14px;
  color: #303133;
}

.group-id {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.empty-hint {
  text-align: center;
  color: #c0c4cc;
  padding: 20px;
  font-size: 13px;
}
</style>
