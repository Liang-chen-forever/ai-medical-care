<template>
  <view class="page-container">
    <view class="auth-card">
      <view class="auth-header">
        <view class="auth-icon">+</view>
        <text class="auth-title">创建账号</text>
        <text class="auth-desc">注册硅谷小智，开启智能医疗服务</text>
      </view>

      <view class="auth-body">
        <view class="form-group">
          <text class="form-label">用户名 <text class="required">*</text></text>
          <input v-model="form.username" class="form-input" placeholder="至少3个字符" />
        </view>
        <view class="form-group">
          <text class="form-label">身份证号 <text class="required">*</text></text>
          <input v-model="form.idCard" class="form-input" placeholder="请输入18位身份证号" maxlength="18" />
        </view>
        <view class="form-group">
          <text class="form-label">手机号</text>
          <input v-model="form.phone" class="form-input" placeholder="请输入手机号（选填）" maxlength="11" />
        </view>
        <view class="form-group">
          <text class="form-label">密码 <text class="required">*</text></text>
          <input v-model="form.password" class="form-input" type="password" placeholder="至少6位密码" />
        </view>
        <view class="form-group">
          <text class="form-label">确认密码 <text class="required">*</text></text>
          <input v-model="confirmPassword" class="form-input" type="password" placeholder="再次输入密码" />
        </view>

        <view v-if="errorMsg" class="error-msg">{{ errorMsg }}</view>

        <button
          class="btn btn-primary btn-block"
          @tap="handleRegister"
          :disabled="loading || !canSubmit"
          :loading="loading"
        >{{ loading ? '注册中...' : '注 册' }}</button>

        <view class="auth-footer">
          已有账号？<navigator url="/pages/login/login" class="link">立即登录</navigator>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { register } from '@/api/index.js'

export default {
  data() {
    return {
      form: { username: '', password: '', idCard: '', phone: '' },
      confirmPassword: '',
      loading: false,
      errorMsg: ''
    }
  },
  computed: {
    canSubmit() {
      return this.form.username.length >= 3 &&
        this.form.password.length >= 6 &&
        this.form.password === this.confirmPassword &&
        this.form.idCard.length === 18
    }
  },
  methods: {
    async handleRegister() {
      if (!this.canSubmit) {
        if (this.form.password !== this.confirmPassword) {
          this.errorMsg = '两次输入的密码不一致'
          return
        }
        return
      }
      this.loading = true
      this.errorMsg = ''
      try {
        await register(this.form)
        uni.showToast({ title: '注册成功', icon: 'success' })
        setTimeout(() => uni.redirectTo({ url: '/pages/login/login' }), 1000)
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
  padding: 40rpx 40rpx 0;
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

.required {
  color: #ef4444;
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
