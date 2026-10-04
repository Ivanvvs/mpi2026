export const API_URL = 'http://localhost:8081'
export const WS_URL = `${API_URL}/ws`

export type ApiClient = <T>(path: string, options?: RequestInit) => Promise<T>

type SessionWithToken = { token: string }

export function createApiClient(session: SessionWithToken): ApiClient {
  return async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
    const response = await fetch(`${API_URL}${path}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${session.token}`,
        ...(options.headers || {})
      }
    })
    const text = await response.text()
    const data = text ? JSON.parse(text) : null
    if (!response.ok) {
      throw new Error(data?.message || data?.error || `HTTP ${response.status}`)
    }
    return data as T
  }
}
