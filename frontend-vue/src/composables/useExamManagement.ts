import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { computed } from 'vue'
import { WS_URL } from '../services/api'
import type { Answer, ExamDetails, ExamResult, ExamSession, Role, UserResponse } from '../types/domain'

type ApiClient = <T>(path: string, options?: RequestInit) => Promise<T>
type MessageKind = 'info' | 'success' | 'error'
type RefLike<T> = { value: T }

interface DeactivatableClient {
  deactivate: () => void
}

interface SessionState {
  role: Role
  userId: number | null
}

interface ExamFormState {
  title: string
  subject: string
  classId: number | null
  durationMinutes: number
  description: string
  scheduledStartTime: string
}

interface ViolationFormState {
  userId: number | null
  description: string
  pointsPenalty: number
}

interface UseExamManagementOptions {
  api: ApiClient
  session: SessionState
  exams: RefLike<ExamSession[]>
  users: RefLike<UserResponse[]>
  selectedExam: RefLike<ExamDetails | null>
  realtimeEvents: RefLike<Array<{ id: string; type: string; time: string }>>
  stompClient: RefLike<DeactivatableClient | null>
  currentQuestionIndex: RefLike<number>
  examStatusFilter: RefLike<string>
  examForm: ExamFormState
  examQuestionsText: RefLike<string>
  answerDrafts: Record<number, string>
  gradeForm: Record<number, number>
  violationForm: ViolationFormState
  now: RefLike<number>
  loadClasses: () => Promise<void>
  loadExams: () => Promise<void>
  setMessage: (text: string, kind?: MessageKind) => void
}

export function useExamManagement(options: UseExamManagementOptions) {
  const {
    api,
    session,
    exams,
    users,
    selectedExam,
    realtimeEvents,
    stompClient,
    currentQuestionIndex,
    examStatusFilter,
    examForm,
    examQuestionsText,
    answerDrafts,
    gradeForm,
    violationForm,
    now,
    loadClasses,
    loadExams,
    setMessage
  } = options

  const reportedClientViolations = new Set<string>()

  const visibleExams = computed(() => {
    const filtered = examStatusFilter.value === 'ALL'
      ? exams.value
      : exams.value.filter((exam) => exam.status === examStatusFilter.value)
    if (session.role !== 'STUDENT') return filtered
    const myClass = users.value.find((user) => user.id === session.userId)?.className
    return filtered.filter((exam) => !myClass || exam.schoolClass?.name === myClass)
  })

  const classStudentsForSelectedExam = computed(() => {
    const className = selectedExam.value?.exam.schoolClass?.name
    return users.value.filter((user) => user.role === 'STUDENT' && (!className || user.className === className))
  })

  const examQuestionCount = computed(() => examQuestionsText.value.split('\n').map((text) => text.trim()).filter(Boolean).length)
  const currentQuestion = computed(() => selectedExam.value?.questions[currentQuestionIndex.value] || null)
  const currentExamSubmitted = computed(() => Boolean(selectedExam.value?.attempt?.submitted))

  function answerCountForStudent(studentId: number) {
    return selectedExam.value?.answers.filter((answer) => answer.userId === studentId && answer.text?.trim()).length || 0
  }

  function examElapsedLabel(exam: ExamSession) {
    if (!exam.scheduledStartTime) return '-'
    return new Date(exam.scheduledStartTime).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
  }

  function examRemainingLabel(exam: ExamSession) {
    if (!exam.durationMinutes) return '—'
    const sourceStartTime = exam.startTime || exam.scheduledStartTime
    const startTime = sourceStartTime ? new Date(sourceStartTime).getTime() : Date.now()
    const endTime = startTime + exam.durationMinutes * 60000
    const remainingMs = endTime - now.value
    if (remainingMs <= 0) return '00:00'
    const minutes = Math.floor(remainingMs / 60000)
    const seconds = Math.floor((remainingMs % 60000) / 1000)
    return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  }

  async function createExam() {
    try {
      const questions = examQuestionsText.value
        .split('\n')
        .map((text) => text.trim())
        .filter(Boolean)
        .map((text, index) => ({
          orderIndex: index + 1,
          text,
          type: 'TEXT',
          maxScore: 1
        }))
      await api<ExamSession>('/exam/session', {
        method: 'POST',
        body: JSON.stringify({
          ...examForm,
          scheduledStartTime: examForm.scheduledStartTime || null,
          questions
        })
      })
      await loadExams()
      setMessage('Экзамен создан', 'success')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Ошибка создания экзамена', 'error')
    }
  }

  async function openExam(id: number) {
    selectedExam.value = await api<ExamDetails>(`/exam/session/${id}/details`)
    currentQuestionIndex.value = 0
    selectedExam.value.questions.forEach((question) => {
      const existing = selectedExam.value?.answers.find((answer) => answer.questionId === question.id && answer.userId === session.userId)
      answerDrafts[question.id] = existing?.text || answerDrafts[question.id] || ''
    })
    classStudentsForSelectedExam.value.forEach((student) => {
      gradeForm[student.id] = gradeForm[student.id] || 0
    })
    if (session.role === 'EXAMINER') connectExamSocket(id)
    if (session.role === 'STUDENT' && selectedExam.value.exam.status === 'ACTIVE' && !currentExamSubmitted.value) {
      requestExamFullscreen()
    }
  }

  async function startExam(id: number) {
    await api<ExamSession>(`/exam/session/start/${id}`, { method: 'POST' })
    await loadExams()
    await openExam(id)
    setMessage('Экзамен запущен', 'success')
  }

  async function finishExam(id: number) {
    await api<ExamSession>(`/exam/session/end/${id}`, { method: 'POST' })
    await loadExams()
    await openExam(id)
    setMessage('Экзамен завершен', 'success')
  }

  async function submitAllAnswers() {
    if (!selectedExam.value || !session.userId || currentExamSubmitted.value) return
    for (const question of selectedExam.value.questions) {
      const text = answerDrafts[question.id]?.trim()
      if (!text) continue
      await api<Answer>(`/exam/session/${selectedExam.value.exam.id}/answers/me`, {
        method: 'POST',
        body: JSON.stringify({ questionId: question.id, text, finalSubmitted: false })
      })
    }
    await api(`/exam/session/${selectedExam.value.exam.id}/attempt/me/submit`, { method: 'POST' })
    await openExam(selectedExam.value.exam.id)
    setMessage('Ответы сохранены', 'success')
  }

  async function saveCurrentAnswer() {
    if (!selectedExam.value || !currentQuestion.value || !session.userId || currentExamSubmitted.value) return

    const text = answerDrafts[currentQuestion.value.id]?.trim()
    if (!text) {
      setMessage('Введите ответ перед сохранением', 'error')
      return
    }

    await saveAnswer(currentQuestion.value.id, text)
    await openExam(selectedExam.value.exam.id)
    setMessage('Ответ сохранен', 'success')
  }

  async function saveAnswer(questionId: number, text: string) {
    if (!selectedExam.value) return

    await api<Answer>(`/exam/session/${selectedExam.value.exam.id}/answers/me`, {
      method: 'POST',
      body: JSON.stringify({ questionId, text, finalSubmitted: false })
    })
  }

  async function autosaveCurrentAnswer() {
    if (session.role !== 'STUDENT' || !selectedExam.value || selectedExam.value.exam.status !== 'ACTIVE' || currentExamSubmitted.value || !currentQuestion.value) {
      return
    }

    const text = answerDrafts[currentQuestion.value.id]?.trim()
    if (!text) return

    try {
      await saveAnswer(currentQuestion.value.id, text)
    } catch {
      // Manual save shows user-facing errors; background autosave stays quiet.
    }
  }

  function nextQuestion() {
    if (!selectedExam.value?.questions.length) return
    currentQuestionIndex.value = Math.min(currentQuestionIndex.value + 1, selectedExam.value.questions.length - 1)
  }

  function requestExamFullscreen() {
    if (document.fullscreenElement || !document.documentElement.requestFullscreen) return
    document.documentElement.requestFullscreen().catch(() => {
      reportClientExamViolation('FULLSCREEN_REQUEST_REJECTED', 'Student did not enter fullscreen mode')
    })
  }

  async function reportClientExamViolation(type: string, description: string) {
    if (session.role !== 'STUDENT' || !selectedExam.value || selectedExam.value.exam.status !== 'ACTIVE' || currentExamSubmitted.value) return
    const key = `${selectedExam.value.exam.id}:${type}`
    if (reportedClientViolations.has(key)) return
    reportedClientViolations.add(key)

    try {
      await api('/violations/report/me', {
        method: 'POST',
        body: JSON.stringify({
          sessionId: selectedExam.value.exam.id,
          type,
          description,
          pointsPenalty: 0
        })
      })
    } catch {
      reportedClientViolations.delete(key)
    }
  }

  function handleFullscreenChange() {
    if (session.role === 'STUDENT' && selectedExam.value?.exam.status === 'ACTIVE' && !document.fullscreenElement) {
      reportClientExamViolation('FULLSCREEN_EXIT', 'Student left fullscreen mode during the exam')
    }
  }

  function handleWindowBlur() {
    reportClientExamViolation('WINDOW_BLUR', 'Student switched away from the exam window')
  }

  function handleVisibilityChange() {
    if (document.visibilityState === 'hidden') {
      reportClientExamViolation('PAGE_HIDDEN', 'Student hid the exam page')
    }
  }

  async function reportViolation() {
    if (!selectedExam.value || !violationForm.userId) {
      setMessage('Выберите ученика для фиксации нарушения', 'error')
      return
    }

    if (!violationForm.description.trim()) {
      setMessage('Введите описание нарушения', 'error')
      return
    }

    try {
      await api('/violations/report', {
        method: 'POST',
        body: JSON.stringify({
          sessionId: selectedExam.value.exam.id,
          userId: violationForm.userId,
          type: 'EXAM_RULE_VIOLATION',
          description: violationForm.description,
          pointsPenalty: Number(violationForm.pointsPenalty || 0)
        })
      })
      Object.assign(violationForm, { userId: null, description: '', pointsPenalty: 0 })
      setMessage('Нарушение зафиксировано', 'success')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Ошибка фиксации нарушения', 'error')
    }
  }

  async function gradeExam() {
    if (!selectedExam.value) return
    const scores = classStudentsForSelectedExam.value.map((student) => ({
      studentId: student.id,
      rawScore: Number(gradeForm[student.id] || 0)
    }))
    await api<ExamResult[]>(`/exam/session/${selectedExam.value.exam.id}/grades`, {
      method: 'POST',
      body: JSON.stringify({ scores })
    })
    await loadClasses()
    await openExam(selectedExam.value.exam.id)
    setMessage('Итоговые баллы сохранены', 'success')
  }

  function connectExamSocket(examId: number) {
    stompClient.value?.deactivate()
    realtimeEvents.value = []
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 3000,
      onConnect: () => {
        client.subscribe(`/topic/exams/${examId}`, (frame) => {
          const payload = JSON.parse(frame.body)
          realtimeEvents.value.unshift({
            id: `${Date.now()}-${Math.random()}`,
            type: payload.type,
            time: new Date().toLocaleTimeString('ru-RU')
          })
        })
      }
    })
    stompClient.value = client
    client.activate()
  }

  return {
    visibleExams,
    classStudentsForSelectedExam,
    examQuestionCount,
    currentQuestion,
    currentExamSubmitted,
    answerCountForStudent,
    examElapsedLabel,
    examRemainingLabel,
    createExam,
    openExam,
    startExam,
    finishExam,
    submitAllAnswers,
    saveCurrentAnswer,
    saveAnswer,
    autosaveCurrentAnswer,
    nextQuestion,
    requestExamFullscreen,
    reportClientExamViolation,
    reportViolation,
    gradeExam,
    connectExamSocket,
    handleFullscreenChange,
    handleWindowBlur,
    handleVisibilityChange
  }
}
