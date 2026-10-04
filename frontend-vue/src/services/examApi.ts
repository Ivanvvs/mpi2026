import type { Answer, ExamDetails, ExamSession } from '../types/domain'
import type { ApiClient } from './api'

export function loadExamDetails(api: ApiClient, examId: number) {
  return api<ExamDetails>(`/exam/session/${examId}/details`)
}

export function startExam(api: ApiClient, examId: number) {
  return api<ExamSession>(`/exam/session/start/${examId}`, { method: 'POST' })
}

export function finishExam(api: ApiClient, examId: number) {
  return api<ExamSession>(`/exam/session/end/${examId}`, { method: 'POST' })
}

export function saveAnswer(api: ApiClient, examId: number, questionId: number, text: string) {
  return api<Answer>(`/exam/session/${examId}/answers/me`, {
    method: 'POST',
    body: JSON.stringify({ questionId, text, finalSubmitted: false })
  })
}

export function submitAttempt(api: ApiClient, examId: number) {
  return api(`/exam/session/${examId}/attempt/me/submit`, { method: 'POST' })
}

export function createExam(api: ApiClient, payload: Record<string, unknown>) {
  return api<ExamSession>('/exam/session', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function saveGrades(api: ApiClient, examId: number, scores: Array<{ studentId: number; rawScore: number }>) {
  return api(`/exam/session/${examId}/grades`, {
    method: 'POST',
    body: JSON.stringify({ scores })
  })
}
