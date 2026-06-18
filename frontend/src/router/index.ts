import { createRouter, createWebHistory } from 'vue-router';
import LoginView from '../views/LoginView.vue';
import AdminLayout from '../layouts/AdminLayout.vue';
import WorkbenchView from '../views/WorkbenchView.vue';
import CoachView from '../views/CoachView.vue';
import MemberView from '../views/MemberView.vue';
import MemberBalanceView from '../views/MemberBalanceView.vue';
import CoachSalaryView from '../views/CoachSalaryView.vue';
import WalletView from '../views/WalletView.vue';
import PublicCourseView from '../views/member/PublicCourseView.vue';
import PrivateCourseView from '../views/member/PrivateCourseView.vue';
import MyCoursesView from '../views/member/MyCoursesView.vue';
import CoachPublicCourseView from '../views/coach/CoachPublicCourseView.vue';
import CoachPrivateCourseView from '../views/coach/CoachPrivateCourseView.vue';
import CoachMyCoursesView from '../views/coach/CoachMyCoursesView.vue';
import CoachMyStudentsView from '../views/coach/CoachMyStudentsView.vue';

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
    {
      path: '/member',
      component: AdminLayout,
      children: [
        {
          path: 'public-courses',
          name: 'member-public-courses',
          component: PublicCourseView,
        },
        {
          path: 'private-courses',
          name: 'member-private-courses',
          component: PrivateCourseView,
        },
        {
          path: 'my-courses',
          name: 'member-my-courses',
          component: MyCoursesView,
        },
      ],
    },
    {
      path: '/coach',
      component: AdminLayout,
      children: [
        {
          path: 'public-courses',
          name: 'coach-public-courses',
          component: CoachPublicCourseView,
        },
        {
          path: 'private-courses',
          name: 'coach-private-courses',
          component: CoachPrivateCourseView,
        },
        {
          path: 'my-courses',
          name: 'coach-my-courses',
          component: CoachMyCoursesView,
        },
        {
          path: 'my-students',
          name: 'coach-my-students',
          component: CoachMyStudentsView,
        },
      ],
    },
  ],
});

router.beforeEach((to) => {
  const token = localStorage.getItem('access_token');

  if ((to.path.startsWith('/dashboard') || to.path.startsWith('/member') || to.path.startsWith('/coach')) && !token) {
    return '/login';
  }

  if (to.path === '/login' && token) {
    return '/dashboard/workbench';
  }

  return true;
});

export default router;
