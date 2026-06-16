import { API_URL } from '../services/api'
import type { LoginResponse } from '../types/domain'

type SessionLike = Pick<LoginResponse, 'token'>

export function createSchoolApiClient(session: SessionLike) {
  function authHeaders(): HeadersInit {
    return {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${session.token}`
    }
  }

  return async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
    const response = await fetch(`${API_URL}${path}`, {
      ...options,
      headers: {
        ...authHeaders(),
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
