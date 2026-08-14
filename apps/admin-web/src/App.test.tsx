import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import { App } from '@/app/App'
import { AuthProvider } from '@/features/auth/model/AuthContext'

beforeEach(() => sessionStorage.clear())
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

function json(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: { 'Content-Type': 'application/json' } })
}

function renderApp(path = '/') {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={client}><MemoryRouter initialEntries={[path]}><AuthProvider><App /></AuthProvider></MemoryRouter></QueryClientProvider>)
}

test('redirects unauthenticated users to login', async () => {
  renderApp()
  expect(await screen.findByRole('heading', { name: '관리자 로그인' })).toBeInTheDocument()
  expect(screen.queryByText('백엔드 연결 상태')).not.toBeInTheDocument()
})

test('logs in and renders the protected dashboard', async () => {
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const url = String(input)
    if (url.endsWith('/auth/login') && init?.method === 'POST') return json({ accessToken: 'access-1', tokenType: 'Bearer', expiresIn: 900, refreshToken: 'refresh-1' })
    if (url.endsWith('/auth/me')) return json({ id: 'user-1', email: 'admin@example.com', displayName: '관리자', status: 'ACTIVE' })
    if (url.endsWith('/samples')) return json({ content: [{ id: 1, name: '서버 연결' }], page: 0, size: 20, totalElements: 1, totalPages: 1 })
    throw new Error(`Unexpected request: ${url}`)
  })

  renderApp()
  fireEvent.change(await screen.findByLabelText('이메일'), { target: { value: 'admin@example.com' } })
  fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'Admin-Password-2026!' } })
  fireEvent.click(screen.getByRole('button', { name: '로그인' }))

  expect(await screen.findByText('서버 연결')).toBeInTheDocument()
  expect(screen.getByText('관리자')).toBeInTheDocument()
  expect(screen.getByText('정상')).toBeInTheDocument()
  expect(sessionStorage.getItem('ino-admin.refresh-token')).toBe('refresh-1')
})

test('restores authentication by rotating the refresh token', async () => {
  sessionStorage.setItem('ino-admin.refresh-token', 'refresh-old')
  const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
    const url = String(input)
    if (url.endsWith('/auth/refresh')) return json({ accessToken: 'access-new', tokenType: 'Bearer', expiresIn: 900, refreshToken: 'refresh-new' })
    if (url.endsWith('/auth/me')) return json({ id: 'user-1', email: 'admin@example.com', displayName: '관리자', status: 'ACTIVE' })
    if (url.endsWith('/samples')) return json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
    throw new Error(`Unexpected request: ${url}`)
  })

  renderApp()

  expect(await screen.findByText('표시할 항목이 없습니다.')).toBeInTheDocument()
  expect(sessionStorage.getItem('ino-admin.refresh-token')).toBe('refresh-new')
  expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/auth/refresh'), expect.objectContaining({ method: 'POST' }))
})

test('shows the server authentication error without exposing account state', async () => {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue(json({ code: 'INVALID_CREDENTIALS', message: '이메일 또는 비밀번호가 올바르지 않습니다.' }, 401))
  renderApp('/login')
  fireEvent.change(await screen.findByLabelText('이메일'), { target: { value: 'admin@example.com' } })
  fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'Wrong-Password!' } })
  fireEvent.click(screen.getByRole('button', { name: '로그인' }))

  expect(await screen.findByRole('alert')).toHaveTextContent('이메일 또는 비밀번호가 올바르지 않습니다.')
})
