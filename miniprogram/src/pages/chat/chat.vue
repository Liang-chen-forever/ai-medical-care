<template>
  <view class="page-container">
    <view class="page-header">
      <text class="title">AI 智能问诊</text>
      <text class="desc">与硅谷小智对话，获取医疗建议或办理就医流程</text>
    </view>

    <view class="card chat-container">
      <scroll-view class="chat-messages" scroll-y scroll-with-animation :scroll-into-view="scrollToId" :scroll-top="scrollTop">
        <!-- 欢迎页 -->
        <view v-if="messages.length === 0" class="chat-welcome">
          <view class="welcome-icon">+</view>
          <text class="welcome-title">你好，我是硅谷小智</text>
          <text class="welcome-desc">北京协和医院的智能客服助手</text>
          <view class="welcome-features">
            <text class="feature-tag">医疗健康咨询</text>
            <text class="feature-tag">智能分诊推荐</text>
            <text class="feature-tag">预约挂号服务</text>
            <text class="feature-tag">科室医生查询</text>
          </view>
        </view>

        <!-- 消息列表 -->
        <view v-for="(msg, idx) in messages" :key="idx" :id="'msg-' + idx" :class="['chat-bubble', msg.role]">
          <view class="bubble-avatar">{{ msg.role === 'user' ? '我' : '医' }}</view>
          <view class="bubble-content">
            <view class="bubble-text">
              <rich-text :nodes="formatContent(msg.content)"></rich-text>
            </view>
            <text class="bubble-time">{{ msg.time }}</text>
          </view>
        </view>

        <view v-if="streaming" class="chat-bubble assistant">
          <view class="bubble-avatar">医</view>
          <view class="bubble-content">
            <view class="bubble-text">
              <rich-text :nodes="formatContent(streamingText)"></rich-text>
            </view>
            <view class="typing-dots">
              <view class="dot"></view>
              <view class="dot"></view>
              <view class="dot"></view>
            </view>
          </view>
        </view>

        <view id="msg-bottom"></view>
      </scroll-view>

      <!-- 快捷问题 -->
      <view class="quick-questions" v-if="messages.length === 0">
        <view
          v-for="q in quickQuestions"
          :key="q"
          class="quick-btn"
          @tap="sendMessage(q)"
        >{{ q }}</view>
      </view>

      <!-- 输入框 -->
      <view class="chat-input-area">
        <input
          v-model="inputText"
          class="chat-input"
          placeholder="输入你的问题..."
          confirm-type="send"
          :disabled="streaming"
          @confirm="sendMessage(inputText)"
        />
        <button
          class="btn btn-primary"
          style="width: 120rpx; height: 72rpx; padding: 0; font-size: 28rpx;"
          @tap="sendMessage(inputText)"
          :disabled="streaming || !inputText.trim()"
        >发送</button>
      </view>
    </view>

    <uni-popup ref="toast" type="message">
      <uni-popup-message :type="toastType" :message="toastMsg" :duration="2000" />
    </uni-popup>
  </view>
</template>

<script>
import { chatWithAI } from '@/api/index.js'

export default {
  data() {
    return {
      messages: [],
      inputText: '',
      streaming: false,
      streamingText: '',
      memoryId: Date.now(),
      scrollToId: '',
      scrollTop: 0,
      toastType: 'success',
      toastMsg: '',
      quickQuestions: [
        '我最近经常头痛，是什么原因？',
        '我想挂神经内科的号',
        '口腔科有哪些医生？',
        '如何取消预约？'
      ]
    }
  },
  methods: {
    formatTime() {
      const now = new Date()
      const h = now.getHours().toString().padStart(2, '0')
      const m = now.getMinutes().toString().padStart(2, '0')
      return `${h}:${m}`
    },
    formatContent(text) {
      if (!text) return ''
      return text
        .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
        .replace(/\n\n/g, '<br/><br/>')
        .replace(/\n/g, '<br/>')
    },
    scrollToBottom() {
      this.$nextTick(() => {
        this.scrollToId = 'msg-bottom'
      })
    },
    async sendMessage(text) {
      const content = (text || this.inputText).trim()
      if (!content || this.streaming) return

      this.messages.push({ role: 'user', content, time: this.formatTime() })
      this.inputText = ''
      this.scrollToBottom()

      this.streaming = true
      this.streamingText = ''

      try {
        // 小程序使用非流式请求（兼容性更好）
        const res = await chatWithAI(this.memoryId, content)
        this.streamingText = typeof res === 'string' ? res : (res.data || res.message || '')
      } catch (e) {
        this.streamingText = '抱歉，连接出现错误，请稍后重试。'
        console.error('Chat error:', e)
      }

      if (this.streamingText) {
        this.messages.push({ role: 'assistant', content: this.streamingText, time: this.formatTime() })
      }
      this.streamingText = ''
      this.streaming = false
      this.scrollToBottom()
    }
  }
}
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 300rpx);
  padding: 0;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  padding: 30rpx;
}

.chat-welcome {
  text-align: center;
  padding: 60rpx 30rpx;
}

.welcome-icon {
  width: 100rpx;
  height: 100rpx;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48rpx;
  color: #fff;
  margin: 0 auto 24rpx;
}

.welcome-title {
  font-size: 36rpx;
  font-weight: 700;
  display: block;
  margin-bottom: 12rpx;
}

.welcome-desc {
  font-size: 26rpx;
  color: #64748b;
  display: block;
  margin-bottom: 24rpx;
}

.welcome-features {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  justify-content: center;
}

.feature-tag {
  padding: 10rpx 24rpx;
  background: #f0f4f8;
  border-radius: 30rpx;
  font-size: 24rpx;
  color: #2563eb;
}

.chat-bubble {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.chat-bubble.user {
  flex-direction: row-reverse;
}

.bubble-avatar {
  width: 60rpx;
  height: 60rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 600;
  flex-shrink: 0;
}

.chat-bubble.user .bubble-avatar {
  background: #2563eb;
  color: #fff;
}

.chat-bubble.assistant .bubble-avatar {
  background: #10b981;
  color: #fff;
}

.bubble-content {
  max-width: 75%;
}

.bubble-text {
  padding: 20rpx 28rpx;
  border-radius: 20rpx;
  font-size: 28rpx;
  line-height: 1.7;
  word-break: break-all;
}

.chat-bubble.user .bubble-text {
  background: #2563eb;
  color: #fff;
  border-bottom-right-radius: 6rpx;
}

.chat-bubble.assistant .bubble-text {
  background: #f1f5f9;
  color: #1e293b;
  border-bottom-left-radius: 6rpx;
}

.bubble-time {
  font-size: 20rpx;
  color: #94a3b8;
  display: block;
  padding: 4rpx 8rpx;
}

.chat-bubble.user .bubble-time {
  text-align: right;
}

.typing-dots {
  display: flex;
  gap: 8rpx;
  padding: 8rpx 28rpx;
}

.typing-dots .dot {
  width: 12rpx;
  height: 12rpx;
  background: #94a3b8;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-dots .dot:nth-child(2) { animation-delay: 0.2s; }
.typing-dots .dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-8rpx); opacity: 1; }
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  padding: 20rpx 30rpx;
  border-top: 2rpx solid #e2e8f0;
}

.quick-btn {
  padding: 12rpx 24rpx;
  border: 2rpx solid #e2e8f0;
  border-radius: 30rpx;
  font-size: 24rpx;
  color: #64748b;
  background: #fff;
}

.chat-input-area {
  display: flex;
  gap: 16rpx;
  padding: 20rpx 30rpx;
  border-top: 2rpx solid #e2e8f0;
}

.chat-input {
  flex: 1;
  height: 72rpx;
  padding: 0 24rpx;
  border: 2rpx solid #e2e8f0;
  border-radius: 36rpx;
  font-size: 28rpx;
  background: #f0f4f8;
}
</style>