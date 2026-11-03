const baseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

interface ApiErrorBody {
  message?: string
  error?: string
  fields?: Record<string, string>
}

export class ApiError extends Error {
  constructor(readonly status: number, readonly code: string, message: string, readonly fields?: Record<string, string>) {
    super(message)
  }
}

let csrfToken: string | null = null

export async function refreshCsrf(): Promise<void> {
  const response = await fetch(`${baseUrl}/api/auth/csrf`, { credentials: 'include' })
  if (!response.ok) throw new ApiError(response.status, 'CSRF', 'Impossible de préparer la connexion sécurisée.')
  const payload = (await response.json()) as { token: string }
  csrfToken = payload.token
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method) && !csrfToken) await refreshCsrf()

  const headers = new Headers(init.headers)
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method) && csrfToken) headers.set('X-XSRF-TOKEN', csrfToken)

  const response = await fetch(`${baseUrl}${path}`, { ...init, method, headers, credentials: 'include' })
  if (response.status === 204) return undefined as T
  const contentType = response.headers.get('content-type') ?? ''
  const body = contentType.includes('json') ? await response.json() : await response.text()
  if (!response.ok) {
    const error = body as ApiErrorBody
    if (error.error === 'CSRF') csrfToken = null
    throw new ApiError(response.status, error.error ?? 'REQUEST_FAILED', error.message ?? 'Une erreur est survenue.', error.fields)
  }
  return body as T
}

export async function login(courriel: string, motDePasse: string) {
  await refreshCsrf()
  await request('/api/auth/login', { method: 'POST', body: JSON.stringify({ courriel, motDePasse }) })
  // Le jeton CSRF est renouvelé par le backend après l'ouverture de session.
  csrfToken = null
  await refreshCsrf()
}

export async function logout() {
  await request('/api/auth/logout', { method: 'POST' })
  csrfToken = null
}

export function resetCsrfForTests() {
  csrfToken = null
}
