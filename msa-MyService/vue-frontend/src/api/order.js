import api, { unwrap } from './index.js'

/**
 * /api/orders/*  — 담당: (팀원) order-service 주문 영역
 * Sprint 1: 조달 신청(PENDING), 목록, 상세
 * Sprint 2: 상태 폴링, 공장 수주 목록 (Ep-01 US2)
 */
export const orderApi = {
  /** POST /api/orders — 조달 신청 (BUYER) */
  create(payload) {
    return api.post('/api/orders', payload).then(unwrap)
  },

  /** GET /api/orders/my — 내 조달 주문 목록 */
  my(params) {
    return api.get('/api/orders/my', { params }).then(unwrap)
  },

  /** GET /api/orders/{id} — 주문 상세 */
  get(id) {
    return api.get(`/api/orders/${id}`).then(unwrap)
  },

  /** [Sprint 2] GET /api/orders/{id}/status — 상태 폴링용 경량 조회 */
  getStatus(id) {
    return api.get(`/api/orders/${id}/status`).then(unwrap)
  },

  /** [Sprint 2] GET /api/orders/supplier — 공장이 수주한 주문 목록 (SUPPLIER) */
  supplier(params) {
    return api.get('/api/orders/supplier', { params }).then(unwrap)
  }
}
