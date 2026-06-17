<template>
        <section class="admin-home">
          <div class="admin-metrics">
            <article class="metric compact-metric">
              <span>Баланс S-очков</span>
              <strong>{{ app.totalSPoints }}</strong>
            </article>
            <article class="metric compact-metric">
              <span>Классов</span>
              <strong>{{ app.classes.length }}</strong>
            </article>
            <article class="metric compact-metric">
              <span>Учеников</span>
              <strong>{{ app.studentCount }}</strong>
            </article>
            <article class="metric compact-metric">
              <span>Активные экзамены</span>
              <strong>{{ app.activeExams.length }}</strong>
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
                    <th>Текущий ранг</th>
                    <th>Новый ранг</th>
                    <th>Учеников</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(schoolClass, index) in app.rankedClasses" :key="schoolClass.name">
                    <td>{{ index + 1 }}</td>
                    <td>{{ schoolClass.name }}</td>
                    <td>{{ app.formatNumber(schoolClass.sPoints) }}</td>
                    <td>{{ schoolClass.rank }}</td>
                    <td :class="schoolClass.rankChangeRequired ? 'positive' : ''">{{ schoolClass.proposedRank || schoolClass.rank }}</td>
                    <td>{{ schoolClass.studentCount ?? '-' }}</td>
                  </tr>
                </tbody>
              </table>
              <div class="quick-actions">
                <button class="secondary wide-action" @click="app.refreshRankPreview">Обновить ранги</button>
                <button
                  v-if="app.rankPreviewVisible && app.pendingRankUpdates.length"
                  class="primary wide-action"
                  @click="app.confirmRankUpdates"
                >
                  Подтвердить ранги
                </button>
              </div>
              <p v-if="app.rankPreviewVisible && !app.pendingRankUpdates.length" class="muted">Изменений рангов не требуется.</p>
              <table v-if="app.rankPreviewVisible && app.pendingRankUpdates.length" class="ranking-table">
                <thead>
                  <tr>
                    <th>Класс</th>
                    <th>S-очки</th>
                    <th>Было</th>
                    <th>Станет</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="schoolClass in app.pendingRankUpdates" :key="schoolClass.id">
                    <td>{{ schoolClass.name }}</td>
                    <td>{{ app.formatNumber(schoolClass.sPoints) }}</td>
                    <td>{{ schoolClass.rank }}</td>
                    <td class="positive">{{ schoolClass.proposedRank }}</td>
                  </tr>
                </tbody>
              </table>
            </section>

            <aside class="admin-side">
              <div class="panel slim-panel">
                <h2>Недавние действия</h2>
                <ul class="recent-list">
                  <li v-for="item in app.recentActions" :key="item">{{ item }}</li>
                  <li v-if="!app.recentActions.length" class="muted">Данных пока нет</li>
                </ul>
              </div>
              <div class="panel slim-panel">
                <h2>Быстрые действия</h2>
                <div class="quick-actions">
                  <button class="secondary" @click="app.openPage('users')">+ Создать пользователя</button>
                  <button class="secondary" @click="app.openPage('violations')">Журнал нарушений</button>
                </div>
              </div>
            </aside>
          </div>
        </section>
</template>

<script setup lang="ts">
import { useSchoolAppContext } from '../../composables/schoolAppContext'

const app = useSchoolAppContext()
</script>
