<template>
  <div class="chat-view">
    <div class="conversation-list">
      <div class="list-header">
        <span>群聊列表</span>
        <el-button type="primary" size="small" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          新建
        </el-button>
      </div>
      <div class="list-body">
        <div v-for="conv in conversations" :key="conv.id" class="conv-item"
          :class="{ active: selectedId === conv.id }" @click="selectConversation(conv)">
          <div class="conv-name">{{ conv.name || '未命名群聊' }}</div>
        </div>
        <el-empty v-if="conversations.length === 0" description="暂无群聊" :image-size="60" />
      </div>
    </div>
    <div class="chat-main">
      <div class="chat-area" v-if="selectedId">
        <div class="chat-header-bar">
          <span class="chat-title">{{ selectedName }}</span>
          <el-button type="primary" text @click="showGroupInfo = !showGroupInfo">
            <el-icon><User /></el-icon>
            群聊信息
          </el-button>
        </div>
        <div class="chat-content">
          <ChatRoom :conversationId="selectedId" :conversationName="selectedName" :currentUserId="currentUserId" />
          <GroupInfoPanel v-if="showGroupInfo" :conversationId="selectedId" :conversationName="selectedName"
            :ownerId="selectedOwnerId" :currentUserId="currentUserId"
            @close="showGroupInfo = false" @left="handleLeft" @nameChanged="handleNameChanged" />
        </div>
      </div>
      <div v-else class="empty-hint">
        <el-empty description="请选择一个群聊" />
      </div>
    </div>

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
          <div class="search-result">
            <span>{{ searchResult.realName || searchResult.username }}</span>
            <el-tag size="small">{{ searchResult.role === 'COACH' ? '教练' : '会员' }}</el-tag>
            <el-button size="small" type="primary" link @click="addMember" :disabled="isMemberAdded">
              {{ isMemberAdded ? '已添加' : '添加' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="已选成员">
          <div class="member-list">
            <el-tag v-for="member in selectedMembers" :key="member.id" closable @close="removeMember(member.id)">
              {{ member.realName || member.username }}
            </el-tag>
            <span v-if="selectedMembers.length === 0" class="empty-text">暂无成员</span>
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
import { Plus, User } from '@element-plus/icons-vue';
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
.chat-view {
  display: flex;
  height: calc(100vh - 160px);
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.conversation-list {
  width: 260px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.list-header {
  padding: 10px 16px;
  font-weight: 600;
  font-size: 15px;
  border-bottom: 1px solid #e4e7ed;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.list-body {
  flex: 1;
  overflow-y: auto;
}

.conv-item {
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.conv-item:hover {
  background: #f5f7fa;
}

.conv-item.active {
  background: #ecf5ff;
  border-right: 3px solid #409eff;
}

.conv-name {
  font-size: 14px;
  color: #303133;
}

.chat-main {
  flex: 1;
  display: flex;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header-bar {
  padding: 10px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-title {
  font-weight: 600;
  font-size: 15px;
}

.chat-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.empty-hint {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-result {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  width: 100%;
}

.member-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-text {
  color: #c0c4cc;
  font-size: 13px;
}
</style>
