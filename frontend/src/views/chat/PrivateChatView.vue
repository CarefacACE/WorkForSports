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
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getConversationsByType, type ChatConversation } from '../../api/chat';
import ChatRoom from './ChatRoom.vue';

const route = useRoute();
const conversations = ref<ChatConversation[]>([]);
const selectedId = ref<number>(0);
const selectedName = ref('');
const currentUserId = ref(0);

function selectConversation(conv: ChatConversation) {
  selectedId.value = conv.id;
  selectedName.value = conv.name || '私信';
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
