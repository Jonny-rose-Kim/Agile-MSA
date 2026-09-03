<!--
  담당 API: GET /api/materials/{id}  ·  GET /api/materials/code/{materialCode}/suppliers
  담당자  : (팀원) material-service
-->
<template>
  <div>
    <div v-if="detail.error.value" class="error-msg">{{ detail.error.value }}</div>
    <div v-if="detail.loading.value" class="loading">불러오는 중...</div>

    <template v-else-if="material">
      <h1 class="page-title">{{ material.materialName }}</h1>
      <p class="page-desc">{{ material.materialCode }} · {{ material.supplierName }}</p>

      <!-- ===== 원료 상세 ===== -->
      <div class="section">
        <h2 class="section-title">원료 정보</h2>
        <table>
          <tbody>
            <tr><th style="width:170px">원료코드</th><td>{{ material.materialCode }}</td></tr>
            <tr><th>CAS 번호</th><td>{{ material.casNumber ?? '-' }}</td></tr>
            <tr><th>카테고리</th><td>{{ material.category }}</td></tr>
            <tr><th>단가</th><td>{{ formatNumber(material.unitPrice) }} 원 / {{ material.unit }}</td></tr>
            <tr><th>최소 주문 수량</th><td>{{ formatNumber(material.minOrderQuantity) }} {{ material.unit }}</td></tr>
            <tr><th>여유 생산능력</th><td>{{ formatNumber(material.availableCapacity) }} {{ material.unit }}</td></tr>
            <tr><th>리드타임</th><td>{{ material.leadTimeDays }}일</td></tr>
            <tr><th>인증</th><td>{{ (material.certifications ?? []).join(', ') || '-' }}</td></tr>
            <tr><th>설명</th><td>{{ material.description ?? '-' }}</td></tr>
          </tbody>
        </table>
        <div class="actions">
          <router-link v-if="auth.isSupplier" :to="`/materials/${material.id}/edit`">
            <button type="button" class="secondary">수정</button>
          </router-link>
          <router-link to="/materials"><button type="button" class="secondary">목록</button></router-link>
        </div>
      </div>

      <!-- ===== Ep-01 US1: 공급 가능 인증 공장 조회 ===== -->
      <div class="section">
        <h2 class="section-title">공급 가능 인증 공장 조회 (Ep-01 US1)</h2>
        <p class="page-desc">
          GET /api/materials/code/{{ material.materialCode }}/suppliers —
          같은 원료를 공급할 수 있는 다른 공장을 찾습니다.
        </p>

        <div class="row">
          <div class="form-group">
            <label class="form-label" for="qty">필요 수량 ({{ material.unit }})</label>
            <input id="qty" v-model.number="requiredQuantity" type="number" min="1" placeholder="800" />
          </div>
          <div class="form-group">
            <label class="form-label" for="cert">인증 조건</label>
            <select id="cert" v-model="certification">
              <option value="">전체</option>
              <option value="GMP">GMP</option>
              <option value="DMF">DMF</option>
            </select>
          </div>
          <button type="button" @click="findSuppliers" :disabled="suppliers.loading.value">공급처 찾기</button>
        </div>

        <div v-if="suppliers.error.value" class="error-msg">{{ suppliers.error.value }}</div>
        <div v-if="suppliers.loading.value" class="loading">조회 중...</div>

        <template v-else-if="suppliers.data.value">
          <!-- totalCount === 0 은 에러가 아니라 정상 응답이다 -->
          <div v-if="!supplierList.length" class="empty">
            현재 조건을 만족하는 인증 공장이 없습니다. 필요 수량을 낮춰 다시 조회해 보세요.
          </div>
          <table v-else>
            <thead>
              <tr><th>공장명</th><th>국가</th><th>여유 생산능력</th><th>리드타임</th><th>단가</th><th>인증</th><th></th></tr>
            </thead>
            <tbody>
              <tr v-for="s in supplierList" :key="s.materialId ?? s.supplierId">
                <td>{{ s.supplierName }}</td>
                <td>{{ s.country ?? '-' }}</td>
                <td>{{ formatNumber(s.availableCapacity) }}</td>
                <td>{{ s.leadTimeDays }}일</td>
                <td>{{ formatNumber(s.unitPrice) }} 원</td>
                <td>{{ (s.certifications ?? []).join(', ') || '-' }}</td>
                <td>
                  <button v-if="auth.isBuyer" type="button" @click="goOrder(s)">조달 신청</button>
                </td>
              </tr>
            </tbody>
          </table>
        </template>
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

onMounted(() => detail.run(route.params.id))
</script>
