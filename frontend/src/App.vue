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
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppTopBar from '@/components/common/AppTopBar.vue'

// Páginas "avulsas" (ver meta.paginaAvulsa nas rotas) não usam o shell do
// app -- hoje só o login, que é a própria porta de entrada e não deve
// carregar topbar/rodapé nem depender de uma sessão para renderizar.
const route = useRoute()
const isPaginaAvulsa = computed(() => !!route.meta.paginaAvulsa)
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
