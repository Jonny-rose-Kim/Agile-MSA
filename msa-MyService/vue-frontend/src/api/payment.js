import api, { unwrap } from './index.js'

/**
 * /api/payments/*  — 담당: (팀원) payment-service
 * Sprint 2
 */
export const paymentApi = {
  /** POST /api/payments — 조달 결제 요청 (BUYER) */
  create(payload) {
    return api.post('/api/payments', payload).then(unwrap)
  },

  /** GET /api/payments/my — 내 결제 내역 */
  my(params) {
    return api.get('/api/payments/my', { params }).then(unwrap)
  }
}
