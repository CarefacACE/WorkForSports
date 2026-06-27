<template>
  <div class="coach-list-page">
    <el-card shadow="never" style="flex: 1; display: flex; flex-direction: column; min-height: 0;">
      <template #header>
        <div class="card-header">
          <span>🏋️ 私教教练</span>
          <el-input v-model="keyword" placeholder="搜索教练..." clearable style="width: 240px"
            @clear="handleSearch" @keyup.enter="handleSearch">
            <template #append><el-button @click="handleSearch">搜索</el-button></template>
          </el-input>
        </div>
      </template>

      <div v-loading="loading" class="coach-grid">
        <div v-for="c in coaches" :key="c.coachId" class="coach-card" @click="goDetail(c.coachId)">
          <div class="coach-cover">
            <img v-if="c.coverImage" :src="c.coverImage" />
            <div v-else class="coach-cover-placeholder">{{ (c.realName || c.username || '?')[0] }}</div>
          </div>
          <div class="coach-info">
            <div class="coach-name">{{ c.realName || c.username }}</div>
            <div class="coach-specialties" v-if="c.specialties">
              <el-tag v-for="s in c.specialties.split(',')" :key="s" size="small" effect="plain" class="s-tag">{{ s }}</el-tag>
            </div>
            <div class="coach-price">¥{{ c.pricePerSession }}<span class="price-unit">/节</span></div>
          </div>
        </div>

        <el-empty v-if="!loading && coaches.length === 0" description="暂无开放私教的教练" />
      </div>

      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination background layout="prev, pager, next" :total="total" :page-size="pageSize" v-model:current-page="pageNum" @current-change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { listCoaches, type CoachListItem } from '../../api/privateCoach';

const router = useRouter();
const loading = ref(false);
const keyword = ref('');
const pageNum = ref(1);
const pageSize = ref(12);
const total = ref(0);
const coaches = ref<CoachListItem[]>([]);

async function fetchData() {
  loading.value = true;
  try {
    const res = await listCoaches(keyword.value, pageNum.value, pageSize.value);
    coaches.value = res.records;
    total.value = res.total;
  } catch {} finally { loading.value = false; }
}

function handleSearch() {
  pageNum.value = 1;
  fetchData();
}

function goDetail(coachId: number) {
  router.push(`/member/coach-detail/${coachId}`);
}

onMounted(fetchData);
</script>

<style scoped>
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }
.coach-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px; }
.coach-card { border: 1px solid #eee; border-radius: 12px; overflow: hidden; cursor: pointer; transition: all 0.25s; }
.coach-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.08); }
.coach-cover { height: 140px; overflow: hidden; background: #f5f7fa; }
.coach-cover img { width: 100%; height: 100%; object-fit: cover; }
.coach-cover-placeholder { height: 100%; display: flex; align-items: center; justify-content: center; font-size: 48px; font-weight: 700; color: #cbd5e1; background: linear-gradient(135deg, #e0e7ff, #c7d2fe); }
.coach-info { padding: 12px; }
.coach-name { font-weight: 600; font-size: 15px; margin-bottom: 6px; }
.coach-specialties { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 8px; }
.s-tag { font-size: 11px; }
.coach-price { color: #ef4444; font-weight: 700; font-size: 18px; }
.price-unit { font-size: 12px; font-weight: 400; color: #999; }
.pagination-wrapper { display: flex; justify-content: center; margin-top: 20px; }
</style>
