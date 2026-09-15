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
import { statusMeta } from '@/utils/statusDocumento.js'

const props = defineProps({
  status: { type: String, required: true },
  variant: { type: String, default: 'tonal' },
  size: { type: String, default: 'sm' },
})

// Quasar não tem "tonal": usamos um fundo claro (bg) + texto colorido (fg)
// para reproduzir a aparência translúcida do Vuetify.
const config = computed(() => statusMeta(props.status))
</script>

<script>
export default { name: 'StatusBadge' }
</script>
