import assert from 'node:assert/strict'
import test from 'node:test'

const apiModule = await import('../src/api/index.js')

test('confirm wrapper posts only the appointment action path', async () => {
  const calls = []
  const originalPost = apiModule.api.post
  apiModule.api.post = (...args) => {
    calls.push(args)
    return Promise.resolve({ data: null })
  }

  try {
    await apiModule.confirmDoctorAppointment(55)
    assert.deepEqual(calls, [['/api/v1/doctor/appointments/55/confirm']])
  } finally {
    apiModule.api.post = originalPost
  }
})

test('rejection wrapper posts only the bounded rejection reason', async () => {
  const calls = []
  const originalPost = apiModule.api.post
  apiModule.api.post = (...args) => {
    calls.push(args)
    return Promise.resolve({ data: null })
  }

  try {
    await apiModule.rejectDoctorAppointment(55, '排班已满')
    assert.deepEqual(calls, [['/api/v1/doctor/appointments/55/reject', { reason: '排班已满' }]])
  } finally {
    apiModule.api.post = originalPost
  }
})

test('encounter wrapper posts summary and follow-up advice to the encounter endpoint', async () => {
  const calls = []
  const originalPost = apiModule.api.post
  apiModule.api.post = (...args) => {
    calls.push(args)
    return Promise.resolve({ data: null })
  }

  try {
    await apiModule.completeDoctorEncounter(55, '完成问诊摘要', '一周后复诊')
    assert.deepEqual(calls, [['/api/v1/doctor/appointments/55/encounter', {
      summary: '完成问诊摘要',
      followUpAdvice: '一周后复诊'
    }]])
  } finally {
    apiModule.api.post = originalPost
  }
})

test('patient encounter wrapper reads the appointment encounter resource', async () => {
  const calls = []
  const originalGet = apiModule.api.get
  apiModule.api.get = (...args) => {
    calls.push(args)
    return Promise.resolve({ data: null })
  }

  try {
    await apiModule.getAppointmentEncounter(55)
    assert.deepEqual(calls, [['/api/v1/appointments/55/encounter']])
  } finally {
    apiModule.api.get = originalGet
  }
})
