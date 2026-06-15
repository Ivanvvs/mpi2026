<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand">SCHOOL SYSTEM</div>
      <div v-if="session.token" class="topbar-user">
        <span>{{ topbarLabel }}</span>
        <button class="link-button" @click="logout">Выход</button>
      </div>
      <div v-else class="topbar-user">
        <span>Вход</span>
      </div>
    </header>

    <main v-if="!session.token" class="login-page">
      <section class="login-panel">
        <h1>Вход в систему</h1>
        <form class="form-stack" @submit.prevent="login">
          <label>
            <span>Логин</span>
            <input v-model.trim="loginForm.username" autocomplete="username" placeholder="student, curator, examiner, admin" />
          </label>
          <label>
            <span>Пароль</span>
            <input v-model="loginForm.password" type="password" autocomplete="current-password" placeholder="1234" />
          </label>
          <div class="inline-row split">
            <label class="check-row">
              <input v-model="rememberMe" type="checkbox" />
              <span>Запомнить меня</span>
            </label>
            <span class="muted">demo: 1234</span>
          </div>
          <button class="primary wide" type="submit" :disabled="loading">Войти</button>
          <p v-if="message.text" :class="['message', message.kind]">{{ message.text }}</p>
        </form>
      </section>
    </main>

    <div v-else class="workspace">
      <aside class="sidebar">
        <button
          v-for="item in currentMenu"
          :key="item.page"
          :class="{ active: currentPage === item.page }"
          @click="openPage(item.page)"
        >
          {{ item.label }}
        </button>

      </aside>

      <section class="content">
        <div class="page-head">
          <div>
            <h1>{{ pageTitle }}</h1>
            <p>{{ pageSubtitle }}</p>
          </div>
          <button class="secondary" @click="refreshCurrent">Обновить</button>
        </div>

        <section v-if="currentPage === 'home' && session.role === 'ADMIN'" class="admin-home">
          <div class="admin-metrics">
            <article class="metric compact-metric">
              <span>Баланс S-очков</span>
              <strong>{{ totalSPoints }}</strong>
            </article>
            <article class="metric compact-metric">
              <span>Классов</span>
              <strong>{{ classes.length }}</strong>
            </article>
            <article class="metric compact-metric">
              <span>Учеников</span>
              <strong>{{ studentCount }}</strong>
            </article>
            <article class="metric compact-metric">
              <span>Активные экзамены</span>
              <strong>{{ activeExams.length }}</strong>
            </article>
          </div>

          <div class="admin-main-grid">
            <section>
              <h2>Рейтинг классов</h2>
              <table class="ranking-table">
                <thead>
                  <tr>
                    <th>Место</th>
                    <th>Класс</th>
                    <th>S-очки</th>
                    <th>Изменение</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(schoolClass, index) in rankedClasses" :key="schoolClass.name">
                    <td>{{ index + 1 }}</td>
                    <td>{{ schoolClass.name }}</td>
                    <td>{{ formatNumber(schoolClass.sPoints) }}</td>
                    <td :class="schoolClass.delta >= 0 ? 'positive' : 'negative'">
                      {{ schoolClass.delta >= 0 ? '+' : '' }}{{ schoolClass.delta }}
                    </td>
                  </tr>
                </tbody>
              </table>
              <button class="secondary wide-action">Обновить ранги</button>
            </section>

            <aside class="admin-side">
              <div class="panel slim-panel">
                <h2>Недавние действия</h2>
                <ul class="recent-list">
                  <li v-for="item in recentActions" :key="item">{{ item }}</li>
                  <li v-if="!recentActions.length" class="muted">Данных пока нет</li>
                </ul>
              </div>
              <div class="panel slim-panel">
                <h2>Быстрые действия</h2>
                <div class="quick-actions">
                  <button class="secondary" @click="openPage('users')">+ Добавить ученика</button>
                  <button class="secondary" @click="openPage('exams')">+ Создать экзамен</button>
                  <button class="secondary" @click="openPage('votings')">+ Голосование</button>
                  <button class="secondary" @click="openPage('violations')">Журнал нарушений</button>
                </div>
              </div>
            </aside>
          </div>
        </section>

        <section v-if="currentPage === 'home' && session.role === 'STUDENT'" class="student-home">
          <article class="student-stat-card">
            <div class="student-icon money">S</div>
            <span>Баланс S-очков</span>
            <strong class="green">{{ studentClassPoints }}</strong>
          </article>
          <article class="student-stat-card">
            <div class="student-icon exams">A</div>
            <span>Пройдено экзаменов</span>
            <strong>{{ myResults.length }}</strong>
          </article>
          <article class="student-stat-card">
            <div class="student-icon rank">{{ studentClassRank }}</div>
            <span>Класс</span>
            <strong class="orange">{{ studentClassRank }}</strong>
          </article>
        </section>

        <section v-if="currentPage === 'home' && session.role === 'CURATOR'" class="curator-home">
          <div class="curator-top">
            <section class="panel balance-panel">
              <h2>Класс: {{ curatorClassName }}</h2>
              <p>Баланс S-очков: <strong class="green">{{ curatorClassPoints }}</strong></p>
            </section>
            <section class="panel quick-panel">
              <h2>Быстрые действия</h2>
              <div class="inline-actions">
                <button class="secondary" @click="openPage('violations')">Зафиксировать нарушение</button>
                <button class="secondary" @click="openPage('privileges')">Подать заявку на привилегию</button>
              </div>
            </section>
          </div>

          <div class="curator-grid">
            <section>
              <h2>Журнал нарушений</h2>
              <table>
                <thead>
                  <tr>
                    <th>Дата</th>
                    <th>Ученик</th>
                    <th>Описание</th>
                    <th>Экзамен</th>
                    <th>Баллы</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="violation in curatorViolations" :key="violation.date + violation.student">
                    <td>{{ violation.date }}</td>
                    <td>{{ violation.student }}</td>
                    <td>{{ violation.description }}</td>
                    <td>{{ violation.exam }}</td>
                    <td class="negative">{{ violation.points }}</td>
                  </tr>
                  <tr v-if="!curatorViolations.length">
                    <td colspan="5">Данных пока нет</td>
                  </tr>
                </tbody>
              </table>
              <button class="secondary wide-action" @click="openPage('violations')">Все нарушения</button>
            </section>

            <section>
              <h2>Заявки на привилегии</h2>
              <table>
                <thead>
                  <tr>
                    <th>Дата</th>
                    <th>Привилегия</th>
                    <th>Статус</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="request in curatorPrivilegeRequests" :key="request.date + request.title">
                    <td>{{ request.date }}</td>
                    <td>{{ request.title }}</td>
                    <td :class="request.kind">{{ request.status }}</td>
                  </tr>
                  <tr v-if="!curatorPrivilegeRequests.length">
                    <td colspan="3">Данных пока нет</td>
                  </tr>
                </tbody>
              </table>
              <button class="secondary wide-action" @click="openPage('privileges')">Все заявки</button>
            </section>
          </div>
        </section>

        <section v-if="currentPage === 'home' && session.role === 'EXAMINER'" class="examiner-home panel">
          <h2>Предстоящие экзамены</h2>
          <table>
            <thead>
              <tr>
                <th>Дата</th>
                <th>Время</th>
                <th>Экзамен</th>
                <th>Предмет</th>
                <th>Класс(ы)</th>
                <th>Количество участников</th>
                <th>Статус</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in examinerHomeRows" :key="row.id">
                <td>{{ row.date }}</td>
                <td>{{ row.time }}</td>
                <td>{{ row.exam }}</td>
                <td>{{ row.subject }}</td>
                <td>{{ row.classes }}</td>
                <td>{{ row.participants }}</td>
                <td><span class="outline-badge">{{ row.status }}</span></td>
              </tr>
              <tr v-if="!examinerHomeRows.length">
                <td colspan="7">Данных пока нет</td>
              </tr>
            </tbody>
          </table>
          <button class="secondary wide-action" @click="openPage('exams')">Все экзамены</button>
        </section>

        <section v-if="currentPage === 'users'" class="stack">
          <div class="panel">
            <h2>Создание нового пользователя</h2>
            <form class="two-column-form" @submit.prevent="registerUser">
              <label>
                <span>ФИО</span>
                <input v-model.trim="registerForm.fullName" placeholder="Введите ФИО полностью" required />
              </label>
              <label>
                <span>Email</span>
                <input v-model.trim="registerForm.email" type="email" placeholder="Введите email" required />
              </label>
              <label>
                <span>Роль</span>
                <select v-model="registerForm.role">
                  <option value="STUDENT">Ученик</option>
                  <option value="EXAMINER">Экзаменатор-модератор</option>
                  <option value="CURATOR">Куратор класса</option>
                  <option value="ADMIN">Администратор</option>
                </select>
              </label>
              <label>
                <span>Логин</span>
                <input v-model.trim="registerForm.username" placeholder="Введите логин" required />
              </label>
              <label>
                <span>Класс</span>
                <select v-model.number="registerForm.classId" :disabled="registerForm.role !== 'STUDENT'">
                  <option :value="null">Выберите класс</option>
                  <option v-for="schoolClass in classes" :key="schoolClass.id" :value="schoolClass.id">
                    {{ schoolClass.name }}
                  </option>
                </select>
              </label>
              <label>
                <span>Пароль</span>
                <input v-model="registerForm.password" placeholder="Введите пароль" required />
              </label>
              <label>
                <span>Дата рождения</span>
                <input v-model="registerForm.birthDate" type="date" />
              </label>
              <label>
                <span>Контакты</span>
                <input v-model.trim="registerForm.contactInfo" placeholder="Телефон или email" />
              </label>
              <label>
                <span>Паспортные данные</span>
                <input v-model.trim="registerForm.passportData" placeholder="Серия и номер" />
              </label>
              <label>
                <span>Вступительный балл</span>
                <input v-model.number="registerForm.entranceExamScore" type="number" min="0" :disabled="registerForm.role !== 'STUDENT'" />
              </label>
              <div class="actions">
                <button class="primary" type="submit" :disabled="loading">Создать пользователя</button>
                <button class="secondary" type="button" @click="resetRegisterForm">Отмена</button>
              </div>
            </form>
          </div>

          <div class="panel">
            <h2>Список пользователей</h2>
            <div class="tabs">
              <button
                v-for="filter in userFilters"
                :key="filter.value"
                :class="{ active: userRoleFilter === filter.value }"
                @click="userRoleFilter = filter.value"
              >
                {{ filter.label }}
              </button>
            </div>
            <table>
              <thead>
                <tr>
                  <th>ФИО</th>
                  <th>Роль</th>
                  <th>Класс</th>
                  <th>Логин</th>
                  <th>Email</th>
                  <th>Статус</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="user in filteredUsers" :key="user.id">
                  <td>{{ user.fullName }}</td>
                  <td>{{ roleLabel(user.role) }}</td>
                  <td>{{ user.className || '-' }}</td>
                  <td>{{ user.username }}</td>
                  <td>{{ user.email }}</td>
                  <td><span class="status ok"></span>{{ user.active ? 'Активен' : 'Отключен' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section v-if="currentPage === 'exams'" class="stack">
          <div v-if="session.role === 'EXAMINER'" class="panel">
            <h2>Создание экзамена</h2>
            <form class="two-column-form" @submit.prevent="createExam">
              <label>
                <span>Название</span>
                <input v-model.trim="examForm.title" placeholder="Например: Математика" required />
              </label>
              <label>
                <span>Предмет</span>
                <input v-model.trim="examForm.subject" placeholder="Предмет" required />
              </label>
              <label>
                <span>Класс</span>
                <select v-model.number="examForm.classId" required>
                  <option :value="null">Выберите класс</option>
                  <option v-for="schoolClass in classes" :key="schoolClass.id" :value="schoolClass.id">
                    {{ schoolClass.name }}
                  </option>
                </select>
              </label>
              <label>
                <span>Длительность, мин</span>
                <input v-model.number="examForm.durationMinutes" type="number" min="1" />
              </label>
              <label class="full">
                <span>Описание</span>
                <textarea v-model.trim="examForm.description" placeholder="Краткое описание экзамена"></textarea>
              </label>
              <label class="full">
                <span>Вопросы</span>
                <textarea v-model="examQuestionsText" placeholder="Один вопрос на строку"></textarea>
              </label>
              <div class="actions">
                <button class="primary" type="submit" :disabled="loading">Создать экзамен</button>
              </div>
            </form>
          </div>

          <div class="panel">
            <h2>{{ session.role === 'STUDENT' ? 'Доступные экзамены' : 'Экзамены' }}</h2>
            <table>
              <thead>
                <tr>
                  <th>Название</th>
                  <th>Класс</th>
                  <th>Статус</th>
                  <th>Вопросы</th>
                  <th>Действия</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="exam in visibleExams" :key="exam.id">
                  <td>{{ exam.title }}</td>
                  <td>{{ exam.schoolClass?.name || '-' }}</td>
                  <td><span :class="['badge', exam.status.toLowerCase()]">{{ statusLabel(exam.status) }}</span></td>
                  <td>{{ exam.totalQuestions || 0 }}</td>
                  <td class="table-actions">
                    <button class="secondary compact" @click="openExam(exam.id)">Открыть</button>
                    <button v-if="session.role === 'EXAMINER' && exam.status === 'PREPARED'" class="primary compact" @click="startExam(exam.id)">Запустить</button>
                    <button v-if="session.role === 'EXAMINER' && exam.status === 'ACTIVE'" class="danger compact" @click="finishExam(exam.id)">Завершить</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="selectedExam" class="panel">
            <div class="inline-row split">
              <h2>{{ selectedExam.exam.title }}</h2>
              <span :class="['badge', selectedExam.exam.status.toLowerCase()]">{{ statusLabel(selectedExam.exam.status) }}</span>
            </div>

            <div v-if="session.role === 'EXAMINER'" class="grid two">
              <section>
                <h3>Мониторинг</h3>
                <table>
                  <thead>
                    <tr>
                      <th>Событие</th>
                      <th>Время</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="event in realtimeEvents" :key="event.id">
                      <td>{{ event.type }}</td>
                      <td>{{ event.time }}</td>
                    </tr>
                    <tr v-if="!realtimeEvents.length">
                      <td colspan="2">Событий пока нет</td>
                    </tr>
                  </tbody>
                </table>
                <button class="secondary" @click="connectExamSocket(selectedExam.exam.id)">Подключить WebSocket</button>
              </section>

              <section>
                <h3>Итоговые баллы</h3>
                <div class="score-list">
                  <label v-for="student in classStudentsForSelectedExam" :key="student.id">
                    <span>{{ student.fullName }}</span>
                    <input v-model.number="gradeForm[student.id]" type="number" min="0" />
                  </label>
                </div>
                <button class="primary" :disabled="selectedExam.exam.status !== 'FINISHED'" @click="gradeExam">Сохранить баллы</button>
              </section>
            </div>

            <div v-if="session.role === 'STUDENT'" class="student-exam">
              <div v-if="selectedExam.exam.status !== 'ACTIVE'" class="empty-state">Экзамен пока недоступен для прохождения.</div>
              <form v-else class="question-list" @submit.prevent="submitAllAnswers">
                <label v-for="question in selectedExam.questions" :key="question.id">
                  <span>{{ question.orderIndex }}. {{ question.text }}</span>
                  <textarea v-model="answerDrafts[question.id]" placeholder="Введите ответ"></textarea>
                </label>
                <button class="primary" type="submit">Сохранить ответы</button>
              </form>
            </div>

            <div class="results-block">
              <h3>Результаты</h3>
              <table>
                <thead>
                  <tr>
                    <th>Ученик</th>
                    <th>Баллы</th>
                    <th>Место</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="result in selectedExam.results" :key="result.id">
                    <td>{{ result.student?.fullName || result.student?.username || result.student?.id }}</td>
                    <td>{{ result.finalScore }}</td>
                    <td>{{ result.rankPlace || '-' }}</td>
                  </tr>
                  <tr v-if="!selectedExam.results.length">
                    <td colspan="3">Результаты еще не выставлены</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>

        <section v-if="currentPage === 'votings'" class="stack">
          <div v-if="session.role === 'CURATOR'" class="panel">
            <h2>Создание тайного голосования</h2>
            <form class="two-column-form" @submit.prevent="createVoting">
              <label>
                <span>Название</span>
                <input v-model.trim="votingForm.title" placeholder="Тема голосования" required />
              </label>
              <label>
                <span>Класс</span>
                <select v-model.number="votingForm.classId" required>
                  <option :value="null">Выберите класс</option>
                  <option v-for="schoolClass in classes" :key="schoolClass.id" :value="schoolClass.id">
                    {{ schoolClass.name }}
                  </option>
                </select>
              </label>
              <label class="full">
                <span>Описание</span>
                <textarea v-model.trim="votingForm.description"></textarea>
              </label>
              <label class="full">
                <span>Варианты</span>
                <textarea v-model="votingOptionsText" placeholder="Один вариант на строку"></textarea>
              </label>
              <div class="actions">
                <button class="primary" type="submit">Создать голосование</button>
              </div>
            </form>
          </div>

          <div class="panel">
            <h2>Голосования</h2>
            <table>
              <thead>
                <tr>
                  <th>Название</th>
                  <th>Класс</th>
                  <th>Статус</th>
                  <th>Действия</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="voting in visibleVotings" :key="voting.id">
                  <td>{{ voting.title }}</td>
                  <td>{{ voting.schoolClass?.name || '-' }}</td>
                  <td><span :class="['badge', voting.status.toLowerCase()]">{{ voting.status === 'ACTIVE' ? 'Активно' : 'Завершено' }}</span></td>
                  <td class="table-actions">
                    <button class="secondary compact" @click="openVoting(voting.id)">Открыть</button>
                    <button v-if="session.role === 'CURATOR' && voting.status === 'ACTIVE'" class="danger compact" @click="finishVoting(voting.id)">Завершить</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="selectedVoting" class="panel">
            <h2>{{ selectedVoting.voting.title }}</h2>
            <p>{{ selectedVoting.voting.description }}</p>
            <div v-if="session.role === 'STUDENT' && selectedVoting.voting.status === 'ACTIVE'" class="option-list">
              <button
                v-for="option in selectedVoting.options"
                :key="option.id"
                class="option-button"
                @click="submitVote(option.id)"
              >
                {{ option.label }}
              </button>
            </div>
            <h3>Итоги</h3>
            <table>
              <thead>
                <tr>
                  <th>Вариант</th>
                  <th>Голоса</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(count, label) in selectedVoting.results" :key="label">
                  <td>{{ label }}</td>
                  <td>{{ count }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section v-if="currentPage === 'results'" class="panel">
          <h2>Мои результаты</h2>
          <table>
            <thead>
              <tr>
                <th>Экзамен</th>
                <th>Баллы</th>
                <th>Место</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="result in myResults" :key="result.id">
                <td>{{ result.session?.title || result.session?.id }}</td>
                <td>{{ result.finalScore }}</td>
                <td>{{ result.rankPlace || '-' }}</td>
              </tr>
              <tr v-if="!myResults.length">
                <td colspan="3">Результатов пока нет</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section v-if="currentPage === 'classes'" class="panel">
          <h2>Классы и рейтинг</h2>
          <table>
            <thead>
              <tr>
                <th>Класс</th>
                <th>Ранг</th>
                <th>S-очки</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="schoolClass in classes" :key="schoolClass.id">
                <td>{{ schoolClass.name }}</td>
                <td>{{ schoolClass.rank }}</td>
                <td>{{ schoolClass.sPoints }}</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section v-if="isStubPage" class="panel empty-state">
          Функционал будет реализован на следующих итерациях
        </section>

        <p v-if="message.text && session.token" :class="['message', message.kind]">{{ message.text }}</p>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { computed, onMounted, reactive, ref } from 'vue'

const API_URL = 'http://localhost:8081'
const WS_URL = `${API_URL}/ws`

type Role = 'STUDENT' | 'CURATOR' | 'EXAMINER' | 'ADMIN'
type ExamStatus = 'PREPARED' | 'ACTIVE' | 'FINISHED'
type VotingStatus = 'ACTIVE' | 'FINISHED'
type Page =
  | 'home'
  | 'classes'
  | 'users'
  | 'exams'
  | 'votings'
  | 'violations'
  | 'privileges'
  | 'settings'
  | 'my-class'
  | 'results'
  | 'profile'
  | 'ratings'

interface LoginResponse {
  token: string
  role: Role
  accountId: number
  userId: number | null
  displayName: string
}

interface SchoolClass {
  id: number
  name: string
  rank: string
  sPoints: number
}

interface UserResponse {
  id: number
  accountId: number
  fullName: string
  username: string
  email: string
  role: Role
  className?: string
  active: boolean
}

interface ExamSession {
  id: number
  title: string
  subject: string
  status: ExamStatus
  schoolClass?: SchoolClass
  totalQuestions?: number
  scheduledStartTime?: string | null
}

interface Question {
  id: number
  orderIndex: number
  text: string
}

interface Answer {
  id: number
  questionId: number
  userId: number
  text: string
}

interface ExamResult {
  id: number
  session?: ExamSession
  student?: UserResponse
  finalScore: number
  rankPlace?: number
}

interface ExamDetails {
  exam: ExamSession
  questions: Question[]
  answers: Answer[]
  results: ExamResult[]
}

interface SecretVoting {
  id: number
  title: string
  description?: string
  schoolClass?: SchoolClass
  status: VotingStatus
}

interface VotingOption {
  id: number
  label: string
}

interface VotingDetails {
  voting: SecretVoting
  options: VotingOption[]
  results: Record<string, number>
}

interface RealtimeEvent {
  id: string
  type: string
  time: string
}

interface RankedClass {
  name: string
  sPoints: number
  delta: number
}

const session = reactive({
  token: '',
  role: 'STUDENT' as Role,
  accountId: null as number | null,
  userId: null as number | null,
  displayName: ''
})

const loginForm = reactive({ username: 'admin', password: '1234' })
const rememberMe = ref(true)
const loading = ref(false)
const currentPage = ref<Page>('home')
const message = reactive({ text: '', kind: 'info' as 'info' | 'success' | 'error' })

const users = ref<UserResponse[]>([])
const classes = ref<SchoolClass[]>([])
const exams = ref<ExamSession[]>([])
const votings = ref<SecretVoting[]>([])
const selectedExam = ref<ExamDetails | null>(null)
const selectedVoting = ref<VotingDetails | null>(null)
const myResults = ref<ExamResult[]>([])
const realtimeEvents = ref<RealtimeEvent[]>([])
const stompClient = ref<Client | null>(null)

const userRoleFilter = ref<Role | 'ALL'>('ALL')
const userFilters = [
  { value: 'ALL' as const, label: 'Все пользователи' },
  { value: 'STUDENT' as const, label: 'Ученик' },
  { value: 'EXAMINER' as const, label: 'Экзаменатор-модератор' },
  { value: 'CURATOR' as const, label: 'Куратор класса' },
  { value: 'ADMIN' as const, label: 'Администратор' }
]

const registerForm = reactive({
  fullName: '',
  username: '',
  email: '',
  password: '',
  role: 'STUDENT' as Role,
  classId: null as number | null,
  passportData: '',
  entranceExamScore: null as number | null,
  contactInfo: '',
  birthDate: ''
})

const examForm = reactive({
  title: '',
  subject: '',
  classId: null as number | null,
  durationMinutes: 45,
  description: ''
})
const examQuestionsText = ref('Вопрос 1\nВопрос 2\nВопрос 3')
const answerDrafts = reactive<Record<number, string>>({})
const gradeForm = reactive<Record<number, number>>({})

const votingForm = reactive({
  title: '',
  description: '',
  classId: null as number | null
})
const votingOptionsText = ref('За\nПротив')

const menus: Record<Role, Array<{ page: Page; label: string }>> = {
  ADMIN: [
    { page: 'home', label: 'Главная' },
    { page: 'classes', label: 'Классы и рейтинг' },
    { page: 'users', label: 'Ученики' },
    { page: 'exams', label: 'Экзамены' },
    { page: 'votings', label: 'Голосования' },
    { page: 'violations', label: 'Нарушения' },
    { page: 'privileges', label: 'Заявки на привилегии' },
    { page: 'settings', label: 'Настройки' }
  ],
  CURATOR: [
    { page: 'home', label: 'Главная' },
    { page: 'my-class', label: 'Мой класс' },
    { page: 'violations', label: 'Нарушения' },
    { page: 'privileges', label: 'Заявки на привилегии' },
    { page: 'exams', label: 'Экзамены' },
    { page: 'votings', label: 'Голосования' }
  ],
  STUDENT: [
    { page: 'home', label: 'Главная' },
    { page: 'exams', label: 'Экзамены' },
    { page: 'votings', label: 'Голосования' },
    { page: 'results', label: 'Мои результаты' },
    { page: 'profile', label: 'Профиль' }
  ],
  EXAMINER: [
    { page: 'home', label: 'Главная' },
    { page: 'exams', label: 'Экзамены' },
    { page: 'users', label: 'Ученики' },
    { page: 'violations', label: 'Нарушения' },
    { page: 'ratings', label: 'Рейтинги' }
  ]
}

const currentMenu = computed(() => menus[session.role] || menus.STUDENT)
const activeExams = computed(() => exams.value.filter((exam) => exam.status === 'ACTIVE'))
const totalSPoints = computed(() => formatNumber(classes.value.reduce((sum, item) => sum + (item.sPoints || 0), 0)))
const studentCount = computed(() => users.value.filter((user) => user.role === 'STUDENT').length)
const filteredUsers = computed(() => userRoleFilter.value === 'ALL' ? users.value : users.value.filter((user) => user.role === userRoleFilter.value))
const visibleExams = computed(() => {
  if (session.role !== 'STUDENT') return exams.value
  const myClass = currentUser.value?.className
  return exams.value.filter((exam) => !myClass || exam.schoolClass?.name === myClass)
})
const visibleVotings = computed(() => {
  if (session.role !== 'STUDENT') return votings.value
  const myClass = currentUser.value?.className
  return votings.value.filter((voting) => !myClass || voting.schoolClass?.name === myClass)
})
const currentUser = computed(() => users.value.find((user) => user.id === session.userId))
const currentUserClass = computed(() => classes.value.find((schoolClass) => schoolClass.name === currentUser.value?.className))
const classStudentsForSelectedExam = computed(() => {
  const className = selectedExam.value?.exam.schoolClass?.name
  return users.value.filter((user) => user.role === 'STUDENT' && (!className || user.className === className))
})
const rankedClasses = computed<RankedClass[]>(() => {
  return classes.value
    .map((schoolClass) => ({
      name: schoolClass.name,
      sPoints: schoolClass.sPoints,
      delta: 0
    }))
    .sort((left, right) => right.sPoints - left.sPoints)
    .slice(0, 5)
})
const studentClassPoints = computed(() => currentUserClass.value ? formatNumber(currentUserClass.value.sPoints) : '-')
const studentClassRank = computed(() => currentUserClass.value?.rank || '-')
const curatorClassName = computed(() => currentUser.value?.className || '-')
const curatorClassPoints = computed(() => currentUserClass.value ? formatNumber(currentUserClass.value.sPoints) : '-')
const topbarLabel = computed(() => {
  if (session.role === 'CURATOR') return currentUser.value?.className ? `Куратор ${currentUser.value.className}` : 'Куратор'
  if (session.role === 'STUDENT') return 'Студент'
  return roleLabel(session.role)
})
const isStubPage = computed(() => ['violations', 'privileges', 'settings', 'my-class', 'profile', 'ratings'].includes(currentPage.value))
const pageTitle = computed(() => {
  const item = currentMenu.value.find((menuItem) => menuItem.page === currentPage.value)
  if (currentPage.value === 'home' && session.role === 'ADMIN') return 'Сводная панель'
  return item?.label || 'Главная'
})
const pageSubtitle = computed(() => {
  if (currentPage.value === 'home') return ''
  if (currentPage.value === 'exams') return 'Создание, запуск, прохождение, завершение и оценивание экзаменов'
  if (currentPage.value === 'votings') return 'Создание тайного голосования, участие студентов и подсчет итогов'
  if (currentPage.value === 'users') return 'Создание пользователей, роли, валидация и список учетных записей'
  return 'Функционал архитектурного прототипа'
})

const recentActions = computed(() => [
  ...exams.value.slice(-3).reverse().map((exam) => `${exam.title}: ${statusLabel(exam.status).toLowerCase()}`),
  ...votings.value.slice(-2).reverse().map((voting) => `${voting.title}: ${voting.status === 'ACTIVE' ? 'активно' : 'завершено'}`)
].slice(0, 4))
const curatorViolations = computed<Array<{ date: string; student: string; description: string; exam: string; points: number }>>(() => [])
const curatorPrivilegeRequests = computed<Array<{ date: string; title: string; status: string; kind: string }>>(() => [])
const examinerHomeRows = computed(() => exams.value.slice(0, 6).map((exam) => ({
  id: exam.id,
  date: exam.scheduledStartTime ? new Date(exam.scheduledStartTime).toLocaleDateString('ru-RU') : '-',
  time: exam.scheduledStartTime ? new Date(exam.scheduledStartTime).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' }) : '-',
  exam: exam.title,
  subject: exam.subject,
  classes: exam.schoolClass?.name || '-',
  participants: users.value.filter((user) => user.role === 'STUDENT' && (!exam.schoolClass?.name || user.className === exam.schoolClass.name)).length,
  status: statusLabel(exam.status)
})))

function authHeaders(): HeadersInit {
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${session.token}`
  }
}

async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      ...authHeaders(),
      ...(options.headers || {})
    }
  })
  const text = await response.text()
  const data = text ? JSON.parse(text) : null
  if (!response.ok) {
    throw new Error(data?.message || data?.error || `HTTP ${response.status}`)
  }
  return data as T
}

function setMessage(text: string, kind: 'info' | 'success' | 'error' = 'info') {
  message.text = text
  message.kind = kind
}

function formatNumber(value: number) {
  return value.toLocaleString('ru-RU')
}

async function login() {
  loading.value = true
  try {
    const response = await fetch(`${API_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(loginForm)
    })
    const data = (await response.json()) as LoginResponse & { message?: string }
    if (!response.ok) throw new Error(data.message || 'Неверный логин или пароль')

    Object.assign(session, data)
    if (rememberMe.value) localStorage.setItem('school-session', JSON.stringify(data))
    currentPage.value = 'home'
    await bootstrap()
    setMessage('Вход выполнен', 'success')
  } catch (error) {
    setMessage(error instanceof Error ? error.message : 'Ошибка входа', 'error')
  } finally {
    loading.value = false
  }
}

function logout() {
  stompClient.value?.deactivate()
  localStorage.removeItem('school-session')
  Object.assign(session, { token: '', role: 'STUDENT', accountId: null, userId: null, displayName: '' })
  selectedExam.value = null
  selectedVoting.value = null
  currentPage.value = 'home'
}

async function bootstrap() {
  await Promise.allSettled([loadClasses(), loadUsers(), loadExams(), loadVotings(), loadMyResults()])
}

async function loadClasses() {
  classes.value = await api<SchoolClass[]>('/users/classes')
}

async function loadUsers() {
  users.value = await api<UserResponse[]>('/users')
}

async function loadExams() {
  exams.value = await api<ExamSession[]>('/exam/session')
}

async function loadVotings() {
  votings.value = await api<SecretVoting[]>('/vote/secret')
}

async function loadMyResults() {
  if (session.userId) {
    myResults.value = await api<ExamResult[]>(`/exam/session/students/${session.userId}/results`)
  }
}

async function refreshCurrent() {
  try {
    await bootstrap()
    if (selectedExam.value) await openExam(selectedExam.value.exam.id)
    if (selectedVoting.value) await openVoting(selectedVoting.value.voting.id)
    setMessage('Данные обновлены', 'success')
  } catch (error) {
    setMessage(error instanceof Error ? error.message : 'Ошибка обновления', 'error')
  }
}

function openPage(page: Page) {
  currentPage.value = page
  setMessage('', 'info')
  if (page === 'results') loadMyResults()
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
}

async function registerUser() {
  loading.value = true
  try {
    const body = {
      ...registerForm,
      classId: registerForm.role === 'STUDENT' ? registerForm.classId : null,
      entranceExamScore: registerForm.role === 'STUDENT' ? registerForm.entranceExamScore : null,
      birthDate: registerForm.birthDate || null
    }
    await api<UserResponse>('/users/register', { method: 'POST', body: JSON.stringify(body) })
    await loadUsers()
    resetRegisterForm()
    setMessage('Пользователь создан', 'success')
  } catch (error) {
    setMessage(error instanceof Error ? error.message : 'Ошибка регистрации', 'error')
  } finally {
    loading.value = false
  }
}

async function createExam() {
  try {
    const questions = examQuestionsText.value
      .split('\n')
      .map((text) => text.trim())
      .filter(Boolean)
      .map((text, index) => ({
        orderIndex: index + 1,
        text,
        type: 'TEXT',
        maxScore: 1
      }))
    await api<ExamSession>('/exam/session', {
      method: 'POST',
      body: JSON.stringify({ ...examForm, questions })
    })
    await loadExams()
    setMessage('Экзамен создан', 'success')
  } catch (error) {
    setMessage(error instanceof Error ? error.message : 'Ошибка создания экзамена', 'error')
  }
}

async function openExam(id: number) {
  selectedExam.value = await api<ExamDetails>(`/exam/session/${id}/details`)
  selectedExam.value.questions.forEach((question) => {
    const existing = selectedExam.value?.answers.find((answer) => answer.questionId === question.id && answer.userId === session.userId)
    answerDrafts[question.id] = existing?.text || answerDrafts[question.id] || ''
  })
  classStudentsForSelectedExam.value.forEach((student) => {
    gradeForm[student.id] = gradeForm[student.id] || 0
  })
  if (session.role === 'EXAMINER') connectExamSocket(id)
}

async function startExam(id: number) {
  await api<ExamSession>(`/exam/session/start/${id}`, { method: 'POST' })
  await loadExams()
  await openExam(id)
  setMessage('Экзамен запущен', 'success')
}

async function finishExam(id: number) {
  await api<ExamSession>(`/exam/session/end/${id}`, { method: 'POST' })
  await loadExams()
  await openExam(id)
  setMessage('Экзамен завершен', 'success')
}

async function submitAllAnswers() {
  if (!selectedExam.value || !session.userId) return
  for (const question of selectedExam.value.questions) {
    const text = answerDrafts[question.id]?.trim()
    if (!text) continue
    await api<Answer>(`/exam/session/${selectedExam.value.exam.id}/answers`, {
      method: 'POST',
      body: JSON.stringify({
        studentId: session.userId,
        questionId: question.id,
        text,
        finalSubmitted: true
      })
    })
  }
  await openExam(selectedExam.value.exam.id)
  setMessage('Ответы сохранены', 'success')
}

async function gradeExam() {
  if (!selectedExam.value) return
  const scores = classStudentsForSelectedExam.value.map((student) => ({
    studentId: student.id,
    rawScore: Number(gradeForm[student.id] || 0)
  }))
  await api<ExamResult[]>(`/exam/session/${selectedExam.value.exam.id}/grades`, {
    method: 'POST',
    body: JSON.stringify({ scores })
  })
  await loadClasses()
  await openExam(selectedExam.value.exam.id)
  setMessage('Итоговые баллы сохранены', 'success')
}

function connectExamSocket(examId: number) {
  stompClient.value?.deactivate()
  realtimeEvents.value = []
  const client = new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    reconnectDelay: 3000,
    onConnect: () => {
      client.subscribe(`/topic/exams/${examId}`, (frame) => {
        const payload = JSON.parse(frame.body)
        realtimeEvents.value.unshift({
          id: `${Date.now()}-${Math.random()}`,
          type: payload.type,
          time: new Date().toLocaleTimeString('ru-RU')
        })
      })
    }
  })
  stompClient.value = client
  client.activate()
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
      body: JSON.stringify({ ...votingForm, options })
    })
    await loadVotings()
    setMessage('Голосование создано', 'success')
  } catch (error) {
    setMessage(error instanceof Error ? error.message : 'Ошибка создания голосования', 'error')
  }
}

async function openVoting(id: number) {
  selectedVoting.value = await api<VotingDetails>(`/vote/secret/${id}`)
}

async function submitVote(optionId: number) {
  if (!selectedVoting.value || !session.userId) return
  try {
    await api(`/vote/secret/${selectedVoting.value.voting.id}/votes`, {
      method: 'POST',
      body: JSON.stringify({ studentId: session.userId, optionId })
    })
    await openVoting(selectedVoting.value.voting.id)
    setMessage('Голос принят', 'success')
  } catch (error) {
    setMessage(error instanceof Error ? error.message : 'Ошибка голосования', 'error')
  }
}

async function finishVoting(id: number) {
  await api<SecretVoting>(`/vote/secret/${id}/finish`, { method: 'POST' })
  await loadVotings()
  await openVoting(id)
  setMessage('Голосование завершено', 'success')
}

function roleLabel(role?: Role) {
  return {
    STUDENT: 'Ученик',
    CURATOR: 'Куратор класса',
    EXAMINER: 'Экзаменатор-модератор',
    ADMIN: 'Администратор'
  }[role || 'STUDENT']
}

function statusLabel(status: ExamStatus) {
  return {
    PREPARED: 'Подготовлен',
    ACTIVE: 'Активен',
    FINISHED: 'Завершен'
  }[status]
}

onMounted(async () => {
  const saved = localStorage.getItem('school-session')
  if (!saved) return

  try {
    const parsed = JSON.parse(saved) as Partial<LoginResponse>
    if (!parsed.token || !parsed.role || !menus[parsed.role]) {
      throw new Error('Invalid saved session')
    }
    Object.assign(session, parsed)
    await bootstrap()
  } catch {
    localStorage.removeItem('school-session')
    Object.assign(session, { token: '', role: 'STUDENT', accountId: null, userId: null, displayName: '' })
  }
})
</script>
