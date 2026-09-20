<template>
  <q-page class="viewer-page">

    <!-- Topbar -->
    <div class="viewer-topbar row items-center q-px-xl q-py-md" style="gap:12px">
      <div class="col">
        <q-breadcrumbs active-color="primary" style="font-size:13px">
          <template v-slot:separator>
            <q-icon name="mdi-chevron-right" size="16px" color="primary" />
          </template>
          <q-breadcrumbs-el :to="{ name: 'home' }" icon="mdi-home" />
          <q-breadcrumbs-el :label="origemCrumb.label" :to="origemCrumb.to" />
          <q-breadcrumbs-el :label="docLabel" />
          <q-breadcrumbs-el
            v-if="podeEditar"
            label="Editar"
            icon="mdi-pencil-outline"
            :to="{ name: 'documento-editar', params: { id: documentoId }, query: route.query }"
          />
        </q-breadcrumbs>
        <div v-if="documento?.titulo" class="text-body2 text-grey-7 q-mt-xs">{{ documento.titulo }}</div>
      </div>

      <StatusBadge v-if="documento" :situacao-bca="documento.situacao_bca" :situacao-local="documento.situacao_local" />

      <q-separator vertical style="height:36px" />

      <q-btn outline color="primary" size="sm"
        :to="{ name: 'documento-comparar', params: { id: documentoId } }">
        <q-icon left name="mdi-source-branch" />
        Versões
      </q-btn>

      <q-btn
        v-if="documento && ehAlteracaoPublicada(documento)"
        outline color="primary" size="sm"
        @click="abrirTextoSugerido"
      >
        <q-icon left name="mdi-file-document-edit-outline" />
        Texto Sugerido
      </q-btn>

      <BotaoBaixarVersao
        label="PDF" icon="mdi-file-pdf-box" testid="baixar-pdf"
        :loading="pdfLoading"
        :tem-vigente="!!documento && temVersaoVigente(documento)"
        :tem-tramitacao="!!documento && temVersaoEmTramitacao(documento)"
        @baixar="baixarPdf"
      />

      <BotaoBaixarVersao
        label="HTML" icon="mdi-language-html5" testid="baixar-html"
        :loading="htmlLoading"
        :tem-vigente="!!documento && temVersaoVigente(documento)"
        :tem-tramitacao="!!documento && temVersaoEmTramitacao(documento)"
        @baixar="baixarHtml"
      />

      <q-btn outline color="primary" size="sm" @click="clonar">
        <q-icon left name="mdi-content-copy" />
        Clonar
      </q-btn>
    </div>

    <q-separator />

    <!-- Sections -->
    <div class="viewer-body q-px-xl q-py-lg">
      <div class="q-gutter-y-sm">

        <!-- 1. Informações do documento (aberta por padrão) -->
        <q-card flat class="section-card">
          <q-expansion-item
            v-model="expanded.info"
            icon="mdi-information-outline"
            label="Informações do Documento"
            header-class="text-primary text-weight-medium"
          >
            <q-separator />
            <q-card-section v-if="documento" class="q-pa-lg">
              <div class="row items-stretch q-col-gutter-y-lg">

                <!-- Metadados principais -->
                <div class="col-12 col-md" style="min-width: 0">
                  <div class="row q-col-gutter-md q-pr-lg">
                    <div class="col-6">
                      <div class="info-label">Espécie</div>
                      <div class="info-value">{{ documento.especie || '—' }}</div>
                    </div>
                    <div class="col-6">
                      <div class="info-label">Número</div>
                      <div class="info-value text-primary text-weight-medium">
                        {{ documento.especie }} {{ documento.numero_basico }}<template v-if="documento.numero_secundario">-{{ documento.numero_secundario }}</template>
                      </div>
                    </div>
                    <div class="col-12">
                      <div class="info-label">Título</div>
                      <div class="info-value">{{ documento.titulo || '—' }}</div>
                    </div>
                    <div class="col-12">
                      <div class="info-label">Assunto Básico</div>
                      <div class="info-value">{{ documento.assunto_basico || '—' }}</div>
                    </div>
                    <div class="col-6">
                      <div class="info-label">Situação BCA</div>
                      <StatusBadge :situacao-bca="documento.situacao_bca" mostrar="bca" class="q-mt-xs" data-testid="info-situacao-bca" />
                    </div>
                    <div class="col-6">
                      <div class="info-label">Situação Local</div>
                      <StatusBadge :situacao-local="documento.situacao_local" mostrar="local" class="q-mt-xs" data-testid="info-situacao-local" />
                    </div>
                    <div class="col-6">
                      <div class="info-label">Código</div>
                      <div class="info-value">{{ documento.codigo_documento || '—' }}</div>
                    </div>
                  </div>
                </div>

                <!-- Divisor semântico: histórico é a única parte que rola, os
                     metadados à esquerda permanecem com altura fixa. A row usa
                     q-col-gutter-y-lg (só espaçamento vertical, para o empilhamento
                     em telas estreitas) em vez de q-col-gutter-xl -- um gutter
                     horizontal aplicaria padding-left só no lado esquerdo de cada
                     filho, o que descentralizaria esse separador de 1px dentro do
                     próprio col-auto. Para o separador ficar de fato centralizado no
                     card, os dois lados (col-12 col-md) precisam ter exatamente o
                     mesmo peso de flex — por isso o espaçamento assimétrico
                     (q-pr-lg / 200px) fica num wrapper INTERNO de cada lado, nunca
                     como padding do próprio flex item: padding no item vira parte da
                     largura mínima de conteúdo dele e quebra a divisão 50/50, mesmo
                     com min-width:0. -->
                <div class="col-auto flex items-stretch gt-sm">
                  <q-separator vertical inset />
                </div>

                <!-- Histórico de situação -->
                <div class="col-12 col-md" style="min-width: 0">
                  <div class="historico-indent">
                    <div class="info-label q-mb-sm">Histórico de Situação</div>
                    <q-scroll-area v-if="timelineEventos.length" style="height: 320px" class="timeline-area">
                      <!-- q-pl-sm: os ícones do q-timeline (layout dense) sangram um
                           pouco à esquerda da própria caixa; sem essa folga, a borda
                           do q-scroll-area corta a lateral esquerda dos ícones. -->
                      <q-timeline color="primary" layout="dense" class="q-pl-sm">
                        <q-timeline-entry
                          v-for="evento in timelineEventos"
                          :key="evento.key"
                          :title="evento.titulo"
                          :subtitle="evento.data"
                          :icon="evento.icon"
                          :color="evento.color"
                        />
                      </q-timeline>
                    </q-scroll-area>
                    <div v-else class="text-grey-6 text-body2 text-center q-py-md">
                      Nenhum registro de histórico.
                    </div>
                  </div>
                </div>

              </div>
            </q-card-section>
            <div v-else class="text-grey-6 text-body2 text-center q-py-md">
              <q-spinner size="24px" class="q-mr-sm" />
              Carregando informações...
            </div>
          </q-expansion-item>
        </q-card>

        <!-- 2. Portarias (edição, alterações e revogação -- fechada por padrão) -->
        <q-card flat class="section-card">
          <q-expansion-item
            v-model="expanded.portarias"
            icon="mdi-file-certificate-outline"
            label="Portarias"
            header-class="text-primary text-weight-medium"
          >
            <q-separator />
            <q-card-section class="q-pa-lg">
              <q-list v-if="portarias.length" separator bordered class="rounded-borders">
                <q-item v-for="p in portarias" :key="p.id">
                  <q-item-section avatar>
                    <q-icon name="mdi-file-certificate-outline" color="primary" />
                  </q-item-section>
                  <q-item-section>
                    <q-item-label class="text-weight-medium">{{ labelPortaria(p) }}</q-item-label>
                    <q-item-label caption>
                      {{ p.orgao }}<template v-if="p.setor">/{{ p.setor }}</template>
                      n° {{ p.numeroPortaria }}, de {{ formatarDataSimples(p.dataPortaria) }}
                      — BCA n° {{ p.numeroBca }}, de {{ formatarDataSimples(p.dataBca) }}
                    </q-item-label>
                  </q-item-section>
                  <q-item-section side>
                    <q-btn
                      flat round dense
                      icon="mdi-download-outline"
                      color="primary"
                      :href="p.urlPdf"
                      target="_blank"
                    >
                      <q-tooltip anchor="top middle" self="bottom middle">Baixar PDF</q-tooltip>
                    </q-btn>
                  </q-item-section>
                </q-item>
              </q-list>
              <div v-else class="text-grey-6 text-body2 text-center q-py-md">
                Nenhuma portaria registrada.
              </div>
            </q-card-section>
          </q-expansion-item>
        </q-card>

        <!-- 3. Visualização do documento (fechada por padrão) -->
        <q-card flat class="section-card">
          <q-expansion-item
            v-model="expanded.preview"
            icon="mdi-file-document-outline"
            label="Visualização do Documento"
            header-class="text-primary text-weight-medium"
          >
            <q-separator />
            <!-- Duas versões possíveis: a EM TRAMITAÇÃO (a etapa local em curso; padrão quando
                 existe) e a VIGENTE (a da Situação BCA). Sem etapa em curso só há a vigente. -->
            <q-card-section
              v-if="documento && temVersaoVigente(documento) && temVersaoEmTramitacao(documento)"
              class="q-py-sm row items-center" style="gap:12px"
            >
              <span class="text-caption text-grey-7">Versão exibida:</span>
              <q-btn-toggle
                v-model="versaoSelecionada"
                no-caps unelevated dense
                toggle-color="primary" color="grey-3" text-color="grey-8"
                data-testid="seletor-versao"
                :options="[
                  { value: 'TRAMITACAO', label: 'Em tramitação' },
                  { value: 'VIGENTE', label: 'Vigente (BCA)' },
                ]"
              />
            </q-card-section>
            <q-banner v-else-if="documento && temVersaoEmTramitacao(documento)" dense class="bg-blue-1 text-blue-10">
              Versão em tramitação. Ainda não há versão vigente: o documento não foi publicado.
            </q-banner>
            <q-card-section class="q-pa-none pdf-section">
              <iframe
                v-if="iframePdfSrc"
                :src="iframePdfSrc"
                class="pdf-viewer"
                title="Visualização do documento"
                @load="pdfIframeLoading = false"
              />
              <div v-else-if="erroPdf" class="column items-center q-py-xl text-grey-6">
                <q-icon name="mdi-file-pdf-box" size="64px" class="q-mb-md" color="grey-4" />
                <div class="text-body1 text-weight-medium q-mb-xs">PDF não disponível</div>
                <div class="text-body2 text-center text-grey-5" style="max-width:480px">{{ erroPdf }}</div>
                <q-btn flat color="primary" icon="mdi-refresh" label="Tentar novamente" class="q-mt-md" data-testid="pdf-tentar-novamente" ="carregarPdf" />
              </div>

              <q-inner-loading :showing="!documento || pdfIframeLoading" data-testid="pdf-carregando">
                <q-spinner-gears size="56px" color="primary" />
                <div class="text-caption text-grey-7 q-mt-sm">Gerando a visualização do documento...</div>
              </q-inner-loading>
            </q-card-section>
          </q-expansion-item>
        </q-card>

        <!-- 4. Histórico de versões (fechada por padrão) -->
        <q-card flat class="section-card">
          <q-expansion-item
            v-model="expanded.versoes"
            icon="mdi-source-branch"
            label="Histórico de Versões"
            header-class="text-primary text-weight-medium"
          >
            <q-separator />
            <q-card-section class="q-pa-lg">
              <div class="column items-center q-py-xl text-grey-6">
                <q-icon name="mdi-source-branch" size="64px" class="q-mb-md" color="grey-4" />
                <div class="text-body2 q-mb-md">Compare as versões do documento lado a lado.</div>
                <q-btn outline color="primary"
                  :to="{ name: 'documento-comparar', params: { id: documentoId } }">
                  <q-icon left name="mdi-source-branch" />
                  Abrir comparação de versões
                </q-btn>
              </div>
            </q-card-section>
          </q-expansion-item>
        </q-card>

      </div>
    </div>

    <!-- Modal: confirmação de clonagem -->
    <q-dialog v-model="dialogClone.open" persistent>
      <q-card style="min-width:360px">
        <q-card-section class="row items-center q-pb-none">
          <q-icon name="mdi-content-copy" color="primary" size="24px" class="q-mr-sm" />
          <span class="text-h6">Clonar documento</span>
        </q-card-section>
        <q-card-section class="q-pt-md">
          <div class="text-body2 text-grey-8 q-mb-xs">
            Deseja clonar o documento abaixo?
          </div>
          <div class="text-weight-medium">{{ docLabel }}</div>
          <div class="text-caption text-grey-6 q-mt-sm">
            Uma cópia em <strong>RASCUNHO</strong> será criada e aberta para edição.
          </div>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancelar" color="grey-7" v-close-popup />
          <q-btn unelevated label="Clonar" color="primary" @click="executarClone" />
        </q-card-actions>
      </q-card>
    </q-dialog>

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
import { ref, computed, reactive, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useDocumentosStore } from '@/stores/documentos.js'
import { useAuthStore } from '@/stores/auth.js'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { gerarPdf, gerarHtml, buscarPdfBlob } from '@/services/pdfService.js'
import { gerarTextoSugeridoPortaria } from '@/utils/textoSugeridoPortaria.js'
import { resolveMinioUrls } from '@/utils/minioUrls.js'
import BotaoBaixarVersao from '@/components/common/BotaoBaixarVersao.vue'
import { itensRenumeracaoUnico } from '@/utils/numbering.js'
import {
  ehAlteracaoPublicada, temVersaoVigente, temVersaoEmTramitacao, versaoPadrao, eventoDoHistorico,
} from '@/utils/fluxoDocumento.js'

const route    = useRoute()
const router   = useRouter()
const $q       = useQuasar()
const docStore = useDocumentosStore()
const auth     = useAuthStore()

const pdfLoading = ref(false)
const htmlLoading = ref(false)

// Só a primeira seção aberta por padrão
const expanded = reactive({
  info:      true,
  portarias: false,
  preview:   false,
  versoes:   false,
})

const documentoId = computed(() => route.params.id)
const documento   = computed(() => docStore.getById(documentoId.value))

// Versão exibida no iframe: a em tramitação (se houver) é a padrão; senão, a vigente. O PDF só
// é buscado com a seção aberta: a versão em tramitação é renderizada na hora pelo backend quando
// o texto ainda muda (Apache FOP é pesado), e ligá-lo à simples abertura desta tela -- que
// qualquer usuário faz a qualquer momento -- já sobrecarregou o backend antes.
const versaoSelecionada = ref(null)
const iframePdfSrc = ref(null)
const pdfIframeLoading = ref(false)
const erroPdf = ref('')
let pdfObjectUrl = null
let pdfRequisicao = 0

watch(documento, (doc) => {
  if (doc && versaoSelecionada.value == null) versaoSelecionada.value = versaoPadrao(doc)
}, { immediate: true })

function liberarPdf() {
  if (pdfObjectUrl) URL.revokeObjectURL(pdfObjectUrl)
  pdfObjectUrl = null
  iframePdfSrc.value = null
}

async function carregarPdf() {
  const doc = documento.value
  if (!doc || !expanded.preview || !versaoSelecionada.value) return
  const requisicao = ++pdfRequisicao
  liberarPdf()
  erroPdf.value = ''
  pdfIframeLoading.value = true
  try {
    const blob = await buscarPdfBlob(doc.id, versaoSelecionada.value)
    if (requisicao !== pdfRequisicao) return // o usuário já trocou de versão
    pdfObjectUrl = URL.createObjectURL(blob)
    iframePdfSrc.value = pdfObjectUrl
  } catch (e) {
    if (requisicao !== pdfRequisicao) return
    pdfIframeLoading.value = false
    erroPdf.value = e?.message ?? 'Erro ao carregar o PDF.'
  }
}
watch([() => expanded.preview, versaoSelecionada, () => documento.value?.id], carregarPdf)
onBeforeUnmount(liberarPdf)

const docLabel = computed(() => {
  const d = documento.value
  if (!d) return 'Documento'
  const num = [d.numero_basico, d.numero_secundario].filter(Boolean).join('-')
  return [d.especie, num].filter(Boolean).join(' ') || 'Documento'
})

// Rascunho/Minuta oferecem o atalho de voltar para o editor pelo breadcrumb
// por posse; EM_REVISAO oferece o mesmo atalho só para o revisor atribuído
// (ver DocumentoAcessoService.podeEditar/isReadonly em DocumentoEditorPage.vue)
// -- as demais situações não têm edição direta de conteúdo (ver "Regra de
// imutabilidade" no README).
const podeEditar = computed(() => {
  const doc = documento.value
  if (!doc) return false
  if (doc.situacao_local === 'EM_REVISAO') {
    return doc.situacao_bca !== 'PUBLICADO' && doc.revisor_atribuido_id === String(auth.usuario?.id)
  }
  return ['RASCUNHO', 'MINUTA'].includes(doc.situacao_local)
})

// Ver comentário equivalente em DocumentoEditorPage.vue -- quando aberto a
// partir da fila pessoal de Revisão/Publicação (query `origem`), o breadcrumb
// do meio volta pra lá em vez de pro acervo geral.
const ORIGEM_CRUMB = {
  revisao:    { label: 'Revisão',    to: { name: 'revisao' } },
  publicacao: { label: 'Publicação', to: { name: 'publicacao' } },
  busca:      { label: 'Busca Textual', to: { name: 'busca' } },
}
const origemCrumb = computed(() => ORIGEM_CRUMB[route.query.origem] ?? { label: 'Documentos', to: { name: 'home' } })
// O histórico vem do log de transições (t_historico_documento), não de um timestamp único por
// situação: as etapas locais se repetem a cada alteração. Ver eventoDoHistorico (utils/fluxoDocumento.js).

const historico = computed(() => docStore.historicoPorDocumento[String(documentoId.value)] ?? [])
const portariasBrutas = computed(() => docStore.portariasPorDocumento[String(documentoId.value)] ?? [])

// urlPdf de cada portaria também é uma URL "canônica" do MinIO (bucket privado) --
// resolve todas de uma vez (um único round-trip) sempre que a lista mudar.
const portarias = ref([])
watch(portariasBrutas, async (lista) => {
  if (!lista.length) { portarias.value = []; return }
  const mapa = await resolveMinioUrls(lista.map(p => p.urlPdf))
  portarias.value = lista.map(p => ({ ...p, urlPdf: mapa.get(p.urlPdf) ?? p.urlPdf }))
}, { immediate: true })

// EDICAO e REVOGACAO só ocorrem uma vez por documento, sem numeração; cada
// ALTERACAO é numerada automaticamente pelo backend (numeroSequencial), mas
// a primeira delas (1) some do rótulo -- só a 2ª em diante aparece numerada.
function labelPortaria(p) {
  if (p.tipo === 'EDICAO') return 'Portaria de Edição'
  if (p.tipo === 'REVOGACAO') return 'Portaria de Revogação'
  return p.numeroSequencial > 1 ? `${p.numeroSequencial}ª Portaria de Alteração` : 'Portaria de Alteração'
}

function formatarDataSimples(isoStr) {
  if (!isoStr) return '—'
  const [y, m, d] = String(isoStr).split('-')
  return `${d}/${m}/${y}`
}

const timelineEventos = computed(() => {
  return historico.value
    .filter(h => h.statusNovo)
    .slice()
    .sort((a, b) => String(a.dtRegistro).localeCompare(String(b.dtRegistro)))
    .map(h => ({
      key: h.id,
      ...eventoDoHistorico(h),
      data: formatarData(h.dtRegistro),
    }))
})

function formatarData(isoStr) {
  if (!isoStr) return '—'
  const [dataParte, horaParte] = String(isoStr).split('T')
  const [y, m, d] = dataParte.split('-')
  const dataFormatada = `${d}/${m}/${y}`
  if (!horaParte) return dataFormatada
  return `${dataFormatada} às ${horaParte.slice(0, 5)}h`
}

onMounted(async () => {
  if (!documentoId.value) {
    router.replace({ name: 'home' })
    return
  }
  try {
    await docStore.fetchDocumento(documentoId.value)
    await docStore.fetchHistorico(documentoId.value)
    await docStore.fetchPortarias(documentoId.value)
    await docStore.fetchMapaAlteracao(documentoId.value)
  } catch (e) {
    console.error('[Viewer] Erro ao buscar documento:', e)
    $q.notify({ type: 'negative', message: 'Erro ao carregar documento.' })
  }
})

async function baixarPdf(versao) {
  if (!documento.value) return
  pdfLoading.value = true
  try {
    await gerarPdf(documento.value, versao)
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao gerar PDF: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    pdfLoading.value = false
  }
}

async function baixarHtml(versao) {
  if (!documento.value) return
  htmlLoading.value = true
  try {
    await gerarHtml(documento.value, versao)
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao gerar HTML: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    htmlLoading.value = false
  }
}

const dialogClone = reactive({ open: false })

function clonar() {
  if (!documento.value) return
  dialogClone.open = true
}

function executarClone() {
  dialogClone.open = false
  if (!documento.value) return
  docStore.cloneDocumento(documento.value.id).then(clone => {
    if (clone) router.push({ name: 'documento-editar', params: { id: clone.id } })
  })
}

// ── Texto sugerido da portaria de alteração (NSCA 5-3, Art. 22) ────────────────
// Geração em si vive em utils/textoSugeridoPortaria.js, compartilhada com
// ComparisonPage.vue. Sempre sobre o ciclo PENDENTE (ainda não publicado).
const mapaAlteracao = computed(() => [
  ...(docStore.mapaAlteracaoPorDocumento[String(documentoId.value)] ?? []),
  ...itensRenumeracaoUnico(documento.value),
])
const itensCicloPendente = computed(() => mapaAlteracao.value.filter(item => item.cicloReferencia == null))

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
.viewer-page {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 60px);
  background: var(--color-background);
}

.viewer-topbar {
  background: var(--color-surface);
}

.viewer-body {
  flex: 1;
  max-width: 1280px;
  width: 100%;
  margin: 0 auto;
}

.viewer-sections {
  border-radius: 8px;
  overflow: hidden;
}

.section-card {
  border: 1px solid rgba(0, 0, 0, 0.07);
  border-radius: 4px;
  overflow: hidden;
}

.pdf-section {
  position: relative;
  min-height: 200px;
}

.pdf-viewer {
  width: 100%;
  height: 80vh;
  border: none;
  display: block;
}

:deep(.q-timeline__subtitle) {
  text-transform: none;
}

/* Afastamento do histórico em relação ao separador central -- só a partir do
   breakpoint md (1024px, mesmo ponto do utilitário "gt-sm" no separador), já
   que abaixo disso as colunas empilham em largura cheia e esse recuo apertaria
   o conteúdo contra a borda da tela. */
.historico-indent {
  padding-left: 0;
}
@media (min-width: 1024px) {
  .historico-indent {
    padding-left: 200px;
  }
}

.info-label {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #90a4ae;
  font-weight: 600;
  margin-bottom: 4px;
}

.info-value {
  font-size: 15px;
  color: var(--color-on-surface, #1a1a2e);
}

:deep(.texto-sugerido-mono) {
  font-family: 'Calibri', 'Carlito', 'Segoe UI', Arial, sans-serif;
  font-size: 0.9rem;
  white-space: pre-wrap;
}
</style>
