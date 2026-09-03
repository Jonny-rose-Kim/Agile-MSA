<template>
  <header class="app-header">
    <div class="app-header__inner">
      <!-- 모바일 메뉴 토글: 화면 표시 전용 상태이며 API와 무관하다 -->
      <button
        v-if="auth.isAuthenticated"
        type="button"
        class="nav-toggle"
        :aria-expanded="mobileOpen"
        aria-controls="mobile-nav"
        aria-label="메뉴 열기"
        @click="mobileOpen = !mobileOpen"
      >
        <svg v-if="!mobileOpen" viewBox="0 0 24 24" width="18" height="18" fill="none"
             stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <path d="M3.5 6.5h17M3.5 12h17M3.5 17.5h17" />
        </svg>
        <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none"
             stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <path d="M6 6l12 12M18 6L6 18" />
        </svg>
      </button>

      <router-link :to="auth.isAuthenticated ? auth.homeRoute : '/'" class="brand">
        <svg class="brand__mark" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M12 2.2 20.5 7.1v9.8L12 21.8 3.5 16.9V7.1z"
                stroke="currentColor" stroke-width="1.7" stroke-linejoin="round" />
          <circle cx="12" cy="12" r="3.1" fill="currentColor" />
        </svg>
        <span class="brand__text">원료의약품 수급 매칭</span>
      </router-link>

      <!-- 역할별 메뉴: auth 스토어의 isBuyer / isSupplier만 보고 결정한다. -->
      <nav v-if="auth.isAuthenticated" class="nav" aria-label="주요 메뉴">
        <router-link class="nav__link" to="/materials">원료 카탈로그</router-link>

        <template v-if="auth.isBuyer">
          <router-link class="nav__link" to="/inventories">재고 관리</router-link>
          <router-link class="nav__link" to="/inventories/alerts">부족 알림</router-link>
          <router-link class="nav__link" to="/orders">조달 주문</router-link>
          <router-link class="nav__link" to="/payments">결제 내역</router-link>
        </template>

        <template v-if="auth.isSupplier">
          <router-link class="nav__link" to="/supplier/materials">내 공급 품목</router-link>
          <router-link class="nav__link" to="/supplier/orders">수주 관리</router-link>
        </template>

        <span class="nav__sep" aria-hidden="true"></span>
        <router-link class="nav__link" to="/recommend">AI 추천</router-link>
      </nav>

      <div class="user-area">
        <template v-if="auth.isAuthenticated">
          <router-link to="/mypage" class="user-chip">
            <span class="avatar" :data-role="auth.user?.role" aria-hidden="true">{{ initial }}</span>
            <span class="user-chip__meta">
              <span class="user-chip__name">{{ auth.user?.name }}</span>
              <span class="user-chip__role">{{ roleLabel }}</span>
            </span>
          </router-link>
          <button type="button" class="btn btn--ghost btn--sm" @click="auth.logout()">
            <svg class="btn__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" />
            </svg>
            <span class="u-hide-sm">로그아웃</span>
          </button>
        </template>
        <router-link v-else to="/login" class="btn btn--sm">로그인</router-link>
      </div>
    </div>

    <!-- 좁은 화면용 메뉴. 데스크톱 nav와 링크 구성이 완전히 동일하다. -->
    <nav v-if="auth.isAuthenticated && mobileOpen" id="mobile-nav" class="mobile-nav" aria-label="주요 메뉴">
      <router-link to="/materials">원료 카탈로그</router-link>

      <template v-if="auth.isBuyer">
        <p class="mobile-nav__label">구매 관리</p>
        <router-link to="/inventories">재고 관리</router-link>
        <router-link to="/inventories/alerts">부족 알림</router-link>
        <router-link to="/orders">조달 주문</router-link>
        <router-link to="/payments">결제 내역</router-link>
      </template>

      <template v-if="auth.isSupplier">
        <p class="mobile-nav__label">공급 관리</p>
        <router-link to="/supplier/materials">내 공급 품목</router-link>
        <router-link to="/supplier/orders">수주 관리</router-link>
      </template>

      <p class="mobile-nav__label">분석</p>
      <router-link to="/recommend">AI 추천</router-link>
    </nav>
  </header>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/store/auth.js'

const auth = useAuthStore()
const route = useRoute()

// 표시 전용 상태. 화면을 이동하면 자동으로 닫는다.
const mobileOpen = ref(false)
watch(() => route.fullPath, () => { mobileOpen.value = false })

const roleLabel = computed(() => {
  if (auth.isBuyer) return '제약사 · 연구실'
  if (auth.isSupplier) return '공급 공장'
  return '-'
})

const initial = computed(() => auth.user?.name?.trim()?.charAt(0) ?? '?')
</script>
