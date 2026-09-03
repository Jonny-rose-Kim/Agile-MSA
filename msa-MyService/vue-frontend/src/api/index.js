import axios from 'axios'
import { useAuthStore } from '@/store/auth.js'

/**
 * 공통 axios 인스턴스
 * - baseURL은 비워둔다: vite.config.js의 proxy가 /api, /oauth2를 Gateway(8080)로 전달한다.
 * - 프론트는 개별 서비스 포트(8081~8085)를 직접 호출하지 않는다.
 */
const api = axios.create({
  baseURL: '',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

// [요청] 모든 호출에 Bearer 토큰 자동 첨부
// X-User-Id는 Gateway가 JWT에서 추출해 주입하므로 프론트에서 절대 보내지 않는다.
api.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

// [응답] 401이면 토큰을 폐기하고 로그인으로 보낸다.
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      const auth = useAuthStore()
      auth.logout(false)
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  }
)

/**
 * 공통 응답 래퍼 { success, message, data } 에서 data만 꺼낸다.
 * 모든 api 모듈은 이 함수를 통과시켜 반환하므로, 화면에서는 res.data.data를 다룰 일이 없다.
 */
export function unwrap(res) {
  return res?.data?.data ?? res?.data
}

/** 서버가 내려준 한글 message를 우선 사용한다. */
export function errorMessage(err, fallback = '요청 처리 중 오류가 발생했습니다') {
  return err?.response?.data?.message ?? err?.message ?? fallback
}

export default api
