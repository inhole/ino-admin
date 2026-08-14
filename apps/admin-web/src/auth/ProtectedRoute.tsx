import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './useAuth'

export function ProtectedRoute() {
  const { isRestoring, user } = useAuth()
  const location = useLocation()

  if (isRestoring) return <main className="centered-page"><p role="status">인증 상태를 확인하는 중…</p></main>
  if (!user) return <Navigate replace state={{ from: location.pathname }} to="/login" />
  return <Outlet />
}
