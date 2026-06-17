<template>
        <section class="stack">
          <div v-if="app.session.role === 'EXAMINER'" class="panel exam-create-panel">
            <h2>Создание экзамена</h2>
            <form class="exam-create-form" @submit.prevent="app.createExam">
              <label>
                <span>Название экзамена:</span>
                <input v-model.trim="app.examForm.title" placeholder="Например: Математика" required />
              </label>
              <label>
                <span>Класс:</span>
                <select v-model.number="app.examForm.classId" required>
                  <option :value="null">Выберите класс</option>
                  <option v-for="schoolClass in app.classes" :key="schoolClass.id" :value="schoolClass.id">
                    {{ schoolClass.name }}
                  </option>
                </select>
              </label>
              <label>
                <span>Предмет:</span>
                <input v-model.trim="app.examForm.subject" placeholder="Предмет" required />
              </label>
              <label>
                <span>Дата и время начала:</span>
                <input v-model="app.examForm.scheduledStartTime" type="datetime-local" />
              </label>
              <label>
                <span>Продолжительность (мин):</span>
                <input v-model.number="app.examForm.durationMinutes" type="number" min="1" />
              </label>
              <label>
                <span>Количество вопросов:</span>
                <input :value="app.examQuestionCount" type="number" min="0" readonly />
              </label>
              <label>
                <span>Описание (необязательно):</span>
                <textarea v-model.trim="app.examForm.description" placeholder="Краткое описание экзамена"></textarea>
              </label>
              <label>
                <span>Вопросы:</span>
                <textarea v-model="app.examQuestionsText" placeholder="Один вопрос на строку"></textarea>
              </label>
              <label class="file-row">
                <span>Прикрепить файл с вопросами:</span>
                <input type="file" />
              </label>
              <div class="actions">
                <button class="primary" type="submit" :disabled="app.loading">Создать экзамен</button>
                <button class="secondary" type="button">Отмена</button>
              </div>
            </form>
          </div>

          <div class="panel">
            <h2>{{ app.session.role === 'STUDENT' ? 'Доступные экзамены' : 'Экзамены' }}</h2>
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
                      <button :class="{ active: app.examStatusFilter === 'ALL' }" type="button" @click="app.examStatusFilter = 'ALL'">Все</button>
                      <button :class="{ active: app.examStatusFilter === 'PREPARED' }" type="button" @click="app.examStatusFilter = 'PREPARED'">Подготовлены</button>
                      <button :class="{ active: app.examStatusFilter === 'ACTIVE' }" type="button" @click="app.examStatusFilter = 'ACTIVE'">Активные</button>
                      <button :class="{ active: app.examStatusFilter === 'FINISHED' }" type="button" @click="app.examStatusFilter = 'FINISHED'">Завершенные</button>
                    </div>
                  </td>
                </tr>
                <tr v-for="exam in app.visibleExams" :key="exam.id">
                  <td>{{ exam.title }}</td>
                  <td>{{ exam.schoolClass?.name || '-' }}</td>
                  <td><span :class="['badge', exam.status.toLowerCase()]">{{ app.statusLabel(exam.status) }}</span></td>
                  <td>{{ exam.totalQuestions || 0 }}</td>
                  <td class="table-actions">
                    <button class="secondary compact" @click="app.openExam(exam.id)">Открыть</button>
                    <button v-if="app.session.role === 'EXAMINER' && exam.status === 'PREPARED'" class="primary compact" @click="app.startExam(exam.id)">Запустить</button>
                    <button v-if="app.session.role === 'EXAMINER' && exam.status === 'ACTIVE'" class="danger compact" @click="app.finishExam(exam.id)">Завершить</button>
                  </td>
                </tr>
                <tr v-if="!app.visibleExams.length">
                  <td colspan="5">Экзаменов по выбранному фильтру пока нет</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="app.selectedExam && app.session.role === 'EXAMINER'" class="panel exam-monitor-panel">
            <div class="inline-row split">
              <div>
                <h2>Экзамен: {{ app.selectedExam.exam.title }} <span v-if="app.selectedExam.exam.schoolClass">({{ app.selectedExam.exam.schoolClass.name }})</span></h2>
                <p>Статус: <span :class="app.selectedExam.exam.status === 'ACTIVE' ? 'positive' : ''">{{ app.statusLabel(app.selectedExam.exam.status) }}</span></p>
              </div>
              <div class="inline-actions">
                <button v-if="app.selectedExam.exam.status === 'ACTIVE'" class="danger" @click="app.finishExam(app.selectedExam.exam.id)">Завершить экзамен</button>
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
                <tr v-for="(student, index) in app.classStudentsForSelectedExam" :key="student.id">
                  <td>{{ index + 1 }}</td>
                  <td>{{ student.fullName }}</td>
                  <td><span class="status ok"></span>Активен</td>
                  <td>{{ app.answerCountForStudent(student.id) }} / {{ app.selectedExam.questions.length }}</td>
                  <td>{{ app.examElapsedLabel(app.selectedExam.exam) }}</td>
                  <td><button class="secondary compact" type="button">Открыть</button></td>
                </tr>
                <tr v-if="!app.classStudentsForSelectedExam.length">
                  <td colspan="6">Участников пока нет</td>
                </tr>
              </tbody>
            </table>

            <div class="exam-bottom-grid">
              <section class="panel compact-panel">
                <h3>Фиксация нарушения</h3>
                <div class="violation-form">
                  <select v-model.number="app.violationForm.userId">
                    <option :value="null">Выберите ученика</option>
                    <option v-for="student in app.classStudentsForSelectedExam" :key="student.id" :value="student.id">{{ student.fullName }}</option>
                  </select>
                  <button class="primary" type="button" @click="app.reportViolation">Зафиксировать</button>
                  <textarea v-model.trim="app.violationForm.description" placeholder="Описание нарушения"></textarea>
                  <input v-model.number="app.violationForm.pointsPenalty" type="number" min="0" placeholder="Штраф, баллы" />
                </div>
              </section>
              <section class="panel compact-panel">
                <h3>Быстрые действия</h3>
                <div class="quick-actions">
                  <button class="secondary" type="button">Сообщение ученикам</button>
                  <button class="secondary" type="button" @click="app.connectExamSocket(app.selectedExam.exam.id)">Подключить WebSocket</button>
                </div>
              </section>
            </div>

            <div class="results-block">
              <h3>Итоговые баллы</h3>
              <div class="score-list">
                <label v-for="student in app.classStudentsForSelectedExam" :key="student.id">
                  <span>{{ student.fullName }}</span>
                  <input v-model.number="app.gradeForm[student.id]" type="number" min="0" />
                </label>
              </div>
              <button class="primary" :disabled="app.selectedExam.exam.status !== 'FINISHED'" @click="app.gradeExam">Сохранить баллы</button>
            </div>
          </div>

          <div v-if="app.selectedExam && app.session.role === 'STUDENT'" class="student-exam-frame">
              <div v-if="app.selectedExam.exam.status !== 'ACTIVE'" class="empty-state">Экзамен пока недоступен для прохождения.</div>
              <div v-else class="student-exam-layout">
                <header class="exam-focus-head">
                  <h2>Экзамен: {{ app.selectedExam.exam.title }}</h2>
                  <strong>Время осталось: {{ app.examRemainingLabel(app.selectedExam.exam) }}</strong>
                  <button class="secondary" type="button" :disabled="app.currentExamSubmitted" @click="app.submitAllAnswers">Завершить экзамен</button>
                </header>
                <aside class="question-nav">
                  <h3>Вопросы</h3>
                  <button
                    v-for="(question, index) in app.selectedExam.questions"
                    :key="question.id"
                    :class="{ active: app.currentQuestionIndex === index }"
                    type="button"
                    @click="app.currentQuestionIndex = index"
                  >
                    {{ question.orderIndex }}
                  </button>
                </aside>
                <section v-if="app.currentQuestion" class="question-workarea">
                  <div class="question-body">
                    <h3>Вопрос {{ app.currentQuestion.orderIndex }}</h3>
                    <p>{{ app.currentQuestion.text }}</p>
                  </div>
                  <label class="answer-box">
                    <span>Ваш ответ / Решение:</span>
                    <textarea v-model="app.answerDrafts[app.currentQuestion.id]" :disabled="app.currentExamSubmitted" placeholder="Введите ответ"></textarea>
                  </label>
                  <div class="actions">
                    <button class="primary" type="button" :disabled="app.currentExamSubmitted" @click="app.saveCurrentAnswer">Сохранить ответ</button>
                    <button class="secondary" type="button" @click="app.nextQuestion">Следующий вопрос →</button>
                  </div>
                </section>
                <div class="exam-warning">Внимание! Экзамен работает в полноэкранном режиме. Потеря фокуса будет зафиксирована как нарушение.</div>
              </div>
          </div>
        </section>
</template>

<script setup lang="ts">
import { useSchoolAppContext } from '../../composables/schoolAppContext'

const app = useSchoolAppContext()
</script>
