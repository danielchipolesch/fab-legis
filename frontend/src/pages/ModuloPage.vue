<template>
  <q-page class="q-pa-xl">

    <q-breadcrumbs active-color="primary" class="q-mb-md" style="font-size:13px">
      <template #separator>
        <q-icon name="mdi-chevron-right" size="16px" color="primary" />
      </template>
      <q-breadcrumbs-el :to="{ name: 'home' }" icon="mdi-home" />
      <q-breadcrumbs-el label="Área de Trabalho" :to="{ name: 'home' }" />
      <q-breadcrumbs-el :label="modulo.nome" />
    </q-breadcrumbs>

    <!-- Page header -->
    <!-- O texto ocupa o que sobra (col) e o botão fica sempre no canto direito (col-auto), sem cair para baixo do texto. -->
    <div class="row items-center no-wrap justify-between q-mb-xl" style="gap: 16px">
      <div class="col row items-center no-wrap" style="gap:12px">
        <q-avatar color="blue-2" text-color="primary" rounded size="48px">
          <q-icon :name="modulo.icone" size="28px" />
        </q-avatar>
        <div>
          <h1 class="text-h5 text-weight-bold text-primary q-my-none" data-testid="titulo-do-modulo">{{ modulo.nome }}</h1>
          <p class="text-body2 text-grey-7 q-mb-none">{{ modulo.descricao }}</p>
        </div>
      </div>
      <q-btn
        v-if="auth.isEditor"
        class="col-auto"
        color="primary"
        unelevated
        size="lg"
        data-testid="novo-documento"
        @click="dialogNovoDoc = true"
      >
        <q-icon left name="mdi-plus" />
        {{ modulo.rotuloDoBotaoNovo }}
      </q-btn>
    </div>

    <NovoDocumentoDialog v-model="dialogNovoDoc" :tipo-de-especie="tipoDeEspecie" />

    <!-- Abas (ownership) + filtros/resumo — tudo dentro do MESMO card de propósito:
         os filtros e as chips abaixo operam só sobre a aba selecionada acima, nunca
         sobre o acervo inteiro, e agrupar visualmente sem nenhum espaço/separador
         entre as duas coisas deixa essa relação óbvia (busca paginada no backend, ver
         carregar()/DocumentoSpecifications.aba). -->
    <q-card flat bordered class="q-mb-lg">
      <q-tabs
        v-model="store.abaAtiva"
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
          <div class="col-12 col-md-3">
            <q-input
              v-model="store.filtros.busca"
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
          <div v-if="especies.length > 1" class="col-6 col-md-2">
            <q-select
              v-model="store.filtros.especie"
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
              v-model="store.filtros.situacaoBca"
              :options="situacaoBcaOptions"
              :label="modulo.rotuloDaSituacaoOficial"
              outlined
              dense
              clearable
              emit-value
              map-options
              hide-bottom-space
            />
          </div>
          <div class="col-6 col-md-2">
            <q-select
              v-model="store.filtros.situacaoLocal"
              :options="situacaoLocalOptions"
              label="Situação Local"
              outlined
              dense
              clearable
              emit-value
              map-options
              hide-bottom-space
            />
          </div>
          <div class="col-12 col-md-3 row justify-end items-center" style="gap:8px">
            <!-- Busca Textual: procura no CONTEÚDO de todos os documentos (não só desta aba); leva o
                 termo já digitado no campo ao lado. -->
            <q-btn
              outline
              color="primary"
              icon="mdi-text-search"
              label="Busca no conteúdo"
              no-caps
              data-testid="busca-conteudo"
              :to="rotaBuscaConteudo(store.filtros.busca)"
            >
              <q-tooltip anchor="top middle" self="bottom middle">
                Procura dentro do texto de todos os documentos, de qualquer aba
              </q-tooltip>
            </q-btn>
            <q-btn flat @click="limparFiltros">
              <q-icon left name="mdi-filter-off" />
              Limpar
            </q-btn>
            <q-btn-toggle
              v-model="store.viewMode"
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
        <div class="row q-gutter-sm q-mt-md items-center">
          <q-chip
            v-for="s in resumoBca"
            :key="'bca-' + s.situacao"
            clickable
            :color="s.bg"
            :text-color="s.fg"
            size="sm"
            square
            data-testid="resumo-situacao-bca"
            @click="store.filtros.situacaoBca = store.filtros.situacaoBca === s.situacao ? null : s.situacao"
          >
            {{ s.label }}: <strong class="q-ml-xs">{{ s.count }}</strong>
          </q-chip>
          <q-separator v-if="resumoBca.length && resumoLocal.length" vertical inset />
          <q-chip
            v-for="s in resumoLocal"
            :key="'local-' + s.situacao"
            clickable
            outline
            :color="s.color"
            :text-color="s.fg"
            class="text-weight-bold chip-resumo-local"
            size="sm"
            square
            data-testid="resumo-situacao-local"
            @click="store.filtros.situacaoLocal = store.filtros.situacaoLocal === s.situacao ? null : s.situacao"
          >
            {{ s.label }}: <strong class="q-ml-xs">{{ s.count }}</strong>
          </q-chip>
        </div>
      </q-card-section>
    </q-card>

    <!-- TABLE VIEW -->
    <template v-if="store.viewMode === 'tabela'">
      <q-card flat bordered>
        <q-table
          :rows="store.documentos"
          :columns="columns"
          row-key="id"
          :loading="store.loading"
          :rows-per-page-options="[15, 25, 50]"
          v-model:pagination="store.tablePagination"
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
                {{ props.row.codigo_documento }}
              </span>
            </q-td>
          </template>

          <template #body-cell-data_criacao="props">
            <q-td :props="props">
              {{ formatarData(props.row.data_criacao) }}
            </q-td>
          </template>

          <!-- As duas situações em colunas próprias: a BCA (real) e a local (etapa interna) -->
          <template #body-cell-situacao_bca="props">
            <q-td :props="props">
              <StatusBadge :situacao-bca="props.row.situacao_bca" mostrar="bca" />
            </q-td>
          </template>

          <template #body-cell-situacao_local="props">
            <q-td :props="props">
              <StatusBadge :situacao-local="props.row.situacao_local" mostrar="local" />
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
                    {{ tooltipEditar(props.row) }}
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

                <q-btn
                  icon="mdi-dots-vertical"
                  size="sm"
                  flat
                  round
                  dense
                  color="primary"
                  :disable="!temAcoesExtras(props.row)"
                >
                  <q-tooltip anchor="top middle" self="bottom middle">
                    {{ temAcoesExtras(props.row) ? 'Mais ações' : 'Nenhuma ação disponível' }}
                  </q-tooltip>
                  <q-menu>
                    <q-list dense style="min-width:200px">
                      <q-item
                        v-for="opt in statusActions(props.row)"
                        :key="opt.destino"
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
                  {{ doc.codigo_documento }}
                </q-item-label>
                <q-item-label caption>{{ formatarData(doc.data_criacao) }}</q-item-label>
              </q-item-section>
              <q-item-section side top>
                <StatusBadge :situacao-bca="doc.situacao_bca" :situacao-local="doc.situacao_local" size="xs" />
              </q-item-section>
            </q-item>

            <q-card-section class="col">
              <p class="text-body2 text-grey-7 q-mb-none text-truncate-2">
                {{ doc.assunto_basico || doc.titulo }}
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
          v-model="store.tablePagination.page"
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
          <strong>{{ dialog.target?.codigo_documento }}</strong>
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
          <strong>{{ dialog.target?.codigo_documento }}</strong>
          <template v-if="dialog.statusOpt?.descricao">{{ dialog.statusOpt.descricao }}</template>
          <template v-else>terá sua etapa alterada para <strong>{{ dialog.statusOpt?.rotuloDestino }}</strong>.</template>
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
      :descricao="dialog.target ? `Documento ${dialog.target.codigo_documento}` : ''"
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
          <strong>{{ dialog.target?.codigo_documento }}</strong>
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
import { ref, computed, reactive, nextTick, onMounted, watch } from 'vue'
import { useQuasar } from 'quasar'
import { useDocumentosStore } from '@/stores/documentos.js'
import { useAuthStore } from '@/stores/auth.js'
import StatusBadge from '@/components/common/StatusBadge.vue'
import NovoDocumentoDialog from '@/components/common/NovoDocumentoDialog.vue'
import SelecionarPessoaDialog from '@/components/editor/SelecionarPessoaDialog.vue'
import { gerarPdf } from '@/services/pdfService.js'
import { listEspeciesNormativas, normalizeEspecie } from '@/api/referencias.js'
import { SITUACAO_BCA_META, SITUACAO_LOCAL_META } from '@/utils/statusDocumento.js'
import { rotaBuscaConteudo } from '@/utils/buscaTextual.js'
import { perfilDoDocumento, moduloDe } from '@/perfis/index.js'

// A tela é a mesma para todos os módulos (perfis/index.js): o tipo de espécie, vindo da rota, decide quais documentos
// entram, as colunas e os rótulos. Nunca se testa a sigla da espécie aqui.
const props = defineProps({
  tipoDeEspecie: { type: String, required: true },
})
const modulo = computed(() => moduloDe(props.tipoDeEspecie))

const $q = useQuasar()
const store = useDocumentosStore()
const auth = useAuthStore()

const dialogNovoDoc = ref(false)
const pdfLoading = reactive({})

// Siglas do catálogo real de espécies normativas (t_especie_normativa), não mais
// uma lista fixa que já ficou desatualizada em relação ao que existe no banco
// (ver EspecieNormativaEnum) -- ver carregarEspecies() no onMounted abaixo.
const especies = ref([])
async function carregarEspecies() {
  try {
    const lista = await listEspeciesNormativas()
    especies.value = lista.map(normalizeEspecie).filter(e => e.tipoDeEspecie === props.tipoDeEspecie).map(e => e.sigla).sort()
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao carregar espécies normativas: ${e?.message ?? 'erro desconhecido'}` })
  }
}
// Filtros: as duas situações são independentes (ver utils/statusDocumento.js). SEM_ETAPA fica
// de fora do filtro local -- "sem etapa em curso" não é uma situação que alguém procure.
const situacaoBcaOptions = Object.entries(SITUACAO_BCA_META).map(([value, m]) => ({ value, label: m.label }))
const situacaoLocalOptions = Object.entries(SITUACAO_LOCAL_META)
  .filter(([value]) => value !== 'SEM_ETAPA')
  .map(([value, m]) => ({ value, label: m.label }))

const TODAS_AS_COLUNAS = [
  { name: 'especie',        label: 'Espécie',        field: 'especie',        align: 'center', sortable: true,  style: 'width: 100px' },
  { name: 'numero',         label: 'Número',         field: 'numero_basico',  align: 'center', sortable: false },
  { name: 'titulo',         label: 'Título',         field: 'titulo',         align: 'center', sortable: true },
  { name: 'assunto_basico', label: 'Assunto Básico', field: 'assunto_basico', align: 'center', sortable: true },
  { name: 'data_criacao',   label: 'Data',           field: 'data_criacao',   align: 'center', sortable: true,  style: 'width: 120px' },
  { name: 'situacao_bca',   label: 'Situação BCA',   field: 'situacao_bca',   align: 'center', sortable: true,  style: 'width: 140px' },
  { name: 'situacao_local', label: 'Situação Local', field: 'situacao_local', align: 'center', sortable: true,  style: 'width: 170px' },
  { name: 'replicas',       label: 'Réplicas',       field: 'qtd_replicas',   align: 'center', sortable: true,  style: 'width: 90px' },
  { name: 'actions',        label: 'Ações',          field: 'actions',        align: 'center', sortable: false, style: 'width: 220px' },
]

// As colunas do módulo: sem as que não fazem sentido para ele, e com os rótulos dele (Número/Identificação; BCA/Boletim Interno).
const columns = computed(() => TODAS_AS_COLUNAS
  .filter(c => !modulo.value.colunasOcultas.includes(c.name))
  .map(c => c.name === 'situacao_bca' ? { ...c, label: modulo.value.rotuloDaSituacaoOficial }
    : c.name === 'numero' ? { ...c, label: modulo.value.rotuloDoNumero } : c))

// Nome da coluna (frontend, snake_case) -> propriedade Java que o backend ordena (ver
// DocumentoController.getAll) -- os dois lados usam nomenclaturas diferentes de
// propósito (ver convenção do projeto), então a ordenação por servidor precisa dessa
// tradução explícita.
const SORT_FIELD_MAP = {
  especie: 'especieNormativa.sigla',
  titulo: 'tituloDocumento',
  assunto_basico: 'assuntoBasico.nome',
  data_criacao: 'dtCriacao',
  situacao_bca: 'situacaoBca',
  situacao_local: 'situacaoLocal',
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
const abaAtivaLabel = computed(() => ABA_LABELS[store.abaAtiva])

// Paginação real no backend (ver DocumentoController.getAll/DocumentoService --
// antes disso, um único fetch de até 200 documentos vinha pro navegador, e aba, busca,
// espécie/situação e a própria paginação da tabela eram calculadas em JS por cima desse
// array fixo -- acima de 200 documentos no acervo o resto simplesmente não aparecia).
const totalPaginas = computed(() => Math.max(1, Math.ceil(store.totalElements / store.tablePagination.rowsPerPage)))

async function carregar() {
  const params = {
    aba: store.abaAtiva,
    busca: store.filtros.busca || undefined,
    tipoDeEspecie: props.tipoDeEspecie,
    especieSigla: store.filtros.especie || undefined,
    situacaoBca: store.filtros.situacaoBca || undefined,
    situacaoLocal: store.filtros.situacaoLocal || undefined,
    page: store.tablePagination.page - 1,
    size: store.tablePagination.rowsPerPage,
    sortBy: SORT_FIELD_MAP[store.tablePagination.sortBy] ?? 'dtCriacao',
    descending: store.tablePagination.descending,
  }
  await Promise.all([
    store.fetchPagina(params),
    store.fetchResumo({ aba: params.aba, busca: params.busca, tipoDeEspecie: params.tipoDeEspecie, especieSigla: params.especieSigla }),
  ])
}

// Disparado pela q-table (clique de página/ordenação/linhas-por-página) -- a própria
// tabela já atualiza store.tablePagination via v-model antes de chamar isso (padrão
// Quasar de paginação por servidor, mesmo usado em AuditoriaPage.vue).
function onRequest(props) {
  store.tablePagination.page = props.pagination.page
  store.tablePagination.rowsPerPage = props.pagination.rowsPerPage
  store.tablePagination.sortBy = props.pagination.sortBy
  store.tablePagination.descending = props.pagination.descending
  carregar()
}

watch(() => store.totalElements, (v) => { store.tablePagination.rowsNumber = v })

// Trocar de aba/espécie/situação busca de novo na hora; busca por texto livre tem um
// debounce curto (a q-table não dispara @request por digitação, então sem isso cada
// tecla viraria uma requisição).
let buscaTimer = null
// Ao entrar no módulo o store traz de volta a aba, os filtros e a página que a pessoa deixou (ver entrarNoModulo): as mudanças
// que isso causa não são escolhas dela, então não recomeçam da primeira página nem buscam duas vezes.
let restaurando = false
watch(() => store.filtros.busca, () => {
  if (restaurando) return
  clearTimeout(buscaTimer)
  buscaTimer = setTimeout(() => { store.tablePagination.page = 1; carregar() }, 350)
})
watch([() => store.abaAtiva, () => store.filtros.especie, () => store.filtros.situacaoBca, () => store.filtros.situacaoLocal], () => {
  if (restaurando) return
  store.tablePagination.page = 1
  carregar()
})

// Alguém te adicionou como coautor em outro documento (ver notificação
// DOCUMENTO_COMPARTILHADO tratada em AppTopBar.vue) -- refaz a busca da aba
// atual sem esperar o usuário trocar de aba ou recarregar a página.
watch(() => store.refreshSignal, () => { carregar() })

// Entrar noutro módulo (a rota muda, a tela é a mesma) traz do store o estado que a pessoa deixou nele (ou o padrão, na
// primeira vez): aba, filtros, página e modo de visualização.
async function iniciar() {
  restaurando = true
  store.entrarNoModulo(props.tipoDeEspecie)
  await nextTick()   // os watchers acima disparam com o que o store restaurou, ainda como "restaurando"
  restaurando = false
  carregar()
  carregarEspecies()
}
onMounted(iniciar)
watch(() => props.tipoDeEspecie, iniciar)

// store.resumoSituacaoBca/resumoSituacaoLocal já vem do servidor com aba/busca/espécie aplicados (ver
// DocumentoService.getResumo) -- o número no chip bate com o que aparece na tabela ao
// clicar nele, mesma garantia de antes, só que calculada no backend agora.
function resumir(meta, contagens, ocultar = []) {
  return Object.entries(meta)
    .filter(([situacao]) => !ocultar.includes(situacao))
    .map(([situacao, m]) => ({ situacao, label: m.label, bg: m.bg, fg: m.fg, color: m.color, count: contagens[situacao] ?? 0 }))
    .filter(s => s.count > 0)
}
const resumoBca = computed(() => resumir(SITUACAO_BCA_META, store.resumoSituacaoBca))
const resumoLocal = computed(() => resumir(SITUACAO_LOCAL_META, store.resumoSituacaoLocal, ['SEM_ETAPA']))

// O ícone da tela do módulo sempre abre em modo leitura (visualizar), mesmo para o
// revisor atribuído durante EM_REVISAO -- ele entra no editor de propósito, pelo
// link "Editar" da tela de visualização (mesmo padrão de Rascunho/Minuta) ou
// pela fila de Revisão, nunca direto por aqui.
//
// Posse (autor OU coautor) é tão obrigatória quanto o status -- mesma regra
// de DocumentoAcessoService.podeEditar no backend (que já barrava a edição de
// verdade; o que faltava era o ÍCONE refletir isso, em vez de ficar habilitado
// pra RASCUNHO/MINUTA de qualquer um só porque a OM bate). eh_autor_ou_coautor
// só vem preenchido (true/false) na listagem paginada -- ver
// DocumentoResponseSemAnexoTextualDto.ehAutorOuCoautor/backendParaFrontend.
function statusPermiteEdicao(doc) {
  return ['RASCUNHO', 'MINUTA', 'EM_ALTERACAO'].includes(doc.situacao_local)
}

function temPosseDocumento(doc) {
  return doc.eh_autor_ou_coautor === true
}

function canEdit(doc) {
  return statusPermiteEdicao(doc) && temPosseDocumento(doc)
}

// Mensagem do tooltip do lápis -- distingue as duas razões de bloqueio
// (situação vs. posse) em vez de uma mensagem genérica, senão um Editor de
// verdade (autor/coautor) não sabe se o problema é o documento estar
// Aprovado ou se é de outra pessoa.
function tooltipEditar(doc) {
  if (!statusPermiteEdicao(doc)) return 'Edição disponível apenas para Rascunho e Minuta'
  if (!temPosseDocumento(doc)) return 'Você só pode editar documentos os quais é autor ou coautor'
  return 'Editar'
}

// Mesma correção de posse que canEdit -- DocumentoAcessoService.podeExcluir
// também exige autor/coautor, não só o status; sem isso "Excluir" aparecia no
// menu "⋮" pra Rascunho/Minuta de qualquer pessoa da mesma OM.
function canDelete(doc) {
  return ['RASCUNHO', 'MINUTA'].includes(doc.situacao_local) && temPosseDocumento(doc)
}

// Mesmo critério que decide o que aparece dentro do menu "⋮" -- se nada
// aparecer (nenhuma transição de status disponível pro papel do usuário nem
// exclusão), o botão fica desabilitado em vez de abrir um menu vazio.
function temAcoesExtras(doc) {
  return statusActions(doc).length > 0 || canDelete(doc)
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
  const publicado = doc.situacao_bca === 'PUBLICADO'
  switch (doc.situacao_local) {
    case 'RASCUNHO':
      return auth.isEditor
        ? [{ destino: 'MINUTA', label: 'Enviar para Minuta', icon: 'mdi-file-edit-outline', rotuloDestino: 'Minuta' }]
        : []
    case 'MINUTA':
      return auth.isEditor
        ? [{ destino: 'EM_REVISAO', label: 'Enviar para Revisão', icon: 'mdi-account-arrow-right-outline', escolherPessoa: true }]
        : []
    case 'EM_ALTERACAO':
      return [
        ...(auth.isEditor ? [{ destino: 'EM_REVISAO', label: 'Enviar Alteração para Revisão', icon: 'mdi-account-arrow-right-outline', escolherPessoa: true }] : []),
        // Desistir da alteração: o documento segue PUBLICADO com a versão vigente (SEM_ETAPA =
        // concluir a etapa, aqui, cancelando-a). Só sem alterações pendentes: cada uma se desfaz
        // elemento a elemento, no painel lateral do editor.
        ...(auth.isAprovador || auth.isEditor ? [{
          destino: 'SEM_ETAPA', label: 'Cancelar Alteração', icon: 'mdi-close-circle-outline',
          descricao: 'terá a alteração cancelada e continua PUBLICADO, com a versão vigente. Só é possível sem alterações pendentes: desfaça-as antes, elemento a elemento, no painel lateral do editor.',
        }] : []),
      ]
    case 'SEM_ETAPA':
      return publicado ? [
        // A NPA não tem alteração: para mudá-la cria-se outra e revoga-se a anterior (perfis/index.js).
        ...(auth.isAprovador && perfilDoDocumento(doc).permiteAlteracao ? [{ destino: 'EM_ALTERACAO', label: 'Iniciar Alteração', icon: 'mdi-pencil-lock-outline', rotuloDestino: 'Em Alteração' }] : []),
        ...(auth.isEditor ? [{ destino: 'ANALISE_REVOGACAO', label: 'Enviar para Revogação', icon: 'mdi-file-remove-outline', escolherPessoa: true }] : []),
      ] : []
    default:
      return []
  }
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
    await store.changeStatus(alvo.id, opt.destino)
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
    await store.changeStatus(alvo.id, opt.destino, { revisorId: usuarioId })
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
  store.filtros.busca = ''
  store.filtros.especie = null
  store.filtros.situacaoBca = null
  store.filtros.situacaoLocal = null
}
</script>

<style scoped>
/* Contorno mais grosso: o outline padrão do Quasar (1px) some em chips pequenos. */
.chip-resumo-local.q-chip--outline:before { border-width: 2px; }
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
