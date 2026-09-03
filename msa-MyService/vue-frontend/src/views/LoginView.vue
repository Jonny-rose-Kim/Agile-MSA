<template>
  <div>
    <h1 class="page-title">{{ showRegister ? '회원가입' : '로그인' }}</h1>

    <!-- ===== 로그인: GET /oauth2/authorize ===== -->
    <div v-if="!showRegister" class="section">
      <p class="page-desc">계정으로 로그인합니다. 인증 서버로 이동합니다.</p>
      <div class="actions">
        <button type="button" @click="auth.redirectToLogin()">로그인</button>
        <button type="button" class="secondary" @click="openRegister">회원가입</button>
      </div>
    </div>

    <!-- ===== 회원가입: POST /api/users/register ===== -->
    <div v-else class="section">
      <div v-if="error" class="error-msg">{{ error }}</div>
      <div v-if="success" class="success-msg">{{ success }}</div>

      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label class="form-label" for="role">계정 유형 *</label>
          <select id="role" v-model="form.role" required>
            <option value="BUYER">제약사 · 연구실 (구매)</option>
            <option value="SUPPLIER">공급 공장 (판매)</option>
          </select>
        </div>

        <div class="form-group">
          <label class="form-label" for="companyName">
            {{ form.role === 'SUPPLIER' ? '공장명' : '기관명' }} *
          </label>
          <input id="companyName" v-model.trim="form.companyName" type="text" required
                 :placeholder="form.role === 'SUPPLIER' ? '한국API공장' : '그린제약'" />
        </div>

        <div class="form-group">
          <label class="form-label" for="businessNumber">사업자등록번호</label>
          <input id="businessNumber" v-model.trim="form.businessNumber" type="text" placeholder="123-45-67890" />
        </div>

        <!-- GMP 인증 여부는 공급 공장에만 해당한다 (Sprint 1 계정·권한 관리) -->
        <div v-if="form.role === 'SUPPLIER'" class="form-group">
          <label class="form-label">
            <input type="checkbox" v-model="form.gmpCertified" />
            GMP 인증 보유
          </label>
        </div>

        <div class="form-group">
          <label class="form-label" for="name">담당자 이름 *</label>
          <input id="name" v-model.trim="form.name" type="text" required placeholder="홍길동" />
        </div>

        <div class="form-group">
          <label class="form-label" for="email">이메일 *</label>
          <input id="email" v-model.trim="form.email" type="email" required placeholder="user@example.com" />
        </div>

        <div class="form-group">
          <label class="form-label" for="password">비밀번호 * (8자 이상)</label>
          <input id="password" v-model="form.password" type="password" required minlength="8" />
        </div>

        <div class="actions">
          <button type="submit" :disabled="loading">
            {{ loading ? '가입 중...' : '회원가입' }}
          </button>
          <button type="button" class="secondary" @click="showRegister = false">로그인으로</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useAuthStore } from '@/store/auth.js'

const auth = useAuthStore()

const showRegister = ref(false)
const loading = ref(false)
const error = ref('')
const success = ref('')

const form = reactive({
  role: 'BUYER',
  companyName: '',
  businessNumber: '',
  gmpCertified: false,
  name: '',
  email: '',
  password: ''
})

function openRegister() {
  showRegister.value = true
  error.value = ''
  success.value = ''
}

async function handleRegister() {
  loading.value = true
  error.value = ''
  success.value = ''

  // SUPPLIER가 아니면 gmpCertified는 보내지 않는다.
  const payload = {
    email: form.email,
    password: form.password,
    name: form.name,
    role: form.role,
    companyName: form.companyName,
    businessNumber: form.businessNumber || undefined,
    ...(form.role === 'SUPPLIER' ? { gmpCertified: form.gmpCertified } : {})
  }

  const res = await auth.register(payload)
  loading.value = false

  if (res.ok) {
    // 가입 시 토큰이 발급되지 않으므로 자동 로그인하지 않는다.
    success.value = '회원가입이 완료되었습니다. 로그인해 주세요.'
    showRegister.value = false
  } else {
    error.value = res.error
  }
}
</script>
