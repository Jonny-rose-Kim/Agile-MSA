<!--
  담당 API: POST /api/orders  ·  GET /api/orders/my
  담당자  : (팀원) order-service 주문 영역
-->
<template>
  <div>
    <h1 class="page-title">조달 주문</h1>
    <p class="page-desc">POST /api/orders — 조달을 신청하면 주문이 PENDING 상태로 생성됩니다.</p>

    <!-- ===== 조달 신청 ===== -->
    <div class="section">
      <h2 class="section-title">조달 신청</h2>
      <div v-if="createError" class="error-msg">{{ createError }}</div>
      <div v-if="createSuccess" class="success-msg">{{ createSuccess }}</div>

      <form class="row" @submit.prevent="submit">
        <div class="form-group">
          <label class="form-label" for="materialId">원료 ID *</label>
          <input id="materialId" v-model.number="form.materialId" type="number" min="1" required />
        </div>
        <div class="form-group">
          <label class="form-label" for="quantity">수량 *</label>
          <input id="quantity" v-model.number="form.quantity" type="number" min="1" required />
        </div>
        <div class="form-group">
          <label class="form-label" for="requiredDate">납기 희망일</label>
          <input id="requiredDate" v-model="form.requiredDate" type="date" />
        </div>
        <button type="submit" :disabled="saving">{{ saving ? '신청 중...' : '조달 신청' }}</button>
      </form>
    </div>

    <!-- ===== 내 주문 목록 ===== -->
    <div v-if="list.error.value" class="error-msg">{{ list.error.value }}</div>
    <div v-if="list.loading.value" class="loading">불러오는 중...</div>

    <template v-else>
      <div v-if="!items.length" class="empty">조달 주문 내역이 없습니다.</div>
      <table v-else>
        <thead>
          <tr><th>주문번호</th><th>원료명</th><th>수량</th><th>금액</th><th>상태</th><th>신청일</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="o in items" :key="o.orderId ?? o.id">
            <td>{{ o.orderId ?? o.id }}</td>
            <td>{{ o.materialName ?? '-' }}</td>
            <td>{{ formatNumber(o.quantity) }}</td>
            <td>{{ formatNumber(o.totalAmount) }} 원</td>
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
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { orderApi } from '@/api/order.js'
import { useAsync } from '@/composables/useAsync.js'
import { errorMessage } from '@/api/index.js'

const route = useRoute()
const list = useAsync(orderApi.my, { initial: { content: [] } })

const saving = ref(false)
const createError = ref('')
const createSuccess = ref('')

// 원료 상세의 "조달 신청" 버튼에서 넘어온 값을 그대로 받는다.
const form = reactive({
  materialId: route.query.materialId ? Number(route.query.materialId) : null,
  quantity: route.query.quantity ? Number(route.query.quantity) : null,
  requiredDate: ''
})

const items = computed(() => list.data.value?.content ?? list.data.value ?? [])

async function load() {
  await list.run({ page: 0, size: 50 })
}

async function submit() {
  saving.value = true
  createError.value = ''
  createSuccess.value = ''
  try {
    const created = await orderApi.create({
      materialId: form.materialId,
      quantity: form.quantity,
      requiredDate: form.requiredDate || undefined
    })
    createSuccess.value = `주문 ${created?.orderId ?? ''}번이 생성되었습니다. (상태: ${created?.status ?? 'PENDING'})`
    await load()
  } catch (e) {
    createError.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}

const formatNumber = (v) => (v == null ? '-' : Number(v).toLocaleString('ko-KR'))
const formatDate = (v) => (v ? new Date(v).toLocaleString('ko-KR') : '-')

onMounted(load)
</script>
