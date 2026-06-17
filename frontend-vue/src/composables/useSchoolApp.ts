import { onMounted, onUnmounted, reactive, watch } from 'vue'
import { createSchoolApiClient } from './schoolApiClient'
import { formatDateTime, formatNumber, totalVotes } from './schoolAppFormatters'
import { roleLabel, statusLabel, userFilters } from './schoolAppLabels'
import { createSchoolAppState } from './schoolAppState'
import { useAuth } from './useAuth'
import { useAdminDashboard } from './useAdminDashboard'
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
    userSaveSuccess,
    loadUsers,
    setMessage
  })

  const {
    pendingRankUpdates,
    refreshRankPreview,
    confirmRankUpdates,
    connectAdminDashboardSocket,
    disconnectAdminDashboardSocket
  } = useAdminDashboard({
    api,
    session,
    currentPage,
    classes,
    stompClient,
    rankPreviewVisible,
    setMessage
  })

  const {
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
  } = useExamManagement({
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
    currentPage,
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
      setMessage('Данные обновлены', 'success')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Ошибка обновления', 'error')
    }
  }

  const clockInterval = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)

  const autosaveInterval = window.setInterval(() => {
    autosaveCurrentAnswer()
  }, 30000)

  watch(currentQuestionIndex, (index) => {
    if (selectedExam.value) {
      localStorage.setItem('school-current-question-index', String(index))
    }
  })

  watch(
    () => [session.token, session.role, currentPage.value] as const,
    ([token, role, page]) => {
      if (!token) {
        disconnectAdminDashboardSocket()
        return
      }

      if (role === 'ADMIN' && page === 'home') {
        connectAdminDashboardSocket()
        return
      }

      if (role === 'ADMIN') {
        disconnectAdminDashboardSocket()
      }
    },
    { immediate: true }
  )

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
    const savedExamId = Number(localStorage.getItem('school-selected-exam-id'))
    const savedQuestionIndex = Number(localStorage.getItem('school-current-question-index'))
    if (session.token && currentPage.value === 'exams' && Number.isInteger(savedExamId) && savedExamId > 0) {
      try {
        currentQuestionIndex.value = Number.isInteger(savedQuestionIndex) && savedQuestionIndex >= 0 ? savedQuestionIndex : 0
        await openExam(savedExamId, true)
      } catch {
        localStorage.removeItem('school-selected-exam-id')
        localStorage.removeItem('school-current-question-index')
      }
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
    userFilters,
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
    pendingRankUpdates,
    examQuestionCount,
    currentQuestion,
    currentExamSubmitted,
    selectedMonitorStudent,
    selectedMonitorAnswers,
    finalScoreRows,
    currentVotingLocked,
    setMessage,
    formatNumber,
    formatDateTime,
    generatePassword,
    answerCountForStudent,
    examStudentStatus,
    examElapsedLabel,
    examEndLabel,
    examRemainingLabel,
    totalVotes,
    votingRemainingLabel,
    login,
    logout,
    bootstrap,
    refreshCurrent,
    openPage,
    refreshRankPreview,
    confirmRankUpdates,
    resetRegisterForm,
    registerUser,
    editUser,
    openExpulsionPlaceholder,
    closeExpulsionPlaceholder,
    deactivateUser,
    createExam,
    handleExamFileChange,
    resetExamForm,
    openExam,
    startExam,
    finishExam,
    submitAllAnswers,
    saveCurrentAnswer,
    saveAnswer,
    nextQuestion,
    previousQuestion,
    openStudentMonitor,
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




