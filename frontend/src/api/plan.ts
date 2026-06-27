import request from '../utils/request'

export interface TrainingPlan {
  id: number
  userId: number
  goal: string
  durationDays: number
  startDate: string | null
  endDate: string | null
  description: string
  status: 'ACTIVE' | 'COMPLETED' | 'CANCELLED'
  createTime?: string
  updateTime?: string
}

export interface PlanDetail {
  id: number
  planId: number
  dayNumber: number
  trainingType: string
  content: string
  durationMinutes: number
  intensity: 'LOW' | 'MEDIUM' | 'HIGH'
  isChecked: number
  checkTime: string | null
  notes: string | null
  createTime?: string
  updateTime?: string
}

export interface PlanSubmitRequest {
  goal: string
  durationDays: number
  startDate: string
  endDate: string
  description: string
  details: PlanDetailItem[]
}

export interface PlanDetailItem {
  dayNumber: number
  trainingType: string
  content: string
  durationMinutes: number
  intensity: string
}

export function submitPlan(userId: number, data: PlanSubmitRequest) {
  return request.post<TrainingPlan>(`/plan/submit?userId=${userId}`, data)
}

export function getMyPlans(userId: number) {
  return request.get<TrainingPlan[]>('/plan/my-plans', { userId: String(userId) })
}

export function getPlanDetails(planId: number) {
  return request.get<PlanDetail[]>(`/plan/${planId}/details`)
}

export function checkInPlanDetail(planId: number, detailId: number) {
  return request.post<PlanDetail>(`/plan/${planId}/checkin/${detailId}`)
}

export function deletePlan(planId: number, userId: number) {
  return request.delete<void>(`/plan/${planId}?userId=${userId}`)
}

export function updatePlanGoal(planId: number, userId: number, goal: string) {
  return request.put<TrainingPlan>(`/plan/${planId}/goal?userId=${userId}`, { goal })
}
