import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { API_URL, WS_URL } from '../services/api'
import type {
  Answer,
  ExamDetails,
  ExamResult,
  ExamSession,
  ExamStatus,
  LoginResponse,
  Page,
  RankedClass,
  RealtimeEvent,
  Role,
  SchoolClass,
  SecretVoting,
  UserResponse,
  VotingDetails,
  VotingStatus
} from '../types/domain'

export function useSchoolApp() {
  const session = reactive({
    token: '',
    role: 'STUDENT' as Role,
    accountId: null as number | null,
    userId: null as number | null,
    displayName: ''
  })

  const loginForm = reactive({ username: 'admin', password: '1234' })
  const rememberMe = ref(true)
  const loading = ref(false)
  const currentPage = ref<Page>('home')
  const message = reactive({ text: '', kind: 'info' as 'info' | 'success' | 'error' })

  const users = ref<UserResponse[]>([])
  const classes = ref<SchoolClass[]>([])
  const exams = ref<ExamSession[]>([])
  const votings = ref<SecretVoting[]>([])
  const selectedExam = ref<ExamDetails | null>(null)
  const selectedVoting = ref<VotingDetails | null>(null)
  const myResults = ref<ExamResult[]>([])
  const realtimeEvents = ref<RealtimeEvent[]>([])
  const stompClient = ref<Client | null>(null)
  const currentQuestionIndex = ref(0)
  const selectedVoteOptionId = ref<number | null>(null)
  const now = ref(Date.now())
  const examStatusFilter = ref<ExamStatus | 'ALL'>('ALL')
  const votingStatusFilter = ref<VotingStatus | 'ALL'>('ALL')
  const reportedClientViolations = new Set<string>()

  const userRoleFilter = ref<Role | 'ALL'>('ALL')
  const editingUserId = ref<number | null>(null)
  const userFilters = [
    { value: 'ALL' as const, label: 'Все пользователи' },
    { value: 'STUDENT' as const, label: 'Ученик' },
    { value: 'EXAMINER' as const, label: 'Экзаменатор-модератор' },
    { value: 'CURATOR' as const, label: 'Куратор класса' },
    { value: 'ADMIN' as const, label: 'Администратор' }
  ]

  const registerForm = reactive({
    fullName: '',
    username: '',
    email: '',
    password: '',
    role: 'STUDENT' as Role,
    classId: null as number | null,
    passportData: '',
    entranceExamScore: null as number | null,
    contactInfo: '',
    birthDate: ''
  })
  const confirmPassword = ref('')
  const sendCredentials = ref(true)

  const examForm = reactive({
    title: 'test',
    subject: 'test',
    classId: null as number | null,
    durationMinutes: 45,
    description: '',
    scheduledStartTime: ''
  })
  const examQuestionsText = ref('1 + 1')
  const answerDrafts = reactive<Record<number, string>>({})
  const gradeForm = reactive<Record<number, number>>({})

  const votingForm = reactive({
    title: '',
    description: '',
    classId: null as number | null
  })
  const votingOptionsText = ref('За\nПротив')
  const votingDurationMinutes = ref(15)
  const violationForm = reactive({
    userId: null as number | null,
    description: '',
    pointsPenalty: 0
  })

  const menus: Record<Role, Array<{ page: Page; label: string }>> = {
    ADMIN: [
      { page: 'home', label: 'Главная' },
      { page: 'classes', label: 'Классы и рейтинг' },
      { page: 'users', label: 'Ученики' },
      { page: 'violations', label: 'Нарушения' },
      { page: 'privileges', label: 'Заявки на привилегии' }
    ],
    CURATOR: [
      { page: 'home', label: 'Главная' },
      { page: 'my-class', label: 'Мой класс' },
      { page: 'violations', label: 'Нарушения' },
      { page: 'privileges', label: 'Заявки на привилегии' },
      { page: 'votings', label: 'Голосования' }
    ],
    STUDENT: [
      { page: 'home', label: 'Главная' },
      { page: 'exams', label: 'Экзамены' },
      { page: 'votings', label: 'Голосования' },
      { page: 'results', label: 'Мои результаты' }
    ],
    EXAMINER: [
      { page: 'home', label: 'Главная' },
      { page: 'exams', label: 'Экзамены' },
      { page: 'violations', label: 'Нарушения' }
    ]
  }

  const currentMenu = computed(() => menus[session.role] || menus.STUDENT)
  const activeExams = computed(() => exams.value.filter((exam) => exam.status === 'ACTIVE'))
  const totalSPoints = computed(() => formatNumber(classes.value.reduce((sum, item) => sum + (item.sPoints || 0), 0)))
  const studentCount = computed(() => users.value.filter((user) => user.role === 'STUDENT').length)
  const filteredUsers = computed(() => userRoleFilter.value === 'ALL' ? users.value : users.value.filter((user) => user.role === userRoleFilter.value))
  const visibleExams = computed(() => {
    const filtered = examStatusFilter.value === 'ALL' ? exams.value : exams.value.filter((exam) => exam.status === examStatusFilter.value)
    if (session.role !== 'STUDENT') return filtered
    const myClass = currentUser.value?.className
    return filtered.filter((exam) => !myClass || exam.schoolClass?.name === myClass)
  })
  const visibleVotings = computed(() => {
    const filtered = votingStatusFilter.value === 'ALL' ? votings.value : votings.value.filter((voting) => voting.status === votingStatusFilter.value)
    if (session.role !== 'STUDENT') return filtered
    const myClass = currentUser.value?.className
    return filtered.filter((voting) => !myClass || voting.schoolClass?.name === myClass)
  })
  const currentUser = computed(() => users.value.find((user) => user.id === session.userId))
  const currentUserClass = computed(() => classes.value.find((schoolClass) => schoolClass.name === currentUser.value?.className))
  const classStudentsForSelectedExam = computed(() => {
    const className = selectedExam.value?.exam.schoolClass?.name
    return users.value.filter((user) => user.role === 'STUDENT' && (!className || user.className === className))
  })
  const rankedClasses = computed<RankedClass[]>(() => {
    return classes.value
      .map((schoolClass) => ({
        name: schoolClass.name,
        sPoints: schoolClass.sPoints,
        delta: 0
      }))
      .sort((left, right) => right.sPoints - left.sPoints)
      .slice(0, 5)
  })
  const studentClassPoints = computed(() => currentUserClass.value ? formatNumber(currentUserClass.value.sPoints) : '-')
  const studentClassRank = computed(() => currentUserClass.value?.rank || '-')
  const curatorClassName = computed(() => currentUser.value?.className || '-')
  const curatorClassPoints = computed(() => currentUserClass.value ? formatNumber(currentUserClass.value.sPoints) : '-')
  const topbarLabel = computed(() => {
    if (session.role === 'CURATOR') return currentUser.value?.className ? `Куратор ${currentUser.value.className}` : 'Куратор'
    if (session.role === 'STUDENT') return 'Студент'
    return roleLabel(session.role)
  })
  const isStubPage = computed(() => ['violations', 'privileges', 'my-class'].includes(currentPage.value))
  const pageTitle = computed(() => {
    const item = currentMenu.value.find((menuItem) => menuItem.page === currentPage.value)
    if (currentPage.value === 'home' && session.role === 'ADMIN') return 'Сводная панель'
    return item?.label || 'Главная'
  })
  const pageSubtitle = computed(() => {
    if (currentPage.value === 'home') return ''
    if (currentPage.value === 'exams') return 'Создание, запуск, прохождение, завершение и оценивание экзаменов'
    if (currentPage.value === 'votings') return 'Создание тайного голосования, участие студентов и подсчет итогов'
    if (currentPage.value === 'users') return 'Создание пользователей, роли, валидация и список учетных записей'
    return 'Функционал архитектурного прототипа'
  })

  const recentActions = computed(() => [
    ...exams.value.slice(-3).reverse().map((exam) => `${exam.title}: ${statusLabel(exam.status).toLowerCase()}`),
    ...votings.value.slice(-2).reverse().map((voting) => `${voting.title}: ${voting.status === 'ACTIVE' ? 'активно' : 'завершено'}`)
  ].slice(0, 4))
  const curatorViolations = computed<Array<{ date: string; student: string; description: string; exam: string; points: number }>>(() => [])
  const curatorPrivilegeRequests = computed<Array<{ date: string; title: string; status: string; kind: string }>>(() => [])
  const examinerHomeRows = computed(() => exams.value.slice(0, 6).map((exam) => ({
    id: exam.id,
    date: exam.scheduledStartTime ? new Date(exam.scheduledStartTime).toLocaleDateString('ru-RU') : '-',
    time: exam.scheduledStartTime ? new Date(exam.scheduledStartTime).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' }) : '-',
    exam: exam.title,
    subject: exam.subject,
    classes: exam.schoolClass?.name || '-',
    participants: users.value.filter((user) => user.role === 'STUDENT' && (!exam.schoolClass?.name || user.className === exam.schoolClass.name)).length,
    status: statusLabel(exam.status)
  })))
  const examQuestionCount = computed(() => examQuestionsText.value.split('\n').map((text) => text.trim()).filter(Boolean).length)
  const currentQuestion = computed(() => selectedExam.value?.questions[currentQuestionIndex.value] || null)
  const currentExamSubmitted = computed(() => Boolean(selectedExam.value?.attempt?.submitted))
  const currentVotingLocked = computed(() => Boolean(selectedVoting.value?.hasVoted || selectedVoting.value?.voting.status !== 'ACTIVE'))

  function authHeaders(): HeadersInit {
    return {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${session.token}`
    }
  }

  async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
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

  function setMessage(text: string, kind: 'info' | 'success' | 'error' = 'info') {
    message.text = text
    message.kind = kind
  }

  function formatNumber(value: number) {
    return value.toLocaleString('ru-RU')
  }

  function formatDateTime(value?: string | null) {
    if (!value) return '-'
    return new Date(value).toLocaleString('ru-RU', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  function generatePassword() {
    const password = Math.random().toString(36).slice(2, 10)
    registerForm.password = password
    confirmPassword.value = password
  }

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

  function totalVotes(results: Record<string, number>) {
    return Object.values(results).reduce((sum, count) => sum + count, 0)
  }

  function toLocalDateTimeInputValue(date: Date) {
    const offset = date.getTimezoneOffset() * 60000
    return new Date(date.getTime() - offset).toISOString().slice(0, 19)
  }

  function votingRemainingLabel(voting: SecretVoting) {
    if (voting.status !== 'ACTIVE' || !voting.endsAt) return '—'
    const remainingMs = new Date(voting.endsAt).getTime() - now.value
    if (remainingMs <= 0) return '—'
    const minutes = Math.floor(remainingMs / 60000)
    const seconds = Math.floor((remainingMs % 60000) / 1000)
    return `${minutes}:${String(seconds).padStart(2, '0')}`
  }

  async function login() {
    loading.value = true
    try {
      const response = await fetch(`${API_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(loginForm)
      })
      const data = (await response.json()) as LoginResponse & { message?: string }
      if (!response.ok) throw new Error(data.message || 'Неверный логин или пароль')

      Object.assign(session, data)
      if (rememberMe.value) localStorage.setItem('school-session', JSON.stringify(data))
      currentPage.value = 'home'
      await bootstrap()
      setMessage('Вход выполнен', 'success')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Ошибка входа', 'error')
    } finally {
      loading.value = false
    }
  }

  function logout() {
    stompClient.value?.deactivate()
    localStorage.removeItem('school-session')
    Object.assign(session, { token: '', role: 'STUDENT', accountId: null, userId: null, displayName: '' })
    selectedExam.value = null
    selectedVoting.value = null
    currentPage.value = 'home'
  }

  async function bootstrap() {
    if (session.role === 'STUDENT') {
      await Promise.allSettled([loadCurrentUser(), loadMyClass(), loadExams(), loadVotings(), loadMyResults()])
      return
    }

    await Promise.allSettled([loadClasses(), loadUsers(), loadExams(), loadVotings(), loadMyResults()])
  }

  async function loadCurrentUser() {
    const user = await api<UserResponse>('/users/me')
    users.value = [user]
  }

  async function loadMyClass() {
    try {
      const schoolClass = await api<SchoolClass>('/users/me/class')
      classes.value = [schoolClass]
    } catch {
      classes.value = []
    }
  }

  async function loadClasses() {
    classes.value = await api<SchoolClass[]>('/users/classes')
  }

  async function loadUsers() {
    users.value = await api<UserResponse[]>('/users')
  }

  async function loadExams() {
    exams.value = await api<ExamSession[]>(session.role === 'STUDENT' ? '/exam/session/my' : '/exam/session')
  }

  async function loadVotings() {
    votings.value = await api<SecretVoting[]>(session.role === 'STUDENT' ? '/vote/secret/my' : '/vote/secret')
  }

  async function loadMyResults() {
    if (session.userId) {
      myResults.value = await api<ExamResult[]>(`/exam/session/students/${session.userId}/results`)
    }
  }

  async function refreshCurrent() {
    try {
      await bootstrap()
      if (selectedExam.value) await openExam(selectedExam.value.exam.id)
      if (selectedVoting.value) await openVoting(selectedVoting.value.voting.id)
      setMessage('Данные обновлены', 'success')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Ошибка обновления', 'error')
    }
  }

  function openPage(page: Page) {
    currentPage.value = page
    setMessage('', 'info')
    if (page === 'results') loadMyResults()
  }

  function resetRegisterForm() {
    Object.assign(registerForm, {
      fullName: '',
      username: '',
      email: '',
      password: '',
      role: 'STUDENT',
      classId: null,
      passportData: '',
      entranceExamScore: null,
      contactInfo: '',
      birthDate: ''
    })
    confirmPassword.value = ''
    editingUserId.value = null
  }

  async function registerUser() {
    loading.value = true
    try {
      if (!editingUserId.value && registerForm.password !== confirmPassword.value) {
        throw new Error('Пароли не совпадают')
      }

      const body = {
        ...registerForm,
        classId: registerForm.role === 'STUDENT' ? registerForm.classId : null,
        entranceExamScore: registerForm.role === 'STUDENT' ? registerForm.entranceExamScore : null,
        birthDate: registerForm.birthDate || null
      }

      const wasEditing = editingUserId.value !== null
      if (editingUserId.value) {
        await api<UserResponse>(`/users/${editingUserId.value}`, {
          method: 'PUT',
          body: JSON.stringify({
            fullName: body.fullName,
            email: body.email,
            role: body.role,
            classId: body.classId,
            passportData: body.passportData,
            entranceExamScore: body.entranceExamScore,
            contactInfo: body.contactInfo,
            birthDate: body.birthDate,
            active: true
          })
        })
      } else {
        await api<UserResponse>('/users/register', { method: 'POST', body: JSON.stringify(body) })
      }

      await loadUsers()
      resetRegisterForm()
      setMessage(wasEditing ? 'Пользователь обновлен' : 'Пользователь создан', 'success')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Ошибка регистрации', 'error')
    } finally {
      loading.value = false
    }
  }

  function editUser(user: UserResponse) {
    editingUserId.value = user.id
    Object.assign(registerForm, {
      fullName: user.fullName,
      username: user.username,
      email: user.email,
      password: '',
      role: user.role,
      classId: user.classId || classes.value.find((schoolClass) => schoolClass.name === user.className)?.id || null,
      passportData: user.passportData || '',
      entranceExamScore: user.entranceExamScore ?? null,
      contactInfo: user.contactInfo || '',
      birthDate: user.birthDate || ''
    })
    confirmPassword.value = ''
  }

  async function deactivateUser(user: UserResponse) {
    const confirmed = window.confirm(`Исключить/деактивировать пользователя "${user.fullName}"?`)
    if (!confirmed) return

    try {
      await api<UserResponse>(`/users/${user.id}`, { method: 'DELETE' })
      await loadUsers()
      setMessage('Пользователь деактивирован', 'success')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Ошибка деактивации пользователя', 'error')
    }
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
    if (!selectedExam.value || !session.userId) return
    if (currentExamSubmitted.value) return
    for (const question of selectedExam.value.questions) {
      const text = answerDrafts[question.id]?.trim()
      if (!text) continue
      await api<Answer>(`/exam/session/${selectedExam.value.exam.id}/answers/me`, {
        method: 'POST',
        body: JSON.stringify({
          questionId: question.id,
          text,
          finalSubmitted: false
        })
      })
    }
    await api(`/exam/session/${selectedExam.value.exam.id}/attempt/me/submit`, { method: 'POST' })
    await openExam(selectedExam.value.exam.id)
    setMessage('Ответы сохранены', 'success')
  }

  async function saveCurrentAnswer() {
    if (!selectedExam.value || !currentQuestion.value || !session.userId) return
    if (currentExamSubmitted.value) return

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
      body: JSON.stringify({
        questionId,
        text,
        finalSubmitted: false
      })
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

  async function createVoting() {
    try {
      const options = votingOptionsText.value
        .split('\n')
        .map((label) => label.trim())
        .filter(Boolean)
        .map((label) => ({ label }))
      await api<SecretVoting>('/vote/secret', {
        method: 'POST',
        body: JSON.stringify({
          ...votingForm,
          endsAt: toLocalDateTimeInputValue(new Date(Date.now() + votingDurationMinutes.value * 60000)),
          options
        })
      })
      await loadVotings()
      setMessage('Голосование создано', 'success')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Ошибка создания голосования', 'error')
    }
  }

  async function openVoting(id: number) {
    selectedVoting.value = await api<VotingDetails>(`/vote/secret/${id}`)
    selectedVoteOptionId.value = null
  }

  async function submitVote(optionId: number) {
    if (!selectedVoting.value || !session.userId) return
    try {
      await api(`/vote/secret/${selectedVoting.value.voting.id}/votes/me`, {
        method: 'POST',
        body: JSON.stringify({ optionId })
      })
      await openVoting(selectedVoting.value.voting.id)
      setMessage('Голос принят', 'success')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Ошибка голосования', 'error')
    }
  }

  async function submitSelectedVote() {
    if (!selectedVoteOptionId.value || currentVotingLocked.value) return
    await submitVote(selectedVoteOptionId.value)
  }

  async function finishVoting(id: number) {
    await api<SecretVoting>(`/vote/secret/${id}/finish`, { method: 'POST' })
    await loadVotings()
    await openVoting(id)
    setMessage('Голосование завершено', 'success')
  }

  function roleLabel(role?: Role) {
    return {
      STUDENT: 'Ученик',
      CURATOR: 'Куратор класса',
      EXAMINER: 'Экзаменатор-модератор',
      ADMIN: 'Администратор'
    }[role || 'STUDENT']
  }

  function statusLabel(status: ExamStatus) {
    return {
      PREPARED: 'Подготовлен',
      ACTIVE: 'Активен',
      FINISHED: 'Завершен'
    }[status]
  }

  const clockInterval = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)

  const autosaveInterval = window.setInterval(() => {
    autosaveCurrentAnswer()
  }, 30000)

  onUnmounted(() => {
    window.clearInterval(clockInterval)
    window.clearInterval(autosaveInterval)
    document.removeEventListener('fullscreenchange', handleFullscreenChange)
    document.removeEventListener('visibilitychange', handleVisibilityChange)
    window.removeEventListener('blur', handleWindowBlur)
  })

  onMounted(async () => {
    document.addEventListener('fullscreenchange', handleFullscreenChange)
    document.addEventListener('visibilitychange', handleVisibilityChange)
    window.addEventListener('blur', handleWindowBlur)

    const saved = localStorage.getItem('school-session')
    if (!saved) return

    try {
      const parsed = JSON.parse(saved) as Partial<LoginResponse>
      if (!parsed.token || !parsed.role || !menus[parsed.role]) {
        throw new Error('Invalid saved session')
      }
      Object.assign(session, parsed)
      await bootstrap()
    } catch {
      localStorage.removeItem('school-session')
      Object.assign(session, { token: '', role: 'STUDENT', accountId: null, userId: null, displayName: '' })
    }
  })

  return reactive({
    session,
    loginForm,
    rememberMe,
    loading,
    currentPage,
    message,
    users,
    classes,
    exams,
    votings,
    selectedExam,
    selectedVoting,
    myResults,
    realtimeEvents,
    currentQuestionIndex,
    selectedVoteOptionId,
    now,
    examStatusFilter,
    votingStatusFilter,
    userRoleFilter,
    editingUserId,
    userFilters,
    registerForm,
    confirmPassword,
    sendCredentials,
    examForm,
    examQuestionsText,
    answerDrafts,
    gradeForm,
    votingForm,
    votingOptionsText,
    votingDurationMinutes,
    violationForm,
    currentMenu,
    activeExams,
    totalSPoints,
    studentCount,
    filteredUsers,
    visibleExams,
    visibleVotings,
    currentUser,
    currentUserClass,
    classStudentsForSelectedExam,
    rankedClasses,
    studentClassPoints,
    studentClassRank,
    curatorClassName,
    curatorClassPoints,
    topbarLabel,
    isStubPage,
    pageTitle,
    pageSubtitle,
    recentActions,
    curatorViolations,
    curatorPrivilegeRequests,
    examinerHomeRows,
    examQuestionCount,
    currentQuestion,
    currentExamSubmitted,
    currentVotingLocked,
    setMessage,
    formatNumber,
    formatDateTime,
    generatePassword,
    answerCountForStudent,
    examElapsedLabel,
    examRemainingLabel,
    totalVotes,
    votingRemainingLabel,
    login,
    logout,
    bootstrap,
    refreshCurrent,
    openPage,
    resetRegisterForm,
    registerUser,
    editUser,
    deactivateUser,
    createExam,
    openExam,
    startExam,
    finishExam,
    submitAllAnswers,
    saveCurrentAnswer,
    saveAnswer,
    nextQuestion,
    requestExamFullscreen,
    reportClientExamViolation,
    reportViolation,
    gradeExam,
    connectExamSocket,
    createVoting,
    openVoting,
    submitVote,
    submitSelectedVote,
    finishVoting,
    roleLabel,
    statusLabel,
  })
}

export type SchoolAppContext = ReturnType<typeof useSchoolApp>
