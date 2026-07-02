/**
 * UniApp WebSocket 客户端
 *
 * 用于接入后端聊天服务（ws://host:8080/api/ws/chat）。
 *
 * 使用方式：
 *   import { connectChat, closeChat } from '@/utils/websocket'
 *
 *   // 建立连接
 *   connectChat(userId, {
 *     onMessage: (msg) => { ... },
 *     onClose: () => { ... },
 *     onError: (err) => { ... },
 *   })
 *
 *   // 主动关闭
 *   closeChat()
 */

let socketTask: UniApp.SocketTask | null = null

export interface ChatSocketOptions {
  onMessage?: (data: unknown) => void
  onClose?: () => void
  onError?: (err: string) => void
}

const WS_BASE = 'ws://192.168.1.100:8080/api'

export function connectChat(userId: number, options: ChatSocketOptions = {}): void {
  // 如果已有连接则先关闭
  if (socketTask) {
    socketTask.close({ code: 1000, reason: 'reconnect' })
    socketTask = null
  }

  const url = `${WS_BASE}/ws/chat?userId=${userId}`

  socketTask = uni.connectSocket({
    url,
    success: () => {
      console.log('[WS] 连接已建立')
    },
    fail: (err) => {
      console.error('[WS] 连接失败:', err.errMsg)
      options.onError?.(err.errMsg || '连接失败')
    },
  })

  socketTask.onMessage((res) => {
    try {
      const data = JSON.parse(res.data as string)
      options.onMessage?.(data)
    } catch {
      // 非 JSON 消息
      options.onMessage?.(res.data)
    }
  })

  socketTask.onClose(() => {
    console.log('[WS] 连接关闭')
    socketTask = null
    options.onClose?.()
  })

  socketTask.onError((err) => {
    console.error('[WS] 错误:', err.errMsg)
    options.onError?.(err.errMsg || '未知错误')
  })
}

export function closeChat(): void {
  if (socketTask) {
    socketTask.close({ code: 1000, reason: 'user close' })
    socketTask = null
  }
}

/** 发送文本消息（底层保留，通常由后端 STOMP 接管，这里作为兜底） */
export function sendChatMessage(content: string): void {
  if (!socketTask) {
    console.warn('[WS] 未连接，无法发送')
    return
  }
  socketTask.send({
    data: content,
  })
}

/** 是否已连接 */
export function isChatConnected(): boolean {
  return socketTask !== null
}
