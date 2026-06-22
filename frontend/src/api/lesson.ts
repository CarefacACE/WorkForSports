import request from '../utils/request';

export interface Lesson {
  id: number;
  courseId: number;
  title: string;
  videoUrl: string;
  sortOrder: number;
  isTrial: number;
  description: string;
  duration: number;
  createTime: string;
}

export interface CreateLessonParams {
  courseId: number;
  title: string;
  videoUrl?: string;
  sortOrder?: number;
  isTrial?: number;
  description?: string;
  duration?: number;
}

export function addLesson(coachId: number, data: CreateLessonParams) {
  return request.post<Lesson>(`/lesson?coachId=${coachId}`, data);
}

export function updateLesson(coachId: number, lessonId: number, data: CreateLessonParams) {
  return request.put<Lesson>(`/lesson/${lessonId}?coachId=${coachId}`, data);
}

export function deleteLesson(coachId: number, lessonId: number) {
  return request.delete<void>(`/lesson/${lessonId}?coachId=${coachId}`);
}

export function getLessons(courseId: number) {
  return request.get<Lesson[]>('/lesson/list', { courseId });
}
