import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { useAuth } from '../auth/authState'
import { ROLE_LABELS } from '../auth/roles'

export function HomePage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [leaving, setLeaving] = useState(false)

  if (!user) {
    return null
  }

  async function handleLogout() {
    setLeaving(true)
    try {
      await logout()
    } finally {
      navigate('/login', { replace: true })
    }
  }

  return (
    <>
      <header className="topbar">
        <span className="brand">SmartCampus</span>
        <button type="button" className="secondary" onClick={handleLogout} disabled={leaving}>
          {leaving ? 'Déconnexion…' : 'Se déconnecter'}
        </button>
      </header>
      <main className="home">
        <h1>
          Bonjour {user.prenom} {user.nom}
        </h1>
        <p>
          Vous êtes connecté en tant que <span className="badge">{ROLE_LABELS[user.role]}</span>
        </p>
        <p className="muted">{user.courriel}</p>
      </main>
    </>
  )
}
