/**
 * UniApp 文件上传/下载封装
 *
 * 提供与 Web 端 api/file.ts 兼容的文件操作接口：
 *   uploadFile(filePath, userId, username)  → Promise<FileInfo>
 *   downloadFile(url)                       → Promise<string>  (返回临时路径)
 *
 * 底层使用 uni.uploadFile / uni.downloadFile。
 */

import type { ApiResponse } from './request'

const BASE_URL = 'http://192.168.1.100:8080/api'

function getToken(): string | null {
  try {
    return uni.getStorageSync('access_token') || null
  } catch {
    return null
  }
}

// -----------------------------------------------------------
// 类型（与 Web 端 api/file.ts 中的 FileInfo 保持一致）
// -----------------------------------------------------------

export interface FileInfo {
  id: number
  originalName: string
  fileSize: number
  fileType: string
  uploadUsername: string
  createTime: string
}

// -----------------------------------------------------------
// 上传
// -----------------------------------------------------------

/**
 * 上传文件
 * @param filePath  本地文件路径（通过 uni.chooseImage / uni.chooseFile 获取）
 * @param userId    上传者 ID
 * @param username  上传者用户名
 */
export function uploadFile(
  filePath: string,
  userId: number,
  username: string,
): Promise<FileInfo> {
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
        Authorization: `Bearer ${getToken()}`,
      },
      success: (res) => {
        try {
          const result = JSON.parse(res.data) as ApiResponse<FileInfo>
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
// 下载
// -----------------------------------------------------------

/**
 * 下载文件到临时路径
 * @param url 完整的下载地址，例如 /api/file/download/123
 * @returns 临时文件路径
 */
export function downloadFile(url: string): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.downloadFile({
      url: `${BASE_URL}${url}`,
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.tempFilePath)
        } else {
          reject(new Error('下载失败'))
        }
      },
      fail: (err) => {
        reject(new Error(err.errMsg || '下载失败'))
      },
    })
  })
}
