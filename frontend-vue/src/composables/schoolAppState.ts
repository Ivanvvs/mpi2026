import type { Client } from '@stomp/stompjs'
import { reactive, ref } from 'vue'
import type {
  ExamDetails,
  ExamResult,
  ExamSession,
  ExamStatus,
  Page,
  RealtimeEvent,
  Role,
  SchoolClass,
  SecretVoting,
  UserResponse,
  VotingDetails,
  VotingStatus
} from '../types/domain'

export function createSchoolAppState() {
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
  const selectedMonitorStudentId = ref<number | null>(null)
  const selectedVoteOptionId = ref<number | null>(null)
  const now = ref(Date.now())
  const examStatusFilter = ref<ExamStatus | 'ALL'>('ALL')
  const examMonitorTab = ref<'MONITOR' | 'VIOLATIONS' | 'RESULTS'>('MONITOR')
  const votingStatusFilter = ref<VotingStatus | 'ALL'>('ALL')

  const userRoleFilter = ref<Role | 'ALL'>('ALL')
  const editingUserId = ref<number | null>(null)
  const expulsionCandidate = ref<UserResponse | null>(null)
  const rankPreviewVisible = ref(false)

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
  const userSaveSuccess = ref(false)

  const examForm = reactive({
    subject: '',
    classId: null as number | null,
    scheduledStartTime: '',
    endsAt: '',
    description: ''
  })
  const examQuestionsText = ref('')
  const examQuestionFileName = ref('')
  const examFileInputKey = ref(0)
  const examCreateSuccess = ref(false)
  const answerSaveSuccess = ref(false)
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

  return {
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
    stompClient,
    currentQuestionIndex,
    selectedMonitorStudentId,
    selectedVoteOptionId,
    now,
    examStatusFilter,
    examMonitorTab,
    votingStatusFilter,
    userRoleFilter,
    editingUserId,
    expulsionCandidate,
    rankPreviewVisible,
    registerForm,
    confirmPassword,
    sendCredentials,
    userSaveSuccess,
    examForm,
    examQuestionsText,
    examQuestionFileName,
    examFileInputKey,
    examCreateSuccess,
    answerSaveSuccess,
    answerDrafts,
    gradeForm,
    votingForm,
    votingOptionsText,
    votingDurationMinutes,
    violationForm
  }
}
