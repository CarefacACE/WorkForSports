<template>
  <div class="admin-chat-page">
    <el-card shadow="never" class="chat-card">
      <template #header><span>💬 管理员聊天面板</span></template>

      <div class="chat-layout">
        <!-- 左侧：对话列表 -->
        <div class="conv-list-panel">
          <el-tabs v-model="convTab" @tab-change="onConvTabChange">
            <el-tab-pane label="👥 群聊" name="GROUP" />
            <el-tab-pane label="💬 私聊" name="PRIVATE" />
            <el-tab-pane label="👤 用户列表" name="USERS" />
          </el-tabs>

          <!-- 对话列表 -->
          <template v-if="convTab !== 'USERS'">
            <div class="conv-list" v-loading="convLoading">
              <div
                v-for="conv in filteredConversations"
                :key="conv.id"
                class="conv-item"
                :class="{ active: activeConvId === conv.id }"
                @click="selectConversation(conv)"
              >
                <div class="conv-name">{{ conv.name || `私聊 #${conv.id}` }}</div>
                <div class="conv-meta">
                  <span>{{ conv.memberCount }} 人</span>
                  <span v-if="conv.lastMessage" class="conv-preview">{{ conv.lastMessage }}</span>
                </div>
              </div>
              <el-empty v-if="!convLoading && filteredConversations.length === 0" description="暂无对话" />
            </div>
          </template>

          <!-- 用户列表（用于发起私聊） -->
          <template v-if="convTab === 'USERS'">
            <div class="user-list" v-loading="usersLoading">
              <div
                v-for="user in users"
                :key="user.id"
                class="user-item"
                @click="startPrivateChat(user)"
              >
                <span>{{ user.realName || user.username }}</span>
                <el-tag size="small" :type="user.role === 'ADMIN' ? 'danger' : user.role === 'COACH' ? 'warning' : 'success'">
                  {{ user.role === 'ADMIN' ? '管理员' : user.role === 'COACH' ? '教练' : '会员' }}
                </el-tag>
              </div>
            </div>
          </template>
        </div>

        <!-- 右侧：聊天区 -->
        <div class="chat-area">
          <template v-if="activeConvId">
            <div class="chat-messages" ref="msgBoxRef">
              <div v-if="msgLoading" style="text-align:center;padding:20px;color:#94a3b8;">加载中...</div>
              <template v-for="m in messages" :key="m.id">
                <div class="msg-bubble" :class="{ own: m.senderId === adminId }">
                  <div class="msg-sender">{{ getUserName(m.senderId) }}</div>
                  <div class="msg-text">{{ m.content }}</div>
                  <div class="msg-time">{{ formatMsgTime(m.createTime) }}</div>
                </div>
              </template>
            </div>
            <div class="chat-input">
              <el-input
                v-model="inputText"
                placeholder="输入消息..."
                @keyup.enter="sendMessage"
              >
                <template #append>
                  <el-button @click="sendMessage" :loading="sending">发送</el-button>
                </template>
              </el-input>
            </div>
          </template>
          <div v-else class="chat-empty">
            <div style="color:#94a3b8;text-align:center;padding-top:80px;">
              👈 选择对话或用户开始聊天
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue';
import { ElMessage } from 'element-plus';
import {
  getAllConversationsForAdmin, adminJoinConversation, getAllUsersForAdmin,
  getMessages, type AdminConversation, type AdminUser,
} from '../api/chat';
import request from '../utils/request';
import { useUserStore } from '../stores/user';

const userStore = useUserStore();
const adminId = computed(() => userStore.user?.id || 0);

/* ─── 对话列表 ─── */
const convTab = ref('GROUP');
const convLoading = ref(false);
const conversations = ref<AdminConversation[]>([]);

const filteredConversations = computed(() =>
  conversations.value.filter(c => c.type === convTab.value)
);

async function fetchConversations() {
  convLoading.value = true;
  try {
    conversations.value = await getAllConversationsForAdmin() || [];
  } catch {
    /* ignore */
  }
  convLoading.value = false;
}

function onConvTabChange() {
  if (convTab.value === 'USERS') fetchUsers();
}

/* ─── 用户列表 ─── */
const users = ref<AdminUser[]>([]);
const usersLoading = ref(false);

async function fetchUsers() {
  usersLoading.value = true;
  try {
    users.value = await getAllUsersForAdmin() || [];
  } catch {
    /* ignore */
  }
  usersLoading.value = false;
}

/* ─── 聊天 ─── */
const activeConvId = ref<number | null>(null);
const messages = ref<{ id: number; senderId: number; content: string; msgType: string; createTime: string }[]>([]);
const msgLoading = ref(false);
const inputText = ref('');
const sending = ref(false);
const msgBoxRef = ref<HTMLElement | null>(null);

const userNameMap = ref<Record<number, string>>({});

function getUserName(id: number) {
  return userNameMap.value[id] || `用户 #${id}`;
}

async function selectConversation(conv: AdminConversation) {
  activeConvId.value = conv.id;
  msgLoading.value = true;
  messages.value = [];
  try {
    // 确保管理员是成员
    await adminJoinConversation(conv.id, adminId.value);

    // 加载消息
    const res = await getMessages(conv.id);
    messages.value = (res as any)?.records || [];
  } catch {
    /* ignore */
  }
  msgLoading.value = false;
  await nextTick();
  scrollToBottom();
}

async function startPrivateChat(user: AdminUser) {
  if (user.id === adminId.value) {
    ElMessage.warning('不能和自己私聊');
    return;
  }
  // 查找是否有现有私聊
  const existing = conversations.value.find(
    c => c.type === 'PRIVATE' &&
    c.name && (
      c.name.includes(user.username || '') ||
      c.name.includes(user.realName || '')
    )
  );
  if (existing) {
    selectConversation(existing);
    return;
  }
  // 简化：直接通过 friend/temp-chat 创建
  try {
    const { startTempChat } = await import('../api/chat');
    const conv = await startTempChat(adminId.value, user.id);
    ElMessage.success('私聊已创建');
    await fetchConversations();
    conversations.value.forEach(c => {
      if (c.id === conv.id || (!c.name && c.memberCount === 2)) {
        selectConversation(c);
      }
    });
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败');
  }
}

async function sendMessage() {
  if (!inputText.value.trim() || !activeConvId.value) return;
  sending.value = true;
  try {
    await request.post('/chat/messages', {
      conversationId: activeConvId.value,
      senderId: adminId.value,
      content: inputText.value.trim(),
      msgType: 'TEXT',
    });
    inputText.value = '';
    // 刷新消息
    const res = await getMessages(activeConvId.value);
    messages.value = (res as any)?.records || [];
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '发送失败');
  } finally {
    sending.value = false;
    await nextTick();
    scrollToBottom();
  }
}

function scrollToBottom() {
  if (msgBoxRef.value) {
    msgBoxRef.value.scrollTop = msgBoxRef.value.scrollHeight;
  }
}

function formatMsgTime(t: string) {
  if (!t) return '';
  return t.slice(11, 16);
}

onMounted(fetchConversations);
</script>

<style scoped>
.admin-chat-page { display: flex; flex-direction: column; height: calc(100vh - 120px); }
.chat-card { flex: 1; display: flex; flex-direction: column; }
.chat-card :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; padding: 0; }

.chat-layout { display: flex; flex: 1; overflow: hidden; }
.conv-list-panel { width: 320px; border-right: 1px solid #e5e7eb; display: flex; flex-direction: column; overflow: hidden; }
.conv-list, .user-list { flex: 1; overflow-y: auto; padding: 4px 0; }

.conv-item, .user-item { padding: 10px 16px; cursor: pointer; border-bottom: 1px solid #f1f5f9; transition: background 0.15s; }
.conv-item:hover, .user-item:hover { background: #f8fafc; }
.conv-item.active { background: #eff6ff; border-left: 3px solid #3b82f6; }
.conv-name { font-weight: 600; font-size: 14px; color: #1e293b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv-meta { display: flex; justify-content: space-between; font-size: 12px; color: #94a3b8; margin-top: 2px; }
.conv-preview { max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.user-item { display: flex; justify-content: space-between; align-items: center; }

.chat-area { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.chat-messages { flex: 1; overflow-y: auto; padding: 16px; }
.msg-bubble { margin-bottom: 12px; max-width: 70%; }
.msg-bubble.own { margin-left: auto; text-align: right; }
.msg-sender { font-size: 11px; color: #64748b; }
.msg-text { padding: 8px 14px; border-radius: 12px; background: #f1f5f9; display: inline-block; font-size: 14px; }
.msg-bubble.own .msg-text { background: #3b82f6; color: #fff; }
.msg-time { font-size: 10px; color: #94a3b8; }

.chat-input { padding: 12px 16px; border-top: 1px solid #e5e7eb; }
.chat-empty { flex: 1; display: flex; align-items: center; justify-content: center; }
</style>
