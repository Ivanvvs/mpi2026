import type { Ref } from 'vue'
import { API_URL } from '../services/api'
import type { ExamDetails, LoginResponse, Page, Role, VotingDetails } from '../types/domain'
import { menus } from './schoolAppLabels'

type MessageKind = 'info' | 'success' | 'error'

interface SessionState {
  token: string
  role: Role
  accountId: number | null
  userId: number | null
  displayName: string
}

interface LoginFormState {
  username: string
  password: string
}

interface MessageState {
  text: string
  kind: MessageKind
}

interface DeactivatableClient {
  deactivate: () => void
}

interface UseAuthOptions {
  session: SessionState
  loginForm: LoginFormState
  rememberMe: Ref<boolean>
  loading: Ref<boolean>
  currentPage: Ref<Page>
  message: MessageState
  selectedExam: Ref<ExamDetails | null>
  selectedVoting: Ref<VotingDetails | null>
  stompClient: Ref<DeactivatableClient | null>
  bootstrap: () => Promise<void>
  setMessage: (text: string, kind?: MessageKind) => void
}

export function useAuth(options: UseAuthOptions) {
  const {
    session,
    loginForm,
    rememberMe,
    loading,
    currentPage,
    selectedExam,
    selectedVoting,
    stompClient,
    bootstrap,
    setMessage
  } = options

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

  async function restoreSession() {
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
  }

  return {
    login,
    logout,
    restoreSession
  }
}
