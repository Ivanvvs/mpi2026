<template>
  <header>
    <h1>Classroom of the Elite</h1>
  </header>

  <nav>
    <button @click="showPage('login')">Login</button>
    <button @click="showPage('dashboard')">Dashboard</button>
    <button @click="showPage('exam')">Exam</button>
    <button @click="showPage('violations')">Violations</button>
    <button @click="showPage('voting')">Secret Voting</button>
    <button @click="showPage('planned')">Planned Features</button>
    <button @click="showPage('admin')">Admin / Debug</button>
  </nav>

  <main>
    <!-- LOGIN PAGE -->
    <section v-if="currentPage === 'login'">
      <h2>Авторизация</h2>
      <p>
        Вход в систему.
      </p>

      <input v-model="loginUsername" type="text" placeholder="Username, например student" />
      <input v-model="loginPassword" type="password" placeholder="Password, например 1234" />

      <button @click="login">Login</button>
      <button @click="logout">Logout</button>

      <p>{{ loginStatus }}</p>

      <div class="hint">
        <p>Тестовые пользователи:</p>
        <pre>student / 1234
examiner / 1234
admin / 1234</pre>
      </div>
    </section>

    <!-- DASHBOARD PAGE -->
    <section v-if="currentPage === 'dashboard'">
      <h2>Dashboard</h2>
      <p>Главный экран после входа.</p>

      <div class="cards">
        <div class="card">
          <h3>Create and Take Exam</h3>
          <p>Создание, запуск, прохождение и завершение экзамена.</p>
          <button @click="showPage('exam')">Open Exam</button>
        </div>

        <div class="card">
          <h3>Violation Detected</h3>
          <p>Фиксация нарушения студента во время экзамена.</p>
          <button @click="showPage('violations')">Open Violations</button>
        </div>

        <div class="card">
          <h3>Conduct Secret Voting</h3>
          <p>Отправка анонимного голоса и просмотр результатов.</p>
          <button @click="showPage('voting')">Open Voting</button>
        </div>
      </div>
    </section>

    <!-- EXAM PAGE -->
    <section v-if="currentPage === 'exam'">
      <h2>Create and Take Exam</h2>

      <h3>Создать экзамен</h3>
      <input v-model="examTitle" placeholder="Название экзамена" />
      <button @click="createExam">Создать экзамен</button>

      <h3>Управление экзаменом</h3>
      <input v-model.number="sessionId" type="number" placeholder="Введите ID созданного экзамена" />
      <button @click="getExam">Открыть экзамен по ID</button>
      <button @click="startExam">Запустить экзамен</button>
      <button @click="endExam">Завершить экзамен</button>

      <h3>Ответ студента</h3>
      <input v-model.number="answerSessionId" type="number" placeholder="ID экзамена" />
      <input v-model.number="answerUserId" type="number" placeholder="ID студента" />
      <input v-model.number="questionId" type="number" placeholder="ID вопроса, например 1" />
      <textarea v-model="answerText" placeholder="Ответ студента"></textarea>
      <button @click="sendAnswer">Отправить ответ</button>
      <button @click="getAnswers">Показать ответы</button>
    </section>

    <!-- VIOLATIONS PAGE -->
    <section v-if="currentPage === 'violations'">
      <h2>Violation Detected</h2>

      <input v-model.number="violationSessionId" type="number" placeholder="ID экзамена" />
      <input v-model.number="violationUserId" type="number" placeholder="ID студента" />
      <textarea v-model="violationDescription" placeholder="Описание нарушения"></textarea>
      <input v-model="evidencePath" placeholder="Путь к доказательству, например screenshot.png" />

      <button @click="reportViolation">Зафиксировать нарушение</button>
      <button @click="getViolations">Показать нарушения</button>

      <div class="hint">
      </div>
    </section>

    <!-- VOTING PAGE -->
    <section v-if="currentPage === 'voting'">
      <h2>Conduct Secret Voting</h2>
      <p>
        Тайное голосование: отправка голоса, запрет повторного голосования
        и просмотр результатов.
      </p>

      <input v-model.number="voteSessionId" type="number" placeholder="ID голосования/сессии" />
      <input v-model="encryptedValue" placeholder="Голос, например EXCLUDE или KEEP" />

      <button @click="sendVote">Отправить голос</button>
      <button @click="getVoteResults">Показать результаты</button>
    </section>

    <!-- PLANNED FEATURES PAGE -->
    <section v-if="currentPage === 'planned'">
      <h2>Planned Features</h2>

      <div class="cards">
        <div class="card">
          <h3>S-points</h3>
          <p>Система очков студентов.</p>
          <button @click="showStub('S-points')">Open</button>
        </div>

        <div class="card">
          <h3>Class Rating</h3>
          <p>Рейтинг классов по результатам экзаменов.</p>
          <button @click="showStub('Class Rating')">Open</button>
        </div>

        <div class="card">
          <h3>Privilege Requests</h3>
          <p>Заявки на привилегии.</p>
          <button @click="showStub('Privilege Requests')">Open</button>
        </div>

        <div class="card">
          <h3>Student Expulsion</h3>
          <p>Исключение студента.</p>
          <button @click="showStub('Student Expulsion')">Open</button>
        </div>

        <div class="card">
          <h3>Rank Update</h3>
          <p>Обновление рангов студентов.</p>
          <button @click="showStub('Rank Update')">Open</button>
        </div>
      </div>
    </section>

    <!-- ADMIN / DEBUG PAGE -->
    <section v-if="currentPage === 'admin'">
      <h2>Admin / Debug</h2>

      <h3>Создать пользователя</h3>
      <input v-model="newUsername" placeholder="username, например Ayanokoji" />
      <input v-model="newPassword" placeholder="password, например 1234" />
      <select v-model="newRole">
        <option value="STUDENT">STUDENT</option>
        <option value="EXAMINER">EXAMINER</option>
        <option value="CURATOR">CURATOR</option>
        <option value="ADMIN">ADMIN</option>
      </select>

      <button @click="createUser">Создать пользователя</button>
      <button @click="getUsers">Показать пользователей</button>

      <h3>Role test</h3>
      <button @click="testStudent">Test student endpoint</button>
      <button @click="testExaminer">Test examiner endpoint</button>
      <button @click="testAdmin">Test admin endpoint</button>
      <p>{{ roleTestResult }}</p>
    </section>

    <!-- RESULT -->
    <section>
      <h2>Ответ сервера</h2>
      <pre>{{ formattedResult }}</pre>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

const API_URL = 'http://localhost:8080'

type Page =
  | 'login'
  | 'dashboard'
  | 'exam'
  | 'violations'
  | 'voting'
  | 'planned'
  | 'admin'

type Role = 'STUDENT' | 'EXAMINER' | 'CURATOR' | 'ADMIN'

interface LoginResponse {
  token: string
  role: Role
}

interface ServerResult {
  status?: number
  ok?: boolean
  data?: unknown
  message?: string
  error?: boolean
  feature?: string
  username?: string
  role?: Role
}

const currentPage = ref<Page>('login')
const result = ref<ServerResult | Record<string, unknown>>({
  message: 'Здесь появится ответ backend...'
})

const loginUsername = ref('')
const loginPassword = ref('')
const loginStatus = ref('Not logged in')

const newUsername = ref('')
const newPassword = ref('')
const newRole = ref<Role>('STUDENT')

const examTitle = ref('')
const sessionId = ref<number | null>(null)

const answerSessionId = ref<number | null>(null)
const answerUserId = ref<number | null>(null)
const questionId = ref<number | null>(null)
const answerText = ref('')

const violationSessionId = ref<number | null>(null)
const violationUserId = ref<number | null>(null)
const violationDescription = ref('')
const evidencePath = ref('')

const voteSessionId = ref<number | null>(null)
const encryptedValue = ref('')

const roleTestResult = ref('')

function showPage(page: Page) {
  currentPage.value = page
}

function getAuthHeaders(): HeadersInit {
  const token = localStorage.getItem('token')

  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`
  }
}

function getJsonHeaders(): HeadersInit {
  return {
    'Content-Type': 'application/json'
  }
}

function sanitizeResponse(data: unknown): unknown {
  const copy = JSON.parse(JSON.stringify(data))

  if (copy?.data?.encryptedValue) {
    copy.data.encryptedValue = '[hidden]'
  }

  if (copy?.encryptedValue) {
    copy.encryptedValue = '[hidden]'
  }

  if (copy?.token) {
    copy.token = '[hidden]'
  }

  if (copy?.data?.token) {
    copy.data.token = '[hidden]'
  }

  return copy
}

function showResult(data: ServerResult | Record<string, unknown>) {
  result.value = sanitizeResponse(data) as ServerResult
}

const formattedResult = computed(() => {
  return JSON.stringify(result.value, null, 2)
})

async function request(path: string, method = 'GET', body: unknown = null) {
  try {
    const options: RequestInit = {
      method,
      headers: getAuthHeaders()
    }

    if (body !== null) {
      options.body = JSON.stringify(body)
    }

    const response = await fetch(API_URL + path, options)
    const text = await response.text()

    let data: unknown

    try {
      data = text ? JSON.parse(text) : {}
    } catch {
      data = text
    }

    if (response.status === 401 || response.status === 403) {
      showResult({
        status: response.status,
        ok: false,
        message: 'Access denied. Сначала войдите в систему или проверьте роль пользователя.'
      })
      return
    }

    showResult({
      status: response.status,
      ok: response.ok,
      data
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unknown error'

    showResult({
      error: true,
      message
    })
  }
}

/* AUTH */

async function login() {
  try {
    const response = await fetch(`${API_URL}/auth/login`, {
      method: 'POST',
      headers: getJsonHeaders(),
      body: JSON.stringify({
        username: loginUsername.value,
        password: loginPassword.value
      })
    })

    if (!response.ok) {
      throw new Error('Login failed')
    }

    const data = (await response.json()) as LoginResponse

    localStorage.setItem('token', data.token)
    localStorage.setItem('role', data.role)
    localStorage.setItem('loginUsername', loginUsername.value)

    loginStatus.value = `Logged in as ${loginUsername.value} (${data.role})`

    showResult({
      message: 'Login successful',
      username: loginUsername.value,
      role: data.role
    })

    showPage('dashboard')
  } catch {
    loginStatus.value = 'Login failed. Check username/password.'

    showResult({
      error: true,
      message: 'Login failed. Check username/password.'
    })
  }
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  localStorage.removeItem('loginUsername')

  loginStatus.value = 'Not logged in'

  showResult({
    message: 'Logged out'
  })

  showPage('login')
}

function updateLoginStatus() {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')
  const username = localStorage.getItem('loginUsername')

  if (token && role && username) {
    loginStatus.value = `Logged in as ${username} (${role})`
  } else {
    loginStatus.value = 'Not logged in'
  }
}

/* ROLE TEST */

async function testStudent() {
  await testRoleEndpoint('/student/test')
}

async function testExaminer() {
  await testRoleEndpoint('/examiner/test')
}

async function testAdmin() {
  await testRoleEndpoint('/admin/test')
}

async function testRoleEndpoint(path: string) {
  try {
    const response = await fetch(`${API_URL}${path}`, {
      method: 'GET',
      headers: getAuthHeaders()
    })

    const text = await response.text()

    if (!response.ok) {
      roleTestResult.value = `Access denied or error: ${response.status}`

      showResult({
        status: response.status,
        ok: response.ok,
        message: text || 'Access denied'
      })

      return
    }

    roleTestResult.value = text

    showResult({
      status: response.status,
      ok: response.ok,
      data: text
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unknown error'

    roleTestResult.value = 'Request failed. Check backend connection.'

    showResult({
      error: true,
      message
    })
  }
}

/* USERS */

function createUser() {
  const body = {
    username: newUsername.value,
    password: newPassword.value,
    role: newRole.value
  }

  request('/users/create', 'POST', body)
}

function getUsers() {
  request('/users', 'GET')
}

/* EXAM SESSION */

function createExam() {
  const body = {
    title: examTitle.value,
    active: false
  }

  request('/exam/session/create', 'POST', body)
}

function getExam() {
  request(`/exam/session/${sessionId.value}`, 'GET')
}

function startExam() {
  request(`/exam/session/start/${sessionId.value}`, 'POST')
}

function endExam() {
  request(`/exam/session/end/${sessionId.value}`, 'POST')
}

/* ANSWERS */

function sendAnswer() {
  const body = {
    sessionId: answerSessionId.value,
    userId: answerUserId.value,
    questionId: questionId.value,
    text: answerText.value
  }

  request(`/exam/${answerSessionId.value}/answer?userId=${answerUserId.value}`, 'POST', body)
}

function getAnswers() {
  request(`/exam/${answerSessionId.value}/answers`, 'GET')
}

/* VIOLATIONS */

function reportViolation() {
  const body = {
    sessionId: violationSessionId.value,
    userId: violationUserId.value,
    description: violationDescription.value,
    evidencePath: evidencePath.value
  }

  request('/violations/report', 'POST', body)
}

function getViolations() {
  request(`/violations/session/${violationSessionId.value}`, 'GET')
}

/* VOTING */

function sendVote() {
  const body = {
    sessionId: voteSessionId.value,
    encryptedValue: encryptedValue.value
  }

  request(`/vote/${voteSessionId.value}`, 'POST', body)
}

function getVoteResults() {
  request(`/vote/${voteSessionId.value}/results`, 'GET')
}

/* STUBS */

function showStub(featureName: string) {
  showResult({
    status: 501,
    ok: false,
    feature: featureName,
    message: 'Not implemented. This feature is planned for the next iteration.'
  })
}

/* INITIALIZATION */

onMounted(() => {
  updateLoginStatus()

  const token = localStorage.getItem('token')

  if (token) {
    showPage('dashboard')
  } else {
    showPage('login')
  }
})
</script>