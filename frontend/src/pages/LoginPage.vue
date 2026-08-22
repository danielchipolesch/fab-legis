<template>
  <q-page class="login-page column items-center justify-center">
    <q-card flat bordered style="width:380px; max-width:92vw">
      <q-card-section class="column items-center q-pt-xl q-pb-md">
        <q-avatar size="56px" color="primary" text-color="white" square style="border-radius:8px">
          <q-icon name="mdi-gavel" size="30px" />
        </q-avatar>
        <div class="text-h6 text-weight-bold text-primary q-mt-md">FAB Legis</div>
        <div class="text-caption text-grey-7">Gestão de Legislação do COMAER</div>
      </q-card-section>

      <q-separator />

      <q-card-section class="q-pa-lg">
        <q-form @submit.prevent="entrar" class="column" style="gap:4px">
          <q-input
            :model-value="cpf"
            @update:model-value="val => cpf = mascaraCpf(val)"
            label="CPF"
            outlined
            dense
            :rules="[v => !!onlyDigits(v) || 'Informe o CPF', v => validarCpf(v) || 'CPF inválido']"
            lazy-rules
            maxlength="14"
            autofocus
          >
            <template #prepend><q-icon name="mdi-card-account-details-outline" /></template>
          </q-input>

          <q-input
            v-model="senha"
            label="Senha"
            outlined
            dense
            :type="mostrarSenha ? 'text' : 'password'"
            :rules="[v => !!v || 'Informe a senha']"
            lazy-rules
          >
            <template #prepend><q-icon name="mdi-lock-outline" /></template>
            <template #append>
              <q-icon
                :name="mostrarSenha ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
                class="cursor-pointer"
                @click="mostrarSenha = !mostrarSenha"
              />
            </template>
          </q-input>

          <q-banner v-if="auth.erro" dense class="bg-red-1 text-negative q-mt-sm" rounded>
            {{ auth.erro }}
          </q-banner>

          <q-btn
            type="submit"
            color="primary"
            unelevated
            class="q-mt-md"
            :loading="auth.loading"
            label="Entrar"
          />
        </q-form>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'
import { validarCpf, mascaraCpf, onlyDigits } from '@/utils/cpf.js'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const cpf = ref('')
const senha = ref('')
const mostrarSenha = ref(false)

async function entrar() {
  if (!validarCpf(cpf.value)) return
  const ok = await auth.login(cpf.value, senha.value)
  if (ok) {
    router.replace(route.query.redirect ?? { name: 'home' })
  }
}
</script>

<style scoped>
.login-page {
  background: var(--color-background, #F4F6FA);
  min-height: 100vh;
}
</style>
