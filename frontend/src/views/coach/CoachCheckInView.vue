<template>
  <div class="checkin-page">
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value">{{ stats.totalRecords || 0 }}</div>
          <div class="stat-label">总排课数</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card signed">
          <div class="stat-value">{{ stats.signedCount || 0 }}</div>
          <div class="stat-label">已签到</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card absent">
          <div class="stat-value">{{ stats.absentCount || 0 }}</div>
          <div class="stat-label">缺勤</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value">{{ stats.checkInRate || 0 }}<span class="stat-unit">%</span></div>
          <div class="stat-label">签到率</div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <span class="section-title">签到记录</span>
      </template>
      <el-table :data="history" v-loading="loading" stripe>
        <el-table-column label="课程" prop="scheduleTitle" min-width="150" show-overflow-tooltip />
        <el-table-column label="签到时间" width="180" align="center">
          <template #default="{ row }">
            {{ row.checkInTime ? row.checkInTime.replace('T', ' ').slice(0, 16) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCheckInStats, getCheckInHistory, type CheckInStats, type CheckInRecord } from '../../api/checkin'

const userStore = JSON.parse(localStorage.getItem('user_info') || '{}')
const userId = userStore.id
const role = 'COACH'

const loading = ref(false)
const stats = ref<CheckInStats>({ totalRecords: 0, signedCount: 0, absentCount: 0, pendingCount: 0, checkInRate: 0 })
const history = ref<(CheckInRecord & { scheduleTitle?: string })[]>([])

const statusType = (status: string) => {
  if (status === 'SIGNED') return 'success'
  if (status === 'ABSENT') return 'danger'
  return 'info'
}

const statusLabel = (status: string) => {
  if (status === 'SIGNED') return '已签到'
  if (status === 'ABSENT') return '缺勤'
  return '待签到'
}

const fetchData = async () => {
  loading.value = true
  try {
    const [statsRes, historyRes] = await Promise.all([
      getCheckInStats(userId, role),
      getCheckInHistory(userId, role)
    ])
    stats.value = statsRes
    history.value = historyRes
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.checkin-page { display: flex; flex-direction: column; flex: 1; min-height: 0; }
.stats-row { margin-bottom: 16px; }
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.stat-card.signed { border-left: 4px solid #67c23a; }
.stat-card.absent { border-left: 4px solid #f56c6c; }
.stat-value { font-size: 28px; font-weight: 700; color: #303133; }
.stat-unit { font-size: 14px; font-weight: 400; color: #909399; margin-left: 2px; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; }
.section-title { font-weight: 600; }
</style>
