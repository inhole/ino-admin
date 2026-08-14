import { useContext } from 'react'
import { AuthContext } from '../provider/authContextValue'

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('AuthProvider 안에서 useAuth를 사용해야 합니다.')
  return context
}
