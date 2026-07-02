<template>
  <view class="page">
    <scroll-view class="list" scroll-y>
      <view v-if="videos.length === 0 && !loading" class="empty">
        <text class="empty-icon">🎬</text>
        <text class="empty-text">暂无健身视频</text>
      </view>

      <view
        v-for="v in videos"
        :key="v.id"
        class="video-card"
      >
        <view class="video-cover">
          <image v-if="v.thumbnailUrl" :src="v.thumbnailUrl" mode="aspectFill" class="cover-img" />
          <view v-else class="cover-placeholder">
            <text class="cover-icon">▶</text>
          </view>
        </view>
        <view class="video-body">
          <text class="video-title">{{ v.title }}</text>
          <text class="video-desc" v-if="v.description">{{ v.description }}</text>
          <view class="video-meta">
            <text class="meta-tag">{{ v.platform }}</text>
            <text v-if="v.playCount" class="meta-play">▶ {{ formatCount(v.playCount) }}</text>
            <text v-if="v.duration" class="meta-duration">{{ v.duration }}</text>
          </view>
          <view class="video-tags" v-if="v.tags">
            <text
              v-for="(tag, i) in v.tags.split(',')"
              :key="i"
              class="v-tag"
            >{{ tag.trim() }}</text>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { getVideoList, type VideoChannel, type VideoListResult } from '@/api/video'

const videos = ref<VideoChannel[]>([])
const loading = ref(false)

function formatCount(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

async function fetchData() {
  loading.value = true
  try {
    const result = await getVideoList({ pageNum: 1, pageSize: 20 })
    videos.value = result.records || []
  } catch {
    // 静默
  } finally {
    loading.value = false
  }
}

fetchData()
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #f5f6fa; }

.list { padding: 16rpx 24rpx; }

.video-card {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}

.video-cover {
  width: 100%;
  height: 360rpx;
  background: #f0f2f5;
  position: relative;
}

.cover-img { width: 100%; height: 100%; }

.cover-placeholder {
  width: 100%; height: 100%;
  background: linear-gradient(135deg, #333, #666);
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-icon { font-size: 72rpx; color: rgba(255,255,255,0.6); }

.video-body { padding: 20rpx 24rpx; }

.video-title { font-size: 30rpx; font-weight: 600; color: #333; margin-bottom: 8rpx; display: block; }
.video-desc { font-size: 24rpx; color: #666; margin-bottom: 12rpx; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }

.video-meta { display: flex; gap: 16rpx; margin-bottom: 8rpx; }

.meta-tag { font-size: 20rpx; padding: 2rpx 10rpx; background: #e8f4fd; color: #2563eb; border-radius: 6rpx; }
.meta-play { font-size: 22rpx; color: #999; }
.meta-duration { font-size: 22rpx; color: #999; }

.video-tags { display: flex; gap: 8rpx; flex-wrap: wrap; }
.v-tag { font-size: 20rpx; padding: 2rpx 12rpx; background: #f5f6fa; color: #999; border-radius: 6rpx; }

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 0;
}
.empty-icon { font-size: 72rpx; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: #999; }
</style>
