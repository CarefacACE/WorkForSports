/**
 * AI Agent API — 移动端适配版
 *
 * 与 Web 端（frontend/src/api/agent.ts）的差异：
 *   1. chatWithAgent 使用 uni.request 接收 SSE 流式响应，而非浏览器 EventSource
 */
import request from '@/utils/request'
import { storage } from '@/utils/storage'

// -----------------------------------------------------------
// 类型（与 Web 端一致）
// -----------------------------------------------------------

export interface PlanGenerateRequest {
  goal?: string
  durationDays?: number
  startDate?: string
  description?: string
  includeCourseRecommendation?: boolean
}

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

// -----------------------------------------------------------
// SSE 流式聊天（移动端实现）
// -----------------------------------------------------------

const SSE_BASE = 'http://192.168.1.100:8080/api'

/**
 * 向 AI 助手发送消息，接收流式响应
 *
 * 移动端 uni.request 的 enableChunked 模式下，通过 onChunkReceived
 * 回调逐条接收 SSE 数据块。
 *
 * @returns 一个取消函数，调用后中止请求
 */
export function chatWithAgent(
  message: string,
  userId: number,
  role: string,
  callbacks: {
    onChunk: (text: string) => void    // 每次收到数据块
    onDone: () => void                  // 流结束
    onError: (err: string) => void     // 出错
  },
): () => void {
  const token = storage.get('access_token')

  const task = uni.request({
    url: `${SSE_BASE}/agent/chat?message=${encodeURIComponent(message)}&userId=${userId}&role=${encodeURIComponent(role)}`,
    method: 'GET',
    header: {
      Authorization: token ? `Bearer ${token}` : '',
    },
    enableChunked: true,
    timeout: 120000,
    success: () => {
      callbacks.onDone()
    },
    fail: (err) => {
      callbacks.onError(err.errMsg || '请求失败')
    },
  })

  // HBuilderX 3.6+ 支持 onChunkReceived
  if (typeof (task as unknown as { onChunkReceived?: (cb: (res: unknown) => void) => void }).onChunkReceived === 'function') {
    ;(task as unknown as { onChunkReceived: (cb: (res: { data: string }) => void) => void }).onChunkReceived((res) => {
      const lines = (res.data || '').toString().split('\n')
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const content = line.slice(5).trim()
          if (content && content !== '[DONE]') {
            callbacks.onChunk(content)
          }
        }
      }
    })
  }

  return () => {
    task.abort()
  }
}

// -----------------------------------------------------------
// 生成训练计划（与 Web 端一致，使用 request.post）
// -----------------------------------------------------------

export function generateTrainingPlan(userId: number, params?: PlanGenerateRequest) {
  return request.post<TrainingPlan>(`/agent/generate-plan?userId=${userId}`, params || {}, { timeout: 120000 })
}
