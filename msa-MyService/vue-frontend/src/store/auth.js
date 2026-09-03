import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth.js'
import { userApi } from '@/api/user.js'
import { errorMessage } from '@/api/index.js'

const AUTH_SERVER_URL = import.meta.env.VITE_AUTH_SERVER_URL || 'http://localhost:8080'

/**
 * 로그인 상태 저장소 — 담당: 프론트엔드(로그인)
 *
 * 화면은 이 스토어의 isBuyer / isSupplier만 보고 메뉴와 버튼 노출을 결정한다.
 * 역할 판단 로직이 여기 한 곳에만 있으므로 디자인을 바꿔도 권한 분기는 그대로 동작한다.
 */
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(sessionStorage.getItem('access_token') || null)
  const user = ref(JSON.parse(sessionStorage.getItem('user') || 'null'))

  const isAuthenticated = computed(() => !!accessToken.value)
  const isBuyer = computed(() => user.value?.role === 'BUYER')       // 제약사·연구실
  const isSupplier = computed(() => user.value?.role === 'SUPPLIER') // 공급 공장

  /** 로그인 직후 이동할 홈. 역할별로 첫 화면이 다르다. */
  const homeRoute = computed(() => (isSupplier.value ? '/supplier/materials' : '/materials'))

  function setToken(token) {
    accessToken.value = token
    sessionStorage.setItem('access_token', token)
  }

  function setUser(userData) {
    user.value = userData
    sessionStorage.setItem('user', JSON.stringify(userData))
  }

  /** GET /api/users/me — 토큰으로 내 정보를 채운다. */
  async function fetchUser() {
    try {
      const data = await userApi.getMe()
      if (!data || typeof data !== 'object') {
        throw new Error('사용자 정보 형식이 올바르지 않습니다')
      }
      setUser(data)
      return { ok: true, data }
    } catch (e) {
      logout(false)
      return { ok: false, error: errorMessage(e, '사용자 정보를 불러오지 못했습니다') }
    }
  }

  /** POST /api/users/register — 회원가입 (가입 후 자동 로그인은 하지 않는다) */
  async function register(payload) {
    try {
      const data = await userApi.register(payload)
      return { ok: true, data }
    } catch (e) {
      return { ok: false, error: errorMessage(e, '회원가입에 실패했습니다') }
    }
  }

  function logout(redirect = true) {
    accessToken.value = null
    user.value = null
    sessionStorage.removeItem('access_token')
    sessionStorage.removeItem('user')
    if (redirect) window.location.href = '/login'
  }

  /** GET /oauth2/authorize — axios가 아니라 브라우저를 통째로 이동시킨다. */
  function redirectToLogin() {
    const params = new URLSearchParams({
      response_type: 'code',
      client_id: import.meta.env.VITE_CLIENT_ID,
      redirect_uri: import.meta.env.VITE_REDIRECT_URI,
      scope: 'openid profile read write'
    })
    window.location.href = `${AUTH_SERVER_URL}/oauth2/authorize?${params.toString()}`
  }

  /** /callback 진입 시 1회: 인가코드 → 토큰 → 내 정보 */
  async function handleCallback(code) {
    try {
      const res = await authApi.exchangeCode(code)
      const token = res?.data?.access_token
      if (!token) throw new Error('액세스 토큰을 받지 못했습니다')
      setToken(token)
      return await fetchUser()
    } catch (e) {
      return { ok: false, error: errorMessage(e, '로그인 처리에 실패했습니다') }
    }
  }

  return {
    accessToken, user,
    isAuthenticated, isBuyer, isSupplier, homeRoute,
    setToken, setUser, fetchUser, register, logout, redirectToLogin, handleCallback
  }
})
