<template>
  <span class="ct-wrapper">
    <span
      v-for="(char, i) in chars"
      :key="`${char}-${count}-${i}`"
      ref="charRefs"
      class="ct-char"
    >{{ char }}</span>
  </span>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue';
import { gsap } from 'gsap';

const props = defineProps<{ text: string }>();

const colors = [
  'rgb(131, 179, 32)', 'rgb(47, 195, 106)', 'rgb(42, 169, 210)',
  'rgb(4, 112, 202)', 'rgb(107, 10, 255)', 'rgb(183, 0, 218)',
  'rgb(218, 0, 171)', 'rgb(230, 64, 92)', 'rgb(232, 98, 63)',
  'rgb(249, 129, 47)',
];

const chars = computed(() => props.text.split(''));
const count = ref(0);
const currentColors = ref([...colors]);
const charRefs = ref<HTMLElement[]>([]);
let intervalId: ReturnType<typeof setInterval> | null = null;
let tweens: gsap.core.Tween[] = [];

function animateChars() {
  tweens.forEach((t) => t.kill());
  tweens = [];

  charRefs.value.forEach((el, i) => {
    if (!el) return;
    const targetColor = currentColors.value[i % currentColors.value.length];
    const tween = gsap.fromTo(
      el,
      { y: 0 },
      {
        color: targetColor,
        y: [0, -3, 0],
        scale: [1, 1.01, 1],
        filter: ['blur(0px)', 'blur(5px)', 'blur(0px)'],
        opacity: [1, 0.8, 1],
        duration: 0.5,
        delay: i * 0.05,
      }
    );
    tweens.push(tween);
  });
}

onMounted(() => {
  animateChars();

  intervalId = setInterval(() => {
    currentColors.value = [...colors].sort(() => Math.random() - 0.5);
    count.value++;
  }, 5000);
});

watch(count, () => {
  nextTick(() => animateChars());
});

onBeforeUnmount(() => {
  if (intervalId) clearInterval(intervalId);
  tweens.forEach((t) => t.kill());
});
</script>

<style scoped>
.ct-wrapper {
  display: inline;
}
.ct-char {
  display: inline-block;
  white-space: pre;
  font-family: inherit;
  letter-spacing: tight;
}
</style>
