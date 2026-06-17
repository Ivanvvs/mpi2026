import type {
  ExamResult,
  ExamSession,
  Page,
  Role,
  SchoolClass,
  SecretVoting,
  UserResponse,
} from '../types/domain'

type ApiClient = <T>(path: string, options?: RequestInit) => Promise<T>
type MessageKind = 'info' | 'success' | 'error'
type RefLike<T> = { value: T }

interface SessionState {
  role: Role
  userId: number | null
}

interface UseSchoolDataOptions {
  api: ApiClient
  session: SessionState
  currentPage: RefLike<Page>
  users: RefLike<UserResponse[]>
  classes: RefLike<SchoolClass[]>
  exams: RefLike<ExamSession[]>
  votings: RefLike<SecretVoting[]>
  myResults: RefLike<ExamResult[]>
  setMessage: (text: string, kind?: MessageKind) => void
}

export function useSchoolData(options: UseSchoolDataOptions) {
  const {
    api,
    session,
    currentPage,
    users,
    classes,
    exams,
    votings,
    myResults,
    setMessage
  } = options

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

  function openPage(page: Page) {
    currentPage.value = page
    setMessage('', 'info')
    if (page === 'results') loadMyResults()
  }

  return {
    bootstrap,
    loadCurrentUser,
    loadMyClass,
    loadClasses,
    loadUsers,
    loadExams,
    loadVotings,
    loadMyResults,
    openPage
  }
}
