<!--
  담당 API: GET /api/inventories/alerts
  담당자  : (팀원) order-service 재고 영역
-->
<template>
  <div>
    <h1 class="page-title">재고 부족 알림</h1>
    <p class="page-desc">GET /api/inventories/alerts — 임계치 미만으로 감지된 원료입니다 (Ep-02 US1).</p>

    <div class="actions">
      <button type="button" class="secondary" @click="alerts.run()" :disabled="alerts.loading.value">새로고침</button>
    </div>

    <div v-if="alerts.error.value" class="error-msg">{{ alerts.error.value }}</div>
    <div v-if="alerts.loading.value" class="loading">불러오는 중...</div>

    <template v-else>
      <div v-if="!items.length" class="empty">부족 알림이 없습니다.</div>
      <table v-else>
        <thead>
          <tr><th>심각도</th><th>원료코드</th><th>원료명</th><th>현재고</th><th>임계치</th><th>부족량</th><th>감지 시각</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="a in items" :key="a.inventoryId">
            <td><span class="badge">{{ a.severity }}</span></td>
            <td>{{ a.materialCode }}</td>
            <td>{{ a.materialName }}</td>
            <td>{{ formatNumber(a.currentStock) }}</td>
            <td>{{ formatNumber(a.threshold) }}</td>
            <td>{{ formatNumber(a.shortageQuantity) }}</td>
            <td>{{ formatDate(a.detectedAt) }}</td>
            <td>
              <router-link :to="`/recommend?materialCode=${a.materialCode}&quantity=${a.shortageQuantity ?? ''}`">
                공급처 찾기
              </router-link>
            </td>
          </tr>
        </tbody>
      </table>
    </template>
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
