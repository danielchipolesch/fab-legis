<template>
  <q-page class="q-pa-xl">

    <!-- Page header -->
    <div class="row items-center justify-between q-mb-xl">
      <div>
        <h1 class="text-h5 text-weight-bold text-primary q-my-none">Gestão de Legislação</h1>
        <p class="text-body2 text-grey-7 q-mb-none">
          Gerencie os atos normativos do Comando da Aeronáutica
        </p>
      </div>
      <q-btn
        color="primary"
        unelevated
        size="lg"
        @click="dialogNovoDoc = true"
      >
        <q-icon left name="mdi-plus" />
        Novo Documento
      </q-btn>
    </div>

    <NewDocumentDialog v-model="dialogNovoDoc" />

    <!-- Filters -->
    <q-card flat bordered class="q-mb-lg">
      <q-card-section class="q-pa-md">
        <div class="row q-col-gutter-sm items-center">
          <div class="col-12 col-md-4">
            <q-input
              v-model="filtros.busca"
              label="Buscar por assunto ou número"
              outlined
              dense
              clearable
              hide-bottom-space
            >
              <template #prepend>
                <q-icon name="mdi-magnify" />
              </template>
            </q-input>
          </div>
          <div class="col-6 col-md-2">
            <q-select
              v-model="filtros.especie"
              :options="especies"
              label="Espécie"
              outlined
              dense
              clearable
              hide-bottom-space
            />
          </div>
          <div class="col-6 col-md-2">
            <q-select
              v-model="filtros.status"
              :options="statusOptions"
              label="Status"
              outlined
              dense
              clearable
              hide-bottom-space
            />
          </div>
          <div class="col-12 col-md-4 row justify-end items-center" style="gap:8px">
            <q-btn flat @click="limparFiltros">
              <q-icon left name="mdi-filter-off" />
              Limpar
            </q-btn>
            <q-btn-toggle
              v-model="viewMode"
              no-caps
              unelevated
              toggle-color="primary"
              color="grey-3"
              text-color="grey-8"
              :options="[
                { value: 'tabela', icon: 'mdi-view-list' },
                { value: 'cards', icon: 'mdi-view-grid' },
              ]"
            />
          </div>
        </div>
      </q-card-section>
    </q-card>

    <!-- Summary chips -->
    <div class="row q-gutter-sm q-mb-lg">
      <q-chip
        v-for="s in statusSummary"
        :key="s.status"
        clickable
        :color="s.bg"
        :text-color="s.fg"
        size="sm"
        square
        @click="filtros.status = filtros.status === s.status ? null : s.status"
      >
        {{ s.label }}: <strong class="q-ml-xs">{{ s.count }}</strong>
      </q-chip>
    </div>

    <!-- TABLE VIEW -->
    <template v-if="viewMode === 'tabela'">
      <q-card flat bordered>
        <q-table
          :rows="documentosFiltrados"
          :columns="columns"
          row-key="id"
          :rows-per-page-options="[15, 25, 50]"
          :pagination="{ rowsPerPage: 15 }"
          flat
          class="legis-table"
        >
          <template #body-cell-especie="props">
            <q-td :props="props">
              <q-chip color="blue-2" text-color="secondary" size="xs" square class="text-weight-bold">
                {{ props.row.especie }}
              </q-chip>
            </q-td>
          </template>

          <template #body-cell-numero="props">
            <q-td :props="props">
              <span class="text-weight-medium text-primary">
                {{ props.row.especie }} {{ props.row.numero_basico }}<template v-if="props.row.numero_secundario">-{{ props.row.numero_secundario }}</template>
              </span>
            </q-td>
          </template>

          <template #body-cell-data_criacao="props">
            <q-td :props="props">
              {{ formatarData(props.row.data_criacao) }}
            </q-td>
          </template>

          <template #body-cell-status="props">
            <q-td :props="props">
              <StatusBadge :status="props.row.status" />
            </q-td>
          </template>

          <template #body-cell-replicas="props">
            <q-td :props="props" class="text-center">
              <q-chip
                v-if="props.row.qtd_replicas > 0"
                :label="String(props.row.qtd_replicas)"
                color="indigo-2"
                text-color="indigo-10"
                size="sm"
                square
                icon="mdi-content-copy"
              />
              <span v-else class="text-grey-5 text-caption">—</span>
            </q-td>
          </template>

          <template #body-cell-actions="props">
            <q-td :props="props" class="text-center">
              <div class="row justify-center no-wrap" style="gap:4px">

                <!-- Editar — só RASCUNHO e MINUTA -->
                <q-btn
                  icon="mdi-pencil-outline"
                  size="sm"
                  flat
                  round
                  dense
                  color="primary"
                  :disable="!canEdit(props.row)"
                  :to="canEdit(props.row) ? { name: 'documento-editar', params: { id: props.row.id } } : undefined"
                >
                  <q-tooltip anchor="top middle" self="bottom middle">
                    {{ canEdit(props.row) ? 'Editar' : 'Edição disponível apenas para Rascunho e Minuta' }}
                  </q-tooltip>
                </q-btn>

                <!-- Visualizar — rota depende do status -->
                <q-btn
                  icon="mdi-eye-outline"
                  size="sm"
                  flat
                  round
                  dense
                  color="primary"
                  :to="docRoute(props.row)"
                >
                  <q-tooltip anchor="top middle" self="bottom middle">Visualizar</q-tooltip>
                </q-btn>

                <!-- Comparar versões -->
                <q-btn
                  icon="mdi-source-branch"
                  size="sm"
                  flat
                  round
                  dense
                  color="primary"
                  :disable="!props.row.versoes?.length"
                  :to="props.row.versoes?.length ? { name: 'documento-comparar', params: { id: props.row.id } } : undefined"
                >
                  <q-tooltip anchor="top middle" self="bottom middle">
                    {{ props.row.versoes?.length ? 'Comparar versões' : 'Sem versões anteriores para comparar' }}
                  </q-tooltip>
                </q-btn>

                <!-- Clonar -->
                <q-btn
                  icon="mdi-content-copy"
                  size="sm"
                  flat
                  round
                  dense
                  color="primary"
                  @click="confirmarClone(props.row)"
                >
                  <q-tooltip anchor="top middle" self="bottom middle">Clonar documento</q-tooltip>
                </q-btn>

                <!-- Baixar PDF -->
                <q-btn
                  icon="mdi-file-pdf-box"
                  size="sm"
                  flat
                  round
                  dense
                  color="primary"
                  @click="baixarPdf(props.row)"
                >
                  <q-tooltip anchor="top middle" self="bottom middle">Baixar PDF</q-tooltip>
                </q-btn>

                <q-btn icon="mdi-dots-vertical" size="sm" flat round dense color="primary">
                  <q-menu>
                    <q-list dense style="min-width:200px">
                      <q-item
                        v-for="opt in statusActions(props.row)"
                        :key="opt.status"
                        clickable
                        v-close-popup
                        @click="confirmarMudancaStatus(props.row, opt)"
                      >
                        <q-item-section avatar>
                          <q-icon :name="opt.icon" />
                        </q-item-section>
                        <q-item-section>{{ opt.label }}</q-item-section>
                      </q-item>
                      <q-separator />
                      <q-item clickable v-close-popup class="text-negative" @click="confirmarExclusao(props.row)">
                        <q-item-section avatar>
                          <q-icon name="mdi-delete-outline" color="negative" />
                        </q-item-section>
                        <q-item-section>Excluir</q-item-section>
                      </q-item>
                    </q-list>
                  </q-menu>
                </q-btn>
              </div>
            </q-td>
          </template>

          <template #no-data>
            <div class="full-width column items-center q-py-xl text-grey-7">
              <q-icon size="56px" class="q-mb-sm" name="mdi-file-search-outline" />
              <p>Nenhum documento encontrado.</p>
            </div>
          </template>
        </q-table>
      </q-card>
    </template>

    <!-- CARDS VIEW -->
    <template v-else>
      <div class="row q-col-gutter-md">
        <div
          v-for="doc in documentosFiltrados"
          :key="doc.id"
          class="col-12 col-sm-6 col-md-4 col-lg-3"
        >
          <q-card flat bordered style="height:100%" class="column">
            <q-item>
              <q-item-section avatar>
                <q-avatar color="blue-2" text-color="primary" rounded size="40px">
                  <q-icon name="mdi-file-document-outline" />
                </q-avatar>
              </q-item-section>
              <q-item-section>
                <q-item-label class="text-subtitle2 text-weight-bold">
                  {{ doc.especie }} {{ doc.numero_basico }}<template v-if="doc.numero_secundario">-{{ doc.numero_secundario }}</template>
                </q-item-label>
                <q-item-label caption>{{ formatarData(doc.data_criacao) }}</q-item-label>
              </q-item-section>
              <q-item-section side top>
                <StatusBadge :status="doc.status" size="xs" />
              </q-item-section>
            </q-item>

            <q-card-section class="col">
              <p class="text-body2 text-grey-7 q-mb-none text-truncate-2">
                {{ doc.assunto_basico }}
              </p>
            </q-card-section>

            <q-separator />

            <q-card-actions class="q-pa-sm">
              <q-btn
                v-if="canEdit(doc)"
                size="sm"
                flat
                color="primary"
                :to="{ name: 'documento-editar', params: { id: doc.id } }"
              >
                <q-icon left name="mdi-pencil-outline" />
                Editar
              </q-btn>
              <q-btn
                v-else
                size="sm"
                flat
                color="primary"
                :to="docRoute(doc)"
              >
                <q-icon left name="mdi-eye-outline" />
                Visualizar
              </q-btn>
              <q-space />
              <q-btn size="sm" icon="mdi-content-copy" flat round dense color="primary" @click="confirmarClone(doc)">
                <q-tooltip anchor="top middle" self="bottom middle">Clonar</q-tooltip>
                <q-badge v-if="doc.qtd_replicas > 0" floating color="indigo" :label="doc.qtd_replicas" />
              </q-btn>
              <q-btn
                size="sm"
                icon="mdi-source-branch"
                flat
                round
                dense
                color="primary"
                :disable="!doc.versoes?.length"
                :to="doc.versoes?.length ? { name: 'documento-comparar', params: { id: doc.id } } : undefined"
              >
                <q-tooltip anchor="top middle" self="bottom middle">Comparar versões</q-tooltip>
              </q-btn>
              <q-btn size="sm" icon="mdi-file-pdf-box" flat round dense color="primary" @click="baixarPdf(doc)">
                <q-tooltip anchor="top middle" self="bottom middle">Baixar PDF</q-tooltip>
              </q-btn>
            </q-card-actions>
          </q-card>
        </div>
        <div v-if="!documentosFiltrados.length" class="col-12">
          <div class="column items-center q-py-xl text-grey-7">
            <q-icon size="64px" class="q-mb-md" name="mdi-file-search-outline" />
            <p>Nenhum documento encontrado.</p>
          </div>
        </div>
      </div>
    </template>

    <!-- Confirm delete dialog -->
    <q-dialog v-model="dialog.delete">
      <q-card style="min-width:420px">
        <q-card-section class="text-h6">Excluir documento?</q-card-section>
        <q-card-section class="q-pt-none">
          Esta ação não pode ser desfeita. O documento
          <strong>{{ dialog.target?.especie }} {{ dialog.target?.numero_basico }}<template v-if="dialog.target?.numero_secundario">-{{ dialog.target?.numero_secundario }}</template></strong>
          será removido permanentemente.
        </q-card-section>
        <q-card-actions align="right" class="q-pb-md q-px-md">
          <q-btn flat v-close-popup>Cancelar</q-btn>
          <q-btn unelevated color="negative" @click="excluir">Excluir</q-btn>
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Confirm status change dialog -->
    <q-dialog v-model="dialog.status">
      <q-card style="min-width:420px">
        <q-card-section class="text-h6">{{ dialog.statusOpt?.label }}?</q-card-section>
        <q-card-section class="q-pt-none">
          O documento
          <strong>{{ dialog.target?.especie }} {{ dialog.target?.numero_basico }}<template v-if="dialog.target?.numero_secundario">-{{ dialog.target?.numero_secundario }}</template></strong>
          terá seu status alterado para <strong>{{ dialog.statusOpt?.status }}</strong>.
        </q-card-section>
        <q-card-actions align="right" class="q-pb-md q-px-md">
          <q-btn flat v-close-popup>Cancelar</q-btn>
          <q-btn unelevated color="primary" @click="executarMudancaStatus">Confirmar</q-btn>
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Confirm clone dialog -->
    <q-dialog v-model="dialog.clone">
      <q-card style="min-width:420px">
        <q-card-section class="text-h6">Clonar documento?</q-card-section>
        <q-card-section class="q-pt-none">
          Será criada uma cópia do documento
          <strong>{{ dialog.target?.especie }} {{ dialog.target?.numero_basico }}<template v-if="dialog.target?.numero_secundario">-{{ dialog.target?.numero_secundario }}</template></strong>
          com status <strong>RASCUNHO</strong>.
        </q-card-section>
        <q-card-actions align="right" class="q-pb-md q-px-md">
          <q-btn flat v-close-popup>Cancelar</q-btn>
          <q-btn unelevated color="primary" @click="executarClone">Clonar</q-btn>
        </q-card-actions>
      </q-card>
    </q-dialog>

  </q-page>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useDocumentsStore } from '@/stores/documents.js'
import StatusBadge from '@/components/common/StatusBadge.vue'
import NewDocumentDialog from '@/components/common/NewDocumentDialog.vue'
import { gerarPdf } from '@/services/pdfService.js'

const $q = useQuasar()
const store = useDocumentsStore()

onMounted(() => store.fetchAll())

const dialogNovoDoc = ref(false)
const viewMode = ref('tabela')
const filtros = reactive({ busca: '', especie: null, status: null })

const especies = ['ICA', 'NSCA', 'Portaria', 'Resolução', 'Decreto', 'Aviso']
const statusOptions = ['RASCUNHO', 'MINUTA', 'APROVADO', 'PUBLICADO', 'ARQUIVADO', 'CANCELADO', 'REVOGADO']

const columns = [
  { name: 'especie',        label: 'Espécie',        field: 'especie',        align: 'center', sortable: true,  style: 'width: 100px' },
  { name: 'numero',         label: 'Número',         field: 'numero_basico',  align: 'center', sortable: false },
  { name: 'titulo',         label: 'Título',         field: 'titulo',         align: 'center', sortable: true },
  { name: 'assunto_basico', label: 'Assunto Básico', field: 'assunto_basico', align: 'center', sortable: true },
  { name: 'data_criacao',   label: 'Data',           field: 'data_criacao',   align: 'center', sortable: true,  style: 'width: 120px' },
  { name: 'status',         label: 'Status',         field: 'status',         align: 'center', sortable: true,  style: 'width: 140px' },
  { name: 'replicas',       label: 'Réplicas',       field: 'qtd_replicas',   align: 'center', sortable: true,  style: 'width: 90px' },
  { name: 'actions',        label: 'Ações',          field: 'actions',        align: 'center', sortable: false, style: 'width: 220px' },
]

function formatarData(isoStr) {
  if (!isoStr) return '—'
  const [y, m, d] = String(isoStr).slice(0, 10).split('-')
  return `${d}/${m}/${y}`
}

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
  RASCUNHO:  { bg: 'grey-3',      fg: 'grey-9',       label: 'Rascunho'  },
  MINUTA:    { bg: 'orange-2',    fg: 'orange-10',    label: 'Minuta'    },
  APROVADO:  { bg: 'green-2',     fg: 'green-10',     label: 'Aprovado'  },
  PUBLICADO: { bg: 'blue-2',      fg: 'primary',      label: 'Publicado' },
  ARQUIVADO: { bg: 'blue-grey-2', fg: 'blue-grey-10', label: 'Arquivado' },
  CANCELADO: { bg: 'red-2',       fg: 'red-10',       label: 'Cancelado' },
  REVOGADO:  { bg: 'brown-2',     fg: 'brown-10',     label: 'Revogado'  },
}

const statusSummary = computed(() =>
  Object.entries(STATUS_CFG).map(([status, cfg]) => ({
    status,
    label: cfg.label,
    bg: cfg.bg,
    fg: cfg.fg,
    count: store.documentos.filter(d => d.status === status).length,
  })).filter(s => s.count > 0)
)

function canEdit(doc) {
  return ['RASCUNHO', 'MINUTA'].includes(doc.status)
}

function docRoute(doc) {
  return canEdit(doc)
    ? { name: 'documento-editar',    params: { id: doc.id } }
    : { name: 'documento-visualizar', params: { id: doc.id } }
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

function confirmarMudancaStatus(doc, opt) {
  dialog.target = doc
  dialog.statusOpt = opt
  dialog.status = true
}

function executarMudancaStatus() {
  if (dialog.target && dialog.statusOpt) store.changeStatus(dialog.target.id, dialog.statusOpt.status)
  dialog.status = false
  dialog.target = null
  dialog.statusOpt = null
}

async function baixarPdf(doc) {
  try {
    await gerarPdf(doc)
  } catch (e) {
    console.error('[PDF]', e)
    $q.notify({
      type: 'negative',
      message: `Erro ao gerar PDF: ${e?.message ?? 'erro desconhecido'}`,
      position: 'bottom-right',
      timeout: 6000,
    })
  }
}

function confirmarClone(doc) {
  dialog.target = doc
  dialog.clone = true
}

function executarClone() {
  if (dialog.target) store.cloneDocumento(dialog.target.id)
  dialog.clone = false
  dialog.target = null
}

const dialog = reactive({ delete: false, status: false, clone: false, target: null, statusOpt: null })

function confirmarExclusao(doc) {
  dialog.target = doc
  dialog.delete = true
}

function excluir() {
  if (dialog.target) store.deleteDocumento(dialog.target.id)
  dialog.delete = false
  dialog.target = null
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
.legis-table :deep(thead th) {
  text-align: center !important;
  position: relative;
}
.legis-table :deep(thead th .q-table__sort-icon) {
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%);
}
.legis-table :deep(tbody tr:hover td) {
  background: rgba(74, 111, 165, 0.06) !important;
}
</style>
