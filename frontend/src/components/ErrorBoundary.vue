<template>
  <div v-if="error" class="error-boundary">
    <div class="error-icon">&#x26A0;</div>
    <h2>页面加载异常</h2>
    <p>{{ error.message || '发生了未知错误，请尝试刷新页面' }}</p>
    <button class="btn btn-primary" @click="retry">刷新页面</button>
  </div>
  <slot v-else />
</template>

<script setup>
import { ref, onErrorCaptured } from 'vue'

const error = ref(null)

onErrorCaptured((err) => {
  error.value = err
  return false
})

function retry() {
  error.value = null
  window.location.reload()
}
</script>