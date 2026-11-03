import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'

import { ApiError } from '../api/client'
import { useAuth } from '../auth/authState'

export function LoginPage() {
  const { user, loading, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/'

  const [courriel, setCourriel] = useState('')
  const [motDePasse, setMotDePasse] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (!loading && user) {
    return <Navigate to={from} replace />
  }

  // Un vrai <form> : la touche Entrée valide, et les gestionnaires de mots de passe le reconnaissent
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(courriel, motDePasse)
      navigate(from, { replace: true })
    } catch (e) {
      setError(errorMessage(e))
      setSubmitting(false)
    }
  }

  return (
    <main className="login-page">
      <form className="card" onSubmit={handleSubmit} aria-labelledby="login-title">
        <h1 id="login-title">Connexion à SmartCampus</h1>
        <p className="hint">Utilisez votre adresse e-mail institutionnelle.</p>

        {error && (
          <div className="alert" role="alert">
            {error}
          </div>
        )}

        <div className="field">
          <label htmlFor="courriel">Adresse e-mail</label>
          <input
            id="courriel"
            name="courriel"
            type="email"
            autoComplete="username"
            required
            value={courriel}
            onChange={(e) => setCourriel(e.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="motDePasse">Mot de passe</label>
          <input
            id="motDePasse"
            name="motDePasse"
            type="password"
            autoComplete="current-password"
            required
            value={motDePasse}
            onChange={(e) => setMotDePasse(e.target.value)}
          />
        </div>

        <button type="submit" className="primary" disabled={submitting}>
          {submitting ? 'Connexion…' : 'Se connecter'}
        </button>
      </form>
    </main>
  )
}

function errorMessage(e: unknown): string {
  if (e instanceof ApiError) {
    // 400 VALIDATION : le détail des champs est plus parlant que le message général
    const details = e.fields ? Object.values(e.fields).join(' ') : ''
    return details || e.message
  }
  return 'Une erreur inattendue est survenue.'
}
