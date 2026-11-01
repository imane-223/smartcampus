import { useAuth } from '../auth/authState'
import { ROLE_LABELS } from '../auth/roles'

export function HomePage() {
  const { user } = useAuth()
  if (!user) {
    return null
  }

  return (
    <>
      <header className="topbar">
        <span className="brand">SmartCampus</span>
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
