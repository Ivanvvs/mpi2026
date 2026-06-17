import { computed } from 'vue'
import type { ExamSession, Page, RankedClass, Role, SchoolClass, SecretVoting, UserResponse } from '../types/domain'
import { formatNumber } from './schoolAppFormatters'
import { menus, roleLabel, statusLabel } from './schoolAppLabels'

type RefLike<T> = { value: T }

interface SessionState {
  role: Role
  userId: number | null
}

interface UseSchoolViewModelOptions {
  session: SessionState
  currentPage: RefLike<Page>
  users: RefLike<UserResponse[]>
  classes: RefLike<SchoolClass[]>
  exams: RefLike<ExamSession[]>
  votings: RefLike<SecretVoting[]>
  userRoleFilter: RefLike<Role | 'ALL'>
}

export function useSchoolViewModel(options: UseSchoolViewModelOptions) {
  const {
    session,
    currentPage,
    users,
    classes,
    exams,
    votings,
    userRoleFilter
  } = options

  const currentMenu = computed(() => menus[session.role] || menus.STUDENT)
  const activeExams = computed(() => exams.value.filter((exam) => exam.status === 'ACTIVE'))
  const totalSPoints = computed(() => formatNumber(classes.value.reduce((sum, item) => sum + (item.sPoints || 0), 0)))
  const studentCount = computed(() => users.value.filter((user) => user.role === 'STUDENT').length)
  const filteredUsers = computed(() => userRoleFilter.value === 'ALL' ? users.value : users.value.filter((user) => user.role === userRoleFilter.value))
  const currentUser = computed(() => users.value.find((user) => user.id === session.userId))
  const currentUserClass = computed(() => classes.value.find((schoolClass) => schoolClass.name === currentUser.value?.className))
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
  const isAdminReferencePage = computed(() => session.role === 'ADMIN' && ['exams', 'votings'].includes(currentPage.value))
  const pageTitle = computed(() => {
    const item = currentMenu.value.find((menuItem) => menuItem.page === currentPage.value)
    if (currentPage.value === 'home' && session.role === 'ADMIN') return 'Сводная панель'
    return item?.label || 'Главная'
  })
  const pageSubtitle = computed(() => {
    if (currentPage.value === 'home') return ''
    if (isAdminReferencePage.value) return ''
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

  return {
    currentMenu,
    activeExams,
    totalSPoints,
    studentCount,
    filteredUsers,
    currentUser,
    currentUserClass,
    rankedClasses,
    studentClassPoints,
    studentClassRank,
    curatorClassName,
    curatorClassPoints,
    topbarLabel,
    isStubPage,
    isAdminReferencePage,
    pageTitle,
    pageSubtitle,
    recentActions,
    curatorViolations,
    curatorPrivilegeRequests,
    examinerHomeRows
  }
}
