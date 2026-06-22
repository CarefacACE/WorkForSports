import { createRouter, createWebHistory } from 'vue-router';
import LoginView from '../views/LoginView.vue';
import AdminLayout from '../layouts/AdminLayout.vue';
import WorkbenchView from '../views/WorkbenchView.vue';
import FileView from '../views/FileView.vue';
import CsvAnalysisView from '../views/CsvAnalysisView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/login',
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/dashboard',
      component: AdminLayout,
      redirect: '/dashboard/workbench',
      children: [
        {
          path: 'workbench',
          name: 'workbench',
          component: WorkbenchView,
        },
        {
          path: 'files',
          name: 'files',
          component: FileView,
        },
        {
          path: 'csv-analysis',
          name: 'csv-analysis',
          component: CsvAnalysisView,
        },
      ],
    },
  ],
});

router.beforeEach((to) => {
  const token = localStorage.getItem('access_token');

  if (to.path.startsWith('/dashboard') && !token) {
    return '/login';
  }

  if (to.path === '/login' && token) {
    return '/dashboard/workbench';
  }

  return true;
});

export default router;
