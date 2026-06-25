<template>
  <el-popover placement="bottom" :width="400" trigger="click" @show="loadNotifications">
    <template #reference>
      <el-badge :value="pendingCount" :hidden="pendingCount === 0" :max="99">
        <el-button text @click="loadCount">
          <el-icon size="18"><WarningFilled /></el-icon>
        </el-button>
      </el-badge>
    </template>
    <div class="stock-dropdown">
      <div class="dropdown-header">
        <span>📦 缺货通知</span>
        <el-tag size="small" :type="pendingCount > 0 ? 'danger' : 'info'">
          {{ pendingCount }} 条待处理
        </el-tag>
      </div>

      <div class="notification-list">
        <div v-for="item in notifications" :key="item.id" class="notification-item">
          <div class="item-top">
            <span class="item-user">{{ item.userName }}</span>
            <el-tag size="small" :type="item.userRole === 'COACH' ? 'warning' : 'success'">
              {{ item.userRole === 'COACH' ? '教练' : '会员' }}
            </el-tag>
          </div>
          <div class="item-product">
            商品：<strong>{{ item.productName }}</strong>
          </div>
          <div class="item-time">{{ formatTime(item.createTime) }}</div>
          <div class="item-actions">
            <el-button type="primary" size="small" @click="handleMarkNotified(item)"
              :loading="loadingSet.has(item.id)">
              标记已处理
            </el-button>
          </div>
        </div>

        <div v-if="loading" class="loading-hint">加载中...</div>
        <el-empty v-if="!loading && notifications.length === 0" description="暂无待处理的缺货通知" :image-size="48" />
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { WarningFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { listPendingNotifications, markNotified, type PendingNotification } from '../api/gym';

const notifications = ref<PendingNotification[]>([]);
const pendingCount = ref(0);
const loading = ref(false);
const loadingSet = ref(new Set<number>());
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
  loading.value = true;
  try {
    notifications.value = await listPendingNotifications();
    pendingCount.value = notifications.value.length;
  } catch { /* ignore */ }
  loading.value = false;
}

async function loadCount() {
  try {
    const list = await listPendingNotifications();
    pendingCount.value = list.length;
  } catch { /* ignore */ }
}

async function handleMarkNotified(item: PendingNotification) {
  loadingSet.value.add(item.id);
  try {
    await markNotified(item.id);
    ElMessage.success(`已标记「${item.productName}」的缺货通知`);
    notifications.value = notifications.value.filter(n => n.id !== item.id);
    pendingCount.value = notifications.value.length;
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败');
  } finally {
    loadingSet.value.delete(item.id);
  }
}

onMounted(() => {
  loadCount();
  pollTimer = setInterval(loadCount, 30000);
});

onBeforeUnmount(() => {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
});

defineExpose({ loadCount });
</script>

<style scoped>
.stock-dropdown { min-height: 80px; }
.dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 14px;
  padding: 8px 0;
  border-bottom: 1px solid #e4e7ed;
  margin-bottom: 8px;
}

.notification-list {
  max-height: 420px;
  overflow-y: auto;
}

.notification-item {
  padding: 12px 4px;
  border-bottom: 1px solid #f0f0f0;
}

.item-top {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.item-user {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.item-product {
  font-size: 13px;
  color: #303133;
  margin-bottom: 2px;
}

.item-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-bottom: 8px;
}

.item-actions {
  display: flex;
  justify-content: flex-end;
}

.loading-hint {
  text-align: center;
  color: #909399;
  padding: 20px 0;
  font-size: 13px;
}
</style>
