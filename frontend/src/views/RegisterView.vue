<template>
  <div class="page-container">
    <div class="auth-card">
      <div class="auth-header">
        <div class="auth-icon">+</div>
        <h2>用户注册</h2>
        <p>注册账号，享受智能医疗服务</p>
      </div>

      <div class="auth-body">
        <div class="form-group">
          <label>用户名 <span class="required">*</span></label>
          <input
            v-model="form.username"
            class="form-input"
            placeholder="请输入用户名"
          />
        </div>
        <div class="form-group">
          <label>密码 <span class="required">*</span></label>
          <input
            v-model="form.password"
            type="password"
            class="form-input"
            placeholder="请输入密码（至少6位）"
          />
        </div>
        <div class="form-group">
          <label>身份证号 <span class="required">*</span></label>
          <input
            v-model="form.idCard"
            class="form-input"
            placeholder="请输入18位身份证号"
            maxlength="18"
          />
        </div>
        <div class="form-group">
          <label>手机号</label>
          <input
            v-model="form.phone"
            class="form-input"
            placeholder="请输入手机号（选填）"
          />
        </div>

        <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>

        <button
          class="btn btn-primary btn-block"
          @click="handleRegister"
          :disabled="loading || !canSubmit"
        >
          {{ loading ? '注册中...' : '注 册' }}
        </button>

        <div class="auth-footer">
          已有账号？<router-link to="/login">立即登录</router-link>
        </div>
      </div>
    </div>

    <div v-if="toast.show" :class="['toast', `toast-${toast.type}`]">
      {{ toast.message }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api/index.js'

const router = useRouter()
const form = ref({ username: '', password: '', idCard: '', phone: '' })
const loading = ref(false)
const errorMsg = ref('')
const toast = ref({ show: false, type: 'success', message: '' })

const canSubmit = computed(() => {
  const idCardValid = validateIdCard(form.value.idCard)
  return form.value.username.trim().length >= 3 && form.value.password.length >= 6 && idCardValid
})

// 中国身份证号校验
function validateIdCard(idCard) {
  if (!idCard || idCard.length !== 18) return false
  const body = idCard.substring(0, 17).toUpperCase()
  const checkDigit = idCard.substring(17, 18).toUpperCase()
  if (!/^\d{17}$/.test(body)) return false

  // 加权因子
  const weight = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
  const checkCode = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']

  let sum = 0
  for (let i = 0; i < 17; i++) {
    sum += parseInt(body.charAt(i)) * weight[i]
  }
  if (checkCode[sum % 11] !== checkDigit) return false

  // 出生日期校验
  const year = parseInt(body.substring(6, 10))
  const month = parseInt(body.substring(10, 12))
  const day = parseInt(body.substring(12, 14))
  if (year < 1900 || year > 2100 || month < 1 || month > 12 || day < 1 || day > 31) return false

  return true
}

function showToast(type, message) {
  toast.value = { show: true, type, message }
  setTimeout(() => { toast.value.show = false }, 3000)
}

async function handleRegister() {
  if (!canSubmit.value) return
  loading.value = true
  errorMsg.value = ''
  try {
    await register(form.value)
    showToast('success', '注册成功，即将跳转登录')
    setTimeout(() => router.push('/login'), 1000)
  } catch (e) {
    errorMsg.value = e.message || '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-card {
  max-width: 420px;
  margin: 40px auto;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.auth-header {
  text-align: center;
  padding: 28px 24px 0;
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

.required {
  color: #ef4444;
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
    padding: 20px 20px 0;
  }

  .auth-body {
    padding: 20px 20px 24px;
  }
}
</style>
