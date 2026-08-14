export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Sample { id: number; name: string }
export interface ApiError { code: string; message: string; traceId?: string }
export interface CurrentUser { id: string; email: string; displayName: string; status: string; role: 'SUPER_ADMIN' | 'ADMIN' | 'VIEWER' }
export interface UserSummary { id: string; email: string; displayName: string; status: string; role: 'SUPER_ADMIN' | 'ADMIN' | 'VIEWER'; createdAt: string }

interface TokenResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  refreshToken: string
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
const accessTokenKey = 'ino-admin.access-token'
const refreshTokenKey = 'ino-admin.refresh-token'
const sessionExpiredListeners = new Set<() => void>()
let refreshRequest: Promise<void> | null = null

export class ApiClientError extends Error {
  constructor(message: string, public readonly status: number, public readonly code?: string) {
    super(message)
  }
}

function saveTokens(tokens: TokenResponse) {
  sessionStorage.setItem(accessTokenKey, tokens.accessToken)
  sessionStorage.setItem(refreshTokenKey, tokens.refreshToken)
}

export function hasRefreshToken() { return sessionStorage.getItem(refreshTokenKey) !== null }

export function clearSession() {
  sessionStorage.removeItem(accessTokenKey)
  sessionStorage.removeItem(refreshTokenKey)
}

export function onSessionExpired(listener: () => void) {
  sessionExpiredListeners.add(listener)
  return () => { sessionExpiredListeners.delete(listener) }
}

async function errorFrom(response: Response) {
  const error = (await response.json().catch(() => null)) as ApiError | null
  return new ApiClientError(error?.message ?? '서버에 연결할 수 없습니다.', response.status, error?.code)
}

async function request<T>(path: string, init: RequestInit = {}, retryAfterRefresh = true): Promise<T> {
  const headers = new Headers(init.headers)
  const accessToken = sessionStorage.getItem(accessTokenKey)
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  if (init.body) headers.set('Content-Type', 'application/json')

  const response = await fetch(`${apiBaseUrl}${path}`, { ...init, headers })
  if (response.status === 401 && retryAfterRefresh && hasRefreshToken()) {
    await refreshSession()
    return request<T>(path, init, false)
  }
  if (!response.ok) throw await errorFrom(response)
  if (response.status === 204 || response.headers.get('Content-Length') === '0') return undefined as T
  return response.json() as Promise<T>
}

export async function login(email: string, password: string) {
  const response = await fetch(`${apiBaseUrl}/api/v1/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password }),
  })
  if (!response.ok) throw await errorFrom(response)
  saveTokens(await response.json() as TokenResponse)
}

export async function refreshSession() {
  if (refreshRequest) return refreshRequest
  const refreshToken = sessionStorage.getItem(refreshTokenKey)
  if (!refreshToken) throw new ApiClientError('인증이 필요합니다.', 401, 'UNAUTHORIZED')

  refreshRequest = (async () => {
    const response = await fetch(`${apiBaseUrl}/api/v1/auth/refresh`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ refreshToken }),
    })
    if (!response.ok) {
      clearSession()
      sessionExpiredListeners.forEach((listener) => listener())
      throw await errorFrom(response)
    }
    saveTokens(await response.json() as TokenResponse)
  })().finally(() => { refreshRequest = null })
  return refreshRequest
}

export async function logout() {
  const refreshToken = sessionStorage.getItem(refreshTokenKey)
  clearSession()
  if (!refreshToken) return
  const response = await fetch(`${apiBaseUrl}/api/v1/auth/logout`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ refreshToken }),
  })
  if (!response.ok) throw await errorFrom(response)
}

export function getCurrentUser() { return request<CurrentUser>('/api/v1/auth/me') }
export function getSamples() { return request<PageResponse<Sample>>('/api/v1/samples') }
export function getUsers() { return request<PageResponse<UserSummary>>('/api/v1/users') }
export function createUser(input: { email: string; password: string; displayName: string; role: 'ADMIN' | 'VIEWER' }) {
  return request<UserSummary>('/api/v1/users', { method: 'POST', body: JSON.stringify(input) })
}
