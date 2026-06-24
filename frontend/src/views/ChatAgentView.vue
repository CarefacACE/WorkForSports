<template>
  <div class="agent-page">
    <el-card shadow="never" class="chat-card">
      <template #header>
        <div class="chat-header">
          <span>🤖 智训健身助手</span>
          <el-tag :type="userStore.user?.role === 'COACH' ? 'warning' : 'success'" size="small">
            {{ userStore.user?.role === 'COACH' ? '教练模式' : '会员模式' }}
          </el-tag>
        </div>
      </template>

      <div class="chat-messages" ref="messagesRef">
        <div v-for="(msg, i) in messages" :key="i" :class="['message', msg.role]">
          <div class="message-avatar">
            <span v-if="msg.role === 'user'">👤</span>
            <span v-else>🤖</span>
          </div>
          <div class="message-bubble">
            <div class="message-text" v-html="formatMessage(msg.content)"></div>
          </div>
        </div>
        <div v-if="loading && !streamingStarted" class="message assistant">
          <div class="message-avatar">🤖</div>
          <div class="message-bubble">
            <div class="message-text typing">思考中...</div>
          </div>
        </div>
      </div>

      <div class="chat-input">
        <el-input
          v-model="inputText"
          placeholder="输入消息，如：帮我查看本周课程安排、分析我的运动数据..."
          @keyup.enter="sendMessage"
          :disabled="loading"
          size="large"
        >
          <template #append>
            <el-button type="primary" @click="sendMessage" :loading="loading">发送</el-button>
          </template>
        </el-input>
        <div class="quick-actions">
          <el-button size="small" @click="quickAsk('帮我查看本周的课程安排')">📅 查看课表</el-button>
          <el-button size="small" @click="quickAsk('我的账户余额是多少')">💰 查询余额</el-button>
          <el-button size="small" @click="quickAsk('分析一下我最近的运动数据')">🏃 运动分析</el-button>
          <el-button size="small" @click="quickAsk('根据我的身体状况制定锻炼计划')">📋 锻炼计划</el-button>
          <el-button size="small" @click="quickAsk('我的签到率怎么样')">✅ 签到统计</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const userId = userStore.user?.id
const role = userStore.user?.role

const messages = ref<{ role: string; content: string }[]>([])
const inputText = ref('')
const loading = ref(false)
const streamingStarted = ref(false)
const messagesRef = ref<HTMLElement>()

onMounted(() => {
  messages.value.push({
    role: 'assistant',
    content: `你好！我是智训健身助手 🏋️\n\n我可以帮你：\n• 查看课程和排课安排\n• 查询余额和消费记录\n• 分析运动数据和热量消耗\n• 制定个性化锻炼计划\n• 查看签到出勤情况\n\n有什么可以帮你的？`
  })
})

function formatMessage(text: string): string {
  return text.replace(/\n/g, '<br>').replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

function quickAsk(text: string) {
  inputText.value = text
  sendMessage()
}

function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value || !userId || !role) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  let assistantContent = ''
  let messageCreated = false
  streamingStarted.value = false

  const url = `/api/agent/chat?message=${encodeURIComponent(text)}&userId=${userId}&role=${role}`
  const eventSource = new EventSource(url)

  eventSource.onmessage = (event) => {
    if (event.data === '[DONE]') {
      eventSource.close()
      loading.value = false
      streamingStarted.value = false
      return
    }
    streamingStarted.value = true
    assistantContent += event.data
    if (!messageCreated) {
      messages.value.push({ role: 'assistant', content: assistantContent })
      messageCreated = true
    } else {
      messages.value[messages.value.length - 1].content = assistantContent
    }
    scrollToBottom()
  }

  eventSource.onerror = () => {
    eventSource.close()
    loading.value = false
    streamingStarted.value = false
    if (!assistantContent) {
      messages.value.push({ role: 'assistant', content: '抱歉，服务暂时不可用，请稍后再试。' })
    }
  }
}
</script>

<style scoped>
.agent-page { padding: 0; height: calc(100vh - 140px); }
.chat-card { height: 100%; display: flex; flex-direction: column; }
.chat-card :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; padding: 0; overflow: hidden; }
.chat-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f7fa;
}

.message { display: flex; gap: 10px; margin-bottom: 16px; }
.message.user { flex-direction: row-reverse; }
.message-avatar { font-size: 24px; flex-shrink: 0; }
.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}
.message.assistant .message-bubble { background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.message.user .message-bubble { background: #409eff; color: #fff; }
.typing { color: #909399; }

.chat-input { padding: 12px 16px; border-top: 1px solid #ebeef5; }
.quick-actions { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 4px; }
</style>
