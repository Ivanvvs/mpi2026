<template>
  <aside class="sidebar">
    <button
      v-for="item in app.currentMenu"
      :key="item.page"
      :class="{ active: app.currentPage === item.page }"
      @click="app.openPage(item.page)"
    >
      {{ item.label }}
    </button>
  </aside>
</template>

<script setup lang="ts">
import { useSchoolAppContext } from '../../composables/schoolAppContext'

const app = useSchoolAppContext()
</script>
