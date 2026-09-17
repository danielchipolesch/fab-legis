<template>
  <q-page class="q-pa-xl">
    <q-breadcrumbs active-color="primary" style="font-size:13px" class="q-mb-md">
      <template v-slot:separator>
        <q-icon name="mdi-chevron-right" size="16px" color="primary" />
      </template>
      <q-breadcrumbs-el :to="{ name: 'home' }" icon="mdi-home" />
      <q-breadcrumbs-el label="Revisão" />
    </q-breadcrumbs>

    <div class="row items-center justify-between q-mb-xl">
      <div>
        <h1 class="text-h5 text-weight-bold text-primary q-my-none">Revisão</h1>
        <p class="text-body2 text-grey-7 q-mb-none">
          Documentos atribuídos a você para revisar ou devolver.
        </p>
      </div>
    </div>

    <q-card flat bordered>
      <q-table
        :rows="documentos"
        :columns="columns"
        row-key="id"
        :loading="carregando"
        :rows-per-page-options="[15, 25, 50]"
        class="legis-table"
      >
        <template #body-cell-autores="props">
          <q-td :props="props">{{ props.row.autores.map(caixaAlta).join(', ') }}</q-td>
        </template>

        <template #body-cell-status="props">
          <q-td :props="props">
            <StatusBadge :status="props.row.status" />
          </q-td>
        </template>

        <template #body-cell-actions="props">
          <q-td :props="props" class="text-center">
            <div class="row justify-center no-wrap" style="gap:4px">
              <q-btn
                icon="mdi-eye-outline"
                size="sm" flat round dense color="primary"
                :to="rotaAbrir(props.row)"
              >
                <q-tooltip anchor="top middle" self="bottom middle">
                  {{ props.row.status === 'EM_REVISAO' ? 'Abrir e revisar' : 'Abrir' }}
                </q-tooltip>
              </q-btn>
              <q-btn
                icon="mdi-check-circle-outline"
                size="sm" flat round dense color="positive"
                @click="abrirAprovacao(props.row)"
              >
                <q-tooltip anchor="top middle" self="bottom middle">
                  {{ props.row.status === 'ANALISE_REVOGACAO' ? 'Aprovar revogação' : 'Aprovar' }}
                </q-tooltip>
              </q-btn>
              <q-btn
                icon="mdi-undo"
                size="sm" flat round dense color="negative"
                @click="devolver(props.row)"
              >
                <q-tooltip anchor="top middle" self="bottom middle">Devolver</q-tooltip>
              </q-btn>
            </div>
          </q-td>
        </template>

        <template #no-data>
          <div class="full-width column items-center q-py-xl text-grey-7">
            <q-icon size="56px" class="q-mb-sm" name="mdi-inbox-outline" />
            <p>Nenhum documento atribuído a você no momento.</p>
          </div>
        </template>
      </q-table>
    </q-card>

    <SelecionarPessoaDialog
      v-model="dialogAprovar"
      papel="PUBLIC"
      :titulo="alvo?.status === 'ANALISE_REVOGACAO' ? 'Aprovar revogação' : 'Aprovar'"
      :descricao="alvo ? `Escolha quem vai publicar ${alvo.codigo_documento}` : ''"
      acao-label="Aprovar"
      :enviando="enviando"
      @confirmar="confirmarAprovacao"
    />
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useDocumentosStore } from '@/stores/documentos.js'
import * as documentosApi from '@/api/documentos.js'
import StatusBadge from '@/components/common/StatusBadge.vue'
import SelecionarPessoaDialog from '@/components/editor/SelecionarPessoaDialog.vue'
import { caixaAlta } from '@/utils/texto.js'

const $q = useQuasar()
const store = useDocumentosStore()

const documentos = ref([])
const carregando = ref(false)

const columns = [
  { name: 'codigo',   label: 'Código',   field: 'codigo_documento', align: 'center', style: 'width: 120px' },
  { name: 'titulo',   label: 'Título',   field: 'titulo',           align: 'center' },
  { name: 'autores',  label: 'Autores',  field: 'autores',          align: 'center' },
  { name: 'status',   label: 'Situação', field: 'status',           align: 'center', style: 'width: 160px' },
  { name: 'actions',  label: 'Ações',    field: 'actions',          align: 'center', style: 'width: 140px' },
]

async function carregar() {
  carregando.value = true
  try {
    documentos.value = await documentosApi.listMinhaRevisao()
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao carregar sua fila de revisão: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    carregando.value = false
  }
}
onMounted(carregar)

// EM_REVISAO abre direto no editor -- é onde o revisor atribuído revisa E edita
// na mesma tela (ver isReadonly em DocumentoEditorPage.vue). ANALISE_REVOGACAO
// não tem conteúdo editável em etapa nenhuma (a revogação nunca reabre o
// documento), então continua indo pra visualização. As duas preservam a
// origem pro breadcrumb voltar pra cá.
function rotaAbrir(doc) {
  return doc.status === 'EM_REVISAO'
    ? { name: 'documento-editar', params: { id: doc.id }, query: { origem: 'revisao' } }
    : { name: 'documento-visualizar', params: { id: doc.id }, query: { origem: 'revisao' } }
}

// EM_REVISAO resolve pra APROVADO (fluxo normal) ou ALTERADO (ciclo de alteração)
// conforme o documento já tenha sido publicado antes -- mesmo discriminante do
// backend (ver DocumentoStatusService.jaPublicadoAntes), pra mandar o status certo.
function alvoAprovacao(doc) {
  if (doc.status === 'ANALISE_REVOGACAO') return 'EM_REVOGACAO'
  return doc.ja_publicado_antes ? 'ALTERADO' : 'APROVADO'
}

function alvoDevolucao(doc) {
  if (doc.status === 'ANALISE_REVOGACAO') return 'PUBLICADO'
  return doc.ja_publicado_antes ? 'EM_ALTERACAO' : 'MINUTA'
}

const dialogAprovar = ref(false)
const alvo = ref(null)
const enviando = ref(false)

function abrirAprovacao(doc) {
  alvo.value = doc
  dialogAprovar.value = true
}

async function confirmarAprovacao(publicadorId) {
  if (!alvo.value || enviando.value) return
  enviando.value = true
  try {
    await store.changeStatus(alvo.value.id, alvoAprovacao(alvo.value), { publicadorId })
    dialogAprovar.value = false
    alvo.value = null
    await carregar()
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao aprovar: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    enviando.value = false
  }
}

async function devolver(doc) {
  try {
    await store.changeStatus(doc.id, alvoDevolucao(doc))
    await carregar()
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao devolver: ${e?.message ?? 'erro desconhecido'}` })
  }
}
</script>
