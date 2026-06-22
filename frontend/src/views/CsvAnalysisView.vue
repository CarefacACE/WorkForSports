<template>
  <div class="csv-analysis-page">
    <!-- 上传区域 -->
    <el-card class="upload-card" shadow="never">
      <div class="upload-area">
        <el-upload
          ref="csvUploadRef"
          :auto-upload="false"
          :on-change="handleCsvChange"
          :show-file-list="false"
          accept=".csv"
          drag
        >
          <el-icon class="upload-icon"><DataAnalysis /></el-icon>
          <div class="upload-text">
            <em>点击或拖拽 CSV 文件到此处</em>
            <p class="upload-hint">支持任意 CSV 文件 — 自动探测数值列并生成图表</p>
          </div>
        </el-upload>
      </div>
    </el-card>

    <!-- 分析结果 -->
    <div v-if="analysisResult" class="result-section">
      <el-card shadow="never">
        <template #header>
          <div class="result-header">
            <span>分析结果：{{ selectedFileName }}</span>
            <el-tag type="success" effect="light">分析完成 · {{ analysisResult.columns.length }} 个数值列</el-tag>
          </div>
        </template>

        <!-- 概览统计 -->
        <el-row :gutter="20" class="stat-cards">
          <el-col :span="8">
            <el-card shadow="hover" class="overview-card">
              <div class="stat-label">数据条数</div>
              <div class="stat-value">{{ analysisResult.totalRecords }}</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" class="overview-card">
              <div class="stat-label">运动时长</div>
              <div class="stat-value duration">{{ analysisResult.durationFormatted }}</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" class="overview-card">
              <div class="stat-label">数值列</div>
              <div class="stat-value">{{ analysisResult.columns.length }}</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 每个数值列 → 一张统计卡 + 一个 ECharts 折线图 -->
        <div v-for="col in analysisResult.columns" :key="col.title" class="column-section">
          <el-card shadow="hover">
            <template #header>
              <div class="column-header">
                <span class="column-title">{{ col.title }}</span>
                <div class="column-stats">
                  <el-tag size="small">avg: {{ col.avg }}</el-tag>
                  <el-tag size="small" type="danger">max: {{ col.max }}</el-tag>
                  <el-tag size="small" type="success">min: {{ col.min }}</el-tag>
                </div>
              </div>
            </template>
            <div :ref="(el) => setChartRef(col.title, el)" class="chart-box"></div>
          </el-card>

          <!-- 心率区间（仅心率列） -->
          <el-card v-if="col.zones" class="zones-card" shadow="hover">
            <template #header><span>{{ col.title }} 区间分布</span></template>
            <div class="zones-container">
              <div v-for="(count, zone) in col.zones" :key="zone" class="zone-item">
                <div class="zone-header">
                  <span class="zone-name">{{ zone }}</span>
                  <span class="zone-count">{{ count }}次 ({{ ((count / analysisResult.totalRecords) * 100).toFixed(1) }}%)</span>
                </div>
                <el-progress
                  :percentage="(count / analysisResult.totalRecords) * 100"
                  :color="getZoneColor(zone)"
                  :show-text="false"
                  :stroke-width="12"
                />
              </div>
            </div>
          </el-card>
        </div>
      </el-card>
    </div>

    <!-- 历史分析记录 -->
    <el-card class="history-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>历史分析记录</span>
          <el-button text @click="loadHistory">刷新</el-button>
        </div>
      </template>
      <el-table :data="historyList" v-loading="historyLoading" stripe empty-text="暂无分析记录">
        <el-table-column prop="originalName" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column label="文件大小" width="120" align="center">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="上传时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="reAnalyze(row)">
              <el-icon><DataAnalysis /></el-icon>
              重新分析
            </el-button>
            <el-button type="success" link @click="downloadFile(row)">
              <el-icon><Download /></el-icon>
              下载
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { DataAnalysis, Download } from '@element-plus/icons-vue';
import * as echarts from 'echarts';
import { getFileList, getDownloadUrl, analyzeCsv, type FileInfo, type CsvAnalysisResult } from '../api/file';
import { useUserStore } from '../stores/user';

const userStore = useUserStore();
const selectedFileName = ref('');
const analysisResult = ref<CsvAnalysisResult | null>(null);
const historyList = ref<FileInfo[]>([]);
const historyLoading = ref(false);

// 动态 chart 引用映射：columnTitle → HTMLDivElement
const chartRefs = new Map<string, HTMLDivElement>();
const chartInstances = new Map<string, echarts.ECharts>();

function setChartRef(title: string, el: any) {
  if (el) {
    chartRefs.set(title, el as HTMLDivElement);
  }
}

// 颜色轮转
const COLORS = ['#ff4757', '#409eff', '#67c23a', '#e6a23c', '#9b59b6', '#1abc9c', '#e74c3c', '#3498db'];

function getZoneColor(zone: string): string {
  if (zone.includes('热身')) return '#67c23a';
  if (zone.includes('燃脂')) return '#409eff';
  if (zone.includes('有氧')) return '#e6a23c';
  if (zone.includes('无氧')) return '#f56c6c';
  return '#909399';
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatTime(time: string): string {
  if (!time) return '';
  return time.replace('T', ' ').substring(0, 19);
}

function getUserId(): number | undefined {
  return userStore.user?.role === 'MEMBER' ? userStore.user.id : undefined;
}

// 为每个数值列渲染 ECharts 折线图
function renderCharts() {
  if (!analysisResult.value) return;

  analysisResult.value.columns.forEach((col, index) => {
    const el = chartRefs.get(col.title);
    if (!el) return;

    // 销毁旧实例（重新分析时）
    const old = chartInstances.get(col.title);
    if (old) old.dispose();

    const chart = echarts.init(el);
    chartInstances.set(col.title, chart);

    const color = COLORS[index % COLORS.length];
    const xData = col.data.map((_, i) => i);

    chart.setOption({
      title: {
        text: col.title,
        left: 'center',
        textStyle: { fontSize: 14, fontWeight: 'normal', color: '#606266' },
      },
      tooltip: {
        trigger: 'axis',
        formatter: (params: any) => {
          const p = Array.isArray(params) ? params[0] : params;
          return `${col.title}: ${p.value} @ ${p.axisIndex}s`;
        },
      },
      grid: {
        left: 50,
        right: 20,
        top: 40,
        bottom: 30,
      },
      xAxis: {
        type: 'category',
        data: xData,
        name: '秒 (s)',
        nameLocation: 'middle',
        nameGap: 25,
        axisLabel: { show: false },
      },
      yAxis: {
        type: 'value',
        name: col.title,
        min: (val: { min: number }) => Math.floor(val.min * 0.9),
        max: (val: { max: number }) => Math.ceil(val.max * 1.1),
      },
      series: [
        {
          name: col.title,
          type: 'line',
          data: col.data,
          smooth: true,
          symbol: 'none',
          lineStyle: { color, width: 1.5 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: color + '4D' },
              { offset: 1, color: color + '0D' },
            ]),
          },
          markLine: {
            silent: true,
            symbol: 'none',
            lineStyle: { type: 'dashed', color: '#909399', width: 1 },
            data: [{ yAxis: col.avg, name: 'avg', label: { formatter: `avg: ${col.avg}` } }],
          },
        },
      ],
    });
  });
}

async function handleCsvChange(file: any) {
  if (!userStore.user) {
    ElMessage.warning('请先登录');
    return;
  }

  selectedFileName.value = file.raw.name;
  historyLoading.value = true;
  try {
    analysisResult.value = await analyzeCsv(file.raw, userStore.user.id, userStore.user.username);
    ElMessage.success('分析完成，文件已自动保存');
    await nextTick();
    renderCharts();
    await loadHistory();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '分析失败');
  } finally {
    historyLoading.value = false;
  }
}

async function loadHistory() {
  historyLoading.value = true;
  try {
    const list = await getFileList(getUserId());
    historyList.value = list.filter((f) => f.originalName.toLowerCase().endsWith('.csv'));
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载历史记录失败');
  } finally {
    historyLoading.value = false;
  }
}

async function reAnalyze(fileInfo: FileInfo) {
  const url = getDownloadUrl(fileInfo.id);
  try {
    const response = await fetch(url);
    if (!response.ok) {
      ElMessage.error('获取文件失败');
      return;
    }
    const blob = await response.blob();
    const csvFile = new File([blob], fileInfo.originalName, { type: 'text/csv' });

    selectedFileName.value = fileInfo.originalName;
    historyLoading.value = true;
    analysisResult.value = await analyzeCsv(csvFile, userStore.user?.id, userStore.user?.username);
    ElMessage.success('重新分析完成');
    await nextTick();
    renderCharts();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '重新分析失败');
  } finally {
    historyLoading.value = false;
  }
}

function downloadFile(fileInfo: FileInfo) {
  const url = getDownloadUrl(fileInfo.id);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileInfo.originalName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

onMounted(() => {
  loadHistory();
});
</script>

<style scoped>
.csv-analysis-page {
  max-width: 960px;
  margin: 0 auto;
}

.upload-card {
  margin-bottom: 24px;
}

.upload-area {
  text-align: center;
}

.upload-icon {
  font-size: 48px;
  color: #409eff;
  margin-bottom: 8px;
}

.upload-text em {
  font-style: normal;
  font-size: 16px;
  color: #303133;
}

.upload-hint {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}

.result-section {
  margin-bottom: 24px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-value.duration {
  color: #e6a23c;
}

.column-section {
  margin-bottom: 20px;
}

.column-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.column-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.column-stats {
  display: flex;
  gap: 8px;
}

.chart-box {
  width: 100%;
  height: 320px;
}

.zones-card {
  margin-top: 16px;
}

.zones-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.zone-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.zone-name {
  font-weight: 500;
}

.zone-count {
  color: #909399;
}

.history-card {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
