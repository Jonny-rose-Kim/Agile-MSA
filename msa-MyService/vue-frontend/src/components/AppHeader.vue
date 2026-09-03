<template>
  <header class="app-header">
    <router-link :to="auth.isAuthenticated ? auth.homeRoute : '/'" class="logo">
      원료의약품 수급 매칭
    </router-link>

    <!-- 역할별 메뉴: auth 스토어의 isBuyer / isSupplier만 보고 결정한다 -->
    <nav v-if="auth.isAuthenticated" class="nav">
      <router-link to="/materials">원료 카탈로그</router-link>

      <template v-if="auth.isBuyer">
        <router-link to="/inventories">재고 관리</router-link>
        <router-link to="/inventories/alerts">부족 알림</router-link>
        <router-link to="/orders">조달 주문</router-link>
        <router-link to="/payments">결제 내역</router-link>
      </template>

      <template v-if="auth.isSupplier">
        <router-link to="/supplier/materials">내 공급 품목</router-link>
        <router-link to="/supplier/orders">수주 관리</router-link>
      </template>

      <router-link to="/recommend">AI 추천</router-link>
    </nav>

    <div class="user-area">
      <template v-if="auth.isAuthenticated">
        <router-link to="/mypage" class="user-name">
          {{ auth.user?.name }} ({{ roleLabel }})
        </router-link>
        <button type="button" @click="auth.logout()">로그아웃</button>
      </template>
      <router-link v-else to="/login">로그인</router-link>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/store/auth.js'

const auth = useAuthStore()

const roleLabel = computed(() => {
  if (auth.isBuyer) return '제약사·연구실'
  if (auth.isSupplier) return '공급 공장'
  return '-'
})
</script>
