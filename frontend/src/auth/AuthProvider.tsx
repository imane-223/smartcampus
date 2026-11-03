import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { login as apiLogin, logout as apiLogout, request } from '../api/http'
import type { User } from '../types'

interface AuthContextValue {
  user: User | null
  loading: boolean
  signIn: (email: string, password: string) => Promise<void>
  signOut: () => Promise<void>
  refresh: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    try {
      setUser(await request<User>('/api/auth/me'))
    } catch {
      setUser(null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { void refresh() }, [refresh])

  const signIn = useCallback(async (email: string, password: string) => {
    await apiLogin(email, password)
    setUser(await request<User>('/api/auth/me'))
  }, [])

  const signOut = useCallback(async () => {
    await apiLogout()
    setUser(null)
  }, [])

  const value = useMemo(() => ({ user, loading, signIn, signOut, refresh }), [user, loading, signIn, signOut, refresh])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth doit être utilisé dans AuthProvider.')
  return value
}
