<template>
        <section class="stack">
          <div v-if="app.session.role === 'CURATOR'" class="tabs voting-tabs">
            <button class="active" type="button">Создать голосование</button>
            <button type="button">Активные голосования</button>
            <button type="button">Завершенные голосования</button>
          </div>

          <div v-if="app.session.role === 'CURATOR'" class="panel voting-create-panel">
            <h2>Создание тайного голосования</h2>
            <form class="voting-create-form" @submit.prevent="app.createVoting">
              <div class="voting-fields">
                <label>
                <span>Тема голосования:</span>
                <input v-model.trim="app.votingForm.title" placeholder="Тема голосования" required />
                </label>
                <label>
                <span>Описание (необязательно):</span>
                <textarea v-model.trim="app.votingForm.description"></textarea>
                </label>
                <label>
                  <span>Класс:</span>
                  <select v-model.number="app.votingForm.classId" required>
                    <option :value="null">Выберите класс</option>
                    <option v-for="schoolClass in app.classes" :key="schoolClass.id" :value="schoolClass.id">
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
                <textarea v-model="app.votingOptionsText" placeholder="Один вариант на строку"></textarea>
                </label>
                <label>
                  <span>Длительность голосования:</span>
                  <select v-model.number="app.votingDurationMinutes">
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
            <h2>{{ app.session.role === 'CURATOR' ? 'Активные голосования' : 'Голосования' }}</h2>
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
                      <button :class="{ active: app.votingStatusFilter === 'ALL' }" type="button" @click="app.votingStatusFilter = 'ALL'">Все</button>
                      <button :class="{ active: app.votingStatusFilter === 'ACTIVE' }" type="button" @click="app.votingStatusFilter = 'ACTIVE'">Активные</button>
                      <button :class="{ active: app.votingStatusFilter === 'FINISHED' }" type="button" @click="app.votingStatusFilter = 'FINISHED'">Завершенные</button>
                    </div>
                  </td>
                </tr>
                <tr v-for="voting in app.visibleVotings" :key="voting.id">
                  <td>{{ voting.title }}</td>
                  <td><span :class="['badge', voting.status.toLowerCase()]">{{ voting.status === 'ACTIVE' ? 'Активно' : 'Завершено' }}</span></td>
                  <td>{{ voting.schoolClass?.name || '-' }}</td>
                  <td>{{ app.selectedVoting?.voting.id === voting.id && app.selectedVoting.resultsVisible ? app.totalVotes(app.selectedVoting.results) : '-' }}</td>
                  <td class="table-actions">
                    <button class="secondary compact" @click="app.openVoting(voting.id)">Просмотр</button>
                    <button v-if="app.session.role === 'CURATOR' && voting.status === 'ACTIVE'" class="danger compact" @click="app.finishVoting(voting.id)">Завершить голосование</button>
                  </td>
                </tr>
                <tr v-if="!app.visibleVotings.length">
                  <td colspan="5">Голосований пока нет</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="app.selectedVoting && app.session.role === 'STUDENT'" class="voting-student-page">
            <h2>Тайное голосование</h2>
            <div class="voting-student-top">
              <section class="panel voting-details">
                <h3>Информация о голосовании</h3>
                <dl>
                  <dt>Название:</dt>
                  <dd>{{ app.selectedVoting.voting.title }}</dd>
                  <dt>Описание:</dt>
                  <dd>{{ app.selectedVoting.voting.description || '-' }}</dd>
                  <dt>Класс:</dt>
                  <dd>{{ app.selectedVoting.voting.schoolClass?.name || '-' }}</dd>
                  <dt>Статус:</dt>
                  <dd>{{ app.selectedVoting.voting.status === 'ACTIVE' ? 'Активно' : 'Завершено' }}</dd>
                  <dt>Время окончания:</dt>
                  <dd>{{ app.formatDateTime(app.selectedVoting.voting.endsAt) }}</dd>
                </dl>
              </section>
              <section class="panel voting-timer">
                <span>До окончания голосования:</span>
                <strong>{{ app.votingRemainingLabel(app.selectedVoting.voting) }}</strong>
                <div class="progress-line"><span></span></div>
              </section>
            </div>
            <div class="anonymous-note">ⓘ Ваш голос анонимен и не может быть отслежен.</div>
            <section class="panel candidate-panel">
              <h3>Выберите вариант</h3>
              <label v-for="option in app.selectedVoting.options" :key="option.id" class="candidate-row">
                <input type="radio" name="vote-option" :value="option.id" v-model="app.selectedVoteOptionId" :disabled="app.currentVotingLocked" />
                <span>{{ option.label }}</span>
              </label>
              <div v-if="!app.selectedVoting.options.length" class="empty-state">Вариантов пока нет</div>
            </section>
            <div class="actions">
              <button class="primary" :disabled="!app.selectedVoteOptionId || app.currentVotingLocked" @click="app.submitSelectedVote">Отправить голос</button>
              <button class="secondary" type="button">Отмена</button>
              <span class="negative">Важно! После отправки изменить голос нельзя.</span>
            </div>
          </div>

          <div v-if="app.selectedVoting && app.session.role !== 'STUDENT'" class="panel voting-results-panel">
            <h2>Итоги голосований</h2>
            <table>
              <thead>
                <tr>
                  <th>Вариант</th>
                  <th>Голоса</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(count, label) in app.selectedVoting.results" :key="label">
                  <td>{{ label }}</td>
                  <td>{{ count }}</td>
                </tr>
                <tr v-if="!app.selectedVoting.resultsVisible">
                  <td colspan="2">Результаты будут доступны после завершения голосования</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
</template>

<script setup lang="ts">
import { useSchoolAppContext } from '../../composables/schoolAppContext'

const app = useSchoolAppContext()
</script>
