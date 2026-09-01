<template>
  <div class="editor-page" style="height:calc(100vh - 60px)">

    <!-- Sidebar fixa, altura total, sem comportamento colapsável -->
    <EditorSidebar
      :documento="documento"
      :doc-label="docLabel"
      :secoes="documento?.secoes ?? []"
      :selected-id="editorStore.selectedElementId"
      :is-em-alteracao="isEmAlteracao"
      @select="editorStore.selectElement($event)"
      @move-up="onMoveUp"
      @move-down="onMoveDown"
      @add-child="onAddChild"
      @add-artigo="addArtigo"
      @add-capitulo="addCapitulo"
      @promote="onPromote"
      @demote="handleDemote"
      @remove="onRemove"
      @move-to-parent="onMoveToParent"
      @reorder-normativa="onReorderNormativa"
      @emenda-alterar="(id, secao) => abrirEmendaDialog(id, secao, 'ALTERAR')"
      @emenda-revogar="(id, secao) => abrirEmendaDialog(id, secao, 'REVOGAR')"
      @emenda-desfazer="(id, secao) => abrirEmendaDialog(id, secao, 'DESFAZER')"
      @emenda-incluir="(secao, elementos) => abrirIncluirDialog(secao, elementos)"
      @reordenar-incluido="onReordenarIncluido"
    />

    <!-- Dialog de emenda (alterar / revogar / desfazer) -->
    <EmendaDialog
      v-if="isEmAlteracao"
      v-model="emendaDialogOpen"
      :acao="emendaAcao"
      :elemento="emendaElemento"
      :secao="emendaSecao"
      :documento-id="documentoId"
    />

    <!-- Dialog de inclusão de novo elemento por emenda -->
    <IncluirElementoDialog
      v-if="isEmAlteracao"
      v-model="incluirDialogOpen"
      :secao="incluirSecao"
      :documento-id="documentoId"
      :elementos="incluirElementos"
    />

    <!-- Dialog de referência: técnica legislativa (LC 95/1998) -->
    <Lc95HelpDialog v-model="lc95DialogOpen" />

    <!-- Dialog de compartilhamento (coautoria) -->
    <CompartilharDialog v-if="documentoId" v-model="compartilharDialogOpen" :documento-id="documentoId" />

    <!-- Coluna principal: topbar + área de edição -->
    <div class="editor-main column">

      <!-- Top action bar (não cobre a sidebar) -->
      <div class="editor-topbar q-px-md q-py-sm row items-center" style="gap:12px">
        <q-breadcrumbs active-color="primary" style="font-size:13px">
          <template v-slot:separator>
            <q-icon name="mdi-chevron-right" size="16px" color="primary" />
          </template>
          <q-breadcrumbs-el :to="{ name: 'home' }" icon="mdi-home" />
          <q-breadcrumbs-el label="Documentos" />
          <q-breadcrumbs-el :label="docLabel" />
          <q-breadcrumbs-el v-if="selectedElement" :label="selectedElementLabel" />
        </q-breadcrumbs>

        <!-- Indicador de salvamento -->
        <div class="save-indicator" :class="saveIndicatorClass">
          <template v-if="saveStatus === 'saving'">
            <q-circular-progress indeterminate size="13px" :thickness="0.35" />
            <span>Salvando…</span>
          </template>
          <template v-else-if="saveStatus === 'error'">
            <q-icon size="15px" name="mdi-alert-circle-outline" />
            <span>Não salvo</span>
          </template>
          <template v-else>
            <q-icon size="15px" name="mdi-check-circle-outline" />
            <span>Salvo</span>
          </template>
        </div>

        <q-space />

        <q-btn round flat color="primary" @click="lc95DialogOpen = true">
          <q-icon name="mdi-help-circle-outline" size="22px" />
          <q-tooltip anchor="bottom middle" self="top middle">Técnica legislativa (LC 95/1998)</q-tooltip>
        </q-btn>

        <q-btn round flat color="primary" @click="compartilharDialogOpen = true">
          <q-icon name="mdi-account-multiple-plus-outline" size="22px" />
          <q-badge v-if="presencaOutros.length" floating color="deep-orange" rounded />
          <q-tooltip anchor="bottom middle" self="top middle">Compartilhar</q-tooltip>
        </q-btn>

        <q-btn
          outline
          color="primary"
          :to="{ name: 'documento-comparar', params: { id: documentoId } }"
        >
          <q-icon left name="mdi-source-branch" />
          Comparar versões
        </q-btn>

        <q-btn
          outline
          color="deep-orange-7"
          :loading="pdfLoading"
          @click="baixarPdf"
        >
          <q-icon left name="mdi-file-pdf-box" />
          PDF
        </q-btn>
      </div>

      <q-separator />

      <!-- Banner de modo EM ALTERAÇÃO -->
      <div v-if="isEmAlteracao" class="q-px-md q-py-xs row items-center" style="background:#FFF3E0;border-bottom:1px solid #FFCC80;gap:8px">
        <q-icon name="mdi-pencil-lock-outline" color="deep-orange-8" size="16px" />
        <span class="text-caption text-deep-orange-9 text-weight-bold">
          MODO EM ALTERAÇÃO — Use os botões de emenda no painel lateral para alterar ou revogar elementos.
          Salvar automático está desativado.
        </span>
      </div>

      <!-- Aviso de presença: alguém mais está editando este documento agora.
           Só avisa -- não bloqueia nem resolve a colisão por si (ver
           DocumentoConcorrenciaService no backend para a checagem que de fato
           impede sobrescrever por cima). -->
      <div v-if="presencaOutros.length" class="q-px-md q-py-xs row items-center" style="background:#FFF3E0;border-bottom:1px solid #FFCC80;gap:8px">
        <q-icon name="mdi-account-alert-outline" color="deep-orange-8" size="16px" />
        <span class="text-caption text-deep-orange-9 text-weight-bold">
          {{ presencaOutros.map(p => p.nome).join(', ') }}
          {{ presencaOutros.length === 1 ? 'também está editando' : 'também estão editando' }} este documento agora.
        </span>
      </div>

      <!-- Área principal: editor + preview -->
      <div class="editor-body row col" style="overflow:hidden">

        <!-- Content area -->
        <div class="editor-content q-pa-lg" style="overflow-y:auto">

        <!-- No element selected -->
        <div v-if="!editorStore.selectedElementId" class="column items-center justify-center text-grey-7" style="height:100%">
          <q-icon size="80px" class="q-mb-md" color="blue-grey-3" name="mdi-cursor-pointer" />
          <p class="text-h6">Selecione um elemento no painel esquerdo</p>
          <p class="text-body2">Clique em qualquer seção ou artigo para editá-lo aqui.</p>
        </div>

        <!-- Element editor -->
        <template v-else-if="selectedElement">
          <div class="element-editor">

            <!-- Element breadcrumb/label -->
            <div class="element-header q-mb-md">
              <div class="row items-center justify-between q-mt-sm">
                <div class="row items-center" style="gap:8px">
                  <q-icon :name="elementIcon(selectedElement.tipo)" color="primary" size="20px" />
                  <h2 class="text-h6 text-weight-bold text-primary q-my-none">
                    {{ formatLabel(selectedElement) }}
                  </h2>
                </div>
                <div class="row">
                  <q-btn
                    v-if="!isEmAlteracao"
                    size="sm"
                    outline
                    color="negative"
                    class="q-ml-sm"
                    @click="editorStore.removeElement(selectedElement.id)"
                  >
                    <q-icon left name="mdi-delete-outline" />
                    Remover
                  </q-btn>
                </div>
              </div>
            </div>

            <!-- Título editor para capítulo / seção / subseção -->
            <template v-if="isGroupingEl">
              <q-input
                :model-value="selectedElement.titulo"
                :label="groupingLabel"
                outlined
                dense
                :readonly="isReadonly"
                @update:model-value="onTituloUpdate"
              />
              <p class="text-caption text-grey-7 q-mt-xs">
                O título aparecerá em maiúsculas no documento (NSCA 5-3).
              </p>
            </template>

            <!-- WYSIWYG editor para elementos de conteúdo. A chave inclui se a sala de
                 colaboração está disponível: muda exatamente UMA vez por elemento, no
                 instante em que ele ganha um id persistido (primeiro autosave), forçando
                 o único remount necessário para trocar do modo local para o Yjs -- ver
                 WysiwygEditor.vue. -->
            <WysiwygEditor
              v-else
              :key="`${selectedElement.id}:${elementoIdColaborativo ? 'collab' : 'local'}`"
              :model-value="wysiwygConteudo"
              :readonly="isReadonly"
              :documento-id="documentoId"
              :elemento-id="elementoIdColaborativo"
              @update:model-value="onContentUpdate"
            />

            <!-- Add child element shortcuts -->
            <div v-if="childOptions.length && !isReadonly" class="q-mt-md row items-center" style="flex-wrap:wrap">
              <span class="text-caption text-grey-7">Adicionar:</span>
              <q-btn
                v-for="opt in childOptions"
                :key="opt.tipo"
                size="sm"
                outline
                color="primary"
                class="q-ml-sm"
                @click="editorStore.addFilho(selectedElement.id, opt.tipo)"
              >
                <q-icon left :name="elementIcon(opt.tipo)" />
                {{ opt.label }}
              </q-btn>
              <q-btn
                v-if="!isGroupingEl"
                size="sm"
                outline
                class="q-ml-sm"
                @click="editorStore.addSibling(selectedElement.id, selectedElement.tipo)"
              >
                <q-icon left name="mdi-plus" />
                Mesmo nível
              </q-btn>
            </div>

          </div>
        </template>

      </div>

        <!-- PDF Preview panel -->
        <div class="preview-panel" style="overflow-y:auto; position:relative">
          <DocumentoPreview
            v-if="previewMounted && documento"
            :documento="documento"
            :selected-element-id="editorStore.selectedElementId"
          />
          <q-inner-loading :showing="!previewMounted || !documento" class="preview-inner-loading">
            <q-spinner-gears size="48px" color="grey-5" />
            <div class="text-caption text-grey-5 q-mt-sm">Carregando prévia…</div>
          </q-inner-loading>
        </div>

      </div><!-- /editor-body -->

    </div><!-- /editor-main -->

  </div><!-- /editor-page -->
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useEditorStore } from '@/stores/editor.js'
import { useDocumentosStore } from '@/stores/documentos.js'
import { useAuthStore } from '@/stores/auth.js'
import { formatLabel, elementIcon, renumberElements } from '@/utils/numbering.js'
import { gerarPdf } from '@/services/pdfService.js'
import EditorSidebar from '@/components/editor/EditorSidebar.vue'
import WysiwygEditor from '@/components/editor/WysiwygEditor.vue'
import DocumentoPreview from '@/components/editor/DocumentoPreview.vue'
import EmendaDialog from '@/components/editor/EmendaDialog.vue'
import IncluirElementoDialog from '@/components/editor/IncluirElementoDialog.vue'
import Lc95HelpDialog from '@/components/editor/Lc95HelpDialog.vue'
import CompartilharDialog from '@/components/editor/CompartilharDialog.vue'
import * as documentsApi from '@/api/documentos.js'
import { clientId } from '@/utils/clientId.js'

const route = useRoute()
const router = useRouter()
const $q = useQuasar()
const editorStore = useEditorStore()
const docStore = useDocumentosStore()
const auth = useAuthStore()

const previewMounted = ref(false)
const pdfLoading    = ref(false)
const lc95DialogOpen = ref(false)
const compartilharDialogOpen = ref(false)

// ── Presença de edição (aviso de colisão, não trava nada) ───────────────────────
// "Quem está editando agora" é literalmente "quem tem esta conexão SSE
// aberta" (ver DocumentoPresencaEmitterRegistry no backend) -- sem polling,
// sem heartbeat: a lista chega pronta a cada mudança (alguém entrou ou
// saiu), incluindo o próprio usuário, que é filtrado aqui antes de exibir.
const presencaOutros = ref([])
let presencaEventSource = null

function iniciarPresenca() {
  if (presencaEventSource || !documentoId.value || !auth.token) return

  presencaEventSource = new EventSource(documentsApi.presencaStreamUrl(documentoId.value, auth.token))
  presencaEventSource.addEventListener('presenca', (event) => {
    const todos = JSON.parse(event.data)
    const antes = new Set(presencaOutros.value.map(p => p.usuarioId))
    const outros = todos.filter(p => p.usuarioId !== auth.usuario?.id)

    // Toast só na TRANSIÇÃO (alguém que não estava editando começou a editar
    // agora), não a cada evento -- o banner permanente (ver template) já
    // cobre o estado contínuo.
    for (const p of outros) {
      if (!antes.has(p.usuarioId)) {
        $q.notify({ type: 'info', icon: 'mdi-account-multiple-outline', position: 'top-right',
          message: `${p.nome} começou a editar este documento agora.` })
      }
    }

    presencaOutros.value = outros
  })

  // Mesma conexão, evento diferente: outra pessoa criou/moveu/renomeou/excluiu um
  // elemento (PATCH /secoes, ver Fase 2 do plano de colaboração). `origem` é o
  // clientId de quem disparou a mudança -- se for o nosso próprio (já aplicamos
  // localmente antes de mandar a requisição, ver api/documentos.js), ignora; senão
  // aplica o patch incremental na árvore (nunca um reload completo, que
  // interromperia quem estiver editando ao vivo nesse instante).
  presencaEventSource.addEventListener('estrutura', (event) => {
    const { origem, eventos } = JSON.parse(event.data)
    if (origem === clientId) return
    const selecaoRemovida = editorStore.aplicarEventosEstrutura(eventos)
    if (selecaoRemovida) {
      $q.notify({
        type: 'warning',
        icon: 'mdi-file-remove-outline',
        position: 'bottom-right',
        message: 'O elemento que você estava editando foi excluído por outra pessoa.',
        timeout: 8000,
      })
    }
  })
  // onerror não precisa de tratamento manual: o browser reconecta o
  // EventSource sozinho enquanto o editor continuar aberto.
}

function pararPresenca() {
  presencaEventSource?.close()
  presencaEventSource = null
  presencaOutros.value = []
}

// ── Auto-save ────────────────────────────────────────────────────────────────
const saveStatus = ref('idle')   // 'idle' | 'saving' | 'error'
let autoSaveTimer = null

function scheduleAutoSave() {
  if (isEmAlteracao.value) return
  saveStatus.value = 'saving'
  clearTimeout(autoSaveTimer)
  autoSaveTimer = setTimeout(autoSave, 2000)
}

async function autoSave() {
  if (!editorStore.isDirty) {
    saveStatus.value = 'idle'
    return
  }
  saveStatus.value = 'saving'
  try {
    await editorStore.save()
    saveStatus.value = 'idle'
  } catch (e) {
    console.error('[AutoSave]', e)
    if (e?.status === 409) {
      // Outra pessoa (ou outra aba) salvou primeiro -- não insiste
      // sobrescrevendo por cima; recarrega (busca a versão real no servidor,
      // não do cache -- ver editorStore.reload) e avisa, igual ao design doc
      // previu para o cenário de colisão de edição concorrente. saveStatus só
      // volta a 'idle' depois que a versão atual chegou, senão o próximo
      // autosave repetiria a mesma versão desatualizada e colidiria de novo.
      await editorStore.reload()
      saveStatus.value = 'idle'
      $q.notify({
        type: 'warning',
        message: 'Este documento foi alterado por outra pessoa. A tela foi recarregada com a versão mais recente.',
        timeout: 8000,
      })
    } else {
      saveStatus.value = 'error'
    }
  }
}

onUnmounted(() => {
  clearTimeout(autoSaveTimer)
  if (editorStore.isDirty) editorStore.save()
  pararPresenca()
})
// ─────────────────────────────────────────────────────────────────────────────

const saveIndicatorClass = computed(() => {
  if (saveStatus.value === 'saving') return 'save-indicator--saving'
  if (saveStatus.value === 'error')  return 'save-indicator--dirty'
  return 'save-indicator--saved'
})

const documentoId    = computed(() => route.params.id)
const documento      = computed(() => editorStore.documento)
const selectedElement = computed(() => editorStore.selectedElement)

const isEmAlteracao = computed(() => documento.value?.status === 'EM_ALTERACAO')

const isReadonly = computed(() =>
  ['PUBLICADO', 'EM_ALTERACAO', 'ALTERADO', 'ARQUIVADO', 'CANCELADO', 'REVOGADO'].includes(documento.value?.status)
)

// Elemento ALTERADO por emenda: o texto vigente fica em conteudoEmenda, não em
// conteudo (que preserva o original para o tachado no preview). Demais status
// (INALTERADO, INCLUIDO, REVOGADO) usam conteudo normalmente.
const wysiwygConteudo = computed(() => {
  const el = selectedElement.value
  if (!el) return ''
  return el.emendaStatus === 'ALTERADO' ? el.conteudoEmenda : el.conteudo
})

// Sala de colaboração ao vivo só faz sentido quando o elemento já existe no backend
// (backendId/id numérico -- ver aplicarIdsPersistidos em api/documentos.js) E o
// documento está em RASCUNHO/MINUTA (fora disso já é isReadonly, e em EM_ALTERACAO
// o texto vigente mora em conteudoEmenda, um campo que o serviço de colaboração não
// conhece -- conectar lá reescreveria o campo errado). null aqui faz o
// WysiwygEditor cair no modo local antigo (sem Yjs).
const elementoIdColaborativo = computed(() => {
  if (isReadonly.value) return null
  const el = selectedElement.value
  if (!el) return null
  return el.backendId ?? documentsApi.idPersistido(el.id)
})

// ── Emenda dialog state ───────────────────────────────────────────────────────
const emendaDialogOpen  = ref(false)
const emendaAcao        = ref('ALTERAR')
const emendaElemento    = ref(null)
const emendaSecao       = ref('PARTE_NORMATIVA')

function abrirEmendaDialog(elementoId, secao, acao) {
  const el = editorStore.findElement(elementoId)
  if (!el) return
  emendaElemento.value   = el
  emendaSecao.value      = secao
  emendaAcao.value       = acao
  emendaDialogOpen.value = true
}

// ── Incluir elemento dialog state ────────────────────────────────────────────
const incluirDialogOpen = ref(false)
const incluirSecao      = ref('PARTE_NORMATIVA')
const incluirElementos  = ref([])

function abrirIncluirDialog(secao, elementos = []) {
  incluirSecao.value      = secao
  incluirElementos.value  = elementos
  incluirDialogOpen.value = true
}

async function onReordenarIncluido(elementoId, direcao) {
  try {
    await docStore.reordenarElementoEmenda(documentoId.value, 'PARTE_NORMATIVA', parseInt(elementoId, 10), direcao)
    editorStore.reload()
  } catch (e) {
    $q.notify({
      type: 'negative',
      message: `Erro ao reordenar: ${e?.message ?? 'erro desconhecido'}`,
      position: 'bottom-right',
      timeout: 4000,
    })
  }
}

const docLabel = computed(() => {
  const d = documento.value
  if (!d) return 'Novo Documento'
  const numStr = [d.numero_basico, d.numero_secundario].filter(Boolean).join('-')
  const num = [d.especie, numStr].filter(Boolean).join(' ')
  return num || 'Documento sem título'
})

const selectedElementLabel = computed(() => {
  const el = selectedElement.value
  if (!el) return ''
  return formatLabel(el)
})

const GROUPING_TIPOS = ['capitulo', 'secao_normativa', 'subsecao_normativa']

const isGroupingEl = computed(() =>
  GROUPING_TIPOS.includes(selectedElement.value?.tipo)
)

const groupingLabel = computed(() => {
  switch (selectedElement.value?.tipo) {
    case 'capitulo':           return 'Título do Capítulo'
    case 'secao_normativa':    return 'Título da Seção'
    case 'subsecao_normativa': return 'Título da Subseção'
    default: return 'Título'
  }
})

const CHILD_OPTIONS = {
  capitulo:           [
    { tipo: 'secao_normativa', label: 'Seção' },
    { tipo: 'artigo',          label: 'Artigo' },
  ],
  secao_normativa:    [
    { tipo: 'subsecao_normativa', label: 'Subseção' },
    { tipo: 'artigo',             label: 'Artigo' },
  ],
  subsecao_normativa: [{ tipo: 'artigo', label: 'Artigo' }],
  artigo:             [
    { tipo: 'paragrafo_unico', label: 'Parágrafo único' },
    { tipo: 'paragrafo',       label: 'Parágrafo (§)' },
    { tipo: 'inciso',          label: 'Inciso' },
  ],
  paragrafo_unico: [{ tipo: 'inciso', label: 'Inciso' }],
  paragrafo:       [{ tipo: 'inciso', label: 'Inciso' }],
  inciso:          [{ tipo: 'alinea', label: 'Alínea' }],
  alinea:          [{ tipo: 'sub_alinea', label: 'Sub-alínea' }],
}

const childOptions = computed(() => {
  const el = selectedElement.value
  return el ? (CHILD_OPTIONS[el.tipo] ?? []) : []
})

onMounted(async () => {
  if (documentoId.value) {
    // Tenta buscar do backend; se falhar, cai para versão em memória (recém-criada)
    let doc = null
    try {
      doc = await docStore.fetchDocumento(documentoId.value)
    } catch (e) {
      console.error('[Editor] Erro ao buscar documento do backend:', e)
    }
    if (!doc) {
      doc = docStore.getById(documentoId.value)
    }
    if (!doc) {
      router.replace({ name: 'home' })
      return
    }
    const ok = editorStore.load(documentoId.value)
    if (!ok) {
      router.replace({ name: 'home' })
      return
    }

    if (!['RASCUNHO', 'MINUTA', 'EM_ALTERACAO'].includes(doc.status)) {
      router.replace({ name: 'documento-visualizar', params: { id: documentoId.value } })
      return
    }

    if (doc._fromTemplate) {
      // Backend não tinha seções — salva o template imediatamente
      editorStore.isDirty = true
      await autoSave()
    }

    const loadedDoc = editorStore.documento
    const normativa = loadedDoc?.secoes?.find(s => s.tipo === 'parte_normativa')
    const primeiro = normativa?.elementos?.[0]
    if (primeiro) editorStore.selectElement(primeiro.id)

    iniciarPresenca()
  } else {
    editorStore.loadNew()
  }
  await nextTick()
  previewMounted.value = true
})

async function baixarPdf() {
  if (!documento.value) return
  pdfLoading.value = true
  try {
    if (editorStore.isDirty) {
      clearTimeout(autoSaveTimer)
      await autoSave()
    }
    await gerarPdf(documento.value)
  } catch (e) {
    console.error('[PDF]', e)
    $q.notify({
      type: 'negative',
      message: `Erro ao gerar PDF: ${e?.message ?? 'erro desconhecido'}`,
      position: 'bottom-right',
      timeout: 6000,
    })
  } finally {
    pdfLoading.value = false
  }
}

function onContentUpdate(html) {
  if (selectedElement.value) {
    editorStore.updateContent(selectedElement.value.id, html)
    scheduleAutoSave()
  }
}

function onTituloUpdate(titulo) {
  if (selectedElement.value) {
    editorStore.updateTitulo(selectedElement.value.id, titulo)
    scheduleAutoSave()
  }
}


function onReorderNormativa() {
  const secao = editorStore.normativaSecao
  if (secao) {
    renumberElements(secao.elementos)
    editorStore.markUserEdit()
    scheduleAutoSave()
  }
}

function onMoveUp(id)   { if (editorStore.moveUp(id).ok)   scheduleAutoSave() }
function onMoveDown(id) { if (editorStore.moveDown(id).ok) scheduleAutoSave() }
function onAddChild(parentId, tipo) { editorStore.addFilho(parentId, tipo); scheduleAutoSave() }
function onRemove(id)            { editorStore.removeElement(id); scheduleAutoSave() }

function onPromote(id) {
  const result = editorStore.promote(id)
  if (result && !result.ok) {
    const messages = {
      ceiling: 'Promoção bloqueada: o elemento está diretamente sob um artigo e promovê-lo o tornaria filho direto de um agrupamento (capítulo/seção/subseção), o que não é permitido.',
      'no-parent': 'Promoção bloqueada: o elemento já está no nível mais alto possível.',
    }
    $q.notify({
      type: 'warning',
      icon: 'mdi-alert-circle-outline',
      message: messages[result.reason] ?? 'Não foi possível promover o elemento.',
      position: 'bottom-right',
      timeout: 4000,
    })
  } else {
    scheduleAutoSave()
  }
}

function handleDemote(id) {
  const result = editorStore.demote(id)
  if (result && !result.ok) {
    const messages = {
      'at-bottom': 'Rebaixamento bloqueado: um subelemento já está no nível mais baixo (sub-alínea).',
      'invalid-sibling': 'Rebaixamento bloqueado: o elemento anterior é um agrupamento (capítulo/seção/subseção) e não pode receber subelementos.',
      'no-prev': 'Rebaixamento bloqueado: não há elemento anterior no mesmo nível para se tornar o novo pai.',
    }
    $q.notify({
      type: 'warning',
      icon: 'mdi-alert-circle-outline',
      message: messages[result.reason] ?? 'Não foi possível rebaixar o elemento.',
      position: 'bottom-right',
      timeout: 4000,
    })
  } else {
    scheduleAutoSave()
  }
}

function onMoveToParent(id, newParentId) {
  const result = editorStore.moveToParent(id, newParentId)
  if (result && !result.ok) {
    const messages = {
      cycle: 'Não é possível mover um elemento para dentro dele mesmo.',
      'invalid-parent': 'Destino inválido para este elemento.',
      'not-movable': 'Este elemento não pode ser movido.',
    }
    $q.notify({
      type: 'warning',
      icon: 'mdi-alert-circle-outline',
      message: messages[result.reason] ?? 'Não foi possível mover o elemento.',
      position: 'bottom-right',
      timeout: 4000,
    })
  } else {
    scheduleAutoSave()
  }
}

function addArtigo() {
  const secao = editorStore.normativaSecao
  if (!secao) return
  const novo = {
    id: crypto.randomUUID(),
    tipo: 'artigo',
    numero: 0,
    conteudo: '{"type":"doc","content":[{"type":"paragraph"}]}',
    filhos: [],
  }
  secao.elementos.push(novo)
  renumberElements(secao.elementos)
  editorStore.selectedElementId = novo.id
  editorStore.markUserEdit()
  scheduleAutoSave()
}

function addCapitulo(titulo) {
  editorStore.addCapitulo(titulo)
  scheduleAutoSave()
}
</script>

<style scoped>
/* Layout raiz: sidebar + coluna principal lado a lado */
.editor-page {
  display: flex;
  flex-direction: row;
  background: var(--color-background);
  overflow: hidden;
}

/* Coluna principal (topbar + body) */
.editor-main {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.editor-topbar {
  background: var(--color-surface);
  flex-shrink: 0;
  position: relative;
}

.editor-body {
  flex: 1 1 0;
  min-height: 0;
  flex-wrap: nowrap;
}

.editor-content {
  flex: 1 1 0;
  min-width: 0;
  background: var(--color-background);
}

.preview-panel {
  flex: 1 1 0;
  min-width: 320px;
  border-left: 2px solid rgba(0, 0, 0, 0.12);
  background: #525659;
}
.preview-panel :deep(.preview-inner-loading) {
  background: rgba(60, 63, 65, 0.92);
  flex-direction: column;
}

.element-editor {
  max-width: 800px;
  margin: 0 auto;
}

.element-header {
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12);
}

/* Indicador de salvamento — centralizado no topbar */
.save-indicator {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.78rem;
  font-weight: 500;
  white-space: nowrap;
  pointer-events: none;
  transition: background 0.2s, color 0.2s;
}
.save-indicator--saving {
  background: rgba(0, 0, 0, 0.07);
  color: #555;
}
.save-indicator--dirty {
  background: rgba(230, 81, 0, 0.12);
  color: #b45000;
}
.save-indicator--saved {
  background: rgba(46, 125, 50, 0.12);
  color: #2e7d32;
}
</style>
