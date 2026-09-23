<template>
  <q-dialog :model-value="modelValue" @update:model-value="$emit('update:model-value', $event)" persistent>
    <q-card style="min-width:580px;max-width:720px;width:100%">
      <q-card-section class="row items-center q-pb-none">
        <q-icon name="mdi-plus-circle-outline" size="20px" color="green-8" class="q-mr-sm" />
        <div class="text-h6">Incluir novo elemento</div>
        <q-space />
        <q-btn icon="mdi-close" flat round dense v-close-popup :disable="salvando" />
      </q-card-section>

      <q-separator class="q-mt-sm" />

      <q-card-section class="q-pa-md scroll" style="max-height:72vh">

        <!-- Tipo -->
        <div class="text-caption text-weight-bold q-mb-xs">Tipo de elemento *</div>
        <q-select
          v-model="tipo"
          :options="tipoOptions"
          emit-value map-options
          outlined dense
          class="q-mb-md"
          :disable="salvando"
        />

        <!-- Onde inserir (elemento pai) -->
        <div class="text-caption text-weight-bold q-mb-xs">Onde inserir *</div>
        <q-select
          v-model="containerEntry"
          :options="containerOptions"
          option-label="label"
          outlined dense
          class="q-mb-md"
          :disable="salvando || containerOptions.length <= 1"
        >
          <template #selected>
            <span>{{ containerEntry?.label ?? 'Selecione…' }}</span>
          </template>
        </q-select>

        <!-- Posição dentro do elemento pai escolhido -->
        <div class="text-caption text-weight-bold q-mb-xs">
          Posição{{ containerEntry ? ' dentro de ' + containerEntry.label : '' }} *
        </div>
        <q-select
          v-model="posicaoEntry"
          :options="posicaoOptions"
          option-label="label"
          option-value="value"
          outlined dense
          class="q-mb-md"
          :disable="salvando || !containerEntry || posicaoOptions.length <= 1"
        >
          <template #selected>
            <span>{{ posicaoEntry?.label ?? 'Selecione…' }}</span>
          </template>
          <template #option="scope">
            <q-item v-bind="scope.itemProps">
              <q-item-section>
                <q-item-label
                  :class="scope.opt.isFirst ? 'text-italic text-grey-7' : ''"
                >{{ scope.opt.label }}</q-item-label>
                <q-item-label v-if="scope.opt.previewTxt" caption class="ellipsis" style="max-width:360px">
                  {{ scope.opt.previewTxt }}
                </q-item-label>
              </q-item-section>
            </q-item>
          </template>
        </q-select>

        <!-- Pré-visualização da numeração resultante -->
        <div v-if="labelResultante" class="q-mb-md q-pa-sm rounded-borders" style="background:rgba(2,110,44,0.08);border:1px solid rgba(2,110,44,0.25)">
          <span class="text-caption text-green-9">Rótulo resultante: </span>
          <strong class="text-green-9">{{ labelResultante }}</strong>
        </div>

        <!-- Conteúdo: título simples para elementos superiores ao artigo, editor para os demais -->
        <template v-if="isSuperTipo">
          <div class="text-caption text-weight-bold q-mb-xs">Título</div>
          <q-input
            v-model="titulo"
            outlined dense
            class="q-mb-md"
            placeholder="Ex.: DISPOSIÇÕES GERAIS"
            :disable="salvando"
          />
        </template>
        <template v-else>
          <div class="text-caption text-weight-bold q-mb-xs">Conteúdo</div>
          <div
            style="border:1px solid rgba(0,0,0,0.2);border-radius:4px;overflow:hidden"
            class="q-mb-md"
          >
            <WysiwygEditor
              :key="dialogKey"
              v-model="conteudo"
            />
          </div>
        </template>

        <!-- Justificativa -->
        <div class="text-caption text-weight-bold q-mb-xs">Justificativa *</div>
        <q-input
          v-model="justificativa"
          outlined dense
          type="textarea"
          :rows="3"
          placeholder="Informe o motivo da inclusão..."
          :disable="salvando"
        />
      </q-card-section>

      <q-separator />

      <q-card-actions align="right" class="q-px-md q-py-sm">
        <q-btn flat label="Cancelar" v-close-popup :disable="salvando" />
        <q-btn
          unelevated
          color="green-8"
          label="Incluir elemento"
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
import { useDocumentosStore } from '@/stores/documentos.js'
import { useEditorStore } from '@/stores/editor.js'
import { formatLabel, rotuloDaInclusao } from '@/utils/numbering.js'
import WysiwygEditor from '@/components/editor/WysiwygEditor.vue'

const props = defineProps({
  modelValue:  { type: Boolean, required: true },
  secao:       { type: String, required: true },
  documentoId: { type: [String, Number], required: true },
  // Elementos da parte normativa (árvore completa) para montar o seletor de posição
  elementos:   { type: Array, default: () => [] },
})

const emit = defineEmits(['update:model-value', 'confirmado'])

const $q          = useQuasar()
const docStore    = useDocumentosStore()
const editorStore = useEditorStore()

const salvando      = ref(false)
const tipo          = ref('ARTIGO')
const conteudo      = ref('')
const titulo        = ref('')
const justificativa = ref('')
const dialogKey     = ref(0)

const SUPER_TIPOS = new Set(['CAPITULO', 'SECAO_NORMATIVA', 'SUBSECAO_NORMATIVA'])
const isSuperTipo = computed(() => SUPER_TIPOS.has(tipo.value))

// ── Tipos disponíveis ──────────────────────────────────────────────────────────
const TIPO_OPTIONS = [
  { label: 'Artigo',                  value: 'ARTIGO'          },
  { label: 'Capítulo',               value: 'CAPITULO'        },
  { label: 'Seção',                   value: 'SECAO_NORMATIVA' },
  { label: 'Subseção',               value: 'SUBSECAO_NORMATIVA' },
  { label: 'Parágrafo (§)',          value: 'PARAGRAFO'       },
  { label: 'Parágrafo único',        value: 'PARAGRAFO_UNICO' },
  { label: 'Inciso',                 value: 'INCISO'          },
  { label: 'Alínea',                 value: 'ALINEA'          },
  { label: 'Sub-alínea',             value: 'SUB_ALINEA'      },
]

const tipoOptions = computed(() => TIPO_OPTIONS)

// Extrai preview de texto de um elemento
function extractPreview(el) {
  if (!el?.conteudo) return ''
  try {
    const visit = (node) => {
      if (!node) return ''
      if (node.text) return node.text
      if (node.content) return node.content.map(visit).join('')
      return ''
    }
    const txt = visit(JSON.parse(el.conteudo)).trim()
    return txt.length > 60 ? txt.slice(0, 60) + '…' : txt
  } catch { return '' }
}

// ── Elemento pai (container) ao qual o novo elemento pode ser vinculado ──────
// Para cada tipo de elemento, `parents` lista os tipos de container aos quais ele
// pode ser filho direto, e `root` indica se ele pode existir solto no nível raiz
// da Parte Normativa. Espelha a mesma hierarquia usada em "Mover para" no editor.
const PARENT_TIPOS_FOR = {
  artigo:             { parents: ['capitulo', 'secao_normativa', 'subsecao_normativa'], root: true },
  capitulo:           { parents: [], root: true },
  secao_normativa:    { parents: ['capitulo'], root: false },
  subsecao_normativa: { parents: ['secao_normativa'], root: false },
  paragrafo:          { parents: ['artigo'], root: false },
  paragrafo_unico:    { parents: ['artigo'], root: false },
  inciso:             { parents: ['artigo', 'paragrafo', 'paragrafo_unico'], root: false },
  alinea:             { parents: ['inciso'], root: false },
  sub_alinea:         { parents: ['alinea'], root: false },
}

function collectContainersOfTypes(elementos, tipos, result = []) {
  for (const el of elementos ?? []) {
    if (tipos.includes(el.tipo) && el.emendaStatus !== 'REVOGADO') result.push(el)
    if (el.filhos?.length) collectContainersOfTypes(el.filhos, tipos, result)
  }
  return result
}

// ── Onde inserir: containers válidos para o tipo selecionado ─────────────────
const containerOptions = computed(() => {
  const targetTipo = tipo.value?.toLowerCase()
  const rule = PARENT_TIPOS_FOR[targetTipo]
  if (!rule) return []

  const opts = []
  if (rule.root) {
    opts.push({ label: 'Nível superior (Parte Normativa)', el: null, filhos: props.elementos })
  }
  for (const c of collectContainersOfTypes(props.elementos, rule.parents)) {
    opts.push({ label: formatLabel(c), el: c, filhos: c.filhos ?? [] })
  }
  return opts
})

// containerOptions recria os objetos de opção a cada reavaliação (mesmo quando os
// elementos subjacentes não mudaram de fato), então comparar por REFERÊNCIA de
// objeto perde a seleção do usuário a qualquer recomputação. containerIdRaw guarda
// só a CHAVE ESTÁVEL (id do elemento, ou '__root__') da escolha explícita — nula
// até o usuário escolher — e containerEntry é um computed com get/set que resolve
// essa chave contra as opções atuais, caindo no último container quando a escolha
// ainda não existe ou deixou de ser válida para o tipo selecionado.
function containerKey(opt) {
  return opt?.el ? String(opt.el.id) : '__root__'
}
const containerIdRaw = ref(null)

const containerEntry = computed({
  get() {
    const opts = containerOptions.value
    if (containerIdRaw.value !== null) {
      const found = opts.find(o => containerKey(o) === containerIdRaw.value)
      if (found) return found
    }
    return opts.length ? opts[opts.length - 1] : null
  },
  set(val) {
    containerIdRaw.value = val ? containerKey(val) : null
  },
})

// ── Posição dentro do container escolhido ─────────────────────────────────────
const posicaoOptions = computed(() => {
  const targetTipo = tipo.value?.toLowerCase()
  const container   = containerEntry.value
  if (!targetTipo || !container) return []

  const filhos = container.filhos ?? []
  const opts = [
    {
      label:    'No início',
      value:    null,
      isFirst:  true,
      siblings: filhos,
      parentEl: container.el,
    },
  ]

  // Inclui todos os elementos do tipo já existentes neste container (INALTERADO,
  // ALTERADO, REVOGADO, INCLUIDO), pois o usuário pode querer inserir após qualquer
  // posição textual.
  for (const el of filhos.filter(f => f.tipo === targetTipo)) {
    opts.push({
      label:      `Após ${formatLabel(el)}`,
      previewTxt: extractPreview(el),
      value:      el.id,
      isFirst:    false,
      el,
      siblings:   filhos,
      parentEl:   container.el,
    })
  }

  return opts
})

// Mesmo raciocínio de containerEntry: posicaoValueRaw guarda a chave estável (o
// `value` da opção — null para "No início", ou o id do elemento-âncora) em vez do
// objeto em si. `undefined` = usuário ainda não escolheu (distinto de `null`, que é
// a escolha explícita de "No início").
const posicaoValueRaw = ref(undefined)

const posicaoEntry = computed({
  get() {
    const opts = posicaoOptions.value
    if (posicaoValueRaw.value !== undefined) {
      const found = opts.find(o => o.value === posicaoValueRaw.value)
      if (found) return found
    }
    return opts.length ? opts[opts.length - 1] : null
  },
  set(val) {
    posicaoValueRaw.value = val ? val.value : undefined
  },
})

// ── elementOrder calculado para inserção ──────────────────────────────────────
const computedOrder = computed(() => {
  const opt = posicaoEntry.value
  if (!opt) return null   // posicaoEntry = null → opção "antes do primeiro"

  if (opt.isFirst) {
    // Insere antes de tudo: acha o menor elementOrder entre irmãos e subtrai 1
    const sibs = opt.siblings ?? props.elementos
    const minOrder = sibs.reduce((m, s) => Math.min(m, s.elementOrder ?? 0), Infinity)
    return isFinite(minOrder) && minOrder > 0 ? minOrder - 1 : 0
  }

  const afterEl  = opt.el
  const siblings = opt.siblings ?? []

  // Conta quantos INCLUIDO já foram inseridos entre afterEl e o próximo não-INCLUIDO
  const sorted = [...siblings].sort((a, b) => (a.elementOrder ?? 0) - (b.elementOrder ?? 0))
  const afterIdx = sorted.findIndex(s => s.id === afterEl.id)

  // Encontra o próximo não-INCLUIDO após afterEl
  let nextNonIncluded = null
  for (let i = afterIdx + 1; i < sorted.length; i++) {
    if (sorted[i].incluidoPorEmenda !== true) { nextNonIncluded = sorted[i]; break }
  }

  const alreadyInserted = sorted
    .slice(afterIdx + 1)
    .filter(s => {
      if (s.incluidoPorEmenda !== true) return false
      if (nextNonIncluded && (s.elementOrder ?? 0) >= (nextNonIncluded.elementOrder ?? 0)) return false
      return true
    }).length

  // Se afterEl não tem elementOrder (ex.: elemento revogado sem order), usa o máximo
  // dos irmãos para garantir que o novo elemento seja inserido ao final, não no início.
  const afterElOrder = afterEl.elementOrder != null
    ? afterEl.elementOrder
    : siblings.reduce((m, s) => Math.max(m, s.elementOrder ?? 0), 0)

  return afterElOrder + alreadyInserted + 1
})

const parentId = computed(() => {
  const opt = posicaoEntry.value
  if (!opt) return null
  // "No início" também pode ser dentro de um container real (ex.: uma seção
  // vazia) — o pai correto é sempre opt.parentEl, com ou sem isFirst. Só é
  // null de fato quando o container escolhido é o "Nível superior".
  return opt.parentEl?.id ? parseInt(opt.parentEl.id, 10) : null
})

// ── Pré-visualização do rótulo resultante ─────────────────────────────────────
// Só uma prévia -- a numeração real é recalculada ao salvar. As regras estão em rotuloDaInclusao (utils/numbering.js),
// com testes; aqui só se junta a posição escolhida.
const labelResultante = computed(() => rotuloDaInclusao(
  props.elementos,
  tipo.value?.toLowerCase(),
  posicaoEntry.value && { ...posicaoEntry.value, containerEl: containerEntry.value?.el },
))

// ── Reset ao abrir ────────────────────────────────────────────────────────────
// containerEntry/posicaoEntry não precisam de reset explícito: são computeds com
// fallback automático (ver acima) — bastam limpar as escolhas EXPLÍCITAS (as chaves raw).
watch(() => props.modelValue, (open) => {
  if (open) {
    tipo.value            = 'ARTIGO'
    containerIdRaw.value  = null
    posicaoValueRaw.value = undefined
    conteudo.value        = ''
    titulo.value          = ''
    justificativa.value   = ''
    dialogKey.value++
  }
})

// Limpa a escolha explícita de container quando o tipo muda (a de posição já se
// invalida sozinha, pois deixa de pertencer a posicaoOptions do novo container).
watch(tipo, () => { containerIdRaw.value = null })

// Limpa a escolha explícita de posição quando o container muda.
watch(containerEntry, () => { posicaoValueRaw.value = undefined })

const podeConfirmar = computed(() =>
  !!tipo.value && !!containerEntry.value && !!posicaoEntry.value && justificativa.value?.trim().length > 0
)

// ── Confirmação ───────────────────────────────────────────────────────────────
async function confirmar() {
  if (!podeConfirmar.value) return
  salvando.value = true
  try {
    await docStore.incluirElementoEmenda(
      props.documentoId,
      props.secao,
      tipo.value,
      isSuperTipo.value  ? (titulo.value.trim()   || null) : null,
      !isSuperTipo.value ? (conteudo.value || null) : null,
      parentId.value,
      computedOrder.value,
      justificativa.value.trim(),
      editorStore.documento?.versao,
    )
    editorStore.reload()
    emit('update:model-value', false)
    emit('confirmado')
    $q.notify({ type: 'positive', message: 'Elemento incluído com sucesso.' })
  } catch (e) {
    if (e?.status === 409) editorStore.reload()
    $q.notify({ type: 'negative', message: `Erro ao incluir elemento: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    salvando.value = false
  }
}
</script>

<script>
export default { name: 'IncluirElementoDialog' }
</script>
