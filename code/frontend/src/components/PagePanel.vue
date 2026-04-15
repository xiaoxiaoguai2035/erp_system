<template>
  <section class="panel">
    <div class="panel-body">
      <div v-if="title || $slots.actions" class="section-header">
        <div>
          <h3 v-if="title">{{ title }}</h3>
        </div>
        <slot name="actions" />
      </div>
      <slot />
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, default: "" },
  description: { type: String, default: "" }
});
</script>
