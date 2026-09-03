<!--
  담당 API: POST /api/inventories  ·  GET /api/inventories/my  ·  PATCH /api/inventories/{id}/stock
  담당자  : (팀원) order-service 재고 영역
-->
<template>
  <div>
    <h1 class="page-title">재고 관리</h1>
    <p class="page-desc">보유 원료의 재고량과 임계치를 등록합니다 (Ep-02 US1).</p>

    <div class="todo-box">
      <h4>담당: /api/inventories (팀원)</h4>
      <ul><li>API 함수는 <code>src/api/inventory.js</code>에 준비되어 있습니다.</li></ul>
    </div>

    <!-- ===== 재고 등록 ===== -->
    <div class="section">
      <h2 class="section-title">재고 등록 — POST /api/inventories</h2>
      <div v-if="createError" class="error-msg">{{ createError }}</div>
      <form class="row" @submit.prevent="submit">
        <div class="form-group">
          <label class="form-label" for="code">원료코드 *</label>
          <input id="code" v-model.trim="form.materialCode" type="text" required placeholder="API-CEFA-500" />
        </div>
        <div class="form-group">
          <label class="form-label" for="stock">현재 재고량 *</label>
          <input id="stock" v-model.number="form.currentStock" type="number" min="0" required />
        </div>
        <div class="form-group">
          <label class="form-label" for="threshold">부족 임계치 *</label>
          <input id="threshold" v-model.number="form.threshold" type="number" min="0" required />
        </div>
        <button type="submit" :disabled="saving">{{ saving ? '등록 중...' : '등록' }}</button>
      </form>
    </div>

    <!-- ===== 내 재고 목록 ===== -->
    <div v-if="list.error.value" class="error-msg">{{ list.error.value }}</div>
    <div v-if="list.loading.value" class="loading">불러오는 중...</div>

    <template v-else>
      <div v-if="!items.length" class="empty">등록된 재고가 없습니다.</div>
      <table v-else>
        <thead>
          <tr><th>원료코드</th><th>원료명</th><th>현재고</th><th>임계치</th><th>상태</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="i in items" :key="i.inventoryId ?? i.id">
            <td>{{ i.materialCode }}</td>
            <td>{{ i.materialName ?? '-' }}</td>
            <td>
              <input type="number" min="0" style="width:100px" v-model.number="stockDraft[i.inventoryId ?? i.id]" />
              <button type="button" class="secondary" @click="saveStock(i)">저장</button>
            </td>
            <td>{{ formatNumber(i.threshold) }}</td>
            <td>
              <span class="badge">{{ isShort(i) ? '부족' : '정상' }}</span>
            </td>
            <td>
              <router-link v-if="isShort(i)" :to="`/recommend?materialCode=${i.materialCode}`">공급처 찾기</router-link>
            </td>
          </tr>
        </tbody>
      </table>
    </template>
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

const isShort = (i) => Number(i.currentStock) < Number(i.threshold)
const formatNumber = (v) => (v == null ? '-' : Number(v).toLocaleString('ko-KR'))

onMounted(load)
</script>
