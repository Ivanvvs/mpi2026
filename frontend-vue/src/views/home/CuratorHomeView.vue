<template>
        <section class="curator-home">
          <div class="curator-top">
            <section class="panel balance-panel">
              <h2>Класс: {{ app.curatorClassName }}</h2>
              <p>Баланс S-очков: <strong class="green">{{ app.curatorClassPoints }}</strong></p>
            </section>
            <section class="panel quick-panel">
              <h2>Быстрые действия</h2>
              <div class="inline-actions">
                <button class="secondary" @click="app.openPage('violations')">Зафиксировать нарушение</button>
                <button class="secondary" @click="app.openPage('privileges')">Подать заявку на привилегию</button>
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
                  <tr v-for="violation in app.curatorViolations" :key="violation.date + violation.student">
                    <td>{{ violation.date }}</td>
                    <td>{{ violation.student }}</td>
                    <td>{{ violation.description }}</td>
                    <td>{{ violation.exam }}</td>
                    <td class="negative">{{ violation.points }}</td>
                  </tr>
                  <tr v-if="!app.curatorViolations.length">
                    <td colspan="5">Данных пока нет</td>
                  </tr>
                </tbody>
              </table>
              <button class="secondary wide-action" @click="app.openPage('violations')">Все нарушения</button>
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
                  <tr v-for="request in app.curatorPrivilegeRequests" :key="request.date + request.title">
                    <td>{{ request.date }}</td>
                    <td>{{ request.title }}</td>
                    <td :class="request.kind">{{ request.status }}</td>
                  </tr>
                  <tr v-if="!app.curatorPrivilegeRequests.length">
                    <td colspan="3">Данных пока нет</td>
                  </tr>
                </tbody>
              </table>
              <button class="secondary wide-action" @click="app.openPage('privileges')">Все заявки</button>
            </section>
          </div>
        </section>
</template>

<script setup lang="ts">
import { useSchoolAppContext } from '../../composables/schoolAppContext'

const app = useSchoolAppContext()
</script>
