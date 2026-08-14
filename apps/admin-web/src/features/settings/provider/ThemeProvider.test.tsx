import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, expect, test } from 'vitest'
import { ThemeSelector } from '../component/ThemeSelector'
import { ThemeProvider } from './ThemeProvider'

beforeEach(() => localStorage.clear())
afterEach(cleanup)

test('uses system theme by default and persists a user selection', () => {
  render(<ThemeProvider><ThemeSelector /></ThemeProvider>)
  expect(screen.getByRole('button', { name: '시스템' })).toHaveAttribute('aria-pressed', 'true')
  fireEvent.click(screen.getByRole('button', { name: '다크' }))
  expect(document.documentElement).toHaveClass('dark')
  expect(localStorage.getItem('ino-admin.theme')).toBe('dark')
})
