<template>
  <v-dialog v-model="aberto" max-width="560" persistent>
    <v-card>

      <v-card-title class="d-flex align-center pa-5 pb-3">
        <v-icon icon="mdi-file-plus-outline" color="primary" class="mr-3" />
        <span class="text-h6 font-weight-bold">Novo Documento</span>
        <v-spacer />
        <v-btn icon="mdi-close" size="small" variant="text" @click="fechar" />
      </v-card-title>

      <v-divider />

      <v-card-text class="pa-5">
        <v-form ref="formRef" v-model="valido" @submit.prevent="confirmar">
          <v-row dense>

            <!-- Organização Militar -->
            <v-col cols="12">
              <v-select
                v-model="form.organizacao"
                :items="organizacoes"
                label="Organização Militar *"
                :rules="[obrigatorio]"
                prepend-inner-icon="mdi-domain"
              />
            </v-col>

            <!-- Espécie Normativa -->
            <v-col cols="12" sm="6">
              <v-select
                v-model="form.especie"
                :items="especies"
                label="Espécie Normativa *"
                :rules="[obrigatorio]"
                prepend-inner-icon="mdi-tag-outline"
                @update:model-value="atualizarNumero"
              />
            </v-col>

            <!-- Numeração Básica (sequencial, somente leitura) -->
            <v-col cols="12" sm="6">
              <v-text-field
                :model-value="proximoNumero"
                label="Numeração Básica"
                readonly
                prepend-inner-icon="mdi-numeric"
                hint="Gerada automaticamente pelo sistema"
                persistent-hint
              >
                <template #append-inner>
                  <v-icon icon="mdi-lock-outline" size="16" color="grey" />
                </template>
              </v-text-field>
            </v-col>

            <!-- Assunto Básico -->
            <v-col cols="12">
              <v-text-field
                v-model="form.assunto_basico"
                label="Assunto Básico *"
                :rules="[obrigatorio, minLen]"
                prepend-inner-icon="mdi-text-short"
                counter="200"
                maxlength="200"
              />
            </v-col>

          </v-row>
        </v-form>

        <!-- Preview do identificador -->
        <v-alert
          v-if="form.especie && proximoNumero"
          type="info"
          variant="tonal"
          density="compact"
          class="mt-2"
          icon="mdi-identifier"
        >
          O documento será identificado como
          <strong>{{ form.especie }} {{ proximoNumero }}</strong>.
        </v-alert>
      </v-card-text>

      <v-divider />

      <v-card-actions class="pa-4 gap-2">
        <v-spacer />
        <v-btn variant="text" @click="fechar">Cancelar</v-btn>
        <v-btn
          color="primary"
          prepend-icon="mdi-check"
          :disabled="!valido"
          @click="confirmar"
        >
          Criar Documento
        </v-btn>
      </v-card-actions>

    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useDocumentsStore } from '@/stores/documents.js'
import { useRouter } from 'vue-router'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

const router = useRouter()
const store  = useDocumentsStore()

const formRef = ref(null)
const valido  = ref(false)

const aberto = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const form = reactive({ organizacao: '', especie: '', assunto_basico: '' })

const organizacoes = [
  'COMAER — Comando da Aeronáutica',
  'EMAER — Estado-Maior da Aeronáutica',
  'DEPENS — Departamento de Ensino',
  'DECEA — Departamento de Controle do Espaço Aéreo',
  'DCTA — Departamento de Ciência e Tecnologia Aeroespacial',
  'DIRENS — Diretoria de Ensino',
  'DIRINT — Diretoria de Inteligência',
  'DIRSA — Diretoria de Saúde',
  'COMARA — Comando Aéreo Regional Amazônico',
  'COMAER/SJC — Subdepartamento de Gestão',
]

const especies = ['ICA', 'NSCA', 'Portaria', 'Resolução', 'Decreto', 'Aviso', 'Mensagem']

const proximoNumero = computed(() =>
  form.especie ? store.getNextBasicNumber(form.especie) : ''
)

const obrigatorio = (v) => !!v?.trim() || 'Campo obrigatório'
const minLen      = (v) => (v?.trim().length ?? 0) >= 5 || 'Mínimo de 5 caracteres'

function atualizarNumero() {
  // Apenas força recompute do proximoNumero via getter reativo
}

async function confirmar() {
  const { valid } = await formRef.value.validate()
  if (!valid) return

  const doc = store.createDocumento({
    organizacao:    form.organizacao,
    especie:        form.especie,
    assunto_basico: form.assunto_basico,
    numero_basico:  proximoNumero.value,
  })

  fechar()
  router.push({ name: 'documento-editar', params: { id: doc.id } })
}

function fechar() {
  aberto.value = false
  formRef.value?.reset()
  Object.assign(form, { organizacao: '', especie: '', assunto_basico: '' })
}

// Limpa o form ao abrir
watch(aberto, (v) => { if (v) formRef.value?.reset() })
</script>
