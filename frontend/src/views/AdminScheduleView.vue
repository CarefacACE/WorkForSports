<template>
  <div class="admin-schedule-page">
    <el-card shadow="never">
      <template #header>
        <div class="schedule-header">
          <span>📅 日程管理（全部教练）</span>
          <div class="schedule-actions">
            <el-button @click="prevWeek">◀ 上一周</el-button>
            <span class="week-label">{{ weekLabel }}</span>
            <el-button @click="nextWeek">下一周 ▶</el-button>
            <el-button type="primary" @click="fetchAll">刷新</el-button>
          </div>
        </div>
      </template>

      <!-- 周视图网格 -->
      <div class="week-grid" v-loading="loading">
        <!-- 表头：星期几 + 日期 -->
        <div class="grid-header">
          <div class="time-col-header">时间</div>
          <div v-for="d in weekDays" :key="d.date" class="day-col-header">
            <div class="day-name">{{ d.label }}</div>
            <div class="day-date">{{ d.date }}</div>
          </div>
        </div>

        <!-- 时间槽行 -->
        <div v-for="slot in timeSlots" :key="slot" class="grid-row">
          <div class="time-label">{{ slot }}</div>
          <div v-for="d in weekDays" :key="d.date" class="grid-cell" :class="{ weekend: d.isWeekend }">
            <div
              v-for="event in getEventsForSlot(slot, d.date)"
              :key="event.id"
              class="schedule-block"
              :style="{ background: event.color || '#3056d3' }"
              @click="openEditDialog(event)"
            >
              <div class="block-title">{{ event.title }}</div>
              <div class="block-time">{{ formatTime(event.startTime) }} - {{ formatTime(event.endTime) }}</div>
              <div class="block-coach">教练ID: {{ event.coachId }}</div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="!loading && allEvents.length === 0" style="text-align:center;color:#94a3b8;padding:40px;">
        暂无日程数据
      </div>
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
import { getAllSchedules, adminUpdateSchedule, adminDeleteSchedule, type ScheduleEvent } from '../api/schedule';

const loading = ref(false);
const allEvents = ref<ScheduleEvent[]>([]);

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

function prevWeek() { weekOffset.value--; fetchAll(); }
function nextWeek() { weekOffset.value++; fetchAll(); }

function formatTime(dt: string) {
  return dt ? dt.slice(11, 16) : '';
}

function getEventsForSlot(slot: string, date: string) {
  const [hour] = slot.split(':').map(Number);
  return allEvents.value.filter(e => {
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

async function handleSave() {
  if (!editingId.value) return;
  saving.value = true;
  try {
    await adminUpdateSchedule({ ...editForm.value, id: editingId.value });
    ElMessage.success('日程已更新');
    editDialogVisible.value = false;
    await fetchAll();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

async function handleDelete() {
  if (!editingId.value) return;
  try {
    await ElMessageBox.confirm('确定删除该日程？', '删除确认', { type: 'warning' });
    await adminDeleteSchedule(editingId.value);
    ElMessage.success('日程已删除');
    editDialogVisible.value = false;
    await fetchAll();
  } catch (e: unknown) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '删除失败');
  }
}

/* ─── 数据加载 ─── */
async function fetchAll() {
  loading.value = true;
  const monday = getMonday();
  const sunday = new Date(monday);
  sunday.setDate(sunday.getDate() + 7);
  try {
    allEvents.value = await getAllSchedules(
      toISODate(monday) + 'T00:00:00',
      toISODate(sunday) + 'T00:00:00',
    ) || [];
  } catch { /* ignore */ }
  loading.value = false;
}

onMounted(fetchAll);
</script>

<style scoped>
.admin-schedule-page { display: flex; flex-direction: column; }

.schedule-header { display: flex; align-items: center; justify-content: space-between; }
.schedule-actions { display: flex; align-items: center; gap: 8px; }
.week-label { font-weight: 600; font-size: 14px; color: #475569; min-width: 180px; text-align: center; }

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
.block-title { font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.block-time { opacity: 0.8; }
.block-coach { opacity: 0.7; font-size: 10px; }
</style>
