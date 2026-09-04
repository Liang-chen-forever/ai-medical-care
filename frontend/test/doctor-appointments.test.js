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
