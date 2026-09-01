<template>
  <Teleport to="body">
    <div v-if="visible" :class="['toast', `toast-${type}`]">
      {{ message }}
    </div>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'

const visible = ref(false)
const message = ref('')
const type = ref('success')
let timer = null

function show(msg, t = 'success', duration = 3000) {
  clearTimeout(timer)
  message.value = msg
  type.value = t
  visible.value = true
  timer = setTimeout(() => { visible.value = false }, duration)
}

defineExpose({ show })
</script>