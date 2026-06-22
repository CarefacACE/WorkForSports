<template>
  <div class="db-control-view">
    <el-row :gutter="16">
      <el-col :span="5">
        <el-card class="table-list-card">
          <template #header>
            <div class="card-header-sm">数据库表</div>
          </template>
          <div v-loading="tablesLoading" class="table-list">
            <div v-for="table in tables" :key="table" class="table-item"
              :class="{ active: table === selectedTable }" @click="selectTable(table)">
              <el-icon><Grid /></el-icon>
              <span>{{ table }}</span>
            </div>
            <el-empty v-if="!tablesLoading && tables.length === 0" description="暂无表" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <el-col :span="19">
        <el-card v-if="selectedTable">
          <template #header>
            <div class="card-header">
              <span>{{ selectedTable }}</span>
              <el-button-group>
                <el-button :type="activeTab === 'data' ? 'primary' : ''" @click="activeTab = 'data'">数据</el-button>
                <el-button :type="activeTab === 'structure' ? 'primary' : ''" @click="activeTab = 'structure'">结构</el-button>
              </el-button-group>
            </div>
          </template>

          <div v-if="activeTab === 'structure'">
            <el-table :data="tableStructure" border stripe v-loading="structureLoading">
              <el-table-column prop="COLUMN_NAME" label="字段名" width="160" />
              <el-table-column prop="COLUMN_TYPE" label="类型" width="160" />
              <el-table-column prop="IS_NULLABLE" label="允许空" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.IS_NULLABLE === 'YES' ? 'success' : 'danger'" size="small">
                    {{ row.IS_NULLABLE === 'YES' ? '是' : '否' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="COLUMN_KEY" label="键" width="80" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.COLUMN_KEY === 'PRI'" type="warning" size="small">主键</el-tag>
                  <el-tag v-else-if="row.COLUMN_KEY === 'UNI'" type="info" size="small">唯一</el-tag>
                  <el-tag v-else-if="row.COLUMN_KEY === 'MUL'" size="small">索引</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="COLUMN_DEFAULT" label="默认值" width="120" />
              <el-table-column prop="COLUMN_COMMENT" label="注释" min-width="160" />
            </el-table>
          </div>

          <div v-if="activeTab === 'data'">
            <div class="toolbar">
              <div class="toolbar-left">
                <el-input v-model="dataKeyword" placeholder="搜索..." clearable style="width: 240px"
                  @clear="fetchTableData" @keyup.enter="fetchTableData">
                  <template #append>
                    <el-button @click="fetchTableData">搜索</el-button>
                  </template>
                </el-input>
              </div>
              <div class="toolbar-right">
                <el-button type="primary" @click="openAddDialog">新增</el-button>
                <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDeleteData">
                  批量删除 ({{ selectedRows.length }})
                </el-button>
              </div>
            </div>

            <el-table :data="tableData" v-loading="dataLoading" border stripe max-height="500"
              @selection-change="handleDataSelectionChange">
              <el-table-column type="selection" width="50" align="center" fixed />
              <el-table-column v-for="col in tableStructure" :key="col.COLUMN_NAME"
                :prop="col.COLUMN_NAME" :label="col.COLUMN_NAME" :min-width="getColumnWidth(col)"
                show-overflow-tooltip>
                <template #default="{ row }">
                  {{ formatCellValue(row[col.COLUMN_NAME]) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
                  <el-popconfirm title="确定删除该记录？" @confirm="handleDeleteData(row)">
                    <template #reference>
                      <el-button type="danger" link>删除</el-button>
                    </template>
                  </el-popconfirm>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-wrapper">
              <el-pagination v-model:current-page="dataPageNum" v-model:page-size="dataPageSize"
                :page-sizes="[10, 20, 50, 100]" :total="dataTotal"
                layout="total, sizes, prev, pager, next, jumper" @size-change="handleDataSizeChange"
                @current-change="handleDataPageChange" />
            </div>
          </div>
        </el-card>
        <el-card v-else>
          <el-empty description="请从左侧选择一个表" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="formDialogVisible" :title="isEditMode ? '编辑记录' : '新增记录'" width="600px">
      <el-form :model="formData" label-position="top">
        <el-row :gutter="16">
          <el-col v-for="col in editableColumns" :key="col.COLUMN_NAME" :span="12">
            <el-form-item :label="col.COLUMN_NAME + (col.COLUMN_COMMENT ? ' (' + col.COLUMN_COMMENT + ')' : '')">
              <el-input v-model="formData[col.COLUMN_NAME]" :placeholder="col.COLUMN_TYPE"
                :disabled="col.COLUMN_KEY === 'PRI' && isEditMode" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="handleSubmitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Grid } from '@element-plus/icons-vue';
import request from '../utils/request';

interface ColumnInfo {
  COLUMN_NAME: string;
  COLUMN_TYPE: string;
  IS_NULLABLE: string;
  COLUMN_KEY: string;
  COLUMN_DEFAULT: string | null;
  COLUMN_COMMENT: string;
}

const tables = ref<string[]>([]);
const tablesLoading = ref(false);
const selectedTable = ref('');
const activeTab = ref('data');
const tableStructure = ref<ColumnInfo[]>([]);
const structureLoading = ref(false);

const tableData = ref<any[]>([]);
const dataLoading = ref(false);
const dataKeyword = ref('');
const dataPageNum = ref(1);
const dataPageSize = ref(20);
const dataTotal = ref(0);
const selectedRows = ref<any[]>([]);

const formDialogVisible = ref(false);
const formLoading = ref(false);
const isEditMode = ref(false);
const editRow = ref<any>(null);
const formData = reactive<Record<string, any>>({});

const pkColumn = computed(() => {
  const pk = tableStructure.value.find(c => c.COLUMN_KEY === 'PRI');
  return pk ? pk.COLUMN_NAME : 'id';
});

const editableColumns = computed(() => {
  return tableStructure.value.filter(c => {
    if (c.COLUMN_NAME === 'deleted') return false;
    if (c.COLUMN_NAME === 'create_time' || c.COLUMN_NAME === 'update_time') return false;
    return true;
  });
});

function getColumnWidth(col: ColumnInfo): number {
  if (col.COLUMN_KEY === 'PRI') return 80;
  if (col.COLUMN_TYPE.includes('datetime')) return 170;
  if (col.COLUMN_TYPE.includes('text') || col.COLUMN_TYPE.includes('longtext')) return 200;
  if (col.COLUMN_TYPE.includes('varchar')) return 150;
  return 120;
}

function formatCellValue(value: any): string {
  if (value === null || value === undefined) return '';
  return String(value);
}

async function fetchTables() {
  tablesLoading.value = true;
  try {
    const res = await request.get<string[]>('/db/tables');
    tables.value = res;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取表列表失败');
  } finally {
    tablesLoading.value = false;
  }
}

async function fetchTableStructure(tableName: string) {
  structureLoading.value = true;
  try {
    const res = await request.get<ColumnInfo[]>(`/db/table/${tableName}`);
    tableStructure.value = res;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取表结构失败');
  } finally {
    structureLoading.value = false;
  }
}

async function fetchTableData() {
  if (!selectedTable.value) return;
  dataLoading.value = true;
  try {
    const res = await request.get<any>(`/db/table/${selectedTable.value}/data`, {
      pageNum: dataPageNum.value,
      pageSize: dataPageSize.value,
      keyword: dataKeyword.value || undefined,
    });
    tableData.value = res.records;
    dataTotal.value = res.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取数据失败');
  } finally {
    dataLoading.value = false;
  }
}

async function selectTable(tableName: string) {
  selectedTable.value = tableName;
  activeTab.value = 'data';
  dataPageNum.value = 1;
  dataKeyword.value = '';
  selectedRows.value = [];
  await fetchTableStructure(tableName);
  fetchTableData();
}

function handleDataSizeChange() {
  dataPageNum.value = 1;
  fetchTableData();
}

function handleDataPageChange() {
  fetchTableData();
}

function handleDataSelectionChange(rows: any[]) {
  selectedRows.value = rows;
}

function openAddDialog() {
  isEditMode.value = false;
  editRow.value = null;
  Object.keys(formData).forEach(key => delete formData[key]);
  tableStructure.value.forEach(col => {
    if (col.COLUMN_DEFAULT !== null && col.COLUMN_DEFAULT !== undefined) {
      formData[col.COLUMN_NAME] = col.COLUMN_DEFAULT;
    } else {
      formData[col.COLUMN_NAME] = '';
    }
  });
  formDialogVisible.value = true;
}

function openEditDialog(row: any) {
  isEditMode.value = true;
  editRow.value = row;
  Object.keys(formData).forEach(key => delete formData[key]);
  tableStructure.value.forEach(col => {
    formData[col.COLUMN_NAME] = row[col.COLUMN_NAME] !== null && row[col.COLUMN_NAME] !== undefined
      ? String(row[col.COLUMN_NAME]) : '';
  });
  formDialogVisible.value = true;
}

async function handleSubmitForm() {
  formLoading.value = true;
  try {
    const submitData: Record<string, any> = {};
    Object.keys(formData).forEach(key => {
      const val = formData[key];
      if (val !== '' && val !== null && val !== undefined) {
        submitData[key] = val;
      }
    });

    if (isEditMode.value && editRow.value) {
      const pkVal = editRow.value[pkColumn.value];
      submitData._pkColumn = pkColumn.value;
      submitData._pkValue = pkVal;
      await request.put(`/db/table/${selectedTable.value}/data`, submitData);
      ElMessage.success('更新成功');
    } else {
      await request.post(`/db/table/${selectedTable.value}/data`, submitData);
      ElMessage.success('新增成功');
    }
    formDialogVisible.value = false;
    fetchTableData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败');
  } finally {
    formLoading.value = false;
  }
}

async function handleDeleteData(row: any) {
  try {
    const pkVal = row[pkColumn.value];
    await request.delete(`/db/table/${selectedTable.value}/data/${pkColumn.value}/${pkVal}`);
    ElMessage.success('删除成功');
    fetchTableData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败');
  }
}

async function handleBatchDeleteData() {
  try {
    const pkValues = selectedRows.value.map(row => row[pkColumn.value]);
    await request.delete(`/db/table/${selectedTable.value}/data/batch`, {
      pkColumn: pkColumn.value,
      pkValues: pkValues,
    });
    ElMessage.success('批量删除成功');
    selectedRows.value = [];
    fetchTableData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '批量删除失败');
  }
}

fetchTables();
</script>

<style scoped>
.db-control-view {
  padding: 0;
}

.table-list-card {
  height: calc(100vh - 160px);
}

.table-list-card :deep(.el-card__body) {
  padding: 0;
  height: calc(100% - 55px);
  overflow-y: auto;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.card-header-sm {
  font-weight: 600;
  font-size: 14px;
}

.table-list {
  padding: 4px 0;
}

.table-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  font-size: 13px;
  color: #1d1d1f;
  transition: all 0.2s;
}

.table-item:hover {
  background: #f5f5f7;
}

.table-item.active {
  background: #e8f0fe;
  color: #0071e3;
  font-weight: 500;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
