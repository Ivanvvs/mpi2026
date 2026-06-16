<template>
  <div class="app-shell">
    <TopBar />
    <LoginView v-if="!app.session.token" />

    <div v-else class="workspace">
      <AppSidebar />

      <section class="content">
        <PageHeader />

        <AdminHomeView v-if="app.currentPage === 'home' && app.session.role === 'ADMIN'" />
        <StudentHomeView v-if="app.currentPage === 'home' && app.session.role === 'STUDENT'" />
        <CuratorHomeView v-if="app.currentPage === 'home' && app.session.role === 'CURATOR'" />
        <ExaminerHomeView v-if="app.currentPage === 'home' && app.session.role === 'EXAMINER'" />
        <UsersView v-if="app.currentPage === 'users'" />
        <ExamsView v-if="app.currentPage === 'exams' && app.session.role !== 'ADMIN'" />
        <VotingsView v-if="app.currentPage === 'votings' && app.session.role !== 'ADMIN'" />
        <BlankPage v-if="app.isAdminReferencePage" />
        <ResultsView v-if="app.currentPage === 'results'" />
        <ClassesView v-if="app.currentPage === 'classes'" />
        <StubPage v-if="app.isStubPage" />

        <p v-if="app.message.text && app.session.token" :class="['message', app.message.kind]">{{ app.message.text }}</p>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import AppSidebar from './components/layout/AppSidebar.vue'
import PageHeader from './components/layout/PageHeader.vue'
import TopBar from './components/layout/TopBar.vue'
import { provideSchoolApp } from './composables/schoolAppContext'
import { useSchoolApp } from './composables/useSchoolApp'
import LoginView from './views/auth/LoginView.vue'
import ClassesView from './views/classes/ClassesView.vue'
import ExamsView from './views/exams/ExamsView.vue'
import BlankPage from './views/common/BlankPage.vue'
import StubPage from './views/common/StubPage.vue'
import AdminHomeView from './views/home/AdminHomeView.vue'
import CuratorHomeView from './views/home/CuratorHomeView.vue'
import ExaminerHomeView from './views/home/ExaminerHomeView.vue'
import StudentHomeView from './views/home/StudentHomeView.vue'
import ResultsView from './views/results/ResultsView.vue'
import UsersView from './views/users/UsersView.vue'
import VotingsView from './views/votings/VotingsView.vue'

const app = useSchoolApp()
provideSchoolApp(app)
</script>
