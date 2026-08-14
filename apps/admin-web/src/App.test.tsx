import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import { App } from './App'

afterEach(() => vi.restoreAllMocks())

test('renders samples returned by the server', async () => {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({
    content: [{ id: 1, name: '서버 연결' }], page: 0, size: 20, totalElements: 1, totalPages: 1
  }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={client}><App /></QueryClientProvider>)
  expect(await screen.findByText('서버 연결')).toBeInTheDocument()
  expect(screen.getByText('정상')).toBeInTheDocument()
})
