<template>
  <div class="cs-container" ref="containerRef">
    <div class="cs-inner">
      <!-- Title -->
      <div class="cs-header" ref="headerRef">
        <slot name="title" />
      </div>
      <!-- 3D Card -->
      <div class="cs-card" ref="cardRef">
        <div class="cs-card-inner">
          <slot />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);

const containerRef = ref<HTMLElement | null>(null);
const headerRef = ref<HTMLElement | null>(null);
const cardRef = ref<HTMLElement | null>(null);
let ctx: gsap.Context | null = null;

onMounted(() => {
  if (!containerRef.value) return;

  const isMobile = window.innerWidth < 768;

  ctx = gsap.context(() => {
    const tl = gsap.timeline({
      scrollTrigger: {
        trigger: containerRef.value,
        start: 'top bottom',
        end: 'bottom top',
        scrub: true,
      },
    });

    // Header: translate up
    tl.to(headerRef.value, { y: -100, ease: 'none' }, 0);

    // Card: rotateX from tilted to flat + scale
    if (isMobile) {
      tl.fromTo(cardRef.value, { rotateX: 20, scale: 0.7 }, { rotateX: 0, scale: 0.9, ease: 'none' }, 0);
    } else {
      tl.fromTo(cardRef.value, { rotateX: 20, scale: 1.05 }, { rotateX: 0, scale: 1, ease: 'none' }, 0);
    }
  }, containerRef.value);
});

onBeforeUnmount(() => {
  if (ctx) ctx.revert();
});
</script>

<style scoped>
.cs-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60rem;
  padding: 0.5rem;
}
@media (min-width: 768px) {
  .cs-container {
    min-height: 80rem;
    padding: 0.5rem 5rem;
  }
}
.cs-inner {
  position: relative;
  width: 100%;
  padding: 2.5rem 0;
  perspective: 1000px;
}
.cs-header {
  max-width: 64rem;
  margin: 0 auto;
  text-align: center;
}
.cs-card {
  margin: -3rem auto 0;
  width: 100%;
  max-width: 64rem;
  height: 30rem;
  border-radius: 30px;
  border: 4px solid #6C6C6C;
  background: #222222;
  padding: 0.5rem;
  box-shadow:
    0 0 0 1px rgba(0,0,0,0.3),
    0 9px 20px rgba(0,0,0,0.29),
    0 37px 37px rgba(0,0,0,0.26),
    0 84px 50px rgba(0,0,0,0.15),
    0 149px 60px rgba(0,0,0,0.04),
    0 233px 65px rgba(0,0,0,0.01);
  transform-style: preserve-3d;
}
@media (min-width: 768px) {
  .cs-card {
    height: 40rem;
    padding: 1.5rem;
  }
}
.cs-card-inner {
  width: 100%;
  height: 100%;
  overflow: hidden;
  border-radius: 1rem;
  background: #171717;
  padding: 1rem;
}
@media (min-width: 768px) {
  .cs-card-inner {
    padding: 1rem;
    border-radius: 1.25rem;
  }
}
</style>
