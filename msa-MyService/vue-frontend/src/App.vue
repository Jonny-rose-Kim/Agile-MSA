<template>
  <div class="app">
    <AppHeader />

    <!-- 랜딩·로그인·콜백은 화면 전체를 쓰는 레이아웃(flush)을 사용한다. -->
    <main :class="['app-main', { 'app-main--flush': $route.meta.flush }]">
      <router-view v-slot="{ Component }">
        <component :is="Component" />
      </router-view>
    </main>
  </div>
</template>

<script setup>
import AppHeader from '@/components/AppHeader.vue'
</script>
