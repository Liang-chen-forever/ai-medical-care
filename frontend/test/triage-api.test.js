import test from 'node:test'
import assert from 'node:assert/strict'
import { routeAccess } from '../src/utils/routeAccess.js'
import { api, createTriageCase, getTriageCase } from '../src/api/index.js'

test('triage API contract helpers and patient route access', async () => {
  assert.deepEqual(routeAccess({ requiresAuth: true, role: 'PATIENT' }, true, 'DOCTOR'), { name: 'Chat' })
})

test('triage wrappers use exact payload and detail path', async () => {
  const originalPost = api.post
  const calls = []
  api.post = async (...args) => { calls.push(args); return { data: {} } }
  const originalGet = api.get
  api.get = async (...args) => { calls.push(args); return { data: {} } }
  try {
    await createTriageCase('反复头痛')
    await getTriageCase(31)
  } finally { api.post = originalPost; api.get = originalGet }
  assert.deepEqual(calls[0], ['/api/v1/triage/cases', { chiefComplaint: '反复头痛' }])
  assert.deepEqual(calls[1], ['/api/v1/triage/cases/31'])
})
