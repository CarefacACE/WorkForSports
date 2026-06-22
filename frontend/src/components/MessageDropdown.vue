<template>
  <el-popover placement="bottom" :width="360" trigger="click">
    <template #reference>
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
        <el-button text @click="loadUnreadConversations">
          <el-icon size="18"><ChatDotRound /></el-icon>
        </el-button>
      </el-badge>
    </template>
    <div class="message-dropdown">
      <div class="dropdown-header">消息</div>
      <div class="message-list">
        <div v-for="item in unreadConversations" :key="item.conversationId" class="message-item"
          @click="goToChat(item)">
          <div class="message-left">
            <el-icon :size="20" :color="item.conversationType === 'GROUP' ? '#409eff' : '#67c23a'">
              <ChatDotRound />
            </el-icon>
          </div>
          <div class="message-body">
            <div class="message-name">
              {{ item.conversationName }}
              <el-tag v-if="item.conversationType === 'GROUP'" size="small" type="info">群</el-tag>
            </div>
            <div class="message-preview">{{ item.lastMessage }}</div>
          </div>
          <div class="message-right">
            <div class="message-time">{{ formatTime(item.lastTime) }}</div>
            <el-badge :value="item.unreadCount" :max="99" class="msg-badge" />
          </div>
        </div>
        <el-empty v-if="unreadConversations.length === 0" description="暂无未读消息" :image-size="48" />
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { ChatDotRound } from '@element-plus/icons-vue';
import { getUnreadCount, getUnreadConversations, markConversationRead, type UnreadConversation } from '../api/chat';

const router = useRouter();
const unreadCount = ref(0);
const unreadConversations = ref<UnreadConversation[]>([]);
const currentUserId = ref(0);
const currentRole = ref('');
let pollTimer: ReturnType<typeof setInterval> | null = null;

function formatTime(time: string): string {
  if (!time) return '';
  const d = new Date(time);
  const now = new Date();
  const diff = now.getTime() - d.getTime();
  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${d.getMinutes().toString().padStart(2, '0')}`;
}

async function loadUnread() {
  try {
    unreadCount.value = await getUnreadCount(currentUserId.value);
  } catch {}
}

async function loadUnreadConversations() {
  try {
    unreadConversations.value = await getUnreadConversations(currentUserId.value);
  } catch {}
}

async function goToChat(item: UnreadConversation) {
  const prefix = currentRole.value === 'COACH' ? '/coach' : '/member';
  try {
    await markConversationRead(item.conversationId, currentUserId.value);
  } catch {}
  if (item.conversationType === 'GROUP') {
    router.push({ path: `${prefix}/chat-group`, query: { conversationId: item.conversationId } });
  } else {
    router.push({ path: `${prefix}/chat-private`, query: { conversationId: item.conversationId } });
  }
  unreadConversations.value = unreadConversations.value.filter(c => c.conversationId !== item.conversationId);
  unreadCount.value = Math.max(0, unreadCount.value - item.unreadCount);
}

onMounted(() => {
  const userInfo = JSON.parse(localStorage.getItem('user_info') || '{}');
  currentUserId.value = userInfo.id || 0;
  currentRole.value = userInfo.role || '';
  if (currentUserId.value) {
    loadUnread();
    pollTimer = setInterval(loadUnread, 30000);
  }
});

onBeforeUnmount(() => {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
});

defineExpose({ loadUnread });
</script>

<style scoped>
.dropdown-header {
  font-weight: 600;
  font-size: 14px;
  padding: 8px 0;
  border-bottom: 1px solid #e4e7ed;
  margin-bottom: 8px;
}

.message-list {
  max-height: 400px;
  overflow-y: auto;
}

.message-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 4px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.message-item:hover {
  background: #f5f7fa;
}

.message-left {
  flex-shrink: 0;
}

.message-body {
  flex: 1;
  min-width: 0;
}

.message-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 4px;
}

.message-preview {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-right {
  flex-shrink: 0;
  text-align: right;
}

.message-time {
  font-size: 11px;
  color: #c0c4cc;
}

.msg-badge {
  margin-top: 4px;
}
</style>
