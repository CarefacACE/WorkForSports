<template>
  <div class="user-manage-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>教练工资管理</span>
        </div>
      </template>

      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="keyword" placeholder="搜索用户名/姓名/手机号" clearable style="width: 280px"
            @clear="handleSearch" @keyup.enter="handleSearch">
            <template #append>
              <el-button @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column label="余额" width="120" align="center">
          <template #default="{ row }">
            <span class="balance-text">¥ {{ (row.balance ?? 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEditBalance(row)">修改余额</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="修改余额" width="420px">
      <el-form label-position="top">
        <el-form-item label="教练">
          <el-input :model-value="editForm.username + '（' + editForm.realName + '）'" disabled />
        </el-form-item>
        <el-form-item label="当前余额">
          <el-input :model-value="'¥ ' + (editForm.oldBalance ?? 0).toFixed(2)" disabled />
        </el-form-item>
        <el-form-item label="新余额">
          <el-input-number v-model="editForm.newBalance" :min="0" :precision="2" :step="100" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmitBalance">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getUsers, type UserItem } from '../api/user';
import { updateBalance } from '../api/finance';

const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const keyword = ref('');
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableData = ref<UserItem[]>([]);

const editForm = reactive({
  userId: 0,
  username: '',
  realName: '',
  oldBalance: 0,
  newBalance: 0,
});

async function fetchData() {
  loading.value = true;
  try {
    const res = await getUsers({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      role: 'COACH',
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

function handleEditBalance(row: UserItem) {
  editForm.userId = row.id;
  editForm.username = row.username;
  editForm.realName = row.realName || '';
  editForm.oldBalance = (row as any).balance ?? 0;
  editForm.newBalance = editForm.oldBalance;
  dialogVisible.value = true;
}

async function handleSubmitBalance() {
  submitLoading.value = true;
  try {
    await updateBalance(editForm.userId, editForm.newBalance);
    ElMessage.success('余额修改成功');
    dialogVisible.value = false;
    fetchData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '修改失败');
  } finally {
    submitLoading.value = false;
  }
}

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.user-manage-view {
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
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.balance-text {
  color: #409eff;
  font-weight: bold;
}
</style>
