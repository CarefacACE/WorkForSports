<template>
  <div class="lp-page">
    <!-- ═══ Navbar ═══ -->
    <LandingNavbar />

    <main class="lp-main">
      <!-- ═══ Hero Section ═══ -->
      <section class="lp-hero">
        <div class="lp-container lp-hero-grid">
          <!-- Left Column -->
          <div class="lp-hero-left">
            <BackgroundLines class="lp-hero-bg">
              <div class="lp-hero-content">
                <h1 class="lp-hero-title">
                  面向体育训练与健身行业的
                  <br />
                  <ColourfulText text="智能管理平台" />
                </h1>
                <p class="lp-hero-sub">
                  课程管理 · 财务结算 · 实时通讯 · 数据分析，一体化解决方案。助力体育培训机构高效运营。
                </p>

                <!-- CTA -->
                <div class="lp-hero-cta">
                  <router-link to="/login" class="lp-btn-primary">
                    快速开始
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" class="lp-btn-icon">
                      <path d="M5 12h14M12 5l7 7-7 7"/>
                    </svg>
                  </router-link>
                  <a href="#features" class="lp-btn-ghost">了解更多</a>
                </div>

                <!-- Stats -->
                <div class="lp-hero-stats">
                  <div class="lp-stat-item">
                    <span class="lp-stat-num">500+</span>
                    <span class="lp-stat-lbl">教练信赖</span>
                  </div>
                  <div class="lp-stat-divider" />
                  <div class="lp-stat-item">
                    <span class="lp-stat-num">10K+</span>
                    <span class="lp-stat-lbl">学员管理</span>
                  </div>
                  <div class="lp-stat-divider" />
                  <div class="lp-stat-item">
                    <span class="lp-stat-num">99.9%</span>
                    <span class="lp-stat-lbl">系统稳定</span>
                  </div>
                </div>
              </div>
            </BackgroundLines>
          </div>

          <!-- Right Column (Desktop) -->
          <div class="lp-hero-right">
            <div class="lp-cards-grid">
              <GlowingEffect v-for="card in heroCards" :key="card.title" :glow="true" :spread="40" :proximity="64" class="lp-glow-card">
                <div class="lp-card-inner">
                  <div class="lp-card-icon" v-html="card.icon" />
                  <div>
                    <h3 class="lp-card-title">{{ card.title }}</h3>
                    <p class="lp-card-desc">{{ card.desc }}</p>
                  </div>
                </div>
              </GlowingEffect>
            </div>
          </div>
        </div>
      </section>

      <!-- ═══ Features Section ═══ -->
      <section id="features" class="lp-features">
        <div class="lp-container lp-features-row">
          <div v-for="f in features" :key="f.title" class="lp-feature-card">
            <div class="lp-feature-icon" v-html="f.icon" />
            <h3 class="lp-feature-title">{{ f.title }}</h3>
            <p class="lp-feature-desc">{{ f.desc }}</p>
          </div>
        </div>
      </section>

      <!-- ═══ Sponsors / Partners ═══ -->
      <section class="lp-sponsors">
        <div class="lp-container lp-sponsors-inner">
          <p class="lp-sponsors-label">合作伙伴</p>
          <div class="lp-sponsors-row">
            <span v-for="s in sponsors" :key="s" class="lp-sponsor-logo">{{ s }}</span>
          </div>
        </div>
      </section>

      <!-- ═══ Video / Demo Scroll ═══ -->
      <section id="solutions" class="lp-video">
        <ContainerScroll>
          <template #title>
            <h2 class="lp-video-title">
              观看演示，在
              <span class="lp-video-highlight">
                <ColourfulText text="5 分钟内" />
              </span>
              了解全部功能
            </h2>
          </template>
          <div class="lp-demo-placeholder">
            <svg viewBox="0 0 800 500" class="lp-demo-svg">
              <rect width="800" height="500" fill="#171717"/>
              <rect x="40" y="30" width="720" height="40" rx="8" fill="#262626"/>
              <circle cx="65" cy="50" r="8" fill="#ef4444"/>
              <circle cx="90" cy="50" r="8" fill="#f59e0b"/>
              <circle cx="115" cy="50" r="8" fill="#22c55e"/>
              <rect x="40" y="90" width="180" height="380" rx="8" fill="#1e1e1e"/>
              <rect x="240" y="90" width="520" height="380" rx="8" fill="#1e1e1e"/>
              <rect x="60" y="120" width="140" height="12" rx="4" fill="#333"/>
              <rect x="60" y="150" width="120" height="12" rx="4" fill="#333"/>
              <rect x="60" y="180" width="130" height="12" rx="4" fill="#333"/>
              <rect x="60" y="210" width="100" height="12" rx="4" fill="#333"/>
              <rect x="270" y="120" width="460" height="180" rx="8" fill="#222"/>
              <rect x="270" y="320" width="220" height="120" rx="8" fill="#222"/>
              <rect x="510" y="320" width="220" height="120" rx="8" fill="#222"/>
              <text x="400" y="220" text-anchor="middle" fill="#525252" font-size="24" font-family="Inter">智训 ERP Dashboard</text>
            </svg>
          </div>
        </ContainerScroll>
      </section>

      <!-- ═══ Testimonials ═══ -->
      <section id="testimonials" class="lp-testimonials">
        <div class="lp-container lp-testimonials-inner">
          <h2 class="lp-section-title">听听大家怎么说</h2>
          <p class="lp-section-desc">不要只听我们说。以下是真实用户对智训 ERP 的评价。</p>

          <div class="lp-marquee-wrap">
            <MarqueeRow :pause-on-hover="true" duration="30s">
              <div v-for="r in reviews.slice(0, 4)" :key="r.name" class="lp-review-card">
                <div class="lp-review-header">
                  <div class="lp-review-avatar">{{ r.name[0] }}</div>
                  <div>
                    <div class="lp-review-name">{{ r.name }}</div>
                    <div class="lp-review-role">{{ r.role }}</div>
                  </div>
                </div>
                <p class="lp-review-body">{{ r.body }}</p>
              </div>
            </MarqueeRow>
            <MarqueeRow :reverse="true" :pause-on-hover="true" duration="35s">
              <div v-for="r in reviews.slice(4)" :key="r.name" class="lp-review-card">
                <div class="lp-review-header">
                  <div class="lp-review-avatar">{{ r.name[0] }}</div>
                  <div>
                    <div class="lp-review-name">{{ r.name }}</div>
                    <div class="lp-review-role">{{ r.role }}</div>
                  </div>
                </div>
                <p class="lp-review-body">{{ r.body }}</p>
              </div>
            </MarqueeRow>
            <!-- Gradient fades -->
            <div class="lp-fade-left" />
            <div class="lp-fade-right" />
          </div>
        </div>
      </section>
    </main>

    <!-- ═══ Footer ═══ -->
    <footer class="lp-footer">
      <div class="lp-container lp-footer-inner">
        <div class="lp-footer-left">
          <svg viewBox="0 0 42 42" class="lp-footer-logo">
            <rect x="2" y="2" width="38" height="38" rx="10" fill="none" stroke="rgba(255,255,255,0.1)" stroke-width="1.5"/>
            <text x="21" y="28" text-anchor="middle" fill="#fafafa" font-family="Inter,sans-serif" font-size="16" font-weight="800">ZX</text>
          </svg>
          <span class="lp-footer-copy">© {{ new Date().getFullYear() }} 智训科技。保留所有权利。</span>
        </div>
        <div class="lp-footer-links">
          <a href="#features">功能</a>
          <a href="#solutions">方案</a>
          <a href="#testimonials">评价</a>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import LandingNavbar from '../components/landing/LandingNavbar.vue';
import BackgroundLines from '../components/landing/BackgroundLines.vue';
import ColourfulText from '../components/landing/ColourfulText.vue';
import ContainerScroll from '../components/landing/ContainerScroll.vue';
import GlowingEffect from '../components/landing/GlowingEffect.vue';
import MarqueeRow from '../components/landing/MarqueeRow.vue';

gsap.registerPlugin(ScrollTrigger);

/* ─── Hero Cards ─── */
const heroCards = [
  {
    title: '智能排课',
    desc: 'AI 驱动的排课引擎，自动避免冲突，优化教练与场地利用率。',
    icon: '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#a78bfa" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>',
  },
  {
    title: '实时数据',
    desc: '运动数据实时采集与分析，助力科学训练决策。',
    icon: '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#a78bfa" stroke-width="1.5"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>',
  },
  {
    title: '多端协作',
    desc: 'Web、移动端、小程序全覆盖，随时随地管理训练。',
    icon: '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#a78bfa" stroke-width="1.5"><rect x="5" y="2" width="14" height="20" rx="2"/><path d="M12 18h.01"/></svg>',
  },
];

/* ─── Features ─── */
const features = [
  {
    title: '课程管理',
    desc: '灵活的课程创建、排期与选课系统，支持团课、私教等多种模式。',
    icon: '<svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="#a78bfa" stroke-width="1.5"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>',
  },
  {
    title: '财务结算',
    desc: '钱包充值、提现、工资结算，全流程自动化财务管理。',
    icon: '<svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="#a78bfa" stroke-width="1.5"><rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20"/></svg>',
  },
  {
    title: '即时通讯',
    desc: '内置群聊与私聊，教练、学员、管理员无缝沟通协作。',
    icon: '<svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="#a78bfa" stroke-width="1.5"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>',
  },
  {
    title: '数据分析',
    desc: 'CSV 上传智能分析，可视化图表一键生成，数据驱动决策。',
    icon: '<svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="#a78bfa" stroke-width="1.5"><path d="M18 20V10M12 20V4M6 20v-6"/></svg>',
  },
];

/* ─── Sponsors ─── */
const sponsors = ['阿里云', '腾讯云', '华为云', '钉钉', '企业微信'];

/* ─── Reviews ─── */
const reviews = [
  { name: '张伟', role: '健身教练', body: '用了智训 ERP 之后，排课效率提升了 3 倍，再也不用手动排课了。' },
  { name: '李芳', role: '培训机构校长', body: '财务管理模块非常强大，工资结算从原来的两天缩短到几分钟。' },
  { name: '王磊', role: '瑜伽教练', body: '学员管理一目了然，课程提醒功能让学员出勤率提升了 40%。' },
  { name: '陈静', role: '游泳馆经理', body: '多端同步非常好用，随时随地查看运营数据，非常放心。' },
  { name: '赵明', role: '篮球训练营', body: '即时通讯功能让教练和家长的沟通变得轻松高效，非常满意。' },
  { name: '刘洋', role: '体能训练师', body: '数据分析功能帮助我科学制定训练计划，学员进步明显。' },
  { name: '孙婷', role: '舞蹈工作室', body: '界面简洁美观，操作简单，团队上手非常快，强烈推荐。' },
  { name: '周涛', role: '搏击俱乐部', body: '系统稳定性非常好，用了半年多从未出过问题，值得信赖。' },
];

/* ─── Entrance Animations ─── */
onMounted(() => {
  const tl = gsap.timeline({ defaults: { ease: 'power3.out' } });

  tl.from('.lp-hero-title', { autoAlpha: 0, y: 30, duration: 0.8 }, 0.2)
    .from('.lp-hero-sub', { autoAlpha: 0, y: 20, duration: 0.6 }, 0.5)
    .from('.lp-hero-cta', { autoAlpha: 0, y: 15, duration: 0.5 }, 0.7)
    .from('.lp-hero-stats', { autoAlpha: 0, y: 10, duration: 0.5 }, 0.9);

  // Features section
  gsap.from('.lp-feature-card', {
    scrollTrigger: { trigger: '.lp-features', start: 'top 80%' },
    autoAlpha: 0,
    y: 30,
    stagger: 0.12,
    duration: 0.6,
  });

  // Testimonials section
  gsap.from('.lp-section-title', {
    scrollTrigger: { trigger: '.lp-testimonials', start: 'top 80%' },
    autoAlpha: 0,
    y: 20,
    duration: 0.6,
  });
  gsap.from('.lp-section-desc', {
    scrollTrigger: { trigger: '.lp-testimonials', start: 'top 80%' },
    autoAlpha: 0,
    y: 15,
    duration: 0.6,
    delay: 0.15,
  });
});
</script>

<style scoped>
/* ─── Page ─── */
.lp-page {
  min-height: 100vh;
  background: #0a0a0a;
  color: #fafafa;
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif;
  -webkit-font-smoothing: antialiased;
}
.lp-main {
  flex: 1;
}
.lp-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

/* ─── Hero ─── */
.lp-hero {
  position: relative;
  overflow: hidden;
}
.lp-hero-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 2.5rem;
  padding-top: 2rem;
  padding-bottom: 4rem;
}
@media (min-width: 1280px) {
  .lp-hero-grid {
    grid-template-columns: 1fr 1fr;
    padding-top: 4rem;
    padding-bottom: 6rem;
  }
}
.lp-hero-left {
  position: relative;
}
.lp-hero-bg {
  min-height: 100%;
}
.lp-hero-content {
  display: flex;
  flex-direction: column;
  padding-top: 2rem;
}
@media (min-width: 768px) {
  .lp-hero-content { padding-top: 6rem; }
}
.lp-hero-title {
  font-size: 2.5rem;
  font-weight: 700;
  line-height: 1.15;
  margin-bottom: 1.5rem;
  color: #fafafa;
}
@media (min-width: 768px) {
  .lp-hero-title { font-size: 3.25rem; line-height: 4rem; }
}
.lp-hero-sub {
  font-size: 1.05rem;
  color: #a3a3a3;
  line-height: 1.75;
  max-width: 500px;
  margin-bottom: 1.5rem;
}

/* CTA */
.lp-hero-cta {
  display: flex;
  gap: 1rem;
  align-items: center;
  margin-bottom: 2rem;
  flex-wrap: wrap;
}
.lp-btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  height: 3rem;
  padding: 0 1.5rem;
  background: #2563eb;
  color: #fff;
  border-radius: 9999px;
  font-size: 1rem;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s;
  border: none;
  cursor: pointer;
}
.lp-btn-primary:hover { background: #1d4ed8; }
.lp-btn-icon { flex-shrink: 0; }
.lp-btn-ghost {
  display: inline-flex;
  align-items: center;
  height: 3rem;
  padding: 0 1.5rem;
  color: #a3a3a3;
  font-size: 1rem;
  font-weight: 500;
  text-decoration: none;
  border: 1px solid hsl(0, 0%, 20%);
  border-radius: 9999px;
  transition: all 0.2s;
}
.lp-btn-ghost:hover { color: #fafafa; border-color: hsl(0, 0%, 30%); }

/* Stats */
.lp-hero-stats {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}
.lp-stat-item {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.lp-stat-num {
  font-size: 1.25rem;
  font-weight: 700;
  color: #fafafa;
}
.lp-stat-lbl {
  font-size: 0.75rem;
  color: #737373;
  letter-spacing: 0.5px;
}
.lp-stat-divider {
  width: 1px;
  height: 2rem;
  background: hsl(0, 0%, 15%);
}

/* Right Column */
.lp-hero-right {
  display: none;
}
@media (min-width: 1280px) {
  .lp-hero-right { display: block; }
}
.lp-cards-grid {
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: auto;
  gap: 1rem;
  max-height: 34rem;
}
@media (min-width: 1280px) {
  .lp-cards-grid {
    grid-template-columns: repeat(2, 1fr);
    grid-template-rows: repeat(2, 1fr);
  }
  .lp-cards-grid > :first-child {
    grid-column: 1 / -1;
  }
}
.lp-glow-card {
  min-height: 12rem;
  border-radius: 1.5rem;
  border: 1px solid hsl(0, 0%, 12%);
  padding: 0.5rem;
}
.lp-card-inner {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 1.5rem;
  height: 100%;
  padding: 1.5rem;
  border-radius: 1rem;
  background: rgba(23, 23, 23, 0.6);
}
.lp-card-icon {
  width: fit-content;
  border-radius: 0.5rem;
  border: 1px solid hsl(0, 0%, 20%);
  padding: 0.5rem;
  display: flex;
}
.lp-card-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #fafafa;
  margin: 0;
}
.lp-card-desc {
  font-size: 0.875rem;
  color: #a3a3a3;
  line-height: 1.5;
  margin: 0.25rem 0 0;
}

/* ─── Features ─── */
.lp-features {
  position: relative;
  margin-top: -8rem;
  padding-bottom: 6rem;
}
@media (min-width: 768px) {
  .lp-features { margin-top: -12rem; }
}
.lp-features-row {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
@media (min-width: 640px) {
  .lp-features-row { flex-direction: row; }
}
.lp-feature-card {
  flex: 1;
  padding: 1.5rem;
  border-radius: 1.5rem;
  border: 1px solid hsl(0, 0%, 12%);
  background: rgba(23, 23, 23, 0.4);
}
.lp-feature-icon {
  width: 3rem;
  height: 3rem;
  border-radius: 9999px;
  background: rgba(167, 139, 250, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1rem;
}
.lp-feature-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #fafafa;
  margin: 0 0 0.5rem;
}
.lp-feature-desc {
  font-size: 0.875rem;
  color: #a3a3a3;
  line-height: 1.65;
  margin: 0;
}

/* ─── Sponsors ─── */
.lp-sponsors {
  padding: 3rem 0;
}
.lp-sponsors-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
}
.lp-sponsors-label {
  font-size: 0.8rem;
  letter-spacing: 2px;
  text-transform: uppercase;
  color: #525252;
  margin: 0;
}
.lp-sponsors-row {
  display: flex;
  align-items: center;
  gap: 2.5rem;
  flex-wrap: wrap;
  justify-content: center;
}
.lp-sponsor-logo {
  font-size: 0.9rem;
  font-weight: 600;
  color: #404040;
  letter-spacing: 1px;
}

/* ─── Video Scroll ─── */
.lp-video {
  padding: 4rem 0;
}
.lp-video-title {
  font-size: 2rem;
  font-weight: 700;
  color: #fafafa;
  text-align: center;
  line-height: 1.3;
}
@media (min-width: 768px) {
  .lp-video-title { font-size: 3rem; }
}
.lp-video-highlight {
  display: inline;
}
.lp-demo-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.lp-demo-svg {
  width: 100%;
  height: 100%;
  border-radius: 0.75rem;
}

/* ─── Testimonials ─── */
.lp-testimonials {
  padding: 5rem 0;
  overflow: hidden;
}
.lp-testimonials-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.lp-section-title {
  font-size: 2rem;
  font-weight: 700;
  color: #fafafa;
  text-align: center;
  margin-bottom: 1rem;
}
@media (min-width: 768px) {
  .lp-section-title { font-size: 3rem; }
}
.lp-section-desc {
  font-size: 1.125rem;
  color: #a3a3a3;
  text-align: center;
  margin-bottom: 3rem;
}
.lp-marquee-wrap {
  position: relative;
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1rem 0;
}
.lp-review-card {
  flex-shrink: 0;
  width: 18rem;
  padding: 1rem;
  border-radius: 0.75rem;
  border: 1px solid hsl(0, 0%, 12%);
  background: rgba(23, 23, 23, 0.5);
  cursor: default;
  transition: background 0.2s;
}
.lp-review-card:hover {
  background: rgba(23, 23, 23, 0.8);
}
.lp-review-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}
.lp-review-avatar {
  width: 2rem;
  height: 2rem;
  border-radius: 9999px;
  background: #2563eb;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 600;
  flex-shrink: 0;
}
.lp-review-name {
  font-size: 0.85rem;
  font-weight: 600;
  color: #fafafa;
}
.lp-review-role {
  font-size: 0.7rem;
  color: #525252;
}
.lp-review-body {
  font-size: 0.85rem;
  color: #a3a3a3;
  line-height: 1.5;
  margin: 0;
}
.lp-fade-left,
.lp-fade-right {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 15%;
  pointer-events: none;
  z-index: 2;
}
.lp-fade-left {
  left: 0;
  background: linear-gradient(to right, #0a0a0a, transparent);
}
.lp-fade-right {
  right: 0;
  background: linear-gradient(to left, #0a0a0a, transparent);
}

/* ─── Footer ─── */
.lp-footer {
  border-top: 1px solid hsl(0, 0%, 12%);
  padding: 2.5rem 0;
}
.lp-footer-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}
@media (min-width: 768px) {
  .lp-footer-inner { flex-direction: row; }
}
.lp-footer-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.lp-footer-logo {
  width: 28px;
  height: 28px;
}
.lp-footer-copy {
  font-size: 0.8rem;
  color: #525252;
}
.lp-footer-links {
  display: flex;
  gap: 1.5rem;
}
.lp-footer-links a {
  font-size: 0.8rem;
  color: #525252;
  text-decoration: none;
  transition: color 0.2s;
}
.lp-footer-links a:hover {
  color: #a3a3a3;
}
</style>
