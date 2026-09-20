<template>
  <span class="status-badge-group">
    <q-chip
      v-if="exibirBca"
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
    <!-- Situação local: contorno e texto na cor forte da família (a cor clara do fundo tonal, usada
         só como borda, quase desaparecia sobre o fundo branco). -->
    <q-chip
      v-if="exibirLocal"
      :color="local.color"
      :text-color="local.fg"
      :size="size"
      square
      outline
      class="text-weight-bold status-badge status-badge-local"
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

// Situação BCA = chip principal (a real); Situação Local = chip secundário (etapa interna).
// `mostrar` escolhe qual(is) exibir: 'ambos' (padrão; o local só aparece com etapa em curso),
// 'bca' ou 'local' -- este último para telas que separam as duas em colunas/campos próprios, e
// que mostram "Sem etapa em curso" quando não há nenhuma.
const props = defineProps({
  situacaoBca: { type: String, default: null },
  situacaoLocal: { type: String, default: null },
  mostrar: { type: String, default: 'ambos', validator: v => ['ambos', 'bca', 'local'].includes(v) },
  size: { type: String, default: 'sm' },
})

const bca = computed(() => situacaoBcaMeta(props.situacaoBca))
const local = computed(() => situacaoLocalMeta(props.situacaoLocal))
const exibirBca = computed(() => !!props.situacaoBca && props.mostrar !== 'local')
const exibirLocal = computed(() => {
  if (props.mostrar === 'bca') return false
  if (props.mostrar === 'local') return !!props.situacaoLocal
  return temEtapaEmCurso(props.situacaoLocal)
})
</script>

<script>
export default { name: 'StatusBadge' }
</script>

<style scoped>
.status-badge-group { display: inline-flex; flex-wrap: wrap; align-items: center; gap: 2px; }
.status-badge-group .q-chip { margin: 0; }
/* Contorno mais grosso: o outline padrão do Quasar (1px) some em chips pequenos. */
.status-badge-local.q-chip--outline:before { border-width: 2px; }
</style>
