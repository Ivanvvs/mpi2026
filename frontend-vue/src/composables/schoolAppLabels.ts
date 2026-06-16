import type { ExamStatus, Page, Role } from '../types/domain'

export const menus: Record<Role, Array<{ page: Page; label: string }>> = {
  ADMIN: [
    { page: 'home', label: 'Главная' },
    { page: 'classes', label: 'Классы и рейтинг' },
    { page: 'users', label: 'Пользователи' },
    { page: 'exams', label: 'Экзамены' },
    { page: 'votings', label: 'Голосования' },
    { page: 'violations', label: 'Нарушения' },
    { page: 'privileges', label: 'Заявки на привилегии' }
  ],
  CURATOR: [
    { page: 'home', label: 'Главная' },
    { page: 'my-class', label: 'Мой класс' },
    { page: 'violations', label: 'Нарушения' },
    { page: 'privileges', label: 'Заявки на привилегии' },
    { page: 'votings', label: 'Голосования' }
  ],
  STUDENT: [
    { page: 'home', label: 'Главная' },
    { page: 'exams', label: 'Экзамены' },
    { page: 'votings', label: 'Голосования' },
    { page: 'results', label: 'Мои результаты' }
  ],
  EXAMINER: [
    { page: 'home', label: 'Главная' },
    { page: 'exams', label: 'Экзамены' },
    { page: 'violations', label: 'Нарушения' }
  ]
}

export const userFilters = [
  { value: 'ALL' as const, label: 'Все пользователи' },
  { value: 'STUDENT' as const, label: 'Ученик' },
  { value: 'EXAMINER' as const, label: 'Экзаменатор-модератор' },
  { value: 'CURATOR' as const, label: 'Куратор класса' },
  { value: 'ADMIN' as const, label: 'Администратор' }
]

export function roleLabel(role?: Role) {
  return {
    STUDENT: 'Ученик',
    CURATOR: 'Куратор класса',
    EXAMINER: 'Экзаменатор-модератор',
    ADMIN: 'Администратор'
  }[role || 'STUDENT']
}

export function statusLabel(status: ExamStatus) {
  return {
    PREPARED: 'Подготовлен',
    ACTIVE: 'Активен',
    FINISHED: 'Завершен'
  }[status]
}
