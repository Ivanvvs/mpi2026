import type { Ref } from 'vue'
import type { Role, SchoolClass, UserResponse } from '../types/domain'

type ApiClient = <T>(path: string, options?: RequestInit) => Promise<T>
type MessageKind = 'info' | 'success' | 'error'

interface RegisterFormState {
  fullName: string
  username: string
  email: string
  password: string
  role: Role
  classId: number | null
  passportData: string
  entranceExamScore: number | null
  contactInfo: string
  birthDate: string
}

interface UseUserManagementOptions {
  api: ApiClient
  registerForm: RegisterFormState
  confirmPassword: Ref<string>
  editingUserId: Ref<number | null>
  expulsionCandidate: Ref<UserResponse | null>
  classes: Ref<SchoolClass[]>
  loading: Ref<boolean>
  userSaveSuccess: Ref<boolean>
  loadUsers: () => Promise<void>
  setMessage: (text: string, kind?: MessageKind) => void
}

export function useUserManagement(options: UseUserManagementOptions) {
  const {
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
  } = options

  let successTimer: ReturnType<typeof window.setTimeout> | null = null

  function generatePassword() {
    const password = Math.random().toString(36).slice(2, 10)
    registerForm.password = password
    confirmPassword.value = password
  }

  function resetRegisterForm() {
    Object.assign(registerForm, {
      fullName: '',
      username: '',
      email: '',
      password: '',
      role: 'STUDENT',
      classId: null,
      passportData: '',
      entranceExamScore: null,
      contactInfo: '',
      birthDate: ''
    })
    confirmPassword.value = ''
    editingUserId.value = null
    expulsionCandidate.value = null
    userSaveSuccess.value = false
  }

  async function registerUser() {
    loading.value = true
    userSaveSuccess.value = false
    try {
      if (!editingUserId.value && registerForm.password !== confirmPassword.value) {
        throw new Error('Пароли не совпадают')
      }

      if ((registerForm.role === 'STUDENT' || registerForm.role === 'CURATOR') && !registerForm.classId) {
        throw new Error('Для ученика или куратора обязательно выберите класс')
      }

      const body = {
        ...registerForm,
        classId: registerForm.role === 'STUDENT' || registerForm.role === 'CURATOR' ? registerForm.classId : null,
        entranceExamScore: registerForm.role === 'STUDENT' ? registerForm.entranceExamScore : null,
        birthDate: registerForm.birthDate || null
      }

      const wasEditing = editingUserId.value !== null
      if (editingUserId.value) {
        await api<UserResponse>(`/users/${editingUserId.value}`, {
          method: 'PUT',
          body: JSON.stringify({
            fullName: body.fullName,
            username: body.username,
            email: body.email,
            role: body.role,
            classId: body.classId,
            passportData: body.passportData,
            entranceExamScore: body.entranceExamScore,
            contactInfo: body.contactInfo,
            birthDate: body.birthDate,
            active: true
          })
        })
      } else {
        await api<UserResponse>('/users/register', { method: 'POST', body: JSON.stringify(body) })
      }

      await loadUsers()
      resetRegisterForm()
      setMessage(wasEditing ? 'Пользователь обновлен' : 'Пользователь создан', 'success')
      showSaveSuccess()
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Ошибка регистрации', 'error')
    } finally {
      loading.value = false
    }
  }

  function editUser(user: UserResponse) {
    editingUserId.value = user.id
    expulsionCandidate.value = null
    Object.assign(registerForm, {
      fullName: user.fullName,
      username: user.username,
      email: user.email,
      password: '',
      role: user.role,
      classId: user.classId || classes.value.find((schoolClass) => schoolClass.name === user.className)?.id || null,
      passportData: user.passportData || '',
      entranceExamScore: user.entranceExamScore ?? null,
      contactInfo: user.contactInfo || '',
      birthDate: user.birthDate || ''
    })
    confirmPassword.value = ''
    userSaveSuccess.value = false
  }

  function showSaveSuccess() {
    if (successTimer) {
      window.clearTimeout(successTimer)
    }
    userSaveSuccess.value = true
    successTimer = window.setTimeout(() => {
      userSaveSuccess.value = false
      successTimer = null
    }, 2500)
  }

  function openExpulsionPlaceholder(user: UserResponse) {
    if (user.role !== 'STUDENT') return
    editingUserId.value = null
    expulsionCandidate.value = user
  }

  function closeExpulsionPlaceholder() {
    expulsionCandidate.value = null
  }

  async function deactivateUser(user: UserResponse) {
    const confirmed = window.confirm(`Исключить/деактивировать пользователя "${user.fullName}"?`)
    if (!confirmed) return

    try {
      await api<UserResponse>(`/users/${user.id}`, { method: 'DELETE' })
      await loadUsers()
      setMessage('Пользователь деактивирован', 'success')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Ошибка деактивации пользователя', 'error')
    }
  }

  return {
    generatePassword,
    resetRegisterForm,
    registerUser,
    editUser,
    openExpulsionPlaceholder,
    closeExpulsionPlaceholder,
    deactivateUser
  }
}
