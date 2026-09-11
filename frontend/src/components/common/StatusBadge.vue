<template>
  <q-chip
    :color="config.bg"
    :text-color="config.fg"
    :size="size"
    square
    class="text-weight-bold status-badge"
  >
    <q-icon :name="config.icon" size="14px" class="q-mr-xs" />
    {{ config.label }}
  </q-chip>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true },
  variant: { type: String, default: 'tonal' },
  size: { type: String, default: 'sm' },
})

// Quasar não tem "tonal": usamos um fundo claro (bg) + texto colorido (fg)
// para reproduzir a aparência translúcida do Vuetify.
const STATUS_MAP = {
  RASCUNHO:  { label: 'Rascunho',  bg: 'grey-3',        fg: 'grey-9',    icon: 'mdi-pencil-outline' },
  MINUTA:    { label: 'Minuta',    bg: 'orange-2',      fg: 'orange-10', icon: 'mdi-file-edit-outline' },
  EM_REVISAO: { label: 'Em Revisão', bg: 'orange-2', fg: 'orange-10', icon: 'mdi-account-search-outline' },
  APROVADO:  { label: 'Aprovado',  bg: 'green-2',       fg: 'green-10',  icon: 'mdi-check-circle-outline' },
  EM_PUBLICACAO: { label: 'Em Publicação', bg: 'blue-2', fg: 'primary', icon: 'mdi-timer-sand' },
  PUBLICADO: { label: 'Publicado', bg: 'blue-2',        fg: 'primary',   icon: 'mdi-publish' },
  EM_ALTERACAO: { label: 'Em Alteração', bg: 'deep-orange-2', fg: 'deep-orange-10', icon: 'mdi-pencil-lock-outline' },
  ALTERADO: { label: 'Alterado', bg: 'teal-2', fg: 'teal-10', icon: 'mdi-check-circle-outline' },
  ANALISE_REVOGACAO: { label: 'Análise de Revogação', bg: 'brown-2', fg: 'brown-10', icon: 'mdi-file-search-outline' },
  EM_REVOGACAO: { label: 'Em Revogação', bg: 'brown-2', fg: 'brown-10', icon: 'mdi-timer-sand' },
  CANCELADO: { label: 'Cancelado', bg: 'red-2',         fg: 'red-10',    icon: 'mdi-cancel' },
  REVOGADO:  { label: 'Revogado',  bg: 'brown-2',       fg: 'brown-10',  icon: 'mdi-file-remove-outline' },
}

const config = computed(() => STATUS_MAP[props.status] ?? { label: props.status, bg: 'grey-3', fg: 'grey-9', icon: 'mdi-help' })
</script>

<script>
export default { name: 'StatusBadge' }
</script>
