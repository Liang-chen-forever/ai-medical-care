// API 层封装 - uni-app 版本，使用 uni.request 替代 axios
const BASE_URL = 'http://localhost:5137'

// 通用请求封装
function request(url, method = 'GET', data = null) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: { 'Content-Type': 'application/json' },
      success(res) {
        if (res.statusCode === 200) {
          resolve(res.data)
        } else {
          reject(new Error(res.data?.message || '请求失败'))
        }
      },
      fail(err) {
        reject(new Error('网络错误，请检查网络连接'))
      }
    })
  })
}

// GET 请求
function get(url, params = {}) {
  const query = Object.keys(params)
    .filter(k => params[k] !== undefined && params[k] !== '')
    .map(k => `${encodeURIComponent(k)}=${encodeURIComponent(params[k])}`)
    .join('&')
  return request(`${url}${query ? '?' + query : ''}`)
}

// POST 请求
function post(url, data) {
  return request(url, 'POST', data)
}

// ===== 用户认证 =====
export function login(username, password) {
  return post('/api/auth/login', { username, password })
}

export function register(data) {
  return post('/api/auth/register', data)
}

// ===== AI 问诊 =====
export function chatWithAI(memoryId, message) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${BASE_URL}/xiaozhi/chat?memoryId=${memoryId}&message=${encodeURIComponent(message)}`,
      method: 'GET',
      responseType: 'text',
      enableChunked: true,
      success(res) {
        resolve(res.data)
      },
      fail(err) {
        reject(new Error('AI 服务暂时不可用'))
      }
    })
  })
}

// ===== 科室医生 =====
export function getDoctors(department) {
  return get('/api/doctor/list', { department })
}

// ===== 预约挂号 =====
export function getSchedules(department, date, time) {
  return get('/api/schedule/query', { department, date, time })
}

export function bookAppointment(data) {
  return post('/api/appointment/book', data)
}

export function getAppointments(idCard) {
  return get('/api/appointment/list', { idCard })
}

export function cancelAppointment(id) {
  return request(`/api/appointment/cancel/${id}`, 'DELETE')
}