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
                <span :class="(row.type === 'WITHDRAW' || row.type === 'CONSUME' || row.type === 'GYM_CONSUME') ? 'amount-minus' : 'amount-plus'">
                  {{ (row.type === 'WITHDRAW' || row.type === 'CONSUME' || row.type === 'GYM_CONSUME') ? '-' : '+' }}¥ {{ Math.abs(row.amount).toFixed(2) }}
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
    <el-dialog v-model="dialogVisible" :title="isCoach ? '提现' : '充值'" width="420px">
      <el-form :model="form" label-position="top">
        <el-form-item :label="isCoach ? '提现金额' : '充值金额'">
          <el-input-number
            v-model="form.amount"
            :min="0.01"
            :precision="2"
            :step="100"
            :placeholder="isCoach ? '请输入提现金额' : '请输入充值金额'"
            style="width: 100%"
          />
        </el-form-item>
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
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { recharge, withdraw, getBalance, getTransactions, exportBill, type WalletTransaction } from '../api/finance';
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

const exportDialogVisible = ref(false);
const exportLoading = ref(false);
const exportForm = reactive({
  type: 'CONSUME',
  period: 'MONTH',
});

const typeLabelMap: Record<string, string> = {
  RECHARGE: '充值',
  WITHDRAW: '提现',
  COURSE: '卖课收入',
  CONSUME: '课程消费',
  GYM_CONSUME: '健身卡消费',
  COURSE_INCOME: '卖课收入',
  PERSONAL_TRAINING: '私教收入',
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
      await withdraw({ userId, amount: form.amount, remark: form.remark });
      ElMessage.success('提现成功');
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

onMounted(() => {
  fetchBalance();
  fetchTransactions();
});
</script>

<style scoped>
.wallet-page {
  padding: 20px;
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
</style>
