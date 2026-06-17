import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { computed } from 'vue'
import { WS_URL } from '../services/api'
import type { Answer, ExamDetails, ExamResult, ExamSession, Role, UserResponse } from '../types/domain'
import { parseServerDateTime, toServerDateTimeValue } from './schoolAppFormatters'
import { parseExamQuestionsText } from './examQuestionParser'

type ApiClient = <T>(path: string, options?: RequestInit) => Promise<T>
type MessageKind = 'info' | 'success' | 'error'
type RefLike<T> = { value: T }

interface DeactivatableClient {
  deactivate: () => void
}

interface SessionState {
  role: Role
  userId: number | null
  displayName: string
}

interface ExamFormState {
  subject: string
  classId: number | null
  scheduledStartTime: string
  endsAt: string
  description: string
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
  realtimeEvents: RefLike<Array<{ id: string; type: string; time: string; userId?: number | null; userName?: string }>>
  stompClient: RefLike<DeactivatableClient | null>
  currentQuestionIndex: RefLike<number>
  selectedMonitorStudentId: RefLike<number | null>
  examStatusFilter: RefLike<string>
  examForm: ExamFormState
  examQuestionsText: RefLike<string>
  examQuestionFileName: RefLike<string>
  examFileInputKey: RefLike<number>
  examCreateSuccess: RefLike<boolean>
  answerSaveSuccess: RefLike<boolean>
  answerDrafts: Record<number, string>
  gradeForm: Record<number, number>
  violationForm: ViolationFormState
  now: RefLike<number>
  loadClasses: () => Promise<void>
  loadExams: () => Promise<void>
  currentPage: RefLike<string>
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
    selectedMonitorStudentId,
    examStatusFilter,
    examForm,
    examQuestionsText,
    examQuestionFileName,
    examFileInputKey,
    examCreateSuccess,
    answerSaveSuccess,
    answerDrafts,
    gradeForm,
    violationForm,
    now,
    loadClasses,
    loadExams,
    currentPage,
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

  const examQuestionCount = computed(() => parseExamQuestionsText(examQuestionsText.value).length)
  const currentQuestion = computed(() => selectedExam.value?.questions[currentQuestionIndex.value] || null)
  const currentExamSubmitted = computed(() => Boolean(selectedExam.value?.attempt?.submitted))
  const selectedMonitorStudent = computed(() => users.value.find((user) => user.id === selectedMonitorStudentId.value) || null)
  const selectedMonitorAnswers = computed(() => {
    if (!selectedExam.value || !selectedMonitorStudentId.value) return []
    return selectedExam.value.questions.map((question) => ({
      question,
      answer: selectedExam.value?.answers.find((item) => item.userId === selectedMonitorStudentId.value && item.questionId === question.id),
      score: scoreAnswer(
        question.correctAnswer,
        selectedExam.value?.answers.find((item) => item.userId === selectedMonitorStudentId.value && item.questionId === question.id)?.text,
        question.maxScore
      )
    }))
  })
  const finalScoreRows = computed(() => classStudentsForSelectedExam.value.map((student) => ({
    student,
    result: selectedExam.value?.results.find((result) => result.student?.id === student.id),
    draftScore: Number(gradeForm[student.id] || 0)
  })))

  function getStudentAttempt(studentId: number) {
    return selectedExam.value?.attempts?.find((attempt) => attempt.studentId === studentId) || null
  }

  function answerCountForStudent(studentId: number) {
    return selectedExam.value?.answers.filter((answer) => answer.userId === studentId && answer.text?.trim()).length || 0
  }

  function examStudentStatus(studentId: number) {
    const attempt = getStudentAttempt(studentId)
    if (!attempt?.started) return 'Не подключен'
    if (attempt.submitted) return 'Завершил экзамен'
    return 'Подключен'
  }

  function scoreAnswer(correctAnswer?: string | null, answerText?: string | null, maxScore?: number | null) {
    if (!correctAnswer || !answerText) return 0
    return normalizeAnswer(answerText) === normalizeAnswer(correctAnswer) ? Number(maxScore || 1) : 0
  }

  function normalizeAnswer(value: string) {
    return value.trim().replace(/\s+/g, ' ').toLowerCase()
  }

  function examElapsedLabel(exam: ExamSession) {
    const startedAt = exam.startTime || exam.scheduledStartTime
    if (!startedAt) return '-'
    return parseServerDateTime(startedAt).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
  }

  function examRemainingLabel(exam: ExamSession) {
    if (!exam.durationMinutes) return '—'
    const sourceStartTime = exam.scheduledStartTime || exam.startTime
    const startTime = sourceStartTime ? parseServerDateTime(sourceStartTime).getTime() : Date.now()
    const endTime = startTime + exam.durationMinutes * 60000
    const remainingMs = endTime - now.value
    if (remainingMs <= 0) return '00:00'
    const minutes = Math.floor(remainingMs / 60000)
    const seconds = Math.floor((remainingMs % 60000) / 1000)
    return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  }

  function examEndLabel(exam: ExamSession) {
    if (exam.endTime) {
      return parseServerDateTime(exam.endTime).toLocaleString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    }

    const sourceStartTime = exam.scheduledStartTime || exam.startTime
    if (!sourceStartTime || !exam.durationMinutes) return '-'

    const endTime = new Date(parseServerDateTime(sourceStartTime).getTime() + exam.durationMinutes * 60000)
    return endTime.toLocaleString('ru-RU', {
      day: '2-digit',
      month: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  async function createExam() {
    examCreateSuccess.value = false
    try {
      if (!examForm.subject.trim()) {
        throw new Error('Введите дисциплину')
      }

      if (!examForm.scheduledStartTime) {
        throw new Error('Укажите дату и время начала экзамена')
      }

      if (!examForm.endsAt) {
        throw new Error('Укажите дату и время окончания экзамена')
      }

      const scheduledStartTime = new Date(examForm.scheduledStartTime)
      const endsAt = new Date(examForm.endsAt)
      const durationMinutes = Math.ceil((endsAt.getTime() - scheduledStartTime.getTime()) / 60000)
      if (durationMinutes <= 0) {
        throw new Error('Дата окончания должна быть позже даты начала')
      }

      const questions = parseExamQuestionsText(examQuestionsText.value)
      if (!questions.length) {
        throw new Error('Прикрепите .txt файл с заданиями')
      }

      await api<ExamSession>('/exam/session', {
        method: 'POST',
        body: JSON.stringify({
          title: examForm.subject,
          subject: examForm.subject,
          classId: examForm.classId,
          description: examForm.description,
          durationMinutes,
          scheduledStartTime: toServerDateTimeValue(scheduledStartTime),
          questions
        })
      })
      await loadExams()
      resetExamForm()
      examCreateSuccess.value = true
      window.setTimeout(() => {
        examCreateSuccess.value = false
      }, 2500)
      setMessage('Экзамен создан', 'success')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Ошибка создания экзамена', 'error')
    }
  }

  async function handleExamFileChange(event: Event) {
    examCreateSuccess.value = false
    const input = event.target as HTMLInputElement
    const file = input.files?.[0]
    if (!file) return

    if (!file.name.toLowerCase().endsWith('.txt')) {
      examQuestionsText.value = ''
      examQuestionFileName.value = ''
      input.value = ''
      setMessage('Прикрепите файл в формате .txt', 'error')
      return
    }

    try {
      examQuestionsText.value = await file.text()
      examQuestionFileName.value = file.name
      const parsedCount = parseExamQuestionsText(examQuestionsText.value).length
      setMessage(`Файл с заданиями загружен: ${parsedCount}`, 'success')
    } catch (error) {
      examQuestionsText.value = ''
      examQuestionFileName.value = ''
      input.value = ''
      setMessage(error instanceof Error ? error.message : 'Не удалось прочитать файл с заданиями', 'error')
    }
  }

  function resetExamForm() {
    Object.assign(examForm, {
      subject: '',
      classId: null,
      scheduledStartTime: '',
      endsAt: '',
      description: ''
    })
    examQuestionsText.value = ''
    examQuestionFileName.value = ''
    examFileInputKey.value += 1
  }

  async function loadExamDetails(id: number, preserveQuestionIndex = false, reconnectSocket = false) {
    const previousIndex = currentQuestionIndex.value
    const previousMonitorStudentId = selectedMonitorStudentId.value
    selectedExam.value = await api<ExamDetails>(`/exam/session/${id}/details`)
    localStorage.setItem('school-selected-exam-id', String(id))
    currentQuestionIndex.value = preserveQuestionIndex
      ? Math.min(previousIndex, Math.max(selectedExam.value.questions.length - 1, 0))
      : 0
    selectedMonitorStudentId.value = preserveQuestionIndex ? previousMonitorStudentId : null
    selectedExam.value.questions.forEach((question) => {
      const existing = selectedExam.value?.answers.find((answer) => answer.questionId === question.id && answer.userId === session.userId)
      answerDrafts[question.id] = existing?.text || answerDrafts[question.id] || ''
    })
    classStudentsForSelectedExam.value.forEach((student) => {
      gradeForm[student.id] = gradeForm[student.id] || 0
    })
    if (reconnectSocket && session.role === 'EXAMINER') connectExamSocket(id)
    if (session.role === 'STUDENT' && selectedExam.value.exam.status === 'ACTIVE' && !currentExamSubmitted.value) {
      requestExamFullscreen()
    }
  }

  async function openExam(id: number, preserveQuestionIndex = false) {
    await loadExamDetails(id, preserveQuestionIndex, true)
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
    const examId = selectedExam.value.exam.id
    for (const question of selectedExam.value.questions) {
      const text = answerDrafts[question.id]?.trim()
      if (!text) continue
      await api<Answer>(`/exam/session/${examId}/answers/me`, {
        method: 'POST',
        body: JSON.stringify({ questionId: question.id, text, finalSubmitted: false })
      })
    }
    await api(`/exam/session/${examId}/attempt/me/submit`, { method: 'POST' })
    await loadExams()
    selectedExam.value = null
    currentPage.value = 'exams'
    localStorage.setItem('school-current-page', 'exams')
    localStorage.removeItem('school-selected-exam-id')
    localStorage.removeItem('school-current-question-index')
    setMessage('Экзамен завершен', 'success')
  }

  async function saveCurrentAnswer() {
    answerSaveSuccess.value = false
    if (!selectedExam.value || !currentQuestion.value || !session.userId || currentExamSubmitted.value) return

    const text = answerDrafts[currentQuestion.value.id]?.trim()
    if (!text) {
      setMessage('Введите ответ перед сохранением', 'error')
      return
    }

    await saveAnswer(currentQuestion.value.id, text)
    await loadExamDetails(selectedExam.value.exam.id, true)
    answerSaveSuccess.value = true
    window.setTimeout(() => {
      answerSaveSuccess.value = false
    }, 2000)
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

  function previousQuestion() {
    currentQuestionIndex.value = Math.max(currentQuestionIndex.value - 1, 0)
  }

  function openStudentMonitor(studentId: number) {
    selectedMonitorStudentId.value = studentId
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
        realtimeEvents.value.unshift({
          id: `${Date.now()}-${Math.random()}`,
          type: 'CONNECTED',
          userId: session.userId,
          userName: session.displayName || userNameById(session.userId),
          time: new Date().toLocaleTimeString('ru-RU')
        })
        client.subscribe(`/topic/exams/${examId}`, async (frame) => {
          const payload = JSON.parse(frame.body)
          const userId = typeof payload.userId === 'number' ? payload.userId : null
          realtimeEvents.value.unshift({
            id: `${Date.now()}-${Math.random()}`,
            type: payload.type,
            userId,
            userName: userNameById(userId),
            time: new Date().toLocaleTimeString('ru-RU')
          })
          try {
            await loadExamDetails(examId, true)
          } catch {
            // Keep the socket session alive even if background refresh fails.
          }
        })
      },
      onWebSocketError: () => {
        setMessage('Не удалось подключиться к WebSocket мониторинга экзамена', 'error')
      }
    })
    stompClient.value = client
    client.activate()
  }

  function userNameById(userId: number | null) {
    if (!userId) return ''
    return users.value.find((user) => user.id === userId)?.fullName || `ID ${userId}`
  }

  return {
    visibleExams,
    classStudentsForSelectedExam,
    examQuestionCount,
    currentQuestion,
    currentExamSubmitted,
    selectedMonitorStudent,
    selectedMonitorAnswers,
    finalScoreRows,
    answerCountForStudent,
    examStudentStatus,
    examElapsedLabel,
    examEndLabel,
    examRemainingLabel,
    createExam,
    handleExamFileChange,
    resetExamForm,
    openExam,
    startExam,
    finishExam,
    submitAllAnswers,
    saveCurrentAnswer,
    saveAnswer,
    autosaveCurrentAnswer,
    nextQuestion,
    previousQuestion,
    openStudentMonitor,
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
