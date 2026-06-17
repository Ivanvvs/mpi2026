<template>
  <header class="topbar">
    <div class="brand">SCHOOL SYSTEM</div>
    <div v-if="app.session.token" class="topbar-user">
      <span>{{ app.topbarLabel }}</span>
      <button class="link-button" @click="app.logout">Выход</button>
    </div>
    <div v-else class="topbar-user">
      <span>↪ Вход</span>
    </div>
  </header>
</template>

<script setup lang="ts">
import { useSchoolAppContext } from '../../composables/schoolAppContext'

const app = useSchoolAppContext()
</script>
