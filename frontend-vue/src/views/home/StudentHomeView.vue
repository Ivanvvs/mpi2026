<template>
        <section class="student-home">
          <article class="student-stat-card">
            <div class="student-icon money">S</div>
            <span>Баланс S-очков</span>
            <strong class="green">{{ app.studentClassPoints }}</strong>
          </article>
          <article class="student-stat-card">
            <div class="student-icon exams">A</div>
            <span>Пройдено экзаменов</span>
            <strong>{{ app.myResults.length }}</strong>
          </article>
          <article class="student-stat-card">
            <div class="student-icon rank">{{ app.studentClassRank }}</div>
            <span>Класс</span>
            <strong class="orange">{{ app.studentClassRank }}</strong>
          </article>
        </section>
</template>

<script setup lang="ts">
import { useSchoolAppContext } from '../../composables/schoolAppContext'

const app = useSchoolAppContext()
</script>
