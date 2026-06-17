import { inject, provide } from 'vue'
import type { SchoolAppContext } from './useSchoolApp'

const schoolAppKey = Symbol('school-app')

export function provideSchoolApp(app: SchoolAppContext) {
  provide(schoolAppKey, app)
}

export function useSchoolAppContext() {
  const app = inject<SchoolAppContext>(schoolAppKey)
  if (!app) {
    throw new Error('School app context was not provided')
  }
  return app
}
