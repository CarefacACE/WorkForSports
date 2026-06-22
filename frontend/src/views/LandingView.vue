<template>
  <div class="landing-page">
    <!-- ====== Header ====== -->
    <header class="ud-header" :class="{ sticky: isSticky }">
      <div class="container">
        <div class="row">
          <div class="col-lg-12">
            <nav class="navbar navbar-expand-lg">
              <a class="navbar-brand" href="/">
                <img src="/landing/images/logo/logo.svg" alt="Logo" />
              </a>
              <button class="navbar-toggler" :class="{ active: mobileMenuOpen }" @click="mobileMenuOpen = !mobileMenuOpen">
                <span class="toggler-icon"> </span>
                <span class="toggler-icon"> </span>
                <span class="toggler-icon"> </span>
              </button>

              <div class="navbar-collapse" :class="{ show: mobileMenuOpen }">
                <ul id="nav" class="navbar-nav mx-auto">
                  <li class="nav-item">
                    <a class="ud-menu-scroll" :class="{ active: activeSection === 'home' }" href="#home" @click.prevent="scrollTo('home')">Home</a>
                  </li>
                  <li class="nav-item">
                    <a class="ud-menu-scroll" :class="{ active: activeSection === 'about' }" href="#about" @click.prevent="scrollTo('about')">About</a>
                  </li>
                  <li class="nav-item">
                    <a class="ud-menu-scroll" :class="{ active: activeSection === 'pricing' }" href="#pricing" @click.prevent="scrollTo('pricing')">Pricing</a>
                  </li>
                  <li class="nav-item">
                    <a class="ud-menu-scroll" :class="{ active: activeSection === 'team' }" href="#team" @click.prevent="scrollTo('team')">Team</a>
                  </li>
                  <li class="nav-item">
                    <a class="ud-menu-scroll" :class="{ active: activeSection === 'contact' }" href="#contact" @click.prevent="scrollTo('contact')">Contact</a>
                  </li>
                </ul>
              </div>

              <div class="navbar-btn d-none d-sm-inline-block">
                <router-link to="/login" class="ud-main-btn ud-login-btn">
                  Sign In
                </router-link>
                <router-link to="/login" class="ud-main-btn ud-white-btn">
                  Sign Up
                </router-link>
              </div>
            </nav>
          </div>
        </div>
      </div>
    </header>

    <!-- ====== Hero ====== -->
    <section class="ud-hero" id="home">
      <div class="container">
        <div class="row">
          <div class="col-lg-12">
            <div class="ud-hero-content wow fadeInUp" data-wow-delay=".2s">
              <h1 class="ud-hero-title">
                智训业财云 — 一站式健身房智能管理平台
              </h1>
              <p class="ud-hero-desc">
                集健康数据分析、定制课程、AI 训练计划、财务结算于一体的新一代健身 ERP 系统。为教练、会员、管理员打造全链路数字化体验。
              </p>
              <ul class="ud-hero-buttons">
                <li>
                  <router-link to="/login" class="ud-main-btn ud-white-btn">
                    立即体验
                  </router-link>
                </li>
                <li>
                  <a href="#features" class="ud-main-btn ud-link-btn" @click.prevent="scrollTo('features')">
                    了解更多 <i class="lni lni-arrow-right"></i>
                  </a>
                </li>
              </ul>
            </div>
            <div class="ud-hero-brands-wrapper wow fadeInUp" data-wow-delay=".3s">
              <img src="/landing/images/hero/brand.svg" alt="brand" />
            </div>
            <div class="ud-hero-image wow fadeInUp" data-wow-delay=".25s">
              <img src="/landing/images/hero/hero-image.svg" alt="hero-image" />
              <img src="/landing/images/hero/dotted-shape.svg" alt="shape" class="shape shape-1" />
              <img src="/landing/images/hero/dotted-shape.svg" alt="shape" class="shape shape-2" />
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ====== Features ====== -->
    <section id="features" class="ud-features">
      <div class="container">
        <div class="row">
          <div class="col-lg-12">
            <div class="ud-section-title">
              <span>产品亮点</span>
              <h2>智训核心功能</h2>
              <p>从数据分析到财务结算，覆盖健身房运营全链路，让管理更高效、训练更科学、体验更出色。</p>
            </div>
          </div>
        </div>
        <div class="row">
          <div class="col-xl-3 col-lg-3 col-sm-6" v-for="(feature, i) in features" :key="i">
            <div class="ud-single-feature wow fadeInUp" :data-wow-delay="`${0.1 + i * 0.05}s`">
              <div class="ud-feature-icon">
                <i :class="`lni ${feature.icon}`"></i>
              </div>
              <div class="ud-feature-content">
                <h3 class="ud-feature-title">{{ feature.title }}</h3>
                <p class="ud-feature-desc">{{ feature.desc }}</p>
                <a href="javascript:void(0)" class="ud-feature-link">Learn More</a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ====== About ====== -->
    <section id="about" class="ud-about">
      <div class="container">
        <div class="ud-about-wrapper wow fadeInUp" data-wow-delay=".2s">
          <div class="ud-about-content-wrapper">
            <div class="ud-about-content">
              <span class="tag">关于我们</span>
              <h2>为健身行业量身打造的数字化解决方案</h2>
              <p>智训业财云专注于解决健身房运营中的核心痛点：会员健康追踪难、课程管理混乱、财务结算繁琐。我们通过数据分析与智能推荐，让每一位会员都能获得个性化的训练体验。</p>
              <p>系统支持管理员、教练、会员三种角色，内置实时聊天、消息通知、健康档案、选课系统等功能模块，实现健身房全业务线上化闭环。</p>
              <a href="javascript:void(0)" class="ud-main-btn">了解更多</a>
            </div>
          </div>
          <div class="ud-about-image">
            <img src="/landing/images/about/about-image.svg" alt="about-image" />
          </div>
        </div>
      </div>
    </section>

    <!-- ====== Pricing ====== -->
    <section id="pricing" class="ud-pricing">
      <div class="container">
        <div class="row">
          <div class="col-lg-12">
            <div class="ud-section-title mx-auto text-center">
              <span>价格方案</span>
              <h2>灵活定价，按需选择</h2>
              <p>从个人健身房到连锁健身集团，我们提供多种方案满足不同规模的需求。</p>
            </div>
          </div>
        </div>
        <div class="row g-0 align-items-center justify-content-center">
          <div class="col-lg-4 col-md-6 col-sm-10" v-for="(plan, i) in pricingPlans" :key="i">
            <div class="ud-single-pricing wow fadeInUp"
                 :class="{ 'first-item': i === 0, active: i === 1, 'last-item': i === 2 }"
                 :data-wow-delay="i === 1 ? '.1s' : '.15s'">
              <span v-if="i === 1" class="ud-popular-tag">POPULAR</span>
              <div class="ud-pricing-header">
                <h3>{{ plan.priceLabel }}</h3>
                <h4>¥ {{ plan.price }}/月</h4>
              </div>
              <div class="ud-pricing-body">
                <ul>
                  <li v-for="(item, j) in plan.items" :key="j">{{ item }}</li>
                </ul>
              </div>
              <div class="ud-pricing-footer">
                <a href="javascript:void(0)" class="ud-main-btn" :class="i === 1 ? 'ud-white-btn' : 'ud-border-btn'">
                  立即开通
                </a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ====== FAQ ====== -->
    <section id="faq" class="ud-faq">
      <div class="shape">
        <img src="/landing/images/faq/shape.svg" alt="shape" />
      </div>
      <div class="container">
        <div class="row">
          <div class="col-lg-12">
            <div class="ud-section-title text-center mx-auto">
              <span>常见问题</span>
              <h2>有疑问？这里为你解答</h2>
              <p>以下是用户最关心的问题，如有更多疑问欢迎联系我们。</p>
            </div>
          </div>
        </div>
        <div class="row">
          <div class="col-lg-6" v-for="(col, colIdx) in faqColumns" :key="colIdx">
            <div class="ud-single-faq wow fadeInUp" v-for="(item, i) in col" :key="i" :data-wow-delay="`${0.1 + i * 0.05}s`">
              <div class="accordion">
                <button class="ud-faq-btn" :class="{ collapsed: openFaq !== colIdx * 3 + i }" @click="toggleFaq(colIdx * 3 + i)">
                  <span class="icon flex-shrink-0">
                    <i class="lni lni-chevron-down"></i>
                  </span>
                  <span>{{ item.q }}</span>
                </button>
                <div class="accordion-collapse" :class="{ collapse: openFaq !== colIdx * 3 + i, show: openFaq === colIdx * 3 + i }">
                  <div class="ud-faq-body">{{ item.a }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ====== Testimonials ====== -->
    <section id="testimonials" class="ud-testimonials">
      <div class="container">
        <div class="row">
          <div class="col-lg-12">
            <div class="ud-section-title mx-auto text-center">
              <span>用户评价</span>
              <h2>听听他们怎么说</h2>
              <p>来自教练、会员和管理员的真实反馈，看看智训如何改变他们的健身体验。</p>
            </div>
          </div>
        </div>
        <div class="row">
          <div class="col-lg-4 col-md-6" v-for="(t, i) in testimonials" :key="i">
            <div class="ud-single-testimonial wow fadeInUp" :data-wow-delay="`${0.1 + i * 0.05}s`">
              <div class="ud-testimonial-ratings">
                <i class="lni lni-star-filled" v-for="s in 5" :key="s"></i>
              </div>
              <div class="ud-testimonial-content">
                <p>{{ t.content }}</p>
              </div>
              <div class="ud-testimonial-info">
                <div class="ud-testimonial-image">
                  <img :src="`/landing/images/testimonials/author-0${i + 1}.png`" alt="author" />
                </div>
                <div class="ud-testimonial-meta">
                  <h4>{{ t.name }}</h4>
                  <p>{{ t.role }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="row">
          <div class="col-lg-12">
            <div class="ud-brands wow fadeInUp" data-wow-delay=".2s">
              <div class="ud-title">
                <h6>受到众多健身房信赖</h6>
              </div>
              <div class="ud-brands-logo">
                <div class="ud-single-logo" v-for="brand in brands" :key="brand">
                  <img :src="`/landing/images/brands/${brand}.svg`" :alt="brand" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ====== Team ====== -->
    <section id="team" class="ud-team">
      <div class="container">
        <div class="row">
          <div class="col-lg-12">
            <div class="ud-section-title mx-auto text-center">
              <span>团队介绍</span>
              <h2>认识我们的团队</h2>
              <p>一群热爱健身与技术的年轻人，致力于打造最好的健身行业数字化平台。</p>
            </div>
          </div>
        </div>
        <div class="row">
          <div class="col-xl-3 col-lg-3 col-sm-6" v-for="(member, i) in teamMembers" :key="i">
            <div class="ud-single-team wow fadeInUp" :data-wow-delay="`${0.1 + i * 0.05}s`">
              <div class="ud-team-image-wrapper">
                <div class="ud-team-image">
                  <img :src="`/landing/images/team/team-0${i + 1}.png`" alt="team" />
                </div>
                <img src="/landing/images/team/dotted-shape.svg" alt="shape" class="shape shape-1" />
                <img src="/landing/images/team/shape-2.svg" alt="shape" class="shape shape-2" />
              </div>
              <div class="ud-team-info">
                <h5>{{ member.name }}</h5>
                <h6>{{ member.role }}</h6>
              </div>
              <ul class="ud-team-socials">
                <li><a href="javascript:void(0)"><i class="lni lni-facebook-filled"></i></a></li>
                <li><a href="javascript:void(0)"><i class="lni lni-twitter-filled"></i></a></li>
                <li><a href="javascript:void(0)"><i class="lni lni-instagram-filled"></i></a></li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ====== Contact ====== -->
    <section id="contact" class="ud-contact">
      <div class="container">
        <div class="row align-items-center">
          <div class="col-xl-8 col-lg-7">
            <div class="ud-contact-content-wrapper">
              <div class="ud-contact-title">
                <span>联系我们</span>
                <h2>联系我们<br />期待与您沟通！</h2>
              </div>
              <div class="ud-contact-info-wrapper">
                <div class="ud-single-info">
                  <div class="ud-info-icon"><i class="lni lni-map-marker"></i></div>
                  <div class="ud-info-meta">
                    <h5>公司地址</h5>
                    <p>北京市海淀区中关村科技园 A 座 12 层</p>
                  </div>
                </div>
                <div class="ud-single-info">
                  <div class="ud-info-icon"><i class="lni lni-envelope"></i></div>
                  <div class="ud-info-meta">
                    <h5>联系方式</h5>
                    <p>contact@zhixun.fit</p>
                    <p>support@zhixun.fit</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="col-xl-4 col-lg-5">
            <div class="ud-contact-form-wrapper wow fadeInUp" data-wow-delay=".2s">
              <h3 class="ud-contact-form-title">给我们留言</h3>
              <form class="ud-contact-form" @submit.prevent>
                <div class="ud-form-group">
                  <label for="fullName">姓名*</label>
                  <input type="text" name="fullName" placeholder="请输入您的姓名" />
                </div>
                <div class="ud-form-group">
                  <label for="email">邮箱*</label>
                  <input type="email" name="email" placeholder="example@yourmail.com" />
                </div>
                <div class="ud-form-group">
                  <label for="phone">电话*</label>
                  <input type="text" name="phone" placeholder="+86 138 0000 0000" />
                </div>
                <div class="ud-form-group">
                  <label for="message">留言*</label>
                  <textarea name="message" rows="1" placeholder="请输入您的留言内容"></textarea>
                </div>
                <div class="ud-form-group mb-0">
                  <button type="submit" class="ud-main-btn">发送消息</button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ====== Footer ====== -->
    <footer class="ud-footer wow fadeInUp" data-wow-delay=".15s">
      <div class="shape shape-1"><img src="/landing/images/footer/shape-1.svg" alt="shape" /></div>
      <div class="shape shape-2"><img src="/landing/images/footer/shape-2.svg" alt="shape" /></div>
      <div class="shape shape-3"><img src="/landing/images/footer/shape-3.svg" alt="shape" /></div>
      <div class="ud-footer-widgets">
        <div class="container">
          <div class="row">
            <div class="col-xl-3 col-lg-4 col-md-6">
              <div class="ud-widget">
                <a href="/" class="ud-footer-logo">
                  <img src="/landing/images/logo/logo.svg" alt="logo" />
                </a>
                <p class="ud-widget-desc">集健康数据分析、定制课程、AI 训练计划、财务结算于一体的新一代健身 ERP 系统。</p>
                <ul class="ud-widget-socials">
                  <li><a href="javascript:void(0)"><i class="lni lni-facebook-filled"></i></a></li>
                  <li><a href="javascript:void(0)"><i class="lni lni-twitter-filled"></i></a></li>
                  <li><a href="javascript:void(0)"><i class="lni lni-instagram-filled"></i></a></li>
                  <li><a href="javascript:void(0)"><i class="lni lni-linkedin-original"></i></a></li>
                </ul>
              </div>
            </div>
            <div class="col-xl-2 col-lg-2 col-md-6 col-sm-6">
              <div class="ud-widget">
                <h5 class="ud-widget-title">产品服务</h5>
                <ul class="ud-widget-links">
                  <li><a href="javascript:void(0)">数据分析</a></li>
                  <li><a href="javascript:void(0)">课程管理</a></li>
                  <li><a href="javascript:void(0)">AI 训练计划</a></li>
                  <li><a href="javascript:void(0)">财务结算</a></li>
                </ul>
              </div>
            </div>
            <div class="col-xl-2 col-lg-3 col-md-6 col-sm-6">
              <div class="ud-widget">
                <h5 class="ud-widget-title">功能模块</h5>
                <ul class="ud-widget-links">
                  <li><a href="javascript:void(0)">健康档案</a></li>
                  <li><a href="javascript:void(0)">实时聊天</a></li>
                  <li><a href="javascript:void(0)">选课系统</a></li>
                  <li><a href="javascript:void(0)">消息通知</a></li>
                </ul>
              </div>
            </div>
            <div class="col-xl-2 col-lg-3 col-md-6 col-sm-6">
              <div class="ud-widget">
                <h5 class="ud-widget-title">支持</h5>
                <ul class="ud-widget-links">
                  <li><a href="javascript:void(0)">帮助中心</a></li>
                  <li><a href="javascript:void(0)">隐私政策</a></li>
                  <li><a href="javascript:void(0)">服务条款</a></li>
                  <li><a href="javascript:void(0)">退款政策</a></li>
                </ul>
              </div>
            </div>
            <div class="col-xl-3 col-lg-6 col-md-8 col-sm-10">
              <div class="ud-widget">
                <h5 class="ud-widget-title">合作生态</h5>
                <ul class="ud-widget-brands">
                  <li v-for="brand in footerBrands" :key="brand">
                    <a :href="`https://${brand}.com/`" rel="nofollow noopener" target="_blank">
                      <img :src="`/landing/images/footer/brands/${brand}.svg`" :alt="brand" />
                    </a>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="ud-footer-bottom">
        <div class="container">
          <div class="row">
            <div class="col-md-8">
              <ul class="ud-footer-bottom-left">
                <li><a href="javascript:void(0)">隐私政策</a></li>
                <li><a href="javascript:void(0)">服务协议</a></li>
                <li><a href="javascript:void(0)">服务条款</a></li>
              </ul>
            </div>
            <div class="col-md-4">
              <p class="ud-footer-bottom-right">
                © 2026 智训业财云 All Rights Reserved
              </p>
            </div>
          </div>
        </div>
      </div>
    </footer>

    <!-- ====== Back To Top ====== -->
    <a href="javascript:void(0)" class="back-to-top" :class="{ active: showBackToTop }" @click.prevent="scrollToTop">
      <i class="lni lni-chevron-up"> </i>
    </a>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';

// ====== Data ======
const features = [
  { icon: 'lni-bar-chart', title: '数据分析', desc: '上传运动数据 CSV，自动识别数值列，生成心率区间、均值、极值等多维分析报表，让训练效果一目了然。' },
  { icon: 'lni-book', title: '定制课程', desc: '教练可创建公共课与私教课，灵活设置课时、试听、视频教学。会员一键选课、付费、确认，全流程线上化。' },
  { icon: 'lni-brain', title: 'AI 训练计划', desc: '基于会员健康档案（身高、体重、体脂率、血压等），智能推荐训练目标与周期计划，助力科学健身。' },
  { icon: 'lni-wallet', title: '财务结算', desc: '内置钱包充值、消费、提现功能，教练工资自动核算，管理员实时掌控资金流水，账目清晰透明。' },
];

const pricingPlans = [
  { price: '0', priceLabel: '基础版', items: ['50 人用户数', '基础分析报表', '公共课管理', '群聊功能', '社区支持', '—'] },
  { price: '99', priceLabel: '专业版', items: ['500 人用户数', '高级分析 + 心率区间', '公共课 + 私教', '群聊 + 私聊', '在线工单支持', '基础 AI 推荐'] },
  { price: '299', priceLabel: '旗舰版', items: ['无限制用户数', '全功能 + API 对接', '全课程类型', '全聊天功能', '专属客服', '深度 AI 定制'] },
];

const faqItems = [
  { q: '智训业财云支持哪些角色？', a: '系统支持管理员、教练、会员三种角色，每种角色拥有独立的菜单和权限体系。管理员可管理所有数据，教练管理自己的课程和学员，会员选课和查看个人健康档案。' },
  { q: '健康数据如何分析？', a: '会员可上传运动数据的 CSV 文件，系统自动识别数值列，计算均值、极值等统计指标。对于心率数据，还会自动划分热身区、燃脂区、有氧区、无氧区、极限区五个心率区间。' },
  { q: '可以定制训练计划吗？', a: '会员可设置目标体重、目标体脂率、目标肌肉量、期望达成日期等目标信息。系统根据健康档案和目标数据，辅助教练为会员制定个性化的训练方案。' },
  { q: '财务结算怎么运作？', a: '系统内置钱包功能，会员可充值和消费。教练工资根据课时和学员确认自动核算。管理员可查看全平台交易流水和余额概览，所有账目实时透明。' },
  { q: '支持实时沟通吗？', a: '支持。系统内置群聊和私聊功能，教练和会员可以在课程群组中交流，也可以发起一对一私聊。还支持好友申请、群公告、未读消息提醒等功能。' },
  { q: '数据安全如何保障？', a: '系统采用逻辑删除机制保护数据不被误删，所有敏感操作记录审计日志。数据库支持 MariaDB，可配合备份策略确保数据安全。' },
];

const faqColumns = [faqItems.slice(0, 3), faqItems.slice(3, 6)];

const testimonials = [
  { content: '"智训系统让我们的课程管理效率提升了 3 倍，会员选课、付费全部线上化，再也不用手动对账了。"', name: '张教练', role: '资深健身教练' },
  { content: '"健康档案和数据分析功能太实用了，每次训练后上传数据就能看到心率区间和运动趋势，训练更有方向了。"', name: '李小姐', role: '健身会员' },
  { content: '"作为健身房老板，财务结算和教练工资核算一直是头疼的问题。智训帮我实现了全自动结算，账目一目了然。"', name: '王总', role: '健身房管理员' },
];

const brands: string[] = [];
const footerBrands: string[] = [];

const teamMembers = [
  { name: '陈志远', role: '产品经理' },
  { name: '林小雨', role: '前端工程师' },
  { name: '赵明辉', role: '后端工程师' },
  { name: '周婷婷', role: 'UI 设计师' },
];

// ====== State ======
const isSticky = ref(false);
const mobileMenuOpen = ref(false);
const activeSection = ref('home');
const openFaq = ref(-1);
const showBackToTop = ref(false);

// ====== Methods ======
function scrollTo(id: string) {
  const el = document.getElementById(id);
  if (el) {
    el.scrollIntoView({ behavior: 'smooth' });
    mobileMenuOpen.value = false;
  }
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function toggleFaq(index: number) {
  openFaq.value = openFaq.value === index ? -1 : index;
}

function handleScroll() {
  // Sticky header
  isSticky.value = window.scrollY > 50;

  // Back to top
  showBackToTop.value = window.scrollY > 300;

  // Active section tracking
  const sectionIds = ['home', 'about', 'pricing', 'team', 'contact'];
  const scrollPos = window.scrollY + 73;

  for (const id of sectionIds) {
    const el = document.getElementById(id);
    if (el && el.offsetTop <= scrollPos && el.offsetTop + el.offsetHeight > scrollPos) {
      activeSection.value = id;
      break;
    }
  }
}

// ====== Scroll animations (IntersectionObserver, replaces WOW.js) ======
let observer: IntersectionObserver | null = null;

function initScrollAnimations() {
  const elements = document.querySelectorAll('.wow');
  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          const el = entry.target as HTMLElement;
          const delay = el.getAttribute('data-wow-delay') || '0s';
          el.style.animationDelay = delay;
          el.classList.add('animated', 'fadeInUp');
          el.style.visibility = 'visible';
          observer?.unobserve(el);
        }
      });
    },
    { threshold: 0.1 }
  );

  elements.forEach((el) => {
    (el as HTMLElement).style.visibility = 'hidden';
    observer?.observe(el);
  });
}

// ====== Lifecycle ======
onMounted(() => {
  window.addEventListener('scroll', handleScroll);
  handleScroll();
  initScrollAnimations();
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
  observer?.disconnect();
});
</script>

<style>
/* Landing page styles - loaded from public directory */
@import '/landing/css/bootstrap.min.css';
@import '/landing/css/animate.css';
@import '/landing/css/lineicons.css';
@import '/landing/css/ud-styles.css';

/* Fix: ensure landing page styles don't leak to other routes */
.landing-page .back-to-top {
  transition: opacity 0.3s;
  opacity: 0;
  pointer-events: none;
}
.landing-page .back-to-top.active {
  opacity: 1;
  pointer-events: auto;
}

/* Animate.css integration */
.wow {
  visibility: hidden;
}
.wow.animated {
  visibility: visible;
}
</style>
