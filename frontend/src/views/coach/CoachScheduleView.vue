<template>
  <div class="schedule-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>排课课表</span>
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

      <!-- 课表网格 -->
      <div class="timetable">
        <!-- 表头 -->
        <div class="timetable-header">
          <div class="timetable-cell timetable-corner">时间</div>
          <div v-for="(day, i) in weekDays" :key="i" class="timetable-cell timetable-day"
               :class="{ today: isToday(day.date) }">
            <div class="day-name">{{ day.name }}</div>
            <div class="day-date">{{ day.dateStr }}</div>
          </div>
        </div>

        <!-- 时间行 -->
        <div v-for="slot in timeSlots" :key="slot.start" class="timetable-row">
          <div class="timetable-cell timetable-time">{{ slot.start }}<br><span class="time-end">{{ slot.end }}</span></div>
          <div v-for="(day, i) in weekDays" :key="i"
               class="timetable-cell timetable-slot"
               :class="{
                 'has-event': getEvent(day.date, slot.start),
                 'is-past': isPast(day.date, slot.start),
                 'drag-over': isDragOver(day.date, slot.start)
               }"
               @click="onCellClick(day.date, slot.start)"
               @dragover.prevent="onDragOver(day.date, slot.start)"
               @dragenter.prevent="onDragEnter(day.date, slot.start)"
               @dragleave="onDragLeave(day.date, slot.start)"
               @drop.prevent="onDrop(day.date, slot.start)">
            <!-- 已排课（可拖拽） -->
            <div v-if="getEvent(day.date, slot.start)" class="event-block"
                 :style="{ background: getEvent(day.date, slot.start)!.color }"
                 :draggable="!isPast(day.date, slot.start) && getEvent(day.date, slot.start)!.bookingStatus !== 'REQUESTED'"
                 :class="{
                   dragging: isDragging(getEvent(day.date, slot.start)!),
                   'event-past': isPast(day.date, slot.start),
                   'event-requested': getEvent(day.date, slot.start)!.bookingStatus === 'REQUESTED'
                 }"
                 @click.stop="onEventClick(getEvent(day.date, slot.start)!)"
                 @dragstart="getEvent(day.date, slot.start)!.bookingStatus !== 'REQUESTED' ? onDragStart($event, getEvent(day.date, slot.start)!) : void 0">
              <div class="event-title">{{ getEvent(day.date, slot.start)!.title }}</div>
              <div class="event-badge" v-if="getEvent(day.date, slot.start)!.bookingStatus === 'REQUESTED'">待审批</div>
              <div class="event-location" v-if="getEvent(day.date, slot.start)!.location">{{ getEvent(day.date, slot.start)!.location }}</div>
            </div>
            <!-- 空位 hover 加号 -->
            <div v-else class="add-btn" @click.stop="openAddDialog(day.date, slot.start)">
              <el-icon :size="20"><Plus /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 添加排课弹窗 -->
    <el-dialog v-model="addDialogVisible" title="添加排课" width="440px">
      <el-form :model="addForm" label-position="top">
        <el-form-item label="选择课程" required>
          <el-select v-model="addForm.courseId" placeholder="请选择已创建的课程" style="width:100%" @change="onAddCourseChange">
            <el-option v-for="c in myCourses" :key="c.id" :label="c.name" :value="c.id">
              <span class="option-dot" :style="{ background: getCourseColor(c.id) }"></span>
              {{ c.name }}
              <el-tag size="small" type="info" style="margin-left:8px">{{ c.type === 'PUBLIC' ? '公共' : '私教' }}</el-tag>
            </el-option>
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="日期">
              <el-input :model-value="addForm.dateStr" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时间段">
              <el-input :model-value="addForm.timeSlot" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="上课地点">
          <el-input v-model="addForm.location" placeholder="可选，如：A区操房" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleAdd">确认排课</el-button>
      </template>
    </el-dialog>

    <!-- 编辑/删除排课弹窗 -->
    <el-dialog v-model="editDialogVisible" title="排课详情" width="440px">
      <!-- 审批提示 -->
      <el-alert v-if="editingEvent?.bookingStatus === 'REQUESTED'"
                title="该时段有待审批的预约请求" type="warning" show-icon :closable="false"
                style="margin-bottom: 16px" />

      <el-form :model="editForm" label-position="top">
        <el-form-item label="课程">
          <el-input :model-value="editForm.title" disabled />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="日期">
              <el-date-picker v-model="editForm.date" type="date" placeholder="选择日期" style="width: 100%"
                value-format="YYYY-MM-DD" :disabled="isPastEvent" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时间">
              <el-select v-model="editForm.timeSlot" placeholder="选择时间" style="width: 100%" :disabled="isPastEvent">
                <el-option v-for="slot in timeSlots" :key="slot.start" :label="`${slot.start} - ${slot.end}`" :value="slot.start" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="isPastEvent" label="提示">
          <el-tag type="info">已结束的课程无法修改时间</el-tag>
        </el-form-item>
        <el-form-item label="上课地点">
          <el-input v-model="editForm.location" placeholder="可选" />
        </el-form-item>
        <el-form-item label="我的签到状态">
          <el-tag :type="coachCheckInStatus === 'SIGNED' ? 'success' : coachCheckInStatus === 'ABSENT' ? 'danger' : 'info'" size="large">
            {{ coachCheckInStatus === 'SIGNED' ? '已签到' : coachCheckInStatus === 'ABSENT' ? '缺勤' : '待签到' }}
          </el-tag>
          <el-button v-if="coachCheckInStatus === 'PENDING'" type="primary" style="margin-left:12px" :loading="checkInLoading" @click="handleCoachCheckIn">签到</el-button>
        </el-form-item>
        <el-form-item v-if="scheduleCheckIns.length > 0" label="学员签到情况">
          <div v-for="item in scheduleCheckIns" :key="item.id" class="checkin-item">
            <span>用户{{ item.userId }}</span>
            <el-tag :type="item.status === 'SIGNED' ? 'success' : item.status === 'ABSENT' ? 'danger' : 'info'" size="small" style="margin-left:8px">
              {{ item.status === 'SIGNED' ? '已签到' : item.status === 'ABSENT' ? '缺勤' : '待签到' }}
            </el-tag>
            <span v-if="item.checkInTime" style="margin-left:8px;font-size:12px;color:#909399">{{ item.checkInTime.replace('T',' ').slice(11,16) }}</span>
            <div class="checkin-actions">
              <el-button v-if="item.status !== 'SIGNED'" type="success" size="small" link @click="handleUpdateCheckIn(item.userId, 'SIGNED')">标记已到</el-button>
              <el-button v-if="item.status !== 'ABSENT'" type="danger" size="small" link @click="handleUpdateCheckIn(item.userId, 'ABSENT')">标记缺勤</el-button>
              <el-button v-if="item.status !== 'PENDING'" type="info" size="small" link @click="handleUpdateCheckIn(item.userId, 'PENDING')">重置</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <template v-if="editingEvent?.bookingStatus === 'REQUESTED'">
          <el-button type="danger" @click="handleRejectRequest">拒绝</el-button>
          <el-button @click="editDialogVisible = false">关闭</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleApproveRequest">通过</el-button>
        </template>
        <template v-else>
          <el-button type="danger" @click="handleDeleteEvent">删除排课</el-button>
          <el-button @click="editDialogVisible = false">关闭</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleEditEvent">保存</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { getMyCourses, type Course } from '../../api/course';
import { getCoachSchedules, createSchedule, updateSchedule, deleteSchedule, type ScheduleEvent } from '../../api/schedule';
import { checkIn, getCheckInStatus, getScheduleCheckIns, updateCheckInStatus, type CheckInRecord } from '../../api/checkin';
import { approveSession, rejectSession } from '../../api/privateCoach';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();
const coachId = userStore.user?.id;

// ====== 时间槽 ======
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

// ====== 周视图 ======
const currentWeekOffset = ref(0);

function getWeekStart(offset: number): Date {
  const now = new Date();
  const day = now.getDay() || 7;
  const monday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - day + 1 + offset * 7);
  return monday;
}

const weekDays = computed(() => {
  const start = getWeekStart(currentWeekOffset.value);
  const names = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
  return names.map((name, i) => {
    const date = new Date(start);
    date.setDate(start.getDate() + i);
    return {
      name,
      date,
      dateStr: `${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`,
    };
  });
});

const weekRangeLabel = computed(() => {
  const days = weekDays.value;
  return `${days[0].dateStr} ~ ${days[6].dateStr}`;
});

function isToday(date: Date): boolean {
  const today = new Date();
  return date.getFullYear() === today.getFullYear() && date.getMonth() === today.getMonth() && date.getDate() === today.getDate();
}

function isPast(date: Date, slot: string): boolean {
  const [h, m] = slot.split(':').map(Number);
  const slotDate = new Date(date);
  slotDate.setHours(h, m, 0, 0);
  return slotDate < new Date();
}

// ====== 课程颜色 ======
const colorPalette = ['#3056d3', '#e6a23c', '#67c23a', '#f56c6c', '#909399', '#9b59b6', '#1abc9c', '#e74c3c'];
const courseColorMap = new Map<number, string>();
function getCourseColor(id: number): string {
  if (!courseColorMap.has(id)) courseColorMap.set(id, colorPalette[courseColorMap.size % colorPalette.length]);
  return courseColorMap.get(id)!;
}

// ====== 数据 ======
const myCourses = ref<Course[]>([]);
const events = ref<(ScheduleEvent & { _date: string; _slot: string })[]>([]);

function getEvent(date: Date, slot: string): (ScheduleEvent & { _date: string; _slot: string }) | undefined {
  const dateStr = formatDate(date);
  return events.value.find(e => e._date === dateStr && e._slot === slot);
}

function formatDate(date: Date): string {
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
}

function formatDateTime(date: Date, slot: string): string {
  return `${formatDate(date)}T${slot}:00`;
}

function endTimeFromSlot(slot: string): string {
  const [h] = slot.split(':').map(Number);
  return `${(h + 1).toString().padStart(2, '0')}:00`;
}

// ====== 添加弹窗 ======
const addDialogVisible = ref(false);
const submitLoading = ref(false);
const addForm = ref({ courseId: null as number | null, dateStr: '', timeSlot: '', date: null as Date | null, location: '' });

function onCellClick(date: Date, slot: string) {
  if (getEvent(date, slot)) return;
  openAddDialog(date, slot);
}

function openAddDialog(date: Date, slot: string) {
  addForm.value = {
    courseId: null,
    dateStr: formatDate(date),
    timeSlot: `${slot} - ${endTimeFromSlot(slot)}`,
    date: date,
    location: '',
  };
  addDialogVisible.value = true;
}

function onAddCourseChange(courseId: number) {
  const course = myCourses.value.find(c => c.id === courseId);
  if (course?.location) addForm.value.location = course.location;
}

async function handleAdd() {
  if (!coachId || !addForm.value.courseId || !addForm.value.date) {
    ElMessage.warning('请选择课程');
    return;
  }
  submitLoading.value = true;
  try {
    const slot = addForm.value.timeSlot.split(' - ')[0];
    const course = myCourses.value.find(c => c.id === addForm.value.courseId);
    await createSchedule(coachId, {
      courseId: addForm.value.courseId!,
      title: course?.name || '',
      startTime: formatDateTime(addForm.value.date, slot),
      endTime: `${formatDate(addForm.value.date)}T${endTimeFromSlot(slot)}:00`,
      location: addForm.value.location,
      color: getCourseColor(addForm.value.courseId!),
    });
    ElMessage.success('排课成功');
    addDialogVisible.value = false;
    await fetchSchedules();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '排课失败');
  } finally {
    submitLoading.value = false;
  }
}

// ====== 编辑弹窗 ======
const editDialogVisible = ref(false);
const editingEvent = ref<(ScheduleEvent & { _date: string; _slot: string }) | null>(null);
const editForm = ref({ title: '', date: '', timeSlot: '', location: '' });
const coachCheckInStatus = ref<string>('PENDING');
const scheduleCheckIns = ref<CheckInRecord[]>([]);
const checkInLoading = ref(false);

const isPastEvent = computed(() => {
  if (!editingEvent.value) return false;
  const endTime = new Date(editingEvent.value.endTime);
  return endTime < new Date();
});

function onEventClick(event: ScheduleEvent & { _date: string; _slot: string }) {
  editingEvent.value = event;
  editForm.value = {
    title: event.title,
    date: event._date,
    timeSlot: event._slot,
    location: event.location || '',
  };
  editDialogVisible.value = true;
  loadCheckInData(event.id!);
}

async function loadCheckInData(scheduleId: number) {
  if (!coachId) return;
  try {
    const [status, checkIns] = await Promise.all([
      getCheckInStatus(scheduleId, coachId, 'COACH'),
      getScheduleCheckIns(scheduleId)
    ]);
    coachCheckInStatus.value = status?.status || 'PENDING';
    scheduleCheckIns.value = checkIns.filter(item => item.role === 'MEMBER');
  } catch {
    coachCheckInStatus.value = 'PENDING';
    scheduleCheckIns.value = [];
  }
}

async function handleCoachCheckIn() {
  if (!coachId || !editingEvent.value?.id) return;
  checkInLoading.value = true;
  try {
    await checkIn(editingEvent.value.id, coachId, 'COACH');
    ElMessage.success('签到成功');
    coachCheckInStatus.value = 'SIGNED';
    await loadCheckInData(editingEvent.value.id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '签到失败');
  } finally {
    checkInLoading.value = false;
  }
}

async function handleUpdateCheckIn(userId: number, status: string) {
  if (!coachId || !editingEvent.value?.id) return;
  try {
    await updateCheckInStatus(coachId, editingEvent.value.id, userId, status);
    ElMessage.success('修改成功');
    await loadCheckInData(editingEvent.value.id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '修改失败');
  }
}

async function handleEditEvent() {
  if (!coachId || !editingEvent.value) return;
  submitLoading.value = true;
  try {
    const slot = editForm.value.timeSlot;
    const endTime = endTimeFromSlot(slot);
    await updateSchedule(coachId, {
      ...editingEvent.value,
      startTime: `${editForm.value.date}T${slot}:00`,
      endTime: `${editForm.value.date}T${endTime}:00`,
      location: editForm.value.location,
    });
    ElMessage.success('已更新');
    editDialogVisible.value = false;
    await fetchSchedules();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新失败');
  } finally {
    submitLoading.value = false;
  }
}

async function handleDeleteEvent() {
  if (!coachId || !editingEvent.value?.id) return;
  try {
    await deleteSchedule(coachId, editingEvent.value.id);
    ElMessage.success('已删除');
    editDialogVisible.value = false;
    await fetchSchedules();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败');
  }
}

/* ─── 私教预约审批 ─── */

async function handleApproveRequest() {
  if (!coachId || !editingEvent.value?.id) return;
  submitLoading.value = true;
  try {
    await approveSession(coachId, editingEvent.value.id);
    ElMessage.success('已通过预约');
    editDialogVisible.value = false;
    await fetchSchedules();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '审批失败');
  } finally {
    submitLoading.value = false;
  }
}

async function handleRejectRequest() {
  if (!coachId || !editingEvent.value?.id) return;
  try {
    const { value: reason } = await ElMessageBox.prompt(
      '请输入拒绝原因（可选）',
      '拒绝预约',
      { confirmButtonText: '确定拒绝', cancelButtonText: '取消', inputPlaceholder: '填写拒绝原因...', inputType: 'textarea', inputValidator: (val: string) => true }
    );
    submitLoading.value = true;
    await rejectSession(coachId, editingEvent.value.id, reason || undefined);
    ElMessage.success('已拒绝预约');
    editDialogVisible.value = false;
    await fetchSchedules();
  } catch (e) {
    if (e !== 'cancel' && (e as any)?.toString() !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '操作失败');
    }
  } finally {
    submitLoading.value = false;
  }
}

// ====== 拖拽排课 ======
const draggingEvent = ref<(ScheduleEvent & { _date: string; _slot: string }) | null>(null);
const dragOverCell = ref<string | null>(null); // "YYYY-MM-DD_HH:mm"

function onDragStart(e: DragEvent, event: ScheduleEvent & { _date: string; _slot: string }) {
  // 不允许拖拽已经过去的课程
  const eventEndTime = new Date(event.endTime);
  if (eventEndTime < new Date()) {
    e.preventDefault();
    ElMessage.warning('已结束的课程无法修改时间');
    return;
  }
  draggingEvent.value = event;
  e.dataTransfer!.effectAllowed = 'move';
  e.dataTransfer!.setData('text/plain', String(event.id));
  // 设置拖拽时的半透明效果
  const el = e.target as HTMLElement;
  setTimeout(() => { el.style.opacity = '0.4'; }, 0);
}

function onDragOver(date: Date, slot: string) {
  // 已经在 handle 上设置了 preventDefault，这里只更新视觉
}

function onDragEnter(date: Date, slot: string) {
  const key = `${formatDate(date)}_${slot}`;
  // 不允许放到有课程的格子
  if (!getEvent(date, slot)) {
    dragOverCell.value = key;
  }
}

function onDragLeave(date: Date, slot: string) {
  const key = `${formatDate(date)}_${slot}`;
  if (dragOverCell.value === key) {
    dragOverCell.value = null;
  }
}

function isDragOver(date: Date, slot: string): boolean {
  return dragOverCell.value === `${formatDate(date)}_${slot}`;
}

function isDragging(event: ScheduleEvent & { _date: string; _slot: string }): boolean {
  return draggingEvent.value?.id === event.id;
}

async function onDrop(date: Date, slot: string) {
  dragOverCell.value = null;
  const event = draggingEvent.value;
  if (!event || !coachId) return;

  // 恢复到原来位置的样式
  document.querySelectorAll('.event-block').forEach(el => {
    (el as HTMLElement).style.opacity = '1';
  });

  // 不允许拖拽已经过去的课程
  const eventEndTime = new Date(event.endTime);
  if (eventEndTime < new Date()) {
    ElMessage.warning('已结束的课程无法修改时间');
    draggingEvent.value = null;
    return;
  }

  // 不允许放到有课程的格子或自己原来的位置
  const targetDate = formatDate(date);
  if ((targetDate === event._date && slot === event._slot) || getEvent(date, slot)) {
    draggingEvent.value = null;
    return;
  }

  // 计算新的开始和结束时间
  const [h] = slot.split(':').map(Number);
  const newStartTime = `${targetDate}T${slot}:00`;
  const newEndTime = `${targetDate}T${(h + 1).toString().padStart(2, '0')}:00:00`;

  try {
    await updateSchedule(coachId, {
      ...event,
      startTime: newStartTime,
      endTime: newEndTime,
    });
    // 乐观更新本地数据
    event.startTime = newStartTime;
    event.endTime = newEndTime;
    event._date = targetDate;
    event._slot = slot;
    ElMessage.success('已移动');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '移动失败');
  }

  draggingEvent.value = null;
}

// ====== 数据加载 ======
async function fetchCourses() {
  if (!coachId) return;
  try {
    const res = await getMyCourses(coachId, 1, 100);
    myCourses.value = res.records;
    myCourses.value.forEach(c => getCourseColor(c.id));
  } catch {}
}

async function fetchSchedules() {
  if (!coachId) return;
  try {
    const start = getWeekStart(currentWeekOffset.value);
    const end = new Date(start);
    end.setDate(start.getDate() + 6);
    end.setHours(23, 59, 59);
    const data = await getCoachSchedules(coachId, start.toISOString(), end.toISOString());
    events.value = data.map(s => ({
      ...s,
      _date: s.startTime.slice(0, 10),
      _slot: s.startTime.slice(11, 16),
    }));
  } catch {}
}

watch(currentWeekOffset, () => fetchSchedules());

onMounted(async () => {
  await fetchCourses();
  await fetchSchedules();
});
</script>

<style scoped>
.schedule-page { display: flex; flex-direction: column; flex: 1; min-height: 0; }
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }
.header-right { display: flex; align-items: center; gap: 12px; }
.week-label { font-size: 14px; color: #606266; }

/* 课表网格 */
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

/* 时间格子 */
.timetable-slot { position: relative; cursor: pointer; transition: background 0.15s; }
.timetable-slot:hover { background: #f5f7fa; }
.timetable-slot.is-past { background: #fafafa; opacity: 0.6; }

/* 加号按钮 */
.add-btn { opacity: 0; transition: opacity 0.15s; color: #c0c4cc; cursor: pointer; width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.add-btn:hover { background: #ecf5ff; color: #409eff; }
.timetable-slot:hover .add-btn { opacity: 1; }

/* 课程块 */
.event-block { width: 100%; padding: 6px 8px; border-radius: 6px; color: #fff; font-size: 12px; text-align: left; cursor: grab; transition: transform 0.1s, opacity 0.15s, box-shadow 0.15s; }
.event-block:hover { transform: scale(1.02); box-shadow: 0 2px 8px rgba(0,0,0,0.15); }
.event-block.dragging { opacity: 0.4; cursor: grabbing; }
.event-block.event-past { opacity: 0.5; cursor: not-allowed; }
.event-block.event-past:hover { transform: none; box-shadow: none; }
.event-block.event-requested { border: 2px dashed #f59e0b; }
.event-block .event-title { font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.event-block .event-badge { font-size: 10px; background: rgba(255,255,255,0.3); border-radius: 4px; padding: 1px 4px; margin-top: 1px; }
.event-block .event-location { font-size: 10px; opacity: 0.85; margin-top: 2px; }
.timetable-slot.has-event { padding: 4px; }
.timetable-slot.has-event:hover { background: transparent; }

/* 拖拽放置目标高亮 */
.timetable-slot.drag-over { background: #ecf5ff !important; outline: 2px dashed #409eff; outline-offset: -2px; }

/* 弹窗中的课程选项 */
.option-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 6px; vertical-align: middle; }
.checkin-item { display: flex; align-items: center; padding: 4px 0; font-size: 13px; }
.checkin-actions { margin-left: auto; display: flex; gap: 4px; }
</style>
