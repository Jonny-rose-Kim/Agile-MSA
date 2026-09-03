<template>
  <div>
    <div v-if="loading" class="loading">로그인 처리 중입니다...</div>
    <div v-else-if="error" class="section">
      <div class="error-msg">{{ error }}</div>
      <div class="actions">
        <button type="button" @click="$router.replace('/login')">로그인으로 돌아가기</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth.js'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loading = ref(true)
const error = ref('')

// /callback?code=xxx 진입 시 1회: 인가코드 → 액세스 토큰 → 내 정보 조회
onMounted(async () => {
  const code = route.query.code

  if (!code) {
    error.value = '인가 코드가 없습니다. 로그인을 다시 시도해 주세요.'
    loading.value = false
    return
  }

  const res = await auth.handleCallback(code)
  loading.value = false

  if (res.ok) {
    // 역할에 따라 첫 화면이 달라진다 (BUYER: 원료 카탈로그 / SUPPLIER: 내 공급 품목)
    router.replace(auth.homeRoute)
  } else {
    error.value = res.error
  }
})
</script>
