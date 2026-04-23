import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/pages/HomePage.vue'),
    meta: { title: 'Gestão de Legislação' },
  },
  {
    path: '/documento/novo',
    name: 'documento-novo',
    component: () => import('@/pages/DocumentEditorPage.vue'),
    meta: { title: 'Novo Documento' },
  },
  {
    path: '/documento/:id/editar',
    name: 'documento-editar',
    component: () => import('@/pages/DocumentEditorPage.vue'),
    meta: { title: 'Editar Documento' },
  },
  {
    path: '/documento/:id/comparar',
    name: 'documento-comparar',
    component: () => import('@/pages/ComparisonPage.vue'),
    meta: { title: 'Comparar Versões' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  document.title = `${to.meta.title} — FAB Legis`
})

export default router
