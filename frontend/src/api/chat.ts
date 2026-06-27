import request from '../utils/request';

export interface ChatConversation {
  id: number;
  type: 'GROUP' | 'PRIVATE';
  name: string | null;
  courseId: number | null;
  ownerId: number | null;
  createTime: string;
  updateTime: string | null;
}

export interface ChatMessage {
  id: number;
  conversationId: number;
  senderId: number;
  content: string;
  msgType: string;
  createTime: string;
}

export interface ConversationPageResult {
  records: ChatConversation[];
  total: number;
  size: number;
  current: number;
}

export interface MessagePageResult {
  records: ChatMessage[];
  total: number;
  size: number;
  current: number;
}

export function getConversations(userId: number, pageNum = 1, pageSize = 20) {
  return request.get<ConversationPageResult>('/chat/conversations', { userId, pageNum, pageSize });
}

export function getConversationsByType(userId: number, type: string) {
  return request.get<ChatConversation[]>('/chat/conversations/type', { userId, type });
}

export function getMessages(conversationId: number, pageNum = 1, pageSize = 50) {
  return request.get<MessagePageResult>(`/chat/conversations/${conversationId}/messages`, { pageNum, pageSize });
}

export function createConversation(data: { type: string; name?: string; courseId?: number; ownerId?: number; memberIds?: number[] }) {
  return request.post<ChatConversation>('/chat/conversations', data);
}

export function joinConversation(conversationId: number, userId: number) {
  return request.post<void>(`/chat/conversations/${conversationId}/join?userId=${userId}`);
}

export function getUnreadCount(userId: number) {
  return request.get<number>('/chat/unread', { userId });
}

export interface UnreadConversation {
  conversationId: number;
  conversationName: string;
  conversationType: string;
  unreadCount: number;
  lastMessage: string;
  lastSenderId: number;
  lastTime: string;
}

export function getUnreadConversations(userId: number) {
  return request.get<UnreadConversation[]>('/chat/unread-conversations', { userId });
}

export function markConversationRead(conversationId: number, userId: number) {
  return request.post<void>(`/chat/conversations/${conversationId}/read?userId=${userId}`);
}

export interface GroupMember {
  userId: number;
  username: string;
  realName: string;
  role: string;
  nickname?: string;
  isMuted?: number;
  mutedUntil?: string | null;
  isOwner?: boolean;
}

export function getGroupMembers(conversationId: number) {
  return request.get<GroupMember[]>(`/chat/conversations/${conversationId}/members`);
}

export function addGroupMember(conversationId: number, userId: number) {
  return request.post<void>(`/chat/conversations/${conversationId}/members?userId=${userId}`);
}

export function removeGroupMember(conversationId: number, userId: number) {
  return request.delete<void>(`/chat/conversations/${conversationId}/members/${userId}`);
}

export function updateConversationName(conversationId: number, name: string) {
  return request.put<void>(`/chat/conversations/${conversationId}`, { name });
}

export function leaveGroup(conversationId: number, userId: number) {
  return request.post<void>(`/chat/conversations/${conversationId}/leave?userId=${userId}`);
}

export interface GroupNotice {
  id: number;
  conversationId: number;
  publisherId: number;
  content: string;
  createTime: string;
}

export function getGroupNotice(conversationId: number) {
  return request.get<GroupNotice | null>(`/chat/conversations/${conversationId}/notice`);
}

export function publishGroupNotice(conversationId: number, publisherId: number, content: string) {
  return request.post<void>(`/chat/conversations/${conversationId}/notice`, { publisherId, content });
}

export function deleteGroupNotice(noticeId: number) {
  return request.delete<void>(`/chat/notice/${noticeId}`);
}

/* ─── 管理员：聊天管理 ─── */

export interface AdminConversation {
  id: number;
  type: string;
  name: string;
  ownerId: number;
  courseId: number;
  memberCount: number;
  lastMessage?: string;
  lastMessageTime?: string;
  createTime: string;
  updateTime: string;
}

export interface AdminUser {
  id: number;
  username: string;
  realName: string;
  role: string;
}

export function getAllConversationsForAdmin() {
  return request.get<AdminConversation[]>('/chat/admin/all-conversations');
}

export function adminJoinConversation(conversationId: number, userId: number) {
  return request.post<void>(`/chat/admin/join/${conversationId}?userId=${userId}`);
}

export function getAllUsersForAdmin() {
  return request.get<AdminUser[]>('/chat/admin/all-users');
}

export interface FriendRequest {
  id: number;
  fromUserId: number;
  toUserId: number;
  requestType: string;
  conversationId: number | null;
  status: string;
  message: string;
  createTime: string;
}

export interface FriendRequestPageResult {
  records: FriendRequest[];
  total: number;
  size: number;
  current: number;
}

export interface SearchResult {
  id: number;
  username: string;
  realName: string;
  role: string;
}

export function searchUser(username: string) {
  return request.get<SearchResult>('/friend/search', { username });
}

export function searchUserById(id: number) {
  return request.get<SearchResult>('/friend/search', { id });
}

export function startTempChat(fromUserId: number, toUserId: number) {
  return request.post<ChatConversation>('/friend/temp-chat', { fromUserId, toUserId });
}

export function sendFriendRequest(fromUserId: number, toUserId: number, type: string, message = '') {
  return request.post<FriendRequest>('/friend/request', { fromUserId, toUserId, type, message });
}

export function sendJoinGroupRequest(userId: number, conversationId: number, message = '') {
  return request.post<void>('/friend/join-group', { userId, conversationId, message });
}

export function approveRequest(requestId: number, operatorId: number) {
  return request.post<void>(`/friend/approve/${requestId}?operatorId=${operatorId}`);
}

export function rejectRequest(requestId: number, operatorId: number) {
  return request.post<void>(`/friend/reject/${requestId}?operatorId=${operatorId}`);
}

export function getMyRequests(userId: number, status?: string, pageNum = 1, pageSize = 20) {
  return request.get<FriendRequestPageResult>('/friend/requests', { userId, status, pageNum, pageSize });
}

export function getSentRequests(userId: number, pageNum = 1, pageSize = 20) {
  return request.get<FriendRequestPageResult>('/friend/sent-requests', { userId, pageNum, pageSize });
}

export function getAvailableGroups(userId: number) {
  return request.get<ChatConversation[]>('/chat/available-groups', { userId });
}

export function searchGroups(keyword: string, userId: number) {
  return request.get<ChatConversation[]>('/chat/search-groups', { keyword, userId });
}

export function searchGroupById(id: number, userId: number) {
  return request.get<ChatConversation>('/chat/search-group-by-id', { id, userId });
}

/* ─── 拉黑 / 删除好友 ─── */

export function blockUser(userId: number, blockedUserId: number) {
  return request.post<void>('/friend/block', { userId, blockedUserId });
}

export function unblockUser(userId: number, blockedUserId: number) {
  return request.delete<void>('/friend/block', { data: { userId, blockedUserId } });
}

export function getBlockedUsers(userId: number) {
  return request.get<{ userId: number; username: string; realName: string }[]>('/friend/blocks', { userId });
}

export function deleteFriend(userId: number, friendUserId: number) {
  return request.delete<void>(`/friend/${friendUserId}`, { data: { userId } });
}

/* ─── 群管理：禁言 / 群昵称 ─── */

export function setMemberNickname(conversationId: number, userId: number, operatorId: number, nickname: string) {
  return request.put<void>(`/chat/conversations/${conversationId}/members/${userId}/nickname`, { operatorId, nickname });
}

export function muteMember(conversationId: number, userId: number, operatorId: number, durationMinutes?: number) {
  return request.post<void>(`/chat/conversations/${conversationId}/members/${userId}/mute`, { operatorId, durationMinutes: durationMinutes ?? null });
}

export function unmuteMember(conversationId: number, userId: number, operatorId: number) {
  return request.delete<void>(`/chat/conversations/${conversationId}/members/${userId}/mute?operatorId=${operatorId}`);
}

export function muteAllMembers(conversationId: number, operatorId: number) {
  return request.post<void>(`/chat/conversations/${conversationId}/mute-all`, { operatorId });
}

export function unmuteAllMembers(conversationId: number, operatorId: number) {
  return request.delete<void>(`/chat/conversations/${conversationId}/mute-all?operatorId=${operatorId}`);
}
