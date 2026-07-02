import request from '../utils/request'

export interface VideoChannel {
  id: number
  title: string
  description: string
  platform: string
  sourceUrl: string
  embedUrl: string
  thumbnailUrl: string
  playCount: number
  duration: string
  author: string
  tags: string
  category: string
  createTime: string
}

export interface VideoListResult {
  records: VideoChannel[]
  total: number
  size: number
  current: number
  pages: number
}

export function getVideoList(params: {
  keyword?: string
  category?: string
  platform?: string
  pageNum?: number
  pageSize?: number
}) {
  return request.get('/video/list', params) as unknown as Promise<VideoListResult>
}

export function getVideoDetail(id: number) {
  return request.get('/video/' + id) as unknown as Promise<VideoChannel>
}

export function triggerCrawl(keyword?: string) {
  return request.post('/video/crawl', { keyword }) as unknown as Promise<string>
}

export function triggerSeed() {
  return request.post('/video/seed') as unknown as Promise<string>
}

export function getCategories() {
  return request.get('/video/categories') as unknown as Promise<string[]>
}
