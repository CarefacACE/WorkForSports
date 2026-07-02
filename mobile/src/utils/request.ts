/**
 * UniApp HTTP 客户端
 *
 * 基于 uni.request 封装，接口签名与 Web 端 Axios 客户端完全一致：
 *   request.get<T>(url, params)  → Promise<T>
 *   request.post<T>(url, data)   → Promise<T>
 *   request.put<T>(url, data)    → Promise<T>
 *   request.delete<T>(url, data) → Promise<T>
 *
 * 后端统一响应格式：{ code: number; message: string; data: T }
 * code === 200 时自动解包返回 data；否则 reject 并携带 message。
 *
 * 与 Web 端（frontend/src/utils/request.ts）的差异仅在于底层使用 uni.request 而非 Axios，
 * 导出的 ApiResponse、request 签名完全一致，所有 API 模块可直接复用。
 */

// ============================================================
// 类型
// ============================================================

/** 后端统一响应包装 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

// ============================================================
// 配置
// ============================================================

/** API 基础路径 —— 开发环境指向本地后端，生产环境需替换为实际部署地址 */
const BASE_URL = 'http://192.168.1.100:8080/api'

// ============================================================
// 内部工具
// ============================================================

function getToken(): string | null {
  try {
    return uni.getStorageSync('access_token') || null
  } catch {
    return null
  }
}

function getUserInfo(): Record<string, unknown> {
  try {
    return JSON.parse(uni.getStorageSync('user_info') || '{}')
  } catch {
    return {}
  }
}

/** 构建 query 字符串 */
function toQueryString(params: Record<string, unknown>): string {
  const parts: string[] = []
  for (const key of Object.keys(params)) {
    const val = params[key]
    if (val === undefined || val === null) continue
    parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(val))}`)
  }
  return parts.length ? '?' + parts.join('&') : ''
}

// ============================================================
// 核心请求
// ============================================================

function doRequest<T>(
  method: 'GET' | 'POST' | 'PUT' | 'DELETE',
  url: string,
  data?: unknown,
  config?: { timeout?: number },
): Promise<T> {
  return new Promise((resolve, reject) => {
    // ---------- 构建请求头 ----------
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    }

    const token = getToken()
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    const userInfo = getUserInfo()
    if (userInfo.id) headers['X-User-Id'] = String(userInfo.id)
    if (userInfo.username) headers['X-Username'] = userInfo.username as string
    if (userInfo.role) headers['X-Role'] = userInfo.role as string

    // ---------- 发起请求 ----------
    uni.request({
      url: BASE_URL + url,
      method,
      header: headers,
      data: data as Parameters<typeof uni.request>[0]['data'],
      timeout: config?.timeout ?? 15000,
      success: (res) => {
        const statusCode = res.statusCode
        const result = res.data as ApiResponse

        // 401 — 清除 token，后续路由守卫会跳转登录页
        if (statusCode === 401) {
          uni.removeStorageSync('access_token')
          uni.removeStorageSync('user_info')
          return reject(new Error('未登录或登录已过期'))
        }

        // 业务错误
        if (result.code !== 200) {
          return reject(new Error(result.message || '请求失败'))
        }

        // 成功 —— 解包 data
        resolve(result.data as T)
      },
      fail: (err) => {
        const msg = err.errMsg || '网络请求失败'
        uni.showToast({ title: msg, icon: 'none' })
        reject(new Error(msg))
      },
    })
  })
}

// ============================================================
// 公开 API —— 与 Web 端 request.ts 签名完全一致
// ============================================================

const request = {
  get<T = unknown>(url: string, params?: unknown): Promise<T> {
    const qs = params ? toQueryString(params as Record<string, unknown>) : ''
    return doRequest<T>('GET', url + qs)
  },

  post<T = unknown>(url: string, data?: unknown, config?: { timeout?: number }): Promise<T> {
    return doRequest<T>('POST', url, data, config)
  },

  put<T = unknown>(url: string, data?: unknown): Promise<T> {
    return doRequest<T>('PUT', url, data)
  },

  delete<T = unknown>(url: string, data?: unknown): Promise<T> {
    return doRequest<T>('DELETE', url, data)
  },
}

export default request
