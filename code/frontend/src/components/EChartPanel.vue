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

const renderChart = async () => {
  await nextTick();
  if (!chartRef.value) return;

  if (!instance) {
    instance = echarts.init(chartRef.value);
  }

  instance.off("click");
  instance.on("click", (params) => {
    emit("chart-click", params);
  });
  instance.setOption(props.option, true);
  instance.resize();
};

const handleResize = () => {
  if (instance) {
    instance.resize();
  }
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
  window.addEventListener("resize", handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
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
