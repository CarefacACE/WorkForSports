<template>
  <div class="wallet-page">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover" class="balance-card">
          <div class="balance-label">当前余额</div>
          <div class="balance-amount">¥ {{ balance.toFixed(2) }}</div>
          <el-button v-if="isCoach" type="warning" class="action-btn" @click="dialogVisible = true">提现</el-button>
          <el-button v-else type="primary" class="action-btn" @click="dialogVisible = true">充值</el-button>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span>{{ isCoach ? '收入记录' : '交易记录' }}</span>
              <el-button type="primary" link @click="exportDialogVisible = true">导出账单</el-button>
            </div>
          </template>
          <el-table :data="transactions" v-loading="tableLoading" stripe>
            <el-table-column prop="amount" label="金额" width="120">
              <template #default="{ row }">
                <span :class="(row.type === 'WITHDRAW' || row.type === 'CONSUME' || row.type === 'GYM_CONSUME' || row.type === 'COMMISSION' || row.type === 'AUTO_DEDUCT') ? 'amount-minus' : 'amount-plus'">
                  {{ (row.type === 'WITHDRAW' || row.type === 'CONSUME' || row.type === 'GYM_CONSUME' || row.type === 'COMMISSION' || row.type === 'AUTO_DEDUCT') ? '-' : '+' }}¥ {{ Math.abs(row.amount).toFixed(2) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getTypeTagType(row.type)" size="small">{{ getTypeLabel(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="150" />
            <el-table-column prop="createTime" label="时间" width="180" />
          </el-table>
          <el-pagination
            class="pagination"
            background
            layout="prev, pager, next, total"
            :total="total"
            :page-size="pageSize"
            :current-page="currentPage"
            @current-change="handlePageChange"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 充值/提现对话框 -->
    <el-dialog v-model="dialogVisible" :title="isCoach ? '提现' : '充值'" width="460px">
      <el-form :model="form" label-position="top">
        <!-- 教练提现时显示段位信息 -->
        <el-alert v-if="isCoach" :title="'当前段位：' + (tierInfo?.tierName || '加载中...')" type="info" :closable="false"
          style="margin-bottom: 16px;">
          <template #default>
            <p style="margin: 4px 0;">
              抽成比例：<strong>{{ ((tierInfo?.commissionRate ?? 0) * 100).toFixed(0) }}%</strong>
              &nbsp;&nbsp;
              <el-button type="primary" link size="small" @click="openCommissionDetail">
                <el-icon><InfoFilled /></el-icon> 查看抽成详情
              </el-button>
            </p>
          </template>
        </el-alert>
        <el-form-item :label="isCoach ? '提现金额（毛金额）' : '充值金额'">
          <el-input-number
            v-model="form.amount"
            :min="0.01"
            :precision="2"
            :step="100"
            :placeholder="isCoach ? '请输入提现金额' : '请输入充值金额'"
            style="width: 100%"
          />
        </el-form-item>
        <!-- 教练提现时显示预估到账 -->
        <el-alert v-if="isCoach && form.amount > 0 && tierInfo" title="预估到账" type="success" :closable="false"
          style="margin-bottom: 12px;">
          <template #default>
            <p style="margin: 4px 0;">
              提现 {{ form.amount.toFixed(2) }}，抽成 {{ tierInfo.commissionRate ? (form.amount * tierInfo.commissionRate).toFixed(2) : '0.00' }}，
              实际到手 <strong>¥{{ (form.amount * (1 - (tierInfo.commissionRate ?? 0))).toFixed(2) }}</strong>
            </p>
          </template>
        </el-alert>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="请输入备注（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :type="isCoach ? 'warning' : 'primary'" :loading="submitLoading" @click="handleSubmit">
          {{ isCoach ? '确认提现' : '确认充值' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 抽成详情对话框 -->
    <el-dialog v-model="commissionDetailVisible" title="抽成详情" width="620px">
      <div v-if="commissionDetail" class="commission-detail">
        <!-- 当前段位总览 -->
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

        <!-- 段位列表 -->
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
              <!-- 进度条（仅当前段位显示） -->
              <div v-if="tier.isCurrent && tier.progressPercent < 100" class="tier-progress">
                <el-progress
                  :percentage="Math.min(Number(tier.progressPercent), 100)"
                  :stroke-width="8"
                  :show-text="true"
                >
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

    <!-- 导出账单对话框 -->
    <el-dialog v-model="exportDialogVisible" title="导出账单" width="420px">
      <el-form label-position="top">
        <el-form-item v-if="!isCoach" label="账单类型">
          <el-radio-group v-model="exportForm.type">
            <el-radio value="CONSUME">消费记录</el-radio>
            <el-radio value="RECHARGE">充值记录</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-radio-group v-model="exportForm.period">
            <el-radio value="WEEK">本周</el-radio>
            <el-radio value="MONTH">本月</el-radio>
            <el-radio value="YEAR">本年</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="exportLoading" @click="handleExport">确认导出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { InfoFilled } from '@element-plus/icons-vue';
import { recharge, withdraw, getBalance, getCommissionTier, getCommissionDetail, getTransactions, exportBill, type WalletTransaction, type WithdrawResult, type CommissionDetailResult } from '../api/finance';
import { useUserStore } from '../stores/user';

const userStore = useUserStore();
const userId = userStore.user?.id;
const isCoach = computed(() => userStore.user?.role === 'COACH');

const balance = ref(0);
const transactions = ref<WalletTransaction[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const tableLoading = ref(false);

const dialogVisible = ref(false);
const submitLoading = ref(false);
const form = reactive({
  amount: 100,
  remark: '',
});

// 段位信息
const tierInfo = ref<WithdrawResult | null>(null);
const showTierPreview = computed(() => isCoach.value && form.amount > 0 && tierInfo.value);

const exportDialogVisible = ref(false);
const exportLoading = ref(false);
const exportForm = reactive({
  type: 'CONSUME',
  period: 'MONTH',
});

// 抽成详情
const commissionDetailVisible = ref(false);
const commissionDetail = ref<CommissionDetailResult | null>(null);

const typeLabelMap: Record<string, string> = {
  RECHARGE: '充值',
  WITHDRAW: '提现',
  COURSE: '卖课收入',
  CONSUME: '课程消费',
  GYM_CONSUME: '健身卡消费',
  COURSE_INCOME: '卖课收入',
  PERSONAL_TRAINING: '私教收入',
  COMMISSION: '平台抽成',
  ADJUST: '余额调整',
};

const typeTagMap: Record<string, string> = {
  RECHARGE: 'success',
  WITHDRAW: 'warning',
  COURSE: '',
  CONSUME: 'danger',
  GYM_CONSUME: 'danger',
  COURSE_INCOME: 'success',
  PERSONAL_TRAINING: '',
  COMMISSION: 'info',
  ADJUST: 'info',
};

function getTypeLabel(type: string) {
  return typeLabelMap[type] || type;
}

function getTypeTagType(type: string) {
  return (typeTagMap[type] || '') as any;
}

async function fetchBalance() {
  if (!userId) return;
  try {
    balance.value = await getBalance(userId);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取余额失败');
  }
}

async function fetchTransactions() {
  if (!userId) return;
  tableLoading.value = true;
  try {
    const result = await getTransactions(userId, currentPage.value, pageSize.value);
    transactions.value = result.records;
    total.value = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取记录失败');
  } finally {
    tableLoading.value = false;
  }
}

async function handleSubmit() {
  if (!userId) {
    ElMessage.warning('请先登录');
    return;
  }
  if (!form.amount || form.amount <= 0) {
    ElMessage.warning(isCoach.value ? '请输入有效的提现金额' : '请输入有效的充值金额');
    return;
  }

  submitLoading.value = true;
  try {
    if (isCoach.value) {
      const result = await withdraw({ userId, amount: form.amount, remark: form.remark });
      ElMessage.success(
        `提现申请成功！实际到账 ¥${result.netAmount.toFixed(2)}（平台抽成 ¥${result.commissionAmount.toFixed(2)}，段位：${result.tierName}）`
      );
    } else {
      await recharge({ userId, amount: form.amount, remark: form.remark });
      ElMessage.success('充值成功');
    }
    dialogVisible.value = false;
    form.amount = 100;
    form.remark = '';
    await fetchBalance();
    await fetchTransactions();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : (isCoach.value ? '提现失败' : '充值失败'));
  } finally {
    submitLoading.value = false;
  }
}

// 教练打开提现弹窗时，预取段位信息
watch(dialogVisible, async (visible) => {
  if (visible && isCoach.value && userId) {
    try {
      tierInfo.value = await getCommissionTier(userId);
    } catch { tierInfo.value = null; }
  }
});

function handlePageChange(page: number) {
  currentPage.value = page;
  fetchTransactions();
}

async function handleExport() {
  if (!userId) return;
  exportLoading.value = true;
  try {
    const type = isCoach.value ? 'COURSE_INCOME' : exportForm.type;
    await exportBill(userId, exportForm.period, type);
    ElMessage.success('导出成功');
    exportDialogVisible.value = false;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败');
  } finally {
    exportLoading.value = false;
  }
}

// 抽成详情
async function openCommissionDetail() {
  if (!userId) return;
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
  fetchBalance();
  fetchTransactions();
});
</script>

<style scoped>
.wallet-page {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  gap: 20px;
}

.wallet-page .el-card {
  height: 100%;
}

.balance-card {
  text-align: center;
  padding: 20px 0;
}

.balance-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.balance-amount {
  font-size: 36px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 20px;
}

.action-btn {
  width: 80%;
}

.amount-plus {
  color: #67c23a;
  font-weight: bold;
}

.amount-minus {
  color: #e6a23c;
  font-weight: bold;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
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
