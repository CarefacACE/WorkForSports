<template>
  <view class="page">
    <!-- 消息列表 -->
    <scroll-view
      class="msg-list"
      scroll-y
      :scroll-with-animation="true"
      :scroll-into-view="scrollToId"
    >
      <view v-if="messages.length === 0 && !loading" class="empty">
        <text class="empty-icon">💬</text>
        <text class="empty-text">暂无消息</text>
      </view>

      <view
        v-for="msg in messages"
        :key="msg.id"
        :id="'msg-' + msg.id"
        class="msg-row"
        :class="{ mine: msg.senderId === userId }"
      >
        <view class="msg-bubble" :class="{ mine: msg.senderId === userId }">
          <text class="msg-text">{{ msg.content }}</text>
        </view>
        <text class="msg-time">{{ msg.createTime?.slice(11, 16) }}</text>
      </view>
    </scroll-view>

    <!-- 输入区 -->
    <view class="input-bar">
      <input
        v-model="text"
        class="msg-input"
        placeholder="输入消息..."
        placeholder-style="color:#bbb"
        confirm-type="send"
        :disabled="sending"
        @confirm="sendMsg"
      />
      <button class="btn-send" :disabled="!text.trim() || sending" @tap="sendMsg">
        <text>{{ sending ? '...' : '发送' }}</text>
      </button>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { getMessages, type ChatMessage } from '@/api/chat'
import { connectChat, closeChat } from '@/utils/websocket'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userId = userStore.userId

const text = ref('')
const sending = ref(false)
const loading = ref(false)
const messages = ref<ChatMessage[]>([])
const scrollToId = ref('')
const convId = ref(0)

onMounted(async () => {
  const pages = getCurrentPages()
  const opts = (pages[pages.length - 1] as unknown as { options: Record<string, string> }).options
  convId.value = Number(opts.id)

  // 设置标题
  uni.setNavigationBarTitle({ title: decodeURIComponent(opts.name || '聊天') })

  // 加载历史消息
  loading.value = true
  try {
    messages.value = (await getMessages(convId.value, 1, 50)).records.reverse()
  } catch {
    // 静默
  } finally {
    loading.value = false
  }

  // 建立 WebSocket 连接
  if (userId) {
    connectChat(userId, {
      onMessage: (data: unknown) => {
        const msg = data as ChatMessage
        if (msg.conversationId === convId.value) {
          messages.value.push(msg)
          scrollToBottom()
        }
      },
    })
  }

  scrollToBottom()
})

onUnmounted(() => {
  closeChat()
})

function scrollToBottom() {
  if (messages.value.length === 0) return
  scrollToId.value = ''
  nextTick(() => {
    scrollToId.value = 'msg-' + messages.value[messages.value.length - 1].id
  })
}

function sendMsg() {
  if (!text.value.trim() || sending.value || !userId) return
  // Chat is handled by WebSocket; actual send logic via backend STOMP
  sending.value = true
  setTimeout(() => {
    text.value = ''
    sending.value = false
  }, 300)
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f5f6fa;
  display: flex;
  flex-direction: column;
}

.msg-list {
  flex: 1;
  padding: 16rpx 24rpx;
  overflow-y: auto;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 200rpx 0;
}
.empty-icon { font-size: 72rpx; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: #999; }

.msg-row {
  margin-bottom: 24rpx;
  display: flex;
  flex-direction: column;
  align-items: flex-start;

  &.mine { align-items: flex-end; }
}

.msg-bubble {
  max-width: 80%;
  padding: 16rpx 24rpx;
  border-radius: 16rpx;
  background: #fff;
  font-size: 28rpx;
  color: #333;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
  word-break: break-all;

  &.mine {
    background: linear-gradient(135deg, #2563eb, #1d4ed8);
    color: #fff;
  }
}

.msg-time {
  font-size: 20rpx;
  color: #bbb;
  margin-top: 4rpx;
}

.input-bar {
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 2rpx solid #f0f2f5;
}

.msg-input {
  flex: 1;
  height: 72rpx;
  border: 2rpx solid #e5e7eb;
  border-radius: 36rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #333;
  margin-right: 16rpx;
  background: #f9fafb;
}

.btn-send {
  height: 72rpx;
  padding: 0 32rpx;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  border-radius: 36rpx;
  font-size: 26rpx;
  border: none;
  display: flex;
  align-items: center;
}
</style>
