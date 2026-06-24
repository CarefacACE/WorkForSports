<template>
  <div class="gym-page">
    <!-- ═══ Member: My Card Info Bar ═══ -->
    <div v-if="isMember" class="my-card-bar">
      <div v-if="myMembership" class="my-card-info">
        <div class="my-card-icon">🏋️</div>
        <div class="my-card-detail">
          <div class="my-card-name">{{ myMembership.cardName }}</div>
          <div class="my-card-meta">
            <span>{{ typeLabel(myMembership.cardType) }}</span>
            <span>到期：{{ myMembership.endDate }}</span>
            <span v-if="myMembership.cardType === 'VISIT'">剩余 {{ myMembership.remainingVisits }} 次</span>
            <span v-else>剩余 {{ myMembership.remainingDays }} 天</span>
          </div>
        </div>
        <el-tag :type="isValid ? 'success' : isExpiringSoon ? 'warning' : 'danger'" size="large">
          {{ isValid ? '有效' : '已过期' }}
        </el-tag>
      </div>
      <div v-else class="my-card-empty">
        <span>暂无健身卡，请从下方选购一张</span>
      </div>
    </div>

    <!-- ═══ Admin: CRUD Card ═══ -->
    <el-card v-if="isAdmin" shadow="never">
      <template #header>
        <div class="card-header">
          <span>💳 健身卡管理</span>
          <el-button type="primary" @click="openAddDialog">＋ 新增健身卡</el-button>
        </div>
      </template>

      <el-table :data="cards" v-loading="loading" border stripe style="flex: 1">
        <el-table-column prop="name" label="卡名" min-width="120" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="duration" label="有效期" width="100">
          <template #default="{ row }">{{ row.duration }} 天</template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- ═══ Member: Card Purchase Grid ═══ -->
    <div v-if="isMember">
      <el-card shadow="never" style="margin-top: 16px;">
        <template #header>
          <div class="card-header"><span>🛒 购买健身卡</span></div>
        </template>
        <div v-loading="loading" class="card-grid">
          <div v-for="card in cards" :key="card.id" class="gym-card-item">
            <div class="gym-card-type">
              <el-tag :type="typeTag(card.type)" effect="dark" size="large">{{ typeLabel(card.type) }}</el-tag>
            </div>
            <div class="gym-card-name">{{ card.name }}</div>
            <div class="gym-card-price">¥{{ card.price }}</div>
            <div class="gym-card-duration">{{ card.duration }} 天</div>
            <div class="gym-card-desc" v-if="card.description">{{ card.description }}</div>
            <el-button type="primary" class="gym-card-buy" @click="handlePurchase(card)">购买</el-button>
          </div>
          <el-empty v-if="cards.length === 0 && !loading" description="暂无可购买的健身卡" />
        </div>
      </el-card>
    </div>

    <!-- ═══ Add Dialog ═══ -->
    <el-dialog v-model="addDialogVisible" title="新增健身卡" width="480px">
      <el-form :model="addForm" label-position="top">
        <el-form-item label="卡名" required>
          <el-input v-model="addForm.name" placeholder="如：次卡、月卡、年卡" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类型" required>
              <el-select v-model="addForm.type" style="width: 100%">
                <el-option label="次卡" value="VISIT" />
                <el-option label="月卡" value="MONTHLY" />
                <el-option label="年卡" value="YEARLY" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="价格（元）" required>
              <el-input-number v-model="addForm.price" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="有效期（天）" required>
          <el-input-number v-model="addForm.duration" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="addForm.description" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleAdd">创建</el-button>
      </template>
    </el-dialog>

    <!-- ═══ Edit Dialog ═══ -->
    <el-dialog v-model="editDialogVisible" title="编辑健身卡" width="480px">
      <el-form :model="editForm" label-position="top">
        <el-form-item label="卡名">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类型">
              <el-select v-model="editForm.type" style="width: 100%">
                <el-option label="次卡" value="VISIT" />
                <el-option label="月卡" value="MONTHLY" />
                <el-option label="年卡" value="YEARLY" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="价格（元）">
              <el-input-number v-model="editForm.price" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="有效期（天）">
              <el-input-number v-model="editForm.duration" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="editForm.status" style="width: 100%">
                <el-option label="上架" value="ACTIVE" />
                <el-option label="下架" value="INACTIVE" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listGymCards, createGymCard, updateGymCard, deleteGymCard, getMyGymMembership, purchaseGymCard, type GymCard, type GymMembership } from '../api/gym';
import { useUserStore } from '../stores/user';

const userStore = useUserStore();
const isAdmin = computed(() => userStore.user?.role === 'ADMIN');
const isMember = computed(() => userStore.user?.role === 'MEMBER');
const userId = computed(() => userStore.user?.id || 0);

const loading = ref(false);
const submitLoading = ref(false);
const cards = ref<GymCard[]>([]);
const myMembership = ref<GymMembership | null>(null);

const isValid = computed(() => {
  if (!myMembership.value) return false;
  const m = myMembership.value;
  if (m.cardType === 'VISIT') return (m.remainingVisits ?? 0) > 0;
  return m.remainingDays > 0;
});

const isExpiringSoon = computed(() => {
  if (!isValid.value || !myMembership.value) return false;
  const m = myMembership.value;
  if (m.cardType === 'VISIT') return (m.remainingVisits ?? 0) <= 3;
  return m.remainingDays <= 7;
});

function typeLabel(type: string): string {
  const map: Record<string, string> = { VISIT: '次卡', MONTHLY: '月卡', YEARLY: '年卡' };
  return map[type] || type;
}

function typeTag(type: string): string {
  const map: Record<string, string> = { VISIT: '', MONTHLY: 'success', YEARLY: 'warning' };
  return map[type] || 'info';
}

// ═══ Admin CRUD ═══
const addDialogVisible = ref(false);
const editDialogVisible = ref(false);
const addForm = ref({ name: '', type: 'MONTHLY' as string, price: 0, duration: 30, description: '' });
const editForm = ref({ id: 0, name: '', type: '', price: 0, duration: 0, description: '', status: '' });

function openAddDialog() {
  addForm.value = { name: '', type: 'MONTHLY', price: 0, duration: 30, description: '' };
  addDialogVisible.value = true;
}

function openEditDialog(row: GymCard) {
  editForm.value = {
    id: row.id!,
    name: row.name,
    type: row.type,
    price: row.price,
    duration: row.duration,
    description: row.description || '',
    status: row.status || 'ACTIVE',
  };
  editDialogVisible.value = true;
}

async function handleAdd() {
  if (!addForm.value.name) { ElMessage.warning('请填写卡名'); return; }
  if (!addForm.value.price || addForm.value.price <= 0) { ElMessage.warning('请填写有效价格'); return; }
  if (!addForm.value.duration || addForm.value.duration <= 0) { ElMessage.warning('请填写有效天数'); return; }
  submitLoading.value = true;
  try {
    await createGymCard(addForm.value as GymCard);
    ElMessage.success('创建成功');
    addDialogVisible.value = false;
    await fetchCards();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败');
  } finally { submitLoading.value = false; }
}

async function handleEdit() {
  submitLoading.value = true;
  try {
    await updateGymCard(editForm.value as GymCard);
    ElMessage.success('更新成功');
    editDialogVisible.value = false;
    await fetchCards();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '更新失败');
  } finally { submitLoading.value = false; }
}

async function handleDelete(id: number) {
  try {
    await deleteGymCard(id);
    ElMessage.success('已删除');
    await fetchCards();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败');
  }
}

// ═══ Member Purchase ═══
async function handlePurchase(card: GymCard) {
  try {
    await ElMessageBox.confirm(`确定购买「${card.name}」？价格 ¥${card.price}，将从余额中扣除。`, '购买确认');
    await purchaseGymCard(userId.value, card.id!);
    ElMessage.success('购买成功');
    await fetchAll();
  } catch (e: unknown) {
    if (e !== 'cancel' && e?.toString() !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '购买失败');
    }
  }
}

// ═══ Data Fetching ═══
async function fetchCards() {
  loading.value = true;
  try {
    cards.value = await listGymCards(isAdmin.value ? undefined : 'ACTIVE');
  } catch { /* ignore */ }
  loading.value = false;
}

async function fetchMembership() {
  if (!isMember.value) return;
  try {
    myMembership.value = await getMyGymMembership(userId.value);
  } catch { myMembership.value = null; }
}

async function fetchAll() {
  await Promise.all([fetchCards(), fetchMembership()]);
}

onMounted(fetchAll);
</script>

<style scoped>
.gym-page { padding: 0; display: flex; flex-direction: column; min-height: calc(100vh - 120px); }
.gym-page :deep(.el-card) { flex: 1; display: flex; flex-direction: column; }
.gym-page :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; }
.gym-page :deep(.el-table) { flex: 1; }
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }

/* Member info bar */
.my-card-bar { margin-bottom: 16px; }
.my-card-info { display: flex; align-items: center; gap: 16px; padding: 20px 24px; background: linear-gradient(135deg, #dbeafe, #eff6ff); border-radius: 12px; border: 1px solid #93c5fd; }
.my-card-icon { font-size: 36px; }
.my-card-detail { flex: 1; }
.my-card-name { font-size: 18px; font-weight: 700; color: #1e3a8a; }
.my-card-meta { display: flex; gap: 12px; margin-top: 4px; font-size: 13px; color: #3b82f6; }
.my-card-empty { padding: 20px 24px; background: #fef3c7; border-radius: 12px; border: 1px solid #fcd34d; text-align: center; color: #92400e; font-size: 14px; }

/* Card grid */
.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 20px; }
.gym-card-item { border: 1px solid #e5e7eb; border-radius: 16px; padding: 24px; text-align: center; transition: all 0.2s; background: #fff; }
.gym-card-item:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.08); border-color: #3b82f6; }
.gym-card-type { margin-bottom: 12px; }
.gym-card-name { font-size: 18px; font-weight: 700; color: #1e293b; }
.gym-card-price { font-size: 32px; font-weight: 800; color: #ef4444; margin: 8px 0 4px; }
.gym-card-duration { font-size: 13px; color: #94a3b8; margin-bottom: 8px; }
.gym-card-desc { font-size: 12px; color: #64748b; margin-bottom: 16px; min-height: 32px; }
.gym-card-buy { width: 100%; }

/* Dark theme */
:global([data-admin-theme="dark"]) .my-card-info { background: linear-gradient(135deg, rgba(59,130,246,0.15), rgba(59,130,246,0.08)); border-color: rgba(59,130,246,0.3); }
:global([data-admin-theme="dark"]) .my-card-name { color: #93c5fd; }
:global([data-admin-theme="dark"]) .my-card-meta { color: #60a5fa; }
:global([data-admin-theme="dark"]) .my-card-empty { background: rgba(245,158,11,0.1); border-color: rgba(245,158,11,0.3); color: #fbbf24; }
:global([data-admin-theme="dark"]) .gym-card-item { background: #1a1c28; border-color: rgba(255,255,255,0.07); }
:global([data-admin-theme="dark"]) .gym-card-item:hover { border-color: rgba(59,130,246,0.4); box-shadow: 0 8px 24px rgba(0,0,0,0.3); }
:global([data-admin-theme="dark"]) .gym-card-name { color: #e8eaed; }
:global([data-admin-theme="dark"]) .gym-card-desc { color: #6b7084; }
:global([data-admin-theme="dark"]) .gym-card-duration { color: #4a4e63; }
</style>
