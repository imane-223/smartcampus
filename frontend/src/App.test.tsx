import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'

import { App } from './App'
import { apiFetch } from './api/client'
import { SARA, fakeApi, headersOf, unauthorized } from './test/api'

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

async function fillAndSubmit(courriel: string, motDePasse: string) {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText('Adresse e-mail'), courriel)
  await user.type(screen.getByLabelText('Mot de passe'), motDePasse)
  await user.click(screen.getByRole('button', { name: 'Se connecter' }))
}

describe('Page de connexion', () => {
  it('affiche le formulaire avec ses champs étiquetés', async () => {
    fakeApi({ 'GET /api/auth/me': () => unauthorized('/api/auth/me') })
    renderAt('/login')

    const form = await screen.findByRole('form', { name: 'Connexion à SmartCampus' })
    expect(form.tagName).toBe('FORM')
    expect(screen.getByLabelText('Adresse e-mail')).toHaveAttribute('autocomplete', 'username')
    expect(screen.getByLabelText('Mot de passe')).toHaveAttribute('autocomplete', 'current-password')
    expect(screen.getByLabelText('Mot de passe')).toHaveAttribute('type', 'password')
    expect(screen.getByRole('button', { name: 'Se connecter' })).toBeEnabled()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('valide le formulaire avec la touche Entrée', async () => {
    const api = fakeApi({
      'GET /api/auth/me': () => unauthorized('/api/auth/me'),
      'POST /api/auth/login': () => ({ status: 200, body: SARA }),
    })
    renderAt('/login')
    const user = userEvent.setup()

    await user.type(await screen.findByLabelText('Adresse e-mail'), 'etudiant@usms.ac.ma')
    await user.type(screen.getByLabelText('Mot de passe'), 'Demo2026!{Enter}')

    expect(await screen.findByRole('heading', { name: 'Bonjour Sara Alaoui' })).toBeInTheDocument()
    const [, init] = api.mock.calls.find(([input]) => input === '/api/auth/login')!
    expect(JSON.parse(String(init?.body))).toEqual({ courriel: 'etudiant@usms.ac.ma', motDePasse: 'Demo2026!' })
  })

  it('affiche le message du serveur dans une alerte sur un 401', async () => {
    fakeApi({
      'GET /api/auth/me': () => unauthorized('/api/auth/me'),
      'POST /api/auth/login': () =>
        unauthorized('/api/auth/login', 'INVALID_CREDENTIALS', 'E-mail ou mot de passe incorrect.'),
    })
    renderAt('/login')
    await screen.findByLabelText('Adresse e-mail')

    await fillAndSubmit('etudiant@usms.ac.ma', 'mauvais')

    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou mot de passe incorrect.')
    expect(screen.getByRole('button', { name: 'Se connecter' })).toBeEnabled()
    // Un 401 du login n'est pas une « session expirée » : on reste sur la page, sans redirection
    expect(screen.getByRole('form', { name: 'Connexion à SmartCampus' })).toBeInTheDocument()
  })

  it('redirige vers l’accueil après une connexion réussie, avec le jeton CSRF décodé', async () => {
    const api = fakeApi({
      'GET /api/auth/me': () => unauthorized('/api/auth/me'),
      'POST /api/auth/login': () => ({ status: 200, body: SARA }),
    })
    renderAt('/login')
    await screen.findByLabelText('Adresse e-mail')

    await fillAndSubmit('etudiant@usms.ac.ma', 'Demo2026!')

    expect(await screen.findByRole('heading', { name: 'Bonjour Sara Alaoui' })).toBeInTheDocument()
    expect(screen.getByText('Étudiant')).toBeInTheDocument()
    // Cookie "jeton%2Bcsrf" → en-tête "jeton+csrf"
    expect(headersOf(api, 'POST /api/auth/login')['X-XSRF-TOKEN']).toBe('jeton+csrf')
  })
})

describe('Session', () => {
  it('redirige vers /login sans session', async () => {
    fakeApi({ 'GET /api/auth/me': () => unauthorized('/api/auth/me') })
    renderAt('/')

    expect(await screen.findByRole('form', { name: 'Connexion à SmartCampus' })).toBeInTheDocument()
  })

  it('restaure la session au chargement (rechargement de page)', async () => {
    fakeApi({ 'GET /api/auth/me': () => ({ status: 200, body: SARA }) })
    renderAt('/')

    expect(await screen.findByRole('heading', { name: 'Bonjour Sara Alaoui' })).toBeInTheDocument()
  })

  it('renvoie vers /login quand la session expire en cours d’utilisation', async () => {
    fakeApi({
      'GET /api/auth/me': () => ({ status: 200, body: SARA }),
      'GET /api/salles': () => unauthorized('/api/salles'),
    })
    renderAt('/')
    await screen.findByRole('heading', { name: 'Bonjour Sara Alaoui' })

    // Une page quelconque appelle l'API après l'expiration de la session
    await act(async () => {
      await apiFetch('/api/salles').catch(() => undefined)
    })

    expect(await screen.findByRole('form', { name: 'Connexion à SmartCampus' })).toBeInTheDocument()
    expect(screen.queryByText('Bonjour Sara Alaoui')).not.toBeInTheDocument()
  })
})

describe('Déconnexion', () => {
  it('appelle POST /api/auth/logout avec le jeton CSRF et revient à /login', async () => {
    let loggedIn = true
    const api = fakeApi({
      'GET /api/auth/me': () => (loggedIn ? { status: 200, body: SARA } : unauthorized('/api/auth/me')),
      'POST /api/auth/logout': () => {
        loggedIn = false
        return { status: 204 }
      },
    })
    renderAt('/')
    await screen.findByRole('heading', { name: 'Bonjour Sara Alaoui' })

    await userEvent.setup().click(screen.getByRole('button', { name: 'Se déconnecter' }))

    expect(await screen.findByRole('form', { name: 'Connexion à SmartCampus' })).toBeInTheDocument()
    expect(headersOf(api, 'POST /api/auth/logout')['X-XSRF-TOKEN']).toBe('jeton+csrf')
    expect(loggedIn).toBe(false)
  })
})
