import request from '../utils/request'
import type { TrainingPlan } from './plan'

export function chatWithAgent(message: string, userId: number, role: string): EventSource {
  const url = `/api/agent/chat?message=${encodeURIComponent(message)}&userId=${userId}&role=${role}`
  return new EventSource(url)
}

export interface PlanGenerateRequest {
  goal?: string
  durationDays?: number
  startDate?: string
  description?: string
  includeCourseRecommendation?: boolean
}

export function generateTrainingPlan(userId: number, params?: PlanGenerateRequest) {
  return request.post<TrainingPlan>(`/agent/generate-plan?userId=${userId}`, params || {}, { timeout: 120000 })
}
