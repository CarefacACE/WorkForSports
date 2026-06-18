<template>
  <el-container class="admin-layout">
    <el-aside width="236px" class="sidebar">
      <div class="logo-area">
        <div class="logo-mark">智</div>
        <div>
          <div class="logo-title">智训业财云</div>
          <div class="logo-subtitle">{{ roleLabel }}后台</div>
        </div>
      </div>

      <el-menu router :default-active="route.path" class="side-menu">
        <el-menu-item index="/dashboard/workbench">
          <el-icon><Monitor /></el-icon>
          <span>工作栏</span>
        </el-menu-item>
        <el-sub-menu v-if="user?.role === 'ADMIN'" index="user-manage">
          <template #title>
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </template>
          <el-menu-item index="/dashboard/coaches">教练信息</el-menu-item>
          <el-menu-item index="/dashboard/coach-salary">教练工资</el-menu-item>
          <el-menu-item index="/dashboard/members">会员信息</el-menu-item>
          <el-menu-item index="/dashboard/member-balance">会员金额</el-menu-item>
        </el-sub-menu>
        <el-sub-menu v-if="user?.role === 'MEMBER'" index="course-select">
          <template #title>
            <el-icon><Reading /></el-icon>
            <span>选课</span>
          </template>
          <el-menu-item index="/member/public-courses">公共课</el-menu-item>
          <el-menu-item index="/member/private-courses">私教</el-menu-item>
        </el-sub-menu>
        <el-sub-menu v-if="user?.role === 'COACH'" index="course-manage">
          <template #title>
            <el-icon><Reading /></el-icon>
            <span>课程</span>
          </template>
          <el-menu-item index="/coach/public-courses">公共课</el-menu-item>
          <el-menu-item index="/coach/private-courses">私教课</el-menu-item>
        </el-sub-menu>
        <el-sub-menu v-if="user?.role !== 'ADMIN'" index="my-menu">
          <template #title>
            <el-icon><User /></el-icon>
            <span>我的</span>
          </template>
          <el-menu-item index="/dashboard/wallet">钱包</el-menu-item>
          <el-menu-item v-if="user?.role === 'MEMBER'" index="/member/my-courses">我的课程</el-menu-item>
          <el-menu-item v-if="user?.role === 'COACH'" index="/coach/my-courses">我的课程</el-menu-item>
          <el-menu-item v-if="user?.role === 'COACH'" index="/coach/my-students">我的学员</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <div class="page-title">{{ roleLabel }}工作台</div>
          <div class="page-subtitle">不同身份后续会开放不同操作功能</div>
        </div>
        <div class="user-panel">
          <el-tag type="primary" effect="light">{{ roleLabel }}</el-tag>
          <span>{{ user?.realName || user?.username || '未登录用户' }}</span>
          <el-button text @click="openProfileDialog">个人信息</el-button>
          <el-button text @click="passwordDialogVisible = true">修改密码</el-button>
          <el-button text @click="logout">退出</el-button>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>

    <el-dialog v-model="profileDialogVisible" title="个人信息" width="560px">
      <el-form :model="profileForm" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名">
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份">
              <el-input :model-value="roleLabel" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名">
              <el-input v-model="profileForm.realName" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="profileForm.gender" placeholder="请选择性别" clearable class="profile-select">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生日">
              <el-date-picker
                v-model="profileForm.birthday"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择生日"
                class="profile-date"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="头像地址">
              <el-input v-model="profileForm.avatar" placeholder="请输入头像 URL" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="profileForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="profileLoading" @click="submitProfile">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="420px">
      <el-form :model="passwordForm" label-position="top">
        <el-form-item label="原密码">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="submitChangePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Monitor, User, Reading } from '@element-plus/icons-vue';
import { changePassword, getProfile, updateProfile, type UserProfile } from '../api/auth';
import { useUserStore } from '../stores/user';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const user = computed(() => userStore.user);
const passwordDialogVisible = ref(false);
const passwordLoading = ref(false);
const profileDialogVisible = ref(false);
const profileLoading = ref(false);

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const profileForm = reactive({
  id: 0,
  username: '',
  realName: '',
  phone: '',
  email: '',
  gender: '',
  birthday: '',
  avatar: '',
  remark: '',
});

const roleLabel = computed(() => {
  const roleMap = {
    ADMIN: '管理员',
    COACH: '教练',
    MEMBER: '会员',
  };

  return roleMap[user.value?.role || 'MEMBER'];
});

function fillProfileForm(profile: UserProfile) {
  profileForm.id = profile.id;
  profileForm.username = profile.username;
  profileForm.realName = profile.realName || '';
  profileForm.phone = profile.phone || '';
  profileForm.email = profile.email || '';
  profileForm.gender = profile.gender || '';
  profileForm.birthday = profile.birthday || '';
  profileForm.avatar = profile.avatar || '';
  profileForm.remark = profile.remark || '';
}

async function openProfileDialog() {
  if (!user.value) {
    ElMessage.warning('请先登录');
    return;
  }

  profileDialogVisible.value = true;
  profileLoading.value = true;
  try {
    const profile = await getProfile(user.value.id);
    fillProfileForm(profile);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取个人信息失败');
  } finally {
    profileLoading.value = false;
  }
}

async function submitProfile() {
  if (!user.value) {
    ElMessage.warning('请先登录');
    return;
  }

  if (!profileForm.realName) {
    ElMessage.warning('请填写姓名');
    return;
  }

  profileLoading.value = true;
  try {
    const profile = await updateProfile({
      id: profileForm.id,
      realName: profileForm.realName,
      phone: profileForm.phone,
      email: profileForm.email,
      gender: profileForm.gender,
      birthday: profileForm.birthday,
      avatar: profileForm.avatar,
      remark: profileForm.remark,
    });
    userStore.updateProfile(profile);
    fillProfileForm(profile);
    profileDialogVisible.value = false;
    ElMessage.success('个人信息保存成功');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存个人信息失败');
  } finally {
    profileLoading.value = false;
  }
}

function logout() {
  userStore.logout();
  router.push('/login');
}

async function submitChangePassword() {
  if (!user.value) {
    ElMessage.warning('请先登录');
    return;
  }

  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请填写完整信息');
    return;
  }

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致');
    return;
  }

  passwordLoading.value = true;
  try {
    await changePassword({
      username: user.value.username,
      role: user.value.role,
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    });
    ElMessage.success('密码修改成功，请重新登录');
    passwordDialogVisible.value = false;
    logout();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '密码修改失败');
  } finally {
    passwordLoading.value = false;
  }
}
</script>

<style src="../styles/layouts/admin-layout.css"></style>
