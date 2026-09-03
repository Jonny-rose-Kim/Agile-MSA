<!--
  담당 API: [Sprint 2] GET /api/orders/supplier
  담당자  : (팀원) order-service 주문 영역
-->
<template>
  <div>
    <div class="page-head">
      <div class="page-head__text">
        <h1 class="page-title">수주 관리</h1>
        <p class="page-desc">우리 공장이 받은 조달 주문입니다. (Ep-01 US2)</p>
      </div>
    </div>

    <div class="stack">
      <section class="card">
        <div class="card__body">
          <form class="filter-bar filter-bar--start" @submit.prevent="load">
            <div class="field">
              <label class="field__label" for="status">상태</label>
              <select id="status" class="select" v-model="status">
                <option value="">전체</option>
                <option value="PENDING">PENDING (결제 대기)</option>
                <option value="CONFIRMED">CONFIRMED (결제 완료)</option>
                <option value="CANCELLED">CANCELLED</option>
              </select>
            </div>
            <div class="filter-bar__actions">
              <button type="submit" class="btn" :disabled="list.loading.value">
                <span v-if="list.loading.value" class="spinner" aria-hidden="true"></span>
                조회
              </button>
            </div>
          </form>
        </div>
      </section>

      <div v-if="list.error.value" class="alert alert--error" role="alert">
        <svg class="alert__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="12" cy="12" r="9" /><path d="M12 7.5v5M12 16h.01" />
        </svg>
        {{ list.error.value }}
      </div>

      <section class="card">
        <div v-if="list.loading.value" class="skeleton-rows">
          <span class="skeleton"></span><span class="skeleton"></span><span class="skeleton"></span>
        </div>

        <template v-else>
          <div v-if="!items.length" class="empty">
            <svg class="empty__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M6 4h12l1.5 16H4.5z" /><path d="M9 8a3 3 0 0 0 6 0" />
            </svg>
            <p class="empty__title">수주 내역이 없습니다.</p>
            <p class="empty__desc">구매처가 조달을 신청하면 이 목록에 표시됩니다.</p>
          </div>

          <div v-else class="card__body card__body--flush">
            <table class="table">
              <thead>
                <tr>
                  <th>주문번호</th><th>구매처</th><th>원료명</th><th class="num">수량</th>
                  <th>상태</th><th>신청일</th><th><span class="sr-only">동작</span></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="o in items" :key="o.orderId ?? o.id" :data-status="o.status">
                  <td data-label="주문번호" class="u-mono">#{{ o.orderId ?? o.id }}</td>
                  <td data-label="구매처" class="u-strong">{{ o.buyerName ?? '-' }}</td>
                  <td data-label="원료명">{{ o.materialName ?? '-' }}</td>
                  <td data-label="수량" class="num">{{ formatNumber(o.quantity) }}</td>
                  <td data-label="상태"><span class="badge" :data-status="o.status">{{ o.status }}</span></td>
                  <td data-label="신청일" class="u-muted u-sm">{{ formatDate(o.createdAt) }}</td>
                  <td class="actions-cell">
                    <router-link :to="`/orders/${o.orderId ?? o.id}`"
                                 class="btn btn--secondary btn--sm">상세</router-link>
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
import { computed, ref, onMounted } from 'vue'
import { orderApi } from '@/api/order.js'
import { useAsync } from '@/composables/useAsync.js'

const list = useAsync(orderApi.supplier, { initial: { content: [] } })
const status = ref('')
const items = computed(() => list.data.value?.content ?? list.data.value ?? [])

async function load() {
  await list.run({ status: status.value || undefined, page: 0, size: 50 })
}

const formatNumber = (v) => (v == null ? '-' : Number(v).toLocaleString('ko-KR'))
const formatDate = (v) => (v ? new Date(v).toLocaleString('ko-KR') : '-')

onMounted(load)
</script>
