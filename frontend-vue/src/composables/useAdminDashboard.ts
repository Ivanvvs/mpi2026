import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { computed, ref } from 'vue'
import { WS_URL } from '../services/api'
import type { AdminDashboardResponse, Page, RankDetails, RankDetailsStudent, Role, SchoolClass, StudentSPointHistory } from '../types/domain'

type ApiClient = <T>(path: string, options?: RequestInit) => Promise<T>
type MessageKind = 'info' | 'success' | 'error'
type RefLike<T> = { value: T }
interface SessionState { token: string; role: Role }
interface DeactivatableClient { deactivate: () => void }
interface UseAdminDashboardOptions {
  api: ApiClient; session: SessionState; currentPage: RefLike<Page>; classes: RefLike<SchoolClass[]>
  stompClient: RefLike<DeactivatableClient | null>; setMessage: (text: string, kind?: MessageKind) => void
}

export function useAdminDashboard(options: UseAdminDashboardOptions) {
  const { api, session, currentPage, classes, stompClient, setMessage } = options
  const selectedRankClassId = ref<number | null>(null)
  const selectedRankClass = computed(() => classes.value.find((schoolClass) => schoolClass.id === selectedRankClassId.value) ?? null)
  const rankDetails = ref<RankDetails | null>(null)
  const selectedStudentHistoryId = ref<number | null>(null)
  const studentSPointHistory = ref<StudentSPointHistory | null>(null)
  const studentSPointHistoryLoading = ref(false)
  const studentSPointHistoryError = ref<string | null>(null)
  let historyRequestId = 0

  async function loadAdminDashboard() {
    const response = await api<AdminDashboardResponse>('/admin/dashboard')
    classes.value = response.classes
  }

  function clearStudentSPointHistory() {
    historyRequestId++
    selectedStudentHistoryId.value = null
    studentSPointHistory.value = null
    studentSPointHistoryLoading.value = false
    studentSPointHistoryError.value = null
  }

  function selectRankUpdate(schoolClass: Pick<SchoolClass, 'id'>) {
    selectedRankClassId.value = schoolClass.id
    rankDetails.value = null
    clearStudentSPointHistory()
  }

  async function loadRankDetails() {
    if (!selectedRankClass.value?.id) return
    try {
      rankDetails.value = await api<RankDetails>(`/admin/dashboard/classes/${selectedRankClass.value.id}/rank-details`)
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Не удалось загрузить детали ранга', 'error')
    }
  }

  async function loadStudentSPointHistory(student: RankDetailsStudent) {
    const classId = selectedRankClass.value?.id
    if (!classId) return
    const requestId = ++historyRequestId
    const studentId = student.studentId
    selectedStudentHistoryId.value = studentId
    studentSPointHistory.value = null
    studentSPointHistoryError.value = null
    studentSPointHistoryLoading.value = true
    const isCurrent = () => requestId === historyRequestId
      && selectedRankClass.value?.id === classId
      && selectedStudentHistoryId.value === studentId
    try {
      const response = await api<StudentSPointHistory>(`/admin/dashboard/classes/${classId}/students/${studentId}/s-point-history`)
      if (isCurrent()) studentSPointHistory.value = response
    } catch (error) {
      if (isCurrent()) {
        studentSPointHistoryError.value = error instanceof Error ? error.message : 'Не удалось загрузить историю S-очков'
      }
    } finally {
      if (isCurrent()) studentSPointHistoryLoading.value = false
    }
  }

  async function toggleStudentSPointHistory(student: RankDetailsStudent) {
    if (selectedStudentHistoryId.value === student.studentId) {
      clearStudentSPointHistory()
      return
    }
    await loadStudentSPointHistory(student)
  }

  async function retryStudentSPointHistory(student: RankDetailsStudent) {
    await loadStudentSPointHistory(student)
  }

  async function confirmRankUpdate() {
    const schoolClass = selectedRankClass.value
    if (!schoolClass?.id) return
    try {
      const response = await api<AdminDashboardResponse>(`/admin/dashboard/classes/${schoolClass.id}/rank/confirm`, { method: 'POST' })
      classes.value = response.classes
      selectedRankClassId.value = null
      rankDetails.value = null
      clearStudentSPointHistory()
      setMessage(`Ранг класса ${schoolClass.name} обновлён`, 'success')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Не удалось обновить ранг класса', 'error')
    }
  }

  function connectAdminDashboardSocket() {
    if (session.role !== 'ADMIN' || !session.token || currentPage.value !== 'home') return
    stompClient.value?.deactivate()
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL), reconnectDelay: 3000,
      onConnect: () => client.subscribe('/topic/admin/dashboard', (frame) => {
        const payload = JSON.parse(frame.body) as AdminDashboardResponse
        const selectedClassId = selectedRankClassId.value
        const previous = classes.value.find((item) => item.id === selectedClassId)
        const next = payload.classes.find((item) => item.id === selectedClassId)
        classes.value = payload.classes
        if (!next || !next.rankChangeRequired || (previous && (
          previous.sPoints !== next.sPoints || previous.rank !== next.rank ||
          previous.proposedRank !== next.proposedRank || previous.rankChangeRequired !== next.rankChangeRequired
        ))) {
          if (!next || !next.rankChangeRequired) selectedRankClassId.value = null
          rankDetails.value = null
          clearStudentSPointHistory()
        }
      }),
      onWebSocketError: () => setMessage('Не удалось подключить realtime-сводку администратора', 'error')
    })
    stompClient.value = client
    client.activate()
  }

  function disconnectAdminDashboardSocket() { stompClient.value?.deactivate(); stompClient.value = null }

  return {
    selectedRankClassId, selectedRankClass, rankDetails, selectedStudentHistoryId,
    studentSPointHistory, studentSPointHistoryLoading, studentSPointHistoryError,
    loadAdminDashboard, selectRankUpdate, loadRankDetails, toggleStudentSPointHistory,
    retryStudentSPointHistory, confirmRankUpdate, connectAdminDashboardSocket, disconnectAdminDashboardSocket
  }
}
