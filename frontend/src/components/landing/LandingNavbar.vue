<template>
  <header class="nav-header" :class="{ 'nav-scrolled': scrolled }" ref="headerRef">
    <div class="nav-container">
      <!-- Logo -->
      <router-link to="/" class="nav-logo">
        <svg viewBox="0 0 42 42" class="nav-logo-svg">
          <rect x="2" y="2" width="38" height="38" rx="10" fill="none" stroke="rgba(255,255,255,0.15)" stroke-width="1.5"/>
          <text x="21" y="28" text-anchor="middle" fill="#fafafa" font-family="Inter,sans-serif" font-size="16" font-weight="800">ZX</text>
        </svg>
        <span class="nav-logo-text">智训 ERP</span>
      </router-link>

      <!-- Nav Links (desktop) -->
      <nav class="nav-links">
        <a v-for="item in navItems" :key="item.href" :href="item.href" class="nav-link">
          {{ item.label }}
        </a>
      </nav>

      <!-- Right Side -->
      <div class="nav-right">
        <div class="nav-divider" />
        <router-link to="/login" class="nav-btn nav-btn-outline">
          登录
        </router-link>
        <router-link to="/login" class="nav-btn nav-btn-solid">
          开始使用
        </router-link>
      </div>

      <!-- Mobile Menu Toggle -->
      <button class="nav-mobile-btn" @click="mobileOpen = !mobileOpen">
        <svg v-if="!mobileOpen" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/>
        </svg>
        <svg v-else viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
        </svg>
      </button>
    </div>

    <!-- Mobile Menu -->
    <Transition name="slide-down">
      <div v-if="mobileOpen" class="nav-mobile-menu">
        <a v-for="item in navItems" :key="item.href" :href="item.href" class="nav-mobile-link" @click="mobileOpen = false">
          {{ item.label }}
        </a>
        <div class="nav-mobile-actions">
          <router-link to="/login" class="nav-btn nav-btn-outline" @click="mobileOpen = false">登录</router-link>
          <router-link to="/login" class="nav-btn nav-btn-solid" @click="mobileOpen = false">开始使用</router-link>
        </div>
      </div>
    </Transition>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { gsap } from 'gsap';

const scrolled = ref(false);
const mobileOpen = ref(false);
const headerRef = ref<HTMLElement | null>(null);

const navItems = [
  { label: '功能', href: '#features' },
  { label: '方案', href: '#solutions' },
  { label: '评价', href: '#testimonials' },
  { label: '关于', href: '#about' },
];

function onScroll() {
  scrolled.value = window.scrollY > 50;
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true });

  // Entrance animation
  if (headerRef.value) {
    gsap.from(headerRef.value, {
      autoAlpha: 0,
      y: -20,
      duration: 0.6,
      ease: 'power2.out',
    });
  }
});

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll);
});
</script>

<style scoped>
.nav-header {
  position: sticky;
  top: 0;
  z-index: 40;
  width: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: rgba(10, 10, 10, 0.6);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  transition: all 0.3s ease;
  border-bottom: 1px solid transparent;
}
.nav-scrolled {
  border-bottom-color: hsl(0, 0%, 15%);
}

.nav-container {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 4rem;
  padding: 0 1.5rem;
}

/* Logo */
.nav-logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-decoration: none;
}
.nav-logo-svg {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
}
.nav-logo-text {
  font-weight: 700;
  font-size: 15px;
  letter-spacing: 1px;
  color: #fafafa;
}

/* Nav Links */
.nav-links {
  display: none;
  gap: 1.5rem;
  align-items: center;
}
@media (min-width: 768px) {
  .nav-links { display: flex; }
}
.nav-link {
  font-size: 0.9rem;
  font-weight: 500;
  color: #a3a3a3;
  text-decoration: none;
  transition: color 0.2s;
}
.nav-link:hover {
  color: #fafafa;
}

/* Right Side */
.nav-right {
  display: none;
  align-items: center;
  gap: 0.75rem;
}
@media (min-width: 768px) {
  .nav-right { display: flex; }
}
.nav-divider {
  width: 1px;
  height: 2rem;
  background: hsl(0, 0%, 15%);
  margin: 0 0.25rem;
}

/* Buttons */
.nav-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 2.25rem;
  padding: 0 1.25rem;
  border-radius: 9999px;
  font-size: 0.85rem;
  font-weight: 500;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}
.nav-btn-outline {
  color: #a3a3a3;
  border-color: hsl(0, 0%, 20%);
  background: transparent;
}
.nav-btn-outline:hover {
  color: #fafafa;
  border-color: hsl(0, 0%, 30%);
}
.nav-btn-solid {
  background: #2563eb;
  color: #ffffff;
}
.nav-btn-solid:hover {
  background: #1d4ed8;
}

/* Mobile */
.nav-mobile-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: #a3a3a3;
  cursor: pointer;
  padding: 0.5rem;
}
@media (min-width: 768px) {
  .nav-mobile-btn { display: none; }
}

.nav-mobile-menu {
  display: flex;
  flex-direction: column;
  padding: 1rem 1.5rem 1.5rem;
  gap: 0.5rem;
  border-top: 1px solid hsl(0, 0%, 15%);
  background: rgba(10, 10, 10, 0.95);
}
.nav-mobile-link {
  font-size: 1rem;
  color: #a3a3a3;
  text-decoration: none;
  padding: 0.5rem 0;
}
.nav-mobile-link:hover {
  color: #fafafa;
}
.nav-mobile-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

/* Slide transition */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}
.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
