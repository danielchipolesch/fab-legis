<template>
  <q-page class="q-pa-xl">

    <!-- Header -->
    <div class="row items-center q-mb-xl" style="gap:12px">
      <div class="col">
        <q-breadcrumbs active-color="primary" style="font-size:13px">
          <template v-slot:separator>
            <q-icon name="mdi-chevron-right" size="16px" color="primary" />
          </template>
          <q-breadcrumbs-el :to="{ name: 'home' }" icon="mdi-home" />
          <q-breadcrumbs-el label="Documentos" />
          <q-breadcrumbs-el
            :label="docLabel"
            :to="{ name: 'documento-visualizar', params: { id: route.params.id } }"
          />
          <q-breadcrumbs-el label="Comparar Versões" />
        </q-breadcrumbs>
        <h1 class="text-h5 text-weight-bold text-primary q-my-none q-mt-xs">Comparação de Versões</h1>
      </div>
      <StatusBadge v-if="documento" :status="documento.status" />
    </div>

    <template v-if="loading">
      <div class="row justify-center q-py-xl">
        <q-spinner color="primary" size="40px" />
      </div>
    </template>

    <template v-else-if="!documento">
      <q-banner class="bg-negative text-white" rounded>
        <template #avatar>
          <q-icon name="mdi-alert-circle-outline" color="white" />
        </template>
        Documento não encontrado
      </q-banner>
    </template>

    <template v-else-if="!ciclos.length">
      <q-banner class="bg-grey-3 text-grey-9" rounded>
        <template #avatar>
          <q-icon name="mdi-information-outline" />
        </template>
        Este documento ainda não teve nenhuma emenda registrada.
      </q-banner>
    </template>

    <template v-else>

      <!-- Seletor de ciclo -->
      <q-card flat bordered class="q-mb-lg">
        <q-card-section class="q-pa-md">
          <div class="row items-center q-col-gutter-md">
            <div class="col-12 col-md-6">
              <q-select
                v-model="selectedCiclo"
                :options="ciclos"
                option-label="label"
                option-value="id"
                emit-value
                map-options
                label="Ciclo de alteração"
                outlined
                dense
                hide-bottom-space
              >
                <template #prepend>
                  <q-icon name="mdi-tag-outline" />
                </template>
              </q-select>
            </div>
            <div class="col-12 col-md-3 row justify-center">
              <q-btn-toggle
                v-model="diffMode"
                no-caps
                unelevated
                toggle-color="primary"
                color="grey-3"
                text-color="grey-8"
                :options="[
                  { value: 'side',    label: 'Lado a lado', icon: 'mdi-view-column-outline' },
                  { value: 'unified', label: 'Unificado',   icon: 'mdi-view-stream-outline' },
                ]"
              />
            </div>
          </div>
        </q-card-section>
      </q-card>

      <!-- Summary badges -->
      <div class="row q-gutter-sm q-mb-lg">
        <q-chip color="green-2" text-color="green-10" size="sm" square icon="mdi-plus-circle-outline">
          {{ stats.incluido }} inclusões
        </q-chip>
        <q-chip color="orange-2" text-color="orange-10" size="sm" square icon="mdi-pencil-circle-outline">
          {{ stats.alterado }} alterações
        </q-chip>
        <q-chip color="red-2" text-color="red-10" size="sm" square icon="mdi-minus-circle-outline">
          {{ stats.revogado }} revogações
        </q-chip>
      </div>

      <!-- Per-section diffs -->
      <template v-for="secao in itensPorSecao" :key="secao.tipo">
        <q-card flat bordered class="q-mb-md">
          <q-card-section class="text-subtitle1 text-weight-bold q-px-md q-py-sm row items-center">
            <q-icon name="mdi-folder-outline" color="amber-8" class="q-mr-sm" size="18px" />
            {{ secao.titulo }}
          </q-card-section>
          <q-separator />
          <q-card-section class="q-pa-md">
            <div v-if="!secao.itens.length" class="text-caption text-grey-7">
              Seção sem modificações neste ciclo.
            </div>
            <DiffViewer
              v-for="item in secao.itens"
              :key="item.id"
              :label="referenciaLabel(item)"
              :elemento="{ conteudo: item.textoAnterior }"
              :elemento-b="{ conteudo: item.textoNovo }"
              label-a="Texto anterior"
              label-b="Texto proposto"
              :mode="diffMode"
            />
          </q-card-section>
        </q-card>
      </template>

      <!-- QUADRO DE JUSTIFICATIVAS -->
      <q-card flat bordered class="q-mt-xl">
        <q-card-section
          class="text-subtitle1 text-weight-bold q-px-md q-py-sm"
          style="display:grid;grid-template-columns:1fr auto 1fr;align-items:center"
        >
          <div></div>
          <div class="text-center">
            <q-icon name="mdi-table-edit" color="primary" class="q-mr-sm" size="18px" />
            Quadro de Justificativas das Modificações Propostas
          </div>
          <div class="row justify-end">
            <q-btn
              size="sm"
              outline
              color="primary"
              :loading="exportando"
              @click="exportarQuadro"
            >
              <q-icon left name="mdi-file-pdf-box" />
              Exportar
            </q-btn>
          </div>
        </q-card-section>
        <q-separator />
        <q-card-section class="q-pa-none">
          <q-markup-table flat dense class="justificativas-table">
            <thead>
              <tr>
                <th style="width:120px">Referência</th>
                <th>Texto em Vigor</th>
                <th>Texto Proposto</th>
                <th>Justificativa</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in itensCiclo" :key="item.id">
                <td class="text-caption text-weight-bold text-primary">
                  {{ referenciaLabel(item) }}
                </td>
                <td class="text-caption" style="max-width:220px">
                  <div v-if="item.acao === 'INCLUIR'" class="text-italic text-grey-7">(novo)</div>
                  <div v-else class="text-truncate-3" v-html="conteudoToHtml(item.textoAnterior)" />
                </td>
                <td class="text-caption" style="max-width:220px">
                  <div v-if="item.acao === 'REVOGAR'" class="text-italic text-grey-7">(revogado)</div>
                  <div v-else class="text-truncate-3" v-html="conteudoToHtml(item.textoNovo)" />
                </td>
                <td class="text-caption">{{ item.justificativa || '—' }}</td>
              </tr>
              <tr v-if="!itensCiclo.length">
                <td colspan="4" class="text-caption text-grey-7 text-center q-py-sm">
                  Sem modificações neste ciclo.
                </td>
              </tr>
            </tbody>
          </q-markup-table>
        </q-card-section>
      </q-card>

    </template>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useDocumentsStore } from '@/stores/documents.js'
import StatusBadge from '@/components/common/StatusBadge.vue'
import DiffViewer from '@/components/comparison/DiffViewer.vue'
import { formatLabel } from '@/utils/numbering.js'
import { generateHTML } from '@tiptap/html'
import { editorExtensions } from '@/editor/extensions.js'
import { gerarMapaAlteracaoPdf } from '@/services/pdfService.js'
import { useQuasar } from 'quasar'

function conteudoToHtml(conteudo) {
  if (!conteudo) return ''
  try { return generateHTML(JSON.parse(conteudo), editorExtensions) } catch { return '' }
}

const route = useRoute()
const store = useDocumentsStore()
const $q = useQuasar()
const loading = ref(true)
const exportando = ref(false)

onMounted(async () => {
  try {
    // store.getById pode já retornar um documento vindo da listagem da homepage
    // (DocumentoResponseSemAnexoTextualDto), que não traz itens/seções — sem elas,
    // referenciaLabel() nunca encontra o elemento na árvore e cai no fallback
    // "#id". Só pula o fetch completo quando as seções já estiverem carregadas.
    if (!store.getById(route.params.id)?.secoes) {
      await store.fetchDocumento(route.params.id)
    }
    await store.fetchMapaAlteracao(route.params.id)
  } finally {
    loading.value = false
  }
})

const documento = computed(() => store.getById(route.params.id))
const mapaAlteracao = computed(() => store.mapaAlteracaoPorDocumento[String(route.params.id)] ?? [])

const docId = computed(() => documento.value?.codigo_documento ?? '')

const docLabel = computed(() => {
  const d = documento.value
  if (!d) return ''
  return docId.value + (d.assunto_basico ? ` — ${d.assunto_basico}` : '')
})

// Ciclos disponíveis: agrupamento de cicloReferencia (a lista já vem ordenada por
// dtEmenda desc do backend, então o primeiro id visto de cada ciclo já é o mais
// recente — preserva essa ordem "mais recente primeiro").
const CICLO_ATUAL_ID = '__atual__'
const ciclos = computed(() => {
  const vistos = new Map()
  for (const item of mapaAlteracao.value) {
    const id = item.cicloReferencia ?? CICLO_ATUAL_ID
    if (!vistos.has(id)) {
      vistos.set(id, {
        id,
        label: item.cicloReferencia ?? 'Ciclo em andamento (não publicado)',
      })
    }
  }
  return Array.from(vistos.values())
})

const selectedCiclo = ref(null)
const diffMode = ref('side')

// Seleciona automaticamente o ciclo mais recente assim que a lista chega
function ensureSelectedCiclo() {
  if (selectedCiclo.value == null && ciclos.value.length) {
    selectedCiclo.value = ciclos.value[0].id
  }
}

const itensCiclo = computed(() => {
  ensureSelectedCiclo()
  return mapaAlteracao.value
    .filter(item => (item.cicloReferencia ?? CICLO_ATUAL_ID) === selectedCiclo.value)
    .slice()
    .sort((a, b) => new Date(a.dtEmenda) - new Date(b.dtEmenda))
})

const SECAO_LABELS = {
  PARTE_PRELIMINAR: 'Parte Preliminar',
  PARTE_NORMATIVA: 'Parte Normativa',
  PARTE_FINAL: 'Parte Final',
}
const SECAO_TIPO_FRONTEND = {
  PARTE_PRELIMINAR: 'parte_preliminar',
  PARTE_NORMATIVA: 'parte_normativa',
}

const itensPorSecao = computed(() => {
  const grupos = new Map()
  for (const item of itensCiclo.value) {
    if (!grupos.has(item.secao)) grupos.set(item.secao, [])
    grupos.get(item.secao).push(item)
  }
  return Array.from(grupos.entries()).map(([tipo, itens]) => ({
    tipo,
    titulo: SECAO_LABELS[tipo] ?? tipo,
    itens,
  }))
})

// Resolve o elemento vivo na árvore atual do documento a partir de secao+elementoId,
// para reaproveitar a numeração já calculada (formatLabel) — o histórico de emenda não
// guarda tipo/número do elemento, só o conteúdo antes/depois.
function findElemento(secaoBackend, elementoId) {
  const tipoFrontend = SECAO_TIPO_FRONTEND[secaoBackend]
  const secao = documento.value?.secoes?.find(s => s.tipo === tipoFrontend)
  if (!secao) return null
  const pilha = [...(secao.elementos ?? [])]
  while (pilha.length) {
    const el = pilha.shift()
    if (String(el.id) === String(elementoId)) return el
    if (el.filhos?.length) pilha.push(...el.filhos)
  }
  return null
}

function referenciaLabel(item) {
  const el = findElemento(item.secao, item.elementoId)
  if (el) return formatLabel(el)
  return (SECAO_LABELS[item.secao] ?? item.secao) + ' — ' + (item.tituloNovo ?? item.tituloAnterior ?? `#${item.elementoId}`)
}

const stats = computed(() => {
  let incluido = 0, alterado = 0, revogado = 0
  for (const item of itensCiclo.value) {
    if (item.acao === 'INCLUIR') incluido++
    else if (item.acao === 'ALTERAR') alterado++
    else if (item.acao === 'REVOGAR') revogado++
  }
  return { incluido, alterado, revogado }
})

async function exportarQuadro() {
  const cicloAtual = ciclos.value.find(c => c.id === selectedCiclo.value)
  const payload = {
    docId: docId.value,
    ciclo: cicloAtual?.label ?? null,
    itens: itensCiclo.value.map(item => ({
      referencia: referenciaLabel(item),
      acao: item.acao,
      textoAnterior: item.textoAnterior,
      textoNovo: item.textoNovo,
      justificativa: item.justificativa,
    })),
  }
  exportando.value = true
  try {
    await gerarMapaAlteracaoPdf(route.params.id, payload, docId.value)
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao exportar o quadro: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    exportando.value = false
  }
}
</script>

<style scoped>
.text-truncate-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
/* -webkit-line-clamp só se aplica a linhas de texto — sem isso, uma figura/anexo
   inserida no conteúdo renderiza no tamanho original e estoura a célula. */
.text-truncate-3 :deep(img) {
  display: block;
  max-width: 100%;
  max-height: 80px;
  object-fit: contain;
  margin: 2px auto;
}
.justificativas-table th {
  background: rgba(11, 61, 145, 0.08) !important;
  color: #0B3D91 !important;
  font-weight: 700 !important;
  font-size: 0.78rem !important;
  text-align: left;
}
.justificativas-table td {
  vertical-align: top;
  padding-top: 8px !important;
  padding-bottom: 8px !important;
  border-bottom: 1px solid rgba(0, 0, 0, 0.2) !important;
}
</style>
