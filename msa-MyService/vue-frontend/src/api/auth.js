import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

/**
 * OAuth2 Authorization Code Flow - 토큰 교환 전용
 * 공통 api 인스턴스를 쓰지 않는다: Content-Type과 Authorization 형식이 다르다.
 * (form-urlencoded + Basic 인증)
 */
export const authApi = {
  exchangeCode(code) {
    const clientId = import.meta.env.VITE_CLIENT_ID
    const clientSecret = import.meta.env.VITE_CLIENT_SECRET
    const redirectUri = import.meta.env.VITE_REDIRECT_URI
    const credentials = btoa(`${clientId}:${clientSecret}`)

    const body = new URLSearchParams({
      grant_type: 'authorization_code',
      code,
      redirect_uri: redirectUri
    })

    return axios.post(`${API_BASE_URL}/oauth2/token`, body.toString(), {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        Authorization: `Basic ${credentials}`
      }
    })
  }
}
