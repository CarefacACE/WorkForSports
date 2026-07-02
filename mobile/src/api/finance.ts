import request from '@/utils/request';

export interface RechargeParams {
  userId: number;
  amount: number;
  remark?: string;
}

export interface WithdrawResult {
  grossAmount: number;
  tierName: string;
  commissionRate: number;
  commissionAmount: number;
  netAmount: number;
  balanceAfter: number;
}

export interface WalletTransaction {
  id: number;
  userId: number;
  amount: number;
  type: string;
  remark?: string;
  createTime: string;
}

export interface TransactionPage {
  records: WalletTransaction[];
  total: number;
  size: number;
  current: number;
}

export function recharge(data: RechargeParams) {
  return request.post<{ id: number; balance: number }>('/finance/recharge', data);
}

export function withdraw(data: RechargeParams) {
  return request.post<WithdrawResult>('/finance/withdraw', data);
}

export function getBalance(userId: number) {
  return request.get<number>(`/finance/balance/${userId}`);
}

export function getCommissionTier(userId: number) {
  return request.get<WithdrawResult>(`/finance/commission-tier/${userId}`);
}

export interface TierDetail {
  name: string;
  rate: number;
  minEarnings: number;
  maxEarnings: number;
  isCurrent: boolean;
  reached: boolean;
  amountToNextTier: number;
  progressPercent: number;
}

export interface CommissionDetailResult {
  currentTierName: string;
  currentRate: number;
  totalEarnings: number;
  balance: number;
  allTiers: TierDetail[];
}

export function getCommissionDetail(userId: number) {
  return request.get<CommissionDetailResult>(`/finance/commission-detail/${userId}`);
}

export function getTransactions(userId: number, pageNum = 1, pageSize = 10) {
  return request.get<TransactionPage>('/finance/transactions', { userId, pageNum, pageSize });
}

export function updateBalance(userId: number, balance: number) {
  return request.put<{ id: number; balance: number }>('/finance/balance', { userId, balance });
}

export async function exportBill(userId: number, period: string, type?: string) {
  const params = new URLSearchParams({ userId: String(userId), period });
  if (type) params.append('type', type);
  const baseURL = (import.meta as any).env?.VITE_API_BASE_URL || '/api';
  const url = `${baseURL}/finance/export?${params.toString()}`;

  const token = localStorage.getItem('access_token');
  const response = await fetch(url, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });

  if (!response.ok) throw new Error('导出失败');

  const blob = await response.blob();
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = `账单_${new Date().toISOString().slice(0, 10)}.csv`;
  a.click();
  URL.revokeObjectURL(a.href);
}
