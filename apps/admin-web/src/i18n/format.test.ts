import { expect, test } from 'vitest'
import { formatDate, formatFileSize } from './format'

test('formats files and dates with the active locale', () => {
  expect(formatFileSize(1536)).toBe('1.5 KB')
  expect(formatDate('2026-08-14T00:00:00Z')).toContain('2026')
})
