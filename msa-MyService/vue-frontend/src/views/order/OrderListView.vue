<!--
  담당 API: POST /api/orders  ·  GET /api/orders/my
  담당자  : (팀원) order-service 주문 영역
-->
<template>
  <div>
    <div class="page-head">
      <div class="page-head__text">
        <h1 class="page-title">조달 주문</h1>
        <p class="page-desc">조달을 신청하면 주문이 PENDING 상태로 생성됩니다.</p>
      </div>
    </div>

    <div class="stack">
      <!-- ===== 조달 신청: POST /api/orders ===== -->
      <section class="card">
        <header class="card__head">
          <div>
            <h2 class="card__title">조달 신청</h2>
            <p class="card__desc">원료 상세·AI 추천 화면의 "조달 신청"을 누르면 값이 자동으로 채워집니다.</p>
          </div>
        </header>
        <div class="card__body stack">
          <div v-if="createError" class="alert alert--error" role="alert">
            <svg class="alert__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="9" /><path d="M12 7.5v5M12 16h.01" />
            </svg>
            {{ createError }}
          </div>
          <div v-if="createSuccess" class="alert alert--success" role="status">
            <svg class="alert__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="9" /><path d="m8.5 12 2.5 2.5 4.5-5" />
            </svg>
            {{ createSuccess }}
          </div>

          <form class="filter-bar" @submit.prevent="submit">
            <div class="field">
              <label class="field__label" for="materialId">원료 ID <span class="req">*</span></label>
              <input id="materialId" class="input" v-model.number="form.materialId"
                     type="number" min="1" required />
            </div>
            <div class="field">
              <label class="field__label" for="quantity">수량 <span class="req">*</span></label>
              <input id="quantity" class="input" v-model.number="form.quantity" type="number" min="1" required />
            </div>
            <div class="field">
              <label class="field__label" for="requiredDate">납기 희망일</label>
              <input id="requiredDate" class="input" v-model="form.requiredDate" type="date" />
            </div>
            <div class="filter-bar__actions">
              <button type="submit" class="btn" :disabled="saving">
                <span v-if="saving" class="spinner" aria-hidden="true"></span>
                {{ saving ? '신청 중...' : '조달 신청' }}
              </button>
            </div>
          </form>
        </div>
      </section>

      <!-- ===== 내 주문 목록: GET /api/orders/my ===== -->
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
            <p class="empty__title">조달 주문 내역이 없습니다.</p>
            <p class="empty__desc">위 양식에서 원료 ID와 수량을 입력해 조달을 신청하세요.</p>
          </div>

          <div v-else class="card__body card__body--flush">
            <table class="table">
              <thead>
                <tr>
                  <th>주문번호</th><th>원료명</th><th class="num">수량</th><th class="num">금액</th>
                  <th>상태</th><th>신청일</th><th><span class="sr-only">동작</span></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="o in items" :key="o.orderId ?? o.id" :data-status="o.status">
                  <td data-label="주문번호" class="u-mono">#{{ o.orderId ?? o.id }}</td>
                  <td data-label="원료명" class="u-strong">{{ o.materialName ?? '-' }}</td>
                  <td data-label="수량" class="num">{{ formatNumber(o.quantity) }}</td>
                  <td data-label="금액" class="num">{{ formatNumber(o.totalAmount) }} 원</td>
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
