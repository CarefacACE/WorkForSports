import request from '../utils/request';

export interface RechargeParams {
  userId: number;
  amount: number;
  remark?: string;
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
  return request.post<{ id: number; balance: number }>('/finance/withdraw', data);
}

export function getBalance(userId: number) {
  return request.get<number>(`/finance/balance/${userId}`);
}

export function getTransactions(userId: number, pageNum = 1, pageSize = 10) {
  return request.get<TransactionPage>('/finance/transactions', { userId, pageNum, pageSize });
}

export function updateBalance(userId: number, balance: number) {
  return request.put<{ id: number; balance: number }>('/finance/balance', { userId, balance });
}
