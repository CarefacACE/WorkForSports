import request from '../utils/request';

export interface ScheduleEvent {
  id?: number;
  courseId?: number;
  coachId?: number;
  title: string;
  startTime: string;
  endTime: string;
  location?: string;
  color?: string;
  memberId?: number;
  enrollmentId?: number;
  bookingStatus?: string;
  courseType?: string; // 课程类型 (PUBLIC/PRIVATE)，用于隐私保护显示
  createTime?: string;
  updateTime?: string;
}

export function getCoachSchedules(coachId: number, from?: string, to?: string) {
  const params: Record<string, string> = { coachId: String(coachId) };
  if (from) params.from = from;
  if (to) params.to = to;
  return request.get<ScheduleEvent[]>('/schedule/coach', params);
}

export function getMemberSchedules(userId: number, from?: string, to?: string) {
  const params: Record<string, string> = { userId: String(userId) };
  if (from) params.from = from;
  if (to) params.to = to;
  return request.get<ScheduleEvent[]>('/schedule/member', params);
}

export function createSchedule(coachId: number, data: ScheduleEvent) {
  return request.post<ScheduleEvent>(`/schedule?coachId=${coachId}`, data);
}

export function updateSchedule(coachId: number, data: ScheduleEvent) {
  return request.put<ScheduleEvent>(`/schedule?coachId=${coachId}`, data);
}

export function deleteSchedule(coachId: number, id: number) {
  return request.delete<void>(`/schedule/${id}?coachId=${coachId}`);
}

export function autoSchedule(coachId: number, courseId: number) {
  return request.post<ScheduleEvent[]>(`/schedule/auto?coachId=${coachId}&courseId=${courseId}`);
}

export function clearAutoSchedule(coachId: number, courseId: number) {
  return request.delete<void>(`/schedule/auto?coachId=${coachId}&courseId=${courseId}`);
}

/* ─── 管理员：日程管理 ─── */
export function getAllSchedules(from?: string, to?: string) {
  const params: Record<string, string> = {};
  if (from) params.from = from;
  if (to) params.to = to;
  return request.get<ScheduleEvent[]>('/schedule/admin/all', params);
}

export function adminUpdateSchedule(data: ScheduleEvent) {
  return request.put<ScheduleEvent>('/schedule/admin/update', data);
}

export function adminDeleteSchedule(id: number) {
  return request.delete<void>(`/schedule/admin/${id}`);
}
