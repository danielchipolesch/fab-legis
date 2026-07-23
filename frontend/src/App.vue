<template>
  <q-layout view="hHh lpR fFf">
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
import AppTopBar from '@/components/common/AppTopBar.vue'
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
