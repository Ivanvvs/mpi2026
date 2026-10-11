<template>
  <section class="admin-home">
    <div class="admin-metrics">
      <article class="metric compact-metric"><span>Баланс S-очков</span><strong>{{ app.totalSPoints }}</strong></article>
      <article class="metric compact-metric"><span>Классов</span><strong>{{ app.classes.length }}</strong></article>
      <article class="metric compact-metric"><span>Учеников</span><strong>{{ app.studentCount }}</strong></article>
      <article class="metric compact-metric"><span>Активные экзамены</span><strong>{{ app.activeExams.length }}</strong></article>
    </div>
    <div class="admin-main-grid">
      <section>
        <h2>Рейтинг классов</h2>
        <table class="ranking-table">
          <thead><tr><th>Место</th><th>Класс</th><th>S-очки</th><th>Текущий ранг</th><th>Новый ранг</th><th>Учеников</th><th>Действие</th></tr></thead>
          <tbody><tr v-for="(schoolClass, index) in app.rankedClasses" :key="schoolClass.name"><td>{{ index + 1 }}</td><td>{{ schoolClass.name }}</td><td>{{ app.formatNumber(schoolClass.sPoints) }}</td><td>{{ schoolClass.rank }}</td><td :class="schoolClass.rankChangeRequired ? 'positive' : ''">{{ schoolClass.proposedRank || schoolClass.rank }}</td><td>{{ schoolClass.studentCount ?? '-' }}</td><td><button v-if="schoolClass.rankChangeRequired" class="secondary compact" @click="app.selectRankUpdate(schoolClass)">Рассмотреть изменение</button></td></tr></tbody>
        </table>
        <section v-if="app.selectedRankClass" class="panel rank-update-panel">
          <h3>Обновление ранга: {{ app.selectedRankClass.name }}</h3>
          <p>S-очки: <strong>{{ app.formatNumber(app.selectedRankClass.sPoints) }}</strong></p>
          <p>Текущий ранг: <strong>{{ app.selectedRankClass.rank }}</strong></p>
          <p>Предлагаемый ранг: <strong class="positive">{{ app.selectedRankClass.proposedRank }}</strong></p>
          <div class="inline-actions"><button class="secondary" @click="app.loadRankDetails">Подробнее</button><button class="primary" @click="app.confirmRankUpdate">Подтвердить обновление ранга</button></div>
          <div v-if="app.rankDetails" class="rank-details">
            <p>Минимум для ранга {{ app.rankDetails.proposedRank }}: {{ app.rankDetails.proposedRankMinimumSPoints }}</p>
            <p>Сумма S-очков активных учеников: {{ app.rankDetails.calculatedStudentsSPoints }}</p>
            <p v-if="app.rankDetails.nextHigherRank">До ранга {{ app.rankDetails.nextHigherRank }}: {{ app.rankDetails.pointsToNextHigherRank }} S-очков</p>
            <table><thead><tr><th>Ученик</th><th>S-очки</th><th>Действие</th></tr></thead><tbody>
              <template v-for="student in app.rankDetails.students" :key="student.studentId">
                <tr><td>{{ student.fullName }}</td><td>{{ student.sPoints }}</td><td><button class="secondary compact" @click="app.toggleStudentSPointHistory(student)">{{ app.selectedStudentHistoryId === student.studentId ? 'Скрыть' : 'Подробнее' }}</button></td></tr>
                <tr v-if="app.selectedStudentHistoryId === student.studentId"><td colspan="3">
                  <p v-if="app.studentSPointHistoryLoading" class="muted">Загрузка истории S-очков...</p>
                  <template v-else-if="app.studentSPointHistoryError"><p class="error">{{ app.studentSPointHistoryError }}</p><button class="secondary compact" @click="app.retryStudentSPointHistory(student)">Повторить</button></template>
                  <template v-else-if="app.studentSPointHistory">
                    <h4>Результаты экзаменов</h4>
                    <p v-if="!app.studentSPointHistory.examResults.length" class="muted">Проверенных экзаменов пока нет.</p>
                    <table v-else><thead><tr><th>Экзамен</th><th>Дата проверки</th><th>Результат</th></tr></thead><tbody>
                      <tr v-for="result in app.studentSPointHistory.examResults" :key="result.examId"><td>{{ result.examTitle }}<span v-if="result.subject"> — {{ result.subject }}</span></td><td>{{ app.formatDateTime(result.gradedAt) }}</td><td>{{ result.finalScore }} / {{ result.maxScore }}</td></tr>
                      <tr><td colspan="2"><strong>Итого</strong></td><td><strong>{{ app.studentSPointHistory.examResultsTotal }}</strong></td></tr>
                    </tbody></table>
                    <h4>История S-очков</h4>
                    <p>Текущий баланс S-очков: <strong>{{ app.studentSPointHistory.totalSPoints }}</strong></p>
                    <p v-if="!app.studentSPointHistory.transactions.length" class="muted">Операций начисления или списания пока нет.</p>
                    <table v-else><thead><tr><th>Источник</th><th>Изменение</th><th>Дата</th><th>Баланс после операции</th></tr></thead><tbody>
                      <tr v-for="transaction in app.studentSPointHistory.transactions" :key="`${transaction.operationAt}-${transaction.resultingBalance}`"><td>{{ transaction.description }}<span v-if="transaction.examTitle">: {{ transaction.examTitle }}<span v-if="transaction.examSubject"> — {{ transaction.examSubject }}</span></span></td><td :class="transaction.pointsDelta >= 0 ? 'positive' : ''">{{ transaction.pointsDelta > 0 ? '+' : '' }}{{ transaction.pointsDelta }}</td><td>{{ app.formatDateTime(transaction.operationAt) }}</td><td>{{ transaction.resultingBalance }}</td></tr>
                    </tbody></table>
                  </template>
                </td></tr>
              </template>
            </tbody></table>
          </div>
        </section>
      </section>
      <aside class="admin-side"><div class="panel slim-panel"><h2>Недавние действия</h2><ul class="recent-list"><li v-for="item in app.recentActions" :key="item">{{ item }}</li><li v-if="!app.recentActions.length" class="muted">Данных пока нет</li></ul></div><div class="panel slim-panel"><h2>Быстрые действия</h2><div class="quick-actions"><button class="secondary" @click="app.openPage('users')">+ Создать пользователя</button><button class="secondary" @click="app.openPage('violations')">Журнал нарушений</button></div></div></aside>
    </div>
  </section>
</template>
<script setup lang="ts">
import { useSchoolAppContext } from '../../composables/schoolAppContext'
const app = useSchoolAppContext()
</script>
