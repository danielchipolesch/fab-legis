<template>
  <v-app>
    <AppTopBar />
    <v-main>
      <router-view />
    </v-main>
  </v-app>
</template>

<script setup>
import AppTopBar from '@/components/common/AppTopBar.vue'
</script>

<style>
html,
body {
  background-color: #e8e8e8 !important;
}
</style>
