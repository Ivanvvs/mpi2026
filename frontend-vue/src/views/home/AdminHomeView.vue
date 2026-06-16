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
                    <th>Изменение</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(schoolClass, index) in app.rankedClasses" :key="schoolClass.name">
                    <td>{{ index + 1 }}</td>
                    <td>{{ schoolClass.name }}</td>
                    <td>{{ app.formatNumber(schoolClass.sPoints) }}</td>
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
