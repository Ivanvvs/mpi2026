import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { computed } from 'vue'
import { WS_URL } from '../services/api'
import type { AdminDashboardResponse, Page, Role, SchoolClass } from '../types/domain'

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
    loadAdminDashboard,
    refreshRankPreview,
    confirmRankUpdates,
    connectAdminDashboardSocket,
    disconnectAdminDashboardSocket
  }
}
