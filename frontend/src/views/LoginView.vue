<template>
  <div class="page-container">
    <div class="auth-card">
      <div class="auth-header">
        <div class="auth-icon">+</div>
        <h2>用户登录</h2>
        <p>登录硅谷小智，享受智能医疗服务</p>
      </div>

      <div class="auth-body">
        <div class="form-group">
          <label>用户名</label>
          <input
            v-model="form.username"
            class="form-input"
            placeholder="请输入用户名"
            @keydown.enter="handleLogin"
          />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input
            v-model="form.password"
            type="password"
            class="form-input"
            placeholder="请输入密码"
            @keydown.enter="handleLogin"
          />
        </div>

        <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>

        <button
          class="btn btn-primary btn-block"
          @click="handleLogin"
          :disabled="loading || !form.username || !form.password"
        >
          {{ loading ? '登录中...' : '登 录' }}
        </button>

        <div class="auth-footer">
          还没有账号？<router-link to="/register">立即注册</router-link>
        </div>
      </div>
    </div>

    <div v-if="toast.show" :class="['toast', `toast-${toast.type}`]">
      {{ toast.message }}
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const form = ref({ username: '', password: '' })
const loading = ref(false)
const errorMsg = ref('')
const toast = ref({ show: false, type: 'success', message: '' })

function showToast(type, message) {
  toast.value = { show: true, type, message }
  setTimeout(() => { toast.value.show = false }, 3000)
}

async function handleLogin() {
  if (!form.value.username || !form.value.password) return
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await axios.post('/api/auth/login', form.value)
    if (res.data.code === 200) {
      const user = res.data.data
      localStorage.setItem('user', JSON.stringify(user))
      showToast('success', '登录成功')
      setTimeout(() => router.push('/chat'), 500)
    } else {
      errorMsg.value = res.data.message || '登录失败'
    }
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-card {
  max-width: 420px;
  margin: 60px auto;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.auth-header {
  text-align: center;
  padding: 32px 24px 0;
}

.auth-icon {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: white;
  margin-bottom: 12px;
}

.auth-header h2 {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 6px;
}

.auth-header p {
  font-size: 13px;
  color: #94a3b8;
}

.auth-body {
  padding: 24px 32px 32px;
}

.error-msg {
  background: #fef2f2;
  color: #dc2626;
  font-size: 13px;
  padding: 10px 14px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.auth-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
  color: #64748b;
}

.auth-footer a {
  color: #2563eb;
  font-weight: 500;
  text-decoration: none;
}

/* 移动端适配 */
@media (max-width: 767px) {
  .auth-card {
    margin: 20px auto;
    max-width: none;
    border-radius: 0;
    box-shadow: none;
  }

  .auth-header {
    padding: 24px 20px 0;
  }

  .auth-body {
    padding: 20px 20px 24px;
  }
}
</style>