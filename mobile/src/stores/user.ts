/**
 * 用户认证 Store — 移动端适配版
 *
 * 与 Web 端（frontend/src/stores/user.ts）的差异：
 *   - localStorage → storage（uni.storage 封装）
 *   - 新增 checkAuth() 和 isLoggedIn 计算属性
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { storage } from '@/utils/storage'
import type { LoginResult, UserProfile } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // ---- State ----
  const user = ref<LoginResult | null>(null)

  // ---- Getters ----
  const isLoggedIn = computed(() => !!user.value)
  const userId = computed(() => user.value?.id)
  const userRole = computed(() => user.value?.role)
  const userName = computed(() => user.value?.username)

  // ---- Actions ----

  /** 从本地存储恢复登录态 */
  function checkAuth() {
    const stored = storage.getJSON<LoginResult>('user_info')
    if (stored && stored.token) {
      user.value = stored
    }
  }

  /** 登录成功后设置用户信息 */
  function setUser(loginResult: LoginResult) {
    user.value = loginResult
    storage.set('access_token', loginResult.token)
    storage.setJSON('user_info', loginResult)
  }

  /** 更新个人资料 */
  function updateProfile(profile: UserProfile) {
    if (!user.value) return
    user.value = {
      ...user.value,
      ...profile,
    }
    storage.setJSON('user_info', user.value)
  }

  /** 退出登录 */
  function logout() {
    user.value = null
    storage.remove('access_token')
    storage.remove('user_info')
  }

  return {
    user,
    isLoggedIn,
    userId,
    userRole,
    userName,
    checkAuth,
    setUser,
    updateProfile,
    logout,
  }
})
