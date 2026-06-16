import { onMounted, onUnmounted, reactive } from 'vue'
import { createSchoolApiClient } from './schoolApiClient'
import { formatDateTime, formatNumber, totalVotes } from './schoolAppFormatters'
import { roleLabel, statusLabel, userFilters } from './schoolAppLabels'
import { createSchoolAppState } from './schoolAppState'
import { useAuth } from './useAuth'
import { useExamManagement } from './useExamManagement'
import { useSchoolData } from './useSchoolData'
import { useSchoolViewModel } from './useSchoolViewModel'
import { useUserManagement } from './useUserManagement'
import { useVotingManagement } from './useVotingManagement'

export function useSchoolApp() {
  const {
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
    selectedVoteOptionId,
    now,
    examStatusFilter,
    votingStatusFilter,
    userRoleFilter,
    editingUserId,
    expulsionCandidate,
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
    violationForm
  } = createSchoolAppState()
  const api = createSchoolApiClient(session)
  const {
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
  } = useSchoolViewModel({
    session,
    currentPage,
    users,
    classes,
    exams,
    votings,
    userRoleFilter
  })

  function setMessage(text: string, kind: 'info' | 'success' | 'error' = 'info') {
    message.text = text
    message.kind = kind
  }

  const {
    bootstrap,
    loadClasses,
    loadUsers,
    loadExams,
    loadVotings,
    openPage
  } = useSchoolData({
    api,
    session,
    currentPage,
    users,
    classes,
    exams,
    votings,
    myResults,
    setMessage
  })

  const { login, logout, restoreSession } = useAuth({
    session,
    loginForm,
    rememberMe,
    loading,
    currentPage,
    message,
    selectedExam,
    selectedVoting,
    stompClient,
    bootstrap,
    setMessage
  })

  const {
    generatePassword,
    resetRegisterForm,
    registerUser,
    editUser,
    openExpulsionPlaceholder,
    closeExpulsionPlaceholder,
    deactivateUser
  } = useUserManagement({
    api,
    registerForm,
    confirmPassword,
    editingUserId,
    expulsionCandidate,
    classes,
    loading,
    loadUsers,
    setMessage
  })

  const {
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
  } = useExamManagement({
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
  })

  const {
    visibleVotings,
    currentVotingLocked,
    votingRemainingLabel,
    createVoting,
    openVoting,
    submitVote,
    submitSelectedVote,
    finishVoting
  } = useVotingManagement({
    api,
    session,
    users,
    votings,
    selectedVoting,
    selectedVoteOptionId,
    votingStatusFilter,
    votingForm,
    votingOptionsText,
    votingDurationMinutes,
    now,
    loadVotings,
    setMessage
  })

  async function refreshCurrent() {
    try {
      await bootstrap()
      if (selectedExam.value) await openExam(selectedExam.value.exam.id)
      if (selectedVoting.value) await openVoting(selectedVoting.value.voting.id)
      setMessage('Р”Р°РЅРЅС‹Рµ РѕР±РЅРѕРІР»РµРЅС‹', 'success')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'РћС€РёР±РєР° РѕР±РЅРѕРІР»РµРЅРёСЏ', 'error')
    }
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

    await restoreSession()
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
    expulsionCandidate,
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
    isAdminReferencePage,
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
    openExpulsionPlaceholder,
    closeExpulsionPlaceholder,
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



