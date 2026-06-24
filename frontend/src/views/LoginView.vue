<template>
  <div class="si-page">
    <!-- ═══ Background ═══ -->
    <BackgroundLines class="si-bg" />

    <!-- ═══ Left Column: Hero Image + Testimonials ═══ -->
    <section class="si-right">
      <div class="si-hero-img" />
      <div class="si-testimonials" v-if="testimonials.length">
        <div
          v-for="(t, i) in testimonials"
          :key="t.name"
          class="si-tcard"
          :class="`si-tcard-d${i}`"
        >
          <img :src="t.avatarSrc" class="si-tcard-avatar" alt="avatar" />
          <div class="si-tcard-text">
            <p class="si-tcard-name">{{ t.name }}</p>
            <p class="si-tcard-handle">{{ t.handle }}</p>
            <p class="si-tcard-body">{{ t.text }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══ Right Column: Sign-in Form ═══ -->
    <section class="si-left">
      <div class="si-form-wrap">
        <div class="si-form-inner">
          <!-- Back -->
          <router-link to="/" class="si-back">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
          </router-link>

          <!-- Title -->
          <h1 class="si-anim si-d1 si-title">
            <span class="si-title-light">欢迎</span>回来
          </h1>
          <p class="si-anim si-d2 si-desc">选择身份并验证以继续访问系统</p>

          <form class="si-form" @submit.prevent="submit">
            <!-- Role Selector -->
            <div class="si-anim si-d3">
              <label class="si-label">身份选择</label>
              <div class="si-glass">
                <div class="si-roles">
                  <button
                    v-for="r in roles"
                    :key="r.value"
                    type="button"
                    class="si-role-btn"
                    :class="{ active: form.role === r.value }"
                    @click="form.role = r.value"
                  >
                    <span class="si-role-icon">
                      <!-- Admin: Settings gear -->
                      <svg v-if="r.icon === 'admin'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 01-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/>
                      </svg>
                      <!-- Coach: Whistle / clipboard -->
                      <svg v-else-if="r.icon === 'coach'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M16 4h2a2 2 0 012 2v14a2 2 0 01-2 2H6a2 2 0 01-2-2V6a2 2 0 012-2h2"/><rect x="8" y="2" width="8" height="4" rx="1"/><path d="M9 14l2 2 4-4"/>
                      </svg>
                      <!-- Member: Person -->
                      <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/>
                      </svg>
                    </span>
                    <span>{{ r.label }}</span>
                  </button>
                </div>
              </div>
            </div>

            <!-- Username -->
            <div class="si-anim si-d3">
              <label class="si-label">用户名</label>
              <div class="si-glass">
                <input
                  v-model="form.username"
                  type="text"
                  placeholder="请输入用户名"
                  autocomplete="username"
                  class="si-input"
                />
              </div>
            </div>

            <!-- Password -->
            <div class="si-anim si-d4">
              <label class="si-label">密码</label>
              <div class="si-glass">
                <div class="si-input-relative">
                  <input
                    v-model="form.password"
                    :type="showPwd ? 'text' : 'password'"
                    placeholder="请输入密码"
                    autocomplete="current-password"
                    class="si-input si-input-pr"
                    @keyup.enter="submit"
                  />
                  <button type="button" class="si-eye-btn" @click="showPwd = !showPwd">
                    <!-- Eye icon -->
                    <svg v-if="!showPwd" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                    </svg>
                    <!-- EyeOff icon -->
                    <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5">
                      <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94"/>
                      <path d="M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19"/>
                      <line x1="1" y1="1" x2="23" y2="23"/>
                    </svg>
                  </button>
                </div>
              </div>
            </div>

            <!-- Remember + Forgot -->
            <div class="si-anim si-d5 si-row-between">
              <label class="si-remember">
                <input type="checkbox" class="si-checkbox" />
                <span>保持登录</span>
              </label>
              <a href="javascript:void(0)" @click="forgotDialogVisible = true" class="si-forgot">忘记密码？</a>
            </div>

            <!-- Submit -->
            <button type="submit" class="si-anim si-d6 si-submit" :disabled="loading">
              {{ loading ? '验证中...' : '登 录' }}
            </button>
          </form>

          <!-- Divider -->
          <div class="si-anim si-d7 si-divider">
            <span class="si-divider-line" />
            <span class="si-divider-text">或</span>
            <span class="si-divider-line" />
          </div>

          <!-- Register Link -->
          <p class="si-anim si-d9 si-register">
            还没有账号？
            <router-link to="/register" class="si-register-link">立即注册</router-link>
          </p>
        </div>
      </div>
    </section>

    <!-- ═══ Forgot Password Modal ═══ -->
    <Transition name="si-modal">
      <div v-if="forgotDialogVisible" class="si-modal-overlay" @click.self="forgotDialogVisible = false">
        <div class="si-modal-card">
          <div class="si-modal-header">
            <h2 class="si-modal-title">重置密码</h2>
            <button class="si-modal-close" @click="forgotDialogVisible = false">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>

          <form class="si-modal-form" @submit.prevent="submitResetPassword">
            <div>
              <label class="si-label">邮箱</label>
              <div class="si-glass">
                <input v-model="forgotForm.email" type="email" placeholder="请输入注册时的邮箱" class="si-input" />
              </div>
            </div>

            <div>
              <label class="si-label">验证码</label>
              <div class="si-glass">
                <div class="si-code-inner">
                  <input v-model="forgotForm.code" type="text" placeholder="请输入验证码" class="si-input si-code-input" />
                  <button type="button" class="si-code-btn" :disabled="codeCooldown > 0" @click="handleSendCode">
                    {{ codeCooldown > 0 ? `${codeCooldown}s` : '获取' }}
                  </button>
                </div>
              </div>
            </div>

            <div>
              <label class="si-label">新密码</label>
              <div class="si-glass">
                <input v-model="forgotForm.newPassword" type="password" placeholder="请输入新密码" class="si-input" />
              </div>
            </div>

            <div>
              <label class="si-label">确认新密码</label>
              <div class="si-glass">
                <input v-model="forgotForm.confirmPassword" type="password" placeholder="请再次输入新密码" class="si-input" />
              </div>
            </div>

            <div class="si-modal-actions">
              <button type="button" class="si-modal-btn-cancel" @click="forgotDialogVisible = false">取消</button>
              <button type="submit" class="si-modal-btn-submit" :disabled="resetLoading">
                {{ resetLoading ? '重置中...' : '确认重置' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { gsap } from 'gsap';
import { login, sendCode, resetPasswordByCode, type UserRole } from '../api/auth';
import { useUserStore } from '../stores/user';
import BackgroundLines from '../components/landing/BackgroundLines.vue';

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const resetLoading = ref(false);
const forgotDialogVisible = ref(false);
const codeCooldown = ref(0);
const showPwd = ref(false);

const roles = [
  { label: '管理员', value: 'ADMIN' as UserRole, icon: 'admin' },
  { label: '教练', value: 'COACH' as UserRole, icon: 'coach' },
  { label: '会员', value: 'MEMBER' as UserRole, icon: 'member' },
];

const form = reactive({
  username: '',
  password: '',
  role: 'MEMBER' as UserRole,
});

const forgotForm = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
});

/* ─── Testimonials ─── */
const testimonials = [
  {
    avatarSrc: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100&h=100&fit=crop&crop=face',
    name: '张伟',
    handle: '@zhangwei_fit',
    text: '用了智训 ERP 之后，排课效率提升了 3 倍，再也不用手动排课了。',
  },
  {
    avatarSrc: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&h=100&fit=crop&crop=face',
    name: '李芳',
    handle: '@lifang_coach',
    text: '财务管理非常强大，工资结算从两天缩短到几分钟。',
  },
  {
    avatarSrc: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&h=100&fit=crop&crop=face',
    name: '王磊',
    handle: '@wanglei_yoga',
    text: '学员管理一目了然，出勤率提升了 40%。',
  },
];

/* ─── GSAP Entrance Animations ─── */
onMounted(() => {
  // Stagger form elements
  gsap.fromTo('.si-anim',
    { autoAlpha: 0, y: 16, filter: 'blur(6px)' },
    { autoAlpha: 1, y: 0, filter: 'blur(0px)', stagger: 0.08, duration: 0.6, ease: 'power2.out', delay: 0.15 }
  );

  // Hero image slide-in
  gsap.fromTo('.si-hero-img',
    { autoAlpha: 0, x: -32, filter: 'blur(12px)' },
    { autoAlpha: 1, x: 0, filter: 'blur(0px)', duration: 1, ease: 'power2.out', delay: 0.3 }
  );

  // Testimonial cards
  gsap.fromTo('.si-tcard',
    { autoAlpha: 0, y: 24, scale: 0.92, filter: 'blur(6px)' },
    { autoAlpha: 1, y: 0, scale: 1, filter: 'blur(0px)', stagger: 0.15, duration: 0.7, ease: 'power2.out', delay: 0.6 }
  );
});

/* ─── Business Logic ─── */
async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写完整信息');
    return;
  }
  loading.value = true;
  try {
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
    ElMessage.success('密码重置成功');
    forgotDialogVisible.value = false;
    forgotForm.email = ''; forgotForm.code = '';
    forgotForm.newPassword = ''; forgotForm.confirmPassword = '';
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '密码重置失败');
  } finally {
    resetLoading.value = false;
  }
}

async function handleSendCode() {
  if (!forgotForm.email) { ElMessage.warning('请输入邮箱'); return; }
  try {
    await sendCode(forgotForm.email);
    ElMessage.success('验证码已发送');
    codeCooldown.value = 60;
    const timer = setInterval(() => {
      codeCooldown.value--;
      if (codeCooldown.value <= 0) clearInterval(timer);
    }, 1000);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发送失败');
  }
}
</script>

<style scoped>
/* ─── Page Layout ─── */
.si-page {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
  width: 100dvw;
  background: #0a0a0a;
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif;
  -webkit-font-smoothing: antialiased;
  overflow: hidden;
}
@media (min-width: 768px) {
  .si-page { flex-direction: row; }
}

.si-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  min-height: 100dvh;
  pointer-events: none;
}

/* ─── Left Column ─── */
.si-left {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}
.si-form-wrap {
  width: 100%;
  max-width: 32rem;
  background: rgba(20, 20, 20, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 1.75rem;
  padding: 2.5rem 2.25rem;
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.03),
    0 10px 50px rgba(0, 0, 0, 0.45);
}
.si-form-inner {
  display: flex;
  flex-direction: column;
  gap: 1.35rem;
}

/* Back */
.si-back {
  position: absolute;
  top: 1.25rem;
  right: 1.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: 0.5rem;
  color: rgba(255, 255, 255, 0.4);
  text-decoration: none;
  transition: all 0.2s;
}
.si-back:hover {
  color: #fafafa;
  background: rgba(255, 255, 255, 0.05);
}
@media (min-width: 768px) {
  .si-back { top: 2rem; right: 2rem; }
}

/* Title */
.si-title {
  font-size: 2.5rem;
  font-weight: 700;
  line-height: 1.15;
  color: #fafafa;
  letter-spacing: -0.02em;
}
@media (min-width: 768px) {
  .si-title { font-size: 2.75rem; }
}
.si-title-light {
  font-weight: 700;
  letter-spacing: -0.04em;
}
.si-desc {
  font-size: 1.1rem;
  color: rgba(255, 255, 255, 0.65);
  line-height: 1.6;
}

/* Form */
.si-form {
  display: flex;
  flex-direction: column;
  gap: 1.4rem;
}

/* Labels */
.si-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 0.5rem;
  padding-left: 2px;
}

/* Glass Input Wrapper */
.si-glass {
  border-radius: 1.25rem;
  border: 1px solid hsl(0, 0%, 15%);
  background: rgba(250, 250, 250, 0.03);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  transition: all 0.25s;
}
.si-glass:focus-within {
  border-color: rgba(139, 92, 246, 0.5);
  background: rgba(139, 92, 246, 0.06);
}

/* Roles */
.si-roles {
  display: flex;
  gap: 0;
  padding: 0.2rem;
}
.si-role-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  padding: 0.75rem 0;
  border: none;
  border-radius: 1rem;
  background: transparent;
  color: rgba(255, 255, 255, 0.45);
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s;
  outline: none;
}
.si-role-btn:hover {
  color: #a3a3a3;
}
.si-role-btn.active {
  background: rgba(139, 92, 246, 0.12);
  color: #fafafa;
  box-shadow: 0 0 0 1px rgba(139, 92, 246, 0.2);
}
.si-role-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.25rem;
  height: 1.25rem;
  flex-shrink: 0;
}

/* Input */
.si-input {
  width: 100%;
  background: transparent;
  border: none;
  outline: none;
  color: #fafafa;
  font-size: 1.05rem;
  font-family: inherit;
  padding: 1rem 1.25rem;
  border-radius: 1.25rem;
}
.si-input::placeholder {
  color: rgba(255, 255, 255, 0.3);
}
.si-input-relative {
  position: relative;
}
.si-input-pr {
  padding-right: 3rem;
}

/* Eye toggle */
.si-eye-btn {
  position: absolute;
  inset: 0 0 0 auto;
  display: flex;
  align-items: center;
  padding-right: 0.875rem;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  transition: color 0.2s;
}
.si-eye-btn:hover {
  color: #a3a3a3;
}

/* Row between */
.si-row-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.95rem;
}

/* Remember me */
.si-remember {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  cursor: pointer;
  color: rgba(250, 250, 250, 0.8);
  font-size: 0.95rem;
}
.si-checkbox {
  width: 1rem;
  height: 1rem;
  border-radius: 0.25rem;
  border: 1px solid hsl(0, 0%, 25%);
  background: transparent;
  accent-color: #8b5cf6;
  cursor: pointer;
}

/* Forgot */
.si-forgot {
  color: #a78bfa;
  text-decoration: none;
  font-size: 0.95rem;
  transition: color 0.2s;
}
.si-forgot:hover {
  color: #c4b5fd;
  text-decoration: underline;
}

/* Submit */
.si-submit {
  width: 100%;
  padding: 1.05rem;
  border: none;
  border-radius: 1.25rem;
  background: #2563eb;
  color: #ffffff;
  font-size: 1.1rem;
  font-weight: 600;
  letter-spacing: 1px;
  cursor: pointer;
  transition: all 0.25s;
  outline: none;
}
.si-submit:hover {
  background: #1d4ed8;
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.3);
}
.si-submit:active {
  transform: scale(0.98);
}
.si-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
}

/* Divider */
.si-divider {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.si-divider-line {
  flex: 1;
  height: 1px;
  background: hsl(0, 0%, 15%);
}
.si-divider-text {
  padding: 0 1rem;
  font-size: 0.95rem;
  color: rgba(255, 255, 255, 0.4);
  background: rgba(10, 10, 10, 0.6);
}

/* Register */
.si-register {
  text-align: center;
  font-size: 0.95rem;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}
.si-register-link {
  color: #a78bfa;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}
.si-register-link:hover {
  color: #c4b5fd;
  text-decoration: underline;
}

/* ─── Right Column ─── */
.si-right {
  display: none;
  position: relative;
  z-index: 1;
  flex: 1;
  padding: 1rem;
}
@media (min-width: 768px) {
  .si-right { display: block; }
}

/* Hero Image */
.si-hero-img {
  position: absolute;
  inset: 1rem;
  border-radius: 1.5rem;
  background-image: url('https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=1200&q=80');
  background-size: cover;
  background-position: center;
}

/* Testimonials */
.si-testimonials {
  position: absolute;
  bottom: 2rem;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 1rem;
  padding: 0 2rem;
  width: 100%;
  justify-content: center;
  z-index: 2;
}

.si-tcard {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  border-radius: 1.5rem;
  background: rgba(23, 23, 23, 0.65);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 1rem;
  width: 16rem;
  flex-shrink: 0;
}
/* Hide 2nd card below xl, 3rd below 2xl */
.si-tcard:nth-child(2) {
  display: none;
}
.si-tcard:nth-child(3) {
  display: none;
}
@media (min-width: 1280px) {
  .si-tcard:nth-child(2) { display: flex; }
}
@media (min-width: 1536px) {
  .si-tcard:nth-child(3) { display: flex; }
}

.si-tcard-avatar {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 0.75rem;
  object-fit: cover;
  flex-shrink: 0;
}
.si-tcard-text {
  font-size: 0.8rem;
  line-height: 1.4;
}
.si-tcard-name {
  font-weight: 600;
  color: #fafafa;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}
.si-tcard-handle {
  font-size: 0.7rem;
  color: #737373;
  margin: 0;
}
.si-tcard-body {
  margin-top: 0.4rem;
  color: rgba(250, 250, 250, 0.7);
  font-size: 0.78rem;
}

/* ─── Animation helpers ─── */
/* initial state set by GSAP fromTo */

/* ─── Forgot Password Modal ─── */
.si-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  padding: 1rem;
}
.si-modal-card {
  width: 100%;
  max-width: 28rem;
  background: rgba(20, 20, 20, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 1.75rem;
  padding: 2rem 2rem 1.75rem;
  box-shadow: 0 16px 64px rgba(0, 0, 0, 0.5);
}
.si-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}
.si-modal-title {
  font-size: 1.35rem;
  font-weight: 700;
  color: #fafafa;
  margin: 0;
}
.si-modal-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border: none;
  border-radius: 0.5rem;
  background: transparent;
  color: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  transition: all 0.2s;
}
.si-modal-close:hover {
  color: #fafafa;
  background: rgba(255, 255, 255, 0.06);
}
.si-modal-form {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}
.si-code-inner {
  display: flex;
  align-items: center;
}
.si-code-input {
  flex: 1;
}
.si-code-btn {
  flex-shrink: 0;
  padding: 0 1.25rem;
  height: 2.5rem;
  margin-right: 0.4rem;
  border: none;
  border-radius: 0.75rem;
  background: linear-gradient(135deg, #5b7cf7, #818cf8);
  color: #fff;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}
.si-code-btn:hover {
  background: linear-gradient(135deg, #6b8cff, #9ba6ff);
}
.si-code-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.si-modal-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 0.5rem;
}
.si-modal-btn-cancel {
  flex: 1;
  padding: 0.85rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.04);
  color: #b0b4c0;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.si-modal-btn-cancel:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fafafa;
}
.si-modal-btn-submit {
  flex: 1;
  padding: 0.85rem;
  border: none;
  border-radius: 1rem;
  background: linear-gradient(135deg, #5b7cf7, #818cf8);
  color: #fff;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
}
.si-modal-btn-submit:hover {
  background: linear-gradient(135deg, #6b8cff, #9ba6ff);
  box-shadow: 0 4px 16px rgba(91, 124, 247, 0.35);
}
.si-modal-btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Modal transition */
.si-modal-enter-active,
.si-modal-leave-active {
  transition: opacity 0.25s ease;
}
.si-modal-enter-active .si-modal-card,
.si-modal-leave-active .si-modal-card {
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.25s ease;
}
.si-modal-enter-from,
.si-modal-leave-to {
  opacity: 0;
}
.si-modal-enter-from .si-modal-card {
  transform: scale(0.95) translateY(10px);
  opacity: 0;
}
.si-modal-leave-to .si-modal-card {
  transform: scale(0.95) translateY(10px);
  opacity: 0;
}
</style>
