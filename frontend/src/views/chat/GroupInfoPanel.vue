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
        </div>
        <div v-if="searchUserResult" class="search-result">
          <span>{{ searchUserResult.realName || searchUserResult.username }}</span>
          <el-button size="small" type="primary" link @click="handleAddMember">添加</el-button>
        </div>
        <div class="member-list">
          <div v-for="member in members" :key="member.userId" class="member-item">
            <div class="member-info">
              <span class="member-name">{{ member.realName || member.username }}</span>
              <el-tag v-if="member.userId === ownerId" size="small" type="warning">群主</el-tag>
              <el-tag v-if="member.userId === currentUserId" size="small" type="info">我</el-tag>
            </div>
            <el-button v-if="isOwner && member.userId !== ownerId" type="danger" link size="small"
              @click="handleRemoveMember(member.userId)">移除</el-button>
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
import { getGroupMembers, addGroupMember, removeGroupMember, updateConversationName, leaveGroup, searchUser, searchUserById, getGroupNotice, publishGroupNotice, deleteGroupNotice, type GroupMember, type SearchResult, type GroupNotice } from '../../api/chat';

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
  width: 300px;
  border-left: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.panel-header {
  padding: 12px 16px;
  font-weight: 600;
  font-size: 15px;
  border-bottom: 1px solid #e4e7ed;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.info-section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #909399;
  margin-bottom: 8px;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.group-name {
  font-size: 15px;
  font-weight: 500;
  flex: 1;
}

.member-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.search-result {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 8px;
}

.member-list {
  max-height: 300px;
  overflow-y: auto;
}

.member-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.member-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.member-name {
  font-size: 14px;
  color: #303133;
}

.notice-content {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px;
}

.notice-text {
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
  margin-bottom: 8px;
}

.notice-actions {
  text-align: right;
}

.notice-empty {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #c0c4cc;
  font-size: 13px;
}
</style>
