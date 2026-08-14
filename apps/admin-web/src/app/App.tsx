import { Route, Routes } from 'react-router-dom'
import { DashboardPage } from '@/features/dashboard/DashboardPage'
import { LoginPage } from '@/features/auth/ui/LoginPage'
import { ProtectedRoute } from '@/features/auth/ui/ProtectedRoute'
import '@/styles.css'

export function App() {
  return (
    <Routes>
      <Route element={<LoginPage />} path="/login" />
      <Route element={<ProtectedRoute />}><Route element={<DashboardPage />} path="/" /></Route>
    </Routes>
  )
}
