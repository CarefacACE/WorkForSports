import request from '@/utils/request';

export interface Enrollment {
  id: number;
  userId: number;
  courseId: number;
  status: 'TRIAL' | 'PAID' | 'CONFIRMED' | 'CANCELLED';
  paidAmount: number;
  totalSessions?: number;
  remainingSessions?: number;
  coachId?: number;
  autoDeductAgreed?: number;
  createTime: string;
}

export interface EnrollmentPage {
  records: Enrollment[];
  total: number;
  size: number;
  current: number;
}

export interface StudentPage {
  records: { id: number; username: string; realName: string; phone: string }[];
  total: number;
  size: number;
  current: number;
}

export function enroll(userId: number, courseId: number) {
  return request.post<Enrollment>(`/enrollment?userId=${userId}`, { courseId });
}

export function payCourse(userId: number, courseId: number) {
  return request.post<Enrollment>(`/enrollment/pay?userId=${userId}&courseId=${courseId}`);
}

export function confirmEnrollment(coachId: number, enrollmentId: number) {
  return request.put<Enrollment>(`/enrollment/confirm/${enrollmentId}?coachId=${coachId}`);
}

export function getMyEnrollments(userId: number, type?: string, pageNum = 1, pageSize = 10) {
  return request.get<EnrollmentPage>('/enrollment/my', { userId, type, pageNum, pageSize });
}

export function getCourseStudents(coachId: number, courseId?: number, keyword?: string, pageNum = 1, pageSize = 10) {
  return request.get<StudentPage>('/enrollment/students', { coachId, courseId, keyword, pageNum, pageSize });
}

export function getCoachEnrollments(coachId: number, keyword?: string, pageNum = 1, pageSize = 10) {
  return request.get<EnrollmentPage>('/enrollment/coach', { coachId, keyword, pageNum, pageSize });
}

export function quitEnrollment(enrollmentId: number) {
  return request.put<void>(`/enrollment/quit/${enrollmentId}`);
}
