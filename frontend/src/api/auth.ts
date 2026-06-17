import request from '../utils/request';

export type UserRole = 'ADMIN' | 'COACH' | 'MEMBER';

export interface LoginParams {
  username: string;
  password: string;
  role: UserRole;
}

export interface RegisterParams extends LoginParams {
  realName: string;
}

export interface ChangePasswordParams {
  username: string;
  role: UserRole;
  oldPassword: string;
  newPassword: string;
}

export interface ResetPasswordParams {
  username: string;
  role: UserRole;
  newPassword: string;
}

export interface UserProfile {
  id: number;
  username: string;
  realName: string;
  role: UserRole;
  phone?: string;
  email?: string;
  gender?: string;
  birthday?: string;
  avatar?: string;
  remark?: string;
}

export interface UpdateProfileParams {
  id: number;
  realName: string;
  phone?: string;
  email?: string;
  gender?: string;
  birthday?: string;
  avatar?: string;
  remark?: string;
}

export interface LoginResult extends UserProfile {
  token: string;
}

export function login(data: LoginParams) {
  return request.post<LoginResult>('/auth/login', data);
}

export function register(data: RegisterParams) {
  return request.post<LoginResult>('/auth/register', data);
}

export function changePassword(data: ChangePasswordParams) {
  return request.post<void>('/auth/change-password', data);
}

export function resetPassword(data: ResetPasswordParams) {
  return request.post<void>('/auth/reset-password', data);
}

export function getProfile(id: number) {
  return request.get<UserProfile>(`/auth/profile/${id}`);
}

export function updateProfile(data: UpdateProfileParams) {
  return request.post<UserProfile>('/auth/profile', data);
}
