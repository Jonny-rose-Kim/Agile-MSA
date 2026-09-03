import api, { unwrap } from './index.js'

/**
 * /api/materials/*  — 담당: (팀원) material-service
 * Sprint 1
 */
export const materialApi = {
  /** GET /api/materials — 목록·검색 (keyword, category, certification, minCapacity, page, size, sort) */
  list(params) {
    return api.get('/api/materials', { params }).then(unwrap)
  },

  /** GET /api/materials/{id} — 상세 */
  get(id) {
    return api.get(`/api/materials/${id}`).then(unwrap)
  },

  /** GET /api/materials/code/{materialCode}/suppliers — 공급 가능 인증 공장 조회 (Ep-01 US1) */
  suppliersByCode(materialCode, params) {
    return api.get(`/api/materials/code/${materialCode}/suppliers`, { params }).then(unwrap)
  },

  /** POST /api/materials — 공급 원료 등록 (SUPPLIER) */
  create(payload) {
    return api.post('/api/materials', payload).then(unwrap)
  },

  /** PUT /api/materials/{id} — 원료 정보 수정 (SUPPLIER 본인) */
  update(id, payload) {
    return api.put(`/api/materials/${id}`, payload).then(unwrap)
  },

  /** PATCH /api/materials/{id}/capacity — 여유 생산능력 갱신 (Ep-02 US2) */
  updateCapacity(id, availableCapacity) {
    return api.patch(`/api/materials/${id}/capacity`, { availableCapacity }).then(unwrap)
  },

  /** GET /api/materials/my — 내 공급 품목 (SUPPLIER) */
  my(params) {
    return api.get('/api/materials/my', { params }).then(unwrap)
  },

  /** DELETE /api/materials/{id} — 공급 중단 (Soft Delete) */
  remove(id) {
    return api.delete(`/api/materials/${id}`).then(unwrap)
  }
}

/** 화면 셀렉트박스용 상수 — 서버 Enum과 반드시 일치시킬 것 */
export const MATERIAL_CATEGORIES = [
  { code: 'API_INGREDIENT', label: '원료의약품(주성분)' },
  { code: 'EXCIPIENT', label: '부형제' },
  { code: 'SOLVENT', label: '용매' },
  { code: 'REAGENT', label: '시약' },
  { code: 'INTERMEDIATE', label: '중간체' },
  { code: 'OTHER', label: '기타' }
]

export const CERTIFICATIONS = ['GMP', 'DMF', 'ISO9001', 'KGMP']
export const UNITS = ['KG', 'L', 'EA']
