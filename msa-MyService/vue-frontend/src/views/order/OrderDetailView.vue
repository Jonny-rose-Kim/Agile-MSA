<!--
  담당 API: GET /api/orders/{id}  ·  [Sprint 2] GET /api/orders/{id}/status
  담당자  : (팀원) order-service 주문 영역
-->
<template>
  <div>
    <div v-if="detail.error.value" class="alert alert--error" role="alert">
      <svg class="alert__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
           stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <circle cx="12" cy="12" r="9" /><path d="M12 7.5v5M12 16h.01" />
      </svg>
      {{ detail.error.value }}
    </div>

    <div v-if="detail.loading.value" class="card">
      <div class="skeleton-rows">
        <span class="skeleton"></span><span class="skeleton"></span><span class="skeleton"></span>
      </div>
    </div>

    <template v-else-if="order">
      <div class="page-head">
        <div class="page-head__text">
          <h1 class="page-title">주문 상세</h1>
          <p class="page-desc u-mono">#{{ order.orderId ?? order.id }}</p>
        </div>
        <div class="page-head__actions">
          <button type="button" class="btn btn--secondary" @click="detail.run(route.params.id)">
            <svg class="btn__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M20 11a8 8 0 1 0-.7 4.4M20 5v6h-6" />
            </svg>
            새로고침
          </button>
          <router-link to="/orders" class="btn btn--secondary">목록</router-link>
        </div>
      </div>

      <div class="stack">
        <!-- 결제 완료 이벤트를 기다리는 동안만 노출 (Sprint 2 폴링) -->
        <div v-if="polling" class="alert alert--info" role="status">
          <span class="pulse-dot" aria-hidden="true"></span>
          결제 완료 이벤트를 기다리는 중입니다. 상태가 바뀌면 자동으로 갱신됩니다.
        </div>

        <section class="card">
          <header class="card__head">
            <h2 class="card__title">주문 정보</h2>
            <span class="badge" :data-status="currentStatus">{{ currentStatus }}</span>
          </header>
          <div class="card__body">
            <dl class="kv">
              <dt>주문번호</dt>
              <dd class="u-mono">#{{ order.orderId ?? order.id }}</dd>

              <dt>원료</dt>
              <dd class="u-row">
                <span class="u-strong">{{ order.materialName ?? '-' }}</span>
                <span v-if="order.materialCode" class="code-tag">{{ order.materialCode }}</span>
              </dd>

              <dt>수량</dt>
              <dd class="u-num">{{ formatNumber(order.quantity) }}</dd>

              <dt>금액</dt>
              <dd class="u-num u-strong">{{ formatNumber(order.totalAmount) }} 원</dd>

              <dt>상태</dt>
              <dd class="u-row">
                <span class="badge" :data-status="currentStatus">{{ currentStatus }}</span>
                <span v-if="polling" class="u-muted u-sm">— 결제 완료 이벤트를 기다리는 중...</span>
              </dd>

              <dt>납기 희망일</dt>
              <dd>{{ order.requiredDate ?? '-' }}</dd>

              <dt>신청일</dt>
              <dd>{{ formatDate(order.createdAt) }}</dd>
            </dl>
          </div>
        </section>
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
