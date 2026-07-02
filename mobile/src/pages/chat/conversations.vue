<template>
  <view class="page">
    <scroll-view class="list" scroll-y>
      <view v-if="conversations.length === 0 && !loading" class="empty">
        <text class="empty-icon">💬</text>
        <text class="empty-text">暂无聊天</text>
      </view>

      <view
        v-for="conv in conversations"
        :key="conv.id"
        class="conv-card"
        @tap="openChat(conv)"
      >
        <view class="conv-avatar" :class="conv.type === 'GROUP' ? 'group' : 'private'">
          <text>{{ conv.type === 'GROUP' ? '👥' : '👤' }}</text>
        </view>
        <view class="conv-info">
          <text class="conv-name">{{ conv.name || ('会话 #' + conv.id) }}</text>
          <text class="conv-last" v-if="conv._lastMsg">{{ conv._lastMsg }}</text>
        </view>
        <view class="conv-meta">
          <text class="conv-time" v-if="conv._lastTime">{{ conv._lastTime?.slice(11, 16) }}</text>
          <text class="conv-badge" v-if="(conv._unread || 0) > 0">{{ conv._unread }}</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { getConversations, getUnreadConversations, type ChatConversation, type UnreadConversation } from '@/api/chat'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userId = userStore.userId

interface ConvItem extends ChatConversation {
  _unread?: number
  _lastMsg?: string
  _lastTime?: string
}

const conversations = ref<ConvItem[]>([])
const loading = ref(false)

async function fetchData() {
  if (!userId) return
  loading.value = true
  try {
    const [convs, unreadRes] = await Promise.all([
      getConversations(userId, 1, 100),
      getUnreadConversations(userId),
    ])

    const unreadMap: Record<number, UnreadConversation> = {}
    for (const u of unreadRes || []) {
      unreadMap[u.conversationId] = u
    }

    conversations.value = (convs.records || []).map((c) => ({
      ...c,
      _unread: unreadMap[c.id]?.unreadCount || 0,
      _lastMsg: unreadMap[c.id]?.lastMessage || '',
      _lastTime: unreadMap[c.id]?.lastTime || '',
    }))
  } catch {
    uni.showToast({ title: '获取会话失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function openChat(conv: ChatConversation) {
  uni.navigateTo({
    url: `/pages/chat/chat-room?id=${conv.id}&name=${encodeURIComponent(conv.name || '聊天')}&type=${conv.type}`,
  })
}

fetchData()
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; }
.list { padding: 16rpx 24rpx; }

.conv-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 12rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
  display: flex;
  align-items: center;
}

.conv-avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  margin-right: 20rpx;

  &.group { background: #e8f4fd; }
  &.private { background: #f5f6fa; }
}

.conv-info { flex: 1; }
.conv-name { font-size: 30rpx; font-weight: 600; color: #333; margin-bottom: 4rpx; display: block; }
.conv-last { font-size: 24rpx; color: #999; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 400rpx; display: block; }

.conv-meta { text-align: right; }
.conv-time { font-size: 20rpx; color: #bbb; display: block; margin-bottom: 4rpx; }

.conv-badge {
  display: inline-block;
  min-width: 36rpx;
  height: 36rpx;
  line-height: 36rpx;
  text-align: center;
  background: #ff4d4f;
  color: #fff;
  font-size: 20rpx;
  border-radius: 18rpx;
  padding: 0 8rpx;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 0;
}

.empty-icon { font-size: 72rpx; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: #999; }
</style>
