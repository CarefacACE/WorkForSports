import request from '../utils/request';

export interface NotificationItem {
  id: number;
  userId: number;
  title: string;
  content: string;
  type: string;
  relatedId: number | null;
  isRead: number;
  createTime: string;
}

export interface NotificationPageResult {
  records: NotificationItem[];
  total: number;
  size: number;
  current: number;
}

export function getNotifications(userId: number, pageNum = 1, pageSize = 20) {
  return request.get<NotificationPageResult>('/notification/list', { userId, pageNum, pageSize });
}

export function markAsRead(id: number) {
  return request.put<void>(`/notification/${id}/read`);
}

export function getUnreadCount(userId: number) {
  return request.get<number>('/notification/unread', { userId });
}
