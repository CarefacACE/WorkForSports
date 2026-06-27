import request from '../utils/request';

/* ==================== 报修相关 ==================== */

export interface SubmitRepairParams {
  userId: number;
  username: string;
  equipmentName: string;
  equipmentLocation?: string;
  description: string;
}

export interface EquipmentRepair {
  id: number;
  userId: number;
  username: string;
  equipmentName: string;
  equipmentLocation: string;
  description: string;
  status: string;
  feedback: string;
  processedTime: string;
  createTime: string;
  updateTime: string;
}

export function submitRepair(data: SubmitRepairParams) {
  return request.post<EquipmentRepair>('/feedback/repair', data);
}

export function getMyRepairs(userId: number, pageNum = 1, pageSize = 10) {
  return request.get<{ records: EquipmentRepair[]; total: number }>('/feedback/repair/my', { userId, pageNum, pageSize });
}

export function listRepairs(pageNum = 1, pageSize = 10, status?: string) {
  return request.get<{ records: EquipmentRepair[]; total: number }>('/feedback/repair/list', { pageNum, pageSize, status });
}

export function countPendingRepairs() {
  return request.get<number>('/feedback/repair/pending-count');
}

export function processRepair(id: number, status: string, feedback: string) {
  return request.put<EquipmentRepair>(`/feedback/repair/${id}/process`, { status, feedback });
}

/* ==================== 投诉相关 ==================== */

export interface SubmitComplaintParams {
  userId: number;
  username: string;
  coachId: number;
  coachUsername: string;
  content: string;
}

export interface CoachComplaint {
  id: number;
  userId: number;
  username: string;
  coachId: number;
  coachUsername: string;
  content: string;
  status: string;
  feedback: string;
  processedTime: string;
  createTime: string;
  updateTime: string;
}

export function submitComplaint(data: SubmitComplaintParams) {
  return request.post<CoachComplaint>('/feedback/complaint', data);
}

export function getMyComplaints(userId: number, pageNum = 1, pageSize = 10) {
  return request.get<{ records: CoachComplaint[]; total: number }>('/feedback/complaint/my', { userId, pageNum, pageSize });
}

export function listComplaints(pageNum = 1, pageSize = 10, status?: string) {
  return request.get<{ records: CoachComplaint[]; total: number }>('/feedback/complaint/list', { pageNum, pageSize, status });
}

export function countPendingComplaints() {
  return request.get<number>('/feedback/complaint/pending-count');
}

export function processComplaint(id: number, status: string, feedback: string) {
  return request.put<CoachComplaint>(`/feedback/complaint/${id}/process`, { status, feedback });
}
