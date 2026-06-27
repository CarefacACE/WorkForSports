<template>
  <div class="coach-list-page">
    <el-card class="full-height-card">
      <template #header>
        <div class="card-header">
          <span>私教</span>
        </div>
      </template>

      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索教练姓名" clearable style="width: 280px"
          @clear="handleSearch" @keyup.enter="handleSearch">
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>

      <el-row :gutter="16" v-loading="loading">
        <el-col :span="6" v-for="coach in coaches" :key="coach.coachId" style="margin-bottom: 16px">
          <el-card shadow="hover" class="coach-card" @click="$router.push(`/member/coach-detail/${coach.coachId}`)">
            <div class="coach-cover">
              <img v-if="coach.coverImage" :src="coach.coverImage" alt="封面" />
              <div v-else class="cover-placeholder">{{ (coach.realName || coach.username || '?')[0] }}</div>
            </div>
            <div class="coach-info">
              <div class="coach-avatar" v-if="coach.avatar">
                <img :src="coach.avatar" />
              </div>
              <div class="coach-avatar-ph" v-else>{{ (coach.realName || coach.username || '?')[0] }}</div>
              <div class="coach-name">{{ coach.realName || coach.username }}</div>
              <div class="coach-tags" v-if="coach.specialties">
                <el-tag v-for="s in coach.specialties.split(',').slice(0, 3)" :key="s" size="small" effect="plain">{{ s.trim() }}</el-tag>
              </div>
              <div class="coach-price">
                <span class="price-num">¥{{ coach.pricePerSession }}</span>
                <span class="price-unit">/节 · {{ coach.sessionDuration }}分钟</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-if="!loading && coaches.length === 0" description="暂无教练开放私教" />

      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { listCoaches, type CoachListItem } from '../../api/privateCoach';

const keyword = ref('');
const pageNum = ref(1);
const pageSize = ref(12);
const total = ref(0);
const loading = ref(false);
const coaches = ref<CoachListItem[]>([]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await listCoaches(keyword.value || undefined, pageNum.value, pageSize.value);
    coaches.value = res.records;
    total.value = res.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取教练列表失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pageNum.value = 1;
  fetchData();
}

onMounted(() => fetchData());
</script>

<style scoped>
.coach-list-page {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}
.full-height-card {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.full-height-card :deep(.el-card__body) {
  flex: 1;
}
.card-header { font-weight: 600; }
.toolbar { margin-bottom: 16px; }
.coach-card { cursor: pointer; transition: transform 0.15s; }
.coach-card:hover { transform: translateY(-2px); }
.coach-cover { height: 120px; overflow: hidden; border-radius: 4px; margin-bottom: 8px; }
.coach-cover img { width: 100%; height: 100%; object-fit: cover; }
.cover-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #2563eb, #3b82f6); color: #fff; font-size: 32px; font-weight: 700; }
.coach-info { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.coach-avatar { width: 48px; height: 48px; border-radius: 50%; overflow: hidden; }
.coach-avatar img { width: 100%; height: 100%; object-fit: cover; }
.coach-avatar-ph { width: 48px; height: 48px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 700; color: #fff; background: linear-gradient(135deg, #2563eb, #3b82f6); }
.coach-name { font-weight: 600; font-size: 15px; margin-top: 4px; }
.coach-tags { display: flex; gap: 4px; flex-wrap: wrap; justify-content: center; }
.coach-price { margin-top: 4px; }
.price-num { color: #ef4444; font-size: 18px; font-weight: 700; }
.price-unit { font-size: 12px; color: #94a3b8; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
