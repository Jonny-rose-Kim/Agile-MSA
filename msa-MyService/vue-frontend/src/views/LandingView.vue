<template>
  <div>
    <h1 class="page-title">원료의약품 수급 매칭 플랫폼</h1>
    <p class="page-desc">
      재고 부족을 미리 감지하고, 공급 가능한 인증 공장을 즉시 찾습니다.
    </p>

    <div class="section">
      <p>제약사·연구실은 보유 원료의 재고와 임계치를 등록해 부족 시점을 미리 파악하고,</p>
      <p>공급 공장은 여유 생산능력을 등록해 실제 공급 가능한 물량만 노출합니다.</p>
      <div class="actions">
        <router-link v-if="!auth.isAuthenticated" to="/login">
          <button type="button">로그인 / 회원가입</button>
        </router-link>
        <router-link v-else :to="auth.homeRoute">
          <button type="button">서비스 시작하기</button>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/store/auth.js'
const auth = useAuthStore()
</script>
