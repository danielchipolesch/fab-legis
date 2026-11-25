<template>
  <q-page class="q-pa-xl">
    <q-breadcrumbs active-color="primary" style="font-size:13px" class="q-mb-md">
      <template v-slot:separator>
        <q-icon name="mdi-chevron-right" size="16px" color="primary" />
      </template>
      <q-breadcrumbs-el :to="{ name: 'home' }" icon="mdi-home" />
      <q-breadcrumbs-el label="Área de Trabalho" :to="{ name: 'home' }" />
      <q-breadcrumbs-el label="Manter Revisão de Documento" />
    </q-breadcrumbs>

    <div class="row items-center justify-between q-mb-xl">
      <div>
        <h1 class="text-h5 text-weight-bold text-primary q-my-none">Manter Revisão de Documento</h1>
        <p class="text-body2 text-grey-7 q-mb-none">
          Minutas da sua OM aguardando aprovação: qualquer aprovador da OM pode aprová-las, devolvê-las aos autores ou arquivá-las (com justificativa).
        </p>
      </div>
    </div>

    <q-card flat bordered>
      <q-table
        :rows="fila"
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
            <div class="row items-center justify-center" style="gap:4px">
              <StatusBadge :situacao-bca="props.row.situacao_bca" :situacao-local="props.row.situacao_local" />
            </div>
            <!-- O autor sugeriu a decisão a um Aprovador: só um destaque -- qualquer aprovador da OM decide. Em linha própria, para não
                 apertar as situações nem as ações da linha. -->
            <div v-if="rotuloDoAprovadorSugerido(props.row, auth.usuario?.id)" class="q-mt-xs">
              <q-badge
                rounded :color="props.row.aprovador_sugerido_id == auth.usuario?.id ? 'deep-orange' : 'grey-7'"
                :label="rotuloDoAprovadorSugerido(props.row, auth.usuario?.id)" data-testid="aprovador-sugerido-fila"
                style="white-space:normal;max-width:100%"
              />
            </div>
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
                  {{ props.row.situacao_local === 'MINUTA' ? 'Abrir e revisar' : 'Abrir' }}
                </q-tooltip>
              </q-btn>
              <q-btn
                icon="mdi-check-circle-outline"
                size="sm" flat round dense color="positive"
                @click="abrirAprovacao(props.row)"
              >
                <q-tooltip anchor="top middle" self="bottom middle">
                  {{ props.row.situacao_local === 'ANALISE_REVOGACAO' ? 'Aprovar revogação' : 'Aprovar' }}
                </q-tooltip>
              </q-btn>
              <q-btn
                icon="mdi-undo"
                size="sm" flat round dense color="negative"
                @click="devolver(props.row)"
              >
                <q-tooltip anchor="top middle" self="bottom middle">
                  {{ props.row.situacao_local === 'ANALISE_REVOGACAO' ? 'Devolver (recusar a revogação)' : 'Devolver aos autores' }}
                </q-tooltip>
              </q-btn>
              <!-- Arquivar: só a minuta de um documento que nunca foi publicado, com a justificativa da autoridade. -->
              <q-btn
                v-if="podeArquivar(props.row)"
                icon="mdi-archive-outline"
                size="sm" flat round dense color="teal"
                data-testid="arquivar"
                @click="abrirArquivamento(props.row)"
              >
                <q-tooltip anchor="top middle" self="bottom middle">Arquivar</q-tooltip>
              </q-btn>
            </div>
          </q-td>
        </template>

        <template #no-data>
          <div class="full-width column items-center q-py-xl text-grey-7">
            <q-icon size="56px" class="q-mb-sm" name="mdi-inbox-outline" />
            <p>Nenhum documento aguardando aprovação na sua OM no momento.</p>
          </div>
        </template>
      </q-table>
    </q-card>

    <!-- Arquivar: encerra a minuta; a justificativa é obrigatória e fica registrada com quem arquivou e quando. -->
    <q-dialog v-model="dialogArquivar" :persistent="enviando">
      <q-card style="min-width:400px;max-width:480px">
        <q-card-section class="row items-center q-pb-none">
          <q-icon name="mdi-archive-outline" color="teal" size="24px" class="q-mr-sm" />
          <span class="text-h6">Arquivar</span>
        </q-card-section>
        <q-card-section class="q-pt-md">
          <div class="text-weight-medium">{{ alvo?.codigo_documento }}</div>
          <div class="text-body2 text-grey-8 q-mt-sm">
            Arquivar encerra o documento: ele não segue para a publicação nem volta aos autores. A justificativa fica registrada com o seu nome e a data.
          </div>
          <q-input
            v-model="justificativa" type="textarea" autogrow outlined dense class="q-mt-md"
            label="Justificativa do arquivamento" data-testid="justificativa-arquivamento"
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" color="grey-7" :disable="enviando" v-close-popup />
          <q-btn
            unelevated label="Arquivar" color="teal" :loading="enviando" :disable="!justificativa.trim()"
            data-testid="confirmar-arquivamento" @click="confirmarArquivamento"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Aprovar: ninguém é escolhido -- o documento vai para a fila de publicação -- a do CENDOC (espécie convencional) ou a da própria OM (NPA) --, onde qualquer publicador age. -->
    <q-dialog v-model="dialogAprovar" :persistent="enviando">
      <q-card style="min-width:400px;max-width:480px">
        <q-card-section class="row items-center q-pb-none">
          <q-icon name="mdi-check-circle-outline" color="positive" size="24px" class="q-mr-sm" />
          <span class="text-h6">{{ alvo?.situacao_local === 'ANALISE_REVOGACAO' ? 'Aprovar revogação' : 'Aprovar' }}</span>
        </q-card-section>
        <q-card-section class="q-pt-md">
          <div class="text-weight-medium">{{ alvo?.codigo_documento }}</div>
          <div class="text-body2 text-grey-8 q-mt-sm">
            Ao aprovar, o documento segue para a fila de publicação (do CENDOC, se for espécie convencional; da sua OM, se for NPA; qualquer publicador pode publicá-lo) e o seu nome fica registrado como quem aprovou.
          </div>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" color="grey-7" :disable="enviando" v-close-popup />
          <q-btn unelevated label="Aprovar" color="positive" :loading="enviando" data-testid="confirmar-aprovacao" @click="confirmarAprovacao" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useQuasar } from 'quasar'
import { useDocumentosStore } from '@/stores/documentos.js'
import { useAuthStore } from '@/stores/auth.js'
import * as documentosApi from '@/api/documentos.js'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { caixaAlta } from '@/utils/texto.js'
import {
  destinoDeAprovacao, destinoDeDevolucao, podeArquivar, DESTINO_DE_ARQUIVAMENTO, rotuloDoAprovadorSugerido, filaComOsMeusPrimeiro,
} from '@/utils/fluxoDocumento.js'

const $q = useQuasar()
const store = useDocumentosStore()
const auth = useAuthStore()

const documentos = ref([])
// O que foi enviado a mim vem primeiro.
const fila = computed(() => filaComOsMeusPrimeiro(documentos.value, auth.usuario?.id))
const carregando = ref(false)

const columns = [
  { name: 'codigo',   label: 'Código',   field: 'codigo_documento', align: 'center', style: 'width: 120px' },
  { name: 'titulo',   label: 'Título',   field: 'titulo',           align: 'center' },
  { name: 'autores',  label: 'Autores',  field: 'autores',          align: 'center' },
  { name: 'status',   label: 'Situação', field: 'situacao_bca',         align: 'center', style: 'width: 160px' },
  { name: 'actions',  label: 'Ações',    field: 'actions',          align: 'center', style: 'width: 140px' },
]

// `silencioso`: a atualização ao vivo (aviso SSE) refaz a lista sem o indicador de carregamento -- a fila só muda, não pisca.
async function carregar(silencioso = false) {
  if (!silencioso) carregando.value = true
  try {
    documentos.value = await documentosApi.listMinhaRevisao()
  } catch (e) {
    if (!silencioso) $q.notify({ type: 'negative', message: `Erro ao carregar sua fila de revisão: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    carregando.value = false
  }
}
onMounted(() => carregar())
// Um documento chegou à fila, saiu dela ou outro aprovador da OM decidiu (aviso ao vivo): a fila se atualiza sozinha.
watch(() => store.refreshSignal, () => carregar(true))

// A MINUTA abre direto no editor -- é onde o aprovador da OM revisa E edita
// na mesma tela (ver isReadonly em DocumentoEditorPage.vue). ANALISE_REVOGACAO
// não tem conteúdo editável em etapa nenhuma (a revogação nunca reabre o
// documento), então continua indo pra visualização. As duas preservam a
// origem pro breadcrumb voltar pra cá.
function rotaAbrir(doc) {
  return doc.situacao_local === 'MINUTA'
    ? { name: 'documento-editar', params: { id: doc.id }, query: { origem: 'revisao' } }
    : { name: 'documento-visualizar', params: { id: doc.id }, query: { origem: 'revisao' } }
}

const dialogAprovar = ref(false)
const dialogArquivar = ref(false)
const justificativa = ref('')
const alvo = ref(null)
const enviando = ref(false)

function abrirAprovacao(doc) {
  alvo.value = doc
  dialogAprovar.value = true
}

async function confirmarAprovacao() {
  if (!alvo.value || enviando.value) return
  enviando.value = true
  try {
    await store.changeStatus(alvo.value.id, destinoDeAprovacao(alvo.value))
    dialogAprovar.value = false
    alvo.value = null
    await carregar()
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao aprovar: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    enviando.value = false
  }
}

function abrirArquivamento(doc) {
  alvo.value = doc
  justificativa.value = ''
  dialogArquivar.value = true
}

async function confirmarArquivamento() {
  if (!alvo.value || enviando.value || !justificativa.value.trim()) return
  enviando.value = true
  try {
    await store.changeStatus(alvo.value.id, DESTINO_DE_ARQUIVAMENTO, { justificativa: justificativa.value.trim() })
    dialogArquivar.value = false
    alvo.value = null
    await carregar()
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao arquivar: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    enviando.value = false
  }
}

async function devolver(doc) {
  try {
    await store.changeStatus(doc.id, destinoDeDevolucao(doc))
    await carregar()
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao devolver: ${e?.message ?? 'erro desconhecido'}` })
  }
}
</script>
