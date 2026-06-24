export function chatWithAgent(message: string, userId: number, role: string): EventSource {
  const url = `/api/agent/chat?message=${encodeURIComponent(message)}&userId=${userId}&role=${role}`
  return new EventSource(url)
}
