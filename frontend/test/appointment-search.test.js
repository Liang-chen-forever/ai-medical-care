import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const source = await readFile(new URL('../src/views/AppointmentView.vue', import.meta.url), 'utf8')

test('appointment search requires the selected period field', () => {
  const computedExpression = source.match(
    /const canSearch = computed\(\(\) => ([^\n]+)\)/
  )?.[1]

  assert.equal(
    computedExpression,
    'form.value.department && form.value.date && form.value.period'
  )
})
