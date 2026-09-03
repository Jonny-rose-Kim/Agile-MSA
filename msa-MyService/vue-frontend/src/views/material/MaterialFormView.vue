<!--
  담당 API: POST /api/materials  ·  PUT /api/materials/{id}  ·  GET /api/materials/{id}
  담당자  : (팀원) material-service
-->
<template>
  <div>
    <div class="page-head">
      <div class="page-head__text">
        <h1 class="page-title">{{ isEdit ? '공급 품목 수정' : '공급 품목 등록' }}</h1>
        <p class="page-desc">supplierId는 보내지 않습니다. API Gateway가 토큰에서 주입합니다.</p>
      </div>
    </div>

    <div class="stack">
      <div v-if="error" class="alert alert--error" role="alert">
        <svg class="alert__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="12" cy="12" r="9" /><path d="M12 7.5v5M12 16h.01" />
        </svg>
        {{ error }}
      </div>
      <div v-if="success" class="alert alert--success" role="status">
        <svg class="alert__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="12" cy="12" r="9" /><path d="m8.5 12 2.5 2.5 4.5-5" />
        </svg>
        {{ success }}
      </div>

      <div v-if="loadingDetail" class="card">
        <div class="skeleton-rows">
          <span class="skeleton"></span><span class="skeleton"></span><span class="skeleton"></span>
        </div>
      </div>

      <form v-else class="stack" @submit.prevent="submit">
        <!-- ===== 식별 정보 ===== -->
        <section class="card">
          <header class="card__head"><h2 class="card__title">식별 정보</h2></header>
          <div class="card__body">
            <div class="form-grid">
              <div class="field">
                <label class="field__label" for="materialCode">원료코드 <span class="req">*</span></label>
                <input id="materialCode" class="input" v-model.trim="form.materialCode" type="text" required
                       :disabled="isEdit" placeholder="API-CEFA-500" />
                <span v-if="isEdit" class="field__hint">원료코드는 등록 후 변경할 수 없습니다.</span>
              </div>
              <div class="field">
                <label class="field__label" for="materialName">원료명 <span class="req">*</span></label>
                <input id="materialName" class="input" v-model.trim="form.materialName" type="text"
                       required placeholder="세파졸린나트륨" />
              </div>
              <div class="field">
                <label class="field__label" for="casNumber">CAS 번호</label>
                <input id="casNumber" class="input" v-model.trim="form.casNumber" type="text"
                       placeholder="27164-46-1" />
              </div>
              <div class="field">
                <label class="field__label" for="category">카테고리 <span class="req">*</span></label>
                <select id="category" class="select" v-model="form.category" required>
                  <option v-for="c in MATERIAL_CATEGORIES" :key="c.code" :value="c.code">{{ c.label }}</option>
                </select>
              </div>
            </div>
          </div>
        </section>

        <!-- ===== 거래 조건 ===== -->
        <section class="card">
          <header class="card__head"><h2 class="card__title">거래 조건</h2></header>
          <div class="card__body">
            <div class="form-grid">
              <div class="field">
                <label class="field__label" for="unit">단위 <span class="req">*</span></label>
                <select id="unit" class="select" v-model="form.unit" required>
                  <option v-for="u in UNITS" :key="u" :value="u">{{ u }}</option>
                </select>
              </div>
              <div class="field">
                <label class="field__label" for="unitPrice">단가(원) <span class="req">*</span></label>
                <input id="unitPrice" class="input" v-model.number="form.unitPrice" type="number" min="0" required />
              </div>
              <div class="field">
                <label class="field__label" for="minOrderQuantity">최소 주문 수량</label>
                <input id="minOrderQuantity" class="input" v-model.number="form.minOrderQuantity"
                       type="number" min="0" />
              </div>
              <div class="field">
                <label class="field__label" for="availableCapacity">여유 생산능력 <span class="req">*</span></label>
                <input id="availableCapacity" class="input" v-model.number="form.availableCapacity"
                       type="number" min="0" required />
                <span class="field__hint">실제 공급 가능한 물량만 노출됩니다. (Ep-02 US2)</span>
              </div>
              <div class="field">
                <label class="field__label" for="leadTimeDays">리드타임(일) <span class="req">*</span></label>
                <input id="leadTimeDays" class="input" v-model.number="form.leadTimeDays"
                       type="number" min="1" required />
              </div>
              <div class="field">
                <label class="field__label" for="country">생산 국가</label>
                <input id="country" class="input" v-model.trim="form.country" type="text" placeholder="KR" />
              </div>
            </div>
          </div>
        </section>

        <!-- ===== 인증 · 설명 ===== -->
        <section class="card">
          <header class="card__head"><h2 class="card__title">인증 · 설명</h2></header>
          <div class="card__body">
            <div class="form-grid form-grid--wide">
              <div class="field">
                <span class="field__label">보유 인증</span>
                <div class="u-row">
                  <label v-for="c in CERTIFICATIONS" :key="c" class="check">
                    <input type="checkbox" :value="c" v-model="form.certifications" /> {{ c }}
                  </label>
                </div>
              </div>
              <div class="field">
                <label class="field__label" for="description">설명</label>
                <textarea id="description" class="textarea" v-model.trim="form.description" rows="3"></textarea>
              </div>
            </div>
          </div>
          <div class="card__foot">
            <p class="u-sm u-muted"><span class="req">*</span> 표시는 필수 항목입니다.</p>
            <div class="actions">
              <button type="button" class="btn btn--secondary" @click="$router.back()">취소</button>
              <button type="submit" class="btn" :disabled="saving">
                <span v-if="saving" class="spinner" aria-hidden="true"></span>
                {{ saving ? '저장 중...' : (isEdit ? '수정' : '등록') }}
              </button>
            </div>
          </div>
        </section>
      </form>
    </div>
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
