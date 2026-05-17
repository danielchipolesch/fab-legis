<template>
  <v-container fluid class="pa-6">

    <!-- Page header -->
    <div class="d-flex align-center justify-space-between mb-6">
      <div>
        <h1 class="text-h5 font-weight-bold text-primary">Gestão de Legislação</h1>
        <p class="text-body-2 text-medium-emphasis mb-0">
          Gerencie os atos normativos do Comando da Aeronáutica
        </p>
      </div>
      <v-btn
        color="primary"
        prepend-icon="mdi-plus"
        size="large"
        @click="dialogNovoDoc = true"
      >
        Novo Documento
      </v-btn>
    </div>

    <NewDocumentDialog v-model="dialogNovoDoc" @created="onDocumentoCriado" />

    <!-- Filters -->
    <v-card class="mb-5" color="surface-card">
      <v-card-text class="pa-4">
        <v-row dense align="center">
          <v-col cols="12" md="4">
            <v-text-field
              v-model="filtros.busca"
              prepend-inner-icon="mdi-magnify"
              label="Buscar por assunto ou número"
              hide-details
              clearable
              density="comfortable"
            />
          </v-col>
          <v-col cols="6" md="2">
            <v-select
              v-model="filtros.especie"
              :items="especies"
              label="Espécie"
              hide-details
              clearable
              density="comfortable"
            />
          </v-col>
          <v-col cols="6" md="2">
            <v-select
              v-model="filtros.status"
              :items="statusOptions"
              label="Status"
              hide-details
              clearable
              density="comfortable"
            />
          </v-col>
          <v-col cols="12" md="4" class="d-flex justify-end gap-2">
            <v-btn variant="text" prepend-icon="mdi-filter-off" @click="limparFiltros">
              Limpar
            </v-btn>
            <v-btn-toggle v-model="viewMode" mandatory color="primary" density="compact" rounded="md">
              <v-btn value="tabela" icon="mdi-view-list" />
              <v-btn value="cards" icon="mdi-view-grid" />
            </v-btn-toggle>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <!-- Summary chips -->
    <div class="d-flex flex-wrap gap-2 mb-5">
      <v-chip
        v-for="s in statusSummary"
        :key="s.status"
        :color="s.color"
        variant="tonal"
        size="small"
        label
        @click="filtros.status = filtros.status === s.status ? null : s.status"
      >
        {{ s.label }}: <strong class="ml-1">{{ s.count }}</strong>
      </v-chip>
    </div>

    <!-- TABLE VIEW -->
    <template v-if="viewMode === 'tabela'">
      <v-card v-if="store.loading">
        <v-skeleton-loader type="table-heading,table-row@8" />
      </v-card>
      <v-card v-else>
        <v-data-table
          :headers="headers"
          :items="documentosFiltrados"
          :items-per-page="15"
          hover
          color="surface"
          class="legis-table"
        >
          <template #item.especie="{ item }">
            <v-chip color="secondary" variant="tonal" size="x-small" label class="font-weight-bold">
              {{ item.especie }}
            </v-chip>
          </template>

          <template #item.numero="{ item }">
            <span class="font-weight-medium text-primary">
              {{ item.especie }} {{ item.numero_basico }}<template v-if="item.numero_secundario">-{{ item.numero_secundario }}</template>
            </span>
          </template>

          <template #item.data_criacao="{ item }">
            {{ formatarData(item.data_criacao) }}
          </template>

          <template #item.status="{ item }">
            <StatusBadge :status="item.status" />
          </template>

          <template #item.actions="{ item }">
            <div class="d-flex gap-2">

              <!-- Editar — só RASCUNHO e MINUTA -->
              <v-btn
                icon="mdi-pencil-outline"
                size="small"
                variant="text"
                color="primary"
                :disabled="!canEdit(item)"
                :to="canEdit(item) ? { name: 'documento-editar', params: { id: item.id } } : undefined"
              >
                <v-icon />
                <v-tooltip activator="parent" location="top">
                  {{ canEdit(item) ? 'Editar' : 'Edição disponível apenas para Rascunho e Minuta' }}
                </v-tooltip>
              </v-btn>

              <!-- Visualizar — sempre habilitado -->
              <v-btn
                icon="mdi-eye-outline"
                size="small"
                variant="text"
                :to="{ name: 'documento-editar', params: { id: item.id } }"
              >
                <v-icon />
                <v-tooltip activator="parent" location="top">Visualizar</v-tooltip>
              </v-btn>

              <!-- Comparar versões — habilitado se houver ao menos 1 versão salva -->
              <v-btn
                icon="mdi-source-branch"
                size="small"
                variant="text"
                :disabled="!item.versoes?.length"
                :to="item.versoes?.length ? { name: 'documento-comparar', params: { id: item.id } } : undefined"
              >
                <v-icon />
                <v-tooltip activator="parent" location="top">
                  {{ item.versoes?.length ? 'Comparar versões' : 'Sem versões anteriores para comparar' }}
                </v-tooltip>
              </v-btn>

              <!-- Clonar — sempre habilitado -->
              <v-btn
                icon="mdi-content-copy"
                size="small"
                variant="text"
                @click="clonar(item)"
              >
                <v-icon />
                <v-tooltip activator="parent" location="top">Clonar documento</v-tooltip>
              </v-btn>

              <!-- Baixar PDF — sempre habilitado -->
              <v-btn
                icon="mdi-file-pdf-box"
                size="small"
                variant="text"
                @click="baixarPdf(item)"
              >
                <v-icon />
                <v-tooltip activator="parent" location="top">Baixar PDF</v-tooltip>
              </v-btn>

              <v-menu>
                <template #activator="{ props: menuProps }">
                  <v-btn v-bind="menuProps" icon="mdi-dots-vertical" size="small" variant="text" color="grey" />
                </template>
                <v-list density="compact" min-width="200">
                  <v-list-item
                    v-for="opt in statusActions(item)"
                    :key="opt.status"
                    :prepend-icon="opt.icon"
                    :title="opt.label"
                    @click="mudarStatus(item, opt.status)"
                  />
                  <v-divider />
                  <v-list-item
                    prepend-icon="mdi-delete-outline"
                    title="Excluir"
                    class="text-error"
                    @click="confirmarExclusao(item)"
                  />
                </v-list>
              </v-menu>
            </div>
          </template>

          <template #no-data>
            <div class="d-flex flex-column align-center py-10 text-medium-emphasis">
              <v-icon size="56" class="mb-3">mdi-file-search-outline</v-icon>
              <p>Nenhum documento encontrado.</p>
            </div>
          </template>
        </v-data-table>
      </v-card>
    </template>

    <!-- CARDS VIEW -->
    <template v-else>
      <v-row v-if="store.loading">
        <v-col v-for="n in 8" :key="n" cols="12" sm="6" md="4" lg="3">
          <v-skeleton-loader type="card" />
        </v-col>
      </v-row>
      <v-row v-else>
        <v-col
          v-for="doc in documentosFiltrados"
          :key="doc.id"
          cols="12" sm="6" md="4" lg="3"
        >
          <v-card height="100%" class="d-flex flex-column">
            <v-card-item>
              <template #prepend>
                <v-avatar color="primary" variant="tonal" rounded="md" size="40">
                  <v-icon>mdi-file-document-outline</v-icon>
                </v-avatar>
              </template>
              <v-card-title class="text-subtitle-2 font-weight-bold">
                {{ doc.especie }} {{ doc.numero_basico }}<template v-if="doc.numero_secundario">-{{ doc.numero_secundario }}</template>
              </v-card-title>
              <v-card-subtitle class="text-caption">{{ formatarData(doc.data_criacao) }}</v-card-subtitle>
              <template #append>
                <StatusBadge :status="doc.status" size="x-small" />
              </template>
            </v-card-item>

            <v-card-text class="flex-grow-1">
              <p class="text-body-2 text-medium-emphasis mb-0 text-truncate-2">
                {{ doc.assunto_basico }}
              </p>
            </v-card-text>

            <v-divider />

            <v-card-actions class="pa-3 gap-1">
              <v-btn
                size="small"
                variant="text"
                color="primary"
                prepend-icon="mdi-pencil-outline"
                :disabled="!canEdit(doc)"
                :to="canEdit(doc) ? { name: 'documento-editar', params: { id: doc.id } } : undefined"
              >
                Editar
              </v-btn>
              <v-spacer />
              <v-btn size="small" icon="mdi-content-copy" variant="text" @click="clonar(doc)">
                <v-tooltip activator="parent" location="top">Clonar</v-tooltip>
              </v-btn>
              <v-btn
                size="small"
                icon="mdi-source-branch"
                variant="text"
                :disabled="!doc.versoes?.length"
                :to="doc.versoes?.length ? { name: 'documento-comparar', params: { id: doc.id } } : undefined"
              >
                <v-tooltip activator="parent" location="top">Comparar versões</v-tooltip>
              </v-btn>
              <v-btn size="small" icon="mdi-file-pdf-box" variant="text" @click="baixarPdf(doc)">
                <v-tooltip activator="parent" location="top">Baixar PDF</v-tooltip>
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-col>
        <v-col v-if="!documentosFiltrados.length" cols="12">
          <div class="d-flex flex-column align-center py-12 text-medium-emphasis">
            <v-icon size="64" class="mb-4">mdi-file-search-outline</v-icon>
            <p>Nenhum documento encontrado.</p>
          </div>
        </v-col>
      </v-row>
    </template>

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

    <!-- Confirm delete dialog -->
    <v-dialog v-model="dialog.delete" max-width="420">
      <v-card>
        <v-card-title class="text-h6 pt-5 px-6">Excluir documento?</v-card-title>
        <v-card-text class="px-6">
          Esta ação não pode ser desfeita. O documento
          <strong>{{ dialog.target?.especie }} {{ dialog.target?.numero_basico }}</strong>
          será removido permanentemente.
        </v-card-text>
        <v-card-actions class="px-6 pb-5 gap-2">
          <v-spacer />
          <v-btn variant="text" @click="dialog.delete = false">Cancelar</v-btn>
          <v-btn variant="flat" color="error" @click="excluir">Excluir</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

  </v-container>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useDocumentsStore } from '@/stores/documents.js'
import StatusBadge from '@/components/common/StatusBadge.vue'
import NewDocumentDialog from '@/components/common/NewDocumentDialog.vue'
import { gerarPdf } from '@/services/pdfService.js'
import { listar as listarEspecies } from '@/api/especiesNormativas.js'

const router = useRouter()
const store = useDocumentsStore()

onMounted(async () => {
  await store.fetchAll()
  try {
    const esp = await listarEspecies()
    especies.value = esp.map(e => e.sigla)
  } catch { /* mantém lista vazia se backend indisponível */ }
})

const dialogNovoDoc = ref(false)
const viewMode = ref('tabela')
const filtros = reactive({ busca: '', especie: null, status: null })
const showPdfError = ref(false)
const pdfErrorMsg = ref('')

function onDocumentoCriado(doc) {
  if (!doc?.id) return
  const numero = doc.numero_secundario
    ? `${doc.especie} ${doc.numero_basico}-${doc.numero_secundario}`
    : `${doc.especie} ${doc.numero_basico}`
  router.push({
    name: 'documento-editar',
    params: { id: doc.id },
    state: { sucessoCriacao: `Documento ${numero} — "${doc.titulo}" criado com sucesso!` },
  })
}

const especies = ref([])
const statusOptions = ['RASCUNHO', 'MINUTA', 'APROVADO', 'PUBLICADO', 'ARQUIVADO', 'CANCELADO', 'REVOGADO']

const headers = [
  { title: 'Espécie', key: 'especie', sortable: true, width: '100px' },
  { title: 'Número',  key: 'numero',  sortable: false, width: '160px' },
  { title: 'Assunto Básico', key: 'assunto_basico', sortable: true, width: '180px' },
  { title: 'Título',  key: 'titulo',  sortable: true },
  { title: 'Data',    key: 'data_criacao', sortable: true, width: '120px' },
  { title: 'Status',  key: 'status',  sortable: true, width: '140px' },
  { title: 'Ações',   key: 'actions', sortable: false, width: '220px', align: 'end' },
]

const documentosFiltrados = computed(() => {
  return store.documentos.filter(doc => {
    if (filtros.especie && doc.especie !== filtros.especie) return false
    if (filtros.status && doc.status !== filtros.status) return false
    if (filtros.busca) {
      const q = filtros.busca.toLowerCase()
      const match = doc.assunto_basico?.toLowerCase().includes(q)
        || doc.numero_basico?.toString().includes(q)
        || doc.especie?.toLowerCase().includes(q)
      if (!match) return false
    }
    return true
  })
})

const STATUS_CFG = {
  RASCUNHO:  { color: 'grey',          label: 'Rascunho'  },
  MINUTA:    { color: 'orange-darken-2', label: 'Minuta'   },
  APROVADO:  { color: 'success',       label: 'Aprovado'  },
  PUBLICADO: { color: 'primary',       label: 'Publicado' },
  ARQUIVADO: { color: 'blue-grey',     label: 'Arquivado' },
  CANCELADO: { color: 'error',         label: 'Cancelado' },
  REVOGADO:  { color: 'brown',         label: 'Revogado'  },
}

const statusSummary = computed(() =>
  Object.entries(STATUS_CFG).map(([status, cfg]) => ({
    status,
    label: cfg.label,
    color: cfg.color,
    count: store.documentos.filter(d => d.status === status).length,
  })).filter(s => s.count > 0)
)

function canEdit(doc) {
  return ['RASCUNHO', 'MINUTA'].includes(doc.status)
}

function statusActions(doc) {
  const transitions = {
    RASCUNHO:  [{ status: 'MINUTA',    label: 'Enviar para Minuta',  icon: 'mdi-file-edit-outline' }],
    MINUTA:    [{ status: 'APROVADO',  label: 'Aprovar',             icon: 'mdi-check-circle-outline' },
                { status: 'RASCUNHO',  label: 'Retornar p/ Rascunho', icon: 'mdi-undo' }],
    APROVADO:  [{ status: 'PUBLICADO', label: 'Publicar',            icon: 'mdi-publish' },
                { status: 'MINUTA',    label: 'Retornar p/ Minuta',  icon: 'mdi-undo' }],
    PUBLICADO: [{ status: 'ARQUIVADO', label: 'Arquivar',            icon: 'mdi-archive-outline' },
                { status: 'REVOGADO',  label: 'Revogar',             icon: 'mdi-file-remove-outline' }],
    ARQUIVADO: [],
    CANCELADO: [],
    REVOGADO:  [],
  }
  return transitions[doc.status] ?? []
}

function mudarStatus(doc, novoStatus) {
  store.changeStatus(doc.id, novoStatus)
}

async function baixarPdf(doc) {
  try {
    await gerarPdf(doc)
  } catch (e) {
    console.error('[PDF]', e)
    pdfErrorMsg.value = `Erro ao gerar PDF: ${e?.message ?? 'erro desconhecido'}`
    showPdfError.value = true
  }
}

function clonar(doc) {
  store.cloneDocumento(doc.id)
}

const dialog = reactive({ delete: false, target: null })

function confirmarExclusao(doc) {
  dialog.target = doc
  dialog.delete = true
}

function excluir() {
  if (dialog.target) store.deleteDocumento(dialog.target.id)
  dialog.delete = false
  dialog.target = null
}

function formatarData(valor) {
  if (!valor) return '—'
  const [ano, mes, dia] = valor.split('-')
  return `${dia}/${mes}/${ano}`
}

function limparFiltros() {
  filtros.busca = ''
  filtros.especie = null
  filtros.status = null
}
</script>

<style scoped>
.text-truncate-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.legis-table :deep(tr:hover td) {
  background: rgb(var(--v-theme-secondary), 0.06) !important;
}
</style>
