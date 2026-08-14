import { Route, Routes } from 'react-router-dom'
import { DashboardPage } from './DashboardPage'
import { LoginPage } from './auth/LoginPage'
import { ProtectedRoute } from './auth/ProtectedRoute'
import './styles.css'

export function App() {
  return (
    <Routes>
      <Route element={<LoginPage />} path="/login" />
      <Route element={<ProtectedRoute />}><Route element={<DashboardPage />} path="/" /></Route>
    </Routes>
  )
}
