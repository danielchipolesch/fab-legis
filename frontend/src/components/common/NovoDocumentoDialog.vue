<template>
  <q-dialog v-model="aberto" persistent>
    <q-card style="min-width:560px;max-width:560px">

      <q-card-section class="row items-center q-pa-lg q-pb-sm">
        <q-icon name="mdi-file-plus-outline" color="primary" size="24px" class="q-mr-sm" />
        <span class="text-h6 text-weight-bold">Novo Documento</span>
        <q-space />
        <q-btn icon="mdi-close" size="sm" flat round dense @click="fechar" />
      </q-card-section>

      <q-separator />

      <q-card-section class="q-pa-lg">
        <q-form ref="formRef" @submit.prevent="confirmar">
          <div class="row q-col-gutter-sm">

            <!-- Espécie Normativa -->
            <div class="col-12">
              <q-select
                v-model="form.especieNormativa"
                :options="especiesFiltradas"
                :loading="carregandoRefs"
                option-label="label"
                option-value="id"
                label="Espécie Normativa *"
                :rules="[obrigatorio]"
                outlined
                use-input
                fill-input
                hide-selected
                input-debounce="0"
                @filter="filtrarEspecies"
              >
                <template #prepend>
                  <q-icon name="mdi-tag-outline" />
                </template>
                <template #no-option>
                  <q-item>
                    <q-item-section class="text-grey">
                      Nenhuma espécie encontrada
                    </q-item-section>
                  </q-item>
                </template>
              </q-select>
            </div>

            <!-- Identificação (NPA): texto livre, conforme o padrão do setor que emite -->
            <div v-if="ehNpa" class="col-12">
              <q-input
                v-model="form.identificacao"
                label="Identificação *"
                hint="Texto livre, conforme o padrão do setor. Ex.: NPA-AGO-01 ou NPA 44-__/2026"
                :rules="[obrigatorio]"
                outlined
                maxlength="120"
              >
                <template #prepend>
                  <q-icon name="mdi-identifier" />
                </template>
              </q-input>
            </div>

            <!-- Assunto Básico (atos normativos; a NPA não usa) -->
            <div v-else class="col-12">
              <q-select
                v-model="form.assuntoBasico"
                :options="assuntosFiltrados"
                :loading="carregandoRefs"
                option-label="label"
                option-value="id"
                label="Assunto Básico *"
                :rules="[obrigatorio]"
                outlined
                use-input
                fill-input
                hide-selected
                input-debounce="0"
                @filter="filtrarAssuntos"
              >
                <template #prepend>
                  <q-icon name="mdi-book-outline" />
                </template>
                <template #no-option>
                  <q-item>
                    <q-item-section class="text-grey">
                      Nenhum assunto encontrado
                    </q-item-section>
                  </q-item>
                </template>
              </q-select>
            </div>

            <!-- Título do documento -->
            <div class="col-12">
              <q-input
                v-model="form.titulo"
                :label="ehNpa ? 'Assunto *' : 'Título do Documento *'"
                :rules="[obrigatorio, minLen]"
                outlined
                counter
                maxlength="500"
              >
                <template #prepend>
                  <q-icon name="mdi-format-title" />
                </template>
              </q-input>
            </div>

          </div>
        </q-form>

        <q-banner
          v-if="!ehNpa && form.especieNormativa && form.assuntoBasico"
          dense
          rounded
          class="bg-info text-white q-mt-sm"
        >
          <template #avatar>
            <q-icon name="mdi-identifier" color="white" />
          </template>
          {{ form.especieNormativa?.sigla }} {{ form.assuntoBasico?.codigo }}-<em>N</em>
          (N gerado automaticamente pelo servidor)
        </q-banner>
      </q-card-section>

      <q-separator />

      <q-card-actions align="right" class="q-pa-md">
        <q-btn flat @click="fechar">Cancelar</q-btn>
        <q-btn
          color="primary"
          unelevated
          :loading="salvando"
          @click="confirmar"
        >
          <q-icon left name="mdi-check" />
          Criar Documento
        </q-btn>
      </q-card-actions>

    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useDocumentosStore } from '@/stores/documentos.js'
import { useRouter } from 'vue-router'
import { perfilDe } from '@/perfis/index.js'
import { listEspeciesNormativas, listAssuntosBasicos, normalizeEspecie, normalizeAssunto } from '@/api/referencias.js'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

const router = useRouter()
const store  = useDocumentosStore()

const formRef  = ref(null)
const salvando = ref(false)

const carregandoRefs = ref(false)
const especies       = ref([])
const assuntos       = ref([])

const especiesFiltradas = ref([])
const assuntosFiltrados = ref([])

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
    especiesFiltradas.value = especies.value
    assuntosFiltrados.value = assuntos.value
  } finally {
    carregandoRefs.value = false
  }
}

function filtrarEspecies(val, update) {
  update(() => {
    if (!val) {
      especiesFiltradas.value = especies.value
    } else {
      const q = val.toLowerCase()
      especiesFiltradas.value = especies.value.filter(e =>
        e.label.toLowerCase().includes(q)
      )
    }
  })
}

function filtrarAssuntos(val, update) {
  update(() => {
    if (!val) {
      assuntosFiltrados.value = assuntos.value
    } else {
      const q = val.toLowerCase()
      assuntosFiltrados.value = assuntos.value.filter(a =>
        a.label.toLowerCase().includes(q)
      )
    }
  })
}

const form = reactive({
  especieNormativa: null,
  assuntoBasico:    null,
  identificacao:    '',
  titulo:           '',
})

// A espécie escolhida decide o que o documento pede na criação (perfis/index.js): um ato normativo, o assunto
// básico; uma NPA, a identificação em texto livre.
const ehNpa = computed(() => perfilDe(form.especieNormativa?.tipoDeEspecie).ehNpa)

const aberto = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

watch(aberto, async (v) => {
  if (v) {
    formRef.value?.resetValidation()
    Object.assign(form, { especieNormativa: null, assuntoBasico: null, identificacao: '', titulo: '' })
    await carregarReferencias()
  }
})

const obrigatorio = (v) => (v != null && String(typeof v === 'object' ? (v.label ?? '') : v).trim() !== '') || 'Campo obrigatório'
const minLen      = (v) => (String(v ?? '').trim().length >= 5) || 'Mínimo de 5 caracteres'

async function confirmar() {
  const valid = await formRef.value.validate()
  if (!valid) return

  salvando.value = true
  try {
    const doc = await store.createDocumento({
      idEspecieNormativa: form.especieNormativa.id,
      idAssuntoBasico:    ehNpa.value ? null : form.assuntoBasico.id,
      identificacao:      ehNpa.value ? form.identificacao : null,
      tituloDocumento:    form.titulo,
    })
    if (doc?.id) {
      fechar()
      router.push({ name: 'documento-editar', params: { id: doc.id } })
    }
  } catch (e) {
    console.error('[NovoDocumentoDialog] Erro ao criar documento:', e)
  } finally {
    salvando.value = false
  }
}

function fechar() {
  aberto.value = false
  formRef.value?.resetValidation()
  Object.assign(form, { especieNormativa: null, assuntoBasico: null, identificacao: '', titulo: '' })
}
</script>
