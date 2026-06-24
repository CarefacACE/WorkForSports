<template>
  <div class="detail-page">
    <el-button link @click="$router.back()" style="margin-bottom: 16px">← 返回列表</el-button>

    <el-card v-if="coach" shadow="never">
      <div class="detail-header">
        <div class="detail-cover" v-if="coach.coverImage">
          <img :src="coach.coverImage" />
        </div>
        <div class="detail-meta">
          <div class="detail-avatar" v-if="coach.avatar">
            <img :src="coach.avatar" />
          </div>
          <div class="detail-avatar-placeholder" v-else>{{ (coach.realName || '?')[0] }}</div>
          <div>
            <h2 class="detail-name">{{ coach.realName || coach.username }}</h2>
            <div class="detail-specialties" v-if="coach.specialties">
              <el-tag v-for="s in coach.specialties.split(',')" :key="s" effect="plain">{{ s }}</el-tag>
            </div>
            <div class="detail-price">¥{{ coach.pricePerSession }}<span class="price-unit">/节 ({{ coach.sessionDuration }}分钟)</span></div>
          </div>
        </div>
      </div>

      <el-divider />

      <div class="detail-desc" v-if="coach.description">
        <h3>教练介绍</h3>
        <p>{{ coach.description }}</p>
      </div>

      <el-divider />

      <!-- Purchase Section -->
      <div class="purchase-section">
        <h3>购买课程</h3>
        <div class="purchase-row">
          <span>节数：</span>
          <el-input-number v-model="sessions" :min="1" :max="100" size="large" />
          <span class="purchase-total">合计：<strong>¥{{ totalCost }}</strong></span>
          <el-button type="primary" size="large" :loading="purchasing" @click="handlePurchase">立即购买</el-button>
        </div>
      </div>

      <el-divider />

      <!-- Available Schedule -->
      <div class="schedule-section">
        <h3>可预约时段</h3>
        <div v-if="coach.availableSchedules && coach.availableSchedules.length > 0" class="schedule-list">
          <div v-for="s in coach.availableSchedules" :key="s.id" class="schedule-item">
            <span class="schedule-time">{{ formatTime(s.startTime) }} - {{ formatEndTime(s.endTime) }}</span>
            <span class="schedule-title">{{ s.title || '空闲' }}</span>
          </div>
        </div>
        <el-empty v-else description="暂无可用时段" :image-size="60" />
      </div>
    </el-card>

    <div v-else v-loading="loading" style="min-height: 300px" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getCoachDetail, purchaseSessions, type CoachDetail } from '../../api/privateCoach';
import { useUserStore } from '../../stores/user';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const userId = userStore.user?.id || 0;
const coachId = Number(route.params.coachId);

const loading = ref(false);
const purchasing = ref(false);
const coach = ref<CoachDetail | null>(null);
const sessions = ref(1);

const totalCost = computed(() => {
  if (!coach.value) return 0;
  return (coach.value.pricePerSession * sessions.value).toFixed(0);
});

function formatTime(dt: string) {
  if (!dt) return '';
  const d = new Date(dt);
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

function formatEndTime(dt: string) {
  if (!dt) return '';
  const d = new Date(dt);
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

async function fetchDetail() {
  loading.value = true;
  try {
    coach.value = await getCoachDetail(coachId);
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败');
  } finally { loading.value = false; }
}

async function handlePurchase() {
  if (!userId) { ElMessage.warning('请先登录'); return; }
  purchasing.value = true;
  try {
    await purchaseSessions(userId, coachId, sessions.value);
    ElMessage.success(`成功购买 ${sessions.value} 节私教课`);
    router.push('/member/my-coaches');
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '购买失败');
  } finally { purchasing.value = false; }
}

onMounted(fetchDetail);
</script>

<style scoped>
.detail-page { max-width: 800px; }
.detail-header { display: flex; flex-direction: column; gap: 20px; }
.detail-cover { border-radius: 12px; overflow: hidden; max-height: 240px; }
.detail-cover img { width: 100%; object-fit: cover; }
.detail-meta { display: flex; align-items: center; gap: 16px; }
.detail-avatar { width: 64px; height: 64px; border-radius: 50%; overflow: hidden; flex-shrink: 0; }
.detail-avatar img { width: 100%; height: 100%; object-fit: cover; }
.detail-avatar-placeholder { width: 64px; height: 64px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 28px; font-weight: 700; color: #fff; background: linear-gradient(135deg, #2563eb, #3b82f6); flex-shrink: 0; }
.detail-name { font-size: 22px; font-weight: 700; margin: 0 0 8px; }
.detail-specialties { display: flex; gap: 6px; margin-bottom: 8px; }
.detail-price { color: #ef4444; font-size: 24px; font-weight: 700; }
.price-unit { font-size: 13px; font-weight: 400; color: #999; }
.detail-desc h3 { margin: 0 0 8px; font-size: 16px; }
.detail-desc p { color: #64748b; line-height: 1.8; white-space: pre-wrap; margin: 0; }
.purchase-section h3 { margin: 0 0 12px; font-size: 16px; }
.purchase-row { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.purchase-total { font-size: 16px; }
.purchase-total strong { color: #ef4444; font-size: 22px; }
.schedule-section h3 { margin: 0 0 12px; font-size: 16px; }
.schedule-list { display: flex; flex-direction: column; gap: 8px; }
.schedule-item { display: flex; align-items: center; justify-content: space-between; padding: 10px 16px; background: #f8fafc; border-radius: 8px; font-size: 14px; }
.schedule-time { font-weight: 600; color: #2563eb; font-family: 'JetBrains Mono', monospace; }
.schedule-title { color: #64748b; }
</style>
