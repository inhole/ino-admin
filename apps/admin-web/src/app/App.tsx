import { Route, Routes } from 'react-router-dom'
import { DashboardPage } from '@/features/dashboard/DashboardPage'
import { LoginPage } from '@/features/auth/ui/LoginPage'
import { ProtectedRoute } from '@/features/auth/ui/ProtectedRoute'
import { AdminLayout } from '@/components/layout/AdminLayout'
import { UsersPage } from '@/features/users/UsersPage'
import '@/styles.css'

export function App() {
  return (
    <Routes>
      <Route element={<LoginPage />} path="/login" />
      <Route element={<ProtectedRoute />}><Route element={<AdminLayout />}><Route element={<DashboardPage />} path="/" /><Route element={<UsersPage />} path="/users" /></Route></Route>
    </Routes>
  )
}
