import api, { unwrap } from './index.js'

/**
 * /api/recommend  — 담당: (팀원) recommend-service
 * 수요 예측(구 /api/forecast/demand)과 공장 추천을 단일 엔드포인트로 통합했다.
 * 한 번 호출로 "예측 수요 + 추천 공장"을 함께 받는다.
 */
export const recommendApi = {
  /** GET /api/recommend?materialCode=&quantity=&horizon= */
  get(params) {
    return api.get('/api/recommend', { params }).then(unwrap)
  }
}
