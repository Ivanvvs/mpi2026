import { computed } from 'vue'
import type { Role, SecretVoting, UserResponse, VotingDetails } from '../types/domain'
import { toLocalDateTimeInputValue } from './schoolAppFormatters'

type ApiClient = <T>(path: string, options?: RequestInit) => Promise<T>
type MessageKind = 'info' | 'success' | 'error'
type RefLike<T> = { value: T }

interface SessionState {
  role: Role
  userId: number | null
}

interface VotingFormState {
  title: string
  description: string
  classId: number | null
}

interface UseVotingManagementOptions {
  api: ApiClient
  session: SessionState
  users: RefLike<UserResponse[]>
  votings: RefLike<SecretVoting[]>
  selectedVoting: RefLike<VotingDetails | null>
  selectedVoteOptionId: RefLike<number | null>
  votingStatusFilter: RefLike<string>
  votingForm: VotingFormState
  votingOptionsText: RefLike<string>
  votingDurationMinutes: RefLike<number>
  now: RefLike<number>
  loadVotings: () => Promise<void>
  setMessage: (text: string, kind?: MessageKind) => void
}

export function useVotingManagement(options: UseVotingManagementOptions) {
  const {
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
  } = options

  const visibleVotings = computed(() => {
    const filtered = votingStatusFilter.value === 'ALL'
      ? votings.value
      : votings.value.filter((voting) => voting.status === votingStatusFilter.value)
    if (session.role !== 'STUDENT') return filtered
    const myClass = users.value.find((user) => user.id === session.userId)?.className
    return filtered.filter((voting) => !myClass || voting.schoolClass?.name === myClass)
  })

  const currentVotingLocked = computed(() => Boolean(selectedVoting.value?.hasVoted || selectedVoting.value?.voting.status !== 'ACTIVE'))

  function votingRemainingLabel(voting: SecretVoting) {
    if (voting.status !== 'ACTIVE' || !voting.endsAt) return '—'
    const remainingMs = new Date(voting.endsAt).getTime() - now.value
    if (remainingMs <= 0) return '—'
    const minutes = Math.floor(remainingMs / 60000)
    const seconds = Math.floor((remainingMs % 60000) / 1000)
    return `${minutes}:${String(seconds).padStart(2, '0')}`
  }

  async function createVoting() {
    try {
      const options = votingOptionsText.value
        .split('\n')
        .map((label) => label.trim())
        .filter(Boolean)
        .map((label) => ({ label }))
      await api<SecretVoting>('/vote/secret', {
        method: 'POST',
        body: JSON.stringify({
          ...votingForm,
          endsAt: toLocalDateTimeInputValue(new Date(Date.now() + votingDurationMinutes.value * 60000)),
          options
        })
      })
      await loadVotings()
      setMessage('Голосование создано', 'success')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Ошибка создания голосования', 'error')
    }
  }

  async function openVoting(id: number) {
    selectedVoting.value = await api<VotingDetails>(`/vote/secret/${id}`)
    selectedVoteOptionId.value = null
  }

  async function submitVote(optionId: number) {
    if (!selectedVoting.value || !session.userId) return
    try {
      await api(`/vote/secret/${selectedVoting.value.voting.id}/votes/me`, {
        method: 'POST',
        body: JSON.stringify({ optionId })
      })
      await openVoting(selectedVoting.value.voting.id)
      setMessage('Голос принят', 'success')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Ошибка голосования', 'error')
    }
  }

  async function submitSelectedVote() {
    if (!selectedVoteOptionId.value || currentVotingLocked.value) return
    await submitVote(selectedVoteOptionId.value)
  }

  async function finishVoting(id: number) {
    await api<SecretVoting>(`/vote/secret/${id}/finish`, { method: 'POST' })
    await loadVotings()
    await openVoting(id)
    setMessage('Голосование завершено', 'success')
  }

  return {
    visibleVotings,
    currentVotingLocked,
    votingRemainingLabel,
    createVoting,
    openVoting,
    submitVote,
    submitSelectedVote,
    finishVoting
  }
}
