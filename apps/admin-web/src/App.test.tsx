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
    if (url.endsWith('/auth/me')) return json({ id: 'user-1', email: 'admin@example.com', displayName: '관리자', status: 'ACTIVE', role: 'SUPER_ADMIN', permissions: ['user:read', 'user:create', 'user:update', 'permission:read'] })
    if (url.endsWith('/menus/me')) return json([{ id: 'dashboard', label: '대시보드', route: '/', icon: 'layout-dashboard', order: 10, children: [] }, { id: 'users', label: '사용자', route: '/users', icon: 'users', order: 20, children: [] }, { id: 'permissions', label: '권한', route: '/permissions', icon: 'key-round', order: 30, children: [] }])
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
    if (url.endsWith('/auth/me')) return json({ id: 'user-1', email: 'admin@example.com', displayName: '관리자', status: 'ACTIVE', role: 'SUPER_ADMIN', permissions: ['user:read', 'user:create', 'user:update', 'permission:read'] })
    if (url.endsWith('/menus/me')) return json([{ id: 'dashboard', label: '대시보드', route: '/', icon: 'layout-dashboard', order: 10, children: [] }])
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

test('navigates to the authenticated user directory', async () => {
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const url = String(input)
    if (url.endsWith('/auth/login') && init?.method === 'POST') return json({ accessToken: 'access-1', tokenType: 'Bearer', expiresIn: 900, refreshToken: 'refresh-1' })
    if (url.endsWith('/auth/me')) return json({ id: 'user-1', email: 'admin@example.com', displayName: '관리자', status: 'ACTIVE', role: 'SUPER_ADMIN', permissions: ['user:read', 'user:create', 'user:update', 'permission:read'] })
    if (url.endsWith('/menus/me')) return json([{ id: 'dashboard', label: '대시보드', route: '/', icon: 'layout-dashboard', order: 10, children: [] }, { id: 'users', label: '사용자', route: '/users', icon: 'users', order: 20, children: [] }])
    if (url.endsWith('/samples')) return json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
    if (url.endsWith('/users')) return json({ content: [{ id: 'user-1', email: 'admin@example.com', displayName: '관리자', status: 'ACTIVE', createdAt: '2026-08-14T00:00:00Z' }], page: 0, size: 20, totalElements: 1, totalPages: 1 })
    throw new Error(`Unexpected request: ${url}`)
  })

  renderApp('/login')
  fireEvent.change(await screen.findByLabelText('이메일'), { target: { value: 'admin@example.com' } })
  fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'Admin-Password-2026!' } })
  fireEvent.click(screen.getByRole('button', { name: '로그인' }))
  fireEvent.click(await screen.findByRole('link', { name: '사용자' }))

  expect(await screen.findByRole('heading', { name: '사용자 관리' })).toBeInTheDocument()
  expect(screen.getByText('admin@example.com')).toBeInTheDocument()
})

test('super admin creates a viewer from the user directory', async () => {
  let created = false
  let disabled = false
  let displayName = '뷰어'
  let role = 'VIEWER'
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const url = String(input)
    if (url.endsWith('/auth/login')) return json({ accessToken: 'access-1', tokenType: 'Bearer', expiresIn: 900, refreshToken: 'refresh-1' })
    if (url.endsWith('/auth/me')) return json({ id: 'user-1', email: 'admin@example.com', displayName: '관리자', status: 'ACTIVE', role: 'SUPER_ADMIN', permissions: ['user:read', 'user:create', 'user:update', 'permission:read'] })
    if (url.endsWith('/menus/me')) return json([{ id: 'dashboard', label: '대시보드', route: '/', icon: 'layout-dashboard', order: 10, children: [] }, { id: 'users', label: '사용자', route: '/users', icon: 'users', order: 20, children: [] }, { id: 'permissions', label: '권한', route: '/permissions', icon: 'key-round', order: 30, children: [] }])
    if (url.endsWith('/samples')) return json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
    if (url.endsWith('/permissions')) return json([{ role: 'SUPER_ADMIN', permissions: ['permission:read', 'user:create', 'user:read', 'user:update'] }, { role: 'ADMIN', permissions: ['user:read'] }, { role: 'VIEWER', permissions: [] }])
    if (url.endsWith('/users') && init?.method === 'POST') { created = true; return json({ id: 'user-2', email: 'viewer@example.com', displayName: '뷰어', status: 'ACTIVE', role: 'VIEWER', createdAt: '2026-08-14T00:00:00Z' }, 201) }
    if (url.endsWith('/users/user-2') && init?.method === 'PATCH') { displayName = '운영자'; role = 'ADMIN'; return json({ id: 'user-2', displayName, role }) }
    if (url.endsWith('/users/user-2/status') && init?.method === 'PATCH') { disabled = true; return json({ id: 'user-2', status: 'DISABLED' }) }
    if (url.endsWith('/users/user-2')) return json({ id: 'user-2', email: 'viewer@example.com', displayName, status: disabled ? 'DISABLED' : 'ACTIVE', role, createdAt: '2026-08-14T00:00:00Z' })
    if (url.endsWith('/users')) return json({ content: created ? [{ id: 'user-2', email: 'viewer@example.com', displayName, status: disabled ? 'DISABLED' : 'ACTIVE', role, createdAt: '2026-08-14T00:00:00Z' }] : [], page: 0, size: 20, totalElements: created ? 1 : 0, totalPages: created ? 1 : 0 })
    throw new Error(`Unexpected request: ${url}`)
  })

  renderApp('/login')
  fireEvent.change(await screen.findByLabelText('이메일'), { target: { value: 'admin@example.com' } })
  fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'Admin-Password-2026!' } })
  fireEvent.click(screen.getByRole('button', { name: '로그인' }))
  fireEvent.click(await screen.findByRole('link', { name: '사용자' }))
  fireEvent.change(await screen.findByLabelText('이름'), { target: { value: '뷰어' } })
  fireEvent.change(screen.getByLabelText('이메일', { selector: '#new-user-email' }), { target: { value: 'viewer@example.com' } })
  fireEvent.change(screen.getByLabelText('초기 비밀번호'), { target: { value: 'Viewer-Password-2026!' } })
  fireEvent.click(screen.getByRole('button', { name: '사용자 생성' }))

  expect(await screen.findByText('뷰어 사용자를 생성했습니다.')).toBeInTheDocument()
  expect(await screen.findByText('viewer@example.com')).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: '수정' }))
  fireEvent.change(await screen.findByLabelText('수정할 이름'), { target: { value: '운영자' } })
  fireEvent.change(screen.getByLabelText('수정할 역할'), { target: { value: 'ADMIN' } })
  fireEvent.click(screen.getByRole('button', { name: '저장' }))
  expect(await screen.findByText('운영자')).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: '비활성화' }))
  expect(await screen.findByRole('button', { name: '활성화' })).toBeInTheDocument()
  fireEvent.click(screen.getByRole('link', { name: '권한' }))
  expect(await screen.findByRole('heading', { name: '권한 카탈로그' })).toBeInTheDocument()
  expect(await screen.findByText('permission:read')).toBeInTheDocument()
})
