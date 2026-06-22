import request from '../utils/request';

export interface Course {
  id: number;
  coachId: number;
  name: string;
  description: string;
  type: 'PUBLIC' | 'PRIVATE';
  price: number;
  coverImage: string;
  status: string;
  createTime: string;
}

export interface CreateCourseParams {
  name: string;
  description?: string;
  type: 'PUBLIC' | 'PRIVATE';
  price?: number;
  coverImage?: string;
}

export interface UpdateCourseParams {
  id: number;
  name?: string;
  description?: string;
  coverImage?: string;
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
