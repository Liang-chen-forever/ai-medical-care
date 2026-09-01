import axios from 'axios'

const api = axios.create({
  baseURL: '/',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

// 响应拦截器：统一解包 Result 包装
api.interceptors.response.use(
  (response) => {
    const body = response.data
    // 后端统一返回 { code, message, data } 格式
    if (body && typeof body.code === 'number') {
      if (body.code === 200) {
        response.data = body.data
        response._message = body.message
        return response
      } else {
        return Promise.reject(new Error(body.message || '请求失败'))
      }
    }
    return response
  },
  (error) => {
    const msg = error.response?.data?.message || error.message || '网络错误'
    return Promise.reject(new Error(msg))
  }
)

// ========== AI 对话 ==========

/** 与 AI 流式聊天（返回 ReadableStream） */
export function chatWithAI(memoryId, userMessage) {
  return fetch('/xiaozhi/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ memoryId, userMessage })
  })
}

// ========== 科室与医生 ==========

/** 获取科室医生列表 */
export function getDoctors(department) {
  return api.get('/api/department/doctors', { params: { department } })
}

/** 获取科室排班可预约号源 */
export function getSchedules(department, date, time) {
  return api.get('/api/department/schedules', { params: { department, date, time } })
}

// ========== 预约管理 ==========

/** 查询用户预约 */
export function getAppointments(idCard) {
  return api.get('/api/appointment/list', { params: { idCard } })
}

/** 预约挂号 */
export function bookAppointment(data) {
  return api.post('/api/appointment/book', data)
}

/** 取消预约 */
export function cancelAppointment(id) {
  return api.delete(`/api/appointment/cancel/${id}`)
}

// ========== 知识库 ==========

/** 重新加载知识库 */
export function reloadKnowledge() {
  return api.post('/api/knowledge/reload')
}