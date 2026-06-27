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
          <el-button v-if="role !== 'COACH'" size="small" type="warning" @click="openPlanDialog">🎯 生成训练计划</el-button>
        </div>
      </div>
    </el-card>

    <!-- 训练计划配置弹窗 -->
    <el-dialog v-model="planDialogVisible" title="🎯 生成训练计划" width="560px" destroy-on-close>
      <el-form v-if="planDialogVisible" :model="planForm" label-position="top">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="训练计划名称">
              <el-input
                v-model="planForm.goal"
                :placeholder="planForm._defaultGoal"
                maxlength="50"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="持续天数">
              <el-input-number
                v-model="planForm.durationDays"
                :min="1"
                :max="120"
                :step="7"
                class="plan-form-number"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始日期">
              <el-date-picker
                v-model="planForm.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="明天"
                class="plan-form-date"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="补充描述（可选）">
              <el-input
                v-model="planForm.description"
                type="textarea"
                :rows="2"
                placeholder="如：侧重下肢训练，避免高冲击动作..."
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 报名状态提示 -->
        <div class="plan-enrollment-hint">
          <template v-if="hasEnrollments === null">查询课程报名中...</template>
          <template v-else-if="hasEnrollments">
            <span class="hint-ok">✅ 已报名 {{ enrollmentCount }} 门课程，AI 将结合课程安排制定计划</span>
          </template>
          <template v-else>
            <span class="hint-warn">⚠️ 你还未报名任何课程</span>
            <div style="margin-top: 4px; font-size: 12px;">建议前往 <strong>选课</strong> 页面报名课程，AI 将结合课程内容生成更合理的训练计划</div>
            <el-button size="small" type="primary" style="margin-top: 8px;" @click="goToCourses">📚 去选课</el-button>
          </template>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="planDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="planGenerating" @click="generatePlan">🎯 生成训练计划</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { generateTrainingPlan, type PlanGenerateRequest } from '../api/agent'
import { getMyEnrollments } from '../api/enrollment'

const userStore = useUserStore()
const router = useRouter()
const userId = userStore.user?.id
const role = userStore.user?.role

const messages = ref<{ role: string; content: string }[]>([])
const inputText = ref('')
const loading = ref(false)
const streamingStarted = ref(false)
const planGenerating = ref(false)
const messagesRef = ref<HTMLElement>()

// Plan dialog
const planDialogVisible = ref(false)
const hasEnrollments = ref<boolean | null>(null)
const enrollmentCount = ref(0)
const planForm = reactive<PlanGenerateRequest & { _defaultGoal: string }>({
  goal: '',
  durationDays: 28,
  startDate: '',
  description: '',
  includeCourseRecommendation: true,
  _defaultGoal: '综合训练',
})

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

async function openPlanDialog() {
  if (!userId) return
  planDialogVisible.value = true
  // Reset form
  planForm.goal = ''
  planForm.durationDays = 28
  planForm.startDate = ''
  planForm.description = ''
  planForm.includeCourseRecommendation = true

  // Check enrollments
  hasEnrollments.value = null
  try {
    const result = await getMyEnrollments(userId, undefined, 1, 50)
    const activeEnrollments = (result.records || []).filter(e => e.status !== 'CANCELLED')
    hasEnrollments.value = activeEnrollments.length > 0
    enrollmentCount.value = activeEnrollments.length
  } catch {
    hasEnrollments.value = false
    enrollmentCount.value = 0
  }
}

function goToCourses() {
  planDialogVisible.value = false
  router.push('/member/public-courses')
}

async function generatePlan() {
  if (!userId || planGenerating.value) return
  planGenerating.value = true
  try {
    const params: PlanGenerateRequest = {}
    if (planForm.goal.trim()) params.goal = planForm.goal.trim()
    params.durationDays = planForm.durationDays
    if (planForm.startDate) params.startDate = planForm.startDate
    if (planForm.description.trim()) params.description = planForm.description.trim()
    params.includeCourseRecommendation = planForm.includeCourseRecommendation

    await generateTrainingPlan(userId, params)
    planDialogVisible.value = false
    messages.value.push({
      role: 'assistant',
      content: '🎉 训练计划已生成！\n\n我已经根据你的需求和健康档案，为你生成了个性化的训练计划。\n\n可以在 **「我的计划」** 页面查看详细内容并开始打卡训练哦~加油！💪'
    })
    scrollToBottom()
  } catch (error) {
    messages.value.push({
      role: 'assistant',
      content: '抱歉，生成训练计划失败：' + (error instanceof Error ? error.message : '请稍后再试') + '\n\n请确保：\n• 已填写健康档案（身高、体重、健身目标等）\n• 已报名至少一门课程'
    })
    scrollToBottom()
  } finally {
    planGenerating.value = false
  }
}
</script>

<style scoped>
.agent-page { display: flex; flex-direction: column; flex: 1; min-height: 0; }
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

/* Plan dialog */
.plan-form-number { width: 100%; }
.plan-form-date { width: 100%; }
.plan-enrollment-hint {
  margin-top: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  background: #f5f7fa;
  font-size: 14px;
}
.hint-ok { color: #67c23a; }
.hint-warn { color: #e6a23c; }

/* Dark theme for plan dialog */
:global([data-admin-theme="dark"]) .plan-enrollment-hint { background: #222536; }
</style>
