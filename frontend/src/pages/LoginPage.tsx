import { useState, type FormEvent } from 'react'
import { ArrowRight, Building2, LockKeyhole } from 'lucide-react'
import { Navigate } from 'react-router-dom'
import { ApiError } from '../api/http'
import { useAuth } from '../auth/AuthProvider'

export function LoginPage() {
  const { user, loading, signIn } = useAuth()
  const [courriel, setCourriel] = useState('')
  const [motDePasse, setMotDePasse] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  if (!loading && user) return <Navigate to="/espaces" replace />

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try { await signIn(courriel, motDePasse) }
    catch (reason) { setError(reason instanceof ApiError ? reason.message : 'Connexion impossible. Vérifiez le serveur et réessayez.') }
    finally { setBusy(false) }
  }

  return (
    <main className="login-layout">
      <section className="login-intro">
        <div className="brand brand-light"><span className="brand-mark"><Building2 size={20} /></span><span>smart<span>campus</span></span></div>
        <div className="intro-copy"><span className="eyebrow">PORTAIL DU CAMPUS</span><h1>Les espaces du campus, à portée de main.</h1><p>Retrouvez les salles, laboratoires et lieux de travail de votre campus.</p></div>
        <div className="campus-illustration" aria-hidden="true"><div className="sun"/><div className="building building-one"/><div className="building building-two"/><div className="ground"/></div>
        <span className="intro-footer">SmartCampus · ENSA Khouribga</span>
      </section>
      <section className="login-panel"><div className="login-card"><div className="login-icon"><LockKeyhole size={22}/></div><span className="eyebrow">ESPACE SÉCURISÉ</span><h2>Connexion</h2><p>Connectez-vous avec votre compte campus pour consulter les espaces.</p>
        <form onSubmit={submit} className="form-stack">
          <label>Adresse e-mail<input type="email" autoComplete="username" required value={courriel} onChange={event => setCourriel(event.target.value)} placeholder="nom@usms.ac.ma"/></label>
          <label>Mot de passe<input type="password" autoComplete="current-password" required value={motDePasse} onChange={event => setMotDePasse(event.target.value)} placeholder="Votre mot de passe"/></label>
          {error && <div className="form-error" role="alert">{error}</div>}
          <button className="button button-primary button-wide" disabled={busy || loading}>{busy ? 'Connexion…' : <>Se connecter <ArrowRight size={17}/></>}</button>
        </form><small className="login-note">La session utilise l’authentification SmartCampus existante.</small>
      </div></section>
    </main>
  )
}
