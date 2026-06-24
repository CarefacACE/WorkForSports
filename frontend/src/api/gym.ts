import request from '../utils/request';

export interface GymCard {
  id?: number;
  name: string;
  type: 'VISIT' | 'MONTHLY' | 'YEARLY';
  price: number;
  duration: number;
  description?: string;
  status?: string;
  createTime?: string;
  updateTime?: string;
}

export interface GymMembership {
  membershipId: number;
  cardId: number;
  cardName: string;
  cardType: string;
  startDate: string;
  endDate: string;
  remainingVisits?: number;
  remainingDays: number;
  paidAmount: number;
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

export function purchaseGymCard(userId: number, cardId: number) {
  return request.post<GymMembership>(`/gym/purchase?userId=${userId}&cardId=${cardId}`);
}
