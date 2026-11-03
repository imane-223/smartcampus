import { vi } from 'vitest'

/** Réponse simulée d'une route de l'API : status et corps JSON (null = pas de corps). */
export interface FakeReply {
  status: number
  body?: unknown
}

type Handler = (init: RequestInit) => FakeReply

export const SARA = {
  id: 1,
  prenom: 'Sara',
  nom: 'Alaoui',
  courriel: 'etudiant@usms.ac.ma',
  role: 'STUDENT',
} as const

export function unauthorized(path: string, error = 'UNAUTHORIZED', message = 'Authentification requise.'): FakeReply {
  return { status: 401, body: { status: 401, error, message, path, timestamp: '2026-11-02T08:00:00Z' } }
}

/**
 * Remplace fetch par un faux backend. Clé : "MÉTHODE /chemin". GET /api/auth/csrf pose le cookie
 * XSRF-TOKEN comme le ferait le serveur (valeur encodée en URL, pour éprouver le décodage).
 */
export function fakeApi(routes: Record<string, Handler>) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init: RequestInit = {}) => {
    const key = `${(init.method ?? 'GET').toUpperCase()} ${String(input)}`
    if (key === 'GET /api/auth/csrf' && !routes[key]) {
      document.cookie = 'XSRF-TOKEN=jeton%2Bcsrf; path=/'
      return new Response(JSON.stringify({ headerName: 'X-XSRF-TOKEN', token: 'jeton+csrf' }), { status: 200 })
    }
    const handler = routes[key]
    if (!handler) {
      throw new Error(`Route non simulée : ${key}`)
    }
    const reply = handler(init)
    const body = reply.body === undefined ? null : JSON.stringify(reply.body)
    return new Response(body, { status: reply.status, headers: { 'Content-Type': 'application/json' } })
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

/** En-têtes envoyés lors de l'appel `key` (ex. "POST /api/auth/login"). */
export function headersOf(fetchMock: ReturnType<typeof fakeApi>, key: string): Record<string, string> {
  const call = fetchMock.mock.calls.find(
    ([input, init]) => `${(init?.method ?? 'GET').toUpperCase()} ${String(input)}` === key,
  )
  if (!call) {
    throw new Error(`Aucun appel ${key}`)
  }
  return (call[1]?.headers ?? {}) as Record<string, string>
}
