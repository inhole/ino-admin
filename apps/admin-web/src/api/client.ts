export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiError {
  code: string;
  message: string;
  traceId?: string;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  refreshToken: string;
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "";
const accessTokenKey = "ino-admin.access-token";
const refreshTokenKey = "ino-admin.refresh-token";
const sessionExpiredListeners = new Set<() => void>();
let refreshRequest: Promise<void> | null = null;

export class ApiClientError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code?: string,
  ) {
    super(message);
  }
}

function saveTokens(tokens: TokenResponse) {
  sessionStorage.setItem(accessTokenKey, tokens.accessToken);
  sessionStorage.setItem(refreshTokenKey, tokens.refreshToken);
}

export function hasRefreshToken() {
  return sessionStorage.getItem(refreshTokenKey) !== null;
}

export function clearSession() {
  sessionStorage.removeItem(accessTokenKey);
  sessionStorage.removeItem(refreshTokenKey);
}

export function onSessionExpired(listener: () => void) {
  sessionExpiredListeners.add(listener);
  return () => {
    sessionExpiredListeners.delete(listener);
  };
}

async function errorFrom(response: Response) {
  const error = (await response.json().catch(() => null)) as ApiError | null;
  return new ApiClientError(
    error?.message ?? i18n.t("common.connectionError"),
    response.status,
    error?.code,
  );
}

function authorizedHeaders(init?: HeadersInit) {
  const headers = new Headers(init);
  const accessToken = sessionStorage.getItem(accessTokenKey);
  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);
  return headers;
}

export async function request<T>(
  path: string,
  init: RequestInit = {},
  retryAfterRefresh = true,
): Promise<T> {
  const headers = authorizedHeaders(init.headers);
  if (init.body) headers.set("Content-Type", "application/json");
  const response = await fetch(`${apiBaseUrl}${path}`, { ...init, headers });
  if (response.status === 401 && retryAfterRefresh && hasRefreshToken()) {
    await refreshSession();
    return request<T>(path, init, false);
  }
  if (!response.ok) throw await errorFrom(response);
  if (
    response.status === 204 ||
    response.headers.get("Content-Length") === "0"
  ) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

export async function requestForm<T>(path: string, form: FormData) {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    method: "POST",
    headers: authorizedHeaders(),
    body: form,
  });
  if (!response.ok) throw await errorFrom(response);
  return response.json() as Promise<T>;
}

export async function requestBlob(path: string) {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    headers: authorizedHeaders(),
  });
  if (!response.ok) throw await errorFrom(response);
  return response.blob();
}

export async function createSession(email: string, password: string) {
  const response = await fetch(`${apiBaseUrl}/api/v1/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  if (!response.ok) throw await errorFrom(response);
  saveTokens((await response.json()) as TokenResponse);
}

export async function refreshSession() {
  if (refreshRequest) return refreshRequest;
  const refreshToken = sessionStorage.getItem(refreshTokenKey);
  if (!refreshToken) {
    throw new ApiClientError(i18n.t("common.authenticationRequired"), 401, "UNAUTHORIZED");
  }
  refreshRequest = (async () => {
    const response = await fetch(`${apiBaseUrl}/api/v1/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (!response.ok) {
      clearSession();
      sessionExpiredListeners.forEach((listener) => listener());
      throw await errorFrom(response);
    }
    saveTokens((await response.json()) as TokenResponse);
  })().finally(() => {
    refreshRequest = null;
  });
  return refreshRequest;
}

export async function destroySession() {
  const refreshToken = sessionStorage.getItem(refreshTokenKey);
  clearSession();
  if (!refreshToken) return;
  const response = await fetch(`${apiBaseUrl}/api/v1/auth/logout`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  });
  if (!response.ok) throw await errorFrom(response);
}
import i18n from "@/i18n";
