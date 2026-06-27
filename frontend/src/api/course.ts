import request from '../utils/request';

export interface Course {
  id: number;
  coachId: number;
  name: string;
  description: string;
  type: 'PUBLIC' | 'PRIVATE';
  price: number;
  coverImage: string;
  category: string;
  difficulty: string;
  maxStudents: number;
  location: string;
  startDate: string;
  tags: string;
  totalLessons: number;
  frequency: string;
  scheduleMode: string;
  defaultTimeSlot: string;
  status: string;
  createTime: string;
}

export interface CreateCourseParams {
  name: string;
  description?: string;
  type: 'PUBLIC' | 'PRIVATE';
  price?: number;
  coverImage?: string;
  category?: string;
  difficulty?: string;
  maxStudents?: number;
  location?: string;
  startDate?: string;
  tags?: string;
  totalLessons?: number;
  frequency?: string;
  scheduleMode?: string;
  defaultTimeSlot?: string;
}

export interface UpdateCourseParams {
  id: number;
  name?: string;
  description?: string;
  coverImage?: string;
  category?: string;
  difficulty?: string;
  maxStudents?: number;
  location?: string;
  startDate?: string;
  tags?: string;
  totalLessons?: number;
  frequency?: string;
  scheduleMode?: string;
  defaultTimeSlot?: string;
}

export interface CoursePage {
  records: Course[];
  total: number;
  size: number;
  current: number;
}

export function createCourse(coachId: number, data: CreateCourseParams) {
  return request.post<Course>(`/course?coachId=${coachId}`, data);
}

export function updateCourse(coachId: number, data: UpdateCourseParams) {
  return request.put<Course>(`/course?coachId=${coachId}`, data);
}

export function updateCoursePrice(coachId: number, courseId: number, newPrice: number) {
  return request.put<Course>(`/course/price?coachId=${coachId}&courseId=${courseId}&newPrice=${newPrice}`);
}

export function listCourses(type?: string, keyword?: string, pageNum = 1, pageSize = 10) {
  return request.get<CoursePage>('/course/list', { type, keyword, pageNum, pageSize });
}

export function getCourseDetail(id: number) {
  return request.get<Course>(`/course/${id}`);
}

export function getMyCourses(coachId: number, pageNum = 1, pageSize = 10) {
  return request.get<CoursePage>('/course/my', { coachId, pageNum, pageSize });
}

/* ─── 管理员：课程审批 ─── */
export function getPendingCourses() {
  return request.get<Course[]>('/course/admin/pending');
}

export function getPendingCourseCount() {
  return request.get<number>('/course/admin/pending/count');
}

export function getAllCoursesForAdmin(status?: string) {
  return request.get<Course[]>('/course/admin/all', { status });
}

export function approveCourse(id: number) {
  return request.put<Course>(`/course/admin/approve/${id}`);
}

export function rejectCourse(id: number, reason?: string) {
  return request.put<Course>(`/course/admin/reject/${id}?reason=${encodeURIComponent(reason || '')}`);
}

/** 管理员：替教练创建课程（直接通过审批） */
export function adminCreateCourse(coachId: number, data: CreateCourseParams) {
  return request.post<Course>(`/course/admin/create?coachId=${coachId}`, data);
}

/** 管理员：修改任意课程，通知教练 */
export function adminUpdateCourse(data: UpdateCourseParams) {
  return request.put<Course>('/course/admin/update', data);
}

/** 教练：重新申请被驳回的课程 */
export function resubmitCourse(id: number, coachId: number) {
  return request.put<Course>(`/course/resubmit/${id}?coachId=${coachId}`);
}



