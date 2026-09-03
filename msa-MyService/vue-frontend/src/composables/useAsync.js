import { ref, shallowRef } from 'vue'
import { errorMessage } from '@/api/index.js'

/**
 * 비동기 호출의 loading / error / data 상태를 한 곳에서 관리한다.
 *
 * 화면(.vue)은 이 함수가 돌려주는 상태만 렌더링하므로,
 * 나중에 디자인을 바꿔도 마크업만 갈아끼우면 기능은 그대로 동작한다.
 *
 * 사용 예)
 *   const materials = useAsync(materialApi.list, { initial: { content: [] } })
 *   await materials.run({ page: 0, size: 20 })
 *   materials.data / materials.loading / materials.error
 */
export function useAsync(fn, { initial = null } = {}) {
  const data = shallowRef(initial)
  const loading = ref(false)
  const error = ref('')

  /** 예외를 던지지 않는다. 성공 여부는 반환값 ok로 판단한다. */
  async function run(...args) {
    loading.value = true
    error.value = ''
    try {
      data.value = await fn(...args)
      return { ok: true, data: data.value }
    } catch (e) {
      error.value = errorMessage(e)
      return { ok: false, error: error.value }
    } finally {
      loading.value = false
    }
  }

  function reset() {
    data.value = initial
    error.value = ''
    loading.value = false
  }

  return { data, loading, error, run, reset }
}
