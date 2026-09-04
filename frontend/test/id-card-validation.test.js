import assert from 'node:assert/strict'
import test from 'node:test'

import { getIdCardValidationMessage } from '../src/utils/idCard.js'

test('registration explains why an invalid ID card keeps the submit button disabled', () => {
  assert.equal(
    getIdCardValidationMessage('445222522188455669'),
    '身份证号校验码不正确，请检查'
  )
})

test('registration accepts a valid 18-digit ID card', () => {
  assert.equal(getIdCardValidationMessage('11010519491231002X'), '')
})
