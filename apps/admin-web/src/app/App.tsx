import { Route, Routes } from 'react-router-dom'
import { DashboardPage } from '@/features/dashboard'
import { LoginPage, ProtectedRoute } from '@/features/auth'
import { AdminLayout } from '@/components/layout/AdminLayout'
import { UsersPage } from '@/features/users'
import { PermissionsPage } from '@/features/permissions'
import { MenuManagementPage } from '@/features/menus'
import { FileManagementPage } from '@/features/files'
import { AccessHistoryPage } from '@/features/audit'

export function App() {
  return (
    <Routes>
      <Route element={<LoginPage />} path="/login" />
      <Route element={<ProtectedRoute />}>
        <Route element={<AdminLayout />}>
          <Route element={<DashboardPage />} path="/" />
          <Route element={<UsersPage />} path="/users" />
          <Route element={<PermissionsPage />} path="/permissions" />
          <Route element={<MenuManagementPage />} path="/menu-management" />
          <Route element={<FileManagementPage />} path="/files" />
          <Route element={<AccessHistoryPage />} path="/access-history" />
        </Route>
      </Route>
    </Routes>
  )
}
