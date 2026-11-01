import { createContext, useContext } from 'react'

import type { User } from '../api/auth'

export interface AuthState {
  user: User | null
  /** true tant que la session n'a pas été vérifiée au chargement */
  loading: boolean
  login: (courriel: string, motDePasse: string) => Promise<User>
}

export const AuthContext = createContext<AuthState | null>(null)

export function useAuth(): AuthState {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth doit être utilisé dans un AuthProvider')
  }
  return context
}
