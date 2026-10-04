import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { WS_URL } from './api'

export interface ExamRealtimeMessage {
  type: string
  userId: number | null
}

interface DeactivatableClient {
  deactivate: () => void | Promise<void>
}

interface ExamRealtimeHandlers {
  onConnected: () => void
  onMessage: (message: ExamRealtimeMessage) => void | Promise<void>
  onError: () => void
}

export function connectExamRealtime(examId: number, handlers: ExamRealtimeHandlers) {
  const client = new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    reconnectDelay: 3000,
    onConnect: () => {
      handlers.onConnected()
      client.subscribe(`/topic/exams/${examId}`, (frame) => {
        const payload = JSON.parse(frame.body) as Partial<ExamRealtimeMessage>
        void handlers.onMessage({
          type: payload.type || '',
          userId: typeof payload.userId === 'number' ? payload.userId : null
        })
      })
    },
    onWebSocketError: handlers.onError
  })
  client.activate()
  return client
}

export function disconnectExamRealtime(client: DeactivatableClient | null) {
  client?.deactivate()
}
