export function formatNumber(value: number) {
  return value.toLocaleString('ru-RU')
}

export function formatDateTime(value?: string | null) {
  if (!value) return '-'
  return parseServerDateTime(value).toLocaleString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

export function totalVotes(results: Record<string, number>) {
  return Object.values(results).reduce((sum, count) => sum + count, 0)
}

export function parseServerDateTime(value: string) {
  return new Date(/[zZ]|[+-]\d\d:\d\d$/.test(value) ? value : `${value}Z`)
}

export function toLocalDateTimeInputValue(date: Date) {
  const offset = date.getTimezoneOffset() * 60000
  return new Date(date.getTime() - offset).toISOString().slice(0, 19)
}

export function toServerDateTimeValue(date: Date) {
  return date.toISOString().slice(0, 19)
}
