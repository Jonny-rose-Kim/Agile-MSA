<!--
  담당 API: GET /api/materials  ·  GET /api/materials/code/{materialCode}/suppliers
  담당자  : (팀원) material-service
-->
<template>
  <div>
    <h1 class="page-title">원료 카탈로그</h1>
    <p class="page-desc">GET /api/materials — 공급 중인 원료를 검색합니다.</p>

    <div class="todo-box">
      <h4>담당: /api/materials (팀원)</h4>
      <ul>
        <li>이 화면의 검색·페이징 로직은 <code>src/api/material.js</code>의 <code>materialApi.list()</code>를 호출합니다.</li>
        <li>마크업만 바꿔도 동작하도록 상태는 <code>useAsync</code>가 관리합니다.</li>
      </ul>
    </div>

    <!-- ===== 검색 필터 ===== -->
    <div class="section">
      <form class="row" @submit.prevent="search(0)">
        <div class="form-group">
          <label class="form-label" for="keyword">검색어</label>
          <input id="keyword" v-model.trim="filters.keyword" type="text" placeholder="원료명 · 원료코드 · CAS번호" />
        </div>
        <div class="form-group">
          <label class="form-label" for="category">카테고리</label>
          <select id="category" v-model="filters.category">
            <option value="">전체</option>
            <option v-for="c in MATERIAL_CATEGORIES" :key="c.code" :value="c.code">{{ c.label }}</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label" for="minCapacity">최소 여유 생산능력</label>
          <input id="minCapacity" v-model.number="filters.minCapacity" type="number" min="0" placeholder="500" />
        </div>
        <div class="form-group">
          <label class="form-label" for="certification">인증</label>
          <select id="certification" v-model="filters.certification">
            <option value="">전체</option>
            <option v-for="c in CERTIFICATIONS" :key="c" :value="c">{{ c }}</option>
          </select>
        </div>
        <button type="submit" :disabled="list.loading.value">검색</button>
        <button type="button" class="secondary" @click="resetFilters">초기화</button>
      </form>
    </div>

    <!-- ===== 결과 ===== -->
    <div v-if="list.error.value" class="error-msg">{{ list.error.value }}</div>
    <div v-if="list.loading.value" class="loading">불러오는 중...</div>

    <template v-else>
      <div v-if="!items.length" class="empty">조건에 맞는 원료가 없습니다.</div>

      <table v-else>
        <thead>
          <tr>
            <th>원료코드</th><th>원료명</th><th>카테고리</th>
            <th>단가</th><th>여유 생산능력</th><th>리드타임</th><th>공급사</th><th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in items" :key="m.id">
            <td>{{ m.materialCode }}</td>
            <td>{{ m.materialName }}</td>
            <td>{{ categoryLabel(m.category) }}</td>
            <td>{{ formatNumber(m.unitPrice) }} 원 / {{ m.unit }}</td>
            <td>{{ formatNumber(m.availableCapacity) }} {{ m.unit }}</td>
            <td>{{ m.leadTimeDays }}일</td>
            <td>{{ m.supplierName }}</td>
            <td><router-link :to="`/materials/${m.id}`">상세</router-link></td>
          </tr>
        </tbody>
      </table>

      <div class="actions" v-if="items.length">
        <button type="button" class="secondary" :disabled="page <= 0" @click="search(page - 1)">이전</button>
        <span>{{ page + 1 }} / {{ totalPages || 1 }}</span>
        <button type="button" class="secondary" :disabled="page + 1 >= totalPages" @click="search(page + 1)">다음</button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { materialApi, MATERIAL_CATEGORIES, CERTIFICATIONS } from '@/api/material.js'
import { useAsync } from '@/composables/useAsync.js'

const list = useAsync(materialApi.list, { initial: { content: [] } })
const page = ref(0)
const size = 20

const filters = reactive({ keyword: '', category: '', minCapacity: null, certification: '' })

const items = computed(() => list.data.value?.content ?? [])
const totalPages = computed(() => list.data.value?.totalPages ?? 0)

async function search(nextPage = 0) {
  page.value = nextPage
  await list.run({
    keyword: filters.keyword || undefined,
    category: filters.category || undefined,
    minCapacity: filters.minCapacity || undefined,
    certification: filters.certification || undefined,
    page: nextPage,
    size
  })
}

function resetFilters() {
  filters.keyword = ''
  filters.category = ''
  filters.minCapacity = null
  filters.certification = ''
  search(0)
}

function categoryLabel(code) {
  return MATERIAL_CATEGORIES.find((c) => c.code === code)?.label ?? code
}
function formatNumber(v) {
  return v == null ? '-' : Number(v).toLocaleString('ko-KR')
}

onMounted(() => search(0))
</script>
