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

    <!-- Abas: hoje idênticas (sem gestão de perfis/OM ainda), ver ABA_FILTROS -->
    <q-tabs
      v-model="abaAtiva"
      dense
      no-caps
      inline-label
      align="left"
      active-color="primary"
      indicator-color="primary"
      class="text-grey-7 q-mb-md"
    >
      <q-tab name="meus" icon="mdi-account-outline" label="Meus Documentos" />
      <q-tab name="minha_om" icon="mdi-office-building-outline" label="Documentos da Minha OM" />
      <q-tab name="outras_oms" icon="mdi-domain" label="Documentos de Outras OMs" />
    </q-tabs>
    <q-separator class="q-mb-lg" />

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
              label="Situação"
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
          :pagination="{ rowsPerPage: 15, sortBy: 'data_criacao', descending: true }"
          flat
          class="legis-table"
        >
          <template #body-cell-especie="props">
            <q-td :props="props">
              <q-chip color="blue-2" text-color="secondary" size="md" square class="text-weight-bold" style="font-size: 14px">
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
                  :disable="!store.temVersoesComparaveis(props.row.id)"
                  :to="store.temVersoesComparaveis(props.row.id) ? { name: 'documento-comparar', params: { id: props.row.id } } : undefined"
                >
                  <q-tooltip anchor="top middle" self="bottom middle">
                    {{ store.temVersoesComparaveis(props.row.id) ? 'Comparar versões' : 'Sem versões anteriores para comparar' }}
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
                  :loading="pdfLoading[props.row.id]"
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
                      <template v-if="canDelete(props.row)">
                        <q-separator />
                        <q-item clickable v-close-popup class="text-negative" @click="confirmarExclusao(props.row)">
                          <q-item-section avatar>
                            <q-icon name="mdi-delete-outline" color="negative" />
                          </q-item-section>
                          <q-item-section>Excluir</q-item-section>
                        </q-item>
                      </template>
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
                :disable="!store.temVersoesComparaveis(doc.id)"
                :to="store.temVersoesComparaveis(doc.id) ? { name: 'documento-comparar', params: { id: doc.id } } : undefined"
              >
                <q-tooltip anchor="top middle" self="bottom middle">
                  {{ store.temVersoesComparaveis(doc.id) ? 'Comparar versões' : 'Sem versões anteriores para comparar' }}
                </q-tooltip>
              </q-btn>
              <q-btn size="sm" icon="mdi-file-pdf-box" flat round dense color="primary" :loading="pdfLoading[doc.id]" @click="baixarPdf(doc)">
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
    <q-dialog v-model="dialog.status" :persistent="alterandoStatus">
      <q-card :style="dialog.statusOpt?.requiresRefs ? 'min-width:420px;max-width:760px;width:100%' : 'min-width:420px;max-width:500px;width:100%'">
        <q-card-section class="text-h6">{{ dialog.statusOpt?.label }}?</q-card-section>
        <q-card-section class="q-pt-none">
          O documento
          <strong>{{ dialog.target?.especie }} {{ dialog.target?.numero_basico }}<template v-if="dialog.target?.numero_secundario">-{{ dialog.target?.numero_secundario }}</template></strong>
          terá sua situação alterada para <strong>{{ dialog.statusOpt?.status }}</strong>.
        </q-card-section>
        <!-- Campos obrigatórios para republicação após alteração -->
        <template v-if="dialog.statusOpt?.requiresRefs">
          <q-separator />
          <q-card-section class="q-pt-md q-pb-sm column q-gutter-y-md">
            <div class="text-caption text-grey-7">
              Informe os dados da Portaria e do BCA que registram esta alteração:
            </div>
            <div class="row q-col-gutter-md">
              <q-input
                v-model="dialog.orgaoPortaria"
                label="Órgão *"
                outlined dense class="col-3"
                placeholder="Ex: DIRAD"
                lazy-rules
                :rules="[v => !!v?.trim() || 'Informe o órgão']"
                :disable="alterandoStatus"
              />
              <q-input
                v-model="dialog.setorPortaria"
                label="Setor(es) *"
                outlined dense class="col-3"
                placeholder="Ex: PP6"
                lazy-rules
                :rules="[v => !!v?.trim() || 'Informe o setor']"
                :disable="alterandoStatus"
              />
              <q-input
                v-model="dialog.numeroPortaria"
                label="Número *"
                outlined dense class="col-2"
                placeholder="Ex: 1.731"
                lazy-rules
                :rules="[v => !!v?.trim() || 'Obrigatório']"
                :disable="alterandoStatus"
              />
              <q-input
                v-model="dialog.dataPortaria"
                type="date"
                label="Data *"
                outlined dense class="col-4"
                lazy-rules
                :rules="[
                  v => !!v || 'Informe a data',
                  v => !dialog.target?.data_portaria_referencia || v >= dialog.target.data_portaria_referencia
                    || 'Anterior à alteração anterior',
                ]"
                :disable="alterandoStatus"
              />
            </div>
            <div class="row q-col-gutter-md">
              <q-input
                v-model="dialog.numeroBca"
                type="number" min="1" max="366"
                label="Número do BCA *"
                outlined dense class="col-4"
                lazy-rules
                :rules="[
                  v => (v !== '' && v !== null && v !== undefined) || 'Informe o número',
                  v => (v >= 1 && v <= 366) || 'Deve estar entre 1 e 366',
                ]"
                :disable="alterandoStatus"
              />
              <q-input
                v-model="dialog.dataBca"
                type="date"
                label="Data *"
                outlined dense class="col-4"
                lazy-rules
                :rules="[
                  v => !!v || 'Informe a data',
                  v => !dialog.target?.data_bca_referencia || v >= dialog.target.data_bca_referencia
                    || 'Anterior à alteração anterior',
                ]"
                :disable="alterandoStatus"
              />
            </div>
            <template v-if="dialog.statusOpt?.isRepublicacao">
              <q-separator />
              <div class="text-caption text-grey-7">Prévia da cláusula:</div>
              <div class="text-body2 text-italic">{{ previewClausula }}</div>
            </template>
          </q-card-section>
        </template>
        <q-card-actions align="right" class="q-pb-md q-px-md">
          <q-btn flat label="Cancelar" :disable="alterandoStatus" v-close-popup />
          <q-btn
            unelevated color="primary" label="Confirmar"
            :loading="alterandoStatus"
            :disable="dialog.statusOpt?.requiresRefs && errosRefs.length > 0"
            @click="executarMudancaStatus"
          />
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
          com situação <strong>RASCUNHO</strong>.
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
const abaAtiva = ref('meus')
const filtros = reactive({ busca: '', especie: null, status: null })
const pdfLoading = reactive({})

const especies = ['ICA', 'NSCA', 'Portaria', 'Resolução', 'Decreto', 'Aviso']
const statusOptions = ['RASCUNHO', 'MINUTA', 'APROVADO', 'PUBLICADO', 'EM_ALTERACAO', 'ALTERADO', 'ARQUIVADO', 'CANCELADO', 'REVOGADO']

const columns = [
  { name: 'especie',        label: 'Espécie',        field: 'especie',        align: 'center', sortable: true,  style: 'width: 100px' },
  { name: 'numero',         label: 'Número',         field: 'numero_basico',  align: 'center', sortable: false },
  { name: 'titulo',         label: 'Título',         field: 'titulo',         align: 'center', sortable: true },
  { name: 'assunto_basico', label: 'Assunto Básico', field: 'assunto_basico', align: 'center', sortable: true },
  { name: 'data_criacao',   label: 'Data',           field: 'data_criacao',   align: 'center', sortable: true,  style: 'width: 120px' },
  { name: 'status',         label: 'Situação',       field: 'status',         align: 'center', sortable: true,  style: 'width: 140px' },
  { name: 'replicas',       label: 'Réplicas',       field: 'qtd_replicas',   align: 'center', sortable: true,  style: 'width: 90px' },
  { name: 'actions',        label: 'Ações',          field: 'actions',        align: 'center', sortable: false, style: 'width: 220px' },
]

function formatarData(isoStr) {
  if (!isoStr) return '—'
  const [y, m, d] = String(isoStr).slice(0, 10).split('-')
  return `${d}/${m}/${y}`
}

// Documentos ainda não guardam autor nem OM (gestão de perfis e a feature de OMs
// não existem no sistema ainda), então as três abas são idênticas por enquanto —
// todas retornam true. Quando esses dados existirem, cada aba passa a comparar
// doc.autorId/doc.omId contra o usuário logado.
const ABA_FILTROS = {
  meus:       () => true,
  minha_om:   () => true,
  outras_oms: () => true,
}

const documentosFiltrados = computed(() => {
  const passaAba = ABA_FILTROS[abaAtiva.value] ?? (() => true)
  return store.documentos.filter(doc => {
    if (!passaAba(doc)) return false
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
  RASCUNHO:     { bg: 'grey-3',        fg: 'grey-9',          label: 'Rascunho'     },
  MINUTA:       { bg: 'orange-2',      fg: 'orange-10',       label: 'Minuta'       },
  APROVADO:     { bg: 'green-2',       fg: 'green-10',        label: 'Aprovado'     },
  PUBLICADO:    { bg: 'blue-2',        fg: 'primary',         label: 'Publicado'    },
  EM_ALTERACAO: { bg: 'deep-orange-2', fg: 'deep-orange-10',  label: 'Em Alteração' },
  ALTERADO:     { bg: 'teal-2',        fg: 'teal-10',         label: 'Alterado'     },
  ARQUIVADO:    { bg: 'blue-grey-2',   fg: 'blue-grey-10',    label: 'Arquivado'    },
  CANCELADO:    { bg: 'red-2',         fg: 'red-10',          label: 'Cancelado'    },
  REVOGADO:     { bg: 'brown-2',       fg: 'brown-10',        label: 'Revogado'     },
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
  return ['RASCUNHO', 'MINUTA', 'EM_ALTERACAO'].includes(doc.status)
}

function canDelete(doc) {
  return ['RASCUNHO', 'MINUTA'].includes(doc.status)
}

function docRoute(doc) {
  return canEdit(doc)
    ? { name: 'documento-editar',    params: { id: doc.id } }
    : { name: 'documento-visualizar', params: { id: doc.id } }
}

function statusActions(doc) {
  // ALTERADO é um status distinto de APROVADO — nunca reaproveitá-lo aqui. Um
  // documento pós-alteração jamais deve poder "Retornar p/ Minuta": ele carrega
  // numeração com sufixo de letra e elementos INCLUIDO/ALTERADO/REVOGADO que a
  // renumeração simples de MINUTA não entende e corromperia.
  const transitions = {
    RASCUNHO:     [{ status: 'MINUTA',        label: 'Enviar para Minuta',   icon: 'mdi-file-edit-outline' }],
    MINUTA:       [{ status: 'APROVADO',      label: 'Aprovar',              icon: 'mdi-check-circle-outline' },
                   { status: 'RASCUNHO',      label: 'Retornar p/ Rascunho', icon: 'mdi-undo' }],
    APROVADO:     [{ status: 'PUBLICADO',     label: 'Publicar',             icon: 'mdi-publish', requiresRefs: true },
                   { status: 'MINUTA',        label: 'Retornar p/ Minuta',   icon: 'mdi-undo' }],
    PUBLICADO:    [{ status: 'EM_ALTERACAO',  label: 'Iniciar Alteração',    icon: 'mdi-pencil-lock-outline' },
                   { status: 'ARQUIVADO',     label: 'Arquivar',             icon: 'mdi-archive-outline' },
                   { status: 'REVOGADO',      label: 'Revogar',             icon: 'mdi-file-remove-outline' }],
    EM_ALTERACAO: [{ status: 'ALTERADO', label: 'Aprovar Alteração', icon: 'mdi-check-circle-outline' }],
    ALTERADO:     [{ status: 'PUBLICADO',    label: 'Republicar',           icon: 'mdi-publish', requiresRefs: true, isRepublicacao: true },
                   { status: 'EM_ALTERACAO', label: 'Retornar p/ Alteração', icon: 'mdi-undo' }],
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

const alterandoStatus = ref(false)

async function executarMudancaStatus() {
  // Evita disparar o PATCH duas vezes num duplo-clique — a segunda requisição
  // chegaria depois da primeira já ter mudado o status no banco e seria rejeitada
  // (403), mesmo com a primeira tendo funcionado normalmente.
  if (alterandoStatus.value) return
  if (dialog.statusOpt?.requiresRefs && errosRefs.value.length) return
  const alvo = dialog.target
  const opt  = dialog.statusOpt
  const refs = opt?.requiresRefs ? {
    orgaoPortaria:  dialog.orgaoPortaria.trim(),
    setorPortaria:  dialog.setorPortaria.trim(),
    numeroPortaria: dialog.numeroPortaria.trim(),
    dataPortaria:   dialog.dataPortaria,
    numeroBca:      parseInt(dialog.numeroBca, 10),
    dataBca:        dialog.dataBca,
  } : undefined
  if (!alvo || !opt) return

  alterandoStatus.value = true
  try {
    await store.changeStatus(alvo.id, opt.status, refs)
    dialog.status = false
    dialog.target = null
    dialog.statusOpt = null
    dialog.orgaoPortaria = ''
    dialog.setorPortaria = ''
    dialog.numeroPortaria = ''
    dialog.dataPortaria = ''
    dialog.numeroBca = ''
    dialog.dataBca = ''
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao mudar situação: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    alterandoStatus.value = false
  }
}

async function baixarPdf(doc) {
  pdfLoading[doc.id] = true
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
  } finally {
    pdfLoading[doc.id] = false
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

const dialog = reactive({
  delete: false, status: false, clone: false,
  target: null, statusOpt: null,
  orgaoPortaria: '', setorPortaria: '',
  numeroPortaria: '', dataPortaria: '',
  numeroBca: '', dataBca: '',
})

const MESES_EXTENSO = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho',
  'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro']

function dataPorExtenso(isoStr) {
  if (!isoStr) return null
  const [y, m, d] = isoStr.split('-')
  return `${parseInt(d, 10)} de ${MESES_EXTENSO[parseInt(m, 10) - 1]} de ${y}`
}

// Prévia da cláusula "Portaria X/Y n° Z, de D, publica no BCA n° W, de D" — só faz
// sentido para republicação, já que é o único momento em que essa cláusula é gerada.
const previewClausula = computed(() => {
  const orgao = dialog.orgaoPortaria?.trim() || 'ÓRGÃO'
  const setor = dialog.setorPortaria?.trim() || 'SETOR'
  const numeroPortaria = dialog.numeroPortaria?.trim() || 'XYZ'
  const dataPortariaExt = dataPorExtenso(dialog.dataPortaria) || 'DD de MÊS de AAAA'
  const numeroBca = dialog.numeroBca !== '' && dialog.numeroBca != null ? dialog.numeroBca : 'ABC'
  const dataBcaExt = dataPorExtenso(dialog.dataBca) || 'DD de mês de AAAA'
  return `Portaria ${orgao}/${setor} n° ${numeroPortaria}, de ${dataPortariaExt}, `
    + `publica no BCA n° ${numeroBca}, de ${dataBcaExt}.`
})

// Espelha a validação exibida por campo (via :rules nos q-inputs) para saber se o
// formulário está completo e habilitar o botão Confirmar — não é mais renderizada
// como lista de erros, cada input mostra sua própria mensagem nativamente.
// Datas são strings ISO "YYYY-MM-DD" (tanto as do formulário quanto as vindas do
// backend), então comparação de string já basta para checar ordem cronológica.
const errosRefs = computed(() => {
  if (!dialog.statusOpt?.requiresRefs) return []
  const errs = []
  if (!dialog.orgaoPortaria?.trim()) errs.push('Informe o órgão da portaria.')
  if (!dialog.setorPortaria?.trim()) errs.push('Informe o setor da portaria.')
  if (!dialog.numeroPortaria?.trim()) errs.push('Informe o número da portaria.')
  if (!dialog.dataPortaria) errs.push('Informe a data da portaria.')

  const bcaNum = parseInt(dialog.numeroBca, 10)
  if (dialog.numeroBca === '' || isNaN(bcaNum)) {
    errs.push('Informe o número do BCA.')
  } else if (bcaNum < 1 || bcaNum > 366) {
    // O BCA é publicado apenas em dias úteis, então nunca passa de 366 (dias do ano).
    errs.push('O número do BCA deve estar entre 1 e 366.')
  }
  if (!dialog.dataBca) errs.push('Informe a data do BCA.')

  // A data de cada alteração não pode ser anterior à alteração anterior.
  if (dialog.dataPortaria && dialog.target?.data_portaria_referencia
      && dialog.dataPortaria < dialog.target.data_portaria_referencia) {
    errs.push('A data da portaria não pode ser anterior à da alteração anterior.')
  }
  if (dialog.dataBca && dialog.target?.data_bca_referencia
      && dialog.dataBca < dialog.target.data_bca_referencia) {
    errs.push('A data do BCA não pode ser anterior à da alteração anterior.')
  }

  return errs
})

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
