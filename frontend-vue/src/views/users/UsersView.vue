<template>
        <section class="stack">
          <div class="panel register-panel">
            <h2>Создание нового пользователя</h2>
            <form class="registration-form" @submit.prevent="app.registerUser">
              <label>
                <span>ФИО</span>
                <input v-model.trim="app.registerForm.fullName" placeholder="Введите ФИО полностью" required />
              </label>
              <label>
                <span>Роль</span>
                <select v-model="app.registerForm.role">
                  <option value="STUDENT">Ученик</option>
                  <option value="EXAMINER">Экзаменатор-модератор</option>
                  <option value="CURATOR">Куратор класса</option>
                  <option value="ADMIN">Администратор</option>
                </select>
              </label>
              <label>
                <span>Класс (для учеников)</span>
                <select v-model.number="app.registerForm.classId" :disabled="app.registerForm.role !== 'STUDENT'">
                  <option :value="null">Выберите класс</option>
                  <option v-for="schoolClass in app.classes" :key="schoolClass.id" :value="schoolClass.id">
                    {{ schoolClass.name }}
                  </option>
                </select>
              </label>
              <label>
                <span>Дата рождения</span>
                <input v-model="app.registerForm.birthDate" type="date" />
              </label>
              <label>
                <span>Паспортные данные</span>
                <input v-model.trim="app.registerForm.passportData" placeholder="Серия, номер или иной идентификатор" />
              </label>
              <label>
                <span>Контактные данные</span>
                <input v-model.trim="app.registerForm.contactInfo" placeholder="Телефон или другой контакт" />
              </label>
              <label v-if="app.registerForm.role === 'STUDENT'">
                <span>Вступительный балл</span>
                <input v-model.number="app.registerForm.entranceExamScore" type="number" min="0" />
              </label>
              <label>
                <span>Email</span>
                <input v-model.trim="app.registerForm.email" type="email" placeholder="Введите email" required />
              </label>
              <label>
                <span>Логин</span>
                <input v-model.trim="app.registerForm.username" placeholder="Введите логин" required />
              </label>
              <label v-if="!app.editingUserId" class="password-with-action">
                <span>Пароль</span>
                <input v-model="app.registerForm.password" placeholder="Введите пароль" required />
                <button class="secondary compact" type="button" @click="app.generatePassword">Сгенерировать</button>
              </label>
              <label v-if="!app.editingUserId">
                <span>Подтверждение пароля</span>
                <input v-model="app.confirmPassword" placeholder="Повторите пароль" required />
              </label>
              <label class="check-row full">
                <input v-model="app.sendCredentials" type="checkbox" disabled />
                <span>Отправить учетные данные пользователю на email</span>
              </label>
              <div class="actions">
                <button class="primary" type="submit" :disabled="app.loading">{{ app.editingUserId ? 'Сохранить изменения' : 'Создать пользователя' }}</button>
                <button class="secondary" type="button" @click="app.resetRegisterForm">Отмена</button>
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
                    <button class="icon-button" type="button" title="Редактировать" @click="app.editUser(user)">✎</button>
                    <button class="icon-button" type="button" title="Удалить" @click="app.deactivateUser(user)">⌫</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
</template>

<script setup lang="ts">
import { useSchoolAppContext } from '../../composables/schoolAppContext'

const app = useSchoolAppContext()
</script>
