import { createRouter, createWebHistory } from 'vue-router';
import LoginView from '../views/LoginView.vue';
import AdminLayout from '../layouts/AdminLayout.vue';
import WorkbenchView from '../views/WorkbenchView.vue';
import CoachView from '../views/CoachView.vue';
import MemberView from '../views/MemberView.vue';
import MemberBalanceView from '../views/MemberBalanceView.vue';
import CoachSalaryView from '../views/CoachSalaryView.vue';
import WalletView from '../views/WalletView.vue';

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
          path: 'coaches',
          name: 'coaches',
          component: CoachView,
        },
        {
          path: 'members',
          name: 'members',
          component: MemberView,
        },
        {
          path: 'member-balance',
          name: 'member-balance',
          component: MemberBalanceView,
        },
        {
          path: 'coach-salary',
          name: 'coach-salary',
          component: CoachSalaryView,
        },
        {
          path: 'wallet',
          name: 'wallet',
          component: WalletView,
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
