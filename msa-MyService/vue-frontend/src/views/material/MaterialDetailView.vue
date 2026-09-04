<!--
  담당 API: GET /api/materials/{id}  ·  GET /api/materials/code/{materialCode}/suppliers
  담당자  : (팀원) material-service
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

    <template v-else-if="material">
      <div class="page-head">
        <div class="page-head__text">
          <div class="u-row">
            <span class="code-tag code-tag--lg">{{ material.materialCode }}</span>
            <span v-for="c in (material.certifications ?? [])" :key="c" class="chip">{{ c }}</span>
          </div>
          <h1 class="page-title u-mt-2">{{ material.materialName }}</h1>
          <p class="page-desc">{{ material.supplierName }}</p>
        </div>
        <div class="page-head__actions">
          <router-link v-if="auth.isSupplier" :to="`/materials/${material.id}/edit`"
                       class="btn btn--secondary">수정</router-link>
          <router-link to="/materials" class="btn btn--secondary">목록</router-link>
        </div>
      </div>

      <div class="stack">
        <!-- ===== 핵심 지표 ===== -->
        <div class="tiles">
          <div class="tile">
            <p class="tile__label">단가</p>
            <p class="tile__value">
              {{ formatNumber(material.unitPrice) }}<span class="tile__unit">원 / {{ material.unit }}</span>
            </p>
          </div>
          <div class="tile">
            <p class="tile__label">여유 생산능력</p>
            <p class="tile__value">
              {{ formatNumber(material.availableCapacity) }}<span class="tile__unit">{{ material.unit }}</span>
            </p>
          </div>
          <div class="tile">
            <p class="tile__label">리드타임</p>
            <p class="tile__value">{{ material.leadTimeDays }}<span class="tile__unit">일</span></p>
          </div>
          <div class="tile">
            <p class="tile__label">최소 주문 수량</p>
            <p class="tile__value">
              {{ formatNumber(material.minOrderQuantity) }}<span class="tile__unit">{{ material.unit }}</span>
            </p>
          </div>
        </div>

        <!-- ===== 원료 정보 ===== -->
        <section class="card">
          <header class="card__head">
            <h2 class="card__title">원료 정보</h2>
          </header>
          <div class="card__body">
            <dl class="kv">
              <dt>원료코드</dt>
              <dd><span class="code-tag">{{ material.materialCode }}</span></dd>

              <dt>CAS 번호</dt>
              <dd class="u-mono">{{ material.casNumber ?? '-' }}</dd>

              <dt>카테고리</dt>
              <dd>{{ material.category }}</dd>

              <dt>단가</dt>
              <dd class="u-num">{{ formatNumber(material.unitPrice) }} 원 / {{ material.unit }}</dd>

              <dt>최소 주문 수량</dt>
              <dd class="u-num">{{ formatNumber(material.minOrderQuantity) }} {{ material.unit }}</dd>

              <dt>여유 생산능력</dt>
              <dd class="u-num">{{ formatNumber(material.availableCapacity) }} {{ material.unit }}</dd>

              <dt>리드타임</dt>
              <dd class="u-num">{{ material.leadTimeDays }}일</dd>

              <dt>인증</dt>
              <dd>
                <div v-if="(material.certifications ?? []).length" class="chips">
                  <span v-for="c in material.certifications" :key="c" class="chip">{{ c }}</span>
                </div>
                <span v-else class="u-muted">-</span>
              </dd>

              <dt>설명</dt>
              <dd>{{ material.description ?? '-' }}</dd>
            </dl>
          </div>
        </section>

        <!-- ===== Ep-01 US1: 공급 가능 인증 공장 조회 ===== -->
        <section class="card">
          <header class="card__head">
            <div>
              <h2 class="card__title">공급 가능 인증 공장 조회</h2>
              <p class="card__desc">같은 원료를 공급할 수 있는 다른 공장을 찾습니다. (Ep-01 US1)</p>
            </div>
          </header>

          <div class="card__body stack">
            <form class="filter-bar" @submit.prevent="findSuppliers">
              <div class="field">
                <label class="field__label" for="qty">필요 수량 ({{ material.unit }})</label>
                <input id="qty" class="input" v-model.number="requiredQuantity"
                       type="number" min="1" placeholder="800" />
                <span class="field__hint">최소 주문 수량으로 채워 뒀습니다. 조회하려면 "공급처 찾기"를 누르세요.</span>
              </div>
              <div class="field">
                <label class="field__label" for="cert">인증 조건</label>
                <select id="cert" class="select" v-model="certification">
                  <option value="">전체</option>
                  <option value="GMP">GMP</option>
                  <option value="DMF">DMF</option>
                </select>
              </div>
              <div class="filter-bar__actions">
                <button type="submit" class="btn" :disabled="suppliers.loading.value">
                  <span v-if="suppliers.loading.value" class="spinner" aria-hidden="true"></span>
                  공급처 찾기
                </button>
              </div>
            </form>

            <div v-if="suppliers.error.value" class="alert alert--error" role="alert">
              <svg class="alert__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <circle cx="12" cy="12" r="9" /><path d="M12 7.5v5M12 16h.01" />
              </svg>
              {{ suppliers.error.value }}
            </div>
          </div>

          <div v-if="suppliers.loading.value" class="skeleton-rows">
            <span class="skeleton"></span><span class="skeleton"></span><span class="skeleton"></span>
          </div>

          <template v-else-if="suppliers.data.value">
            <!-- totalCount === 0 은 에러가 아니라 정상 응답이다 -->
            <div v-if="!supplierList.length" class="empty">
              <svg class="empty__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M3 20h18M4 20V10l5 3.5V10l5 3.5V10l5 3.5V20" />
              </svg>
              <p class="empty__title">현재 조건을 만족하는 인증 공장이 없습니다.</p>
              <p class="empty__desc">필요 수량을 낮춰 다시 조회해 보세요.</p>
            </div>

            <div v-else class="card__body card__body--flush">
              <table class="table">
                <thead>
                  <tr>
                    <th>공장명</th><th>국가</th><th class="num">여유 생산능력</th>
                    <th class="num">리드타임</th><th class="num">단가</th><th>인증</th>
                    <th><span class="sr-only">동작</span></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="s in supplierList" :key="s.materialId ?? s.supplierId">
                    <td data-label="공장명" class="u-strong">{{ s.supplierName }}</td>
                    <td data-label="국가" class="u-muted">{{ s.country ?? '-' }}</td>
                    <td data-label="여유 생산능력" class="num">{{ formatNumber(s.availableCapacity) }}</td>
                    <td data-label="리드타임" class="num">{{ s.leadTimeDays }}일</td>
                    <td data-label="단가" class="num">{{ formatNumber(s.unitPrice) }} 원</td>
                    <td data-label="인증">
                      <div v-if="(s.certifications ?? []).length" class="chips">
                        <span v-for="c in s.certifications" :key="c" class="chip">{{ c }}</span>
                      </div>
                      <span v-else class="u-muted">-</span>
                    </td>
                    <td class="actions-cell">
                      <button v-if="auth.isBuyer" type="button" class="btn btn--sm" @click="goOrder(s)">
                        조달 신청
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { materialApi } from '@/api/material.js'
import { useAsync } from '@/composables/useAsync.js'
import { useAuthStore } from '@/store/auth.js'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const detail = useAsync(materialApi.get)
const suppliers = useAsync((code, params) => materialApi.suppliersByCode(code, params))

const requiredQuantity = ref(null)
const certification = ref('')

const material = computed(() => detail.data.value)
const supplierList = computed(() => suppliers.data.value?.suppliers ?? [])

async function findSuppliers() {
  if (!material.value) return
  await suppliers.run(material.value.materialCode, {
    requiredQuantity: requiredQuantity.value || undefined,
    certification: certification.value || undefined,
    sort: 'leadTimeDays,asc'
  })
}

// 조달 신청 화면으로 값을 넘긴다. (order 담당자가 /orders 에서 받아 처리)
function goOrder(supplier) {
  router.push({
    path: '/orders',
    query: {
      materialId: supplier.materialId,
      quantity: requiredQuantity.value ?? ''
    }
  })
}

function formatNumber(v) {
  return v == null ? '-' : Number(v).toLocaleString('ko-KR')
}

onMounted(async () => {
  await detail.run(route.params.id)
  // 필요 수량만 미리 채운다. 공급처 조회는 버튼을 눌렀을 때 실행한다.
  if (requiredQuantity.value == null) {
    requiredQuantity.value = material.value?.minOrderQuantity ?? null
  }
})
</script>
