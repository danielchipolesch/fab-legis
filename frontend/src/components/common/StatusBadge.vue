<template>
  <v-chip
    :color="config.color"
    :variant="variant"
    :size="size"
    label
    class="font-weight-bold"
  >
    <v-icon start :icon="config.icon" size="14" />
    {{ config.label }}
  </v-chip>
</template>

<script setup>
const props = defineProps({
  status: { type: String, required: true },
  variant: { type: String, default: 'tonal' },
  size: { type: String, default: 'small' },
})

const STATUS_MAP = {
  RASCUNHO:  { label: 'Rascunho',  color: 'grey',        icon: 'mdi-pencil-outline' },
  MINUTA:    { label: 'Minuta',    color: 'orange-darken-2', icon: 'mdi-file-edit-outline' },
  APROVADO:  { label: 'Aprovado',  color: 'success',     icon: 'mdi-check-circle-outline' },
  PUBLICADO: { label: 'Publicado', color: 'primary',     icon: 'mdi-publish' },
  ARQUIVADO: { label: 'Arquivado', color: 'blue-grey',   icon: 'mdi-archive-outline' },
  CANCELADO: { label: 'Cancelado', color: 'error',       icon: 'mdi-cancel' },
  REVOGADO:  { label: 'Revogado',  color: 'brown',       icon: 'mdi-file-remove-outline' },
}

const config = computed(() => STATUS_MAP[props.status] ?? { label: props.status, color: 'grey', icon: 'mdi-help' })
</script>

<script>
import { computed } from 'vue'
export default { name: 'StatusBadge' }
</script>
