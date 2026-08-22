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

        <!-- ALTERAR: editor para novo conteúdo + justificativa (ou edição livre se INCLUIDO) -->
        <template v-if="acao === 'ALTERAR'">
          <div v-if="isIncluido" class="q-mb-md q-pa-sm rounded-borders text-caption text-green-9"
               style="background:rgba(2,110,44,0.08);border:1px solid rgba(2,110,44,0.25)">
            Este elemento foi <strong>incluído por emenda</strong>. A edição do conteúdo não gera
            cláusula de alteração — a cláusula de inclusão continua vigente.
          </div>
          <!-- Campo de título para elementos superiores ao artigo (capítulo, seção, subseção) -->
          <template v-if="isSuperTipo">
            <div class="text-caption text-weight-bold q-mb-xs">Novo título *</div>
            <q-input
              v-model="novoTitulo"
              outlined dense
              class="q-mb-md"
              :disable="salvando"
            />
          </template>
          <template v-else>
            <div class="text-caption text-weight-bold q-mb-xs">Novo conteúdo *</div>
            <div style="border:1px solid rgba(0,0,0,0.2);border-radius:4px;overflow:hidden" class="q-mb-md">
              <WysiwygEditor
                :key="elemento?.id + '-alterar'"
                v-model="novoConteudo"
              />
            </div>
          </template>
          <template v-if="!isIncluido">
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
            <template v-if="isIncluido">
              Por ser um elemento <strong>incluído por emenda</strong> ainda não publicado, ele será <strong>removido</strong> permanentemente.
            </template>
            <template v-else>
              O conteúdo da emenda será removido e a situação voltará a <strong>INALTERADO</strong>.
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

const SUPER_TIPOS = new Set(['capitulo', 'secao_normativa', 'subsecao_normativa'])

const salvando      = ref(false)
const justificativa = ref('')
const novoConteudo  = ref('')
const novoTitulo    = ref('')

// Só é "edição livre de inclusão" enquanto a inclusão ainda não foi publicada
// (clausulaEmenda null). Uma vez publicada, uma nova emenda sobre ela segue o fluxo
// normal — exige justificativa e gera sua própria cláusula, exatamente como uma
// alteração comum, pois o ciclo de emendas se repete também para inclusões.
const isIncluido = computed(() =>
  props.elemento?.emendaStatus === 'INCLUIDO' && !props.elemento?.clausulaEmenda
)
const isSuperTipo = computed(() => SUPER_TIPOS.has(props.elemento?.tipo ?? ''))

watch(() => [props.modelValue, props.elemento, props.acao], ([open]) => {
  if (open) {
    justificativa.value = ''
    novoConteudo.value  = props.elemento?.conteudoEmenda ?? props.elemento?.conteudo ?? ''
    novoTitulo.value    = props.elemento?.tituloEmenda   ?? props.elemento?.titulo   ?? ''
  }
}, { immediate: false })

const acaoTitulo = computed(() => {
  if (props.acao === 'ALTERAR' && isIncluido.value) return 'Editar conteúdo'
  if (props.acao === 'DESFAZER' && isIncluido.value) return 'Excluir elemento'
  return { ALTERAR: 'Alterar texto', REVOGAR: 'Revogar elemento', DESFAZER: 'Desfazer emenda' }[props.acao] ?? props.acao
})

const acaoIcon = computed(() => ({
  ALTERAR:  'mdi-pencil-outline',
  REVOGAR:  'mdi-delete-outline',
  DESFAZER: isIncluido.value ? 'mdi-trash-can-outline' : 'mdi-undo-variant',
}[props.acao] ?? 'mdi-help'))

const acaoColor = computed(() => ({
  ALTERAR:  'primary',
  REVOGAR:  'negative',
  DESFAZER: 'negative',
}[props.acao] ?? 'grey'))

const elementoLabel = computed(() => props.elemento ? formatLabel(props.elemento) : '—')

const podeConfirmar = computed(() => {
  if (props.acao === 'DESFAZER') return true
  // Edição de INCLUIDO não requer justificativa
  if (props.acao === 'ALTERAR' && isIncluido.value) return true
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
      props.acao === 'ALTERAR' && !isSuperTipo.value ? novoConteudo.value : null,
      props.acao === 'ALTERAR' && isSuperTipo.value  ? novoTitulo.value  : null,
      props.acao !== 'DESFAZER' && !isIncluido.value ? justificativa.value : null,
      editorStore.documento?.versao,
    )
    editorStore.reload()
    emit('update:model-value', false)
    emit('confirmado')
    $q.notify({ type: 'positive', message: 'Emenda aplicada com sucesso.' })
  } catch (e) {
    // 409: outra pessoa salvou primeiro -- recarrega para mostrar o estado
    // real antes que o usuário tente de novo com dados desatualizados.
    if (e?.status === 409) editorStore.reload()
    $q.notify({ type: 'negative', message: `Erro ao aplicar emenda: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    salvando.value = false
  }
}
</script>

<script>
export default { name: 'EmendaDialog' }
</script>
