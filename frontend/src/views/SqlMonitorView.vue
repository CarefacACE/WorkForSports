<template>
  <div class="sql-monitor-view">
    <el-card class="full-card">
      <template #header>
        <div class="card-header">
          <span>SQL监控</span>
          <el-button type="primary" @click="openInNewTab">新窗口打开</el-button>
        </div>
      </template>
      <div class="iframe-wrapper">
        <iframe :src="druidUrl" frameborder="0"></iframe>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
const druidUrl = '/api/druid/';

function openInNewTab() {
  window.open(druidUrl, '_blank');
}
</script>

<style scoped>
.sql-monitor-view {
  padding: 0;
  height: calc(100vh - 140px);
}

.full-card {
  height: 100%;
}

.full-card :deep(.el-card__body) {
  height: calc(100% - 60px);
  padding: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.iframe-wrapper {
  width: 100%;
  height: 100%;
}

.iframe-wrapper iframe {
  width: 100%;
  height: 100%;
  border: none;
}
</style>
