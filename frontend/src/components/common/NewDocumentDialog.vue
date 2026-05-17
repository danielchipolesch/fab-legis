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

            <!-- MODO MOCK ─────────────────────────────────────────────── -->
            <template v-if="useMock">

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
                  :items="especiesMock"
                  label="Espécie Normativa *"
                  :rules="[obrigatorio]"
                  prepend-inner-icon="mdi-tag-outline"
                />
              </v-col>

              <!-- Numeração Básica (somente leitura) -->
              <v-col cols="12" sm="6">
                <v-text-field
                  :model-value="proximoNumeroMock"
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

              <!-- Assunto Básico (texto livre no mock) -->
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

            </template>

            <!-- MODO REAL ─────────────────────────────────────────────── -->
            <template v-else>

              <!-- Espécie Normativa (select do backend) -->
              <v-col cols="12">
                <v-autocomplete
                  v-model="form.especieNormativa"
                  :items="especiesReal"
                  :loading="carregandoRefs"
                  item-title="label"
                  item-value="id"
                  label="Espécie Normativa *"
                  :rules="[obrigatorio]"
                  prepend-inner-icon="mdi-tag-outline"
                  return-object
                  no-data-text="Nenhuma espécie encontrada"
                />
              </v-col>

              <!-- Assunto Básico (select do backend) -->
              <v-col cols="12">
                <v-autocomplete
                  v-model="form.assuntoBasico"
                  :items="assuntosReal"
                  :loading="carregandoRefs"
                  item-title="label"
                  item-value="id"
                  label="Assunto Básico *"
                  :rules="[obrigatorio]"
                  prepend-inner-icon="mdi-book-outline"
                  return-object
                  no-data-text="Nenhum assunto encontrado"
                />
              </v-col>

              <!-- Título do documento -->
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

            </template>

          </v-row>
        </v-form>

        <!-- Preview do identificador (mock) -->
        <v-alert
          v-if="useMock && form.especie && proximoNumeroMock"
          type="info"
          variant="tonal"
          density="compact"
          class="mt-2"
          icon="mdi-identifier"
        >
          O documento será identificado como
          <strong>{{ form.especie }} {{ proximoNumeroMock }}</strong>.
        </v-alert>

        <!-- Preview do identificador (real) -->
        <v-alert
          v-if="!useMock && form.especieNormativa && form.assuntoBasico"
          type="info"
          variant="tonal"
          density="compact"
          class="mt-2"
          icon="mdi-identifier"
        >
          {{ form.especieNormativa?.sigla }} {{ form.assuntoBasico?.codigo }}-<em>N</em>
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
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useDocumentsStore } from '@/stores/documents.js'
import { useRouter } from 'vue-router'
import { USE_MOCK } from '@/api/documents.js'
import { listEspeciesNormativas, listAssuntosBasicos, normalizeEspecie, normalizeAssunto } from '@/api/referencias.js'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

const router  = useRouter()
const store   = useDocumentsStore()
const useMock = USE_MOCK

const formRef  = ref(null)
const valido   = ref(false)
const salvando = ref(false)

// ─── Referências (modo real) ──────────────────────────────────────────────────

const carregandoRefs = ref(false)
const especiesReal   = ref([])
const assuntosReal   = ref([])

async function carregarReferencias() {
  if (useMock || especiesReal.value.length) return
  carregandoRefs.value = true
  try {
    const [especies, assuntos] = await Promise.all([
      listEspeciesNormativas(),
      listAssuntosBasicos(),
    ])
    especiesReal.value = especies.map(normalizeEspecie)
    assuntosReal.value = assuntos.map(normalizeAssunto)
  } finally {
    carregandoRefs.value = false
  }
}

// ─── Form ──────────────────────────────────────────────────────────────────────

const form = reactive({
  // Mock
  organizacao:   '',
  especie:       '',
  assunto_basico: '',
  // Real
  especieNormativa: null,
  assuntoBasico:    null,
  titulo:           '',
})

// ─── Modo mock ─────────────────────────────────────────────────────────────────

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

const especiesMock = ['ICA', 'NSCA', 'MCA', 'RCA', 'DCA', 'PCA', 'OCA', 'TCA']

const proximoNumeroMock = computed(() =>
  form.especie ? store.getNextBasicNumber(form.especie) : ''
)

// ─── Validações ────────────────────────────────────────────────────────────────

const obrigatorio = (v) => (v != null && String(v).trim() !== '') || 'Campo obrigatório'
const minLen      = (v) => (String(v ?? '').trim().length >= 5) || 'Mínimo de 5 caracteres'

// ─── Dialog ────────────────────────────────────────────────────────────────────

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

// ─── Ações ─────────────────────────────────────────────────────────────────────

async function confirmar() {
  const { valid } = await formRef.value.validate()
  if (!valid) return

  salvando.value = true
  try {
    let doc
    if (useMock) {
      doc = await store.createDocumento({
        organizacao:    form.organizacao,
        especie:        form.especie,
        assunto_basico: form.assunto_basico,
        numero_basico:  proximoNumeroMock.value,
      })
    } else {
      doc = await store.createDocumento({
        idEspecieNormativa: form.especieNormativa.id,
        idAssuntoBasico:    form.assuntoBasico.id,
        tituloDocumento:    form.titulo,
      })
    }

    fechar()
    if (doc?.id) router.push({ name: 'documento-editar', params: { id: doc.id } })
  } finally {
    salvando.value = false
  }
}

function fechar() {
  aberto.value = false
  formRef.value?.reset()
  Object.assign(form, {
    organizacao: '', especie: '', assunto_basico: '',
    especieNormativa: null, assuntoBasico: null, titulo: '',
  })
}
</script>
