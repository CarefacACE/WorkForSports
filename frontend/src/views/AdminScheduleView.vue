<template>
  <div class="admin-schedule-page">
    <el-card shadow="never">
      <template #header>
        <div class="schedule-header">
          <span>📅 日程管理</span>
          <div class="schedule-actions">
            <!-- 人员选择器 -->
            <el-select
              v-model="selectedUserId"
              filterable
              remote
              reserve-keyword
              placeholder="搜索姓名选择用户"
              :remote-method="searchPeople"
              :loading="searchingUsers"
              clearable
              style="width: 220px;"
              @change="onUserChange"
            >
              <el-option
                v-for="u in userOptions"
                :key="u.id"
                :label="`${u.realName || u.username} (${roleTag(u.role)})`"
                :value="u.id"
              />
            </el-select>
            <template v-if="selectedUser">
              <el-tag :type="selectedUser.role === 'COACH' ? 'warning' : 'success'" effect="dark">
                {{ roleTag(selectedUser.role) }}
              </el-tag>
              <el-button @click="prevWeek">◀ 上一周</el-button>
              <span class="week-label">{{ weekLabel }}</span>
              <el-button @click="nextWeek">下一周 ▶</el-button>
              <el-button type="primary" @click="fetchSchedules">刷新</el-button>
            </template>
          </div>
        </div>
      </template>

      <!-- 空状态提示 -->
      <div v-if="!selectedUser" style="text-align:center;color:#94a3b8;padding:80px 0;">
        👆 请在上方选择一个教练或会员查看其日程
      </div>

      <!-- 周视图网格 -->
      <template v-if="selectedUser">
        <div class="week-grid" v-loading="loading">
          <div class="grid-header">
            <div class="time-col-header">时间</div>
            <div v-for="d in weekDays" :key="d.date" class="day-col-header">
              <div class="day-name">{{ d.label }}</div>
              <div class="day-date">{{ d.date }}</div>
            </div>
          </div>

          <div v-for="slot in timeSlots" :key="slot" class="grid-row">
            <div class="time-label">{{ slot }}</div>
            <div v-for="d in weekDays" :key="d.date" class="grid-cell" :class="{ weekend: d.isWeekend, 'drag-over-cell': isDragOver(d.date, slot) }"
              @dragover.prevent="() => onDragOver(d.date, slot)"
              @dragenter.prevent="() => onDragEnter(d.date, slot)"
              @dragleave="() => onDragLeave(d.date, slot)"
              @drop.prevent="() => onDrop(d.date, slot)">
              <div
                v-for="event in getEventsForSlot(slot, d.date)"
                :key="event.id"
                class="schedule-block"
                :class="{ dragging: isDragging(event), 'event-past': isPast(event) }"
                :style="{ background: event.color || '#3056d3' }"
                :draggable="!isPast(event)"
                @click.stop="openEditDialog(event)"
                @dragstart="onDragStart($event, event, slot, d.date)"
              >
                <div class="block-title">{{ event.title }}</div>
                <div class="block-time">{{ formatTime(event.startTime) }} - {{ formatTime(event.endTime) }}</div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="!loading && events.length === 0" style="text-align:center;color:#94a3b8;padding:40px;">
          该用户本周暂无日程
        </div>
      </template>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑日程" width="460px">
      <el-form :model="editForm" label-position="top">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="editForm.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="editForm.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="editForm.location" placeholder="如：A教室" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleDelete">删除</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getAllSchedules, getCoachSchedules, getMemberSchedules, adminUpdateSchedule, adminDeleteSchedule, type ScheduleEvent } from '../api/schedule';
import { getAllUsersForAdmin, type AdminUser } from '../api/chat';

const loading = ref(false);
const events = ref<ScheduleEvent[]>([]);

/* ─── 人员选择 ─── */
const selectedUserId = ref<number | null>(null);
const selectedUser = ref<AdminUser | null>(null);
const searchingUsers = ref(false);
const userOptions = ref<AdminUser[]>([]);

function roleTag(role: string) { return role === 'ADMIN' ? '管理员' : role === 'COACH' ? '教练' : '会员'; }

async function searchPeople(query: string) {
  if (!query) { userOptions.value = []; return; }
  searchingUsers.value = true;
  try {
    const all = await getAllUsersForAdmin();
    const q = query.toLowerCase();
    userOptions.value = all.filter(
      u => (u.username || '').toLowerCase().includes(q) || (u.realName || '').toLowerCase().includes(q)
    ).slice(0, 20);
  } catch { /* ignore */ }
  searchingUsers.value = false;
}

async function onUserChange() {
  if (!selectedUserId.value) { selectedUser.value = null; events.value = []; return; }
  const all = await getAllUsersForAdmin();
  selectedUser.value = all.find(u => u.id === selectedUserId.value) || null;
  weekOffset.value = 0;
  await fetchSchedules();
}

/* ─── 周视图 ─── */
const timeSlots = ['08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00', '19:00', '20:00', '21:00'];
const weekOffset = ref(0);

const weekLabel = computed(() => {
  const monday = getMonday();
  const sunday = new Date(monday);
  sunday.setDate(sunday.getDate() + 6);
  return `${fmtDate(monday)} ~ ${fmtDate(sunday)}`;
});

const weekDays = computed(() => {
  const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
  const monday = getMonday();
  return days.map((label, i) => {
    const d = new Date(monday);
    d.setDate(d.getDate() + i);
    return { label, date: toISODate(d), isWeekend: i >= 5 };
  });
});

function getMonday() {
  const now = new Date();
  now.setDate(now.getDate() - (now.getDay() || 7) + 1 + weekOffset.value * 7);
  return now;
}
function fmtDate(d: Date) { return `${d.getMonth() + 1}/${d.getDate()}`; }
function toISODate(d: Date) { return d.toISOString().slice(0, 10); }
function prevWeek() { weekOffset.value--; fetchSchedules(); }
function nextWeek() { weekOffset.value++; fetchSchedules(); }
function formatTime(dt: string) { return dt ? dt.slice(11, 16) : ''; }

function getEventsForSlot(slot: string, date: string) {
  const [hour] = slot.split(':').map(Number);
  return events.value.filter(e => {
    if (!e.startTime) return false;
    const eventDate = e.startTime.slice(0, 10);
    const eventHour = parseInt(e.startTime.slice(11, 13));
    return eventDate === date && eventHour === hour;
  });
}

/* ─── 编辑弹窗 ─── */
const editDialogVisible = ref(false);
const saving = ref(false);
const editForm = ref<ScheduleEvent>({ title: '', startTime: '', endTime: '', location: '', color: '#3056d3' });
const editingId = ref<number | null>(null);

function openEditDialog(event: ScheduleEvent) {
  editingId.value = event.id || null;
  editForm.value = {
    title: event.title,
    startTime: event.startTime,
    endTime: event.endTime,
    location: event.location || '',
    color: event.color || '#3056d3',
  };
  editDialogVisible.value = true;
}

/* ─── 拖拽排课 ─── */
const draggingEvent = ref<(ScheduleEvent & { _date?: string; _slot?: string }) | null>(null);
const dragOverCell = ref<string | null>(null); // "date_slot"

function isPast(event: ScheduleEvent): boolean {
  return new Date(event.endTime) < new Date();
}

function isDragging(event: ScheduleEvent): boolean {
  return draggingEvent.value?.id === event.id;
}

function isDragOver(date: string, slot: string): boolean {
  return dragOverCell.value === `${date}_${slot}`;
}

function onDragStart(e: DragEvent, event: ScheduleEvent, slot: string, date: string) {
  if (isPast(event)) {
    e.preventDefault();
    ElMessage.warning('已结束的日程无法拖拽');
    return;
  }
  draggingEvent.value = { ...event, _slot: slot, _date: date };
  e.dataTransfer!.effectAllowed = 'move';
  e.dataTransfer!.setData('text/plain', String(event.id));
  const el = e.target as HTMLElement;
  setTimeout(() => { el.style.opacity = '0.4'; }, 0);
}

function onDragOver(_date: string, _slot: string) { /* preventDefault in handler */ }
function onDragEnter(date: string, slot: string) {
  const key = `${date}_${slot}`;
  if (!getEventsForSlot(slot, date).length) {
    dragOverCell.value = key;
  }
}
function onDragLeave(date: string, slot: string) {
  const key = `${date}_${slot}`;
  if (dragOverCell.value === key) {
    dragOverCell.value = null;
  }
}

async function onDrop(date: string, slotStr: string) {
  dragOverCell.value = null;
  const event = draggingEvent.value;
  if (!event) return;

  // restore opacity
  document.querySelectorAll('.schedule-block').forEach(el => {
    (el as HTMLElement).style.opacity = '1';
  });

  if (isPast(event)) {
    ElMessage.warning('已结束的日程无法拖拽');
    draggingEvent.value = null;
    return;
  }

  // same cell or target occupied
  if ((date === event._date && slotStr === event._slot) || getEventsForSlot(slotStr, date).length) {
    draggingEvent.value = null;
    return;
  }

  const [h] = slotStr.split(':').map(Number);
  const startTime = `${date}T${slotStr}:00`;
  const endTime = `${date}T${String(h + 1).padStart(2, '0')}:00:00`;

  // compute duration for multi-hour events
  if (event.startTime && event.endTime) {
    const oldStart = new Date(event.startTime);
    const oldEnd = new Date(event.endTime);
    const durationMs = oldEnd.getTime() - oldStart.getTime();
    const newEnd = new Date(startTime);
    newEnd.setTime(newEnd.getTime() + durationMs);
    const endStr = newEnd.toISOString().slice(0, 19);
    await doDropUpdate(event, startTime, endStr);
  } else {
    await doDropUpdate(event, startTime, endTime);
  }
}

async function doDropUpdate(event: ScheduleEvent, startTime: string, endTime: string) {
  try {
    await adminUpdateSchedule({
      id: event.id,
      title: event.title,
      startTime,
      endTime,
      location: event.location,
      color: event.color,
    });
    ElMessage.success('日程已移动');
    await fetchSchedules();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '移动失败');
  }
  draggingEvent.value = null;
}

async function handleSave() {
  if (!editingId.value) return;
  saving.value = true;
  try {
    await adminUpdateSchedule({ ...editForm.value, id: editingId.value });
    ElMessage.success('日程已更新');
    editDialogVisible.value = false;
    await fetchSchedules();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败');
  } finally { saving.value = false; }
}

async function handleDelete() {
  if (!editingId.value) return;
  try {
    await ElMessageBox.confirm('确定删除该日程？', '删除确认', { type: 'warning' });
    await adminDeleteSchedule(editingId.value);
    ElMessage.success('日程已删除');
    editDialogVisible.value = false;
    await fetchSchedules();
  } catch (e: unknown) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '删除失败');
  }
}

/* ─── 数据加载 ─── */
async function fetchSchedules() {
  if (!selectedUserId.value || !selectedUser.value) return;
  loading.value = true;
  const monday = getMonday();
  const sunday = new Date(monday);
  sunday.setDate(sunday.getDate() + 7);
  const from = toISODate(monday) + 'T00:00:00';
  const to = toISODate(sunday) + 'T00:00:00';

  try {
    if (selectedUser.value.role === 'COACH') {
      events.value = await getCoachSchedules(selectedUserId.value, from, to) || [];
    } else {
      events.value = await getMemberSchedules(selectedUserId.value, from, to) || [];
    }
  } catch { /* ignore */ }
  loading.value = false;
}

onMounted(async () => {
  // 预加载用户列表
  try { userOptions.value = await getAllUsersForAdmin() || []; } catch { /* ignore */ }
});
</script>

<style scoped>
.admin-schedule-page { display: flex; flex-direction: column; flex: 1; min-height: 0; }

.schedule-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; }
.schedule-actions { display: flex; align-items: center; gap: 8px; }
.week-label { font-weight: 600; font-size: 14px; color: #475569; min-width: 160px; text-align: center; }

.week-grid { border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden; }
.grid-header, .grid-row { display: flex; }
.time-col-header { width: 64px; text-align: center; font-size: 12px; color: #94a3b8; padding: 8px 0; border-bottom: 1px solid #e5e7eb; background: #f8fafc; }
.day-col-header { flex: 1; text-align: center; padding: 8px 0; border-left: 1px solid #e5e7eb; border-bottom: 1px solid #e5e7eb; background: #f8fafc; }
.day-name { font-weight: 600; font-size: 13px; color: #475569; }
.day-date { font-size: 11px; color: #94a3b8; }

.time-label { width: 64px; text-align: center; font-size: 12px; color: #94a3b8; padding-top: 4px; border-bottom: 1px solid #f1f5f9; }
.grid-cell { flex: 1; min-height: 60px; padding: 2px; border-left: 1px solid #f1f5f9; border-bottom: 1px solid #f1f5f9; }
.grid-cell.weekend { background: #fafafa; }

.schedule-block { border-radius: 4px; padding: 3px 6px; margin: 1px 0; cursor: pointer; color: #fff; font-size: 11px; transition: opacity 0.15s; }
.schedule-block:hover { opacity: 0.85; }
.schedule-block.dragging { opacity: 0.4; cursor: grabbing; }
.schedule-block.event-past { opacity: 0.5; cursor: default; }
.schedule-block.event-past:hover { opacity: 0.5; transform: none; }
.block-title { font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.block-time { opacity: 0.8; }

.grid-cell.drag-over-cell { background: #ecf5ff !important; outline: 2px dashed #409eff; outline-offset: -2px; }
</style>
