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

      <StatusBadge v-if="documento" :status="documento.status" />

      <v-btn
        variant="outlined"
        color="primary"
        prepend-icon="mdi-source-branch"
        size="small"
        :to="{ name: 'documento-comparar', params: { id: documentoId } }"
      >
        Comparar versões
      </v-btn>

      <v-btn
        variant="outlined"
        color="error"
        prepend-icon="mdi-file-pdf-box"
        size="small"
        :loading="pdfLoading"
        @click="baixarPdf"
      >
        PDF
      </v-btn>

      <v-btn
        color="primary"
        prepend-icon="mdi-content-save-outline"
        size="small"
        :loading="saving"
        :disabled="!editorStore.isDirty"
        @click="salvar"
      >
        Salvar
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
                <div class="d-flex gap-2">
                  <v-btn
                    v-if="!isGroupingEl && hasChildren(selectedElement)"
                    size="small"
                    variant="outlined"
                    color="secondary"
                    prepend-icon="mdi-arrow-expand-down"
                    @click="editorStore.demote(selectedElement.id)"
                  >
                    Rebaixar
                  </v-btn>
                  <v-btn
                    v-if="!isGroupingEl"
                    size="small"
                    variant="outlined"
                    color="secondary"
                    prepend-icon="mdi-arrow-collapse-up"
                    @click="editorStore.promote(selectedElement.id)"
                  >
                    Promover
                  </v-btn>
                  <v-btn
                    size="small"
                    variant="outlined"
                    color="error"
                    prepend-icon="mdi-delete-outline"
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
            <div v-if="childOptions.length && !isReadonly" class="mt-4 d-flex flex-wrap gap-2">
              <span class="text-caption text-medium-emphasis align-self-center">Adicionar:</span>
              <v-btn
                v-for="opt in childOptions"
                :key="opt.tipo"
                size="small"
                variant="tonal"
                color="primary"
                :prepend-icon="elementIcon(opt.tipo)"
                @click="editorStore.addFilho(selectedElement.id, opt.tipo)"
              >
                {{ opt.label }}
              </v-btn>
              <v-btn
                v-if="!isGroupingEl"
                size="small"
                variant="tonal"
                color="secondary"
                prepend-icon="mdi-plus"
                @click="editorStore.addSibling(selectedElement.id, selectedElement.tipo)"
              >
                Mesmo nível
              </v-btn>
            </div>

          </div>
        </template>

      </div>
      <!-- PDF Preview panel — sempre visível, carrega após o editor -->
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

    <!-- Dirty indicator snackbar -->
    <v-snackbar
      v-model="showDirtySnack"
      location="bottom right"
      color="warning"
      timeout="3000"
    >
      <v-icon start>mdi-alert-circle-outline</v-icon>
      Há alterações não salvas.
      <template #actions>
        <v-btn variant="text" @click="salvar">Salvar agora</v-btn>
      </template>
    </v-snackbar>

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
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
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
const editorStore = useEditorStore()
const docStore = useDocumentsStore()

const saving = ref(false)
const showMeta = ref(false)
const sidebarOpen = ref(true)
const previewMounted = ref(false)
const showDirtySnack = ref(false)
const pdfLoading = ref(false)
const showPdfError = ref(false)
const pdfErrorMsg = ref('')

const documentoId = computed(() => route.params.id)
const documento = computed(() => editorStore.documento)
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
    editorStore.load(documentoId.value)
  } else {
    editorStore.loadNew()
  }
  // Garante que o editor renderiza antes de montar o preview pesado
  await nextTick()
  previewMounted.value = true
})

watch(() => editorStore.isDirty, (val) => {
  if (val) showDirtySnack.value = true
})

async function salvar() {
  saving.value = true
  await new Promise(r => setTimeout(r, 300))
  editorStore.save()
  saving.value = false
}

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
  }
}

function onTituloUpdate(titulo) {
  if (selectedElement.value) {
    editorStore.updateTitulo(selectedElement.value.id, titulo)
  }
}

function onMetaUpdate(meta) {
  if (editorStore.documento) {
    Object.assign(editorStore.documento, meta)
    editorStore.isDirty = true
  }
}

function onReorderNormativa(newElements) {
  const secao = editorStore.normativaSecao
  if (secao) {
    secao.elementos = newElements
    renumberElements(secao.elementos)
    editorStore.isDirty = true
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
}

function addCapitulo(titulo) {
  editorStore.addCapitulo(titulo)
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
</style>
