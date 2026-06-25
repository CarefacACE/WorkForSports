<template>
  <div class="supermarket-page">
    <!-- ═══ Admin: CRUD Products ═══ -->
    <template v-if="isAdmin">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <span>🏪 超市商品管理</span>
            <el-button type="primary" @click="openAddDialog">＋ 新增商品</el-button>
          </div>
        </template>
        <el-table :data="products" v-loading="loading" border stripe>
          <el-table-column label="图片" width="80">
            <template #default="{ row }">
              <el-image
                v-if="row.image"
                :src="row.image"
                style="width: 48px; height: 48px; border-radius: 4px;"
                fit="cover"
              />
              <span v-else class="no-img">无图</span>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="商品名称" min-width="120" />
          <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
          <el-table-column label="售价" width="90">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column label="成本价" width="90">
            <template #default="{ row }">¥{{ row.cost ?? 0 }}</template>
          </el-table-column>
          <el-table-column label="库存" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.stock <= 0" type="danger" size="small">缺货</el-tag>
              <span v-else>{{ row.stock }}</span>
            </template>
          </el-table-column>
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
      </el-card>
    </template>

    <!-- ═══ Member/Coach: Supermarket Shopping ═══ -->
    <div v-if="!isAdmin">
      <!-- Product Grid -->
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <span>🏪 健身房超市</span>
            <el-button type="primary" link @click="showPurchaseHistory = !showPurchaseHistory">
              {{ showPurchaseHistory ? '返回商品列表' : '我的购买记录' }}
            </el-button>
          </div>
        </template>

        <!-- 购买记录 -->
        <template v-if="showPurchaseHistory">
          <el-table :data="purchaseRecords" v-loading="purchaseLoading" border stripe empty-text="暂无购买记录">
            <el-table-column prop="productName" label="商品" min-width="120" />
            <el-table-column label="单价" width="90">
              <template #default="{ row }">¥{{ row.unitPrice }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="70" />
            <el-table-column label="总价" width="100">
              <template #default="{ row }">¥{{ row.totalPrice }}</template>
            </el-table-column>
            <el-table-column prop="createTime" label="购买时间" min-width="160" />
          </el-table>
        </template>

        <!-- 商品列表 -->
        <div v-if="!showPurchaseHistory" v-loading="loading" class="product-grid">
          <div v-for="product in activeProducts" :key="product.id" class="product-card">
            <!-- 商品图片 -->
            <div class="product-image-area">
              <el-image
                v-if="product.image"
                :src="product.image"
                class="product-image"
                fit="cover"
              />
              <div v-else class="product-image-placeholder">
                <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="2" y="2" width="20" height="20" rx="3" /><circle cx="8.5" cy="8.5" r="1.5" /><path d="M21 15l-5-5L5 21" />
                </svg>
              </div>
            </div>

            <!-- 缺货标记 -->
            <div v-if="product.stock <= 0" class="out-of-stock-badge">缺货</div>

            <!-- 商品信息 -->
            <div class="product-info">
              <div class="product-name">{{ product.name }}</div>
              <div class="product-desc" v-if="product.description">{{ product.description }}</div>
              <div class="product-price">¥{{ product.price }}</div>
            </div>

            <!-- 操作区 -->
            <div class="product-actions">
              <template v-if="product.stock > 0">
                <div class="quantity-control">
                  <el-button size="small" circle :disabled="(purchaseQty[product.id!] || 1) <= 1"
                    @click="decreaseQty(product.id!)">−</el-button>
                  <span class="qty-value">{{ purchaseQty[product.id!] || 1 }}</span>
                  <el-button size="small" circle :disabled="(purchaseQty[product.id!] || 1) >= product.stock"
                    @click="increaseQty(product.id!)">+</el-button>
                </div>
                <el-button type="primary" size="small" :loading="buyingId === product.id"
                  @click="handlePurchase(product)">立即购买</el-button>
              </template>
              <template v-else>
                <el-button
                  size="small"
                  :type="notifiedMap[product.id!] ? 'info' : 'warning'"
                  :disabled="notifiedMap[product.id!]"
                  :loading="notifyingId === product.id"
                  @click="handleNotify(product)">
                  {{ notifiedMap[product.id!] ? '已提交通知' : '到货通知我' }}
                </el-button>
              </template>
            </div>
          </div>
          <el-empty v-if="activeProducts.length === 0 && !loading" description="暂无可购买的商品" />
        </div>
      </el-card>
    </div>

    <!-- ═══ Add / Edit Dialog (Admin) ═══ -->
    <el-dialog v-model="formDialogVisible" :title="isEditing ? '编辑商品' : '新增商品'" width="520px">
      <el-form :model="form" label-position="top">
        <el-form-item label="商品名称" required>
          <el-input v-model="form.name" placeholder="如：蛋白粉、运动毛巾" />
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
        <el-form-item label="售价（元）" required>
          <el-input-number v-model="form.price" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="成本价（进货价，元）" required>
          <el-input-number v-model="form.cost" :min="0" :precision="2" style="width:100%" placeholder="用于计算利润" />
        </el-form-item>
        <el-form-item label="库存数量" required>
          <el-input-number v-model="form.stock" :min="0" :step="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="商品图片URL">
          <el-input v-model="form.image" placeholder="可选，输入图片地址" />
        </el-form-item>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listGymProducts, createGymProduct, updateGymProduct, deleteGymProduct,
  purchaseGymProduct, submitStockNotification, checkStockNotification,
  getMyPurchaseRecords,
  type GymProduct, type ProductPurchaseRecord,
} from '../api/gym';
import { useUserStore } from '../stores/user';

const userStore = useUserStore();
const isAdmin = computed(() => userStore.user?.role === 'ADMIN');
const userId = computed(() => userStore.user?.id || 0);

const loading = ref(false);
const products = ref<GymProduct[]>([]);
const showPurchaseHistory = ref(false);
const purchaseRecords = ref<ProductPurchaseRecord[]>([]);
const purchaseLoading = ref(false);

/* ─── 购买数量管理 ─── */
const purchaseQty = reactive<Record<number, number>>({});
const buyingId = ref<number | null>(null);

function decreaseQty(productId: number) {
  const current = purchaseQty[productId] || 1;
  if (current > 1) purchaseQty[productId] = current - 1;
}
function increaseQty(productId: number) {
  const current = purchaseQty[productId] || 1;
  const product = products.value.find(p => p.id === productId);
  if (product && current < product.stock) purchaseQty[productId] = current + 1;
}

/* ─── 缺货通知管理 ─── */
const notifiedMap = reactive<Record<number, boolean>>({});
const notifyingId = ref<number | null>(null);

/* ─── 过滤可用商品 ─── */
const activeProducts = computed(() =>
  products.value.filter(p => p.status === 'ACTIVE')
);

/* ═══════════════════════════════════════════════════════════════
   Admin CRUD
   ═══════════════════════════════════════════════════════════════ */

const formDialogVisible = ref(false);
const isEditing = ref(false);
const submitLoading = ref(false);
const form = ref<GymProduct>({
  name: '', description: '', price: 0, cost: 0, image: '', stock: 0, status: 'ACTIVE',
});

function resetForm() {
  form.value = { name: '', description: '', price: 0, cost: 0, image: '', stock: 0, status: 'ACTIVE' };
  isEditing.value = false;
}

function openAddDialog() {
  resetForm();
  formDialogVisible.value = true;
}

function openEditDialog(row: GymProduct) {
  form.value = {
    id: row.id, name: row.name, description: row.description || '',
    price: row.price, cost: row.cost ?? 0, image: row.image || '', stock: row.stock,
    status: row.status || 'ACTIVE',
  };
  isEditing.value = true;
  formDialogVisible.value = true;
}

async function handleFormSubmit() {
  if (!form.value.name) { ElMessage.warning('请填写商品名称'); return; }
  if (!form.value.price || form.value.price <= 0) { ElMessage.warning('请填写有效价格'); return; }
  submitLoading.value = true;
  try {
    if (isEditing.value) {
      await updateGymProduct(form.value as GymProduct);
      ElMessage.success('更新成功');
    } else {
      await createGymProduct(form.value as GymProduct);
      ElMessage.success('创建成功');
    }
    formDialogVisible.value = false;
    await fetchProducts();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败');
  } finally {
    submitLoading.value = false;
  }
}

async function handleDelete(id: number) {
  try {
    await deleteGymProduct(id);
    ElMessage.success('已删除');
    await fetchProducts();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败');
  }
}

/* ═══════════════════════════════════════════════════════════════
   Member/Coach: Purchase
   ═══════════════════════════════════════════════════════════════ */

async function handlePurchase(product: GymProduct) {
  if (!userId.value) { ElMessage.warning('请先登录'); return; }
  const qty = purchaseQty[product.id!] || 1;
  const confirmMsg = `确定购买「${product.name}」x${qty}？总价 ¥${(product.price * qty).toFixed(2)}，将从余额中扣除。`;
  try {
    await ElMessageBox.confirm(confirmMsg, '购买确认');
    buyingId.value = product.id!;
    await purchaseGymProduct(userId.value, product.id!, qty);
    ElMessage.success('购买成功');
    purchaseQty[product.id!] = 1;
    await fetchProducts();
  } catch (e: unknown) {
    if (e !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '购买失败');
    }
  } finally {
    buyingId.value = null;
  }
}

/* ═══════════════════════════════════════════════════════════════
   缺货通知
   ═══════════════════════════════════════════════════════════════ */

async function handleNotify(product: GymProduct) {
  if (!userId.value) { ElMessage.warning('请先登录'); return; }
  try {
    notifyingId.value = product.id!;
    await submitStockNotification(userId.value, product.id!);
    ElMessage.success('已提交缺货通知，补货后将通知您');
    notifiedMap[product.id!] = true;
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '提交失败');
  } finally {
    notifyingId.value = null;
  }
}

/* ═══════════════════════════════════════════════════════════════
   购买记录
   ═══════════════════════════════════════════════════════════════ */

async function fetchPurchaseRecords() {
  if (!userId.value) return;
  purchaseLoading.value = true;
  try {
    purchaseRecords.value = await getMyPurchaseRecords(userId.value) || [];
  } catch {
    purchaseRecords.value = [];
  } finally {
    purchaseLoading.value = false;
  }
}

/* ═══════════════════════════════════════════════════════════════
   Data Fetching
   ═══════════════════════════════════════════════════════════════ */

async function fetchProducts() {
  loading.value = true;
  try {
    const allProducts = await listGymProducts(isAdmin.value ? undefined : 'ACTIVE');
    products.value = allProducts || [];
  } catch { /* ignore */ }
  loading.value = false;
}

async function fetchNotificationStatus() {
  if (isAdmin.value || !userId.value) return;
  for (const p of products.value) {
    if (p.stock <= 0 && p.id) {
      try {
        const res = await checkStockNotification(userId.value, p.id);
        if (res.submitted) notifiedMap[p.id] = true;
      } catch { /* ignore */ }
    }
  }
}

async function fetchAll() {
  await fetchProducts();
  if (!isAdmin.value && userId.value) {
    await fetchNotificationStatus();
  }
}

// 监听购买记录切换
watch(showPurchaseHistory, async (val) => {
  if (val) await fetchPurchaseRecords();
});

onMounted(fetchAll);
</script>

<style scoped>
.supermarket-page { padding: 0; display: flex; flex-direction: column; min-height: calc(100vh - 120px); }
.supermarket-page :deep(.el-card) { flex: 1; display: flex; flex-direction: column; }
.supermarket-page :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; }
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }

/* ─── Product Grid ─── */
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 20px; }
.product-card { border: 1px solid #e5e7eb; border-radius: 16px; overflow: hidden; transition: all 0.2s; background: #fff; position: relative; display: flex; flex-direction: column; }
.product-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.08); border-color: #3b82f6; }

/* 图片 */
.product-image-area { height: 160px; background: #f8fafc; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.product-image { width: 100%; height: 100%; }
.product-image-placeholder { color: #cbd5e1; }
.no-img { color: #94a3b8; font-size: 12px; }

/* 缺货标记 */
.out-of-stock-badge { position: absolute; top: 12px; right: 12px; background: #ef4444; color: #fff; font-size: 12px; font-weight: 700; padding: 3px 10px; border-radius: 12px; }

/* 商品信息 */
.product-info { padding: 12px 16px 8px; flex: 1; }
.product-name { font-size: 16px; font-weight: 700; color: #1e293b; }
.product-desc { font-size: 12px; color: #64748b; margin-top: 4px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.product-price { font-size: 22px; font-weight: 800; color: #ef4444; margin-top: 8px; }

/* 操作区 */
.product-actions { padding: 8px 16px 16px; display: flex; flex-direction: column; gap: 8px; }
.quantity-control { display: flex; align-items: center; justify-content: center; gap: 8px; }
.qty-value { font-size: 16px; font-weight: 600; min-width: 24px; text-align: center; color: #1e293b; }

/* ─── Dark theme ─── */
:global([data-admin-theme="dark"]) .product-card { background: #1a1c28; border-color: rgba(255,255,255,0.07); }
:global([data-admin-theme="dark"]) .product-card:hover { border-color: rgba(59,130,246,0.4); box-shadow: 0 8px 24px rgba(0,0,0,0.3); }
:global([data-admin-theme="dark"]) .product-image-area { background: rgba(255,255,255,0.03); }
:global([data-admin-theme="dark"]) .product-name { color: #e8eaed; }
:global([data-admin-theme="dark"]) .product-desc { color: #6b7084; }
:global([data-admin-theme="dark"]) .qty-value { color: #e8eaed; }
</style>
