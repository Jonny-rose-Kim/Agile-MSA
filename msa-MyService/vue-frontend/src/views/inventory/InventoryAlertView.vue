<!--
  담당 API: GET /api/inventories/alerts
  담당자  : (팀원) order-service 재고 영역
-->
<template>
  <div>
    <div class="page-head">
      <div class="page-head__text">
        <h1 class="page-title">재고 부족 알림</h1>
        <p class="page-desc">임계치 미만으로 감지된 원료입니다. (Ep-02 US1)</p>
      </div>
      <div class="page-head__actions">
        <button type="button" class="btn btn--secondary" @click="alerts.run()" :disabled="alerts.loading.value">
          <svg class="btn__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M20 11a8 8 0 1 0-.7 4.4M20 5v6h-6" />
          </svg>
          새로고침
        </button>
      </div>
    </div>

    <div class="stack">
      <div v-if="alerts.error.value" class="alert alert--error" role="alert">
        <svg class="alert__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="12" cy="12" r="9" /><path d="M12 7.5v5M12 16h.01" />
        </svg>
        {{ alerts.error.value }}
      </div>

      <section class="card">
        <div v-if="alerts.loading.value" class="skeleton-rows">
          <span class="skeleton"></span><span class="skeleton"></span><span class="skeleton"></span>
        </div>

        <template v-else>
          <div v-if="!items.length" class="empty">
            <svg class="empty__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="9" /><path d="m8.5 12 2.5 2.5 4.5-5" />
            </svg>
            <p class="empty__title">부족 알림이 없습니다.</p>
            <p class="empty__desc">등록된 모든 원료가 임계치 이상을 유지하고 있습니다.</p>
          </div>

          <div v-else class="card__body card__body--flush">
            <table class="table">
              <thead>
                <tr>
                  <th>심각도</th><th>원료코드</th><th>원료명</th>
                  <th class="num">현재고</th><th class="num">임계치</th><th class="num">부족량</th>
                  <th>감지 시각</th><th><span class="sr-only">동작</span></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="a in items" :key="a.inventoryId" :data-severity="a.severity">
                  <td data-label="심각도"><span class="badge" :data-severity="a.severity">{{ a.severity }}</span></td>
                  <td data-label="원료코드"><span class="code-tag">{{ a.materialCode }}</span></td>
                  <td data-label="원료명" class="u-strong">{{ a.materialName }}</td>
                  <td data-label="현재고" class="num">{{ formatNumber(a.currentStock) }}</td>
                  <td data-label="임계치" class="num">{{ formatNumber(a.threshold) }}</td>
                  <td data-label="부족량" class="num u-strong">{{ formatNumber(a.shortageQuantity) }}</td>
                  <td data-label="감지 시각" class="u-muted u-sm">{{ formatDate(a.detectedAt) }}</td>
                  <td class="actions-cell">
                    <router-link
                      :to="`/recommend?materialCode=${a.materialCode}&quantity=${a.shortageQuantity ?? ''}`"
                      class="btn btn--sm">공급처 찾기</router-link>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { inventoryApi } from '@/api/inventory.js'
import { useAsync } from '@/composables/useAsync.js'

const alerts = useAsync(inventoryApi.alerts, { initial: { alerts: [] } })
const items = computed(() => alerts.data.value?.alerts ?? [])

const formatNumber = (v) => (v == null ? '-' : Number(v).toLocaleString('ko-KR'))
const formatDate = (v) => (v ? new Date(v).toLocaleString('ko-KR') : '-')

onMounted(() => alerts.run())
</script>
