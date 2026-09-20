<template>
  <span class="status-badge-group">
    <q-chip
      v-if="situacaoBca"
      :color="bca.bg"
      :text-color="bca.fg"
      :size="size"
      square
      class="text-weight-bold status-badge"
      data-testid="chip-situacao-bca"
    >
      <q-icon :name="bca.icon" size="14px" class="q-mr-xs" />
      {{ bca.label }}
    </q-chip>
    <q-chip
      v-if="mostrarLocal"
      :color="local.bg"
      :text-color="local.fg"
      :size="size"
      square
      outline
      class="text-weight-medium status-badge"
      data-testid="chip-situacao-local"
    >
      <q-icon :name="local.icon" size="14px" class="q-mr-xs" />
      {{ local.label }}
    </q-chip>
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { situacaoBcaMeta, situacaoLocalMeta, temEtapaEmCurso } from '@/utils/statusDocumento.js'

// Situação BCA = chip principal (a real); Situação Local = chip secundário (etapa interna), só
// aparece quando há etapa em curso.
const props = defineProps({
  situacaoBca: { type: String, default: null },
  situacaoLocal: { type: String, default: null },
  size: { type: String, default: 'sm' },
})

const bca = computed(() => situacaoBcaMeta(props.situacaoBca))
const local = computed(() => situacaoLocalMeta(props.situacaoLocal))
const mostrarLocal = computed(() => temEtapaEmCurso(props.situacaoLocal))
</script>

<script>
export default { name: 'StatusBadge' }
</script>

<style scoped>
.status-badge-group { display: inline-flex; flex-wrap: wrap; align-items: center; gap: 2px; }
.status-badge-group .q-chip { margin: 0; }
</style>
