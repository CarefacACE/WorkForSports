import request from '../utils/request'

export interface CheckInRecord {
  id: number
  scheduleId: number
  userId: number
  role: 'COACH' | 'MEMBER'
  checkInTime: string | null
  status: 'PENDING' | 'SIGNED' | 'ABSENT'
  createTime?: string
  updateTime?: string
}

export interface CheckInStats {
  totalRecords: number
  signedCount: number
  absentCount: number
  pendingCount: number
  checkInRate: number
}

export function checkIn(scheduleId: number, userId: number, role: string) {
  return request.post<CheckInRecord>(`/checkin?scheduleId=${scheduleId}&userId=${userId}&role=${role}`)
}

export function getCheckInStatus(scheduleId: number, userId: number, role: string) {
  return request.get<CheckInRecord | null>('/checkin/status', {
    scheduleId: String(scheduleId),
    userId: String(userId),
    role
  })
}

export function getScheduleCheckIns(scheduleId: number) {
  return request.get<CheckInRecord[]>(`/checkin/schedule/${scheduleId}`)
}

export function getCheckInHistory(userId: number, role: string, from?: string, to?: string) {
  const params: Record<string, string> = {
    userId: String(userId),
    role
  }
  if (from) params.from = from
  if (to) params.to = to
  return request.get<CheckInRecord[]>('/checkin/history', params)
}

export function getCheckInStats(userId: number, role: string) {
  return request.get<CheckInStats>('/checkin/stats', {
    userId: String(userId),
    role
  })
}
