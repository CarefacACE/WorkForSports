import { createRouter, createWebHistory } from 'vue-router';
import LoginView from '../views/LoginView.vue';
import LandingView from '../views/LandingView.vue';
import AdminLayout from '../layouts/AdminLayout.vue';
import WorkbenchView from '../views/WorkbenchView.vue';
import FileView from '../views/FileView.vue';
import CsvAnalysisView from '../views/CsvAnalysisView.vue';
import CoachView from '../views/CoachView.vue';
import MemberView from '../views/MemberView.vue';
import MemberBalanceView from '../views/MemberBalanceView.vue';
import CoachSalaryView from '../views/CoachSalaryView.vue';
import WalletView from '../views/WalletView.vue';
import PublicCourseView from '../views/member/PublicCourseView.vue';
import PrivateCourseView from '../views/member/PrivateCourseView.vue';
import MyCoursesView from '../views/member/MyCoursesView.vue';
import MemberProfileView from '../views/member/MemberProfileView.vue';
import CoachPublicCourseView from '../views/coach/CoachPublicCourseView.vue';
import CoachPrivateCourseView from '../views/coach/CoachPrivateCourseView.vue';
import CoachMyCoursesView from '../views/coach/CoachMyCoursesView.vue';
import CoachMyStudentsView from '../views/coach/CoachMyStudentsView.vue';
import CoachScheduleView from '../views/coach/CoachScheduleView.vue';
import MemberScheduleView from '../views/member/MemberScheduleView.vue';
import MemberExerciseView from '../views/member/MemberExerciseView.vue';
import MemberCheckInView from '../views/member/MemberCheckInView.vue';
import CoachCheckInView from '../views/coach/CoachCheckInView.vue';
import LogView from '../views/LogView.vue';
import SqlMonitorView from '../views/SqlMonitorView.vue';
import SystemMonitorView from '../views/SystemMonitorView.vue';
import DbControlView from '../views/DbControlView.vue';
import GroupChatView from '../views/chat/GroupChatView.vue';
import PrivateChatView from '../views/chat/PrivateChatView.vue';
import RequestManageView from '../views/chat/RequestManageView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'landing',
      component: LandingView,
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
        {
          path: 'logs',
          name: 'logs',
          component: LogView,
        },
        {
          path: 'sql-monitor',
          name: 'sql-monitor',
          component: SqlMonitorView,
        },
        {
          path: 'system-monitor',
          name: 'system-monitor',
          component: SystemMonitorView,
        },
        {
          path: 'db-control',
          name: 'db-control',
          component: DbControlView,
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
        {
          path: 'profile',
          name: 'member-profile',
          component: MemberProfileView,
        },
        {
          path: 'my-schedule',
          name: 'member-my-schedule',
          component: MemberScheduleView,
        },
        {
          path: 'exercise',
          name: 'member-exercise',
          component: MemberExerciseView,
        },
        {
          path: 'checkin',
          name: 'member-checkin',
          component: MemberCheckInView,
        },
        {
          path: 'chat-group',
          name: 'member-chat-group',
          component: GroupChatView,
        },
        {
          path: 'chat-private',
          name: 'member-chat-private',
          component: PrivateChatView,
        },
        {
          path: 'chat-requests',
          name: 'member-chat-requests',
          component: RequestManageView,
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
        {
          path: 'my-schedule',
          name: 'coach-my-schedule',
          component: CoachScheduleView,
        },
        {
          path: 'checkin',
          name: 'coach-checkin',
          component: CoachCheckInView,
        },
        {
          path: 'chat-group',
          name: 'coach-chat-group',
          component: GroupChatView,
        },
        {
          path: 'chat-private',
          name: 'coach-chat-private',
          component: PrivateChatView,
        },
        {
          path: 'chat-requests',
          name: 'coach-chat-requests',
          component: RequestManageView,
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

/* Dark theme isolation: only activate on landing + login pages */
const darkRoutes = ['/', '/login'];
router.afterEach((to) => {
  if (darkRoutes.includes(to.path)) {
    document.documentElement.setAttribute('data-theme', 'dark');
  } else {
    document.documentElement.removeAttribute('data-theme');
  }
});

export default router;
