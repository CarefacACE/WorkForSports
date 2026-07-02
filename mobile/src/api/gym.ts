import request from '@/utils/request';

export interface GymCard {
  id?: number;
  name: string;
  cardCategory?: 'SESSION' | 'TIME';
  type: 'VISIT' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY' | 'TRIAL';
  price: number;
  duration: number;
  subCardLimit?: number;
  description?: string;
  status?: string;
  createTime?: string;
  updateTime?: string;
}

export interface SubCardInfo {
  id: number;
  userId: number;
  userName: string;
  holderName: string;
  status: string;
  createTime: string;
}

export interface GymMembership {
  membershipId: number;
  cardId: number;
  cardName: string;
  cardCategory: string;
  cardType: string;
  cardHolderType: 'PRIMARY' | 'SUB';
  holderName?: string;
  startDate: string;
  endDate: string;
  remainingVisits?: number;
  remainingDays: number;
  paidAmount: number;
  subCards?: SubCardInfo[];
  primaryOwnerName?: string;
  primaryMembershipId?: number;
}

export function listGymCards(status?: string) {
  return request.get<GymCard[]>('/gym/cards', { status: status || undefined });
}

export function createGymCard(data: GymCard) {
  return request.post<GymCard>('/gym/card', data);
}

export function updateGymCard(data: GymCard) {
  return request.put<GymCard>('/gym/card', data);
}

export function deleteGymCard(id: number) {
  return request.delete<void>(`/gym/card/${id}`);
}

export function getMyGymMembership(userId: number) {
  return request.get<GymMembership | null>('/gym/my-membership', { userId });
}

export function purchaseGymCard(userId: number, cardId: number, trialDays?: number) {
  let url = `/gym/purchase?userId=${userId}&cardId=${cardId}`;
  if (trialDays) url += `&trialDays=${trialDays}`;
  return request.post<GymMembership>(url);
}

/** 创建副卡（分享次卡给他人） */
export function createSubCard(userId: number, primaryMembershipId: number, targetUserId: number, holderName?: string) {
  let url = `/gym/sub-card?userId=${userId}&primaryMembershipId=${primaryMembershipId}&targetUserId=${targetUserId}`;
  if (holderName) url += `&holderName=${encodeURIComponent(holderName)}`;
  return request.post<GymMembership>(url);
}

/** 查看主卡的副卡列表 */
export function listSubCards(userId: number, primaryMembershipId: number) {
  return request.get<SubCardInfo[]>('/gym/sub-cards', { userId, primaryMembershipId });
}

/** 撤销副卡 */
export function revokeSubCard(userId: number, subMembershipId: number) {
  return request.delete<void>(`/gym/sub-card/${subMembershipId}?userId=${userId}`);
}

/* ═══════════════════════════════════════════════════════════════
   健身房超市 - 商品 API
   ═══════════════════════════════════════════════════════════════ */

export interface GymProduct {
  id?: number;
  name: string;
  description?: string;
  price: number;
  cost?: number;
  image?: string;
  stock: number;
  status?: string;
  createTime?: string;
  updateTime?: string;
}

export interface ProductPurchaseRecord {
  id: number;
  userId: number;
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  createTime: string;
}

export interface StockNotification {
  id: number;
  userId: number;
  productId: number;
  status: string;
  createTime: string;
  updateTime?: string;
}

/** 获取商品列表 */
export function listGymProducts(status?: string) {
  return request.get<GymProduct[]>('/gym/product/list', { status: status || undefined });
}

/** 管理员创建商品 */
export function createGymProduct(data: GymProduct) {
  return request.post<GymProduct>('/gym/product/create', data);
}

/** 管理员更新商品 */
export function updateGymProduct(data: GymProduct) {
  return request.put<GymProduct>('/gym/product/update', data);
}

/** 管理员删除商品 */
export function deleteGymProduct(id: number) {
  return request.delete<void>(`/gym/product/${id}`);
}

/** 会员/教练购买商品 */
export function purchaseGymProduct(userId: number, productId: number, quantity: number) {
  return request.post<ProductPurchaseRecord>(`/gym/product/purchase?userId=${userId}&productId=${productId}&quantity=${quantity}`);
}

/** 提交缺货通知 */
export function submitStockNotification(userId: number, productId: number) {
  return request.post<StockNotification>(`/gym/product/notify-stock?userId=${userId}&productId=${productId}`);
}

/** 检查是否已提交过缺货通知 */
export function checkStockNotification(userId: number, productId: number) {
  return request.get<{ submitted: boolean }>('/gym/product/check-notification', { userId, productId });
}

/** 获取我的购买记录 */
export function getMyPurchaseRecords(userId: number) {
  return request.get<ProductPurchaseRecord[]>('/gym/product/my-purchases', { userId });
}

/** 类型对应的中文标签 */
export const typeLabelMap: Record<string, string> = {
  VISIT: '次卡',
  MONTHLY: '月卡',
  QUARTERLY: '季卡',
  YEARLY: '年卡',
  TRIAL: '体验卡',
};

/** 类型对应的 Tag 样式 */
export const typeTagMap: Record<string, string> = {
  VISIT: '',
  MONTHLY: 'success',
  QUARTERLY: 'warning',
  YEARLY: 'danger',
  TRIAL: 'info',
};

/** 卡类别中文 */
export const categoryLabelMap: Record<string, string> = {
  SESSION: '次卡',
  TIME: '时间卡',
};

/* ═══════════════════════════════════════════════════════════════
   缺货通知 - 待处理 API（StockNotificationDropdown 使用）
   ═══════════════════════════════════════════════════════════════ */

export interface PendingNotification {
  id: number;
  userName: string;
  userRole: string;
  productName: string;
  createTime: string;
}

/** 获取待处理的缺货通知列表 */
export function listPendingNotifications() {
  return request.get<PendingNotification[]>('/gym/product/pending-notifications');
}

/** 标记缺货通知为已处理 */
export function markNotified(id: number) {
  return request.put<void>(`/gym/product/notify-stock/${id}`);
}

/* ═══════════════════════════════════════════════════════════════
   超市收支分析 API（仅管理员）
   ═══════════════════════════════════════════════════════════════ */

export interface FinanceOverview {
  totalRevenue: number;       // 总营收
  totalCost: number;           // 总成本
  grossProfit: number;         // 毛利润
  profitMargin: number;        // 利润率 (%)
  totalSoldQuantity: number;   // 总售出数量
  productCount: number;        // 商品种类数
}

export interface ProductFinance {
  productId: number;
  productName: string;
  soldQuantity: number;
  totalRevenue: number;
  unitCost: number;
  totalCost: number;
  profit: number;
  profitMargin: number;
}

export interface MonthlyFinance {
  month: string;      // "2024-01"
  revenue: number;
  cost: number;
  profit: number;
}

/** 获取收支总览 */
export function getFinanceOverview(startDate?: string, endDate?: string) {
  return request.get<FinanceOverview>('/gym/finance/overview', { startDate, endDate });
}

/** 获取商品维度收支分析 */
export function getProductFinanceBreakdown(startDate?: string, endDate?: string) {
  return request.get<ProductFinance[]>('/gym/finance/product-breakdown', { startDate, endDate });
}

/** 获取月度趋势 */
export function getFinanceMonthlyTrend() {
  return request.get<MonthlyFinance[]>('/gym/finance/monthly-trend');
}
