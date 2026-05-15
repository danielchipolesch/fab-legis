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

        <!-- Erro de carregamento de referências -->
        <v-alert
          v-if="erroRefs"
          type="error"
          variant="tonal"
          density="compact"
          closable
          class="mb-4"
          @click:close="erroRefs = ''"
        >
          {{ erroRefs }}
        </v-alert>

        <v-form ref="formRef" v-model="valido" @submit.prevent="confirmar">
          <v-row dense>

<<<<<<< HEAD
            <!-- Espécie Normativa -->
=======
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
            <v-col cols="12">
              <v-autocomplete
                v-model="form.especieNormativa"
                :items="especies"
<<<<<<< HEAD
                :loading="carregandoRefs"
=======
                :loading="carregando"
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
                item-title="label"
                item-value="id"
                label="Espécie Normativa *"
                :rules="[obrigatorio]"
                prepend-inner-icon="mdi-tag-outline"
                return-object
                no-data-text="Nenhuma espécie encontrada"
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

          </v-row>
        </v-form>

        <v-alert
          v-if="form.especieNormativa && form.assuntoBasico"
          type="info"
          variant="tonal"
          density="compact"
          class="mt-3"
          icon="mdi-identifier"
        >
<<<<<<< HEAD
          {{ form.especieNormativa?.sigla }} {{ form.assuntoBasico?.codigo }}-<em>N</em>
=======
          {{ form.especieNormativa.sigla }} {{ form.assuntoBasico.codigo }}-<em>N</em>
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
          (N gerado automaticamente pelo servidor)
        </v-alert>

        <!-- Erro de criação -->
        <v-alert
          v-if="erroCriacao"
          type="error"
          variant="tonal"
          density="compact"
          closable
          class="mt-3"
          @click:close="erroCriacao = ''"
        >
          {{ erroCriacao }}
        </v-alert>

      </v-card-text>

      <v-divider />

      <v-card-actions class="pa-4 gap-2">
        <v-spacer />
        <v-btn variant="text" :disabled="salvando" @click="fechar">Cancelar</v-btn>
        <v-btn
          color="primary"
          prepend-icon="mdi-check"
          :disabled="!valido || salvando || carregando"
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
<<<<<<< HEAD
import { listEspeciesNormativas, listAssuntosBasicos, normalizeEspecie, normalizeAssunto } from '@/api/referencias.js'
=======
import { listar as listarEspecies } from '@/api/especiesNormativas.js'
import { listar as listarAssuntos } from '@/api/assuntosBasicos.js'
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

<<<<<<< HEAD
const router = useRouter()
const store  = useDocumentsStore()
=======
const router  = useRouter()
const store   = useDocumentsStore()
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)

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

const form = reactive({
  especieNormativa: null,
  assuntoBasico: null,
  titulo: '',
})

<<<<<<< HEAD
=======
async function carregarReferencias() {
  if (especies.value.length) return
  carregando.value = true
  erroRefs.value = ''
  try {
    const [esp, ass] = await Promise.all([listarEspecies(), listarAssuntos()])
    especies.value = esp
    assuntos.value = ass
  } catch (e) {
    erroRefs.value = `Não foi possível carregar as referências: ${e?.message ?? 'erro desconhecido'}. Verifique se o backend está rodando.`
  } finally {
    carregando.value = false
  }
}

>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
const aberto = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

watch(aberto, async (v) => {
  if (v) {
    erroCriacao.value = ''
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
  erroCriacao.value = ''
  try {
    const doc = await store.createDocumento({
      idEspecieNormativa: form.especieNormativa.id,
      idAssuntoBasico:    form.assuntoBasico.id,
      tituloDocumento:    form.titulo,
    })
    fechar()
    if (doc?.id) router.push({ name: 'documento-editar', params: { id: doc.id } })
  } catch (e) {
    erroCriacao.value = `Erro ao criar o documento: ${e?.message ?? 'erro desconhecido'}`
  } finally {
    salvando.value = false
  }
}

function fechar() {
  if (salvando.value) return
  aberto.value = false
  formRef.value?.reset()
  Object.assign(form, { especieNormativa: null, assuntoBasico: null, titulo: '' })
  erroCriacao.value = ''
}
</script>
