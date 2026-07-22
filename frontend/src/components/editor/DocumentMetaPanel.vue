<template>
  <q-card flat bordered>
    <q-card-section class="text-subtitle2 text-weight-bold q-pa-md q-pb-sm row items-center">
      <q-icon name="mdi-information-outline" size="16px" class="q-mr-sm" />
      Metadados do Documento
    </q-card-section>
    <q-separator />
    <q-card-section class="q-pa-md">
      <div class="row q-col-gutter-sm">
        <div class="col-12 col-md-4">
          <q-select
            v-model="local.especie"
            :options="especies"
            label="Espécie Normativa *"
            outlined
            dense
            @update:model-value="emit('update', local)"
          />
        </div>
        <div class="col-6 col-md-2">
          <q-input
            v-model="local.numero_basico"
            label="Numeração Básica *"
            outlined
            dense
            @update:model-value="emit('update', local)"
          />
        </div>
        <div class="col-6 col-md-2">
          <q-input
            v-model="local.numero_secundario"
            label="Numeração Secundária"
            outlined
            dense
            @update:model-value="emit('update', local)"
          />
        </div>
        <div class="col-12">
          <q-input
            v-model="local.assunto_basico"
            label="Assunto Básico *"
            outlined
            dense
            @update:model-value="emit('update', local)"
          />
        </div>
        <div class="col-12 col-md-4">
          <q-select
            v-model="local.status"
            :options="statusOptions"
            label="Status"
            outlined
            dense
            :readonly="isStatusReadonly"
          >
            <template #selected>
              <StatusBadge v-if="local.status" :status="local.status" size="sm" />
            </template>
          </q-select>
        </div>
      </div>
    </q-card-section>
  </q-card>
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

const isStatusReadonly = computed(() => ['PUBLICADO', 'ARQUIVADO', 'CANCELADO', 'REVOGADO'].includes(local.status))
</script>
