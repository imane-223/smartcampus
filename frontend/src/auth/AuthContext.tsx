import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'

import * as authApi from '../api/auth'
import type { User } from '../api/auth'
import { onUnauthorized } from '../api/client'
import { AuthContext } from './authState'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // Toute requête qui reçoit 401 (session expirée) déconnecte l'interface : ProtectedRoute renvoie vers /login
    onUnauthorized(() => setUser(null))
    return () => onUnauthorized(null)
  }, [])

  useEffect(() => {
    let cancelled = false
    // Restaure la session après un rechargement de page
    authApi
      .fetchCurrentUser()
      .then((current) => !cancelled && setUser(current))
      .catch(() => !cancelled && setUser(null))
      .finally(() => !cancelled && setLoading(false))
    return () => {
      cancelled = true
    }
  }, [])

  const login = useCallback(async (courriel: string, motDePasse: string) => {
    const loggedIn = await authApi.login(courriel, motDePasse)
    setUser(loggedIn)
    return loggedIn
  }, [])

  const value = useMemo(() => ({ user, loading, login }), [user, loading, login])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
