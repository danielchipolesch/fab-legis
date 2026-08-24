<template>
  <q-page class="q-pa-xl">

    <div class="row items-center justify-between q-mb-xl">
      <div>
        <h1 class="text-h5 text-weight-bold text-primary q-my-none">Auditoria</h1>
        <p class="text-body2 text-grey-7 q-mb-none">
          Log de acesso e ações sobre documentos — quem viu, quem editou, quando
        </p>
      </div>
    </div>

    <q-card flat bordered class="q-mb-lg">
      <q-card-section class="q-pa-md">
        <div class="row q-col-gutter-sm items-center">
          <div class="col-6 col-md-2">
            <q-input
              v-model.number="filtros.documentoId"
              label="ID do documento"
              type="number"
              outlined dense clearable hide-bottom-space
            />
          </div>
          <div class="col-6 col-md-3">
            <q-select
              v-model="filtros.acao"
              :options="acaoOptions"
              emit-value map-options
              label="Ação"
              outlined dense clearable hide-bottom-space
            />
          </div>
          <div class="col-6 col-md-2">
            <q-input v-model="filtros.dataInicio" type="date" label="De" outlined dense clearable hide-bottom-space />
          </div>
          <div class="col-6 col-md-2">
            <q-input v-model="filtros.dataFim" type="date" label="Até" outlined dense clearable hide-bottom-space />
          </div>
          <div class="col-12 col-md-3 row justify-end items-center">
            <q-btn flat @click="limparFiltros">
              <q-icon left name="mdi-filter-off" />
              Limpar
            </q-btn>
          </div>
        </div>
      </q-card-section>
    </q-card>

    <q-card flat bordered>
      <q-table
        :rows="registros"
        :columns="columns"
        row-key="id"
        :loading="carregando"
        v-model:pagination="pagination"
        :rows-per-page-options="[25, 50, 100]"
        @request="onRequest"
      >
        <template #body-cell-dtOcorrencia="props">
          <q-td :props="props">{{ formatarDataHora(props.row.dtOcorrencia) }}</q-td>
        </template>

        <template #body-cell-usuario="props">
          <q-td :props="props">
            <div>{{ props.row.usuarioNome }}</div>
            <div class="text-caption text-grey-7">{{ formatarCpf(props.row.usuarioCpf) }}</div>
          </q-td>
        </template>

        <template #body-cell-documento="props">
          <q-td :props="props">
            <div class="text-weight-medium text-primary">{{ props.row.documentoDescricao }}</div>
            <div class="text-caption text-grey-7">#{{ props.row.documentoId }}</div>
          </q-td>
        </template>

        <template #body-cell-acao="props">
          <q-td :props="props">
            <q-chip dense square size="sm" :color="acaoCor(props.row.acao)" text-color="white">
              {{ acaoLabel(props.row.acao) }}
            </q-chip>
          </q-td>
        </template>

        <template #no-data>
          <div class="full-width column items-center q-py-xl text-grey-7">
            <q-icon size="56px" class="q-mb-sm" name="mdi-text-box-search-outline" />
            <p>Nenhum registro encontrado.</p>
          </div>
        </template>
      </q-table>
    </q-card>

  </q-page>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useQuasar } from 'quasar'
import { listAuditoria } from '@/api/auditoria.js'
import { formatarCpf } from '@/utils/cpf.js'

const $q = useQuasar()

const registros  = ref([])
const carregando = ref(false)

const filtros = reactive({ documentoId: null, acao: null, dataInicio: '', dataFim: '' })

const pagination = ref({ page: 1, rowsPerPage: 25, rowsNumber: 0 })

const columns = [
  { name: 'dtOcorrencia', label: 'Data/Hora',  field: 'dtOcorrencia',       align: 'left',   sortable: false, style: 'width:160px' },
  { name: 'usuario',      label: 'Usuário',     field: 'usuarioNome',       align: 'left' },
  { name: 'documento',    label: 'Documento',   field: 'documentoDescricao', align: 'left' },
  { name: 'acao',         label: 'Ação',        field: 'acao',              align: 'center', style: 'width:160px' },
  { name: 'detalhe',      label: 'Detalhe',     field: 'detalhe',           align: 'left' },
]

const ACAO_CFG = {
  VISUALIZOU:              { label: 'Visualizou',           color: 'grey-6' },
  CRIOU:                   { label: 'Criou',                color: 'green-8' },
  EDITOU:                  { label: 'Editou',               color: 'primary' },
  EXCLUIU:                 { label: 'Excluiu',              color: 'negative' },
  CLONOU:                  { label: 'Clonou',               color: 'indigo-8' },
  MUDOU_STATUS:            { label: 'Mudou situação',       color: 'deep-orange-8' },
  COMPARTILHOU:            { label: 'Compartilhou',         color: 'teal-8' },
  REMOVEU_COMPARTILHAMENTO:{ label: 'Removeu compart.',     color: 'brown-6' },
}

const acaoOptions = Object.entries(ACAO_CFG).map(([value, cfg]) => ({ label: cfg.label, value }))
function acaoLabel(acao) { return ACAO_CFG[acao]?.label ?? acao }
function acaoCor(acao) { return ACAO_CFG[acao]?.color ?? 'grey' }

function formatarDataHora(iso) {
  if (!iso) return '—'
  const [data, hora] = String(iso).split('T')
  const [y, m, d] = data.split('-')
  return `${d}/${m}/${y} ${hora?.slice(0, 5) ?? ''}`
}

async function carregar() {
  carregando.value = true
  try {
    const resp = await listAuditoria({
      documentoId: filtros.documentoId,
      acao: filtros.acao,
      dataInicio: filtros.dataInicio,
      dataFim: filtros.dataFim,
      page: pagination.value.page - 1,
      size: pagination.value.rowsPerPage,
    })
    registros.value = resp.content ?? []
    pagination.value.rowsNumber = resp.totalElements ?? 0
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao carregar auditoria: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    carregando.value = false
  }
}

function onRequest(props) {
  pagination.value.page = props.pagination.page
  pagination.value.rowsPerPage = props.pagination.rowsPerPage
  carregar()
}

watch(filtros, () => {
  pagination.value.page = 1
  carregar()
}, { deep: true })

function limparFiltros() {
  Object.assign(filtros, { documentoId: null, acao: null, dataInicio: '', dataFim: '' })
}

onMounted(carregar)
</script>
