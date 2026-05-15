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
body,
.v-application,
.v-application__wrap,
.v-main {
  background-color: #F0F0F0 !important;
}
</style>
