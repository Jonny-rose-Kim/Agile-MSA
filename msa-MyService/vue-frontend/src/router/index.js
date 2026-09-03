import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth.js'

/**
 * meta.roles: 접근 가능한 역할 목록. 비어 있으면 로그인만 확인한다.
 * meta.flush: 본문 최대폭·여백을 없앤 전체화면 레이아웃(랜딩·인증 화면 전용).
 * 화면을 추가할 때 라우트만 등록하면 권한 가드가 자동 적용된다.
 */
const routes = [
  { path: '/', name: 'Landing', component: () => import('@/views/LandingView.vue'), meta: { flush: true } },
  { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue'), meta: { guestOnly: true, flush: true } },
  { path: '/callback', name: 'Callback', component: () => import('@/views/CallbackView.vue'), meta: { flush: true } },

  // --- /api/materials --- 담당: 팀원
  { path: '/materials', name: 'MaterialList', component: () => import('@/views/material/MaterialListView.vue'), meta: { requiresAuth: true } },
  { path: '/materials/new', name: 'MaterialCreate', component: () => import('@/views/material/MaterialFormView.vue'), meta: { requiresAuth: true, roles: ['SUPPLIER'] } },
  { path: '/materials/:id(\\d+)', name: 'MaterialDetail', component: () => import('@/views/material/MaterialDetailView.vue'), meta: { requiresAuth: true } },
  { path: '/materials/:id(\\d+)/edit', name: 'MaterialEdit', component: () => import('@/views/material/MaterialFormView.vue'), meta: { requiresAuth: true, roles: ['SUPPLIER'] } },
  { path: '/supplier/materials', name: 'SupplierMaterials', component: () => import('@/views/material/SupplierMaterialsView.vue'), meta: { requiresAuth: true, roles: ['SUPPLIER'] } },

  // --- /api/inventories --- 담당: 팀원
  { path: '/inventories', name: 'InventoryList', component: () => import('@/views/inventory/InventoryListView.vue'), meta: { requiresAuth: true, roles: ['BUYER'] } },
  { path: '/inventories/alerts', name: 'InventoryAlerts', component: () => import('@/views/inventory/InventoryAlertView.vue'), meta: { requiresAuth: true, roles: ['BUYER'] } },

  // --- /api/orders --- 담당: 팀원
  { path: '/orders', name: 'OrderList', component: () => import('@/views/order/OrderListView.vue'), meta: { requiresAuth: true, roles: ['BUYER'] } },
  { path: '/orders/:id(\\d+)', name: 'OrderDetail', component: () => import('@/views/order/OrderDetailView.vue'), meta: { requiresAuth: true } },
  { path: '/supplier/orders', name: 'SupplierOrders', component: () => import('@/views/order/SupplierOrderView.vue'), meta: { requiresAuth: true, roles: ['SUPPLIER'] } },

  // --- /api/payments --- 담당: 팀원
  { path: '/payments', name: 'PaymentList', component: () => import('@/views/payment/PaymentListView.vue'), meta: { requiresAuth: true, roles: ['BUYER'] } },

  // --- /api/recommend --- 담당: 팀원
  { path: '/recommend', name: 'Recommend', component: () => import('@/views/recommend/RecommendView.vue'), meta: { requiresAuth: true } },

  // --- /api/users --- 담당: 프론트엔드(로그인)
  { path: '/mypage', name: 'MyPage', component: () => import('@/views/MyPageView.vue'), meta: { requiresAuth: true } },

  { path: '/:pathMatch(.*)*', name: 'NotFound', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  // 새로고침으로 user만 비었을 때 복구 (토큰은 sessionStorage에 남아 있음)
  if (auth.isAuthenticated && !auth.user) {
    await auth.fetchUser()
  }

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }

  if (to.meta.guestOnly && auth.isAuthenticated) {
    return auth.homeRoute
  }

  if (to.meta.roles?.length && !to.meta.roles.includes(auth.user?.role)) {
    return auth.homeRoute
  }
})

export default router
