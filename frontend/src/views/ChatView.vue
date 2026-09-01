<template>
  <div class="page-container">
    <div class="page-header">
      <h1>AI 智能问诊</h1>
      <p>与硅谷小智对话，获取医疗建议或办理就医流程</p>
    </div>

    <div class="card chat-container">
      <!-- 消息列表 -->
      <div class="chat-messages" ref="chatMessagesRef">
        <div v-if="messages.length === 0" class="chat-welcome">
          <div class="welcome-icon">+</div>
          <h2>你好，我是硅谷小智</h2>
          <p>北京协和医院的智能客服助手。我可以：</p>
          <div class="welcome-features">
            <span>医疗健康咨询</span>
            <span>智能分诊推荐</span>
            <span>预约挂号服务</span>
            <span>科室医生查询</span>
          </div>
        </div>

        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          :class="['chat-bubble', msg.role]"
        >
          <div class="bubble-avatar">
            {{ msg.role === 'user' ? '我' : '医' }}
          </div>
          <div class="bubble-content">
            <div class="bubble-text" v-html="renderMarkdown(msg.content)"></div>
            <div class="bubble-time">{{ msg.time }}</div>
          </div>
        </div>

        <div v-if="streaming" class="chat-bubble assistant">
          <div class="bubble-avatar">医</div>
          <div class="bubble-content">
            <div class="bubble-text" v-html="renderMarkdown(streamingText)"></div>
            <div class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入框 -->
      <div class="chat-input-area">
        <div class="chat-suggestions" v-if="messages.length === 0">
          <button
            v-for="q in quickQuestions"
            :key="q"
            class="btn btn-outline btn-sm"
            @click="sendMessage(q)"
          >{{ q }}</button>
        </div>
        <div class="chat-input-row">
          <input
            v-model="inputText"
            class="form-input chat-input"
            placeholder="输入你的问题..."
            @keydown.enter="sendMessage(inputText)"
            :disabled="streaming"
          />
          <button
            class="btn btn-primary"
            @click="sendMessage(inputText)"
            :disabled="streaming || !inputText.trim()"
          >
            发送
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { chatWithAI } from '../api/index.js'
import { useChatStore } from '../store/chatStore.js'

const { messages, memoryId } = useChatStore()

const inputText = ref('')
const streaming = ref(false)
const streamingText = ref('')
const chatMessagesRef = ref(null)

const quickQuestions = [
  '我最近经常头痛，是什么原因？',
  '我想挂神经内科的号',
  '口腔科有哪些医生？',
  '如何取消预约？'
]

function scrollToBottom() {
  nextTick(() => {
    if (chatMessagesRef.value) {
      chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
    }
  })
}

function formatTime() {
  const now = new Date()
  return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
}

function renderMarkdown(text) {
  if (!text) return ''
  // 简单 markdown 渲染
  return text
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n\n/g, '<br/><br/>')
    .replace(/\n/g, '<br/>')
    .replace(/- (.+)/g, '&bull; $1')
}

async function sendMessage(text) {
  const content = (text || inputText.value).trim()
  if (!content || streaming.value) return

  messages.value.push({ role: 'user', content, time: formatTime() })
  inputText.value = ''
  scrollToBottom()

  streaming.value = true
  streamingText.value = ''

  try {
    const response = await chatWithAI(memoryId.value, content)
    const reader = response.body.getReader()
    const decoder = new TextDecoder()

    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      // 处理 SSE 格式: data:xxx
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          streamingText.value += line.slice(5)
        } else if (line.trim()) {
          streamingText.value += line
        }
        scrollToBottom()
      }
    }
    // 处理剩余 buffer
    if (buffer.trim()) {
      if (buffer.startsWith('data:')) {
        streamingText.value += buffer.slice(5)
      } else {
        streamingText.value += buffer
      }
    }
  } catch (e) {
    streamingText.value = '抱歉，连接出现错误，请稍后重试。'
    console.error('Chat error:', e)
  }

  if (streamingText.value) {
    messages.value.push({ role: 'assistant', content: streamingText.value, time: formatTime() })
  }
  streamingText.value = ''
  streaming.value = false
  scrollToBottom()
}

onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 180px);
  min-height: 500px;
  padding: 0;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chat-welcome {
  text-align: center;
  padding: 48px 24px;
}

.welcome-icon {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
  margin: 0 auto 16px;
}

.chat-welcome h2 {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 8px;
}

.chat-welcome p {
  color: #64748b;
  font-size: 14px;
  margin-bottom: 16px;
}

.welcome-features {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.welcome-features span {
  padding: 6px 14px;
  background: #f0f4f8;
  border-radius: 20px;
  font-size: 13px;
  color: #2563eb;
  font-weight: 500;
}

.chat-bubble {
  display: flex;
  gap: 10px;
  max-width: 80%;
}

.chat-bubble.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.chat-bubble.assistant {
  align-self: flex-start;
}

.bubble-avatar {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.chat-bubble.user .bubble-avatar {
  background: #2563eb;
  color: white;
}

.chat-bubble.assistant .bubble-avatar {
  background: #10b981;
  color: white;
}

.bubble-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bubble-text {
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.chat-bubble.user .bubble-text {
  background: #2563eb;
  color: white;
  border-bottom-right-radius: 4px;
}

.chat-bubble.assistant .bubble-text {
  background: #f1f5f9;
  color: #1e293b;
  border-bottom-left-radius: 4px;
}

.bubble-time {
  font-size: 11px;
  color: #94a3b8;
  padding: 0 4px;
}

.chat-bubble.user .bubble-time {
  text-align: right;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 16px;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  background: #94a3b8;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-4px); opacity: 1; }
}

.chat-input-area {
  border-top: 1px solid #e2e8f0;
  padding: 16px 24px;
  background: #fff;
}

.chat-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.chat-input-row {
  display: flex;
  gap: 10px;
}

.chat-input {
  flex: 1;
}

/* 移动端适配 */
@media (max-width: 767px) {
  .chat-container {
    height: calc(100vh - 140px);
    min-height: 400px;
    border-radius: 0;
  }

  .chat-messages {
    padding: 16px 12px;
    gap: 12px;
  }

  .chat-bubble {
    max-width: 90%;
  }

  .bubble-text {
    font-size: 13px;
    padding: 10px 14px;
  }

  .chat-input-area {
    padding: 12px 16px;
  }

  .chat-welcome {
    padding: 32px 16px;
  }

  .chat-welcome h2 {
    font-size: 18px;
  }

  .welcome-features span {
    font-size: 12px;
    padding: 5px 12px;
  }

  .chat-suggestions {
    gap: 6px;
  }

  .chat-suggestions .btn {
    font-size: 12px;
    padding: 6px 10px;
  }
}

/* 平板适配 */
@media (min-width: 768px) and (max-width: 1023px) {
  .chat-container {
    height: calc(100vh - 160px);
  }
}
</style>