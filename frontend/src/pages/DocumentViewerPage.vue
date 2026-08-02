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
          <q-breadcrumbs-el label="Documentos" />
          <q-breadcrumbs-el :label="docLabel" />
        </q-breadcrumbs>
        <div v-if="documento?.titulo" class="text-body2 text-grey-7 q-mt-xs">{{ documento.titulo }}</div>
      </div>

      <StatusBadge v-if="documento" :status="documento.status" />

      <q-separator vertical style="height:36px" />

      <q-btn outline color="primary" size="sm"
        :to="{ name: 'documento-comparar', params: { id: documentoId } }">
        <q-icon left name="mdi-source-branch" />
        Versões
      </q-btn>

      <q-btn outline color="deep-orange-7" size="sm" :loading="pdfLoading" @click="baixarPdf">
        <q-icon left name="mdi-file-pdf-box" />
        PDF
      </q-btn>

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
            <q-card-section class="q-pa-lg">
              <div v-if="documento" class="row q-col-gutter-xl">

                <!-- Metadados principais -->
                <div class="col-12 col-md-6">
                  <div class="row q-col-gutter-md">
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
                      <div class="info-label">Status atual</div>
                      <StatusBadge :status="documento.status" class="q-mt-xs" />
                    </div>
                    <div class="col-6">
                      <div class="info-label">Código</div>
                      <div class="info-value">{{ documento.codigo_documento || '—' }}</div>
                    </div>
                  </div>
                </div>

                <!-- Timeline de datas -->
                <div class="col-12 col-md-6">
                  <div class="info-label q-mb-sm">Histórico de Status</div>
                  <q-timeline color="primary" layout="dense">
                    <q-timeline-entry
                      v-for="evento in timelineEventos"
                      :key="evento.status"
                      :title="evento.titulo"
                      :subtitle="evento.data"
                      :icon="evento.icon"
                      :color="evento.color"
                    />
                  </q-timeline>
                </div>

              </div>
              <div v-else class="text-grey-6 text-body2 text-center q-py-md">
                <q-spinner size="24px" class="q-mr-sm" />
                Carregando informações...
              </div>
            </q-card-section>
          </q-expansion-item>
        </q-card>

        <!-- 2. Visualização do documento (fechada por padrão) -->
        <q-card flat class="section-card">
          <q-expansion-item
            v-model="expanded.preview"
            icon="mdi-file-document-outline"
            label="Visualização do Documento"
            header-class="text-primary text-weight-medium"
          >
            <q-separator />
            <q-card-section class="q-pa-none pdf-section">
              <iframe
                v-if="iframePdfSrc"
                :src="iframePdfSrc"
                class="pdf-viewer"
                title="Visualização do documento"
                @load="pdfIframeLoading = false"
              />
              <div v-else-if="documento" class="column items-center q-py-xl text-grey-6">
                <q-icon name="mdi-file-pdf-box" size="64px" class="q-mb-md" color="grey-4" />
                <div class="text-body1 text-weight-medium q-mb-xs">PDF não disponível</div>
                <div class="text-body2 text-center text-grey-5" style="max-width:480px">
                  O PDF é gerado automaticamente quando o documento é <strong>aprovado</strong>.
                  Use o botão <strong>PDF</strong> na barra superior para baixar o rascunho.
                </div>
              </div>

              <q-inner-loading :showing="!documento || pdfIframeLoading" />
            </q-card-section>
          </q-expansion-item>
        </q-card>

        <!-- 3. Histórico de versões (fechada por padrão) -->
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

  </q-page>
</template>

<script setup>
import { ref, computed, reactive, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useDocumentsStore } from '@/stores/documents.js'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { gerarPdf, pdfUrl } from '@/services/pdfService.js'

const route    = useRoute()
const router   = useRouter()
const $q       = useQuasar()
const docStore = useDocumentsStore()

const pdfLoading = ref(false)

// Só a primeira seção aberta por padrão
const expanded = reactive({
  info:    true,
  preview: false,
  versoes: false,
})

const STATUS_COM_PDF = new Set(['APROVADO', 'PUBLICADO', 'ARQUIVADO', 'REVOGADO'])

const documentoId = computed(() => route.params.id)
const documento   = computed(() => docStore.getById(documentoId.value))

const iframePdfSrc = computed(() => {
  const doc = documento.value
  if (!doc) return null
  if (doc.url_pdf) return doc.url_pdf
  if (STATUS_COM_PDF.has(doc.status)) return pdfUrl(documentoId.value)
  return null
})

const pdfIframeLoading = ref(false)
watch(iframePdfSrc, (src) => { pdfIframeLoading.value = !!src }, { immediate: true })

const docLabel = computed(() => {
  const d = documento.value
  if (!d) return 'Documento'
  const num = [d.numero_basico, d.numero_secundario].filter(Boolean).join('-')
  return [d.especie, num].filter(Boolean).join(' ') || 'Documento'
})

const STATUS_TIMELINE = [
  { campo: 'data_criacao',      titulo: 'Criado',     icon: 'mdi-file-plus-outline',    color: 'grey'       },
  { campo: 'data_minuta',       titulo: 'Minuta',     icon: 'mdi-file-edit-outline',    color: 'orange'     },
  { campo: 'data_aprovacao',    titulo: 'Aprovado',   icon: 'mdi-check-circle-outline', color: 'green'      },
  { campo: 'data_publicacao',   titulo: 'Publicado',  icon: 'mdi-publish',              color: 'primary'    },
  { campo: 'data_arquivamento', titulo: 'Arquivado',  icon: 'mdi-archive-outline',      color: 'blue-grey'  },
  { campo: 'data_revogacao',    titulo: 'Revogado',   icon: 'mdi-file-remove-outline',  color: 'brown'      },
  { campo: 'data_cancelamento', titulo: 'Cancelado',  icon: 'mdi-close-circle-outline', color: 'negative'   },
]

const timelineEventos = computed(() => {
  const d = documento.value
  if (!d) return []
  return STATUS_TIMELINE
    .filter(ev => d[ev.campo])
    .map(ev => ({
      ...ev,
      data: formatarData(d[ev.campo]),
    }))
})

function formatarData(isoStr) {
  if (!isoStr) return '—'
  const [y, m, d] = String(isoStr).slice(0, 10).split('-')
  return `${d}/${m}/${y}`
}

onMounted(async () => {
  if (!documentoId.value) {
    router.replace({ name: 'home' })
    return
  }
  try {
    await docStore.fetchDocumento(documentoId.value)
  } catch (e) {
    console.error('[Viewer] Erro ao buscar documento:', e)
    $q.notify({ type: 'negative', message: 'Erro ao carregar documento.' })
  }
})

async function baixarPdf() {
  if (!documento.value) return
  pdfLoading.value = true
  try {
    await gerarPdf(documento.value)
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao gerar PDF: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    pdfLoading.value = false
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
</style>
