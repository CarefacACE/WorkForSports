<template>
  <div class="mq-wrapper" :class="[pauseOnHover ? 'mq-pause-hover' : '', reverse ? 'mq-reverse' : '']">
    <div class="mq-track" :style="{ animationDuration: duration }">
      <slot />
    </div>
    <div class="mq-track mq-track-dup" aria-hidden="true" :style="{ animationDuration: duration }">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  reverse?: boolean;
  pauseOnHover?: boolean;
  duration?: string;
}>(), {
  reverse: false,
  pauseOnHover: false,
  duration: '40s',
});
</script>

<style scoped>
.mq-wrapper {
  display: flex;
  overflow: hidden;
  gap: 1rem;
  width: 100%;
}
.mq-track {
  display: flex;
  flex-shrink: 0;
  justify-content: flex-start;
  gap: 1rem;
  animation: marquee var(--duration, 40s) linear infinite;
  min-width: 50%;
}
.mq-track-dup {
  animation: marquee var(--duration, 40s) linear infinite;
}
.mq-reverse .mq-track,
.mq-reverse .mq-track-dup {
  animation-direction: reverse;
}
.mq-pause-hover:hover .mq-track,
.mq-pause-hover:hover .mq-track-dup {
  animation-play-state: paused;
}

@keyframes marquee {
  from { transform: translateX(0); }
  to   { transform: translateX(-100%); }
}
</style>
