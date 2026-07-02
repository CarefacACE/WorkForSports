/**
 * 文件管理 API — 移动端适配版
 *
 * 与 Web 端（frontend/src/api/file.ts）的差异：
 *   1. import 路径改为 @/utils/request
 *   2. uploadFile / analyzeCsv 接受 uni.chooseFile / uni.chooseImage 返回的临时路径
 *   3. uploadFile / analyzeCsv 内部使用 uni.uploadFile 而非 FormData + Axios
 */
import request from '@/utils/request'
import { storage } from '@/utils/storage'

const BASE_URL = 'http://192.168.1.100:8080/api'

export interface FileInfo {
  id: number
  originalName: string
  fileSize: number
  fileType: string
  uploadUsername: string
  createTime: string
}

export interface NumericColumn {
  title: string
  data: number[]
  avg: number
  max: number
  min: number
  zones: Record<string, number> | null
}

export interface CsvAnalysisResult {
  totalRecords: number
  durationSeconds: number
  durationFormatted: string
  columns: NumericColumn[]
}

// -----------------------------------------------------------
// 获取文件列表 / 删除（与 Web 端一致）
// -----------------------------------------------------------

export function getFileList(userId?: number) {
  return request.get<FileInfo[]>('/file/list', { userId })
}

export function deleteFile(id: number) {
  return request.delete<void>(`/file/${id}`)
}

export function getDownloadUrl(id: number) {
  return `${BASE_URL}/file/download/${id}`
}

// -----------------------------------------------------------
// 移动端文件上传（使用 uni.uploadFile）
// -----------------------------------------------------------

export function uploadFile(
  filePath: string,
  userId: number,
  username: string,
): Promise<FileInfo> {
  const token = storage.get('access_token')

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${BASE_URL}/file/upload`,
      filePath,
      name: 'file',
      formData: {
        userId: String(userId),
        username,
      },
      header: {
        Authorization: token ? `Bearer ${token}` : '',
      },
      success: (res) => {
        try {
          const result = JSON.parse(res.data)
          if (result.code === 200) {
            resolve(result.data)
          } else {
            reject(new Error(result.message || '上传失败'))
          }
        } catch {
          reject(new Error('解析上传响应失败'))
        }
      },
      fail: (err) => {
        reject(new Error(err.errMsg || '上传失败'))
      },
    })
  })
}

// -----------------------------------------------------------
// CSV 分析上传
// -----------------------------------------------------------

export function analyzeCsv(
  filePath: string,
  userId?: number,
  username?: string,
): Promise<CsvAnalysisResult> {
  const token = storage.get('access_token')
  const formData: Record<string, string> = {}
  if (userId !== undefined) formData.userId = String(userId)
  if (username !== undefined) formData.username = username

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${BASE_URL}/csv/analyze`,
      filePath,
      name: 'file',
      formData,
      header: {
        Authorization: token ? `Bearer ${token}` : '',
      },
      success: (res) => {
        try {
          const result = JSON.parse(res.data)
          if (result.code === 200) {
            resolve(result.data)
          } else {
            reject(new Error(result.message || '分析失败'))
          }
        } catch {
          reject(new Error('解析分析结果失败'))
        }
      },
      fail: (err) => {
        reject(new Error(err.errMsg || '上传失败'))
      },
    })
  })
}
