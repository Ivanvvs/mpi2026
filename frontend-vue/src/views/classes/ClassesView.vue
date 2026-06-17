<template>
        <section class="panel">
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
              <tr v-for="schoolClass in app.classes" :key="schoolClass.id">
                <td>{{ schoolClass.name }}</td>
                <td>{{ schoolClass.rank }}</td>
                <td>{{ schoolClass.sPoints }}</td>
              </tr>
            </tbody>
          </table>
        </section>
</template>

<script setup lang="ts">
import { useSchoolAppContext } from '../../composables/schoolAppContext'

const app = useSchoolAppContext()
</script>
