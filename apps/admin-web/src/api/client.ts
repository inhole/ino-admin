import i18n from "@/i18n";

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
    public readonly traceId?: string,
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
    error?.traceId,
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

function formErrorFrom(request: XMLHttpRequest) {
  const error = request.response as ApiError | null;
  return new ApiClientError(
    error?.message ?? i18n.t("common.connectionError"),
    request.status,
    error?.code,
    error?.traceId,
  );
}

function sendForm<T>(
  path: string,
  form: FormData,
  onProgress?: (percentage: number) => void,
) {
  return new Promise<T>((resolve, reject) => {
    const request = new XMLHttpRequest();
    request.open("POST", `${apiBaseUrl}${path}`);
    request.responseType = "json";
    authorizedHeaders().forEach((value, name) =>
      request.setRequestHeader(name, value),
    );
    request.upload.addEventListener("progress", (event) => {
      if (event.lengthComputable) {
        onProgress?.(Math.round((event.loaded / event.total) * 100));
      }
    });
    request.addEventListener("load", () => {
      if (request.status >= 200 && request.status < 300) {
        onProgress?.(100);
        resolve(request.response as T);
        return;
      }
      reject(formErrorFrom(request));
    });
    request.addEventListener("error", () => reject(formErrorFrom(request)));
    request.send(form);
  });
}

export async function requestForm<T>(
  path: string,
  form: FormData,
  onProgress?: (percentage: number) => void,
  retryAfterRefresh = true,
): Promise<T> {
  try {
    return await sendForm<T>(path, form, onProgress);
  } catch (caught) {
    if (
      caught instanceof ApiClientError &&
      caught.status === 401 &&
      retryAfterRefresh &&
      hasRefreshToken()
    ) {
      await refreshSession();
      return requestForm<T>(path, form, onProgress, false);
    }
    throw caught;
  }
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
