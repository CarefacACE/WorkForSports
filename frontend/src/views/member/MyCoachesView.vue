<template>
  <div class="my-coaches-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>🏋️ 我的私教</span>
          <div class="header-actions">
            <el-select v-model="selectedCoachId" placeholder="选择私教查看课表" style="width: 240px" @change="onCoachChange">
              <el-option v-for="mc in myCoaches" :key="mc.coachId" :label="mc.realName" :value="mc.coachId">
                <div class="coach-option">
                  <span>{{ mc.realName }}</span>
                  <el-tag size="small" :type="mc.remainingSessions > 0 ? 'success' : 'danger'" style="margin-left: auto">
                    {{ mc.remainingSessions }}/{{ mc.totalSessions }} 节
                  </el-tag>
                </div>
              </el-option>
            </el-select>
            <div class="week-nav">
              <el-button-group>
                <el-button :type="currentWeekOffset === 0 ? 'primary' : ''" size="small" @click="currentWeekOffset = 0">本周</el-button>
                <el-button size="small" @click="currentWeekOffset--">上一周</el-button>
                <el-button size="small" @click="currentWeekOffset++">下一周</el-button>
              </el-button-group>
              <span class="week-label">{{ weekRangeLabel }}</span>
            </div>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <div v-if="myCoaches.length === 0 && !loading" style="text-align: center; padding: 40px 0;">
          <el-empty description="还没有购买私教课">
            <el-button type="primary" @click="$router.push('/member/private-courses')">去浏览教练</el-button>
          </el-empty>
        </div>

        <div v-if="selectedCoach" class="coach-block">
          <div class="coach-header">
            <div class="coach-avatar" v-if="selectedCoach.avatar"><img :src="selectedCoach.avatar" /></div>
            <div class="coach-avatar-ph" v-else>{{ (selectedCoach.realName || '?')[0] }}</div>
            <div class="coach-info">
              <div class="coach-name">{{ selectedCoach.realName }}</div>
              <div class="coach-tags" v-if="selectedCoach.specialties">
                <el-tag v-for="s in selectedCoach.specialties.split(',')" :key="s" size="small" effect="plain">{{ s }}</el-tag>
              </div>
            </div>
            <div class="coach-sessions">
              <span class="sessions-num">{{ selectedCoach.remainingSessions }}</span>
              <span class="sessions-total">/{{ selectedCoach.totalSessions }} 节</span>
            </div>
          </div>

          <div class="legend-bar">
            <span class="legend-item"><span class="legend-dot dot-mine"></span>已预约（可拖拽改期）</span>
            <span class="legend-item"><span class="legend-dot dot-booked"></span>教练已有课</span>
            <span class="legend-item"><span class="legend-dot dot-empty"></span>空闲可预约</span>
            <span class="legend-tip">💡 点击空格预约 · 拖拽蓝色课程改期</span>
          </div>

          <div class="timetable">
            <div class="timetable-header">
              <div class="timetable-corner">时间</div>
              <div v-for="(d, i) in weekDays" :key="i" class="timetable-day" :class="{ today: isToday(d) }">
                <div class="day-name">{{ dayNames[i] }}</div>
                <div class="day-date">{{ formatDayDate(d) }}</div>
              </div>
            </div>

            <div v-for="slot in timeSlots" :key="slot" class="timetable-row">
              <div class="timetable-time">{{ slot }}</div>
              <div v-for="(d, di) in weekDays" :key="di"
                   class="timetable-cell"
                   :class="{
                     'has-event': getEvent(selectedCoach.coachId, d, slot),
                     'drag-over': isDragOver(d, slot)
                   }"
                   @dragover.prevent
                   @dragenter.prevent="onDragEnter(d, slot)"
                   @dragleave="onDragLeave(d, slot)"
                   @drop.prevent="onDrop(d, slot)">

                <!-- Has event -->
                <template v-if="getEvent(selectedCoach.coachId, d, slot)">
                  <!-- Booked by me: draggable -->
                  <div v-if="isBookedByMe(getEvent(selectedCoach.coachId, d, slot)!)"
                       class="event-block ev-mine"
                       draggable="true"
                       :class="{ dragging: isDragging(getEvent(selectedCoach.coachId, d, slot)!) }"
                       @dragstart="onDragStart($event, getEvent(selectedCoach.coachId, d, slot)!)">
                    <span class="ev-drag-icon">⠿</span>
                    <span class="ev-label">已预约</span>
                    <span class="ev-hint">拖拽改期</span>
                    <el-button type="danger" size="small" class="ev-cancel"
                               @click.stop="handleCancel(selectedCoach.enrollmentId, getEvent(selectedCoach.coachId, d, slot)!.id!)">✕ 取消</el-button>
                  </div>

                  <!-- Other events (coach's courses, booked by others, etc) -->
                  <div v-else class="event-block ev-course">
                    <span class="ev-label">{{ getEvent(selectedCoach.coachId, d, slot)!.title || '课程' }}</span>
                  </div>
                </template>

                <!-- Empty cell: member books directly -->
                <div v-else class="add-btn" @click="openBookDialog(d, slot)">
                  <span>+</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- ═══════ Booking Dialog ═══════ -->
    <el-dialog v-model="bookDialogVisible" title="预约私教课" width="400px">
      <div class="dialog-info">
        <div class="dialog-coach" v-if="selectedCoach">
          <div class="coach-avatar-ph small">{{ (selectedCoach.realName || '?')[0] }}</div>
          <span>{{ selectedCoach.realName }}</span>
        </div>
        <div class="dialog-time">
          <span>📅 {{ bookDialogDate }}</span>
          <span>🕐 {{ bookDialogSlot }}</span>
        </div>
        <div class="dialog-sessions">
          剩余课时：<strong>{{ selectedCoach?.remainingSessions ?? 0 }}</strong>
        </div>
      </div>
      <template #footer>
        <el-button @click="bookDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bookLoading"
                   :disabled="!selectedCoach || selectedCoach.remainingSessions <= 0"
                   @click="handleConfirmBook">确认预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getMyCoaches, bookDirect, cancelBooking, rescheduleBooking, type MyCoachItem } from '../../api/privateCoach';
import { getCoachSchedules, type ScheduleEvent } from '../../api/schedule';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();
const userId = userStore.user?.id || 0;

const loading = ref(false);
const bookLoading = ref(false);
const myCoaches = ref<MyCoachItem[]>([]);
const coachSchedules = ref<Record<number, ScheduleEvent[]>>({});
const currentWeekOffset = ref(0);
const selectedCoachId = ref<number | null>(null);

const selectedCoach = computed(() =>
  myCoaches.value.find(mc => mc.coachId === selectedCoachId.value) || null
);

const dayNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
const timeSlots = Array.from({ length: 14 }, (_, i) => `${String(8 + i).padStart(2, '0')}:00`);

function getWeekStart(offset: number) {
  const now = new Date();
  const day = now.getDay();
  const monday = new Date(now);
  monday.setDate(now.getDate() - (day === 0 ? 6 : day - 1) + offset * 7);
  monday.setHours(0, 0, 0, 0);
  return monday;
}

const weekDays = computed(() => {
  const start = getWeekStart(currentWeekOffset.value);
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(start);
    d.setDate(start.getDate() + i);
    return d;
  });
});

const weekRangeLabel = computed(() => {
  const days = weekDays.value;
  if (!days.length) return '';
  const fmt = (d: Date) => `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  return `${fmt(days[0])} ~ ${fmt(days[6])}`;
});

function isToday(d: Date) {
  const now = new Date();
  return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth() && d.getDate() === now.getDate();
}

function formatDayDate(d: Date) {
  return `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function getEvent(coachId: number, day: Date, slot: string): ScheduleEvent | undefined {
  const schedules = coachSchedules.value[coachId] || [];
  const dayStr = `${day.getFullYear()}-${String(day.getMonth() + 1).padStart(2, '0')}-${String(day.getDate()).padStart(2, '0')}`;
  return schedules.find(s => {
    if (!s.startTime) return false;
    const st = new Date(s.startTime);
    const sDateStr = `${st.getFullYear()}-${String(st.getMonth() + 1).padStart(2, '0')}-${String(st.getDate()).padStart(2, '0')}`;
    const sSlot = `${String(st.getHours()).padStart(2, '0')}:${String(st.getMinutes()).padStart(2, '0')}`;
    return sDateStr === dayStr && sSlot === slot;
  });
}

function isBookedByMe(ev: ScheduleEvent) {
  return ev.bookingStatus === 'BOOKED' && ev.memberId === userId;
}

function onCoachChange() {
  if (selectedCoachId.value) fetchSchedules(selectedCoachId.value);
}

// ====== Booking Dialog ======
const bookDialogVisible = ref(false);
const bookDialogDate = ref('');
const bookDialogSlot = ref('');
const bookDialogDay = ref<Date | null>(null);
const bookDialogSlotStr = ref('');

function openBookDialog(day: Date, slot: string) {
  if (!selectedCoach.value || selectedCoach.value.remainingSessions <= 0) {
    ElMessage.warning('课时不足，请先购买该教练的私教课');
    return;
  }
  bookDialogDay.value = day;
  bookDialogSlotStr.value = slot;
  bookDialogDate.value = `${day.getFullYear()}年${day.getMonth() + 1}月${day.getDate()}日 ${dayNames[weekDays.value.findIndex(w => w.getTime() === day.getTime())] || ''}`;
  const [h] = slot.split(':').map(Number);
  bookDialogSlot.value = `${slot} - ${String(h + 1).padStart(2, '0')}:00`;
  bookDialogVisible.value = true;
}

async function handleConfirmBook() {
  if (!selectedCoachId.value || !selectedCoach.value || !bookDialogDay.value || !bookDialogSlotStr.value) return;
  if (selectedCoach.value.remainingSessions <= 0) {
    ElMessage.warning('课时不足');
    return;
  }
  bookLoading.value = true;
  try {
    const day = bookDialogDay.value;
    const [h] = bookDialogSlotStr.value.split(':').map(Number);
    const dateStr = `${day.getFullYear()}-${String(day.getMonth() + 1).padStart(2, '0')}-${String(day.getDate()).padStart(2, '0')}`;
    const startTime = `${dateStr}T${String(h).padStart(2, '0')}:00:00`;
    const endTime = `${dateStr}T${String(h + 1).padStart(2, '0')}:00:00`;

    await bookDirect(userId, selectedCoachId.value, startTime, endTime);
    ElMessage.success('预约成功');
    bookDialogVisible.value = false;
    await fetchSchedules(selectedCoachId.value);
    // Refresh coach list to update remaining sessions
    myCoaches.value = await getMyCoaches(userId);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '预约失败');
  } finally {
    bookLoading.value = false;
  }
}

// ====== Drag & drop (reschedule to any empty slot) ======
const draggingEvent = ref<ScheduleEvent | null>(null);
const dragOverCell = ref<string | null>(null);

function onDragStart(e: DragEvent, event: ScheduleEvent) {
  draggingEvent.value = event;
  e.dataTransfer!.effectAllowed = 'move';
  e.dataTransfer!.setData('text/plain', String(event.id));
  const el = e.target as HTMLElement;
  setTimeout(() => { el.style.opacity = '0.4'; }, 0);
}

function onDragEnter(day: Date, slot: string) {
  if (!draggingEvent.value || !selectedCoachId.value) return;
  // Only allow drop on empty cells (no existing event)
  if (!getEvent(selectedCoachId.value, day, slot)) {
    dragOverCell.value = `${formatDayDate(day)}_${slot}`;
  }
}

function onDragLeave(day: Date, slot: string) {
  const key = `${formatDayDate(day)}_${slot}`;
  if (dragOverCell.value === key) dragOverCell.value = null;
}

function isDragOver(day: Date, slot: string): boolean {
  return dragOverCell.value === `${formatDayDate(day)}_${slot}`;
}

function isDragging(event: ScheduleEvent): boolean {
  return draggingEvent.value?.id === event.id;
}

async function onDrop(day: Date, slot: string) {
  dragOverCell.value = null;
  const event = draggingEvent.value;
  if (!event || !selectedCoachId.value) return;

  document.querySelectorAll('.event-block').forEach(el => {
    (el as HTMLElement).style.opacity = '1';
  });

  // Target must be empty
  if (getEvent(selectedCoachId.value, day, slot)) {
    draggingEvent.value = null;
    return;
  }

  const [h] = slot.split(':').map(Number);
  const dateStr = `${day.getFullYear()}-${String(day.getMonth() + 1).padStart(2, '0')}-${String(day.getDate()).padStart(2, '0')}`;
  const targetStart = `${dateStr}T${String(h).padStart(2, '0')}:00:00`;
  const targetEnd = `${dateStr}T${String(h + 1).padStart(2, '0')}:00:00`;

  try {
    await rescheduleBooking(userId, event.id!, targetStart, targetEnd);
    ElMessage.success('改期成功');
    await fetchSchedules(selectedCoachId.value);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '改期失败');
  }

  draggingEvent.value = null;
}

// ====== Cancel booking ======
async function handleCancel(enrollmentId: number, scheduleId: number) {
  try {
    await ElMessageBox.confirm('确定取消预约？将归还 1 节课时。', '取消确认');
    await cancelBooking(userId, scheduleId);
    ElMessage.success('已取消预约');
    await fetchAll();
  } catch (e: unknown) {
    if (e !== 'cancel' && e?.toString() !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '取消失败');
    }
  }
}

// ====== Data fetching ======
async function fetchAll() {
  loading.value = true;
  try {
    myCoaches.value = await getMyCoaches(userId);
    if (!selectedCoachId.value && myCoaches.value.length > 0) {
      selectedCoachId.value = myCoaches.value[0].coachId;
    }
    if (selectedCoachId.value) {
      await fetchSchedules(selectedCoachId.value);
    }
  } catch { /* ignore */ } finally { loading.value = false; }
}

async function fetchSchedules(coachId: number) {
  const start = weekDays.value[0];
  const end = new Date(weekDays.value[6]);
  end.setHours(23, 59, 59);
  try {
    const data = await getCoachSchedules(coachId, start.toISOString(), end.toISOString());
    coachSchedules.value[coachId] = data;
  } catch {
    coachSchedules.value[coachId] = [];
  }
}

watch(currentWeekOffset, () => {
  if (selectedCoachId.value) fetchSchedules(selectedCoachId.value);
});

onMounted(fetchAll);
</script>

<style scoped>
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }
.header-actions { display: flex; align-items: center; gap: 16px; }
.week-nav { display: flex; align-items: center; gap: 12px; }
.week-label { font-size: 13px; color: #64748b; font-family: 'JetBrains Mono', monospace; }
.coach-option { display: flex; align-items: center; width: 100%; }

.coach-block { border: 1px solid #d1d5db; border-radius: 12px; overflow: hidden; }
.coach-header { display: flex; align-items: center; gap: 12px; padding: 16px; background: #f8fafc; border-bottom: 1px solid #d1d5db; }
.coach-avatar { width: 48px; height: 48px; border-radius: 50%; overflow: hidden; flex-shrink: 0; }
.coach-avatar img { width: 100%; height: 100%; object-fit: cover; }
.coach-avatar-ph { width: 48px; height: 48px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 700; color: #fff; background: linear-gradient(135deg, #2563eb, #3b82f6); flex-shrink: 0; }
.coach-avatar-ph.small { width: 32px; height: 32px; font-size: 14px; }
.coach-info { flex: 1; }
.coach-name { font-weight: 600; font-size: 16px; }
.coach-tags { display: flex; gap: 4px; margin-top: 4px; }
.coach-sessions { text-align: right; }
.sessions-num { font-size: 28px; font-weight: 800; color: #2563eb; }
.sessions-total { font-size: 13px; color: #94a3b8; }

.legend-bar { display: flex; align-items: center; gap: 16px; padding: 8px 16px; background: #fafbfc; border-bottom: 1px solid #e2e8f0; font-size: 12px; color: #64748b; flex-wrap: wrap; }
.legend-item { display: flex; align-items: center; gap: 4px; }
.legend-dot { width: 10px; height: 10px; border-radius: 3px; }
.dot-mine { background: #3b82f6; border: 1px solid #2563eb; }
.dot-booked { background: #f59e0b; border: 1px solid #d97706; }
.dot-empty { background: #f1f5f9; border: 1px dashed #94a3b8; }
.legend-tip { margin-left: auto; color: #94a3b8; font-style: italic; }

/* Timetable */
.timetable { overflow-x: auto; }
.timetable-header { display: grid; grid-template-columns: 80px repeat(7, 1fr); border-bottom: 2px solid #cbd5e1; }
.timetable-corner { padding: 8px; text-align: center; font-size: 12px; color: #94a3b8; font-weight: 600; }
.timetable-day { padding: 8px 4px; text-align: center; border-left: 1px solid #cbd5e1; }
.timetable-day.today { background: #eff6ff; }
.day-name { font-size: 13px; font-weight: 600; color: #1e293b; }
.day-date { font-size: 11px; color: #94a3b8; font-family: 'JetBrains Mono', monospace; }
.timetable-day.today .day-name { color: #2563eb; }
.timetable-row { display: grid; grid-template-columns: 80px repeat(7, 1fr); border-bottom: 1px solid #cbd5e1; min-height: 56px; }
.timetable-time { padding: 4px 8px; font-size: 12px; font-weight: 500; color: #64748b; font-family: 'JetBrains Mono', monospace; background: #fafafa; display: flex; align-items: flex-start; justify-content: center; }
.timetable-cell { border-left: 1px solid #cbd5e1; padding: 2px; position: relative; min-height: 54px; display: flex; align-items: stretch; }
.timetable-cell.has-event { padding: 0; }

.event-block { width: 100%; border-radius: 6px; padding: 4px 6px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px; font-size: 11px; text-align: center; transition: all 0.2s; position: relative; }
.ev-label { font-weight: 600; font-size: 11px; line-height: 1.3; color: inherit !important; }
.ev-hint { font-size: 9px; opacity: 0.65; color: inherit !important; }

/* Mine (booked by me — draggable) */
.ev-mine { background: #dbeafe !important; border: 1px solid #3b82f6 !important; color: #1e3a8a !important; cursor: grab; user-select: none; }
.ev-mine .ev-label { color: #1e3a8a !important; }
.ev-mine .ev-hint { color: #1e3a8a !important; }
.ev-mine .ev-drag-icon { color: #1e3a8a !important; }
.ev-mine:hover { box-shadow: 0 2px 8px rgba(37,99,235,0.25); transform: scale(1.02); }
.ev-mine .ev-drag-icon { font-size: 10px; opacity: 0.5; letter-spacing: -2px; line-height: 1; }
.ev-mine:hover .ev-drag-icon { opacity: 0.8; }
.ev-mine .ev-cancel { margin-top: 2px; font-size: 10px; background: rgba(0,0,0,0.06); border: 1px solid rgba(0,0,0,0.15); border-radius: 4px; padding: 1px 6px; height: auto; }
.event-block.dragging { opacity: 0.4 !important; cursor: grabbing; }

/* Other events (coach's courses etc) */
.ev-course { background: #fef3c7; border: 1px solid #f59e0b; color: #1e293b !important; font-weight: 500; }

/* Drag over empty cell */
.timetable-cell.drag-over { background: #eff6ff !important; outline: 2px dashed #60a5fa; outline-offset: -2px; animation: pulse-border 0.8s ease-in-out infinite; }
@keyframes pulse-border { 0%,100% { outline-color: #60a5fa; } 50% { outline-color: #93c5fd; } }

/* + button */
.add-btn { width: 100%; display: flex; align-items: center; justify-content: center; cursor: pointer; border-radius: 6px; color: #cbd5e1; font-size: 22px; font-weight: 300; transition: all 0.2s; opacity: 0; }
.timetable-cell:hover .add-btn { opacity: 1; }
.add-btn:hover { background: #eff6ff; color: #2563eb; border: 1px dashed #93c5fd; }

/* Booking dialog */
.dialog-info { display: flex; flex-direction: column; gap: 12px; }
.dialog-coach { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; }
.dialog-time { display: flex; gap: 16px; padding: 10px 14px; background: #f0f9ff; border-radius: 8px; font-size: 14px; color: #1e40af; }
.dialog-sessions { font-size: 14px; color: #64748b; }
.dialog-sessions strong { font-size: 18px; color: #2563eb; }

/* ═══════ Dark theme ═══════ */
/* Moved to unscoped style block below for reliable :global() matching */
</style>

<!-- Unscoped overrides — beat nike-theme.css global !important rules -->
<style>
/* ── Beat .event-block { color: #fff !important } from nike-theme.css ── */

/* Light: ev-mine text = dark blue */
.my-coaches-page .ev-mine,
.my-coaches-page .ev-mine .ev-label,
.my-coaches-page .ev-mine .ev-hint,
.my-coaches-page .ev-mine .ev-drag-icon { color: #1e3a8a !important; }
.my-coaches-page .ev-mine .ev-cancel { color: #1e293b !important; font-weight: 600 !important; }

/* Light: ev-course text = black */
.my-coaches-page .ev-course,
.my-coaches-page .ev-course .ev-label { color: #1e293b !important; }

/* ── Dark theme: full overrides ── */

/* Card & containers */
[data-admin-theme="dark"] .my-coaches-page .el-card { background: #161822; border-color: rgba(255,255,255,0.06); color: #e0e0e0; }
[data-admin-theme="dark"] .my-coaches-page .card-header { color: #e8eaed; }
[data-admin-theme="dark"] .my-coaches-page .week-label { color: #5a5f73; }
[data-admin-theme="dark"] .my-coaches-page .sessions-num { color: #60a5fa; }
[data-admin-theme="dark"] .my-coaches-page .sessions-total { color: #4a4e63; }

/* Coach block */
[data-admin-theme="dark"] .my-coaches-page .coach-block { border-color: rgba(255,255,255,0.12); background: #1a1c28; }
[data-admin-theme="dark"] .my-coaches-page .coach-header { background: #1e2030 !important; border-bottom-color: rgba(255,255,255,0.1); }
[data-admin-theme="dark"] .my-coaches-page .coach-name { color: #e8eaed !important; }
[data-admin-theme="dark"] .my-coaches-page .coach-info .coach-tags .el-tag { background: rgba(255,255,255,0.06); border-color: rgba(255,255,255,0.08); color: #9ca3af; }
[data-admin-theme="dark"] .my-coaches-page .coach-avatar-ph { background: linear-gradient(135deg, #1d4ed8, #2563eb); }

/* Legend */
[data-admin-theme="dark"] .my-coaches-page .legend-bar { background: #12141d; border-bottom-color: rgba(255,255,255,0.04); color: #6b7084; }
[data-admin-theme="dark"] .my-coaches-page .legend-tip { color: #4a4e63; }
[data-admin-theme="dark"] .my-coaches-page .dot-empty { background: #2a2d3a; border-color: #4a4e63; }

/* Timetable grid */
[data-admin-theme="dark"] .my-coaches-page .timetable { border-color: rgba(255,255,255,0.06); }
[data-admin-theme="dark"] .my-coaches-page .timetable-header { border-bottom-color: rgba(255,255,255,0.15); background: #12141d; }
[data-admin-theme="dark"] .my-coaches-page .timetable-row { border-bottom-color: rgba(255,255,255,0.08); }
[data-admin-theme="dark"] .my-coaches-page .timetable-day { border-left-color: rgba(255,255,255,0.08); }
[data-admin-theme="dark"] .my-coaches-page .timetable-cell { border-left-color: rgba(255,255,255,0.08); background: #161822; }
[data-admin-theme="dark"] .my-coaches-page .timetable-time { background: #12141d; color: #7a7f96 !important; }
[data-admin-theme="dark"] .my-coaches-page .timetable-corner { color: #4a4e63; }
[data-admin-theme="dark"] .my-coaches-page .day-name { color: #c8cad0 !important; }
[data-admin-theme="dark"] .my-coaches-page .day-date { color: #4a4e63 !important; }
[data-admin-theme="dark"] .my-coaches-page .timetable-day.today { background: rgba(45,212,191,0.08); }
[data-admin-theme="dark"] .my-coaches-page .timetable-day.today .day-name { color: #2dd4bf !important; }

/* Booked by me: TEAL in dark */
[data-admin-theme="dark"] .my-coaches-page .ev-mine { background: rgba(45,212,191,0.15) !important; border-color: rgba(45,212,191,0.45) !important; }
[data-admin-theme="dark"] .my-coaches-page .ev-mine,
[data-admin-theme="dark"] .my-coaches-page .ev-mine .ev-label,
[data-admin-theme="dark"] .my-coaches-page .ev-mine .ev-hint,
[data-admin-theme="dark"] .my-coaches-page .ev-mine .ev-drag-icon { color: #5eead4 !important; }
[data-admin-theme="dark"] .my-coaches-page .ev-mine:hover { box-shadow: 0 2px 8px rgba(45,212,191,0.25) !important; }
[data-admin-theme="dark"] .my-coaches-page .ev-mine .ev-cancel { color: #f87171 !important; }

/* Courses: PURPLE in dark */
[data-admin-theme="dark"] .my-coaches-page .ev-course { background: rgba(167,139,250,0.15) !important; border-color: rgba(167,139,250,0.4) !important; }
[data-admin-theme="dark"] .my-coaches-page .ev-course,
[data-admin-theme="dark"] .my-coaches-page .ev-course .ev-label { color: #c4b5fd !important; }

/* Drag over */
[data-admin-theme="dark"] .my-coaches-page .timetable-cell.drag-over { background: rgba(45,212,191,0.08) !important; outline-color: rgba(45,212,191,0.5); }

/* + button */
[data-admin-theme="dark"] .my-coaches-page .add-btn { color: #3d4155; }
[data-admin-theme="dark"] .my-coaches-page .add-btn:hover { background: rgba(45,212,191,0.08); color: #2dd4bf !important; border-color: rgba(45,212,191,0.3); }

/* Dialog */
[data-admin-theme="dark"] .my-coaches-page .dialog-time { background: rgba(45,212,191,0.1); color: #5eead4; }
[data-admin-theme="dark"] .my-coaches-page .dialog-sessions { color: #6b7084; }
[data-admin-theme="dark"] .my-coaches-page .dialog-sessions strong { color: #5eead4; }
[data-admin-theme="dark"] .my-coaches-page .dialog-coach { color: #e8eaed; }
</style>
