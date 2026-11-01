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
