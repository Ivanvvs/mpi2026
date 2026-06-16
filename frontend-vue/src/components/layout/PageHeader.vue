<template>
  <div class="page-head">
    <div>
      <h1>{{ app.pageTitle }}</h1>
      <p>{{ app.pageSubtitle }}</p>
    </div>
    <button class="secondary" @click="app.refreshCurrent">Обновить</button>
  </div>
</template>

<script setup lang="ts">
import { useSchoolAppContext } from '../../composables/schoolAppContext'

const app = useSchoolAppContext()
</script>
