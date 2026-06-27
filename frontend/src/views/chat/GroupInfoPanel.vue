<template>
  <div class="group-info-panel">
    <div class="panel-header">
      <span>群聊信息</span>
      <el-button text @click="$emit('close')">
        <el-icon><Close /></el-icon>
      </el-button>
    </div>

    <div class="panel-body">
      <div class="info-section">
        <div class="section-title">群聊名称</div>
        <div class="name-row" v-if="!editingName">
          <span class="group-name">{{ conversationName }}</span>
          <el-button v-if="isOwner" type="primary" link size="small" @click="startEditName">编辑</el-button>
        </div>
        <div class="name-row" v-else>
          <el-input v-model="newName" size="small" style="flex:1" />
          <el-button type="primary" size="small" @click="saveName">保存</el-button>
          <el-button size="small" @click="editingName = false">取消</el-button>
        </div>
      </div>

      <div class="info-section">
        <div class="section-title">群公告</div>
        <div v-if="groupNotice" class="notice-content">
          <div class="notice-text">{{ groupNotice.content }}</div>
          <div class="notice-actions">
            <el-button v-if="isOwner" type="danger" link size="small" @click="handleDeleteNotice">删除</el-button>
          </div>
        </div>
        <div v-else class="notice-empty">
          <span>暂无公告</span>
          <el-button v-if="isOwner" type="primary" link size="small" @click="showNoticeDialog = true">发布公告</el-button>
        </div>
      </div>

      <div class="info-section">
        <div class="section-title">成员 ({{ members.length }})</div>
        <div class="member-actions" v-if="isOwner">
          <el-input v-model="addMemberKeyword" placeholder="输入用户名搜索" size="small" clearable
            @keyup.enter="handleSearchUser" style="flex:1">
          </el-input>
          <el-button size="small" @click="handleSearchUser">添加</el-button>
          <el-button size="small" type="warning" @click="handleMuteAll">全员禁言</el-button>
          <el-button size="small" type="success" @click="handleUnmuteAll">解除禁言</el-button>
        </div>
        <div v-if="searchUserResult" class="search-result">
          <span>{{ searchUserResult.realName || searchUserResult.username }}</span>
          <el-button size="small" type="primary" link @click="handleAddMember">添加</el-button>
        </div>
        <div class="member-list">
          <div v-for="member in members" :key="member.userId" class="member-item">
            <div class="member-info">
              <span class="member-name">
                {{ member.nickname || member.realName || member.username }}
                <span v-if="member.nickname" class="member-original-name">({{ member.realName || member.username }})</span>
              </span>
              <el-tag v-if="member.isOwner" size="small" type="warning">群主</el-tag>
              <el-tag v-if="member.userId === currentUserId" size="small" type="info">我</el-tag>
              <el-tag v-if="member.isMuted === 1" size="small" type="danger">禁言</el-tag>
            </div>
            <div class="member-actions-group">
              <!-- 群主专属操作 -->
              <template v-if="isOwner && member.userId !== ownerId">
                <el-button type="primary" link size="small" @click="handleEditNickname(member)">改昵称</el-button>
                <el-button v-if="member.isMuted === 1" type="warning" link size="small" @click="handleUnmute(member)">取消禁言</el-button>
                <el-button v-else type="warning" link size="small" @click="handleMute(member)">禁言</el-button>
                <el-button type="danger" link size="small" @click="handleRemoveMember(member.userId)">移除</el-button>
              </template>
              <!-- 普通成员操作 -->
              <template v-if="member.userId !== currentUserId">
                <el-button type="success" link size="small" @click="handleAddFriend(member)">加好友</el-button>
                <el-button type="primary" link size="small" @click="handleTempChat(member)">临时私信</el-button>
              </template>
            </div>
          </div>
        </div>
      </div>

      <div class="info-section">
        <el-button type="danger" plain @click="handleLeaveGroup" style="width: 100%">退出群聊</el-button>
      </div>
    </div>

    <el-dialog v-model="showNoticeDialog" title="发布公告" width="400px">
      <el-input v-model="noticeContent" type="textarea" :rows="4" placeholder="请输入公告内容" />
      <template #footer>
        <el-button @click="showNoticeDialog = false">取消</el-button>
        <el-button type="primary" @click="handlePublishNotice" :disabled="!noticeContent.trim()">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Close } from '@element-plus/icons-vue';
import { getGroupMembers, addGroupMember, removeGroupMember, updateConversationName, leaveGroup, searchUser, searchUserById, getGroupNotice, publishGroupNotice, deleteGroupNotice, sendFriendRequest, startTempChat, setMemberNickname, muteMember, unmuteMember, muteAllMembers, unmuteAllMembers, type GroupMember, type SearchResult, type GroupNotice } from '../../api/chat';

const props = defineProps<{
  conversationId: number;
  conversationName: string;
  ownerId: number | null;
  currentUserId: number;
}>();

const emit = defineEmits<{
  close: [];
  left: [];
  nameChanged: [name: string];
}>();

const members = ref<GroupMember[]>([]);
const editingName = ref(false);
const newName = ref('');
const addMemberKeyword = ref('');
const searchUserResult = ref<SearchResult | null>(null);
const groupNotice = ref<GroupNotice | null>(null);
const showNoticeDialog = ref(false);
const noticeContent = ref('');

const isOwner = computed(() => props.ownerId === props.currentUserId);

function startEditName() {
  newName.value = props.conversationName;
  editingName.value = true;
}

async function saveName() {
  if (!newName.value.trim()) return;
  try {
    await updateConversationName(props.conversationId, newName.value.trim());
    ElMessage.success('修改成功');
    editingName.value = false;
    emit('nameChanged', newName.value.trim());
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '修改失败');
  }
}

async function loadMembers() {
  try {
    members.value = await getGroupMembers(props.conversationId);
  } catch {}
}

async function loadNotice() {
  try {
    groupNotice.value = await getGroupNotice(props.conversationId);
  } catch {
    groupNotice.value = null;
  }
}

async function handlePublishNotice() {
  if (!noticeContent.value.trim()) return;
  try {
    await publishGroupNotice(props.conversationId, props.currentUserId, noticeContent.value.trim());
    ElMessage.success('公告发布成功');
    showNoticeDialog.value = false;
    noticeContent.value = '';
    loadNotice();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发布失败');
  }
}

async function handleDeleteNotice() {
  if (!groupNotice.value) return;
  try {
    await ElMessageBox.confirm('确定删除该公告？', '提示', { type: 'warning' });
    await deleteGroupNotice(groupNotice.value.id);
    ElMessage.success('公告已删除');
    groupNotice.value = null;
  } catch {}
}

async function handleSearchUser() {
  if (!addMemberKeyword.value.trim()) return;
  try {
    const keyword = addMemberKeyword.value.trim();
    if (/^\d+$/.test(keyword)) {
      searchUserResult.value = await searchUserById(Number(keyword));
    } else {
      searchUserResult.value = await searchUser(keyword);
    }
  } catch {
    searchUserResult.value = null;
    ElMessage.error('用户不存在');
  }
}

async function handleAddMember() {
  if (!searchUserResult.value) return;
  try {
    await addGroupMember(props.conversationId, searchUserResult.value.id);
    ElMessage.success('添加成功');
    searchUserResult.value = null;
    addMemberKeyword.value = '';
    loadMembers();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '添加失败');
  }
}

async function handleRemoveMember(userId: number) {
  try {
    await ElMessageBox.confirm('确定移除该成员？', '提示', { type: 'warning' });
    await removeGroupMember(props.conversationId, userId);
    ElMessage.success('已移除');
    loadMembers();
  } catch {}
}

async function handleLeaveGroup() {
  try {
    await ElMessageBox.confirm('确定退出该群聊？', '提示', { type: 'warning' });
    await leaveGroup(props.conversationId, props.currentUserId);
    ElMessage.success('已退出群聊');
    emit('left');
  } catch {}
}

/* ─── 群管理：禁言 / 昵称 ─── */

async function handleEditNickname(member: GroupMember) {
  try {
    const { value } = await ElMessageBox.prompt(
      `设置 ${member.realName || member.username} 的群昵称`,
      '修改群昵称',
      { inputValue: member.nickname || '', confirmButtonText: '确定', cancelButtonText: '取消' }
    );
    await setMemberNickname(props.conversationId, member.userId, props.currentUserId, value || '');
    ElMessage.success('修改成功');
    loadMembers();
  } catch { /* cancel */ }
}

async function handleMute(member: GroupMember) {
  try {
    const { value } = await ElMessageBox.prompt(
      `输入禁言时长（分钟），留空为永久禁言`,
      `禁言 ${member.realName || member.username}`,
      { inputType: 'number', inputPlaceholder: '分钟数，留空为永久', confirmButtonText: '确定禁言', cancelButtonText: '取消' }
    );
    const duration = value ? Number(value) : undefined;
    await muteMember(props.conversationId, member.userId, props.currentUserId, duration);
    ElMessage.success('已禁言');
    loadMembers();
  } catch { /* cancel */ }
}

async function handleUnmute(member: GroupMember) {
  try {
    await ElMessageBox.confirm(`确定取消 ${member.realName || member.username} 的禁言？`, '取消禁言', { type: 'info' });
    await unmuteMember(props.conversationId, member.userId, props.currentUserId);
    ElMessage.success('已取消禁言');
    loadMembers();
  } catch { /* cancel */ }
}

/* ─── 加好友 / 临时私信 ─── */

async function handleAddFriend(member: GroupMember) {
  try {
    await sendFriendRequest(props.currentUserId, member.userId, 'FRIEND', `来自群聊的好友申请`);
    ElMessage.success('好友申请已发送');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败');
  }
}

async function handleTempChat(member: GroupMember) {
  try {
    const conv = await startTempChat(props.currentUserId, member.userId);
    // 跳转到私信页面
    const basePath = JSON.parse(localStorage.getItem('user_info') || '{}').role === 'COACH' ? '/coach' : '/member';
    window.open(`${basePath}/chat-private?conversationId=${conv.id}`, '_blank');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败');
  }
}

/* ─── 全员禁言 ─── */

async function handleMuteAll() {
  try {
    await ElMessageBox.confirm('确定开启全员禁言？', '全员禁言', { type: 'warning', confirmButtonText: '确定' });
    await muteAllMembers(props.conversationId, props.currentUserId);
    ElMessage.success('已开启全员禁言');
    loadMembers();
  } catch { /* cancel */ }
}

async function handleUnmuteAll() {
  try {
    await ElMessageBox.confirm('确定解除全员禁言？', '解除禁言', { type: 'info', confirmButtonText: '确定' });
    await unmuteAllMembers(props.conversationId, props.currentUserId);
    ElMessage.success('已关闭全员禁言');
    loadMembers();
  } catch { /* cancel */ }
}

watch(() => props.conversationId, () => {
  if (props.conversationId) {
    loadMembers();
    loadNotice();
  }
});

onMounted(() => {
  if (props.conversationId) {
    loadMembers();
    loadNotice();
  }
});
</script>

<style scoped>
.group-info-panel {
  width: 320px;
  border-left: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

.panel-header {
  padding: 16px 20px;
  font-weight: 700;
  font-size: 15px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  background: rgba(255, 255, 255, 0.6);
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #0f172a;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.info-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  margin-bottom: 8px;
  letter-spacing: 1px;
  text-transform: uppercase;
  font-family: 'JetBrains Mono', monospace;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.group-name {
  font-size: 15px;
  font-weight: 600;
  flex: 1;
  color: #1e293b;
}

.member-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.search-result {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #f8fafc;
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: 10px;
  margin-bottom: 10px;
}

.member-list {
  max-height: 320px;
  overflow-y: auto;
}

.member-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.member-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.member-name {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
}

.notice-content {
  background: #f8fafc;
  border: 1px solid rgba(0, 0, 0, 0.04);
  border-radius: 10px;
  padding: 14px;
}

.notice-text {
  font-size: 14px;
  color: #1e293b;
  line-height: 1.6;
  margin-bottom: 8px;
}

.notice-actions {
  text-align: right;
}

.notice-empty {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #94a3b8;
  font-size: 13px;
}
</style>
