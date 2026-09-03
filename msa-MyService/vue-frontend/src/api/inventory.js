import api, { unwrap } from './index.js'

/**
 * /api/inventories/*  — 담당: (팀원) order-service 재고 영역
 * Sprint 1 / Ep-02 US1
 */
export const inventoryApi = {
  /** POST /api/inventories — 보유 원료 재고·임계치 등록 (BUYER) */
  create(payload) {
    return api.post('/api/inventories', payload).then(unwrap)
  },

  /** GET /api/inventories/my — 내 재고 목록 */
  my(params) {
    return api.get('/api/inventories/my', { params }).then(unwrap)
  },

  /** PATCH /api/inventories/{id}/stock — 현재 재고량 수정 */
  updateStock(id, currentStock) {
    return api.patch(`/api/inventories/${id}/stock`, { currentStock }).then(unwrap)
  },

  /** GET /api/inventories/alerts — 재고 부족 감지 알림 조회 (Ep-02 US1) */
  alerts() {
    return api.get('/api/inventories/alerts').then(unwrap)
  }
}
