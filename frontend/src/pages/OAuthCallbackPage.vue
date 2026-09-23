<template>
  <q-page class="callback-page column items-center justify-center">
    <q-spinner v-if="!erro" color="primary" size="42px" />
    <q-banner v-else dense class="bg-red-1 text-negative q-mt-md" rounded style="max-width:420px">
      {{ erro }}
      <template #action>
        <q-btn flat color="negative" label="Voltar ao login" :to="{ name: 'login' }" />
      </template>
    </q-banner>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const erro = ref(null)

onMounted(async () => {
  // Renovação silenciosa (auth.js refresh()) carrega esta mesma rota dentro
  // de um iframe oculto -- o PAI lê a URL final (code/state) direto do
  // iframe.contentWindow.location, sem precisar que nenhum JS rode aqui
  // dentro. Se este código também trocasse o code, teria duas trocas correndo
  // pro mesmo code (que só serve uma vez), uma delas falhando por corrida --
  // então quando estamos dentro de um iframe, não fazemos nada, só deixamos o
  // documento carregar pro pai poder ler a URL.
  if (window !== window.top) return

  const { code, state, error, error_description: erroDescricao } = route.query
  if (error) {
    erro.value = erroDescricao ?? 'Login cancelado ou negado.'
    return
  }
  if (!code || !state) {
    erro.value = 'Retorno de login inválido.'
    return
  }
  try {
    const redirect = await auth.trocarCodePorToken(code, state)
    router.replace(redirect)
  } catch (e) {
    erro.value = e?.message ?? 'Não foi possível concluir o login.'
  }
})
</script>

<style scoped>
.callback-page {
  background: var(--color-background, #F4F6FA);
  min-height: 100vh;
}
</style>
