<!--
  담당 API: POST /api/materials  ·  PUT /api/materials/{id}  ·  GET /api/materials/{id}
  담당자  : (팀원) material-service
-->
<template>
  <div>
    <h1 class="page-title">{{ isEdit ? '공급 품목 수정' : '공급 품목 등록' }}</h1>
    <p class="page-desc">
      {{ isEdit ? 'PUT /api/materials/{id}' : 'POST /api/materials' }} — supplierId는 보내지 않습니다(Gateway가 주입).
    </p>

    <div v-if="error" class="error-msg">{{ error }}</div>
    <div v-if="success" class="success-msg">{{ success }}</div>
    <div v-if="loadingDetail" class="loading">불러오는 중...</div>

    <form v-else class="section" @submit.prevent="submit">
      <div class="form-group">
        <label class="form-label" for="materialCode">원료코드 *</label>
        <input id="materialCode" v-model.trim="form.materialCode" type="text" required
               :disabled="isEdit" placeholder="API-CEFA-500" />
      </div>
      <div class="form-group">
        <label class="form-label" for="materialName">원료명 *</label>
        <input id="materialName" v-model.trim="form.materialName" type="text" required placeholder="세파졸린나트륨" />
      </div>
      <div class="form-group">
        <label class="form-label" for="casNumber">CAS 번호</label>
        <input id="casNumber" v-model.trim="form.casNumber" type="text" placeholder="27164-46-1" />
      </div>
      <div class="form-group">
        <label class="form-label" for="category">카테고리 *</label>
        <select id="category" v-model="form.category" required>
          <option v-for="c in MATERIAL_CATEGORIES" :key="c.code" :value="c.code">{{ c.label }}</option>
        </select>
      </div>
      <div class="form-group">
        <label class="form-label" for="unit">단위 *</label>
        <select id="unit" v-model="form.unit" required>
          <option v-for="u in UNITS" :key="u" :value="u">{{ u }}</option>
        </select>
      </div>
      <div class="form-group">
        <label class="form-label" for="unitPrice">단가(원) *</label>
        <input id="unitPrice" v-model.number="form.unitPrice" type="number" min="0" required />
      </div>
      <div class="form-group">
        <label class="form-label" for="minOrderQuantity">최소 주문 수량</label>
        <input id="minOrderQuantity" v-model.number="form.minOrderQuantity" type="number" min="0" />
      </div>
      <div class="form-group">
        <label class="form-label" for="availableCapacity">여유 생산능력 * (Ep-02 US2)</label>
        <input id="availableCapacity" v-model.number="form.availableCapacity" type="number" min="0" required />
      </div>
      <div class="form-group">
        <label class="form-label" for="leadTimeDays">리드타임(일) *</label>
        <input id="leadTimeDays" v-model.number="form.leadTimeDays" type="number" min="1" required />
      </div>
      <div class="form-group">
        <label class="form-label">보유 인증</label>
        <div class="row">
          <label v-for="c in CERTIFICATIONS" :key="c" class="form-label">
            <input type="checkbox" :value="c" v-model="form.certifications" /> {{ c }}
          </label>
        </div>
      </div>
      <div class="form-group">
        <label class="form-label" for="country">생산 국가</label>
        <input id="country" v-model.trim="form.country" type="text" placeholder="KR" />
      </div>
      <div class="form-group">
        <label class="form-label" for="description">설명</label>
        <textarea id="description" v-model.trim="form.description" rows="3"></textarea>
      </div>

      <div class="actions">
        <button type="submit" :disabled="saving">{{ saving ? '저장 중...' : (isEdit ? '수정' : '등록') }}</button>
        <button type="button" class="secondary" @click="$router.back()">취소</button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { materialApi, MATERIAL_CATEGORIES, CERTIFICATIONS, UNITS } from '@/api/material.js'
import { errorMessage } from '@/api/index.js'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const loadingDetail = ref(false)
const saving = ref(false)
const error = ref('')
const success = ref('')

const form = reactive({
  materialCode: '', materialName: '', casNumber: '',
  category: 'API_INGREDIENT', unit: 'KG',
  unitPrice: null, minOrderQuantity: null,
  availableCapacity: null, leadTimeDays: null,
  certifications: [], country: 'KR', description: '', status: 'ACTIVE'
})

async function submit() {
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    if (isEdit.value) {
      await materialApi.update(route.params.id, { ...form })
      success.value = '수정되었습니다.'
    } else {
      const created = await materialApi.create({ ...form })
      router.push(`/materials/${created.id}`)
      return
    }
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  if (!isEdit.value) return
  loadingDetail.value = true
  try {
    Object.assign(form, await materialApi.get(route.params.id))
    form.certifications = form.certifications ?? []
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loadingDetail.value = false
  }
})
</script>
