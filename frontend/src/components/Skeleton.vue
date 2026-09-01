<template>
  <div class="skeleton-wrapper">
    <div v-if="type === 'text'" class="skeleton skeleton-text" :style="{ width }" />
    <div v-else-if="type === 'title'" class="skeleton skeleton-title" :style="{ width }" />
    <div v-else-if="type === 'card'" class="skeleton skeleton-card" />
    <div v-else-if="type === 'avatar'" class="skeleton skeleton-avatar" />
    <div v-else class="skeleton" :style="{ width, height }" />
  </div>
</template>

<script setup>
defineProps({
  type: { type: String, default: 'text' },
  width: { type: String, default: '80%' },
  height: { type: String, default: '16px' }
})
</script>

<style scoped>
.skeleton-wrapper {
  width: 100%;
}
</style>