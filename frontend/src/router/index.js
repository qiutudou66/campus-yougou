import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/shops',
    name: 'shops',
    component: () => import('@/views/ShopListView.vue'),
    meta: { title: '逛店铺' }
  },
  {
    path: '/shop/:id',
    name: 'shop-detail',
    component: () => import('@/views/ShopDetailView.vue'),
    meta: { title: '店铺详情' }
  },
  {
    path: '/blog/new',
    name: 'blog-edit',
    component: () => import('@/views/BlogEditView.vue'),
    meta: { title: '发布动态', requiresAuth: true }
  },
  {
    path: '/blog/:id',
    name: 'blog-detail',
    component: () => import('@/views/BlogDetailView.vue'),
    meta: { title: '动态详情', requiresAuth: true }
  },
  {
    path: '/chat',
    name: 'chat',
    component: () => import('@/views/ChatView.vue'),
    meta: { title: 'AI 助手' }
  },
  {
    path: '/me',
    name: 'me',
    component: () => import('@/views/ProfileView.vue'),
    meta: { title: '我的', requiresAuth: true }
  },
  {
    path: '/me/edit',
    name: 'me-edit',
    component: () => import('@/views/ProfileEditView.vue'),
    meta: { title: '编辑资料', requiresAuth: true }
  },
  {
    path: '/user/:id',
    name: 'user-profile',
    component: () => import('@/views/UserProfileView.vue'),
    meta: { title: '同学主页', requiresAuth: true }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面走丢了' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0, behavior: 'smooth' }
  }
})

router.beforeEach((to) => {
  const token = localStorage.getItem('campus_token')
  if (to.meta.requiresAuth && !token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 校园优购` : '校园优购'
})

export default router
