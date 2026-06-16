<template>
        <section class="examiner-home panel">
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
              <tr v-for="row in app.examinerHomeRows" :key="row.id">
                <td>{{ row.date }}</td>
                <td>{{ row.time }}</td>
                <td>{{ row.exam }}</td>
                <td>{{ row.subject }}</td>
                <td>{{ row.classes }}</td>
                <td>{{ row.participants }}</td>
                <td><span class="outline-badge">{{ row.status }}</span></td>
              </tr>
              <tr v-if="!app.examinerHomeRows.length">
                <td colspan="7">Данных пока нет</td>
              </tr>
            </tbody>
          </table>
          <button class="secondary wide-action" @click="app.openPage('exams')">Все экзамены</button>
        </section>
</template>

<script setup lang="ts">
import { useSchoolAppContext } from '../../composables/schoolAppContext'

const app = useSchoolAppContext()
</script>
