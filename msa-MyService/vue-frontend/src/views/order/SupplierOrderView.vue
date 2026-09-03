<!--
  담당 API: [Sprint 2] GET /api/orders/supplier
  담당자  : (팀원) order-service 주문 영역
-->
<template>
  <div>
    <h1 class="page-title">수주 관리</h1>
    <p class="page-desc">GET /api/orders/supplier — 우리 공장이 받은 조달 주문입니다 (Ep-01 US2).</p>

    <div class="section">
      <div class="row">
        <div class="form-group">
          <label class="form-label" for="status">상태</label>
          <select id="status" v-model="status">
            <option value="">전체</option>
            <option value="PENDING">PENDING (결제 대기)</option>
            <option value="CONFIRMED">CONFIRMED (결제 완료)</option>
            <option value="CANCELLED">CANCELLED</option>
          </select>
        </div>
        <button type="button" @click="load" :disabled="list.loading.value">조회</button>
      </div>
    </div>

    <div v-if="list.error.value" class="error-msg">{{ list.error.value }}</div>
    <div v-if="list.loading.value" class="loading">불러오는 중...</div>

    <template v-else>
      <div v-if="!items.length" class="empty">수주 내역이 없습니다.</div>
      <table v-else>
        <thead>
          <tr><th>주문번호</th><th>구매처</th><th>원료명</th><th>수량</th><th>상태</th><th>신청일</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="o in items" :key="o.orderId ?? o.id">
            <td>{{ o.orderId ?? o.id }}</td>
            <td>{{ o.buyerName ?? '-' }}</td>
            <td>{{ o.materialName ?? '-' }}</td>
            <td>{{ formatNumber(o.quantity) }}</td>
            <td><span class="badge">{{ o.status }}</span></td>
            <td>{{ formatDate(o.createdAt) }}</td>
            <td><router-link :to="`/orders/${o.orderId ?? o.id}`">상세</router-link></td>
          </tr>
        </tbody>
      </table>
    </template>
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
