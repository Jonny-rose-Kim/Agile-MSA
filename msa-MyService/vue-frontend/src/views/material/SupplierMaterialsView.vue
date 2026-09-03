<!--
  담당 API: GET /api/materials/my  ·  PATCH /api/materials/{id}/capacity  ·  DELETE /api/materials/{id}
  담당자  : (팀원) material-service
-->
<template>
  <div>
    <h1 class="page-title">내 공급 품목</h1>
    <p class="page-desc">
      GET /api/materials/my — 여유 생산능력은 표에서 바로 수정할 수 있습니다 (Ep-02 US2).
    </p>

    <div class="actions">
      <router-link to="/materials/new"><button type="button">공급 품목 등록</button></router-link>
      <button type="button" class="secondary" @click="load" :disabled="list.loading.value">새로고침</button>
    </div>

    <div v-if="list.error.value" class="error-msg">{{ list.error.value }}</div>
    <div v-if="rowMessage" class="success-msg">{{ rowMessage }}</div>
    <div v-if="list.loading.value" class="loading">불러오는 중...</div>

    <template v-else>
      <div v-if="!items.length" class="empty">등록한 공급 품목이 없습니다.</div>

      <table v-else>
        <thead>
          <tr>
            <th>원료코드</th><th>원료명</th><th>단가</th>
            <th>여유 생산능력</th><th>리드타임</th><th>상태</th><th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in items" :key="m.id">
            <td>{{ m.materialCode }}</td>
            <td>{{ m.materialName }}</td>
            <td>{{ formatNumber(m.unitPrice) }} 원</td>
            <td>
              <!-- PATCH /api/materials/{id}/capacity : 숫자만 고치고 저장 -->
              <input type="number" min="0" style="width:110px"
                     v-model.number="capacityDraft[m.id]" />
              <button type="button" class="secondary"
                      :disabled="savingId === m.id || capacityDraft[m.id] === m.availableCapacity"
                      @click="saveCapacity(m)">
                {{ savingId === m.id ? '저장 중' : '저장' }}
              </button>
            </td>
            <td>{{ m.leadTimeDays }}일</td>
            <td><span class="badge">{{ m.status }}</span></td>
            <td>
              <router-link :to="`/materials/${m.id}/edit`">수정</router-link>
              <button type="button" class="danger" @click="stopSupply(m)">공급 중단</button>
            </td>
          </tr>
        </tbody>
      </table>
    </template>
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
