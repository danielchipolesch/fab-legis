<template>
  <div class="editor-page column" style="height:calc(100vh - 60px)">

    <!-- Top action bar -->
    <div class="editor-topbar q-px-md q-py-sm row items-center" style="gap:12px">
      <q-btn
        :to="{ name: 'home' }"
        icon="mdi-arrow-left"
        flat
        round
        dense
      />

      <!-- Título do documento (esquerda) -->
      <div class="col" style="min-width:0">
        <div class="text-subtitle2 text-weight-bold text-primary text-no-wrap ellipsis">
          {{ docLabel }}
        </div>
        <div class="text-caption text-grey-7 ellipsis">
          {{ documento?.assunto_basico }}
        </div>
      </div>

      <!-- Indicador de salvamento (centro absoluto) -->
      <div class="save-indicator" :class="saveIndicatorClass">
        <template v-if="saveStatus === 'saving'">
          <q-circular-progress indeterminate size="13px" :thickness="0.35" />
          <span>Salvando…</span>
        </template>
        <template v-else-if="saveStatus === 'dirty' || !hasSaved">
          <q-icon size="15px" name="mdi-pencil-circle-outline" />
          <span>Não salvo</span>
        </template>
        <template v-else>
          <q-icon size="15px" name="mdi-check-circle-outline" />
          <span>Salvo</span>
        </template>
      </div>

      <q-space />

      <StatusBadge v-if="documento" :status="documento.status" size="md" />

      <q-btn
        outline
        color="primary"
        class="q-ml-sm"
        :to="{ name: 'documento-comparar', params: { id: documentoId } }"
      >
        <q-icon left name="mdi-source-branch" />
        Comparar versões
      </q-btn>

      <q-btn
        outline
        color="deep-orange-7"
        class="q-ml-sm"
        :loading="pdfLoading"
        @click="baixarPdf"
      >
        <q-icon left name="mdi-file-pdf-box" />
        PDF
      </q-btn>
    </div>

    <q-separator />

    <!-- Metadata panel (collapsible) -->
    <transition name="slide-down">
      <div v-show="showMeta" class="q-px-md q-py-sm">
        <DocumentMetaPanel
          v-if="documento"
          :documento="documento"
          @update="onMetaUpdate"
        />
      </div>
    </transition>

    <div
      class="meta-toggle row items-center justify-center q-px-md"
      style="cursor:pointer;user-select:none"
      @click="showMeta = !showMeta"
    >
      <q-icon size="16px" :name="showMeta ? 'mdi-chevron-up' : 'mdi-chevron-down'" class="q-mr-xs" />
      <span class="text-caption text-grey-7">
        {{ showMeta ? 'Ocultar metadados' : 'Mostrar metadados' }}
      </span>
    </div>

    <q-separator />

    <!-- Main editor area -->
    <div class="editor-body row col" style="overflow:hidden">

      <!-- Left sidebar -->
      <EditorSidebar
        v-model="sidebarOpen"
        :secoes="documento?.secoes ?? []"
        :selected-id="editorStore.selectedElementId"
        @select="editorStore.selectElement($event)"
        @move-up="editorStore.moveUp($event)"
        @move-down="editorStore.moveDown($event)"
        @add-child="(parentId, tipo) => editorStore.addFilho(parentId, tipo)"
        @add-artigo="addArtigo"
        @add-capitulo="addCapitulo"
        @promote="editorStore.promote($event)"
        @demote="editorStore.demote($event)"
        @remove="editorStore.removeElement($event)"
        @reorder-normativa="onReorderNormativa"
      />

      <!-- Toggle sidebar button -->
      <q-btn
        v-if="!sidebarOpen"
        icon="mdi-menu"
        size="sm"
        unelevated
        color="primary"
        class="sidebar-toggle-btn"
        @click="sidebarOpen = true"
      />

      <!-- Content area -->
      <div class="editor-content q-pa-lg" style="overflow-y:auto">

        <!-- No element selected -->
        <div v-if="!editorStore.selectedElementId" class="column items-center justify-center text-grey-7" style="height:100%">
          <q-icon size="80px" class="q-mb-md" color="blue-grey-3" name="mdi-cursor-pointer" />
          <p class="text-h6">Selecione um elemento no painel esquerdo</p>
          <p class="text-body2">Clique em qualquer seção ou artigo para editá-lo aqui.</p>
        </div>

        <!-- Element editor -->
        <template v-else-if="selectedElement">
          <div class="element-editor">

            <!-- Element breadcrumb/label -->
            <div class="element-header q-mb-md">
              <q-breadcrumbs class="q-pa-none" active-color="grey-7">
                <q-breadcrumbs-el
                  v-for="item in breadcrumb"
                  :key="item.title"
                  :label="item.title"
                />
              </q-breadcrumbs>
              <div class="row items-center justify-between q-mt-sm">
                <div class="row items-center" style="gap:8px">
                  <q-icon :name="elementIcon(selectedElement.tipo)" color="primary" size="20px" />
                  <h2 class="text-h6 text-weight-bold text-primary q-my-none">
                    {{ formatLabel(selectedElement) }}
                  </h2>
                </div>
                <div class="row">
                  <q-btn
                    v-if="!isGroupingEl && hasChildren(selectedElement)"
                    size="sm"
                    outline
                    class="q-ml-sm"
                    @click="editorStore.demote(selectedElement.id)"
                  >
                    <q-icon left name="mdi-arrow-expand-down" />
                    Rebaixar
                  </q-btn>
                  <q-btn
                    v-if="!isGroupingEl"
                    size="sm"
                    outline
                    class="q-ml-sm"
                    @click="editorStore.promote(selectedElement.id)"
                  >
                    <q-icon left name="mdi-arrow-collapse-up" />
                    Promover
                  </q-btn>
                  <q-btn
                    size="sm"
                    outline
                    color="negative"
                    class="q-ml-sm"
                    @click="editorStore.removeElement(selectedElement.id)"
                  >
                    <q-icon left name="mdi-delete-outline" />
                    Remover
                  </q-btn>
                </div>
              </div>
            </div>

            <!-- Título editor para capítulo / seção / subseção -->
            <template v-if="isGroupingEl">
              <q-input
                :model-value="selectedElement.titulo"
                :label="groupingLabel"
                outlined
                dense
                :readonly="isReadonly"
                @update:model-value="onTituloUpdate"
              />
              <p class="text-caption text-grey-7 q-mt-xs">
                O título aparecerá em maiúsculas no documento (NSCA 5-3).
              </p>
            </template>

            <!-- WYSIWYG editor para elementos de conteúdo -->
            <WysiwygEditor
              v-else
              :model-value="selectedElement.conteudo"
              :readonly="isReadonly"
              @update:model-value="onContentUpdate"
            />

            <!-- Add child element shortcuts -->
            <div v-if="childOptions.length && !isReadonly" class="q-mt-md row items-center" style="flex-wrap:wrap">
              <span class="text-caption text-grey-7">Adicionar:</span>
              <q-btn
                v-for="opt in childOptions"
                :key="opt.tipo"
                size="sm"
                outline
                color="primary"
                class="q-ml-sm"
                @click="editorStore.addFilho(selectedElement.id, opt.tipo)"
              >
                <q-icon left :name="elementIcon(opt.tipo)" />
                {{ opt.label }}
              </q-btn>
              <q-btn
                v-if="!isGroupingEl"
                size="sm"
                outline
                class="q-ml-sm"
                @click="editorStore.addSibling(selectedElement.id, selectedElement.tipo)"
              >
                <q-icon left name="mdi-plus" />
                Mesmo nível
              </q-btn>
            </div>

          </div>
        </template>

      </div>

      <!-- PDF Preview panel -->
      <div class="preview-panel" style="overflow-y:auto">
        <template v-if="previewMounted && documento">
          <DocumentPreview
            :documento="documento"
            :selected-element-id="editorStore.selectedElementId"
          />
        </template>
        <div v-else class="preview-loading">
          <q-circular-progress indeterminate color="white" size="40px" :thickness="0.3" />
          <p class="q-mt-sm text-caption" style="color:#ccc">Carregando prévia…</p>
        </div>
      </div>

    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useEditorStore } from '@/stores/editor.js'
import { useDocumentsStore } from '@/stores/documents.js'
import { formatLabel, elementIcon, renumberElements } from '@/utils/numbering.js'
import { gerarPdf } from '@/services/pdfService.js'
import EditorSidebar from '@/components/editor/EditorSidebar.vue'
import WysiwygEditor from '@/components/editor/WysiwygEditor.vue'
import DocumentMetaPanel from '@/components/editor/DocumentMetaPanel.vue'
import DocumentPreview from '@/components/editor/DocumentPreview.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'

const route = useRoute()
const router = useRouter()
const $q = useQuasar()
const editorStore = useEditorStore()
const docStore = useDocumentsStore()

const showMeta      = ref(false)
const sidebarOpen   = ref(true)
const previewMounted = ref(false)
const pdfLoading    = ref(false)

// ── Auto-save ────────────────────────────────────────────────────────────────
const saveStatus = ref('idle')   // 'idle' | 'dirty' | 'saving'
const hasSaved   = ref(false)    // true somente após pelo menos um save bem-sucedido
let autoSaveTimer = null

function scheduleAutoSave() {
  saveStatus.value = 'dirty'
  clearTimeout(autoSaveTimer)
  autoSaveTimer = setTimeout(autoSave, 2000)
}

async function autoSave() {
  if (!editorStore.isDirty) return
  saveStatus.value = 'saving'
  try {
    await editorStore.save()
    hasSaved.value = true
    saveStatus.value = 'idle'
  } catch (e) {
    console.error('[AutoSave]', e)
    saveStatus.value = 'dirty'
  }
}

onUnmounted(() => {
  clearTimeout(autoSaveTimer)
  if (editorStore.isDirty) editorStore.save()
})
// ─────────────────────────────────────────────────────────────────────────────

const saveIndicatorClass = computed(() => {
  if (saveStatus.value === 'saving') return 'save-indicator--saving'
  if (saveStatus.value === 'dirty' || !hasSaved.value) return 'save-indicator--dirty'
  return 'save-indicator--saved'
})

const documentoId    = computed(() => route.params.id)
const documento      = computed(() => editorStore.documento)
const selectedElement = computed(() => editorStore.selectedElement)

const isReadonly = computed(() =>
  ['PUBLICADO', 'ARQUIVADO', 'CANCELADO', 'REVOGADO'].includes(documento.value?.status)
)

const docLabel = computed(() => {
  const d = documento.value
  if (!d) return 'Novo Documento'
  const numStr = [d.numero_basico, d.numero_secundario].filter(Boolean).join('-')
  const num = [d.especie, numStr].filter(Boolean).join(' ')
  return num || 'Documento sem título'
})

const breadcrumb = computed(() => {
  const el = selectedElement.value
  if (!el) return []
  const tipo = el.tipo.replace(/_/g, ' ')
  return [
    { title: 'Documento', disabled: false },
    { title: tipo.charAt(0).toUpperCase() + tipo.slice(1), disabled: true },
  ]
})

const GROUPING_TIPOS = ['capitulo', 'secao_normativa', 'subsecao_normativa']

const isGroupingEl = computed(() =>
  GROUPING_TIPOS.includes(selectedElement.value?.tipo)
)

const groupingLabel = computed(() => {
  switch (selectedElement.value?.tipo) {
    case 'capitulo':           return 'Título do Capítulo'
    case 'secao_normativa':    return 'Título da Seção'
    case 'subsecao_normativa': return 'Título da Subseção'
    default: return 'Título'
  }
})

const CHILD_OPTIONS = {
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
    { tipo: 'inciso',          label: 'Inciso' },
  ],
  paragrafo_unico: [{ tipo: 'inciso', label: 'Inciso' }],
  paragrafo:       [{ tipo: 'inciso', label: 'Inciso' }],
  inciso:          [{ tipo: 'alinea', label: 'Alínea' }],
  alinea:          [{ tipo: 'sub_alinea', label: 'Sub-alínea' }],
}

const childOptions = computed(() => {
  const el = selectedElement.value
  return el ? (CHILD_OPTIONS[el.tipo] ?? []) : []
})

function hasChildren(el) {
  return el?.filhos?.length > 0
}

onMounted(async () => {
  if (documentoId.value) {
    // Tenta buscar do backend; se falhar, cai para versão em memória (recém-criada)
    let doc = null
    try {
      doc = await docStore.fetchDocumento(documentoId.value)
    } catch (e) {
      console.error('[Editor] Erro ao buscar documento do backend:', e)
    }
    if (!doc) {
      doc = docStore.getById(documentoId.value)
    }
    if (!doc) {
      router.replace({ name: 'home' })
      return
    }
    const ok = editorStore.load(documentoId.value)
    if (!ok) {
      router.replace({ name: 'home' })
      return
    }

    if (doc._fromTemplate) {
      // Backend não tinha seções — salva o template imediatamente
      editorStore.isDirty = true
      await autoSave()
    } else {
      // Seções vieram do banco — já estão salvas
      hasSaved.value = true
    }

    const loadedDoc = editorStore.documento
    const prelim = loadedDoc?.secoes?.find(s => s.tipo === 'parte_preliminar')
    const primeiro = prelim?.elementos?.[0]
    if (primeiro) editorStore.selectElement(primeiro.id)
  } else {
    editorStore.loadNew()
  }
  await nextTick()
  previewMounted.value = true
})

async function baixarPdf() {
  if (!documento.value) return
  pdfLoading.value = true
  try {
    await gerarPdf(documento.value)
  } catch (e) {
    console.error('[PDF]', e)
    $q.notify({
      type: 'negative',
      message: `Erro ao gerar PDF: ${e?.message ?? 'erro desconhecido'}`,
      position: 'bottom-right',
      timeout: 6000,
    })
  } finally {
    pdfLoading.value = false
  }
}

function onContentUpdate(html) {
  if (selectedElement.value) {
    editorStore.updateContent(selectedElement.value.id, html)
    scheduleAutoSave()
  }
}

function onTituloUpdate(titulo) {
  if (selectedElement.value) {
    editorStore.updateTitulo(selectedElement.value.id, titulo)
    scheduleAutoSave()
  }
}

function onMetaUpdate(meta) {
  if (editorStore.documento) {
    Object.assign(editorStore.documento, meta)
    editorStore.isDirty = true
    scheduleAutoSave()
  }
}

function onReorderNormativa(newElements) {
  const secao = editorStore.normativaSecao
  if (secao) {
    secao.elementos = newElements
    renumberElements(secao.elementos)
    editorStore.isDirty = true
    scheduleAutoSave()
  }
}

function addArtigo() {
  const secao = editorStore.normativaSecao
  if (!secao) return
  const novo = {
    id: crypto.randomUUID(),
    tipo: 'artigo',
    numero: 0,
    conteudo: '<p></p>',
    filhos: [],
  }
  secao.elementos.push(novo)
  renumberElements(secao.elementos)
  editorStore.selectedElementId = novo.id
  editorStore.isDirty = true
  scheduleAutoSave()
}

function addCapitulo(titulo) {
  editorStore.addCapitulo(titulo)
  scheduleAutoSave()
}
</script>

<style scoped>
.editor-page {
  background: var(--color-background);
}
.editor-topbar {
  background: var(--color-surface);
  position: relative;
}
.meta-toggle {
  background: var(--color-surface);
  min-height: 28px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12);
}
.meta-toggle:hover {
  background: rgba(0, 0, 0, 0.04);
}
.editor-body {
  position: relative;
  flex-wrap: nowrap;
}
.sidebar-toggle-btn {
  position: absolute;
  left: 8px;
  top: 8px;
  z-index: 10;
}
.editor-content {
  flex: 1 1 0;
  min-width: 0;
  background: var(--color-background);
}
.preview-panel {
  flex: 1 1 0;
  min-width: 320px;
  border-left: 2px solid rgba(0, 0, 0, 0.12);
  background: #525659;
}
.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}
.element-editor {
  max-width: 800px;
  margin: 0 auto;
}
.element-header {
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12);
}
.save-indicator {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.78rem;
  font-weight: 500;
  white-space: nowrap;
  pointer-events: none;
  transition: background 0.2s, color 0.2s;
}
.save-indicator--saving {
  background: rgba(0, 0, 0, 0.07);
  color: #555;
}
.save-indicator--dirty {
  background: rgba(230, 81, 0, 0.12);
  color: #b45000;
}
.save-indicator--saved {
  background: rgba(46, 125, 50, 0.12);
  color: #2e7d32;
}

/* Transição do painel de metadados (substitui v-expand-transition) */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
  max-height: 400px;
}
.slide-down-enter-from,
.slide-down-leave-to {
  max-height: 0;
  opacity: 0;
}
</style>
