import { DoorOpen, LogOut, MapPin } from 'lucide-react'
import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'

export function AppShell() {
  const { user, signOut } = useAuth()
  const navigate = useNavigate()
  return (
    <div className="app-shell">
      <header className="topbar">
        <Link className="brand" to="/espaces" aria-label="SmartCampus, accueil des espaces">
          <span className="brand-mark"><MapPin size={20} /></span>
          <span>smart<span>campus</span></span>
        </Link>
        <nav className="top-nav"><Link className="nav-link active" to="/espaces"><DoorOpen size={17} /> Espaces</Link></nav>
        <div className="user-menu">
          <div className="user-avatar">{user?.prenom?.slice(0, 1) ?? 'U'}</div>
          <div className="user-details"><strong>{user?.prenom} {user?.nom}</strong><small>{user?.role}</small></div>
          <button className="icon-button" aria-label="Se déconnecter" title="Se déconnecter" onClick={async () => { await signOut(); navigate('/') }}><LogOut size={18} /></button>
        </div>
      </header>
      <main><Outlet /></main>
    </div>
  )
}
