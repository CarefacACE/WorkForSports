import request from '@/utils/request';
import type { ScheduleEvent } from './schedule';

export interface PrivateCoachProfile {
  id?: number;
  coachId?: number;
  description?: string;
  specialties?: string;
  pricePerSession?: number;
  sessionDuration?: number;
  coverImage?: string;
  status?: string;
  createTime?: string;
  updateTime?: string;
}

export interface CoachListItem {
  coachId: number;
  username: string;
  realName: string;
  avatar?: string;
  phone?: string;
  description?: string;
  specialties?: string;
  pricePerSession: number;
  sessionDuration: number;
  coverImage?: string;
}

export interface CoachDetail extends CoachListItem {
  availableSchedules: any[];
}

export interface MyCoachItem {
  enrollmentId: number;
  coachId: number;
  realName: string;
  avatar?: string;
  specialties?: string;
  pricePerSession: number;
  totalSessions: number;
  remainingSessions: number;
  paidAmount: number;
  autoDeductAgreed?: number;
}

export interface Enrollment {
  id: number;
  userId: number;
  courseId: number;
  status: string;
  paidAmount: number;
  totalSessions: number;
  remainingSessions: number;
  createTime: string;
}

export function listCoaches(keyword?: string, pageNum = 1, pageSize = 12) {
  return request.get<{ records: CoachListItem[]; total: number }>('/private-coach/list', {
    keyword: keyword || undefined,
    pageNum,
    pageSize,
  });
}

export function getCoachDetail(coachId: number) {
  return request.get<CoachDetail>(`/private-coach/${coachId}`);
}

export function getMyProfile(coachId: number) {
  return request.get<PrivateCoachProfile | null>('/private-coach/profile', { coachId });
}

export function saveMyProfile(coachId: number, data: PrivateCoachProfile) {
  return request.put<PrivateCoachProfile>(`/private-coach/profile?coachId=${coachId}`, data);
}

export function purchaseSessions(userId: number, coachId: number, sessions: number) {
  return request.post<Enrollment>(`/private-coach/purchase?userId=${userId}&coachId=${coachId}&sessions=${sessions}`);
}

export function enrollCoach(userId: number, coachId: number, autoDeductAgreed: number) {
  return request.post<Enrollment>(`/private-coach/enroll?userId=${userId}&coachId=${coachId}&autoDeductAgreed=${autoDeductAgreed}`);
}

export function requestSession(userId: number, coachId: number, startTime: string, endTime: string) {
  return request.post<ScheduleEvent>(`/private-coach/request-session?userId=${userId}&coachId=${coachId}&startTime=${encodeURIComponent(startTime)}&endTime=${encodeURIComponent(endTime)}`);
}

export function approveSession(coachId: number, scheduleId: number) {
  return request.put<void>(`/private-coach/approve-session/${scheduleId}?coachId=${coachId}`);
}

export function rejectSession(coachId: number, scheduleId: number, reason?: string) {
  return request.put<void>(`/private-coach/reject-session/${scheduleId}?coachId=${coachId}`, reason ? { reason } : {});
}

export function quitCoach(userId: number, coachId: number) {
  return request.delete<void>(`/private-coach/quit/${coachId}?userId=${userId}`);
}

export function getMyCoaches(userId: number) {
  return request.get<MyCoachItem[]>('/private-coach/my-coaches', { userId });
}

export function bookSession(userId: number, scheduleId: number) {
  return request.post<void>(`/private-coach/book-session?userId=${userId}&scheduleId=${scheduleId}`);
}

export function bookDirect(userId: number, coachId: number, startTime: string, endTime: string) {
  return request.post<ScheduleEvent>(`/private-coach/book-direct?userId=${userId}&coachId=${coachId}&startTime=${encodeURIComponent(startTime)}&endTime=${encodeURIComponent(endTime)}`);
}

export function cancelBooking(userId: number, scheduleId: number) {
  return request.delete<void>(`/private-coach/cancel-booking/${scheduleId}?userId=${userId}`);
}

export function rescheduleBooking(userId: number, currentScheduleId: number, targetStart: string, targetEnd: string) {
  return request.put<void>(`/private-coach/reschedule?userId=${userId}&currentScheduleId=${currentScheduleId}&targetStart=${encodeURIComponent(targetStart)}&targetEnd=${encodeURIComponent(targetEnd)}`);
}
