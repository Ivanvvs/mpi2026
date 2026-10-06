import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { computed, ref } from 'vue'
import { WS_URL } from '../services/api'
import type { AdminDashboardResponse, Page, RankDetails, RankedClass, Role, SchoolClass } from '../types/domain'

type ApiClient = <T>(path: string, options?: RequestInit) => Promise<T>
type MessageKind = 'info' | 'success' | 'error'
type RefLike<T> = { value: T }

interface SessionState {
  token: string
  role: Role
}

interface DeactivatableClient {
  deactivate: () => void
}

interface UseAdminDashboardOptions {
  api: ApiClient
  session: SessionState
  currentPage: RefLike<Page>
  classes: RefLike<SchoolClass[]>
  stompClient: RefLike<DeactivatableClient | null>
  rankPreviewVisible: RefLike<boolean>
  setMessage: (text: string, kind?: MessageKind) => void
}

export function useAdminDashboard(options: UseAdminDashboardOptions) {
  const {
    api,
    session,
    currentPage,
    classes,
    stompClient,
    rankPreviewVisible,
    setMessage
  } = options

  const pendingRankUpdates = computed(() => classes.value.filter((schoolClass) => schoolClass.rankChangeRequired))
  const selectedRankClass = ref<RankedClass | null>(null)
  const rankDetails = ref<RankDetails | null>(null)

  async function loadAdminDashboard() {
    const response = await api<AdminDashboardResponse>('/admin/dashboard')
    classes.value = response.classes
  }

  async function refreshRankPreview() {
    const response = await api<AdminDashboardResponse>('/admin/dashboard/rank-preview')
    classes.value = response.classes
    rankPreviewVisible.value = true
    setMessage(
      response.classes.some((schoolClass) => schoolClass.rankChangeRequired)
        ? 'Предварительный пересчет рангов обновлен'
        : 'Изменений рангов не требуется',
      'info'
    )
  }

  async function confirmRankUpdates() {
    const classIds = pendingRankUpdates.value.map((schoolClass) => schoolClass.id)
    if (!classIds.length) {
      setMessage('Нет рангов для подтверждения', 'info')
      return
    }

    const response = await api<AdminDashboardResponse>('/admin/dashboard/ranks/confirm', {
      method: 'POST',
      body: JSON.stringify({ classIds })
    })
    classes.value = response.classes
    rankPreviewVisible.value = false
    setMessage('Ранги классов подтверждены', 'success')
  }

  function selectRankUpdate(schoolClass: RankedClass) {
    selectedRankClass.value = schoolClass
    rankDetails.value = null
  }

  async function loadRankDetails() {
    if (!selectedRankClass.value?.id) return
    rankDetails.value = await api<RankDetails>(
      `/admin/dashboard/classes/${selectedRankClass.value.id}/rank-details`
    )
  }

  async function confirmRankUpdate() {
    const schoolClass = selectedRankClass.value
    if (!schoolClass?.id) return

    const response = await api<AdminDashboardResponse>(
      `/admin/dashboard/classes/${schoolClass.id}/rank/confirm`,
      { method: 'POST' }
    )
    classes.value = response.classes
    selectedRankClass.value = null
    rankDetails.value = null
    rankPreviewVisible.value = false
    setMessage(`Ранг класса ${schoolClass.name} обновлён`, 'success')
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
          classes.value = payload.classes
        })
      },
      onWebSocketError: () => {
        setMessage('Не удалось подключить realtime сводки администратора', 'error')
      }
    })

    stompClient.value = client
    client.activate()
  }

  function disconnectAdminDashboardSocket() {
    stompClient.value?.deactivate()
    stompClient.value = null
  }

  return {
    pendingRankUpdates,
    selectedRankClass,
    rankDetails,
    loadAdminDashboard,
    refreshRankPreview,
    confirmRankUpdates,
    selectRankUpdate,
    loadRankDetails,
    confirmRankUpdate,
    connectAdminDashboardSocket,
    disconnectAdminDashboardSocket
  }
}
