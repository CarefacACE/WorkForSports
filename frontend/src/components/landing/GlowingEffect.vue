<template>
  <div class="ge-wrapper" ref="elRef" @pointermove="handleMove" @pointerleave="handleLeave">
    <!-- Static border glow -->
    <div class="ge-border" :class="{ 'ge-border--active': glow }" />
    <!-- Animated glow -->
    <div
      class="ge-container"
      :class="{ 'ge-container--hidden': disabled }"
      ref="containerRef"
    >
      <div class="ge-glow" />
    </div>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { gsap } from 'gsap';

const props = withDefaults(defineProps<{
  blur?: number;
  proximity?: number;
  spread?: number;
  glow?: boolean;
  disabled?: boolean;
  movementDuration?: number;
  borderWidth?: number;
}>(), {
  blur: 0,
  proximity: 0,
  spread: 20,
  glow: false,
  disabled: false,
  movementDuration: 2,
  borderWidth: 1,
});

const elRef = ref<HTMLElement | null>(null);
const containerRef = ref<HTMLElement | null>(null);
let currentAngle = 0;
let isActive = false;

function handleMove(e: PointerEvent) {
  const el = elRef.value;
  if (!el) return;
  const { left, top, width, height } = el.getBoundingClientRect();
  const centerX = left + width * 0.5;
  const centerY = top + height * 0.5;
  const targetAngle = (Math.atan2(e.clientY - centerY, e.clientX - centerX) * 180) / Math.PI + 90;

  const inBounds =
    e.clientX > left - props.proximity &&
    e.clientX < left + width + props.proximity &&
    e.clientY > top - props.proximity &&
    e.clientY < top + height + props.proximity;

  if (inBounds && !isActive) {
    isActive = true;
    el.style.setProperty('--active', '1');
  } else if (!inBounds && isActive) {
    isActive = false;
    el.style.setProperty('--active', '0');
  }

  const angleDiff = ((targetAngle - currentAngle + 180) % 360) - 180;
  const newAngle = currentAngle + angleDiff;

  gsap.to({ val: currentAngle }, {
    val: newAngle,
    duration: props.movementDuration,
    ease: 'power2.out',
    onUpdate() {
      currentAngle = (this.targets()[0] as { val: number }).val;
      el.style.setProperty('--start', String(currentAngle));
    },
  });
}

function handleLeave() {
  if (elRef.value) {
    elRef.value.style.setProperty('--active', '0');
    isActive = false;
  }
}

onMounted(() => {
  const el = elRef.value;
  if (!el) return;
  el.style.setProperty('--spread', String(props.spread));
  el.style.setProperty('--blur', `${props.blur}px`);
  el.style.setProperty('--active', '0');
  el.style.setProperty('--start', '0');
  el.style.setProperty('--border-width', `${props.borderWidth}px`);
});
</script>

<style scoped>
.ge-wrapper {
  position: relative;
  overflow: hidden;
}
.ge-border {
  pointer-events: none;
  position: absolute;
  inset: -1px;
  border-radius: inherit;
  border: var(--border-width, 1px) solid transparent;
  opacity: 0;
  transition: opacity 0.3s;
}
.ge-border--active {
  opacity: 1;
  border-color: rgba(255, 255, 255, 0.15);
}
.ge-container {
  pointer-events: none;
  position: absolute;
  inset: 0;
  border-radius: inherit;
  opacity: 1;
  transition: opacity 0.3s;
}
.ge-container--hidden {
  display: none;
}
.ge-glow {
  position: absolute;
  inset: 0;
  border-radius: inherit;
}
.ge-glow::after {
  content: '';
  position: absolute;
  inset: calc(-1 * var(--border-width, 1px));
  border-radius: inherit;
  border: var(--border-width, 1px) solid transparent;
  background:
    radial-gradient(circle, #dd7bbb 10%, transparent 20%),
    radial-gradient(circle at 40% 40%, #d79f1e 5%, transparent 15%),
    radial-gradient(circle at 60% 60%, #5a922c 10%, transparent 20%),
    radial-gradient(circle at 40% 60%, #4c7894 10%, transparent 20%),
    repeating-conic-gradient(from 236.84deg at 50% 50%,
      #dd7bbb 0%, #d79f1e 5%, #5a922c 10%, #4c7894 15%, #dd7bbb 20%
    );
  background-attachment: fixed;
  opacity: var(--active, 0);
  transition: opacity 0.3s;
  mask-clip: padding-box, border-box;
  mask-composite: intersect;
  mask-image:
    linear-gradient(#0000, #0000),
    conic-gradient(
      from calc((var(--start, 0) - var(--spread, 20)) * 1deg),
      transparent 0deg,
      #fff,
      transparent calc(var(--spread, 20) * 2deg)
    );
}
</style>
