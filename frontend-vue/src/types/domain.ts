export type Role = 'STUDENT' | 'CURATOR' | 'EXAMINER' | 'ADMIN'
export type ExamStatus = 'PREPARED' | 'ACTIVE' | 'FINISHED'
export type VotingStatus = 'ACTIVE' | 'FINISHED'
export type Page =
  | 'home'
  | 'classes'
  | 'users'
  | 'exams'
  | 'votings'
  | 'violations'
  | 'privileges'
  | 'my-class'
  | 'results'

export interface LoginResponse {
  token: string
  role: Role
  accountId: number
  userId: number | null
  displayName: string
}

export interface SchoolClass {
  id: number
  name: string
  rank: string
  proposedRank?: string
  sPoints: number
  studentCount?: number
  rankChangeRequired?: boolean
}

export interface UserResponse {
  id: number
  accountId: number
  fullName: string
  username: string
  email: string
  role: Role
  classId?: number | null
  className?: string
  passportData?: string | null
  entranceExamScore?: number | null
  contactInfo?: string | null
  birthDate?: string | null
  sPoints?: number
  active: boolean
  createdAt?: string | null
}

export interface ExamSession {
  id: number
  title: string
  subject: string
  status: ExamStatus
  schoolClass?: SchoolClass
  totalQuestions?: number
  durationMinutes?: number
  scheduledStartTime?: string | null
  startTime?: string | null
  endTime?: string | null
}

export interface Question {
  id: number
  orderIndex: number
  text: string
  correctAnswer?: string | null
  maxScore?: number | null
}

export interface Answer {
  id: number
  questionId: number
  userId: number
  text: string
  finalSubmitted?: boolean
}

export interface ExamAttempt {
  started: boolean
  submitted: boolean
  startedAt?: string | null
  submittedAt?: string | null
}

export interface ExamStudentAttempt {
  studentId: number
  started: boolean
  submitted: boolean
  startedAt?: string | null
  submittedAt?: string | null
}

export interface ExamResult {
  id: number
  session?: ExamSession
  student?: UserResponse
  finalScore: number
  rankPlace?: number
}

export interface ExamDetails {
  exam: ExamSession
  questions: Question[]
  answers: Answer[]
  results: ExamResult[]
  violations?: unknown[]
  attempt?: ExamAttempt
  attempts?: ExamStudentAttempt[]
}

export interface SecretVoting {
  id: number
  title: string
  description?: string
  schoolClass?: SchoolClass
  status: VotingStatus
  startedAt?: string | null
  endsAt?: string | null
  finishedAt?: string | null
}

export interface VotingOption {
  id: number
  label: string
}

export interface VotingDetails {
  voting: SecretVoting
  options: VotingOption[]
  results: Record<string, number>
  hasVoted: boolean
  resultsVisible: boolean
}

export interface RealtimeEvent {
  id: string
  type: string
  time: string
  userId?: number | null
  userName?: string
}

export interface AdminDashboardResponse {
  classes: SchoolClass[]
}

export interface RankedClass {
  id?: number
  name: string
  rank?: string
  proposedRank?: string
  sPoints: number
  studentCount?: number
  rankChangeRequired?: boolean
  delta: number
}
