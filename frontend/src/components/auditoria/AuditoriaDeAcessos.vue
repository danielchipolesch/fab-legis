<template>
  <div data-testid="auditoria-de-acessos">
    <q-card flat bordered class="q-mb-lg">
      <q-card-section class="q-pa-md">
        <div class="row q-col-gutter-sm items-center">
          <div class="col-6 col-md-3">
            <q-input v-model="filtros.cpf" label="CPF do usuário" outlined dense clearable hide-bottom-space
                     @update:model-value="val => filtros.cpf = mascaraCpf(val ?? '')" maxlength="14" />
          </div>
          <div class="col-6 col-md-3">
            <q-select v-model="filtros.acao" :options="acaoOptions" emit-value map-options label="Ação"
                      outlined dense clearable hide-bottom-space />
          </div>
          <div class="col-6 col-md-2">
            <q-input v-model="filtros.dataInicio" type="date" label="De" outlined dense clearable hide-bottom-space />
          </div>
          <div class="col-6 col-md-2">
            <q-input v-model="filtros.dataFim" type="date" label="Até" outlined dense clearable hide-bottom-space />
          </div>
          <div class="col-12 col-md-2 row justify-end items-center">
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
        :rows="registros" :columns="columns" row-key="id" :loading="carregando"
        v-model:pagination="pagination" :rows-per-page-options="[25, 50, 100]" @request="onRequest"
      >
        <template #body-cell-dtOcorrencia="props">
          <q-td :props="props">{{ formatarDataHora(props.row.dtOcorrencia) }}</q-td>
        </template>
        <template #body-cell-ator="props">
          <q-td :props="props">
            <div>{{ caixaAlta(atorDoAcesso(props.row)) }}</div>
            <div v-if="props.row.atorCpf" class="text-caption text-grey-7">{{ ocultarCpf(props.row.atorCpf) }}</div>
          </q-td>
        </template>
        <template #body-cell-alvo="props">
          <q-td :props="props">
            <div>{{ caixaAlta(props.row.alvoNome) }}</div>
            <div class="text-caption text-grey-7">{{ ocultarCpf(props.row.alvoCpf) }} · {{ props.row.omSigla ?? '—' }}</div>
          </q-td>
        </template>
        <template #body-cell-acao="props">
          <q-td :props="props">
            <q-chip dense square size="sm" :color="acaoDeAcessoCor(props.row.acao)" text-color="white">
              {{ acaoDeAcessoLabel(props.row.acao) }}
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useQuasar } from 'quasar'
import { listAuditoriaDeAcessos } from '@/api/auditoria.js'
import { mascaraCpf, ocultarCpf, onlyDigits } from '@/utils/cpf.js'
import { caixaAlta } from '@/utils/texto.js'
import { ACAO_DE_ACESSO, acaoDeAcessoLabel, acaoDeAcessoCor, atorDoAcesso } from '@/utils/auditoriaDeAcessos.js'

const $q = useQuasar()

const registros = ref([])
const carregando = ref(false)
const filtros = reactive({ cpf: '', acao: null, dataInicio: '', dataFim: '' })
const pagination = ref({ page: 1, rowsPerPage: 25, rowsNumber: 0 })

const columns = [
  { name: 'dtOcorrencia', label: 'Data/Hora',       field: 'dtOcorrencia', align: 'left', style: 'width:160px' },
  { name: 'ator',         label: 'Quem fez',        field: 'atorNome',     align: 'left' },
  { name: 'alvo',         label: 'Usuário afetado', field: 'alvoNome',     align: 'left' },
  { name: 'acao',         label: 'Ação',            field: 'acao',         align: 'center', style: 'width:170px' },
  { name: 'detalhe',      label: 'Detalhe',         field: 'detalhe',      align: 'left' },
]

const acaoOptions = Object.entries(ACAO_DE_ACESSO).map(([value, cfg]) => ({ label: cfg.label, value }))

function formatarDataHora(iso) {
  if (!iso) return '—'
  const [data, hora] = String(iso).split('T')
  const [y, m, d] = data.split('-')
  return `${d}/${m}/${y} ${hora?.slice(0, 5) ?? ''}`
}

async function carregar() {
  carregando.value = true
  try {
    const resp = await listAuditoriaDeAcessos({
      cpf: onlyDigits(filtros.cpf),
      acao: filtros.acao,
      dataInicio: filtros.dataInicio,
      dataFim: filtros.dataFim,
      page: pagination.value.page - 1,
      size: pagination.value.rowsPerPage,
    })
    registros.value = resp.content ?? []
    pagination.value.rowsNumber = resp.totalElements ?? 0
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao carregar a auditoria de acessos: ${e?.message ?? 'erro desconhecido'}` })
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
  Object.assign(filtros, { cpf: '', acao: null, dataInicio: '', dataFim: '' })
}

onMounted(carregar)
</script>
