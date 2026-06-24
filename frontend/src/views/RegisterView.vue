<template>
  <div class="si-page">
    <!-- Background -->
    <BackgroundLines class="si-bg" />

    <!-- Left: Hero Image + Testimonials -->
    <section class="si-right">
      <div class="si-hero-img" />
      <div class="si-testimonials" v-if="testimonials.length">
        <div v-for="t in testimonials" :key="t.name" class="si-tcard">
          <img :src="t.avatarSrc" class="si-tcard-avatar" alt="avatar" />
          <div class="si-tcard-text">
            <p class="si-tcard-name">{{ t.name }}</p>
            <p class="si-tcard-handle">{{ t.handle }}</p>
            <p class="si-tcard-body">{{ t.text }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Right: Register Form -->
    <section class="si-left">
      <div class="si-form-wrap">
        <div class="si-form-inner">
          <router-link to="/" class="si-back">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
          </router-link>

          <h1 class="si-anim si-title">
            <span class="si-title-light">创建</span>账号
          </h1>
          <p class="si-anim si-desc">选择身份并填写信息以注册新账号</p>

          <form class="si-form" @submit.prevent="submit">
            <!-- Role -->
            <div class="si-anim">
              <label class="si-label">身份选择</label>
              <div class="si-glass">
                <div class="si-roles">
                  <button v-for="r in roles" :key="r.value" type="button" class="si-role-btn" :class="{ active: form.role === r.value }" @click="form.role = r.value">
                    <span class="si-role-icon">
                      <svg v-if="r.icon === 'coach'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M16 4h2a2 2 0 012 2v14a2 2 0 01-2 2H6a2 2 0 01-2-2V6a2 2 0 012-2h2"/><rect x="8" y="2" width="8" height="4" rx="1"/><path d="M9 14l2 2 4-4"/></svg>
                      <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                    </span>
                    <span>{{ r.label }}</span>
                  </button>
                </div>
              </div>
            </div>

            <!-- Username -->
            <div class="si-anim">
              <label class="si-label">用户名</label>
              <div class="si-glass">
                <input v-model="form.username" type="text" placeholder="请输入用户名" autocomplete="username" class="si-input" />
              </div>
            </div>

            <!-- Real Name -->
            <div class="si-anim">
              <label class="si-label">姓名</label>
              <div class="si-glass">
                <input v-model="form.realName" type="text" placeholder="请输入真实姓名" class="si-input" />
              </div>
            </div>

            <!-- Password -->
            <div class="si-anim">
              <label class="si-label">密码</label>
              <div class="si-glass">
                <div class="si-input-relative">
                  <input v-model="form.password" :type="showPwd ? 'text' : 'password'" placeholder="请设置密码" autocomplete="new-password" class="si-input si-input-pr" />
                  <button type="button" class="si-eye-btn" @click="showPwd = !showPwd">
                    <svg v-if="!showPwd" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                    <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                  </button>
                </div>
              </div>
            </div>

            <!-- Phone (optional) -->
            <div class="si-anim">
              <label class="si-label">手机号 <span class="si-optional">（选填）</span></label>
              <div class="si-glass">
                <input v-model="form.phone" type="tel" placeholder="请输入手机号" class="si-input" />
              </div>
            </div>

            <!-- Submit -->
            <button type="submit" class="si-anim si-submit" :disabled="loading">
              {{ loading ? '注册中...' : '注 册' }}
            </button>
          </form>

          <!-- Divider -->
          <div class="si-anim si-divider">
            <span class="si-divider-line" />
            <span class="si-divider-text">或</span>
            <span class="si-divider-line" />
          </div>

          <p class="si-anim si-register">
            已有账号？
            <router-link to="/login" class="si-register-link">立即登录</router-link>
          </p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { gsap } from 'gsap';
import { register, type UserRole } from '../api/auth';
import { useUserStore } from '../stores/user';
import BackgroundLines from '../components/landing/BackgroundLines.vue';

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const showPwd = ref(false);

const roles = [
  { label: '教练', value: 'COACH' as UserRole, icon: 'coach' },
  { label: '会员', value: 'MEMBER' as UserRole, icon: 'member' },
];

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  role: 'MEMBER' as UserRole,
});

const testimonials = [
  { avatarSrc: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100&h=100&fit=crop&crop=face', name: '张伟', handle: '@zhangwei', text: '注册流程非常简单，几分钟就完成了。' },
  { avatarSrc: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&h=100&fit=crop&crop=face', name: '李芳', handle: '@lifang', text: '平台功能强大，管理课程变得轻松。' },
  { avatarSrc: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&h=100&fit=crop&crop=face', name: '王磊', handle: '@wanglei', text: '强烈推荐给所有健身从业者。' },
];

onMounted(() => {
  gsap.fromTo('.si-anim',
    { autoAlpha: 0, y: 16, filter: 'blur(6px)' },
    { autoAlpha: 1, y: 0, filter: 'blur(0px)', stagger: 0.07, duration: 0.6, ease: 'power2.out', delay: 0.15 }
  );
  gsap.fromTo('.si-hero-img',
    { autoAlpha: 0, x: -32, filter: 'blur(12px)' },
    { autoAlpha: 1, x: 0, filter: 'blur(0px)', duration: 1, ease: 'power2.out', delay: 0.3 }
  );
  gsap.fromTo('.si-tcard',
    { autoAlpha: 0, y: 24, scale: 0.92, filter: 'blur(6px)' },
    { autoAlpha: 1, y: 0, scale: 1, filter: 'blur(0px)', stagger: 0.15, duration: 0.7, ease: 'power2.out', delay: 0.6 }
  );
});

async function submit() {
  if (!form.username || !form.password || !form.realName) {
    ElMessage.warning('请填写必填信息');
    return;
  }
  loading.value = true;
  try {
    const user = await register({
      username: form.username,
      password: form.password,
      role: form.role,
      realName: form.realName,
      phone: form.phone || undefined,
    });
    userStore.setUser(user);
    ElMessage.success(`注册成功，欢迎 ${user.realName || user.username}`);
    router.push('/dashboard/workbench');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '注册失败');
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
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
@media (min-width: 768px) { .si-page { flex-direction: row; } }
.si-bg { position: fixed; inset: 0; z-index: 0; min-height: 100dvh; pointer-events: none; }

/* Left form */
.si-left { position: relative; z-index: 1; flex: 1; display: flex; align-items: center; justify-content: center; padding: 2rem; overflow-y: auto; }
.si-form-wrap {
  width: 100%; max-width: 32rem;
  background: rgba(20, 20, 20, 0.6);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 1.75rem;
  padding: 2.5rem 2.25rem;
  box-shadow: 0 0 0 1px rgba(255,255,255,0.03), 0 10px 50px rgba(0,0,0,0.45);
}
.si-form-inner { display: flex; flex-direction: column; gap: 1.15rem; }

.si-back { position: absolute; top: 1.25rem; right: 1.25rem; display: flex; align-items: center; justify-content: center; width: 2rem; height: 2rem; border-radius: 0.5rem; color: rgba(255,255,255,0.4); text-decoration: none; transition: all 0.2s; }
.si-back:hover { color: #fafafa; background: rgba(255,255,255,0.05); }
@media (min-width: 768px) { .si-back { top: 2rem; right: 2rem; } }

.si-title { font-size: 2.5rem; font-weight: 700; line-height: 1.15; color: #fafafa; letter-spacing: -0.02em; }
@media (min-width: 768px) { .si-title { font-size: 2.75rem; } }
.si-title-light { font-weight: 700; letter-spacing: -0.04em; }
.si-desc { font-size: 1rem; color: rgba(255,255,255,0.65); line-height: 1.6; }
.si-form { display: flex; flex-direction: column; gap: 1.15rem; }
.si-label { display: block; font-size: 0.8rem; font-weight: 500; letter-spacing: 1.5px; text-transform: uppercase; color: rgba(255,255,255,0.5); margin-bottom: 0.5rem; padding-left: 2px; }
.si-optional { text-transform: none; letter-spacing: 0; font-size: 0.7rem; color: rgba(255,255,255,0.3); }

.si-glass { border-radius: 1.25rem; border: 1px solid hsl(0,0%,15%); background: rgba(250,250,250,0.03); backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px); transition: all 0.25s; }
.si-glass:focus-within { border-color: rgba(139,92,246,0.5); background: rgba(139,92,246,0.06); }

.si-roles { display: flex; padding: 0.2rem; }
.si-role-btn { flex: 1; display: flex; align-items: center; justify-content: center; gap: 0.35rem; padding: 0.75rem 0; border: none; border-radius: 1rem; background: transparent; color: rgba(255,255,255,0.45); font-size: 1rem; font-weight: 500; cursor: pointer; transition: all 0.25s; outline: none; }
.si-role-btn:hover { color: #a3a3a3; }
.si-role-btn.active { background: rgba(139,92,246,0.12); color: #fafafa; box-shadow: 0 0 0 1px rgba(139,92,246,0.2); }
.si-role-icon { display: flex; align-items: center; justify-content: center; width: 1.25rem; height: 1.25rem; flex-shrink: 0; }

.si-input { width: 100%; background: transparent; border: none; outline: none; color: #fafafa; font-size: 1.05rem; font-family: inherit; padding: 1rem 1.25rem; border-radius: 1.25rem; }
.si-input::placeholder { color: rgba(255,255,255,0.3); }
.si-input-relative { position: relative; }
.si-input-pr { padding-right: 3rem; }
.si-eye-btn { position: absolute; inset: 0 0 0 auto; display: flex; align-items: center; padding-right: 0.875rem; background: none; border: none; color: rgba(255,255,255,0.4); cursor: pointer; transition: color 0.2s; }
.si-eye-btn:hover { color: #a3a3a3; }

.si-submit { width: 100%; padding: 1.05rem; border: none; border-radius: 1.25rem; background: #2563eb; color: #ffffff; font-size: 1.1rem; font-weight: 600; letter-spacing: 1px; cursor: pointer; transition: all 0.25s; outline: none; }
.si-submit:hover { background: #1d4ed8; box-shadow: 0 4px 16px rgba(37,99,235,0.3); }
.si-submit:active { transform: scale(0.98); }
.si-submit:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }

.si-divider { position: relative; display: flex; align-items: center; justify-content: center; }
.si-divider-line { flex: 1; height: 1px; background: hsl(0,0%,15%); }
.si-divider-text { padding: 0 1rem; font-size: 0.95rem; color: rgba(255,255,255,0.4); background: rgba(10,10,10,0.6); }

.si-register { text-align: center; font-size: 0.95rem; color: rgba(255,255,255,0.5); margin: 0; }
.si-register-link { color: #a78bfa; text-decoration: none; font-weight: 500; transition: color 0.2s; }
.si-register-link:hover { color: #c4b5fd; text-decoration: underline; }

/* Right image */
.si-right { display: none; position: relative; z-index: 1; flex: 1; padding: 1rem; }
@media (min-width: 768px) { .si-right { display: block; } }
.si-hero-img { position: absolute; inset: 1rem; border-radius: 1.5rem; background-image: url('https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=1200&q=80'); background-size: cover; background-position: center; }
.si-testimonials { position: absolute; bottom: 2rem; left: 50%; transform: translateX(-50%); display: flex; gap: 1rem; padding: 0 2rem; width: 100%; justify-content: center; z-index: 2; }
.si-tcard { display: flex; align-items: flex-start; gap: 0.75rem; border-radius: 1.5rem; background: rgba(23,23,23,0.65); backdrop-filter: blur(24px); -webkit-backdrop-filter: blur(24px); border: 1px solid rgba(255,255,255,0.08); padding: 1rem; width: 16rem; flex-shrink: 0; }
.si-tcard:nth-child(2) { display: none; }
.si-tcard:nth-child(3) { display: none; }
@media (min-width: 1280px) { .si-tcard:nth-child(2) { display: flex; } }
@media (min-width: 1536px) { .si-tcard:nth-child(3) { display: flex; } }
.si-tcard-avatar { width: 2.5rem; height: 2.5rem; border-radius: 0.75rem; object-fit: cover; flex-shrink: 0; }
.si-tcard-text { font-size: 0.8rem; line-height: 1.4; }
.si-tcard-name { font-weight: 600; color: #fafafa; margin: 0; }
.si-tcard-handle { font-size: 0.7rem; color: #737373; margin: 0; }
.si-tcard-body { margin-top: 0.4rem; color: rgba(250,250,250,0.7); font-size: 0.78rem; }

.si-anim { opacity: 0; }
</style>
