<template>
  <div ref="chartEl" class="base-echart-container" :style="{ height, width }" />
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: {
    type: Object,
    required: true
  },
  width: {
    type: String,
    default: '100%'
  },
  height: {
    type: String,
    default: '100%'
  }
})

const chartEl = ref(null)
let chartInstance = null
let resizeObserver = null

function initChart() {
  if (!chartEl.value) return
  if (chartInstance) {
    chartInstance.dispose()
  }
  chartInstance = echarts.init(chartEl.value, null, { renderer: 'canvas' })
  if (props.option) {
    chartInstance.setOption(props.option, true)
  }
}

function handleResize() {
  if (chartInstance && !chartInstance.isDisposed()) {
    chartInstance.resize()
  }
}

watch(
  () => props.option,
  (nextOpt) => {
    if (!nextOpt) return
    if (!chartInstance) {
      nextTick(() => initChart())
      return
    }
    chartInstance.setOption(nextOpt, true)
  },
  { deep: true }
)

onMounted(() => {
  nextTick(() => {
    initChart()
    if (chartEl.value && typeof ResizeObserver !== 'undefined') {
      resizeObserver = new ResizeObserver(() => {
        handleResize()
      })
      resizeObserver.observe(chartEl.value)
    }
    window.addEventListener('resize', handleResize)
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.base-echart-container {
  min-width: 0;
  min-height: 0;
}
</style>
