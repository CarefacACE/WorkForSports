<template>
  <div class="cv">
    <!-- 会话列表 -->
    <div class="cv-sidebar">
      <div class="cv-sidebar-header">
        <span class="cv-sidebar-title">群聊</span>
        <button class="cv-new-btn" @click="showCreateDialog = true">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </button>
      </div>
      <div class="cv-list">
        <div v-for="conv in conversations" :key="conv.id" class="cv-item"
          :class="{ active: selectedId === conv.id }" @click="selectConversation(conv)">
          <div class="cv-item-avatar">{{ (conv.name || '群')[0] }}</div>
          <div class="cv-item-info">
            <div class="cv-item-name">{{ conv.name || '未命名群聊' }}</div>
          </div>
        </div>
        <div v-if="conversations.length === 0" class="cv-list-empty">暂无群聊</div>
      </div>
    </div>

    <!-- 聊天区域 -->
    <div class="cv-main">
      <div v-if="selectedId" class="cv-chat-wrapper">
        <div class="cv-chat-header">
          <span class="cv-chat-title">{{ selectedName }}</span>
          <button class="cv-info-btn" @click="showGroupInfo = !showGroupInfo">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
            群信息
          </button>
        </div>
        <div class="cv-chat-body">
          <ChatRoom :conversationId="selectedId" :conversationName="selectedName" :currentUserId="currentUserId" />
          <GroupInfoPanel v-if="showGroupInfo" :conversationId="selectedId" :conversationName="selectedName"
            :ownerId="selectedOwnerId" :currentUserId="currentUserId"
            @close="showGroupInfo = false" @left="handleLeft" @nameChanged="handleNameChanged" />
        </div>
      </div>
      <div v-else class="cv-empty">
        <div class="cv-empty-icon">💬</div>
        <div class="cv-empty-text">选择一个群聊开始对话</div>
      </div>
    </div>

    <!-- 新建群聊弹窗 -->
    <el-dialog v-model="showCreateDialog" title="新建群聊" width="480px">
      <el-form label-position="top">
        <el-form-item label="群聊名称" required>
          <el-input v-model="newGroupName" placeholder="请输入群聊名称" />
        </el-form-item>
        <el-form-item label="搜索添加成员">
          <el-input v-model="searchKeyword" placeholder="输入用户名搜索" clearable @keyup.enter="handleSearch">
            <template #append>
              <el-button @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="searchResult" label="搜索结果">
          <div class="cv-search-result">
            <span>{{ searchResult.realName || searchResult.username }}</span>
            <el-tag size="small">{{ searchResult.role === 'COACH' ? '教练' : '会员' }}</el-tag>
            <el-button size="small" type="primary" link @click="addMember" :disabled="isMemberAdded">
              {{ isMemberAdded ? '已添加' : '添加' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="已选成员">
          <div class="cv-member-list">
            <el-tag v-for="member in selectedMembers" :key="member.id" closable @close="removeMember(member.id)">
              {{ member.realName || member.username }}
            </el-tag>
            <span v-if="selectedMembers.length === 0" class="cv-empty-text">暂无成员</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :disabled="!newGroupName.trim()" :loading="creating">
          创建群聊
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getConversationsByType, createConversation, searchUser, searchUserById, type ChatConversation, type SearchResult } from '../../api/chat';
import ChatRoom from './ChatRoom.vue';
import GroupInfoPanel from './GroupInfoPanel.vue';

const route = useRoute();
const conversations = ref<ChatConversation[]>([]);
const selectedId = ref<number>(0);
const selectedName = ref('');
const selectedOwnerId = ref<number | null>(null);
const currentUserId = ref(0);
const showGroupInfo = ref(false);

const showCreateDialog = ref(false);
const newGroupName = ref('');
const searchKeyword = ref('');
const searchResult = ref<SearchResult | null>(null);
const selectedMembers = ref<SearchResult[]>([]);
const creating = ref(false);

const isMemberAdded = computed(() => {
  if (!searchResult.value) return false;
  return selectedMembers.value.some(m => m.id === searchResult.value!.id);
});

function selectConversation(conv: ChatConversation) {
  selectedId.value = conv.id;
  selectedName.value = conv.name || '未命名群聊';
  selectedOwnerId.value = conv.ownerId;
  showGroupInfo.value = false;
}

function handleLeft() {
  selectedId.value = 0;
  selectedName.value = '';
  selectedOwnerId.value = null;
  showGroupInfo.value = false;
  loadData();
}

function handleNameChanged(name: string) {
  selectedName.value = name;
  const conv = conversations.value.find(c => c.id === selectedId.value);
  if (conv) conv.name = name;
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

function addMember() {
  if (!searchResult.value || isMemberAdded.value) return;
  selectedMembers.value.push(searchResult.value);
  searchResult.value = null;
  searchKeyword.value = '';
}

function removeMember(id: number) {
  selectedMembers.value = selectedMembers.value.filter(m => m.id !== id);
}

async function handleCreate() {
  if (!newGroupName.value.trim()) return;
  creating.value = true;
  try {
    const memberIds = selectedMembers.value.map(m => m.id);
    const conv = await createConversation({
      type: 'GROUP',
      name: newGroupName.value.trim(),
      ownerId: currentUserId.value,
      memberIds,
    });
    ElMessage.success('群聊创建成功');
    showCreateDialog.value = false;
    newGroupName.value = '';
    selectedMembers.value = [];
    searchKeyword.value = '';
    searchResult.value = null;
    await loadData();
    selectConversation(conv);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败');
  } finally {
    creating.value = false;
  }
}

async function loadData() {
  try {
    const userInfo = JSON.parse(localStorage.getItem('user_info') || '{}');
    currentUserId.value = userInfo.id || 0;
    if (!currentUserId.value) return;

    conversations.value = await getConversationsByType(currentUserId.value, 'GROUP');

    const queryConvId = Number(route.query.conversationId);
    if (queryConvId && conversations.value.some(c => c.id === queryConvId)) {
      const conv = conversations.value.find(c => c.id === queryConvId);
      if (conv) selectConversation(conv);
    }
  } catch (error) {
    ElMessage.error('加载群聊列表失败');
  }
}

onMounted(() => { loadData(); });
</script>

<style scoped>
.cv {
  display: flex;
  height: calc(100vh - 76px);
  margin: -28px -32px;
  background: #f8fafc;
  overflow: hidden;
}

/* === Sidebar === */
.cv-sidebar {
  width: 300px;
  min-width: 300px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(24px) saturate(1.6);
  -webkit-backdrop-filter: blur(24px) saturate(1.6);
  border-right: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
}

.cv-sidebar-header {
  padding: 20px 20px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}
.cv-sidebar-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 0.2px;
}

.cv-new-btn {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  border: 1px solid rgba(37, 99, 235, 0.15);
  background: rgba(37, 99, 235, 0.06);
  color: #2563eb;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s cubic-bezier(0.22, 1, 0.36, 1);
}
.cv-new-btn:hover {
  background: #2563eb;
  color: #ffffff;
  border-color: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
}

.cv-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px;
}

.cv-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 14px;
  border-radius: 12px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: all 0.2s cubic-bezier(0.22, 1, 0.36, 1);
}
.cv-item:hover {
  background: rgba(0, 0, 0, 0.03);
}
.cv-item.active {
  background: rgba(37, 99, 235, 0.08);
}

.cv-item-avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #e0e7ff, #c7d2fe);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
  color: #3730a3;
  flex-shrink: 0;
}
.cv-item.active .cv-item-avatar {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
}

.cv-item-info {
  flex: 1;
  min-width: 0;
}
.cv-item-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cv-item.active .cv-item-name {
  color: #2563eb;
}

.cv-list-empty {
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
  padding: 48px 0;
}

/* === Main Chat === */
.cv-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.cv-chat-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.cv-chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}
.cv-chat-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}
.cv-info-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 10px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.6);
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}
.cv-info-btn:hover {
  background: #ffffff;
  border-color: rgba(0, 0, 0, 0.1);
  color: #1e293b;
}

.cv-chat-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* === Empty State === */
.cv-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}
.cv-empty-icon {
  font-size: 48px;
  opacity: 0.4;
}
.cv-empty-text {
  font-size: 15px;
  color: #94a3b8;
}

/* === Dialog Helpers === */
.cv-search-result {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #f8fafc;
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: 10px;
  width: 100%;
}
.cv-member-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* Scrollbar */
.cv-list::-webkit-scrollbar {
  width: 4px;
}
.cv-list::-webkit-scrollbar-track {
  background: transparent;
}
.cv-list::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 2px;
}
</style>
