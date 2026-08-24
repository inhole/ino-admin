import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, expect, test } from 'vitest'
import { ApiClientError } from '@/api/client'
import { ErrorState } from '@/components/states/PageStates'

afterEach(cleanup)

test('renders a shared forbidden message and inquiry code for a 403 error', () => {
  render(
    <ErrorState
      error={
        new ApiClientError(
          '서버의 상세 권한 오류',
          403,
          'FORBIDDEN',
          '01K3TRACE53',
        )
      }
      forbiddenDescription="사용자 목록을 볼 권한이 없습니다."
      title="사용자 목록을 불러올 수 없습니다."
    />,
  )

  expect(screen.getByRole('alert')).toHaveTextContent(
    '사용자 목록을 볼 권한이 없습니다.',
  )
  expect(screen.getByText('문의 코드: 01K3TRACE53')).toBeInTheDocument()
  expect(screen.queryByText('서버의 상세 권한 오류')).not.toBeInTheDocument()
})
