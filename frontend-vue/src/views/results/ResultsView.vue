<template>
        <section class="panel">
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
              <tr v-for="result in app.myResults" :key="result.id">
                <td>{{ result.session?.title || result.session?.id }}</td>
                <td>{{ result.finalScore }}</td>
                <td>{{ result.rankPlace || '-' }}</td>
              </tr>
              <tr v-if="!app.myResults.length">
                <td colspan="3">Результатов пока нет</td>
              </tr>
            </tbody>
          </table>
        </section>
</template>

<script setup lang="ts">
import { useSchoolAppContext } from '../../composables/schoolAppContext'

const app = useSchoolAppContext()
</script>
