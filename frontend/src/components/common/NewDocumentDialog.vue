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

            <v-col cols="12">
              <v-autocomplete
                v-model="form.especieNormativa"
                :items="especies"
                :loading="carregando"
                item-title="label"
                item-value="id"
                label="Espécie Normativa *"
                :rules="[obrigatorio]"
                prepend-inner-icon="mdi-tag-outline"
                return-object
                no-data-text="Nenhuma espécie encontrada"
              />
            </v-col>

            <v-col cols="12">
              <v-autocomplete
                v-model="form.assuntoBasico"
                :items="assuntos"
                :loading="carregando"
                item-title="label"
                item-value="id"
                label="Assunto Básico *"
                :rules="[obrigatorio]"
                prepend-inner-icon="mdi-book-outline"
                return-object
                no-data-text="Nenhum assunto encontrado"
              />
            </v-col>

            <v-col cols="12">
              <v-text-field
                v-model="form.titulo"
                label="Título do Documento *"
                :rules="[obrigatorio, minLen]"
                prepend-inner-icon="mdi-format-title"
                counter="500"
                maxlength="500"
              />
            </v-col>

          </v-row>
        </v-form>

        <v-alert
          v-if="form.especieNormativa && form.assuntoBasico"
          type="info"
          variant="tonal"
          density="compact"
          class="mt-2"
          icon="mdi-identifier"
        >
          {{ form.especieNormativa.sigla }} {{ form.assuntoBasico.codigo }}-<em>N</em>
          (N gerado automaticamente pelo servidor)
        </v-alert>
      </v-card-text>

      <v-divider />

      <v-card-actions class="pa-4 gap-2">
        <v-spacer />
        <v-btn variant="text" @click="fechar">Cancelar</v-btn>
        <v-btn
          color="primary"
          prepend-icon="mdi-check"
          :disabled="!valido || salvando"
          :loading="salvando"
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
import { listar as listarEspecies } from '@/api/especiesNormativas.js'
import { listar as listarAssuntos } from '@/api/assuntosBasicos.js'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

const router  = useRouter()
const store   = useDocumentsStore()

const formRef  = ref(null)
const valido   = ref(false)
const salvando = ref(false)
const carregando = ref(false)
const especies = ref([])
const assuntos = ref([])

const form = reactive({
  especieNormativa: null,
  assuntoBasico: null,
  titulo: '',
})

async function carregarReferencias() {
  if (especies.value.length) return
  carregando.value = true
  try {
    const [esp, ass] = await Promise.all([listarEspecies(), listarAssuntos()])
    especies.value = esp
    assuntos.value = ass
  } finally {
    carregando.value = false
  }
}

const aberto = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

watch(aberto, async (v) => {
  if (v) {
    formRef.value?.reset()
    await carregarReferencias()
  }
})

const obrigatorio = (v) => (v != null && String(v).trim() !== '') || 'Campo obrigatório'
const minLen      = (v) => (String(v ?? '').trim().length >= 5) || 'Mínimo de 5 caracteres'

async function confirmar() {
  const { valid } = await formRef.value.validate()
  if (!valid) return

  salvando.value = true
  try {
    const doc = await store.createDocumento({
      idEspecieNormativa: form.especieNormativa.id,
      idAssuntoBasico:    form.assuntoBasico.id,
      tituloDocumento:    form.titulo,
    })
    fechar()
    if (doc?.id) router.push({ name: 'documento-editar', params: { id: doc.id } })
  } finally {
    salvando.value = false
  }
}

function fechar() {
  aberto.value = false
  formRef.value?.reset()
  Object.assign(form, { especieNormativa: null, assuntoBasico: null, titulo: '' })
}
</script>
