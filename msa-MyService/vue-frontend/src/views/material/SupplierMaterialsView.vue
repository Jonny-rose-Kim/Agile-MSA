<!--
  담당 API: GET /api/materials/my  ·  PATCH /api/materials/{id}/capacity  ·  DELETE /api/materials/{id}
  담당자  : (팀원) material-service
-->
<template>
  <div>
    <div class="page-head">
      <div class="page-head__text">
        <h1 class="page-title">내 공급 품목</h1>
        <p class="page-desc">여유 생산능력은 표에서 바로 수정할 수 있습니다. (Ep-02 US2)</p>
      </div>
      <div class="page-head__actions">
        <button type="button" class="btn btn--secondary" @click="load" :disabled="list.loading.value">
          <svg class="btn__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M20 11a8 8 0 1 0-.7 4.4M20 5v6h-6" />
          </svg>
          새로고침
        </button>
        <router-link to="/materials/new" class="btn">
          <svg class="btn__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2" stroke-linecap="round" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
          공급 품목 등록
        </router-link>
      </div>
    </div>

    <div class="stack">
      <div v-if="list.error.value" class="alert alert--error" role="alert">
        <svg class="alert__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="12" cy="12" r="9" /><path d="M12 7.5v5M12 16h.01" />
        </svg>
        {{ list.error.value }}
      </div>
      <div v-if="rowMessage" class="alert alert--success" role="status">
        <svg class="alert__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="12" cy="12" r="9" /><path d="m8.5 12 2.5 2.5 4.5-5" />
        </svg>
        {{ rowMessage }}
      </div>

      <section class="card">
        <div v-if="list.loading.value" class="skeleton-rows">
          <span class="skeleton"></span><span class="skeleton"></span><span class="skeleton"></span>
        </div>

        <template v-else>
          <div v-if="!items.length" class="empty">
            <svg class="empty__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M4 7.5 12 3l8 4.5v9L12 21l-8-4.5z" /><path d="M4 7.5 12 12l8-4.5M12 12v9" />
            </svg>
            <p class="empty__title">등록한 공급 품목이 없습니다.</p>
            <p class="empty__desc">공급 품목을 등록하면 구매처의 검색 결과에 노출됩니다.</p>
            <router-link to="/materials/new" class="btn">공급 품목 등록</router-link>
          </div>

          <div v-else class="card__body card__body--flush">
            <table class="table">
              <thead>
                <tr>
                  <th>원료코드</th><th>원료명</th><th class="num">단가</th>
                  <th class="num">여유 생산능력</th><th class="num">리드타임</th><th>상태</th>
                  <th><span class="sr-only">동작</span></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="m in items" :key="m.id" :data-status="m.status">
                  <td data-label="원료코드"><span class="code-tag">{{ m.materialCode }}</span></td>
                  <td data-label="원료명">
                    <router-link :to="`/materials/${m.id}`" class="u-strong">{{ m.materialName }}</router-link>
                  </td>
                  <td data-label="단가" class="num">{{ formatNumber(m.unitPrice) }} 원</td>

                  <!-- PATCH /api/materials/{id}/capacity : 숫자만 고치고 저장 -->
                  <td data-label="여유 생산능력">
                    <span class="inline-edit">
                      <label class="sr-only" :for="`cap-${m.id}`">{{ m.materialName }} 여유 생산능력</label>
                      <input :id="`cap-${m.id}`" class="input" type="number" min="0"
                             v-model.number="capacityDraft[m.id]" />
                      <button type="button" class="btn btn--secondary btn--sm"
                              :disabled="savingId === m.id || capacityDraft[m.id] === m.availableCapacity"
                              @click="saveCapacity(m)">
                        {{ savingId === m.id ? '저장 중' : '저장' }}
                      </button>
                    </span>
                  </td>

                  <td data-label="리드타임" class="num">{{ m.leadTimeDays }}일</td>
                  <td data-label="상태"><span class="badge" :data-status="m.status">{{ m.status }}</span></td>
                  <td class="actions-cell">
                    <router-link :to="`/materials/${m.id}/edit`" class="btn btn--secondary btn--sm">수정</router-link>
                    <button type="button" class="btn btn--danger btn--sm" @click="stopSupply(m)">공급 중단</button>
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
import { materialApi } from '@/api/material.js'
import { useAsync } from '@/composables/useAsync.js'
import { errorMessage } from '@/api/index.js'

const list = useAsync(materialApi.my, { initial: { content: [] } })
const capacityDraft = reactive({})
const savingId = ref(null)
const rowMessage = ref('')

const items = computed(() => list.data.value?.content ?? [])

// 목록이 갱신되면 인라인 입력값을 서버 값으로 다시 맞춘다.
watch(items, (rows) => {
  rows.forEach((m) => { capacityDraft[m.id] = m.availableCapacity })
})

async function load() {
  rowMessage.value = ''
  await list.run({ page: 0, size: 50 })
}

async function saveCapacity(m) {
  savingId.value = m.id
  rowMessage.value = ''
  try {
    const updated = await materialApi.updateCapacity(m.id, capacityDraft[m.id])
    m.availableCapacity = updated?.availableCapacity ?? capacityDraft[m.id]
    rowMessage.value = `${m.materialName} 여유 생산능력을 저장했습니다.`
  } catch (e) {
    list.error.value = errorMessage(e)
    capacityDraft[m.id] = m.availableCapacity
  } finally {
    savingId.value = null
  }
}

async function stopSupply(m) {
  if (!confirm(`${m.materialName} 공급을 중단하시겠습니까?`)) return
  try {
    await materialApi.remove(m.id)
    await load()
  } catch (e) {
    list.error.value = errorMessage(e)
  }
}

function formatNumber(v) {
  return v == null ? '-' : Number(v).toLocaleString('ko-KR')
}

onMounted(load)
</script>
