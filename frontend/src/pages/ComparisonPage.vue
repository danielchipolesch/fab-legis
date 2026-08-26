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
          <q-breadcrumbs-el
            v-if="podeEditar"
            label="Editar"
            icon="mdi-pencil-outline"
            :to="{ name: 'documento-editar', params: { id: route.params.id } }"
          />
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
              :label-ancestrais="referenciaPartes(item).ancestrais.join(', ')"
              :label-atual="referenciaPartes(item).atual"
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
          <div class="row justify-end q-gutter-x-sm">
            <q-btn
              v-if="documento?.status === 'ALTERADO'"
              size="sm"
              outline
              color="primary"
              @click="abrirTextoSugerido"
            >
              <q-icon left name="mdi-file-document-edit-outline" />
              Texto Sugerido
            </q-btn>
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
                <td class="text-caption">
                  <span v-if="referenciaPartes(item).ancestrais.length" class="text-grey-6">
                    {{ referenciaPartes(item).ancestrais.join(', ') }},
                  </span>
                  <span class="text-weight-bold text-primary">{{ referenciaPartes(item).atual }}</span>
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

    <!-- Texto sugerido para a portaria de alteração (NSCA 5-3, Art. 22) -->
    <q-dialog v-model="dialogTextoSugerido">
      <q-card style="min-width:560px;max-width:720px;width:100%">
        <q-card-section class="row items-center q-pb-none">
          <q-icon name="mdi-file-document-edit-outline" color="primary" size="24px" class="q-mr-sm" />
          <span class="text-h6">Texto Sugerido da Portaria</span>
        </q-card-section>
        <q-card-section class="q-pt-sm q-pb-none">
          <div class="text-caption text-grey-7">
            Rascunho gerado automaticamente conforme o Art. 22 da NSCA 5-3 — revise antes de usar.
            Não implementa a compactação com linha pontilhada para o caso em que o caput e o
            dispositivo seguinte de um mesmo artigo são ambos preservados (Art. 22, VI-c-2).
          </div>
        </q-card-section>
        <q-card-section class="q-pt-md">
          <q-input
            :model-value="textoSugerido"
            type="textarea"
            outlined
            readonly
            autogrow
            input-class="texto-sugerido-mono"
          />
        </q-card-section>
        <q-card-actions align="right" class="q-pb-md q-px-md">
          <q-btn flat label="Fechar" v-close-popup />
          <q-btn unelevated color="primary" label="Copiar" icon="mdi-content-copy" @click="copiarTextoSugerido" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useDocumentsStore } from '@/stores/documents.js'
import StatusBadge from '@/components/common/StatusBadge.vue'
import DiffViewer from '@/components/comparison/DiffViewer.vue'
import { formatReferenciaLabel } from '@/utils/numbering.js'
import { gerarTextoSugeridoPortaria } from '@/utils/textoSugeridoPortaria.js'
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
    // referenciaPartes() nunca encontra o elemento na árvore e cai no fallback
    // "#id". Só pula o fetch completo quando as seções já estiverem carregadas.
    if (!store.getById(route.params.id)?.secoes) {
      await store.fetchDocumento(route.params.id)
    }
    await store.fetchMapaAlteracao(route.params.id)
    await store.fetchPortarias(route.params.id)
  } finally {
    loading.value = false
  }
})

const documento = computed(() => store.getById(route.params.id))
const mapaAlteracao = computed(() => store.mapaAlteracaoPorDocumento[String(route.params.id)] ?? [])
const portarias = computed(() => store.portariasPorDocumento[String(route.params.id)] ?? [])

const docId = computed(() => documento.value?.codigo_documento ?? '')

const docLabel = computed(() => {
  const d = documento.value
  if (!d) return ''
  return docId.value + (d.assunto_basico ? ` — ${d.assunto_basico}` : '')
})

// Só Rascunho/Minuta oferecem o atalho de voltar para o editor pelo
// breadcrumb -- as demais situações não têm edição direta de conteúdo (ver
// "Regra de imutabilidade" no README).
const podeEditar = computed(() => ['RASCUNHO', 'MINUTA'].includes(documento.value?.status))

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
// junto da cadeia de ancestrais (capítulo/seção/... ou artigo/parágrafo/...), para
// reaproveitar a numeração já calculada (formatReferenciaLabel) — o histórico de
// emenda não guarda tipo/número do elemento, só o conteúdo antes/depois.
function findElementoComAncestrais(secaoBackend, elementoId) {
  const tipoFrontend = SECAO_TIPO_FRONTEND[secaoBackend]
  const secao = documento.value?.secoes?.find(s => s.tipo === tipoFrontend)
  if (!secao) return null

  function dfs(elementos, ancestrais) {
    for (const el of elementos) {
      if (String(el.id) === String(elementoId)) return { elemento: el, ancestrais }
      if (el.filhos?.length) {
        const achado = dfs(el.filhos, [...ancestrais, el])
        if (achado) return achado
      }
    }
    return null
  }

  return dfs(secao.elementos ?? [], [])
}

// Referência isolada (ex.: "Inciso I") não identifica o elemento fora do contexto
// do artigo/agrupamento — por isso a referência inclui a cadeia de ancestrais
// relevante: Art./§/Inciso/Alínea/Subalínea para repartições de artigo, ou
// Capítulo/Seção/Subseção para agrupamentos. Cada família filtra só os ancestrais
// do próprio tipo (ex.: uma alínea nunca herda capítulo/seção, que ficam acima do
// artigo, fora do que o leitor precisa para localizá-la dentro DELE).
const TIPOS_REPARTICAO_ARTIGO = new Set(['artigo', 'paragrafo', 'paragrafo_unico', 'inciso', 'alinea', 'sub_alinea'])
const TIPOS_AGRUPAMENTO = new Set(['capitulo', 'secao_normativa', 'subsecao_normativa'])

// Separa a referência em ancestrais (contexto — exibidos em tom neutro) e o
// elemento que de fato sofreu a alteração (destacado), para que a tela e o PDF
// deem ênfase visual a quem importa na linha, não à cadeia inteira.
function referenciaPartes(item) {
  const achado = findElementoComAncestrais(item.secao, item.elementoId)
  if (!achado) {
    return {
      ancestrais: [],
      atual: (SECAO_LABELS[item.secao] ?? item.secao) + ' — ' + (item.tituloNovo ?? item.tituloAnterior ?? `#${item.elementoId}`),
    }
  }
  const { elemento, ancestrais } = achado
  let familia = null
  if (TIPOS_REPARTICAO_ARTIGO.has(elemento.tipo)) familia = TIPOS_REPARTICAO_ARTIGO
  else if (TIPOS_AGRUPAMENTO.has(elemento.tipo)) familia = TIPOS_AGRUPAMENTO
  const relevantes = familia ? ancestrais.filter(a => familia.has(a.tipo)) : []
  return {
    ancestrais: relevantes.map(formatReferenciaLabel),
    atual: formatReferenciaLabel(elemento),
  }
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
    itens: itensCiclo.value.map(item => {
      const { ancestrais, atual } = referenciaPartes(item)
      return {
        referenciaAncestrais: ancestrais.join(', '),
        referenciaAtual: atual,
        acao: item.acao,
        textoAnterior: item.textoAnterior,
        textoNovo: item.textoNovo,
        justificativa: item.justificativa,
      }
    }),
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

// ── Texto sugerido da portaria de alteração (NSCA 5-3, Art. 22) ────────────────
// Sempre sobre o ciclo PENDENTE (ainda não publicado), independente do ciclo
// selecionado no seletor da tela (que pode estar mostrando um ciclo antigo já
// publicado) -- é o ciclo que a próxima portaria vai de fato republicar.
// Geração em si vive em utils/textoSugeridoPortaria.js, compartilhada com
// DocumentViewerPage.vue.
const itensCicloPendente = computed(() =>
  mapaAlteracao.value.filter(item => item.cicloReferencia == null)
)

const dialogTextoSugerido = ref(false)
const textoSugerido = ref('')

function abrirTextoSugerido() {
  textoSugerido.value = gerarTextoSugeridoPortaria({
    documento: documento.value,
    itensCicloPendente: itensCicloPendente.value,
    portarias: portarias.value,
    docLabel: docLabel.value,
  })
  dialogTextoSugerido.value = true
}

async function copiarTextoSugerido() {
  try {
    await navigator.clipboard.writeText(textoSugerido.value)
    $q.notify({ type: 'positive', message: 'Texto copiado.' })
  } catch {
    $q.notify({ type: 'negative', message: 'Não foi possível copiar automaticamente. Selecione o texto manualmente.' })
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
:deep(.texto-sugerido-mono) {
  font-family: 'Courier New', monospace;
  font-size: 0.85rem;
  white-space: pre-wrap;
}
</style>
