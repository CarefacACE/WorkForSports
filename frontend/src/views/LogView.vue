<template>
  <div class="log-manage-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>日志管理</span>
        </div>
      </template>

      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="keyword" placeholder="搜索用户名/操作/IP" clearable style="width: 200px"
            @clear="handleSearch" @keyup.enter="handleSearch">
            <template #append>
              <el-button @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
          <el-select v-model="filterRole" placeholder="角色筛选" clearable style="width: 120px; margin-left: 8px"
            @change="handleSearch">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="教练" value="COACH" />
            <el-option label="会员" value="MEMBER" />
          </el-select>
          <el-select v-model="filterOperation" placeholder="操作类型" clearable style="width: 120px; margin-left: 8px"
            @change="handleSearch">
            <el-option label="登录" value="登录" />
            <el-option label="注册" value="注册" />
            <el-option label="新增" value="新增" />
            <el-option label="编辑" value="编辑" />
            <el-option label="删除" value="删除" />
            <el-option label="查询" value="查询" />
          </el-select>
          <el-date-picker v-model="filterTimeRange" type="daterange" range-separator="至" start-placeholder="开始日期"
            end-placeholder="结束日期" value-format="YYYY-MM-DD" style="margin-left: 8px" @change="handleSearch" />
        </div>
        <div class="toolbar-right">
          <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
            批量删除 ({{ selectedIds.length }})
          </el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="username" label="用户名" width="100" />
        <el-table-column prop="role" label="角色" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operation" label="操作" width="120" />
        <el-table-column prop="method" label="请求方法" min-width="180" show-overflow-tooltip />
        <el-table-column prop="params" label="参数" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP地址" width="130" />
        <el-table-column prop="createTime" label="操作时间" width="170" align="center" />
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-popconfirm title="确定删除该日志？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]" :total="total"
          layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
          @current-change="handlePageChange" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getLogs, deleteLog, deleteLogs, type LogItem } from '../api/log';

const loading = ref(false);
const keyword = ref('');
const filterRole = ref('');
const filterOperation = ref('');
const filterTimeRange = ref<string[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableData = ref<LogItem[]>([]);
const selectedIds = ref<number[]>([]);

function roleLabel(role: string) {
  const map: Record<string, string> = { ADMIN: '管理员', COACH: '教练', MEMBER: '会员' };
  return map[role] || role;
}

function roleTagType(role: string) {
  const map: Record<string, string> = { ADMIN: 'danger', COACH: 'warning', MEMBER: 'success' };
  return map[role] || 'info';
}

async function fetchData() {
  loading.value = true;
  try {
    const res = await getLogs({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      role: filterRole.value || undefined,
      operationType: filterOperation.value || undefined,
      startTime: filterTimeRange.value?.[0] || undefined,
      endTime: filterTimeRange.value?.[1] || undefined,
    });
    tableData.value = res.records;
    total.value = res.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取数据失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pageNum.value = 1;
  fetchData();
}

function handleSizeChange() {
  pageNum.value = 1;
  fetchData();
}

function handlePageChange() {
  fetchData();
}

function handleSelectionChange(rows: LogItem[]) {
  selectedIds.value = rows.map(r => r.id);
}

async function handleDelete(id: number) {
  try {
    await deleteLog(id);
    ElMessage.success('删除成功');
    fetchData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败');
  }
}

async function handleBatchDelete() {
  try {
    await deleteLogs(selectedIds.value);
    ElMessage.success('批量删除成功');
    selectedIds.value = [];
    fetchData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '批量删除失败');
  }
}

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.log-manage-view {
  padding: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
