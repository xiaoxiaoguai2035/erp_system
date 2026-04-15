<template>
  <article class="metric-card" :class="tone">
    <p class="metric-label">{{ title }}</p>
    <div class="metric-value">
      {{ value }}
      <small>{{ unit }}</small>
    </div>
    <div class="metric-trend">{{ trend }}</div>
  </article>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  value: { type: [String, Number], required: true },
  unit: { type: String, default: "" },
  trend: { type: String, default: "" },
  tone: { type: String, default: "sunrise" }
});
</script>

<style scoped>
small {
  font-size: 18px;
  margin-left: 6px;
  opacity: 0.8;
}
</style>
