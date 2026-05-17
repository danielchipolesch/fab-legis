<template>
  <v-card color="surface-card" rounded="lg">
    <v-card-title class="text-subtitle-2 font-weight-bold pa-4 pb-2">
      <v-icon icon="mdi-information-outline" size="16" class="mr-2" />
      Metadados do Documento
    </v-card-title>
    <v-divider />
    <v-card-text class="pa-4">
      <v-row dense>
        <v-col cols="12" md="4">
          <v-select
            v-model="local.especie"
            :items="especies"
            label="Espécie Normativa *"
            hide-details="auto"
            @update:model-value="emit('update', local)"
          />
        </v-col>
        <v-col cols="6" md="2">
          <v-text-field
            v-model="local.numero_basico"
            label="Numeração Básica *"
            hide-details="auto"
            @update:model-value="emit('update', local)"
          />
        </v-col>
        <v-col cols="6" md="2">
          <v-text-field
            v-model="local.numero_secundario"
            label="Numeração Secundária"
            hide-details="auto"
            @update:model-value="emit('update', local)"
          />
        </v-col>
<<<<<<< HEAD
<<<<<<< HEAD
        <v-col cols="12">
=======
        <v-col cols="12" md="4">
          <v-text-field
            v-model="local.data_criacao"
            label="Data"
            type="date"
            hide-details="auto"
            :readonly="isReadonly"
            @update:model-value="emit('update', local)"
          />
        </v-col>
        <v-col cols="12" md="8">
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
=======
        <v-col cols="12">
>>>>>>> 1e3a004 (Correção de bugs)
          <v-text-field
            v-model="local.assunto_basico"
            label="Assunto Básico *"
            hide-details="auto"
            @update:model-value="emit('update', local)"
          />
        </v-col>
        <v-col cols="12" md="4">
          <v-select
            v-model="local.status"
            :items="statusOptions"
            label="Status"
            hide-details="auto"
            :readonly="isStatusReadonly"
          >
            <template #selection="{ item }">
              <StatusBadge :status="item.value" size="small" />
            </template>
          </v-select>
        </v-col>
      </v-row>
    </v-card-text>
  </v-card>
</template>

<script setup>
import { reactive, computed, watch } from 'vue'
import StatusBadge from '@/components/common/StatusBadge.vue'

const props = defineProps({
  documento: { type: Object, required: true },
})

const emit = defineEmits(['update'])

const especies = ['ICA', 'NSCA', 'Portaria', 'Resolução', 'Decreto', 'Aviso', 'Mensagem']
const statusOptions = ['RASCUNHO', 'MINUTA', 'APROVADO', 'PUBLICADO', 'ARQUIVADO', 'CANCELADO', 'REVOGADO']

const local = reactive({ ...props.documento })

watch(() => props.documento, (val) => Object.assign(local, val), { deep: true })
<<<<<<< HEAD
<<<<<<< HEAD

const isStatusReadonly = computed(() => ['PUBLICADO', 'ARQUIVADO', 'CANCELADO', 'REVOGADO'].includes(local.status))
=======
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
=======

const isStatusReadonly = computed(() => ['PUBLICADO', 'ARQUIVADO', 'CANCELADO', 'REVOGADO'].includes(local.status))
>>>>>>> 1e3a004 (Correção de bugs)
</script>
