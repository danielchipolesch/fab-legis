<template>
  <q-drawer :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" side="right" bordered :width="360">
    <div class="column full-height">
      <div class="row items-center q-pa-md">
        <q-icon name="mdi-comment-text-multiple-outline" color="primary" size="22px" class="q-mr-sm" />
        <div class="text-h6 text-primary">Comentários</div>
        <q-space />
        <q-btn icon="mdi-close" flat round dense @click="$emit('update:modelValue', false)" />
      </div>
      <q-separator />

      <div v-if="!elemento" class="q-pa-md text-grey-6 text-caption text-center">
        Selecione um elemento no editor para ver ou adicionar comentários sobre ele.
      </div>

      <template v-else>
        <div class="q-pa-md q-pb-sm">
          <div class="text-caption text-grey-7">Elemento selecionado</div>
          <div class="text-body2 text-weight-medium">{{ rotuloElemento }}</div>
        </div>

        <!-- Só aparece com 2+ elementos pendentes no documento -- com 1 só, o
             clique no ícone da topbar já leva direto pra lá (ver
             DocumentoEditorPage.vue abrirComentarios), não tem "próximo" a
             oferecer. -->
        <div v-if="elementosPendentes.length > 1" class="row items-center q-px-md q-pb-sm" style="gap:8px">
          <q-icon name="mdi-comment-alert-outline" color="deep-orange" size="16px" />
          <div class="text-caption text-grey-7 col">
            {{ indicePendenteAtual >= 0 ? `${indicePendenteAtual + 1} de ${elementosPendentes.length}` : elementosPendentes.length }}
            elemento{{ elementosPendentes.length > 1 ? 's' : '' }} com comentário pendente
          </div>
          <q-btn flat dense no-caps size="sm" color="primary" icon-right="mdi-arrow-right" label="Próximo" @click="irParaProximoPendente" />
        </div>

        <q-separator />

        <div v-if="!elementoIdBackend" class="q-pa-md text-grey-6 text-caption text-center">
          Este elemento ainda não foi salvo. Aguarde o salvamento automático para comentar nele.
        </div>

        <template v-else>

        <q-scroll-area class="col">
          <div class="q-pa-md">
            <q-spinner v-if="carregando" color="primary" size="24px" class="q-mb-md" />

            <div v-if="!carregando && threadsDoElemento.length === 0" class="text-grey-6 text-caption text-center q-py-md">
              Nenhum comentário neste elemento ainda.
            </div>

            <q-card v-for="raiz in threadsDoElemento" :key="raiz.id" flat bordered class="q-mb-md" :class="{ 'bg-grey-2': raiz.resolvido }">
              <q-card-section class="q-pb-xs">
                <div class="row items-start no-wrap" style="gap:8px">
                  <q-avatar size="28px" color="blue-2" text-color="primary">
                    <q-icon name="mdi-account" size="16px" />
                  </q-avatar>
                  <div class="col">
                    <div class="text-caption text-weight-medium">{{ caixaAlta(raiz.autorNome) }}</div>
                    <div class="text-caption text-grey-6">{{ formatarData(raiz.dtCriacao) }}</div>
                  </div>
                  <q-chip v-if="raiz.resolvido" dense color="positive" text-color="white" icon="mdi-check" size="sm">Resolvido</q-chip>
                </div>
                <div class="text-body2 q-mt-sm" style="white-space:pre-wrap">{{ raiz.texto }}</div>
              </q-card-section>

              <q-card-section v-for="resp in raiz.respostas" :key="resp.id" class="q-pt-none q-pl-xl">
                <div class="row items-start no-wrap" style="gap:8px">
                  <q-avatar size="24px" color="grey-3" text-color="grey-8">
                    <q-icon name="mdi-account" size="14px" />
                  </q-avatar>
                  <div class="col">
                    <div class="text-caption text-weight-medium">{{ caixaAlta(resp.autorNome) }}</div>
                    <div class="text-caption text-grey-6">{{ formatarData(resp.dtCriacao) }}</div>
                    <div class="text-body2" style="white-space:pre-wrap">{{ resp.texto }}</div>
                  </div>
                </div>
              </q-card-section>

              <q-card-actions align="right">
                <q-btn flat dense size="sm" color="primary" icon="mdi-reply" label="Responder" @click="respondendoA = respondendoA === raiz.id ? null : raiz.id" />
                <q-btn
                  flat dense size="sm"
                  :color="raiz.resolvido ? 'grey-7' : 'positive'"
                  :icon="raiz.resolvido ? 'mdi-restart' : 'mdi-check'"
                  :label="raiz.resolvido ? 'Reabrir' : 'Resolver'"
                  :loading="resolvendo === raiz.id"
                  @click="alternarResolvido(raiz)"
                />
              </q-card-actions>

              <q-card-section v-if="respondendoA === raiz.id" class="q-pt-none">
                <q-input
                  v-model="textoResposta"
                  outlined dense autogrow
                  placeholder="Escreva uma resposta…"
                  @keyup.enter.ctrl="enviarResposta(raiz)"
                />
                <div class="row justify-end q-mt-xs" style="gap:8px">
                  <q-btn flat dense size="sm" label="Cancelar" @click="respondendoA = null" />
                  <q-btn unelevated dense size="sm" color="primary" label="Enviar" :loading="enviando" :disable="!textoResposta.trim()" @click="enviarResposta(raiz)" />
                </div>
              </q-card-section>
            </q-card>
          </div>
        </q-scroll-area>

        <q-separator />
        <div class="q-pa-md">
          <q-input
            v-model="novoTexto"
            outlined dense autogrow
            placeholder="Novo comentário sobre este elemento…"
            @keyup.enter.ctrl="enviarNovo"
          />
          <div class="row justify-end q-mt-xs">
            <q-btn unelevated dense color="primary" label="Comentar" icon="mdi-send" :loading="enviando" :disable="!novoTexto.trim()" @click="enviarNovo" />
          </div>
        </div>
        </template>
      </template>
    </div>
  </q-drawer>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import * as api from '@/api/comentarios.js'
import { formatLabel } from '@/utils/numbering.js'
import { idPersistido } from '@/api/documentos.js'
import { caixaAlta } from '@/utils/texto.js'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  documentoId: { type: [String, Number], required: true },
  elemento: { type: Object, default: null }, // elemento selecionado no editor (ver editorStore.selectedElement)
  secao: { type: String, default: 'PARTE_NORMATIVA' },
})
const emit = defineEmits(['update:modelValue', 'contagem', 'selecionar-elemento'])

const $q = useQuasar()
const comentarios = ref([])
const carregando = ref(false)
const novoTexto = ref('')
const textoResposta = ref('')
const respondendoA = ref(null)
const enviando = ref(false)
const resolvendo = ref(null)

const rotuloElemento = computed(() => props.elemento ? formatLabel(props.elemento) : '')

// Elemento recém-criado no editor ainda tem só um id local (UUID) até o primeiro
// autosave persistir de verdade no backend -- backendId cobre quem já foi salvo ao
// menos uma vez nesta sessão (mesma lógica de converterElemento em api/documentos.js).
// Comentário sempre referencia o id NUMÉRICO do backend, nunca o UUID local.
const elementoIdBackend = computed(() => {
  if (!props.elemento) return null
  return props.elemento.backendId ?? idPersistido(props.elemento.id)
})

// Agrupa em threads: raízes (sem parentId) com suas respostas aninhadas, só do
// elemento atualmente selecionado -- o painel mostra um elemento de cada vez.
const threadsDoElemento = computed(() => {
  if (!elementoIdBackend.value) return []
  const doElemento = comentarios.value.filter(c => c.elementoId === elementoIdBackend.value)
  const raizes = doElemento.filter(c => !c.parentId)
  return raizes
    .map(raiz => ({ ...raiz, respostas: doElemento.filter(c => c.parentId === raiz.id) }))
    .sort((a, b) => new Date(a.dtCriacao) - new Date(b.dtCriacao))
})

// Um elemento por comentário-raiz não resolvido, mais recente primeiro -- mesmo
// critério de "pra onde ir" que DocumentoEditorPage.vue usa no clique do ícone.
// Alimenta o "Próximo" abaixo: sem isso, quem tem 3 elementos com pendência só
// consegue ver o mais recente (o clique no ícone só pula pra lá uma vez) e fica
// sem saber como chegar aos outros dois sem resolver o primeiro.
const elementosPendentes = computed(() => {
  const raizesNaoResolvidas = comentarios.value
    .filter(c => !c.parentId && !c.resolvido)
    .sort((a, b) => new Date(b.dtCriacao) - new Date(a.dtCriacao))
  const vistos = new Set()
  const ordem = []
  for (const c of raizesNaoResolvidas) {
    if (!vistos.has(c.elementoId)) { vistos.add(c.elementoId); ordem.push(c.elementoId) }
  }
  return ordem
})

const indicePendenteAtual = computed(() => elementosPendentes.value.indexOf(elementoIdBackend.value))

function irParaProximoPendente() {
  if (elementosPendentes.value.length < 2) return
  const proximo = (indicePendenteAtual.value + 1) % elementosPendentes.value.length
  emit('selecionar-elemento', elementosPendentes.value[proximo])
}

async function carregar() {
  carregando.value = true
  try {
    comentarios.value = await api.listComentarios(props.documentoId)
    emit('contagem', comentarios.value.filter(c => !c.resolvido && !c.parentId).length)
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao carregar comentários: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    carregando.value = false
  }
}

watch(() => props.modelValue, (aberto) => { if (aberto) carregar() })

async function enviarNovo() {
  if (!novoTexto.value.trim() || !elementoIdBackend.value) return
  enviando.value = true
  try {
    await api.criarComentario(props.documentoId, {
      elementoId: elementoIdBackend.value, secao: props.secao, texto: novoTexto.value.trim(),
    })
    novoTexto.value = ''
    await carregar()
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao comentar: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    enviando.value = false
  }
}

async function enviarResposta(raiz) {
  if (!textoResposta.value.trim()) return
  enviando.value = true
  try {
    await api.criarComentario(props.documentoId, {
      elementoId: raiz.elementoId, secao: raiz.secao, texto: textoResposta.value.trim(), parentId: raiz.id,
    })
    textoResposta.value = ''
    respondendoA.value = null
    await carregar()
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao responder: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    enviando.value = false
  }
}

async function alternarResolvido(raiz) {
  resolvendo.value = raiz.id
  try {
    if (raiz.resolvido) await api.reabrirComentario(props.documentoId, raiz.id)
    else await api.resolverComentario(props.documentoId, raiz.id)
    await carregar()
  } catch (e) {
    $q.notify({ type: 'negative', message: `Erro ao atualizar comentário: ${e?.message ?? 'erro desconhecido'}` })
  } finally {
    resolvendo.value = null
  }
}

function formatarData(dt) {
  if (!dt) return ''
  return new Date(dt).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}
</script>
