import { createRouter, createWebHistory } from 'vue-router'
import { Notify } from 'quasar'
import { useAuthStore } from '@/stores/auth.js'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/pages/LoginPage.vue'),
    meta: { title: 'Entrar', public: true, paginaAvulsa: true },
  },
  {
    path: '/callback',
    name: 'oauth-callback',
    component: () => import('@/pages/OAuthCallbackPage.vue'),
    meta: { title: 'Entrando…', public: true, paginaAvulsa: true },
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
    path: '/busca',
    name: 'busca',
    component: () => import('@/pages/BuscaPage.vue'),
    meta: { title: 'Busca Textual' },
  },
  {
    path: '/revisao',
    name: 'revisao',
    component: () => import('@/pages/RevisaoPage.vue'),
    meta: { title: 'Revisão', requiresAprovador: true },
  },
  {
    path: '/publicacao',
    name: 'publicacao',
    component: () => import('@/pages/PublicacaoPage.vue'),
    meta: { title: 'Publicação', requiresPublicador: true },
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
  if (to.meta.requiresAprovador && !auth.isAprovador) {
    return { name: 'home' }
  }
  if (to.meta.requiresPublicador && !auth.isPublicador) {
    return { name: 'home' }
  }
  return true
})

// Navegação que falha (ex.: a tela é carregada sob demanda e o módulo não pôde ser baixado -- app
// atualizado desde que a página foi aberta, servidor reiniciando) não pode ser silenciosa: sem isto
// o link parece "ativo" mas nada acontece. Mesmo feedback (canto inferior direito) das demais falhas.
const ERRO_CARGA_MODULO = /dynamically imported module|Importing a module script failed|error loading dynamically|Failed to fetch/i

router.onError((erro, to) => {
  console.error('[router] Falha ao navegar:', erro)
  const falhaDeCarga = ERRO_CARGA_MODULO.test(String(erro?.message ?? erro))
  Notify.create({
    type: 'negative',
    message: falhaDeCarga
      ? 'Não foi possível abrir esta tela: a aplicação foi atualizada ou perdeu a conexão com o servidor.'
      : 'Não foi possível abrir esta tela. Tente novamente.',
    timeout: falhaDeCarga ? 0 : 6000,
    actions: falhaDeCarga
      ? [{ label: 'Recarregar', color: 'white', handler: () => { window.location.href = to?.fullPath ?? '/' } }]
      : [{ icon: 'mdi-close', color: 'white', round: true, dense: true }],
  })
})

export default router
