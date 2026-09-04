<template>
  <div>
    <div class="page-head">
      <div class="page-head__text">
        <h1 class="page-title">마이페이지</h1>
        <p class="page-desc">로그인한 계정 정보와 역할을 확인합니다.</p>
      </div>
      <div class="page-head__actions">
        <button type="button" class="btn btn--secondary" @click="refresh" :disabled="loading">
          <svg class="btn__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M20 11a8 8 0 1 0-.7 4.4M20 5v6h-6" />
          </svg>
          새로고침
        </button>
        <button type="button" class="btn btn--danger" @click="auth.logout()">로그아웃</button>
      </div>
    </div>

    <div v-if="error" class="alert alert--error u-mt-4" role="alert">
      <svg class="alert__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
           stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <circle cx="12" cy="12" r="9" /><path d="M12 7.5v5M12 16h.01" />
      </svg>
      {{ error }}
    </div>

    <div class="stack">
      <!-- ===== 내 정보: GET /api/users/me ===== -->
      <section class="card">
        <header class="card__head">
          <div>
            <h2 class="card__title">내 정보</h2>
            <p class="card__desc">GET /api/users/me</p>
          </div>
          <span v-if="auth.user" class="badge" :data-status="auth.user.role">
            {{ auth.isSupplier ? '공급 공장' : '제약사 · 연구실' }}
          </span>
        </header>

        <div v-if="loading" class="skeleton-rows">
          <span class="skeleton"></span><span class="skeleton"></span><span class="skeleton"></span>
        </div>

        <div v-else-if="auth.user" class="card__body">
          <dl class="kv">
            <dt>사용자 ID</dt>
            <dd class="u-mono">{{ auth.user.id }}</dd>

            <dt>이름</dt>
            <dd class="u-strong">{{ auth.user.name }}</dd>

            <dt>이메일</dt>
            <dd>{{ auth.user.email }}</dd>

            <dt>계정 유형</dt>
            <dd class="u-row">
              <span class="badge" :data-status="auth.user.role">{{ auth.user.role }}</span>
              <span class="u-muted">{{ auth.isSupplier ? '공급 공장' : '제약사 · 연구실' }}</span>
            </dd>

            <dt>소속</dt>
            <dd>{{ auth.user.companyName ?? '-' }}</dd>

            <template v-if="auth.isSupplier">
              <dt>GMP 인증</dt>
              <dd>
                <span class="badge" :data-status="auth.user.gmpCertified ? 'ACTIVE' : 'INACTIVE'">
                  {{ auth.user.gmpCertified ? '보유' : '미보유' }}
                </span>
              </dd>
            </template>

            <dt>가입일</dt>
            <dd>{{ formatDate(auth.user.createdAt) }}</dd>
          </dl>
        </div>
      </section>

      <!-- ===== 공급사 조회: GET /api/users/{id} ===== -->
      <section class="card">
        <header class="card__head">
          <div>
            <h2 class="card__title">공급사 정보 조회</h2>
            <p class="card__desc">원료 상세 화면에서 공급사 정보를 표시할 때 쓰는 API입니다.</p>
          </div>
        </header>

        <div class="card__body stack">
          <form class="filter-bar filter-bar--start" @submit.prevent="lookup">
            <div class="field">
              <label class="field__label" for="lookupId">사용자 ID</label>
              <input id="lookupId" class="input" v-model.number="lookupId" type="number" min="1" placeholder="3" />
          <span class="field__hint">조회를 누르면 해당 계정의 소속·인증 정보를 가져옵니다.</span>
            </div>
            <div class="filter-bar__actions">
              <button type="submit" class="btn" :disabled="!lookupId || lookupUser.loading.value">
                <span v-if="lookupUser.loading.value" class="spinner" aria-hidden="true"></span>
                조회
              </button>
            </div>
          </form>

          <div v-if="lookupUser.error.value" class="alert alert--error" role="alert">
            <svg class="alert__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="9" /><path d="M12 7.5v5M12 16h.01" />
            </svg>
            {{ lookupUser.error.value }}
          </div>

          <dl v-if="lookupUser.data.value" class="kv">
            <dt>이름</dt>
            <dd class="u-strong">{{ lookupUser.data.value.name }}</dd>

            <dt>계정 유형</dt>
            <dd><span class="badge" :data-status="lookupUser.data.value.role">{{ lookupUser.data.value.role }}</span></dd>

            <dt>소속</dt>
            <dd>{{ lookupUser.data.value.companyName ?? '-' }}</dd>

            <dt>GMP 인증</dt>
            <dd>
              <span class="badge" :data-status="lookupUser.data.value.gmpCertified ? 'ACTIVE' : 'INACTIVE'">
                {{ lookupUser.data.value.gmpCertified ? '보유' : '미보유' }}
              </span>
            </dd>
          </dl>
        </div>
      </section>
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

// 값만 채워 두고 조회하지는 않는다. "조회"를 눌렀을 때 동작을 볼 수 있어야 한다.
// 3번은 시드 계정 중 소속(한국API공장)이 채워진 공급 공장이라 결과가 잘 보인다.
const lookupId = ref(3)
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
