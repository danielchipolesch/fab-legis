import { createRouter, createWebHistory } from 'vue-router'
import { Notify } from 'quasar'
import { useAuthStore } from '@/stores/auth.js'
import { MODULOS } from '@/regras/index.js'

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
  // Manual do usuário: aberto a todos, com ou sem login (o texto não tem nada restrito). Sem login a página leva a sua própria
  // barra (avulsaSemLogin, ver App.vue); com login, usa a barra do sistema.
  {
    path: '/manual/:pagina?',
    name: 'manual',
    component: () => import('@/pages/ManualPage.vue'),
    meta: { title: 'Manual do Usuário', public: true, avulsaSemLogin: true },
  },
  {
    path: '/',
    name: 'home',
    component: () => import('@/pages/HubPage.vue'),
    meta: { title: 'Área de Trabalho', manual: 'area-de-trabalho' },
  },
  // Painel Gerencial: o módulo dos indicadores, só para quem tem o perfil do Painel Gerencial (da OM ou Geral).
  {
    path: '/painel-gerencial',
    name: 'painel-gerencial',
    component: () => import('@/pages/PainelGerencialPage.vue'),
    meta: { title: 'Painel Gerencial', requiresPainelGerencial: true, manual: 'painel-gerencial' },
  },
  // A tela inicial de cada módulo (regras/index.js): a mesma tela, configurada pelo tipo de espécie.
  ...MODULOS.map(modulo => ({
    path: modulo.caminho,
    name: modulo.rota,
    component: () => import('@/pages/ModuloPage.vue'),
    props: { tipoDeEspecie: modulo.tipoDeEspecie },
    meta: { title: modulo.nome, manual: modulo.manual },
  })),
  {
    path: '/documento/novo',
    name: 'documento-novo',
    component: () => import('@/pages/DocumentoEditorPage.vue'),
    meta: { title: 'Novo Documento', manual: 'elaborar-um-documento' },
  },
  {
    path: '/documento/:id/editar',
    name: 'documento-editar',
    component: () => import('@/pages/DocumentoEditorPage.vue'),
    meta: { title: 'Editar Documento', manual: 'elaborar-um-documento#o-editor' },
  },
  {
    path: '/documento/:id/visualizar',
    name: 'documento-visualizar',
    component: () => import('@/pages/DocumentoViewerPage.vue'),
    meta: { title: 'Visualizar Documento', manual: 'consultas-e-exportacao#visualizar-um-documento' },
  },
  {
    path: '/documento/:id/comparar',
    name: 'documento-comparar',
    component: () => import('@/pages/ComparisonPage.vue'),
    meta: { title: 'Comparar Versões', manual: 'alterar-um-ato-publicado#comparar-versoes' },
  },
  {
    path: '/busca',
    name: 'busca',
    component: () => import('@/pages/BuscaPage.vue'),
    meta: { title: 'Busca Textual', manual: 'consultas-e-exportacao#busca-no-conteudo' },
  },
  {
    path: '/revisao',
    name: 'revisao',
    component: () => import('@/pages/RevisaoPage.vue'),
    meta: { title: 'Manter Revisão de Documento', requiresAprovador: true, manual: 'revisao-e-publicacao#aprovar-aprovador-da-om' },
  },
  {
    path: '/publicacao',
    name: 'publicacao',
    component: () => import('@/pages/PublicacaoPage.vue'),
    meta: { title: 'Manter Publicação de Documento', requiresPublicador: true, manual: 'revisao-e-publicacao#publicar-publicador-do-bca' },
  },
  {
    path: '/usuarios',
    name: 'usuarios',
    component: () => import('@/pages/UsuariosPage.vue'),
    meta: { title: 'Gestão de Usuários', requiresAdmin: true, manual: 'administracao#manter-usuarios' },
  },
  {
    path: '/habilitacao-de-documentos',
    name: 'habilitacao-de-documentos',
    component: () => import('@/pages/HabilitacaoDeDocumentosPage.vue'),
    meta: { title: 'Manter Habilitação de Espécies Normativas por OM', requiresAdminGeral: true, manual: 'administracao#habilitacao-de-especies-por-om' },
  },
  {
    path: '/distintivos-das-oms',
    name: 'distintivos-das-oms',
    component: () => import('@/pages/DistintivosDasOmsPage.vue'),
    meta: { title: 'Manter DOM de OM', requiresAdminGeral: true, manual: 'administracao#dom-distintivo-da-om' },
  },
  {
    path: '/auditoria',
    name: 'auditoria',
    component: () => import('@/pages/AuditoriaPage.vue'),
    meta: { title: 'Auditoria', requiresAuditor: true, manual: 'administracao#auditoria' },
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
  if (to.meta.requiresAdminGeral && !auth.isAdminGeral) {
    return { name: 'home' }
  }
  if (to.meta.requiresPainelGerencial && !auth.temAcessoAoPainel) {
    return { name: 'home' }
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
