import { defineStore } from 'pinia';
import type { LoginResult, UserProfile } from '../api/auth';

function getStoredUser() {
  const userInfo = localStorage.getItem('user_info');

  if (!userInfo) {
    return null;
  }

  try {
    return JSON.parse(userInfo) as LoginResult;
  } catch {
    localStorage.removeItem('user_info');
    return null;
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    user: getStoredUser(),
  }),
  actions: {
    setUser(user: LoginResult) {
      this.user = user;
      localStorage.setItem('access_token', user.token);
      localStorage.setItem('user_info', JSON.stringify(user));
    },
    updateProfile(profile: UserProfile) {
      if (!this.user) {
        return;
      }

      this.user = {
        ...this.user,
        ...profile,
      };
      localStorage.setItem('user_info', JSON.stringify(this.user));
    },
    logout() {
      this.user = null;
      localStorage.removeItem('access_token');
      localStorage.removeItem('user_info');
    },
  },
});
