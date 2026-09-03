<template>
  <div class="auth-shell">
    <!-- ===== 좌측 브랜드 패널 (940px 이상에서만 노출) ===== -->
    <aside class="auth-aside">
      <div class="auth-aside__body">
        <h2 class="auth-aside__title">원료의약품 수급 매칭 플랫폼</h2>
        <p class="auth-aside__text">
          재고 부족을 미리 감지하고, 공급 가능한 인증 공장을 즉시 찾습니다.
        </p>
        <ul class="auth-aside__list">
          <li>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"
                 stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M20 6 9 17l-5-5" />
            </svg>
            보유 원료의 재고·임계치 등록
          </li>
          <li>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"
                 stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M20 6 9 17l-5-5" />
            </svg>
            여유 생산능력 기반 공급처 탐색
          </li>
          <li>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"
                 stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M20 6 9 17l-5-5" />
            </svg>
            AI 수요 예측 · 공급 리스크 추천
          </li>
        </ul>
      </div>
    </aside>

    <!-- ===== 우측 패널 ===== -->
    <div class="auth-main">
      <div class="auth-panel">
        <!-- ===== 로그인: GET /oauth2/authorize ===== -->
        <div v-if="!showRegister" class="stack">
          <div>
            <h1 class="auth-panel__title">로그인</h1>
            <p class="auth-panel__desc">계정으로 로그인합니다. 인증 서버로 이동합니다.</p>
          </div>

          <div v-if="success" class="alert alert--success" role="status">
            <svg class="alert__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="9" /><path d="m8.5 12 2.5 2.5 4.5-5" />
            </svg>
            {{ success }}
          </div>

          <div class="actions stack-sm">
            <button type="button" class="btn btn--lg btn--block" @click="auth.redirectToLogin()">
              로그인
            </button>
            <button type="button" class="btn btn--secondary btn--lg btn--block" @click="openRegister">
              회원가입
            </button>
          </div>

          <p class="u-sm u-muted u-center">
            인증 서버(Spring Authorization Server)에서 로그인 후 이 화면으로 돌아옵니다.
          </p>
        </div>

        <!-- ===== 회원가입: POST /api/users/register ===== -->
        <div v-else class="stack">
          <div>
            <h1 class="auth-panel__title">회원가입</h1>
            <p class="auth-panel__desc">계정 유형에 따라 입력 항목이 달라집니다.</p>
          </div>

          <div v-if="error" class="alert alert--error" role="alert">
            <svg class="alert__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="9" /><path d="M12 7.5v5M12 16h.01" />
            </svg>
            {{ error }}
          </div>

          <form class="stack" @submit.prevent="handleRegister">
            <!-- 계정 유형: 라디오를 카드로 표현한다 -->
            <div class="field">
              <span class="field__label">계정 유형 <span class="req">*</span></span>
              <div class="role-picker">
                <label class="role-option">
                  <input type="radio" name="role" value="BUYER" v-model="form.role" />
                  <span class="role-option__name">제약사 · 연구실</span>
                  <span class="role-option__desc">원료를 구매합니다</span>
                </label>
                <label class="role-option">
                  <input type="radio" name="role" value="SUPPLIER" v-model="form.role" />
                  <span class="role-option__name">공급 공장</span>
                  <span class="role-option__desc">원료를 공급합니다</span>
                </label>
              </div>
            </div>

            <div class="form-grid">
              <div class="field">
                <label class="field__label" for="companyName">
                  {{ form.role === 'SUPPLIER' ? '공장명' : '기관명' }} <span class="req">*</span>
                </label>
                <input id="companyName" class="input" v-model.trim="form.companyName" type="text" required
                       :placeholder="form.role === 'SUPPLIER' ? '한국API공장' : '그린제약'" />
              </div>

              <div class="field">
                <label class="field__label" for="businessNumber">사업자등록번호</label>
                <input id="businessNumber" class="input" v-model.trim="form.businessNumber"
                       type="text" placeholder="123-45-67890" />
              </div>
            </div>

            <!-- GMP 인증 여부는 공급 공장에만 해당한다 (Sprint 1 계정·권한 관리) -->
            <div v-if="form.role === 'SUPPLIER'" class="field">
              <label class="check">
                <input type="checkbox" v-model="form.gmpCertified" />
                GMP 인증 보유
              </label>
            </div>

            <div class="field">
              <label class="field__label" for="name">담당자 이름 <span class="req">*</span></label>
              <input id="name" class="input" v-model.trim="form.name" type="text" required placeholder="홍길동" />
            </div>

            <div class="field">
              <label class="field__label" for="email">이메일 <span class="req">*</span></label>
              <input id="email" class="input" v-model.trim="form.email" type="email" required
                     autocomplete="email" placeholder="user@example.com" />
            </div>

            <div class="field">
              <label class="field__label" for="password">비밀번호 <span class="req">*</span></label>
              <input id="password" class="input" v-model="form.password" type="password" required
                     minlength="8" autocomplete="new-password" />
              <span class="field__hint">8자 이상 입력하세요.</span>
            </div>

            <div class="actions stack-sm">
              <button type="submit" class="btn btn--lg btn--block" :disabled="loading">
                <span v-if="loading" class="spinner" aria-hidden="true"></span>
                {{ loading ? '가입 중...' : '회원가입' }}
              </button>
              <button type="button" class="btn btn--secondary btn--block" @click="showRegister = false">
                로그인으로
              </button>
            </div>
          </form>
        </div>
      </div>
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
