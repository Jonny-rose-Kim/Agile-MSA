<!--
  담당 API: GET /api/recommend   (구 /api/forecast/demand + 공장 추천을 단일 엔드포인트로 통합)
  담당자  : (팀원) recommend-service
-->
<template>
  <div>
    <h1 class="page-title">AI 수요 예측 · 공장 추천</h1>
    <p class="page-desc">
      GET /api/recommend — 원료 하나를 입력하면 예측 수요와 추천 공장을 함께 받습니다.
    </p>

    <div class="todo-box">
      <h4>담당: /api/recommend (팀원)</h4>
      <ul>
        <li>수요 예측(Ep-03 US1)과 공장 추천을 <strong>단일 엔드포인트</strong>로 통합했습니다.</li>
        <li>API 함수는 <code>src/api/recommend.js</code>에 준비되어 있습니다.</li>
      </ul>
    </div>

    <!-- ===== 조회 조건 ===== -->
    <div class="section">
      <form class="row" @submit.prevent="search">
        <div class="form-group">
          <label class="form-label" for="materialCode">원료코드 *</label>
          <input id="materialCode" v-model.trim="materialCode" type="text" required placeholder="API-CEFA-500" />
        </div>
        <div class="form-group">
          <label class="form-label" for="quantity">필요 수량</label>
          <input id="quantity" v-model.number="quantity" type="number" min="1" placeholder="800" />
        </div>
        <div class="form-group">
          <label class="form-label" for="horizon">예측 기간(일)</label>
          <input id="horizon" v-model.number="horizon" type="number" min="1" placeholder="90" />
        </div>
        <button type="submit" :disabled="result.loading.value">조회</button>
      </form>
    </div>

    <div v-if="result.error.value" class="error-msg">{{ result.error.value }}</div>
    <div v-if="result.loading.value" class="loading">예측 중...</div>

    <template v-else-if="data">
      <!-- ===== 수요 예측 ===== -->
      <div class="section">
        <h2 class="section-title">수요 예측 (Ep-03 US1)</h2>
        <table>
          <tbody>
            <tr><th style="width:170px">예측 수요</th><td>{{ formatNumber(data.predictedDemand) }}</td></tr>
            <tr><th>신뢰도</th><td>{{ data.confidence != null ? (data.confidence * 100).toFixed(1) + '%' : '-' }}</td></tr>
            <tr><th>예측 모델</th><td>{{ data.modelType ?? '-' }}</td></tr>
            <tr><th>반영 외부 지표</th><td>{{ (data.externalFactors ?? []).join(', ') || '-' }}</td></tr>
          </tbody>
        </table>

        <!-- 기간별 예측값 (차트로 교체 가능) -->
        <table v-if="points.length" style="margin-top:12px">
          <thead><tr><th>일자</th><th>예측</th><th>하한</th><th>상한</th></tr></thead>
          <tbody>
            <tr v-for="p in points" :key="p.date">
              <td>{{ p.date }}</td>
              <td>{{ formatNumber(p.predicted) }}</td>
              <td>{{ formatNumber(p.lower) }}</td>
              <td>{{ formatNumber(p.upper) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- ===== 추천 공장 ===== -->
      <div class="section">
        <h2 class="section-title">공급 리스크 기반 추천 공장</h2>
        <div v-if="!suppliers.length" class="empty">추천할 공장이 없습니다.</div>
        <table v-else>
          <thead>
            <tr><th>공장명</th><th>리스크 점수</th><th>등급</th><th>추천 사유</th><th></th></tr>
          </thead>
          <tbody>
            <tr v-for="s in suppliers" :key="s.supplierId">
              <td>{{ s.supplierName }}</td>
              <td>{{ s.riskScore }}</td>
              <td><span class="badge">{{ s.grade }}</span></td>
              <td>{{ (s.reasons ?? []).join(' · ') }}</td>
              <td>
                <button v-if="auth.isBuyer" type="button" @click="goOrder(s)">조달 신청</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
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

// 부족 알림에서 원료코드를 들고 들어온 경우 바로 조회한다.
onMounted(() => { if (materialCode.value) search() })
</script>
