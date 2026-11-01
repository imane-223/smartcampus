import { apiFetch } from './client'

export type Role = 'STUDENT' | 'TEACHER' | 'ADMIN'

/** UserResponse du backend. */
export interface User {
  id: number
  prenom: string
  nom: string
  courriel: string
  role: Role
}

export function fetchCurrentUser(): Promise<User> {
  return apiFetch<User>('/api/auth/me')
}

export function login(courriel: string, motDePasse: string): Promise<User> {
  return apiFetch<User>('/api/auth/login', {
    method: 'POST',
    body: { courriel, motDePasse },
    notifyUnauthorized: false,
  })
}

/** Invalide la session côté serveur (204). Le jeton CSRF est effacé : le prochain POST en obtiendra un neuf. */
export function logout(): Promise<void> {
  return apiFetch<void>('/api/auth/logout', { method: 'POST' })
}
