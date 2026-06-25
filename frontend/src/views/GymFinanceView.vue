<template>
  <div class="finance-page">
    <!-- ═══ 筛选栏 ═══ -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <span class="filter-label">统计周期</span>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @change="onDateChange"
          clearable
        />
        <el-button type="primary" @click="fetchAll" :loading="loading">刷新数据</el-button>
      </div>
    </el-card>

    <!-- ═══ 总览卡片 ═══ -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <el-card shadow="never" class="stat-card" :style="{ borderTop: `3px solid ${card.color}` }">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value" :style="{ color: card.color }">
            <template v-if="card.prefix === '¥'">¥{{ card.value }}</template>
            <template v-else-if="card.suffix === '%'">{{ card.value }}%</template>
            <template v-else>{{ card.value }}</template>
          </div>
          <div class="stat-icon" v-html="card.icon"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ═══ 月度趋势图 & 商品维度表 ═══ -->
    <el-row :gutter="16">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header><span>📈 月度收支趋势</span></template>
          <div ref="chartRef" style="height: 340px;"></div>
          <div v-if="!monthlyData.length && !loading" style="text-align:center;color:#94a3b8;padding:40px 0;">
            暂无销售数据
          </div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span>📊 盈亏总览</span></template>
          <div style="padding: 8px 0;">
            <div class="mini-stat-item">
              <span class="mini-stat-label">商品种类</span>
              <span class="mini-stat-value">{{ overview.productCount || 0 }} 种</span>
            </div>
            <div class="mini-stat-item">
              <span class="mini-stat-label">售出总量</span>
              <span class="mini-stat-value">{{ overview.totalSoldQuantity || 0 }} 件</span>
            </div>
            <el-divider style="margin: 12px 0;" />
            <div class="mini-stat-item profit-item">
              <span class="mini-stat-label">总营收</span>
              <span class="mini-stat-value revenue">¥{{ overview.totalRevenue || '0.00' }}</span>
            </div>
            <div class="mini-stat-item">
              <span class="mini-stat-label">总成本</span>
              <span class="mini-stat-value cost">¥{{ overview.totalCost || '0.00' }}</span>
            </div>
            <div class="mini-stat-item profit-item">
              <span class="mini-stat-label">毛利润</span>
              <span class="mini-stat-value" :class="profitClass">{{ overview.grossProfit || '0.00' }}</span>
            </div>
            <div class="mini-stat-item">
              <span class="mini-stat-label">利润率</span>
              <span class="mini-stat-value" :class="profitClass">{{ overview.profitMargin || '0.00' }}%</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ═══ 商品维度分析表 ═══ -->
    <el-card shadow="never" class="table-card">
      <template #header><span>📦 商品维度分析</span></template>
      <el-table :data="productData" v-loading="loading" border stripe default-sort="{ prop: 'profit', order: 'descending' }" empty-text="暂无销售数据">
        <el-table-column prop="productName" label="商品名称" min-width="120" />
        <el-table-column prop="soldQuantity" label="销量" width="80" sortable />
        <el-table-column label="营收" width="120" sortable prop="totalRevenue">
          <template #default="{ row }">¥{{ row.totalRevenue }}</template>
        </el-table-column>
        <el-table-column label="成本(单价)" width="110">
          <template #default="{ row }">¥{{ row.unitCost }}</template>
        </el-table-column>
        <el-table-column label="总成本" width="110" sortable prop="totalCost">
          <template #default="{ row }">¥{{ row.totalCost }}</template>
        </el-table-column>
        <el-table-column label="利润" width="120" sortable prop="profit">
          <template #default="{ row }">
            <span :class="row.profit >= 0 ? 'text-green' : 'text-red'">
              {{ row.profit >= 0 ? '+' : '' }}¥{{ row.profit }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="利润率" width="180" sortable prop="profitMargin">
          <template #default="{ row }">
            <div class="margin-bar-wrapper">
              <el-progress
                :percentage="Math.abs(Math.round(row.profitMargin))"
                :color="row.profitMargin >= 0 ? '#10b981' : '#ef4444'"
                :stroke-width="16"
                :text-inside="true"
              >
                <span>{{ row.profitMargin.toFixed(1) }}%</span>
              </el-progress>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import {
  getFinanceOverview,
  getProductFinanceBreakdown,
  getFinanceMonthlyTrend,
  type FinanceOverview,
  type ProductFinance,
  type MonthlyFinance,
} from '../api/gym';

const loading = ref(false);
const dateRange = ref<[string, string] | null>(null);

const overview = ref<FinanceOverview>({
  totalRevenue: 0, totalCost: 0, grossProfit: 0,
  profitMargin: 0, totalSoldQuantity: 0, productCount: 0,
});

const productData = ref<ProductFinance[]>([]);
const monthlyData = ref<MonthlyFinance[]>([]);
const chartRef = ref<HTMLElement | null>(null);
let chartInstance: echarts.ECharts | null = null;

const profitClass = computed(() => {
  const p = overview.value.grossProfit || 0;
  return p >= 0 ? 'text-green' : 'text-red';
});

const statCards = computed(() => [
  { label: '总营收', value: formatMoney(overview.value.totalRevenue || 0), prefix: '¥', color: '#3b82f6', icon: svgArrow('up') },
  { label: '总成本', value: formatMoney(overview.value.totalCost || 0), prefix: '¥', color: '#f59e0b', icon: svgArrow('down') },
  { label: '毛利润', value: formatMoney(overview.value.grossProfit || 0), prefix: '¥', color: (overview.value.grossProfit || 0) >= 0 ? '#10b981' : '#ef4444', icon: svgArrow('up') },
  { label: '利润率', value: (overview.value.profitMargin || 0).toFixed(1), suffix: '%', color: '#8b5cf6', icon: '%' },
  { label: '总售出', value: String(overview.value.totalSoldQuantity || 0), prefix: '', color: '#06b6d4', icon: '#' },
]);

function formatMoney(val: number): string {
  return val.toFixed(2);
}

function svgArrow(dir: string): string {
  return `<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><polyline points="${dir === 'up' ? '18 15 12 9 6 15' : '6 9 12 15 18 9'}"/></svg>`;
}

function onDateChange() {
  fetchAll();
}

async function fetchAll() {
  loading.value = true;
  try {
    const startDate = dateRange.value?.[0] || undefined;
    const endDate = dateRange.value?.[1] || undefined;

    const [ov, prod, monthly] = await Promise.all([
      getFinanceOverview(startDate, endDate),
      getProductFinanceBreakdown(startDate, endDate),
      getFinanceMonthlyTrend(),
    ]);

    overview.value = ov;
    productData.value = prod || [];
    monthlyData.value = monthly || [];
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '获取数据失败');
  } finally {
    loading.value = false;
  }

  await nextTick();
  renderChart();
}

function renderChart() {
  if (!chartRef.value) return;

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value);
  }

  const months = monthlyData.value.map(d => d.month);
  const revenues = monthlyData.value.map(d => d.revenue);
  const costs = monthlyData.value.map(d => d.cost);
  const profits = monthlyData.value.map(d => d.profit);

  if (months.length === 0) {
    chartInstance.clear();
    return;
  }

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      valueFormatter: (v: number) => '¥' + v.toFixed(2),
    },
    legend: {
      data: ['营收', '成本', '利润'],
      top: 0,
    },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      data: months,
      axisLabel: { fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      axisLabel: { formatter: '¥{value}' },
    },
    series: [
      {
        name: '营收',
        type: 'line',
        data: revenues,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: '#3b82f6' },
        areaStyle: { color: 'rgba(59,130,246,0.08)' },
      },
      {
        name: '成本',
        type: 'line',
        data: costs,
        smooth: true,
        symbol: 'diamond',
        symbolSize: 6,
        lineStyle: { width: 2, color: '#f59e0b' },
        areaStyle: { color: 'rgba(245,158,11,0.08)' },
      },
      {
        name: '利润',
        type: 'line',
        data: profits,
        smooth: true,
        symbol: 'triangle',
        symbolSize: 6,
        lineStyle: { width: 2, color: '#10b981' },
        areaStyle: { color: 'rgba(16,185,129,0.08)' },
      },
    ],
  });
}

// 窗口 resize 自适应
watch(() => chartRef.value, () => {
  if (chartInstance) {
    const obs = new ResizeObserver(() => chartInstance?.resize());
    if (chartRef.value) obs.observe(chartRef.value);
  }
});

onMounted(fetchAll);
</script>

<style scoped>
.finance-page { display: flex; flex-direction: column; gap: 16px; }

/* ─── Filter ─── */
.filter-card { flex-shrink: 0; }
.filter-bar { display: flex; align-items: center; gap: 12px; }
.filter-label { font-weight: 600; color: #475569; font-size: 14px; white-space: nowrap; }

/* ─── Stat Cards ─── */
.stat-row { flex-shrink: 0; }
.stat-card { position: relative; border-radius: 12px; transition: transform 0.15s; }
.stat-card:hover { transform: translateY(-2px); }
.stat-label { font-size: 13px; color: #64748b; margin-bottom: 6px; }
.stat-value { font-size: 26px; font-weight: 800; letter-spacing: -0.5px; }
.stat-icon { position: absolute; top: 16px; right: 16px; opacity: 0.15; width: 32px; height: 32px; font-size: 32px; font-weight: 800; }

/* ─── Mini Stats ─── */
.mini-stat-item { display: flex; justify-content: space-between; padding: 6px 0; font-size: 14px; }
.mini-stat-label { color: #64748b; }
.mini-stat-value { font-weight: 600; color: #1e293b; }
.profit-item { padding: 8px 0; }

/* ─── Table ─── */
.table-card { flex: 1; }
.text-green { color: #10b981; font-weight: 600; }
.text-red { color: #ef4444; font-weight: 600; }
.margin-bar-wrapper { padding-right: 8px; }

.revenue { color: #3b82f6; }
.cost { color: #f59e0b; }

/* ─── Dark theme ─── */
:global([data-admin-theme="dark"]) .stat-card { background: #1a1c28; }
:global([data-admin-theme="dark"]) .stat-label { color: #6b7084; }
:global([data-admin-theme="dark"]) .mini-stat-label { color: #6b7084; }
:global([data-admin-theme="dark"]) .mini-stat-value { color: #e8eaed; }
:global([data-admin-theme="dark"]) .filter-label { color: #9ca3af; }
</style>
