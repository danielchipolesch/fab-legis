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

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
            <!-- Espécie Normativa -->
=======
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
=======
            <!-- Espécie Normativa -->
>>>>>>> 1e3a004 (Correção de bugs)
            <v-col cols="12">
              <v-autocomplete
                v-model="form.especieNormativa"
                :items="especies"
<<<<<<< HEAD
<<<<<<< HEAD
                :loading="carregandoRefs"
=======
                :loading="carregando"
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
=======
                :loading="carregandoRefs"
>>>>>>> 1e3a004 (Correção de bugs)
                item-title="label"
                item-value="id"
                label="Espécie Normativa *"
                :rules="[obrigatorio]"
                prepend-inner-icon="mdi-tag-outline"
                return-object
                no-data-text="Nenhuma espécie encontrada"
<<<<<<< HEAD
                :disabled="carregando"
              />
            </v-col>

<<<<<<< HEAD
            <!-- Assunto Básico -->
=======
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
            <v-col cols="12">
              <v-autocomplete
                v-model="form.assuntoBasico"
                :items="assuntos"
<<<<<<< HEAD
                :loading="carregandoRefs"
=======
                :loading="carregando"
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
                item-title="label"
                item-value="id"
                label="Assunto Básico *"
                :rules="[obrigatorio]"
                prepend-inner-icon="mdi-book-outline"
                return-object
                no-data-text="Nenhum assunto encontrado"
                :disabled="carregando"
              />
            </v-col>

<<<<<<< HEAD
            <!-- Título do documento -->
=======
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
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
=======
            <!-- MODO MOCK ─────────────────────────────────────────────── -->
            <template v-if="useMock">
=======
              />
            </v-col>
>>>>>>> 1e3a004 (Correção de bugs)

            <!-- Assunto Básico -->
            <v-col cols="12">
              <v-autocomplete
                v-model="form.assuntoBasico"
                :items="assuntos"
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

<<<<<<< HEAD
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
>>>>>>> ffd8177 (Uso do HATEOAS)
=======
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
>>>>>>> 1e3a004 (Correção de bugs)

          </v-row>
        </v-form>

        <v-alert
<<<<<<< HEAD
          v-if="useMock && form.especie && proximoNumeroMock"
          type="info"
          variant="tonal"
          density="compact"
          class="mt-2"
          icon="mdi-identifier"
        >
<<<<<<< HEAD
<<<<<<< HEAD
          {{ form.especieNormativa?.sigla }} {{ form.assuntoBasico?.codigo }}-<em>N</em>
=======
          {{ form.especieNormativa.sigla }} {{ form.assuntoBasico.codigo }}-<em>N</em>
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
          (N gerado automaticamente pelo servidor)
=======
          O documento será identificado como
          <strong>{{ form.especie }} {{ proximoNumeroMock }}</strong>.
>>>>>>> ffd8177 (Uso do HATEOAS)
        </v-alert>

        <!-- Preview do identificador (real) -->
        <v-alert
          v-if="!useMock && form.especieNormativa && form.assuntoBasico"
=======
          v-if="form.especieNormativa && form.assuntoBasico"
>>>>>>> 1e3a004 (Correção de bugs)
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
import { ref, reactive, computed, watch } from 'vue'
import { useDocumentsStore } from '@/stores/documents.js'
<<<<<<< HEAD
<<<<<<< HEAD
import { useRouter } from 'vue-router'
<<<<<<< HEAD
import { listEspeciesNormativas, listAssuntosBasicos, normalizeEspecie, normalizeAssunto } from '@/api/referencias.js'
=======
=======
>>>>>>> 3662220 (Corrige SPA: elementos aparecem imediatamente, snackbar visível, skeleton loaders)
import { listar as listarEspecies } from '@/api/especiesNormativas.js'
import { listar as listarAssuntos } from '@/api/assuntosBasicos.js'
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
=======
import { useRouter } from 'vue-router'
import { listEspeciesNormativas, listAssuntosBasicos, normalizeEspecie, normalizeAssunto } from '@/api/referencias.js'
>>>>>>> ffd8177 (Uso do HATEOAS)

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
const router = useRouter()
const store  = useDocumentsStore()
=======
const router  = useRouter()
const store   = useDocumentsStore()
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
=======
const store = useDocumentsStore()
>>>>>>> 3662220 (Corrige SPA: elementos aparecem imediatamente, snackbar visível, skeleton loaders)

<<<<<<< HEAD
const formRef  = ref(null)
const valido   = ref(false)
const salvando = ref(false)
<<<<<<< HEAD

const carregandoRefs = ref(false)
const especies       = ref([])
const assuntos       = ref([])

async function carregarReferencias() {
  if (especies.value.length) return
  carregandoRefs.value = true
  try {
    const [esp, ass] = await Promise.all([
      listEspeciesNormativas(),
      listAssuntosBasicos(),
    ])
    especies.value = esp.map(normalizeEspecie)
    assuntos.value = ass.map(normalizeAssunto)
  } finally {
    carregandoRefs.value = false
  }
}
=======
=======
const formRef    = ref(null)
const valido     = ref(false)
const salvando   = ref(false)
>>>>>>> c23bdb3 (Corrige criação de documento: @NoArgsConstructor, tipo enum mapping e error handling)
const carregando = ref(false)
const erroRefs   = ref('')
const erroCriacao = ref('')

const especies = ref([])
const assuntos = ref([])
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
=======
const router  = useRouter()
const store   = useDocumentsStore()
const useMock = USE_MOCK
=======
const router = useRouter()
const store  = useDocumentsStore()
>>>>>>> 1e3a004 (Correção de bugs)

const formRef  = ref(null)
const valido   = ref(false)
const salvando = ref(false)

<<<<<<< HEAD
// ─── Referências (modo real) ──────────────────────────────────────────────────
>>>>>>> ffd8177 (Uso do HATEOAS)

=======
>>>>>>> 1e3a004 (Correção de bugs)
const carregandoRefs = ref(false)
const especies       = ref([])
const assuntos       = ref([])

<<<<<<< HEAD
=======
async function carregarReferencias() {
  if (especies.value.length) return
  carregandoRefs.value = true
  try {
    const [esp, ass] = await Promise.all([
      listEspeciesNormativas(),
      listAssuntosBasicos(),
    ])
    especies.value = esp.map(normalizeEspecie)
    assuntos.value = ass.map(normalizeAssunto)
  } finally {
    carregandoRefs.value = false
  }
}

<<<<<<< HEAD
<<<<<<< HEAD
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
=======
// ─── Form ──────────────────────────────────────────────────────────────────────

=======
>>>>>>> 1e3a004 (Correção de bugs)
const form = reactive({
  especieNormativa: null,
  assuntoBasico:    null,
  titulo:           '',
})

<<<<<<< HEAD
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

>>>>>>> ffd8177 (Uso do HATEOAS)
=======
>>>>>>> 1e3a004 (Correção de bugs)
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
