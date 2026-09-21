<template>
  <!-- Mesmo padrão das tabelas das outras telas (ModuloPage, Revisão, Publicação): q-table com paginação no servidor. O cabeçalho
       de colunas fica oculto (hide-header): num card de uma lista só, "Documento" é óbvio e só polui. -->
  <q-table
    flat
    bordered
    dense
    hide-header
    row-key="id"
    :rows="documentos"
    :columns="columns"
    :loading="carregando"
    v-model:pagination="paginacao"
    :rows-per-page-options="[TAMANHO_DA_PAGINA]"
    class="painel-tabela"
    :data-testid="`cartao-${cartao}`"
    @request="aoPaginar"
  >
    <template #top>
      <div class="row items-center no-wrap full-width">
        <div class="text-subtitle2 text-weight-bold text-primary ellipsis">
          {{ titulo }} <span data-testid="total">({{ paginacao.rowsNumber }})</span>
        </div>
        <q-icon v-if="ajuda" name="mdi-information-outline" color="primary" size="18px" class="q-ml-xs" data-testid="ajuda">
          <q-tooltip anchor="top middle" self="bottom middle" max-width="280px">{{ ajuda }}</q-tooltip>
        </q-icon>
        <q-space />
        <q-btn flat round dense size="sm" icon="mdi-refresh" :loading="carregando" @click="carregar">
          <q-tooltip anchor="top middle" self="bottom middle">Atualizar</q-tooltip>
        </q-btn>
      </div>
    </template>

    <template #body-cell-documento="props">
      <q-td :props="props" class="documento-td" data-testid="linha">
        <div class="row items-start no-wrap" style="gap: 8px">
          <q-icon :name="moduloDe(props.row.tipo_de_especie).icone" color="primary" size="20px" class="q-mt-xs">
            <q-tooltip anchor="top middle" self="bottom middle">{{ moduloDe(props.row.tipo_de_especie).nome }}</q-tooltip>
          </q-icon>
          <div class="documento-celula">
            <!-- Código e situação lado a lado; embaixo o título e, por último, a ação a seguir. -->
            <div class="row items-center" style="gap: 2px 8px">
              <span class="text-weight-medium text-primary ellipsis">{{ props.row.codigo_documento }}</span>
              <StatusBadge
                :situacao-bca="props.row.situacao_bca"
                :situacao-local="props.row.situacao_local"
                :mostrar="selosDaLinha(cartao, props.row)"
                dense
              >
                <q-tooltip v-if="selosDaLinha(cartao, props.row) === 'ambos'" anchor="top middle" self="bottom middle">
                  Há uma versão publicada em vigor e uma etapa em tramitação sobre ela.
                </q-tooltip>
              </StatusBadge>
            </div>
            <div class="text-caption text-grey-7 ellipsis">{{ props.row.titulo }}</div>
            <div class="text-caption text-grey-7 ellipsis" data-testid="om-da-linha">{{ props.row.om_sigla }}</div>
            <div class="row items-center no-wrap">
              <q-icon v-if="acao(props.row).pendente" name="mdi-alert" color="warning" size="18px" class="q-mr-xs" data-testid="pendente">
                <q-tooltip anchor="top middle" self="bottom middle">Aguarda uma ação sua</q-tooltip>
              </q-icon>
              <q-btn
                flat
                dense
                no-caps
                color="primary"
                class="q-px-none"
                :label="acao(props.row).rotulo"
                :to="acao(props.row).rota"
                data-testid="acao-da-linha"
              />
            </div>
          </div>
        </div>
      </q-td>
    </template>

    <!-- À direita, só o ⋮ (Exibir detalhes). -->
    <template #body-cell-detalhes="props">
      <q-td :props="props" auto-width>
        <q-btn flat round dense size="sm" color="primary" icon="mdi-dots-vertical" data-testid="exibir-detalhes" @click="$emit('detalhes', props.row)">
          <q-tooltip anchor="top middle" self="bottom middle">Exibir detalhes</q-tooltip>
        </q-btn>
      </q-td>
    </template>

    <template #no-data>
      <div class="full-width column items-center q-py-lg text-grey-7" data-testid="vazio">
        <q-icon size="40px" class="q-mb-sm" name="mdi-inbox-outline" />
        <p class="q-mb-none">{{ vazio }}</p>
      </div>
    </template>
  </q-table>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useQuasar } from 'quasar'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { listarCartao } from '@/api/painel.js'
import { acaoDaLinha, selosDaLinha } from '@/utils/painel.js'
import { moduloDe } from '@/perfis/index.js'
import { useDocumentosStore } from '@/stores/documentos.js'
import { usePainelStore } from '@/stores/painel.js'

// Um dos três cards do hub (utils/painel.js): carrega a própria página, do backend, e mostra cada documento com o módulo
// dele, a situação e a ação a seguir (um link -- nada é executado aqui). O ⋮ só pede ao hub que mostre os detalhes.
const props = defineProps({
  cartao: { type: String, required: true },     // minhas_em_tramitacao | aguardando | em_tramitacao_de_outros | publicados
  titulo: { type: String, required: true },
  vazio:  { type: String, default: 'Nenhum documento.' },
  ajuda:  { type: String, default: null },       // o que o card mostra, num tooltip ao lado do título
})
defineEmits(['detalhes'])

const $q = useQuasar()
const docStore = useDocumentosStore()
const painelStore = usePainelStore()

const TAMANHO_DA_PAGINA = 8
const columns = [
  { name: 'documento', label: 'Documento', field: 'codigo_documento', align: 'left' },
  { name: 'detalhes',  label: '',          field: 'detalhes',         align: 'right' },
]

const documentos = ref([])
const carregando = ref(false)
// A página em que o card estava fica no Pinia (stores/painel.js): ao voltar ao hub, cada card continua onde parou.
const paginacao = ref({ page: painelStore.paginas[props.cartao] ?? 1, rowsPerPage: TAMANHO_DA_PAGINA, rowsNumber: 0 })

const acao = (doc) => acaoDaLinha(props.cartao, doc)

async function carregar() {
  carregando.value = true
  try {
    const { items, totalElements } = await listarCartao(props.cartao, {
      page: paginacao.value.page - 1, size: TAMANHO_DA_PAGINA,
    })
    documentos.value = items
    paginacao.value.rowsNumber = totalElements
    // Última página esvaziada (documentos saíram do card): volta para a anterior.
    if (!items.length && paginacao.value.page > 1) {
      paginacao.value.page--
      painelStore.definirPagina(props.cartao, paginacao.value.page)
      await carregar()
    }
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao carregar "${props.titulo}": ${e?.message ?? 'erro desconhecido'}`, position: 'bottom-right' })
  } finally {
    carregando.value = false
  }
}

// Disparado pela q-table ao trocar de página -- mesmo padrão de ModuloPage/AuditoriaPage.
function aoPaginar(req) {
  paginacao.value.page = req.pagination.page
  painelStore.definirPagina(props.cartao, paginacao.value.page)
  carregar()
}

onMounted(carregar)
// Algo fora da tela (ex.: te adicionaram como coautor) mudou o que este card mostra.
watch(() => docStore.refreshSignal, carregar)

defineExpose({ carregar })
</script>

<style scoped>
/* Faixa do título levemente tingida com a cor da marca (como nos cards do Compras.gov.br): separa o cabeçalho das linhas sem
   pesar -- é o próprio primário do tema a 8%, então acompanha a paleta do sistema. */
.painel-tabela :deep(.q-table__top) { background: color-mix(in srgb, var(--q-primary) 8%, white); }
/* O card é estreito (três lado a lado): a célula do documento ocupa o que sobra ao lado do ⋮ (max-width: 0 deixa o texto truncar
   com reticências dentro da tabela) e a situação passa para a linha de baixo quando não cabe ao lado do código. */
.documento-td { width: 100%; max-width: 0; }
.documento-celula { min-width: 0; flex: 1 1 0; }
</style>
