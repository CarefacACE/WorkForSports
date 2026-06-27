import { createRouter, createWebHistory } from 'vue-router';
import LoginView from '../views/LoginView.vue';
import RegisterView from '../views/RegisterView.vue';
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
import MyCoachesView from '../views/member/MyCoachesView.vue';
import CoachDetailView from '../views/member/CoachDetailView.vue';
import MemberProfileView from '../views/member/MemberProfileView.vue';
import CoachPublicCourseView from '../views/coach/CoachPublicCourseView.vue';
import CoachPrivateCourseView from '../views/coach/CoachPrivateCourseView.vue';
import CoachMyCoursesView from '../views/coach/CoachMyCoursesView.vue';
import CoachMyStudentsView from '../views/coach/CoachMyStudentsView.vue';
import CoachScheduleView from '../views/coach/CoachScheduleView.vue';
import CoachProfileView from '../views/coach/CoachProfileView.vue';
import MemberScheduleView from '../views/member/MemberScheduleView.vue';
import MemberExerciseView from '../views/member/MemberExerciseView.vue';
import MemberCheckInView from '../views/member/MemberCheckInView.vue';
import MyPlanView from '../views/member/MyPlanView.vue';
import CoachCheckInView from '../views/coach/CoachCheckInView.vue';
import LogView from '../views/LogView.vue';
import SqlMonitorView from '../views/SqlMonitorView.vue';
import SystemMonitorView from '../views/SystemMonitorView.vue';
import DbControlView from '../views/DbControlView.vue';
import GroupChatView from '../views/chat/GroupChatView.vue';
import PrivateChatView from '../views/chat/PrivateChatView.vue';
import WechatChatView from '../views/chat/WechatChatView.vue';
import RequestManageView from '../views/chat/RequestManageView.vue';
import GymView from '../views/GymView.vue';
import GymSupermarketView from '../views/GymSupermarketView.vue';
import GymFinanceView from '../views/GymFinanceView.vue';
import AdminCourseView from '../views/AdminCourseView.vue';
import AdminScheduleView from '../views/AdminScheduleView.vue';
import AdminNotificationView from '../views/AdminNotificationView.vue';
import AdminChatView from '../views/AdminChatView.vue';
import ChatAgentView from '../views/ChatAgentView.vue';
import AdminRepairView from '../views/AdminRepairView.vue';
import AdminComplaintView from '../views/AdminComplaintView.vue';

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
      path: '/register',
      name: 'register',
      component: RegisterView,
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
        {
          path: 'gym',
          name: 'gym-manage',
          component: GymView,
        },
        {
          path: 'ai-assistant',
          name: 'ai-assistant',
          component: ChatAgentView,
        },
        {
          path: 'supermarket',
          name: 'admin-supermarket',
          component: GymSupermarketView,
        },
        {
          path: 'finance',
          name: 'admin-finance',
          component: GymFinanceView,
        },
        {
          path: 'course-approval',
          name: 'admin-course-approval',
          component: AdminCourseView,
        },
        {
          path: 'schedule-manage',
          name: 'admin-schedule-manage',
          component: AdminScheduleView,
        },
        {
          path: 'notification-manage',
          name: 'admin-notification-manage',
          component: AdminNotificationView,
        },
        {
          path: 'chat-manage',
          name: 'admin-chat-manage',
          component: AdminChatView,
        },
        {
          path: 'repair-manage',
          name: 'admin-repair-manage',
          component: AdminRepairView,
        },
        {
          path: 'complaint-manage',
          name: 'admin-complaint-manage',
          component: AdminComplaintView,
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
          path: 'coach-detail/:coachId',
          name: 'member-coach-detail',
          component: CoachDetailView,
        },
        {
          path: 'my-coaches',
          name: 'member-my-coaches',
          component: MyCoachesView,
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
          path: 'my-plan',
          name: 'member-my-plan',
          component: MyPlanView,
        },
        {
          path: 'checkin',
          name: 'member-checkin',
          component: MemberCheckInView,
        },
        {
          path: 'chat-group',
          name: 'member-chat-group',
          component: WechatChatView,
          props: { chatType: 'GROUP' as const },
        },
        {
          path: 'chat-private',
          name: 'member-chat-private',
          component: WechatChatView,
          props: { chatType: 'PRIVATE' as const },
        },
        {
          path: 'chat-requests',
          name: 'member-chat-requests',
          component: RequestManageView,
        },
        {
          path: 'gym',
          name: 'member-gym',
          component: GymView,
        },
        {
          path: 'supermarket',
          name: 'member-supermarket',
          component: GymSupermarketView,
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
          component: CoachProfileView,
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
          component: WechatChatView,
          props: { chatType: 'GROUP' as const },
        },
        {
          path: 'chat-private',
          name: 'coach-chat-private',
          component: WechatChatView,
          props: { chatType: 'PRIVATE' as const },
        },
        {
          path: 'chat-requests',
          name: 'coach-chat-requests',
          component: RequestManageView,
        },
        {
          path: 'supermarket',
          name: 'coach-supermarket',
          component: GymSupermarketView,
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
const darkRoutes = ['/', '/login', '/register'];
router.afterEach((to) => {
  if (darkRoutes.includes(to.path)) {
    document.documentElement.setAttribute('data-theme', 'dark');
  } else {
    document.documentElement.removeAttribute('data-theme');
  }
});

export default router;
