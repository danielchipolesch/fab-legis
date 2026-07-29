<template>
  <aside class="editor-sidebar">

    <q-linear-progress
      v-if="editorStore.adicionando"
      indeterminate
      color="primary"
      style="height:2px;flex-shrink:0"
    />

    <!-- Informações do documento -->
    <div class="sidebar-doc-header q-px-sm q-pt-md q-pb-sm">
      <div class="text-h6 text-weight-bold text-primary ellipsis" style="line-height:1.3">
        {{ docLabel }}
      </div>
      <div v-if="documento?.titulo" class="text-body2 text-grey-8 q-mt-xxs ellipsis-2-lines">
        {{ documento.titulo }}
      </div>
      <div v-if="documento?.assunto_basico" class="text-caption text-grey-6 q-mt-xxs ellipsis">
        {{ documento.assunto_basico }}
      </div>
    </div>

    <!-- Status -->
    <div class="q-px-sm q-pb-sm">
      <StatusBadge v-if="documento?.status" :status="documento.status" size="sm" />
    </div>

    <q-separator />

    <!-- 5 ícones de ação (funções a definir) -->
    <div class="sidebar-actions row justify-around items-center q-py-xs">
      <q-btn flat round dense size="sm" color="grey-7" icon="mdi-file-document-edit-outline">
        <q-tooltip anchor="top middle" self="bottom middle">Metadados</q-tooltip>
      </q-btn>
      <q-btn flat round dense size="sm" color="grey-7" icon="mdi-source-branch">
        <q-tooltip anchor="top middle" self="bottom middle">Comparar versões</q-tooltip>
      </q-btn>
      <q-btn flat round dense size="sm" color="grey-7" icon="mdi-download-outline">
        <q-tooltip anchor="top middle" self="bottom middle">Exportar</q-tooltip>
      </q-btn>
      <q-btn flat round dense size="sm" color="grey-7" icon="mdi-history">
        <q-tooltip anchor="top middle" self="bottom middle">Histórico</q-tooltip>
      </q-btn>
      <q-btn flat round dense size="sm" color="grey-7" icon="mdi-dots-horizontal">
        <q-tooltip anchor="top middle" self="bottom middle">Mais opções</q-tooltip>
      </q-btn>
    </div>

    <q-separator />

    <!-- Árvore de seções do documento -->
    <div class="sidebar-body">
      <template v-for="secao in secoes" :key="secao.id">

        <div
          class="secao-header q-pa-sm row items-center"
          :class="{ 'secao-header--active': isExpandida(secao.tipo) }"
          @click="toggleSecao(secao.tipo)"
        >
          <q-icon
            :name="isExpandida(secao.tipo) ? 'mdi-chevron-down' : 'mdi-chevron-right'"
            size="16px"
            class="q-mr-xs"
          />
          <q-icon :name="secaoIcon(secao.tipo)" size="14px" :color="secaoIconColor(secao.tipo)" class="q-mr-sm" />
          <span class="text-caption text-weight-bold text-uppercase text-grey-7">
            {{ secao.titulo }}
          </span>
        </div>

        <div v-show="isExpandida(secao.tipo)" class="secao-elementos q-px-xs q-pb-sm">

          <!-- Elementos fixos (parte preliminar / parte final) -->
          <template v-if="secao.tipo !== 'parte_normativa'">
            <div
              v-for="el in secao.elementos"
              :key="el.id"
              class="fixed-item row items-center q-px-md q-py-xs"
              :class="{ 'fixed-item--active': selectedId === el.id }"
              @click="$emit('select', el.id)"
            >
              <q-icon :name="elementIcon(el.tipo)" size="13px" color="teal-6" class="q-mr-sm" />
              <span class="text-caption col ellipsis">{{ formatLabel(el) }}</span>
              <q-icon
                v-if="!isElFilled(el)"
                name="mdi-alert-circle"
                color="amber-7"
                size="13px"
                class="q-ml-xs"
              >
                <q-tooltip anchor="top middle" self="bottom middle">
                  Seção vazia — necessária para aprovação
                </q-tooltip>
              </q-icon>
            </div>
          </template>

          <!-- Parte normativa: árvore recursiva com drag-and-drop -->
          <template v-else>
            <NormTreeItem
              v-if="normativaElementos.length"
              :elements="normativaElementos"
              :selected-id="selectedId"
              @reorder="$emit('reorder-normativa')"
            />
            <div v-else class="text-caption text-grey-6 q-pa-sm text-italic">
              Sem elementos normativos
            </div>

            <div class="q-mt-sm column q-px-xs" style="gap:4px">
              <div>
                <q-btn
                  outline
                  color="primary"
                  size="sm"
                  class="full-width"
                  :disable="hasTopLevelArtigos || editorStore.adicionando"
                  :loading="editorStore.adicionando"
                >
                  <q-icon left name="mdi-folder-plus-outline" />
                  Novo Capítulo
                  <q-tooltip v-if="hasTopLevelArtigos" anchor="top middle" self="bottom middle">
                    Remova os artigos soltos antes de adicionar capítulos
                  </q-tooltip>
                  <q-menu v-if="!hasTopLevelArtigos">
                    <q-list dense style="min-width:240px">
                      <q-item-label header>Título do capítulo</q-item-label>
                      <q-item
                        v-for="preset in CAPITULO_PRESETS"
                        :key="preset"
                        clickable
                        v-close-popup
                        :disable="existingCapituloTitulos.has(preset)"
                        @click="$emit('add-capitulo', preset)"
                      >
                        <q-item-section>{{ preset }}</q-item-section>
                      </q-item>
                      <q-separator />
                      <q-item clickable v-close-popup @click="$emit('add-capitulo', '')">
                        <q-item-section avatar>
                          <q-icon name="mdi-pencil-outline" />
                        </q-item-section>
                        <q-item-section>Personalizado (sem título)</q-item-section>
                      </q-item>
                    </q-list>
                  </q-menu>
                </q-btn>
              </div>

              <q-btn
                outline
                size="sm"
                class="full-width"
                :disable="hasCapitulos || editorStore.adicionando"
                :loading="editorStore.adicionando"
                @click="$emit('add-artigo')"
              >
                <q-icon left name="mdi-plus" />
                Novo Artigo
                <q-tooltip v-if="hasCapitulos" anchor="top middle" self="bottom middle">
                  Adicione artigos dentro dos capítulos existentes
                </q-tooltip>
              </q-btn>
            </div>
          </template>

        </div>

        <q-separator />
      </template>
    </div>
  </aside>
</template>

<script setup>
import { reactive, ref, computed, watch, provide } from 'vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { formatLabel, elementIcon } from '@/utils/numbering.js'
import { useEditorStore } from '@/stores/editor.js'
import NormTreeItem from './NormTreeItem.vue'

const editorStore = useEditorStore()

const props = defineProps({
  documento:  { type: Object, default: null },
  docLabel:   { type: String, default: '' },
  secoes:     { type: Array, default: () => [] },
  selectedId: { type: String, default: null },
})

const emit = defineEmits([
  'select',
  'move-up', 'move-down',
  'add-child', 'add-artigo', 'add-capitulo',
  'promote', 'demote', 'remove',
  'reorder-normativa',
])

const CAPITULO_PRESETS = [
  'DISPOSIÇÕES PRELIMINARES',
  'DISPOSIÇÕES GERAIS',
  'DISPOSIÇÕES FINAIS',
  'DISPOSIÇÕES TRANSITÓRIAS',
]

// ── Estado das seções colapsadas ─────────────────────────────────────────────
const secaoExpandida = reactive({
  parte_preliminar: true,
  parte_normativa:  true,
  parte_final:      true,
})

function toggleSecao(tipo) {
  secaoExpandida[tipo] = !secaoExpandida[tipo]
}

function isExpandida(tipo) {
  return secaoExpandida[tipo] !== false
}

// ── Elementos normativos (lista reativa) ─────────────────────────────────────
const normativaElementos = computed(() => {
  const s = props.secoes.find(s => s.tipo === 'parte_normativa')
  return s?.elementos ?? []
})

// ── Estado de expansão: { [id]: boolean } — undefined = expandido por padrão ─
const expanded = reactive({})

function markExpandable(elements) {
  for (const el of elements ?? []) {
    if (el.filhos?.length) {
      if (!(el.id in expanded)) expanded[el.id] = true
      markExpandable(el.filhos)
    }
  }
}

watch(normativaElementos, (els) => {
  markExpandable(els)
}, { immediate: true, deep: true })

// ── Provide para NormTreeItem (injeção sem prop-drilling) ─────────────────────
provide('normExpanded', expanded)
provide('normCallbacks', {
  select:   (id)       => emit('select', id),
  addChild: (pid, t)   => emit('add-child', pid, t),
  promote:  (id)       => emit('promote', id),
  demote:   (id)       => emit('demote', id),
  remove:   (id)       => emit('remove', id),
})

// ── Computeds para os botões de adicionar ────────────────────────────────────
const hasCapitulos       = computed(() => normativaElementos.value.some(e => e.tipo === 'capitulo'))
const hasTopLevelArtigos = computed(() => normativaElementos.value.some(e => e.tipo === 'artigo'))

const existingCapituloTitulos = computed(() =>
  new Set(
    normativaElementos.value
      .filter(e => e.tipo === 'capitulo' && e.titulo)
      .map(e => e.titulo.toUpperCase())
  )
)

// ── Helpers p/ itens fixos ───────────────────────────────────────────────────
function extractElText(conteudo) {
  if (!conteudo) return ''
  try {
    const visit = (node) => {
      if (!node) return ''
      if (node.text) return node.text
      if (node.content) return node.content.map(visit).join('')
      return ''
    }
    return visit(JSON.parse(conteudo)).trim()
  } catch { return '' }
}
const isElFilled = (el) => extractElText(el?.conteudo).length > 0

function secaoIcon(tipo) {
  const m = {
    parte_preliminar: 'mdi-text-box-outline',
    parte_normativa:  'mdi-format-list-numbered',
    parte_final:      'mdi-flag-outline',
  }
  return m[tipo] ?? 'mdi-folder-outline'
}

function secaoIconColor(tipo) {
  const m = {
    parte_preliminar: 'teal-6',
    parte_normativa:  'primary',
    parte_final:      'teal-6',
  }
  return m[tipo] ?? 'grey-6'
}
</script>

<style scoped>
.editor-sidebar {
  flex-shrink: 0;
  width: 290px;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.12);
  z-index: 1;
  overflow: hidden;
}

/* Cabeçalho do documento */
.sidebar-doc-header {
  flex-shrink: 0;
}

/* Ícones de ação */
.sidebar-actions {
  flex-shrink: 0;
}
.sidebar-actions .q-btn:hover {
  color: var(--q-primary) !important;
  background: rgba(74, 111, 165, 0.08);
}

/* Árvore de seções */
.sidebar-body {
  overflow-y: auto;
  flex: 1 1 auto;
}

.secao-header {
  cursor: pointer;
  background: var(--color-surface);
}
.secao-header:hover {
  background: rgba(74, 111, 165, 0.08);
}
.secao-header--active {
  background: rgba(74, 111, 165, 0.14);
}

/* Itens fixos (parte preliminar / parte final) */
.fixed-item {
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.15s;
  min-height: 26px;
}
.fixed-item:hover {
  background: rgba(74, 111, 165, 0.08);
}
.fixed-item--active {
  background: rgba(74, 111, 165, 0.16);
}

/* Área da parte normativa */
.secao-elementos :deep(.sortable-chosen) {
  outline: 2px dashed rgba(74, 111, 165, 0.4);
  border-radius: 6px;
}
</style>
