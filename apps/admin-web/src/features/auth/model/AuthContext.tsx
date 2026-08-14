import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { clearSession, getCurrentUser, hasRefreshToken, login as loginRequest, logout as logoutRequest, onSessionExpired, refreshSession, type CurrentUser } from '@/api/client'
import { AuthContext, type AuthContextValue } from './authContextValue'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [isRestoring, setIsRestoring] = useState(true)

  useEffect(() => {
    const unsubscribe = onSessionExpired(() => setUser(null))
    const restore = async () => {
      if (!hasRefreshToken()) return
      try {
        await refreshSession()
        setUser(await getCurrentUser())
      } catch {
        clearSession()
      }
    }
    void restore().finally(() => setIsRestoring(false))
    return unsubscribe
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    user,
    isRestoring,
    login: async (email, password) => {
      await loginRequest(email, password)
      setUser(await getCurrentUser())
    },
    logout: async () => {
      setUser(null)
      await logoutRequest()
    },
  }), [isRestoring, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
