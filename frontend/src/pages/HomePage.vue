<template>
  <q-page class="q-pa-xl">

    <!-- Page header -->
    <div class="row items-center justify-between q-mb-xl">
      <div>
        <h1 class="text-h5 text-weight-bold text-primary q-my-none">Gestão de Legislação</h1>
        <p class="text-body2 text-grey-7 q-mb-none">
          Gestão e acompanhamento dos atos normativos do Comando da Aeronáutica
        </p>
      </div>
      <q-btn
        v-if="auth.isEditor"
        color="primary"
        unelevated
        size="lg"
        @click="dialogNovoDoc = true"
      >
        <q-icon left name="mdi-plus" />
        Novo Documento
      </q-btn>
    </div>

    <NovoDocumentoDialog v-model="dialogNovoDoc" />

    <!-- Abas (ownership) + filtros/resumo — tudo dentro do MESMO card de propósito:
         os filtros e as chips abaixo operam só sobre a aba selecionada acima, nunca
         sobre o acervo inteiro, e agrupar visualmente sem nenhum espaço/separador
         entre as duas coisas deixa essa relação óbvia (busca paginada no backend, ver
         carregar()/DocumentoSpecifications.aba). -->
    <q-card flat bordered class="q-mb-lg">
      <q-tabs
        v-model="abaAtiva"
        dense
        no-caps
        inline-label
        align="left"
        active-color="primary"
        indicator-color="primary"
        class="text-grey-7"
      >
        <q-tab name="meus" icon="mdi-account-outline">
          <div class="row items-center no-wrap" style="gap:6px">
            <span>Meus Documentos</span>
            <q-badge rounded color="primary">{{ store.resumoAbas.meus }}</q-badge>
          </div>
        </q-tab>
        <q-tab name="minha_om" icon="mdi-office-building-outline">
          <div class="row items-center no-wrap" style="gap:6px">
            <span>Documentos da Minha OM</span>
            <q-badge rounded color="primary">{{ store.resumoAbas.minha_om }}</q-badge>
          </div>
        </q-tab>
        <q-tab name="outras_oms" icon="mdi-domain">
          <div class="row items-center no-wrap" style="gap:6px">
            <span>Documentos de Outras OMs</span>
            <q-badge rounded color="primary">{{ store.resumoAbas.outras_oms }}</q-badge>
          </div>
        </q-tab>
        <q-tab name="revogados" icon="mdi-file-remove-outline">
          <div class="row items-center no-wrap" style="gap:6px">
            <span>Documentos Revogados</span>
            <q-badge rounded color="primary">{{ store.resumoAbas.revogados }}</q-badge>
          </div>
        </q-tab>
      </q-tabs>

      <q-separator />

      <!-- Filters -->
      <q-card-section class="q-pa-md">
        <div class="text-caption text-grey-6 q-mb-sm">
          <q-icon name="mdi-information-outline" size="14px" class="q-mr-xs" />
          Busca, filtros e resumo abaixo consideram apenas a aba
          <strong>{{ abaAtivaLabel }}</strong>, selecionada acima.
        </div>
        <div class="row q-col-gutter-sm items-center">
          <div class="col-12 col-md-4">
            <q-input
              v-model="filtros.busca"
              label="Buscar nesta aba, por assunto ou número"
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

        <!-- Summary chips -->
        <div class="row q-gutter-sm q-mt-md">
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
      </q-card-section>
    </q-card>

    <!-- TABLE VIEW -->
    <template v-if="viewMode === 'tabela'">
      <q-card flat bordered>
        <q-table
          :rows="store.documentos"
          :columns="columns"
          row-key="id"
          :loading="store.loading"
          :rows-per-page-options="[15, 25, 50]"
          v-model:pagination="tablePagination"
          @request="onRequest"
          flat
          class="legis-table"
        >
          <template #body-cell-especie="props">
            <q-td :props="props">
              <q-chip color="blue-2" text-color="secondary" size="sm" square class="text-weight-bold">
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
          v-for="doc in store.documentos"
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
        <div v-if="!store.documentos.length" class="col-12">
          <div class="column items-center q-py-xl text-grey-7">
            <q-icon size="64px" class="q-mb-md" name="mdi-file-search-outline" />
            <p>Nenhum documento encontrado.</p>
          </div>
        </div>
      </div>

      <!-- Paginação própria -- a q-table cuida disso sozinha (@request), mas o modo
           cartões não usa q-table, então precisa do próprio controle pra navegar pelas
           páginas que agora vêm do servidor (antes, o array inteiro já filtrado vinha
           de uma vez, sem precisar de paginação aqui). -->
      <div v-if="totalPaginas > 1" class="row justify-center q-mt-lg">
        <q-pagination
          v-model="tablePagination.page"
          :max="totalPaginas"
          direction-links
          boundary-links
          @update:model-value="carregar"
        />
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

    <!-- Confirm status change dialog -- só para transições sem escolha de pessoa
         (Enviar para Minuta, Iniciar Alteração). Enviar para Revisão/Revogação abre
         SelecionarPessoaDialog abaixo; publicar/revogar de fato (com portaria/BCA)
         mudou para PublicacaoPage.vue, que é quem tem a atribuição pra isso. -->
    <q-dialog v-model="dialog.status" :persistent="alterandoStatus">
      <q-card style="min-width:420px;max-width:500px;width:100%">
        <q-card-section class="text-h6">{{ dialog.statusOpt?.label }}?</q-card-section>
        <q-card-section class="q-pt-none">
          O documento
          <strong>{{ dialog.target?.especie }} {{ dialog.target?.numero_basico }}<template v-if="dialog.target?.numero_secundario">-{{ dialog.target?.numero_secundario }}</template></strong>
          terá sua situação alterada para <strong>{{ dialog.statusOpt?.status }}</strong>.
        </q-card-section>
        <q-card-actions align="right" class="q-pb-md q-px-md">
          <q-btn flat label="Cancelar" :disable="alterandoStatus" v-close-popup />
          <q-btn
            unelevated color="primary" label="Confirmar"
            :loading="alterandoStatus"
            @click="executarMudancaStatus"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Enviar para revisão/revogação: exige escolher a pessoa (papel APROV) --
         ver SelecionarPessoaDialog.vue. -->
    <SelecionarPessoaDialog
      v-model="dialog.pessoa"
      papel="APROV"
      :titulo="dialog.statusOpt?.label ?? ''"
      :descricao="dialog.target ? `Documento ${dialog.target.especie} ${dialog.target.numero_basico}${dialog.target.numero_secundario ? '-' + dialog.target.numero_secundario : ''}` : ''"
      acao-label="Enviar"
      :enviando="alterandoStatus"
      @confirmar="executarEnvioPessoa"
    />

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
import { ref, computed, reactive, onMounted, watch } from 'vue'
import { useQuasar } from 'quasar'
import { useDocumentosStore } from '@/stores/documentos.js'
import { useAuthStore } from '@/stores/auth.js'
import StatusBadge from '@/components/common/StatusBadge.vue'
import NovoDocumentoDialog from '@/components/common/NovoDocumentoDialog.vue'
import SelecionarPessoaDialog from '@/components/editor/SelecionarPessoaDialog.vue'
import { gerarPdf } from '@/services/pdfService.js'

const $q = useQuasar()
const store = useDocumentosStore()
const auth = useAuthStore()

const dialogNovoDoc = ref(false)
const viewMode = ref('tabela')
const abaAtiva = ref('meus')
const filtros = reactive({ busca: '', especie: null, status: null })
const pdfLoading = reactive({})

const especies = ['ICA', 'NSCA', 'Portaria', 'Resolução', 'Decreto', 'Aviso']
const statusOptions = [
  'RASCUNHO', 'MINUTA', 'EM_REVISAO', 'APROVADO', 'EM_PUBLICACAO', 'PUBLICADO',
  'EM_ALTERACAO', 'ALTERADO', 'ANALISE_REVOGACAO', 'EM_REVOGACAO', 'CANCELADO', 'REVOGADO',
]

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

// Nome da coluna (frontend, snake_case) -> propriedade Java que o backend ordena (ver
// DocumentoController.getAll) -- os dois lados usam nomenclaturas diferentes de
// propósito (ver convenção do projeto), então a ordenação por servidor precisa dessa
// tradução explícita.
const SORT_FIELD_MAP = {
  especie: 'especieNormativa.sigla',
  titulo: 'tituloDocumento',
  assunto_basico: 'assuntoBasico.nome',
  data_criacao: 'dtCriacao',
  status: 'documentoStatus',
  replicas: 'qtdReplicas',
}

function formatarData(isoStr) {
  if (!isoStr) return '—'
  const [y, m, d] = String(isoStr).slice(0, 10).split('-')
  return `${d}/${m}/${y}`
}

const ABA_LABELS = {
  meus: 'Meus Documentos',
  minha_om: 'Documentos da Minha OM',
  outras_oms: 'Documentos de Outras OMs',
  revogados: 'Documentos Revogados',
}
const abaAtivaLabel = computed(() => ABA_LABELS[abaAtiva.value])

// Paginação real no backend (ver DocumentoController.getAll/DocumentoService --
// antes disso, um único fetch de até 200 documentos vinha pro navegador, e aba, busca,
// espécie/situação e a própria paginação da tabela eram calculadas em JS por cima desse
// array fixo -- acima de 200 documentos no acervo o resto simplesmente não aparecia).
const totalPaginas = computed(() => Math.max(1, Math.ceil(store.totalElements / tablePagination.value.rowsPerPage)))

async function carregar() {
  const params = {
    aba: abaAtiva.value,
    busca: filtros.busca || undefined,
    especieSigla: filtros.especie || undefined,
    status: filtros.status || undefined,
    page: tablePagination.value.page - 1,
    size: tablePagination.value.rowsPerPage,
    sortBy: SORT_FIELD_MAP[tablePagination.value.sortBy] ?? 'dtCriacao',
    descending: tablePagination.value.descending,
  }
  await Promise.all([
    store.fetchPagina(params),
    store.fetchResumo({ aba: params.aba, busca: params.busca, especieSigla: params.especieSigla }),
  ])
}

// Disparado pela q-table (clique de página/ordenação/linhas-por-página) -- a própria
// tabela já atualiza tablePagination via v-model antes de chamar isso (padrão Quasar de
// paginação por servidor, mesmo usado em AuditoriaPage.vue).
function onRequest(props) {
  tablePagination.value.page = props.pagination.page
  tablePagination.value.rowsPerPage = props.pagination.rowsPerPage
  tablePagination.value.sortBy = props.pagination.sortBy
  tablePagination.value.descending = props.pagination.descending
  carregar()
}

const tablePagination = ref({ page: 1, rowsPerPage: 15, sortBy: 'data_criacao', descending: true, rowsNumber: 0 })
watch(() => store.totalElements, (v) => { tablePagination.value.rowsNumber = v })

// Trocar de aba/espécie/situação busca de novo na hora; busca por texto livre tem um
// debounce curto (a q-table não dispara @request por digitação, então sem isso cada
// tecla viraria uma requisição).
let buscaTimer = null
watch(() => filtros.busca, () => {
  clearTimeout(buscaTimer)
  buscaTimer = setTimeout(() => { tablePagination.value.page = 1; carregar() }, 350)
})
watch([abaAtiva, () => filtros.especie, () => filtros.status], () => {
  tablePagination.value.page = 1
  carregar()
})

onMounted(() => carregar())

const STATUS_CFG = {
  RASCUNHO:          { bg: 'grey-3',        fg: 'grey-9',         label: 'Rascunho'             },
  MINUTA:            { bg: 'orange-2',      fg: 'orange-10',      label: 'Minuta'                },
  EM_REVISAO:        { bg: 'orange-2',      fg: 'orange-10',      label: 'Em Revisão'            },
  APROVADO:          { bg: 'green-2',       fg: 'green-10',       label: 'Aprovado'              },
  EM_PUBLICACAO:     { bg: 'blue-2',        fg: 'primary',        label: 'Em Publicação'         },
  PUBLICADO:         { bg: 'blue-2',        fg: 'primary',        label: 'Publicado'             },
  EM_ALTERACAO:      { bg: 'deep-orange-2', fg: 'deep-orange-10', label: 'Em Alteração'          },
  ALTERADO:          { bg: 'teal-2',        fg: 'teal-10',        label: 'Alterado'              },
  ANALISE_REVOGACAO: { bg: 'brown-2',       fg: 'brown-10',       label: 'Análise de Revogação'  },
  EM_REVOGACAO:      { bg: 'brown-2',       fg: 'brown-10',       label: 'Em Revogação'          },
  CANCELADO:         { bg: 'red-2',         fg: 'red-10',         label: 'Cancelado'             },
  REVOGADO:          { bg: 'brown-2',       fg: 'brown-10',       label: 'Revogado'              },
}

// store.resumoStatus já vem do servidor com aba/busca/espécie aplicados (ver
// DocumentoService.getResumo) -- o número no chip bate com o que aparece na tabela ao
// clicar nele, mesma garantia de antes, só que calculada no backend agora.
const statusSummary = computed(() =>
  Object.entries(STATUS_CFG).map(([status, cfg]) => ({
    status,
    label: cfg.label,
    bg: cfg.bg,
    fg: cfg.fg,
    count: store.resumoStatus[status] ?? 0,
  })).filter(s => s.count > 0)
)

function canEdit(doc) {
  if (doc.status === 'EM_REVISAO') return doc.revisor_atribuido_id === String(auth.usuario?.id)
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

// Só as ações que o Editor conduz sozinho (sem escolher pessoa) ou a única
// exceção sem atribuição prévia (Iniciar Alteração, papel APROV da própria OM --
// ver DocumentoAcessoService.podeMudarStatus). Revisar/aprovar/publicar/revogar
// de fato viraram telas dedicadas (RevisaoPage.vue/PublicacaoPage.vue), cada
// uma restrita a quem tem a atribuição pessoal daquela etapa -- por isso não
// aparecem mais aqui.
function statusActions(doc) {
  const transitions = {
    RASCUNHO: auth.isEditor
      ? [{ status: 'MINUTA', label: 'Enviar para Minuta', icon: 'mdi-file-edit-outline' }]
      : [],
    MINUTA: auth.isEditor
      ? [{ status: 'EM_REVISAO', label: 'Enviar para Revisão', icon: 'mdi-account-arrow-right-outline', escolherPessoa: true }]
      : [],
    EM_ALTERACAO: auth.isEditor
      ? [{ status: 'EM_REVISAO', label: 'Enviar Alteração para Revisão', icon: 'mdi-account-arrow-right-outline', escolherPessoa: true }]
      : [],
    PUBLICADO: [
      ...(auth.isAprovador ? [{ status: 'EM_ALTERACAO', label: 'Iniciar Alteração', icon: 'mdi-pencil-lock-outline' }] : []),
      ...(auth.isEditor ? [{ status: 'ANALISE_REVOGACAO', label: 'Enviar para Revogação', icon: 'mdi-file-remove-outline', escolherPessoa: true }] : []),
    ],
  }
  return transitions[doc.status] ?? []
}

function confirmarMudancaStatus(doc, opt) {
  dialog.target = doc
  dialog.statusOpt = opt
  if (opt.escolherPessoa) {
    dialog.pessoa = true
  } else {
    dialog.status = true
  }
}

const alterandoStatus = ref(false)

async function executarMudancaStatus() {
  // Evita disparar o PATCH duas vezes num duplo-clique — a segunda requisição
  // chegaria depois da primeira já ter mudado o status no banco e seria rejeitada
  // (403), mesmo com a primeira tendo funcionado normalmente.
  if (alterandoStatus.value) return
  const alvo = dialog.target
  const opt  = dialog.statusOpt
  if (!alvo || !opt) return

  alterandoStatus.value = true
  try {
    await store.changeStatus(alvo.id, opt.status)
    await fecharDialogStatus()
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao mudar situação: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    alterandoStatus.value = false
  }
}

// Enviar para revisão/revogação -- mesma mudança de status acima, só que com a
// pessoa escolhida no SelecionarPessoaDialog (sempre revisorId aqui: as duas
// transições que passam por esse diálogo, EM_REVISAO e ANALISE_REVOGACAO, são de
// atribuir um revisor -- ver DocumentoStatusRequestDto).
async function executarEnvioPessoa(usuarioId) {
  if (alterandoStatus.value) return
  const alvo = dialog.target
  const opt  = dialog.statusOpt
  if (!alvo || !opt) return

  alterandoStatus.value = true
  try {
    await store.changeStatus(alvo.id, opt.status, { revisorId: usuarioId })
    dialog.pessoa = false
    await fecharDialogStatus()
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao enviar: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    alterandoStatus.value = false
  }
}

// Mudar a situação afeta as contagens das abas/chips (ex.: revogar tira o
// documento do total normal e o soma em "Revogados") -- store.changeStatus já
// atualiza a linha em si, mas resumo/total só refletem isso com um recarregamento.
async function fecharDialogStatus() {
  await carregar()
  dialog.status = false
  dialog.target = null
  dialog.statusOpt = null
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

// Antes, o clone só entrava direto no array local (store.documentos.unshift) porque
// esse array já era "o acervo inteiro" -- agora que é só a página atual, precisa
// recarregar de verdade pra refletir o total/ordenação corretos (ver carregar()).
async function executarClone() {
  if (dialog.target) {
    await store.cloneDocumento(dialog.target.id)
    await carregar()
  }
  dialog.clone = false
  dialog.target = null
}

const dialog = reactive({
  delete: false, status: false, clone: false, pessoa: false,
  target: null, statusOpt: null,
})

function confirmarExclusao(doc) {
  dialog.target = doc
  dialog.delete = true
}

async function excluir() {
  if (dialog.target) {
    await store.deleteDocumento(dialog.target.id)
    await carregar()
  }
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
}
.legis-table :deep(tbody tr:hover td) {
  background: rgba(74, 111, 165, 0.06) !important;
}
</style>
