import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { computed, ref } from 'vue'
import { WS_URL } from '../services/api'
import type { AdminDashboardResponse, Page, RankDetails, Role, SchoolClass } from '../types/domain'

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
  const selectedRankClassId = ref<number | null>(null)
  const selectedRankClass = computed(() => classes.value.find((schoolClass) => schoolClass.id === selectedRankClassId.value) ?? null)
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

  function selectRankUpdate(schoolClass: Pick<SchoolClass, 'id'>) {
    selectedRankClassId.value = schoolClass.id
    rankDetails.value = null
  }

  async function loadRankDetails() {
    if (!selectedRankClass.value?.id) return
    try {
      rankDetails.value = await api<RankDetails>(
        `/admin/dashboard/classes/${selectedRankClass.value.id}/rank-details`
      )
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Не удалось загрузить детали ранга', 'error')
    }
  }

  async function confirmRankUpdate() {
    const schoolClass = selectedRankClass.value
    if (!schoolClass?.id) return

    try {
      const response = await api<AdminDashboardResponse>(
        `/admin/dashboard/classes/${schoolClass.id}/rank/confirm`,
        { method: 'POST' }
      )
      classes.value = response.classes
      selectedRankClassId.value = null
      rankDetails.value = null
      rankPreviewVisible.value = false
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
          const previousClasses = classes.value
          const selectedClassId = selectedRankClassId.value
          const previousSelectedClass = previousClasses.find((item) => item.id === selectedClassId)
          const nextSelectedClass = payload.classes.find((item) => item.id === selectedClassId)
          const changedClasses = payload.classes.filter((nextClass) => {
            const previousClass = previousClasses.find((item) => item.id === nextClass.id)
            return previousClass && (
              previousClass.sPoints !== nextClass.sPoints ||
              (!previousClass.rankChangeRequired && nextClass.rankChangeRequired)
            )
          })
          classes.value = payload.classes
          if (!nextSelectedClass || !nextSelectedClass.rankChangeRequired) {
            selectedRankClassId.value = null
            rankDetails.value = null
          } else if (previousSelectedClass && (
            previousSelectedClass.sPoints !== nextSelectedClass.sPoints ||
            previousSelectedClass.rank !== nextSelectedClass.rank ||
            previousSelectedClass.proposedRank !== nextSelectedClass.proposedRank ||
            previousSelectedClass.rankChangeRequired !== nextSelectedClass.rankChangeRequired
          )) {
            rankDetails.value = null
          }
          if (changedClasses.length === 1) {
            const changedClass = changedClasses[0]
            const previousClass = previousClasses.find((item) => item.id === changedClass.id)
            setMessage(
              previousClass?.sPoints !== changedClass.sPoints
                ? changedClass.rankChangeRequired
                  ? `S-очки класса ${changedClass.name} изменились. Требуется проверить ранг.`
                  : `S-очки класса ${changedClass.name} изменились.`
                : `Для класса ${changedClass.name} требуется проверить ранг.`,
              'info'
            )
          } else if (changedClasses.length > 1) {
            setMessage(
              changedClasses.some((schoolClass) => schoolClass.rankChangeRequired)
                ? 'Изменились данные нескольких классов. Требуется проверить ранги.'
                : 'Изменились S-очки нескольких классов.',
              'info'
            )
          }
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
    selectedRankClassId,
    selectedRankClass,
    rankDetails,
    loadAdminDashboard,
    refreshRankPreview,
    selectRankUpdate,
    loadRankDetails,
    confirmRankUpdate,
    connectAdminDashboardSocket,
    disconnectAdminDashboardSocket
  }
}
