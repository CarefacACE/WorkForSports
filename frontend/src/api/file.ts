import request from '../utils/request';

export interface FileInfo {
  id: number;
  originalName: string;
  fileSize: number;
  fileType: string;
  uploadUsername: string;
  createTime: string;
}

export interface NumericColumn {
  /** 列标题，即 CSV 表头原名，如 "HeartRate", "Cadence" */
  title: string;
  /** 该列的数值列表 */
  data: number[];
  /** 平均值 */
  avg: number;
  /** 最大值 */
  max: number;
  /** 最小值 */
  min: number;
  /** 心率区间分布（仅心率列有值） */
  zones: Record<string, number> | null;
}

export interface CsvAnalysisResult {
  totalRecords: number;
  durationSeconds: number;
  durationFormatted: string;
  columns: NumericColumn[];
}

export function uploadFile(file: File, userId: number, username: string) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('userId', String(userId));
  formData.append('username', username);

  return request.post<FileInfo>('/file/upload', formData);
}

export function getFileList(userId?: number) {
  return request.get<FileInfo[]>('/file/list', { userId });
}

export function deleteFile(id: number) {
  return request.delete<void>(`/file/${id}`);
}

export function getDownloadUrl(id: number) {
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';
  return `${baseURL}/file/download/${id}`;
}

export function analyzeCsv(file: File, userId?: number, username?: string) {
  const formData = new FormData();
  formData.append('file', file);
  if (userId !== undefined) {
    formData.append('userId', String(userId));
  }
  if (username !== undefined) {
    formData.append('username', username);
  }

  return request.post<CsvAnalysisResult>('/csv/analyze', formData);
}
