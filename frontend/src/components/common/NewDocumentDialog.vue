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

            <!-- Assunto Básico -->
            <div class="col-12">
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
                label="Título do Documento *"
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
          v-if="form.especieNormativa && form.assuntoBasico"
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
import { useDocumentsStore } from '@/stores/documents.js'
import { useRouter } from 'vue-router'
import { listEspeciesNormativas, listAssuntosBasicos, normalizeEspecie, normalizeAssunto } from '@/api/referencias.js'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

const router = useRouter()
const store  = useDocumentsStore()

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
  titulo:           '',
})

const aberto = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

watch(aberto, async (v) => {
  if (v) {
    formRef.value?.resetValidation()
    Object.assign(form, { especieNormativa: null, assuntoBasico: null, titulo: '' })
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
      idAssuntoBasico:    form.assuntoBasico.id,
      tituloDocumento:    form.titulo,
    })
    if (doc?.id) {
      fechar()
      router.push({ name: 'documento-editar', params: { id: doc.id } })
    }
  } catch (e) {
    console.error('[NewDocumentDialog] Erro ao criar documento:', e)
  } finally {
    salvando.value = false
  }
}

function fechar() {
  aberto.value = false
  formRef.value?.resetValidation()
  Object.assign(form, { especieNormativa: null, assuntoBasico: null, titulo: '' })
}
</script>
