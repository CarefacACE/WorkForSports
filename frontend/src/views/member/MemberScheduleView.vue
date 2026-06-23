<template>
  <div class="schedule-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>我的课表</span>
          <div class="header-right">
            <el-button-group>
              <el-button :type="currentWeekOffset === 0 ? 'primary' : ''" size="small" @click="currentWeekOffset = 0">本周</el-button>
              <el-button size="small" @click="currentWeekOffset--">上一周</el-button>
              <el-button size="small" @click="currentWeekOffset++">下一周</el-button>
            </el-button-group>
            <span class="week-label">{{ weekRangeLabel }}</span>
          </div>
        </div>
      </template>

      <div class="timetable">
        <div class="timetable-header">
          <div class="timetable-cell timetable-corner">时间</div>
          <div v-for="(day, i) in weekDays" :key="i" class="timetable-cell timetable-day"
               :class="{ today: isToday(day.date) }">
            <div class="day-name">{{ day.name }}</div>
            <div class="day-date">{{ day.dateStr }}</div>
          </div>
        </div>

        <div v-for="slot in timeSlots" :key="slot.start" class="timetable-row">
          <div class="timetable-cell timetable-time">{{ slot.start }}<br><span class="time-end">{{ slot.end }}</span></div>
          <div v-for="(day, i) in weekDays" :key="i"
               class="timetable-cell timetable-slot"
               :class="{ 'has-event': getEvent(day.date, slot.start) }">
            <div v-if="getEvent(day.date, slot.start)" class="event-block"
                 :style="{ background: getEvent(day.date, slot.start)!.color || '#3056d3' }">
              <div class="event-title">{{ getEvent(day.date, slot.start)!.title }}</div>
              <div class="event-location" v-if="getEvent(day.date, slot.start)!.location">{{ getEvent(day.date, slot.start)!.location }}</div>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-if="events.length === 0 && !loading" description="暂无课程安排，请先选课" :image-size="80" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { getMemberSchedules, type ScheduleEvent } from '../../api/schedule';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();
const userId = userStore.user?.id;

const loading = ref(false);
const timeSlots = [
  { start: '08:00', end: '09:00' },
  { start: '09:00', end: '10:00' },
  { start: '10:00', end: '11:00' },
  { start: '11:00', end: '12:00' },
  { start: '12:00', end: '13:00' },
  { start: '13:00', end: '14:00' },
  { start: '14:00', end: '15:00' },
  { start: '15:00', end: '16:00' },
  { start: '16:00', end: '17:00' },
  { start: '17:00', end: '18:00' },
  { start: '18:00', end: '19:00' },
  { start: '19:00', end: '20:00' },
  { start: '20:00', end: '21:00' },
  { start: '21:00', end: '22:00' },
];

const currentWeekOffset = ref(0);

function getWeekStart(offset: number): Date {
  const now = new Date();
  const day = now.getDay() || 7;
  return new Date(now.getFullYear(), now.getMonth(), now.getDate() - day + 1 + offset * 7);
}

const weekDays = computed(() => {
  const start = getWeekStart(currentWeekOffset.value);
  const names = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
  return names.map((name, i) => {
    const date = new Date(start);
    date.setDate(start.getDate() + i);
    return { name, date, dateStr: `${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}` };
  });
});

const weekRangeLabel = computed(() => `${weekDays.value[0].dateStr} ~ ${weekDays.value[6].dateStr}`);

function isToday(date: Date): boolean {
  const t = new Date();
  return date.getFullYear() === t.getFullYear() && date.getMonth() === t.getMonth() && date.getDate() === t.getDate();
}

function formatDate(date: Date): string {
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
}

const colorPalette = ['#3056d3', '#e6a23c', '#67c23a', '#f56c6c', '#909399', '#9b59b6', '#1abc9c', '#e74c3c'];
const courseColors = new Map<number, string>();
function getCourseColor(id: number): string {
  if (!courseColors.has(id)) courseColors.set(id, colorPalette[courseColors.size % colorPalette.length]);
  return courseColors.get(id)!;
}

const events = ref<(ScheduleEvent & { _date: string; _slot: string })[]>([]);

function getEvent(date: Date, slot: string) {
  return events.value.find(e => e._date === formatDate(date) && e._slot === slot);
}

async function fetchSchedules() {
  if (!userId) return;
  loading.value = true;
  try {
    const start = getWeekStart(currentWeekOffset.value);
    const end = new Date(start);
    end.setDate(start.getDate() + 6);
    end.setHours(23, 59, 59);
    const data = await getMemberSchedules(userId, start.toISOString(), end.toISOString());
    events.value = data.map(s => ({
      ...s,
      color: s.color || getCourseColor(s.courseId),
      _date: s.startTime.slice(0, 10),
      _slot: s.startTime.slice(11, 16),
    }));
  } catch {}
  loading.value = false;
}

watch(currentWeekOffset, () => fetchSchedules());
onMounted(() => fetchSchedules());
</script>

<style scoped>
.schedule-page { padding: 0; }
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }
.header-right { display: flex; align-items: center; gap: 12px; }
.week-label { font-size: 14px; color: #606266; }

.timetable { border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden; }
.timetable-header { display: grid; grid-template-columns: 70px repeat(7, 1fr); background: #f5f7fa; }
.timetable-row { display: grid; grid-template-columns: 70px repeat(7, 1fr); border-top: 1px solid #ebeef5; }
.timetable-cell { padding: 8px; text-align: center; border-right: 1px solid #ebeef5; min-height: 56px; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.timetable-cell:last-child { border-right: none; }
.timetable-corner { font-size: 12px; color: #909399; }
.timetable-day { gap: 2px; }
.timetable-day .day-name { font-size: 13px; font-weight: 600; color: #303133; }
.timetable-day .day-date { font-size: 11px; color: #909399; }
.timetable-day.today { background: #ecf5ff; }
.timetable-time { font-size: 12px; color: #606266; font-weight: 500; background: #fafafa; line-height: 1.6; }
.timetable-time .time-end { font-size: 11px; color: #909399; font-weight: 400; }
.timetable-slot { min-height: 56px; }
.timetable-slot.has-event { padding: 4px; }

.event-block { width: 100%; padding: 6px 8px; border-radius: 6px; color: #fff; font-size: 12px; text-align: left; }
.event-block .event-title { font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.event-block .event-location { font-size: 10px; opacity: 0.85; margin-top: 2px; }
</style>
