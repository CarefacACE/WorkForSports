<template>
  <div class="gym-page">
    <!-- ═══ Member: My Card Info Bar ═══ -->
    <div v-if="isMember" class="my-card-bar">
      <div v-if="myMembership" class="my-card-info" :class="myMembership.cardHolderType === 'SUB' ? 'is-sub' : ''">
        <div class="my-card-icon">🏋️</div>
        <div class="my-card-detail">
          <div class="my-card-name">
            {{ myMembership.cardName }}
            <el-tag v-if="myMembership.cardHolderType === 'SUB'" size="small" type="warning">副卡</el-tag>
          </div>
          <div class="my-card-meta">
            <span>{{ typeLabel(myMembership.cardType) }}</span>
            <span v-if="myMembership.endDate">到期：{{ myMembership.endDate }}</span>
            <span v-if="myMembership.cardCategory === 'SESSION' && myMembership.remainingVisits !== undefined">
              剩余 {{ myMembership.remainingVisits }} 次
            </span>
            <span v-else-if="myMembership.cardCategory === 'TIME'">
              剩余 {{ myMembership.remainingDays }} 天
            </span>
            <span v-if="myMembership.primaryOwnerName">
              ｜主卡持有人：{{ myMembership.primaryOwnerName }}
            </span>
          </div>
        </div>
        <div class="my-card-actions">
          <el-tag :type="isValid ? 'success' : isExpiringSoon ? 'warning' : 'danger'" size="large">
            {{ isValid ? '有效' : '已过期' }}
          </el-tag>
          <!-- 主卡次卡 → 副卡管理 -->
          <el-button v-if="canManageSubCards" type="primary" link size="small" @click="openSubCardDialog">
            管理副卡 ({{ myMembership.subCards?.length || 0 }})
          </el-button>
        </div>
      </div>
      <div v-else class="my-card-empty">
        <span>暂无健身卡，请从下方选购一张</span>
      </div>
    </div>

    <!-- ═══ Admin: CRUD Card ═══ -->
    <template v-if="isAdmin">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <span>💳 健身卡管理</span>
            <el-button type="primary" @click="openAddDialog">＋ 新增健身卡</el-button>
          </div>
        </template>

        <!-- Admin tabs -->
        <el-tabs v-model="adminTab" @tab-change="fetchCards">
          <el-tab-pane label="次卡管理" name="SESSION">
            <el-table :data="sessionCards" v-loading="loading" border stripe>
              <el-table-column prop="name" label="卡名" min-width="120" />
              <el-table-column label="次数" width="80">
                <template #default="{ row }">{{ row.duration }} 次</template>
              </el-table-column>
              <el-table-column label="副卡上限" width="90">
                <template #default="{ row }">{{ row.subCardLimit ?? 2 }} 张</template>
              </el-table-column>
              <el-table-column prop="price" label="价格" width="100">
                <template #default="{ row }">¥{{ row.price }}</template>
              </el-table-column>
              <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
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
                  <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id!)">
                    <template #reference>
                      <el-button type="danger" link size="small">删除</el-button>
                    </template>
                  </el-popconfirm>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="时间卡管理" name="TIME">
            <el-table :data="timeCards" v-loading="loading" border stripe>
              <el-table-column prop="name" label="卡名" min-width="120" />
              <el-table-column label="类型" width="90">
                <template #default="{ row }">
                  <el-tag :type="typeTag(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="有效期" width="90">
                <template #default="{ row }">
                  {{ row.type === 'TRIAL' ? row.duration + '天' : typeDaysLabel(row.type) }}
                </template>
              </el-table-column>
              <el-table-column prop="price" label="价格" width="100">
                <template #default="{ row }">¥{{ row.price }}</template>
              </el-table-column>
              <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
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
                  <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id!)">
                    <template #reference>
                      <el-button type="danger" link size="small">删除</el-button>
                    </template>
                  </el-popconfirm>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </template>

    <!-- ═══ Member: Card Purchase Grid ═══ -->
    <div v-if="isMember">
      <el-card shadow="never" style="margin-top: 16px;">
        <template #header>
          <div class="card-header"><span>🛒 选择健身卡类型</span></div>
        </template>
        <el-tabs v-model="memberTab">
          <el-tab-pane label="🏃 次卡专区（按次数）" name="SESSION">
            <div v-loading="loading" class="card-grid">
              <div v-for="card in sessionCards" :key="card.id" class="gym-card-item session-card">
                <div class="gym-card-count-badge">{{ card.duration }}次</div>
                <div class="gym-card-name">{{ card.name }}</div>
                <div class="gym-card-price">¥{{ card.price }}</div>
                <div class="gym-card-features">
                  <div class="feature-row">✓ 可创建 {{ card.subCardLimit ?? 2 }} 张副卡</div>
                  <div class="feature-row">✓ 有效期一年</div>
                </div>
                <div class="gym-card-desc" v-if="card.description">{{ card.description }}</div>
                <el-button type="primary" class="gym-card-buy" @click="handlePurchase(card)">立即购买</el-button>
              </div>
              <el-empty v-if="sessionCards.length === 0 && !loading" description="暂无可购买的次卡" />
            </div>
          </el-tab-pane>

          <el-tab-pane label="📅 时间卡专区（按月/季/年/体验）" name="TIME">
            <div v-loading="loading" class="card-grid">
              <div v-for="card in timeCards" :key="card.id" class="gym-card-item">
                <div class="gym-card-type">
                  <el-tag :type="typeTag(card.type)" effect="dark" size="large">{{ typeLabel(card.type) }}</el-tag>
                </div>
                <div class="gym-card-name">{{ card.name }}</div>
                <div class="gym-card-price">¥{{ card.price }}</div>
                <div class="gym-card-duration">{{ typeDaysLabel(card.type) || card.duration + '天' }}</div>
                <div class="gym-card-desc" v-if="card.description">{{ card.description }}</div>
                <div v-if="card.type === 'TRIAL'" class="trial-hint">☕ 体验天数：1-7天灵活选择</div>
                <el-button type="primary" class="gym-card-buy" @click="handlePurchase(card)">立即购买</el-button>
              </div>
              <el-empty v-if="timeCards.length === 0 && !loading" description="暂无可购买的时间卡" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>

    <!-- ═══ Add / Edit Dialog ═══ -->
    <el-dialog v-model="formDialogVisible" :title="isEditing ? '编辑健身卡' : '新增健身卡'" width="520px">
      <el-form :model="form" label-position="top">
        <el-form-item label="卡名" required>
          <el-input v-model="form.name" placeholder="如：次卡、月卡、年卡" />
        </el-form-item>

        <!-- 新增时选卡类别，编辑时不可改 -->
        <el-form-item v-if="!isEditing" label="卡类别" required>
          <el-radio-group v-model="form.cardCategory">
            <el-radio value="SESSION">次卡（按次数）</el-radio>
            <el-radio value="TIME">时间卡（按月/季/年/体验）</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 次卡表单 -->
        <template v-if="form.cardCategory === 'SESSION'">
          <el-form-item label="次数" required>
            <el-input-number v-model="form.duration" :min="1" :max="999" style="width:100%" />
          </el-form-item>
          <el-form-item label="副卡上限">
            <el-input-number v-model="form.subCardLimit" :min="0" :max="10" style="width:100%" />
          </el-form-item>
        </template>

        <!-- 时间卡表单 -->
        <template v-if="form.cardCategory === 'TIME'">
          <el-form-item label="时间卡类型" required>
            <el-select v-model="form.type" style="width:100%">
              <el-option label="月卡（30天）" value="MONTHLY" />
              <el-option label="季卡（90天）" value="QUARTERLY" />
              <el-option label="年卡（365天）" value="YEARLY" />
              <el-option label="体验卡（1-7天）" value="TRIAL" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="form.type === 'TRIAL'" label="体验天数" required>
            <el-input-number v-model="form.duration" :min="1" :max="7" style="width:100%" />
          </el-form-item>
        </template>

        <el-form-item label="价格（元）" required>
          <el-input-number v-model="form.price" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>

        <!-- 编辑时可选状态 -->
        <el-form-item v-if="isEditing" label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="上架" value="ACTIVE" />
            <el-option label="下架" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleFormSubmit">
          {{ isEditing ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ═══ Sub-Card Management Dialog ═══ -->
    <el-dialog v-model="subCardDialogVisible" title="副卡管理" width="600px">
      <div v-if="myMembership?.subCards">
        <div class="sub-card-section">
          <div class="sub-card-section-title">当前副卡（{{ myMembership.subCards.length }}/{{ subCardLimit }}）</div>
          <el-table :data="myMembership.subCards" border stripe size="small">
            <el-table-column prop="holderName" label="副卡名称" min-width="120" />
            <el-table-column prop="userName" label="持有人" min-width="100" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                  {{ row.status === 'ACTIVE' ? '有效' : '无效' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-popconfirm title="确定撤销该副卡？撤销后该用户将无法使用此卡" @confirm="handleRevokeSubCard(row.id)">
                  <template #reference>
                    <el-button type="danger" link size="small">撤销</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <el-divider />

        <div class="sub-card-add-section" v-if="(myMembership.subCards.length || 0) < subCardLimit">
          <div class="sub-card-section-title">新增副卡</div>
          <el-form :model="subCardForm" label-position="top">
            <el-form-item label="选择会员" required>
              <el-select v-model="subCardForm.targetUserId" filterable remote
                :remote-method="searchMembers"
                :loading="memberSearchLoading"
                placeholder="搜索会员姓名/手机号" style="width:100%">
                <el-option v-for="m in memberOptions" :key="m.id" :label="m.label" :value="m.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="副卡名称">
              <el-input v-model="subCardForm.holderName" placeholder="如：老婆的副卡" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="subCardLoading" @click="handleAddSubCard">
                添加副卡
              </el-button>
            </el-form-item>
          </el-form>
        </div>
        <div v-else class="sub-card-full-hint">
          副卡数量已达上限（{{ subCardLimit }}张）
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listGymCards, createGymCard, updateGymCard, deleteGymCard,
  getMyGymMembership, purchaseGymCard, createSubCard, revokeSubCard,
  typeLabelMap, typeTagMap, categoryLabelMap,
  type GymCard, type GymMembership, type SubCardInfo,
} from '../api/gym';
import { useUserStore } from '../stores/user';
import request from '../utils/request';

const userStore = useUserStore();
const isAdmin = computed(() => userStore.user?.role === 'ADMIN');
const isMember = computed(() => userStore.user?.role === 'MEMBER');
const userId = computed(() => userStore.user?.id || 0);

const loading = ref(false);
const submitLoading = ref(false);
const cards = ref<GymCard[]>([]);
const myMembership = ref<GymMembership | null>(null);

const adminTab = ref('SESSION');
const memberTab = ref('SESSION');

/* ─── 根据 tab 过滤卡片 ─── */
const sessionCards = computed(() =>
  cards.value.filter(c => c.cardCategory === 'SESSION' && (!isAdmin.value || c.status))
);
const timeCards = computed(() =>
  cards.value.filter(c => c.cardCategory === 'TIME' && (!isAdmin.value || c.status))
);

/* ─── 我的卡状态 ─── */
const isValid = computed(() => {
  if (!myMembership.value) return false;
  const m = myMembership.value;
  if (m.cardCategory === 'SESSION') return (m.remainingVisits ?? 0) > 0;
  return m.remainingDays > 0;
});

const isExpiringSoon = computed(() => {
  if (!isValid.value || !myMembership.value) return false;
  const m = myMembership.value;
  if (m.cardCategory === 'SESSION') return (m.remainingVisits ?? 0) <= 3;
  return m.remainingDays <= 7;
});

const canManageSubCards = computed(() =>
  isMember.value && myMembership.value?.cardHolderType === 'PRIMARY'
  && myMembership.value?.cardCategory === 'SESSION'
);

const subCardLimit = computed(() => {
  if (!myMembership.value) return 2;
  const card = cards.value.find(c => c.id === myMembership.value!.cardId);
  return card?.subCardLimit ?? 2;
});

/* ─── 类型帮助函数 ─── */
function typeLabel(type: string): string {
  return typeLabelMap[type] || type;
}
function typeTag(type: string): string {
  return typeTagMap[type] || 'info';
}
function typeDaysLabel(type: string): string {
  const map: Record<string, string> = { MONTHLY: '30天', QUARTERLY: '90天', YEARLY: '365天', TRIAL: '体验' };
  return map[type] || '';
}

/* ═══════════════════════════════════════════════════════════
   Admin CRUD
   ═══════════════════════════════════════════════════════════ */

const formDialogVisible = ref(false);
const isEditing = ref(false);
const form = ref<GymCard>({
  name: '', cardCategory: 'SESSION', type: 'VISIT',
  price: 0, duration: 30, subCardLimit: 2, description: '', status: 'ACTIVE'
});

function resetForm() {
  form.value = {
    name: '', cardCategory: 'SESSION', type: 'VISIT',
    price: 0, duration: 30, subCardLimit: 2, description: '', status: 'ACTIVE'
  };
  isEditing.value = false;
}

function openAddDialog() {
  resetForm();
  formDialogVisible.value = true;
}

function openEditDialog(row: GymCard) {
  form.value = {
    id: row.id, name: row.name, cardCategory: row.cardCategory || 'SESSION',
    type: row.type, price: row.price, duration: row.duration,
    subCardLimit: row.subCardLimit ?? 2,
    description: row.description || '', status: row.status || 'ACTIVE'
  };
  isEditing.value = true;
  formDialogVisible.value = true;
}

// 监听卡类别变化，自动设置 type
watch(() => form.value.cardCategory, (cat) => {
  if (cat === 'SESSION') {
    form.value.type = 'VISIT';
    if (form.value.duration < 1) form.value.duration = 10;
  } else {
    form.value.type = 'MONTHLY';
    if (form.value.duration < 1) form.value.duration = 30;
  }
});

async function handleFormSubmit() {
  if (!form.value.name) { ElMessage.warning('请填写卡名'); return; }
  if (!form.value.price || form.value.price <= 0) { ElMessage.warning('请填写有效价格'); return; }
  if (form.value.cardCategory === 'SESSION') {
    if (!form.value.duration || form.value.duration <= 0) { ElMessage.warning('请填写有效次数'); return; }
  }
  submitLoading.value = true;
  try {
    if (isEditing.value) {
      await updateGymCard(form.value as GymCard);
      ElMessage.success('更新成功');
    } else {
      await createGymCard(form.value as GymCard);
      ElMessage.success('创建成功');
    }
    formDialogVisible.value = false;
    await fetchAll();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败');
  } finally { submitLoading.value = false; }
}

async function handleDelete(id: number) {
  try {
    await deleteGymCard(id);
    ElMessage.success('已删除');
    await fetchAll();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败');
  }
}

/* ═══════════════════════════════════════════════════════════
   Member: Purchase
   ═══════════════════════════════════════════════════════════ */

async function handlePurchase(card: GymCard) {
  let trialDays: number | undefined;

  // 体验卡：让用户选择天数
  if (card.type === 'TRIAL') {
    try {
      const { value } = await ElMessageBox.prompt('请选择体验天数（1-7天）', '体验卡设置', {
        inputType: 'number',
        inputValue: 3,
        inputValidator: (v: string) => {
          const n = Number(v);
          if (!Number.isInteger(n) || n < 1 || n > 7) return '请输入 1-7 之间的整数';
          return true;
        },
        inputErrorMessage: '天数必须在 1-7 之间',
      });
      trialDays = Number(value);
    } catch {
      return; // 用户取消
    }
  }

  const confirmMsg = `确定购买「${card.name}」？价格 ¥${card.price}，将从余额中扣除。`;
  try {
    await ElMessageBox.confirm(confirmMsg, '购买确认');
    await purchaseGymCard(userId.value, card.id!, trialDays);
    ElMessage.success('购买成功');
    await fetchAll();
  } catch (e: unknown) {
    if (e !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '购买失败');
    }
  }
}

/* ═══════════════════════════════════════════════════════════
   副卡管理
   ═══════════════════════════════════════════════════════════ */

const subCardDialogVisible = ref(false);
const subCardLoading = ref(false);
const memberSearchLoading = ref(false);
const memberOptions = ref<Array<{ id: number; label: string }>>([]);
const subCardForm = ref({ targetUserId: null as number | null, holderName: '' });

let searchTimer: ReturnType<typeof setTimeout> | null = null;
async function searchMembers(query: string) {
  if (!query || query.length < 1) { memberOptions.value = []; return; }
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(async () => {
    memberSearchLoading.value = true;
    try {
      const res = await request.get<{ records: any[] }>('/user/list', { keyword: query, role: 'MEMBER', pageNum: 1, pageSize: 20 });
      const records = res?.records || [];
      memberOptions.value = records.map((u: any) => ({
        id: u.id, label: `${u.realName || u.username}${u.phone ? ' (' + u.phone + ')' : ''}`
      }));
    } catch { memberOptions.value = []; }
    memberSearchLoading.value = false;
  }, 300);
}

function openSubCardDialog() {
  subCardForm.value = { targetUserId: null, holderName: '' };
  subCardDialogVisible.value = true;
}

async function handleAddSubCard() {
  if (!subCardForm.value.targetUserId) { ElMessage.warning('请选择会员'); return; }
  if (!myMembership.value) return;
  subCardLoading.value = true;
  try {
    await createSubCard(
      userId.value,
      myMembership.value.membershipId,
      subCardForm.value.targetUserId,
      subCardForm.value.holderName || undefined
    );
    ElMessage.success('副卡创建成功');
    // 刷新会员信息
    await fetchMembership();
    subCardForm.value = { targetUserId: null, holderName: '' };
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败');
  } finally { subCardLoading.value = false; }
}

async function handleRevokeSubCard(subId: number) {
  try {
    await revokeSubCard(userId.value, subId);
    ElMessage.success('副卡已撤销');
    await fetchMembership();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '撤销失败');
  }
}

/* ═══════════════════════════════════════════════════════════
   Data Fetching
   ═══════════════════════════════════════════════════════════ */

async function fetchCards() {
  loading.value = true;
  try {
    const allCards = await listGymCards(isAdmin.value ? undefined : 'ACTIVE');
    cards.value = allCards || [];
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
.gym-page { display: flex; flex-direction: column; flex: 1; min-height: 0; }
.gym-page :deep(.el-card) { flex: 1; display: flex; flex-direction: column; }
.gym-page :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; }
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }

/* ─── Member info bar ─── */
.my-card-bar { margin-bottom: 16px; }
.my-card-info { display: flex; align-items: center; gap: 16px; padding: 20px 24px; background: linear-gradient(135deg, #dbeafe, #eff6ff); border-radius: 12px; border: 1px solid #93c5fd; }
.my-card-info.is-sub { background: linear-gradient(135deg, #fef3c7, #fffbeb); border-color: #fcd34d; }
.my-card-icon { font-size: 36px; }
.my-card-detail { flex: 1; }
.my-card-name { font-size: 18px; font-weight: 700; color: #1e3a8a; display: flex; align-items: center; gap: 8px; }
.my-card-info.is-sub .my-card-name { color: #92400e; }
.my-card-meta { display: flex; gap: 12px; margin-top: 4px; font-size: 13px; color: #3b82f6; flex-wrap: wrap; }
.my-card-info.is-sub .my-card-meta { color: #d97706; }
.my-card-actions { display: flex; align-items: center; gap: 12px; }
.my-card-empty { padding: 20px 24px; background: #fef3c7; border-radius: 12px; border: 1px solid #fcd34d; text-align: center; color: #92400e; font-size: 14px; }

/* ─── Card grid ─── */
.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)); gap: 20px; }
.gym-card-item { border: 1px solid #e5e7eb; border-radius: 16px; padding: 24px; text-align: center; transition: all 0.2s; background: #fff; position: relative; }
.gym-card-item:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.08); border-color: #3b82f6; }
.gym-card-type { margin-bottom: 12px; }
.gym-card-name { font-size: 18px; font-weight: 700; color: #1e293b; }
.gym-card-price { font-size: 32px; font-weight: 800; color: #ef4444; margin: 8px 0 4px; }
.gym-card-duration { font-size: 13px; color: #94a3b8; margin-bottom: 8px; }
.gym-card-desc { font-size: 12px; color: #64748b; margin: 8px 0 12px; min-height: 20px; }
.gym-card-buy { width: 100%; }
.gym-card-features { font-size: 12px; color: #6b7280; margin: 8px 0; }
.feature-row { padding: 2px 0; }

/* 次卡样式 */
.session-card { border-color: #dbeafe; }
.session-card:hover { border-color: #3b82f6; }
.gym-card-count-badge { position: absolute; top: 12px; right: 12px; background: linear-gradient(135deg, #3b82f6, #2563eb); color: #fff; font-size: 20px; font-weight: 800; padding: 4px 14px; border-radius: 20px; }

.trial-hint { font-size: 12px; color: #f59e0b; margin-bottom: 8px; }

/* ─── Sub-card management ─── */
.sub-card-section, .sub-card-add-section { margin-bottom: 8px; }
.sub-card-section-title { font-weight: 600; font-size: 14px; color: #374151; margin-bottom: 12px; }
.sub-card-full-hint { text-align: center; color: #9ca3af; padding: 20px 0; }

/* ─── Dark theme ─── */
:global([data-admin-theme="dark"]) .my-card-info { background: linear-gradient(135deg, rgba(59,130,246,0.15), rgba(59,130,246,0.08)); border-color: rgba(59,130,246,0.3); }
:global([data-admin-theme="dark"]) .my-card-info.is-sub { background: linear-gradient(135deg, rgba(245,158,11,0.15), rgba(245,158,11,0.08)); border-color: rgba(245,158,11,0.3); }
:global([data-admin-theme="dark"]) .my-card-name { color: #93c5fd; }
:global([data-admin-theme="dark"]) .my-card-info.is-sub .my-card-name { color: #fbbf24; }
:global([data-admin-theme="dark"]) .my-card-meta { color: #60a5fa; }
:global([data-admin-theme="dark"]) .my-card-info.is-sub .my-card-meta { color: #f59e0b; }
:global([data-admin-theme="dark"]) .my-card-empty { background: rgba(245,158,11,0.1); border-color: rgba(245,158,11,0.3); color: #fbbf24; }
:global([data-admin-theme="dark"]) .gym-card-item { background: #1a1c28; border-color: rgba(255,255,255,0.07); }
:global([data-admin-theme="dark"]) .gym-card-item:hover { border-color: rgba(59,130,246,0.4); box-shadow: 0 8px 24px rgba(0,0,0,0.3); }
:global([data-admin-theme="dark"]) .session-card { border-color: rgba(59,130,246,0.2); }
:global([data-admin-theme="dark"]) .gym-card-name { color: #e8eaed; }
:global([data-admin-theme="dark"]) .gym-card-desc { color: #6b7084; }
:global([data-admin-theme="dark"]) .gym-card-duration { color: #4a4e63; }
:global([data-admin-theme="dark"]) .gym-card-features { color: #6b7280; }
:global([data-admin-theme="dark"]) .sub-card-section-title { color: #d1d5db; }
</style>
