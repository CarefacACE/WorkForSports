<template>
  <el-popover placement="bottom" :width="320" trigger="click">
    <template #reference>
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
        <el-button text @click="loadNotifications">
          <el-icon size="18"><Bell /></el-icon>
        </el-button>
      </el-badge>
    </template>
    <div class="notification-dropdown">
      <div class="dropdown-header">通知</div>
      <div class="notification-list">
        <div v-for="item in notifications" :key="item.id" class="notification-item"
          :class="{ unread: item.isRead === 0 }" @click="handleRead(item)">
          <div class="notification-title">{{ item.title }}</div>
          <div class="notification-content">{{ item.content }}</div>
          <div class="notification-time">{{ formatTime(item.createTime) }}</div>
        </div>
        <el-empty v-if="notifications.length === 0" description="暂无通知" :image-size="48" />
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { Bell } from '@element-plus/icons-vue';
import { getNotifications, markAsRead, getUnreadCount, type NotificationItem } from '../api/notification';

const router = useRouter();
const notifications = ref<NotificationItem[]>([]);
const unreadCount = ref(0);
const currentUserId = ref(0);
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

async function loadNotifications() {
  try {
    const res = await getNotifications(currentUserId.value, 1, 20);
    notifications.value = res.records;
  } catch {}
}

async function loadUnread() {
  try {
    unreadCount.value = await getUnreadCount(currentUserId.value);
  } catch {}
}

async function handleRead(item: NotificationItem) {
  if (item.isRead === 0) {
    await markAsRead(item.id);
    item.isRead = 1;
    unreadCount.value = Math.max(0, unreadCount.value - 1);
  }
  // 根据通知类型跳转到对应页面
  if (item.type === 'SESSION_REQUEST') {
    router.push('/coach/my-schedule');
  } else if (item.type === 'SESSION_APPROVED' || item.type === 'SESSION_REJECTED') {
    router.push('/member/my-coaches');
  }
}

onMounted(() => {
  const userInfo = JSON.parse(localStorage.getItem('user_info') || '{}');
  currentUserId.value = userInfo.id || 0;
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

.notification-list {
  max-height: 360px;
  overflow-y: auto;
}

.notification-item {
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}

.notification-item.unread {
  background: #ecf5ff;
  padding: 10px 8px;
  border-radius: 4px;
}

.notification-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.notification-content {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.notification-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 4px;
}
</style>
