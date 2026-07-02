import request from '@/utils/request';
import type { UserRole } from './auth';

export interface UserPageResult {
  records: UserItem[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface UserItem {
  id: number;
  username: string;
  realName: string;
  role: UserRole;
  phone: string;
  email: string;
  gender: string;
  birthday: string;
  avatar: string;
  remark: string;
  createTime: string;
  updateTime: string;
}

export interface UserQueryParams {
  pageNum: number;
  pageSize: number;
  keyword?: string;
  role: UserRole;
}

export interface RegisterUserParams {
  username: string;
  password: string;
  realName: string;
  role: UserRole;
  phone?: string;
  email?: string;
}

export function getUsers(params: UserQueryParams) {
  return request.get<UserPageResult>('/user/list', params);
}

export function registerUser(data: RegisterUserParams) {
  return request.post<UserItem>('/user/register', data);
}

export function updateUser(data: Partial<UserItem>) {
  return request.put<void>('/user', data);
}

export function deleteUser(id: number) {
  return request.delete<void>(`/user/${id}`);
}

export function deleteUsers(ids: number[]) {
  return request.delete<void>('/user/batch', ids);
}
