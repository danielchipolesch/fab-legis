<template>
  <q-dialog :model-value="modelValue" @update:model-value="$emit('update:model-value', $event)" persistent>
    <q-card style="min-width:560px;max-width:700px;width:100%">
      <q-card-section class="row items-center q-pb-none">
        <q-icon :name="acaoIcon" size="20px" :color="acaoColor" class="q-mr-sm" />
        <div class="text-h6">{{ acaoTitulo }}</div>
        <q-space />
        <q-btn icon="mdi-close" flat round dense v-close-popup :disable="salvando" />
      </q-card-section>

      <q-separator class="q-mt-sm" />

      <q-card-section class="q-pa-md scroll" style="max-height:70vh">
        <div class="text-caption text-grey-7 q-mb-md">
          Elemento: <strong>{{ elementoLabel }}</strong>
        </div>

        <!-- ALTERAR: editor para novo conteúdo + justificativa -->
        <template v-if="acao === 'ALTERAR'">
          <div class="text-caption text-weight-bold q-mb-xs">Novo conteúdo *</div>
          <div style="border:1px solid rgba(0,0,0,0.2);border-radius:4px;overflow:hidden" class="q-mb-md">
            <WysiwygEditor
              :key="elemento?.id + '-alterar'"
              v-model="novoConteudo"
            />
          </div>
          <div class="text-caption text-weight-bold q-mb-xs">Justificativa *</div>
          <q-input
            v-model="justificativa"
            outlined dense
            type="textarea"
            :rows="3"
            placeholder="Informe o motivo da alteração..."
            :disable="salvando"
          />
        </template>

        <!-- REVOGAR: conteúdo atual (referência) + justificativa -->
        <template v-else-if="acao === 'REVOGAR'">
          <div class="text-caption text-weight-bold q-mb-xs">Conteúdo atual (será revogado):</div>
          <div class="q-pa-sm q-mb-md" style="background:rgba(204,0,0,0.05);border:1px solid rgba(204,0,0,0.2);border-radius:4px">
            <WysiwygEditor
              :key="elemento?.id + '-revogar-ro'"
              :model-value="elemento?.conteudo"
              :readonly="true"
            />
          </div>
          <div class="text-caption text-weight-bold q-mb-xs">Justificativa *</div>
          <q-input
            v-model="justificativa"
            outlined dense
            type="textarea"
            :rows="3"
            placeholder="Informe o motivo da revogação..."
            :disable="salvando"
          />
        </template>

        <!-- DESFAZER: confirmação -->
        <template v-else-if="acao === 'DESFAZER'">
          <p class="text-body2 q-mb-md">
            Deseja desfazer a emenda neste elemento?
            <template v-if="elemento?.emendaStatus === 'INCLUIDO'">
              Por ser um elemento <strong>incluído por emenda</strong>, ele será <strong>removido</strong> permanentemente.
            </template>
            <template v-else>
              O conteúdo da emenda será removido e o status voltará a <strong>INALTERADO</strong>.
              O texto original publicado permanece intacto.
            </template>
          </p>
          <template v-if="elemento?.conteudoEmenda && elemento?.emendaStatus === 'ALTERADO'">
            <div class="text-caption text-weight-bold q-mb-xs">Emenda que será desfeita:</div>
            <div class="q-pa-sm q-mb-sm" style="background:rgba(0,0,0,0.04);border:1px solid rgba(0,0,0,0.12);border-radius:4px">
              <WysiwygEditor
                :key="elemento?.id + '-desfazer-ro'"
                :model-value="elemento?.conteudoEmenda"
                :readonly="true"
              />
            </div>
          </template>
        </template>
      </q-card-section>

      <q-separator />

      <q-card-actions align="right" class="q-px-md q-py-sm">
        <q-btn flat label="Cancelar" v-close-popup :disable="salvando" />
        <q-btn
          unelevated
          :color="acaoColor"
          :label="acaoTitulo"
          :loading="salvando"
          :disable="!podeConfirmar"
          @click="confirmar"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { useDocumentsStore } from '@/stores/documents.js'
import { useEditorStore } from '@/stores/editor.js'
import { formatLabel } from '@/utils/numbering.js'
import WysiwygEditor from '@/components/editor/WysiwygEditor.vue'

const props = defineProps({
  modelValue:  { type: Boolean, required: true },
  acao:        { type: String, required: true },   // 'ALTERAR' | 'REVOGAR' | 'DESFAZER'
  elemento:    { type: Object, default: null },
  secao:       { type: String, required: true },   // 'PARTE_PRELIMINAR' | 'PARTE_NORMATIVA'
  documentoId: { type: [String, Number], required: true },
})

const emit = defineEmits(['update:model-value', 'confirmado'])

const $q = useQuasar()
const docStore  = useDocumentsStore()
const editorStore = useEditorStore()

const salvando     = ref(false)
const justificativa = ref('')
const novoConteudo  = ref('')

watch(() => [props.modelValue, props.elemento, props.acao], ([open]) => {
  if (open) {
    justificativa.value = ''
    // Se o elemento já foi alterado, o editor começa a partir do conteúdo da emenda anterior.
    // Caso contrário, começa a partir do conteúdo original publicado.
    novoConteudo.value = props.elemento?.conteudoEmenda ?? props.elemento?.conteudo ?? ''
  }
}, { immediate: false })

const acaoTitulo = computed(() => ({
  ALTERAR:  'Alterar texto',
  REVOGAR:  'Revogar elemento',
  DESFAZER: 'Desfazer emenda',
}[props.acao] ?? props.acao))

const acaoIcon = computed(() => ({
  ALTERAR:  'mdi-pencil-outline',
  REVOGAR:  'mdi-delete-outline',
  DESFAZER: 'mdi-undo-variant',
}[props.acao] ?? 'mdi-help'))

const acaoColor = computed(() => ({
  ALTERAR:  'primary',
  REVOGAR:  'negative',
  DESFAZER: 'warning',
}[props.acao] ?? 'grey'))

const elementoLabel = computed(() => props.elemento ? formatLabel(props.elemento) : '—')

const podeConfirmar = computed(() => {
  if (props.acao === 'DESFAZER') return true
  return justificativa.value?.trim().length > 0
})

async function confirmar() {
  if (!props.elemento || !podeConfirmar.value) return
  salvando.value = true
  try {
    const id = parseInt(props.elemento.id, 10)
    await docStore.emendar(
      props.documentoId,
      props.secao,
      id,
      props.acao,
      props.acao === 'ALTERAR' ? novoConteudo.value : null,
      null,
      props.acao !== 'DESFAZER' ? justificativa.value : null,
    )
    editorStore.reload()
    emit('update:model-value', false)
    emit('confirmado')
    $q.notify({ type: 'positive', message: 'Emenda aplicada com sucesso.' })
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao aplicar emenda: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    salvando.value = false
  }
}
</script>

<script>
export default { name: 'EmendaDialog' }
</script>
