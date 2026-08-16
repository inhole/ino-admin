import '@testing-library/jest-dom/vitest'
import { beforeEach } from 'vitest'
import i18n from '@/i18n'

beforeEach(async () => {
  localStorage.removeItem('ino-admin.locale')
  await i18n.changeLanguage('ko')
})

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({ matches: false, media: query, onchange: null, addEventListener: () => undefined, removeEventListener: () => undefined, addListener: () => undefined, removeListener: () => undefined, dispatchEvent: () => true }),
})
