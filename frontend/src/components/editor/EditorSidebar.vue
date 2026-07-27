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

          <!-- Parte normativa: q-tree hierárquico nativo do Quasar -->
          <template v-else>
            <q-tree
              :nodes="normativaElementos"
              node-key="id"
              children-key="filhos"
              v-model:expanded="expandedNorm"
              :selected="selectedId"
              @update:selected="val => val && $emit('select', val)"
              no-selection-unset
              dense
              class="norm-tree"
              no-nodes-label="Sem elementos normativos"
            >
              <template v-slot:default-header="{ node }">
                <div class="row items-center full-width norm-node">
                  <!-- Ícone do tipo -->
                  <q-icon
                    :name="elementIcon(node.tipo)"
                    size="13px"
                    :color="isGroupingType(node.tipo) ? 'primary' : 'blue-grey-6'"
                    class="q-mr-xs"
                    style="flex-shrink:0"
                  />
                  <!-- Rótulo -->
                  <span
                    class="norm-label col ellipsis"
                    :class="{
                      'text-weight-bold text-uppercase': node.tipo === 'capitulo',
                      'text-weight-bold': node.tipo === 'secao_normativa' || node.tipo === 'subsecao_normativa',
                      'text-weight-medium': !isGroupingType(node.tipo),
                    }"
                  >{{ formatLabel(node) }}</span>
                  <!-- Preview de conteúdo -->
                  <span
                    v-if="!isGroupingType(node.tipo) && nodePreview(node)"
                    class="text-caption text-grey-6 q-ml-xs norm-preview"
                  >{{ nodePreview(node) }}</span>
                  <!-- Indicador de vazio -->
                  <q-icon
                    v-if="!isGroupingType(node.tipo) && !isNodeFilled(node)"
                    name="mdi-alert-circle"
                    color="amber-7"
                    size="12px"
                    class="q-ml-xs"
                    style="flex-shrink:0"
                  >
                    <q-tooltip anchor="top middle" self="bottom middle">
                      Vazio — necessário para aprovação
                    </q-tooltip>
                  </q-icon>
                  <!-- Ações (visíveis ao passar o mouse) -->
                  <div class="norm-actions row items-center q-ml-xs" style="gap:2px;flex-shrink:0">
                    <q-btn round size="xs" flat dense color="grey" @click.stop="$emit('move-up', node.id)">
                      <q-icon size="11px" name="mdi-arrow-up" />
                      <q-tooltip anchor="center right" self="center left">Mover acima</q-tooltip>
                    </q-btn>
                    <q-btn round size="xs" flat dense color="grey" @click.stop="$emit('move-down', node.id)">
                      <q-icon size="11px" name="mdi-arrow-down" />
                      <q-tooltip anchor="center right" self="center left">Mover abaixo</q-tooltip>
                    </q-btn>
                    <q-btn
                      v-if="childOptions(node).length || canPromoteNode(node)"
                      round size="xs" flat dense color="grey"
                      @click.stop
                    >
                      <q-icon size="11px" name="mdi-plus" />
                      <q-menu>
                        <q-list dense style="min-width:180px">
                          <template v-if="childOptions(node).length">
                            <q-item-label header>Adicionar como filho</q-item-label>
                            <q-item
                              v-for="opt in childOptions(node)"
                              :key="opt.tipo"
                              clickable v-close-popup
                              @click="$emit('add-child', node.id, opt.tipo)"
                            >
                              <q-item-section avatar>
                                <q-icon :name="elementIcon(opt.tipo)" />
                              </q-item-section>
                              <q-item-section>{{ opt.label }}</q-item-section>
                            </q-item>
                            <q-separator />
                          </template>
                          <template v-if="canPromoteNode(node)">
                            <q-item-label header>Reorganizar</q-item-label>
                            <q-item clickable v-close-popup @click="$emit('promote', node.id)">
                              <q-item-section avatar>
                                <q-icon name="mdi-arrow-collapse-up" />
                              </q-item-section>
                              <q-item-section>Promover nível</q-item-section>
                            </q-item>
                            <q-item clickable v-close-popup @click="$emit('demote', node.id)">
                              <q-item-section avatar>
                                <q-icon name="mdi-arrow-expand-down" />
                              </q-item-section>
                              <q-item-section>Rebaixar nível</q-item-section>
                            </q-item>
                            <q-separator />
                          </template>
                          <q-item clickable v-close-popup class="text-negative" @click="$emit('remove', node.id)">
                            <q-item-section avatar>
                              <q-icon name="mdi-delete-outline" color="negative" />
                            </q-item-section>
                            <q-item-section>Remover</q-item-section>
                          </q-item>
                        </q-list>
                      </q-menu>
                    </q-btn>
                    <q-btn v-else round size="xs" flat dense color="negative" @click.stop="$emit('remove', node.id)">
                      <q-icon size="11px" name="mdi-delete-outline" />
                      <q-tooltip anchor="center right" self="center left">Remover</q-tooltip>
                    </q-btn>
                  </div>
                </div>
              </template>
            </q-tree>

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
import { reactive, ref, computed, watch } from 'vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { formatLabel, elementIcon } from '@/utils/numbering.js'
import { useEditorStore } from '@/stores/editor.js'

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

// ── Tipos de agrupamento / conteúdo ──────────────────────────────────────────
const GROUPING_TIPOS = new Set(['capitulo', 'secao_normativa', 'subsecao_normativa'])
const ARTIGO_TIPOS   = new Set(['artigo', 'paragrafo', 'paragrafo_unico', 'inciso', 'alinea', 'sub_alinea'])

const CHILD_MAP = {
  capitulo:           [
    { tipo: 'secao_normativa', label: 'Seção' },
    { tipo: 'artigo',          label: 'Artigo' },
  ],
  secao_normativa:    [
    { tipo: 'subsecao_normativa', label: 'Subseção' },
    { tipo: 'artigo',             label: 'Artigo' },
  ],
  subsecao_normativa: [{ tipo: 'artigo', label: 'Artigo' }],
  artigo:             [
    { tipo: 'paragrafo_unico', label: 'Parágrafo único' },
    { tipo: 'paragrafo',       label: 'Parágrafo (§)' },
    { tipo: 'inciso',          label: 'Inciso' },
  ],
  paragrafo_unico: [{ tipo: 'inciso', label: 'Inciso' }],
  paragrafo:       [{ tipo: 'inciso', label: 'Inciso' }],
  inciso:          [{ tipo: 'alinea', label: 'Alínea' }],
  alinea:          [{ tipo: 'sub_alinea', label: 'Sub-alínea' }],
}

// ── Helpers p/ q-tree ────────────────────────────────────────────────────────
const isGroupingType = (tipo) => GROUPING_TIPOS.has(tipo)
const canPromoteNode = (node) => ARTIGO_TIPOS.has(node.tipo)
const childOptions   = (node) => CHILD_MAP[node.tipo] ?? []

const isNodeFilled = (node) => {
  if (!node?.conteudo) return false
  return node.conteudo.replace(/<[^>]*>/g, '').trim().length > 0
}

const nodePreview = (node) => {
  const text = node.conteudo?.replace(/<[^>]+>/g, '').trim() ?? ''
  return text.length > 28 ? text.slice(0, 28) + '…' : text
}

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

// ── Nós expandidos no q-tree (começa com tudo aberto) ───────────────────────
const expandedNorm = ref([])

function collectIds(elements) {
  const ids = []
  for (const el of elements ?? []) {
    ids.push(el.id)
    if (el.filhos?.length) ids.push(...collectIds(el.filhos))
  }
  return ids
}

watch(normativaElementos, (els) => {
  const all = new Set([...expandedNorm.value, ...collectIds(els)])
  expandedNorm.value = [...all]
}, { immediate: true })

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
const isElFilled = (el) => {
  if (!el?.conteudo) return false
  return el.conteudo.replace(/<[^>]*>/g, '').trim().length > 0
}

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

/* ── Q-Tree parte normativa ──────────────────────────────────────────────── */
.norm-tree {
  padding: 0 !important;
}

/* Cabeçalho de cada nó */
.norm-tree :deep(.q-tree__node-header) {
  padding: 2px 4px 2px 2px;
  border-radius: 6px;
  min-height: 28px;
  align-items: center;
}
.norm-tree :deep(.q-tree__node-header:hover) {
  background: rgba(74, 111, 165, 0.08);
}

/* Nó selecionado */
.norm-tree :deep(.q-tree__node--selected > .q-tree__node-header) {
  background: rgba(74, 111, 165, 0.16) !important;
}

/* Seta de expansão */
.norm-tree :deep(.q-tree__arrow) {
  color: var(--q-primary);
  opacity: 0.65;
  font-size: 14px;
}

/* Rótulo do nó */
.norm-label {
  font-size: 0.78rem;
  line-height: 1.25;
}

/* Preview de conteúdo (max ~60px) */
.norm-preview {
  max-width: 56px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex-shrink: 0;
  font-size: 0.7rem;
}

/* Ações aparecem ao passar o mouse */
.norm-actions {
  opacity: 0;
  transition: opacity 0.15s;
}
.norm-tree :deep(.q-tree__node-header:hover .norm-actions) {
  opacity: 1;
}
</style>
