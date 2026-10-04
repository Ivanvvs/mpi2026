import type { ApiClient } from './api'

interface ViolationRequest {
  sessionId: number
  userId?: number
  type: string
  description: string
  pointsPenalty: number
}

export function reportViolation(api: ApiClient, violation: ViolationRequest, forCurrentUser = false) {
  return api(forCurrentUser ? '/violations/report/me' : '/violations/report', {
    method: 'POST',
    body: JSON.stringify(violation)
  })
}
