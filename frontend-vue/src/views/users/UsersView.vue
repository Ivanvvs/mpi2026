<template>
  <section class="stack">
    <div class="panel register-panel">
      <h2>Создание нового пользователя</h2>
      <form class="registration-form" @submit.prevent="app.registerUser">
        <label>
          <span>ФИО <small class="field-note required-note">обязательно</small></span>
          <input v-model.trim="app.registerForm.fullName" placeholder="Введите ФИО полностью" required />
        </label>
        <label>
          <span>Роль <small class="field-note required-note">обязательно</small></span>
          <select v-model="app.registerForm.role" required>
            <option value="STUDENT">Ученик</option>
            <option value="EXAMINER">Экзаменатор-модератор</option>
            <option value="CURATOR">Куратор класса</option>
            <option value="ADMIN">Администратор</option>
          </select>
        </label>
        <label>
          <span>
            Класс
            <small class="field-note" :class="['STUDENT', 'CURATOR'].includes(app.registerForm.role) ? 'required-note' : 'optional-note'">
              {{ ['STUDENT', 'CURATOR'].includes(app.registerForm.role) ? 'обязательно' : 'необязательно' }}
            </small>
          </span>
          <select
            v-model.number="app.registerForm.classId"
            :disabled="!['STUDENT', 'CURATOR'].includes(app.registerForm.role)"
            :required="['STUDENT', 'CURATOR'].includes(app.registerForm.role)"
          >
            <option :value="null">Выберите класс</option>
            <option v-for="schoolClass in app.classes" :key="schoolClass.id" :value="schoolClass.id">
              {{ schoolClass.name }}
            </option>
          </select>
        </label>
        <label>
          <span>Дата рождения <small class="field-note optional-note">необязательно</small></span>
          <input v-model="app.registerForm.birthDate" type="date" />
        </label>
        <label>
          <span>Паспортные данные <small class="field-note optional-note">необязательно</small></span>
          <input v-model.trim="app.registerForm.passportData" placeholder="Серия, номер или иной идентификатор" />
        </label>
        <label>
          <span>Контактные данные <small class="field-note optional-note">необязательно</small></span>
          <input v-model.trim="app.registerForm.contactInfo" placeholder="Телефон или другой контакт; если пусто, будет email" />
        </label>
        <label v-if="app.registerForm.role === 'STUDENT'">
          <span>Вступительный балл <small class="field-note optional-note">необязательно</small></span>
          <input v-model.number="app.registerForm.entranceExamScore" type="number" min="0" />
        </label>
        <label>
          <span>Email <small class="field-note required-note">обязательно</small></span>
          <input v-model.trim="app.registerForm.email" type="email" placeholder="Введите email" required />
        </label>
        <label>
          <span>Логин <small class="field-note required-note">обязательно</small></span>
          <input v-model.trim="app.registerForm.username" placeholder="Введите логин" required />
        </label>
        <label v-if="!app.editingUserId" class="password-with-action">
          <span>Пароль <small class="field-note required-note">обязательно</small></span>
          <input v-model="app.registerForm.password" placeholder="Введите пароль" required />
          <button class="secondary compact" type="button" @click="app.generatePassword">Сгенерировать</button>
        </label>
        <label v-if="!app.editingUserId">
          <span>Подтверждение пароля <small class="field-note required-note">обязательно</small></span>
          <input v-model="app.confirmPassword" placeholder="Повторите пароль" required />
        </label>
        <label class="check-row full">
          <input v-model="app.sendCredentials" type="checkbox" disabled />
          <span>Отправить учетные данные пользователю на email</span>
        </label>
        <div class="actions">
          <button class="primary" type="submit" :disabled="app.loading">
            {{ app.editingUserId ? 'Сохранить изменения' : 'Создать пользователя' }}
          </button>
          <button class="secondary" type="button" @click="app.resetRegisterForm">Отмена</button>
          <span v-if="app.userSaveSuccess" class="save-success" aria-live="polite" title="Сохранено">✓</span>
        </div>
      </form>
    </div>

    <div class="panel">
      <h2>Список пользователей</h2>
      <div class="tabs">
        <button
          v-for="filter in app.userFilters"
          :key="filter.value"
          :class="{ active: app.userRoleFilter === filter.value }"
          @click="app.userRoleFilter = filter.value"
        >
          {{ filter.label }}
        </button>
      </div>
      <table>
        <thead>
          <tr>
            <th>ФИО</th>
            <th>Роль</th>
            <th>Класс</th>
            <th>Логин</th>
            <th>Email</th>
            <th>Статус</th>
            <th>Дата создания</th>
            <th>Действия</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in app.filteredUsers" :key="user.id">
            <td>{{ user.fullName }}</td>
            <td>{{ app.roleLabel(user.role) }}</td>
            <td>{{ user.className || '-' }}</td>
            <td>{{ user.username }}</td>
            <td>{{ user.email }}</td>
            <td><span class="status ok"></span>{{ user.active ? 'Активен' : 'Отключен' }}</td>
            <td>{{ app.formatDateTime(user.createdAt) }}</td>
            <td class="table-actions">
              <button class="icon-button" type="button" title="Редактировать" @click="app.editUser(user)">Edit</button>
              <button
                v-if="user.role === 'STUDENT'"
                class="icon-button"
                type="button"
                title="Исключить студента"
                @click="app.openExpulsionPlaceholder(user)"
              >
                Off
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="app.expulsionCandidate" class="panel expulsion-placeholder">
      <div>
        <h2>Форма для исключения студента</h2>
        <p>{{ app.expulsionCandidate.fullName }}</p>
      </div>
      <button class="secondary" type="button" @click="app.closeExpulsionPlaceholder">Отмена</button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useSchoolAppContext } from '../../composables/schoolAppContext'

const app = useSchoolAppContext()
</script>
