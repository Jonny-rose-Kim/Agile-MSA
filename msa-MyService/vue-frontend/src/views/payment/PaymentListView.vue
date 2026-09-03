<!--
  담당 API: [Sprint 2] POST /api/payments  ·  GET /api/payments/my
  담당자  : (팀원) payment-service
  참고    : Sprint 1에서는 주문이 PENDING에서 멈추고, 결제 연동은 Sprint 2에서 붙는다.
-->
<template>
  <div>
    <div class="page-head">
      <div class="page-head__text">
        <h1 class="page-title">결제 내역</h1>
        <p class="page-desc">조달 주문에 대한 결제 내역입니다.</p>
      </div>
    </div>

    <div class="stack">
      <!-- ===== 결제 요청: POST /api/payments ===== -->
      <section class="card">
        <header class="card__head">
          <div>
            <h2 class="card__title">결제 요청</h2>
            <p class="card__desc">결제가 완료되면 주문 상태가 PENDING에서 CONFIRMED로 바뀝니다.</p>
          </div>
        </header>
        <div class="card__body stack">
          <div v-if="payError" class="alert alert--error" role="alert">
            <svg class="alert__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="9" /><path d="M12 7.5v5M12 16h.01" />
            </svg>
            {{ payError }}
          </div>
          <div v-if="paySuccess" class="alert alert--success" role="status">
            <svg class="alert__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="9" /><path d="m8.5 12 2.5 2.5 4.5-5" />
            </svg>
            {{ paySuccess }}
          </div>

          <form class="filter-bar filter-bar--start" @submit.prevent="pay">
            <div class="field">
              <label class="field__label" for="orderId">주문번호 <span class="req">*</span></label>
              <input id="orderId" class="input" v-model.number="orderId" type="number" min="1" required />
            </div>
            <div class="filter-bar__actions">
              <button type="submit" class="btn" :disabled="paying">
                <span v-if="paying" class="spinner" aria-hidden="true"></span>
                {{ paying ? '결제 중...' : '결제하기' }}
              </button>
            </div>
          </form>
        </div>
      </section>

      <!-- ===== 결제 내역: GET /api/payments/my ===== -->
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
              <rect x="2.5" y="5.5" width="19" height="13" rx="2" /><path d="M2.5 10h19" />
            </svg>
            <p class="empty__title">결제 내역이 없습니다.</p>
            <p class="empty__desc">조달 주문의 주문번호로 결제를 요청하세요.</p>
          </div>

          <div v-else class="card__body card__body--flush">
            <table class="table">
              <thead>
                <tr>
                  <th>결제번호</th><th>주문번호</th><th class="num">금액</th>
                  <th>상태</th><th>거래ID</th><th>결제일</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in items" :key="p.paymentId ?? p.id" :data-status="p.status">
                  <td data-label="결제번호" class="u-mono">#{{ p.paymentId ?? p.id }}</td>
                  <td data-label="주문번호" class="u-mono">#{{ p.orderId }}</td>
                  <td data-label="금액" class="num u-strong">{{ formatNumber(p.amount) }} 원</td>
                  <td data-label="상태"><span class="badge" :data-status="p.status">{{ p.status }}</span></td>
                  <td data-label="거래ID" class="u-mono u-sm u-muted">
                    <span class="truncate" :title="p.transactionId ?? '-'">{{ p.transactionId ?? '-' }}</span>
                  </td>
                  <td data-label="결제일" class="u-muted u-sm">{{ formatDate(p.createdAt) }}</td>
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
import { paymentApi } from '@/api/payment.js'
import { useAsync } from '@/composables/useAsync.js'
import { errorMessage } from '@/api/index.js'

const list = useAsync(paymentApi.my, { initial: { content: [] } })
const orderId = ref(null)
const paying = ref(false)
const payError = ref('')
const paySuccess = ref('')

const items = computed(() => list.data.value?.content ?? list.data.value ?? [])

async function load() {
  await list.run({ page: 0, size: 50 })
}

async function pay() {
  paying.value = true
  payError.value = ''
  paySuccess.value = ''
  try {
    const res = await paymentApi.create({ orderId: orderId.value })
    paySuccess.value = `결제 요청 완료 (상태: ${res?.status ?? '-'})`
    await load()
  } catch (e) {
    payError.value = errorMessage(e)
  } finally {
    paying.value = false
  }
}

const formatNumber = (v) => (v == null ? '-' : Number(v).toLocaleString('ko-KR'))
const formatDate = (v) => (v ? new Date(v).toLocaleString('ko-KR') : '-')

onMounted(load)
</script>
