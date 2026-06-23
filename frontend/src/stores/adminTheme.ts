import { defineStore } from 'pinia';
import { ref, watch } from 'vue';

export type AdminTheme = 'light' | 'dark';

export const useAdminThemeStore = defineStore('adminTheme', () => {
  const theme = ref<AdminTheme>(
    (localStorage.getItem('admin-theme') as AdminTheme) || 'light'
  );

  function applyTheme(t: AdminTheme) {
    document.documentElement.setAttribute('data-admin-theme', t);
    localStorage.setItem('admin-theme', t);
  }

  function toggle() {
    theme.value = theme.value === 'light' ? 'dark' : 'light';
  }

  // Watch and apply
  watch(theme, (val) => applyTheme(val), { immediate: true });

  return { theme, toggle };
});
