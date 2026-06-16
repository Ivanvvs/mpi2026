<template>
    <main class="login-page">
      <section class="login-panel">
        <h1>Вход в систему</h1>
        <form class="form-stack" @submit.prevent="app.login">
          <label>
            <span>Логин или email</span>
            <input v-model.trim="app.loginForm.username" autocomplete="username" placeholder="Введите логин или email" />
          </label>
          <label>
            <span>Пароль</span>
            <input v-model="app.loginForm.password" type="password" autocomplete="current-password" placeholder="Введите пароль" />
          </label>
          <div class="inline-row split">
            <label class="check-row">
              <input v-model="app.rememberMe" type="checkbox" />
              <span>Запомнить меня</span>
            </label>
          </div>
          <button class="primary wide" type="submit" :disabled="app.loading">Войти</button>
          <p v-if="app.message.text" :class="['message', app.message.kind]">{{ app.message.text }}</p>
        </form>
      </section>
    </main>
</template>

<script setup lang="ts">
import { useSchoolAppContext } from '../../composables/schoolAppContext'

const app = useSchoolAppContext()
</script>
