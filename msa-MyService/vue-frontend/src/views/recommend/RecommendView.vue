<!--
  담당 API: GET /api/recommend   (구 /api/forecast/demand + 공장 추천을 단일 엔드포인트로 통합)
  담당자  : (팀원) recommend-service
  참고    : 수요 예측(Ep-03 US1)과 공장 추천을 한 번의 호출로 함께 받는다.
-->
<template>
  <div>
    <div class="page-head">
      <div class="page-head__text">
        <h1 class="page-title">AI 수요 예측 · 공장 추천</h1>
        <p class="page-desc">원료 하나를 입력하면 예측 수요와 추천 공장을 함께 받습니다.</p>
      </div>
    </div>

    <div class="stack">
      <!-- ===== 조회 조건 ===== -->
      <section class="card">
        <div class="card__body">
          <form class="filter-bar" @submit.prevent="search">
            <div class="field">
              <label class="field__label" for="materialCode">원료코드 <span class="req">*</span></label>
              <input id="materialCode" class="input" v-model.trim="materialCode" type="text"
                     required placeholder="API-CEFA-500" />
            </div>
            <div class="field">
              <label class="field__label" for="quantity">필요 수량</label>
              <input id="quantity" class="input" v-model.number="quantity" type="number" min="1" placeholder="800" />
            </div>
            <div class="field">
              <label class="field__label" for="horizon">예측 기간(일)</label>
              <input id="horizon" class="input" v-model.number="horizon" type="number" min="1" placeholder="90" />
            </div>
            <div class="filter-bar__actions">
              <button type="submit" class="btn" :disabled="result.loading.value">
                <span v-if="result.loading.value" class="spinner" aria-hidden="true"></span>
                {{ result.loading.value ? '예측 중...' : '조회' }}
              </button>
            </div>
          </form>
        </div>
      </section>

      <div v-if="result.error.value" class="alert alert--error" role="alert">
        <svg class="alert__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="12" cy="12" r="9" /><path d="M12 7.5v5M12 16h.01" />
        </svg>
        {{ result.error.value }}
      </div>

      <div v-if="result.loading.value" class="card">
        <div class="skeleton-rows">
          <span class="skeleton"></span><span class="skeleton"></span>
          <span class="skeleton"></span><span class="skeleton"></span>
        </div>
      </div>

      <template v-else-if="data">
        <!-- ===== 수요 예측 (Ep-03 US1) ===== -->
        <div class="tiles">
          <div class="tile">
            <p class="tile__label">예측 수요</p>
            <p class="tile__value">{{ formatNumber(data.predictedDemand) }}</p>
            <p class="tile__sub">향후 {{ horizon || '-' }}일 기준</p>
          </div>
          <div class="tile">
            <p class="tile__label">신뢰도</p>
            <p class="tile__value">
              {{ data.confidence != null ? (data.confidence * 100).toFixed(1) + '%' : '-' }}
            </p>
            <div v-if="data.confidence != null" class="meter u-mt-2">
              <span class="meter__track">
                <span class="meter__fill" :style="{ width: (data.confidence * 100).toFixed(1) + '%' }"></span>
              </span>
            </div>
          </div>
          <div class="tile">
            <p class="tile__label">예측 모델</p>
            <p class="tile__value tile__value--sm">{{ data.modelType ?? '-' }}</p>
          </div>
          <div class="tile">
            <p class="tile__label">반영 외부 지표</p>
            <div class="chips u-mt-2">
              <span v-for="f in (data.externalFactors ?? [])" :key="f" class="chip">{{ f }}</span>
              <span v-if="!(data.externalFactors ?? []).length" class="u-muted">-</span>
            </div>
          </div>
        </div>

        <!-- 기간별 예측값 -->
        <section class="card" v-if="points.length">
          <header class="card__head">
            <div>
              <h2 class="card__title">기간별 예측</h2>
              <p class="card__desc">하한 · 상한은 예측 구간입니다.</p>
            </div>
          </header>
          <div class="card__body card__body--flush">
            <table class="table">
              <thead>
                <tr><th>일자</th><th class="num">예측</th><th class="num">하한</th><th class="num">상한</th></tr>
              </thead>
              <tbody>
                <tr v-for="p in points" :key="p.date">
                  <td data-label="일자" class="u-mono">{{ p.date }}</td>
                  <td data-label="예측" class="num u-strong">{{ formatNumber(p.predicted) }}</td>
                  <td data-label="하한" class="num u-muted">{{ formatNumber(p.lower) }}</td>
                  <td data-label="상한" class="num u-muted">{{ formatNumber(p.upper) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ===== 추천 공장 ===== -->
        <section class="card">
          <header class="card__head">
            <div>
              <h2 class="card__title">공급 리스크 기반 추천 공장</h2>
              <p class="card__desc">리스크 점수가 낮을수록 안정적인 공급처입니다.</p>
            </div>
          </header>

          <div v-if="!suppliers.length" class="empty">
            <svg class="empty__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M3 20h18M4 20V10l5 3.5V10l5 3.5V10l5 3.5V20" />
            </svg>
            <p class="empty__title">추천할 공장이 없습니다.</p>
            <p class="empty__desc">원료코드나 필요 수량을 조정한 뒤 다시 조회해 보세요.</p>
          </div>

          <div v-else class="card__body card__body--flush">
            <table class="table">
              <thead>
                <tr>
                  <th>공장명</th><th>리스크 점수</th><th>등급</th><th>추천 사유</th>
                  <th><span class="sr-only">동작</span></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="s in suppliers" :key="s.supplierId">
                  <td data-label="공장명" class="u-strong">{{ s.supplierName }}</td>
                  <td data-label="리스크 점수">
                    <span class="meter" data-tone="risk">
                      <span class="meter__track">
                        <span class="meter__fill" :style="{ width: riskPercent(s.riskScore) + '%' }"></span>
                      </span>
                      <span class="meter__value">{{ s.riskScore }}</span>
                    </span>
                  </td>
                  <td data-label="등급"><span class="badge badge--plain">{{ s.grade }}</span></td>
                  <td data-label="추천 사유">
                    <div class="chips">
                      <span v-for="r in (s.reasons ?? [])" :key="r" class="chip">{{ r }}</span>
                      <span v-if="!(s.reasons ?? []).length" class="u-muted">-</span>
                    </div>
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
        </section>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { recommendApi } from '@/api/recommend.js'
import { useAsync } from '@/composables/useAsync.js'
import { useAuthStore } from '@/store/auth.js'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const result = useAsync(recommendApi.get)

// 재고 부족 알림에서 넘어온 값을 그대로 받는다.
const materialCode = ref(route.query.materialCode ?? '')
const quantity = ref(route.query.quantity ? Number(route.query.quantity) : null)
const horizon = ref(90)

const data = computed(() => result.data.value)
const points = computed(() => data.value?.points ?? [])
const suppliers = computed(() => data.value?.suppliers ?? data.value?.recommendations ?? [])

async function search() {
  if (!materialCode.value) return
  await result.run({
    materialCode: materialCode.value,
    quantity: quantity.value || undefined,
    horizon: horizon.value || undefined
  })
}

function goOrder(supplier) {
  router.push({
    path: '/orders',
    query: { materialId: supplier.materialId ?? '', quantity: quantity.value ?? '' }
  })
}

const formatNumber = (v) => (v == null ? '-' : Number(v).toLocaleString('ko-KR'))

/**
 * 리스크 점수 막대의 채움 비율(표시 전용).
 * 서버가 0~1로 주는지 0~100으로 주는지 확정되면 이 함수만 고치면 된다.
 */
function riskPercent(score) {
  const n = Number(score)
  if (!Number.isFinite(n)) return 0
  const pct = n <= 1 ? n * 100 : n
  return Math.max(0, Math.min(100, pct))
}

// 부족 알림에서 원료코드를 들고 들어온 경우 바로 조회한다.
onMounted(() => { if (materialCode.value) search() })
</script>
