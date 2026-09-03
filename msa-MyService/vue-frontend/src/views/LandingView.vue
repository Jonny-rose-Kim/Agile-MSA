<template>
  <div>
    <!-- ===== 히어로 ===== -->
    <section class="hero">
      <div class="hero__inner">
        <p class="hero__eyebrow">
          <span class="dot" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="10" height="10" fill="none" stroke="#fff"
                 stroke-width="3.4" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 6 9 17l-5-5" />
            </svg>
          </span>
          제약사 · 연구실 ↔ 공급 공장
        </p>

        <h1 class="hero__title">원료의약품 수급 매칭 플랫폼</h1>

        <p class="hero__lead">
          재고 부족을 미리 감지하고, 공급 가능한 인증 공장을 즉시 찾습니다.
        </p>

        <div class="hero__cta">
          <router-link v-if="!auth.isAuthenticated" to="/login">
            <button type="button" class="btn btn--lg">
              로그인 / 회원가입
              <svg class="btn__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M5 12h14M13 6l6 6-6 6" />
              </svg>
            </button>
          </router-link>
          <router-link v-else :to="auth.homeRoute">
            <button type="button" class="btn btn--lg">
              서비스 시작하기
              <svg class="btn__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M5 12h14M13 6l6 6-6 6" />
              </svg>
            </button>
          </router-link>
        </div>
      </div>
    </section>

    <!-- ===== 역할별 안내 ===== -->
    <section class="role-cards">
      <article class="role-card" data-role="BUYER">
        <div class="role-card__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor"
               stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 9.5 12 4l9 5.5v9L12 24l-9-5.5z" opacity=".35" />
            <path d="M3 9.5 12 15l9-5.5M12 15v9" />
            <path d="M3 9.5V18l9 5.5" />
          </svg>
        </div>
        <h2 class="role-card__title">제약사 · 연구실</h2>
        <p class="role-card__text">
          보유 원료의 재고와 임계치를 등록해 부족 시점을 미리 파악합니다.
        </p>
      </article>

      <article class="role-card" data-role="SUPPLIER">
        <div class="role-card__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor"
               stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 20h18M4 20V10l5 3.5V10l5 3.5V10l5 3.5V20" />
            <path d="M8.5 16.5h1M13 16.5h1M17.5 16.5h1" />
          </svg>
        </div>
        <h2 class="role-card__title">공급 공장</h2>
        <p class="role-card__text">
          여유 생산능력을 등록해 실제 공급 가능한 물량만 노출합니다.
        </p>
      </article>
    </section>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/store/auth.js'
const auth = useAuthStore()
</script>
