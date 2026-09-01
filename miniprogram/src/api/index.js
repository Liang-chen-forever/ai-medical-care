const BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:5137').replace(/\/$/, '')

function readStorage(key) {
  const stored = uni.getStorageSync(key)
  if (!stored) return null
  if (typeof stored === 'string') {
    try {
      return JSON.parse(stored)
    } catch {
      return null
    }
  }
  return stored
}

export function getAuth() {
  const auth = readStorage('auth')
  return auth?.accessToken ? auth : null
}

export function getUserInfo() {
  return readStorage('userInfo')
}

export function saveAuth(auth) {
  uni.setStorageSync('auth', auth)
  uni.setStorageSync('userInfo', {
    userId: auth.userId,
    username: auth.username,
    idCard: auth.idCard,
    phone: auth.phone
  })
}

export function clearAuth() {
  uni.removeStorageSync('auth')
  uni.removeStorageSync('userInfo')
}

function redirectToLogin() {
  const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : []
  const currentRoute = pages[pages.length - 1]?.route || ''
  if (!currentRoute.includes('pages/login/login')) {
    uni.reLaunch({ url: '/pages/login/login' })
  }
}

function handleUnauthorized() {
  clearAuth()
  redirectToLogin()
}

function request(url, method = 'GET', data = null) {
  return new Promise((resolve, reject) => {
    const auth = getAuth()
    uni.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        ...(auth?.accessToken ? { Authorization: `Bearer ${auth.accessToken}` } : {})
      },
      success(res) {
        const body = res.data
        if (res.statusCode === 401 || body?.code === 401) {
          handleUnauthorized()
          reject(new Error(body?.message || '请先登录'))
          return
        }
        if (res.statusCode >= 200 && res.statusCode < 300 && body?.code === 200) {
          resolve(body)
          return
        }
        reject(new Error(body?.message || '请求失败'))
      },
      fail() {
        reject(new Error('网络错误，请检查网络连接'))
      }
    })
  })
}

function get(url, params = {}) {
  const query = Object.keys(params)
    .filter((key) => params[key] !== undefined && params[key] !== '')
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')
  return request(`${url}${query ? `?${query}` : ''}`)
}

function post(url, data) {
  return request(url, 'POST', data)
}

export function login(username, password) {
  return post('/api/v1/auth/login', { username, password })
}

export function register(data) {
  return post('/api/v1/auth/register', data)
}

export function chatWithAI(memoryId, userMessage) {
  return new Promise((resolve, reject) => {
    const auth = getAuth()
    uni.request({
      url: `${BASE_URL}/xiaozhi/chat`,
      method: 'POST',
      data: { memoryId, userMessage },
      responseType: 'text',
      enableChunked: true,
      header: {
        'Content-Type': 'application/json',
        ...(auth?.accessToken ? { Authorization: `Bearer ${auth.accessToken}` } : {})
      },
      success(res) {
        if (res.statusCode === 401) {
          handleUnauthorized()
          reject(new Error('请先登录'))
          return
        }
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data)
          return
        }
        reject(new Error('AI 服务暂时不可用'))
      },
      fail() {
        reject(new Error('AI 服务暂时不可用'))
      }
    })
  })
}

export function getDepartments() {
  return get('/api/v1/departments')
}

export function getDoctors(department) {
  return get(`/api/v1/departments/${encodeURIComponent(department)}/doctors`)
}

export function getSchedules(department, date, period) {
  return get('/api/v1/schedules', { department, date, period })
}

export function bookAppointment(scheduleId) {
  return post('/api/v1/appointments', { scheduleId })
}

export function getAppointments() {
  return get('/api/v1/appointments/me')
}

export function cancelAppointment(id) {
  return request(`/api/v1/appointments/${id}`, 'DELETE')
}
