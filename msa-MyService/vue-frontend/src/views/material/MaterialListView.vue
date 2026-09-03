<!--
  담당 API: GET /api/materials  ·  GET /api/materials/code/{materialCode}/suppliers
  담당자  : (팀원) material-service
  참고    : 검색·페이징은 src/api/material.js 의 materialApi.list() 를 호출한다.
            로딩·에러 상태는 useAsync 가 관리하므로 마크업만 바꿔도 동작한다.
-->
<template>
  <div>
    <div class="page-head">
      <div class="page-head__text">
        <h1 class="page-title">원료 카탈로그</h1>
        <p class="page-desc">공급 중인 원료를 검색합니다.</p>
      </div>
    </div>

    <div class="stack">
      <!-- ===== 검색 필터 ===== -->
      <section class="card">
        <div class="card__body">
          <form class="filter-bar" @submit.prevent="search(0)">
            <div class="field">
              <label class="field__label" for="keyword">검색어</label>
              <input id="keyword" class="input" v-model.trim="filters.keyword" type="text"
                     placeholder="원료명 · 원료코드 · CAS번호" />
            </div>
            <div class="field">
              <label class="field__label" for="category">카테고리</label>
              <select id="category" class="select" v-model="filters.category">
                <option value="">전체</option>
                <option v-for="c in MATERIAL_CATEGORIES" :key="c.code" :value="c.code">{{ c.label }}</option>
              </select>
            </div>
            <div class="field">
              <label class="field__label" for="minCapacity">최소 여유 생산능력</label>
              <input id="minCapacity" class="input" v-model.number="filters.minCapacity"
                     type="number" min="0" placeholder="500" />
            </div>
            <div class="field">
              <label class="field__label" for="certification">인증</label>
              <select id="certification" class="select" v-model="filters.certification">
                <option value="">전체</option>
                <option v-for="c in CERTIFICATIONS" :key="c" :value="c">{{ c }}</option>
              </select>
            </div>
            <div class="filter-bar__actions">
              <button type="submit" class="btn" :disabled="list.loading.value">
                <span v-if="list.loading.value" class="spinner" aria-hidden="true"></span>
                검색
              </button>
              <button type="button" class="btn btn--secondary" @click="resetFilters">초기화</button>
            </div>
          </form>
        </div>
      </section>

      <div v-if="list.error.value" class="alert alert--error" role="alert">
        <svg class="alert__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="12" cy="12" r="9" /><path d="M12 7.5v5M12 16h.01" />
        </svg>
        {{ list.error.value }}
      </div>

      <!-- ===== 결과 ===== -->
      <section class="card">
        <div v-if="list.loading.value" class="skeleton-rows">
          <span class="skeleton"></span><span class="skeleton"></span>
          <span class="skeleton"></span><span class="skeleton"></span><span class="skeleton"></span>
        </div>

        <template v-else>
          <div v-if="!items.length" class="empty">
            <svg class="empty__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="11" cy="11" r="7" /><path d="m20 20-3.6-3.6" />
            </svg>
            <p class="empty__title">조건에 맞는 원료가 없습니다.</p>
            <p class="empty__desc">검색어나 필터를 조정한 뒤 다시 검색해 보세요.</p>
          </div>

          <template v-else>
            <div class="card__body card__body--flush">
              <table class="table">
                <thead>
                  <tr>
                    <th>원료코드</th><th>원료명</th><th>카테고리</th>
                    <th class="num">단가</th><th class="num">여유 생산능력</th>
                    <th class="num">리드타임</th><th>공급사</th><th><span class="sr-only">동작</span></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="m in items" :key="m.id">
                    <td data-label="원료코드"><span class="code-tag">{{ m.materialCode }}</span></td>
                    <td data-label="원료명">
                      <router-link :to="`/materials/${m.id}`" class="u-strong">{{ m.materialName }}</router-link>
                    </td>
                    <td data-label="카테고리" class="u-muted">{{ categoryLabel(m.category) }}</td>
                    <td data-label="단가" class="num">
                      {{ formatNumber(m.unitPrice) }} <span class="u-muted u-xs">원/{{ m.unit }}</span>
                    </td>
                    <td data-label="여유 생산능력" class="num">
                      {{ formatNumber(m.availableCapacity) }} <span class="u-muted u-xs">{{ m.unit }}</span>
                    </td>
                    <td data-label="리드타임" class="num">{{ m.leadTimeDays }}일</td>
                    <td data-label="공급사">{{ m.supplierName }}</td>
                    <td class="actions-cell">
                      <router-link :to="`/materials/${m.id}`" class="btn btn--secondary btn--sm">상세</router-link>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="card__foot" v-if="items.length">
              <p class="u-sm u-muted">총 {{ formatNumber(totalElements) }}건</p>
              <div class="pager">
                <button type="button" class="btn btn--secondary btn--sm"
                        :disabled="page <= 0" @click="search(page - 1)">이전</button>
                <span class="pager__pos"><strong>{{ page + 1 }}</strong> / {{ totalPages || 1 }}</span>
                <button type="button" class="btn btn--secondary btn--sm"
                        :disabled="page + 1 >= totalPages" @click="search(page + 1)">다음</button>
              </div>
            </div>
          </template>
        </template>
      </section>
    </div>
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
const totalElements = computed(() => list.data.value?.totalElements ?? items.value.length)

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
