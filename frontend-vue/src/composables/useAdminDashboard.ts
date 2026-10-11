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
  api: ApiClient
  session: SessionState
  currentPage: RefLike<Page>
  classes: RefLike<SchoolClass[]>
  stompClient: RefLike<DeactivatableClient | null>
  setMessage: (text: string, kind?: MessageKind) => void
}

export function useAdminDashboard(options: UseAdminDashboardOptions) {
  const { api, session, currentPage, classes, stompClient, setMessage } = options
  const selectedRankClassId = ref<number | null>(null)
  const selectedRankClass = computed(() => classes.value.find((schoolClass) => schoolClass.id === selectedRankClassId.value) ?? null)
  const rankDetails = ref<RankDetails | null>(null)
  const selectedStudentHistoryId = ref<number | null>(null)
  const studentSPointHistory = ref<StudentSPointHistory | null>(null)
  const studentSPointHistoryLoading = ref(false)

  async function loadAdminDashboard() {
    const response = await api<AdminDashboardResponse>('/admin/dashboard')
    classes.value = response.classes
  }

  function clearStudentSPointHistory() {
    selectedStudentHistoryId.value = null
    studentSPointHistory.value = null
    studentSPointHistoryLoading.value = false
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

  async function toggleStudentSPointHistory(student: RankDetailsStudent) {
    if (!selectedRankClass.value) return
    if (selectedStudentHistoryId.value === student.studentId) {
      clearStudentSPointHistory()
      return
    }
    selectedStudentHistoryId.value = student.studentId
    studentSPointHistory.value = null
    studentSPointHistoryLoading.value = true
    try {
      studentSPointHistory.value = await api<StudentSPointHistory>(
        `/admin/dashboard/classes/${selectedRankClass.value.id}/students/${student.studentId}/s-point-history`
      )
    } catch (error) {
      clearStudentSPointHistory()
      setMessage(error instanceof Error ? error.message : 'Не удалось загрузить историю S-очков', 'error')
    } finally {
      studentSPointHistoryLoading.value = false
    }
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
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 3000,
      onConnect: () => {
        client.subscribe('/topic/admin/dashboard', (frame) => {
          const payload = JSON.parse(frame.body) as AdminDashboardResponse
          const selectedClassId = selectedRankClassId.value
          const previousSelectedClass = classes.value.find((item) => item.id === selectedClassId)
          const nextSelectedClass = payload.classes.find((item) => item.id === selectedClassId)
          classes.value = payload.classes
          if (!nextSelectedClass || !nextSelectedClass.rankChangeRequired) {
            selectedRankClassId.value = null
            rankDetails.value = null
            clearStudentSPointHistory()
          } else if (previousSelectedClass && (
            previousSelectedClass.sPoints !== nextSelectedClass.sPoints ||
            previousSelectedClass.rank !== nextSelectedClass.rank ||
            previousSelectedClass.proposedRank !== nextSelectedClass.proposedRank ||
            previousSelectedClass.rankChangeRequired !== nextSelectedClass.rankChangeRequired
          )) {
            rankDetails.value = null
            clearStudentSPointHistory()
          }
        })
      },
      onWebSocketError: () => setMessage('Не удалось подключить realtime-сводку администратора', 'error')
    })
    stompClient.value = client
    client.activate()
  }

  function disconnectAdminDashboardSocket() {
    stompClient.value?.deactivate()
    stompClient.value = null
  }

  return {
    selectedRankClassId,
    selectedRankClass,
    rankDetails,
    selectedStudentHistoryId,
    studentSPointHistory,
    studentSPointHistoryLoading,
    loadAdminDashboard,
    selectRankUpdate,
    loadRankDetails,
    toggleStudentSPointHistory,
    confirmRankUpdate,
    connectAdminDashboardSocket,
    disconnectAdminDashboardSocket
  }
}
