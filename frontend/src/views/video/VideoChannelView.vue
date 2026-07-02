<template>
  <div class="video-channel-page">
    <!-- Search & Filter Bar -->
    <div class="filter-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索健身视频..."
        clearable
        class="search-input"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select
        v-model="selectedCategory"
        placeholder="全部类别"
        clearable
        class="category-select"
        @change="handleSearch"
      >
        <el-option label="全部类别" value="" />
        <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
      </el-select>
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon> 搜索
      </el-button>
      <el-button v-if="role !== 'ADMIN'" @click="handleCrawl" :loading="crawling">
        <el-icon><Refresh /></el-icon> 刷新视频
      </el-button>
    </div>

    <!-- Video Grid -->
    <div v-loading="loading" class="video-grid-container">
      <template v-if="videoList.length > 0">
        <div
          v-for="video in videoList"
          :key="video.id"
          class="video-card"
          @click="openVideo(video)"
        >
          <div class="video-cover">
            <img
              :src="video.thumbnailUrl"
              :alt="video.title"
              @error="handleImgError"
            />
            <div class="video-duration">{{ formatDuration(video.duration) }}</div>
            <div class="video-play-overlay">
              <el-icon :size="40"><VideoPlay /></el-icon>
            </div>
          </div>
          <div class="video-info">
            <h3 class="video-title" :title="video.title">{{ video.title }}</h3>
            <div class="video-meta">
              <span class="video-author">{{ video.author }}</span>
              <span class="video-stats">
                <el-icon><View /></el-icon> {{ formatPlayCount(video.playCount) }}
              </span>
            </div>
            <div class="video-footer">
              <el-tag size="small" type="primary">{{ platformLabel(video.platform) }}</el-tag>
              <el-tag v-if="video.category" size="small" type="success">{{ video.category }}</el-tag>
            </div>
          </div>
        </div>
      </template>
      <el-empty v-else description="暂无教学视频，点击「刷新视频」加载" />
    </div>

    <!-- Pagination -->
    <div v-if="total > 0" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 36]"
        layout="total, sizes, prev, pager, next"
        @change="fetchVideos"
      />
    </div>

    <!-- Video Player Dialog -->
    <el-dialog
      v-model="playerVisible"
      :title="currentVideo?.title"
      width="860px"
      destroy-on-close
      class="video-player-dialog"
    >
      <div v-if="currentVideo" class="player-wrapper">
        <iframe
          :src="currentVideo.embedUrl"
          allowfullscreen
          class="video-iframe"
        />
      </div>
      <div v-if="currentVideo" class="player-info">
        <el-tag type="primary">{{ platformLabel(currentVideo.platform) }}</el-tag>
        <el-tag v-if="currentVideo.category" type="success">{{ currentVideo.category }}</el-tag>
        <span>👤 {{ currentVideo.author }}</span>
        <span>▶ {{ formatPlayCount(currentVideo.playCount) }}</span>
      </div>
      <template #footer>
        <el-button @click="playerVisible = false">关闭</el-button>
        <el-button type="primary" @click="openSourceUrl">去源站观看</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '../../stores/user'
import { getVideoList, triggerCrawl, triggerSeed, getCategories, type VideoChannel } from '../../api/video'
import { Search, VideoPlay, View, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const role = userStore.user?.role || 'MEMBER'

const videoList = ref<VideoChannel[]>([])
const categories = ref<string[]>([])
const loading = ref(false)
const crawling = ref(false)
const searchKeyword = ref('')
const selectedCategory = ref('')
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

const playerVisible = ref(false)
const currentVideo = ref<VideoChannel | null>(null)

onMounted(async () => {
  fetchCategories()
  await fetchVideos()
  // 首次加载没有数据则自动用种子填充
  if (videoList.value.length === 0 && role !== 'ADMIN') {
    await handleSeed()
  }
})

function fetchCategories() {
  getCategories().then(res => {
    categories.value = res as unknown as string[]
  }).catch(() => {})
}

function fetchVideos() {
  loading.value = true
  return getVideoList({
    keyword: searchKeyword.value || undefined,
    category: selectedCategory.value || undefined,
    pageNum: currentPage.value,
    pageSize: pageSize.value,
  }).then(res => {
    const data = res as unknown as { records: VideoChannel[]; total: number }
    videoList.value = data.records || []
    total.value = data.total || 0
  }).catch(() => {
    videoList.value = []
    total.value = 0
  }).finally(() => {
    loading.value = false
  })
}

function handleSearch() {
  currentPage.value = 1
  fetchVideos()
}

function handleCrawl() {
  crawling.value = true
  return triggerCrawl('健身教学').then(res => {
    ElMessage.success(res as unknown as string)
    fetchVideos()
    fetchCategories()
  }).catch(() => {
    ElMessage.error('爬取失败')
  }).finally(() => {
    crawling.value = false
  })
}

function handleSeed() {
  return triggerSeed().then(res => {
    ElMessage.success(res as unknown as string)
    fetchVideos()
    fetchCategories()
  }).catch(() => {})
}

function openVideo(video: VideoChannel) {
  currentVideo.value = video
  playerVisible.value = true
}

function openSourceUrl() {
  if (currentVideo.value?.sourceUrl) {
    window.open(currentVideo.value.sourceUrl, '_blank')
  }
}

function handleImgError(e: Event) {
  (e.target as HTMLImageElement).src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 180"><rect fill="%23f0f2f5" width="320" height="180"/><text fill="%23909399" x="160" y="95" text-anchor="middle" font-size="14">暂无封面</text></svg>'
}

function formatPlayCount(count: number): string {
  if (!count) return '0'
  if (count >= 10000) return (count / 10000).toFixed(1) + '万'
  return count.toString()
}

function formatDuration(duration: string): string {
  if (!duration) return ''
  const parts = duration.split(':')
  if (parts.length === 3) {
    const h = parseInt(parts[0])
    const m = parseInt(parts[1])
    const s = parseInt(parts[2])
    if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
    return `${m}:${String(s).padStart(2, '0')}`
  }
  return duration
}

function platformLabel(platform: string): string {
  return platform === 'BILIBILI' ? 'B站' : platform
}
</script>

<style scoped>
.video-channel-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.filter-bar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  padding: 16px 20px;
}
.search-input { flex: 1; min-width: 240px; }
.category-select { width: 160px; }

.video-grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  min-height: 200px;
}

.video-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.video-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
}

.video-cover {
  position: relative;
  width: 100%;
  padding-top: 56.25%; /* 16:9 */
  background: #f0f2f5;
  overflow: hidden;
}

.video-cover img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-duration {
  position: absolute;
  right: 6px;
  bottom: 6px;
  background: rgba(0,0,0,0.75);
  color: #fff;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
}

.video-play-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,0.3);
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s;
}

.video-card:hover .video-play-overlay { opacity: 1; }

.video-info { padding: 12px 14px; }

.video-title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.video-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.video-author { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.video-stats { display: flex; align-items: center; gap: 2px; white-space: nowrap; }

.video-footer { display: flex; gap: 6px; flex-wrap: wrap; }

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}

/* Player Dialog */
.video-player-dialog :deep(.el-dialog__header) { padding: 16px 20px 8px; }

.player-wrapper {
  position: relative;
  padding-top: 56.25%;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}

.video-iframe {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border: none;
}

.player-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  font-size: 13px;
  color: #606266;
}

/* Dark */
:global([data-admin-theme="dark"]) .video-card { background: #1d1e2c; }
:global([data-admin-theme="dark"]) .video-cover { background: #2a2b3a; }
:global([data-admin-theme="dark"]) .video-title { color: #e0e0e0; }
</style>
