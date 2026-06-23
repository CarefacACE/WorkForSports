import request from '../utils/request';

export interface ExerciseRecord {
  id?: number;
  userId?: number;
  type: string;
  duration: number;
  distance: number;
  calories: number;
  heartRateAvg?: number;
  heartRateMax?: number;
  pace?: string;
  exerciseDate: string;
  notes?: string;
  createTime?: string;
}

export interface ExerciseStats {
  totalDuration: number;
  totalDistance: number;
  totalCalories: number;
  totalDays: number;
  totalSessions: number;
  avgDuration: number;
  avgCalories: number;
  totalRunning: number;
  outdoorRunning: number;
  indoorRunning: number;
  avgPace: string;
}

export interface TrendPoint {
  date: string;
  duration: number;
  calories: number;
  distance: number;
}

export function getExerciseRecords(userId: number, from?: string, to?: string) {
  const params: Record<string, string> = { userId: String(userId) };
  if (from) params.from = from;
  if (to) params.to = to;
  return request.get<ExerciseRecord[]>('/exercise/list', params);
}

export function getExerciseStats(userId: number, from?: string, to?: string) {
  const params: Record<string, string> = { userId: String(userId) };
  if (from) params.from = from;
  if (to) params.to = to;
  return request.get<ExerciseStats>('/exercise/stats', params);
}

export function getExerciseTrend(userId: number, days = 30) {
  return request.get<TrendPoint[]>('/exercise/trend', { userId, days });
}

export function addExerciseRecord(userId: number, data: ExerciseRecord) {
  return request.post<ExerciseRecord>(`/exercise?userId=${userId}`, data);
}

export function deleteExerciseRecord(userId: number, id: number) {
  return request.delete<void>(`/exercise/${id}?userId=${userId}`);
}
