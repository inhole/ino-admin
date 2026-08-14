import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/features/auth/model/useAuth'
import { LoadingPanel } from '@/components/layout/Page'

export function ProtectedRoute() {
  const { isRestoring, user } = useAuth()
  const location = useLocation()

  if (isRestoring) return <main className="centered-page"><LoadingPanel label="인증 상태를 확인하는 중…" /></main>
  if (!user) return <Navigate replace state={{ from: location.pathname }} to="/login" />
  return <Outlet />
}
