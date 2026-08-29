import { expect, test, type Page, type Route } from '@playwright/test'

const tokens = {
  accessToken: 'e2e-access-token',
  tokenType: 'Bearer',
  expiresIn: 900,
  refreshToken: 'e2e-refresh-token',
}

const managementMenus = [
  { id: 'dashboard', label: '대시보드', route: '/', icon: 'layout-dashboard', order: 10, children: [] },
  { id: 'user-management', label: '사용자 관리', route: '/users', icon: 'users', order: 20, children: [
    { id: 'users', label: '사용자', route: '/users', icon: 'users', order: 10, children: [] },
    { id: 'permissions', label: '권한 관리', route: '/permissions', icon: 'key-round', order: 10, children: [] },
    { id: 'access-history', label: '접속 이력', route: '/access-history', icon: 'history', order: 20, children: [] },
  ] },
  { id: 'menus', label: '메뉴 관리', route: '/menu-management', icon: 'menu', order: 40, children: [] },
  { id: 'files', label: '파일 관리', route: '/files', icon: 'file', order: 50, children: [] },
]

async function json(route: Route, body: unknown, status = 200) {
  await route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })
}

async function authenticate(page: Page, role: 'SUPER_ADMIN' | 'VIEWER', files: unknown[] = []) {
  const isSuperAdmin = role === 'SUPER_ADMIN'
  await page.route('**/api/v1/**', async (route) => {
    const url = new URL(route.request().url())
    const path = url.pathname
    if (path === '/api/v1/auth/login') return json(route, tokens)
    if (path === '/api/v1/auth/refresh') return json(route, tokens)
    if (path === '/api/v1/auth/me') return json(route, {
      id: role.toLowerCase(), email: `${role.toLowerCase()}@example.com`, displayName: role,
      status: 'ACTIVE', role, permissions: isSuperAdmin ? ['user:read', 'user:create', 'user:update', 'permission:read', 'permission:update', 'menu:read', 'menu:write', 'access-history:read'] : [],
    })
    if (path === '/api/v1/menus/me') return json(route, isSuperAdmin ? managementMenus : managementMenus.slice(0, 1))
    if (path === '/api/v1/users') {
      if (!isSuperAdmin) {
        return json(route, { code: 'FORBIDDEN', message: '접근 권한이 없습니다.', traceId: '01K3E2ETRACE53' }, 403)
      }
      const pageNumber = Number(url.searchParams.get('page') ?? '0')
      if (
        url.searchParams.get('query') === 'kim'
        && url.searchParams.get('role') === 'ADMIN'
        && (pageNumber === 0 || pageNumber === 1)
      ) {
        const content = pageNumber === 0
          ? [{ id: 'user-kim-1', email: 'kim@example.com', displayName: '김 관리자', status: 'ACTIVE', role: 'ADMIN', createdAt: '2026-08-01T00:00:00Z' }]
          : [{ id: 'user-kim-2', email: 'kim.page2@example.com', displayName: '김 관리자 2', status: 'ACTIVE', role: 'ADMIN', createdAt: '2026-08-02T00:00:00Z' }]
        return json(route, { content, page: pageNumber, size: 20, totalElements: 21, totalPages: 2 })
      }
      return json(route, { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
    }
    if (path === '/api/v1/users/roles') return json(route, [
      { role: 'ADMIN', displayName: 'ADMIN' },
    ])
    if (path === '/api/v1/permissions') return json(route, [
      { role: 'ADMIN', displayName: 'ADMIN', systemRole: true, enabled: true, permissions: ['user:read'] },
    ])
    if (path === '/api/v1/access-history') return json(route, {
      content: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
    })
    if (path === '/api/v1/files') return json(route, {
      content: files, page: 0, size: 20, totalElements: files.length, totalPages: files.length > 0 ? 1 : 0,
    })
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

  const userManagement = page.getByRole('button', { name: '사용자 관리' })
  await expect(userManagement).toHaveAttribute('aria-expanded', 'false')
  await userManagement.click()
  await expect(userManagement).toHaveAttribute('aria-expanded', 'true')
  await expect(page.getByRole('link', { name: '사용자' })).toBeVisible()
  await expect(page.getByRole('link', { name: '권한' })).toBeVisible()
  await expect(page.getByRole('link', { name: '접속 이력' })).toBeVisible()
  await expect(page.getByRole('link', { name: '메뉴 관리' })).toBeVisible()
  await page.getByRole('button', { name: '계정 메뉴' }).click()
  await expect(page.getByRole('menuitem', { name: '로그아웃' })).toBeVisible()
  await page.keyboard.press('Escape')
  await page.getByRole('link', { name: '접속 이력' }).click()
  await expect(page.getByRole('heading', { name: '접속 이력' })).toBeVisible()
  await page.getByRole('link', { name: '파일 관리' }).click()
  await expect(page.getByRole('heading', { name: '파일 관리' })).toBeVisible()
  await expect(page.getByText('업로드한 파일이 없습니다.')).toBeVisible()
})

test('SUPER_ADMIN이 사용자 조회 조건과 페이지를 URL에 유지한다', async ({ page }) => {
  await authenticate(page, 'SUPER_ADMIN')

  await page.getByRole('button', { name: '사용자 관리' }).click()
  await page.getByRole('link', { name: '사용자' }).click()
  await page.getByPlaceholder('이름 또는 이메일 검색').fill('kim')
  await expect(page).toHaveURL(/query=kim/)
  await page.getByRole('combobox', { name: '역할' }).last().click()
  await page.getByRole('option', { name: 'ADMIN' }).click()
  await expect(page).toHaveURL(/role=ADMIN/)
  await expect(page.getByText('kim@example.com').first()).toBeVisible()
  await page.getByRole('button', { name: '다음 페이지' }).click()
  await expect(page).toHaveURL(/page=1/)
  await expect(page.getByText('kim.page2@example.com').first()).toBeVisible()
  await page.reload()
  await expect(page.getByPlaceholder('이름 또는 이메일 검색')).toHaveValue('kim')
})

test('VIEWER는 관리 메뉴가 없고 직접 접근해도 서버의 403을 처리한다', async ({ page }) => {
  await authenticate(page, 'VIEWER')

  await expect(page.getByRole('link', { name: '사용자' })).toHaveCount(0)
  await expect(page.getByRole('link', { name: '권한' })).toHaveCount(0)
  await page.goto('/users')
  await expect(page.getByRole('alert')).toContainText('사용자 목록을 볼 권한이 없습니다.')
  await expect(page.getByRole('alert')).toContainText('문의 코드: 01K3E2ETRACE53')
})

test('모바일에서 메뉴를 열어 파일 관리로 이동한다', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 })
  await authenticate(page, 'SUPER_ADMIN')
  await page.getByRole('button', { name: '메뉴 열기' }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await page.getByRole('dialog').getByRole('link', { name: '파일 관리' }).click()
  await expect(page.getByRole('heading', { name: '파일 관리' })).toBeVisible()
})

test('파일 작업 메뉴에서 상세 정보를 확인한다', async ({ page }) => {
  await authenticate(page, 'SUPER_ADMIN', [{
    id: 'file-1', originalName: 'report.pdf', contentType: 'application/pdf',
    size: 2048, createdAt: '2026-08-16T00:00:00Z',
  }])
  await page.getByRole('link', { name: '파일 관리' }).click()

  await page.getByRole('button', { name: 'report.pdf 작업 메뉴' }).click()
  await page.getByRole('menuitem', { name: '상세 보기' }).click()

  const sheet = page.getByRole('dialog', { name: '파일 상세' })
  await expect(sheet).toBeVisible()
  await expect(sheet.getByText('application/pdf')).toBeVisible()
  await expect(sheet.getByText('2 KB')).toBeVisible()
  await sheet.getByRole('button', { name: '닫기' }).click()
  await expect(sheet).toBeHidden()
})

test('선택한 다크 테마를 새로고침 후에도 유지한다', async ({ page }) => {
  await page.goto('/login')
  await page.getByRole('button', { name: '테마' }).click()
  await page.getByRole('menuitemradio', { name: '다크' }).click()
  await expect(page.locator('html')).toHaveClass(/dark/)
  await page.reload()
  await expect(page.locator('html')).toHaveClass(/dark/)
  await page.getByRole('button', { name: '테마' }).click()
  await expect(page.getByRole('menuitemradio', { name: '다크' })).toHaveAttribute('aria-checked', 'true')
})
