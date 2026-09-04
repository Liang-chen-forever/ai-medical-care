import test from 'node:test'
import assert from 'node:assert/strict'
import { routeAccess } from '../src/utils/routeAccess.js'

test('triage API contract helpers and patient route access', async () => {
  assert.deepEqual(routeAccess({ requiresAuth: true, role: 'PATIENT' }, true, 'DOCTOR'), { name: 'Chat' })
})
