<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand">SCHOOL SYSTEM</div>
      <div v-if="session.token" class="topbar-user">
        <span>{{ topbarLabel }}</span>
        <button class="link-button" @click="logout">Выход</button>
      </div>
      <div v-else class="topbar-user">
        <span>↪ Вход</span>
      </div>
    </header>

    <main v-if="!session.token" class="login-page">
      <section class="login-panel">
        <h1>Вход в систему</h1>
        <form class="form-stack" @submit.prevent="login">
          <label>
            <span>Логин или email</span>
            <input v-model.trim="loginForm.username" autocomplete="username" placeholder="Введите логин или email" />
          </label>
          <label>
            <span>Пароль</span>
            <input v-model="loginForm.password" type="password" autocomplete="current-password" placeholder="Введите пароль" />
          </label>
          <div class="inline-row split">
            <label class="check-row">
              <input v-model="rememberMe" type="checkbox" />
              <span>Запомнить меня</span>
            </label>
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
          <div class="panel register-panel">
            <h2>Создание нового пользователя</h2>
            <form class="registration-form" @submit.prevent="registerUser">
              <label>
                <span>ФИО</span>
                <input v-model.trim="registerForm.fullName" placeholder="Введите ФИО полностью" required />
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
                <span>Класс (для учеников)</span>
                <select v-model.number="registerForm.classId" :disabled="registerForm.role !== 'STUDENT'">
                  <option :value="null">Выберите класс</option>
                  <option v-for="schoolClass in classes" :key="schoolClass.id" :value="schoolClass.id">
                    {{ schoolClass.name }}
                  </option>
                </select>
              </label>
              <label>
                <span>Дата рождения</span>
                <input v-model="registerForm.birthDate" type="date" />
              </label>
              <label>
                <span>Паспортные данные</span>
                <input v-model.trim="registerForm.passportData" placeholder="Серия, номер или иной идентификатор" />
              </label>
              <label>
                <span>Контактные данные</span>
                <input v-model.trim="registerForm.contactInfo" placeholder="Телефон или другой контакт" />
              </label>
              <label v-if="registerForm.role === 'STUDENT'">
                <span>Вступительный балл</span>
                <input v-model.number="registerForm.entranceExamScore" type="number" min="0" />
              </label>
              <label>
                <span>Email</span>
                <input v-model.trim="registerForm.email" type="email" placeholder="Введите email" required />
              </label>
              <label>
                <span>Логин</span>
                <input v-model.trim="registerForm.username" placeholder="Введите логин" required />
              </label>
              <label v-if="!editingUserId" class="password-with-action">
                <span>Пароль</span>
                <input v-model="registerForm.password" placeholder="Введите пароль" required />
                <button class="secondary compact" type="button" @click="generatePassword">Сгенерировать</button>
              </label>
              <label v-if="!editingUserId">
                <span>Подтверждение пароля</span>
                <input v-model="confirmPassword" placeholder="Повторите пароль" required />
              </label>
              <label class="check-row full">
                <input v-model="sendCredentials" type="checkbox" disabled />
                <span>Отправить учетные данные пользователю на email</span>
              </label>
              <div class="actions">
                <button class="primary" type="submit" :disabled="loading">{{ editingUserId ? 'Сохранить изменения' : 'Создать пользователя' }}</button>
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
                  <th>Дата создания</th>
                  <th>Действия</th>
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
                  <td>{{ formatDateTime(user.createdAt) }}</td>
                  <td class="table-actions">
                    <button class="icon-button" type="button" title="Редактировать" @click="editUser(user)">✎</button>
                    <button class="icon-button" type="button" title="Удалить" @click="deactivateUser(user)">⌫</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section v-if="currentPage === 'exams'" class="stack">
          <div v-if="session.role === 'EXAMINER'" class="panel exam-create-panel">
            <h2>Создание экзамена</h2>
            <form class="exam-create-form" @submit.prevent="createExam">
              <label>
                <span>Название экзамена:</span>
                <input v-model.trim="examForm.title" placeholder="Например: Математика" required />
              </label>
              <label>
                <span>Класс:</span>
                <select v-model.number="examForm.classId" required>
                  <option :value="null">Выберите класс</option>
                  <option v-for="schoolClass in classes" :key="schoolClass.id" :value="schoolClass.id">
                    {{ schoolClass.name }}
                  </option>
                </select>
              </label>
              <label>
                <span>Предмет:</span>
                <input v-model.trim="examForm.subject" placeholder="Предмет" required />
              </label>
              <label>
                <span>Дата и время начала:</span>
                <input v-model="examForm.scheduledStartTime" type="datetime-local" />
              </label>
              <label>
                <span>Продолжительность (мин):</span>
                <input v-model.number="examForm.durationMinutes" type="number" min="1" />
              </label>
              <label>
                <span>Количество вопросов:</span>
                <input :value="examQuestionCount" type="number" min="0" readonly />
              </label>
              <label>
                <span>Описание (необязательно):</span>
                <textarea v-model.trim="examForm.description" placeholder="Краткое описание экзамена"></textarea>
              </label>
              <label>
                <span>Вопросы:</span>
                <textarea v-model="examQuestionsText" placeholder="Один вопрос на строку"></textarea>
              </label>
              <label class="file-row">
                <span>Прикрепить файл с вопросами:</span>
                <input type="file" />
              </label>
              <div class="actions">
                <button class="primary" type="submit" :disabled="loading">Создать экзамен</button>
                <button class="secondary" type="button">Отмена</button>
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
                <tr>
                  <td colspan="5">
                    <div class="tabs">
                      <button :class="{ active: examStatusFilter === 'ALL' }" type="button" @click="examStatusFilter = 'ALL'">Все</button>
                      <button :class="{ active: examStatusFilter === 'PREPARED' }" type="button" @click="examStatusFilter = 'PREPARED'">Подготовлены</button>
                      <button :class="{ active: examStatusFilter === 'ACTIVE' }" type="button" @click="examStatusFilter = 'ACTIVE'">Активные</button>
                      <button :class="{ active: examStatusFilter === 'FINISHED' }" type="button" @click="examStatusFilter = 'FINISHED'">Завершенные</button>
                    </div>
                  </td>
                </tr>
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
                <tr v-if="!visibleExams.length">
                  <td colspan="5">Экзаменов по выбранному фильтру пока нет</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="selectedExam && session.role === 'EXAMINER'" class="panel exam-monitor-panel">
            <div class="inline-row split">
              <div>
                <h2>Экзамен: {{ selectedExam.exam.title }} <span v-if="selectedExam.exam.schoolClass">({{ selectedExam.exam.schoolClass.name }})</span></h2>
                <p>Статус: <span :class="selectedExam.exam.status === 'ACTIVE' ? 'positive' : ''">{{ statusLabel(selectedExam.exam.status) }}</span></p>
              </div>
              <div class="inline-actions">
                <button v-if="selectedExam.exam.status === 'ACTIVE'" class="danger" @click="finishExam(selectedExam.exam.id)">Завершить экзамен</button>
                <button class="secondary" @click="refreshCurrent">Обновить</button>
              </div>
            </div>

            <div class="tabs exam-tabs">
              <button class="active" type="button">Мониторинг</button>
              <button type="button">Вопросы</button>
              <button type="button">Нарушения</button>
              <button type="button">Итоговые баллы</button>
            </div>

            <table class="monitor-table">
              <thead>
                <tr>
                  <th>№</th>
                  <th>Ученик</th>
                  <th>Статус</th>
                  <th>Ответы</th>
                  <th>Время</th>
                  <th>Действия</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(student, index) in classStudentsForSelectedExam" :key="student.id">
                  <td>{{ index + 1 }}</td>
                  <td>{{ student.fullName }}</td>
                  <td><span class="status ok"></span>Активен</td>
                  <td>{{ answerCountForStudent(student.id) }} / {{ selectedExam.questions.length }}</td>
                  <td>{{ examElapsedLabel(selectedExam.exam) }}</td>
                  <td><button class="secondary compact" type="button">Открыть</button></td>
                </tr>
                <tr v-if="!classStudentsForSelectedExam.length">
                  <td colspan="6">Участников пока нет</td>
                </tr>
              </tbody>
            </table>

            <div class="exam-bottom-grid">
              <section class="panel compact-panel">
                <h3>Фиксация нарушения</h3>
                <div class="violation-form">
                  <select v-model.number="violationForm.userId">
                    <option :value="null">Выберите ученика</option>
                    <option v-for="student in classStudentsForSelectedExam" :key="student.id" :value="student.id">{{ student.fullName }}</option>
                  </select>
                  <button class="primary" type="button" @click="reportViolation">Зафиксировать</button>
                  <textarea v-model.trim="violationForm.description" placeholder="Описание нарушения"></textarea>
                  <input v-model.number="violationForm.pointsPenalty" type="number" min="0" placeholder="Штраф, баллы" />
                </div>
              </section>
              <section class="panel compact-panel">
                <h3>Быстрые действия</h3>
                <div class="quick-actions">
                  <button class="secondary" type="button">Сообщение ученикам</button>
                  <button class="secondary" type="button" @click="refreshCurrent">Обновить список</button>
                  <button class="secondary" type="button" @click="connectExamSocket(selectedExam.exam.id)">Подключить WebSocket</button>
                </div>
              </section>
            </div>

            <div class="results-block">
              <h3>Итоговые баллы</h3>
              <div class="score-list">
                <label v-for="student in classStudentsForSelectedExam" :key="student.id">
                  <span>{{ student.fullName }}</span>
                  <input v-model.number="gradeForm[student.id]" type="number" min="0" />
                </label>
              </div>
              <button class="primary" :disabled="selectedExam.exam.status !== 'FINISHED'" @click="gradeExam">Сохранить баллы</button>
            </div>
          </div>

          <div v-if="selectedExam && session.role === 'STUDENT'" class="student-exam-frame">
              <div v-if="selectedExam.exam.status !== 'ACTIVE'" class="empty-state">Экзамен пока недоступен для прохождения.</div>
              <div v-else class="student-exam-layout">
                <header class="exam-focus-head">
                  <h2>Экзамен: {{ selectedExam.exam.title }}</h2>
                  <strong>Время осталось: {{ examRemainingLabel(selectedExam.exam) }}</strong>
                  <button class="secondary" type="button" :disabled="currentExamSubmitted" @click="submitAllAnswers">Завершить экзамен</button>
                </header>
                <aside class="question-nav">
                  <h3>Вопросы</h3>
                  <button
                    v-for="(question, index) in selectedExam.questions"
                    :key="question.id"
                    :class="{ active: currentQuestionIndex === index }"
                    type="button"
                    @click="currentQuestionIndex = index"
                  >
                    {{ question.orderIndex }}
                  </button>
                </aside>
                <section v-if="currentQuestion" class="question-workarea">
                  <div class="question-body">
                    <h3>Вопрос {{ currentQuestion.orderIndex }}</h3>
                    <p>{{ currentQuestion.text }}</p>
                  </div>
                  <label class="answer-box">
                    <span>Ваш ответ / Решение:</span>
                    <textarea v-model="answerDrafts[currentQuestion.id]" :disabled="currentExamSubmitted" placeholder="Введите ответ"></textarea>
                  </label>
                  <div class="actions">
                    <button class="primary" type="button" :disabled="currentExamSubmitted" @click="saveCurrentAnswer">Сохранить ответ</button>
                    <button class="secondary" type="button" @click="nextQuestion">Следующий вопрос →</button>
                  </div>
                </section>
                <div class="exam-warning">Внимание! Экзамен работает в полноэкранном режиме. Потеря фокуса будет зафиксирована как нарушение.</div>
              </div>
          </div>
        </section>

        <section v-if="currentPage === 'votings'" class="stack">
          <div v-if="session.role === 'CURATOR'" class="tabs voting-tabs">
            <button class="active" type="button">Создать голосование</button>
            <button type="button">Активные голосования</button>
            <button type="button">Завершенные голосования</button>
          </div>

          <div v-if="session.role === 'CURATOR'" class="panel voting-create-panel">
            <h2>Создание тайного голосования</h2>
            <form class="voting-create-form" @submit.prevent="createVoting">
              <div class="voting-fields">
                <label>
                <span>Тема голосования:</span>
                <input v-model.trim="votingForm.title" placeholder="Тема голосования" required />
                </label>
                <label>
                <span>Описание (необязательно):</span>
                <textarea v-model.trim="votingForm.description"></textarea>
                </label>
                <label>
                  <span>Класс:</span>
                  <select v-model.number="votingForm.classId" required>
                    <option :value="null">Выберите класс</option>
                    <option v-for="schoolClass in classes" :key="schoolClass.id" :value="schoolClass.id">
                      {{ schoolClass.name }}
                    </option>
                  </select>
                </label>
                <label>
                  <span>Тип голосования:</span>
                  <select>
                    <option>Один вариант</option>
                  </select>
                </label>
                <label>
                <span>Кандидаты (варианты):</span>
                <textarea v-model="votingOptionsText" placeholder="Один вариант на строку"></textarea>
                </label>
                <label>
                  <span>Длительность голосования:</span>
                  <select v-model.number="votingDurationMinutes">
                    <option :value="15">15 минут</option>
                    <option :value="30">30 минут</option>
                    <option :value="60">60 минут</option>
                  </select>
                </label>
                <button class="primary" type="submit">Создать голосование</button>
              </div>
              <aside class="panel voting-info">
                <h3>ⓘ Информация</h3>
                <ul>
                  <li>Голоса студентов анонимны.</li>
                  <li>Каждый студент может проголосовать только один раз.</li>
                  <li>После завершения голосования результаты будут подсчитаны автоматически.</li>
                </ul>
              </aside>
            </form>
          </div>

          <div class="panel">
            <h2>{{ session.role === 'CURATOR' ? 'Активные голосования' : 'Голосования' }}</h2>
            <table>
              <thead>
                <tr>
                  <th>Тема голосования</th>
                  <th>Статус</th>
                  <th>Класс</th>
                  <th>Проголосовало</th>
                  <th>Действия</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td colspan="5">
                    <div class="tabs">
                      <button :class="{ active: votingStatusFilter === 'ALL' }" type="button" @click="votingStatusFilter = 'ALL'">Все</button>
                      <button :class="{ active: votingStatusFilter === 'ACTIVE' }" type="button" @click="votingStatusFilter = 'ACTIVE'">Активные</button>
                      <button :class="{ active: votingStatusFilter === 'FINISHED' }" type="button" @click="votingStatusFilter = 'FINISHED'">Завершенные</button>
                    </div>
                  </td>
                </tr>
                <tr v-for="voting in visibleVotings" :key="voting.id">
                  <td>{{ voting.title }}</td>
                  <td><span :class="['badge', voting.status.toLowerCase()]">{{ voting.status === 'ACTIVE' ? 'Активно' : 'Завершено' }}</span></td>
                  <td>{{ voting.schoolClass?.name || '-' }}</td>
                  <td>{{ selectedVoting?.voting.id === voting.id && selectedVoting.resultsVisible ? totalVotes(selectedVoting.results) : '-' }}</td>
                  <td class="table-actions">
                    <button class="secondary compact" @click="openVoting(voting.id)">Просмотр</button>
                    <button v-if="session.role === 'CURATOR' && voting.status === 'ACTIVE'" class="danger compact" @click="finishVoting(voting.id)">Завершить голосование</button>
                  </td>
                </tr>
                <tr v-if="!visibleVotings.length">
                  <td colspan="5">Голосований пока нет</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="selectedVoting && session.role === 'STUDENT'" class="voting-student-page">
            <h2>Тайное голосование</h2>
            <div class="voting-student-top">
              <section class="panel voting-details">
                <h3>Информация о голосовании</h3>
                <dl>
                  <dt>Название:</dt>
                  <dd>{{ selectedVoting.voting.title }}</dd>
                  <dt>Описание:</dt>
                  <dd>{{ selectedVoting.voting.description || '-' }}</dd>
                  <dt>Класс:</dt>
                  <dd>{{ selectedVoting.voting.schoolClass?.name || '-' }}</dd>
                  <dt>Статус:</dt>
                  <dd>{{ selectedVoting.voting.status === 'ACTIVE' ? 'Активно' : 'Завершено' }}</dd>
                  <dt>Время окончания:</dt>
                  <dd>{{ formatDateTime(selectedVoting.voting.endsAt) }}</dd>
                </dl>
              </section>
              <section class="panel voting-timer">
                <span>До окончания голосования:</span>
                <strong>{{ votingRemainingLabel(selectedVoting.voting) }}</strong>
                <div class="progress-line"><span></span></div>
              </section>
            </div>
            <div class="anonymous-note">ⓘ Ваш голос анонимен и не может быть отслежен.</div>
            <section class="panel candidate-panel">
              <h3>Выберите вариант</h3>
              <label v-for="option in selectedVoting.options" :key="option.id" class="candidate-row">
                <input type="radio" name="vote-option" :value="option.id" v-model="selectedVoteOptionId" :disabled="currentVotingLocked" />
                <span>{{ option.label }}</span>
              </label>
              <div v-if="!selectedVoting.options.length" class="empty-state">Вариантов пока нет</div>
            </section>
            <div class="actions">
              <button class="primary" :disabled="!selectedVoteOptionId || currentVotingLocked" @click="submitSelectedVote">Отправить голос</button>
              <button class="secondary" type="button">Отмена</button>
              <span class="negative">Важно! После отправки изменить голос нельзя.</span>
            </div>
          </div>

          <div v-if="selectedVoting && session.role !== 'STUDENT'" class="panel voting-results-panel">
            <h2>Итоги голосований</h2>
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
                <tr v-if="!selectedVoting.resultsVisible">
                  <td colspan="2">Результаты будут доступны после завершения голосования</td>
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
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'

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
  classId?: number | null
  className?: string
  passportData?: string | null
  entranceExamScore?: number | null
  contactInfo?: string | null
  birthDate?: string | null
  active: boolean
  createdAt?: string | null
}

interface ExamSession {
  id: number
  title: string
  subject: string
  status: ExamStatus
  schoolClass?: SchoolClass
  totalQuestions?: number
  durationMinutes?: number
  scheduledStartTime?: string | null
  startTime?: string | null
  endTime?: string | null
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
  finalSubmitted?: boolean
}

interface ExamAttempt {
  started: boolean
  submitted: boolean
  startedAt?: string | null
  submittedAt?: string | null
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
  violations?: unknown[]
  attempt?: ExamAttempt
}

interface SecretVoting {
  id: number
  title: string
  description?: string
  schoolClass?: SchoolClass
  status: VotingStatus
  startedAt?: string | null
  endsAt?: string | null
  finishedAt?: string | null
}

interface VotingOption {
  id: number
  label: string
}

interface VotingDetails {
  voting: SecretVoting
  options: VotingOption[]
  results: Record<string, number>
  hasVoted: boolean
  resultsVisible: boolean
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
const currentQuestionIndex = ref(0)
const selectedVoteOptionId = ref<number | null>(null)
const now = ref(Date.now())
const examStatusFilter = ref<ExamStatus | 'ALL'>('ALL')
const votingStatusFilter = ref<VotingStatus | 'ALL'>('ALL')
const reportedClientViolations = new Set<string>()

const userRoleFilter = ref<Role | 'ALL'>('ALL')
const editingUserId = ref<number | null>(null)
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
const confirmPassword = ref('')
const sendCredentials = ref(true)

const examForm = reactive({
  title: 'test',
  subject: 'test',
  classId: null as number | null,
  durationMinutes: 45,
  description: '',
  scheduledStartTime: ''
})
const examQuestionsText = ref('1 + 1')
const answerDrafts = reactive<Record<number, string>>({})
const gradeForm = reactive<Record<number, number>>({})

const votingForm = reactive({
  title: '',
  description: '',
  classId: null as number | null
})
const votingOptionsText = ref('За\nПротив')
const votingDurationMinutes = ref(15)
const violationForm = reactive({
  userId: null as number | null,
  description: '',
  pointsPenalty: 0
})

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
  const filtered = examStatusFilter.value === 'ALL' ? exams.value : exams.value.filter((exam) => exam.status === examStatusFilter.value)
  if (session.role !== 'STUDENT') return filtered
  const myClass = currentUser.value?.className
  return filtered.filter((exam) => !myClass || exam.schoolClass?.name === myClass)
})
const visibleVotings = computed(() => {
  const filtered = votingStatusFilter.value === 'ALL' ? votings.value : votings.value.filter((voting) => voting.status === votingStatusFilter.value)
  if (session.role !== 'STUDENT') return filtered
  const myClass = currentUser.value?.className
  return filtered.filter((voting) => !myClass || voting.schoolClass?.name === myClass)
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
const examQuestionCount = computed(() => examQuestionsText.value.split('\n').map((text) => text.trim()).filter(Boolean).length)
const currentQuestion = computed(() => selectedExam.value?.questions[currentQuestionIndex.value] || null)
const currentExamSubmitted = computed(() => Boolean(selectedExam.value?.attempt?.submitted))
const currentVotingLocked = computed(() => Boolean(selectedVoting.value?.hasVoted || selectedVoting.value?.voting.status !== 'ACTIVE'))

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

function formatDateTime(value?: string | null) {
  if (!value) return '-'
  return new Date(value).toLocaleString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function generatePassword() {
  const password = Math.random().toString(36).slice(2, 10)
  registerForm.password = password
  confirmPassword.value = password
}

function answerCountForStudent(studentId: number) {
  return selectedExam.value?.answers.filter((answer) => answer.userId === studentId && answer.text?.trim()).length || 0
}

function examElapsedLabel(exam: ExamSession) {
  if (!exam.scheduledStartTime) return '-'
  return new Date(exam.scheduledStartTime).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
}

function examRemainingLabel(exam: ExamSession) {
  if (!exam.durationMinutes) return '—'
  const sourceStartTime = exam.startTime || exam.scheduledStartTime
  const startTime = sourceStartTime ? new Date(sourceStartTime).getTime() : Date.now()
  const endTime = startTime + exam.durationMinutes * 60000
  const remainingMs = endTime - now.value
  if (remainingMs <= 0) return '00:00'
  const minutes = Math.floor(remainingMs / 60000)
  const seconds = Math.floor((remainingMs % 60000) / 1000)
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

function totalVotes(results: Record<string, number>) {
  return Object.values(results).reduce((sum, count) => sum + count, 0)
}

function toLocalDateTimeInputValue(date: Date) {
  const offset = date.getTimezoneOffset() * 60000
  return new Date(date.getTime() - offset).toISOString().slice(0, 19)
}

function votingRemainingLabel(voting: SecretVoting) {
  if (voting.status !== 'ACTIVE' || !voting.endsAt) return '—'
  const remainingMs = new Date(voting.endsAt).getTime() - now.value
  if (remainingMs <= 0) return '—'
  const minutes = Math.floor(remainingMs / 60000)
  const seconds = Math.floor((remainingMs % 60000) / 1000)
  return `${minutes}:${String(seconds).padStart(2, '0')}`
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
  if (session.role === 'STUDENT') {
    await Promise.allSettled([loadCurrentUser(), loadMyClass(), loadExams(), loadVotings(), loadMyResults()])
    return
  }

  await Promise.allSettled([loadClasses(), loadUsers(), loadExams(), loadVotings(), loadMyResults()])
}

async function loadCurrentUser() {
  const user = await api<UserResponse>('/users/me')
  users.value = [user]
}

async function loadMyClass() {
  try {
    const schoolClass = await api<SchoolClass>('/users/me/class')
    classes.value = [schoolClass]
  } catch {
    classes.value = []
  }
}

async function loadClasses() {
  classes.value = await api<SchoolClass[]>('/users/classes')
}

async function loadUsers() {
  users.value = await api<UserResponse[]>('/users')
}

async function loadExams() {
  exams.value = await api<ExamSession[]>(session.role === 'STUDENT' ? '/exam/session/my' : '/exam/session')
}

async function loadVotings() {
  votings.value = await api<SecretVoting[]>(session.role === 'STUDENT' ? '/vote/secret/my' : '/vote/secret')
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
  confirmPassword.value = ''
  editingUserId.value = null
}

async function registerUser() {
  loading.value = true
  try {
    if (!editingUserId.value && registerForm.password !== confirmPassword.value) {
      throw new Error('Пароли не совпадают')
    }

    const body = {
      ...registerForm,
      classId: registerForm.role === 'STUDENT' ? registerForm.classId : null,
      entranceExamScore: registerForm.role === 'STUDENT' ? registerForm.entranceExamScore : null,
      birthDate: registerForm.birthDate || null
    }

    const wasEditing = editingUserId.value !== null
    if (editingUserId.value) {
      await api<UserResponse>(`/users/${editingUserId.value}`, {
        method: 'PUT',
        body: JSON.stringify({
          fullName: body.fullName,
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
  } catch (error) {
    setMessage(error instanceof Error ? error.message : 'Ошибка регистрации', 'error')
  } finally {
    loading.value = false
  }
}

function editUser(user: UserResponse) {
  editingUserId.value = user.id
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
      body: JSON.stringify({
        ...examForm,
        scheduledStartTime: examForm.scheduledStartTime || null,
        questions
      })
    })
    await loadExams()
    setMessage('Экзамен создан', 'success')
  } catch (error) {
    setMessage(error instanceof Error ? error.message : 'Ошибка создания экзамена', 'error')
  }
}

async function openExam(id: number) {
  selectedExam.value = await api<ExamDetails>(`/exam/session/${id}/details`)
  currentQuestionIndex.value = 0
  selectedExam.value.questions.forEach((question) => {
    const existing = selectedExam.value?.answers.find((answer) => answer.questionId === question.id && answer.userId === session.userId)
    answerDrafts[question.id] = existing?.text || answerDrafts[question.id] || ''
  })
  classStudentsForSelectedExam.value.forEach((student) => {
    gradeForm[student.id] = gradeForm[student.id] || 0
  })
  if (session.role === 'EXAMINER') connectExamSocket(id)
  if (session.role === 'STUDENT' && selectedExam.value.exam.status === 'ACTIVE' && !currentExamSubmitted.value) {
    requestExamFullscreen()
  }
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
  if (currentExamSubmitted.value) return
  for (const question of selectedExam.value.questions) {
    const text = answerDrafts[question.id]?.trim()
    if (!text) continue
    await api<Answer>(`/exam/session/${selectedExam.value.exam.id}/answers/me`, {
      method: 'POST',
      body: JSON.stringify({
        questionId: question.id,
        text,
        finalSubmitted: false
      })
    })
  }
  await api(`/exam/session/${selectedExam.value.exam.id}/attempt/me/submit`, { method: 'POST' })
  await openExam(selectedExam.value.exam.id)
  setMessage('Ответы сохранены', 'success')
}

async function saveCurrentAnswer() {
  if (!selectedExam.value || !currentQuestion.value || !session.userId) return
  if (currentExamSubmitted.value) return

  const text = answerDrafts[currentQuestion.value.id]?.trim()
  if (!text) {
    setMessage('Введите ответ перед сохранением', 'error')
    return
  }

  await saveAnswer(currentQuestion.value.id, text)
  await openExam(selectedExam.value.exam.id)
  setMessage('Ответ сохранен', 'success')
}

async function saveAnswer(questionId: number, text: string) {
  if (!selectedExam.value) return

  await api<Answer>(`/exam/session/${selectedExam.value.exam.id}/answers/me`, {
    method: 'POST',
    body: JSON.stringify({
      questionId,
      text,
      finalSubmitted: false
    })
  })
}

async function autosaveCurrentAnswer() {
  if (session.role !== 'STUDENT' || !selectedExam.value || selectedExam.value.exam.status !== 'ACTIVE' || currentExamSubmitted.value || !currentQuestion.value) {
    return
  }

  const text = answerDrafts[currentQuestion.value.id]?.trim()
  if (!text) return

  try {
    await saveAnswer(currentQuestion.value.id, text)
  } catch {
    // Manual save shows user-facing errors; background autosave stays quiet.
  }
}

function nextQuestion() {
  if (!selectedExam.value?.questions.length) return
  currentQuestionIndex.value = Math.min(currentQuestionIndex.value + 1, selectedExam.value.questions.length - 1)
}

function requestExamFullscreen() {
  if (document.fullscreenElement || !document.documentElement.requestFullscreen) return
  document.documentElement.requestFullscreen().catch(() => {
    reportClientExamViolation('FULLSCREEN_REQUEST_REJECTED', 'Student did not enter fullscreen mode')
  })
}

async function reportClientExamViolation(type: string, description: string) {
  if (session.role !== 'STUDENT' || !selectedExam.value || selectedExam.value.exam.status !== 'ACTIVE' || currentExamSubmitted.value) return
  const key = `${selectedExam.value.exam.id}:${type}`
  if (reportedClientViolations.has(key)) return
  reportedClientViolations.add(key)

  try {
    await api('/violations/report/me', {
      method: 'POST',
      body: JSON.stringify({
        sessionId: selectedExam.value.exam.id,
        type,
        description,
        pointsPenalty: 0
      })
    })
  } catch {
    reportedClientViolations.delete(key)
  }
}

function handleFullscreenChange() {
  if (session.role === 'STUDENT' && selectedExam.value?.exam.status === 'ACTIVE' && !document.fullscreenElement) {
    reportClientExamViolation('FULLSCREEN_EXIT', 'Student left fullscreen mode during the exam')
  }
}

function handleWindowBlur() {
  reportClientExamViolation('WINDOW_BLUR', 'Student switched away from the exam window')
}

function handleVisibilityChange() {
  if (document.visibilityState === 'hidden') {
    reportClientExamViolation('PAGE_HIDDEN', 'Student hid the exam page')
  }
}

async function reportViolation() {
  if (!selectedExam.value || !violationForm.userId) {
    setMessage('Выберите ученика для фиксации нарушения', 'error')
    return
  }

  if (!violationForm.description.trim()) {
    setMessage('Введите описание нарушения', 'error')
    return
  }

  try {
    await api('/violations/report', {
      method: 'POST',
      body: JSON.stringify({
        sessionId: selectedExam.value.exam.id,
        userId: violationForm.userId,
        type: 'EXAM_RULE_VIOLATION',
        description: violationForm.description,
        pointsPenalty: Number(violationForm.pointsPenalty || 0)
      })
    })
    Object.assign(violationForm, { userId: null, description: '', pointsPenalty: 0 })
    setMessage('Нарушение зафиксировано', 'success')
  } catch (error) {
    setMessage(error instanceof Error ? error.message : 'Ошибка фиксации нарушения', 'error')
  }
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

const clockInterval = window.setInterval(() => {
  now.value = Date.now()
}, 1000)

const autosaveInterval = window.setInterval(() => {
  autosaveCurrentAnswer()
}, 30000)

onUnmounted(() => {
  window.clearInterval(clockInterval)
  window.clearInterval(autosaveInterval)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('blur', handleWindowBlur)
})

onMounted(async () => {
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('blur', handleWindowBlur)

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
