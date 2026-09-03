<template>
  <div>
    <h1 class="page-title">마이페이지</h1>
    <p class="page-desc">GET /api/users/me 로 조회한 내 계정 정보입니다.</p>

    <div v-if="error" class="error-msg">{{ error }}</div>

    <!-- ===== 내 정보 ===== -->
    <div class="section">
      <h2 class="section-title">내 정보</h2>
      <div v-if="loading" class="loading">불러오는 중...</div>
      <table v-else-if="auth.user">
        <tbody>
          <tr><th style="width:160px">사용자 ID</th><td>{{ auth.user.id }}</td></tr>
          <tr><th>이름</th><td>{{ auth.user.name }}</td></tr>
          <tr><th>이메일</th><td>{{ auth.user.email }}</td></tr>
          <tr>
            <th>계정 유형</th>
            <td>
              <span class="badge">{{ auth.user.role }}</span>
              {{ auth.isSupplier ? '공급 공장' : '제약사 · 연구실' }}
            </td>
          </tr>
          <tr><th>소속</th><td>{{ auth.user.companyName ?? '-' }}</td></tr>
          <tr v-if="auth.isSupplier">
            <th>GMP 인증</th>
            <td>{{ auth.user.gmpCertified ? '보유' : '미보유' }}</td>
          </tr>
          <tr><th>가입일</th><td>{{ formatDate(auth.user.createdAt) }}</td></tr>
        </tbody>
      </table>
      <div class="actions">
        <button type="button" class="secondary" @click="refresh" :disabled="loading">새로고침</button>
        <button type="button" class="danger" @click="auth.logout()">로그아웃</button>
      </div>
    </div>

    <!-- ===== 공급사 조회: GET /api/users/{id} ===== -->
    <div class="section">
      <h2 class="section-title">공급사 정보 조회</h2>
      <p class="page-desc">원료 상세 화면에서 공급사 정보를 표시할 때 쓰는 API입니다.</p>
      <div class="row">
        <div class="form-group">
          <label class="form-label" for="lookupId">사용자 ID</label>
          <input id="lookupId" v-model.number="lookupId" type="number" min="1" placeholder="7" />
        </div>
        <button type="button" @click="lookup" :disabled="!lookupId || lookupUser.loading.value">조회</button>
      </div>

      <div v-if="lookupUser.error.value" class="error-msg">{{ lookupUser.error.value }}</div>
      <table v-if="lookupUser.data.value">
        <tbody>
          <tr><th style="width:160px">이름</th><td>{{ lookupUser.data.value.name }}</td></tr>
          <tr><th>계정 유형</th><td>{{ lookupUser.data.value.role }}</td></tr>
          <tr><th>소속</th><td>{{ lookupUser.data.value.companyName ?? '-' }}</td></tr>
          <tr><th>GMP 인증</th><td>{{ lookupUser.data.value.gmpCertified ? '보유' : '미보유' }}</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/store/auth.js'
import { userApi } from '@/api/user.js'
import { useAsync } from '@/composables/useAsync.js'

const auth = useAuthStore()

const loading = ref(false)
const error = ref('')

const lookupId = ref(null)
const lookupUser = useAsync(userApi.getById)

async function refresh() {
  loading.value = true
  error.value = ''
  const res = await auth.fetchUser()
  if (!res.ok) error.value = res.error
  loading.value = false
}

async function lookup() {
  await lookupUser.run(lookupId.value)
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('ko-KR')
}

onMounted(() => {
  if (!auth.user) refresh()
})
</script>
