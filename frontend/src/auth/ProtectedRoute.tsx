import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'

import { useAuth } from './authState'

/** Réservé aux utilisateurs connectés ; sinon, redirige vers /login en mémorisant la page demandée. */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return (
      <p className="loading" role="status">
        Chargement…
      </p>
    )
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return children
}
