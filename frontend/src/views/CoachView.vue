<template>
  <div class="user-manage-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>教练信息管理</span>
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
        <div class="toolbar-right">
          <el-button type="primary" @click="addDialogVisible = true">新增教练</el-button>
          <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
            批量删除 ({{ selectedIds.length }})
          </el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column prop="gender" label="性别" width="70" align="center" />
        <el-table-column prop="birthday" label="生日" width="110" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该用户？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
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

    <el-dialog v-model="addDialogVisible" title="新增教练" width="480px">
      <el-form :model="addForm" label-position="top">
        <el-form-item label="用户名" required>
          <el-input v-model="addForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="addForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="addForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleAdd">确认添加</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="编辑用户信息" width="560px">
      <el-form :model="editForm" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名">
              <el-input v-model="editForm.username" placeholder="请输入用户名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名">
              <el-input v-model="editForm.realName" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码（留空则不修改）">
              <el-input v-model="editForm.password" type="password" placeholder="留空则不修改" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份">
              <el-select v-model="editForm.role" style="width: 100%">
                <el-option label="教练" value="COACH" />
                <el-option label="会员" value="MEMBER" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="editForm.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="editForm.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="editForm.gender" placeholder="请选择性别" clearable style="width: 100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生日">
              <el-date-picker v-model="editForm.birthday" type="date" value-format="YYYY-MM-DD"
                placeholder="请选择生日" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="editForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getUsers, registerUser, updateUser, deleteUser, deleteUsers, type UserItem } from '../api/user';

const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const addDialogVisible = ref(false);
const keyword = ref('');
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableData = ref<UserItem[]>([]);
const selectedIds = ref<number[]>([]);

const editForm = reactive({
  id: 0,
  username: '',
  password: '',
  role: 'COACH' as string,
  realName: '',
  phone: '',
  email: '',
  gender: '',
  birthday: '',
  remark: '',
});

const addForm = reactive({
  username: '',
  password: '',
  realName: '',
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

function handleSelectionChange(rows: UserItem[]) {
  selectedIds.value = rows.map(r => r.id);
}

function handleEdit(row: UserItem) {
  editForm.id = row.id;
  editForm.username = row.username;
  editForm.password = '';
  editForm.role = row.role;
  editForm.realName = row.realName || '';
  editForm.phone = row.phone || '';
  editForm.email = row.email || '';
  editForm.gender = row.gender || '';
  editForm.birthday = row.birthday || '';
  editForm.remark = row.remark || '';
  dialogVisible.value = true;
}

async function handleSubmitEdit() {
  submitLoading.value = true;
  try {
    await updateUser({
      id: editForm.id,
      username: editForm.username,
      password: editForm.password || undefined,
      role: editForm.role,
      realName: editForm.realName,
      phone: editForm.phone,
      email: editForm.email,
      gender: editForm.gender,
      birthday: editForm.birthday,
      remark: editForm.remark,
    } as Partial<UserItem>);
    ElMessage.success('更新成功');
    dialogVisible.value = false;
    fetchData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新失败');
  } finally {
    submitLoading.value = false;
  }
}

async function handleDelete(id: number) {
  try {
    await deleteUser(id);
    ElMessage.success('删除成功');
    fetchData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败');
  }
}

async function handleBatchDelete() {
  try {
    await deleteUsers(selectedIds.value);
    ElMessage.success('批量删除成功');
    selectedIds.value = [];
    fetchData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '批量删除失败');
  }
}

async function handleAdd() {
  if (!addForm.username || !addForm.password || !addForm.realName) {
    ElMessage.warning('请填写完整信息');
    return;
  }
  submitLoading.value = true;
  try {
    await registerUser({
      username: addForm.username,
      password: addForm.password,
      realName: addForm.realName,
      role: 'COACH',
    });
    ElMessage.success('添加成功');
    addDialogVisible.value = false;
    addForm.username = '';
    addForm.password = '';
    addForm.realName = '';
    fetchData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '添加失败');
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
</style>
