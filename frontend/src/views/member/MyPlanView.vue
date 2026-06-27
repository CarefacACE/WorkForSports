<template>
  <div class="my-plan-page">

    <!-- 空状态 -->
    <div v-if="plans.length === 0 && !plansLoading" class="empty-state">
      <el-empty description="暂无训练计划，由AI助手生成一份计划开始你的训练之旅吧" :image-size="120" />
    </div>

    <!-- 计划模块 -->
    <div v-else v-loading="plansLoading" class="plans-container">
      <!-- 训练计划下拉选择 -->
      <el-card shadow="never" class="plan-selector-card">
        <div class="plan-selector-row">
          <span class="plan-selector-label">训练计划</span>
          <el-select
            v-model="selectedPlanId"
            placeholder="请选择训练计划"
            class="plan-selector"
            @change="onPlanChange"
          >
            <el-option
              v-for="plan in plans"
              :key="plan.id"
              :label="plan.goal"
              :value="plan.id"
            >
              <span>{{ plan.goal }}</span>
              <el-tag
                :type="plan.status === 'ACTIVE' ? 'success' : plan.status === 'COMPLETED' ? '' : 'info'"
                size="small"
                effect="dark"
                style="margin-left: 8px"
              >{{ plan.status === 'ACTIVE' ? '进行中' : plan.status === 'COMPLETED' ? '已完成' : '已取消' }}</el-tag>
            </el-option>
          </el-select>
        </div>
      </el-card>

      <!-- 当前选中计划的头部信息 -->
      <el-card v-if="currentPlan" shadow="never" class="plan-header-card">
        <div class="plan-header-row">
          <div class="plan-header-left">
            <!-- 可双击改名的计划名称 -->
            <div class="plan-goal-wrapper" @dblclick="startRename">
              <h3 v-if="!renaming" class="plan-goal" title="双击修改名称">{{ currentPlan.goal }}</h3>
              <el-input
                v-else
                ref="renameInputRef"
                v-model="renameValue"
                size="default"
                class="plan-goal-input"
                @blur="confirmRename"
                @keyup.enter="confirmRename"
                @keyup.escape="cancelRename"
              />
            </div>
            <el-tag
              :type="currentPlan.status === 'ACTIVE' ? 'success' : currentPlan.status === 'COMPLETED' ? '' : 'info'"
              size="small"
              effect="dark"
            >{{ currentPlan.status === 'ACTIVE' ? '进行中' : currentPlan.status === 'COMPLETED' ? '已完成' : '已取消' }}</el-tag>
            <span class="plan-meta-text">
              📅 {{ currentPlan.durationDays }} 天
              <template v-if="currentPlan.startDate">{{ currentPlan.startDate }} ~ {{ currentPlan.endDate }}</template>
            </span>
          </div>
          <div class="plan-header-right">
            <el-progress :percentage="getProgressPercent(currentPlan.id)" :stroke-width="6" :color="progressColors" :width="80" type="circle" />
            <span class="progress-text">{{ getCheckedCount(currentPlan.id) }}/{{ getDetailCount(currentPlan.id) }}</span>
            <el-button text type="primary" size="small" @click="showPlanDetailDialog = true">详情</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(currentPlan.id)">删除</el-button>
          </div>
        </div>
        <p v-if="currentPlan.description" class="plan-desc">{{ currentPlan.description }}</p>
      </el-card>

      <!-- 训练日程模块 -->
      <el-card v-if="currentPlan" shadow="never" class="calendar-card">
        <template #header>
          <div class="calendar-header">
            <el-button text @click="prevMonth">‹</el-button>
            <span class="calendar-title">{{ calendarTitle }}</span>
            <el-button text @click="nextMonth">›</el-button>
            <el-switch v-model="showAllDetails[currentPlan.id]" active-text="全部" inactive-text="待打卡" size="small" style="margin-left:auto" />
          </div>
        </template>
        <div class="calendar-body">
          <!-- 星期标题 -->
          <div class="cal-weekdays">
            <span v-for="d in weekdays" :key="d" class="cal-weekday">{{ d }}</span>
          </div>
          <!-- 日期网格 -->
          <div class="cal-grid">
            <div
              v-for="day in calendarDays(currentPlan)"
              :key="day.dateStr"
              :class="[
                'cal-cell',
                { 'cal-cell-other': !day.inMonth, 'cal-cell-today': day.isToday, 'cal-cell-has': day.detail }
              ]"
              @click="day.detail && scrollToDetail(day.detail)"
            >
              <div class="cal-cell-date">{{ day.dateNum }}</div>
              <div v-if="day.detail" class="cal-cell-content" :class="{ 'is-checked': day.detail.isChecked, 'is-rest': day.detail.trainingType === '休息' }">
                <div class="cal-cell-tags">
                  <span class="cal-training-type">{{ day.detail.trainingType }}</span>
                  <span class="cal-intensity">{{ day.detail.intensity === 'HIGH' ? '🔥​' : day.detail.intensity === 'MEDIUM' ? '💪​' : '🌿​' }}</span>
                </div>
                <div class="cal-cell-duration">{{ day.detail.durationMinutes }}分钟</div>
                <el-button v-if="!day.detail.isChecked" text type="success" size="small" :loading="day.detail._loading" @click.stop="handleCheckIn(currentPlan.id, day.detail)">✅</el-button>
                <el-tag v-else type="success" size="small" effect="dark" class="cal-checked-tag">✓</el-tag>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 选中日期的训练详情弹窗 -->
      <el-dialog v-model="showDetailDialog" :title="`第 ${selectedDetail?.dayNumber} 天 — ${selectedDetail?.trainingType}`" width="500px" destroy-on-close>
        <div v-if="selectedDetail" class="detail-dialog-body">
          <div class="detail-dialog-meta">
            <el-tag :type="getIntensityType(selectedDetail.intensity)" size="small" effect="dark">{{ selectedDetail.trainingType }}</el-tag>
            <span :class="['intensity-tag', selectedDetail.intensity.toLowerCase()]">
              {{ selectedDetail.intensity === 'HIGH' ? '🔥高强度' : selectedDetail.intensity === 'MEDIUM' ? '💪中强度' : '🌿低强度' }}
            </span>
            <span class="detail-duration">⏱ {{ selectedDetail.durationMinutes }}分钟</span>
            <span v-if="selectedDetail.isChecked" class="checked-label">✓ {{ formatTime(selectedDetail.checkTime) }}</span>
          </div>
          <p class="detail-dialog-content">{{ selectedDetail.content }}</p>
        </div>
        <template #footer>
          <el-button v-if="selectedDetail && !selectedDetail.isChecked" type="success" :loading="selectedDetail._loading" @click="handleCheckInFromDialog(planIdForDialog!, selectedDetail!)">✅ 标记完成</el-button>
          <el-tag v-else type="success" size="large">✓ 已完成</el-tag>
        </template>
      </el-dialog>

      <!-- 计划完整详情弹窗 -->
      <el-dialog v-model="showPlanDetailDialog" width="620px" destroy-on-close>
        <template #header>
          <div class="plan-detail-dialog-header">
            <h3 class="plan-detail-dialog-title">{{ currentPlan?.goal }}</h3>
            <el-tag
              :type="currentPlan?.status === 'ACTIVE' ? 'success' : currentPlan?.status === 'COMPLETED' ? '' : 'info'"
              size="small"
              effect="dark"
            >{{ currentPlan?.status === 'ACTIVE' ? '进行中' : currentPlan?.status === 'COMPLETED' ? '已完成' : '已取消' }}</el-tag>
          </div>
        </template>
        <div v-if="currentPlan" class="plan-detail-dialog-body">
          <!-- 基本信息 -->
          <div class="plan-detail-section">
            <h4 class="plan-detail-section-title">📋 基本信息</h4>
            <div class="plan-detail-grid">
              <div class="plan-detail-item">
                <span class="plan-detail-label">训练目标</span>
                <span class="plan-detail-value">{{ currentPlan.goal }}</span>
              </div>
              <div class="plan-detail-item">
                <span class="plan-detail-label">状态</span>
                <el-tag
                  :type="currentPlan.status === 'ACTIVE' ? 'success' : currentPlan.status === 'COMPLETED' ? '' : 'info'"
                  size="small"
                  effect="dark"
                >{{ currentPlan.status === 'ACTIVE' ? '进行中' : currentPlan.status === 'COMPLETED' ? '已完成' : '已取消' }}</el-tag>
              </div>
              <div class="plan-detail-item">
                <span class="plan-detail-label">计划时长</span>
                <span class="plan-detail-value">{{ currentPlan.durationDays }} 天</span>
              </div>
              <div class="plan-detail-item">
                <span class="plan-detail-label">开始日期</span>
                <span class="plan-detail-value">{{ currentPlan.startDate || '-' }}</span>
              </div>
              <div class="plan-detail-item">
                <span class="plan-detail-label">结束日期</span>
                <span class="plan-detail-value">{{ currentPlan.endDate || '-' }}</span>
              </div>
              <div class="plan-detail-item">
                <span class="plan-detail-label">完成进度</span>
                <span class="plan-detail-value">{{ getCheckedCount(currentPlan.id) }} / {{ getDetailCount(currentPlan.id) }} ({{ getProgressPercent(currentPlan.id) }}%)</span>
              </div>
            </div>
          </div>

          <!-- 计划描述 -->
          <div v-if="currentPlan.description" class="plan-detail-section">
            <h4 class="plan-detail-section-title">📝 计划描述</h4>
            <p class="plan-detail-description">{{ currentPlan.description }}</p>
          </div>

          <!-- 每日训练明细 -->
          <div class="plan-detail-section">
            <h4 class="plan-detail-section-title">📅 每日训练明细</h4>
            <div class="plan-detail-day-list">
              <div
                v-for="d in (detailsMap[currentPlan.id] || [])"
                :key="d.id"
                class="plan-detail-day-item"
                :class="{ 'is-checked': d.isChecked }"
              >
                <span class="plan-detail-day-num">第{{ d.dayNumber }}天</span>
                <el-tag size="small" effect="dark" :type="d.trainingType === '休息' ? 'info' : ''">{{ d.trainingType }}</el-tag>
                <span class="plan-detail-day-intensity">
                  {{ d.intensity === 'HIGH' ? '🔥高' : d.intensity === 'MEDIUM' ? '💪中' : '🌿低' }}
                </span>
                <span class="plan-detail-day-time">⏱ {{ d.durationMinutes }}分钟</span>
                <span class="plan-detail-day-content">{{ d.content }}</span>
                <el-tag v-if="d.isChecked" type="success" size="small" effect="dark">✓</el-tag>
              </div>
            </div>
          </div>
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../stores/user'
import {
  getMyPlans, getPlanDetails, checkInPlanDetail, deletePlan, updatePlanGoal,
  type TrainingPlan, type PlanDetail
} from '../../api/plan'

const userStore = useUserStore()
const userId = userStore.user?.id

const plansLoading = ref(false)
const plans = ref<TrainingPlan[]>([])
const detailsMap = reactive<Record<number, (PlanDetail & { _loading?: boolean })[]>>({})
const showAllDetails = reactive<Record<number, boolean>>({})

// 当前选中的训练计划
const selectedPlanId = ref<number | null>(null)
const currentPlan = computed(() => plans.value.find(p => p.id === selectedPlanId.value) ?? null)

const progressColors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#409eff', percentage: 60 },
  { color: '#67c23a', percentage: 100 },
]

const weekdays = ['一', '二', '三', '四', '五', '六', '日']

// Calendar pagination state per plan
const calendarPage = reactive<Record<number, number>>({})

// Detail dialog
const showDetailDialog = ref(false)
const selectedDetail = ref<PlanDetail | null>(null)
const planIdForDialog = ref<number | null>(null)

// Plan full detail dialog
const showPlanDetailDialog = ref(false)

// Rename state
const renaming = ref(false)
const renameValue = ref('')
const renameInputRef = ref<InstanceType<typeof import('element-plus').ElInput> | null>(null)

async function startRename() {
  if (!currentPlan.value) return
  renameValue.value = currentPlan.value.goal
  renaming.value = true
  await nextTick()
  // focus the input
  const el = renameInputRef.value?.$el?.querySelector('input') as HTMLInputElement | null
  if (el) {
    el.focus()
    el.select()
  }
}

async function confirmRename() {
  if (!renaming.value) return
  const plan = currentPlan.value
  if (!plan || !userId) {
    renaming.value = false
    return
  }
  const newGoal = renameValue.value.trim()
  if (!newGoal) {
    ElMessage.warning('计划名称不能为空')
    renameValue.value = plan.goal
    renaming.value = false
    return
  }
  if (newGoal === plan.goal) {
    renaming.value = false
    return
  }
  try {
    const updated = await updatePlanGoal(plan.id, userId, newGoal)
    // 更新 plans 数组中的对应项，使下拉框同步刷新
    const idx = plans.value.findIndex(p => p.id === plan.id)
    if (idx !== -1) {
      plans.value[idx] = { ...plans.value[idx], goal: updated.goal }
    }
    ElMessage.success('计划名称已更新')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '修改失败')
    renameValue.value = plan.goal
  } finally {
    renaming.value = false
  }
}

function cancelRename() {
  renaming.value = false
}

function onPlanChange(planId: number) {
  // Ensure details are loaded and calendar page is initialized
  if (!detailsMap[planId]) {
    fetchPlanDetails(planId)
  }
  if (calendarPage[planId] === undefined) {
    calendarPage[planId] = 0
  }
}

function getCheckedCount(planId: number) {
  return (detailsMap[planId] || []).filter(d => d.isChecked === 1).length
}

function getDetailCount(planId: number) {
  return (detailsMap[planId] || []).length
}

function getProgressPercent(planId: number) {
  const total = getDetailCount(planId)
  return total === 0 ? 0 : Math.round(getCheckedCount(planId) / total * 100)
}

function getIntensityType(intensity: string) {
  if (intensity === 'HIGH') return 'danger'
  if (intensity === 'MEDIUM') return 'warning'
  return 'info'
}

function formatTime(time: string | null) {
  if (!time) return ''
  return time.slice(0, 16).replace('T', ' ')
}

function parseDate(s: string | null | undefined): Date {
  if (!s) return new Date()
  const d = new Date(s)
  return isNaN(d.getTime()) ? new Date() : d
}

function formatDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function getMonthStart(plan: TrainingPlan, page: number): Date {
  const start = parseDate(plan.startDate)
  const d = new Date(start.getFullYear(), start.getMonth() + page, 1)
  return d
}

function calendarTitleFor(plan: TrainingPlan, page: number): string {
  const ms = getMonthStart(plan, page)
  const months = ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月']
  return `${ms.getFullYear()}年 ${months[ms.getMonth()]}`
}

const calendarTitle = computed(() => {
  const plan = currentPlan.value
  if (!plan) return ''
  return calendarTitleFor(plan, calendarPage[plan.id] ?? 0)
})

interface CalendarDay {
  dateStr: string
  dateNum: number
  inMonth: boolean
  isToday: boolean
  detail: (PlanDetail & { _loading?: boolean }) | null
}

function calendarDays(plan: TrainingPlan): CalendarDay[] {
  const page = calendarPage[plan.id] ?? 0
  const ms = getMonthStart(plan, page)
  const year = ms.getFullYear()
  const month = ms.getMonth()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const firstDow = new Date(year, month, 1).getDay() // 0=Sun
  // Convert to Mon=0 ... Sun=6
  let startOffset = firstDow - 1
  if (startOffset < 0) startOffset = 6

  const details = detailsMap[plan.id] || []
  const detailByDate = new Map<string, PlanDetail & { _loading?: boolean }>()
  const planStart = parseDate(plan.startDate)
  for (const d of details) {
    const date = new Date(planStart)
    date.setDate(date.getDate() + (d.dayNumber - 1))
    detailByDate.set(formatDate(date), d)
  }

  const today = new Date()
  const todayStr = formatDate(today)
  const result: CalendarDay[] = []

  // Previous month trailing days
  const prevMonthDays = new Date(year, month, 0).getDate()
  for (let i = startOffset - 1; i >= 0; i--) {
    const dateNum = prevMonthDays - i
    const dateStr = formatDate(new Date(year, month - 1, dateNum))
    result.push({ dateStr, dateNum, inMonth: false, isToday: dateStr === todayStr, detail: detailByDate.get(dateStr) || null })
  }

  // Current month days
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = formatDate(new Date(year, month, d))
    result.push({ dateStr, dateNum: d, inMonth: true, isToday: dateStr === todayStr, detail: detailByDate.get(dateStr) || null })
  }

  // Next month leading days to fill last week
  const remaining = 7 - (result.length % 7)
  if (remaining < 7) {
    for (let d = 1; d <= remaining; d++) {
      const dateStr = formatDate(new Date(year, month + 1, d))
      result.push({ dateStr, dateNum: d, inMonth: false, isToday: dateStr === todayStr, detail: detailByDate.get(dateStr) || null })
    }
  }

  return result
}

function prevMonth() {
  const plan = currentPlan.value
  if (!plan) return
  if (!(calendarPage[plan.id] ?? 0)) return
  calendarPage[plan.id] = (calendarPage[plan.id] ?? 0) - 1
}

function nextMonth() {
  const plan = currentPlan.value
  if (!plan) return
  const maxPages = 3
  if ((calendarPage[plan.id] ?? 0) >= maxPages) return
  calendarPage[plan.id] = (calendarPage[plan.id] ?? 0) + 1
}

function scrollToDetail(detail: PlanDetail & { _loading?: boolean }) {
  selectedDetail.value = detail
  planIdForDialog.value = currentPlan.value?.id ?? null
  showDetailDialog.value = true
}

async function handleCheckInFromDialog(planId: number, detail: PlanDetail & { _loading?: boolean }) {
  detail._loading = true
  try {
    const updated = await checkInPlanDetail(planId, detail.id)
    detail.isChecked = updated.isChecked
    detail.checkTime = updated.checkTime
    ElMessage.success('打卡成功！💪')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '打卡失败')
  } finally {
    detail._loading = false
  }
}

async function handleCheckIn(planId: number, detail: PlanDetail & { _loading?: boolean }) {
  detail._loading = true
  try {
    const updated = await checkInPlanDetail(planId, detail.id)
    detail.isChecked = updated.isChecked
    detail.checkTime = updated.checkTime
    ElMessage.success('打卡成功！💪')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '打卡失败')
  } finally {
    detail._loading = false
  }
}

async function handleDelete(planId: number) {
  if (!userId) return
  try {
    await ElMessageBox.confirm('确定删除该训练计划吗？删除后不可恢复。', '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deletePlan(planId, userId)
    ElMessage.success('删除成功')
    // Reset selection
    selectedPlanId.value = null
    await fetchPlans()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

async function fetchPlanDetails(planId: number) {
  try {
    detailsMap[planId] = await getPlanDetails(planId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取计划详情失败')
  }
}

async function fetchPlans() {
  if (!userId) return
  plansLoading.value = true
  try {
    plans.value = await getMyPlans(userId)
    // Load all plan details
    for (const plan of plans.value) {
      detailsMap[plan.id] = await getPlanDetails(plan.id)
      if (calendarPage[plan.id] === undefined) calendarPage[plan.id] = 0
    }
    // Auto-select first plan if none selected
    if (!selectedPlanId.value && plans.value.length > 0) {
      selectedPlanId.value = plans.value[0].id
    }
    // If previously selected plan no longer exists, select first
    if (selectedPlanId.value && !plans.value.find(p => p.id === selectedPlanId.value)) {
      selectedPlanId.value = plans.value[0]?.id ?? null
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取计划失败')
  } finally {
    plansLoading.value = false
  }
}

onMounted(() => fetchPlans())
</script>

<style scoped>
/* ============================================
   1. PARENT CONTAINER — Fill available space
   ============================================ */
.my-plan-page {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0;
  /* Fill the height given by .main-content > router-view */
  flex: 1;
  min-height: 0;
}

.empty-state {
  padding: 24px 0;
}

.plans-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  min-height: 0;
}

/* ============================================
   2. MODULE 1 & 2 — Compact, content-fit height
   Override global .el-card flex:1 from admin-layout.css
   ============================================ */

/* Plan Selector card */
.plan-selector-card {
  border-left: 4px solid #409eff;
  flex: 0 0 auto !important;
  height: max-content;
}
.plan-selector-card :deep(.el-card__body) {
  padding: 6px 12px;
  flex: none !important;
  overflow: visible !important;
}
.plan-selector-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.plan-selector-label {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  flex-shrink: 0;
}
.plan-selector {
  min-width: 280px;
  flex: 1;
}

/* Plan Header card */
.plan-header-card {
  border-left: 4px solid #409eff;
  flex: 0 0 auto !important;
  height: max-content;
}
.plan-header-card :deep(.el-card__body) {
  padding: 6px 10px;
  flex: none !important;
  overflow: visible !important;
}
.plan-header-row { display: flex; align-items: center; justify-content: space-between; gap: 6px; }
.plan-header-left { display: flex; align-items: center; gap: 6px; min-width: 0; }
.plan-goal { margin: 0; font-size: 14px; font-weight: 700; color: #303133; flex-shrink: 0; }
.plan-goal-wrapper {
  flex-shrink: 0;
  cursor: pointer;
}
.plan-goal-wrapper .plan-goal:hover {
  color: #409eff;
  text-decoration: underline dashed;
}
.plan-goal-input {
  min-width: 160px;
  max-width: 300px;
}
.plan-meta-text { font-size: 11px; color: #909399; white-space: nowrap; }
.plan-header-right { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.plan-header-right .progress-text { font-size: 10px; color: #909399; white-space: nowrap; }
.plan-desc { margin: 1px 0 0 0; font-size: 10px; color: #909399; line-height: 1.3; }

/* ============================================
   3. MODULE 3 — Calendar, fills remaining space
   ============================================ */
.calendar-card {
  margin-left: 8px;
  flex: 1 !important;
  min-height: 0;
  display: flex !important;
  flex-direction: column !important;
}

.calendar-card :deep(.el-card__body) {
  padding: 4px 6px;
  flex: 1 !important;
  min-height: 0;
  overflow: hidden !important;
  display: flex;
  flex-direction: column;
}

.calendar-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.calendar-header {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
  font-size: 13px;
  flex-shrink: 0;
}

.calendar-title {
  min-width: 100px;
  text-align: center;
}

.cal-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-size: 11px;
  color: #909399;
  padding: 2px 0;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 2px;
  flex-shrink: 0;
}

.cal-weekday:last-child,
.cal-weekday:nth-child(6) {
  color: #e6a23c;
}

.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  grid-auto-rows: 1fr;
  gap: 2px;
  flex: 1;
  min-height: 0;
}

.cal-cell {
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 2px;
  display: flex;
  flex-direction: column;
  cursor: default;
  overflow: hidden;
  min-height: 0;
}

.cal-cell-other {
  opacity: 0.3;
}

.cal-cell-today {
  border-color: #409eff;
  background: #ecf5ff;
}

.cal-cell:hover {
  background: #f5f7fa;
}

.cal-cell-date {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  line-height: 1;
  flex-shrink: 0;
}

.cal-cell-today .cal-cell-date {
  color: #409eff;
}

.cal-cell-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: 1px 0;
  cursor: pointer;
  min-height: 0;
  justify-content: center;
}

.cal-cell-content.is-checked {
  opacity: 0.6;
}

.cal-cell-content.is-rest {
  background: #fdf6ec;
  border-radius: 2px;
}

.cal-cell-tags {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: nowrap;
}

.cal-training-type {
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
  background: #ecf5ff;
  padding: 2px 8px;
  border-radius: 4px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.cal-cell-content.is-rest .cal-training-type {
  background: #faecd8;
  color: #b38a4a;
}

.cal-cell-content.is-checked .cal-training-type {
  background: #e1f3d8;
  color: #67c23a;
}

.cal-intensity {
  font-size: 14px;
  flex-shrink: 0;
}

.cal-cell-duration {
  font-size: 11px;
  color: #a8abb2;
  line-height: 1;
}

.cal-cell-content .el-button {
  padding: 0;
  font-size: 11px;
  min-height: 14px;
  height: 16px;
  margin-top: auto;
}

.cal-checked-tag {
  font-size: 9px;
  padding: 0 4px;
  height: 16px;
  line-height: 16px;
  margin-top: auto;
}

/* ---- Detail Dialog ---- */
.detail-dialog-body { padding: 8px 0; }
.detail-dialog-meta { display: flex; align-items: center; gap: 6px; margin-bottom: 12px; flex-wrap: wrap; }
.detail-dialog-meta .intensity-tag { font-size: 12px; color: #909399; }
.detail-dialog-meta .detail-duration { font-size: 12px; color: #909399; }
.detail-dialog-meta .checked-label { font-size: 12px; color: #67c23a; }
.detail-dialog-content { font-size: 14px; color: #303133; line-height: 1.8; white-space: pre-wrap; }

/* ---- Plan Detail Dialog ---- */
.plan-detail-dialog-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.plan-detail-dialog-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}
.plan-detail-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.plan-detail-section-title {
  margin: 0 0 10px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.plan-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
.plan-detail-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.plan-detail-label {
  font-size: 12px;
  color: #909399;
}
.plan-detail-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}
.plan-detail-description {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
}
.plan-detail-day-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 360px;
  overflow-y: auto;
  overflow-x: auto;
}
.plan-detail-day-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  background: #f5f7fa;
  font-size: 13px;
  min-width: max-content;
  flex-wrap: nowrap;
}
.plan-detail-day-item.is-checked {
  opacity: 0.55;
  background: #e1f3d8;
}
.plan-detail-day-num {
  font-weight: 600;
  color: #409eff;
  min-width: 56px;
  flex-shrink: 0;
}
.plan-detail-day-item.is-checked .plan-detail-day-num {
  color: #67c23a;
}
.plan-detail-day-intensity {
  font-size: 13px;
  flex-shrink: 0;
}
.plan-detail-day-time {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}
.plan-detail-day-content {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

/* Dark theme for plan detail dialog */
:global([data-admin-theme="dark"]) .plan-detail-dialog-title { color: #e5eaf3; }
:global([data-admin-theme="dark"]) .plan-detail-section-title { color: #e5eaf3; }
:global([data-admin-theme="dark"]) .plan-detail-label { color: #6b7084; }
:global([data-admin-theme="dark"]) .plan-detail-value { color: #b0b4c0; }
:global([data-admin-theme="dark"]) .plan-detail-description { color: #b0b4c0; }
:global([data-admin-theme="dark"]) .plan-detail-day-item { background: #222536; }
:global([data-admin-theme="dark"]) .plan-detail-day-item.is-checked { background: #1e3028; }
:global([data-admin-theme="dark"]) .plan-detail-day-content { color: #b0b4c0; }

/* ---- Other ---- */
.plan-detail-card { margin-left: 8px; }

/* Dark theme */
:global([data-admin-theme="dark"]) .plan-goal { color: #e5eaf3; }
:global([data-admin-theme="dark"]) .plan-desc { color: #73767a; }
:global([data-admin-theme="dark"]) .plan-header-card { border-left-color: #409eff; }
:global([data-admin-theme="dark"]) .cal-cell { border-color: #3a3a4c; }
:global([data-admin-theme="dark"]) .cal-cell-today { border-color: #409eff; background: #1e2a4a; }
:global([data-admin-theme="dark"]) .cal-cell:hover { background: #2c2c3a; }
:global([data-admin-theme="dark"]) .cal-cell-today .cal-cell-date { color: #7ab8ff; }
:global([data-admin-theme="dark"]) .cal-cell-date { color: #a3a6ad; }
:global([data-admin-theme="dark"]) .cal-weekdays { border-bottom-color: #3a3a4c; }
:global([data-admin-theme="dark"]) .cal-cell-content.is-rest { background: #2a2518; }
:global([data-admin-theme="dark"]) .cal-cell-content.is-rest .cal-training-type { background: #4a3f20; color: #d4a84e; }
:global([data-admin-theme="dark"]) .cal-cell-content.is-checked { opacity: 0.5; }
:global([data-admin-theme="dark"]) .cal-cell-content.is-checked .cal-training-type { background: #1e3a2a; color: #67c23a; }
:global([data-admin-theme="dark"]) .cal-training-type { background: #1e2a4a; color: #7ab8ff; }
</style>
