<template>
  <view class="page">
    <!-- 角色筛选 -->
    <view class="tabs">
      <view
        v-for="r in roles"
        :key="r.value"
        class="tab"
        :class="{ active: filterRole === r.value }"
        @tap="filterRole = r.value; doSearch()"
      >
        <text>{{ r.label }}</text>
      </view>
    </view>

    <view class="search-bar">
      <input v-model="keyword" placeholder="搜索用户..." placeholder-style="color:#bbb" confirm-type="search" @confirm="doSearch" />
      <text v-if="keyword" class="clear" @tap="keyword = ''; doSearch()">✕</text>
    </view>

    <scroll-view class="list" scroll-y>
      <view v-if="users.length === 0 && !loading" class="empty">
        <text class="empty-icon">👥</text><text class="empty-text">暂无用户</text>
      </view>

      <view v-for="u in users" :key="u.id" class="user-card">
        <view class="u-avatar">
          <text>{{ (u.realName || u.username).charAt(0) }}</text>
        </view>
        <view class="u-info">
          <text class="u-name">{{ u.realName || u.username }}</text>
          <view class="u-meta">
            <text class="u-role" :class="u.role">{{ roleLabel(u.role) }}</text>
            <text>{{ u.phone || '-' }}</text>
            <text>{{ u.email || '-' }}</text>
          </view>
        </view>
        <view class="u-date">{{ u.createTime?.split(' ')[0] }}</view>
      </view>

      <uni-load-more :status="loadStatus" />
    </scroll-view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { getUsers } from '@/api/user'
import type { UserItem } from '@/api/user'

const roles = [
  { value: 'MEMBER', label: '会员' },
  { value: 'COACH', label: '教练' },
  { value: 'ADMIN', label: '管理员' },
]

const filterRole = ref<string>('MEMBER')
const keyword = ref('')
const users = ref<UserItem[]>([])
const loading = ref(false)
const loadStatus = ref<'more' | 'loading' | 'noMore'>('more')
const pageNum = ref(1)

function roleLabel(r: string) { return { MEMBER: '会员', COACH: '教练', ADMIN: '管理员' }[r] || r }

async function doSearch(reset = true) {
  if (loading.value) return
  loading.value = true
  if (reset) { pageNum.value = 1; loadStatus.value = 'more'; }

  try {
    const res = await getUsers({ pageNum: pageNum.value, pageSize: 20, keyword: keyword.value || undefined, role: filterRole.value as 'MEMBER' | 'COACH' | 'ADMIN' })
    if (reset) users.value = res.records
    else users.value = [...users.value, ...res.records]
    loadStatus.value = res.records.length < 20 ? 'noMore' : 'more'
  } catch {
    uni.showToast({ title: '获取用户失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

doSearch()
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; }

.tabs { display: flex; background: #fff; border-bottom: 2rpx solid #f0f2f5; position: sticky; top: 0; z-index: 10; }
.tab { flex: 1; text-align: center; padding: 24rpx 0; font-size: 26rpx; color: #666; border-bottom: 4rpx solid transparent; }
.tab.active { color: #2563eb; font-weight: 600; border-bottom-color: #2563eb; }

.search-bar {
  margin: 16rpx 24rpx; height: 72rpx; background: #fff; border-radius: 36rpx;
  padding: 0 24rpx; display: flex; align-items: center; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
}
.search-bar input { flex: 1; font-size: 26rpx; color: #333; }
.clear { font-size: 24rpx; color: #999; padding: 8rpx; }

.list { padding: 0 24rpx; }

.user-card {
  background: #fff; border-radius: 16rpx; padding: 20rpx 24rpx;
  margin-bottom: 12rpx; display: flex; align-items: center;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
}
.u-avatar {
  width: 80rpx; height: 80rpx; border-radius: 40rpx;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  display: flex; align-items: center; justify-content: center;
  font-size: 32rpx; color: #fff; font-weight: 600; margin-right: 20rpx;
}
.u-info { flex: 1; }
.u-name { font-size: 30rpx; font-weight: 600; color: #333; margin-bottom: 4rpx; display: block; }
.u-meta { font-size: 22rpx; color: #999; display: flex; gap: 12rpx; align-items: center; }
.u-role { font-size: 20rpx; padding: 2rpx 10rpx; border-radius: 6rpx; }
.u-role.MEMBER { background: #e8f4fd; color: #2563eb; }
.u-role.COACH { background: #fef0d0; color: #e6a23c; }
.u-role.ADMIN { background: #fde2e2; color: #f56c6c; }
.u-date { font-size: 20rpx; color: #bbb; }

.empty { display: flex; flex-direction: column; align-items: center; padding: 160rpx 0; }
.empty-icon { font-size: 72rpx; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: #999; }
</style>
