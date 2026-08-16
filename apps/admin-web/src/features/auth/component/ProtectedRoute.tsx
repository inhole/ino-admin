import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '@/features/auth/hook/useAuth'
import { LoadingPanel } from '@/components/layout/Page'

export function ProtectedRoute() {
  const { t } = useTranslation('auth')
  const { isRestoring, user } = useAuth()
  const location = useLocation()

  if (isRestoring) return <main className="centered-page"><LoadingPanel label={t('restoring')} /></main>
  if (!user) return <Navigate replace state={{ from: location.pathname }} to="/login" />
  return <Outlet />
}
