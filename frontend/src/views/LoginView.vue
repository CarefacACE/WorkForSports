<template>
  <div class="login-page">
    <!-- ═══ Canvas 遥测网络背景 (White Theme) ═══ -->
    <div class="lt-bg-layer">
      <canvas ref="bgCanvas" class="lt-bg-canvas"></canvas>
    </div>

    <!-- ═══ 左侧品牌面板 ═══ -->
    <div class="lt-brand-panel">
      <div class="lt-logo">
        <svg viewBox="0 0 42 42" class="lt-logo-svg">
          <rect x="2" y="2" width="38" height="38" rx="10" fill="none" stroke="rgba(37,99,235,0.15)" stroke-width="1.5"/>
          <text x="21" y="28" text-anchor="middle" fill="#2563eb" font-family="Inter,sans-serif" font-size="16" font-weight="800">ZX</text>
        </svg>
        <div>
          <div class="lt-logo-title">智训业财云</div>
          <div class="lt-logo-sub">ZHIXUN · ERP · v2.0</div>
        </div>
      </div>

      <div class="lt-heading">
        <h1>BIOMETRIC<br/>FITNESS ERP</h1>
        <p>面向体育训练与健身行业的一体化智能管理平台。课程管理 · 财务结算 · 实时通讯 · 数据分析。</p>
      </div>

      <!-- F1 Telemetry SVG -->
      <div class="lt-telemetry">
        <svg viewBox="0 0 420 64" class="lt-telemetry-svg" preserveAspectRatio="none">
          <path class="lt-flow-path primary"
                d="M 0,32 Q 40,32 60,32 L 75,10 L 85,52 L 95,20 L 105,44 L 115,30 Q 140,32 170,32 L 210,32 L 225,6 L 235,56 L 245,16 L 255,46 L 265,30 Q 300,32 340,32 T 420,32"/>
          <path class="lt-flow-path secondary"
                d="M 0,44 C 50,40 100,20 150,36 S 250,50 300,26 S 380,18 420,30"/>
          <path class="lt-flow-path tertiary"
                d="M 0,18 C 60,22 120,44 180,28 S 280,14 340,38 L 420,34"/>
          <circle class="lt-flow-dot" cx="75" cy="10" r="2"/>
          <circle class="lt-flow-dot" cx="225" cy="6" r="2"/>
          <circle class="lt-flow-dot" cx="380" cy="32" r="2"/>
          <text class="lt-flow-label" x="70" y="6">HR</text>
          <text class="lt-flow-label" x="220" y="3">PWR</text>
          <text class="lt-flow-label" x="372" y="26">SPD</text>
        </svg>
      </div>

      <div class="lt-stat-row">
        <div class="lt-stat-card">
          <div class="lt-stat-val">9</div>
          <div class="lt-stat-lbl">Modules</div>
        </div>
        <div class="lt-stat-card">
          <div class="lt-stat-val">24/7</div>
          <div class="lt-stat-lbl">Uptime</div>
        </div>
        <div class="lt-stat-card">
          <div class="lt-stat-val">AES</div>
          <div class="lt-stat-lbl">Encrypt</div>
        </div>
      </div>

      <div class="lt-status">
        <div class="lt-status-item">
          <div class="lt-status-dot"></div>
          <span class="lt-status-lbl">System</span>
        </div>
        <div class="lt-status-item">
          <div class="lt-status-dot blue"></div>
          <span class="lt-status-lbl">API</span>
        </div>
        <div class="lt-status-item">
          <div class="lt-status-dot amber"></div>
          <span class="lt-status-lbl">DB</span>
        </div>
      </div>
    </div>

    <!-- ═══ 右侧登录面板 ═══ -->
    <div class="lt-login-panel">
      <div class="lt-card">
        <div class="lt-card-body">
          <div class="lt-card-header">
            <h2 class="lt-card-title">欢迎回来</h2>
            <p class="lt-card-sub">选择身份并验证以继续访问系统</p>
          </div>

          <!-- Apple Segmented Control -->
          <div class="lt-segmented">
            <div class="lt-seg-pill moved" :data-pos="String(roleIndex)"></div>
            <button
              v-for="(r, i) in roles"
              :key="r.value"
              class="lt-seg-btn"
              :class="{ active: form.role === r.value }"
              @click="form.role = r.value"
              type="button"
            >
              <span class="lt-seg-icon">
                <span v-if="r.value === 'ADMIN'">⚙️</span>
                <span v-else-if="r.value === 'COACH'">🏋️</span>
                <span v-else>💪</span>
              </span>
              {{ r.label }}
            </button>
          </div>

          <!-- Username -->
          <div class="lt-field-wrap" :class="{ focused: focusField === 'username' }">
            <label class="lt-field-label">Username</label>
            <div class="lt-field" :class="{ focused: focusField === 'username' }">
              <span class="lt-field-icon">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 4-7 8-7s8 3 8 7"/>
                </svg>
              </span>
              <input
                v-model="form.username"
                type="text"
                placeholder="请输入用户名"
                autocomplete="username"
                @focus="focusField = 'username'"
                @blur="focusField = ''"
              />
            </div>
          </div>

          <!-- Password -->
          <div class="lt-field-wrap" :class="{ focused: focusField === 'password' }">
            <label class="lt-field-label">Password</label>
            <div class="lt-field" :class="{ focused: focusField === 'password' }">
              <span class="lt-field-icon">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V7a4 4 0 018 0v4"/>
                </svg>
              </span>
              <input
                v-model="form.password"
                :type="showPwd ? 'text' : 'password'"
                placeholder="请输入密码"
                autocomplete="current-password"
                @focus="focusField = 'password'"
                @blur="focusField = ''"
                @keyup.enter="submit"
              />
              <button class="lt-pwd-btn" @click="showPwd = !showPwd" type="button">
                <svg v-if="!showPwd" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94"/>
                  <path d="M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19"/>
                  <line x1="1" y1="1" x2="23" y2="23"/>
                </svg>
              </button>
            </div>
          </div>

          <div class="lt-link-row">
            <a href="javascript:void(0)" @click="forgotDialogVisible = true" class="lt-link-dim">忘记密码？</a>
          </div>

          <!-- Submit -->
          <button class="lt-submit-btn" :disabled="loading" @click="submit">
            <div class="lt-btn-shimmer"></div>
            <span class="lt-btn-text">{{ loading ? '验证中...' : '登 录' }}</span>
          </button>

          <div class="lt-switch-row">
            <span class="lt-text-dim">还没有账号？</span>
            <a href="javascript:void(0)" class="lt-link-accent" @click="$router.push('/register')">立即注册</a>
          </div>
        </div>
      </div>

      <div class="lt-footer">
        <div class="lt-footer-dot"></div>
        <span class="lt-footer-text">System Online</span>
        <div class="lt-footer-dot"></div>
        <span class="lt-footer-text">v2.0</span>
        <div class="lt-footer-dot"></div>
        <span class="lt-footer-text">Encrypted</span>
      </div>
    </div>

    <!-- ═══ 忘记密码弹窗 ═══ -->
    <el-dialog v-model="forgotDialogVisible" title="重置密码" width="440px" class="lt-dialog" :append-to-body="true">
      <el-form :model="forgotForm" label-position="top">
        <el-form-item label="邮箱">
          <el-input v-model="forgotForm.email" placeholder="请输入注册时的邮箱" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="lt-code-row">
            <el-input v-model="forgotForm.code" placeholder="请输入验证码" />
            <el-button type="primary" :disabled="codeCooldown > 0" @click="handleSendCode">
              {{ codeCooldown > 0 ? `${codeCooldown}s` : '获取验证码' }}
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
import { reactive, ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { login, sendCode, resetPasswordByCode, type UserRole } from '../api/auth';
import { useUserStore } from '../stores/user';

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const resetLoading = ref(false);
const forgotDialogVisible = ref(false);
const codeCooldown = ref(0);
const showPwd = ref(false);
const focusField = ref('');
const bgCanvas = ref<HTMLCanvasElement | null>(null);
let animFrame = 0;

const roles = [
  { label: '管理员', value: 'ADMIN' },
  { label: '教练', value: 'COACH' },
  { label: '会员', value: 'MEMBER' },
] as const;

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

const roleIndex = computed(() => roles.findIndex(r => r.value === form.role));

/* ─── Canvas: Blue Telemetry Net (Light Theme) ─── */
interface TPoint {
  x: number; y: number;
  vx: number; vy: number;
  r: number;
  phase: number;
  speed: number;
  alpha: number;
}

function initCanvas() {
  const canvas = bgCanvas.value;
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  if (!ctx) return;

  let w = 0, h = 0;
  const pts: TPoint[] = [];
  const N = 50;
  const LINK = 150;
  let t = 0;

  function resize() {
    const dpr = window.devicePixelRatio || 1;
    w = window.innerWidth;
    h = window.innerHeight;
    canvas!.width = w * dpr;
    canvas!.height = h * dpr;
    canvas!.style.width = w + 'px';
    canvas!.style.height = h + 'px';
    ctx!.setTransform(dpr, 0, 0, dpr, 0, 0);
  }

  function seed() {
    pts.length = 0;
    for (let i = 0; i < N; i++) {
      pts.push({
        x: Math.random() * w,
        y: Math.random() * h,
        vx: (Math.random() - 0.5) * 0.12,
        vy: (Math.random() - 0.5) * 0.12,
        r: Math.random() * 1.2 + 0.4,
        phase: Math.random() * Math.PI * 2,
        speed: Math.random() * 0.002 + 0.0008,
        alpha: Math.random() * 0.15 + 0.04,
      });
    }
  }

  function draw() {
    if (!ctx) return;
    ctx.clearRect(0, 0, w, h);
    t += 0.016;

    // Update — gentle Artemis orbital drift
    for (const p of pts) {
      p.phase += p.speed;
      p.x += p.vx + Math.sin(p.phase) * 0.06;
      p.y += p.vy + Math.cos(p.phase * 0.7) * 0.04;
      if (p.x < -20) p.x = w + 20;
      if (p.x > w + 20) p.x = -20;
      if (p.y < -20) p.y = h + 20;
      if (p.y > h + 20) p.y = -20;
      p.alpha = 0.04 + Math.sin(t * 0.4 + p.phase) * 0.03;
    }

    // Connections — very subtle blue net
    ctx.lineWidth = 0.4;
    for (let i = 0; i < pts.length; i++) {
      for (let j = i + 1; j < pts.length; j++) {
        const dx = pts[i].x - pts[j].x;
        const dy = pts[i].y - pts[j].y;
        const d = Math.sqrt(dx * dx + dy * dy);
        if (d < LINK) {
          const a = (1 - d / LINK) * 0.04;
          ctx.strokeStyle = `rgba(37, 99, 235, ${a})`;
          ctx.beginPath();
          ctx.moveTo(pts[i].x, pts[i].y);
          ctx.lineTo(pts[j].x, pts[j].y);
          ctx.stroke();
        }
      }
    }

    // Dots
    for (const p of pts) {
      ctx.fillStyle = `rgba(37, 99, 235, ${p.alpha})`;
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
      ctx.fill();
    }

    // F1 flow lines — barely visible sweeping curves
    const fa = 0.02 + Math.sin(t * 0.25) * 0.008;
    ctx.strokeStyle = `rgba(37, 99, 235, ${fa})`;
    ctx.lineWidth = 0.6;
    ctx.beginPath();
    for (let x = 0; x < w; x += 5) {
      const y = h * 0.3 + Math.sin(x * 0.003 + t * 0.18) * 70 + Math.sin(x * 0.007 + t * 0.12) * 35;
      if (x === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
    }
    ctx.stroke();

    ctx.strokeStyle = `rgba(37, 99, 235, ${fa * 0.6})`;
    ctx.beginPath();
    for (let x = 0; x < w; x += 5) {
      const y = h * 0.65 + Math.sin(x * 0.004 + t * 0.15 + 1) * 50 + Math.cos(x * 0.005 + t * 0.1) * 30;
      if (x === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
    }
    ctx.stroke();

    animFrame = requestAnimationFrame(draw);
  }

  resize();
  seed();
  draw();

  const onResize = () => { resize(); seed(); };
  window.addEventListener('resize', onResize);
}

onMounted(initCanvas);
onBeforeUnmount(() => { if (animFrame) cancelAnimationFrame(animFrame); });

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
