<template>
  <div class="bl-wrapper" :class="className" ref="wrapperRef">
    <svg
      ref="svgRef"
      viewBox="0 0 1440 900"
      class="bl-svg"
      preserveAspectRatio="xMidYMid slice"
    >
      <path
        v-for="(path, i) in allPaths"
        :key="i"
        :d="path.d"
        :stroke="path.color"
        stroke-width="2.3"
        stroke-linecap="round"
        fill="none"
        class="bl-path"
      />
    </svg>
    <div class="bl-content">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue';
import { gsap } from 'gsap';

defineProps<{ className?: string }>();

const wrapperRef = ref<HTMLElement | null>(null);
const svgRef = ref<SVGElement | null>(null);

const colors = [
  '#46A5CA', '#8C2F2F', '#4FAE4D', '#D6590C', '#811010',
  '#247AFB', '#A534A0', '#A8A438', '#D6590C', '#46A29C',
  '#670F6D', '#D7C200', '#59BBEB', '#504F1C', '#55BC54',
  '#4D3568', '#9F39A5', '#363636', '#860909', '#6A286F',
  '#604483',
];

const pathData = [
  'M 0 100 Q 150 50 300 120 T 600 80 T 900 150 T 1200 100 T 1440 130',
  'M 0 200 Q 200 250 400 180 T 800 220 T 1100 170 T 1440 210',
  'M 0 350 C 200 300 400 400 600 350 S 1000 300 1200 380 S 1440 340 1440 340',
  'M 0 500 Q 180 450 360 520 T 720 480 T 1080 530 T 1440 490',
  'M 0 650 C 150 700 350 600 500 660 S 850 620 1050 680 S 1300 640 1440 670',
  'M 0 780 Q 250 730 500 800 T 1000 760 T 1440 790',
  'M 100 0 Q 80 150 120 300 T 90 600 T 130 900',
  'M 300 0 C 280 200 320 400 300 500 S 280 700 310 900',
  'M 500 0 Q 520 180 490 360 T 530 720 T 500 900',
  'M 700 0 C 720 150 680 350 710 500 S 730 700 690 900',
  'M 900 0 Q 880 200 920 400 T 890 700 T 910 900',
  'M 1100 0 C 1080 180 1120 360 1100 540 S 1080 720 1110 900',
  'M 1300 0 Q 1320 250 1290 500 T 1310 750 T 1300 900',
  'M 0 50 C 300 100 600 20 900 80 S 1200 40 1440 70',
  'M 0 420 Q 360 380 720 440 T 1080 400 T 1440 430',
  'M 200 0 Q 180 300 220 600 T 200 900',
  'M 600 0 C 620 250 580 500 610 750 S 590 850 600 900',
  'M 1000 0 Q 1020 220 990 440 T 1010 660 T 1000 900',
  'M 1440 100 Q 1200 80 960 130 T 480 90 T 0 120',
  'M 1440 550 C 1200 500 960 600 720 540 S 240 580 0 530',
  'M 0 850 Q 360 810 720 870 T 1080 830 T 1440 860',
];

const allPaths = computed(() => {
  return pathData.map((d, i) => ({
    d,
    color: colors[i % colors.length],
  }));
});

let tweens: gsap.core.Tween[] = [];

onMounted(() => {
  if (!svgRef.value) return;

  // Fade in SVG
  gsap.fromTo(svgRef.value, { opacity: 0 }, { opacity: 1, duration: 1 });

  // Animate each path
  const pathEls = svgRef.value.querySelectorAll('.bl-path');
  pathEls.forEach((el, i) => {
    const len = (el as SVGGeometryElement).getTotalLength?.() || 800;
    gsap.set(el, { strokeDasharray: `50 ${len}`, strokeDashoffset: 800 });

    const tween = gsap.to(el, {
      strokeDashoffset: 0,
      strokeDasharray: `20 ${len}`,
      opacity: [0, 1, 1, 0],
      duration: 10 + Math.random() * 4,
      ease: 'none',
      repeat: -1,
      delay: Math.random() * 8,
      repeatDelay: Math.random() * 8 + 2,
    });
    tweens.push(tween);
  });
});

onBeforeUnmount(() => {
  tweens.forEach((t) => t.kill());
});
</script>

<style scoped>
.bl-wrapper {
  position: relative;
  width: 100%;
  min-height: 20rem;
}
.bl-svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
.bl-path {
  opacity: 0;
}
.bl-content {
  position: relative;
  z-index: 1;
}
</style>
