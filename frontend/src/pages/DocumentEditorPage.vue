<template>
  <div class="editor-page d-flex flex-column" style="height:calc(100vh - 60px)">

    <!-- Top action bar -->
    <div class="editor-topbar px-4 py-2 d-flex align-center gap-3">
      <v-btn
        :to="{ name: 'home' }"
        icon="mdi-arrow-left"
        variant="text"
        size="small"
      />

      <div class="flex-grow-1">
        <div class="text-subtitle-2 font-weight-bold text-primary">
          {{ docLabel }}
        </div>
        <div class="text-caption text-medium-emphasis">
          {{ documento?.assunto_basico }}
        </div>
      </div>

      <!-- Auto-save status indicator — centralizado na barra -->
      <div class="save-indicator d-flex align-center gap-1">
        <template v-if="saveStatus === 'saving'">
          <v-progress-circular size="14" width="2" indeterminate color="primary" />
          <span class="text-caption text-medium-emphasis">Salvando…</span>
        </template>
        <template v-else-if="saveStatus === 'dirty'">
          <v-icon size="14" color="warning">mdi-circle-medium</v-icon>
          <span class="text-caption text-medium-emphasis">Não salvo</span>
        </template>
        <template v-else>
          <v-icon size="16" color="success">mdi-check-circle-outline</v-icon>
          <span class="text-caption text-success">Salvo</span>
        </template>
      </div>

      <div class="flex-grow-1" />

      <StatusBadge v-if="documento" :status="documento.status" />

      <v-btn
        variant="outlined"
        color="primary"
        prepend-icon="mdi-source-branch"
        size="small"
        class="ms-2"
        :to="{ name: 'documento-comparar', params: { id: documentoId } }"
      >
        Comparar versões
      </v-btn>

      <v-btn
        variant="outlined"
        prepend-icon="mdi-file-pdf-box"
        size="small"
        class="ms-2"
        :loading="pdfLoading"
        @click="baixarPdf"
      >
        PDF
      </v-btn>
    </div>

    <v-divider />

    <!-- Metadata panel (collapsible) -->
    <v-expand-transition>
      <div v-show="showMeta" class="px-4 py-3">
        <DocumentMetaPanel
          v-if="documento"
          :documento="documento"
          @update="onMetaUpdate"
        />
      </div>
    </v-expand-transition>

    <div
      class="meta-toggle d-flex align-center justify-center px-4"
      style="cursor:pointer;user-select:none"
      @click="showMeta = !showMeta"
    >
      <v-icon size="16" :icon="showMeta ? 'mdi-chevron-up' : 'mdi-chevron-down'" class="mr-1" />
      <span class="text-caption text-medium-emphasis">
        {{ showMeta ? 'Ocultar metadados' : 'Mostrar metadados' }}
      </span>
    </div>

    <v-divider />

    <!-- Main editor area -->
    <div class="editor-body d-flex flex-grow-1 overflow-hidden">

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
      <v-btn
        v-if="!sidebarOpen"
        icon="mdi-menu"
        size="small"
        variant="tonal"
        color="primary"
        class="sidebar-toggle-btn"
        @click="sidebarOpen = true"
      />

      <!-- Content area -->
      <div class="editor-content pa-6 overflow-y-auto">

        <!-- No element selected -->
        <div v-if="!editorStore.selectedElementId" class="d-flex flex-column align-center justify-center h-100 text-medium-emphasis">
          <v-icon size="80" class="mb-4" color="blue-grey-lighten-3">mdi-cursor-pointer</v-icon>
          <p class="text-h6">Selecione um elemento no painel esquerdo</p>
          <p class="text-body-2">Clique em qualquer seção ou artigo para editá-lo aqui.</p>
        </div>

        <!-- Element editor -->
        <template v-else-if="selectedElement">
          <div class="element-editor">

            <!-- Element breadcrumb/label -->
            <div class="element-header mb-4">
              <v-breadcrumbs :items="breadcrumb" density="compact" class="pa-0" />
              <div class="d-flex align-center justify-space-between mt-2">
                <div class="d-flex align-center gap-2">
                  <v-icon :icon="elementIcon(selectedElement.tipo)" color="primary" size="20" />
                  <h2 class="text-h6 font-weight-bold text-primary mb-0">
                    {{ formatLabel(selectedElement) }}
                  </h2>
                </div>
                <div class="d-flex">
                  <v-btn
                    v-if="!isGroupingEl && hasChildren(selectedElement)"
                    size="small"
                    variant="outlined"
                    prepend-icon="mdi-arrow-expand-down"
                    class="ms-2"
                    @click="editorStore.demote(selectedElement.id)"
                  >
                    Rebaixar
                  </v-btn>
                  <v-btn
                    v-if="!isGroupingEl"
                    size="small"
                    variant="outlined"
                    prepend-icon="mdi-arrow-collapse-up"
                    class="ms-2"
                    @click="editorStore.promote(selectedElement.id)"
                  >
                    Promover
                  </v-btn>
                  <v-btn
                    size="small"
                    variant="outlined"
                    color="error"
                    prepend-icon="mdi-delete-outline"
                    class="ms-2"
                    @click="editorStore.removeElement(selectedElement.id)"
                  >
                    Remover
                  </v-btn>
                </div>
              </div>
            </div>

            <!-- Título editor para capítulo / seção / subseção -->
            <template v-if="isGroupingEl">
              <v-text-field
                :model-value="selectedElement.titulo"
                :label="groupingLabel"
                variant="outlined"
                density="comfortable"
                :readonly="isReadonly"
                @update:model-value="onTituloUpdate"
              />
              <p class="text-caption text-medium-emphasis mt-1">
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
            <div v-if="childOptions.length && !isReadonly" class="mt-4 d-flex flex-wrap align-center">
              <span class="text-caption text-medium-emphasis">Adicionar:</span>
              <v-btn
                v-for="opt in childOptions"
                :key="opt.tipo"
                size="small"
                variant="outlined"
                color="primary"
                :prepend-icon="elementIcon(opt.tipo)"
                class="ms-2"
                @click="editorStore.addFilho(selectedElement.id, opt.tipo)"
              >
                {{ opt.label }}
              </v-btn>
              <v-btn
                v-if="!isGroupingEl"
                size="small"
                variant="outlined"
                prepend-icon="mdi-plus"
                class="ms-2"
                @click="editorStore.addSibling(selectedElement.id, selectedElement.tipo)"
              >
                Mesmo nível
              </v-btn>
            </div>

          </div>
        </template>

      </div>

      <!-- PDF Preview panel -->
      <div class="preview-panel overflow-y-auto">
        <template v-if="previewMounted && documento">
          <DocumentPreview
            :documento="documento"
            :selected-element-id="editorStore.selectedElementId"
          />
        </template>
        <div v-else class="preview-loading">
          <v-progress-circular indeterminate color="white" size="40" />
          <p class="mt-3 text-caption" style="color:#ccc">Carregando prévia…</p>
        </div>
      </div>

    </div>

    <!-- PDF error snackbar -->
    <v-snackbar
      v-model="showPdfError"
      location="bottom right"
      color="error"
      timeout="6000"
    >
      <v-icon start>mdi-alert-circle-outline</v-icon>
      {{ pdfErrorMsg }}
    </v-snackbar>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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
const editorStore = useEditorStore()
const docStore = useDocumentsStore()

const showMeta      = ref(false)
const sidebarOpen   = ref(true)
const previewMounted = ref(false)
const pdfLoading    = ref(false)
const showPdfError  = ref(false)
const pdfErrorMsg   = ref('')

// ── Auto-save ────────────────────────────────────────────────────────────────
// Estados: 'idle' | 'dirty' | 'saving' | 'saved'
const saveStatus = ref('idle')
let autoSaveTimer = null

function scheduleAutoSave() {
  saveStatus.value = 'dirty'
  clearTimeout(autoSaveTimer)
  autoSaveTimer = setTimeout(autoSave, 2000)
}

async function autoSave() {
  if (!editorStore.isDirty) return
  saveStatus.value = 'saving'
  await new Promise(r => setTimeout(r, 350))
  editorStore.save()
  saveStatus.value = 'idle'
}

onUnmounted(() => {
  clearTimeout(autoSaveTimer)
  if (editorStore.isDirty) editorStore.save()
})
// ─────────────────────────────────────────────────────────────────────────────

const documentoId    = computed(() => route.params.id)
const documento      = computed(() => editorStore.documento)
const selectedElement = computed(() => editorStore.selectedElement)

const isReadonly = computed(() =>
  ['PUBLICADO', 'ARQUIVADO', 'CANCELADO', 'REVOGADO'].includes(documento.value?.status)
)

const docLabel = computed(() => {
  const d = documento.value
  if (!d) return 'Novo Documento'
  const num = [d.especie, d.numero_basico, d.numero_secundario].filter(Boolean).join(' ')
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
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 1e3a004 (Correção de bugs)
=======
>>>>>>> 1e3a00414085c7fbd6b74578b3e5b54391f7d9fd
    let ok = editorStore.load(documentoId.value)
    if (!ok) {
      // Document not in store yet — fetch from API (direct URL access or page refresh)
      const doc = await docStore.fetchDocumento(documentoId.value)
      if (!doc) {
        router.replace({ name: 'home' })
        return
      }
      ok = editorStore.load(documentoId.value)
    }
<<<<<<< HEAD
<<<<<<< HEAD
=======
    const ok = await editorStore.load(documentoId.value)
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
=======
    const ok = editorStore.load(documentoId.value)
>>>>>>> ffd8177 (Uso do HATEOAS)
=======
>>>>>>> 1e3a004 (Correção de bugs)
=======
>>>>>>> 1e3a00414085c7fbd6b74578b3e5b54391f7d9fd
    if (!ok) {
      router.replace({ name: 'home' })
      return
    }
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
    // Auto-seleciona o primeiro elemento da parte preliminar (Epígrafe)
>>>>>>> ffd8177 (Uso do HATEOAS)
=======
>>>>>>> 1e3a004 (Correção de bugs)
=======
>>>>>>> 1e3a00414085c7fbd6b74578b3e5b54391f7d9fd
    const doc = editorStore.documento
    const prelim = doc?.secoes?.find(s => s.tipo === 'parte_preliminar')
    const primeiro = prelim?.elementos?.[0]
    if (primeiro) editorStore.selectElement(primeiro.id)
  } else {
    editorStore.loadNew()
<<<<<<< HEAD
=======
>>>>>>> 95ae163 (Remove mock: conecta frontend ao backend via SRP por contexto de controller)
=======
>>>>>>> ffd8177 (Uso do HATEOAS)
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
    pdfErrorMsg.value = `Erro ao gerar PDF: ${e?.message ?? 'erro desconhecido'}`
    showPdfError.value = true
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
  background: rgb(var(--v-theme-background));
}
.editor-topbar {
  background: rgb(var(--v-theme-surface));
  min-height: 52px;
}
.meta-toggle {
  background: rgb(var(--v-theme-surface));
  min-height: 28px;
  border-bottom: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
}
.meta-toggle:hover {
  background: rgba(var(--v-theme-secondary), 0.05);
}
.editor-body {
  position: relative;
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
  background: rgb(var(--v-theme-background));
}
.preview-panel {
  flex: 1 1 0;
  min-width: 320px;
  border-left: 2px solid rgba(var(--v-border-color), var(--v-border-opacity));
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
  border-bottom: 2px solid rgba(var(--v-theme-primary), 0.12);
}
.save-indicator {
  min-width: 96px;
  justify-content: center;
}
</style>
