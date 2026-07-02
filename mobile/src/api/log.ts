import request from '@/utils/request';

export interface LogItem {
  id: number;
  userId: number;
  username: string;
  role: string;
  operation: string;
  method: string;
  params: string;
  ip: string;
  createTime: string;
}

export interface LogPageResult {
  records: LogItem[];
  total: number;
  size: number;
  current: number;
}

export interface LogQueryParams {
  pageNum: number;
  pageSize: number;
  keyword?: string;
  role?: string;
  operationType?: string;
  startTime?: string;
  endTime?: string;
}

export function getLogs(params: LogQueryParams) {
  return request.get<LogPageResult>('/log/list', params);
}

export function deleteLog(id: number) {
  return request.delete<void>(`/log/${id}`);
}

export function deleteLogs(ids: number[]) {
  return request.delete<void>('/log/batch', ids);
}
