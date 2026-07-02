import request from '@/utils/request';

export interface HealthProfile {
  id?: number;
  userId?: number;

  // 健康信息
  height?: number;
  weight?: number;
  bodyFat?: number;
  muscleMass?: number;
  bpSystolic?: number;
  bpDiastolic?: number;
  restingHeartRate?: number;
  bloodType?: string;
  allergies?: string;
  medicalHistory?: string;
  currentMedications?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;

  // 达成目标
  targetWeight?: number;
  targetBodyFat?: number;
  targetMuscleMass?: number;
  fitnessGoal?: string;
  weeklyWorkoutFreq?: number;
  targetDate?: string;
  goalNotes?: string;

  createTime?: string;
  updateTime?: string;
}

export function getHealthProfile(userId: number) {
  return request.get<HealthProfile>('/health/profile', { userId });
}

export function saveHealthProfile(userId: number, data: HealthProfile) {
  return request.put<HealthProfile>(`/health/profile?userId=${userId}`, data);
}
