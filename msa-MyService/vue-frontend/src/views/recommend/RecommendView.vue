<!--
  담당 API: GET /api/recommend   (구 /api/forecast/demand + 공장 추천을 단일 엔드포인트로 통합)
  담당자  : (팀원) recommend-service
  참고    : 재고 조회 → 수요 예측 → 소진 시뮬레이션 → GMP 인증 공급사 추천을 한 번에 받는다.
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
              <input id="quantity" class="input" v-model.number="quantity" type="number" min="1"
                     placeholder="생략하면 권장 발주량" />
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
        <!-- 데모 데이터로 동작 중임을 숨기지 않는다 -->
        <div v-if="data.mock" class="notice">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
               stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="12" cy="12" r="9" /><path d="M12 16v-4.5M12 8h.01" />
          </svg>
          <span>
            데모 데이터 기준입니다. 원료 · 재고 · 공급사는 고정값이며 실제 카탈로그와 연결돼 있지 않습니다.
            (<code>MOCK_MODE=false</code> 로 전환하면 실제 서비스를 호출합니다)
          </span>
        </div>

        <!-- ===== 대상 원료 ===== -->
        <section class="card">
          <header class="card__head">
            <div>
              <h2 class="card__title">{{ material.name ?? material.materialCode }}</h2>
              <p class="card__desc">{{ material.drug }}</p>
            </div>
            <div class="u-row">
              <span class="code-tag">{{ material.materialCode }}</span>
              <span class="badge" :data-status="forecast.basis === 'LLM_FORECAST' ? 'ACTIVE' : 'PENDING'">
                {{ forecast.basis === 'LLM_FORECAST' ? 'LLM 예측' : '합성 추세' }}
              </span>
            </div>
          </header>
        </section>

        <!-- ===== 핵심 지표 ===== -->
        <div class="tiles">
          <div class="tile">
            <p class="tile__label">수요 증감률</p>
            <p class="tile__value" :class="{ 'tile__value--risk': forecast.demandChange > 0 }">
              {{ signedPercent(forecast.demandChange) }}
            </p>
            <p class="tile__sub">최근 {{ forecast.days }}일 · 직전 대비</p>
          </div>
          <div class="tile">
            <p class="tile__label">신뢰도</p>
            <p class="tile__value">{{ forecast.confidence != null ? (forecast.confidence * 100).toFixed(0) + '%' : '-' }}</p>
            <div v-if="forecast.confidence != null" class="meter u-mt-2">
              <span class="meter__track">
                <span class="meter__fill" :style="{ width: (forecast.confidence * 100).toFixed(1) + '%' }"></span>
              </span>
            </div>
          </div>
          <div class="tile">
            <p class="tile__label">임계치 하회 예상</p>
            <p class="tile__value tile__value--sm">{{ forecast.shortageDate ?? '기간 내 없음' }}</p>
            <p class="tile__sub" v-if="forecast.stockoutDate">재고 소진 {{ forecast.stockoutDate }}</p>
          </div>
          <div class="tile">
            <p class="tile__label">권장 발주량</p>
            <p class="tile__value">
              {{ formatNumber(forecast.recommendedOrderQty) }}<span class="tile__unit">{{ material.unit }}</span>
            </p>
            <p class="tile__sub">기준일 {{ forecast.asOf }}</p>
          </div>
        </div>

        <div v-if="forecast.summary" class="alert alert--info">
          <svg class="alert__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="12" cy="12" r="9" /><path d="M12 16v-4.5M12 8h.01" />
          </svg>
          {{ forecast.summary }}
        </div>

        <!-- ===== 소진 시뮬레이션 ===== -->
        <section class="card">
          <header class="card__head">
            <div>
              <h2 class="card__title">재고 소진 시뮬레이션</h2>
              <p class="card__desc">평상시 소진량에 예측 증감률을 반영해 {{ forecast.days }}일간 재고를 계산했습니다.</p>
            </div>
          </header>
          <div class="card__body">
            <dl class="kv">
              <dt>현재 재고</dt>
              <dd class="u-num">{{ formatNumber(inventory.quantity) }} {{ material.unit }}</dd>

              <dt>부족 임계치</dt>
              <dd class="u-num">{{ formatNumber(inventory.threshold) }} {{ material.unit }}</dd>

              <dt>일 소진량</dt>
              <dd class="u-num">
                평상시 {{ formatNumber(forecast.dailyConsumption?.baseline) }}
                → 예측 <strong>{{ formatNumber(forecast.dailyConsumption?.predicted) }}</strong> {{ material.unit }}
              </dd>

              <dt>임계치 하회일</dt>
              <dd>{{ forecast.shortageDate ?? '-' }}</dd>

              <dt>재고 소진일</dt>
              <dd>{{ forecast.stockoutDate ?? '-' }}</dd>

              <dt>{{ forecast.days }}일 후 잔여 재고</dt>
              <dd class="u-num">{{ formatNumber(forecast.projectedQuantity) }} {{ material.unit }}</dd>

              <dt>부족량</dt>
              <dd class="u-num u-strong">{{ formatNumber(forecast.shortfall) }} {{ material.unit }}</dd>

              <dt>권장 임계치</dt>
              <dd class="u-num">{{ formatNumber(forecast.recommendedThreshold) }} {{ material.unit }}</dd>
            </dl>
          </div>
        </section>

        <!-- ===== 반영 외부 지표 ===== -->
        <section class="card" v-if="factors.length">
          <header class="card__head">
            <div>
              <h2 class="card__title">반영 외부 지표</h2>
              <p class="card__desc">수요 변화의 근거로 쓴 지표입니다.</p>
            </div>
          </header>
          <div class="card__body card__body--flush">
            <table class="table">
              <thead>
                <tr><th>지표</th><th>추세</th><th>영향도</th><th>근거</th></tr>
              </thead>
              <tbody>
                <tr v-for="f in factors" :key="f.name" :data-severity="f.impact">
                  <td data-label="지표" class="u-strong">{{ f.name }}</td>
                  <td data-label="추세">
                    <span class="badge" :data-status="f.trend === 'RISING' ? 'PENDING' : f.trend === 'FALLING' ? 'CONFIRMED' : ''">
                      {{ TREND_LABELS[f.trend] ?? f.trend }}
                    </span>
                  </td>
                  <td data-label="영향도"><span class="badge" :data-severity="f.impact">{{ f.impact }}</span></td>
                  <td data-label="근거">
                    <span class="u-sm">{{ f.evidence }}</span>
                    <a v-if="f.source_url" :href="f.source_url" target="_blank" rel="noopener"
                       class="u-sm u-nowrap"> · 출처</a>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ===== 추천 공급사 ===== -->
        <section class="card">
          <header class="card__head">
            <div>
              <h2 class="card__title">공급 가능 인증 공장</h2>
              <p class="card__desc">
                GMP 인증을 보유하고 필요 수량 {{ formatNumber(data.needQuantity) }} {{ material.unit }}을
                댈 수 있는 공장입니다. 과거 거래 횟수와 단가 순으로 정렬했습니다.
              </p>
            </div>
          </header>

          <div v-if="!suppliers.length" class="empty">
            <svg class="empty__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M3 20h18M4 20V10l5 3.5V10l5 3.5V10l5 3.5V20" />
            </svg>
            <p class="empty__title">조건을 만족하는 인증 공장이 없습니다.</p>
            <p class="empty__desc">필요 수량을 낮춰 다시 조회하거나, 여러 공장에 나눠 발주해야 합니다.</p>
          </div>

          <div v-else class="card__body card__body--flush">
            <table class="table">
              <thead>
                <tr>
                  <th>공장명</th><th>국가</th><th>인증</th>
                  <th class="num">단가</th><th class="num">여유 생산능력</th>
                  <th class="num">과거 거래</th><th class="num">예상 비용</th>
                  <th><span class="sr-only">동작</span></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="s in suppliers" :key="s.supplierId">
                  <td data-label="공장명" class="u-strong">{{ s.supplierName }}</td>
                  <td data-label="국가" class="u-muted">{{ s.originCountry ?? '-' }}</td>
                  <td data-label="인증">
                    <span v-if="s.gmpCertified" class="chip">GMP</span>
                    <span v-else class="u-muted">-</span>
                  </td>
                  <td data-label="단가" class="num">{{ formatNumber(s.price) }} 원</td>
                  <td data-label="여유 생산능력" class="num">{{ formatNumber(s.availableCapacity) }}</td>
                  <td data-label="과거 거래" class="num">{{ s.pastOrderCount ?? 0 }}회</td>
                  <td data-label="예상 비용" class="num u-strong">{{ formatNumber(s.estimatedCost) }} 원</td>
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
import { recommendApi, TREND_LABELS } from '@/api/recommend.js'
import { useAsync } from '@/composables/useAsync.js'
import { useAuthStore } from '@/store/auth.js'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const result = useAsync(recommendApi.get)

/**
 * 화면에 처음 들어왔을 때 바로 결과가 보이도록 조회 조건을 채워 둔다.
 *
 * API-CEFA-500 을 쓰는 이유: 데모 카탈로그(mock_catalog)와 실제 material-service
 * 시드 데이터 양쪽에 모두 있는 원료코드라, MOCK_MODE 를 꺼도 그대로 동작한다.
 */
const DEFAULT_MATERIAL_CODE = 'API-CEFA-500'
const DEFAULT_HORIZON_DAYS = 90

// 재고 부족 알림에서 넘어온 값이 있으면 그쪽을 우선한다.
const materialCode = ref(route.query.materialCode ?? DEFAULT_MATERIAL_CODE)
const quantity = ref(route.query.quantity ? Number(route.query.quantity) : null)
const horizon = ref(DEFAULT_HORIZON_DAYS)

const data = computed(() => result.data.value)
const material = computed(() => data.value?.material ?? {})
const inventory = computed(() => data.value?.inventory ?? {})
const forecast = computed(() => data.value?.forecast ?? {})
const factors = computed(() => forecast.value?.factors ?? [])
const suppliers = computed(() => data.value?.suppliers ?? [])

async function search() {
  if (!materialCode.value) return
  await result.run({
    materialCode: materialCode.value,
    quantity: quantity.value || undefined,
    days: horizon.value || undefined
  })
}

/**
 * 조달 주문 화면으로 값을 넘긴다.
 * 데모 데이터일 때는 materialId 가 실제 카탈로그 ID 와 다르므로 수량만 넘긴다.
 */
function goOrder(supplier) {
  router.push({
    path: '/orders',
    query: {
      materialId: data.value?.mock ? '' : (supplier.materialId ?? ''),
      quantity: data.value?.needQuantity ? Math.ceil(data.value.needQuantity) : ''
    }
  })
}

const formatNumber = (v) => (v == null ? '-' : Number(v).toLocaleString('ko-KR', { maximumFractionDigits: 1 }))
const signedPercent = (v) => (v == null ? '-' : `${v > 0 ? '+' : ''}${(v * 100).toFixed(1)}%`)

// 조건이 채워진 상태로 들어오므로 진입 즉시 1회 조회한다.
onMounted(() => { if (materialCode.value) search() })
</script>
