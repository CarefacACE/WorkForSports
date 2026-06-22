<template>
  <div class="login-page">
    <div class="brand-panel">
      <div class="brand-badge">智训</div>
      <h1>智训业财云</h1>
      <p>硬核健身房教务与财务结算 ERP 系统</p>
      <div class="feature-list">
        <span>教练端</span>
        <span>管理员端</span>
        <span>会员端</span>
      </div>
    </div>

    <el-card class="login-card" shadow="always">
      <template #header>
        <div class="card-header">
          <h2>{{ isRegister ? '注册账号' : '登录系统' }}</h2>
          <p>{{ isRegister ? '选择身份后创建账号' : '请选择身份并登录' }}</p>
        </div>
      </template>

      <el-form :model="form" label-position="top" size="large">
        <el-form-item label="用户身份">
          <el-radio-group v-model="form.role" class="role-group">
            <el-radio-button v-if="!isRegister" value="ADMIN">管理员</el-radio-button>
            <el-radio-button value="COACH">教练</el-radio-button>
            <el-radio-button value="MEMBER">会员</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" clearable />
        </el-form-item>

        <el-form-item v-if="isRegister" label="姓名">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" clearable />
        </el-form-item>

        <el-form-item v-if="isRegister" label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" clearable />
        </el-form-item>

        <el-form-item v-if="isRegister" label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" clearable />
        </el-form-item>

        <el-form-item label="密码">
          <el-input v-model="form.password" placeholder="请输入密码" type="password" show-password />
        </el-form-item>

        <div v-if="!isRegister" class="form-extra-line">
          <el-button link type="primary" @click="forgotDialogVisible = true">忘记密码？</el-button>
        </div>

        <el-button class="submit-button" type="primary" size="large" :loading="loading" @click="submit">
          {{ isRegister ? '立即注册' : '立即登录' }}
        </el-button>

        <div class="switch-line">
          <span>{{ isRegister ? '已有账号？' : '还没有账号？' }}</span>
          <el-button link type="primary" @click="isRegister = !isRegister">
            {{ isRegister ? '去登录' : '去注册' }}
          </el-button>
        </div>
      </el-form>
    </el-card>

    <el-dialog v-model="forgotDialogVisible" title="忘记密码" width="420px">
      <el-form :model="forgotForm" label-position="top">
        <el-form-item label="邮箱">
          <el-input v-model="forgotForm.email" placeholder="请输入注册时的邮箱" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="code-row">
            <el-input v-model="forgotForm.code" placeholder="请输入验证码" />
            <el-button type="primary" :disabled="codeCooldown > 0" @click="handleSendCode">
              {{ codeCooldown > 0 ? `${codeCooldown}s后重试` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="forgotForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="forgotForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forgotDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetLoading" @click="submitResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { login, register, sendCode, resetPasswordByCode, type UserRole } from '../api/auth';
import { useUserStore } from '../stores/user';

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const resetLoading = ref(false);
const isRegister = ref(false);
const forgotDialogVisible = ref(false);
const codeCooldown = ref(0);

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  role: 'ADMIN' as UserRole,
});

const forgotForm = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
});

async function submit() {
  if (!form.username || !form.password || (isRegister.value && !form.realName)) {
    ElMessage.warning('请填写完整信息');
    return;
  }

  loading.value = true;
  try {
    if (isRegister.value) {
      await register(form);
      ElMessage.success('注册成功，请登录');
      isRegister.value = false;
      form.password = '';
      return;
    }

    const user = await login(form);
    userStore.setUser(user);
    ElMessage.success(`登录成功，欢迎 ${user.realName || user.username}`);
    router.push('/dashboard/workbench');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败');
  } finally {
    loading.value = false;
  }
}

async function submitResetPassword() {
  if (!forgotForm.email || !forgotForm.code || !forgotForm.newPassword || !forgotForm.confirmPassword) {
    ElMessage.warning('请填写完整信息');
    return;
  }

  if (forgotForm.newPassword !== forgotForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致');
    return;
  }

  resetLoading.value = true;
  try {
    await resetPasswordByCode(forgotForm.email, forgotForm.code, forgotForm.newPassword);
    ElMessage.success('密码重置成功，请使用新密码登录');
    forgotDialogVisible.value = false;
    forgotForm.email = '';
    forgotForm.code = '';
    forgotForm.newPassword = '';
    forgotForm.confirmPassword = '';
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '密码重置失败');
  } finally {
    resetLoading.value = false;
  }
}

async function handleSendCode() {
  if (!forgotForm.email) {
    ElMessage.warning('请输入邮箱');
    return;
  }
  try {
    await sendCode(forgotForm.email);
    ElMessage.success('验证码已发送到邮箱');
    codeCooldown.value = 60;
    const timer = setInterval(() => {
      codeCooldown.value--;
      if (codeCooldown.value <= 0) {
        clearInterval(timer);
      }
    }, 1000);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发送失败');
  }
}
</script>

<style src="../styles/views/login.css"></style>
