<template>
  <div class="wb-page">
    <!-- ═══ 通用欢迎栏 ═══ -->
    <div class="wb-welcome">
      <div>
        <h1 class="wb-greeting">{{ greeting }}，{{ user?.realName || user?.username }} 👋</h1>
        <p class="wb-greeting-sub">{{ roleDesc }}</p>
      </div>
      <div class="wb-date-badge">
        <span class="wb-date-text">{{ todayStr }}</span>
      </div>
    </div>

    <!-- ═══ ADMIN 仪表盘 ═══ -->
    <template v-if="user?.role === 'ADMIN'">
      <div class="wb-stats">
        <div class="wb-stat-card" v-for="s in adminStats" :key="s.label">
          <div class="wb-stat-icon" :style="{ background: s.bg }">
            <component :is="s.icon" />
          </div>
          <div class="wb-stat-info">
            <div class="wb-stat-value">{{ s.value }}</div>
            <div class="wb-stat-label">{{ s.label }}</div>
          </div>
        </div>
      </div>

      <div class="wb-grid-2">
        <div class="wb-section">
          <h3 class="wb-section-title">快捷入口</h3>
          <div class="wb-quick-links">
            <a v-for="q in adminQuickLinks" :key="q.label" class="wb-quick-link" @click="$router.push(q.to)">
              <span class="wb-ql-icon">{{ q.icon }}</span>
              <span class="wb-ql-text">{{ q.label }}</span>
            </a>
          </div>
        </div>

        <div class="wb-section">
          <h3 class="wb-section-title">最近操作日志</h3>
          <div class="wb-log-list" v-if="recentLogs.length">
            <div class="wb-log-item" v-for="log in recentLogs" :key="log.id">
              <span class="wb-log-user">{{ log.username }}</span>
              <span class="wb-log-op">{{ log.operation }}</span>
              <span class="wb-log-time">{{ formatTime(log.createTime) }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无日志" :image-size="60" />
        </div>
      </div>
    </template>

    <!-- ═══ COACH 仪表盘 ═══ -->
    <template v-if="user?.role === 'COACH'">
      <div class="wb-stats">
        <div class="wb-stat-card" v-for="s in coachStats" :key="s.label">
          <div class="wb-stat-icon" :style="{ background: s.bg }">
            <component :is="s.icon" />
          </div>
          <div class="wb-stat-info">
            <div class="wb-stat-value">{{ s.value }}</div>
            <div class="wb-stat-label">{{ s.label }}</div>
          </div>
        </div>
      </div>

      <div class="wb-grid-2">
        <div class="wb-section">
          <h3 class="wb-section-title">我的课程</h3>
          <div class="wb-course-list" v-if="coachCourses.length">
            <div class="wb-course-item" v-for="c in coachCourses" :key="c.id" @click="$router.push('/coach/my-courses')">
              <div class="wb-course-name">{{ c.name }}</div>
              <div class="wb-course-meta">
                <el-tag size="small" :type="c.type === 'PUBLIC' ? 'primary' : 'warning'" effect="light">
                  {{ c.type === 'PUBLIC' ? '公共课' : '私教' }}
                </el-tag>
                <span class="wb-course-price">¥{{ c.price }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无课程" :image-size="60" />
        </div>

        <div class="wb-section">
          <h3 class="wb-section-title">近期课表</h3>
          <div class="wb-schedule-list" v-if="coachSchedule.length">
            <div class="wb-schedule-item" v-for="(ev, i) in coachSchedule" :key="i">
              <div class="wb-sch-time">{{ formatScheduleTime(ev.startTime) }}</div>
              <div class="wb-sch-info">
                <div class="wb-sch-title">{{ ev.title }}</div>
                <div class="wb-sch-loc" v-if="ev.location">{{ ev.location }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无排课" :image-size="60" />
        </div>
      </div>

      <div class="wb-section">
        <h3 class="wb-section-title">快捷入口</h3>
        <div class="wb-quick-links">
          <a v-for="q in coachQuickLinks" :key="q.label" class="wb-quick-link" @click="$router.push(q.to)">
            <span class="wb-ql-icon">{{ q.icon }}</span>
            <span class="wb-ql-text">{{ q.label }}</span>
          </a>
        </div>
      </div>
    </template>

    <!-- ═══ MEMBER 仪表盘 ═══ -->
    <template v-if="user?.role === 'MEMBER'">
      <div class="wb-stats">
        <div class="wb-stat-card" v-for="s in memberStats" :key="s.label">
          <div class="wb-stat-icon" :style="{ background: s.bg }">
            <component :is="s.icon" />
          </div>
          <div class="wb-stat-info">
            <div class="wb-stat-value">{{ s.value }}</div>
            <div class="wb-stat-label">{{ s.label }}</div>
          </div>
        </div>
      </div>

      <div class="wb-grid-2">
        <div class="wb-section">
          <h3 class="wb-section-title">我的课程</h3>
          <div class="wb-course-list" v-if="memberEnrollments.length">
            <div class="wb-course-item" v-for="e in memberEnrollments" :key="e.id" @click="$router.push('/member/my-courses')">
              <div class="wb-course-name">{{ courseNames[e.courseId] || `课程 #${e.courseId}` }}</div>
              <div class="wb-course-meta">
                <el-tag size="small" :type="e.status === 'PAID' ? 'success' : e.status === 'CONFIRMED' ? 'primary' : 'info'" effect="light">
                  {{ e.status === 'PAID' ? '已付费' : e.status === 'CONFIRMED' ? '已确认' : '试听' }}
                </el-tag>
                <span class="wb-course-price" v-if="e.paidAmount">¥{{ e.paidAmount }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无课程，去选课吧" :image-size="60" />
        </div>

        <div class="wb-section">
          <h3 class="wb-section-title">近期课表</h3>
          <div class="wb-schedule-list" v-if="memberSchedule.length">
            <div class="wb-schedule-item" v-for="(ev, i) in memberSchedule" :key="i">
              <div class="wb-sch-time">{{ formatScheduleTime(ev.startTime) }}</div>
              <div class="wb-sch-info">
                <div class="wb-sch-title">{{ ev.title }}</div>
                <div class="wb-sch-loc" v-if="ev.location">{{ ev.location }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无排课" :image-size="60" />
        </div>
      </div>

      <div class="wb-section">
        <h3 class="wb-section-title">快捷入口</h3>
        <div class="wb-quick-links">
          <a v-for="q in memberQuickLinks" :key="q.label" class="wb-quick-link" @click="$router.push(q.to)">
            <span class="wb-ql-icon">{{ q.icon }}</span>
            <span class="wb-ql-text">{{ q.label }}</span>
          </a>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, markRaw, type Component } from 'vue';
import { gsap } from 'gsap';
import { useUserStore } from '../stores/user';
import { getUsers } from '../api/user';
import { getLogs, type LogItem } from '../api/log';
import { getMyCourses, listCourses, getCourseDetail, type Course } from '../api/course';
import { getMyEnrollments, getCoachEnrollments, type Enrollment } from '../api/enrollment';
import { getExerciseStats, type ExerciseStats } from '../api/exercise';
import { getCoachSchedules, getMemberSchedules, type ScheduleEvent } from '../api/schedule';
import {
  User, Reading, DataLine, Timer, Odometer,
  TrendCharts, Calendar, Document, Setting, Monitor,
  ChatDotRound, Wallet, Trophy, DataAnalysis
} from '@element-plus/icons-vue';

const userStore = useUserStore();
const user = computed(() => userStore.user);

// ─── Shared ───
const greeting = computed(() => {
  const h = new Date().getHours();
  if (h < 6) return '夜深了';
  if (h < 12) return '早上好';
  if (h < 14) return '中午好';
  if (h < 18) return '下午好';
  return '晚上好';
});

const roleDesc = computed(() => {
  const map = {
    ADMIN: '系统管理员 · 管理平台全局数据与用户',
    COACH: '教练工作台 · 管理课程、学员与排课',
    MEMBER: '会员中心 · 查看课程、锻炼与课表',
  };
  return map[user.value?.role || 'MEMBER'];
});

const todayStr = computed(() => {
  const d = new Date();
  const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
  return `${d.getMonth() + 1}月${d.getDate()}日 周${weekdays[d.getDay()]}`;
});

function formatTime(t: string) {
  if (!t) return '';
  const d = new Date(t);
  const now = new Date();
  const diff = now.getTime() - d.getTime();
  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

function formatScheduleTime(t: string) {
  if (!t) return '';
  const d = new Date(t);
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

// ═══════════════════════════════════════
// ADMIN
// ═══════════════════════════════════════
const coachCount = ref(0);
const memberCount = ref(0);
const courseCount = ref(0);
const logCount = ref(0);
const recentLogs = ref<LogItem[]>([]);

const adminStats = computed(() => [
  { label: '教练总数', value: coachCount.value, icon: markRaw(User as Component), bg: 'rgba(37,99,235,0.1)' },
  { label: '会员总数', value: memberCount.value, icon: markRaw(Reading as Component), bg: 'rgba(34,197,94,0.1)' },
  { label: '课程总数', value: courseCount.value, icon: markRaw(DataLine as Component), bg: 'rgba(245,158,11,0.1)' },
  { label: '操作日志', value: logCount.value, icon: markRaw(Document as Component), bg: 'rgba(168,85,247,0.1)' },
]);

const adminQuickLinks = [
  { icon: '👨‍🏫', label: '教练管理', to: '/dashboard/coaches' },
  { icon: '👥', label: '会员管理', to: '/dashboard/members' },
  { icon: '📊', label: 'CSV 分析', to: '/dashboard/csv-analysis' },
  { icon: '🖥️', label: '系统监控', to: '/dashboard/system-monitor' },
  { icon: '📁', label: '文件管理', to: '/dashboard/files' },
  { icon: '📝', label: '日志管理', to: '/dashboard/logs' },
];

async function loadAdminData() {
  try {
    const [coaches, members, courses, logs] = await Promise.all([
      getUsers({ pageNum: 1, pageSize: 1, role: 'COACH' }).catch(() => ({ total: 0 })),
      getUsers({ pageNum: 1, pageSize: 1, role: 'MEMBER' }).catch(() => ({ total: 0 })),
      listCourses(undefined, undefined, 1, 1).catch(() => ({ total: 0 })),
      getLogs({ pageNum: 1, pageSize: 5 }).catch(() => ({ total: 0, records: [] })),
    ]);
    coachCount.value = coaches.total ?? 0;
    memberCount.value = members.total ?? 0;
    courseCount.value = courses.total ?? 0;
    logCount.value = logs.total ?? 0;
    recentLogs.value = logs.records ?? [];
  } catch { /* ignore */ }
}

// ═══════════════════════════════════════
// COACH
// ═══════════════════════════════════════
const coachCourseTotal = ref(0);
const coachEnrollTotal = ref(0);
const coachPendingCount = ref(0);
const coachTodayCount = ref(0);
const coachCourses = ref<Course[]>([]);
const coachSchedule = ref<ScheduleEvent[]>([]);

const coachStats = computed(() => [
  { label: '我的课程', value: coachCourseTotal.value, icon: markRaw(Reading as Component), bg: 'rgba(37,99,235,0.1)' },
  { label: '选课人数', value: coachEnrollTotal.value, icon: markRaw(User as Component), bg: 'rgba(34,197,94,0.1)' },
  { label: '待确认', value: coachPendingCount.value, icon: markRaw(Timer as Component), bg: 'rgba(245,158,11,0.1)' },
  { label: '近期排课', value: coachTodayCount.value, icon: markRaw(Calendar as Component), bg: 'rgba(168,85,247,0.1)' },
]);

const coachQuickLinks = [
  { icon: '📚', label: '我的课程', to: '/coach/my-courses' },
  { icon: '🎓', label: '我的学员', to: '/coach/my-students' },
  { icon: '📅', label: '我的课表', to: '/coach/my-schedule' },
  { icon: '💬', label: '群聊', to: '/coach/chat-group' },
  { icon: '✉️', label: '私信', to: '/coach/chat-private' },
  { icon: '💰', label: '我的钱包', to: '/dashboard/wallet' },
];

async function loadCoachData() {
  const id = user.value!.id;
  try {
    const [courses, enrollments, schedules] = await Promise.all([
      getMyCourses(id, 1, 5).catch(() => ({ total: 0, records: [] })),
      getCoachEnrollments(id, undefined, 1, 1).catch(() => ({ total: 0, records: [] })),
      getCoachSchedules(id).catch(() => []),
    ]);
    coachCourseTotal.value = courses.total ?? 0;
    coachCourses.value = courses.records ?? [];

    const enrollList = enrollments.records ?? [];
    coachEnrollTotal.value = enrollments.total ?? 0;
    coachPendingCount.value = enrollList.filter((e: Enrollment) => e.status === 'TRIAL').length;

    const allSchedules = schedules ?? [];
    coachTodayCount.value = allSchedules.length;
    coachSchedule.value = allSchedules.slice(0, 5);
  } catch { /* ignore */ }
}

// ═══════════════════════════════════════
// MEMBER
// ═══════════════════════════════════════
const exerciseData = ref<ExerciseStats | null>(null);
const memberEnrollments = ref<Enrollment[]>([]);
const memberEnrollTotal = ref(0);
const memberSchedule = ref<ScheduleEvent[]>([]);
const courseNames = ref<Record<number, string>>({});

const memberStats = computed(() => {
  const s = exerciseData.value;
  return [
    { label: '运动时长(分钟)', value: s?.totalDuration ?? 0, icon: markRaw(Timer as Component), bg: 'rgba(37,99,235,0.1)' },
    { label: '运动距离(km)', value: s?.totalDistance?.toFixed(1) ?? '0', icon: markRaw(Odometer as Component), bg: 'rgba(34,197,94,0.1)' },
    { label: '消耗卡路里', value: s?.totalCalories ?? 0, icon: markRaw(TrendCharts as Component), bg: 'rgba(245,158,11,0.1)' },
    { label: '运动天数', value: s?.totalDays ?? 0, icon: markRaw(Trophy as Component), bg: 'rgba(168,85,247,0.1)' },
  ];
});

const memberQuickLinks = [
  { icon: '📖', label: '浏览课程', to: '/member/public-courses' },
  { icon: '📚', label: '我的课程', to: '/member/my-courses' },
  { icon: '🏃', label: '我的锻炼', to: '/member/exercise' },
  { icon: '📅', label: '我的课表', to: '/member/my-schedule' },
  { icon: '💬', label: '群聊', to: '/member/chat-group' },
  { icon: '💰', label: '我的钱包', to: '/dashboard/wallet' },
];

async function loadMemberData() {
  const id = user.value!.id;
  try {
    const [stats, enrollments, schedules] = await Promise.all([
      getExerciseStats(id).catch(() => null),
      getMyEnrollments(id, undefined, 1, 5).catch(() => ({ total: 0, records: [] })),
      getMemberSchedules(id).catch(() => []),
    ]);
    exerciseData.value = stats;
    memberEnrollTotal.value = enrollments.total ?? 0;
    memberEnrollments.value = enrollments.records ?? [];
    memberSchedule.value = (schedules ?? []).slice(0, 5);

    // Resolve course names
    const ids = [...new Set(memberEnrollments.value.map((e: Enrollment) => e.courseId))];
    const resolved: Record<number, string> = {};
    await Promise.all(ids.map(async (cid) => {
      try {
        const c = await getCourseDetail(cid);
        resolved[cid] = c.name;
      } catch {
        resolved[cid] = `课程 #${cid}`;
      }
    }));
    courseNames.value = resolved;
  } catch { /* ignore */ }
}

// ═══════════════════════════════════════
// INIT
// ═══════════════════════════════════════
onMounted(() => {
  if (!user.value) return;
  if (user.value.role === 'ADMIN') loadAdminData();
  else if (user.value.role === 'COACH') loadCoachData();
  else loadMemberData();

  /* ─── GSAP Entrance Animations ─── */
  const tl = gsap.timeline({ defaults: { ease: 'power2.out' } });

  // Welcome bar
  tl.fromTo('.wb-welcome',
    { autoAlpha: 0, y: -12 },
    { autoAlpha: 1, y: 0, duration: 0.5 },
    0.05
  );

  // Stats cards — stagger from bottom
  tl.fromTo('.wb-stat-card',
    { autoAlpha: 0, y: 20, scale: 0.96 },
    { autoAlpha: 1, y: 0, scale: 1, stagger: 0.08, duration: 0.5 },
    0.15
  );

  // Section titles
  tl.fromTo('.wb-section-title',
    { autoAlpha: 0, x: -12 },
    { autoAlpha: 1, x: 0, stagger: 0.06, duration: 0.4 },
    0.3
  );

  // Section panels
  tl.fromTo('.wb-section',
    { autoAlpha: 0, y: 16 },
    { autoAlpha: 1, y: 0, stagger: 0.1, duration: 0.5 },
    0.35
  );

  // Quick links
  tl.fromTo('.wb-quick-link',
    { autoAlpha: 0, y: 10, scale: 0.9 },
    { autoAlpha: 1, y: 0, scale: 1, stagger: 0.05, duration: 0.4 },
    0.5
  );

  // Log items / course items
  tl.fromTo('.wb-log-item, .wb-course-item, .wb-schedule-item',
    { autoAlpha: 0, x: -10 },
    { autoAlpha: 1, x: 0, stagger: 0.04, duration: 0.35 },
    0.55
  );
});
</script>

<style src="../styles/views/workbench.css"></style>
