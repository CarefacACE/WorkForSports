<template>
  <div class="chat-room">
    <div class="chat-header">
      <span>{{ conversationName }}</span>
    </div>
    <div class="messages-area" ref="messagesArea">
      <div v-for="msg in messages" :key="msg.id" class="message-item"
        :class="{ 'self': msg.senderId === currentUserId }">
        <div class="message-sender" v-if="msg.senderId !== currentUserId">
          {{ getSenderName(msg.senderId) }}
        </div>
        <div class="message-bubble">
          <div class="message-content">{{ msg.content }}</div>
          <div class="message-time">{{ formatTime(msg.createTime) }}</div>
        </div>
      </div>
      <div v-if="messages.length === 0" class="empty-hint">暂无消息</div>
    </div>
    <div class="input-area">
      <el-input v-model="inputText" placeholder="输入消息..." @keyup.enter="sendMessage"
        :disabled="!connected" style="flex:1">
      </el-input>
      <el-button type="primary" @click="sendMessage" :disabled="!connected || !inputText.trim()">
        发送
      </el-button>
    </div>
    <div v-if="!connected" class="connecting-hint">正在连接...</div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onBeforeUnmount } from 'vue';
import { getMessages, type ChatMessage } from '../../api/chat';

const props = defineProps<{
  conversationId: number;
  conversationName: string;
  currentUserId: number;
}>();

const messages = ref<ChatMessage[]>([]);
const inputText = ref('');
const messagesArea = ref<HTMLElement>();
const loading = ref(false);
const connected = ref(false);
let ws: WebSocket | null = null;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;

function getSenderName(senderId: number): string {
  return `用户${senderId}`;
}

function formatTime(time: string): string {
  if (!time) return '';
  const d = new Date(time);
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesArea.value) {
      messagesArea.value.scrollTop = messagesArea.value.scrollHeight;
    }
  });
}

async function loadMessages() {
  loading.value = true;
  try {
    const res = await getMessages(props.conversationId, 1, 100);
    messages.value = res.records;
    scrollToBottom();
  } catch (error) {
    console.error('加载消息失败', error);
  } finally {
    loading.value = false;
  }
}

function connectWs() {
  if (ws) {
    ws.close();
    ws = null;
  }

  const userId = props.currentUserId;
  if (!userId) {
    console.warn('No userId, skipping WebSocket connection');
    return;
  }

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const host = window.location.host;
  const url = `${protocol}//${host}/api/ws/chat?userId=${userId}`;
  console.log('Connecting WebSocket to:', url);
  
  ws = new WebSocket(url);

  ws.onopen = () => {
    console.log('WebSocket connected');
    connected.value = true;
  };

  ws.onclose = (event) => {
    console.log('WebSocket closed:', event.code, event.reason);
    connected.value = false;
    reconnectTimer = setTimeout(connectWs, 3000);
  };

  ws.onerror = (error) => {
    console.error('WebSocket error:', error);
    connected.value = false;
  };

  ws.onmessage = (event: MessageEvent) => {
    try {
      const data = JSON.parse(event.data);
      if (data.type === 'message' && data.conversationId === props.conversationId) {
        messages.value.push({
          id: data.messageId,
          conversationId: data.conversationId,
          senderId: data.senderId,
          content: data.content,
          msgType: data.msgType,
          createTime: data.createTime,
        });
        scrollToBottom();
      }
    } catch {}
  };
}

function sendMessage() {
  if (!inputText.value.trim() || !ws || !connected.value) return;
  ws.send(JSON.stringify({
    action: 'send',
    conversationId: props.conversationId,
    content: inputText.value.trim(),
    msgType: 'TEXT',
  }));
  inputText.value = '';
}

watch(() => props.conversationId, () => {
  if (props.conversationId) loadMessages();
});

onMounted(() => {
  if (props.conversationId) loadMessages();
  connectWs();
});

onBeforeUnmount(() => {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  if (ws) {
    ws.close();
    ws = null;
  }
});
</script>

<style scoped>
.chat-room {
  display: flex;
  flex-direction: column;
  height: 100%;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.chat-header {
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  font-weight: 600;
  font-size: 15px;
}

.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #fafafa;
}

.message-item {
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
}

.message-item.self {
  align-items: flex-end;
}

.message-sender {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.message-bubble {
  max-width: 70%;
  padding: 8px 12px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
}

.self .message-bubble {
  background: #409eff;
  color: #fff;
}

.message-content {
  font-size: 14px;
  word-break: break-all;
}

.message-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 4px;
  text-align: right;
}

.self .message-time {
  color: rgba(255,255,255,0.7);
}

.empty-hint {
  text-align: center;
  color: #c0c4cc;
  padding: 40px 0;
}

.input-area {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #e4e7ed;
  background: #fff;
}

.connecting-hint {
  text-align: center;
  padding: 4px;
  font-size: 12px;
  color: #e6a23c;
  background: #fdf6ec;
}
</style>
