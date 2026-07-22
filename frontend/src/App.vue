<template>
  <q-layout view="hHh lpR fFf">
    <AppTopBar />
    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
import AppTopBar from '@/components/common/AppTopBar.vue'
</script>

<style>
html,
body,
#app,
.q-layout,
.q-page-container {
  background-color: #F0F0F0;
}
</style>
