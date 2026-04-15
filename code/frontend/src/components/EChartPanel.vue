<template>
  <div ref="chartRef" class="chart-host"></div>
</template>

<script setup>
import * as echarts from "echarts";
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";

const emit = defineEmits(["chart-click"]);
const props = defineProps({
  option: {
    type: Object,
    required: true
  },
  height: {
    type: String,
    default: "320px"
  }
});

const chartRef = ref(null);
let instance = null;
let resizeObserver = null;

const ensureInstance = () => {
  if (!chartRef.value) {
    return null;
  }

  if (!instance) {
    instance = echarts.init(chartRef.value);
  }

  return instance;
};

const renderChart = async () => {
  await nextTick();
  if (!chartRef.value) {
    return;
  }

  // Defer rendering until the host has a real size. This avoids charts being
  // initialized inside hidden menu panels with a zero-width canvas.
  if (!chartRef.value.clientWidth || !chartRef.value.clientHeight) {
    return;
  }

  const chart = ensureInstance();
  if (!chart) {
    return;
  }

  chart.off("click");
  chart.on("click", (params) => {
    emit("chart-click", params);
  });
  chart.setOption(props.option, true);
  chart.resize();
};

const handleResize = () => {
  if (!chartRef.value?.clientWidth || !chartRef.value?.clientHeight) {
    return;
  }

  renderChart();
};

watch(
  () => props.option,
  () => {
    renderChart();
  },
  { deep: true }
);

onMounted(() => {
  renderChart();

  if (typeof ResizeObserver !== "undefined" && chartRef.value) {
    resizeObserver = new ResizeObserver(() => {
      handleResize();
    });
    resizeObserver.observe(chartRef.value);
  }

  window.addEventListener("resize", handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  if (resizeObserver) {
    resizeObserver.disconnect();
    resizeObserver = null;
  }
  if (instance) {
    instance.dispose();
    instance = null;
  }
});
</script>

<style scoped>
.chart-host {
  width: 100%;
  height: v-bind(height);
}
</style>
