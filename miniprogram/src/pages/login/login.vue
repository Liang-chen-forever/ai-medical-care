<template>
  <view class="page-container">
    <view class="auth-card">
      <view class="auth-header">
        <view class="auth-icon">+</view>
        <text class="auth-title">用户登录</text>
        <text class="auth-desc">登录硅谷小智，享受智能医疗服务</text>
      </view>

      <view class="auth-body">
        <view class="form-group">
          <text class="form-label">用户名</text>
          <input v-model="form.username" class="form-input" placeholder="请输入用户名" />
        </view>
        <view class="form-group">
          <text class="form-label">密码</text>
          <input v-model="form.password" class="form-input" type="password" placeholder="请输入密码" />
        </view>

        <view v-if="errorMsg" class="error-msg">{{ errorMsg }}</view>

        <button
          class="btn btn-primary btn-block"
          @tap="handleLogin"
          :disabled="loading || !form.username || !form.password"
          :loading="loading"
        >{{ loading ? '登录中...' : '登 录' }}</button>

        <view class="auth-footer">
          还没有账号？<navigator url="/pages/register/register" class="link">立即注册</navigator>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { login, saveAuth } from '@/api/index.js'

export default {
  data() {
    return {
      form: { username: '', password: '' },
      loading: false,
      errorMsg: ''
    }
  },
  methods: {
    async handleLogin() {
      if (!this.form.username || !this.form.password) return
      this.loading = true
      this.errorMsg = ''
      try {
        const res = await login(this.form.username, this.form.password)
        saveAuth(res.data)
        getApp().globalData.userInfo = res.data
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => uni.switchTab({ url: '/pages/chat/chat' }), 500)
      } catch (e) {
        this.errorMsg = e.message || '网络错误，请稍后重试'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.auth-card {
  margin: 40rpx auto;
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 40rpx rgba(0,0,0,0.08);
}

.auth-header {
  text-align: center;
  padding: 50rpx 40rpx 0;
}

.auth-icon {
  width: 100rpx;
  height: 100rpx;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  border-radius: 24rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 48rpx;
  color: #fff;
  margin-bottom: 20rpx;
}

.auth-title {
  font-size: 38rpx;
  font-weight: 700;
  display: block;
  margin-bottom: 10rpx;
}

.auth-desc {
  font-size: 26rpx;
  color: #94a3b8;
  display: block;
}

.auth-body {
  padding: 40rpx 50rpx 50rpx;
}

.error-msg {
  background: #fef2f2;
  color: #dc2626;
  font-size: 24rpx;
  padding: 20rpx;
  border-radius: 12rpx;
  margin-bottom: 24rpx;
}

.auth-footer {
  text-align: center;
  margin-top: 30rpx;
  font-size: 26rpx;
  color: #64748b;
}

.link {
  color: #2563eb;
  font-weight: 500;
}
</style>
