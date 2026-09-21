<template>
  <!-- As etapas do ciclo do documento (utils/fluxoDocumento.js, etapasDoCiclo) num q-stepper só de leitura: as cumpridas
       com o ícone de feito, a atual ativa e as que faltam apagadas. Os passos não têm conteúdo nem são clicáveis. -->
  <q-stepper
    :model-value="ciclo.atual + 1"
    flat
    alternative-labels
    active-color="primary"
    done-color="primary"
    inactive-color="grey-6"
    active-icon="mdi-record-circle-outline"
    class="etapas"
    data-testid="etapas-do-ciclo"
  >
    <q-step
      v-for="(etapa, i) in ciclo.etapas"
      :key="etapa.chave"
      :name="i + 1"
      :title="etapa.rotulo"
      :done="i < ciclo.atual"
      :data-estado="estado(i)"
    />
  </q-stepper>
</template>

<script setup>
import { computed } from 'vue'
import { etapasDoCiclo } from '@/utils/fluxoDocumento.js'

const props = defineProps({ doc: { type: Object, required: true } })

const ciclo = computed(() => etapasDoCiclo(props.doc))

function estado(i) {
  if (i < ciclo.value.atual) return 'cumprida'
  return i === ciclo.value.atual ? 'atual' : 'futura'
}
</script>

<style scoped>
/* O q-stepper sem painéis: tira o espaço que o conteúdo (vazio) ocuparia e mantém só o cabeçalho. */
.etapas :deep(.q-stepper__step-inner) { padding: 0; }
.etapas :deep(.q-stepper__header) { border-bottom: 0; }
</style>
