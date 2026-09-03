<!--
  담당 API: POST /api/inventories  ·  GET /api/inventories/my  ·  PATCH /api/inventories/{id}/stock
  담당자  : (팀원) order-service 재고 영역
-->
<template>
  <div>
    <div class="page-head">
      <div class="page-head__text">
        <h1 class="page-title">재고 관리</h1>
        <p class="page-desc">보유 원료의 재고량과 임계치를 등록합니다. (Ep-02 US1)</p>
      </div>
    </div>

    <div class="stack">
      <!-- ===== 재고 요약 ===== -->
      <div class="tiles" v-if="items.length">
        <div class="tile">
          <p class="tile__label">등록 품목</p>
          <p class="tile__value">{{ items.length }}<span class="tile__unit">건</span></p>
        </div>
        <div class="tile">
          <p class="tile__label">부족 품목</p>
          <p class="tile__value" :class="{ 'tile__value--risk': shortCount }">
            {{ shortCount }}<span class="tile__unit">건</span>
          </p>
          <p class="tile__sub">임계치 미만으로 감지된 원료</p>
        </div>
      </div>

      <!-- ===== 재고 등록: POST /api/inventories ===== -->
      <section class="card">
        <header class="card__head">
          <div>
            <h2 class="card__title">재고 등록</h2>
            <p class="card__desc">현재 재고량이 임계치 아래로 내려가면 부족 알림이 생성됩니다.</p>
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

          <form class="filter-bar" @submit.prevent="submit">
            <div class="field">
              <label class="field__label" for="code">원료코드 <span class="req">*</span></label>
              <input id="code" class="input" v-model.trim="form.materialCode" type="text"
                     required placeholder="API-CEFA-500" />
            </div>
            <div class="field">
              <label class="field__label" for="stock">현재 재고량 <span class="req">*</span></label>
              <input id="stock" class="input" v-model.number="form.currentStock" type="number" min="0" required />
            </div>
            <div class="field">
              <label class="field__label" for="threshold">부족 임계치 <span class="req">*</span></label>
              <input id="threshold" class="input" v-model.number="form.threshold" type="number" min="0" required />
            </div>
            <div class="filter-bar__actions">
              <button type="submit" class="btn" :disabled="saving">
                <span v-if="saving" class="spinner" aria-hidden="true"></span>
                {{ saving ? '등록 중...' : '등록' }}
              </button>
            </div>
          </form>
        </div>
      </section>

      <!-- ===== 내 재고 목록: GET /api/inventories/my ===== -->
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
              <path d="M3 8.5 12 4l9 4.5v7L12 20l-9-4.5z" /><path d="M3 8.5 12 13l9-4.5M12 13v7" />
            </svg>
            <p class="empty__title">등록된 재고가 없습니다.</p>
            <p class="empty__desc">위 양식에서 보유 원료의 재고량과 임계치를 먼저 등록하세요.</p>
          </div>

          <div v-else class="card__body card__body--flush">
            <table class="table">
              <thead>
                <tr>
                  <th>원료코드</th><th>원료명</th><th class="num">현재고</th>
                  <th class="num">임계치</th><th>상태</th><th><span class="sr-only">동작</span></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="i in items" :key="i.inventoryId ?? i.id" :data-status="isShort(i) ? '부족' : '정상'">
                  <td data-label="원료코드"><span class="code-tag">{{ i.materialCode }}</span></td>
                  <td data-label="원료명">{{ i.materialName ?? '-' }}</td>

                  <!-- PATCH /api/inventories/{id}/stock : 현재고만 고치고 저장 -->
                  <td data-label="현재고">
                    <span class="inline-edit">
                      <label class="sr-only" :for="`stock-${i.inventoryId ?? i.id}`">
                        {{ i.materialCode }} 현재 재고량
                      </label>
                      <input :id="`stock-${i.inventoryId ?? i.id}`" class="input" type="number" min="0"
                             v-model.number="stockDraft[i.inventoryId ?? i.id]" />
                      <button type="button" class="btn btn--secondary btn--sm" @click="saveStock(i)">저장</button>
                    </span>
                  </td>

                  <td data-label="임계치" class="num">{{ formatNumber(i.threshold) }}</td>
                  <td data-label="상태">
                    <span class="badge" :data-status="isShort(i) ? '부족' : '정상'">
                      {{ isShort(i) ? '부족' : '정상' }}
                    </span>
                  </td>
                  <td class="actions-cell">
                    <router-link v-if="isShort(i)" :to="`/recommend?materialCode=${i.materialCode}`"
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
import { computed, reactive, ref, watch, onMounted } from 'vue'
import { inventoryApi } from '@/api/inventory.js'
import { useAsync } from '@/composables/useAsync.js'
import { errorMessage } from '@/api/index.js'

const list = useAsync(inventoryApi.my, { initial: { content: [] } })
const stockDraft = reactive({})
const saving = ref(false)
const createError = ref('')

const form = reactive({ materialCode: '', currentStock: null, threshold: null })
const items = computed(() => list.data.value?.content ?? list.data.value ?? [])
const shortCount = computed(() => items.value.filter(isShort).length)

watch(items, (rows) => {
  rows.forEach((i) => { stockDraft[i.inventoryId ?? i.id] = i.currentStock })
})

async function load() {
  await list.run({ page: 0, size: 50 })
}

async function submit() {
  saving.value = true
  createError.value = ''
  try {
    await inventoryApi.create({ ...form })
    form.materialCode = ''
    form.currentStock = null
    form.threshold = null
    await load()
  } catch (e) {
    createError.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}

async function saveStock(i) {
  const id = i.inventoryId ?? i.id
  try {
    await inventoryApi.updateStock(id, stockDraft[id])
    await load()
  } catch (e) {
    list.error.value = errorMessage(e)
  }
}

function isShort(i) { return Number(i.currentStock) < Number(i.threshold) }
const formatNumber = (v) => (v == null ? '-' : Number(v).toLocaleString('ko-KR'))

onMounted(load)
</script>
