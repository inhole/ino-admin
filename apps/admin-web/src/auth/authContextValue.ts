import { createContext } from 'react'
import type { CurrentUser } from '../api/client'

export interface AuthContextValue {
  user: CurrentUser | null
  isRestoring: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
