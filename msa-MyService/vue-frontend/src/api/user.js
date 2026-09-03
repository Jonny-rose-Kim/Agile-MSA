import api, { unwrap } from './index.js'

/**
 * /api/users/*  — 담당: 프론트엔드(로그인)
 * 모든 함수는 공통 래퍼를 벗긴 data를 반환한다.
 */
export const userApi = {
  /** POST /api/users/register — 회원가입 (인증 불필요) */
  register(payload) {
    return api.post('/api/users/register', payload).then(unwrap)
  },

  /** GET /api/users/me — 내 정보 (역할 분기의 기준점) */
  getMe() {
    return api.get('/api/users/me').then(unwrap)
  },

  /** GET /api/users/{id} — 공급사 정보 조회 */
  getById(id) {
    return api.get(`/api/users/${id}`).then(unwrap)
  }
}
