<template>
  <div class="exercise-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value">{{ formatDuration(stats.totalDuration) }}</div>
          <div class="stat-label">总运动时长</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value">{{ stats.totalDistance?.toFixed(1) || '0' }}<span class="stat-unit">km</span></div>
          <div class="stat-label">总运动距离</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value">{{ stats.totalCalories || 0 }}<span class="stat-unit">kcal</span></div>
          <div class="stat-label">总热量消耗</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value">{{ stats.totalDays || 0 }}<span class="stat-unit">天</span></div>
          <div class="stat-label">运动天数</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 跑步数据 -->
      <el-col :span="8">
        <el-card shadow="never" class="running-card">
          <template #header><span class="section-title">🏃 跑步数据</span></template>
          <div class="running-stat"><span>户外跑步</span><strong>{{ stats.outdoorRunning?.toFixed(1) || '0' }} km</strong></div>
          <div class="running-stat"><span>室内跑步</span><strong>{{ stats.indoorRunning?.toFixed(1) || '0' }} km</strong></div>
          <div class="running-stat total"><span>总跑步</span><strong>{{ stats.totalRunning?.toFixed(1) || '0' }} km</strong></div>
          <div class="running-stat"><span>平均配速</span><strong>{{ stats.avgPace || '-' }}</strong></div>
          <div class="running-stat"><span>运动次数</span><strong>{{ stats.totalSessions || 0 }} 次</strong></div>
          <div class="running-stat"><span>平均时长</span><strong>{{ stats.avgDuration || 0 }} 分钟</strong></div>
          <div class="running-stat"><span>平均热量</span><strong>{{ stats.avgCalories || 0 }} kcal</strong></div>
        </el-card>
      </el-col>

      <!-- 趋势图 -->
      <el-col :span="16">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span class="section-title">📈 运动趋势</span>
              <el-radio-group v-model="trendDays" size="small" @change="fetchTrend">
                <el-radio-button :value="7">7天</el-radio-button>
                <el-radio-button :value="30">30天</el-radio-button>
                <el-radio-button :value="90">90天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="chartRef" class="trend-chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 运动记录列表 -->
    <el-card shadow="never" class="records-card">
      <template #header>
        <div class="records-header">
          <span class="section-title">📋 运动记录</span>
          <el-button type="primary" @click="addDialogVisible = true">+ 添加记录</el-button>
        </div>
      </template>

      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column label="日期" width="110" align="center">
          <template #default="{ row }">{{ row.exerciseDate }}</template>
        </el-table-column>
        <el-table-column label="类型" width="130">
          <template #default="{ row }">
            <span>{{ typeIcon(row.type) }} {{ typeLabel(row.type) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时长" width="80" align="center">
          <template #default="{ row }">{{ row.duration }}分</template>
        </el-table-column>
        <el-table-column label="距离" width="80" align="center">
          <template #default="{ row }">{{ row.distance > 0 ? row.distance + 'km' : '-' }}</template>
        </el-table-column>
        <el-table-column label="热量" width="80" align="center">
          <template #default="{ row }">{{ row.calories > 0 ? row.calories + 'kcal' : '-' }}</template>
        </el-table-column>
        <el-table-column label="心率" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.heartRateAvg">{{ row.heartRateAvg }}<span v-if="row.heartRateMax">/{{ row.heartRateMax }}</span> bpm</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="配速" width="70" align="center">
          <template #default="{ row }">{{ row.pace || '-' }}</template>
        </el-table-column>
        <el-table-column prop="notes" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ row }">
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加记录弹窗 -->
    <el-dialog v-model="addDialogVisible" title="添加运动记录" width="520px" top="5vh">
      <el-form :model="addForm" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="运动类型" required>
              <el-select v-model="addForm.type" style="width:100%">
                <el-option v-for="t in typeOptions" :key="t.value" :label="t.icon + ' ' + t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="日期" required>
              <el-date-picker v-model="addForm.exerciseDate" type="date" value-format="YYYY-MM-DD"
                placeholder="选择日期" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="时长(分钟)">
              <el-input-number v-model="addForm.duration" :min="0" :max="600" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="距离(km)">
              <el-input-number v-model="addForm.distance" :min="0" :precision="2" :step="0.5" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item>
              <template #label>
                <span>热量(kcal)</span>
                <el-tag v-if="caloriesAuto" size="small" type="success" effect="plain" style="margin-left:6px;font-size:10px">Keytel 自动</el-tag>
              </template>
              <el-input-number v-model="addForm.calories" :min="0" :step="10" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="平均心率">
              <el-input-number v-model="addForm.heartRateAvg" :min="0" :max="250" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大心率">
              <el-input-number v-model="addForm.heartRateMax" :min="0" :max="250" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="配速">
              <el-input v-model="addForm.pace" placeholder="如 5'30" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="addForm.notes" type="textarea" :rows="2" placeholder="可选备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleAdd">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import {
  getExerciseRecords, getExerciseStats, getExerciseTrend,
  addExerciseRecord, deleteExerciseRecord,
  type ExerciseRecord, type ExerciseStats, type TrendPoint,
} from '../../api/exercise';
import { getHealthProfile } from '../../api/health';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();
const userId = userStore.user?.id;

// ====== 运动类型 ======
const typeOptions = [
  { label: '户外跑步', value: 'RUNNING_OUTDOOR', icon: '🏃' },
  { label: '室内跑步', value: 'RUNNING_INDOOR', icon: '🏃‍♂️' },
  { label: '健走', value: 'WALKING', icon: '🚶' },
  { label: '骑行', value: 'CYCLING', icon: '🚴' },
  { label: '游泳', value: 'SWIMMING', icon: '🏊' },
  { label: '瑜伽', value: 'YOGA', icon: '🧘' },
  { label: '力量训练', value: 'STRENGTH', icon: '💪' },
  { label: 'HIIT', value: 'HIIT', icon: '🔥' },
];

function typeLabel(v: string) { return typeOptions.find(t => t.value === v)?.label || v; }
function typeIcon(v: string) { return typeOptions.find(t => t.value === v)?.icon || '📌'; }
function formatDuration(min: number) {
  if (!min) return '0';
  const h = Math.floor(min / 60);
  const m = min % 60;
  return h > 0 ? `${h}h${m > 0 ? m + 'm' : ''}` : `${m}m`;
}

// ====== 数据 ======
const loading = ref(false);
const records = ref<ExerciseRecord[]>([]);
const stats = ref<Partial<ExerciseStats>>({});
const trendDays = ref(30);
const trendData = ref<TrendPoint[]>([]);

// ====== 图表 ======
const chartRef = ref<HTMLElement>();
let chart: echarts.ECharts | null = null;

function renderChart() {
  if (!chartRef.value) return;
  if (!chart) {
    chart = echarts.init(chartRef.value);
  }
  const dates = trendData.value.map(p => p.date.slice(5));
  const durations = trendData.value.map(p => p.duration);
  const calories = trendData.value.map(p => p.calories);
  const distances = trendData.value.map(p => p.distance);

  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['运动时长(分)', '热量(kcal)', '距离(km)'], bottom: 0 },
    grid: { left: 50, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: 45, fontSize: 10 } },
    yAxis: [
      { type: 'value', name: '分钟/kcal', position: 'left' },
      { type: 'value', name: 'km', position: 'right' },
    ],
    series: [
      { name: '运动时长(分)', type: 'bar', data: durations, itemStyle: { color: '#409eff', borderRadius: [3, 3, 0, 0] } },
      { name: '热量(kcal)', type: 'line', data: calories, smooth: true, lineStyle: { color: '#e6a23c' }, itemStyle: { color: '#e6a23c' } },
      { name: '距离(km)', type: 'line', yAxisIndex: 1, data: distances, smooth: true, lineStyle: { color: '#67c23a' }, itemStyle: { color: '#67c23a' } },
    ],
  });
}

// ====== 添加弹窗 ======
const addDialogVisible = ref(false);
const submitLoading = ref(false);
const addForm = reactive({
  type: 'RUNNING_OUTDOOR',
  exerciseDate: new Date().toISOString().slice(0, 10),
  duration: 0,
  distance: 0,
  calories: 0,
  heartRateAvg: undefined as number | undefined,
  heartRateMax: undefined as number | undefined,
  pace: '',
  notes: '',
});

// ====== Keytel 公式自动计算热量 ======
const healthData = reactive({
  weight: 70,
  gender: '男',
  age: 25,
  loaded: false,
});

async function fetchHealth() {
  if (!userId) return;
  try {
    const profile = await getHealthProfile(userId);
    if (profile.weight) healthData.weight = profile.weight;
    if (profile.gender) healthData.gender = profile.gender;
    if (userStore.user?.birthday) {
      const birth = new Date(userStore.user.birthday);
      const today = new Date();
      healthData.age = today.getFullYear() - birth.getFullYear();
    }
    healthData.loaded = true;
  } catch {
    // use defaults if no health profile
  }
}

function calcKeytelCalories(): number {
  const { duration, heartRateAvg } = addForm;
  if (!duration || !heartRateAvg || duration <= 0 || heartRateAvg <= 0) return 0;
  const hr = heartRateAvg;
  const w = healthData.weight;
  const a = healthData.age;
  const hours = duration / 60;
  let kcal: number;
  if (healthData.gender === '女') {
    kcal = ((-20.4022 + 0.4472 * hr - 0.1263 * w + 0.074 * a) / 4.184) * 60 * hours;
  } else {
    kcal = ((-55.0969 + 0.6309 * hr + 0.1988 * w + 0.2017 * a) / 4.184) * 60 * hours;
  }
  return Math.max(0, Math.round(kcal));
}

const caloriesAuto = ref(false);

watch(
  () => [addForm.duration, addForm.heartRateAvg],
  () => {
    const result = calcKeytelCalories();
    if (result > 0) {
      addForm.calories = result;
      caloriesAuto.value = true;
    } else {
      caloriesAuto.value = false;
    }
  }
);

async function handleAdd() {
  if (!userId || !addForm.exerciseDate) {
    ElMessage.warning('请选择日期');
    return;
  }
  submitLoading.value = true;
  try {
    await addExerciseRecord(userId, { ...addForm } as ExerciseRecord);
    ElMessage.success('记录已添加');
    addDialogVisible.value = false;
    addForm.duration = 0;
    addForm.distance = 0;
    addForm.calories = 0;
    addForm.heartRateAvg = undefined;
    addForm.heartRateMax = undefined;
    addForm.pace = '';
    addForm.notes = '';
    await fetchAll();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '添加失败');
  } finally {
    submitLoading.value = false;
  }
}

async function handleDelete(row: ExerciseRecord) {
  if (!userId || !row.id) return;
  try {
    await deleteExerciseRecord(userId, row.id);
    ElMessage.success('已删除');
    await fetchAll();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败');
  }
}

// ====== 数据加载 ======
async function fetchRecords() {
  if (!userId) return;
  loading.value = true;
  try {
    records.value = await getExerciseRecords(userId);
  } catch {}
  loading.value = false;
}

async function fetchStats() {
  if (!userId) return;
  try {
    stats.value = await getExerciseStats(userId);
  } catch {}
}

async function fetchTrend() {
  if (!userId) return;
  try {
    trendData.value = await getExerciseTrend(userId, trendDays.value);
    await nextTick();
    renderChart();
  } catch {}
}

async function fetchAll() {
  await Promise.all([fetchRecords(), fetchStats(), fetchTrend()]);
}

function handleResize() { chart?.resize(); }

onMounted(async () => {
  await fetchHealth();
  await fetchAll();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  chart?.dispose();
});
</script>

<style scoped>
.exercise-page { display: flex; flex-direction: column; flex: 1; min-height: 0; }
.stats-row { margin-bottom: 16px; }
.stat-card { background: #fff; border: 1px solid #ebeef5; border-radius: 8px; padding: 20px; text-align: center; }
.stat-value { font-size: 28px; font-weight: 700; color: #303133; }
.stat-unit { font-size: 14px; font-weight: 400; color: #909399; margin-left: 2px; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; }

.running-card { margin-bottom: 16px; }
.section-title { font-weight: 600; font-size: 15px; }
.running-stat { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f0f0f0; font-size: 13px; color: #606266; }
.running-stat:last-child { border-bottom: none; }
.running-stat.total { font-weight: 600; color: #303133; padding-top: 12px; border-top: 2px solid #ebeef5; margin-top: 4px; }
.running-stat strong { color: #303133; }

.chart-card { margin-bottom: 16px; }
.chart-header { display: flex; justify-content: space-between; align-items: center; }
.trend-chart { height: 300px; }

.records-card { margin-top: 16px; }
.records-header { display: flex; justify-content: space-between; align-items: center; }
</style>
