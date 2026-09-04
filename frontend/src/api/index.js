import axios from 'axios'

const API_BASE_URL = (import.meta.env?.VITE_API_BASE_URL || '/').replace(/\/$/, '')
const AUTH_STORAGE_KEY = 'auth'
const USER_STORAGE_KEY = 'user'

export function getAuth() {
  try {
    const auth = JSON.parse(localStorage.getItem(AUTH_STORAGE_KEY) || 'null')
    return auth?.accessToken ? auth : null
  } catch {
    return null
  }
}

export function isAuthenticated() {
  return Boolean(getAuth())
}

export function getUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_STORAGE_KEY) || 'null')
  } catch {
    return null
  }
}

export function saveAuth(auth) {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth))
  localStorage.setItem(USER_STORAGE_KEY, JSON.stringify({
    userId: auth.userId,
    username: auth.username,
    role: auth.role,
    idCard: auth.idCard,
    phone: auth.phone
  }))
}

export function clearAuth() {
  localStorage.removeItem(AUTH_STORAGE_KEY)
  localStorage.removeItem(USER_STORAGE_KEY)
}

export const api = axios.create({
  baseURL: API_BASE_URL || '/',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use((config) => {
  const auth = getAuth()
  if (auth?.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

api.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number') {
      if (body.code === 200) {
        response.data = body.data
        response._message = body.message
        return response
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401 || error.response?.data?.code === 401) {
      clearAuth()
    }
    const message = error.response?.data?.message || error.message || '网络错误'
    error.message = message
    return Promise.reject(error)
  }
)

export function login(username, password) {
  return api.post('/api/v1/auth/login', { username, password })
}

export function register(data) {
  return api.post('/api/v1/auth/register', data)
}

export function chatWithAI(conversationId, userMessage) {
  const auth = getAuth()
  return fetch(`${API_BASE_URL}/api/v1/chat/conversations/${conversationId}/messages`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(auth?.accessToken ? { Authorization: `Bearer ${auth.accessToken}` } : {})
    },
    body: JSON.stringify({ userMessage })
  }).then((response) => {
    if (response.status === 401) {
      clearAuth()
      throw new Error('请先登录')
    }
    if (!response.ok) {
      throw new Error('AI 服务暂时不可用')
    }
    return response
  })
}

export function getDepartments() {
  return api.get('/api/v1/departments')
}

export function getDoctors(department) {
  return api.get(`/api/v1/departments/${encodeURIComponent(department)}/doctors`)
}

export function getSchedules(department, date, period) {
  return api.get('/api/v1/schedules', { params: { department, date, period } })
}

export function getAppointments() {
  return api.get('/api/v1/appointments/me')
}

export function bookAppointment(scheduleId) {
  return api.post('/api/v1/appointments', { scheduleId })
}

export function cancelAppointment(id) {
  return api.delete(`/api/v1/appointments/${id}`)
}

export function getDoctorAppointments(status) {
  return api.get('/api/v1/doctor/appointments', status ? { params: { status } } : undefined)
}

export function confirmDoctorAppointment(id) {
  return api.post(`/api/v1/doctor/appointments/${id}/confirm`)
}

export function rejectDoctorAppointment(id, reason) {
  return api.post(`/api/v1/doctor/appointments/${id}/reject`, { reason })
}

export function completeDoctorAppointment(id) {
  return api.post(`/api/v1/doctor/appointments/${id}/complete`)
}

export function reloadKnowledge() {
  return api.post('/api/knowledge/reload')
}
