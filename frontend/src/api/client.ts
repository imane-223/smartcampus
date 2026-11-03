// Client HTTP unique de l'application : toute page appelle l'API par apiFetch, jamais par fetch directement.

/** Corps des erreurs du backend (com.smartcampus.common.ErrorResponse). */
export interface ErrorResponse {
  status: number
  error: string
  message: string
  path: string
  timestamp: string
  fields?: Record<string, string>
  retryAt?: string
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fields?: Record<string, string>
  readonly retryAt?: string

  constructor(status: number, body: Partial<ErrorResponse> | null) {
    super(body?.message ?? defaultMessage(status))
    this.name = 'ApiError'
    this.status = status
    this.code = body?.error ?? 'UNKNOWN'
    this.fields = body?.fields
    this.retryAt = body?.retryAt
  }
}

function defaultMessage(status: number): string {
  if (status >= 500 || status === 0) {
    return 'Le serveur ne répond pas. Réessayez dans un instant.'
  }
  return 'Une erreur est survenue.'
}

const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'
const UNSAFE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

/**
 * Lit le jeton CSRF dans le cookie, à chaque appel : il change après la connexion et la déconnexion.
 * document.cookie renvoie la valeur encodée en URL, d'où le decodeURIComponent.
 */
function readCsrfCookie(): string | null {
  for (const part of document.cookie.split(';')) {
    const [name, ...rest] = part.trim().split('=')
    if (name === CSRF_COOKIE) {
      return decodeURIComponent(rest.join('='))
    }
  }
  return null
}

async function csrfToken(): Promise<string | null> {
  const existing = readCsrfCookie()
  if (existing) {
    return existing
  }
  // Pas encore de cookie (premier chargement, ou effacé au logout) : le serveur le pose
  await fetch('/api/auth/csrf', { credentials: 'include' })
  return readCsrfCookie()
}

/** Appelé quand une requête authentifiée reçoit 401 (session expirée). */
let unauthorizedHandler: (() => void) | null = null

export function onUnauthorized(handler: (() => void) | null): void {
  unauthorizedHandler = handler
}

export interface ApiOptions {
  method?: string
  body?: unknown
  /** false pour le login : un 401 y signifie « mauvais identifiants », pas « session expirée ». */
  notifyUnauthorized?: boolean
}

export async function apiFetch<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const method = (options.method ?? 'GET').toUpperCase()
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (UNSAFE_METHODS.has(method)) {
    const token = await csrfToken()
    if (token) {
      headers[CSRF_HEADER] = token
    }
  }

  let response: Response
  try {
    response = await fetch(path, {
      method,
      headers,
      credentials: 'include',
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    })
  } catch {
    throw new ApiError(0, null)
  }

  if (!response.ok) {
    if (response.status === 401 && options.notifyUnauthorized !== false) {
      unauthorizedHandler?.()
    }
    throw new ApiError(response.status, await readErrorBody(response))
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

async function readErrorBody(response: Response): Promise<Partial<ErrorResponse> | null> {
  try {
    return (await response.json()) as Partial<ErrorResponse>
  } catch {
    // Corps absent ou non JSON (proxy coupé, page d'erreur HTML...)
    return null
  }
}
