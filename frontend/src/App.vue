<template>
  <q-layout v-if="isPaginaAvulsa" view="hHh lpR fFf">
    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
  <q-layout v-else view="hHh lpR fFf">
    <AppTopBar />
    <q-page-container>
      <router-view />
    </q-page-container>
    <q-footer class="app-footer">
      <span class="text-caption">
        &copy; {{ new Date().getFullYear() }} FAB Legis — Gestão de Legislação do COMAER
      </span>
    </q-footer>
  </q-layout>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppTopBar from '@/components/common/AppTopBar.vue'
import { useAuthStore } from '@/stores/auth.js'

// Páginas "avulsas" (ver meta.paginaAvulsa nas rotas) não usam o shell do
// app -- hoje só o login, que é a própria porta de entrada e não deve
// carregar topbar/rodapé nem depender de uma sessão para renderizar.
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isPaginaAvulsa = computed(() => !!route.meta.paginaAvulsa)

// A guarda de rota (router/index.js) só barra ao NAVEGAR -- se a sessão cair
// enquanto o usuário já está numa página protegida (token expirado/revogado,
// detectado pelo onUnauthorized do client.js ao tomar 401 numa chamada de
// API), nada disparava a navegação de volta ao login sozinho. App.vue está
// sempre montado, então observar isAuthenticated aqui cobre esse caso.
watch(() => auth.isAuthenticated, (autenticado) => {
  if (!autenticado && !route.meta.public) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
  }
})
</script>

<style>
.app-footer {
  background: #E0E0E0;
  color: #616161;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
