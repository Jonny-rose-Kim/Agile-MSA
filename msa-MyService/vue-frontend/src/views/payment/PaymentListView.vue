<!--
  담당 API: [Sprint 2] POST /api/payments  ·  GET /api/payments/my
  담당자  : (팀원) payment-service
-->
<template>
  <div>
    <h1 class="page-title">결제 내역</h1>
    <p class="page-desc">GET /api/payments/my — 조달 주문에 대한 결제 내역입니다.</p>

    <div class="todo-box">
      <h4>담당: /api/payments (팀원) · Sprint 2</h4>
      <ul>
        <li>Sprint 1에서는 주문이 PENDING에서 멈추고, 결제 연동은 Sprint 2에서 붙습니다.</li>
        <li>API 함수는 <code>src/api/payment.js</code>에 준비되어 있습니다.</li>
      </ul>
    </div>

    <!-- ===== 결제 요청 ===== -->
    <div class="section">
      <h2 class="section-title">결제 요청 — POST /api/payments</h2>
      <div v-if="payError" class="error-msg">{{ payError }}</div>
      <div v-if="paySuccess" class="success-msg">{{ paySuccess }}</div>
      <form class="row" @submit.prevent="pay">
        <div class="form-group">
          <label class="form-label" for="orderId">주문번호 *</label>
          <input id="orderId" v-model.number="orderId" type="number" min="1" required />
        </div>
        <button type="submit" :disabled="paying">{{ paying ? '결제 중...' : '결제하기' }}</button>
      </form>
    </div>

    <!-- ===== 결제 내역 ===== -->
    <div v-if="list.error.value" class="error-msg">{{ list.error.value }}</div>
    <div v-if="list.loading.value" class="loading">불러오는 중...</div>

    <template v-else>
      <div v-if="!items.length" class="empty">결제 내역이 없습니다.</div>
      <table v-else>
        <thead>
          <tr><th>결제번호</th><th>주문번호</th><th>금액</th><th>상태</th><th>거래ID</th><th>결제일</th></tr>
        </thead>
        <tbody>
          <tr v-for="p in items" :key="p.paymentId ?? p.id">
            <td>{{ p.paymentId ?? p.id }}</td>
            <td>{{ p.orderId }}</td>
            <td>{{ formatNumber(p.amount) }} 원</td>
            <td><span class="badge">{{ p.status }}</span></td>
            <td>{{ p.transactionId ?? '-' }}</td>
            <td>{{ formatDate(p.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
    </template>
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
