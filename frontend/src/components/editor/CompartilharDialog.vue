<template>
  <q-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)">
    <q-card style="width:420px; max-width:95vw">
      <q-card-section class="row items-center q-pb-sm">
        <q-icon name="mdi-account-multiple-plus-outline" color="primary" size="22px" class="q-mr-sm" />
        <div class="text-h6 text-primary">Compartilhar documento</div>
        <q-space />
        <q-btn icon="mdi-close" flat round dense v-close-popup />
      </q-card-section>
      <div class="text-caption text-grey-7 q-px-md q-pb-sm">
        Coautores podem editar este documento junto com você e, enquanto ele
        estiver em Rascunho ou Minuta, também excluí-lo. Só você, como autor,
        pode adicionar ou remover coautores.
      </div>
      <q-separator />

      <q-card-section>
        <q-form class="row items-start" style="gap:8px" @submit.prevent="adicionar">
          <q-input
            :model-value="cpf"
            @update:model-value="val => cpf = mascaraCpf(val)"
            label="CPF do coautor"
            outlined
            dense
            class="col"
            maxlength="14"
            :error="!!erro"
            :error-message="erro"
          />
          <q-btn color="primary" unelevated icon="mdi-plus" :loading="adicionando" type="submit" style="height:40px" />
        </q-form>
      </q-card-section>

      <q-separator />

      <q-card-section class="q-pa-none" style="max-height:280px; overflow-y:auto">
        <q-list separator>
          <q-item v-for="c in compartilhamentos" :key="c.usuarioId">
            <q-item-section avatar>
              <q-avatar size="32px" color="blue-2" text-color="primary">
                <q-icon name="mdi-account" size="18px" />
              </q-avatar>
            </q-item-section>
            <q-item-section>
              <q-item-label>{{ c.nome }}</q-item-label>
              <q-item-label caption>{{ formatarCpf(c.cpf) }}</q-item-label>
            </q-item-section>
            <q-item-section side>
              <q-btn icon="mdi-delete-outline" flat round dense size="sm" color="negative" @click="remover(c)">
                <q-tooltip>Remover coautor</q-tooltip>
              </q-btn>
            </q-item-section>
          </q-item>
          <q-item v-if="!compartilhamentos.length">
            <q-item-section class="text-grey-6 text-caption text-center q-py-md">
              Nenhum coautor ainda.
            </q-item-section>
          </q-item>
        </q-list>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import * as api from '@/api/documents.js'
import { validarCpf, mascaraCpf, formatarCpf, onlyDigits } from '@/utils/cpf.js'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  documentoId: { type: [String, Number], required: true },
})
defineEmits(['update:modelValue'])

const $q = useQuasar()
const cpf = ref('')
const erro = ref('')
const adicionando = ref(false)
const compartilhamentos = ref([])

async function carregar() {
  try {
    compartilhamentos.value = await api.listCompartilhamentos(props.documentoId)
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao carregar compartilhamentos: ${e?.message ?? 'erro desconhecido'}` })
  }
}

watch(() => props.modelValue, (aberto) => { if (aberto) carregar() })

async function adicionar() {
  erro.value = ''
  if (!validarCpf(cpf.value)) {
    erro.value = 'CPF inválido'
    return
  }
  adicionando.value = true
  try {
    await api.compartilharDocumento(props.documentoId, onlyDigits(cpf.value))
    cpf.value = ''
    await carregar()
    $q.notify({ type: 'positive', message: 'Documento compartilhado.' })
  } catch (e) {
    erro.value = e?.message ?? 'Erro ao compartilhar'
  } finally {
    adicionando.value = false
  }
}

async function remover(c) {
  try {
    await api.removerCompartilhamento(props.documentoId, c.usuarioId)
    await carregar()
    $q.notify({ type: 'positive', message: 'Coautor removido.' })
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao remover: ${e?.message ?? 'erro desconhecido'}` })
  }
}
</script>
