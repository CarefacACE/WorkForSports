<template>
  <div class="chat-view">
    <div class="conversation-list">
      <div class="list-header">
        <span>私信列表</span>
      </div>
      <div class="list-body">
        <div v-for="conv in conversations" :key="conv.id" class="conv-item"
          :class="{ active: selectedId === conv.id }" @click="selectConversation(conv)">
          <div class="conv-name">{{ conv.name || '私信' }}</div>
        </div>
        <el-empty v-if="conversations.length === 0" description="暂无私信" :image-size="60" />
      </div>
    </div>
    <div class="chat-main">
      <ChatRoom v-if="selectedId" :conversationId="selectedId"
        :conversationName="selectedName" :currentUserId="currentUserId" />
      <div v-else class="empty-hint">
        <el-empty description="请选择一个私信" />
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
  padding: 14px 16px;
  font-weight: 600;
  font-size: 15px;
  border-bottom: 1px solid #e4e7ed;
  background: #f5f7fa;
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

.empty-hint {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
