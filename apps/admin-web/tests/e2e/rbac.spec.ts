import { expect, test, type Page, type Route } from '@playwright/test'

const tokens = {
  accessToken: 'e2e-access-token',
  tokenType: 'Bearer',
  expiresIn: 900,
  refreshToken: 'e2e-refresh-token',
}

const managementMenus = [
  { id: 'dashboard', label: '대시보드', route: '/', icon: 'layout-dashboard', order: 10, children: [] },
  { id: 'users', label: '사용자 관리', route: '/users', icon: 'users', order: 20, children: [] },
  { id: 'permissions', label: '권한 관리', route: '/permissions', icon: 'key-round', order: 30, children: [] },
  { id: 'menus', label: '메뉴 관리', route: '/menu-management', icon: 'menu', order: 40, children: [] },
]

async function json(route: Route, body: unknown, status = 200) {
  await route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })
}

async function authenticate(page: Page, role: 'SUPER_ADMIN' | 'VIEWER') {
  const isSuperAdmin = role === 'SUPER_ADMIN'
  await page.route('**/api/v1/**', async (route) => {
    const path = new URL(route.request().url()).pathname
    if (path === '/api/v1/auth/login') return json(route, tokens)
    if (path === '/api/v1/auth/refresh') return json(route, tokens)
    if (path === '/api/v1/auth/me') return json(route, {
      id: role.toLowerCase(), email: `${role.toLowerCase()}@example.com`, displayName: role,
      status: 'ACTIVE', role, permissions: isSuperAdmin ? ['user:read', 'user:create', 'user:update', 'permission:read', 'permission:update', 'menu:read', 'menu:write'] : [],
    })
    if (path === '/api/v1/menus/me') return json(route, isSuperAdmin ? managementMenus : managementMenus.slice(0, 1))
    if (path === '/api/v1/users') return isSuperAdmin
      ? json(route, { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
      : json(route, { code: 'FORBIDDEN', message: '접근 권한이 없습니다.' }, 403)
    if (path === '/api/v1/permissions') return json(route, [])
    if (path === '/api/v1/samples') return json(route, { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
    return json(route, { code: 'NOT_FOUND', message: path }, 404)
  })

  await page.goto('/login')
  await page.getByLabel('이메일').fill(`${role.toLowerCase()}@example.com`)
  await page.getByLabel('비밀번호').fill('e2e-password-1234')
  await page.getByRole('button', { name: '로그인' }).click()
  await expect(page).toHaveURL('/')
}

test('SUPER_ADMIN에게 관리 메뉴를 모두 노출한다', async ({ page }) => {
  await authenticate(page, 'SUPER_ADMIN')

  await expect(page.getByRole('link', { name: '사용자 관리' })).toBeVisible()
  await expect(page.getByRole('link', { name: '권한 관리' })).toBeVisible()
  await expect(page.getByRole('link', { name: '메뉴 관리' })).toBeVisible()
})

test('VIEWER는 관리 메뉴가 없고 직접 접근해도 서버의 403을 처리한다', async ({ page }) => {
  await authenticate(page, 'VIEWER')

  await expect(page.getByRole('link', { name: '사용자 관리' })).toHaveCount(0)
  await expect(page.getByRole('link', { name: '권한 관리' })).toHaveCount(0)
  await page.goto('/users')
  await expect(page.getByRole('alert')).toContainText('사용자 목록을 볼 권한이 없습니다.')
})
