<template>
  <svg
    class="adha-logo-mark"
    :width="size"
    :height="size"
    viewBox="0 0 40 40"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-label="ADHA Sneaker Logo"
  >
    <rect width="40" height="40" :fill="bgFill" />
    <!-- Architectural corner precision brackets -->
    <path
      d="M31.5 4.5 H35.5 V8.5"
      :stroke="fgFill"
      stroke-width="1.35"
      stroke-opacity="0.45"
    />
    <path
      d="M4.5 31.5 V35.5 H8.5"
      :stroke="fgFill"
      stroke-width="1.35"
      stroke-opacity="0.45"
    />
    <!-- Bold geometric ADHA 'A' monogram -->
    <path
      d="M10.2 31.5 L18.1 8.5 H21.9 L29.8 31.5 H25.2 L20 15.1 L14.8 31.5 Z"
      :fill="fgFill"
    />
    <!-- Negative-space cutout under speed slash -->
    <path
      d="M8.6 25.5 L31.6 19.3 L30.1 23.4 L11.5 28.0 Z"
      :fill="bgFill"
    />
    <!-- Signature crimson sneaker speed-sole chevron slash -->
    <path
      d="M9.6 25.2 L31.0 19.6 L29.7 22.8 L12.0 27.3 Z"
      fill="#EF4444"
    />
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  size: {
    type: [Number, String],
    default: 36
  },
  variant: {
    type: String,
    default: 'dark' // 'dark' = black badge on white bg, 'light' = white badge on dark bg
  }
})

const bgFill = computed(() => (props.variant === 'light' ? '#FFFFFF' : '#0A0A0A'))
const fgFill = computed(() => (props.variant === 'light' ? '#0A0A0A' : '#FFFFFF'))
</script>

<style scoped>
.adha-logo-mark {
  display: block;
  flex-shrink: 0;
}
</style>
