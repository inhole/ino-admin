import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import { getSamples } from '@/features/dashboard/api/dashboardApi'
import { login } from '@/features/auth/api/authApi'
import { ApiClientError } from '@/api/client'

function json(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: { 'Content-Type': 'application/json' } })
}

beforeEach(() => sessionStorage.clear())
afterEach(() => vi.restoreAllMocks())

test('shares one refresh request across concurrent expired API calls', async () => {
  let refreshCalls = 0
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const url = String(input)
    if (url.endsWith('/auth/login')) return json({ accessToken: 'access-old', tokenType: 'Bearer', expiresIn: 900, refreshToken: 'refresh-old' })
    if (url.endsWith('/auth/refresh')) {
      refreshCalls++
      return json({ accessToken: 'access-new', tokenType: 'Bearer', expiresIn: 900, refreshToken: 'refresh-new' })
    }
    if (url.endsWith('/samples')) {
      const authorization = new Headers(init?.headers).get('Authorization')
      if (authorization === 'Bearer access-old') return json({ code: 'UNAUTHORIZED', message: '인증이 필요합니다.' }, 401)
      if (authorization === 'Bearer access-new') return json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
    }
    throw new Error(`Unexpected request: ${url}`)
  })

  await login('admin@example.com', 'Admin-Password-2026!')
  await Promise.all([getSamples(), getSamples()])

  expect(refreshCalls).toBe(1)
  expect(sessionStorage.getItem('ino-admin.refresh-token')).toBe('refresh-new')
})

test('preserves the trace ID from an API error response', async () => {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue(
    json(
      {
        code: 'FORBIDDEN',
        message: '접근 권한이 없습니다.',
        traceId: '01K3TRACE53',
      },
      403,
    ),
  )

  const error = await getSamples().catch((caught: unknown) => caught)

  expect(error).toBeInstanceOf(ApiClientError)
  expect(error).toMatchObject({
    status: 403,
    code: 'FORBIDDEN',
    traceId: '01K3TRACE53',
  })
})
