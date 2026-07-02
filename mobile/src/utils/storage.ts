/**
 * UniApp 本地存储封装
 *
 * 提供与 localStorage 一致的 get/set/remove 接口，
 * 底层使用 uni.getStorageSync / uni.setStorageSync / uni.removeStorageSync。
 *
 * 用途：替代 Pinia Store 和工具函数中所有 localStorage 调用。
 */

export const storage = {
  get(key: string): string | null {
    try {
      const value = uni.getStorageSync(key)
      if (typeof value === 'string') return value
      return value !== undefined && value !== null ? String(value) : null
    } catch {
      return null
    }
  },

  set(key: string, value: string): void {
    try {
      uni.setStorageSync(key, value)
    } catch {
      console.error(`[storage] 写入失败: ${key}`)
    }
  },

  remove(key: string): void {
    try {
      uni.removeStorageSync(key)
    } catch {
      console.error(`[storage] 删除失败: ${key}`)
    }
  },

  getJSON<T>(key: string): T | null {
    try {
      const raw = this.get(key)
      if (!raw) return null
      return JSON.parse(raw) as T
    } catch {
      return null
    }
  },

  setJSON(key: string, value: unknown): void {
    try {
      this.set(key, JSON.stringify(value))
    } catch {
      console.error(`[storage] JSON 写入失败: ${key}`)
    }
  },
}
