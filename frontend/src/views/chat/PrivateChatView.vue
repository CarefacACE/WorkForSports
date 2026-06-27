<template>
  <div class="cv">
    <!-- 会话列表 -->
    <div class="cv-sidebar">
      <div class="cv-sidebar-header">
        <span class="cv-sidebar-title">私信</span>
      </div>
      <div class="cv-list">
        <div v-for="conv in conversations" :key="conv.id" class="cv-item"
          :class="{ active: selectedId === conv.id }" @click="selectConversation(conv)">
          <div class="cv-item-avatar">{{ (conv.name || '私')[0] }}</div>
          <div class="cv-item-info">
            <div class="cv-item-name">{{ conv.name || '私信' }}</div>
          </div>
        </div>
        <div v-if="conversations.length === 0" class="cv-list-empty">暂无私信</div>
      </div>
    </div>

    <!-- 聊天区域 -->
    <div class="cv-main">
      <div v-if="selectedId" class="cv-chat-wrapper">
        <div class="cv-chat-header">
          <span class="cv-chat-title">{{ selectedName }}</span>
          <el-dropdown v-if="selectedId" trigger="click" @command="handleChatCommand">
            <el-button text><el-icon><MoreFilled /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="block">拉黑</el-dropdown-item>
                <el-dropdown-item command="deleteFriend" divided>删除好友</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="cv-chat-body">
          <ChatRoom :conversationId="selectedId" :conversationName="selectedName" :currentUserId="currentUserId" />
        </div>
      </div>
      <div v-else class="cv-empty">
        <div class="cv-empty-icon">✉️</div>
        <div class="cv-empty-text">选择一个私信开始对话</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { MoreFilled } from '@element-plus/icons-vue';
import { getConversationsByType, getGroupMembers, deleteFriend, blockUser, type ChatConversation, type GroupMember } from '../../api/chat';
import ChatRoom from './ChatRoom.vue';

const router = useRouter();
const route = useRoute();
const conversations = ref<ChatConversation[]>([]);
const selectedId = ref<number>(0);
const selectedName = ref('');
const currentUserId = ref(0);
const memberCache = ref<GroupMember[]>([]);

function selectConversation(conv: ChatConversation) {
  selectedId.value = conv.id;
  selectedName.value = conv.name || '私信';
  // 加载该会话的成员列表以便获取对方用户ID
  getGroupMembers(conv.id).then(members => {
    memberCache.value = members;
  }).catch(() => { memberCache.value = []; });
}

async function loadData() {
  try {
    const userInfo = JSON.parse(localStorage.getItem('user_info') || '{}');
    currentUserId.value = userInfo.id || 0;
    if (!currentUserId.value) return;

    conversations.value = await getConversationsByType(currentUserId.value, 'PRIVATE');

    const queryConvId = Number(route.query.conversationId);
    if (queryConvId && conversations.value.some(c => c.id === queryConvId)) {
      const conv = conversations.value.find(c => c.id === queryConvId);
      if (conv) selectConversation(conv);
    }
  } catch (error) {
    ElMessage.error('加载私信列表失败');
  }
}

onMounted(() => { loadData(); });

function getOtherUserId(): number | null {
  const other = memberCache.value.find(m => m.userId !== currentUserId.value);
  return other ? other.userId : null;
}

async function handleChatCommand(command: string) {
  const otherUserId = getOtherUserId();
  if (!otherUserId) {
    ElMessage.warning('无法获取对方信息');
    return;
  }
  const name = selectedName.value;
  if (command === 'deleteFriend') {
    try {
      await ElMessageBox.confirm(`确定删除与 ${name} 的好友关系？`, '删除好友', { type: 'warning', confirmButtonText: '确定删除' });
      await deleteFriend(currentUserId.value, otherUserId);
      ElMessage.success('已删除好友');
      // 重新加载列表，该会话会消失
      await loadData();
      selectedId.value = 0;
      selectedName.value = '';
    } catch (e) {
      if (e !== 'cancel' && (e as any)?.toString() !== 'cancel') {
        ElMessage.error(e instanceof Error ? e.message : '操作失败');
      }
    }
  } else if (command === 'block') {
    try {
      await ElMessageBox.confirm(`确定拉黑 ${name}？拉黑后将无法接收对方消息`, '拉黑', { type: 'warning', confirmButtonText: '确定拉黑' });
      await blockUser(currentUserId.value, otherUserId);
      ElMessage.success('已拉黑');
      await loadData();
      selectedId.value = 0;
      selectedName.value = '';
    } catch (e) {
      if (e !== 'cancel' && (e as any)?.toString() !== 'cancel') {
        ElMessage.error(e instanceof Error ? e.message : '操作失败');
      }
    }
  }
}
</script>

<style scoped>
.cv {
  display: flex;
  flex: 1;
  min-height: 0;
  margin: -24px;
  background: #f8fafc;
  overflow: hidden;
  flex-direction: row !important;
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
  background: linear-gradient(135deg, #dcfce7, #bbf7d0);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
  color: #166534;
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
