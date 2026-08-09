export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Sample {
  id: number
  name: string
}

export interface ApiError {
  code: string
  message: string
  traceId?: string
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

export async function getSamples(): Promise<PageResponse<Sample>> {
  const response = await fetch(`${apiBaseUrl}/api/v1/samples`)
  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as ApiError | null
    throw new Error(error?.message ?? '서버에 연결할 수 없습니다.')
  }
  return response.json() as Promise<PageResponse<Sample>>
}
