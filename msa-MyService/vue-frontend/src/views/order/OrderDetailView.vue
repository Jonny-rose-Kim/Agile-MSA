<!--
  담당 API: GET /api/orders/{id}  ·  [Sprint 2] GET /api/orders/{id}/status
  담당자  : (팀원) order-service 주문 영역
-->
<template>
  <div>
    <h1 class="page-title">주문 상세</h1>

    <div v-if="detail.error.value" class="error-msg">{{ detail.error.value }}</div>
    <div v-if="detail.loading.value" class="loading">불러오는 중...</div>

    <template v-else-if="order">
      <div class="section">
        <table>
          <tbody>
            <tr><th style="width:160px">주문번호</th><td>{{ order.orderId ?? order.id }}</td></tr>
            <tr><th>원료</th><td>{{ order.materialName ?? '-' }} ({{ order.materialCode ?? '-' }})</td></tr>
            <tr><th>수량</th><td>{{ formatNumber(order.quantity) }}</td></tr>
            <tr><th>금액</th><td>{{ formatNumber(order.totalAmount) }} 원</td></tr>
            <tr>
              <th>상태</th>
              <td>
                <span class="badge">{{ currentStatus }}</span>
                <span v-if="polling"> — 결제 완료 이벤트를 기다리는 중...</span>
              </td>
            </tr>
            <tr><th>납기 희망일</th><td>{{ order.requiredDate ?? '-' }}</td></tr>
            <tr><th>신청일</th><td>{{ formatDate(order.createdAt) }}</td></tr>
          </tbody>
        </table>

        <div class="actions">
          <button type="button" class="secondary" @click="detail.run(route.params.id)">새로고침</button>
          <router-link to="/orders"><button type="button" class="secondary">목록</button></router-link>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { orderApi } from '@/api/order.js'
import { useAsync } from '@/composables/useAsync.js'

const route = useRoute()
const detail = useAsync(orderApi.get)

const order = computed(() => detail.data.value)
const liveStatus = ref('')
const currentStatus = computed(() => liveStatus.value || order.value?.status || '-')

const FINAL_STATUSES = ['CONFIRMED', 'CANCELLED', 'FAILED']
const polling = ref(false)
let timer = null

// [Sprint 2] PENDING인 동안만 상태를 폴링한다. 종료 상태에 도달하면 멈춘다.
function startPolling() {
  if (FINAL_STATUSES.includes(currentStatus.value)) return
  polling.value = true
  timer = setInterval(async () => {
    try {
      const res = await orderApi.getStatus(route.params.id)
      liveStatus.value = res?.status ?? liveStatus.value
      if (FINAL_STATUSES.includes(liveStatus.value)) stopPolling()
    } catch {
      stopPolling()
    }
  }, 5000)
}

function stopPolling() {
  polling.value = false
  if (timer) clearInterval(timer)
  timer = null
}

const formatNumber = (v) => (v == null ? '-' : Number(v).toLocaleString('ko-KR'))
const formatDate = (v) => (v ? new Date(v).toLocaleString('ko-KR') : '-')

onMounted(async () => {
  await detail.run(route.params.id)
  if (order.value) startPolling()
})
onUnmounted(stopPolling)
</script>
