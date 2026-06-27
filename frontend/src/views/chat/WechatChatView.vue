<template>
  <div class="wechat-layout">
    <!-- ====== 左侧：会话列表 ====== -->
    <div class="wc-sidebar">
      <!-- 搜索框 -->
      <div class="wc-search-bar">
        <div class="wc-search-inner">
          <svg class="wc-search-icon" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input v-model="searchKeyword" placeholder="搜索" class="wc-search-input" />
        </div>
        <button v-if="isGroupPage" class="wc-add-btn" @click="showCreateDialog = true" title="新建群聊">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </button>
      </div>

      <!-- 会话列表 -->
      <div class="wc-conv-list" ref="listRef">
        <div v-if="filteredConversations.length === 0 && !loading" class="wc-empty">暂无{{ isGroupPage ? '群聊' : '私信' }}</div>
        <div
          v-for="conv in filteredConversations"
          :key="conv.id"
          class="wc-conv-item"
          :class="{ active: selectedId === conv.id }"
          @click="selectConversation(conv)"
        >
          <div class="wc-avatar" :class="{ group: conv.type === 'GROUP' }">
            <img v-if="conv.avatar" :src="conv.avatar" />
            <span v-else>{{ (conv.displayName || '?')[0] }}</span>
          </div>
          <div class="wc-conv-info">
            <div class="wc-conv-top">
              <span class="wc-conv-name">{{ conv.displayName || (isGroupPage ? '未命名群聊' : '私信') }}</span>
              <span class="wc-conv-time">{{ formatListTime(conv.lastTime || conv.createTime) }}</span>
            </div>
            <div class="wc-conv-bottom">
              <span class="wc-conv-preview">{{ conv.lastMessage || '暂无消息' }}</span>
              <span v-if="conv.unreadCount > 0" class="wc-badge">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ====== 右侧：聊天主区域 ====== -->
    <div class="wc-main">
      <template v-if="selectedId">
        <!-- 聊天头部 -->
        <div class="wc-chat-header">
          <div class="wc-chat-header-left">
            <span class="wc-chat-title">{{ selectedName }}</span>
            <span v-if="isGroupPage" class="wc-member-count">{{ groupMemberCount }} 人</span>
          </div>
          <div class="wc-chat-header-right">
            <el-dropdown v-if="selectedId" trigger="click" @command="handleChatCommand">
              <button class="wc-header-btn">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="5" r="1.5"/><circle cx="12" cy="12" r="1.5"/><circle cx="12" cy="19" r="1.5"/></svg>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <template v-if="isGroupPage">
                    <el-dropdown-item command="group-info">群聊信息</el-dropdown-item>
                    <el-dropdown-item command="leave" divided>退出群聊</el-dropdown-item>
                  </template>
                  <template v-else>
                    <el-dropdown-item command="block">拉黑</el-dropdown-item>
                    <el-dropdown-item command="deleteFriend" divided>删除好友</el-dropdown-item>
                  </template>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <!-- 聊天消息区 -->
        <div class="wc-chat-body" :class="{ 'with-panel': showGroupInfo && isGroupPage }">
          <ChatRoom
            :conversationId="selectedId"
            :conversationName="selectedName"
            :currentUserId="currentUserId"
            @message-sent="onMessageSent"
          />
          <!-- 群信息侧边面板 -->
          <GroupInfoPanel
            v-if="showGroupInfo && isGroupPage"
            :conversationId="selectedId"
            :conversationName="selectedName"
            :ownerId="selectedOwnerId"
            :currentUserId="currentUserId"
            @close="showGroupInfo = false"
            @left="handleLeft"
            @nameChanged="handleNameChanged"
          />
        </div>
      </template>

      <!-- 空状态 -->
      <div v-else class="wc-chat-empty">
        <div class="wc-empty-icon">
          <svg viewBox="0 0 80 80" width="80" height="80" fill="none">
            <rect x="10" y="15" width="60" height="50" rx="8" stroke="#cbd5e1" stroke-width="2"/>
            <circle cx="30" cy="40" r="3" fill="#cbd5e1"/>
            <circle cx="40" cy="40" r="3" fill="#cbd5e1"/>
            <circle cx="50" cy="40" r="3" fill="#cbd5e1"/>
            <path d="M20 25h40" stroke="#cbd5e1" stroke-width="1.5"/>
          </svg>
        </div>
        <div class="wc-empty-text">{{ isGroupPage ? '选择一个群聊开始对话' : '选择一个私信开始对话' }}</div>
      </div>
    </div>

    <!-- ====== 新建群聊弹窗（仅群聊页面） ====== -->
    <el-dialog v-if="isGroupPage" v-model="showCreateDialog" title="新建群聊" width="480px">
      <el-form label-position="top">
        <el-form-item label="群聊名称" required>
          <el-input v-model="newGroupName" placeholder="请输入群聊名称" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="搜索添加成员">
          <el-input v-model="memberSearchKeyword" placeholder="输入用户名或ID搜索" clearable @keyup.enter="handleMemberSearch">
            <template #append>
              <el-button @click="handleMemberSearch">搜索</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="memberSearchResult" label="搜索结果">
          <div class="wc-search-result">
            <span>{{ memberSearchResult.realName || memberSearchResult.username }}</span>
            <el-tag size="small">{{ memberSearchResult.role === 'COACH' ? '教练' : '会员' }}</el-tag>
            <el-button size="small" type="primary" link @click="addMemberToList" :disabled="isMemberInList">
              {{ isMemberInList ? '已添加' : '添加' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="已选成员">
          <div class="wc-member-tags">
            <el-tag v-for="m in selectedMembers" :key="m.id" closable @close="removeMemberFromList(m.id)" size="large" round>
              {{ m.realName || m.username }}
            </el-tag>
            <span v-if="selectedMembers.length === 0" class="wc-dim-text">暂未添加成员</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateGroup" :disabled="!newGroupName.trim()" :loading="creating">
          创建群聊
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getConversationsByType, getUnreadConversations, markConversationRead,
  getGroupMembers, deleteFriend, blockUser, leaveGroup,
  createConversation, searchUser, searchUserById, updateConversationName,
  type ChatConversation, type UnreadConversation, type GroupMember, type SearchResult,
} from '../../api/chat';
import ChatRoom from './ChatRoom.vue';
import GroupInfoPanel from './GroupInfoPanel.vue';

const props = defineProps<{
  /** 'GROUP' | 'PRIVATE' */
  chatType: 'GROUP' | 'PRIVATE';
}>();

const route = useRoute();
const isGroupPage = computed(() => props.chatType === 'GROUP');

/* ─── 会话列表 ─── */
const loading = ref(false);
const conversations = ref<ChatConversation[]>([]);
const unreadMap = ref<Record<number, UnreadConversation>>({});
const selectedId = ref<number>(0);
const selectedName = ref('');
const selectedOwnerId = ref<number | null>(null);
const currentUserId = ref(0);
const searchKeyword = ref('');
const showGroupInfo = ref(false);
const groupMemberCount = ref(0);

// 合并对话列表和未读信息
interface MergedConversation {
  id: number;
  type: string;
  name: string | null;
  displayName: string;
  ownerId: number | null;
  createTime: string;
  lastMessage: string;
  lastTime: string;
  unreadCount: number;
  lastSenderId: number;
  avatar?: string;
}

const mergedConversations = computed<MergedConversation[]>(() => {
  return conversations.value.map(conv => {
    const unread = unreadMap.value[conv.id];
    return {
      id: conv.id,
      type: conv.type,
      name: conv.name,
      displayName: (unread?.conversationName) || conv.name || (props.chatType === 'PRIVATE' ? '私信' : '未命名群聊'),
      ownerId: conv.ownerId,
      createTime: conv.createTime,
      lastMessage: unread?.lastMessage || '',
      lastTime: unread?.lastTime || conv.createTime || '',
      unreadCount: unread?.unreadCount || 0,
      lastSenderId: unread?.lastSenderId || 0,
    };
  }).sort((a, b) => {
    // 按最后消息时间降序排列（微信风格）
    if (!a.lastTime) return 1;
    if (!b.lastTime) return -1;
    return b.lastTime.localeCompare(a.lastTime);
  });
});

const filteredConversations = computed(() => {
  if (!searchKeyword.value.trim()) return mergedConversations.value;
  const kw = searchKeyword.value.trim().toLowerCase();
  return mergedConversations.value.filter(c =>
    c.displayName.toLowerCase().includes(kw)
  );
});

function formatListTime(timeStr: string): string {
  if (!timeStr) return '';
  const d = new Date(timeStr);
  const now = new Date();
  const isToday = d.toDateString() === now.toDateString();
  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  const isYesterday = d.toDateString() === yesterday.toDateString();

  if (isToday) {
    return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
  }
  if (isYesterday) return '昨天';
  // 本周内
  const diffDays = Math.floor((now.getTime() - d.getTime()) / 86400000);
  if (diffDays < 7) {
    const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    return weekdays[d.getDay()];
  }
  return `${d.getMonth() + 1}/${d.getDate()}`;
}

/* ─── WebSocket 轮询未读数 ─── */
let pollTimer: ReturnType<typeof setInterval> | null = null;

async function loadConversations() {
  loading.value = true;
  try {
    conversations.value = await getConversationsByType(currentUserId.value, props.chatType);
  } catch (e) {
    console.error('加载对话列表失败', e);
  }
  loading.value = false;
}

async function loadUnreadInfo() {
  if (!currentUserId.value) return;
  try {
    const list = await getUnreadConversations(currentUserId.value);
    const map: Record<number, UnreadConversation> = {};
    for (const item of list) {
      if (item.conversationType === props.chatType) {
        map[item.conversationId] = item;
      }
    }
    unreadMap.value = map;
  } catch {
    // ignore
  }
}

async function refreshAll() {
  await Promise.all([loadConversations(), loadUnreadInfo()]);
}

function selectConversation(conv: MergedConversation) {
  selectedId.value = conv.id;
  selectedName.value = conv.displayName;
  selectedOwnerId.value = conv.ownerId;
  showGroupInfo.value = false;

  // 加载群成员数
  if (isGroupPage.value) {
    getGroupMembers(conv.id).then(m => { groupMemberCount.value = m.length; }).catch(() => {});
  } else {
    groupMemberCount.value = 2;
  }

  // 清除未读
  if (conv.unreadCount > 0 && currentUserId.value) {
    unreadMap.value[conv.id] = { ...unreadMap.value[conv.id], unreadCount: 0 } as any;
    markConversationRead(conv.id, currentUserId.value).catch(() => {});
  }
}

async function onMessageSent() {
  await loadUnreadInfo();
}

/* ─── 操作菜单 ─── */
async function handleChatCommand(command: string) {
  if (command === 'group-info') {
    showGroupInfo.value = !showGroupInfo.value;
    return;
  }
  if (command === 'leave') {
    try {
      await ElMessageBox.confirm('确定退出该群聊？', '提示', { type: 'warning' });
      await leaveGroup(selectedId.value, currentUserId.value);
      ElMessage.success('已退出群聊');
      selectedId.value = 0;
      selectedName.value = '';
      await loadConversations();
    } catch { /* cancel */ }
    return;
  }
  if (command === 'block' || command === 'deleteFriend') {
    // 获取对方 userId
    try {
      const members = await getGroupMembers(selectedId.value);
      const other = members.find(m => m.userId !== currentUserId.value);
      if (!other) { ElMessage.warning('无法获取对方信息'); return; }

      if (command === 'block') {
        await ElMessageBox.confirm(`确定拉黑 ${other.realName || other.username}？`, '拉黑', { type: 'warning' });
        await blockUser(currentUserId.value, other.userId);
        ElMessage.success('已拉黑');
      } else {
        await ElMessageBox.confirm(`确定删除与 ${other.realName || other.username} 的好友关系？`, '删除好友', { type: 'warning' });
        await deleteFriend(currentUserId.value, other.userId);
        ElMessage.success('已删除好友');
      }
      selectedId.value = 0;
      selectedName.value = '';
      await loadConversations();
    } catch (e: any) {
      if (e?.toString() !== 'cancel') {
        ElMessage.error(e instanceof Error ? e.message : '操作失败');
      }
    }
  }
}

function handleLeft() {
  selectedId.value = 0;
  selectedName.value = '';
  selectedOwnerId.value = null;
  showGroupInfo.value = false;
  loadConversations();
}

function handleNameChanged(name: string) {
  selectedName.value = name;
  const conv = conversations.value.find(c => c.id === selectedId.value);
  if (conv) conv.name = name;
}

/* ─── 新建群聊 ─── */
const showCreateDialog = ref(false);
const newGroupName = ref('');
const memberSearchKeyword = ref('');
const memberSearchResult = ref<SearchResult | null>(null);
const selectedMembers = ref<SearchResult[]>([]);
const creating = ref(false);

const isMemberInList = computed(() =>
  memberSearchResult.value ? selectedMembers.value.some(m => m.id === memberSearchResult.value!.id) : false
);

async function handleMemberSearch() {
  if (!memberSearchKeyword.value.trim()) return;
  try {
    const kw = memberSearchKeyword.value.trim();
    memberSearchResult.value = /^\d+$/.test(kw) ? await searchUserById(Number(kw)) : await searchUser(kw);
  } catch {
    memberSearchResult.value = null;
    ElMessage.error('用户不存在');
  }
}

function addMemberToList() {
  if (!memberSearchResult.value || isMemberInList.value) return;
  selectedMembers.value.push(memberSearchResult.value);
  memberSearchResult.value = null;
  memberSearchKeyword.value = '';
}

function removeMemberFromList(id: number) {
  selectedMembers.value = selectedMembers.value.filter(m => m.id !== id);
}

async function handleCreateGroup() {
  if (!newGroupName.value.trim()) return;
  creating.value = true;
  try {
    const conv = await createConversation({
      type: 'GROUP',
      name: newGroupName.value.trim(),
      ownerId: currentUserId.value,
      memberIds: selectedMembers.value.map(m => m.id),
    });
    ElMessage.success('群聊创建成功');
    showCreateDialog.value = false;
    newGroupName.value = '';
    selectedMembers.value = [];
    memberSearchKeyword.value = '';
    memberSearchResult.value = null;
    await loadConversations();
    // 直接选中新创建的群聊
    selectedId.value = conv.id;
    selectedName.value = conv.name || '未命名群聊';
    selectedOwnerId.value = conv.ownerId;
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败');
  } finally {
    creating.value = false;
  }
}

/* ─── 路由参数处理 ─── */
watch(() => route.query.conversationId, async (newId) => {
  if (newId && conversations.value.length > 0) {
    const conv = conversations.value.find(c => c.id === Number(newId));
    if (conv) {
      const merged = mergedConversations.value.find(m => m.id === conv.id);
      if (merged) selectConversation(merged);
    }
  }
});

onMounted(async () => {
  const userInfo = JSON.parse(localStorage.getItem('user_info') || '{}');
  currentUserId.value = userInfo.id || 0;
  if (!currentUserId.value) return;

  await refreshAll();

  // 处理路由参数
  const queryConvId = Number(route.query.conversationId);
  if (queryConvId) {
    const conv = conversations.value.find(c => c.id === queryConvId);
    if (conv) {
      const merged = mergedConversations.value.find(m => m.id === conv.id);
      if (merged) selectConversation(merged);
    }
  }

  // 轮询未读消息（15秒一次）
  pollTimer = setInterval(loadUnreadInfo, 15000);
});

onBeforeUnmount(() => {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
});
</script>

<style scoped>
/* ============================================
   WECHAT-STYLE CHAT LAYOUT
   ============================================ */
.wechat-layout {
  display: flex;
  flex-direction: row !important;
  flex: 1;
  min-height: 0;
  margin: -24px;
  background: #f5f5f5;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;
}

/* ====== 左侧边栏 ====== */
.wc-sidebar {
  width: 300px;
  min-width: 300px;
  background: #e8e8e8;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #d6d6d6;
}

/* 搜索框 */
.wc-search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  background: #f0f0f0;
  border-bottom: 1px solid #d9d9d9;
}

.wc-search-inner {
  flex: 1;
  display: flex;
  align-items: center;
  background: #e0e0e0;
  border-radius: 6px;
  padding: 6px 10px;
  gap: 6px;
  transition: background 0.2s;
}
.wc-search-inner:focus-within {
  background: #fff;
  outline: 1px solid #07c160;
}

.wc-search-icon {
  color: #999;
  flex-shrink: 0;
}

.wc-search-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 13px;
  color: #333;
}
.wc-search-input::placeholder {
  color: #999;
}

.wc-add-btn {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: #e0e0e0;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.15s;
}
.wc-add-btn:hover {
  background: #07c160;
  color: #fff;
}

/* 会话列表 */
.wc-conv-list {
  flex: 1;
  overflow-y: auto;
  background: #e8e8e8;
}
.wc-conv-list::-webkit-scrollbar { width: 4px; }
.wc-conv-list::-webkit-scrollbar-track { background: transparent; }
.wc-conv-list::-webkit-scrollbar-thumb { background: #c0c0c0; border-radius: 2px; }

.wc-empty {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 14px;
}

.wc-conv-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: background 0.1s;
  position: relative;
}
.wc-conv-item:hover {
  background: #dedede;
}
.wc-conv-item.active {
  background: #c9c9c9;
}

/* 头像 */
.wc-avatar {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  background: linear-gradient(135deg, #07c160, #06ae56);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
  overflow: hidden;
}
.wc-avatar.group {
  border-radius: 8px;
  background: linear-gradient(135deg, #576b95, #4a5c7f);
}
.wc-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 会话信息 */
.wc-conv-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.wc-conv-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
.wc-conv-name {
  font-size: 15px;
  font-weight: 500;
  color: #191919;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 140px;
}
.wc-conv-time {
  font-size: 11px;
  color: #999;
  flex-shrink: 0;
  margin-left: 4px;
}
.wc-conv-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.wc-conv-preview {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 170px;
}

/* 未读角标（微信绿） */
.wc-badge {
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  background: #fa5151;
  color: #fff;
  font-size: 11px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
  flex-shrink: 0;
}

/* ====== 右侧主区域 ====== */
.wc-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #f5f5f5;
}

/* 聊天头部 */
.wc-chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: #f0f0f0;
  border-bottom: 1px solid #d9d9d9;
  height: 56px;
  flex-shrink: 0;
}
.wc-chat-header-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.wc-chat-title {
  font-size: 17px;
  font-weight: 600;
  color: #191919;
}
.wc-member-count {
  font-size: 12px;
  color: #999;
}
.wc-chat-header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}
.wc-header-btn {
  width: 34px;
  height: 34px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}
.wc-header-btn:hover {
  background: #e0e0e0;
}

/* 聊天内容区 */
.wc-chat-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}
.wc-chat-body.with-panel {
  /* ChatRoom 和 GroupInfoPanel 并排 */
}

/* 空状态 */
.wc-chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: #f5f5f5;
}
.wc-empty-text {
  font-size: 14px;
  color: #b0b0b0;
}

/* ====== 新建群聊弹窗 ====== */
.wc-search-result {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: #f5f5f5;
  border-radius: 8px;
}
.wc-member-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.wc-dim-text {
  color: #999;
  font-size: 13px;
}

/* ====== 暗色主题 ====== */
:global([data-admin-theme="dark"]) .wc-sidebar {
  background: #1a1c28;
  border-right-color: rgba(255,255,255,0.06);
}
:global([data-admin-theme="dark"]) .wc-search-bar {
  background: #222536;
  border-bottom-color: rgba(255,255,255,0.06);
}
:global([data-admin-theme="dark"]) .wc-search-inner {
  background: #2a2d42;
}
:global([data-admin-theme="dark"]) .wc-search-inner:focus-within {
  background: #2a2d42;
  outline-color: rgba(91,124,247,0.4);
}
:global([data-admin-theme="dark"]) .wc-search-input {
  color: #e8eaed;
}
:global([data-admin-theme="dark"]) .wc-search-input::placeholder {
  color: #4a4e63;
}
:global([data-admin-theme="dark"]) .wc-add-btn {
  background: #2a2d42;
  color: #7a7f96;
}
:global([data-admin-theme="dark"]) .wc-add-btn:hover {
  background: #5b7cf7;
}
:global([data-admin-theme="dark"]) .wc-conv-list {
  background: #1a1c28;
}
:global([data-admin-theme="dark"]) .wc-conv-item:hover {
  background: #222536;
}
:global([data-admin-theme="dark"]) .wc-conv-item.active {
  background: #282a3a;
}
:global([data-admin-theme="dark"]) .wc-conv-name {
  color: #e8eaed;
}
:global([data-admin-theme="dark"]) .wc-conv-preview {
  color: #6b7084;
}
:global([data-admin-theme="dark"]) .wc-conv-time {
  color: #4a4e63;
}
:global([data-admin-theme="dark"]) .wc-chat-header {
  background: #222536;
  border-bottom-color: rgba(255,255,255,0.06);
}
:global([data-admin-theme="dark"]) .wc-chat-title {
  color: #f0f0f5;
}
:global([data-admin-theme="dark"]) .wc-chat-empty {
  background: #0b0d14;
}
:global([data-admin-theme="dark"]) .wc-chat-body {
  background: #0b0d14;
}
</style>
