<template>
  <div class="cr">
    <div class="cr-messages" ref="messagesArea">
      <div v-for="msg in messages" :key="msg.id" class="cr-msg"
        :class="{ self: msg.senderId === currentUserId }">
        <div class="cr-sender" v-if="msg.senderId !== currentUserId">
          {{ getSenderName(msg.senderId) }}
        </div>
        <div class="cr-bubble">
          <div class="cr-text">{{ msg.content }}</div>
          <div class="cr-time">{{ formatTime(msg.createTime) }}</div>
        </div>
      </div>
      <div v-if="messages.length === 0" class="cr-empty">暂无消息，发送第一条吧</div>
    </div>
    <div class="cr-input-bar">
      <div class="cr-input-wrap">
        <textarea
          v-model="inputText"
          placeholder="输入消息…"
          :disabled="!connected"
          rows="1"
          @keydown.enter.exact.prevent="sendMessage"
          @input="autoResize"
          ref="textareaEl"
        ></textarea>
      </div>
      <button class="cr-send-btn" :disabled="!connected || !inputText.trim()" @click="sendMessage">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
        </svg>
      </button>
    </div>
    <div v-if="!connected" class="cr-connecting">
      <span class="cr-dot-pulse"></span> 正在连接…
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onBeforeUnmount } from 'vue';
import { ElMessage } from 'element-plus';
import { getMessages, type ChatMessage } from '../../api/chat';

const props = defineProps<{
  conversationId: number;
  conversationName: string;
  currentUserId: number;
}>();

const emit = defineEmits<{
  'message-sent': [];
}>();

const messages = ref<ChatMessage[]>([]);
const inputText = ref('');
const messagesArea = ref<HTMLElement>();
const textareaEl = ref<HTMLTextAreaElement>();
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

function autoResize() {
  const el = textareaEl.value;
  if (!el) return;
  el.style.height = 'auto';
  el.style.height = Math.min(el.scrollHeight, 120) + 'px';
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

  ws = new WebSocket(url);

  ws.onopen = () => {
    connected.value = true;
  };

  ws.onclose = () => {
    connected.value = false;
    reconnectTimer = setTimeout(connectWs, 3000);
  };

  ws.onerror = () => {
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
      } else if (data.type === 'error') {
        ElMessage.error(data.message || '发送失败');
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
  emit('message-sent');
  nextTick(() => {
    if (textareaEl.value) {
      textareaEl.value.style.height = 'auto';
    }
  });
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
.cr {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  background: #f8fafc;
  overflow: hidden;
}

/* === Messages Area === */
.cr-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cr-msg {
  display: flex;
  flex-direction: column;
  max-width: 65%;
  margin-bottom: 6px;
}
.cr-msg.self {
  align-self: flex-end;
  align-items: flex-end;
}

.cr-sender {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 4px;
  padding-left: 4px;
}

.cr-bubble {
  padding: 12px 16px;
  border-radius: 18px 18px 18px 4px;
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
  transition: all 0.15s ease;
}
.cr-bubble:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.cr-msg.self .cr-bubble {
  border-radius: 18px 18px 4px 18px;
  background: #2563eb;
  border-color: transparent;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.2);
}

.cr-text {
  font-size: 14px;
  line-height: 1.55;
  color: #1e293b;
  word-break: break-word;
  white-space: pre-wrap;
}
.cr-msg.self .cr-text {
  color: #ffffff;
}

.cr-time {
  font-size: 10px;
  color: #94a3b8;
  margin-top: 6px;
  text-align: right;
  font-family: 'JetBrains Mono', monospace;
  letter-spacing: 0.3px;
}
.cr-msg.self .cr-time {
  color: rgba(255, 255, 255, 0.55);
}

.cr-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 14px;
}

/* === Input Bar === */
.cr-input-bar {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 16px 24px 20px;
  background: #ffffff;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.cr-input-wrap {
  flex: 1;
  background: #f1f5f9;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 16px;
  padding: 4px;
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}
.cr-input-wrap:focus-within {
  background: #ffffff;
  border-color: rgba(37, 99, 235, 0.3);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.08);
}

.cr-input-wrap textarea {
  width: 100%;
  background: none;
  border: none;
  outline: none;
  resize: none;
  padding: 10px 14px;
  font-size: 14px;
  font-family: 'Inter', 'PingFang SC', system-ui, sans-serif;
  color: #1e293b;
  line-height: 1.5;
  min-height: 24px;
  max-height: 120px;
}
.cr-input-wrap textarea::placeholder {
  color: #94a3b8;
}
.cr-input-wrap textarea:disabled {
  opacity: 0.5;
}

/* === Send Button === */
.cr-send-btn {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  border: none;
  background: #2563eb;
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.25s cubic-bezier(0.22, 1, 0.36, 1);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
}
.cr-send-btn:hover:not(:disabled) {
  background: #1d4ed8;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}
.cr-send-btn:active:not(:disabled) {
  transform: scale(0.95);
}
.cr-send-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
  box-shadow: none;
}

/* === Connecting === */
.cr-connecting {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px;
  font-size: 12px;
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.06);
  border-top: 1px solid rgba(245, 158, 11, 0.1);
}

.cr-dot-pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f59e0b;
  animation: crPulse 1.2s ease-in-out infinite;
}
@keyframes crPulse {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
}

/* Scrollbar */
.cr-messages::-webkit-scrollbar {
  width: 5px;
}
.cr-messages::-webkit-scrollbar-track {
  background: transparent;
}
.cr-messages::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}
.cr-messages::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>
