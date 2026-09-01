// 全局聊天状态存储，跨路由保持消息不丢失
import { ref } from 'vue'

const messages = ref([])
const memoryId = ref(Number(localStorage.getItem('xiaozhi_memory_id')) || Date.now())

// 确保 memoryId 持久化
localStorage.setItem('xiaozhi_memory_id', memoryId.value)

export function useChatStore() {
  return { messages, memoryId }
}