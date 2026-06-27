<template>
  <div class="user-manage-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>教练工资管理</span>
        </div>
      </template>

      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="keyword" placeholder="搜索用户名/姓名/手机号" clearable style="width: 280px"
            @clear="handleSearch" @keyup.enter="handleSearch">
            <template #append>
              <el-button @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="余额" width="120" align="center">
          <template #default="{ row }">
            <span class="balance-text">¥ {{ (row.balance ?? 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="累计收入" width="130" align="center">
          <template #default="{ row }">
            <span class="earnings-text">¥ {{ (tierCache[row.id]?.totalEarnings ?? 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="当前段位" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getTierTagType(tierCache[row.id]?.commissionRate ?? 0)" size="small">
              {{ tierCache[row.id]?.tierName || '加载中...' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="抽成比例" width="100" align="center">
          <template #default="{ row }">
            <span>{{ ((tierCache[row.id]?.commissionRate ?? 0) * 100).toFixed(0) }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEditBalance(row)">修改余额</el-button>
            <el-button type="primary" link @click="openCommissionDetail(row.id)">抽成详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="修改余额" width="420px">
      <el-form label-position="top">
        <el-form-item label="教练">
          <el-input :model-value="editForm.username + '（' + editForm.realName + '）'" disabled />
        </el-form-item>
        <el-form-item label="当前余额">
          <el-input :model-value="'¥ ' + (editForm.oldBalance ?? 0).toFixed(2)" disabled />
        </el-form-item>
        <el-form-item label="新余额">
          <el-input-number v-model="editForm.newBalance" :min="0" :precision="2" :step="100" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmitBalance">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 抽成详情对话框 -->
    <el-dialog v-model="commissionDetailVisible" title="抽成详情" width="620px">
      <div v-if="commissionDetail" class="commission-detail">
        <div class="tier-summary">
          <el-row :gutter="16">
            <el-col :span="8">
              <div class="stat-box">
                <div class="stat-label">当前段位</div>
                <div class="stat-value tier-name">{{ commissionDetail.currentTierName }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-box">
                <div class="stat-label">抽成比例</div>
                <div class="stat-value">{{ (commissionDetail.currentRate * 100).toFixed(0) }}%</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-box">
                <div class="stat-label">累计收入</div>
                <div class="stat-value">¥{{ (commissionDetail.totalEarnings ?? 0).toFixed(2) }}</div>
              </div>
            </el-col>
          </el-row>
        </div>
        <div class="tier-list-title">全部段位与升级进度</div>
        <div class="tier-list">
          <div
            v-for="tier in commissionDetail.allTiers"
            :key="tier.name"
            class="tier-item"
            :class="{ 'is-current': tier.isCurrent, 'is-reached': tier.reached && !tier.isCurrent, 'is-locked': !tier.reached }"
          >
            <div class="tier-rank-badge">
              <span v-if="tier.isCurrent" class="badge-current">当前</span>
              <span v-else-if="tier.reached" class="badge-reached">✓</span>
              <span v-else class="badge-locked">🔒</span>
            </div>
            <div class="tier-info">
              <div class="tier-name">{{ tier.name }}</div>
              <div class="tier-rate">抽成 {{ (tier.rate * 100).toFixed(0) }}%</div>
              <div class="tier-range">
                累计收入 {{ tier.minEarnings.toFixed(0) }} ~ {{ tier.maxEarnings < 0 ? '∞' : tier.maxEarnings.toFixed(0) }}
              </div>
              <div v-if="tier.isCurrent && tier.progressPercent < 100" class="tier-progress">
                <el-progress :percentage="Math.min(Number(tier.progressPercent), 100)" :stroke-width="8" :show-text="true">
                  <template #default="{ percentage }">
                    <span class="progress-text">{{ Number(tier.progressPercent).toFixed(1) }}%</span>
                  </template>
                </el-progress>
                <div v-if="getNextTierName(tier.name)" class="upgrade-hint">
                  距离升级至<strong>{{ getNextTierName(tier.name) }}</strong>还需累计赚取
                  <strong>¥{{ (tier.amountToNextTier ?? 0).toFixed(0) }}</strong>
                </div>
              </div>
              <div v-else-if="tier.isCurrent && tier.progressPercent >= 100" class="tier-progress">
                <el-progress :percentage="100" :stroke-width="8" color="#67c23a" />
                <div class="upgrade-hint reached">已达最高段位</div>
              </div>
              <div v-else-if="tier.reached && tier.maxEarnings >= 0" class="tier-progress">
                <el-progress :percentage="100" :stroke-width="8" color="#67c23a" />
                <div class="upgrade-hint reached">已达标</div>
              </div>
              <div v-else-if="!tier.reached" class="tier-progress">
                <el-progress :percentage="0" :stroke-width="8" color="#c0c4cc" />
                <div class="upgrade-hint locked">尚未解锁</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getUsers, type UserItem } from '../api/user';
import { updateBalance, getCommissionTier, getCommissionDetail, type CommissionDetailResult } from '../api/finance';

const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const keyword = ref('');
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableData = ref<UserItem[]>([]);

// 段位信息缓存
const tierCache = ref<Record<number, { tierName: string; commissionRate: number; totalEarnings: number }>>({});

const editForm = reactive({
  userId: 0,
  username: '',
  realName: '',
  oldBalance: 0,
  newBalance: 0,
});

async function fetchData() {
  loading.value = true;
  try {
    const res = await getUsers({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      role: 'COACH',
    });
    tableData.value = res.records;
    total.value = res.total;

    // 批量获取段位信息
    for (const coach of res.records) {
      if (!tierCache.value[coach.id]) {
        try {
          const tier = await getCommissionTier(coach.id);
          tierCache.value[coach.id] = {
            tierName: tier.tierName,
            commissionRate: tier.commissionRate,
            totalEarnings: tier.grossAmount,  // 复用字段传累计收入
          };
        } catch {
          tierCache.value[coach.id] = { tierName: '-', commissionRate: 0, totalEarnings: 0 };
        }
      }
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取数据失败');
  } finally {
    loading.value = false;
  }
}

function getTierTagType(rate: number): string {
  if (rate >= 0.40) return 'danger';
  if (rate >= 0.28) return 'warning';
  if (rate >= 0.18) return '';
  return 'success';
}

function handleSearch() {
  pageNum.value = 1;
  fetchData();
}

function handleSizeChange() {
  pageNum.value = 1;
  fetchData();
}

function handlePageChange() {
  fetchData();
}

function handleEditBalance(row: UserItem) {
  editForm.userId = row.id;
  editForm.username = row.username;
  editForm.realName = row.realName || '';
  editForm.oldBalance = (row as any).balance ?? 0;
  editForm.newBalance = editForm.oldBalance;
  dialogVisible.value = true;
}

async function handleSubmitBalance() {
  submitLoading.value = true;
  try {
    await updateBalance(editForm.userId, editForm.newBalance);
    ElMessage.success('余额修改成功');
    dialogVisible.value = false;
    fetchData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '修改失败');
  } finally {
    submitLoading.value = false;
  }
}

// 抽成详情
const commissionDetailVisible = ref(false);
const commissionDetail = ref<CommissionDetailResult | null>(null);

async function openCommissionDetail(userId: number) {
  try {
    commissionDetail.value = await getCommissionDetail(userId);
    commissionDetailVisible.value = true;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取抽成详情失败');
  }
}

function getNextTierName(currentName: string): string {
  if (!commissionDetail.value) return '';
  const tiers = commissionDetail.value.allTiers;
  const idx = tiers.findIndex(t => t.name === currentName);
  if (idx >= 0 && idx < tiers.length - 1) {
    return tiers[idx + 1].name;
  }
  return '';
}

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.user-manage-view {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.balance-text {
  color: #409eff;
  font-weight: bold;
}

.earnings-text {
  color: #67c23a;
  font-weight: bold;
}

/* 抽成详情对话框样式 */
.commission-detail {
  padding: 0;
}

.tier-summary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
}

.tier-summary .stat-box {
  text-align: center;
  color: #fff;
}

.tier-summary .stat-label {
  font-size: 12px;
  opacity: 0.85;
  margin-bottom: 6px;
}

.tier-summary .stat-value {
  font-size: 18px;
  font-weight: bold;
}

.tier-summary .stat-value.tier-name {
  font-size: 16px;
}

.tier-list-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-left: 4px;
}

.tier-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 420px;
  overflow-y: auto;
}

.tier-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 10px;
  border: 1.5px solid #e4e7ed;
  background: #fafafa;
  transition: all 0.2s;
}

.tier-item.is-current {
  border-color: #667eea;
  background: linear-gradient(135deg, #f0f1ff 0%, #e8e9ff 100%);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.15);
}

.tier-item.is-reached {
  border-color: #c0e0c0;
  background: #f6fdf6;
}

.tier-item.is-locked {
  opacity: 0.65;
}

.tier-rank-badge {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 16px;
}

.badge-current {
  background: #667eea;
  color: #fff;
  font-size: 11px;
  font-weight: bold;
  padding: 4px 8px;
  border-radius: 12px;
}

.badge-reached {
  background: #67c23a;
  color: #fff;
  font-size: 14px;
  font-weight: bold;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.badge-locked {
  font-size: 20px;
  opacity: 0.5;
}

.tier-info {
  flex: 1;
  min-width: 0;
}

.tier-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.tier-rate {
  font-size: 13px;
  color: #e6a23c;
  margin-bottom: 2px;
}

.tier-range {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.tier-progress {
  margin-top: 6px;
}

.upgrade-hint {
  font-size: 12px;
  color: #606266;
  margin-top: 4px;
  line-height: 1.6;
}

.upgrade-hint.reached {
  color: #67c23a;
}

.upgrade-hint.locked {
  color: #c0c4cc;
}

.progress-text {
  font-size: 11px;
  font-weight: bold;
}
</style>
