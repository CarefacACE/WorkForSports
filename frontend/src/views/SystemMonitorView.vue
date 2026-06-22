<template>
  <div class="system-monitor-view">
    <div class="monitor-header">
      <div class="monitor-actions">
        <el-button type="primary" :loading="loading" @click="loadSystemInfo">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
        <el-checkbox v-model="autoRefresh" @change="toggleAutoRefresh">自动刷新</el-checkbox>
      </div>
      <span class="last-update" v-if="lastUpdate">最后更新: {{ lastUpdate }}</span>
    </div>

    <div class="monitor-grid" v-loading="loading">
      <el-card v-if="data.os" class="monitor-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="card-icon blue"><el-icon size="20"><Monitor /></el-icon></div>
            <div>
              <div class="card-title">操作系统</div>
              <div class="card-subtitle">System Information</div>
            </div>
          </div>
        </template>
        <div class="info-row"><span class="info-label">系统名称</span><span class="info-value">{{ data.os.name || 'N/A' }}</span></div>
        <div class="info-row"><span class="info-label">系统版本</span><span class="info-value">{{ data.os.version || 'N/A' }}</span></div>
        <div class="info-row"><span class="info-label">系统架构</span><span class="info-value">{{ data.os.arch || 'N/A' }}</span></div>
        <div class="info-row"><span class="info-label">运行时间</span><span class="info-value">{{ data.os.uptime || 'N/A' }}</span></div>
      </el-card>

      <el-card v-if="data.cpu" class="monitor-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="card-icon" :class="cpuColor"><el-icon size="20"><Cpu /></el-icon></div>
            <div>
              <div class="card-title">CPU信息</div>
              <div class="card-subtitle">Central Processing Unit</div>
            </div>
          </div>
        </template>
        <div class="info-row"><span class="info-label">处理器型号</span><span class="info-value text-ellipsis">{{ data.cpu.name || 'N/A' }}</span></div>
        <div class="info-row"><span class="info-label">物理核心数</span><span class="info-value">{{ data.cpu.cores || 'N/A' }}</span></div>
        <div class="info-row"><span class="info-label">逻辑核心数</span><span class="info-value">{{ data.cpu.logicalCores || 'N/A' }}</span></div>
        <div class="info-row"><span class="info-label">CPU频率</span><span class="info-value">{{ data.cpu.frequency || 'N/A' }}</span></div>
        <div class="info-row"><span class="info-label">使用率</span><span class="usage-text" :class="cpuColor">{{ data.cpu.load }}%</span></div>
        <el-progress :percentage="data.cpu.load" :color="progressColor(data.cpu.load)" :stroke-width="8" />
      </el-card>

      <el-card v-if="data.memory" class="monitor-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="card-icon" :class="memColor"><el-icon size="20"><Coin /></el-icon></div>
            <div>
              <div class="card-title">内存信息</div>
              <div class="card-subtitle">Memory Usage</div>
            </div>
          </div>
        </template>
        <div class="info-row"><span class="info-label">总内存</span><span class="info-value">{{ data.memory.total || 'N/A' }}</span></div>
        <div class="info-row"><span class="info-label">已使用</span><span class="info-value">{{ data.memory.used || 'N/A' }}</span></div>
        <div class="info-row"><span class="info-label">可用</span><span class="info-value">{{ data.memory.available || 'N/A' }}</span></div>
        <div class="info-row"><span class="info-label">使用率</span><span class="usage-text" :class="memColor">{{ data.memory.usage }}%</span></div>
        <el-progress :percentage="data.memory.usage" :color="progressColor(data.memory.usage)" :stroke-width="8" />
      </el-card>

      <el-card v-if="data.jvm" class="monitor-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="card-icon purple"><el-icon size="20"><Box /></el-icon></div>
            <div>
              <div class="card-title">JVM信息</div>
              <div class="card-subtitle">Java Virtual Machine</div>
            </div>
          </div>
        </template>
        <div class="info-row"><span class="info-label">JVM名称</span><span class="info-value">{{ data.jvm.name || 'N/A' }}</span></div>
        <div class="info-row"><span class="info-label">Java版本</span><span class="info-value">{{ data.jvm.version || 'N/A' }}</span></div>
        <div class="info-row"><span class="info-label">总内存</span><span class="info-value">{{ data.jvm.totalMemory || 'N/A' }}</span></div>
        <div class="info-row"><span class="info-label">已使用</span><span class="info-value">{{ data.jvm.usedMemory || 'N/A' }}</span></div>
        <div class="info-row"><span class="info-label">最大内存</span><span class="info-value">{{ data.jvm.maxMemory || 'N/A' }}</span></div>
        <div class="info-row"><span class="info-label">使用率</span><span class="usage-text" :class="jvmColor">{{ data.jvm.usage }}%</span></div>
        <el-progress :percentage="data.jvm.usage" :color="progressColor(data.jvm.usage)" :stroke-width="8" />
      </el-card>

      <el-card v-if="data.app" class="monitor-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="card-icon green"><el-icon size="20"><Platform /></el-icon></div>
            <div>
              <div class="card-title">项目信息</div>
              <div class="card-subtitle">Application Info</div>
            </div>
          </div>
        </template>
        <div class="info-row"><span class="info-label">项目名称</span><span class="info-value">{{ data.app.name || 'N/A' }}</span></div>
        <div class="info-row"><span class="info-label">项目版本</span><span class="info-value">{{ data.app.version || 'N/A' }}</span></div>
        <div class="info-row"><span class="info-label">启动时间</span><span class="info-value">{{ data.app.startupTime || 'N/A' }}</span></div>
        <div class="info-row"><span class="info-label">运行时长</span><span class="info-value running-time">{{ data.app.runTime || 'N/A' }}</span></div>
      </el-card>

      <el-card v-if="data.disk && data.disk.length > 0" class="monitor-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="card-icon orange"><el-icon size="20"><Files /></el-icon></div>
            <div>
              <div class="card-title">磁盘信息</div>
              <div class="card-subtitle">Disk Usage</div>
            </div>
          </div>
        </template>
        <div v-for="disk in data.disk" :key="disk.mount" class="disk-item">
          <div class="disk-header">
            <span class="disk-name">{{ disk.name || disk.mount || 'Local Disk' }}</span>
            <span class="disk-type">{{ disk.type }}</span>
          </div>
          <div class="info-row"><span class="info-label">总容量</span><span class="info-value">{{ disk.total }}</span></div>
          <div class="info-row"><span class="info-label">已使用</span><span class="info-value">{{ disk.used }}</span></div>
          <div class="info-row"><span class="info-label">可用</span><span class="info-value">{{ disk.free }}</span></div>
          <el-progress :percentage="disk.usage" :color="progressColor(disk.usage)" :stroke-width="6" style="margin-top: 8px" />
        </div>
      </el-card>

      <el-card v-if="data.network && data.network.length > 0" class="monitor-card wide" shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="card-icon green"><el-icon size="20"><Connection /></el-icon></div>
            <div>
              <div class="card-title">网络信息</div>
              <div class="card-subtitle">Network Interfaces</div>
            </div>
          </div>
        </template>
        <div v-for="net in data.network" :key="net.name" class="network-item">
          <div class="info-row"><span class="info-label">名称</span><span class="info-value">{{ net.name }}</span></div>
          <div class="info-row"><span class="info-label">MAC地址</span><span class="info-value">{{ net.mac || 'N/A' }}</span></div>
          <div class="info-row"><span class="info-label">IPv4</span><span class="info-value">{{ net.ipv4 || 'N/A' }}</span></div>
          <div class="info-row"><span class="info-label">速度</span><span class="info-value">{{ net.speed || 'N/A' }}</span></div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh, Monitor, Cpu, Coin, Box, Platform, Files, Connection } from '@element-plus/icons-vue';
import request from '../utils/request';

const loading = ref(false);
const autoRefresh = ref(false);
const lastUpdate = ref('');
let autoRefreshTimer: ReturnType<typeof setInterval> | null = null;

const data = reactive<Record<string, any>>({});

const cpuColor = computed(() => {
  const load = data.cpu?.load || 0;
  if (load > 80) return 'red';
  if (load > 60) return 'orange';
  return 'blue';
});

const memColor = computed(() => {
  const usage = data.memory?.usage || 0;
  if (usage > 80) return 'red';
  if (usage > 60) return 'orange';
  return 'green';
});

const jvmColor = computed(() => {
  const usage = data.jvm?.usage || 0;
  if (usage > 80) return 'red';
  if (usage > 60) return 'orange';
  return 'green';
});

function progressColor(percentage: number) {
  if (percentage > 80) return '#ff3b30';
  if (percentage > 60) return '#ff9500';
  return '#34c759';
}

async function loadSystemInfo() {
  loading.value = true;
  try {
    const res = await request.get<any>('/systemMonitor/info');
    Object.assign(data, res);
    lastUpdate.value = new Date().toLocaleTimeString();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取系统信息失败');
  } finally {
    loading.value = false;
  }
}

function toggleAutoRefresh() {
  if (autoRefresh.value) {
    autoRefreshTimer = setInterval(loadSystemInfo, 5000);
  } else {
    if (autoRefreshTimer) {
      clearInterval(autoRefreshTimer);
      autoRefreshTimer = null;
    }
  }
}

onMounted(() => {
  loadSystemInfo();
});

onBeforeUnmount(() => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer);
  }
});
</script>

<style scoped>
.system-monitor-view {
  padding: 0;
}

.monitor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.monitor-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.last-update {
  font-size: 13px;
  color: #86868b;
}

.monitor-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: 16px;
}

.monitor-card.wide {
  grid-column: 1 / -1;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.card-icon.blue { background: linear-gradient(135deg, #0071e3, #5856d6); }
.card-icon.green { background: linear-gradient(135deg, #34c759, #30d158); }
.card-icon.orange { background: linear-gradient(135deg, #ff9500, #ff6b00); }
.card-icon.purple { background: linear-gradient(135deg, #af52de, #5856d6); }
.card-icon.red { background: linear-gradient(135deg, #ff3b30, #ff6b6b); }

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
}

.card-subtitle {
  font-size: 12px;
  color: #86868b;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 13px;
  color: #86868b;
}

.info-value {
  font-size: 13px;
  font-weight: 500;
  color: #1d1d1f;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-ellipsis {
  max-width: 180px;
}

.usage-text {
  font-size: 20px;
  font-weight: 600;
}

.usage-text.blue { color: #0071e3; }
.usage-text.green { color: #34c759; }
.usage-text.orange { color: #ff9500; }
.usage-text.red { color: #ff3b30; }

.running-time {
  color: #34c759 !important;
  font-weight: 600 !important;
}

.disk-item {
  background: #f5f5f7;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 10px;
}

.disk-item:last-child {
  margin-bottom: 0;
}

.disk-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.disk-name {
  font-weight: 500;
  color: #1d1d1f;
  font-size: 13px;
}

.disk-type {
  font-size: 12px;
  color: #86868b;
}

.network-item {
  background: #f5f5f7;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 10px;
}

.network-item:last-child {
  margin-bottom: 0;
}
</style>
