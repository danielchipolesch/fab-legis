import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/pages/LoginPage.vue'),
    meta: { title: 'Entrar', public: true, paginaAvulsa: true },
  },
  {
    path: '/',
    name: 'home',
    component: () => import('@/pages/HomePage.vue'),
    meta: { title: 'Gestão de Legislação' },
  },
  {
    path: '/documento/novo',
    name: 'documento-novo',
    component: () => import('@/pages/DocumentoEditorPage.vue'),
    meta: { title: 'Novo Documento' },
  },
  {
    path: '/documento/:id/editar',
    name: 'documento-editar',
    component: () => import('@/pages/DocumentoEditorPage.vue'),
    meta: { title: 'Editar Documento' },
  },
  {
    path: '/documento/:id/visualizar',
    name: 'documento-visualizar',
    component: () => import('@/pages/DocumentoViewerPage.vue'),
    meta: { title: 'Visualizar Documento' },
  },
  {
    path: '/documento/:id/comparar',
    name: 'documento-comparar',
    component: () => import('@/pages/ComparisonPage.vue'),
    meta: { title: 'Comparar Versões' },
  },
  {
    path: '/usuarios',
    name: 'usuarios',
    component: () => import('@/pages/UsuariosPage.vue'),
    meta: { title: 'Gestão de Usuários', requiresAdmin: true },
  },
  {
    path: '/auditoria',
    name: 'auditoria',
    component: () => import('@/pages/AuditoriaPage.vue'),
    meta: { title: 'Auditoria', requiresAuditor: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  document.title = `${to.meta.title} — FAB Legis`

  if (to.meta.public) return true

  const auth = useAuthStore()
  if (!auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: 'home' }
  }
  if (to.meta.requiresAuditor && !auth.isAuditor) {
    return { name: 'home' }
  }
  return true
})

export default router
